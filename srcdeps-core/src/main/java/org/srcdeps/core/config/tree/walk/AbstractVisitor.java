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
package org.srcdeps.core.config.tree.walk;

import java.util.Stack;

import org.srcdeps.core.config.tree.ContainerNode;
import org.srcdeps.core.config.tree.ListNode;
import org.srcdeps.core.config.tree.Node;
import org.srcdeps.core.config.tree.ScalarNode;
import org.srcdeps.core.config.tree.Visitor;

/**
 * A base for several other {@link Visitor}s that maintains the {@link #stack} of {@link Node}s.
 *
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */
public abstract class AbstractVisitor implements Visitor {

    protected final Stack<Node> stack = new Stack<>();

    /**
     * Pushes the given node to the {@link #stack}.
     *
     * @param node
     *            {@inheritDoc}
     */
    @Override
    public void containerBegin(ContainerNode<? extends Node> node) {
        this.stack.push(node);
    }

    /**
     * Pops from the {@link #stack}.
     */
    @Override
    public void containerEnd() {
        this.stack.pop();
    }

    /**
     * A helper method to tell if some of the present node's ancestors is a {@link ListNode}.
     *
     * @param distance
     *            the numer of steps to take from the last element in the {@link #stack}. {@code 0} means the last
     *            {@link #stack} element.
     * @return {@code true} if the ancestor in the given {@code distance} is a {@link ListNode}
     */
    protected boolean hasListAncestor(int distance) {
        if (distance < 0) {
            throw new IllegalArgumentException(
                    String.format("Cannot lookup items in negative distance [%d] on the stack", distance));
        }
        int lookupIndex = stack.size() - 1 - distance;
        return lookupIndex >= 0 && stack.get(lookupIndex) instanceof ListNode<?>;
    }

    /**
     * Pushes the given node to the {@link #stack}.
     *
     * @param node
     *            {@inheritDoc}
     */
    @Override
    public void listBegin(ListNode<? extends Node> node) {
        this.stack.push(node);
    }

    /**
     * Pops from the {@link #stack}.
     */
    @Override
    public void listEnd() {
        this.stack.pop();
    }

    /** {@inheritDoc} */
    @Override
    public abstract void scalar(ScalarNode<Object> node);

}
