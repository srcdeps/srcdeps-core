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
package org.srcdeps.core.impl.scm;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ResetCommand.ResetType;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.NoHeadException;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.Assert;
import org.junit.Test;
import org.srcdeps.core.BuildRequest;
import org.srcdeps.core.ScmException;
import org.srcdeps.core.SrcVersion;
import org.srcdeps.core.config.Maven;
import org.srcdeps.core.config.scalar.CharStreamSource;
import org.srcdeps.core.util.SrcdepsCoreUtils;

/**
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */
public class JGitScmTest {

    private static final Path targetDir = Paths.get(System.getProperty("project.build.directory", "target"))
            .toAbsolutePath();

    private void assertCommit(Path dir, String expectedSha1) throws IOException, NoHeadException, GitAPIException {
        try (Git git = Git.open(dir.toFile())) {
            Iterable<RevCommit> history = git.log().call();
            String foundSha1 = history.iterator().next().getName();
            Assert.assertEquals(String.format("Git repository in [%s] not at the expected revision", dir), expectedSha1,
                    foundSha1);
        }
    }

    @Test
    public void testCheckout() throws IOException, ScmException, NoHeadException, GitAPIException {

        /* Create a local clone */
        final Path localGitRepos = targetDir.resolve("local-git-repos");
        final Path srcdepsTestArtifactDirectory = localGitRepos.resolve("srcdeps-test-artifact");
        SrcdepsCoreUtils.deleteDirectory(srcdepsTestArtifactDirectory);

        final String remoteGitUri = "https://github.com/srcdeps/srcdeps-test-artifact.git";
        final String mornigBranch = "morning-branch";
        try (Git git = Git.cloneRepository().setURI(remoteGitUri).setDirectory(srcdepsTestArtifactDirectory.toFile())
                .setCloneAllBranches(true).call()) {
            git.checkout().setName(mornigBranch).setCreateBranch(true).setStartPoint("origin/" + mornigBranch).call();
        }
        final String localGitUri = srcdepsTestArtifactDirectory.resolve(".git").toUri().toString();

        Path dir = targetDir.resolve("test-repo");
        SrcdepsCoreUtils.ensureDirectoryExistsAndEmpty(dir);

        /* first clone */
        BuildRequest cloningRequest = BuildRequest.builder() //
                .srcVersion(SrcVersion.parse("0.0.1-SRC-tag-0.0.1")) //
                .dependentProjectRootDirectory(dir) //
                .projectRootDirectory(dir) //
                .scmUrl("git:" + localGitUri) //
                .versionsMavenPluginVersion(Maven.getDefaultVersionsMavenPluginVersion()) //
                .gradleModelTransformer(CharStreamSource.defaultModelTransformer()) //
                .build();
        JGitScm jGitScm = new JGitScm();

        String commitId = jGitScm.checkout(cloningRequest);
        Assert.assertEquals("19ef91ed30fd8b1a459803ee0c279dcf8e236184", commitId);

        /* ensure that the tag is there through checking that it has a known commit hash */
        assertCommit(dir, "19ef91ed30fd8b1a459803ee0c279dcf8e236184");

        /* try if the fetch works after we have cloned already */
        BuildRequest fetchingRequest = BuildRequest.builder() //
                .srcVersion(SrcVersion.parse("0.0.1-SRC-revision-0a5ab902099b24c2b13ed1dad8c5f537458bcc89")) //
                .dependentProjectRootDirectory(dir) //
                .projectRootDirectory(dir) //
                .scmUrl("git:" + localGitUri) //
                .versionsMavenPluginVersion(Maven.getDefaultVersionsMavenPluginVersion()) //
                .gradleModelTransformer(CharStreamSource.defaultModelTransformer()) //
                .build();

        commitId = jGitScm.checkout(fetchingRequest);
        Assert.assertEquals("0a5ab902099b24c2b13ed1dad8c5f537458bcc89", commitId);

        /* ensure that the WC's HEAD has the known commit hash */
        assertCommit(dir, "0a5ab902099b24c2b13ed1dad8c5f537458bcc89");

        BuildRequest fetchBranchRequest = BuildRequest.builder() //
                .srcVersion(SrcVersion.parse("0.0.1-SRC-branch-morning-branch")) //
                .dependentProjectRootDirectory(dir) //
                .projectRootDirectory(dir) //
                .scmUrl("git:" + localGitUri) //
                .versionsMavenPluginVersion(Maven.getDefaultVersionsMavenPluginVersion()) //
                .gradleModelTransformer(CharStreamSource.defaultModelTransformer()) //
                .build();

        commitId = jGitScm.checkout(fetchBranchRequest);
        Assert.assertEquals("a84403b6fb44c5a588a9fe39d939c977e1e5c6a4", commitId);

        /* ensure that the WC's HEAD has the known commit hash */
        assertCommit(dir, "a84403b6fb44c5a588a9fe39d939c977e1e5c6a4");

        /* Add a new commit to morning-branch */
        final Path testTxtPath = srcdepsTestArtifactDirectory.resolve("test.txt");
        Files.write(testTxtPath, "Test".getBytes(StandardCharsets.UTF_8));
        /* Commit */
        String expectedCommit = null;
        try (Git git = Git.init().setDirectory(srcdepsTestArtifactDirectory.toFile()).call()) {
            git.add().addFilepattern("test.txt").call();
            expectedCommit = git.commit().setMessage("Added test.txt").call().getId().getName();
        }

        commitId = jGitScm.checkout(fetchBranchRequest);
        Assert.assertEquals(expectedCommit, commitId);

        /* Reset back the morning-branch */
        try (Git git = Git.init().setDirectory(srcdepsTestArtifactDirectory.toFile()).call()) {
            git.reset().setMode(ResetType.HARD).setRef("a84403b6fb44c5a588a9fe39d939c977e1e5c6a4").call();
        }

        commitId = jGitScm.checkout(fetchBranchRequest);
        Assert.assertEquals("a84403b6fb44c5a588a9fe39d939c977e1e5c6a4", commitId);
        assertCommit(dir, "a84403b6fb44c5a588a9fe39d939c977e1e5c6a4");

    }

    @Test
    public void testCheckoutMultiUrl() throws IOException, ScmException, NoHeadException, GitAPIException {

        /* Create a local clone */
        final Path localGitRepos = targetDir.resolve("local-git-repos");
        final Path srcdepsTestArtifactDirectory0 = localGitRepos.resolve("srcdeps-test-artifact-testCheckoutMultiUrl-0");
        SrcdepsCoreUtils.deleteDirectory(srcdepsTestArtifactDirectory0);
        final Path srcdepsTestArtifactDirectory1 = localGitRepos.resolve("srcdeps-test-artifact-testCheckoutMultiUrl-1");
        SrcdepsCoreUtils.deleteDirectory(srcdepsTestArtifactDirectory1);

        final String remoteGitUri = "https://github.com/srcdeps/srcdeps-test-artifact.git";
        final String mornigBranch = "morning-branch";
        try (Git git = Git.cloneRepository().setURI(remoteGitUri).setDirectory(srcdepsTestArtifactDirectory0.toFile())
                .setCloneAllBranches(true).call()) {
            git.checkout().setName(mornigBranch).setCreateBranch(true).setStartPoint("origin/" + mornigBranch).call();
        }
        SrcdepsCoreUtils.copyDirectory(srcdepsTestArtifactDirectory0, srcdepsTestArtifactDirectory1);

        final String localGitUri0 = srcdepsTestArtifactDirectory0.resolve(".git").toUri().toString();
        final String localGitUri1 = srcdepsTestArtifactDirectory1.resolve(".git").toUri().toString();

        /* Add a commit to repo 1 */
        final Path testTxtPath = srcdepsTestArtifactDirectory1.resolve("test.txt");
        Files.write(testTxtPath, "Test0".getBytes(StandardCharsets.UTF_8));

        final Path dir = targetDir.resolve("test-repo-testCheckoutMultiUrl");
        SrcdepsCoreUtils.ensureDirectoryExistsAndEmpty(dir);
        {
            final String addedCommitId;
            try (Git git = Git.init().setDirectory(srcdepsTestArtifactDirectory1.toFile()).call()) {
                git.add().addFilepattern(testTxtPath.getFileName().toString()).call();
                addedCommitId = git.commit().setMessage("Added test.txt").call().getId().getName();
            }

            /* first clone */
            final BuildRequest cloningRequest = BuildRequest.builder() //
                    .srcVersion(SrcVersion.parse("0.0.1-SRC-revision-"+ addedCommitId)) //
                    .dependentProjectRootDirectory(dir) //
                    .projectRootDirectory(dir) //
                    .scmUrl("git:" + localGitUri0) //
                    .scmUrl("git:" + localGitUri1) //
                    .versionsMavenPluginVersion(Maven.getDefaultVersionsMavenPluginVersion()) //
                    .gradleModelTransformer(CharStreamSource.defaultModelTransformer()) //
                    .build();
            final JGitScm jGitScm = new JGitScm();

            final String commitId = jGitScm.checkout(cloningRequest);
            Assert.assertEquals(addedCommitId, commitId);
        }

        /* Add one more commit to repo 1 */
        Files.write(testTxtPath, "Test1".getBytes(StandardCharsets.UTF_8));
        {
            final String addedCommitId;
            try (Git git = Git.init().setDirectory(srcdepsTestArtifactDirectory1.toFile()).call()) {
                git.add().addFilepattern(testTxtPath.getFileName().toString()).call();
                addedCommitId = git.commit().setMessage("Changed test.txt").call().getId().getName();
            }

            /* fetch and reset */
            final BuildRequest fetchResetRequest = BuildRequest.builder() //
                    .srcVersion(SrcVersion.parse("0.0.1-SRC-revision-"+ addedCommitId)) //
                    .dependentProjectRootDirectory(dir) //
                    .projectRootDirectory(dir) //
                    .scmUrl("git:" + localGitUri0) //
                    .scmUrl("git:" + localGitUri1) //
                    .versionsMavenPluginVersion(Maven.getDefaultVersionsMavenPluginVersion()) //
                    .gradleModelTransformer(CharStreamSource.defaultModelTransformer()) //
                    .build();
            final JGitScm jGitScm = new JGitScm();

            final String commitId = jGitScm.checkout(fetchResetRequest);
            Assert.assertEquals(addedCommitId, commitId);
        }

    }

    @Test
    public void toRemoteAlias() {
        Assert.assertEquals("origin-OracyX45LTLgEE14zEKVWpi-CTg=", JGitScm.toRemoteAlias("https://github.com/srcdeps/srcdeps-test-artifact.git"));
    }
}
