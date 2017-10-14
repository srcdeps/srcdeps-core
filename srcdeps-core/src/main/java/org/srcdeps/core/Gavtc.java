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

import java.util.StringTokenizer;

/**
 * An immutable {@link #groupId}, {@link #artifactId}, {@link #version}, {@code type}, {@code classifier} tuple.
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
            throw new IllegalStateException(String.format("Cannot parse [%s] to a " + Gav.class.getName(), gavtcString));
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

    private final String classifier;

    private final String type;

    public Gavtc(String groupId, String artifactId, String version, String type) {
        this(groupId, artifactId, version, type, null);
    }

    public Gavtc(String groupId, String artifactId, String version, String type, String classifier) {
        super(groupId, artifactId, version);
        this.type = type;
        this.classifier = classifier;
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
        if (classifier == null) {
            if (other.classifier != null)
                return false;
        } else if (!classifier.equals(other.classifier))
            return false;
        if (type == null) {
            if (other.type != null)
                return false;
        } else if (!type.equals(other.type))
            return false;
        return true;
    }

    public String getClassifier() {
        return classifier;
    }

    public String getType() {
        return type;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((classifier == null) ? 0 : classifier.hashCode());
        result = prime * result + ((type == null) ? 0 : type.hashCode());
        return result;
    }

    @Override
    public String toString() {
        return super.toString() + ":" + type + (classifier == null ? "" : ":" + classifier);
    }

}
