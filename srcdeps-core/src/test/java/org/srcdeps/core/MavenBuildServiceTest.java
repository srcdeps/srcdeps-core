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

import org.junit.Test;
import org.srcdeps.core.BuildRequest.BuildRequestBuilder;

/**
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */
public class MavenBuildServiceTest extends AbstractBuildServiceTest {

    protected static final BuilderTransformer MAVEN_SOURCE_TREE_VERSIONS_SET = new BuilderTransformer() {

        @Override
        public BuildRequestBuilder transform(BuildRequestBuilder builder) {
            return builder.useVersionsMavenPlugin(false);
        }
    };
    protected static final BuilderTransformer VERSIONS_SET_MAVEN_PLUGIN = new BuilderTransformer() {

        @Override
        public BuildRequestBuilder transform(BuildRequestBuilder builder) {
            return builder.useVersionsMavenPlugin(true);
        }
    };

    private void assertMvnBuild(String srcVersion, BuilderTransformer transformer) throws IOException, BuildException {
        int cnt = transformer == VERSIONS_SET_MAVEN_PLUGIN ? 2 : 1;
        assertBuild("git:https://github.com/srcdeps/srcdeps-test-artifact.git", srcVersion, transformer, cnt,
                "org.l2x6.maven.srcdeps.itest:srcdeps-test-artifact:${version}:[pom,jar]");
    }

    @Test
    public void testMvnGitBranch() throws BuildException, IOException {
        assertMvnBuild("0.0.1-SRC-branch-morning-branch", MAVEN_SOURCE_TREE_VERSIONS_SET);
    }

    @Test
    public void testMvnGitRevision() throws BuildException, IOException {
        assertMvnBuild("0.0.1-SRC-revision-66ea95d890531f4eaaa5aa04a9b1c69b409dcd0b", MAVEN_SOURCE_TREE_VERSIONS_SET);
    }

    @Test
    public void testMvnGitRevisionNonMaster() throws BuildException, IOException {
        assertMvnBuild("0.0.1-SRC-revision-dbad2cdc30b5bb3ff62fc89f57987689a5f3c220", MAVEN_SOURCE_TREE_VERSIONS_SET);
    }

    @Test
    public void testMvnGitRevisionSetVersionsPlugin() throws BuildException, IOException {
        assertMvnBuild("0.0.1-SRC-revision-66ea95d890531f4eaaa5aa04a9b1c69b409dcd0b", VERSIONS_SET_MAVEN_PLUGIN);
    }

    @Test
    public void testMvnGitTag() throws BuildException, IOException {
        assertMvnBuild("0.0.1-SRC-tag-0.0.1", MAVEN_SOURCE_TREE_VERSIONS_SET);
    }

}
