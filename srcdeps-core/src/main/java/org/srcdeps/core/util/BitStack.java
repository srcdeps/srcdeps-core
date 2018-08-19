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
package org.srcdeps.core.util;

import java.util.BitSet;
import java.util.EmptyStackException;

/**
 * A stack of boolean values backed by a {@link BitSet}.
 *
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */
public class BitStack {
    private final BitSet bits = new BitSet();
    private int size = 0;

    /**
     * Pushes an item onto the top of this stack.
     *
     * @param item the item to push
     * @return the {@code item} argument.
     */
    public boolean push(boolean item) {
        bits.set(size++, item);
        return item;
    }

    /**
     * Removes the object at the top of this stack and returns it.
     *
     * @return The object at the top of this stack
     * @throws EmptyStackException if this stack is empty.
     */
    public synchronized boolean pop() {
        boolean result = peek();
        size--;
        return result;
    }

    /**
     * Returns the object at the top of this stack without removing it from the stack.
     *
     * @return the object at the top of this stack
     * @throws EmptyStackException if this stack is empty.
     */
    public synchronized boolean peek() {
        if (size == 0)
            throw new EmptyStackException();
        return bits.get(size - 1);
    }

    /**
     * @return {@code true} if and only if this stack contains no items; {@code false} otherwise.
     */
    public boolean empty() {
        return size == 0;
    }

}
