/**
 * Copyright 2015-2019 Maven Source Dependencies
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
import java.util.regex.Pattern;

import org.junit.Assert;
import org.junit.Test;
import org.srcdeps.core.BuildRequest.Verbosity;
import org.srcdeps.core.SrcVersion;
import org.srcdeps.core.config.BuilderIo.BuilderIoScheme;
import org.srcdeps.core.config.ScmRepository.Builder;
import org.srcdeps.core.config.scalar.Duration;
import org.srcdeps.core.config.tree.Node;
import org.srcdeps.core.config.tree.walk.DefaultsAndInheritanceVisitor;
import org.srcdeps.core.util.Equals.EqualsImplementations;

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
        Assert.assertNull(configBuilder.encoding.getValue());
        Assert.assertNull(configBuilder.buildTimeout.getValue());
        Assert.assertNull(configBuilder.buildVersionPattern.getValue());
        Assert.assertNull(configBuilder.buildRef.getValue());
        Assert.assertNull(configBuilder.builderIo.stdin.getValue());
        Assert.assertNull(configBuilder.builderIo.stdout.getValue());
        Assert.assertNull(configBuilder.builderIo.stderr.getValue());

        Assert.assertNull(configBuilder.maven.versionsMavenPluginVersion.getValue());
        Assert.assertNull(configBuilder.maven.useVersionsMavenPlugin.getValue());
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
        Assert.assertNull(repo1Builder.maven.useVersionsMavenPlugin.getValue());
        Assert.assertNull(repo1Builder.maven.excludeNonRequired.getValue());
        Assert.assertNull(repo1Builder.maven.includeRequired.getValue());
        Assert.assertEquals(Collections.emptyList(), repo1Builder.maven.includes.asListOfValues());

        Assert.assertNull(repo1Builder.gradle.modelTransformer.getValue());
        Assert.assertEquals(Collections.singletonList("org.example"), repo1Builder.includes.asListOfValues());
        Assert.assertNull(repo1Builder.skipTests.getValue());
        Assert.assertEquals(Collections.singletonList("file:///whereever"), repo1Builder.urls.asListOfValues());
        Assert.assertNull(repo1Builder.buildTimeout.getValue());
        Assert.assertNull(repo1Builder.builderIo.stdin.getValue());
        Assert.assertNull(repo1Builder.builderIo.stdout.getValue());
        Assert.assertNull(repo1Builder.builderIo.stderr.getValue());
        Assert.assertNull(repo1Builder.verbosity.getValue());
        Assert.assertNull(repo1Builder.encoding.getValue());
        Assert.assertNull(repo1Builder.buildVersionPattern.getValue());
        Assert.assertNull(repo1Builder.buildRef.getValue());

        configBuilder.accept(new DefaultsAndInheritanceVisitor());

        Assert.assertTrue(failWithBuilder.isInDefaultState(stack));

        Configuration config = configBuilder.build();
        Assert.assertEquals(Configuration.getLatestConfigModelVersion(), config.getConfigModelVersion());
        Assert.assertEquals(Configuration.getDefaultForwardProperties(), config.getForwardProperties());
        Assert.assertEquals(false, config.isSkip());
        Assert.assertNull(config.getSourcesDirectory());
        Assert.assertEquals(Verbosity.warn, configBuilder.verbosity.getValue());
        Assert.assertEquals(Configuration.getDefaultEncoding(), configBuilder.encoding.getValue());
        Assert.assertEquals(Duration.maxValue(), configBuilder.buildTimeout.getValue());
        Assert.assertNull(configBuilder.buildVersionPattern.getValue());
        Assert.assertEquals(SrcVersion.getBranchMaster(), configBuilder.buildRef.getValue());

        Assert.assertEquals(BuilderIoScheme.inherit.name(), configBuilder.builderIo.stdin.getValue());
        Assert.assertEquals(BuilderIoScheme.inherit.name(), configBuilder.builderIo.stdout.getValue());
        Assert.assertEquals(BuilderIoScheme.inherit.name(), configBuilder.builderIo.stderr.getValue());

        Assert.assertEquals(Maven.getDefaultVersionsMavenPluginVersion(),
                configBuilder.maven.versionsMavenPluginVersion.getValue());
        Assert.assertEquals(Maven.getDefaultUseVersionsMavenPlugin(),
                configBuilder.maven.useVersionsMavenPlugin.getValue());
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
        Assert.assertEquals(Maven.getDefaultUseVersionsMavenPlugin(),
                repo1.getMaven().isUseVersionsMavenPlugin());
        Assert.assertEquals(false, repo1.getMaven().isExcludeNonRequired());
        Assert.assertEquals(false, repo1.getMaven().isIncludeRequired());
        Assert.assertEquals(Collections.emptyList(), repo1.getMaven().getIncludes());
        Assert.assertEquals(org.srcdeps.core.config.scalar.CharStreamSource.defaultModelTransformer(),
                repo1.getGradle().getModelTransformer());
        Assert.assertEquals(Verbosity.warn, repo1.getVerbosity());
        Assert.assertEquals(Configuration.getDefaultEncoding(), repo1.getEncoding());
        Assert.assertEquals(Duration.maxValue(), repo1.getBuildTimeout());

        Assert.assertNull(repo1.getBuildVersionPattern());
        Assert.assertEquals(SrcVersion.getBranchMaster(), repo1.getBuildRef());

        Assert.assertEquals(BuilderIoScheme.inherit.name(), repo1.getBuilderIo().getStdin());
        Assert.assertEquals(BuilderIoScheme.inherit.name(), repo1.getBuilderIo().getStdout());
        Assert.assertEquals(BuilderIoScheme.inherit.name(), repo1.getBuilderIo().getStderr());

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

        Configuration.Builder configBuilder = Configuration.builder() //
                .buildTimeout(Duration.of("32m")) //
                .verbosity(Verbosity.trace) //
                .buildVersionPattern(Pattern.compile(".*-SNAPSHOT")).buildRef(SrcVersion.parseRef("branch-3.x"))
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

        Assert.assertEquals(new Duration(32, TimeUnit.MINUTES), configBuilder.buildTimeout.getValue());
        Assert.assertEquals(Verbosity.trace, configBuilder.verbosity.getValue());
        Assert.assertTrue(EqualsImplementations.equalsPattern().test(Pattern.compile(".*-SNAPSHOT"),
                configBuilder.buildVersionPattern.getValue()));
        Assert.assertEquals(SrcVersion.parseRef("branch-3.x"), configBuilder.buildRef.getValue());
        Assert.assertEquals("0.1", configBuilder.maven.versionsMavenPluginVersion.getValue());
        Assert.assertEquals("read:/path/to/input-file.txt", configBuilder.builderIo.stdin.getValue());
        Assert.assertEquals("write:/path/to/log.txt", configBuilder.builderIo.stdout.getValue());
        Assert.assertEquals("write:/path/to/err.txt", configBuilder.builderIo.stderr.getValue());

        Builder repo1Builder = configBuilder.repositories.getChildren().get("repo1");
        Assert.assertNull(repo1Builder.buildTimeout.getValue());
        Assert.assertNull(repo1Builder.maven.versionsMavenPluginVersion.getValue());
        Assert.assertNull(repo1Builder.gradle.modelTransformer.getValue());
        Assert.assertNull(repo1Builder.builderIo.stdin.getValue());
        Assert.assertNull(repo1Builder.builderIo.stdout.getValue());
        Assert.assertNull(repo1Builder.builderIo.stderr.getValue());
        Assert.assertNull(repo1Builder.verbosity.getValue());
        Assert.assertNull(repo1Builder.buildVersionPattern.getValue());
        Assert.assertNull(repo1Builder.buildRef.getValue());

        configBuilder.accept(new DefaultsAndInheritanceVisitor());

        Configuration config = configBuilder.build();
        Assert.assertEquals(new Duration(32, TimeUnit.MINUTES), configBuilder.buildTimeout.getValue());
        Assert.assertEquals(Verbosity.trace, configBuilder.verbosity.getValue());
        Assert.assertTrue(EqualsImplementations.equalsPattern().test(Pattern.compile(".*-SNAPSHOT"),
                configBuilder.buildVersionPattern.getValue()));
        Assert.assertEquals(SrcVersion.parseRef("branch-3.x"), configBuilder.buildRef.getValue());
        Assert.assertEquals("0.1", configBuilder.maven.versionsMavenPluginVersion.getValue());
        Assert.assertEquals("read:/path/to/input-file.txt", configBuilder.builderIo.stdin.getValue());
        Assert.assertEquals("write:/path/to/log.txt", configBuilder.builderIo.stdout.getValue());
        Assert.assertEquals("write:/path/to/err.txt", configBuilder.builderIo.stderr.getValue());

        ScmRepository repo1 = config.getRepositories().iterator().next();
        Assert.assertEquals("repo1", repo1.getId());
        Assert.assertEquals(new Duration(32, TimeUnit.MINUTES), repo1.getBuildTimeout());
        Assert.assertEquals(Verbosity.trace, repo1.getVerbosity());
        Assert.assertTrue(EqualsImplementations.equalsPattern().test(Pattern.compile(".*-SNAPSHOT"),
                repo1.getBuildVersionPattern()));
        Assert.assertEquals(SrcVersion.parseRef("branch-3.x"), repo1.getBuildRef());
        Assert.assertEquals("0.1", repo1.getMaven().getVersionsMavenPluginVersion());
        Assert.assertEquals("read:/path/to/input-file.txt", repo1.getBuilderIo().getStdin());
        Assert.assertEquals("write:/path/to/log.txt", repo1.getBuilderIo().getStdout());
        Assert.assertEquals("write:/path/to/err.txt", repo1.getBuilderIo().getStderr());

    }

}
