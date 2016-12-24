/**
 * Copyright 2015-2016 Maven Source Dependencies
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
package org.srcdeps.core.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

/**
 * A class responsible for matching artifacts against patterns available in {@link ScmRepository#getSelectors()}.
 *
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */
public class ScmRepositoryFinder {

    static class SelectionResolver {
        private static final Pattern MATCH_ALL_PATTERN = Pattern.compile(".*");

        private static Pattern toPattern(String token) {
            return Pattern.compile(token.replace("*", ".*"));
        }

        private final Pattern artifactIdPattern;
        private final Pattern groupIdPattern;
        private final ScmRepository repository;

        private final Pattern versionPattern;

        SelectionResolver(String selector, ScmRepository repository) {
            this.repository = repository;

            StringTokenizer st = new StringTokenizer(selector, ":");
            if (st.hasMoreTokens()) {
                groupIdPattern = toPattern(st.nextToken());
            } else {
                groupIdPattern = MATCH_ALL_PATTERN;
            }
            if (st.hasMoreTokens()) {
                artifactIdPattern = toPattern(st.nextToken());
            } else {
                artifactIdPattern = MATCH_ALL_PATTERN;
            }
            if (st.hasMoreTokens()) {
                versionPattern = toPattern(st.nextToken());
            } else {
                versionPattern = MATCH_ALL_PATTERN;
            }
        }

        public ScmRepository getRepository() {
            return repository;
        }

        public boolean matches(String groupId, String artifactId, String version) {
            return groupIdPattern.matcher(groupId).matches() && artifactIdPattern.matcher(artifactId).matches()
                    && versionPattern.matcher(version).matches();
        }
    }

    private final List<SelectionResolver> resolvers;

    public ScmRepositoryFinder(Configuration configuration) {
        super();
        List<SelectionResolver> resolvers = new ArrayList<>();

        for (ScmRepository repository : configuration.getRepositories()) {
            for (String selector : repository.getSelectors()) {
                resolvers.add(new SelectionResolver(selector, repository));
            }
        }

        this.resolvers = Collections.unmodifiableList(resolvers);
    }

    /**
     * Finds the {@link ScmRepository} that has a selector matching the given {@code groupId}, {@code artifactId} and
     * {@code version}. Returns the matching {@link ScmRepository} or throws an {@link IllegalStateException}.
     *
     * @param groupId
     *            the groupId of the artifact for which we seek a matching {@link ScmRepository}
     * @param artifactId
     *            the artifactId of the artifact for which we seek a matching {@link ScmRepository}
     * @param version
     *            the version of the artifact for which we seek a matching {@link ScmRepository}
     * @return the matching {@link ScmRepository} or throws an {@link IllegalStateException}
     * @throws IllegalStateException
     *             if no matching {@link ScmRepository} was found
     */
    public ScmRepository findRepository(String groupId, String artifactId, String version) {
        for (SelectionResolver resolver : resolvers) {
            if (resolver.matches(groupId, artifactId, version)) {
                return resolver.getRepository();
            }
        }
        throw new IllegalStateException(
                String.format("No srcdeps SCM repository found for GAV [%s:%s:%s]", groupId, artifactId, version));
    }

}
