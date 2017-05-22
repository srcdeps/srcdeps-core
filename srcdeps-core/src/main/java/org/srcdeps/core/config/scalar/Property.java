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
package org.srcdeps.core.config.scalar;

import java.util.Map;

/**
 * A scalar value that can be interpreted as either a key without value or as a
 * key-value pair.
 *
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */
public class Property {

    static class PropertyParser {
        private int offset = 0;
        private final String source;
        private final int sourceLength;

        PropertyParser(String source) {
            super();
            this.source = source;
            this.sourceLength = source.length();
        }

        public boolean hasNextToken() {
            return offset < sourceLength;
        }

        public String nextToken() {
            StringBuilder result = new StringBuilder(sourceLength - offset);
            boolean escaped = false;
            LOOP: while (offset < sourceLength) {
                final char ch = source.charAt(offset++);
                switch (ch) {
                case BACKSLASH:
                    if (offset == sourceLength) {
                        throw new IllegalStateException(String.format(
                                "Cannot parse a %s: The input string %s ends with an escape character '%s'. Remove the terminal '%s'",
                                getClass().getSimpleName(), source, BACKSLASH, BACKSLASH));
                    } else if (escaped) {
                        result.append(ch);
                        escaped = false;
                        break;
                    } else {
                        escaped = true;
                        continue;
                    }
                case EQUALS:
                    if (escaped) {
                        result.append(ch);
                        escaped = false;
                        break;
                    } else {
                        break LOOP;
                    }
                default:
                    if (escaped) {
                        throw new IllegalStateException(String.format(
                                "Cannot parse a %s: The input string \"%s\" contains an unknown escape sequence \"%s%s\" at offset %d.",
                                getClass().getSimpleName(), source, BACKSLASH, ch, offset-2));
                    }
                    result.append(ch);
                    escaped = false;
                    break;
                }
            }
            return result.length() == 0 ? null : result.toString();
        }

    }

    private static final char BACKSLASH = '\\';

    private static final char EQUALS = '=';

    private static void escape(StringBuilder result, String token) {
        for (int i = 0; i < token.length(); i++) {
            final char ch = token.charAt(i);
            switch (ch) {
            case EQUALS:
            case BACKSLASH:
                result.append(BACKSLASH);
            default:
                result.append(ch);
                break;
            }
        }
    }

    /**
     * Parses the given {@code rawNegatable} into a {@link Negatable} of
     * {@link String}.
     *
     * @param rawNegatable
     *            the {@link String} to parse
     * @return a new {@link Negatable} of {@link String}
     */
    public static Property of(String rawNegatable) {
        if (rawNegatable == null || rawNegatable.isEmpty()) {
            return null;
        }
        PropertyParser parser = new PropertyParser(rawNegatable);
        final String key = parser.nextToken();
        if (key == null) {
            return null;
        }
        final String val = parser.hasNextToken() ? parser.nextToken() : null;
        return new Property(key, val);
    }

    private final String key;
    private final String value;

    public Property(String key, String value) {
        super();
        this.key = key;
        this.value = value;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Property other = (Property) obj;
        if (key == null) {
            if (other.key != null)
                return false;
        } else if (!key.equals(other.key))
            return false;
        if (value == null) {
            if (other.value != null)
                return false;
        } else if (!value.equals(other.value))
            return false;
        return true;
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((key == null) ? 0 : key.hashCode());
        result = prime * result + ((value == null) ? 0 : value.hashCode());
        return result;
    }

    /**
     * Returns {@code true} if and only if the given map satisfies the condition
     * given by this {@link Property}.
     *
     * @param map
     *            to check the is-satisfied relationship against
     * @return {@code true} or {@code false}
     */
    public boolean isSatisfiedBy(Map<?, ?> map) {
        if (value != null) {
            return value.equals(map.get(key));
        } else {
            return map.containsKey(key);
        }
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder(key.length() + 1 + (value == null ? 0 : value.length()));
        escape(result, key);
        if (value != null) {
            result.append(EQUALS);
            escape(result, value);
        }
        return result.toString();
    }
}
