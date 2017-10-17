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

import java.util.Map;

import org.srcdeps.core.config.scalar.CharStreamSource;
import org.srcdeps.core.config.tree.Node;
import org.srcdeps.core.config.tree.ScalarNode;
import org.srcdeps.core.config.tree.impl.DefaultContainerNode;
import org.srcdeps.core.config.tree.impl.DefaultScalarNode;

/**
 * Gradle specific settings for a {@link ScmRepository} under which this hangs.
 *
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */
public class ScmRepositoryGradle {
    public static class Builder extends DefaultContainerNode<Node> {

        final ScalarNode<CharStreamSource> modelTransformer = new DefaultScalarNode<>("modelTransformer",
                CharStreamSource.defaultModelTransformer());

        public Builder() {
            super("gradle");
            addChildren(modelTransformer);
        }

        public ScmRepositoryGradle build() {
            return new ScmRepositoryGradle(modelTransformer.getValue());
        }

        @Override
        public Map<String, Node> getChildren() {
            return children;
        }

        public Builder modelTransformer(CharStreamSource modelTransformer) {
            this.modelTransformer.setValue(modelTransformer);
            return this;
        }

    }

    public static Builder builder() {
        return new Builder();
    }

    private final CharStreamSource modelTransformer;

    public ScmRepositoryGradle(CharStreamSource modelTransformer) {
        super();
        this.modelTransformer = modelTransformer;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ScmRepositoryGradle other = (ScmRepositoryGradle) obj;
        if (modelTransformer == null) {
            if (other.modelTransformer != null)
                return false;
        } else if (!modelTransformer.equals(other.modelTransformer))
            return false;
        return true;
    }

    /**
     * @return an URI-like specification of a Gradle script to append to the given dependency project's
     *         {@code settings.gradle}.
     */
    public CharStreamSource getModelTransformer() {
        return modelTransformer;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((modelTransformer == null) ? 0 : modelTransformer.hashCode());
        return result;
    }

    @Override
    public String toString() {
        return "ScmRepositoryGradle [modelTransformer=" + modelTransformer + "]";
    }

}
