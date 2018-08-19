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
package org.srcdeps.core.config;

import java.io.Reader;

/**
 * An interface for loading a {@link Configuration}.
 *
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */
public interface ConfigurationIo {
    /**
     * Read the {@link Configuration} from the given stream.
     *
     * @param reader the stream to read from
     * @return {@link Configuration.Builder} as read from the given stream that can be further customized
     * @throws ConfigurationException on configuration consistency checks
     */
    Configuration.Builder read(Reader reader) throws ConfigurationException;
}
