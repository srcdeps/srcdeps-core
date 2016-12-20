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

import java.util.Collections;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Test;
import org.srcdeps.core.BuildRequest.Verbosity;
import org.srcdeps.core.config.ScmRepository.Builder;
import org.srcdeps.core.config.scalar.Duration;
import org.srcdeps.core.config.tree.walk.DefaultsAndInheritanceVisitor;

public class DefaultsAndInheritanceTest {
    @Test
    public void defaults() {

        Configuration.Builder configBuilder = Configuration.builder() //
                .repository(//
                        ScmRepository.builder() //
                                .id("repo1") //
                                .selector("org.example") //
                                .url("file:///whereever") //
        );

        Assert.assertNull(configBuilder.configModelVersion.getValue());
        Assert.assertEquals(Collections.emptySet(), configBuilder.forwardProperties.asSetOfValues());
        Assert.assertNull(configBuilder.skip.getValue());
        Assert.assertNull(configBuilder.sourcesDirectory.getValue());
        Assert.assertNull(configBuilder.verbosity.getValue());
        Assert.assertNull(configBuilder.buildTimeout.getValue());

        Assert.assertNull(configBuilder.maven.versionsMavenPluginVersion.getValue());
        MavenFailWith.Builder failWithBuilder = configBuilder.maven.failWith;
        Assert.assertNull(failWithBuilder.addDefaults.getValue());
        Assert.assertEquals(Collections.emptySet(), failWithBuilder.goals.asSetOfValues());
        Assert.assertEquals(Collections.emptySet(), failWithBuilder.profiles.asSetOfValues());
        Assert.assertEquals(Collections.emptySet(), failWithBuilder.properties.asSetOfValues());

        ScmRepository.Builder repo1Builder = configBuilder.repositories.getChildren().get("repo1");
        Assert.assertEquals("repo1", repo1Builder.getName());
        Assert.assertNull(repo1Builder.addDefaultBuildArguments.getValue());
        Assert.assertEquals(Collections.emptyList(), repo1Builder.buildArguments.asListOfValues());
        Assert.assertNull(repo1Builder.maven.versionsMavenPluginVersion.getValue());
        Assert.assertEquals(Collections.singletonList("org.example"), repo1Builder.selectors.asListOfValues());
        Assert.assertNull(repo1Builder.skipTests.getValue());
        Assert.assertEquals(Collections.singletonList("file:///whereever"), repo1Builder.urls.asListOfValues());
        Assert.assertNull(repo1Builder.buildTimeout.getValue());

        configBuilder.accept(new DefaultsAndInheritanceVisitor());

        Configuration config = configBuilder.build();
        Assert.assertEquals(Configuration.getLatestConfigModelVersion(), config.getConfigModelVersion());
        Assert.assertEquals(Configuration.getDefaultForwardProperties(), config.getForwardProperties());
        Assert.assertEquals(false, config.isSkip());
        Assert.assertNull(config.getSourcesDirectory());
        Assert.assertEquals(Verbosity.warn, config.getVerbosity());
        Assert.assertEquals(Duration.maxValue(), configBuilder.buildTimeout.getValue());

        Assert.assertEquals(Maven.getDefaultVersionsMavenPluginVersion(),
                configBuilder.maven.versionsMavenPluginVersion.getValue());
        MavenFailWith failWith = config.getMaven().getFailWith();
        Assert.assertEquals(true, failWith.isAddDefaults());
        Assert.assertEquals(MavenFailWith.getDefaultFailGoals(), failWith.getGoals());
        Assert.assertEquals(Collections.emptySet(), failWith.getProfiles());
        Assert.assertEquals(Collections.emptySet(), failWith.getProperties());

        ScmRepository repo1 = config.getRepositories().iterator().next();
        Assert.assertEquals("repo1", repo1.getId());
        Assert.assertEquals(true, repo1.isAddDefaultBuildArguments());
        Assert.assertEquals(Collections.emptyList(), repo1.getBuildArguments());
        Assert.assertEquals(Collections.singletonList("org.example"), repo1.getSelectors());
        Assert.assertEquals(true, repo1.isSkipTests());
        Assert.assertEquals(Collections.singletonList("file:///whereever"), repo1.getUrls());
        Assert.assertEquals(Maven.getDefaultVersionsMavenPluginVersion(),
                repo1.getMaven().getVersionsMavenPluginVersion());
        Assert.assertEquals(Duration.maxValue(), repo1.getBuildTimeout());

    }

    @Test
    public void failWithDefaults() {
        Configuration.Builder config = Configuration.builder();
        Assert.assertEquals(Collections.emptySet(), config.maven.failWith.goals.asSetOfValues());

        config.accept(new DefaultsAndInheritanceVisitor());
        Assert.assertEquals(MavenFailWith.getDefaultFailGoals(), config.maven.failWith.goals.asSetOfValues());
    }

    @Test
    public void inheritance() {

        Configuration.Builder config = Configuration.builder() //
                .buildTimeout(Duration.of("32m")) //
                .maven( //
                        Maven.builder() //
                                .versionsMavenPluginVersion("0.1")) //
                .repository(//
                        ScmRepository.builder() //
                                .id("repo1") //
                                .selector("org.example") //
                                .url("file:///whereever") //
        );

        Assert.assertEquals(new Duration(32, TimeUnit.MINUTES), config.buildTimeout.getValue());
        Assert.assertEquals("0.1", config.maven.versionsMavenPluginVersion.getValue());

        Builder repo1 = config.repositories.getChildren().get("repo1");
        Assert.assertNull(repo1.buildTimeout.getValue());
        Assert.assertNull(repo1.maven.versionsMavenPluginVersion.getValue());

        config.accept(new DefaultsAndInheritanceVisitor());

        Assert.assertEquals(new Duration(32, TimeUnit.MINUTES), config.buildTimeout.getValue());
        Assert.assertEquals("0.1", config.maven.versionsMavenPluginVersion.getValue());
        Assert.assertEquals(new Duration(32, TimeUnit.MINUTES), repo1.buildTimeout.getValue());
        Assert.assertEquals("0.1",
                repo1.maven.versionsMavenPluginVersion.getValue());

    }

}
