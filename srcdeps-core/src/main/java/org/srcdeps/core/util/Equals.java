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
package org.srcdeps.core.util;

import java.util.regex.Pattern;

/**
 * A Java 8's {@code BiPredicate}-like interface for determining equality between objects. Introduced to be able to
 * compare also types that do not override {@link Object#equals(Object)}, most notably {@link Pattern}.
 *
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 *
 * @param <T>
 *            the type to compare
 */
public interface Equals<T> {
    class EqualsImplementations {
        private static final Equals<Object> DEFAULT = new Equals<Object>() {

            @Override
            public boolean test(Object value1, Object value2) {
                return value1 == value2 || (value1 != null && value1.equals(value2));
            }

        };
        private static final Equals<Pattern> PATTERN = new Equals<Pattern>() {

            @Override
            public boolean test(Pattern value1, Pattern value2) {
                return value1 == value2
                        || (value1 != null && value1.pattern().equals(value2 == null ? null : value2.pattern()));
            }

        };

        /**
         * @return a default {@link Equals} implementation that works for any type {@code T} which overrides
         *         {@link Object#equals(Object)}.
         */
        @SuppressWarnings("unchecked")
        public static <T> Equals<T> equals() {
            return (Equals<T>) DEFAULT;
        }

        /**
         * @return an {@link Equals} implementation that compares based on {@link Pattern#pattern()}.
         */
        public static Equals<Pattern> equalsPattern() {
            return PATTERN;
        }
    }

    /**
     * As long as type {@code T} overrides {@link Object#equals(Object)}, must be equivalent to {@code value1 == value2
     *                  || (value1 != null && value1.pattern().equals(value2 == null ? null : value2.pattern()))}
     * otherwise the implementations have to come up with some alternative which obey the contact of
     * {@link Object#equals(Object)}.
     *
     * @param value1
     *            the first value to compare
     * @param value2
     *            the second value to compare
     * @return {@code true} of {@code value1} and {@code value2} are equal
     */
    boolean test(T value1, T value2);
}
