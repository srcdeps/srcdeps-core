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
package org.srcdeps.core.config.scalar;

import org.junit.Assert;
import org.junit.Test;
import org.srcdeps.core.config.scalar.CharStreamSource.Scheme;

public class CharStreamSourceTest {

    @Test
    public void of() {
        Assert.assertEquals(new CharStreamSource(Scheme.classpath, "/my/path"),
                CharStreamSource.of("classpath:/my/path"));
        Assert.assertEquals(new CharStreamSource(Scheme.file, "my/path"), CharStreamSource.of("file:my/path"));
        Assert.assertEquals(new CharStreamSource(Scheme.literal, " hello"), CharStreamSource.of("literal: hello"));
        Assert.assertEquals(new CharStreamSource(Scheme.literal, ""), CharStreamSource.of("literal:"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void ofInvalid() {
        CharStreamSource.of("foo");
    }
}
