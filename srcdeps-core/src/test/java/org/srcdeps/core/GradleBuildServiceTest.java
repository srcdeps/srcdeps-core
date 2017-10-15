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

import org.junit.Test;

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

    @Test
    public void testGradleGitRevisionMultiModule() throws BuildException, IOException {
        assertBuild("git:https://github.com/srcdeps/srcdeps-test-artifact-gradle.git",
                "0.0.1-SRC-revision-e63539236a94e8f6c2d720f8bda0323d1ce4db0f", //
                "org.srcdeps.test.gradle:srcdeps-test-artifact-gradle-api:${version}:[pom,jar]", //
                "org.srcdeps.test.gradle:srcdeps-test-artifact-gradle-impl:${version}:[pom,jar]" //
        );
    }

    @Test
    public void testGradleGitRevisionMockito() throws BuildException, IOException {
        assertBuild("git:https://github.com/srcdeps/mockito.git",
                "0.0.1-SRC-revision-6d6361fc72c16c947ef5f2f587fd9269a9d47f23", //
                "org.mockito:mockito-android:${version}:[pom,jar]", //
                "org.mockito:mockito-core:${version}:[pom,jar]", //
                "org.mockito:mockito-inline:${version}:[pom,jar]" //
        );
    }

}
