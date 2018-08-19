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
package org.srcdeps.core.config.tree.walk;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.srcdeps.core.config.tree.ContainerNode;
import org.srcdeps.core.config.tree.ListNode;
import org.srcdeps.core.config.tree.Node;
import org.srcdeps.core.config.tree.ScalarNode;
import org.srcdeps.core.config.tree.Visitor;

/**
 * A simple configuration tree walker.
 *
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */
public class TreeWalker {
    @SuppressWarnings("unused")
    private static final Logger log = LoggerFactory.getLogger(TreeWalker.class);

    public TreeWalker() {
        super();
    }

    /**
     * Walks a tree starting at the given {@code node} notifying the {@code visitor}.
     *
     * @param node    the node to start at
     * @param visitor the visitor to notify
     */
    @SuppressWarnings("unchecked")
    public void walk(Node node, Visitor visitor) {
        if (node instanceof ScalarNode) {
            visitor.scalar((ScalarNode<Object>) node);
        } else if (node instanceof ListNode) {
            ListNode<Node> list = (ListNode<Node>) node;
            if (visitor.listBegin(list)) {
                for (Node elem : list.getElements()) {
                    walk(elem, visitor);
                }
            }
            visitor.listEnd();
        } else if (node instanceof ContainerNode) {
            ContainerNode<Node> branch = (ContainerNode<Node>) node;
            if (visitor.containerBegin(branch)) {
                for (Node child : branch.getChildren().values()) {
                    walk(child, visitor);
                }
            }
            visitor.containerEnd();
        }
    }
}
