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
package org.srcdeps.core.config;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import org.srcdeps.core.BuildRequest.Verbosity;
import org.srcdeps.core.config.tree.ListOfScalarsNode;
import org.srcdeps.core.config.tree.Node;
import org.srcdeps.core.config.tree.ScalarNode;
import org.srcdeps.core.config.tree.Visitor;
import org.srcdeps.core.config.tree.impl.DefaultContainerNode;
import org.srcdeps.core.config.tree.impl.DefaultListOfScalarsNode;
import org.srcdeps.core.config.tree.impl.DefaultScalarNode;
import org.srcdeps.core.config.tree.walk.TreeWalker;

/**
 * A configuration suitable for a build system wanting to handle source dependencies.
 *
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */
public class Configuration {
    public static class Builder extends DefaultContainerNode<Node> {

        final BuilderIo.Builder builderIo = BuilderIo.builder();
        final ScalarNode<String> configModelVersion = new DefaultScalarNode<>("configModelVersion",
                LATEST_CONFIG_MODEL_VERSION);
        final ListOfScalarsNode<String> forwardProperties = new DefaultListOfScalarsNode<String>("forwardProperties",
                String.class) {

            @Override
            public void applyDefaultsAndInheritance(Stack<Node> configurationStack) {
                if (getElements().isEmpty()) {
                    addAll(DEFAULT_FORWARD_PROPERTIES);
                }
            }

            @Override
            public boolean isInDefaultState(Stack<Node> configurationStack) {
                return getElements().isEmpty() || DEFAULT_FORWARD_PROPERTIES.equals(asSetOfValues());
            }

        };
        final Maven.Builder maven = Maven.builder();
        final DefaultContainerNode<ScmRepository.Builder> repositories = new DefaultContainerNode<>("repositories");
        final ScalarNode<Boolean> skip = new DefaultScalarNode<>("skip", Boolean.FALSE);
        final ScalarNode<Path> sourcesDirectory = new DefaultScalarNode<>("sourcesDirectory", Path.class);
        final ScalarNode<Verbosity> verbosity = new DefaultScalarNode<>("verbosity", Verbosity.warn);

        private Builder() {
            super("srcdeps");
            addChildren( //
                    configModelVersion, //
                    forwardProperties, //
                    builderIo, //
                    skip, //
                    sourcesDirectory, //
                    verbosity, //
                    maven, //
                    repositories //
            );
        }

        public Builder accept(Visitor visitor) {
            new TreeWalker().walk(this, visitor);
            return this;
        }

        public Configuration build() {
            Collection<ScmRepository.Builder> repoBuilders = repositories.getChildren().values();
            List<ScmRepository> repos = new ArrayList<>(repoBuilders.size());
            for (ScmRepository.Builder repoBuilder : repoBuilders) {
                repos.add(repoBuilder.build());
            }

            Configuration result = new Configuration( //
                    configModelVersion.getValue(), //
                    Collections.unmodifiableList(repos), //
                    sourcesDirectory.getValue(), //
                    skip.getValue(), //
                    verbosity.getValue(), //
                    builderIo.build(), //
                    forwardProperties.asSetOfValues(), //
                    maven.build() //
            );
            return result;
        }

        public Builder builderIo(BuilderIo.Builder builderIo) {
            this.builderIo.init(builderIo);
            return this;
        }

        public Builder configModelVersion(String configModelVersion) {
            if (!SUPPORTED_CONFIG_MODEL_VERSIONS.contains(configModelVersion)) {
                throw new IllegalArgumentException(
                        String.format("Cannot parse configModelVersion [%s]; expected any of [%s]", configModelVersion,
                                SUPPORTED_CONFIG_MODEL_VERSIONS));
            }
            this.configModelVersion.setValue(configModelVersion);
            return this;
        }

        public Builder forwardProperties(Collection<String> values) {
            forwardProperties.addAll(values);
            return this;
        }

        public Builder forwardProperty(String value) {
            forwardProperties.add(value);
            return this;
        }

        @Override
        public Map<String, Node> getChildren() {
            return children;
        }

        public Builder maven(Maven.Builder maven) {
            this.maven.init(maven);
            return this;
        }

        public Builder repositories(Map<String, ScmRepository.Builder> repoBuilders) {
            for (Map.Entry<String, ScmRepository.Builder> en : repoBuilders.entrySet()) {
                ScmRepository.Builder repoBuilder = en.getValue();
                repoBuilder.id(en.getKey());
                this.repositories.addChild(repoBuilder);
            }
            return this;
        }

        public Builder repository(ScmRepository.Builder repo) {
            this.repositories.addChild(repo);
            return this;
        }

        public Builder skip(boolean value) {
            this.skip.setValue(value);
            return this;
        }

        public Builder sourcesDirectory(Path value) {
            this.sourcesDirectory.setValue(value);
            return this;
        }

        public Builder verbosity(Verbosity value) {
            this.verbosity.setValue(value);
            return this;
        }

    }

    private static final Set<String> DEFAULT_FORWARD_PROPERTIES = Collections
            .unmodifiableSet(new LinkedHashSet<>(Arrays.asList("srcdeps.mvn.*")));

    private static final String LATEST_CONFIG_MODEL_VERSION = "2.0";

    public static final String SRCDEPS_MVN_SETTINGS_PROP = "srcdeps.mvn.settings";

    public static final Set<String> SUPPORTED_CONFIG_MODEL_VERSIONS = Collections
            .unmodifiableSet(new LinkedHashSet<>(Arrays.asList(LATEST_CONFIG_MODEL_VERSION)));

    public static Builder builder() {
        return new Builder();
    }

    public static Set<String> getDefaultForwardProperties() {
        return DEFAULT_FORWARD_PROPERTIES;
    }

    public static String getLatestConfigModelVersion() {
        return LATEST_CONFIG_MODEL_VERSION;
    }

    private final BuilderIo builderIo;
    private final String configModelVersion;
    private final Set<String> forwardProperties;
    private final Maven maven;
    private final List<ScmRepository> repositories;

    private final boolean skip;

    private final Path sourcesDirectory;

    private final Verbosity verbosity;

    private Configuration(String configModelVersion, List<ScmRepository> repositories, Path sourcesDirectory,
            boolean skip, Verbosity verbosity, BuilderIo redirects, Set<String> forwardProperties, Maven maven) {
        super();
        this.configModelVersion = configModelVersion;
        this.repositories = repositories;
        this.sourcesDirectory = sourcesDirectory;
        this.skip = skip;
        this.verbosity = verbosity;
        this.forwardProperties = forwardProperties;
        this.builderIo = redirects;
        this.maven = maven;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Configuration other = (Configuration) obj;
        if (builderIo == null) {
            if (other.builderIo != null)
                return false;
        } else if (!builderIo.equals(other.builderIo))
            return false;
        if (forwardProperties == null) {
            if (other.forwardProperties != null)
                return false;
        } else if (!forwardProperties.equals(other.forwardProperties))
            return false;
        if (maven == null) {
            if (other.maven != null)
                return false;
        } else if (!maven.equals(other.maven))
            return false;
        if (repositories == null) {
            if (other.repositories != null)
                return false;
        } else if (!repositories.equals(other.repositories))
            return false;
        if (skip != other.skip)
            return false;
        if (sourcesDirectory == null) {
            if (other.sourcesDirectory != null)
                return false;
        } else if (!sourcesDirectory.equals(other.sourcesDirectory))
            return false;
        if (verbosity != other.verbosity)
            return false;
        return true;
    }

    /**
     * @return a {@link BuilderIo} to use for the child builders
     */
    public BuilderIo getBuilderIo() {
        return builderIo;
    }

    public String getConfigModelVersion() {
        return configModelVersion;
    }

    /**
     * Returns a set of property names that the top level builder A should pass as java system properties to every
     * dependency builder B (using {@code -DmyProperty=myValue}) command line arguments. Further, in case a child
     * builder B spawns its own new child builder C, B must pass all these properties to C in the very same manner as A
     * did to B.
     * <p>
     * A property name may end with asterisk {@code *} to denote that all properties starting with the part before the
     * asterisk should be forwared. E.g. {@code my.prop.*} would forward both {@code my.prop.foo} and
     * {@code my.prop.bar}.
     *
     * @return a {@link Set} of property names
     */
    public Set<String> getForwardProperties() {
        return forwardProperties;
    }

    /**
     * @return Maven specific settings that apply both on the top level and for each source repository (unless they are
     *         overriden at the repository level).
     */
    public Maven getMaven() {
        return maven;
    }

    /**
     * @return the list of {@link ScmRepository ScmRepositories} that should be used for building of the dependencies
     */
    public List<ScmRepository> getRepositories() {
        return repositories;
    }

    /**
     * Returns a {@link Path} to the sources directory where the dependency sources should be checked out. Each SCM
     * repository will have a subdirectory named after its {@code id} there.
     *
     * @return a {@link Path} to the sources
     */
    public Path getSourcesDirectory() {
        return sourcesDirectory;
    }

    /**
     * Returns the verbosity level the appropriate dependency build tool (such as Maven) should use during the build of
     * a dependency. The interpretation of the individual levels is up to the given build tool. Some build tools may map
     * the levels listed here to a distinct set of levels they support internally.
     *
     * @return the verbosity level
     */
    public Verbosity getVerbosity() {
        return verbosity;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((builderIo == null) ? 0 : builderIo.hashCode());
        result = prime * result + ((forwardProperties == null) ? 0 : forwardProperties.hashCode());
        result = prime * result + ((maven == null) ? 0 : maven.hashCode());
        result = prime * result + ((repositories == null) ? 0 : repositories.hashCode());
        result = prime * result + (skip ? 1231 : 1237);
        result = prime * result + ((sourcesDirectory == null) ? 0 : sourcesDirectory.hashCode());
        result = prime * result + ((verbosity == null) ? 0 : verbosity.hashCode());
        return result;
    }

    /**
     * @return {@code true} if the whole srcdeps processing should be skipped or {@code false} otherwise
     */
    public boolean isSkip() {
        return skip;
    }

    @Override
    public String toString() {
        return "Configuration [builderIo=" + builderIo + ", forwardProperties=" + forwardProperties + ", repositories="
                + repositories + ", skip=" + skip + ", maven=" + maven + ", sourcesDirectory=" + sourcesDirectory
                + ", verbosity=" + verbosity + "]";
    }

}
