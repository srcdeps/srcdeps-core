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

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Stack;

import org.srcdeps.core.config.tree.ContainerNode;
import org.srcdeps.core.config.tree.Node;
import org.srcdeps.core.config.tree.walk.DefaultsAndInheritanceVisitor;

/**
 * The default implementation of {@link ContainerNode}.
 *
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 *
 * @param <C>
 *            the type of child nodes
 */
public class DefaultContainerNode<C extends Node> implements ContainerNode<C> {

    protected final Map<String, C> children = new LinkedHashMap<>();

    protected String name;

    private boolean shouldEscapeName;

    public DefaultContainerNode(String name) {
        this(name, false);
    }

    public DefaultContainerNode(String name, boolean shouldEscapeName) {
        super();
        this.name = name;
        this.shouldEscapeName = shouldEscapeName;
    }

    public void addChild(C child) {
        this.children.put(child.getName(), child);
    }

    public void addChildren(@SuppressWarnings("unchecked") C... children) {
        for (C child : children) {
            this.children.put(child.getName(), child);
        }
    }

    /**
     * Does nothing in this default implementation because the state of a {@link ContainerNode} is typically given by
     * the states of its child nodes and {@link DefaultsAndInheritanceVisitor} visits the children separately.
     *
     * @param configurationStack
     *            the stack of ancestor configuration nodes. Can be queried to inherit values.
     */
    @Override
    public void applyDefaultsAndInheritance(Stack<Node> configurationStack) {
    }

    /** {@inheritDoc} */
    @Override
    public Map<String, C> getChildren() {
        return children;
    }

    /** {@inheritDoc} */
    @Override
    public String getName() {
        return name;
    }

    /** {@inheritDoc} */
    @Override
    public void init(Node source) {
        if (getClass() != source.getClass()) {
            throw new IllegalArgumentException(
                    String.format("Cannot init [%s] from [%s]", this.getClass(), source.getClass()));
        }
        @SuppressWarnings("unchecked")
        Iterator<C> sourceChildren = ((DefaultContainerNode<C>) source).getChildren().values().iterator();
        Iterator<C> targetChildren = this.getChildren().values().iterator();
        while (sourceChildren.hasNext()) {
            targetChildren.next().init(sourceChildren.next());
        }
    }

    /**
     * Note that this implementation calls {@link #isInDefaultState(Stack)} of all children.
     *
     * @param stack
     *            the ancestor hierarchy of this {@link Node}
     * @return {@inheritDoc}
     */
    @Override
    public boolean isInDefaultState(Stack<Node> configurationStack) {
        for (Node child : children.values()) {
            configurationStack.push(child);
            final boolean result = child.isInDefaultState(configurationStack);
            configurationStack.pop();
            if (!result) {
                return false;
            }
        }
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public boolean shouldEscapeName() {
        return shouldEscapeName;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder() //
                .append(name).append(": {\n");
        for (Node child : getChildren().values()) {
            sb.append(child.toString()).append('\n');
        }
        return sb.toString();
    }

}