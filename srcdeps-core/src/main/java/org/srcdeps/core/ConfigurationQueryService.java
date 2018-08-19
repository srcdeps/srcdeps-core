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

import java.util.regex.Pattern;

import org.srcdeps.core.config.Configuration;
import org.srcdeps.core.config.ScmRepository;

/**
 * A service to query {@link Configuration}.
 *
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */
public class ConfigurationQueryService {

    /**
     * A result of {@link ConfigurationQueryService#findScmRepo(String, String, String)}.
     */
    public static class ScmRepositoryResult {
        private final String queryVersion;
        private final ScmRepository repository;

        public ScmRepositoryResult(ScmRepository payload, String queryVersion) {
            super();
            this.repository = payload;
            this.queryVersion = queryVersion;
        }

        /**
         * @return this {@link ScmRepositoryResult}
         * @throws IllegalStateException if {@link #repository} is {@code null}
         */
        public ScmRepositoryResult assertSuccess() {
            if (repository == null) {
                throw new IllegalStateException();
            }
            return this;
        }

        /**
         * @return the repository matching the query or {@code null} if the query had no match
         */
        public ScmRepository getRepository() {
            return repository;
        }

        /**
         * @return {@code true} if {@link ScmRepository#getBuildVersionPattern()} of {@link #repository} matches the
         *         queried version; {@code false} otherwise
         */
        public boolean matchesBuildVersionPattern() {
            final Pattern pattern = repository.getBuildVersionPattern();
            return pattern != null && pattern.matcher(queryVersion).matches();
        }

    }

    private final Configuration configuration;

    public ConfigurationQueryService(Configuration configuration) {
        super();
        this.configuration = configuration;
    }

    /**
     * Finds the first {@link ScmRepository} associated with the given {@code groupId:artifactId:version} triple in
     * {@link #configuration}.
     *
     * @param groupId
     * @param artifactId
     * @param version
     * @return the matching {@link ScmRepository}
     */
    public ScmRepositoryResult findScmRepo(String groupId, String artifactId, String version) {
        for (ScmRepository scmRepository : configuration.getRepositories()) {
            if (scmRepository.getGavSet().contains(groupId, artifactId, version)) {
                return new ScmRepositoryResult(scmRepository, version);
            }
        }
        return new ScmRepositoryResult(null, version);
    }

}
