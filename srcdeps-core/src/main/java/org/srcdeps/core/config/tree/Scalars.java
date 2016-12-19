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
package org.srcdeps.core.config.tree;

import java.lang.reflect.Type;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.srcdeps.core.BuildRequest.Verbosity;

/**
 * Hosts scalar types related helper methods.
 *
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */
public final class Scalars {

    /**
     * {@link Map} from types we consider primitive to their respective {@link ScalarDeserializer}s.
     */
    private static final Map<Type, ScalarDeserializer> scalarTypes;

    static {

        /* Put a ScalarDeserializer for every scalar type to scalarTypes map */
        Map<Type, ScalarDeserializer> primitives = new HashMap<>();
        primitives.put(String.class, new ScalarDeserializer() {
            @Override
            public Object deserialize(String value) {
                return value;
            }
        });
        primitives.put(Boolean.class, new ScalarDeserializer() {
            @Override
            public Object deserialize(String value) {
                return Boolean.valueOf(value);
            }
        });
        primitives.put(boolean.class, new ScalarDeserializer() {
            @Override
            public Object deserialize(String value) {
                return Boolean.parseBoolean(value);
            }
        });
        primitives.put(Integer.class, new ScalarDeserializer() {
            @Override
            public Object deserialize(String value) {
                return Integer.valueOf(value);
            }
        });
        primitives.put(int.class, new ScalarDeserializer() {
            @Override
            public Object deserialize(String value) {
                return Integer.parseInt(value);
            }
        });
        primitives.put(Long.class, new ScalarDeserializer() {
            @Override
            public Object deserialize(String value) {
                return Long.valueOf(value);
            }
        });
        primitives.put(long.class, new ScalarDeserializer() {
            @Override
            public Object deserialize(String value) {
                return Long.parseLong(value);
            }
        });
        primitives.put(Double.class, new ScalarDeserializer() {
            @Override
            public Object deserialize(String value) {
                return Double.valueOf(value);
            }
        });
        primitives.put(double.class, new ScalarDeserializer() {
            @Override
            public Object deserialize(String value) {
                return Double.parseDouble(value);
            }
        });
        primitives.put(Float.class, new ScalarDeserializer() {
            @Override
            public Object deserialize(String value) {
                return Float.valueOf(value);
            }
        });
        primitives.put(float.class, new ScalarDeserializer() {
            @Override
            public Object deserialize(String value) {
                return Float.parseFloat(value);
            }
        });
        primitives.put(Short.class, new ScalarDeserializer() {
            @Override
            public Object deserialize(String value) {
                return Short.valueOf(value);
            }
        });
        primitives.put(short.class, new ScalarDeserializer() {
            @Override
            public Object deserialize(String value) {
                return Short.parseShort(value);
            }
        });
        primitives.put(Character.class, new ScalarDeserializer() {
            @Override
            public Object deserialize(String value) {
                if (value.length() != 1) {
                    throw new RuntimeException(
                            String.format("Cannot transform String [%s] of length %d to char", value, value.length()));
                }
                return Character.valueOf(value.charAt(0));
            }
        });
        primitives.put(char.class, new ScalarDeserializer() {
            @Override
            public Object deserialize(String value) {
                if (value.length() != 1) {
                    throw new RuntimeException(
                            String.format("Cannot transform String [%s] of length %d to char", value, value.length()));
                }
                return value.charAt(0);
            }
        });

        primitives.put(Verbosity.class, new ScalarDeserializer() {
            @Override
            public Object deserialize(String value) {
                return Verbosity.fastValueOf(value);
            }
        });
        primitives.put(Path.class, new ScalarDeserializer() {
            @Override
            public Object deserialize(String value) {
                return Paths.get(value);
            }
        });
        scalarTypes = Collections.unmodifiableMap(primitives);

    }

    /**
     * @param type
     *            the type to get a {@link ScalarDeserializer} for
     * @return a {@link ScalarDeserializer} for the given {@code type} or null if the given {@code type} is not a scalar
     *         type
     */
    public static ScalarDeserializer getDeserializer(Class<?> type) {
        return scalarTypes.get(type);
    }

    /**
     * A shorthand for {@code getDeserializer(type) != null}.
     *
     * @param type
     *            the type to get a {@link ScalarDeserializer} for
     * @return {@code true} if the given {@code type} is not a scalar type or {@code false} otherwise
     */
    public static boolean isScalarType(Class<?> type) {
        return scalarTypes.containsKey(type);
    }

    private Scalars() {
        super();
    }

}
