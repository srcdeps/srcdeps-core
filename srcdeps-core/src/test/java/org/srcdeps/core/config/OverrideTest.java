/**
 * Copyright 2015-2017 Maven Source Dependencies
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
import java.util.Properties;

import org.junit.Assert;
import org.junit.Test;
import org.srcdeps.core.BuildRequest.Verbosity;
import org.srcdeps.core.config.tree.walk.OverrideVisitor;

public class OverrideTest {
    @Test
    public void appendScmRepositoryUrl() {

        Configuration.Builder config = Configuration.builder() //
                .repository( //
                        ScmRepository.builder() //
                                .id("org.repo1") //
                                .selector("org.example") //
                                .url("file:///whereever") //
        );
        ScmRepository.Builder nonOverlayedRepo = config.repositories.getChildren().get("org.repo1");
        Assert.assertEquals("org.repo1", nonOverlayedRepo.getName());
        Assert.assertEquals(Collections.singletonList("org.example"), nonOverlayedRepo.selectors.asListOfValues());
        Assert.assertEquals(Collections.singletonList("file:///whereever"), nonOverlayedRepo.urls.asListOfValues());

        Properties props = new Properties();
        props.put("srcdeps.repositories[org.repo1].urls[1]", "file:///here");

        config.accept(new OverrideVisitor(props));

        ScmRepository.Builder overlayedRepo = config.repositories.getChildren().get("org.repo1");
        Assert.assertSame(nonOverlayedRepo, overlayedRepo);

        Assert.assertEquals("org.repo1", nonOverlayedRepo.getName());
        Assert.assertEquals(Collections.singletonList("org.example"), nonOverlayedRepo.selectors.asListOfValues());
        Assert.assertEquals(Arrays.asList("file:///whereever", "file:///here"), nonOverlayedRepo.urls.asListOfValues());

    }

    @Test
    public void modifyListOfStrings() {

        Configuration.Builder config = Configuration.builder() //
                .forwardProperty("fwd1") //
                .forwardProperty("fwd2") //
                .maven( //
                        Maven.builder() //
                                .versionsMavenPluginVersion("0.1") //
                                .failWith( //
                                        MavenAssertions.failWithBuilder() //
                                                .addDefaults(false) //
                                                .goal("g1") //
                                                .profile("p1") //
                                                .property("prop1") //
                        ) //
        );
        Assert.assertEquals(Arrays.asList("fwd1", "fwd2"), config.forwardProperties.asListOfValues());

        MavenAssertions.FailWithBuilder failWith = config.maven.failWith;
        Assert.assertEquals(false, failWith.addDefaults.getValue());
        Assert.assertEquals(Collections.singleton("g1"), failWith.goals.asSetOfValues());
        Assert.assertEquals(Collections.singleton("p1"), failWith.profiles.asSetOfValues());
        Assert.assertEquals(Collections.singleton("prop1"), failWith.properties.asSetOfValues());

        Properties props = new Properties();
        props.put("srcdeps.forwardProperties", ""); // replace by empty
        props.put("srcdeps.maven.failWith.goals[1]", "g2"); // append
        props.put("srcdeps.maven.failWith.profiles", "pro1,pro2"); // replace
        props.put("srcdeps.maven.failWith.properties[-1]", "prop0"); // prepend

        config.accept(new OverrideVisitor(props));

        MavenAssertions.FailWithoutBuilder overlayedFailWith = config.maven.failWith;

        Assert.assertSame(failWith, overlayedFailWith);

        Assert.assertEquals(Collections.emptyList(), config.forwardProperties.asListOfValues());
        Assert.assertEquals(false, failWith.addDefaults.getValue());
        Assert.assertEquals(Arrays.asList("g1", "g2"), failWith.goals.asListOfValues());
        Assert.assertEquals(Arrays.asList("pro1", "pro2"), failWith.profiles.asListOfValues());
        Assert.assertEquals(Arrays.asList("prop0", "prop1"), failWith.properties.asListOfValues());

    }

    @Test
    public void overrideBoolean() {

        Configuration.Builder config = Configuration.builder().skip(true);
        Assert.assertEquals(true, config.skip.getValue());

        Properties props = new Properties();
        props.put("srcdeps.skip", "false");
        config.accept(new OverrideVisitor(props));
        Assert.assertEquals(false, config.skip.getValue());

    }

    @Test
    public void overridePath() {

        Path myDir = Paths.get("/my/dir");

        Configuration.Builder config = Configuration.builder().sourcesDirectory(myDir);
        Assert.assertEquals(myDir, config.sourcesDirectory.getValue());

        Properties props = new Properties();
        props.put("srcdeps.sourcesDirectory", "/your/dir");
        config.accept(new OverrideVisitor(props));
        Assert.assertEquals(Paths.get("/your/dir"), config.sourcesDirectory.getValue());

    }

    @Test
    public void overrideScmRepositoryMaven() {

        Configuration.Builder config = Configuration.builder()
                .repository(ScmRepository.builder() //
                        .id("org.repo1") //
                        .selector("org.example") //
                        .url("file:///whereever") //
                        .maven( //
                                ScmRepositoryMaven.builder() //
                                        .versionsMavenPluginVersion("1.2")) //
        );

        ScmRepository.Builder nonOverlayedRepo = config.repositories.getChildren().get("org.repo1");
        Assert.assertEquals("org.repo1", nonOverlayedRepo.getName());
        Assert.assertEquals(Collections.singletonList("org.example"), nonOverlayedRepo.selectors.asListOfValues());
        Assert.assertEquals(Collections.singletonList("file:///whereever"), nonOverlayedRepo.urls.asListOfValues());
        Assert.assertEquals("1.2", nonOverlayedRepo.maven.versionsMavenPluginVersion.getValue());

        Properties props = new Properties();
        props.put("srcdeps.repositories[org.repo1].maven.versionsMavenPluginVersion", "1.3");

        config.accept(new OverrideVisitor(props));

        ScmRepository.Builder overlayedRepo = config.repositories.getChildren().get("org.repo1");
        Assert.assertSame(nonOverlayedRepo, overlayedRepo);
        Assert.assertEquals("org.repo1", nonOverlayedRepo.getName());
        Assert.assertEquals(Collections.singletonList("org.example"), nonOverlayedRepo.selectors.asListOfValues());
        Assert.assertEquals(Collections.singletonList("file:///whereever"), nonOverlayedRepo.urls.asListOfValues());
        Assert.assertEquals("1.3", nonOverlayedRepo.maven.versionsMavenPluginVersion.getValue());

    }

    @Test
    public void overrideVerbosity() {

        Configuration.Builder config = Configuration.builder().verbosity(Verbosity.trace);
        Assert.assertEquals(Verbosity.trace, config.verbosity.getValue());

        Properties props = new Properties();
        props.put("srcdeps.verbosity", "debug");
        config.accept(new OverrideVisitor(props));
        Assert.assertEquals(Verbosity.debug, config.verbosity.getValue());

    }

    @Test
    public void replaceScmRepositoryUrl() {

        Configuration.Builder config = Configuration.builder() //
                .skip(false) //
                .repository( //
                        ScmRepository.builder() //
                                .id("repo1") //
                                .selector("org.example") //
                                .url("file:///whereever") //
        );
        ScmRepository.Builder nonOverlayedRepo = config.repositories.getChildren().get("repo1");
        Assert.assertEquals("repo1", nonOverlayedRepo.getName());
        Assert.assertEquals(Collections.singletonList("org.example"), nonOverlayedRepo.selectors.asListOfValues());
        Assert.assertEquals(Collections.singletonList("file:///whereever"), nonOverlayedRepo.urls.asListOfValues());

        Properties props = new Properties();
        props.put("srcdeps.repositories[repo1].urls[0]", "file:///here");

        config.accept(new OverrideVisitor(props));

        ScmRepository.Builder overlayedRepo = config.repositories.getChildren().get("repo1");
        Assert.assertSame(nonOverlayedRepo, overlayedRepo);

        Assert.assertEquals("repo1", nonOverlayedRepo.getName());
        Assert.assertEquals(Collections.singletonList("org.example"), nonOverlayedRepo.selectors.asListOfValues());
        Assert.assertEquals(Collections.singletonList("file:///here"), nonOverlayedRepo.urls.asListOfValues());

    }
}
