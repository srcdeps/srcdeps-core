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

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * A scalar value that can be negated by placing {@code !} at the beginning.
 *
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 *
 * @param <T>
 *            the type of the value
 */
public abstract class Negatable<T> {

    public static class NegatableProperty extends Negatable<Property> {

        public static List<NegatableProperty> listOf(String... rawNegatable) {
            ArrayList<NegatableProperty> result = new ArrayList<>(rawNegatable.length);
            for (String item : rawNegatable) {
                result.add(NegatableProperty.of(item));
            }
            return Collections.unmodifiableList(result);
        }

        public static Set<NegatableProperty> setOf(String... rawNegatable) {
            Set<NegatableProperty> result = new LinkedHashSet<>(rawNegatable.length);
            for (String item : rawNegatable) {
                result.add(NegatableProperty.of(item));
            }
            return Collections.unmodifiableSet(result);
        }

        public static NegatableProperty of(String rawNegatable) {
            NegatableString s = NegatableString.of(rawNegatable);
            if (s == null) {
                return null;
            }
            final Property prop = Property.of(s.getValue());
            if (prop == null) {
                return null;
            }
            return new NegatableProperty(s.isNegated(), prop);
        }

        NegatableProperty(boolean negated, String key, String value) {
            super(negated, new Property(key, value));
        }

        NegatableProperty(boolean negated, Property value) {
            super(negated, value);
        }

    }

    public static class NegatableString extends Negatable<String> {

        public static List<NegatableString> listOf(String... rawNegatable) {
            ArrayList<NegatableString> result = new ArrayList<>(rawNegatable.length);
            for (String item : rawNegatable) {
                result.add(NegatableString.of(item));
            }
            return Collections.unmodifiableList(result);
        }

        public static Set<NegatableString> setOf(String... rawNegatable) {
            Set<NegatableString> result = new LinkedHashSet<>(rawNegatable.length);
            for (String item : rawNegatable) {
                result.add(NegatableString.of(item));
            }
            return Collections.unmodifiableSet(result);
        }

        public static NegatableString of(String rawNegatable) {
            if (rawNegatable == null || rawNegatable.isEmpty()) {
                return null;
            }
            boolean negated = rawNegatable.charAt(0) == '!';
            final int len = rawNegatable.length();
            if (negated && len == 1) {
                /* negated null is null */
                return null;
            }
            final String value = negated
                    || (len >= 2 && rawNegatable.charAt(0) == '\\' && rawNegatable.charAt(1) == '!')
                            ? rawNegatable.substring(1) : rawNegatable;
            return new NegatableString(negated, value);
        }

        public NegatableString(boolean negated, String value) {
            super(negated, value);
        }

    }

    /**
     * If {@code true} the {@link #value} has to be interpreted as negated;
     * {@code false} otherwise
     */
    private final boolean negated;

    /** The value */
    private final T value;

    Negatable(boolean negated, T value) {
        super();
        this.negated = negated;
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
        @SuppressWarnings("rawtypes")
        Negatable other = (Negatable) obj;
        if (negated != other.negated)
            return false;
        if (value == null) {
            if (other.value != null)
                return false;
        } else if (!value.equals(other.value))
            return false;
        return true;
    }

    /**
     * @return the {@link #value}
     */
    public T getValue() {
        return value;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (negated ? 1231 : 1237);
        result = prime * result + ((value == null) ? 0 : value.hashCode());
        return result;
    }

    /**
     * @return {@code true} if the {@link #value} has to be interpreted as
     *         negated; {@code false} otherwise
     */
    public boolean isNegated() {
        return negated;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        final String val = value.toString();
        return negated ? "!" + val : (val.startsWith("!") ? "\\" + val : val);
    }

}
