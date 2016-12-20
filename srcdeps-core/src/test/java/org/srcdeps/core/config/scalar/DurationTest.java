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
package org.srcdeps.core.config.scalar;

import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Test;

public class DurationTest {
    private static void assertDuration(long value, TimeUnit unit, String rawString) {
        Duration d = new Duration(value, unit);
        Assert.assertEquals(d, Duration.of(rawString));
        Assert.assertEquals(d.toString(), rawString);

    }

    @Test
    public void of() {
        assertDuration(0, TimeUnit.NANOSECONDS, "0ns");
        assertDuration(200, TimeUnit.NANOSECONDS, "200ns");

        assertDuration(0, TimeUnit.MICROSECONDS, "0us");
        assertDuration(200, TimeUnit.MICROSECONDS, "200us");

        assertDuration(0, TimeUnit.MILLISECONDS, "0ms");
        assertDuration(200, TimeUnit.MILLISECONDS, "200ms");

        assertDuration(0, TimeUnit.SECONDS, "0s");
        assertDuration(200, TimeUnit.SECONDS, "200s");

        assertDuration(0, TimeUnit.MINUTES, "0m");
        assertDuration(200, TimeUnit.MINUTES, "200m");

        assertDuration(0, TimeUnit.HOURS, "0h");
        assertDuration(200, TimeUnit.HOURS, "200h");

        assertDuration(0, TimeUnit.DAYS, "0d");
        assertDuration(200, TimeUnit.DAYS, "200d");
    }

    @Test(expected = IllegalArgumentException.class)
    public void ofBadUnit() {
        Assert.assertNull(Duration.of("0a"));
        Assert.assertNull(Duration.of("0fs"));
        Assert.assertNull(Duration.of("0fms"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void ofEmpty() {
        Assert.assertNull(Duration.of(""));
    }

    @Test(expected = IllegalArgumentException.class)
    public void ofNoNumber() {
        Assert.assertNull(Duration.of("ms"));
    }

    @Test
    public void ofNull() {
        Assert.assertNull(Duration.of(null));
    }
}
