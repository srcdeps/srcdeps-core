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

/**
 * To deserialize from String to an object of another type such as int, Integer, boolean, Boolean, etc.
 */
public interface ScalarDeserializer {
    /**
     * Deserialize the given {@link String} value to an object of another type such as int, Integer, boolean, Boolean,
     * etc.
     *
     * @param value
     *            the {@link String} value to deserialize.
     * @return the deserialized value
     */
    Object deserialize(String value);
}