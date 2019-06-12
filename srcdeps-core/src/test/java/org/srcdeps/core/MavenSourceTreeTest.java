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
import java.lang.ProcessBuilder.Redirect;
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
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Test;
import org.srcdeps.core.MavenSourceTree.Builder;
import org.srcdeps.core.MavenSourceTree.Expression;
import org.srcdeps.core.MavenSourceTree.GaExpression;
import org.srcdeps.core.MavenSourceTree.Module;
import org.srcdeps.core.MavenSourceTree.Module.Profile;
import org.srcdeps.core.MavenSourceTree.Module.Profile.PropertyBuilder;
import org.srcdeps.core.shell.BadExitCodeException;
import org.srcdeps.core.shell.CommandTimeoutException;
import org.srcdeps.core.shell.IoRedirects;
import org.srcdeps.core.shell.Shell;
import org.srcdeps.core.shell.ShellCommand;
import org.srcdeps.core.shell.ShellCommand.ShellCommandBuilder;
import org.srcdeps.core.util.SrcdepsCoreUtils;

public class MavenSourceTreeTest {
    private static final Path BASEDIR = Paths.get(System.getProperty("project.basedir", "."));
    private static final Path MVN_LOCAL_REPO;
    private static final Path MVNW;

    static {
        MVN_LOCAL_REPO = BASEDIR.resolve("target/mvn-local-repo");
        MVNW = SrcdepsCoreUtils.findMvnw(BASEDIR);
    }

    static void assertProperty(MavenSourceTree t, String propertyName, Ga ga, String expectedValue, String... profiles)
            throws BadExitCodeException, CommandTimeoutException, BuildException, IOException {

        final StringBuilder profs = new StringBuilder("-P");
        if (profiles.length > 0) {
            for (int i = 0; i < profiles.length; i++) {
                if (i > 0) {
                    profs.append(',');
                }
                profs.append(profiles[i]);
            }
        }
        final Path out = BASEDIR.resolve("target/prop-" + propertyName + "-" + ga.getGroupId() + "-"
                + ga.getArtifactId() + profs.toString() + ".txt");
        final ShellCommandBuilder cmd = ShellCommand.builder() //
                .workingDirectory(t.getRootDirectory()) //
                .executable(MVNW.toString()) //
                .arguments( //
                        "org.apache.maven.plugins:maven-help-plugin:3.2.0:evaluate", //
                        "-Dexpression=" + propertyName, //
                        "-Dartifact=" + ga.toString(), //
                        "-Dmaven.repo.local=" + MVN_LOCAL_REPO.toString(), //
                        "-q", //
                        "-DforceStdout") //
                .ioRedirects(IoRedirects.builder().stdout(Redirect.to(out.toFile())).build()) //
        ;
        if (profiles.length > 0) {
            cmd.arguments(profs.toString());
        }
        Shell.execute(cmd.build()).assertSuccess();
        final String helpEval = new String(Files.readAllBytes(out), StandardCharsets.UTF_8);
        Assert.assertEquals(expectedValue, helpEval);

        t.evaluate(Expression.of("${" + propertyName + "}", ga), MavenSourceTree.profiles(profiles));
    }

    static GaExpression moduleGae(String gavString) {
        final Gav gav = Gav.of(gavString);
        final Ga ga = new Ga(gav.getGroupId(), gav.getArtifactId());
        return new GaExpression(ga, Expression.of(gav.getVersion(), ga));
    }

    static Map<String, Expression> props(Ga ga, String... keyVals) {
        final Map<String, Expression> props = new LinkedHashMap<>();
        for (int i = 0; i < keyVals.length;) {
            props.put(keyVals[i++], Expression.of(keyVals[i++], ga));
        }
        return props;
    }

    @Test
    public void propertyEval() throws IOException, CommandTimeoutException, BuildException {
        final Path root = BASEDIR.resolve("target/test-classes/MavenSourceTree/properties");
        final MavenSourceTree t = new Builder(root, StandardCharsets.UTF_8).pomXml(root.resolve("pom.xml")).build();

        final Module m8 = t.getModulesByGa().get(Ga.of("org.srcdeps.properties:module-1"));
        Assert.assertEquals(new Expression.Constant("val-1/main"),
                m8.findPropertyDefinition("prop1", MavenSourceTree.profiles()).getValue());
        Assert.assertEquals(new Expression.Constant("val-1/p1"),
                m8.findPropertyDefinition("prop1", MavenSourceTree.profiles("p1")).getValue());
        Assert.assertEquals(new Expression.Constant("val-1/p2"),
                m8.findPropertyDefinition("prop1", MavenSourceTree.profiles("p2")).getValue());
        Assert.assertEquals(new Expression.Constant("val-1/p2"),
                m8.findPropertyDefinition("prop1", MavenSourceTree.profiles("p1", "p2")).getValue());

        final ShellCommand cmd = ShellCommand.builder() //
                .workingDirectory(root) //
                .executable(MVNW.toString()) //
                .arguments("clean", "install", "-Dmaven.repo.local=" + MVN_LOCAL_REPO.toString()) //
                .build();
        Shell.execute(cmd).assertSuccess();

        assertProperty(t, "prop1", Ga.of("org.srcdeps.properties:module-1"), "val-1/main");
        assertProperty(t, "prop1", Ga.of("org.srcdeps.properties:module-1"), "val-1/p1", "p1");
        assertProperty(t, "prop1", Ga.of("org.srcdeps.properties:module-1"), "val-1/p2", "p2");
        assertProperty(t, "prop1", Ga.of("org.srcdeps.properties:module-1"), "val-1/p2", "p1", "p2");
    }

    @Test
    public void setVersions() throws IOException {
        final Path root = BASEDIR.resolve("target/test-classes/MavenSourceTree/set-versions");

        final MavenSourceTree t = new Builder(root, StandardCharsets.UTF_8).pomXml(root.resolve("pom.xml")).build();
        t.setVersions("2.2.2", MavenSourceTree.profiles());

        final Path expectedRoot = BASEDIR.resolve("target/test-classes/MavenSourceTree/set-versions-expected");

        for (String path : t.getModulesByPath().keySet()) {
            final Path actualPath = root.resolve(path);
            final Path expectedPath = expectedRoot.resolve(path);
            Assert.assertEquals(new String(Files.readAllBytes(expectedPath), StandardCharsets.UTF_8),
                    new String(Files.readAllBytes(actualPath), StandardCharsets.UTF_8).replace("\r", ""));
        }

    }

    @Test
    public void tree() throws IOException {
        final Path root = BASEDIR.resolve("target/test-classes/MavenSourceTree/tree-1");
        final Builder b = new Builder(root, StandardCharsets.UTF_8).pomXml(root.resolve("pom.xml"));

        Assert.assertEquals(12, b.modulesByGa.size());
        Assert.assertEquals(12, b.modulesByPath.size());

        final Module.Builder parent = b.modulesByGa.get(Ga.of("org.srcdeps.tree-1:tree-parent"));
        Assert.assertTrue(b.modulesByPath.get("pom.xml") == parent);
        Assert.assertEquals("pom.xml", parent.pomPath);
        final GaExpression treeParentGav = moduleGae("org.srcdeps.tree-1:tree-parent:0.0.1");
        Assert.assertEquals(treeParentGav, parent.moduleGav.build());
        Assert.assertEquals(moduleGae("org.srcdeps.external:external-parent:1.2.3"), parent.parentGav.build());

        Assert.assertEquals(new LinkedHashSet<String>(Arrays.asList("module-1/pom.xml", "module-2/pom.xml",
                "module-3/pom.xml", "module-4/pom.xml", "module-6/pom.xml", "module-7/pom.xml", "plugin/pom.xml",
                "proper-parent/pom.xml", "declared-parent/pom.xml")), parent.profiles.get(0).children);
        Assert.assertEquals(Collections.emptyList(), parent.profiles.get(0).dependencies.stream()
                .map(bu -> bu.build().toString()).collect(Collectors.toList()));
        Assert.assertEquals(//
                props(treeParentGav.getGa(), "prop1", "val-parent").entrySet(), //
                parent.profiles.get(0).properties.stream().map(PropertyBuilder::build)
                        .collect(Collectors.toCollection(LinkedHashSet::new)));

        {
            final Module.Builder properParent = b.modulesByGa.get(Ga.of("org.srcdeps.tree-1:proper-parent"));
            Assert.assertTrue(b.modulesByPath.get("proper-parent/pom.xml") == properParent);
            Assert.assertEquals("proper-parent/pom.xml", properParent.pomPath);
            GaExpression gav = moduleGae("org.srcdeps.tree-1:proper-parent:0.0.1");
            Assert.assertEquals(gav, properParent.moduleGav.build());
            Assert.assertEquals(treeParentGav, properParent.parentGav.build());
            Assert.assertEquals(new LinkedHashSet<String>(Arrays.asList("proper-parent/module-5/pom.xml")),
                    properParent.profiles.get(0).children);
            Assert.assertEquals(Collections.emptyList(), properParent.profiles.get(0).dependencies.stream()
                    .map(bu -> bu.build().toString()).collect(Collectors.toList()));
            Assert.assertEquals(//
                    props(gav.getGa(), "prop1", "val-proper-parent").entrySet(), //
                    properParent.profiles.get(0).properties.stream().map(PropertyBuilder::build)
                            .collect(Collectors.toCollection(LinkedHashSet::new)));
        }

        {
            final Module.Builder m1 = b.modulesByGa.get(Ga.of("org.srcdeps.tree-1:tree-module-1"));
            Assert.assertTrue(b.modulesByPath.get("module-1/pom.xml") == m1);
            Assert.assertEquals("module-1/pom.xml", m1.pomPath);
            Assert.assertEquals(moduleGae("org.srcdeps.tree-1:tree-module-1:0.0.1"), m1.moduleGav.build());
            Assert.assertEquals(treeParentGav, m1.parentGav.build());
            Assert.assertEquals(Arrays.asList("org.srcdeps.external:artifact-3:1.2.3"), m1.profiles.get(0).dependencies
                    .stream().map(bu -> bu.build().toString()).collect(Collectors.toList()));
            Assert.assertEquals(Collections.emptySet(), m1.profiles.get(0).children);
        }

        {
            final Module.Builder m2 = b.modulesByGa.get(Ga.of("org.srcdeps.tree-1:tree-module-2"));
            Assert.assertTrue(b.modulesByPath.get("module-2/pom.xml") == m2);
            Assert.assertEquals("module-2/pom.xml", m2.pomPath);
            Assert.assertEquals(moduleGae("org.srcdeps.tree-1:tree-module-2:0.0.1"), m2.moduleGav.build());
            Assert.assertEquals(treeParentGav, m2.parentGav.build());
            Assert.assertEquals(
                    Arrays.asList("org.srcdeps.tree-1:tree-module-4:0.0.1", "org.srcdeps.tree-1:tree-module-7:0.0.1",
                            "org.srcdeps.tree-1:tree-module-8:0.0.1", "org.srcdeps.external:artifact-4:1.2.3"),
                    m2.profiles.get(0).dependencies.stream().map(bu -> bu.build().toString())
                            .collect(Collectors.toList()));
            Assert.assertEquals(Collections.emptySet(), m2.profiles.get(0).children);
        }

        {
            final Module.Builder m3 = b.modulesByGa.get(Ga.of("org.srcdeps.tree-1:tree-module-3"));
            Assert.assertTrue(b.modulesByPath.get("module-3/pom.xml") == m3);
            Assert.assertEquals("module-3/pom.xml", m3.pomPath);
            Assert.assertEquals(moduleGae("org.srcdeps.tree-1:tree-module-3:0.0.1"), m3.moduleGav.build());
            Assert.assertEquals(treeParentGav, m3.parentGav.build());
            Assert.assertEquals(Arrays.asList("org.srcdeps.external:artifact-1:1.2.3"), m3.profiles.get(0).dependencies
                    .stream().map(bu -> bu.build().toString()).collect(Collectors.toList()));
            Assert.assertEquals(Collections.emptySet(), m3.profiles.get(0).children);
        }

        {
            final Module.Builder m4 = b.modulesByGa.get(Ga.of("org.srcdeps.tree-1:tree-module-4"));
            Assert.assertTrue(b.modulesByPath.get("module-4/pom.xml") == m4);
            Assert.assertEquals("module-4/pom.xml", m4.pomPath);
            Assert.assertEquals(moduleGae("org.srcdeps.tree-1:tree-module-4:0.0.1"), m4.moduleGav.build());
            Assert.assertEquals(treeParentGav, m4.parentGav.build());
            Assert.assertEquals(
                    Arrays.asList("org.srcdeps.tree-1:tree-module-1:0.0.1", "org.srcdeps.tree-1:tree-module-5:0.0.1"),
                    m4.profiles.get(0).dependencies.stream().map(bu -> bu.build().toString())
                            .collect(Collectors.toList()));
            Assert.assertEquals(Collections.emptySet(), m4.profiles.get(0).children);
        }

        {
            final Module.Builder m5 = b.modulesByGa.get(Ga.of("org.srcdeps.tree-1:tree-module-5"));
            Assert.assertTrue(b.modulesByPath.get("proper-parent/module-5/pom.xml") == m5);
            Assert.assertEquals("proper-parent/module-5/pom.xml", m5.pomPath);
            GaExpression gav = moduleGae("org.srcdeps.tree-1:tree-module-5:0.0.1");
            Assert.assertEquals(gav, m5.moduleGav.build());
            Assert.assertEquals(treeParentGav, m5.parentGav.build());
            Assert.assertEquals(Collections.emptyList(), m5.profiles.get(0).dependencies.stream()
                    .map(bu -> bu.build().toString()).collect(Collectors.toList()));
            Assert.assertEquals(Collections.emptySet(), m5.profiles.get(0).children);
            Assert.assertEquals(//
                    props(gav.getGa(), "prop1", "val-5").entrySet(), //
                    m5.profiles.get(0).properties.stream().map(PropertyBuilder::build)
                            .collect(Collectors.toCollection(LinkedHashSet::new)));
        }

        final MavenSourceTree t = b.build();
        Assert.assertEquals(new Expression.Constant("val-parent"),
                t.getRootModule().findPropertyDefinition("prop1", MavenSourceTree.profiles()).getValue());
        {
            final Module m8 = t.getModulesByGa().get(Ga.of("org.srcdeps.tree-1:tree-module-8"));
            Assert.assertEquals(new Expression.Constant("val-8/main"),
                    m8.findPropertyDefinition("prop2", MavenSourceTree.profiles()).getValue());
            Assert.assertEquals(new Expression.Constant("val-8/p1"),
                    m8.findPropertyDefinition("prop2", MavenSourceTree.profiles("p1")).getValue());
            Assert.assertEquals(new Expression.Constant("val-8/p2"),
                    m8.findPropertyDefinition("prop2", MavenSourceTree.profiles("p2")).getValue());
            Assert.assertEquals(new Expression.Constant("val-8/p2"),
                    m8.findPropertyDefinition("prop2", MavenSourceTree.profiles("p1", "p2")).getValue());
            Assert.assertEquals(new Expression.Constant("val-8/p2"),
                    m8.findPropertyDefinition("prop2", MavenSourceTree.profiles("p1", "p2")).getValue());
        }

        final Predicate<Profile> profileSelector = p -> true;
        final Set<Ga> expandedIncludes = t
                .computeModuleClosure(Arrays.asList(Ga.of("org.srcdeps.tree-1:tree-module-2")), profileSelector);
        Assert.assertEquals(Arrays
                .asList("org.srcdeps.tree-1:tree-module-2", "org.srcdeps.tree-1:tree-parent",
                        "org.srcdeps.tree-1:tree-module-4", "org.srcdeps.tree-1:tree-module-1",
                        "org.srcdeps.tree-1:tree-module-5", "org.srcdeps.tree-1:proper-parent",
                        "org.srcdeps.tree-1:tree-module-7", "org.srcdeps.tree-1:tree-module-8",
                        "org.srcdeps.tree-1:declared-parent", "org.srcdeps.tree-1:tree-plugin")
                .stream().map(Ga::of).collect(Collectors.toCollection(LinkedHashSet::new)), expandedIncludes);

        final Map<String, Set<String>> removeChildPaths = t.unlinkUneededModules(expandedIncludes, t.getRootModule(),
                new LinkedHashMap<String, Set<String>>(), profileSelector);
        Assert.assertEquals(1, removeChildPaths.size());
        Set<String> rootUnlinks = removeChildPaths.get("pom.xml");
        Assert.assertEquals(new LinkedHashSet<String>(Arrays.asList("module-3/pom.xml", "module-6/pom.xml")),
                rootUnlinks);

        t.unlinkUneededModules(expandedIncludes, profileSelector);

        final Path expectedRoot = BASEDIR.resolve("target/test-classes/MavenSourceTree/tree-1-expected");

        for (String path : removeChildPaths.keySet()) {
            final Path actualPath = root.resolve(path);
            final Path expectedPath = expectedRoot.resolve(path);
            Assert.assertEquals(new String(Files.readAllBytes(expectedPath), StandardCharsets.UTF_8),
                    new String(Files.readAllBytes(actualPath), StandardCharsets.UTF_8).replace("\r", ""));
        }
    }

}
