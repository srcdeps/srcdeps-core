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
package org.srcdeps.core;

import java.util.Map;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

/**
 * A general purpose pattern for matching GAVs (the {@code groupId}, {@code artifactId}, {@code version} triples).
 * <p>
 * To create a new {@link GavPattern}, use either {@link #of(String)} or {@link #builder()}, both of which accept
 * wildcard patterns (rather than regular expression patterns). See the JavaDocs of the two respective methods for more
 * details.
 * <p>
 * {@link GavPattern} overrides {@link #hashCode()} and {@link #equals(Object)} and can thus be used as a key in a
 * {@link Map}.
 *
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */
public class GavPattern {

    public static class Builder {

        private Pattern artifactIdPattern = MATCH_ALL_PATTERN;
        private Pattern groupIdPattern = MATCH_ALL_PATTERN;
        private Pattern versionPattern = MATCH_ALL_PATTERN;

        private Builder() {
        }

        /**
         * Sets the pattern for {@code artifactId}
         *
         * @param wildcardPattern
         *            a pattern that can contain string literals and asterisk {@code *} wildcards
         * @return this {@link Builder}
         */
        public Builder artifactIdPattern(String wildcardPattern) {
            this.artifactIdPattern = toPattern(wildcardPattern);
            return this;
        }

        public GavPattern build() {
            return new GavPattern(groupIdPattern, artifactIdPattern, versionPattern);
        }

        /**
         * Sets the pattern for {@code groupId}
         *
         * @param wildcardPattern
         *            a pattern that can contain string literals and asterisk {@code *} wildcards
         * @return this {@link Builder}
         */
        public Builder groupIdPattern(String wildcardPattern) {
            this.groupIdPattern = toPattern(wildcardPattern);
            return this;
        }

        /**
         * Sets the pattern for {@code version}
         *
         * @param wildcardPattern
         *            a pattern that can contain string literals and asterisk {@code *} wildcards
         * @return this {@link Builder}
         */
        public Builder versionPattern(String wildcardPattern) {
            this.versionPattern = toPattern(wildcardPattern);
            return this;
        }

    }

    private static final char DELIMITER = ':';
    private static final String DELIMITER_STRING;
    private static final GavPattern MATCH_ALL;
    private static final Pattern MATCH_ALL_PATTERN;
    private static final String MATCH_ALL_PATTERN_SOURCE = ".*";

    private static final GavPattern MATCH_SNAPSHOTS;
    private static final String MULTI_WILDCARD = "*";
    private static final String SNAPSHOT_SUFFIX = "-SNAPSHOT";

    static {
        MATCH_ALL_PATTERN = Pattern.compile(MATCH_ALL_PATTERN_SOURCE);
        DELIMITER_STRING = String.valueOf(DELIMITER);
        MATCH_ALL = new GavPattern(MATCH_ALL_PATTERN, MATCH_ALL_PATTERN, MATCH_ALL_PATTERN);
        MATCH_SNAPSHOTS = new GavPattern(MATCH_ALL_PATTERN, MATCH_ALL_PATTERN, toPattern(MULTI_WILDCARD + SNAPSHOT_SUFFIX));
    }

    /**
     * @return a new {@link Builder}
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * @return a singleton that matches all possible GAVs
     */
    public static GavPattern matchAll() {
        return MATCH_ALL;
    }

    /**
     * @return a singleton that matches any GAV that has a version ending with {@value #SNAPSHOT_SUFFIX}
     */
    public static GavPattern matchSnapshots() {
        return MATCH_SNAPSHOTS;
    }

    /**
     * Creates a new {@link GavPattern} out of the given {@code wildcardPattern}. A wildcard pattern consists of string
     * literals and asterisk wildcard {@code *}. {@code *} matches zero or many arbitrary characters. Wildcard patterns
     * for groupId, artifactId and version need to be delimited by colon {@value #DELIMITER}.
     * <p>
     * GAV pattern examples:
     * <p>
     * {@code org.my-group} - an equivalent of {@code org.my-group:*:*}. It will match any version of any artifact
     * having groupId {@code org.my-group}.
     * <p>
     * {@code org.my-group*} - an equivalent of {@code org.my-group*:*:*}. It will match any version of any artifact
     * whose groupId starts with {@code org.my-group} - i.e. it will match all of {@code org.my-group},
     * {@code org.my-group.api}, {@code org.my-group.impl}, etc.
     * <p>
     * {@code org.my-group:my-artifact} - an equivalent of {@code org.my-group:my-artifact:*}. It will match any version
     * of all such artifacts that have groupId {@code org.my-group} and artifactId {@code my-artifact}
     * <p>
     * {@code org.my-group:my-artifact:1.2.3} - will match just the version 1.2.3 of artifacts
     * {@code org.my-group:my-artifact}.
     *
     * @param wildcardPattern
     *            a string pattern to parse and create a new {@link GavPattern} from
     * @return a new {@link GavPattern}
     */
    public static GavPattern of(String wildcardPattern) {
        final Pattern groupIdPattern;
        StringTokenizer st = new StringTokenizer(wildcardPattern, DELIMITER_STRING);
        if (st.hasMoreTokens()) {
            groupIdPattern = toPattern(st.nextToken());
        } else {
            groupIdPattern = MATCH_ALL_PATTERN;
        }
        final Pattern artifactIdPattern;
        if (st.hasMoreTokens()) {
            artifactIdPattern = toPattern(st.nextToken());
        } else {
            artifactIdPattern = MATCH_ALL_PATTERN;
        }
        final Pattern versionPattern;
        if (st.hasMoreTokens()) {
            versionPattern = toPattern(st.nextToken());
        } else {
            versionPattern = MATCH_ALL_PATTERN;
        }
        return new GavPattern(groupIdPattern, artifactIdPattern, versionPattern);
    }

    /**
     * Transforms the given {@code wildcardPattern} to a new {@link Pattern}.
     *
     * @param wildcardPattern
     * @return a new {@link Pattern}
     */
    private static Pattern toPattern(String wildcardPattern) {
        return Pattern.compile(wildcardPattern.replace(MULTI_WILDCARD, MATCH_ALL_PATTERN_SOURCE));
    }

    /**
     * Transforms the given {@code pattern} to its wildcard representation.
     *
     * @param pattern
     *            the {@link Pattern} to transform
     * @return a wildcard representation of the given {@code pattern}
     */
    private static String toWildcard(Pattern pattern) {
        return pattern.pattern().replace(MATCH_ALL_PATTERN_SOURCE, MULTI_WILDCARD);
    }

    private final Pattern artifactIdPattern;
    private final Pattern groupIdPattern;
    private final String source;
    private final Pattern versionPattern;

    GavPattern(Pattern groupIdPattern, Pattern artifactIdPattern, Pattern versionPattern) {
        super();
        this.groupIdPattern = groupIdPattern;
        this.artifactIdPattern = artifactIdPattern;
        this.versionPattern = versionPattern;

        StringBuilder source = new StringBuilder(groupIdPattern.pattern().length()
                + artifactIdPattern.pattern().length() + versionPattern.pattern().length() + 2);

        source.append(toWildcard(groupIdPattern));
        final boolean artifactMatchesAll = MATCH_ALL_PATTERN_SOURCE.equals(artifactIdPattern.pattern());
        final boolean versionMatchesAll = MATCH_ALL_PATTERN_SOURCE.equals(versionPattern.pattern());
        if (!versionMatchesAll) {
            source.append(DELIMITER).append(toWildcard(artifactIdPattern));
            source.append(DELIMITER).append(toWildcard(versionPattern));
        } else if (!artifactMatchesAll) {
            source.append(DELIMITER).append(toWildcard(artifactIdPattern));
        }
        this.source = source.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        GavPattern other = (GavPattern) obj;
        return this.source.equals(other.source);
    }

    @Override
    public int hashCode() {
        return this.source.hashCode();
    }

    /**
     * Matches the given {@code groupId}, {@code artifactId}, {@code version} triple against this {@link GavPattern}.
     *
     * @param groupId
     * @param artifactId
     * @param version
     * @return {@code true} if this {@link GavPattern} matches the given {@code groupId}, {@code artifactId},
     *         {@code version} triple and {@code false otherwise}
     */
    public boolean matches(String groupId, String artifactId, String version) {
        return groupIdPattern.matcher(groupId).matches() && artifactIdPattern.matcher(artifactId).matches()
                && versionPattern.matcher(version).matches();
    }

    @Override
    public String toString() {
        return source;
    }

}
