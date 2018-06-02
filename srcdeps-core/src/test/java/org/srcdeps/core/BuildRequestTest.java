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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;
import org.srcdeps.core.BuildRequest.Verbosity;

public class BuildRequestTest {

    @Test
    public void getHash() {

        final Map<String, String> env1 = new HashMap<>();
        env1.put("k1", "v1");
        env1.put("k2", "v2");

        final Set<String> props = new HashSet<>();
        props.add("prop1");
        props.add("prop2");

        String id1 = BuildRequest.computeHash(true, true, Arrays.<String>asList("arg1", "arg2"), env1, props,
                GavSet.builder().include("org.mygroup").exclude("other-group").build(), Arrays.asList("url1", "url2"),
                true, SrcVersion.parse("1.2.3-SRC-revision-deadbeef"), "1.2.3", 50000, Verbosity.error);
        String id2 = BuildRequest.computeHash(true, true, new ArrayList<>(Arrays.<String>asList("arg1", "arg2")),
                new LinkedHashMap<String, String>(env1), new LinkedHashSet<String>(props),
                GavSet.builder().include("org.mygroup").exclude("other-group").build(),
                new ArrayList<>(Arrays.<String>asList("url1", "url2")), true,
                SrcVersion.parse("1.2.3-SRC-revision-deadbeef"), "1.2.3", 50000, Verbosity.error);

        Assert.assertEquals(id1, id2);
        Assert.assertEquals(id1.hashCode(), id2.hashCode());
        Assert.assertEquals("f1e6ff1667a2fb9cb8a6419fe9b33accac619479", id1);

    }

}
