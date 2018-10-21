/**
 * Copyright 2015-2018 Maven Source Dependencies
 * Plugin contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.srcdeps.config.yaml;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

import org.junit.Assert;
import org.junit.Test;
import org.srcdeps.core.config.Configuration;
import org.srcdeps.core.config.ConfigurationException;
import org.srcdeps.core.config.ConfigurationLocator;
import org.srcdeps.core.config.tree.walk.DefaultsAndInheritanceVisitor;
import org.srcdeps.core.config.tree.walk.OverrideVisitor;

public class ConfigurationLocatorTest {
    private static final Path BASEDIR = Paths.get(System.getProperty("project.basedir", "."));
    private static final Path SRCDEPS_YAML_IN_ROOT_PATH;
    private static final Path SRCDEPS_MASTER_CONFIG_PATH;
    private static final Path SRCDEPS_YAML_IN_ROOT_FORWARD_AS_MASTER_CONFIG_PATH;

    static {
        SRCDEPS_YAML_IN_ROOT_PATH = BASEDIR.resolve("target/test-classes/ConfigurationLocator/srcdeps-yaml-in-root")
                .normalize().toAbsolutePath();
        SRCDEPS_YAML_IN_ROOT_FORWARD_AS_MASTER_CONFIG_PATH = BASEDIR
                .resolve("target/test-classes/ConfigurationLocator/srcdeps-yaml-in-root-forwardAsMasterConfig")
                .normalize().toAbsolutePath();
        SRCDEPS_MASTER_CONFIG_PATH = BASEDIR.resolve("target/test-classes/ConfigurationLocator/srcdeps-master.yaml")
                .normalize().toAbsolutePath();
    }

    @Test
    public void forwardAsMasterConfig() throws IOException, ConfigurationException {
        final Properties props = new Properties();
        final Configuration expected = Configuration.builder() //
                .sourcesDirectory(Paths.get("in-root-forwardAsMasterConfig")) //
                .forwardAsMasterConfig(true) //
                .accept(new DefaultsAndInheritanceVisitor()) //
                .accept(new OverrideVisitor(props)) //
                .build();
        final Configuration actual = assertSrcdepsYamlContent(SRCDEPS_YAML_IN_ROOT_FORWARD_AS_MASTER_CONFIG_PATH, props,
                expected);

        final Map<String, String> expectedFwdPropValues = new LinkedHashMap<>();
        expectedFwdPropValues.put(Configuration.getSrcdepsMasterConfigProperty(),
                SRCDEPS_YAML_IN_ROOT_FORWARD_AS_MASTER_CONFIG_PATH.resolve("srcdeps.yaml").toUri().toString());
        Assert.assertEquals(expectedFwdPropValues, actual.getForwardPropertyValues());

    }

    @Test
    public void masterPropertyPrecedesForwardAsMasterConfig() throws IOException, ConfigurationException {
        final Properties props = new Properties();
        props.setProperty(Configuration.getSrcdepsMasterConfigProperty(),
                SRCDEPS_MASTER_CONFIG_PATH.toUri().toString());
        final Configuration expected = Configuration.builder() //
                .sourcesDirectory(Paths.get("master")) //
                .accept(new DefaultsAndInheritanceVisitor()) //
                .accept(new OverrideVisitor(props)) //
                .build();
        final Configuration actual = assertSrcdepsYamlContent(SRCDEPS_YAML_IN_ROOT_FORWARD_AS_MASTER_CONFIG_PATH, props,
                expected);

        final Map<String, String> expectedFwdPropValues = new LinkedHashMap<>();
        expectedFwdPropValues.put(Configuration.getSrcdepsMasterConfigProperty(),
                SRCDEPS_MASTER_CONFIG_PATH.toUri().toString());
        Assert.assertEquals(expectedFwdPropValues, actual.getForwardPropertyValues());
    }

    @Test
    public void srcdepdYamlInRoot() throws IOException, ConfigurationException {
        final Properties props = new Properties();
        final Configuration expected = Configuration.builder() //
                .sourcesDirectory(Paths.get("in-root")) //
                .accept(new DefaultsAndInheritanceVisitor()) //
                .accept(new OverrideVisitor(props)) //
                .build();
        assertSrcdepsYamlContent(SRCDEPS_YAML_IN_ROOT_PATH, props, expected);
    }

    @Test
    public void srcdepdYamlFromMasterProperty() throws IOException, ConfigurationException {
        final Properties props = new Properties();
        props.setProperty(Configuration.getSrcdepsMasterConfigProperty(),
                SRCDEPS_MASTER_CONFIG_PATH.toUri().toString());
        final Configuration expected = Configuration.builder() //
                .sourcesDirectory(Paths.get("master")) //
                .accept(new DefaultsAndInheritanceVisitor()) //
                .accept(new OverrideVisitor(props)) //
                .build();
        assertSrcdepsYamlContent(SRCDEPS_YAML_IN_ROOT_PATH, props, expected);
    }

    private static Configuration assertSrcdepsYamlContent(Path sourceTreeRoot, Properties props, Configuration expected)
            throws IOException, ConfigurationException {
        final ConfigurationLocator cl = new ConfigurationLocator(props, true);
        final Configuration actual = cl.locate(sourceTreeRoot, new YamlConfigurationReader()) //
                .accept(new DefaultsAndInheritanceVisitor()) //
                .accept(new OverrideVisitor(props)) //
                .build();
        Assert.assertEquals(expected, actual);
        return actual;
    }
}
