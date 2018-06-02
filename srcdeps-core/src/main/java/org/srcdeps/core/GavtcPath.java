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
package org.srcdeps.core;

import java.nio.file.Path;
import java.util.Comparator;

/**
 * A {@link Gavtc} with a {@link Path} of the given artifact in the local Maven repository.
 *
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 * @since 3.2.2
 */
public class GavtcPath extends Gavtc {

    private static final Comparator<GavtcPath> COMPARATOR = new Comparator<GavtcPath>() {

        @Override
        public int compare(GavtcPath o1, GavtcPath o2) {
            return o1.path.compareTo(o2.path);
        }

    };

    /**
     * @return a {@link Comparator} comparing through {@link #getPath()}
     */
    public static Comparator<GavtcPath> comparator() {
        return COMPARATOR;
    }

    public static GavtcPath of(String gavtcString, Path path) {
        final Gavtc gavtc = Gavtc.of(gavtcString);
        return new GavtcPath(gavtc.getGroupId(), gavtc.getArtifactId(), gavtc.getVersion(), gavtc.getType(),
                gavtc.getClassifier(), path);
    }

    private final Path path;

    public GavtcPath(String groupId, String artifactId, String version, String type, String classifier, Path path) {
        super(groupId, artifactId, version, type, classifier);
        this.path = path;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        if (getClass() != obj.getClass())
            return false;
        GavtcPath other = (GavtcPath) obj;
        if (path == null) {
            if (other.path != null)
                return false;
        } else if (!path.equals(other.path))
            return false;
        return true;
    }

    /**
     * @return the absolute {@link Path} of the given artifact in the local Maven repository
     */
    public Path getPath() {
        return path;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((path == null) ? 0 : path.hashCode());
        return result;
    }

    @Override
    public String toString() {
        return super.toString() + " [" + path + "]";
    }

}
