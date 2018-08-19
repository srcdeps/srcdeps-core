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
package org.srcdeps.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link SrcdepsInner} gathers the information passed from an outer build to its inner build.
 * <p>
 * This class and the classes it depends on is supposed to be embedded inside a {@code gradle.settings} script. We
 * maintain it as a Java file inside the main source tree to see compilation issues early.
 *
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */
public class SrcdepsInner {
    private static final Logger log = LoggerFactory.getLogger(SrcdepsInner.class);

    /** The source version we are building */
    final String version;

    /** The set of artifacts we should build */
    final GavSet gavSet;

    SrcdepsInner() {
        log.debug("srcdeps: Initializing " + SrcdepsInner.class.getSimpleName());
        this.version = System.getProperty("srcdeps.inner.version");
        this.gavSet = GavSet.builder() //
                .includes(System.getProperty("srcdeps.inner.includes")) //
                .excludes(System.getProperty("srcdeps.inner.excludes")) //
                .build();
    }
}
