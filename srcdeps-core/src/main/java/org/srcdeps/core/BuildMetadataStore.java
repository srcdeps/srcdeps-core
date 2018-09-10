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
package org.srcdeps.core;

import java.io.IOException;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.srcdeps.core.util.Consumer;
import org.srcdeps.core.util.SrcdepsCoreUtils;

/**
 * A service to tell whether a given {@link BuildRequest} was performed already in the past, out of which SCM revision
 * it was built and what were the sha1 hashes of the artifacts built.
 *
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 * @since 3.2.2
 */
public interface BuildMetadataStore {

    /**
     * A {@link Consumer} to check whether the sha1 hash stored in a {@link BuildMetadataStore} is the same as of the
     * one in the local Maven repository.
     *
     * @since 3.2.2
     */
    class CheckSha1Consumer implements Consumer<GavtcPath> {

        private static final Logger log = LoggerFactory.getLogger(CheckSha1Consumer.class);

        private boolean anyArtifactChanged = false;
        private final BuildMetadataStore buildMetadataStore;

        private final String buildRequestIdHash;

        public CheckSha1Consumer(BuildMetadataStore buildMetadataStore, String buildRequestIdHash) {
            this.buildMetadataStore = buildMetadataStore;
            this.buildRequestIdHash = buildRequestIdHash;
        }

        @Override
        public void accept(GavtcPath gavtcPath) {
            if (!anyArtifactChanged) {
                final String pastSha1 = buildMetadataStore.retrieveSha1(buildRequestIdHash, gavtcPath);
                if (pastSha1 == null) {
                    log.info("srcdeps: Rebuilding: sha1 of artifact [{}] was not found in {}", gavtcPath.getGavtcString(),
                            BuildMetadataStore.class.getSimpleName());
                    anyArtifactChanged = true;
                } else {
                    final Path path = gavtcPath.getPath();
                    try {
                        final String mvnLocalRepoArtifactSha1 = SrcdepsCoreUtils.sha1HexString(path);
                        if (!pastSha1.equals(mvnLocalRepoArtifactSha1)) {
                            log.info(
                                    "srcdeps: Rebuilding: sha1 of artifact [{}] in local Maven repository differs from last known sha1 built by srcdeps",
                                    gavtcPath.getGavtcString());
                            anyArtifactChanged = true;
                        }
                    } catch (NoSuchAlgorithmException | IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }

        /**
         * @return {@code true} if any of the artifacts passed in through {@link #accept(GavtcPath)} had a different
         *         sha1 as compared with the value stored in {@link #buildMetadataStore}
         */
        public boolean isAnyArtifactChanged() {
            return anyArtifactChanged;
        }

    }

    /**
     * A {@link Consumer} to store the sha1 hashes of artifacts to {@link BuildMetadataStore}.
     *
     * @since 3.2.2
     */
    class StoreSha1Consumer implements Consumer<GavtcPath> {
        private final BuildMetadataStore buildMetadataStore;
        private final String buildRequestIdHash;
        private int count = 0;

        public StoreSha1Consumer(BuildMetadataStore buildMetadataStore, String buildRequestIdHash) {
            this.buildMetadataStore = buildMetadataStore;
            this.buildRequestIdHash = buildRequestIdHash;
        }

        /**
         * Computes sha1 of the file under the given {@link GavtcPath#getPath()} and stores it in
         * {@link #buildMetadataStore}
         */
        @Override
        public void accept(GavtcPath gavtcPath) {
            try {
                final Path path = gavtcPath.getPath();
                final String mvnLocalRepoArtifactSha1 = SrcdepsCoreUtils.sha1HexString(path);
                buildMetadataStore.storeSha1(buildRequestIdHash, gavtcPath, mvnLocalRepoArtifactSha1);
                count++;
            } catch (NoSuchAlgorithmException | IOException e) {
                throw new RuntimeException(e);
            }
        }

        /**
         * @return the number of {@link GavtcPath}s processed by {@link #accept(GavtcPath)}
         */
        public int getCount() {
            return count;
        }

    }

    /**
     * @param buildRequestIdHash hash of a {@link BuildRequest}
     * @return a new {@link CheckSha1Consumer}
     *
     * @since 3.2.2
     */
    CheckSha1Consumer createCheckSha1Checker(String buildRequestIdHash);

    /**
     * @param buildRequestIdHash hash of a {@link BuildRequest}
     * @return a new {@link StoreSha1Consumer}
     *
     * @since 3.2.2
     */
    StoreSha1Consumer createStoreSha1Consumer(String buildRequestIdHash);

    /**
     * Returns a {@code commitId} out of which the {@link BuildRequest} characterized by the given
     * {@code buildRequestIdHash} was built in the past or {@code null} if the {@code buildRequestIdHash} is not know to
     * this {@link BuildMetadataStore}.
     *
     * @param buildRequestIdHash hash of a {@link BuildRequest} to check
     * @return a non-null {@code commitId} out of which the given {@link BuildRequest} was built in the past or
     *         {@code null} if the {@link BuildRequest} represented by the given {@link BuildRequestId} was not built
     *         yet.
     *
     * @since 3.2.2
     */
    String retrieveCommitId(String buildRequestIdHash);

    /**
     * @param buildRequestIdHash hash of a {@link BuildRequest}
     * @param gavtc              the artifact
     * @return the sha1 of the given {@link Gavtc} in hex form or {@code null} if the given {@link Gavtc} was not stored
     *         for the given {@code buildRequestIdHash} before
     *
     * @since 3.2.2
     */
    String retrieveSha1(String buildRequestIdHash, Gavtc gavtc);

    /**
     * Link the given {@code buildRequestIdHash} with the given {@code commitId}.
     *
     * @param buildRequestIdHash hash of a {@link BuildRequest} to add to this {@link BuildMetadataStore}
     * @param commitId           the commitId out of which the given {@code buildRequestIdHash} was built
     *
     * @since 3.2.2
     */
    void storeCommitId(String buildRequestIdHash, String commitId);

    /**
     * Store the given {@code sha1} for the given {@code buildRequestIdHash} and {@link Gavtc} in this
     * {@link BuildMetadataStore}
     *
     * @param buildRequestIdHash hash of a {@link BuildRequest}
     * @param gavtc              the artifact
     * @param sha1               the sha1 hash in hex form
     *
     * @since 3.2.2
     */
    void storeSha1(String buildRequestIdHash, Gavtc gavtc, String sha1);

    /**
     * Iterate over {@link BuildRequest} hashes stored in this {@link BuildMetadataStore} and pass them to the given
     * {@link Consumer}
     *
     * @param consumer the {@link Consumer} to feed
     *
     * @since 3.2.2
     */
    void walkBuildRequestHashes(Consumer<String> consumer);

}
