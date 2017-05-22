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
import org.srcdeps.core.config.scalar.Negatable.NegatableString;

public class NegatableStringTest {

    private static void assertNegatable(boolean negated, String value, String rawString) {
        NegatableString n = new NegatableString(negated, value);
        Assert.assertEquals(n, NegatableString.of(rawString));
        Assert.assertEquals(n.toString(), rawString);

    }

    @Test
    public void of() {
        assertNegatable(true, "val", "!val");
        assertNegatable(false, "val", "val");
    }

    @Test
    public void ofEscaped() {
        assertNegatable(false, "!val", "\\!val");
        assertNegatable(false, "v\\!al", "v\\!al");
        assertNegatable(false, "v!al", "v!al");
        assertNegatable(false, "\\val", "\\val");
    }

    @Test
    public void ofEmpty() {
        Assert.assertNull(NegatableString.of(""));
        Assert.assertNull(NegatableString.of("!"));
    }

    @Test
    public void ofNull() {
        Assert.assertNull(NegatableString.of(null));
    }


}
