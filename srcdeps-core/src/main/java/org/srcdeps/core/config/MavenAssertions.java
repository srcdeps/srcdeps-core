/**
 * Copyright 2015-2017 Maven Source Dependencies
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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import org.srcdeps.core.config.tree.ListOfScalarsNode;
import org.srcdeps.core.config.tree.Node;
import org.srcdeps.core.config.tree.ScalarNode;
import org.srcdeps.core.config.tree.impl.DefaultContainerNode;
import org.srcdeps.core.config.tree.impl.DefaultListOfScalarsNode;
import org.srcdeps.core.config.tree.impl.DefaultScalarNode;

/**
 * A configuration node to define conditions under which the outer Maven build that has source dependencies should fail.
 * Note that these settings are effective only if source dependencies exist. In case there are no source dependencies in
 * the outer project, the build will not fail even if any of the goals, profiles or properties are present.
 * <p>
 * This option may come in handy e.g. to avoid releases with source dependencies (see {@link Maven#getFailWith()}) or to
 * opt-in source dependencies only under some specific circumstances, e.g. in a CI job when some unlocking property is
 * present (see {@link Maven#getFailWithout()}).
 *
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */
public class MavenAssertions {

    public static class FailWithBuilder extends FailWithoutBuilder {
        ScalarNode<Boolean> addDefaults = new DefaultScalarNode<>("addDefaults", Boolean.TRUE);
        public FailWithBuilder() {
            super("failWith");
            this.goals = new DefaultListOfScalarsNode<String>("goals", String.class) {

                @Override
                public void applyDefaultsAndInheritance(Stack<Node> configurationStack) {
                    FailWithBuilder.this.addDefaults.applyDefaultsAndInheritance(configurationStack);
                    if (FailWithBuilder.this.addDefaults.getValue() && getElements().isEmpty()) {
                        addAll(DEFAULT_FAIL_GOALS);
                    }
                }

                @Override
                public boolean isInDefaultState(Stack<Node> configurationStack) {
                    Boolean addDefaultsValue = FailWithBuilder.this.addDefaults.getValue();
                    return getElements().isEmpty() || ((addDefaultsValue == null || Boolean.TRUE.equals(addDefaultsValue))
                            && DEFAULT_FAIL_GOALS.equals(asSetOfValues()));
                }

            };
            addChildren(addDefaults, goals, profiles, properties);
        }

        public FailWithBuilder addDefaults(boolean addDefaults) {
            this.addDefaults.setValue(addDefaults);
            return this;
        }

        @Override
        public MavenAssertions build() {
            MavenAssertions result = new MavenAssertions( //
                    addDefaults.getValue(), //
                    goals.asSetOfValues(), //
                    profiles.asSetOfValues(), //
                    properties.asSetOfValues() //
            );
            this.goals = null;
            this.profiles = null;
            this.properties = null;
            return result;
        }

        @Override
        public FailWithBuilder goal(String goal) {
            super.goal(goal);
            return this;
        }

        @Override
        public FailWithBuilder goals(Collection<String> goals) {
            super.goals(goals);
            return this;
        }

        @Override
        public FailWithBuilder profile(String profile) {
            super.profile(profile);
            return this;
        }

        @Override
        public FailWithBuilder profiles(Collection<String> profiles) {
            super.profiles(profiles);
            return this;
        }

        @Override
        public FailWithBuilder properties(Collection<String> properties) {
            super.properties(properties);
            return this;
        }

        @Override
        public FailWithBuilder property(String property) {
            super.property(property);
            return this;
        }


    }

    public static class FailWithoutBuilder extends DefaultContainerNode<Node> {

        ListOfScalarsNode<String> goals;
        ListOfScalarsNode<String> profiles = new DefaultListOfScalarsNode<>("profiles", String.class);
        ListOfScalarsNode<String> properties = new DefaultListOfScalarsNode<>("properties", String.class);

        public FailWithoutBuilder() {
            this("failWithout");
            this.goals = new DefaultListOfScalarsNode<String>("goals", String.class);
            addChildren(goals, profiles, properties);
        }

        FailWithoutBuilder(String name) {
            super(name);
        }

        public MavenAssertions build() {
            MavenAssertions result = new MavenAssertions( //
                    false, //
                    goals.asSetOfValues(), //
                    profiles.asSetOfValues(), //
                    properties.asSetOfValues() //
            );
            this.goals = null;
            this.profiles = null;
            this.properties = null;
            return result;
        }

        public FailWithoutBuilder commentBefore(String value) {
            commentBefore.add(value);
            return this;
        }

        @Override
        public Map<String, Node> getChildren() {
            return children;
        }

        public FailWithoutBuilder goal(String goal) {
            this.goals.add(goal);
            return this;
        }

        public FailWithoutBuilder goals(Collection<String> goals) {
            this.goals.addAll(goals);
            return this;
        }

        public FailWithoutBuilder profile(String profile) {
            this.profiles.add(profile);
            return this;
        }

        public FailWithoutBuilder profiles(Collection<String> profiles) {
            this.profiles.addAll(profiles);
            return this;
        }

        public FailWithoutBuilder properties(Collection<String> properties) {
            this.properties.addAll(properties);
            return this;
        }

        public FailWithoutBuilder property(String property) {
            this.properties.add(property);
            return this;
        }

    }

    private static Set<String> DEFAULT_FAIL_GOALS = Collections
            .unmodifiableSet(new LinkedHashSet<>(Arrays.asList("release:prepare", "release:perform")));

    public static FailWithBuilder failWithBuilder() {
        return new FailWithBuilder();
    }

    public static FailWithoutBuilder failWithoutBuilder() {
        return new FailWithoutBuilder();
    }

    public static Set<String> getDefaultFailGoals() {
        return DEFAULT_FAIL_GOALS;
    }

    private final boolean addDefaults;

    private final Set<String> goals;
    private final Set<String> profiles;

    private final Set<String> properties;

    private MavenAssertions(boolean addDefaults, Set<String> goals, Set<String> profiles, Set<String> properties) {
        super();
        this.addDefaults = addDefaults;
        this.goals = goals;
        this.profiles = profiles;
        this.properties = properties;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        MavenAssertions other = (MavenAssertions) obj;
        if (addDefaults != other.addDefaults)
            return false;
        if (goals == null) {
            if (other.goals != null)
                return false;
        } else if (!goals.equals(other.goals))
            return false;
        if (profiles == null) {
            if (other.profiles != null)
                return false;
        } else if (!profiles.equals(other.profiles))
            return false;
        if (properties == null) {
            if (other.properties != null)
                return false;
        } else if (!properties.equals(other.properties))
            return false;
        return true;
    }

    /**
     * @return a {@link Set} of Maven goals. Presence (in case of {@link Maven#getFailWith()}) or absence (in case of
     *         {@link Maven#getFailWithout()}) of any of the returned goals will make the outer build fail. The goals
     *         can be either in the short form, e.g. {@code "release:prepare"}, or in the fully qualified form e.g.
     *         {@code "org.my-group:my-plugin:my-mojo"}
     */
    public Set<String> getGoals() {
        return goals;
    }

    /**
     * @return a {@link Set} of Maven profile IDs. Presence (in case of {@link Maven#getFailWith()}) or absence (in case
     *         of {@link Maven#getFailWithout()}) of any of the returned profile IDs will make the outer build fail
     */
    public Set<String> getProfiles() {
        return profiles;
    }

    /**
     * @return a {@link Set} of Maven properties. Presence (in case of {@link Maven#getFailWith()}) or absence (in case
     *         of {@link Maven#getFailWithout()}) of any of the returned properties will make the outer build fail. The
     *         {@link Set} can contain either bare property names (in which case a mere property presence will make the
     *         build fail) or name=value pairs (in which case the build will only fail if the named property has the
     *         given value).
     */
    public Set<String> getProperties() {
        return properties;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (addDefaults ? 1231 : 1237);
        result = prime * result + ((goals == null) ? 0 : goals.hashCode());
        result = prime * result + ((profiles == null) ? 0 : profiles.hashCode());
        result = prime * result + ((properties == null) ? 0 : properties.hashCode());
        return result;
    }

    /**
     * @return if {@code true} the respective defaults will be added to the {@link Set}s returned by
     *         {@link #getGoals()}, {@link #getProfiles()} and {@link #getProperties()}. Otherwise, the defaults will be
     *         disregared and only the {@link Set}s as returned by {@link #getGoals()}, {@link #getProfiles()} and
     *         {@link #getProperties()} will be effective.
     */
    public boolean isAddDefaults() {
        return addDefaults;
    }

    @Override
    public String toString() {
        return "MavenFailWith [addDefaults=" + addDefaults + ", goals=" + goals + ", profiles=" + profiles
                + ", properties=" + properties + "]";
    }

}
