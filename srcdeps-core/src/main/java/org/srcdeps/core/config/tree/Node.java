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

import java.util.Stack;

import org.srcdeps.core.config.tree.walk.DefaultsAndInheritanceVisitor;
import org.w3c.dom.NodeList;

/**
 * The base interface for configuration tree nodes.
 *
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */
public interface Node {
    /**
     * Apply the defaults or apply some kind of inheritance in case the values of some attributes were not set
     * explicitly. See also {@link DefaultsAndInheritanceVisitor}.
     *
     * @param configurationStack
     *            the stack of ancestor configuration nodes. Can be queried to inherit values.
     */
    void applyDefaultsAndInheritance(Stack<Node> configurationStack);

    /**
     * @return the name of the present node that is supposed to be unique within the parent node. Can be {@code null} in
     *         case this {@link Node} is an element of a {@link NodeList}.
     */
    String getName();

    /**
     * Fully reset the state of this {@link Node} using the given {@code source} {@link Node}.
     *
     * @param source
     *            the {@link Node} to take the values from
     */
    void init(Node source);

    /**
     * @param stack
     *            the ancestor hierarchy of this {@link Node}
     * @return {@code true} if this {@link Node}'s internal state has not been set yet or if the state is in the
     *         instance or implementation specific default state. Otherwise returns {@code false}
     */
    boolean isInDefaultState(Stack<Node> stack);

    /**
     * @return {@code true} if the name of the node will commonly contain special characters, esp. periods. Otherwise
     *         returns {@code false}
     */
    boolean shouldEscapeName();
}
