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

import java.util.Properties;
import java.util.Stack;
import java.util.StringTokenizer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.srcdeps.core.config.Configuration;
import org.srcdeps.core.config.scalar.Scalars;
import org.srcdeps.core.config.tree.ContainerNode;
import org.srcdeps.core.config.tree.ListNode;
import org.srcdeps.core.config.tree.ListOfScalarsNode;
import org.srcdeps.core.config.tree.Node;
import org.srcdeps.core.config.tree.ScalarDeserializer;
import org.srcdeps.core.config.tree.ScalarNode;
import org.srcdeps.core.config.tree.Visitor;
import org.srcdeps.core.config.tree.impl.DefaultScalarNode;

/**
 * A {@link Visitor} to override values in a tree by values coming from some higher-ranked sources, such as command
 * line. The overriding values are passed in as {@link Properties}. These {@link Properties} are queried during the tree
 * traversal and if a property whose name matches the path to a tree node has a non-null value that value is used to
 * override the existing value of the node.
 *
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */
public class OverrideVisitor extends AbstractVisitor {
    private static class IndexedSegment extends StringSegment {
        private int index = 0;

        private IndexedSegment(String name, boolean escape) {
            super(name, escape);
        }

        @Override
        public void appendTo(StringBuilder out) {
            super.appendTo(out);
            out.append('[').append(index).append(']');
        }

        public void next() {
            this.index++;
        }
    }

    private static class StringSegment {
        private final boolean escaped;
        private final String segment;

        private StringSegment(String segment, boolean escaped) {
            super();
            this.escaped = escaped;
            this.segment = segment;
        }

        public void appendTo(StringBuilder out) {
            if (escaped) {
                out.append('[').append(segment).append(']');
            } else {
                if (out.length() > 0) {
                    out.append('.');
                }
                out.append(segment);
            }
        }
    }

    private static final Logger log = LoggerFactory.getLogger(OverrideVisitor.class);

    private final Properties overrideSource;
    /** A stack of field names, rooted by {@code "srcdeps"} */
    private Stack<StringSegment> path = new Stack<>();

    public OverrideVisitor(Properties overrideSource) {
        super();
        this.overrideSource = overrideSource;
    }

    @Override
    public boolean containerBegin(ContainerNode<? extends Node> node) {
        if (hasListAncestor(0)) {
            /* do nothing this is an element of a list */
        } else {
            path.push(new StringSegment(node.getName(), node.shouldEscapeName()));
        }
        super.containerBegin(node);
        return true;
    }

    @Override
    public void containerEnd() {
        if (stack.size() == 1) {
            final Configuration.Builder configBuilder = (Configuration.Builder) stack.peek();
            @SuppressWarnings("unchecked")
            final ListOfScalarsNode<String> forwardPropertyNamesNode = (ListOfScalarsNode<String>) configBuilder.getChildren().get(Configuration.getForwardPropertiesAttribute());
            forwardPropertyNamesNode.asListOfValues();
            for (ScalarNode<String> node : forwardPropertyNamesNode.getElements()) {
                final String propName = node.getValue();
                if (propName.endsWith("*")) {
                    /* prefix */
                    String prefix = propName.substring(propName.length() - 1);
                    for (Object key : overrideSource.keySet()) {
                        if (key instanceof String && ((String) key).startsWith(prefix)) {
                            String value = overrideSource.getProperty((String) key);
                            if (value != null) {
                                configBuilder.forwardPropertyValue(propName, value);
                            }
                        }
                    }
                } else {
                    String value = overrideSource.getProperty(propName);
                    if (value != null) {
                        configBuilder.forwardPropertyValue(propName, value);
                    }
                }
            }
        }
        super.containerEnd();
        StringSegment segment = null;
        if (!path.isEmpty() && (segment = path.peek()) instanceof IndexedSegment) {
            ((IndexedSegment) segment).next();
        } else {
            path.pop();
        }
    }


    private void handleListOfScalars(ListOfScalarsNode<Object> list, ScalarDeserializer handler) {
        path.push(new StringSegment(list.getName(), list.shouldEscapeName()));
        String joinedPath = joinPath();
        path.pop();
        String stringList = overrideSource.getProperty(joinedPath);
        if (stringList != null) {
            log.info("srcdeps: Configuration override [{}] = [{}].", joinedPath, stringList);
            replaceElements(stringList, handler, list);
        }

        /* replace individual elements */
        int oldSize = list.getElements().size();
        for (int i = 0; i < oldSize; i++) {
            final String key = joinedPath + "[" + i + "]";
            final String val = overrideSource.getProperty(key);
            if (val != null) {
                log.info("srcdeps: Configuration override [{}] = [{}].", key, val);
                list.getElements().get(i).setValue(handler.deserialize(val));
            }
        }

        /* prepend */
        for (int i = -1;; i--) {
            final String key = joinedPath + "[" + i + "]";
            final String val = overrideSource.getProperty(key);
            if (val != null) {
                log.info("srcdeps: Configuration override [{}] = [{}].", key, val);
                list.getElements().add(0, DefaultScalarNode.of(handler.deserialize(val)));
            } else {
                break;
            }
        }

        /* append */
        for (int i = oldSize;; i++) {
            final String key = joinedPath + "[" + i + "]";
            final String val = overrideSource.getProperty(key);
            if (val != null) {
                log.info("srcdeps: Configuration override [{}] = [{}].", key, val);
                list.add(handler.deserialize(val));
            } else {
                break;
            }
        }
    }

    /**
     * @return {@link #path} elements joined by {@code '.'}
     */
    private String joinPath() {
        StringBuilder result = new StringBuilder();
        for (StringSegment segment : path) {
            segment.appendTo(result);
        }
        return result.toString();
    }

    @Override
    public boolean listBegin(ListNode<? extends Node> node) {
        super.listBegin(node);
        if (node instanceof ListOfScalarsNode<?>) {
            @SuppressWarnings("unchecked")
            ListOfScalarsNode<Object> list = (ListOfScalarsNode<Object>) node;
            ScalarDeserializer deserializer = Scalars.getDeserializer(list.getElementType());
            handleListOfScalars(list, deserializer);
        }
        path.push(new IndexedSegment(node.getName(), node.shouldEscapeName()));
        return true;
    }

    @Override
    public void listEnd() {
        path.pop();
        super.listEnd();
    }

    /**
     * First clears the given {@code list} and then add elements to it that it parses out of the given {@code source}.
     * {@code source} is supposed to be a comma-delimited list of primitive values.
     *
     * @param source  the string to parse
     * @param handler the deserializer to transform the values from string to the appropriate primitive type
     * @param list    the destination
     */
    private void replaceElements(final String source, final ScalarDeserializer handler,
            ListOfScalarsNode<Object> list) {
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
     * @param field the field to traverse
     */
    @Override
    public void scalar(ScalarNode<Object> node) {
        if (hasListAncestor(0)) {
            /* do nothing */
        } else {
            path.push(new StringSegment(node.getName(), node.shouldEscapeName()));
            final String joinedPath = joinPath();
            final String newValue = overrideSource.getProperty(joinedPath);
            if (newValue != null) {
                if (newValue.isEmpty() && node.getType().equals(Boolean.class)) {
                    /*
                     * -DmyProp set on commandline results in System.getProperty("myProp") returning an empty string We
                     * want to interpret this case as true
                     */
                    log.info("srcdeps: Configuration override [{}] = [<empty> -> true].", joinedPath, newValue);
                    node.setValue(Boolean.TRUE);
                } else {
                    ScalarDeserializer deserializer = Scalars.getDeserializer(node.getType());
                    log.info("srcdeps: Configuration override [{}] = [{}].", joinedPath, newValue);
                    node.setValue(deserializer.deserialize(newValue));
                }
            }
            path.pop();
        }
    }

}
