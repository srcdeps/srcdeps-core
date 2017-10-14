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
package org.srcdeps.core;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.eclipse.sisu.launch.InjectedTest;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.srcdeps.core.config.Maven;
import org.srcdeps.core.util.SrcdepsCoreUtils;

/**
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */
public class BuildServiceTest extends InjectedTest {

    private static final Logger log = LoggerFactory.getLogger(BuildServiceTest.class);
    private static final String mrmSettingsXmlPath = System.getProperty("mrm.settings.xml");
    private static final Path mvnLocalRepo;
    private static final Path projectsDirectory;
    private static final Path targetDirectory = Paths.get(System.getProperty("project.build.directory", "target"))
            .toAbsolutePath();

    static {
        projectsDirectory = targetDirectory.resolve("test-projects");
        mvnLocalRepo = targetDirectory.resolve("mvn-local-repo");
    }

    public static void assertExists(Path path) {
        Assert.assertTrue(String.format("File or directory does not exist [%s]", path.toString()), Files.exists(path));
    }

    @BeforeClass
    public static void beforeClass() throws IOException {

        SrcdepsCoreUtils.ensureDirectoryExistsAndEmpty(mvnLocalRepo);

        System.setProperty(Maven.getSrcdepsMavenSettingsProperty(), mrmSettingsXmlPath);

    }

    @Inject
    private BuildService buildService;

    protected String currentTestName;

    @Rule
    public TestRule watcher = new TestWatcher() {

        @Override
        protected void starting(Description description) {
            BuildServiceTest.this.currentTestName = description.getMethodName();
        }

    };

    public void assertBuild(String gitRepoUri, String srcVersion, String... artifactPathTemplates)
            throws IOException, BuildException {
        Assert.assertNotNull("buildService not injected", buildService);
        log.info("Using {} as {}", buildService.getClass().getName(), BuildService.class.getName());

        final Path projectRoot = projectsDirectory.resolve(currentTestName);
        final Path projectBuildDirectory = projectRoot.resolve("build");

        final List<Path> paths = new ArrayList<>(artifactPathTemplates.length * 2);
        for (String artifactPathTemplate : artifactPathTemplates) {
            if (artifactPathTemplate.endsWith("]")) {
                int leftSqBracket = artifactPathTemplate.lastIndexOf('[');
                final String typesString = artifactPathTemplate.substring(leftSqBracket + 1,
                        artifactPathTemplate.length() - 1);
                final String[] types = typesString.split(",");
                final String template = artifactPathTemplate.substring(0, leftSqBracket);
                for (String type : types) {
                    Path path = mvnLocalRepo.resolve(template.replace("${version}", srcVersion) + type);
                    paths.add(path);
                }
            } else {
                Path path = mvnLocalRepo.resolve(artifactPathTemplate.replace("${version}", srcVersion));
                paths.add(path);
            }
        }

        SrcdepsCoreUtils.deleteDirectory(projectRoot);

        for (Path path : paths) {
            SrcdepsCoreUtils.deleteDirectory(path.getParent());
        }

        BuildRequest request = BuildRequest.builder() //
                .scmUrl(gitRepoUri) //
                .srcVersion(SrcVersion.parse(srcVersion)).projectRootDirectory(projectBuildDirectory) //
                .buildArgument("-Dmaven.repo.local=" + mvnLocalRepo.toString()) //
                .versionsMavenPluginVersion(Maven.getDefaultVersionsMavenPluginVersion()) //
                .build();

        buildService.build(request);

        for (Path path : paths) {
            assertExists(path);
        }

    }

    public void assertMvnBuild(String srcVersion) throws IOException, BuildException {
        assertBuild("git:https://github.com/srcdeps/srcdeps-test-artifact.git", srcVersion,
                "org/l2x6/maven/srcdeps/itest/srcdeps-test-artifact/${version}/srcdeps-test-artifact-${version}.[pom,jar]");
    }

    @Test
    public void testGradleGitRevision() throws BuildException, IOException {
        assertBuild("git:https://github.com/srcdeps/srcdeps-test-artifact-gradle.git",
                "0.0.1-SRC-revision-389765a9de4f8526b6b2776c39bb0de67668de62",
                "org/srcdeps/test/gradle/srcdeps-test-artifact-gradle/${version}/srcdeps-test-artifact-gradle-${version}.[pom,jar]");
    }

    @Test
    public void testGradleGitRevisionMultiModule() throws BuildException, IOException {
        assertBuild("git:https://github.com/srcdeps/srcdeps-test-artifact-gradle.git",
                "0.0.1-SRC-revision-e63539236a94e8f6c2d720f8bda0323d1ce4db0f",
                "org/srcdeps/test/gradle/srcdeps-test-artifact-gradle-api/${version}/srcdeps-test-artifact-gradle-api-${version}.[pom,jar]",
                "org/srcdeps/test/gradle/srcdeps-test-artifact-gradle-impl/${version}/srcdeps-test-artifact-gradle-impl-${version}.[pom,jar]");
    }

    @Test
    public void testGradleGitRevisionMockito() throws BuildException, IOException {
        assertBuild("git:https://github.com/srcdeps/mockito.git",
                "0.0.1-SRC-revision-6d6361fc72c16c947ef5f2f587fd9269a9d47f23",
                "org/mockito/mockito-android/${version}/mockito-android-${version}.[pom,jar]",
                "org/mockito/mockito-core/${version}/mockito-core-${version}.[pom,jar]",
                "org/mockito/mockito-inline/${version}/mockito-inline-${version}.[pom,jar]"
                );
    }

    @Test
    public void testMvnGitBranch() throws BuildException, IOException {
        assertMvnBuild("0.0.1-SRC-branch-morning-branch");
    }

    @Test
    public void testMvnGitRevision() throws BuildException, IOException {
        assertMvnBuild("0.0.1-SRC-revision-66ea95d890531f4eaaa5aa04a9b1c69b409dcd0b");
    }

    @Test
    public void testMvnGitRevisionNonMaster() throws BuildException, IOException {
        assertMvnBuild("0.0.1-SRC-revision-dbad2cdc30b5bb3ff62fc89f57987689a5f3c220");
    }

    @Test
    public void testMvnGitTag() throws BuildException, IOException {
        assertMvnBuild("0.0.1-SRC-tag-0.0.1");
    }

}
