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
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Stack;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
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

import org.srcdeps.core.MavenSourceTree.Expression.Constant;
import org.srcdeps.core.MavenSourceTree.Expression.NonConstant;
import org.srcdeps.core.MavenSourceTree.GavExpression.DependencyGavBuilder;
import org.srcdeps.core.MavenSourceTree.GavExpression.GavBuilder;
import org.srcdeps.core.MavenSourceTree.GavExpression.ModuleGavBuilder;
import org.srcdeps.core.MavenSourceTree.GavExpression.ParentGavBuilder;
import org.srcdeps.core.MavenSourceTree.Module.Profile;
import org.srcdeps.core.util.SrcdepsCoreUtils;
import org.w3c.dom.Comment;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * A representation of a Maven module hierarchy.
 *
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 * @since 4.1
 */
public class MavenSourceTree {

    public static class ActiveProfiles implements Predicate<Profile> {

        static final Predicate<Profile> EMPTY = new ActiveProfiles();

        /**
         * @param profileIds the active profiles (can be empty)
         * @return a new {@link Profile} filter which will hold the named {@code profileIds} for active
         */
        public static Predicate<Profile> of(String... profileIds) {
            return profileIds.length == 0 ? EMPTY : new ActiveProfiles(profileIds);
        }

        /**
         * @param args Maven command line arguments
         * @return a new {@link Predicate}
         */
        public static Predicate<Profile> ofArgs(List<String> args) {
            for (Iterator<String> it = args.iterator(); it.hasNext();) {
                final String arg = it.next();
                if ("-P".equals(arg) || "--activate-profiles".equals(arg)) {
                    return of(it.next().split(","));
                } else if (arg.startsWith("-P")) {
                    return of(arg.substring(2).split(","));
                }
            }
            return EMPTY;
        }

        final Set<String> profileIds;

        ActiveProfiles(String... profileIds) {
            super();
            Set<String> m = new LinkedHashSet<>(profileIds.length);
            for (String profileId : profileIds) {
                m.add(profileId);
            }
            this.profileIds = m;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            ActiveProfiles other = (ActiveProfiles) obj;
            if (profileIds == null) {
                if (other.profileIds != null)
                    return false;
            } else if (!profileIds.equals(other.profileIds))
                return false;
            return true;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((profileIds == null) ? 0 : profileIds.hashCode());
            return result;
        }

        @Override
        public boolean test(Profile t) {
            return t.getId() == null || profileIds.contains(t.getId());
        }

    }

    /**
     * A {@link MavenSourceTree} builder.
     */
    static class Builder {

        private final Charset encoding;

        final Map<Ga, Module.Builder> modulesByGa = new LinkedHashMap<>();

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
            final Map<Ga, Module> byGa = new LinkedHashMap<>(modulesByPath.size());
            for (org.srcdeps.core.MavenSourceTree.Module.Builder e : modulesByPath.values()) {
                final Module module = e.build();
                byGa.put(module.getGav().getGa(), module);
                byPath.put(module.pomPath, module);
            }
            return new MavenSourceTree(rootDirectory, encoding, Collections.unmodifiableMap(byPath),
                    Collections.unmodifiableMap(byGa));
        }

        Builder pomXml(final Path pomXml) {
            final Module.Builder module = new Module.Builder(rootDirectory, pomXml, encoding);
            modulesByPath.put(module.pomPath, module);
            modulesByGa.put(module.moduleGav.getGa(), module);
            for (Profile.Builder profile : module.profiles) {
                for (String path : profile.children) {
                    if (!modulesByPath.containsKey(path)) {
                        pomXml(rootDirectory.resolve(path));
                    }
                }
            }
            return this;
        }
    }

    /**
     * An edit operation on a text element of an XML {@link Document}.
     */
    static class DomEdit {
        private final String newValue;
        private final String selector;

        DomEdit(String selector, String newValue) {
            super();
            this.selector = selector;
            this.newValue = newValue;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            DomEdit other = (DomEdit) obj;
            if (newValue == null) {
                if (other.newValue != null)
                    return false;
            } else if (!newValue.equals(other.newValue))
                return false;
            if (selector == null) {
                if (other.selector != null)
                    return false;
            } else if (!selector.equals(other.selector))
                return false;
            return true;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((newValue == null) ? 0 : newValue.hashCode());
            result = prime * result + ((selector == null) ? 0 : selector.hashCode());
            return result;
        }

        /**
         * Performs this operation.
         *
         * @param xPath {@link XPath} to use when looking up {@link Node}s
         * @param document the {@link Document} to perform this operation on
         * @param path the file system path to the given {@code document}
         */
        public void perform(XPath xPath, Node document, String path) {
            try {
                final NodeList nodes = (NodeList) xPath.evaluate(selector, document, XPathConstants.NODESET);
                if (nodes == null || nodes.getLength() == 0) {
                    throw new IllegalStateException(
                            String.format("Xpath expression [%s] did not select any nodes in [%s]", selector, path));
                }
                for (int i = 0; i < nodes.getLength(); i++) {
                    nodes.item(i).setTextContent(newValue);
                }
            } catch (XPathExpressionException | DOMException e) {
                throw new IllegalStateException(String.format("Could not evaluate [%s] on [%s]", selector, path));
            }
        }
    }

    /**
     * A set of {@link DomEdit}s.
     */
    static class DomEdits {
        final Map<String, Set<DomEdit>> domEditsByPath = new LinkedHashMap<>();

        /**
         * @param path a file system path to a {@code pom.xml} file relative to {@link MavenSourceTree#rootDirectory}
         * @param domEdit the operation to add
         */
        public void add(String path, DomEdit domEdit) {
            Set<DomEdit> edits = domEditsByPath.get(path);
            if (edits == null) {
                domEditsByPath.put(path, edits = new LinkedHashSet<MavenSourceTree.DomEdit>());
            }
            edits.add(domEdit);
        }

        /**
         * Perform the operations added via {@link #add(String, DomEdit)}.
         *
         * @param rootDirectory
         * @param encoding
         */
        public void perform(Path rootDirectory, Charset encoding) {
            try {
                final Transformer transformer = TransformerFactory.newInstance().newTransformer();
                final XPath xPath = XPathFactory.newInstance().newXPath();
                for (Entry<String, Set<DomEdit>> e : domEditsByPath.entrySet()) {
                    final Path pomXml = rootDirectory.resolve(e.getKey());
                    final Node doc = readDom(transformer, pomXml, encoding);
                    for (DomEdit edit : e.getValue()) {
                        edit.perform(xPath, doc, e.getKey());
                    }
                    writeDom(transformer, pomXml, doc, encoding);
                }
            } catch (TransformerConfigurationException | TransformerFactoryConfigurationError e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * An expression used in Maven {@code pom.xml} files, such as <code>${my-property}</code> or
     * <code>my-prefix-${my-property}</code>
     */
    public interface Expression {

        /**
         * A constant containing no <code>${...}</code> placeholders.
         */
        class Constant implements Expression {
            final String expression;

            public Constant(String expression) {
                super();
                this.expression = expression;
            }

            @Override
            public boolean equals(Object obj) {
                if (this == obj)
                    return true;
                if (obj == null)
                    return false;
                if (getClass() != obj.getClass())
                    return false;
                return this.expression.equals(((Constant) obj).expression);
            }

            @Override
            public String evaluate(MavenSourceTree tree, Predicate<Profile> isProfileActive) {
                return expression;
            }

            public String getExpression() {
                return expression;
            }

            @Override
            public int hashCode() {
                return expression.hashCode();
            }

            @Override
            public String toString() {
                return expression;
            }

        }

        /**
         * An {@link Expression} containing <code>${...}</code> placeholders.
         */
        class NonConstant extends Constant {

            private static final Pattern PLACE_HOLDER_PATTERN = Pattern.compile("\\$\\{([^\\}]+)\\}");

            static void evaluateExpression(final NonConstant expression, final MavenSourceTree tree,
                    final Predicate<Profile> isProfileActive, final Consumer<ValueDefinition> consumer) {
                final Module context = tree.getModulesByGa().get(expression.ga);
                final String src = expression.expression;
                final Matcher m = PLACE_HOLDER_PATTERN.matcher(src);
                int offset = 0;
                while (m.find()) {
                    if (m.start() > offset) {
                        consumer.accept(new ValueDefinition(context, null,
                                new Expression.Constant(src.substring(offset, m.start()))));
                    }
                    final String propName = m.group(1);
                    evaluateProperty(tree, context, isProfileActive, propName, consumer);
                    offset = m.end();
                }
                if (offset < src.length()) {
                    consumer.accept(new ValueDefinition(context, null,
                            new Expression.Constant(src.substring(offset, src.length()))));
                }
            }

            static void evaluateProperty(final MavenSourceTree tree, final Module context,
                    final Predicate<Profile> isProfileActive, final String propertyName,
                    final Consumer<ValueDefinition> consumer) {

                if ("project.version".equals(propertyName)) {
                    consumer.accept(new ValueDefinition(context, PROJECT_VERSION_XPATH, context.getGav().getVersion()));
                } else {
                    final ValueDefinition propertyDefinition = context.findPropertyDefinition(propertyName,
                            isProfileActive);
                    if (propertyDefinition == null) {
                        /* No such property: climb up */
                        final Module parent = tree.getDeclaredParentModule(context);
                        if (parent == null) {
                            /* unable to resolve */
                            throw new IllegalStateException(String.format(
                                    "Unable to resolve property [%s]: root of the module hierarchy reached",
                                    propertyName));
                        } else {
                            evaluateProperty(tree, parent, isProfileActive, propertyName, consumer);
                        }
                    } else {
                        consumer.accept(propertyDefinition);
                    }

                }
            }

            /**
             * {@link Ga} of the module where the resolution of this {@link Expression} should start. For expressions
             * used in {@code <parent>} block, this would be the parent {@link Ga}. For all other cases, this is the
             * {@link Gav} of the {@code pom.xml} where this {@link Expression} occurs.
             */
            private final Ga ga;

            public NonConstant(String expression, Ga ga) {
                super(expression);
                this.ga = ga;
            }

            @Override
            public boolean equals(Object obj) {
                if (this == obj)
                    return true;
                if (!super.equals(obj))
                    return false;
                if (getClass() != obj.getClass())
                    return false;
                NonConstant other = (NonConstant) obj;
                if (ga == null) {
                    if (other.ga != null)
                        return false;
                } else if (!ga.equals(other.ga))
                    return false;
                return true;
            }

            /** {@inheritDoc} */
            @Override
            public String evaluate(final MavenSourceTree tree, final Predicate<Profile> isProfileActive) {

                final StringBuffer result = new StringBuffer();
                final Consumer<ValueDefinition> consumer = new Consumer<ValueDefinition>() {
                    @Override
                    public void accept(ValueDefinition propertyDefinition) {
                        final Expression propertyValue = propertyDefinition.getValue();
                        if (propertyValue instanceof Constant) {
                            result.append(propertyValue.evaluate(tree, isProfileActive));
                        } else if (propertyValue instanceof NonConstant) {
                            evaluateExpression((NonConstant) propertyValue, tree, isProfileActive, this);
                        } else {
                            throw new IllegalStateException();
                        }
                    }
                };

                evaluateExpression(this, tree, isProfileActive, consumer);
                return result.toString();
            }

            @Override
            public int hashCode() {
                final int prime = 31;
                int result = super.hashCode();
                result = prime * result + ((ga == null) ? 0 : ga.hashCode());
                return result;
            }

        }

        /**
         * @param expression the expression possibly containing <code>${...}</code> placeholders
         * @param ga the {@link Ga} against which the resulting {@link Expression} should be evaluated
         * @return a {@link NonConstant} or {@link Constant} depending on whether the given {@code expression} contains
         *         <code>${</code>
         */
        static Expression of(String expression, Ga ga) {
            if (expression.indexOf("${") >= 0) {
                return new NonConstant(expression, ga);
            } else {
                return new Constant(expression);
            }
        }

        /**
         * Evaluate this {@link Expression} by recursively expanding all ${...} placeholders.
         *
         * @param tree the {@link MavenSourceTree} against which this {@link Expression} should be evaluated
         * @param isProfileActive the profile selector to use when evaluating properties
         * @return the result of evaluation
         */
        String evaluate(MavenSourceTree tree, Predicate<Profile> isProfileActive);
    }

    /**
     * A {@link Ga} combined with a version {@link Expression}.
     */
    public static class GavExpression {

        public static class DependencyGavBuilder extends ParentGavBuilder {

            public static DependencyGavBuilder plugin(ModuleGavBuilder module) {
                DependencyGavBuilder result = new DependencyGavBuilder(module);
                result.groupId = "org.apache.maven.plugins";
                return result;
            }

            private final ModuleGavBuilder module;

            DependencyGavBuilder(ModuleGavBuilder module) {
                super();
                this.module = module;
            }

            public GavExpression build() {
                final Ga ga = new Ga(groupId, artifactId);
                return new GavExpression(ga, version != null ? Expression.of(version, module.getGa()) : null);
            }
        }

        interface GavBuilder {
            void artifactId(String artifactId);

            GavExpression build();

            void groupId(String groupId);

            void version(String version);

        }

        static class ModuleGavBuilder extends ParentGavBuilder {
            private Ga ga;
            private final ParentGavBuilder parent;

            ModuleGavBuilder(ParentGavBuilder parent) {
                super();
                this.parent = parent;
            }

            public GavExpression build() {
                final Ga ga = getGa();
                final Expression v = version != null ? Expression.of(version, ga) : parent.build().getVersion();
                return new GavExpression(ga, v);
            }

            public Ga getGa() {
                if (this.ga == null) {
                    final String g = groupId != null ? groupId : parent.groupId;
                    this.ga = new Ga(g, artifactId);
                }
                return this.ga;
            }
        }

        static class ParentGavBuilder implements GavBuilder {

            String artifactId;
            String groupId;
            String version;

            @Override
            public void artifactId(String artifactId) {
                this.artifactId = artifactId;
            }

            public GavExpression build() {
                final int sum = (groupId != null ? 1 : 0) + (artifactId != null ? 1 : 0) + (version != null ? 1 : 0);
                switch (sum) {
                case 0:
                    /* none of the three set */
                    return null;
                case 3:
                    final Ga ga = new Ga(groupId, artifactId);
                    return new GavExpression(ga, Expression.of(version, ga));
                default:
                    throw new IllegalStateException(String.format(
                            "groupId, artifactId and version must be all null or both not null: groupId: [%s], artifactId: [%s], version: [%s]",
                            groupId, artifactId, version));
                }
            }

            @Override
            public void groupId(String groupId) {
                this.groupId = groupId;
            }

            @Override
            public void version(String version) {
                this.version = version;
            }
        }

        private final Ga ga;
        private final Expression version;

        GavExpression(Ga ga, Expression version) {
            SrcdepsCoreUtils.assertArgNotNull(ga, "ga");
            this.ga = ga;
            this.version = version;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            GavExpression other = (GavExpression) obj;
            if (ga == null) {
                if (other.ga != null)
                    return false;
            } else if (!ga.equals(other.ga))
                return false;
            if (version == null) {
                if (other.version != null)
                    return false;
            } else if (!version.equals(other.version))
                return false;
            return true;
        }

        public Ga getGa() {
            return ga;
        }

        public Expression getVersion() {
            return version;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((ga == null) ? 0 : ga.hashCode());
            result = prime * result + ((version == null) ? 0 : version.hashCode());
            return result;
        }

        public Gav resolve(final MavenSourceTree tree, Predicate<Profile> isProfileActive) {
            return new Gav(ga.getGroupId(), ga.getArtifactId(), version.evaluate(tree, isProfileActive));
        }

        @Override
        public String toString() {
            return ga.getGroupId() + ":" + ga.getArtifactId() + ":" + version;
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
            Profile.Builder implicitProfile = new Profile.Builder();
            final ModuleGavBuilder moduleGav;
            final ParentGavBuilder parentGav;
            /** Relative to source tree root directory */
            final String pomPath;
            List<Profile.Builder> profiles;

            Builder(Path rootDirectory, Path pomXml, Charset encoding) {
                parentGav = new ParentGavBuilder();
                moduleGav = new ModuleGavBuilder(parentGav);
                profiles = new ArrayList<>();
                profiles.add(implicitProfile);

                final Stack<String> elementStack = new Stack<>();
                final Path dir = pomXml.getParent();
                try (Reader in = Files.newBufferedReader(pomXml, encoding)) {
                    final XMLEventReader r = xmlInputFactory.createXMLEventReader(in);
                    this.pomPath = SrcdepsCoreUtils.toUnixPath(rootDirectory.relativize(pomXml).toString());

                    GavExpression.GavBuilder gav = moduleGav;
                    Profile.Builder profile = implicitProfile;

                    while (r.hasNext()) {
                        final XMLEvent e = r.nextEvent();
                        if (e.isStartElement()) {
                            final String elementName = e.asStartElement().getName().getLocalPart();
                            final int elementStackSize = elementStack.size();
                            if ("parent".equals(elementName) && r.hasNext()) {
                                gav = parentGav;
                            } else if ("dependency".equals(elementName)) {
                                gav = new DependencyGavBuilder(moduleGav);
                                final String grandParent = elementStack.get(elementStackSize - 2);
                                if ("dependencyManagement".equals(grandParent)) {
                                    profile.dependencyManagement.add(gav);
                                } else if ("project".equals(grandParent)) {
                                    profile.dependencies.add(gav);
                                } else if ("plugin".equals(grandParent)) {
                                    // TODO
                                    throw new IllegalStateException(String
                                            .format("Unexpected grand parent of <dependency>: <%s>", grandParent));
                                } else {
                                    throw new IllegalStateException(String
                                            .format("Unexpected grand parent of <dependency>: <%s>", grandParent));
                                }
                            } else if ("plugin".equals(elementName)) {
                                gav = DependencyGavBuilder.plugin(moduleGav);
                                final String parentElement = elementStack.peek();
                                if ("plugins".equals(parentElement) && elementStack.size() > 1
                                        && "pluginManagement".equals(elementStack.get(elementStackSize - 2))) {
                                    profile.pluginManagement.add(gav);
                                } else if ("plugins".equals(parentElement)) {
                                    profile.plugins.add(gav);
                                } else {
                                    throw new IllegalStateException(
                                            String.format("Unexpected grand parent of <plugin>: <%s>", parentElement));
                                }
                            } else if ("module".equals(elementName)) {
                                final String relPath = r.nextEvent().asCharacters().getData() + "/pom.xml";
                                final Path childPomXml = dir.resolve(relPath).normalize();
                                final String rootRelPath = rootDirectory.relativize(childPomXml).toString();
                                profile.children.add(SrcdepsCoreUtils.toUnixPath(rootRelPath));
                            } else if (elementStackSize > 0 && "properties".equals(elementStack.peek())) {
                                profile.properties.add(new Profile.PropertyBuilder(elementName,
                                        r.nextEvent().asCharacters().getData(), moduleGav));
                            } else if ("profile".equals(elementName)) {
                                profile = new Profile.Builder();
                            } else if ("id".equals(elementName)) {
                                if ("profile".equals(elementStack.peek())) {
                                    final String id = r.nextEvent().asCharacters().getData();
                                    profile.id(id);
                                    profiles.add(profile);
                                }
                            } else if ("groupId".equals(elementName)) {
                                gav.groupId(r.nextEvent().asCharacters().getData());
                            } else if ("artifactId".equals(elementName)) {
                                gav.artifactId(r.nextEvent().asCharacters().getData());
                            } else if ("version".equals(elementName)) {
                                gav.version(r.nextEvent().asCharacters().getData());
                            }
                            elementStack.push(elementName);
                        } else if (e.isEndElement()) {
                            final String elementName = elementStack.pop();
                            if ("parent".equals(elementName)) {
                                gav = moduleGav;
                            } else if ("profile".equals(elementName)) {
                                profile = implicitProfile;
                            }
                        }
                    }
                } catch (IOException | XMLStreamException e1) {
                    throw new RuntimeException(e1);
                }
            }

            public Module build() {
                final List<Profile> useProfiles = Collections
                        .unmodifiableList(profiles.stream().map(Profile.Builder::build).collect(Collectors.toList()));
                profiles = null;
                return new Module(pomPath, moduleGav.build(), parentGav.build(), useProfiles);
            }

        }

        /**
         * A Maven profile.
         */
        public static class Profile {

            /**
             * A Maven {@link Profile} builder.
             */
            public static class Builder {
                Set<String> children = new LinkedHashSet<>();
                List<GavBuilder> dependencies = new ArrayList<>();
                List<GavBuilder> dependencyManagement = new ArrayList<>();
                private String id;
                List<GavBuilder> pluginManagement = new ArrayList<>();
                List<GavBuilder> plugins = new ArrayList<>();
                List<PropertyBuilder> properties = new ArrayList<>();

                public Profile build() {
                    final Set<String> useChildren = Collections.unmodifiableSet(children);
                    children = null;
                    final Set<GavExpression> useDependencies = Collections.unmodifiableSet(dependencies.stream()
                            .map(GavBuilder::build).collect(Collectors.toCollection(LinkedHashSet::new)));
                    dependencies = null;
                    final Set<GavExpression> useManagedDependencies = Collections.unmodifiableSet(dependencyManagement
                            .stream().map(GavBuilder::build).collect(Collectors.toCollection(LinkedHashSet::new)));
                    dependencyManagement = null;
                    final Set<GavExpression> usePlugins = Collections.unmodifiableSet(plugins.stream()
                            .map(GavBuilder::build).collect(Collectors.toCollection(LinkedHashSet::new)));
                    plugins = null;
                    final Set<GavExpression> usePluginManagement = Collections.unmodifiableSet(pluginManagement.stream()
                            .map(GavBuilder::build).collect(Collectors.toCollection(LinkedHashSet::new)));
                    pluginManagement = null;
                    final Map<String, Expression> useProps = Collections.unmodifiableMap(properties.stream() //
                            .map(PropertyBuilder::build) //
                            .collect( //
                                    Collectors.toMap( //
                                            e -> e.getKey(), //
                                            e -> e.getValue(), //
                                            (u, v) -> {
                                                throw new IllegalStateException(String.format("Duplicate key %s", u));
                                            }, //
                                            LinkedHashMap::new //
                                    ) //
                            ) //
                    );
                    this.properties = null;
                    return new Profile(id, useChildren, useDependencies, useManagedDependencies, usePlugins,
                            usePluginManagement, useProps);
                }

                public void id(String id) {
                    this.id = id;
                }
            }

            static class PropertyBuilder {
                final GavBuilder ga;

                final String key;
                final String value;

                PropertyBuilder(String key, String value, GavBuilder ga) {
                    super();
                    this.key = key;
                    this.value = value;
                    this.ga = ga;
                }

                public Map.Entry<String, Expression> build() {
                    final Expression val = Expression.of(value, ga.build().getGa());
                    return new AbstractMap.SimpleImmutableEntry<>(key, val);
                }
            }

            /** A path to child project's pom.xml relative to {@link MavenSourceTree#rootDirectory} */
            private final Set<String> children;
            private final Set<GavExpression> dependencies;
            private final Set<GavExpression> dependencyManagement;
            private final String id;
            private final Set<GavExpression> pluginManagement;

            private final Set<GavExpression> plugins;
            private final Map<String, Expression> properties;

            Profile(String id, Set<String> children, Set<GavExpression> dependencies,
                    Set<GavExpression> dependencyManagement, Set<GavExpression> plugins,
                    Set<GavExpression> pluginManagement, Map<String, Expression> properties) {
                super();
                this.id = id;
                this.children = children;
                this.dependencies = dependencies;
                this.dependencyManagement = dependencyManagement;
                this.plugins = plugins;
                this.pluginManagement = pluginManagement;
                this.properties = properties;
            }

            /**
             * @return a {@link Set} of paths to {@code pom.xml} files of child modules of this Module realtive to
             *         {@link MavenSourceTree#getRootDirectory()}
             */
            public Set<String> getChildren() {
                return children;
            }

            /**
             * @return a {@link Set} of dependencies declared in this {@link Profile}
             */
            public Set<GavExpression> getDependencies() {
                return dependencies;
            }

            /**
             * @return a {@link Set} of dependencyManagement entries declared in this {@link Profile}
             */
            public Set<GavExpression> getDependencyManagement() {
                return dependencyManagement;
            }

            /**
             * @return an ID of this profile or {@code null} if this is the representation of a top level profile-less
             *         dependencies, plugins, etc.
             */
            public String getId() {
                return id;
            }

            /**
             * @return a {@link Set} of pluginManagement entries declared in this {@link Profile}
             */
            public Set<GavExpression> getPluginManagement() {
                return pluginManagement;
            }

            /**
             * @return a {@link Set} of plugins declared in this {@link Profile}
             */
            public Set<GavExpression> getPlugins() {
                return plugins;
            }

            /**
             * @return the {@link Map} of properties declared in this {@link Profile}
             */
            public Map<String, Expression> getProperties() {
                return properties;
            }

        }

        private final GavExpression gav;
        private final GavExpression parentGav;
        /** Relative to source tree root directory */
        private final String pomPath;

        private final List<Profile> profiles;

        Module(String pomPath, GavExpression gav, GavExpression parentGa, List<Profile> profiles) {
            super();
            this.pomPath = pomPath;
            this.gav = gav;
            this.parentGav = parentGa;
            this.profiles = profiles;
        }

        /**
         * Goes through active profiles and find the definition of the property having the given {@code propertyName}.
         *
         * @param propertyName the property name to find a definition for
         * @param isProfileActive tells which profiles are active
         * @return the {@link ValueDefinition} of the seeked property or {@code null} if no such property is defined in
         *         this {@link Module}
         */
        public ValueDefinition findPropertyDefinition(String propertyName, Predicate<Profile> isProfileActive) {
            final ListIterator<Profile> it = this.profiles.listIterator(this.profiles.size());
            while (it.hasPrevious()) {
                final Profile p = it.previous();
                if (isProfileActive.test(p)) {
                    final Expression result = p.properties.get(propertyName);
                    if (result != null) {
                        final String xPath = xPathProfile(p.getId(), "properties", propertyName);
                        return new ValueDefinition(this, xPath, result);
                    }
                }
            }
            return null;
        }

        /**
         * @return the {@link GavExpression} of this Maven module
         */
        public GavExpression getGav() {
            return gav;
        }

        /**
         * @return the {@link GavExpression} of the Maven parent module of this module or {@code null} if this module
         *         has no parent
         */
        public GavExpression getParentGav() {
            return parentGav;
        }

        /**
         * @return a path to the this module's {@code pom.xml} relative to {@link MavenSourceTree#getRootModule()}
         */
        public String getPomPath() {
            return pomPath;
        }

        /**
         * @return the {@link List} of profiles defined in this {@link Module}. Note that the top level profile-less
         *         dependencies, dependencyManagement, etc. are defined in {@link Module} with {@code id} {@code null}.
         */
        public List<Profile> getProfiles() {
            return profiles;
        }

        /**
         * @param childPomPath a path to {@code pom.xml} file relative to {@link MavenSourceTree#rootDirectory}
         * @param isProfileActive
         * @return {@code true} if the {@code pom.xml} represented by this {@link Module} has a {@code <module>} with
         *         the given pom.xml path or {@code false} otherwise
         */
        public boolean hasChild(String childPomPath, Predicate<Profile> isProfileActive) {
            for (Profile p : profiles) {
                if (isProfileActive.test(p)) {
                    if (p.children.contains(childPomPath)) {
                        return true;
                    }
                }
            }
            return false;
        }

    }

    /**
     * Decides which {@link ValueDefinition}s delivered via {@link SimplePlaceHolderConsumer#accept(ValueDefinition)}
     * are relevant for setting a new version and eventually adds a new {@link DomEdit} operation to
     * {@link SimplePlaceHolderConsumer#edits}.
     */
    static class SimplePlaceHolderConsumer implements Consumer<ValueDefinition> {

        int counter = 0;

        final DomEdits edits;
        final String newValue;

        SimplePlaceHolderConsumer(DomEdits edits, String newValue) {
            super();
            this.edits = edits;
            this.newValue = newValue;
        }

        @Override
        public void accept(ValueDefinition valueDefinition) {
            if (counter++ > 0) {
                throw new IllegalStateException(String.format("Cannot call [%s] more than once",
                        SimplePlaceHolderConsumer.class.getSimpleName()));
            }
            if (valueDefinition.getXPath() == null) {
                throw new IllegalStateException(String.format("[%s] cannot accept a value without an xPath",
                        SimplePlaceHolderConsumer.class.getSimpleName()));
            } else if (PROJECT_VERSION_XPATH.equals(valueDefinition.getXPath())) {
                /* ignore */
            } else {
                edits.add(valueDefinition.getModule().getPomPath(), new DomEdit(valueDefinition.getXPath(), newValue));
            }
        }

    }

    /**
     * Where some {@link Expression}'s value is defined - a semi-result of an evaluation of some {@link Expression}.
     */
    static class ValueDefinition {
        private final Module module;
        private final Expression value;
        private final String xPath;

        ValueDefinition(Module module, String xPath, Expression value) {
            super();
            this.module = module;
            this.xPath = xPath;
            this.value = value;
        }

        /**
         * @return the {@link Module} in which {@link #value} is defined
         */
        public Module getModule() {
            return module;
        }

        /**
         * @return the value of the {@link Expression}
         */
        public Expression getValue() {
            return value;
        }

        /**
         * @return an XPath expression pointing at the element where the {@link #value} is defined
         */
        public String getXPath() {
            return xPath;
        }
    }

    private static final String PROJECT_VERSION_XPATH = "/*[local-name()='project']/*[local-name()='version']";

    private static final XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();

    /**
     * @param rootPomXml the path to the {@code pom.xml} file of the root Maven module
     * @param encoding the encoding to use when reading {@code pom.xml} files in the given file tree
     * @return a new {@link MavenSourceTree}
     */
    public static MavenSourceTree of(Path rootPomXml, Charset encoding) {
        return new Builder(rootPomXml.getParent(), encoding).pomXml(rootPomXml).build();
    }

    static Node readDom(Transformer transformer, final Path pomXml, final Charset encoding) {
        try (Reader in = Files.newBufferedReader(pomXml, encoding)) {
            final DOMResult result = new DOMResult();
            transformer.transform(new StreamSource(in), result);
            return result.getNode();
        } catch (IOException | TransformerException | TransformerFactoryConfigurationError e) {
            throw new RuntimeException(String.format("Could not read DOM from [%s]", pomXml), e);
        }
    }

    static void writeDom(Transformer transformer, Path pomXml, final Node pomXmlDocument, Charset encoding) {
        try (Writer out = Files.newBufferedWriter(pomXml, encoding)) {
            transformer.transform(new DOMSource(pomXmlDocument), new StreamResult(out));
        } catch (IOException | TransformerException | TransformerFactoryConfigurationError e) {
            throw new RuntimeException(String.format("Could not write DOM to [%s]", pomXml), e);
        }
    }

    static String xPath(String... elements) {
        final StringBuilder result = new StringBuilder();
        for (String e : elements) {
            result.append("/*[local-name()='").append(e).append("']");
        }
        return result.toString();
    }

    static String xPathDependencyVersion(String dependencyKind, Ga ga) {
        return "/*[local-name()='" + dependencyKind + "' and *[local-name()='groupId' and text()='" + ga.getGroupId()
                + "'] and *[local-name()='artifactId' and text()='" + ga.getArtifactId()
                + "']]/*[local-name()='version']";
    }

    static String xPathProfile(String id, String... elements) {
        return "/*[local-name()='project']" + (id == null ? ""
                : "/*[local-name()='profiles']/*[local-name()='profile' and *[local-name()='id' and text()='" + id
                        + "']]")
                + xPath(elements);
    }

    private final Charset encoding;

    private final Map<Ga, Module> modulesByGa;

    private final Map<String, Module> modulesByPath;

    private final Path rootDirectory;

    MavenSourceTree(Path rootDirectory, Charset encoding, Map<String, Module> modulesByPath,
            Map<Ga, Module> modulesByGa) {
        this.rootDirectory = rootDirectory;
        this.modulesByPath = modulesByPath;
        this.modulesByGa = modulesByGa;
        this.encoding = encoding;
    }

    private void addDeclaredParents(final Module module, final Set<Ga> result, Set<Ga> visited,
            Predicate<Profile> isProfileActive) {
        Module parent;
        Module child = module;
        while ((parent = getDeclaredParentModule(child)) != null) {
            addModule(parent.getGav().getGa(), result, visited, isProfileActive);
            child = parent;
        }
    }

    private void addModule(Ga includeGa, Set<Ga> result, Set<Ga> visited, Predicate<Profile> isProfileActive) {
        final Module module = modulesByGa.get(includeGa);
        if (module != null && !visited.contains(includeGa)) {
            visited.add(includeGa);
            result.add(includeGa);
            addProperParents(module, result, visited, isProfileActive);
            addDeclaredParents(module, result, visited, isProfileActive);
            for (Profile p : module.profiles) {
                if (isProfileActive.test(p)) {
                    for (GavExpression depGa : p.dependencies) {
                        addModule(depGa.getGa(), result, visited, isProfileActive);
                    }
                    for (GavExpression depGa : p.plugins) {
                        addModule(depGa.getGa(), result, visited, isProfileActive);
                    }
                }
            }
        }
    }

    private void addProperParents(final Module module, final Set<Ga> result, Set<Ga> visited,
            Predicate<Profile> isProfileActive) {
        Module parent;
        Module child = module;
        while ((parent = getProperParentModule(child, isProfileActive)) != null) {
            addModule(parent.getGav().getGa(), result, visited, isProfileActive);
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
    public Set<Ga> computeModuleClosure(Collection<Ga> initialModules, Predicate<Profile> isProfileActive) {
        final Set<Ga> visited = new HashSet<>();
        final Set<Ga> result = new LinkedHashSet<>();
        for (Ga includeGa : initialModules) {
            addModule(includeGa, result, visited, isProfileActive);
        }
        return result;
    }

    void edit(final String newVersion, final Predicate<Profile> isProfileActive, final DomEdits edits, Module module,
            final Expression moduleVersion, String xPath) {
        if (moduleVersion instanceof NonConstant) {
            NonConstant.evaluateExpression((NonConstant) moduleVersion, this, isProfileActive,
                    new SimplePlaceHolderConsumer(edits, newVersion));
        } else if (moduleVersion instanceof Constant) {
            edits.add(module.getPomPath(), new DomEdit(xPath, newVersion));
        }
    }

    void edit(String newVersion, final Predicate<Profile> isProfileActive, final DomEdits edits, Module module,
            String profileId, Set<GavExpression> dependencies, String dependencyKind, String... path) {
        for (GavExpression gav : dependencies) {
            if (gav.getVersion() != null && modulesByGa.containsKey(gav.getGa())) {
                final String xPath = xPathProfile(profileId, path)
                        + xPathDependencyVersion(dependencyKind, gav.getGa());
                edit(newVersion, isProfileActive, edits, module, gav.getVersion(), xPath);
            }
        }
    }

    /**
     * Evaluate the given {@link Expression} by recursively expanding all its ${...} placeholders.
     *
     * @param expression the {@link Expression} to evaluate
     * @param isProfileActive the profile selector to use when evaluating properties
     * @return the result of evaluation
     */
    public String evaluate(Expression expression, Predicate<Profile> isProfileActive) {
        return expression.evaluate(this, isProfileActive);
    }

    /**
     * Goes through dependencies and plugins this source tree requires and returns the set of those satisfying
     * {@link GavSet#contains(String, String)}.
     *
     * @param gavSet
     * @param isProfileActive
     * @return a
     */
    public Set<Ga> filterDependencies(GavSet gavSet, final Predicate<Profile> isProfileActive) {
        final Set<Ga> result = new TreeSet<>();
        for (Module module : getModulesByGa().values()) {
            final GavExpression parentGav = module.getParentGav();
            if (parentGav != null) {
                final Ga ga = parentGav.getGa();
                if (gavSet.contains(ga.getGroupId(), ga.getArtifactId())) {
                    result.add(ga);
                }
            }
            for (Profile p : module.getProfiles()) {
                if (isProfileActive.test(p)) {
                    for (GavExpression depGa : p.getDependencies()) {
                        final Ga ga = depGa.getGa();
                        if (gavSet.contains(ga.getGroupId(), ga.getArtifactId())) {
                            result.add(ga);
                        }
                    }
                    for (GavExpression depGa : p.getPlugins()) {
                        final Ga ga = depGa.getGa();
                        if (gavSet.contains(ga.getGroupId(), ga.getArtifactId())) {
                            result.add(ga);
                        }
                    }
                }
            }
        }
        return result;
    }

    Module getDeclaredParentModule(Module child) {
        final GavExpression parentGa = child.parentGav;
        if (parentGa != null) {
            return modulesByGa.get(parentGa.getGa());
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
    public Map<Ga, Module> getModulesByGa() {
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
    Module getProperParentModule(Module child, Predicate<Profile> isProfileActive) {
        final GavExpression parentGa = child.parentGav;
        if (parentGa != null) {
            final Module declaredParent = modulesByGa.get(parentGa.getGa());
            if (declaredParent != null && declaredParent.hasChild(child.pomPath, isProfileActive)) {
                return declaredParent;
            }
            return modulesByGa.values().stream().filter(m -> m.hasChild(child.pomPath, isProfileActive)).findFirst()
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
     * A fast alternative to {@code mvn versions:set -DnewVersion=...}
     *
     * @param newVersion the new version to set
     * @param isProfileActive a {@link Profile} filter, see {@link #profiles(String...)}
     */
    public void setVersions(final String newVersion, final Predicate<Profile> isProfileActive) {
        final DomEdits edits = new DomEdits();
        for (Module module : modulesByGa.values()) {

            /* self */
            final GavExpression parentGav = module.getParentGav();
            final Expression moduleVersion = module.getGav().getVersion();
            if (parentGav == null || !moduleVersion.equals(module.getParentGav().getVersion())) {
                /* explicitly defined version */
                edit(newVersion, isProfileActive, edits, module, moduleVersion, xPath("project", "version"));
            }

            /* parent */
            if (parentGav != null) {
                final Expression parentVersion = parentGav.getVersion();
                edit(newVersion, isProfileActive, edits, module, parentVersion, xPath("project", "parent", "version"));
            }

            for (Profile profile : module.getProfiles()) {
                /* dependencyManagement */
                edit(newVersion, isProfileActive, edits, module, profile.getId(), profile.getDependencyManagement(),
                        "dependency", "dependencyManagement", "dependencies");
                edit(newVersion, isProfileActive, edits, module, profile.getId(), profile.getDependencies(),
                        "dependency", "dependencies");
                edit(newVersion, isProfileActive, edits, module, profile.getId(), profile.getPluginManagement(),
                        "plugin", "build", "pluginManagement", "plugins");
                edit(newVersion, isProfileActive, edits, module, profile.getId(), profile.getPlugins(), "plugin",
                        "build", "plugins");
            }
        }
        edits.perform(rootDirectory, encoding);
    }

    Map<String, Set<String>> unlinkUneededModules(Set<Ga> includes, Module module,
            Map<String, Set<String>> removeChildPaths, Predicate<Profile> isProfileActive) {
        for (Profile p : module.profiles) {
            if (isProfileActive.test(p)) {
                for (String childPath : p.children) {
                    final Module childModule = modulesByPath.get(childPath);
                    final GavExpression childGa = childModule.gav;
                    if (!includes.contains(childGa.getGa())) {
                        Set<String> set = removeChildPaths.get(module.pomPath);
                        if (set == null) {
                            set = new LinkedHashSet<String>();
                            removeChildPaths.put(module.pomPath, set);
                        }
                        set.add(childPath);
                    } else {
                        unlinkUneededModules(includes, childModule, removeChildPaths, isProfileActive);
                    }
                }
            }
        }
        return removeChildPaths;
    }

    /**
     * Edit the {@code pom.xml} files so that just the given @{@code includes} are buildable, removing all unnecessary
     * {@code <module>} elements from {@code pom.xml} files.
     *
     * @param includes a list of {@code groupId:artifactId}s
     * @param isProfileActive a {@link Profile} filter, see {@link #profiles(String...)}
     */
    public void unlinkUneededModules(Set<Ga> includes, Predicate<Profile> isProfileActive) {
        final Module rootModule = modulesByPath.get("pom.xml");
        final Map<String, Set<String>> removeChildPaths = unlinkUneededModules(includes, rootModule,
                new LinkedHashMap<String, Set<String>>(), isProfileActive);
        Transformer transformer = null;
        XPathFactory xPathFactory = null;
        for (Entry<String, Set<String>> e : removeChildPaths.entrySet()) {
            if (transformer == null) {
                try {
                    transformer = TransformerFactory.newInstance().newTransformer();
                } catch (TransformerConfigurationException | TransformerFactoryConfigurationError e1) {
                    throw new RuntimeException(e1);
                }
                xPathFactory = XPathFactory.newInstance();
            }
            unlinkUneededModules(transformer, xPathFactory, rootDirectory.resolve(e.getKey()), e.getValue());
        }
    }

    void unlinkUneededModules(Transformer transformer, XPathFactory xPathFactory, Path pomXml,
            Set<String> removeChildPaths) {
        final Node pomXmlDocument = readDom(transformer, pomXml, encoding);
        final XPath xPath = xPathFactory.newXPath();
        try {
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
            writeDom(transformer, pomXml, pomXmlDocument, encoding);
        } catch (XPathExpressionException | DOMException e) {
            throw new RuntimeException(String.format("Could not transform [%s]", pomXml), e);
        }
    }

}
