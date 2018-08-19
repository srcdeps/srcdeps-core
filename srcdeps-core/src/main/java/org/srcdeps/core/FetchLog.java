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

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A facility to ensure that we do not fetch and build the same SCM repo multiple times during a single outer build. If
 * a build of a SCM repo produces multiple artifacts, we want to build them only once pre outer build. This not only
 * saves time but is also especially important with branch srcdeps versions, where fetching twice might lead to two
 * builds out of two different commits. The remote branch might have changed between the two fetch operations.
 * <p>
 * {@link FetchLog} uses an in-memory store and does not require a fetch from a remote SMC repository.
 * {@link BuildMetadataStore} on the other hand requires both persistent storage and a fetch from the remote.
 *
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 * @since 3.2.1
 */
public class FetchLog {
    private static final Logger log = LoggerFactory.getLogger(FetchLog.class);

    private final Set<FetchId> fetchIds = Collections.newSetFromMap(new ConcurrentHashMap<FetchId, Boolean>());

    /**
     * @param fetchId
     *                    the {@link FetchId} to query
     * @return {@code true} if the repository identified by the given {@link FetchId} can be considered up-to-date (i.e.
     *         fetched) and built in the current JVM; {@code false} otherwise
     */
    public boolean contains(FetchId fetchId) {
        final boolean result = fetchIds.contains(fetchId);
        log.debug("srcdeps: SCM repo {} in {}: {}", (result ? "present" : "absent"), FetchLog.class.getSimpleName(),
                fetchId);
        return result;
    }

    /**
     * Mark the given {@link FetchId} as being up-to-date (i.e. fetched) and eventually re-built in the current JVM.
     *
     * @param fetchId
     *                    the {@link FetchId} to add
     */
    public void add(FetchId fetchId) {
        log.debug("srcdeps: Adding SCM repo to {}: {}", FetchLog.class.getSimpleName(), fetchId);
        fetchIds.add(fetchId);
    }
}
