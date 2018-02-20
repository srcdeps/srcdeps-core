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
 * A collection of {@link Scm}s.
 *
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 * @since 3.2.1
 */
public interface ScmService {

    /**
     * Finds the {@link Scm} suitable for the given {@link BuildRequest} and checkout the source tree of a project to
     * build, esp. using {@link BuildRequest#getScmUrls()} and {@link BuildRequest#getSrcVersion()} of the given
     * {@code request}.
     *
     * @param request
     *            determines the project to checkout
     * @return the {@code commitId} the {@code HEAD} points at
     * @throws ScmException
     *             on any SCM related problem
     */
    String checkout(BuildRequest request) throws ScmException;

}
