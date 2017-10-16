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

import java.util.Collections;
import java.util.Stack;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Test;
import org.srcdeps.core.BuildRequest.Verbosity;
import org.srcdeps.core.config.BuilderIo.BuilderIoScheme;
import org.srcdeps.core.config.ScmRepository.Builder;
import org.srcdeps.core.config.scalar.Duration;
import org.srcdeps.core.config.tree.Node;
import org.srcdeps.core.config.tree.walk.DefaultsAndInheritanceVisitor;

public class DefaultsAndInheritanceTest {
    @Test
    public void defaults() {

        Configuration.Builder configBuilder = Configuration.builder() //
                .repository(//
                        ScmRepository.builder() //
                                .id("repo1") //
                                .include("org.example") //
                                .url("file:///whereever") //
        );

        Assert.assertNull(configBuilder.configModelVersion.getValue());
        Assert.assertEquals(Collections.emptySet(), configBuilder.forwardProperties.asSetOfValues());
        Assert.assertNull(configBuilder.skip.getValue());
        Assert.assertNull(configBuilder.sourcesDirectory.getValue());
        Assert.assertNull(configBuilder.verbosity.getValue());
        Assert.assertNull(configBuilder.buildTimeout.getValue());
        Assert.assertNull(configBuilder.builderIo.stdin.getValue());
        Assert.assertNull(configBuilder.builderIo.stdout.getValue());
        Assert.assertNull(configBuilder.builderIo.stderr.getValue());

        Assert.assertNull(configBuilder.maven.versionsMavenPluginVersion.getValue());
        MavenAssertions.FailWithBuilder failWithBuilder = configBuilder.maven.failWith;
        Stack<Node> stack = new Stack<>();
        stack.push(configBuilder);
        stack.push(configBuilder.maven);
        stack.push(configBuilder.maven.failWith);
        Assert.assertTrue(failWithBuilder.isInDefaultState(stack));
        Assert.assertNull(failWithBuilder.addDefaults.getValue());
        Assert.assertEquals(Collections.emptySet(), failWithBuilder.goals.asSetOfValues());
        Assert.assertEquals(Collections.emptySet(), failWithBuilder.profiles.asSetOfValues());
        Assert.assertEquals(Collections.emptySet(), failWithBuilder.properties.asSetOfValues());

        ScmRepository.Builder repo1Builder = configBuilder.repositories.getChildren().get("repo1");
        Assert.assertEquals("repo1", repo1Builder.getName());
        Assert.assertNull(repo1Builder.addDefaultBuildArguments.getValue());
        Assert.assertEquals(Collections.emptyList(), repo1Builder.buildArguments.asListOfValues());
        Assert.assertNull(repo1Builder.maven.versionsMavenPluginVersion.getValue());
        Assert.assertEquals(Collections.singletonList("org.example"), repo1Builder.includes.asListOfValues());
        Assert.assertNull(repo1Builder.skipTests.getValue());
        Assert.assertEquals(Collections.singletonList("file:///whereever"), repo1Builder.urls.asListOfValues());
        Assert.assertNull(repo1Builder.buildTimeout.getValue());
        Assert.assertNull(repo1Builder.builderIo.stdin.getValue());
        Assert.assertNull(repo1Builder.builderIo.stdout.getValue());
        Assert.assertNull(repo1Builder.builderIo.stderr.getValue());
        Assert.assertNull(repo1Builder.verbosity.getValue());

        configBuilder.accept(new DefaultsAndInheritanceVisitor());

        Assert.assertTrue(failWithBuilder.isInDefaultState(stack));

        Configuration config = configBuilder.build();
        Assert.assertEquals(Configuration.getLatestConfigModelVersion(), config.getConfigModelVersion());
        Assert.assertEquals(Configuration.getDefaultForwardProperties(), config.getForwardProperties());
        Assert.assertEquals(false, config.isSkip());
        Assert.assertNull(config.getSourcesDirectory());
        Assert.assertEquals(Verbosity.warn, configBuilder.verbosity.getValue());
        Assert.assertEquals(Duration.maxValue(), configBuilder.buildTimeout.getValue());

        Assert.assertEquals(BuilderIoScheme.inherit.name(), configBuilder.builderIo.stdin.getValue());
        Assert.assertEquals(BuilderIoScheme.inherit.name(), configBuilder.builderIo.stdout.getValue());
        Assert.assertEquals(BuilderIoScheme.inherit.name(), configBuilder.builderIo.stderr.getValue());

        Assert.assertEquals(Maven.getDefaultVersionsMavenPluginVersion(),
                configBuilder.maven.versionsMavenPluginVersion.getValue());
        MavenAssertions failWith = config.getMaven().getFailWith();
        Assert.assertEquals(true, failWith.isAddDefaults());
        Assert.assertEquals(MavenAssertions.getDefaultFailGoals(), failWith.getGoals());
        Assert.assertEquals(Collections.emptySet(), failWith.getProfiles());
        Assert.assertEquals(Collections.emptySet(), failWith.getProperties());

        ScmRepository repo1 = config.getRepositories().iterator().next();
        Assert.assertEquals("repo1", repo1.getId());
        Assert.assertEquals(true, repo1.isAddDefaultBuildArguments());
        Assert.assertEquals(Collections.emptyList(), repo1.getBuildArguments());
        Assert.assertEquals(Collections.singletonList("org.example"), repo1.getIncludes());
        Assert.assertEquals(true, repo1.isSkipTests());
        Assert.assertEquals(Collections.singletonList("file:///whereever"), repo1.getUrls());
        Assert.assertEquals(Maven.getDefaultVersionsMavenPluginVersion(),
                repo1.getMaven().getVersionsMavenPluginVersion());
        Assert.assertEquals(Duration.maxValue(), repo1.getBuildTimeout());
        Assert.assertEquals(BuilderIoScheme.inherit.name(), repo1.getBuilderIo().getStdin());
        Assert.assertEquals(BuilderIoScheme.inherit.name(), repo1.getBuilderIo().getStdout());
        Assert.assertEquals(BuilderIoScheme.inherit.name(), repo1.getBuilderIo().getStderr());
        Assert.assertEquals(Verbosity.warn, repo1.getVerbosity());

    }

    @Test
    public void failWithDefaults() {
        Configuration.Builder config = Configuration.builder();
        Assert.assertEquals(Collections.emptySet(), config.maven.failWith.goals.asSetOfValues());

        config.accept(new DefaultsAndInheritanceVisitor());
        Assert.assertEquals(MavenAssertions.getDefaultFailGoals(), config.maven.failWith.goals.asSetOfValues());
    }

    @Test
    public void inheritance() {

        Configuration.Builder config = Configuration.builder() //
                .buildTimeout(Duration.of("32m")) //
                .verbosity(Verbosity.trace)
                .builderIo( //
                        BuilderIo.builder() //
                                .stdin("read:/path/to/input-file.txt") //
                                .stdout("write:/path/to/log.txt") //
                                .stderr("write:/path/to/err.txt") //
                ) //
                .maven( //
                        Maven.builder() //
                                .versionsMavenPluginVersion("0.1")) //
                .repository(//
                        ScmRepository.builder() //
                                .id("repo1") //
                                .include("org.example") //
                                .url("file:///whereever") //
        );

        Assert.assertEquals(new Duration(32, TimeUnit.MINUTES), config.buildTimeout.getValue());
        Assert.assertEquals(Verbosity.trace, config.verbosity.getValue());
        Assert.assertEquals("0.1", config.maven.versionsMavenPluginVersion.getValue());
        Assert.assertEquals("read:/path/to/input-file.txt", config.builderIo.stdin.getValue());
        Assert.assertEquals("write:/path/to/log.txt", config.builderIo.stdout.getValue());
        Assert.assertEquals("write:/path/to/err.txt", config.builderIo.stderr.getValue());

        Builder repo1 = config.repositories.getChildren().get("repo1");
        Assert.assertNull(repo1.buildTimeout.getValue());
        Assert.assertNull(repo1.maven.versionsMavenPluginVersion.getValue());
        Assert.assertNull(repo1.builderIo.stdin.getValue());
        Assert.assertNull(repo1.builderIo.stdout.getValue());
        Assert.assertNull(repo1.builderIo.stderr.getValue());
        Assert.assertNull(repo1.verbosity.getValue());

        config.accept(new DefaultsAndInheritanceVisitor());

        Assert.assertEquals(new Duration(32, TimeUnit.MINUTES), config.buildTimeout.getValue());
        Assert.assertEquals(Verbosity.trace, config.verbosity.getValue());
        Assert.assertEquals("0.1", config.maven.versionsMavenPluginVersion.getValue());
        Assert.assertEquals("read:/path/to/input-file.txt", config.builderIo.stdin.getValue());
        Assert.assertEquals("write:/path/to/log.txt", config.builderIo.stdout.getValue());
        Assert.assertEquals("write:/path/to/err.txt", config.builderIo.stderr.getValue());

        Assert.assertEquals(new Duration(32, TimeUnit.MINUTES), repo1.buildTimeout.getValue());
        Assert.assertEquals(Verbosity.trace, repo1.verbosity.getValue());
        Assert.assertEquals("0.1", repo1.maven.versionsMavenPluginVersion.getValue());
        Assert.assertEquals("read:/path/to/input-file.txt", repo1.builderIo.stdin.getValue());
        Assert.assertEquals("write:/path/to/log.txt", repo1.builderIo.stdout.getValue());
        Assert.assertEquals("write:/path/to/err.txt", repo1.builderIo.stderr.getValue());

    }

}
