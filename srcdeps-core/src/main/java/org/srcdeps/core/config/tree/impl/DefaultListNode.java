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
package org.srcdeps.core.config.tree.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import org.srcdeps.core.config.tree.ContainerNode;
import org.srcdeps.core.config.tree.ListNode;
import org.srcdeps.core.config.tree.Node;
import org.srcdeps.core.config.tree.walk.DefaultsAndInheritanceVisitor;

/**
 * The default implementation of {@link ListNode}.
 *
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 *
 * @param <E> the type of this list's elements
 */
public class DefaultListNode<E extends Node> implements ListNode<E> {

    private final List<String> commentBefore = new ArrayList<>();
    protected final List<E> elements;
    private final String name;

    public DefaultListNode(String name) {
        super();
        this.name = name;
        this.elements = new ArrayList<>();
    }

    /**
     * Does nothing in this default implementation because the state of a {@link ContainerNode} is typically given by
     * the states of its child nodes and {@link DefaultsAndInheritanceVisitor} visits the children separately.
     *
     * @param configurationStack the stack of ancestor configuration nodes. Can be queried to inherit values.
     */
    @Override
    public void applyDefaultsAndInheritance(Stack<Node> configurationStack) {
    }

    @Override
    public List<String> getCommentBefore() {
        return commentBefore;
    }

    /** {@inheritDoc} */
    @Override
    public List<E> getElements() {
        return elements;
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
        ListNode<E> sourceList = (DefaultListNode<E>) source;
        this.elements.clear();
        this.elements.addAll(sourceList.getElements());
    }

    /**
     * Note that this implementation calls {@link #isInDefaultState(Stack)} of all children.
     *
     * @param stack the ancestor hierarchy of this {@link Node}
     * @return {@inheritDoc}
     */
    @Override
    public boolean isInDefaultState(Stack<Node> configurationStack) {
        for (Node child : elements) {
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
        return false;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder() //
                .append(name).append(": \n");
        for (Node child : getElements()) {
            sb.append(child.toString()).append('\n');
        }
        return sb.toString();
    }
}
