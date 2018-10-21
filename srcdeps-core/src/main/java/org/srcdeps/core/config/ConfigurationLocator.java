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
package org.srcdeps.core.config;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.srcdeps.core.config.Configuration.Builder;

/**
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */
public class ConfigurationLocator {
    private static final Logger log = LoggerFactory.getLogger(ConfigurationLocator.class);

    /** Before srcdeps-maven 3.1.0 this used to be the default location of srcdeps.yaml file */
    private static final Path MVN_SRCDEPS_YAML_PATH = Paths.get(".mvn", "srcdeps.yaml");

    /**
     * Since srcdeps-maven 3.1.0 this is the default location of srcdeps.yaml file for Maven and all other build tools
     */
    private static final Path SRCDEPS_YAML_PATH = Paths.get("srcdeps.yaml");

    private static Configuration.Builder locateUrl(final ConfigurationReader configurationReader, final String url,
            final Charset encoding) throws ConfigurationException {
        try {
            if (url.startsWith("file://")) {
                final Path path = Paths.get(new URI(url));
                if (Files.exists(path)) {
                    try (BufferedReader reader = Files.newBufferedReader(path, encoding)) {
                        return configurationReader.read(reader);
                    }
                } else {
                    throw new ConfigurationException(String.format("The file [%s] specified via %s does not exist", url,
                            Configuration.getSrcdepsMasterConfigProperty()));
                }
            } else {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(new URL(url).openStream(), encoding))) {
                    return configurationReader.read(reader);
                }
            }
        } catch (IOException | URISyntaxException e) {
            throw new ConfigurationException(String.format("Could not read [%s] specified via %s", url,
                    Configuration.getSrcdepsMasterConfigProperty()), e);
        }
    }

    public static ConfigurationLocator ofSystemProperties() {
        return new ConfigurationLocator(System.getProperties(), false);
    }

    private static Configuration.Builder read(ConfigurationReader configurationReader, Charset encoding,
            final Path path) throws ConfigurationException {
        try (BufferedReader reader = Files.newBufferedReader(path, encoding)) {
            final Builder result = configurationReader.read(reader);
            final Boolean forwardAsMasterConfig = result.forwardAsMasterConfig.getValue();
            if (forwardAsMasterConfig != null && forwardAsMasterConfig.booleanValue()) {
                result.forwardPropertyValue(Configuration.getSrcdepsMasterConfigProperty(), path.toUri().toString());
            }
            return result;
        } catch (IOException e) {
            throw new ConfigurationException(String.format("Could not read [%s]", path), e);
        }
    }

    private final boolean considerMvnSubdirectory;

    private final Properties systemProperties;

    public ConfigurationLocator(Properties systemProperties, boolean condiderMvnSubdirectory) {
        super();
        this.systemProperties = systemProperties;
        this.considerMvnSubdirectory = condiderMvnSubdirectory;
    }

    public Configuration.Builder locate(Path sourceTreeRoot, ConfigurationReader configurationReader)
            throws ConfigurationException {
        final String srcdepsMasterConfigUrl = systemProperties
                .getProperty(Configuration.getSrcdepsMasterConfigProperty());
        final Charset encoding = Charset
                .forName(systemProperties.getProperty(Configuration.getSrcdepsEncodingProperty(), "utf-8"));
        if (srcdepsMasterConfigUrl != null) {
            return locateUrl(configurationReader, srcdepsMasterConfigUrl, encoding);
        } else {
            return locateLocal(sourceTreeRoot, configurationReader, encoding);
        }
    }

    private Configuration.Builder locateLocal(Path sourceTreeRoot, ConfigurationReader configurationReader,
            Charset encoding) throws ConfigurationException {
        final Path defaultSrcdepsYamlPath = sourceTreeRoot.resolve(SRCDEPS_YAML_PATH);
        if (Files.exists(defaultSrcdepsYamlPath)) {
            return read(configurationReader, encoding, defaultSrcdepsYamlPath);
        } else if (considerMvnSubdirectory) {
            final Path legacySrcdepsYamlPath = sourceTreeRoot.resolve(MVN_SRCDEPS_YAML_PATH);
            if (Files.exists(legacySrcdepsYamlPath)) {
                return read(configurationReader, encoding, legacySrcdepsYamlPath);
            } else {
                log.warn(
                        "srcdeps: Could not locate srcdeps configuration at neither [{}] nor [{}], defaulting to an empty configuration",
                        defaultSrcdepsYamlPath, legacySrcdepsYamlPath);
                return Configuration.builder();
            }
        } else {
            log.warn("srcdeps: Could not locate srcdeps configuration at [{}], defaulting to an empty configuration",
                    defaultSrcdepsYamlPath);
            return Configuration.builder();
        }
    }
}
