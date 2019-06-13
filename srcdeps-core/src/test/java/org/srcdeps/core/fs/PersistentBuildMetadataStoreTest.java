/**
 * Copyright 2015-2019 Maven Source Dependencies
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
package org.srcdeps.core.fs;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;
import org.srcdeps.core.BuildRequest;
import org.srcdeps.core.BuildRequest.Verbosity;
import org.srcdeps.core.GavSet;
import org.srcdeps.core.Gavtc;
import org.srcdeps.core.SrcVersion;

public class PersistentBuildMetadataStoreTest {

    private static final Path mdStorepath = Paths.get(System.getProperty("project.build.directory", "target"))
            .resolve(PersistentBuildMetadataStore.class.getSimpleName()).toAbsolutePath();

    @Test
    public void createBuildRequestIdPath() {
        final PersistentBuildMetadataStore store = new PersistentBuildMetadataStore(mdStorepath);
        Assert.assertEquals(mdStorepath.resolve("a/b/c/d/efgh"), store.createBuildRequestIdPath("abcdefgh"));
    }

    @Test
    public void writeRead() {
        final Map<String, String> env1 = new HashMap<>();
        env1.put("k1", "v1");
        env1.put("k2", "v2");

        final Set<String> props = new HashSet<>();
        props.add("prop1");
        props.add("prop2");

        String id1 = BuildRequest.computeHash(true, true, Arrays.<String>asList("arg1", "arg2"), env1, props,
                StandardCharsets.UTF_8, GavSet.builder().include("org.mygroup").exclude("other-group").build(),
                Arrays.asList("url1", "url2"), true, SrcVersion.parse("1.2.3-SRC-revision-deadbeef"), "1.2.3", true,
                Collections.singleton("org:a"), false, 50000, Verbosity.error);

        final PersistentBuildMetadataStore store = new PersistentBuildMetadataStore(mdStorepath);
        {
            final String commitId = "deadbeef";
            store.storeCommitId(id1, commitId);

            final String actual = store.retrieveCommitId(id1);
            Assert.assertEquals(commitId, actual);
        }

        {
            final String sha1 = "shashshs";
            final Gavtc gavtc1 = Gavtc.of("org.o1:a1:1.2.3:jar");
            store.storeSha1(id1, gavtc1, sha1);

            final String sha2 = "shashshs";
            final Gavtc gavtc2 = Gavtc.of("org.o2:a2:1.2.3:jar");
            store.storeSha1(id1, gavtc2, sha2);

            final String actual1 = store.retrieveSha1(id1, gavtc1);
            final String actual2 = store.retrieveSha1(id1, gavtc2);

            Assert.assertEquals(sha1, actual1);
            Assert.assertEquals(sha2, actual2);
        }

        PersistentBuildMetadataStore.BuildRequestIdCollector collector = new PersistentBuildMetadataStore.BuildRequestIdCollector();
        store.walkBuildRequestHashes(collector);
        Assert.assertEquals(1, collector.getHashes().size());
        Assert.assertEquals(id1, collector.getHashes().get(0));

    }
}
