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
package org.srcdeps.core.config.tree.impl;

import java.util.Stack;

import org.srcdeps.core.config.tree.Node;
import org.srcdeps.core.config.tree.ScalarNode;

/**
 * The default implementation of {@link ScalarNode}.
 *
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 *
 * @param <T>
 *            the type of the value stored in this {@link ScalarNode}.
 */
public class DefaultScalarNode<T> implements ScalarNode<T> {
    public static <V> ScalarNode<V> of(V value) {
        @SuppressWarnings("unchecked")
        DefaultScalarNode<V> result = new DefaultScalarNode<>((String) null, (Class<V>) value.getClass());
        result.setValue(value);
        return result;
    }

    private T defaultValue;

    private final String name;
    private final Class<T> type;
    private T value;

    public DefaultScalarNode(String name, Class<T> type) {
        this(name, null, type);
    }

    @SuppressWarnings("unchecked")
    public DefaultScalarNode(String name, T defaultValue) {
        this(name, defaultValue, (Class<T>) defaultValue.getClass());
    }

    public DefaultScalarNode(String name, T defaultValue, Class<T> type) {
        super();
        this.name = name;
        this.defaultValue = defaultValue;
        this.type = type;
    }

    /** {@inheritDoc} */
    @Override
    public void applyDefaultsAndInheritance(Stack<Node> configurationStack) {
        if (value == null) {
            value = defaultValue;
        }
    }

    /** {@inheritDoc} */
    @Override
    public T getDefaultValue() {
        return defaultValue;
    }

    /** {@inheritDoc} */
    @Override
    public String getName() {
        return name;
    }

    /** {@inheritDoc} */
    @Override
    public Class<T> getType() {
        return type;
    }

    /** {@inheritDoc} */
    @Override
    public T getValue() {
        return value;
    }

    /** {@inheritDoc} */
    @Override
    public void init(Node source) {
        if (getClass() != source.getClass()) {
            throw new IllegalArgumentException(
                    String.format("Cannot init [%s] from [%s]", this.getClass(), source.getClass()));
        }
        @SuppressWarnings("unchecked")
        ScalarNode<T> sourceScalar = (DefaultScalarNode<T>) source;
        this.value = sourceScalar.getValue();
    }

    /** {@inheritDoc} */
    @Override
    public boolean isInDefaultState(Stack<Node> configurationStack) {
        return value == defaultValue || (value != null && value.equals(defaultValue));
    }

    /** {@inheritDoc} */
    @Override
    public void setValue(T value) {
        this.value = value;
    }

    /** {@inheritDoc} */
    @Override
    public boolean shouldEscapeName() {
        return false;
    }

    @Override
    public String toString() {
        return new StringBuilder() //
                .append(name) //
                .append(": ") //
                .append(getValue()) //
                .append('\n') //
                .toString();
    }
}
