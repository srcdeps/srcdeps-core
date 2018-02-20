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
package org.srcdeps.core.fs;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.srcdeps.core.BuildRefStore;
import org.srcdeps.core.BuildRequestId;

/**
 * A {@link BuildRefStore} that stores its entries to the filesystem.
 *
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 * @since 3.2.1
 */
@Named
@Singleton
public class PersistentBuildRefStore implements BuildRefStore {
    private static final Logger log = LoggerFactory.getLogger(PersistentBuildRefStore.class);

    private final Path rootDirectory;

    public PersistentBuildRefStore(Path rootDirectory) {
        super();
        this.rootDirectory = rootDirectory;
        try {
            Files.createDirectories(rootDirectory);
        } catch (IOException e) {
            throw new RuntimeException(
                    String.format("Could not create %s.rootDirectory [%s]", this.getClass().getName(), rootDirectory));
        }
    }

    /** {@inheritDoc} */
    @Override
    public String retrieve(BuildRequestId request) {

        Path p = rootDirectory.resolve(request.getHash());
        if (Files.exists(p)) {
            try {
                final String commitId = new String(Files.readAllBytes(p), StandardCharsets.UTF_8);
                log.debug("{} path {} points at commitId {}", BuildRequestId.class.getSimpleName(), p, commitId);
                return commitId;
            } catch (IOException e) {
                throw new RuntimeException(String.format("Could not read path [%s]", p), e);
            }
        }
        log.debug("{} path {} does not exist", BuildRequestId.class.getSimpleName(), p);
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public void store(BuildRequestId request, String commitId) {
        Path p = rootDirectory.resolve(request.getHash());
        log.debug("{} path {} will point at commitId {}", BuildRequestId.class.getSimpleName(), p, commitId);
        try {
            Files.write(p, commitId.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new RuntimeException(String.format("Could write to path [%s]", p), e);
        }
    }

}
