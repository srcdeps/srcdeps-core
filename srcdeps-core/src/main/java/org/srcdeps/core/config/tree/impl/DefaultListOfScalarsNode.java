/**
 * Copyright 2015-2017 Maven Source Dependencies
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
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.srcdeps.core.config.tree.ListOfScalarsNode;
import org.srcdeps.core.config.tree.ScalarNode;

/**
 * The default implementation of {@link ListOfScalarsNode}.
 *
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 *
 * @param <E>
 *            the type of the scalar values stored in this list
 */
public class DefaultListOfScalarsNode<E> extends DefaultListNode<ScalarNode<E>> implements ListOfScalarsNode<E> {
    private final Class<E> elementType;

    public DefaultListOfScalarsNode(String name, Class<E> elementType) {
        super(name);
        this.elementType = elementType;
    }

    /** {@inheritDoc} */
    @Override
    public void add(E value) {
        elements.add(DefaultScalarNode.of(value));
    }

    /** {@inheritDoc} */
    @Override
    public void addAll(Collection<E> values) {
        for (E val : values) {
            elements.add(DefaultScalarNode.of(val));
        }
    }

    /** {@inheritDoc} */
    @Override
    public List<E> asListOfValues() {
        List<E> result = new ArrayList<>(elements.size());
        for (ScalarNode<E> elem : elements) {
            result.add(elem.getValue());
        }
        return Collections.unmodifiableList(result);
    }

    /** {@inheritDoc} */
    @Override
    public Set<E> asSetOfValues() {
        Set<E> result = new LinkedHashSet<>(elements.size());
        for (ScalarNode<E> elem : elements) {
            result.add(elem.getValue());
        }
        return Collections.unmodifiableSet(result);
    }

    /** {@inheritDoc} */
    @Override
    public void clear() {
        elements.clear();
    }

    /** {@inheritDoc} */
    @Override
    public Class<E> getElementType() {
        return elementType;
    }

}
