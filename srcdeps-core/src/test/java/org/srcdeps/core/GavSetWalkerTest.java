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

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import org.junit.Assert;
import org.junit.Test;
import org.srcdeps.core.util.Consumer;

public class GavSetWalkerTest {

    private static class Collector implements Consumer<GavtcPath> {

        private final TreeSet<GavtcPath> actual = new TreeSet<>(GavtcPath.comparator());
        private final TreeSet<GavtcPath> expected = new TreeSet<>(GavtcPath.comparator());

        @Override
        public void accept(GavtcPath t) {
            actual.add(t);
        }

        public void assertExpected() {
            Assert.assertEquals(expected, actual);
        }

        public Collector expect(String gavtc, String path) {
            expected.add(GavtcPath.of(gavtc, LOCAL_MAVEN_REPO_ROOT_DIR.resolve(path)));
            return this;
        }

    }

    private static final Path BASEDIR = Paths.get(System.getProperty("project.basedir", "."));
    private static final Path LOCAL_MAVEN_REPO_ROOT_DIR;

    static {
        LOCAL_MAVEN_REPO_ROOT_DIR = BASEDIR.resolve("target/test-classes/GavSetWalker/local-maven-repo").normalize()
                .toAbsolutePath();
    }

    private void assertSubtree(String gavPattern, String expectedPath) {
        GavPattern gp = GavPattern.of(gavPattern);
        Path actual = GavSetWalker.patternToSubtree(gp, "1.2.3");
        Assert.assertEquals(Paths.get(expectedPath), actual);
    }

    private void assertSubtrees(GavSet gavSet, String... expectedPaths) {
        List<Path> expected = new ArrayList<>(expectedPaths.length);
        for (String path : expectedPaths) {
            expected.add(Paths.get(path));
        }
        final List<Path> actual = GavSetWalker.gavSetToSubtrees(gavSet, "1.2.3");
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void gavSetToSubtreesParentBeforeAfterChild() {
        final GavSet gavSet = GavSet.builder() //
                .include("org.group1.compon2") //
                .include("org.group1") //
                .include("org.group1.compon1") //
                .include("org.group2.compon1") //
                .build();
        assertSubtrees(gavSet, "org/group1", "org/group2/compon1");
    }

    @Test
    public void gavSetToSubtreesParentBeforeChild() {
        final GavSet gavSet = GavSet.builder() //
                .include("org.group1") //
                .include("org.group1.compon1") //
                .include("org.group2.compon1") //
                .build();
        assertSubtrees(gavSet, "org/group1", "org/group2/compon1");
    }

    @Test
    public void gavSetToSubtreesSingle() {
        final GavSet gavSet = GavSet.builder() //
                .include("org.group1.compon1") //
                .build();
        assertSubtrees(gavSet, "org/group1/compon1");
    }

    @Test
    public void patternToSubtree() {
        assertSubtree("*", "");
        assertSubtree("junit", "junit");
        assertSubtree("org.mygroup", "org/mygroup");
        assertSubtree("org.mygroup*", "org");
        assertSubtree("org.mygroup.*", "org/mygroup");
        assertSubtree("*:myartifact:1.2.3", "");
        assertSubtree("org.mygroup:prefix*:1.2.3", "org/mygroup");
        assertSubtree("org.mygroup:my.artifact:1.2.3", "org/mygroup/my.artifact/1.2.3");
        assertSubtree("org.mygroup:my.artifact*:1.2.3", "org/mygroup");
    }

    @Test
    public void walkGroup() throws IOException {
        final GavSet gavSet = GavSet.builder() //
                .include("org.group1.compon1") //
                .build();
        Collector c = new Collector()//
                .expect("org.group1.compon1:compon-artifact1:2.3.4:jar",
                        "org/group1/compon1/compon-artifact1/2.3.4/compon-artifact1-2.3.4.jar") //
                .expect("org.group1.compon1:compon-artifact1:2.3.4:pom",
                        "org/group1/compon1/compon-artifact1/2.3.4/compon-artifact1-2.3.4.pom") //
                .expect("org.group1.compon1:compon-artifact1:2.3.4:jar:javadoc",
                        "org/group1/compon1/compon-artifact1/2.3.4/compon-artifact1-2.3.4-javadoc.jar") //
                .expect("org.group1.compon1:compon-artifact1:2.3.4:jar:sources",
                        "org/group1/compon1/compon-artifact1/2.3.4/compon-artifact1-2.3.4-sources.jar") //
                .expect("org.group1.compon1:compon-artifact1:2.3.4:tar.gz:sources",
                        "org/group1/compon1/compon-artifact1/2.3.4/compon-artifact1-2.3.4-sources.tar.gz") //
                .expect("org.group1.compon1:compon-artifact1:2.3.4:tar.gz",
                        "org/group1/compon1/compon-artifact1/2.3.4/compon-artifact1-2.3.4.tar.gz") //
        ;
        new GavSetWalker(LOCAL_MAVEN_REPO_ROOT_DIR, gavSet, "2.3.4").walk(c);
        c.assertExpected();
    }

    @Test
    public void walkGroupHavingSubgroup() throws IOException {
        final GavSet gavSet = GavSet.builder() //
                .include("org.group1") //
                .build();
        Collector c = new Collector()//
                .expect("org.group1:artifact1:1.2.3:jar", "org/group1/artifact1/1.2.3/artifact1-1.2.3.jar") //
                .expect("org.group1:artifact1:1.2.3:pom", "org/group1/artifact1/1.2.3/artifact1-1.2.3.pom") //
        ;
        new GavSetWalker(LOCAL_MAVEN_REPO_ROOT_DIR, gavSet, "1.2.3").walk(c);
        c.assertExpected();
    }
}
