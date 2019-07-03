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
package org.srcdeps.core;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.srcdeps.core.BuildRequest.BuildRequestBuilder;
import org.srcdeps.core.config.scalar.CharStreamSource;
import org.srcdeps.core.util.SrcdepsCoreUtils;

/**
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */
public class GradleBuildServiceTest extends AbstractBuildServiceTest {

    @Test
    public void testGradleGitRevision() throws BuildException, IOException {
        assertBuild("git:https://github.com/srcdeps/srcdeps-test-artifact-gradle.git",
                "0.0.1-SRC-revision-389765a9de4f8526b6b2776c39bb0de67668de62", //
                "org.srcdeps.test.gradle:srcdeps-test-artifact-gradle:${version}:[pom,jar]" //
        );
    }

    /**
     * Uses a custom Gradle model transformer so that some mockito subprojects are not installed to the local maven
     * repository.
     *
     * @throws BuildException
     * @throws IOException
     */
    @Test
    public void testGradleGitRevisionMockito() throws BuildException, IOException {
        final String srcVersion = "0.0.1-SRC-revision-6030bb9be4795870a47d41c0bff8615a0e6a54f9";
        final List<Path> unwantedPaths = resolve(srcVersion, //
                "org.mockito:mockito-testng:${version}:[pom,jar]", //
                "org.mockito:mockito-android:${version}:[pom,jar]", //
                "org.mockito:mockito-inline:${version}:[pom,jar]" //
        );

        for (Path path : unwantedPaths) {
            SrcdepsCoreUtils.deleteDirectory(path.getParent());
        }

        BuilderTransformer bt = new BuilderTransformer() {
            @Override
            public BuildRequestBuilder transform(BuildRequestBuilder builder) {
                final GavSet gavSet = GavSet.builder() //
                        .include("org.mockito") //
                        .exclude("org.mockito:mockito-testng") //
                        .exclude("org.mockito:mockito-android") //
                        .exclude("org.mockito:mockito-inline") //
                        .build();
                final CharStreamSource modelTransformer = CharStreamSource.of( //
                        "literal:" //
                                + "logger.error(\"srcdeps: executing custom model transformer\")\n" //
                                + "def name2ArtifactIdMap = [\n" + "    mockito: 'mockito-core',\n"
                                + "    android: 'mockito-android',\n" + "    extTest: 'mockito-extTest',\n"
                                + "    inline: 'mockito-inline',\n" + "    kotlinTest: 'mockito-kotlinTest',\n"
                                + "    testng: 'mockito-testng',\n" + "]\n" //
                                + "gradle.projectsLoaded {\n" //
                                + "\n" //
                                + "    gradle.rootProject.properties['allprojects'].each {\n" //
                                + "        it.afterEvaluate { project ->\n" //
                                + "            def groupId = project.group\n" //
                                + "            def artifactId = name2ArtifactIdMap.get(project.name)\n" //
                                + "            def version = project.version\n" //
                                + "\n" //
                                + "            logger.error(\"srcdeps: mapped project name ${project.name} to artifactId ${artifactId}\")\n" //
                                + "            logger.error(\"srcdeps: processing ${groupId}:${artifactId}:${version}\")\n" //
                                + "            def plugins = project.plugins;\n" //
                                + "            if (srcdepsInner.gavSet.contains(groupId, artifactId, version)) {\n" //
                                + "                if (!plugins.hasPlugin('maven')) {\n" //
                                + "                    logger.error(\"srcdeps: adding maven plugin to ${groupId}:${artifactId}\")\n" //
                                + "                    plugins.apply('maven')\n" //
                                + "                }\n" //
                                + "                logger.error(\"srcdeps: changing version of ${groupId}:${artifactId} from \"+ project.version +\" to \"+ srcdepsInner.version)\n" //
                                + "                project.version = srcdepsInner.version\n" //
                                + "            }\n" //
                                + "        }\n" //
                                + "    }\n" //
                                + "}"//
                );
                return builder //
                        .gavSet(gavSet) //
                        .gradleModelTransformer(modelTransformer) //
                        .buildArgument("--stacktrace");
            }
        };

        assertBuild("git:https://github.com/srcdeps/mockito.git", srcVersion, //
                bt, //
                1, //
                "org.mockito:mockito-core:${version}:[pom,jar]"//
        );

        for (Path path : unwantedPaths) {
            Assert.assertFalse(String.format("File should not exist [%s]", path), Files.exists(path));
        }

    }

    @Test
    public void testGradleGitRevisionMultiModule() throws BuildException, IOException {
        assertBuild("git:https://github.com/srcdeps/srcdeps-test-artifact-gradle.git",
                "0.0.1-SRC-revision-e63539236a94e8f6c2d720f8bda0323d1ce4db0f", //
                "org.srcdeps.test.gradle:srcdeps-test-artifact-gradle-api:${version}:[pom,jar]", //
                "org.srcdeps.test.gradle:srcdeps-test-artifact-gradle-impl:${version}:[pom,jar]" //
        );
    }

}
