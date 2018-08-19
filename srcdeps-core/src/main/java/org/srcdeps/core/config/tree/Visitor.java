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
package org.srcdeps.core.config.tree;

import org.srcdeps.core.config.Configuration.Builder;
import org.srcdeps.core.config.tree.walk.TreeWalker;

/**
 * A visitor for a traversal of a configuration tree. See {@link Builder#accept(Visitor)} and
 * {@link TreeWalker#walk(Node, Visitor)}.
 *
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */
public interface Visitor {

    /**
     * Check whether the children of the given {@link ContainerNode} should be visited.
     *
     * @param node the current {@link ContainerNode}
     * @return {@code true} if the calling walker should traverse the children of the given {@code node}; {@code false}
     *         otherwise
     */
    boolean containerBegin(ContainerNode<? extends Node> node);

    /**
     * Called just after {@link #containerBegin(ContainerNode)} if it returned {@code true} or after the children of a
     * {@link ContainerNode} were visited.
     */
    void containerEnd();

    /**
     * Check whether the elements of the given {@link ListNode} should be visited.
     *
     * @param node the current {@link ListNode}
     * @return {@code true} if the calling walker should traverse the elements of the given {@code node}; {@code false}
     *         otherwise
     */
    boolean listBegin(ListNode<? extends Node> node);

    /**
     * Called just after {@link #containerBegin(ContainerNode)} if it returned {@code true} or after the children of a
     * {@link ListNode} were visited.
     */
    void listEnd();

    /**
     * Visit the given {@link ScalarNode}.
     *
     * @param node the {@link ScalarNode} to visit
     */
    void scalar(ScalarNode<Object> node);

}
