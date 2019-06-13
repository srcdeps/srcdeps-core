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
package org.srcdeps.core.config;

import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.srcdeps.core.MavenSourceTree;
import org.srcdeps.core.config.tree.ListOfScalarsNode;
import org.srcdeps.core.config.tree.Node;
import org.srcdeps.core.config.tree.ScalarNode;
import org.srcdeps.core.config.tree.impl.DefaultContainerNode;
import org.srcdeps.core.config.tree.impl.DefaultListOfScalarsNode;
import org.srcdeps.core.config.tree.impl.DefaultScalarNode;

/**
 * Maven specific settings for a {@link ScmRepository} under which this hangs.
 *
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */
public class ScmRepositoryMaven {
    public static class Builder extends DefaultContainerNode<Node> {
        final ScalarNode<Boolean> excludeNonRequired = new DefaultScalarNode<>("excludeNonRequired", Boolean.FALSE);
        final ScalarNode<Boolean> includeRequired = new DefaultScalarNode<>("includeRequired", Boolean.FALSE);
        final ListOfScalarsNode<String> includes = new DefaultListOfScalarsNode<>("includes", String.class);
        ScalarNode<Boolean> useVersionsMavenPlugin = new DefaultScalarNode<Boolean>("useVersionsMavenPlugin", null,
                Boolean.class) {

            @Override
            public void applyDefaultsAndInheritance(Stack<Node> configurationStack) {
                Configuration.Builder configuratioBuilder = (Configuration.Builder) configurationStack.get(0);
                inheritFrom(configuratioBuilder.maven.useVersionsMavenPlugin, configurationStack);
            }

            @Override
            public boolean isInDefaultState(Stack<Node> configurationStack) {
                Configuration.Builder configuratioBuilder = (Configuration.Builder) configurationStack.get(0);
                return isInDefaultState(configuratioBuilder.maven.useVersionsMavenPlugin, configurationStack);
            }

        };
        ScalarNode<String> versionsMavenPluginVersion = new DefaultScalarNode<String>("versionsMavenPluginVersion",
                null, String.class) {

            @Override
            public void applyDefaultsAndInheritance(Stack<Node> configurationStack) {
                Configuration.Builder configuratioBuilder = (Configuration.Builder) configurationStack.get(0);
                inheritFrom(configuratioBuilder.maven.versionsMavenPluginVersion, configurationStack);
            }

            @Override
            public boolean isInDefaultState(Stack<Node> configurationStack) {
                Configuration.Builder configuratioBuilder = (Configuration.Builder) configurationStack.get(0);
                return isInDefaultState(configuratioBuilder.maven.versionsMavenPluginVersion, configurationStack);
            }

        };

        public Builder() {
            super("maven");
            addChildren(versionsMavenPluginVersion, useVersionsMavenPlugin, includeRequired, includes,
                    excludeNonRequired);
        }

        public ScmRepositoryMaven build() {
            return new ScmRepositoryMaven(versionsMavenPluginVersion.getValue(), //
                    useVersionsMavenPlugin.getValue(), //
                    includes.asListOfValues(), //
                    includeRequired.getValue(), //
                    excludeNonRequired.getValue());
        }

        public Builder commentBefore(String value) {
            commentBefore.add(value);
            return this;
        }

        public Builder excludeNonRequired(boolean value) {
            this.excludeNonRequired.setValue(value);
            return this;
        }

        @Override
        public Map<String, Node> getChildren() {
            return children;
        }

        public Builder include(String include) {
            this.includes.add(include);
            return this;
        }

        public Builder includeRequired(boolean value) {
            this.includeRequired.setValue(value);
            return this;
        }

        public Builder includes(List<String> includes) {
            this.includes.addAll(includes);
            return this;
        }

        public Builder useVersionsMavenPlugin(boolean useVersionsMavenPlugin) {
            this.useVersionsMavenPlugin.setValue(useVersionsMavenPlugin);
            return this;
        }

        public Builder versionsMavenPluginVersion(String versionsMavenPluginVersion) {
            this.versionsMavenPluginVersion.setValue(versionsMavenPluginVersion);
            return this;
        }

    }

    public static Builder builder() {
        return new Builder();
    }

    private final boolean excludeNonRequired;

    private final boolean includeRequired;

    private final List<String> includes;

    private final boolean useVersionsMavenPlugin;

    private final String versionsMavenPluginVersion;

    public ScmRepositoryMaven(String versionsMavenPluginVersion, boolean useVersionsMavenPlugin, List<String> includes,
            boolean includeRequired, boolean excludeNonRequired) {
        super();
        this.versionsMavenPluginVersion = versionsMavenPluginVersion;
        this.includes = includes;
        this.includeRequired = includeRequired;
        this.excludeNonRequired = excludeNonRequired;
        this.useVersionsMavenPlugin = useVersionsMavenPlugin;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ScmRepositoryMaven other = (ScmRepositoryMaven) obj;
        if (excludeNonRequired != other.excludeNonRequired)
            return false;
        if (includeRequired != other.includeRequired)
            return false;
        if (includes == null) {
            if (other.includes != null)
                return false;
        } else if (!includes.equals(other.includes))
            return false;
        if (useVersionsMavenPlugin != other.useVersionsMavenPlugin)
            return false;
        if (versionsMavenPluginVersion == null) {
            if (other.versionsMavenPluginVersion != null)
                return false;
        } else if (!versionsMavenPluginVersion.equals(other.versionsMavenPluginVersion))
            return false;
        return true;
    }

    /**
     * Returns a {@link List} of {@code groupId:artifactId} identifiers that should be included in the build of the
     * given dependency source tree. The inclusion is achieved by using {@code -pl} (a.k.a. {@code --projects}) and
     * {@code -am} (a.k.a. {@code --also-make}) Maven command line options. If the returned {@link List} is empty, no
     * additional Maven CLI options will be used.
     * <p>
     * Using this option directly or via {@link #isIncludeRequired()} or in combination with
     * {@link #isExcludeNonRequired()} may speedup the build of dependency projects substantially.
     *
     * @return a {@link List} of {@code groupId:artifactId} identifiers
     * @see #isIncludeRequired()
     * @see #isExcludeNonRequired()
     */
    public List<String> getIncludes() {
        return includes;
    }

    /**
     * @return the version of {@code org.codehaus.mojo:versions-maven-plugin} to use when setting versions in the given
     *         source repository. When not set explicitly, the default is
     *         {@value ScmRepositoryMaven#DEFAULT_VERSIONS_MAVEN_PLUGIN_VERSION}.
     */
    public String getVersionsMavenPluginVersion() {
        return versionsMavenPluginVersion;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (excludeNonRequired ? 1231 : 1237);
        result = prime * result + (includeRequired ? 1231 : 1237);
        result = prime * result + ((includes == null) ? 0 : includes.hashCode());
        result = prime * result + (useVersionsMavenPlugin ? 1231 : 1237);
        result = prime * result + ((versionsMavenPluginVersion == null) ? 0 : versionsMavenPluginVersion.hashCode());
        return result;
    }

    /**
     * If this method returns {@code true} and {@link #getIncludes()} returns a non-empty list, {@code srcdeps} will
     * remove uneeded {@code <module>} elements from {@code pom.xml} files in the dependency project before building it.
     * Otherwise, the value returned by this method has no effect.
     * <p>
     * The set of uneeded {@code <module>} elements is defined as follows:
     * <ul>
     * <li>First, the set of artifacts returned by {@link #getIncludes()} is used to create a closure under the <i>is
     * child of</i> and <i>is dependendent on</i> relationships. Building this closure set of Maven modules should be
     * just enough to produce all dependencies required by the dependent source tree.</li>
     * <li>Then the complement of that closure set is created, the universe being the set of all modules in the
     * dependency source tree. This complement set contains all Maven modules from the dependency source tree which do
     * not need to be built to produce the artifacts the dependent source tree requires.</li>
     * </ul>
     *
     * The unneeded {@code <module>} elements are removed as follows: The dependency source tree is traversed from the
     * root Maven module following the {@code <module>} <i>child</i> links. Each encountered {@code <module>} element is
     * looked up in the set of unneeded modules: if ṕresent there, the given {@code <module>} element is removed from
     * the {@code pom.xml} file and the travesal towards that module does not continue.
     *
     * @return {@code true} or {@code false}
     * @see #getIncludes()
     * @see #isIncludeRequired()
     * @see MavenSourceTree
     */
    public boolean isExcludeNonRequired() {
        return excludeNonRequired;
    }

    /**
     * If this method returns {@code true} the source tree triggering the build of the given dependency will be scanned
     * for dependencies included in {@link ScmRepository#getGavSet()}. These dependencies will then be added to
     * {@link #getIncludes()}. Otherwise the source tree triggering the build will not be scanned and no items will be
     * added to {@link #getIncludes()}.
     *
     * @return {@code true} or {@code false}
     * @see #getIncludes()
     * @see #isExcludeNonRequired()
     */
    public boolean isIncludeRequired() {
        return includeRequired;
    }

    /**
     * Tells which tool should be used to set versions in source trees to prepare them for the building of a dependency.
     * If {@code true} the versions will be set using {@code mvn versions:set -DnewVersion=...}; otherwise srcdep's own
     * version setter will be used.
     *
     * @return {@code true} or {@code false}
     */
    public boolean isUseVersionsMavenPlugin() {
        return useVersionsMavenPlugin;
    }

    @Override
    public String toString() {
        return "ScmRepositoryMaven [excludeNonRequired=" + excludeNonRequired + ", includeRequired=" + includeRequired
                + ", includes=" + includes + ", versionsMavenPluginVersion=" + versionsMavenPluginVersion
                + ", useVersionsMavenPlugin=" + useVersionsMavenPlugin + "]";
    }

}
