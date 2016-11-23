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

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Stack;
import java.util.StringTokenizer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.srcdeps.core.BuildRequest.Verbosity;

/**
 * A {@link ConfigurationNodeVisitor} to override values in a {@link Configuration.Builder} by values coming from some
 * higher-ranked sources, such as command line.
 *
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */
public class ConfigurationOverrideVisitor implements ConfigurationNodeVisitor {

    /**
     * To deserialize from String to an object of another type such as int, Integer, boolean, Boolean, etc.
     */
    private interface Deserializer {
        /**
         * Deserialize the given {@link String} value to an object of another type such as int, Integer, boolean,
         * Boolean, etc.
         *
         * @param value
         *            the {@link String} value to deserialize.
         * @return the deserialized value
         */
        Object deserialize(String value);
    }

    private static final Logger log = LoggerFactory.getLogger(ConfigurationOverrideVisitor.class);

    /**
     * {@link Map} from types we consider primitive to their respective {@link Deserializer}s.
     */
    private static final Map<Class<?>, Deserializer> primitiveTypes;

    static {

        /* Put a Deserializer for every primitive type to primitiveTypes map */
        Map<Class<?>, Deserializer> primitives = new HashMap<>();
        primitives.put(String.class, new Deserializer() {
            @Override
            public Object deserialize(String value) {
                return value;
            }
        });
        primitives.put(Boolean.class, new Deserializer() {
            @Override
            public Object deserialize(String value) {
                return Boolean.valueOf(value);
            }
        });
        primitives.put(boolean.class, new Deserializer() {
            @Override
            public Object deserialize(String value) {
                return Boolean.parseBoolean(value);
            }
        });
        primitives.put(Integer.class, new Deserializer() {
            @Override
            public Object deserialize(String value) {
                return Integer.valueOf(value);
            }
        });
        primitives.put(int.class, new Deserializer() {
            @Override
            public Object deserialize(String value) {
                return Integer.parseInt(value);
            }
        });
        primitives.put(Long.class, new Deserializer() {
            @Override
            public Object deserialize(String value) {
                return Long.valueOf(value);
            }
        });
        primitives.put(long.class, new Deserializer() {
            @Override
            public Object deserialize(String value) {
                return Long.parseLong(value);
            }
        });
        primitives.put(Double.class, new Deserializer() {
            @Override
            public Object deserialize(String value) {
                return Double.valueOf(value);
            }
        });
        primitives.put(double.class, new Deserializer() {
            @Override
            public Object deserialize(String value) {
                return Double.parseDouble(value);
            }
        });
        primitives.put(Float.class, new Deserializer() {
            @Override
            public Object deserialize(String value) {
                return Float.valueOf(value);
            }
        });
        primitives.put(float.class, new Deserializer() {
            @Override
            public Object deserialize(String value) {
                return Float.parseFloat(value);
            }
        });
        primitives.put(Short.class, new Deserializer() {
            @Override
            public Object deserialize(String value) {
                return Short.valueOf(value);
            }
        });
        primitives.put(short.class, new Deserializer() {
            @Override
            public Object deserialize(String value) {
                return Short.parseShort(value);
            }
        });
        primitives.put(Character.class, new Deserializer() {
            @Override
            public Object deserialize(String value) {
                if (value.length() != 1) {
                    throw new RuntimeException(
                            String.format("Cannot transform String [%s] of length %d to char", value, value.length()));
                }
                return Character.valueOf(value.charAt(0));
            }
        });
        primitives.put(char.class, new Deserializer() {
            @Override
            public Object deserialize(String value) {
                if (value.length() != 1) {
                    throw new RuntimeException(
                            String.format("Cannot transform String [%s] of length %d to char", value, value.length()));
                }
                return value.charAt(0);
            }
        });

        primitives.put(Verbosity.class, new Deserializer() {
            @Override
            public Object deserialize(String value) {
                return Verbosity.fastValueOf(value);
            }
        });
        primitives.put(Path.class, new Deserializer() {
            @Override
            public Object deserialize(String value) {
                return Paths.get(value);
            }
        });
        primitiveTypes = Collections.unmodifiableMap(primitives);

    }

    private static Object getSilently(Object node, Field field) {
        try {
            if (!field.isAccessible()) {
                field.setAccessible(true);
            }
            return field.get(node);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private static void setSilently(Object node, Field field, Object value) {
        try {
            if (!field.isAccessible()) {
                field.setAccessible(true);
            }
            field.set(node, value);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private final Properties overrideSource;

    /** A stack of field names, rooted by {@code "srcdeps"} */
    private Stack<String> path = new Stack<>();

    public ConfigurationOverrideVisitor(Properties overrideSource) {
        super();
        this.overrideSource = overrideSource;
        path.push("srcdeps");
    }

    private void handleListOfPrimitives(Object node, List<Object> list, Deserializer handler, String joinedPath) {
        String stringList = overrideSource.getProperty(joinedPath);
        if (stringList != null) {
            log.info("Srcdeps configuration override [{}] = [{}].", joinedPath, stringList);
            replaceElements(stringList, handler, list);
        }

        /* replace individual elements */
        int oldSize = list.size();
        for (int i = 0; i < oldSize; i++) {
            final String key = joinedPath + "[" + i + "]";
            final String val = overrideSource.getProperty(key);
            if (val != null) {
                log.info("Srcdeps configuration override [{}] = [{}].", key, val);
                list.set(i, handler.deserialize(val));
            }
        }

        /* prepend */
        for (int i = -1;; i--) {
            final String key = joinedPath + "[" + i + "]";
            final String val = overrideSource.getProperty(key);
            if (val != null) {
                log.info("Srcdeps configuration override [{}] = [{}].", key, val);
                list.add(0, handler.deserialize(val));
            } else {
                break;
            }
        }

        /* append */
        for (int i = oldSize;; i++) {
            final String key = joinedPath + "[" + i + "]";
            final String val = overrideSource.getProperty(key);
            if (val != null) {
                log.info("Srcdeps configuration override [{}] = [{}].", key, val);
                list.add(handler.deserialize(val));
            } else {
                break;
            }
        }
    }

    private void handleListOfTraversables(Object node, String fieldName, List<TraversableConfigurationNode<?>> list)
            throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        /* replace individual elements */
        int oldSize = list.size();
        for (int i = 0; i < oldSize; i++) {
            TraversableConfigurationNode<?> subbuilder = list.get(i);
            if (subbuilder instanceof IdProvider) {
                path.set(path.size() - 1, fieldName + "[" + ((IdProvider) subbuilder).getId() + "]");
            } else {
                path.set(path.size() - 1, fieldName + "[" + i + "]");
            }
            subbuilder.accept(this);
        }
        path.set(path.size() - 1, fieldName);
    }

    /**
     * @return {@link #path} elements joined by {@code '.'}
     */
    private String joinPath() {
        StringBuilder result = new StringBuilder();
        for (String segment : path) {
            if (result.length() != 0) {
                result.append('.');
            }
            result.append(segment);
        }
        return result.toString();
    }

    /**
     * First clears the given {@code list} and then add elements to it that it parses out of the given {@code source}.
     * {@code source} is supposed to be a comma-delimited list of primitive values.
     *
     * @param source
     *            the string to parse
     * @param handler
     *            the deserializer to transform the values from string to the appropriate primitive type
     * @param list
     *            the destination
     */
    private void replaceElements(final String source, final Deserializer handler, List<Object> list) {
        list.clear();
        StringTokenizer st = new StringTokenizer(source, ",");
        while (st.hasMoreTokens()) {
            String token = st.nextToken().trim();
            Object val = handler.deserialize(token);
            list.add(val);
        }
    }

    /**
     * Visits the given field of the given node object. Looks up the given field by its string {@link #path} in
     * {@link #overrideSource} and eventually overrides the value of the field by the value foung in
     * {@link #overrideSource}.
     *
     * @param node
     *            the object the given {@code field} belongs to
     * @param field
     *            the field to traverse
     */
    @Override
    public void visit(Object node, Field field) {
        path.push(field.getName());
        final Type fieldType = field.getGenericType();
        final String joinedPath = joinPath();
        final Object oldValue = getSilently(node, field);
        try {
            Deserializer handler;
            if ((handler = primitiveTypes.get(fieldType)) != null) {
                /* primitive type */
                final String val = overrideSource.getProperty(joinedPath);
                if (val != null) {
                    log.info("Srcdeps configuration override [{}] = [{}].", joinedPath, val);
                    setSilently(node, field, handler.deserialize(val));
                }
            } else if (oldValue instanceof TraversableConfigurationNode<?>) {
                ((TraversableConfigurationNode<?>) oldValue).accept(this);
            } else if (fieldType instanceof ParameterizedType) {
                ParameterizedType parameterizedFieldType = (ParameterizedType) fieldType;
                Type[] typeArgs = parameterizedFieldType.getActualTypeArguments();
                Type rawType = parameterizedFieldType.getRawType();
                if (rawType instanceof Class && List.class.isAssignableFrom((Class<?>) rawType)) {
                    if ((handler = primitiveTypes.get(typeArgs[0])) != null) {
                        /* list of primitive types */
                        handleListOfPrimitives(node, (List<Object>) oldValue, handler, joinedPath);
                    } else if (typeArgs[0] instanceof Class
                            && TraversableConfigurationNode.class.isAssignableFrom((Class<?>) typeArgs[0])) {
                        /* list of supported subbuilders */
                        handleListOfTraversables(node, field.getName(),
                                (List<TraversableConfigurationNode<?>>) oldValue);
                    } else {
                        throw new IllegalStateException(String.format("Srcdeps unable to handle field [%s] of [%s]",
                                field, node.getClass().getName()));
                    }
                }
            } else {
                throw new IllegalStateException(
                        String.format("Srcdeps unable to handle field [%s] of [%s]", field, node.getClass().getName()));
            }
        } catch (IllegalAccessException | InvocationTargetException | IllegalArgumentException e) {
            throw new RuntimeException(e);
        }
        path.pop();
    }

}
