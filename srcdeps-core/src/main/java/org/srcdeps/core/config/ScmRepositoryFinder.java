/**
 * Copyright 2015-2017 Maven Source Dependencies
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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.srcdeps.core.GavPattern;

/**
 * A class responsible for matching artifacts against patterns available in {@link ScmRepository#getSelectors()}.
 *
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */
public class ScmRepositoryFinder {


    private final Map<GavPattern, ScmRepository> gavPatternRepositoryMap;

    public ScmRepositoryFinder(Configuration configuration) {
        super();
        Map<GavPattern, ScmRepository> resolvers = new LinkedHashMap<>();

        for (ScmRepository repository : configuration.getRepositories()) {
            for (String selector : repository.getSelectors()) {
                resolvers.put(GavPattern.of(selector), repository);
            }
        }

        this.gavPatternRepositoryMap = Collections.unmodifiableMap(resolvers);
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
        for (Entry<GavPattern, ScmRepository> en : gavPatternRepositoryMap.entrySet()) {
            if (en.getKey().matches(groupId, artifactId, version)) {
                return en.getValue();
            }
        }
        throw new IllegalStateException(
                String.format("No srcdeps SCM repository found for GAV [%s:%s:%s]", groupId, artifactId, version));
    }

}
