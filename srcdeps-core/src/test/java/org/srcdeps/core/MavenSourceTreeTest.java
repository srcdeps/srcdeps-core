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
package org.srcdeps.core;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;
import org.srcdeps.core.MavenSourceTree.Builder;
import org.srcdeps.core.MavenSourceTree.Module;

public class MavenSourceTreeTest {
    private static final Path BASEDIR = Paths.get(System.getProperty("project.basedir", "."));

    @Test
    public void tree() throws IOException {
        final Path root = BASEDIR.resolve("target/test-classes/MavenSourceTree/tree-1");
        final Builder b = new Builder(root, StandardCharsets.UTF_8).pomXml(root.resolve("pom.xml"));

        Assert.assertEquals(11, b.modulesByGa.size());
        Assert.assertEquals(11, b.modulesByPath.size());

        final Module.Builder parent = b.modulesByGa.get("org.srcdeps.tree-1:tree-parent");
        Assert.assertTrue(b.modulesByPath.get("pom.xml") == parent);
        Assert.assertEquals("pom.xml", parent.pomPath);
        Assert.assertEquals("tree-parent", parent.artifactId);
        Assert.assertEquals("org.srcdeps.tree-1", parent.groupId);
        Assert.assertEquals("org.srcdeps.tree-1:tree-parent", parent.getGa());
        Assert.assertEquals("external-parent", parent.parentArtifactId);
        Assert.assertEquals("org.srcdeps.external", parent.parentGroupId);
        Assert.assertEquals("org.srcdeps.external:external-parent", parent.getParentGa());
        Assert.assertEquals(new LinkedHashSet<String>(
                Arrays.asList("module-1/pom.xml", "module-2/pom.xml", "module-3/pom.xml", "module-4/pom.xml",
                        "module-6/pom.xml", "module-7/pom.xml", "proper-parent/pom.xml", "declared-parent/pom.xml")),
                parent.children);
        Assert.assertEquals(Collections.emptySet(), parent.dependencies);

        final Module.Builder properParent = b.modulesByGa.get("org.srcdeps.tree-1:proper-parent");
        Assert.assertTrue(b.modulesByPath.get("proper-parent/pom.xml") == properParent);
        Assert.assertEquals("proper-parent/pom.xml", properParent.pomPath);
        Assert.assertEquals("proper-parent", properParent.artifactId);
        Assert.assertNull(properParent.groupId);
        Assert.assertEquals("org.srcdeps.tree-1:proper-parent", properParent.getGa());
        Assert.assertEquals("tree-parent", properParent.parentArtifactId);
        Assert.assertEquals("org.srcdeps.tree-1", properParent.parentGroupId);
        Assert.assertEquals("org.srcdeps.tree-1:tree-parent", properParent.getParentGa());
        Assert.assertEquals(new LinkedHashSet<String>(Arrays.asList("proper-parent/module-5/pom.xml")),
                properParent.children);
        Assert.assertEquals(Collections.emptySet(), properParent.dependencies);

        final Module.Builder m1 = b.modulesByGa.get("org.srcdeps.tree-1:tree-module-1");
        Assert.assertTrue(b.modulesByPath.get("module-1/pom.xml") == m1);
        Assert.assertEquals("module-1/pom.xml", m1.pomPath);
        Assert.assertEquals("tree-module-1", m1.artifactId);
        Assert.assertNull(m1.groupId);
        Assert.assertEquals("org.srcdeps.tree-1:tree-module-1", m1.getGa());
        Assert.assertEquals("tree-parent", m1.parentArtifactId);
        Assert.assertEquals("org.srcdeps.tree-1", m1.parentGroupId);
        Assert.assertEquals("org.srcdeps.tree-1:tree-parent", m1.getParentGa());
        Assert.assertEquals(new LinkedHashSet<String>(Arrays.asList("org.srcdeps.external:artifact-3")),
                m1.dependencies);
        Assert.assertEquals(Collections.emptySet(), m1.children);

        final Module.Builder m2 = b.modulesByGa.get("org.srcdeps.tree-1:tree-module-2");
        Assert.assertTrue(b.modulesByPath.get("module-2/pom.xml") == m2);
        Assert.assertEquals("module-2/pom.xml", m2.pomPath);
        Assert.assertEquals("tree-module-2", m2.artifactId);
        Assert.assertNull(m2.groupId);
        Assert.assertEquals("org.srcdeps.tree-1:tree-module-2", m2.getGa());
        Assert.assertEquals("tree-parent", m1.parentArtifactId);
        Assert.assertEquals("org.srcdeps.tree-1", m2.parentGroupId);
        Assert.assertEquals("org.srcdeps.tree-1:tree-parent", m2.getParentGa());
        Assert.assertEquals(
                new LinkedHashSet<String>(
                        Arrays.asList("org.srcdeps.tree-1:tree-module-4", "org.srcdeps.tree-1:tree-module-7",
                                "org.srcdeps.tree-1:tree-module-8", "org.srcdeps.external:artifact-4")),
                m2.dependencies);
        Assert.assertEquals(Collections.emptySet(), m2.children);

        final Module.Builder m3 = b.modulesByGa.get("org.srcdeps.tree-1:tree-module-3");
        Assert.assertTrue(b.modulesByPath.get("module-3/pom.xml") == m3);
        Assert.assertEquals("module-3/pom.xml", m3.pomPath);
        Assert.assertEquals("tree-module-3", m3.artifactId);
        Assert.assertNull(m3.groupId);
        Assert.assertEquals("org.srcdeps.tree-1:tree-module-3", m3.getGa());
        Assert.assertEquals("tree-parent", m1.parentArtifactId);
        Assert.assertEquals("org.srcdeps.tree-1", m3.parentGroupId);
        Assert.assertEquals("org.srcdeps.tree-1:tree-parent", m3.getParentGa());
        Assert.assertEquals(new LinkedHashSet<String>(Arrays.asList("org.srcdeps.external:artifact-1")),
                m3.dependencies);
        Assert.assertEquals(Collections.emptySet(), m3.children);

        final Module.Builder m4 = b.modulesByGa.get("org.srcdeps.tree-1:tree-module-4");
        Assert.assertTrue(b.modulesByPath.get("module-4/pom.xml") == m4);
        Assert.assertEquals("module-4/pom.xml", m4.pomPath);
        Assert.assertEquals("tree-module-4", m4.artifactId);
        Assert.assertNull(m4.groupId);
        Assert.assertEquals("org.srcdeps.tree-1:tree-module-4", m4.getGa());
        Assert.assertEquals("tree-parent", m4.parentArtifactId);
        Assert.assertEquals("org.srcdeps.tree-1", m4.parentGroupId);
        Assert.assertEquals("org.srcdeps.tree-1:tree-parent", m4.getParentGa());
        Assert.assertEquals(
                new LinkedHashSet<String>(
                        Arrays.asList("org.srcdeps.tree-1:tree-module-1", "org.srcdeps.tree-1:tree-module-5")),
                m4.dependencies);
        Assert.assertEquals(Collections.emptySet(), m4.children);

        final MavenSourceTree t = b.build();
        final Set<String> expandedIncludes = t.computeModuleClosure(Arrays.asList("org.srcdeps.tree-1:tree-module-2"));
        Assert.assertEquals(new LinkedHashSet<>(Arrays.asList("org.srcdeps.tree-1:tree-module-2",
                "org.srcdeps.tree-1:tree-parent", "org.srcdeps.tree-1:tree-module-4",
                "org.srcdeps.tree-1:tree-module-1", "org.srcdeps.tree-1:tree-module-5",
                "org.srcdeps.tree-1:proper-parent", "org.srcdeps.tree-1:tree-module-7",
                "org.srcdeps.tree-1:tree-module-8", "org.srcdeps.tree-1:declared-parent")), expandedIncludes);

        final Map<String, Set<String>> removeChildPaths = t.unlinkUneededModules(expandedIncludes, t.getRootModule(),
                new LinkedHashMap<String, Set<String>>());
        Assert.assertEquals(1, removeChildPaths.size());
        Set<String> rootUnlinks = removeChildPaths.get("pom.xml");
        Assert.assertEquals(new LinkedHashSet<String>(Arrays.asList("module-3/pom.xml", "module-6/pom.xml")),
                rootUnlinks);

        t.unlinkUneededModules(expandedIncludes);

        final Path expectedRoot = BASEDIR.resolve("target/test-classes/MavenSourceTree/tree-1-expected");

        for (String path : removeChildPaths.keySet()) {
            final Path actualPath = root.resolve(path);
            final Path expectedPath = expectedRoot.resolve(path);
            Assert.assertEquals(new String(Files.readAllBytes(expectedPath), StandardCharsets.UTF_8),
                    new String(Files.readAllBytes(actualPath), StandardCharsets.UTF_8).replace("\r", ""));
        }
    }

}
