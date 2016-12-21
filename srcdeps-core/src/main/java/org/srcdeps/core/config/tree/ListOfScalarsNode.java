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

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * A list of {@link ScalarNode}s that all have the same value type.
 *
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 *
 * @param <E>
 *            the type of the scalar values stored in this list
 */
public interface ListOfScalarsNode<E> extends ListNode<ScalarNode<E>> {
    /**
     * Creates a new {@link ScalarNode} out of the given scalar {@code value} and adds it to this list.
     *
     * @param value
     *            the scalar value to add
     */
    void add(E value);

    /**
     * For each of the given {@code values}, creates a new {@link ScalarNode} out of the given scalar value and adds it
     * to this list.
     *
     * @param values
     *            the scalar values to add
     */
    void addAll(Collection<E> values);

    /**
     * Gathers the values of each {@link ScalarNode} present in {@link #getElements()} and returns them as a
     * {@link List}
     *
     * @return a new list of scalar values stored in this list
     */
    List<E> asListOfValues();

    /**
     * Gathers the values of each {@link ScalarNode} present in {@link #getElements()} and returns them as a {@link Set}
     *
     * @return a new {@link LinkedHashSet} of scalar values stored in this list
     */
    Set<E> asSetOfValues();

    /**
     * Removes all elements from this list.
     */
    void clear();

    /**
     * @return the type of the value of this list's elements. Basically the value of the {@code <E>} type parameter.
     */
    Class<E> getElementType();
}
