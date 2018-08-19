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

import org.srcdeps.core.config.scalar.Scalars;

/**
 * A {@link Node} that stores a scalar value.
 *
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 *
 * @param <T> the type of the value stored in this {@link ScalarNode}.
 *
 * @see Scalars
 */
public interface ScalarNode<T> extends Node {
    /**
     * @return the default value to be used if the value of this {@link ScalarNode} is not set explicitly
     */
    T getDefaultValue();

    /**
     * @return the type of the value
     */
    Class<T> getType();

    /**
     * @return the value stored in this {@link ScalarNode}
     */
    T getValue();

    /**
     * Sets the value stored in this {@link ScalarNode}.
     *
     * @param value the value to set
     */
    void setValue(T value);
}
