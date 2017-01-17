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
package org.srcdeps.core;

import org.junit.Assert;
import org.junit.Test;

public class GavPatternTest {

    @Test
    public void source() {
        assertSource("org.group");
        assertSource("org.group:artifact");
        assertSource("org.group:artifact:1.2.3");
        assertSource("org.group:*:1.2.3");
    }

    private void assertSource(String wildcardString) {

        GavPattern gavPattern = GavPattern.of(wildcardString);

        Assert.assertEquals(wildcardString, gavPattern.toString());

    }

    @Test
    public void resolveAny() {
        GavPattern gavPattern = GavPattern.of("*:*:*");

        Assert.assertTrue(gavPattern.matches("group", "artifact", "1.2.3"));
        Assert.assertTrue(gavPattern.matches("group1", "artifact", "1.2.3"));
        Assert.assertTrue(gavPattern.matches("group", "artifact1", "1.2.3"));
        Assert.assertTrue(gavPattern.matches("group", "artifact", "1.2.4"));

        gavPattern = GavPattern.of("");

        Assert.assertTrue(gavPattern.matches("group", "artifact", "1.2.3"));
        Assert.assertTrue(gavPattern.matches("group1", "artifact", "1.2.3"));
        Assert.assertTrue(gavPattern.matches("group", "artifact1", "1.2.3"));
        Assert.assertTrue(gavPattern.matches("group", "artifact", "1.2.4"));

    }

    @Test
    public void resolveAnyArtifactOrVersion() {
        GavPattern gavPattern = GavPattern.of("group:*:*");

        Assert.assertTrue(gavPattern.matches("group", "artifact", "1.2.3"));
        Assert.assertFalse(gavPattern.matches("group1", "artifact", "1.2.3"));
        Assert.assertTrue(gavPattern.matches("group", "artifact1", "1.2.3"));
        Assert.assertTrue(gavPattern.matches("group", "artifact", "1.2.4"));

        gavPattern = GavPattern.of("group");

        Assert.assertTrue(gavPattern.matches("group", "artifact", "1.2.3"));
        Assert.assertFalse(gavPattern.matches("group1", "artifact", "1.2.3"));
        Assert.assertTrue(gavPattern.matches("group", "artifact1", "1.2.3"));
        Assert.assertTrue(gavPattern.matches("group", "artifact", "1.2.4"));

    }

    @Test
    public void resolveAnyVersion() {
        GavPattern gavPattern = GavPattern.of("group:artifact:*");

        Assert.assertTrue(gavPattern.matches("group", "artifact", "1.2.3"));
        Assert.assertFalse(gavPattern.matches("group1", "artifact", "1.2.3"));
        Assert.assertFalse(gavPattern.matches("group", "artifact1", "1.2.3"));
        Assert.assertTrue(gavPattern.matches("group", "artifact", "1.2.4"));

        gavPattern = GavPattern.of("group:artifact");

        Assert.assertTrue(gavPattern.matches("group", "artifact", "1.2.3"));
        Assert.assertFalse(gavPattern.matches("group1", "artifact", "1.2.3"));
        Assert.assertFalse(gavPattern.matches("group", "artifact1", "1.2.3"));
        Assert.assertTrue(gavPattern.matches("group", "artifact", "1.2.4"));

    }

    @Test
    public void resolveLiteral() {
        GavPattern gavPattern = GavPattern.of("group:artifact:1.2.3");

        Assert.assertTrue(gavPattern.matches("group", "artifact", "1.2.3"));
        Assert.assertFalse(gavPattern.matches("group1", "artifact", "1.2.3"));
        Assert.assertFalse(gavPattern.matches("group", "artifact1", "1.2.3"));
        Assert.assertFalse(gavPattern.matches("group", "artifact", "1.2.4"));
    }
}
