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
        final MavenAssertions.FailWithBuilder failWith = MavenAssertions.failWithBuilder();
        final MavenAssertions.FailWithoutBuilder failWithout = MavenAssertions.failWithoutBuilder();
        final ScalarNode<String> versionsMavenPluginVersion = new DefaultScalarNode<>("versionsMavenPluginVersion",
                DEFAULT_VERSIONS_MAVEN_PLUGIN_VERSION);

        public Builder() {
            super("maven");
            addChildren(versionsMavenPluginVersion, failWith, failWithout);
        }

        public Maven build() {
            return new Maven(failWith.build(), failWithout.build());
        }

        public Builder commentBefore(String value) {
            commentBefore.add(value);
            return this;
        }

        public Builder failWith(MavenAssertions.FailWithBuilder failWith) {
            this.failWith.init(failWith);
            return this;
        }

        public Builder failWithout(MavenAssertions.FailWithoutBuilder failWithout) {
            this.failWithout.init(failWithout);
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
    private static final String SRCDEPS_MAVEN_VERSION_PROPERTY = "srcdeps.maven.version";

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

    /**
     * @return the name of the system property to pass the version of {@code srcdeps-maven-plugin} that should be used
     *         in the builds of source dependencies. The value is {@value #SRCDEPS_MAVEN_VERSION_PROPERTY}
     */
    public static String getSrcdepsMavenVersionProperty() {
        return SRCDEPS_MAVEN_VERSION_PROPERTY;
    }

    private final MavenAssertions failWith;
    private final MavenAssertions failWithout;

    private Maven(MavenAssertions failWith, MavenAssertions failWithout) {
        super();
        this.failWith = failWith;
        this.failWithout = failWithout;
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
        if (failWithout == null) {
            if (other.failWithout != null)
                return false;
        } else if (!failWithout.equals(other.failWithout))
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
    public MavenAssertions getFailWith() {
        return failWith;
    }

    /**
     * To be used to prevent building with srcdeps when any of the returned build arguments is present in the top level
     * build. Note that this list is appended to the default {@code failWithAnyOfArguments} list of the given Build
     * Tool. Maven's default {@code failWithAnyOfArguments} are <code>{"release:prepare", "release:perform"}</code>.
     *
     * @return a {@link Set} of build arguments that make the top level build fail if they are not present.
     */
    public MavenAssertions getFailWithout() {
        return failWithout;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((failWith == null) ? 0 : failWith.hashCode());
        result = prime * result + ((failWithout == null) ? 0 : failWithout.hashCode());
        return result;
    }

    @Override
    public String toString() {
        return "Maven [failWith=" + failWith + ", failWithout=" + failWithout + "]";
    }
}
