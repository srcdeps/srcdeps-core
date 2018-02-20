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

/**
 * A service to tell whether a given {@link BuildRequest} was performed already in the past either by the current JVM or
 * by any other process.
 *
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 * @since 3.2.1
 */
public interface BuildRefStore {

    /**
     * Returns a {@code commitId} out of which the given {@code request} was built in the past or {@code null} if the
     * {@code request} was not built yet.
     *
     * @param request
     *            the {@link BuildRequestId} to check
     * @return a non-null {@code commitId} out of which the given {@link BuildRequest} built in the past or {@code null}
     *         if the {@link BuildRequest} represeneted by the given {@link BuildRequestId} was not built yet.
     */
    String retrieve(BuildRequestId request);

    /**
     * Link the given {@code BuildRequestId} with the given {@code commitId}.
     *
     * @param request
     *            the {@link BuildRequest} to add to this {@link BuildRefStore}
     * @param commitId
     *            the commitId out of which the given {@code request} was built
     */
    void store(BuildRequestId request, String commitId);

}
