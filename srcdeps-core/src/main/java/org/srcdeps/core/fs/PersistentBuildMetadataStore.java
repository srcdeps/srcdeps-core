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
package org.srcdeps.core.fs;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.srcdeps.core.BuildMetadataStore;
import org.srcdeps.core.Gavtc;
import org.srcdeps.core.util.Consumer;

/**
 * A {@link BuildMetadataStore} that stores its entries in the filesystem.
 *
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 * @since 3.2.2
 */
@Named
@Singleton
public class PersistentBuildMetadataStore implements BuildMetadataStore {

    static class BuildRequestHashVisitor implements FileVisitor<Path> {
        private Consumer<String> consumer;

        private int depth = 0;

        BuildRequestHashVisitor(Consumer<String> consumer) {
            this.consumer = consumer;
        }

        @Override
        public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
            depth--;
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
            if (depth == DISTRIBUTION_DEPTH) {
                int i = dir.getNameCount() - DISTRIBUTION_DEPTH;
                String buildRequestIdHash = dir.getName(i++).toString() + dir.getName(i++).toString()
                        + dir.getName(i++).toString() + dir.getName(i++).toString() + dir.getName(i++).toString();
                assert dir.getNameCount() == i;
                consumer.accept(buildRequestIdHash);
                depth++;
                return FileVisitResult.SKIP_SUBTREE;
            } else {
                depth++;
                return FileVisitResult.CONTINUE;
            }
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
            throw exc;
        }

    }

    public static class BuildRequestIdCollector implements Consumer<String> {
        private final List<String> hashes = new ArrayList<>();

        @Override
        public void accept(String t) {
            hashes.add(t);
        }

        public List<String> getHashes() {
            return hashes;
        }
    }

    private static final String COMMIT_ID = "commitId";

    private static final int DISTRIBUTION_DEPTH = 4 + 1;

    private static final Logger log = LoggerFactory.getLogger(PersistentBuildMetadataStore.class);

    private static void store(final Path p, String content) {
        try {
            Files.createDirectories(p.getParent());
            Files.write(p, content.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new RuntimeException(String.format("Could not write to path [%s]", p), e);
        }
    }

    private final Path rootDirectory;

    public PersistentBuildMetadataStore(Path rootDirectory) {
        super();
        this.rootDirectory = rootDirectory;
        try {
            Files.createDirectories(rootDirectory);
        } catch (IOException e) {
            throw new RuntimeException(
                    String.format("Could not create %s.rootDirectory [%s]", this.getClass().getName(), rootDirectory));
        }
    }

    public Path createBuildRequestIdPath(String hash) {
        int i = 0;
        final Path p = rootDirectory.resolve(hash.substring(i++, i)).resolve(hash.substring(i++, i))
                .resolve(hash.substring(i++, i)).resolve(hash.substring(i++, i)).resolve(hash.substring(i++));
        assert i == DISTRIBUTION_DEPTH;
        return p;
    }

    /** {@inheritDoc} */
    @Override
    public CheckSha1Consumer createCheckSha1Checker(String requestId, String buildRequestIdHash) {
        return new CheckSha1Consumer(this, requestId, buildRequestIdHash);
    }

    /** {@inheritDoc} */
    @Override
    public StoreSha1Consumer createStoreSha1Consumer(String requestId, String buildRequestIdHash) {
        return new StoreSha1Consumer(this, requestId, buildRequestIdHash);
    }

    /** {@inheritDoc} */
    @Override
    public String retrieveCommitId(String requestId, String buildRequestIdHash) {
        final Path p = createBuildRequestIdPath(buildRequestIdHash).resolve(COMMIT_ID);
        if (Files.exists(p)) {
            try {
                String result = new String(Files.readAllBytes(p), StandardCharsets.UTF_8);
                log.debug("srcdeps[{}]: Path [{}] points at commitId [{}]", requestId, p, result);
                return result;
            } catch (IOException e) {
                throw new RuntimeException(String.format("Could not read %s", p), e);
            }
        }
        log.debug("srcdeps[{}]: commitId path [{}] does not exist", requestId, p);
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public String retrieveSha1(String requestId, String buildRequestIdHash, Gavtc gavtc) {
        final String gavtcString = gavtc.getGavtcString().replace(':', '_');
        final Path p = createBuildRequestIdPath(buildRequestIdHash).resolve(gavtcString);
        if (Files.exists(p)) {
            try {
                final String result = new String(Files.readAllBytes(p), StandardCharsets.UTF_8);
                log.debug("srcdeps[{}]: Path [{}] points at sha1 [{}]", requestId, p, result);
                return result;
            } catch (IOException e) {
                throw new RuntimeException(String.format("Could not read %s", p), e);
            }
        }
        log.debug("srcdeps[{}]: sha1 path [{}] does not exist", requestId, p);
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public void storeCommitId(String requestId, String buildRequestIdHash, String commitId) {
        final Path p = createBuildRequestIdPath(buildRequestIdHash).resolve(COMMIT_ID);
        log.debug("srcdeps[{}]: Path [{}] will point at commitId [{}]", requestId, p, commitId);
        store(p, commitId);
    }

    /** {@inheritDoc} */
    @Override
    public void storeSha1(String requestId, String buildRequestIdHash, Gavtc gavtc, String sha1) {
        final String gavtcString = gavtc.getGavtcString().replace(':', '_');
        final Path p = createBuildRequestIdPath(buildRequestIdHash).resolve(gavtcString);
        log.debug("srcdeps[{}]: Path [{}] will point at sha1 [{}]", requestId, p, sha1);
        store(p, sha1);
    }

    /** {@inheritDoc} */
    @Override
    public void walkBuildRequestHashes(Consumer<String> consumer) {
        try {
            Files.walkFileTree(rootDirectory, new BuildRequestHashVisitor(consumer));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
