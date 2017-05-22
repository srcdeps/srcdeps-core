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
package org.srcdeps.core.config.scalar;

import org.junit.Assert;
import org.junit.Test;
import org.srcdeps.core.config.scalar.Negatable.NegatableProperty;

public class NegatablePropertyTest {

    private static void assertNegatable(boolean negated, String key, String value, String rawString) {
        NegatableProperty n = new NegatableProperty(negated, key, value);
        Assert.assertEquals(n, NegatableProperty.of(rawString));
        Assert.assertEquals(n.toString(), rawString);

    }

    @Test
    public void of() {
        assertNegatable(true, "key", "val", "!key=val");
        assertNegatable(true, "key", null, "!key");
        assertNegatable(false, "key", "val", "key=val");
        assertNegatable(false, "key", null, "key");
    }

    @Test
    public void ofEscaped() {
        assertNegatable(false, "!key", null, "\\!key");
        assertNegatable(false, "\\key", null, "\\\\key");

        try {
            assertNegatable(false, "\\key", null, "\\key");
            Assert.fail("Expected "+ IllegalStateException.class.getName());
        } catch (IllegalStateException e) {
            Assert.assertEquals("Cannot parse a PropertyParser: The input string \"\\key\" contains an unknown escape sequence \"\\k\" at offset 0.", e.getMessage());
        }

        assertNegatable(false, "=key", null, "\\=key");
        assertNegatable(false, "\\=key", null, "\\\\\\=key");
        assertNegatable(false, "!key", "val", "\\!key=val");
        assertNegatable(false, "k\\!ey", null, "k\\\\!ey");
        assertNegatable(false, "k!ey", "val", "k!ey=val");
        assertNegatable(false, "key", "v!al", "key=v!al");
        assertNegatable(false, "key=val", null, "key\\=val");
        assertNegatable(false, "key=key", "val=val", "key\\=key=val\\=val");
    }

    @Test
    public void ofEmpty() {
        Assert.assertNull(NegatableProperty.of(""));
        Assert.assertNull(NegatableProperty.of("!"));
        Assert.assertNull(NegatableProperty.of("="));
        Assert.assertNull(NegatableProperty.of("!="));
    }

    @Test
    public void ofNull() {
        Assert.assertNull(NegatableProperty.of(null));
    }
}
