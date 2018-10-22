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
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.inject.Named;
import javax.inject.Singleton;

import org.eclipse.jgit.api.FetchCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ResetCommand.ResetType;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.transport.FetchResult;
import org.eclipse.jgit.transport.RefSpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.srcdeps.core.BuildRequest;
import org.srcdeps.core.Scm;
import org.srcdeps.core.ScmException;
import org.srcdeps.core.SrcVersion;
import org.srcdeps.core.SrcVersion.WellKnownType;
import org.srcdeps.core.util.SrcdepsCoreUtils;

/**
 * A JGit based implementation of a Git {@link Scm}.
 *
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */
@Named
@Singleton
public class JGitScm implements Scm {
    private static final Logger log = LoggerFactory.getLogger(JGitScm.class);
    private static final String REMOTE = "remote";
    private static final String SCM_GIT_PREFIX = "git:";

    private static final String SRCDEPS_WORKING_BRANCH = "srcdeps-working-branch";

    static void ensureRemoteAvailable(String useUrl, String remoteAlias, Git git) throws IOException {
        final StoredConfig config = git.getRepository().getConfig();
        boolean save = false;
        final String foundUrl = config.getString(REMOTE, remoteAlias, "url");
        if (!useUrl.equals(foundUrl)) {
            config.setString(REMOTE, remoteAlias, "url", useUrl);
            save = true;
        }
        final String foundFetch = config.getString(REMOTE, remoteAlias, "fetch");
        final String expectedFetch = "+refs/heads/*:refs/remotes/" + remoteAlias + "/*";
        if (!expectedFetch.equals(foundFetch)) {
            config.setString(REMOTE, remoteAlias, "fetch", expectedFetch);
            save = true;
        }
        if (save) {
            config.save();
        }
    }

    public static String getScmGitPrefix() {
        return SCM_GIT_PREFIX;
    }

    /**
     * @return srcdeps will use this branch to perform its magic
     */
    public static String getSrcdepsWorkingBranch() {
        return SRCDEPS_WORKING_BRANCH;
    }

    private static Git openGit(Path dir) throws ScmException {
        try {
            return Git.open(dir.toFile());
        } catch (IOException e) {
            log.debug(String.format("srcdeps: No git repository in [%s]", dir), e);
        }
        try {
            SrcdepsCoreUtils.ensureDirectoryExistsAndEmpty(dir);
            return Git.init().setDirectory(dir.toFile()).call();
        } catch (IOException | GitAPIException e) {
            throw new ScmException(String.format("Could not create directory [%s]", dir), e);
        }
    }

    private static String stripUriPrefix(String url) {
        return url.substring(SCM_GIT_PREFIX.length());
    }

    /**
     * @param url the git URL to generate a remote alias for
     * @return a Byte64 encoded sha1 hash of the given {@code url} prefixed with {@code origin-}
     */
    static String toRemoteAlias(String url) {
        try {
            final MessageDigest sha1Digest = MessageDigest.getInstance("SHA-1");
            sha1Digest.update(url.getBytes(StandardCharsets.UTF_8));
            final byte[] bytes = sha1Digest.digest();
            return "origin-" + Base64.getUrlEncoder().encodeToString(bytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Makes sure that the given {@code refToFind} is available in the {@code advertisedRefs}.
     *
     * @param advertisedRefs the {@link Collection} of {@link Ref}s to search in
     * @param refToFind      the ref name to find
     * @param url            the URL used to fetch
     * @throws ScmException if the given {@code refToFind} could not be found in the {@code advertisedRefs}
     */
    private void assertRefFetched(Collection<Ref> advertisedRefs, String refToFind, String url) throws ScmException {
        for (Ref ref : advertisedRefs) {
            if (refToFind.equals(ref.getName())) {
                return;
            }
        }
        throw new ScmException(String.format("Could not fetch ref [%s] from [%s]", refToFind, url));
    }

    /**
     * Walks back through the history of the {@code advertisedRefs} and tries to find the given {@code commitSha1}.
     *
     * @param repository     the current {@link Repository} to search in
     * @param advertisedRefs the list of refs that were fetched and whose histories should be searched through
     * @param commitSha1     the commit to find
     * @param url            the URL that was used to fetch
     * @throws ScmException if the given {@code commitSha1} could not be found in the history of any of the
     *                      {@code advertisedRefs}
     */
    private void assertRevisionFetched(Repository repository, Collection<Ref> advertisedRefs, String commitSha1,
            String url) throws ScmException {
        ObjectId needle = ObjectId.fromString(commitSha1);
        try {
            for (Ref ref : advertisedRefs) {

                try (RevWalk walk = new RevWalk(repository)) {
                    walk.markStart(walk.parseCommit(ref.getTarget().getObjectId()));
                    walk.setRetainBody(false);

                    for (RevCommit commit : walk) {
                        if (commit.getId().equals(needle)) {
                            return;
                        }
                    }
                }

            }
        } catch (IOException e) {
            new ScmException(String.format("Could not fetch ref [%s] from [%s]", commitSha1, url), e);
        }
        throw new ScmException(String.format("Could not fetch ref [%s] from [%s]", commitSha1, url));
    }

    /**
     * Checkout the source tree of a project to build, esp. using {@link BuildRequest#getScmUrls()} and
     * {@link BuildRequest#getSrcVersion()} of the given {@code request}.
     * <p>
     * This implementation first checks if {@code request.getProjectRootDirectory()} returns a directory containing a
     * valid git repository. If it does not, git init operation is invoked. After that git fetch and git reset are used
     * to checkout the sources.
     *
     * @param request determines the project to checkout
     * @return the {@code commitId} the {@code HEAD} points at
     * @throws ScmException on any SCM related problem
     * @see org.srcdeps.core.Scm#checkout(org.srcdeps.core.BuildRequest)
     */
    @Override
    public String checkout(BuildRequest request) throws ScmException {
        final Path dir = request.getProjectRootDirectory();
        int i = 0;
        final List<String> urls = request.getScmUrls();

        try (Git git = openGit(dir)) {
            for (String url : urls) {
                final String useUrl = stripUriPrefix(url);
                final String result = fetchAndReset(useUrl, i, urls.size(), request.getSrcVersion(), dir, git);
                if (result != null) {
                    return result;
                }
                i++;
            }
        }
        throw new ScmException(
                String.format("Could not checkout [%s] from URLs %s", request.getSrcVersion(), request.getScmUrls()));
    }

    String fetchAndReset(String useUrl, int urlIndex, int urlCount, SrcVersion srcVersion, Path dir, Git git)
            throws ScmException {
        /* Forget local changes */
        try {
            Set<String> removedFiles = git.clean().setCleanDirectories(true).call();
            for (String removedFile : removedFiles) {
                log.debug("srcdeps: Removed an unstaged file [{}]", removedFile);
            }
            git.reset().setMode(ResetType.HARD).call();

        } catch (Exception e) {
            log.warn(String.format("srcdeps: Could not forget local changes in [%s]", dir), e);
        }

        log.info("srcdeps: Fetching version [{}] from SCM URL {}/{} [{}]", srcVersion, urlIndex + 1, urlCount, useUrl);
        final String remoteAlias = toRemoteAlias(useUrl);
        try {

            ensureRemoteAvailable(useUrl, remoteAlias, git);

            final String scmVersion = srcVersion.getScmVersion();
            final String startPoint;
            final String refToFetch;
            final FetchCommand fetch = git.fetch().setRemote(remoteAlias);
            switch (srcVersion.getWellKnownType()) {
            case branch:
                refToFetch = "refs/heads/" + scmVersion;
                fetch.setRefSpecs(
                        new RefSpec("+refs/heads/" + scmVersion + ":refs/remotes/" + remoteAlias + "/" + scmVersion));
                startPoint = remoteAlias + "/" + scmVersion;
                break;
            case tag:
                refToFetch = "refs/tags/" + scmVersion;
                fetch.setRefSpecs(new RefSpec(refToFetch));
                startPoint = scmVersion;
                break;
            case revision:
                refToFetch = null;
                startPoint = scmVersion;
                break;
            default:
                throw new IllegalStateException("Unexpected " + WellKnownType.class.getName() + " value '"
                        + srcVersion.getWellKnownType() + "'.");
            }
            FetchResult fetchResult = fetch.call();

            /*
             * Let's check that the desired startPoint was really fetched from the current URL. Otherwise, the
             * startPoint may come from an older fetch of the same repo URL (but was removed in between) or it may come
             * from an older fetch of another URL. These cases may introduce situations when one developer can see a
             * successful srcdep build (because he still has the outdated ref in his local git repo) but another dev
             * with exectly the same setup cannot checkout because the ref is not there in any of the remote repos
             * anymore.
             */
            Collection<Ref> advertisedRefs = fetchResult.getAdvertisedRefs();
            switch (srcVersion.getWellKnownType()) {
            case branch:
            case tag:
                assertRefFetched(advertisedRefs, refToFetch, useUrl);
                break;
            case revision:
                assertRevisionFetched(git.getRepository(), advertisedRefs, scmVersion, useUrl);
                break;
            default:
                throw new IllegalStateException("Unexpected " + WellKnownType.class.getName() + " value '"
                        + srcVersion.getWellKnownType() + "'.");
            }

            /* Reset the srcdeps-working-branch */
            git.branchCreate().setName(SRCDEPS_WORKING_BRANCH).setForce(true).setStartPoint(startPoint).call();
            final Ref ref = git.checkout().setName(SRCDEPS_WORKING_BRANCH).call();

            return ref.getObjectId().getName();
        } catch (ScmException e) {
            final String msg = String.format("srcdeps: Could not checkout [%s] from SCM URL %d/%d [%s]", srcVersion,
                    urlIndex + 1, urlCount, useUrl);
            if (urlIndex + 1 == urlCount) {
                throw new ScmException(msg, e);
            } else {
                log.warn(msg, e);
            }
        } catch (Exception e) {
            throw new ScmException(String.format("Could not checkout [%s] from SCM URL %d/%d [%s]", srcVersion,
                    urlIndex + 1, urlCount, useUrl), e);
        }
        return null;
    }

    @Override
    public boolean supports(String url) {
        return url.startsWith(SCM_GIT_PREFIX);
    }

}
