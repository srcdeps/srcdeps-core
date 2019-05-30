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
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Stack;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.srcdeps.core.util.SrcdepsCoreUtils;
import org.w3c.dom.Comment;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * A representtion of a Maven module hierarchy.
 *
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 * @since 4.1
 */
public class MavenSourceTree {

    /**
     * A {@link MavenSourceTree} builder.
     */
    static class Builder {
        static Module.Builder getParentModule(Map<String, Module.Builder> modulesByGa, Module.Builder child) {
            final String parentGa = child.getParentGa();
            if (parentGa != null) {
                return modulesByGa.get(parentGa);
            } else {
                return null;
            }
        }

        private final Charset encoding;

        final Map<String, Module.Builder> modulesByGa = new LinkedHashMap<>();

        /** By pom.xml path relative to {@link MavenSourceTree#rootDirectory} */
        final Map<String, Module.Builder> modulesByPath = new LinkedHashMap<>();

        private final Path rootDirectory;

        Builder(Path rootDirectory, Charset encoding) {
            super();
            this.rootDirectory = rootDirectory;
            this.encoding = encoding;
        }

        public MavenSourceTree build() {

            final Map<String, Module> byPath = new LinkedHashMap<>(modulesByPath.size());
            final Map<String, Module> byGa = new LinkedHashMap<>(modulesByPath.size());
            for (org.srcdeps.core.MavenSourceTree.Module.Builder e : modulesByPath.values()) {
                final Module module = e.build();
                byGa.put(module.ga, module);
                byPath.put(module.pomPath, module);
            }
            return new MavenSourceTree(rootDirectory, encoding, Collections.unmodifiableMap(byPath),
                    Collections.unmodifiableMap(byGa));
        }

        Builder pomXml(final Path pomXml) {
            final Module.Builder module = new Module.Builder(rootDirectory, pomXml, encoding);
            modulesByPath.put(module.pomPath, module);
            modulesByGa.put(module.getGa(), module);
            for (String path : module.children) {
                if (!modulesByPath.containsKey(path)) {
                    pomXml(rootDirectory.resolve(path));
                }
            }

            return this;
        }
    }

    /**
     * A Maven module.
     */
    public static class Module {

        /**
         * A {@link Module} builder.
         */
        static class Builder {
            String artifactId;
            Set<String> children = new LinkedHashSet<>();
            Set<String> dependencies = new LinkedHashSet<>();
            String groupId;
            Set<String> managedDependencies = new LinkedHashSet<>();
            String parentArtifactId;
            String parentGroupId;
            Set<String> plugins = new LinkedHashSet<>();
            /** Relative to source tree root directory */
            final String pomPath;

            Builder(Path rootDirectory, Path pomXml, Charset encoding) {

                final Stack<String> elementStack = new Stack<>();
                final Path dir = pomXml.getParent();
                try (Reader in = Files.newBufferedReader(pomXml, encoding)) {
                    final XMLEventReader r = xmlInputFactory.createXMLEventReader(in);
                    this.pomPath = SrcdepsCoreUtils.toUnixPath(rootDirectory.relativize(pomXml).toString());

                    String depArtifactId = null;
                    String depGroupId = null;
                    Set<String> depTarget = null;

                    while (r.hasNext()) {
                        final XMLEvent e = r.nextEvent();
                        if (e.isStartElement()) {
                            final String elementName = e.asStartElement().getName().getLocalPart();
                            final int elementStackSize = elementStack.size();
                            if ("module".equals(elementName) && r.hasNext()) {
                                final String relPath = r.nextEvent().asCharacters().getData() + "/pom.xml";
                                final Path childPomXml = dir.resolve(relPath).normalize();
                                final String rootRelPath = rootDirectory.relativize(childPomXml).toString();
                                children.add(SrcdepsCoreUtils.toUnixPath(rootRelPath));
                            } else if (elementStackSize >= 1) {
                                final String parentElement = elementStack.peek();
                                if ("parent".equals(parentElement)) {
                                    if ("artifactId".equals(elementName) && r.hasNext()) {
                                        parentArtifactId = r.nextEvent().asCharacters().getData();
                                    } else if ("groupId".equals(elementName) && r.hasNext()) {
                                        parentGroupId = r.nextEvent().asCharacters().getData();
                                    }
                                } else if ("dependencyManagement".equals(elementName)) {
                                    depTarget = managedDependencies;
                                } else if ("dependencies".equals(elementName) && depTarget == null) {
                                    depTarget = dependencies;
                                } else if ("plugins".equals(elementName)) {
                                    depTarget = plugins;
                                } else if (depTarget != null
                                        && ("dependency".equals(parentElement) || "plugin".equals(parentElement))) {
                                    if ("artifactId".equals(elementName) && r.hasNext()) {
                                        depArtifactId = r.nextEvent().asCharacters().getData();
                                    } else if ("groupId".equals(elementName) && r.hasNext()) {
                                        depGroupId = r.nextEvent().asCharacters().getData();
                                    }
                                } else if ("project".equals(parentElement)) {
                                    if ("artifactId".equals(elementName) && r.hasNext()) {
                                        artifactId = r.nextEvent().asCharacters().getData();
                                    } else if ("groupId".equals(elementName) && r.hasNext()) {
                                        groupId = r.nextEvent().asCharacters().getData();
                                    }
                                }
                            }
                            elementStack.push(elementName);
                        } else if (e.isEndElement()) {
                            final String elementName = elementStack.pop();
                            if (depTarget != null && depArtifactId != null && depGroupId != null
                                    && ("plugin".equals(elementName) || "dependency".equals(elementName))) {
                                depTarget.add(depGroupId + ":" + depArtifactId);
                                depArtifactId = null;
                                depGroupId = null;
                            } else if ("dependencyManagement".equals(elementName) && depTarget == managedDependencies) {
                                depTarget = null;
                            } else if ("dependencies".equals(elementName) && depTarget == dependencies) {
                                depTarget = null;
                            } else if ("plugins".equals(elementName) && depTarget == plugins) {
                                depTarget = null;
                            }
                        }
                    }
                } catch (IOException | XMLStreamException e1) {
                    throw new RuntimeException(e1);
                }
            }

            public Module build() {
                if ((parentGroupId == null) == (parentArtifactId != null)) {
                    throw new IllegalArgumentException(String.format(
                            "Malformed pom.xml: parent groupId and parent artifactId have to be both null or non-null. Path: [%s]",
                            pomPath));
                }
                final Set<String> useChildren = Collections.unmodifiableSet(children);
                children = null;
                final Set<String> useDependencies = Collections.unmodifiableSet(dependencies);
                dependencies = null;
                final Set<String> useManagedDependencies = Collections.unmodifiableSet(managedDependencies);
                managedDependencies = null;
                final Set<String> usePlugins = Collections.unmodifiableSet(plugins);
                plugins = null;
                return new Module(pomPath, getGa(), getParentGa(), useChildren, useDependencies, useManagedDependencies, usePlugins);
            }

            public String getGa() {
                return artifactId != null ? (groupId != null ? groupId : parentGroupId) + ":" + artifactId : null;
            }

            public String getParentGa() {
                return parentArtifactId != null && parentGroupId != null ? parentGroupId + ":" + parentArtifactId
                        : null;
            }
        }

        /** A path to child project's pom.xml relative to {@link MavenSourceTree#rootDirectory} */
        private final Set<String> children;
        private final Set<String> dependencies;
        private final String ga;
        private final Set<String> managedDependencies;
        private final String parentGa;
        private final Set<String> plugins;
        /** Relative to source tree root directory */
        private final String pomPath;

        Module(String pomPath, String ga, String parentGa, Set<String> children, Set<String> dependencies,
                Set<String> managedDependencies, Set<String> plugins) {
            super();
            this.pomPath = pomPath;
            this.ga = ga;
            this.parentGa = parentGa;
            this.children = children;
            this.dependencies = dependencies;
            this.managedDependencies = managedDependencies;
            this.plugins = plugins;
        }

        /**
         * @return a {@link Set} of paths to {@code pom.xml} files of child modules of this Module realtive to
         *         {@link MavenSourceTree#getRootDirectory()}
         */
        public Set<String> getChildren() {
            return children;
        }

        /**
         * @return a {@link Set} of {@code groupId:artifactId} identifiers
         */
        public Set<String> getDependencies() {
            return dependencies;
        }

        /**
         * @return the {@code groupId:artifactId} of this Maven module
         */
        public String getGa() {
            return ga;
        }

        public Set<String> getManagedDependencies() {
            return managedDependencies;
        }

        /**
         * @return the {@code groupId:artifactId} of the Maven parent module of this module or {@code null} if this
         *         module has no parent
         */
        public String getParentGa() {
            return parentGa;
        }

        /**
         * @return a {@link Set} of {@code groupId:artifactId} identifiers
         */
        public Set<String> getPlugins() {
            return plugins;
        }

        /**
         * @return a path to the this module's {@code pom.xml} relative to {@link MavenSourceTree#getRootModule()}
         */
        public String getPomPath() {
            return pomPath;
        }

    }

    private static final XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();

    /**
     * @param rootPomXml the path to the {@code pom.xml} file of the root Maven module
     * @param encoding the encoding to use when reading {@code pom.xml} files in the given file tree
     * @return a new {@link MavenSourceTree}
     */
    public static MavenSourceTree of(Path rootPomXml, Charset encoding) {
        return new Builder(rootPomXml.getParent(), encoding).pomXml(rootPomXml).build();
    }

    private final Charset encoding;

    private final Map<String, Module> modulesByGa;
    private final Map<String, Module> modulesByPath;
    private final Path rootDirectory;

    MavenSourceTree(Path rootDirectory, Charset encoding, Map<String, Module> modulesByPath,
            Map<String, Module> modulesByGa) {
        this.rootDirectory = rootDirectory;
        this.modulesByPath = modulesByPath;
        this.modulesByGa = modulesByGa;
        this.encoding = encoding;
    }

    private void addDeclaredParents(final Module module, final Set<String> result, Set<String> visited) {
        Module parent;
        Module child = module;
        while ((parent = getDeclaredParentModule(child)) != null) {
            addModule(parent.getGa(), result, visited);
            child = parent;
        }
    }

    private void addModule(String includeGa, Set<String> result, Set<String> visited) {
        final Module module = modulesByGa.get(includeGa);
        if (module != null && !visited.contains(includeGa)) {
            visited.add(includeGa);
            result.add(includeGa);
            addProperParents(module, result, visited);
            addDeclaredParents(module, result, visited);
            for (String depGa : module.dependencies) {
                addModule(depGa, result, visited);
            }
            for (String depGa : module.plugins) {
                addModule(depGa, result, visited);
            }
        }
    }

    private void addProperParents(final Module module, final Set<String> result, Set<String> visited) {
        Module parent;
        Module child = module;
        while ((parent = getProperParentModule(child)) != null) {
            addModule(parent.getGa(), result, visited);
            child = parent;
        }
    }

    /**
     * Returns a {@link Set} that contains all given {@code initialModules} and all such modules from the current
     * {@link MavenSourceTree} that are reachable from the {@code initialModules} via <i>depends on</i> and <i>is parent
     * of</i> relationships.
     *
     * @param initialModules
     * @return {@link Set} of {@code groupId:artifactId}
     */
    public Set<String> computeModuleClosure(Collection<String> initialModules) {
        final Set<String> visited = new HashSet<>();
        final Set<String> result = new LinkedHashSet<>();
        for (String includeGa : initialModules) {
            addModule(includeGa, result, visited);
        }
        return result;
    }

    Module getDeclaredParentModule(Module child) {
        final String parentGa = child.parentGa;
        if (parentGa != null) {
            return modulesByGa.get(parentGa);
        } else {
            return null;
        }
    }

    /**
     * @return a {@link Charset} to use when reading and writing the {@code pom.xml} files in this
     *         {@link MavenSourceTree}
     */
    public Charset getEncoding() {
        return encoding;
    }

    /**
     * @return a {@link Map} of modules in this {@link MavenSourceTree} by their {@code groupId:artifactId}
     */
    public Map<String, Module> getModulesByGa() {
        return modulesByGa;
    }

    /**
     * @return a {@link Map} of modules in this {@link MavenSourceTree} by their {@code pom.xml} path realtive to
     *         {@link #getRootDirectory()}
     */
    public Map<String, Module> getModulesByPath() {
        return modulesByPath;
    }

    /**
     * @param child
     * @return the {@link Module} having the given gild in its {@code <modules>}
     */
    Module getProperParentModule(Module child) {
        final String parentGa = child.parentGa;
        if (parentGa != null) {
            final Module declaredParent = modulesByGa.get(parentGa);
            if (declaredParent != null && declaredParent.children.contains(child.pomPath)) {
                return declaredParent;
            }
            return modulesByGa.values().stream().filter(m -> m.children.contains(child.pomPath)).findFirst()
                    .orElse(null);
        }
        return null;
    }

    /**
     * @return the root directory of this {@link MavenSourceTree}
     */
    public Path getRootDirectory() {
        return rootDirectory;
    }

    /**
     * @return the module in the directory returned by {@link #getRootDirectory()}
     */
    public Module getRootModule() {
        return modulesByPath.get("pom.xml");
    }

    /**
     * Edit the {@code pom.xml} files so that just the given @{@code includes} are buildable, removing all unnecessary
     * {@code <module>} elements from {@code pom.xml} files.
     *
     * @param includes a list of {@code groupId:artifactId}s
     */
    public void unlinkUneededModules(Set<String> includes) {
        final Module rootModule = modulesByPath.get("pom.xml");
        final Map<String, Set<String>> removeChildPaths = unlinkUneededModules(includes, rootModule,
                new LinkedHashMap<String, Set<String>>());
        TransformerFactory transformerFactory = null;
        XPathFactory xPathFactory = null;
        for (Entry<String, Set<String>> e : removeChildPaths.entrySet()) {
            if (transformerFactory == null) {
                transformerFactory = TransformerFactory.newInstance();
                xPathFactory = XPathFactory.newInstance();
            }
            unlinkUneededModules(transformerFactory, xPathFactory, rootDirectory.resolve(e.getKey()), e.getValue());
        }
    }

    Map<String, Set<String>> unlinkUneededModules(Set<String> includes, Module module,
            Map<String, Set<String>> removeChildPaths) {
        for (String childPath : module.children) {
            final Module childModule = modulesByPath.get(childPath);
            final String childGa = childModule.ga;
            if (!includes.contains(childGa)) {
                Set<String> set = removeChildPaths.get(module.pomPath);
                if (set == null) {
                    set = new LinkedHashSet<String>();
                    removeChildPaths.put(module.pomPath, set);
                }
                set.add(childPath);
            } else {
                unlinkUneededModules(includes, childModule, removeChildPaths);
            }
        }
        return removeChildPaths;
    }

    void unlinkUneededModules(TransformerFactory transformerFactory, XPathFactory xPathFactory, Path pomXml,
            Set<String> removeChildPaths) {
        try (Reader in = Files.newBufferedReader(pomXml, encoding)) {
            final DOMResult result = new DOMResult();
            transformerFactory.newTransformer().transform(new StreamSource(in), result);
            final Node pomXmlDocument = result.getNode();

            final XPath xPath = xPathFactory.newXPath();
            final NodeList moduleNodes = (NodeList) xPath.evaluate("//*[local-name()='module']", pomXmlDocument,
                    XPathConstants.NODESET);
            final Path dir = pomXml.getParent();
            for (int i = 0; i < moduleNodes.getLength(); i++) {
                final Node moduleNode = moduleNodes.item(i);
                final String moduleText = moduleNode.getTextContent();
                final Path childPath = dir.resolve(moduleText + "/pom.xml").normalize();
                final String rootRelChildPath = SrcdepsCoreUtils
                        .toUnixPath(rootDirectory.relativize(childPath).toString());
                if (removeChildPaths.contains(rootRelChildPath)) {
                    final Node parent = moduleNode.getParentNode();
                    final Comment moduleComment = moduleNode.getOwnerDocument()
                            .createComment(" <module>" + moduleText + "</module> removed by srcdeps ");
                    parent.replaceChild(moduleComment, moduleNode);
                }
            }
            try (Writer out = Files.newBufferedWriter(pomXml, encoding)) {
                transformerFactory.newTransformer().transform(new DOMSource(pomXmlDocument), new StreamResult(out));
            }
        } catch (IOException | TransformerException | TransformerFactoryConfigurationError
                | XPathExpressionException e) {
            throw new RuntimeException(String.format("Could not unlink children in [%s]", pomXml), e);
        }
    }

}
