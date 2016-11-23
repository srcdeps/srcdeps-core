/**
 * Copyright 2015-2016 Maven Source Dependencies
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

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Properties;

import org.junit.Assert;
import org.junit.Test;
import org.srcdeps.core.BuildRequest.Verbosity;

public class ConfigurationOverlayTest {
    @Test
    public void appendListOfStrings() {

        Configuration nonOverlayed = Configuration.builder().addDefaultFailWithAnyOfArguments(false)
                .failWithAnyOfArgument("fail1").build();
        Assert.assertEquals(false, nonOverlayed.isAddDefaultFailWithAnyOfArguments());
        Assert.assertEquals(Collections.singleton("fail1"), nonOverlayed.getFailWithAnyOfArguments());

        Properties props = new Properties();
        props.put("srcdeps.failWithAnyOfArguments[1]", "FAIL2");

        Configuration overlayed = Configuration.builder().addDefaultFailWithAnyOfArguments(false)
                .failWithAnyOfArgument("fail1")
                .accept(new ConfigurationOverrideVisitor(props)).build();

        Assert.assertEquals(false, overlayed.isAddDefaultFailWithAnyOfArguments());
        Assert.assertEquals(new LinkedHashSet<>(Arrays.asList("fail1", "FAIL2")), overlayed.getFailWithAnyOfArguments());

    }

    @Test
    public void overlayBoolean() {

        Configuration nonOverlayed = Configuration.builder().addDefaultFailWithAnyOfArguments(false).build();
        Assert.assertEquals(false, nonOverlayed.isAddDefaultFailWithAnyOfArguments());

        Properties props = new Properties();
        props.put("srcdeps.addDefaultFailWithAnyOfArguments", "true");
        Configuration overlayed = Configuration.builder().addDefaultFailWithAnyOfArguments(false)
                .accept(new ConfigurationOverrideVisitor(props)).build();
        Assert.assertEquals(true, overlayed.isAddDefaultFailWithAnyOfArguments());

    }

    @Test
    public void overlayListOfStrings() {

        Configuration nonOverlayed = Configuration.builder().addDefaultFailWithAnyOfArguments(false)
                .failWithAnyOfArgument("fail1").build();
        Assert.assertEquals(false, nonOverlayed.isAddDefaultFailWithAnyOfArguments());
        Assert.assertEquals(Collections.singleton("fail1"), nonOverlayed.getFailWithAnyOfArguments());

        Properties props = new Properties();
        props.put("srcdeps.failWithAnyOfArguments", "FAIL2");

        Configuration overlayed = Configuration.builder().addDefaultFailWithAnyOfArguments(false)
                .failWithAnyOfArgument("fail1")
                .accept(new ConfigurationOverrideVisitor(props)).build();

        Assert.assertEquals(false, overlayed.isAddDefaultFailWithAnyOfArguments());
        Assert.assertEquals(Collections.singleton("FAIL2"), overlayed.getFailWithAnyOfArguments());

    }

    @Test
    public void overlayListOfStringsByEmpty() {

        Configuration nonOverlayed = Configuration.builder().addDefaultFailWithAnyOfArguments(false)
                .failWithAnyOfArgument("fail1").build();
        Assert.assertEquals(false, nonOverlayed.isAddDefaultFailWithAnyOfArguments());
        Assert.assertEquals(Collections.singleton("fail1"), nonOverlayed.getFailWithAnyOfArguments());

        Properties props = new Properties();
        props.put("srcdeps.failWithAnyOfArguments", "");

        Configuration overlayed = Configuration.builder().addDefaultFailWithAnyOfArguments(false)
                .failWithAnyOfArgument("fail1")
                .accept(new ConfigurationOverrideVisitor(props)).build();

        Assert.assertEquals(false, overlayed.isAddDefaultFailWithAnyOfArguments());
        Assert.assertEquals(Collections.emptySet(), overlayed.getFailWithAnyOfArguments());

    }


    @Test
    public void overlayPath() {

        Path myDir = Paths.get("/my/dir");

        Configuration nonOverlayed = Configuration.builder().sourcesDirectory(myDir).build();
        Assert.assertEquals(myDir, nonOverlayed.getSourcesDirectory());

        Properties props = new Properties();
        props.put("srcdeps.sourcesDirectory", "/your/dir");
        Configuration overlayed = Configuration.builder().sourcesDirectory(myDir)
                .accept(new ConfigurationOverrideVisitor(props)).build();
        Assert.assertEquals(Paths.get("/your/dir"), overlayed.getSourcesDirectory());

    }


    @Test
    public void overlayScmRepositoryUrl() {

        Configuration nonOverlayed = Configuration.builder()
                .repository(ScmRepository.builder().id("repo1").selector("org.example").url("file:///whereever"))
                .build();
        Assert.assertEquals(
                ScmRepository.builder().id("repo1").selector("org.example").url("file:///whereever").build(),
                nonOverlayed.getRepositories().iterator().next());

        Properties props = new Properties();
        props.put("srcdeps.repositories[repo1].urls[1]", "file:///here");

        Configuration overlayed = Configuration.builder()
                .repository(ScmRepository.builder().id("repo1").selector("org.example").url("file:///whereever"))
                .accept(new ConfigurationOverrideVisitor(props)).build();

        Assert.assertEquals(
                ScmRepository.builder().id("repo1").selector("org.example").url("file:///whereever").url("file:///here").build(),
                overlayed.getRepositories().iterator().next());

    }


    @Test
    public void overlayVerbosity() {


        Configuration nonOverlayed = Configuration.builder().verbosity(Verbosity.trace).build();
        Assert.assertEquals(Verbosity.trace, nonOverlayed.getVerbosity());

        Properties props = new Properties();
        props.put("srcdeps.verbosity", "debug");
        Configuration overlayed = Configuration.builder().verbosity(Verbosity.trace)
                .accept(new ConfigurationOverrideVisitor(props)).build();
        Assert.assertEquals(Verbosity.debug, overlayed.getVerbosity());

    }

    @Test
    public void prependListOfStrings() {

        Configuration nonOverlayed = Configuration.builder().addDefaultFailWithAnyOfArguments(false)
                .failWithAnyOfArgument("fail1").build();
        Assert.assertEquals(false, nonOverlayed.isAddDefaultFailWithAnyOfArguments());
        Assert.assertEquals(Collections.singleton("fail1"), nonOverlayed.getFailWithAnyOfArguments());

        Properties props = new Properties();
        props.put("srcdeps.failWithAnyOfArguments[-1]", "FAIL2");

        Configuration overlayed = Configuration.builder().addDefaultFailWithAnyOfArguments(false)
                .failWithAnyOfArgument("fail1")
                .accept(new ConfigurationOverrideVisitor(props)).build();

        Assert.assertEquals(false, overlayed.isAddDefaultFailWithAnyOfArguments());
        Assert.assertEquals(new LinkedHashSet<>(Arrays.asList("FAIL2", "fail1")), overlayed.getFailWithAnyOfArguments());

    }

    @Test
    public void replaceScmRepositoryUrl() {

        Configuration nonOverlayed = Configuration.builder()
                .repository(ScmRepository.builder().id("repo1").selector("org.example").url("file:///whereever"))
                .build();
        Assert.assertEquals(
                ScmRepository.builder().id("repo1").selector("org.example").url("file:///whereever").build(),
                nonOverlayed.getRepositories().iterator().next());

        Properties props = new Properties();
        props.put("srcdeps.repositories[repo1].urls[0]", "file:///here");

        Configuration overlayed = Configuration.builder()
                .repository(ScmRepository.builder().id("repo1").selector("org.example").url("file:///whereever"))
                .accept(new ConfigurationOverrideVisitor(props)).build();

        Assert.assertEquals(
                ScmRepository.builder().id("repo1").selector("org.example").url("file:///here").build(),
                overlayed.getRepositories().iterator().next());

    }
}
