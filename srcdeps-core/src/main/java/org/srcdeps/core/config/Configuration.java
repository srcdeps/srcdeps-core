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

import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.srcdeps.core.BuildRequest.Verbosity;

/**
 * A configuration suitable for a build system wanting to handle source dependencies.
 *
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */
public class Configuration {
    public static class Builder implements TraversableConfigurationNode<Builder> {

        private boolean addDefaultFailWithAnyOfArguments = true;
        private BuilderIo.Builder builderIo = BuilderIo.builder();
        private List<String> failWithAnyOfArguments = new ArrayList<>();
        private List<String> forwardProperties = new ArrayList<>(defaultForwardProperties);
        private List<ScmRepository.Builder> repositories = new ArrayList<>();
        private boolean skip = false;
        private Path sourcesDirectory;
        private Verbosity verbosity = Verbosity.warn;

        private Builder() {
            super();
        }

        /**
         * Used to override the configuration by values coming from some higher-ranking source
         *
         * @param visitor
         *            the {@link ConfigurationNodeVisitor} to accept
         * @return this builder
         */
        @Override
        public Builder accept(ConfigurationNodeVisitor visitor) {
            for (Field field : this.getClass().getDeclaredFields()) {
                visitor.visit(this, field);
            }
            return this;
        }

        public Builder addDefaultFailWithAnyOfArguments(boolean addDefaultFailWithAnyOfArguments) {
            this.addDefaultFailWithAnyOfArguments = addDefaultFailWithAnyOfArguments;
            return this;
        }

        public Configuration build() {
            List<ScmRepository> repos = new ArrayList<>(repositories.size());
            for (ScmRepository.Builder repoBuilder : repositories) {
                repos.add(repoBuilder.build());
            }

            Configuration result = new Configuration(Collections.unmodifiableList(repos), sourcesDirectory, skip,
                    verbosity, builderIo.build(), Collections.unmodifiableSet(new LinkedHashSet<>(forwardProperties)),
                    Collections.unmodifiableSet(new LinkedHashSet<>(failWithAnyOfArguments)),
                    addDefaultFailWithAnyOfArguments);
            repositories = null;
            forwardProperties = null;
            failWithAnyOfArguments = null;
            return result;
        }

        public Builder builderIo(BuilderIo.Builder builderIo) {
            this.builderIo = builderIo;
            return this;
        }

        public Builder configModelVersion(String configModelVersion) {
            if (!SUPPORTED_CONFIG_MODEL_VERSIONS.contains(configModelVersion)) {
                throw new IllegalArgumentException(
                        String.format("Cannot parse configModelVersion [%s]; expected any of [%s]", configModelVersion,
                                SUPPORTED_CONFIG_MODEL_VERSIONS));
            }
            return this;
        }

        public Builder failWithAnyOfArgument(String value) {
            failWithAnyOfArguments.add(value);
            return this;
        }

        public Builder failWithAnyOfArguments(Collection<String> values) {
            failWithAnyOfArguments.addAll(values);
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

        public Builder repositories(Map<String, ScmRepository.Builder> repoBuilders) {
            for (Map.Entry<String, ScmRepository.Builder> en : repoBuilders.entrySet()) {
                ScmRepository.Builder repoBuilder = en.getValue();
                repoBuilder.id(en.getKey());
                this.repositories.add(repoBuilder);
            }
            return this;
        }

        public Builder repository(ScmRepository.Builder repo) {
            this.repositories.add(repo);
            return this;
        }

        public Builder skip(boolean value) {
            this.skip = value;
            return this;
        }

        public Builder sourcesDirectory(Path value) {
            this.sourcesDirectory = value;
            return this;
        }

        public Builder verbosity(Verbosity value) {
            this.verbosity = value;
            return this;
        }

    }

    public static final List<String> defaultForwardProperties = Collections.singletonList("srcdeps.mvn.*");

    public static final String SRCDEPS_MVN_SETTINGS_PROP = "srcdeps.mvn.settings";

    public static final Set<String> SUPPORTED_CONFIG_MODEL_VERSIONS = Collections
            .unmodifiableSet(new LinkedHashSet<>(Arrays.asList("1.0", "1.1")));

    public static Builder builder() {
        return new Builder();
    }

    private final boolean addDefaultFailWithAnyOfArguments;
    private final BuilderIo builderIo;
    private final Set<String> failWithAnyOfArguments;
    private final Set<String> forwardProperties;
    private final List<ScmRepository> repositories;
    private final boolean skip;

    private final Path sourcesDirectory;

    private final Verbosity verbosity;

    private Configuration(List<ScmRepository> repositories, Path sourcesDirectory, boolean skip, Verbosity verbosity,
            BuilderIo redirects, Set<String> forwardProperties, Set<String> failWithAnyOfArguments,
            boolean addDefaultFailWithAnyOfArguments) {
        super();
        this.repositories = repositories;
        this.sourcesDirectory = sourcesDirectory;
        this.skip = skip;
        this.verbosity = verbosity;
        this.forwardProperties = forwardProperties;
        this.builderIo = redirects;
        this.failWithAnyOfArguments = failWithAnyOfArguments;
        this.addDefaultFailWithAnyOfArguments = addDefaultFailWithAnyOfArguments;
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
        if (addDefaultFailWithAnyOfArguments != other.addDefaultFailWithAnyOfArguments)
            return false;
        if (builderIo == null) {
            if (other.builderIo != null)
                return false;
        } else if (!builderIo.equals(other.builderIo))
            return false;
        if (failWithAnyOfArguments == null) {
            if (other.failWithAnyOfArguments != null)
                return false;
        } else if (!failWithAnyOfArguments.equals(other.failWithAnyOfArguments))
            return false;
        if (forwardProperties == null) {
            if (other.forwardProperties != null)
                return false;
        } else if (!forwardProperties.equals(other.forwardProperties))
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

    /**
     * To be used to prevent building with srcdeps when any of the returned build arguments is present in the top level
     * build. Note that this list is appended to the default {@code failWithAnyOfArguments} list of the given Build
     * Tool. Maven's default {@code failWithAnyOfArguments} are <code>{"release:prepare", "release:perform"}</code>.
     *
     * @return a {@link Set} of build arguments that make the top level build fail if they are present.
     */
    public Set<String> getFailWithAnyOfArguments() {
        return failWithAnyOfArguments;
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
        result = prime * result + (addDefaultFailWithAnyOfArguments ? 1231 : 1237);
        result = prime * result + ((builderIo == null) ? 0 : builderIo.hashCode());
        result = prime * result + ((failWithAnyOfArguments == null) ? 0 : failWithAnyOfArguments.hashCode());
        result = prime * result + ((forwardProperties == null) ? 0 : forwardProperties.hashCode());
        result = prime * result + ((repositories == null) ? 0 : repositories.hashCode());
        result = prime * result + (skip ? 1231 : 1237);
        result = prime * result + ((sourcesDirectory == null) ? 0 : sourcesDirectory.hashCode());
        result = prime * result + ((verbosity == null) ? 0 : verbosity.hashCode());
        return result;
    }

    /**
     * @return if {@code true} the {@link Set} returned by {@link #getFailWithAnyOfArguments()} will be added to the
     *         default list of {@code failWithAnyOfArguments} of the given build tool. Otherwise, the default list of
     *         {@code failWithAnyOfArguments} of the given build tool will be disregared and only the {@link Set}
     *         returned by {@link #getFailWithAnyOfArguments()} will be effective.
     */
    public boolean isAddDefaultFailWithAnyOfArguments() {
        return addDefaultFailWithAnyOfArguments;
    }

    /**
     * @return {@code true} if the whole srcdeps processing should be skipped or {@code false} otherwise
     */
    public boolean isSkip() {
        return skip;
    }

    @Override
    public String toString() {
        return "Configuration [addDefaultFailWithAnyOfArguments=" + addDefaultFailWithAnyOfArguments + ", builderIo="
                + builderIo + ", failWithAnyOfArguments=" + failWithAnyOfArguments + ", forwardProperties="
                + forwardProperties + ", repositories=" + repositories + ", skip=" + skip + ", sourcesDirectory="
                + sourcesDirectory + ", verbosity=" + verbosity + "]";
    }

}
