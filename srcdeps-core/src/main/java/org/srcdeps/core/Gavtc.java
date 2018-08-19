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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.StringTokenizer;

/**
 * An immutable {@link #groupId}, {@link #artifactId}, {@link #version}, {@code type}, {@code classifier} tuple. Note
 * that only {@code classifier} can be {@code null}.
 *
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */
public class Gavtc extends Gav {

    /**
     * Returns a new {@link Gavtc} instance parsed out of the given {@code gavtcString}.
     *
     * @param gavtcString the string to parse, something of the form {@code groupId:artifactId:version:type:classifier}
     * @return a new {@link Gavtc} instance parsed out of the given {@code gavtcString}
     */
    public static Gavtc of(String gavtcString) {
        StringTokenizer st = new StringTokenizer(gavtcString, ":");
        if (!st.hasMoreTokens()) {
            throw new IllegalStateException(
                    String.format("Cannot parse [%s] to a " + Gav.class.getName(), gavtcString));
        } else {
            final String g = st.nextToken();
            if (!st.hasMoreTokens()) {
                throw new IllegalStateException(
                        String.format("Cannot parse [%s] to a " + Gavtc.class.getName(), gavtcString));
            } else {
                final String a = st.nextToken();
                if (!st.hasMoreTokens()) {
                    throw new IllegalStateException(
                            String.format("Cannot parse [%s] to a " + Gavtc.class.getName(), gavtcString));
                } else {
                    final String v = st.nextToken();
                    if (!st.hasMoreTokens()) {
                        throw new IllegalStateException(
                                String.format("Cannot parse [%s] to a " + Gavtc.class.getName(), gavtcString));
                    } else {
                        final String t = st.nextToken();
                        final String c = st.hasMoreTokens() ? st.nextToken() : null;
                        return new Gavtc(g, a, v, t, c);
                    }
                }
            }
        }
    }

    /**
     * Returns an unmodifiable {@link List} of {@link Gavtc}s parsed out of the given {@code gavtcPattern}. The only
     * supported pattern-like feature is {@code [alternative1,alternative2]} factorization of artifact type.
     * <p>
     * Example: {@code ofPattern("g:a:v:[jar,pom]")} creates a list equivalent to
     * {@code Collections.unmodifiableList(Arrays.asList(new Gavtc("g", "a", "v", "jar"), new Gavtc("g", "a", "v", "pom")))}
     *
     * @param gavtcPattern
     * @return an unmodifiable {@link List} of {@link Gavtc}s
     */
    public static List<Gavtc> ofPattern(String gavtcPattern) {
        StringTokenizer st = new StringTokenizer(gavtcPattern, ":");
        if (!st.hasMoreTokens()) {
            throw new IllegalStateException(
                    String.format("Cannot parse [%s] to a " + Gav.class.getName(), gavtcPattern));
        } else {
            final String g = st.nextToken();
            if (!st.hasMoreTokens()) {
                throw new IllegalStateException(
                        String.format("Cannot parse [%s] to a " + Gavtc.class.getName(), gavtcPattern));
            } else {
                final String a = st.nextToken();
                if (!st.hasMoreTokens()) {
                    throw new IllegalStateException(
                            String.format("Cannot parse [%s] to a " + Gavtc.class.getName(), gavtcPattern));
                } else {
                    final String v = st.nextToken();
                    if (!st.hasMoreTokens()) {
                        throw new IllegalStateException(
                                String.format("Cannot parse [%s] to a " + Gavtc.class.getName(), gavtcPattern));
                    } else {
                        final String t = st.nextToken();
                        final String c = st.hasMoreTokens() ? st.nextToken() : null;
                        List<Gavtc> result = new ArrayList<>();
                        StringTokenizer typeTokenizer = new StringTokenizer(t, "[,]");
                        while (typeTokenizer.hasMoreTokens()) {
                            String type = typeTokenizer.nextToken();
                            result.add(new Gavtc(g, a, v, type, c));
                        }
                        return Collections.unmodifiableList(result);
                    }
                }
            }
        }
    }

    private final String classifier;
    private final int hashCode;
    private final String type;

    public Gavtc(String groupId, String artifactId, String version, String type) {
        this(groupId, artifactId, version, type, null);
    }

    public Gavtc(String groupId, String artifactId, String version, String type, String classifier) {
        super(groupId, artifactId, version);
        this.type = type;
        this.classifier = "".equals(classifier) ? null : classifier;
        this.hashCode = 31 * (31 * super.hashCode() + type.hashCode())
                + ((classifier == null) ? 0 : classifier.hashCode());
    }

    @Override
    public int compareTo(Gav other) {
        Gavtc o = (Gavtc) other;
        int result = super.compareTo(o);
        if (result != 0) {
            return result;
        } else {
            result = this.type.compareTo(o.type);
            if (result != 0) {
                return result;
            } else if (this.classifier == null) {
                if (o.classifier == null) {
                    return 0;
                } else {
                    return -1;
                }
            } else if (o.classifier == null) {
                return 1;
            } else {
                return this.classifier.compareTo(o.classifier);
            }
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        if (getClass() != obj.getClass())
            return false;
        Gavtc other = (Gavtc) obj;
        if (!type.equals(other.type))
            return false;
        if (classifier == null) {
            if (other.classifier != null)
                return false;
        } else if (!classifier.equals(other.classifier))
            return false;
        return true;
    }

    /**
     * @return the classifier or {@code null} if none was specified
     */
    public String getClassifier() {
        return classifier;
    }

    /**
     * @return a canonical colon delimited string
     */
    public String getGavtcString() {
        return super.toString() + ":" + type + (classifier == null ? "" : ":" + classifier);
    }

    /**
     * @return the artifact type, such as {@code jar} or {@code pom}
     */
    public String getType() {
        return type;
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    @Override
    public String toString() {
        return getGavtcString();
    }

}
