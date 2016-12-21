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

import java.util.Map;
import java.util.Set;

import org.srcdeps.core.config.tree.Node;
import org.srcdeps.core.config.tree.ScalarNode;
import org.srcdeps.core.config.tree.impl.DefaultContainerNode;
import org.srcdeps.core.config.tree.impl.DefaultScalarNode;

/**
 * Maven specific settings that apply both on the top level and for each source repository.
 *
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */
public class Maven {
    public static class Builder extends DefaultContainerNode<Node> {
        final MavenFailWith.Builder failWith = MavenFailWith.builder();
        final ScalarNode<String> versionsMavenPluginVersion = new DefaultScalarNode<>("versionsMavenPluginVersion",
                DEFAULT_VERSIONS_MAVEN_PLUGIN_VERSION);

        public Builder() {
            super("maven");
            addChildren(versionsMavenPluginVersion, failWith);
        }

        public Maven build() {
            return new Maven(failWith.build());
        }

        public Builder failWith(MavenFailWith.Builder failWith) {
            this.failWith.init(failWith);
            return this;
        }

        @Override
        public Map<String, Node> getChildren() {
            return children;
        }

        public Builder versionsMavenPluginVersion(String versionsMavenPluginVersion) {
            this.versionsMavenPluginVersion.setValue(versionsMavenPluginVersion);
            return this;
        }

    }

    /** Keep in sync with doc/srcdeps.yaml */
    private static final String DEFAULT_VERSIONS_MAVEN_PLUGIN_VERSION = "2.3";

    private static final String SRCDEPS_MAVEN_PROPERTIES_PATTERN = "srcdeps.maven.*";

    private static final String SRCDEPS_MAVEN_SETTINGS_PROPERTY = "srcdeps.maven.settings";

    public static Builder builder() {
        return new Builder();
    }

    /**
     * @return the default version of {@code versions-maven-plugin} to use when
     *         {@code srcdeps.maven.versionsMavenPluginVersion} is neither set in {@code srcdeps.yaml} nor on the
     *         command line. The current value is {@value #DEFAULT_VERSIONS_MAVEN_PLUGIN_VERSION}
     */
    public static String getDefaultVersionsMavenPluginVersion() {
        return DEFAULT_VERSIONS_MAVEN_PLUGIN_VERSION;
    }

    /**
     * @return {@value #SRCDEPS_MAVEN_PROPERTIES_PATTERN}
     */
    public static String getSrcdepsMavenPropertiesPattern() {
        return SRCDEPS_MAVEN_PROPERTIES_PATTERN;
    }

    /**
     * @return the name of the system property to pass the path to settings.xml file that should be used in the builds
     *         of source dependencies. The value is {@value #SRCDEPS_MAVEN_SETTINGS_PROPERTY}
     */
    public static String getSrcdepsMavenSettingsProperty() {
        return SRCDEPS_MAVEN_SETTINGS_PROPERTY;
    }

    private final MavenFailWith failWith;

    private Maven(MavenFailWith failWith) {
        super();
        this.failWith = failWith;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Maven other = (Maven) obj;
        if (failWith == null) {
            if (other.failWith != null)
                return false;
        } else if (!failWith.equals(other.failWith))
            return false;
        return true;
    }

    /**
     * To be used to prevent building with srcdeps when any of the returned build arguments is present in the top level
     * build. Note that this list is appended to the default {@code failWithAnyOfArguments} list of the given Build
     * Tool. Maven's default {@code failWithAnyOfArguments} are <code>{"release:prepare", "release:perform"}</code>.
     *
     * @return a {@link Set} of build arguments that make the top level build fail if they are present.
     */
    public MavenFailWith getFailWith() {
        return failWith;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((failWith == null) ? 0 : failWith.hashCode());
        return result;
    }

    @Override
    public String toString() {
        return "Maven [failWith=" + failWith + "]";
    }
}
