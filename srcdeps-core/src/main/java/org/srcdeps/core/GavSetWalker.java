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

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.TreeMap;

import org.srcdeps.core.util.BitStack;
import org.srcdeps.core.util.Consumer;

/**
 * A tree walker through the files in the local Maven repository which belong to a specific {@link GavSet}.
 *
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 * @since 3.2.2
 */
public class GavSetWalker {

    /**
     * A simple {@link Consumer} that just collects the GAV {@link Path}s into {@link #gavPaths} {@link Map}.
     */
    public static class GavPathCollector implements Consumer<GavtcPath> {

        private final Map<Path, Gav> gavPaths = new TreeMap<>();

        @Override
        public void accept(GavtcPath t) {
            gavPaths.put(t.getPath().getParent(), t);
        }

        /**
         * @return the GAV {@link Path}s collected
         */
        public Map<Path, Gav> getGavPaths() {
            return gavPaths;
        }

    }

    /**
     * A {@link FileVisitor} for walking the local Maven repository.
     */
    static class GavtcPathVisitor implements FileVisitor<Path> {
        static boolean hasIgnorableExtension(String name) {
            for (String ext : IGNORABLE_EXTENSIONS) {
                if (name.endsWith(ext)) {
                    return true;
                }
            }
            return false;
        }

        private final Consumer<GavtcPath> callback;
        private final BitStack dirCanContainArtifacts = new BitStack();
        private final GavSet gavSet;
        private final Path localMavenRepoRoot;

        private final String version;

        GavtcPathVisitor(Path localMavenRepoRoot, GavSet gavSet, String version, Consumer<GavtcPath> callback) {
            super();
            this.localMavenRepoRoot = localMavenRepoRoot;
            this.gavSet = gavSet;
            this.version = version;
            this.callback = callback;
        }

        @Override
        public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
            dirCanContainArtifacts.pop();
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
            dirCanContainArtifacts.push(version.equals(dir.getFileName().toString()));
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            if (dirCanContainArtifacts.peek()) {
                final Path gavPath = file.getParent();
                final Path gaPath = gavPath.getParent();
                final String artifactId = gaPath.getFileName().toString();
                final String name = file.getFileName().toString();
                if (name.startsWith(artifactId) && !hasIgnorableExtension(name)) {
                    final Path gPath = localMavenRepoRoot.relativize(gaPath.getParent());
                    final String groupId = gPath.toString().replace(File.separatorChar, '.');

                    if (gavSet.contains(groupId, artifactId, version)) {
                        final int avStringLength = artifactId.length() + version.length() + 1;
                        if (avStringLength + 1 < name.length()) {
                            switch (name.charAt(avStringLength)) {
                            case '.': {
                                final String type = name.substring(avStringLength + 1);
                                final GavtcPath gavtcPath = new GavtcPath(groupId, artifactId, version, type, null,
                                        file);
                                callback.accept(gavtcPath);
                            }
                                break;
                            case '-': {
                                int ext = name.indexOf('.', avStringLength + 1);
                                if (ext >= 0) {
                                    final String classifier = name.substring(avStringLength + 1, ext);
                                    ext++;
                                    if (ext < name.length()) {
                                        final String type = name.substring(ext);
                                        final GavtcPath gavtcPath = new GavtcPath(groupId, artifactId, version, type,
                                                classifier, file);
                                        callback.accept(gavtcPath);
                                    }
                                }
                                break;
                            }
                            default:
                                assert false;
                                break;
                            }
                        }
                    }
                }
            }
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
            throw exc;
        }

    }

    private static final Path EMPTY_PATH = Paths.get("");
    private static final List<Path> EMPTY_PATH_LIST;
    private static final List<String> IGNORABLE_EXTENSIONS = Arrays.asList(".sha1", ".md5", ".asc");

    static {
        EMPTY_PATH_LIST = Collections.singletonList(EMPTY_PATH);
    }

    /**
     * @param gavSet
     *                    the {@link GavSet} to walk through
     * @param version
     *                    the version of artifacts to look for
     * @return a {@link List} of Paths (relative to local Maven repository root) at which the filesystem travesal should
     *         start to cover all artifacts belonging to the given {@link GavSet}
     */
    static List<Path> gavSetToSubtrees(GavSet gavSet, String version) {
        List<GavPattern> includes = gavSet.getIncludes();
        if (includes.isEmpty()) {
            return Collections.emptyList();
        } else {
            List<Path> subtrees = new ArrayList<>(includes.size());
            for (GavPattern include : includes) {
                Path newSubtree = patternToSubtree(include, version);
                if (EMPTY_PATH.equals(newSubtree)) {
                    return EMPTY_PATH_LIST;
                }
                boolean processed = false;
                boolean replace = true;
                for (ListIterator<Path> it = subtrees.listIterator(); it.hasNext();) {
                    final Path p = it.next();
                    if (p.equals(newSubtree) || newSubtree.startsWith(p)) {
                        /* same or a subpath of a known subtree - nothing to do */
                        processed = true;
                        break;
                    } else if (p.startsWith(newSubtree)) {
                        /* superpath of a known subtree */
                        if (replace) {
                            it.set(newSubtree);
                            processed = true;
                            replace = false;
                        } else {
                            it.remove();
                        }
                    }
                }
                if (!processed) {
                    subtrees.add(newSubtree);
                }
            }
            return subtrees;
        }
    }

    /**
     * @return a Path relative to local Maven repository
     */
    static Path patternToSubtree(GavPattern gavPattern, String version) {
        return patternToSubtree(EMPTY_PATH, new String[] { gavPattern.groupIdPattern.getSource(),
                gavPattern.artifactIdPattern.getSource(), version }, 0);
    }

    /**
     * @return a Path relative to local Maven repository
     */
    static Path patternToSubtree(Path parentPath, String[] patterns, int index) {
        final String patternSource = patterns[index];
        /* we pass in versions without wildcards */
        final int wildcardPos = index == 2 ? -1 : patternSource.indexOf(GavPattern.MULTI_WILDCARD);
        switch (wildcardPos) {
        case 0:
            return parentPath;
        case -1:
            final Path result = parentPath.resolve(index == 0 ? patternSource.replace('.', '/') : patternSource);
            return index == 2 ? result : patternToSubtree(result, patterns, index + 1);
        default:
            if (index == 0) {
                /* do this only for group patterns */
                final int periodPos = patternSource.lastIndexOf('.', wildcardPos - 1);
                switch (periodPos) {
                case 0:
                case -1:
                    return parentPath;
                default:
                    return parentPath.resolve(patternSource.substring(0, periodPos).replace('.', '/'));
                }
            } else {
                return parentPath;
            }
        }
    }

    private final GavSet gavSet;
    private final Path localMavenRepoRoot;
    private final List<Path> subtrees;
    private final String version;

    public GavSetWalker(Path localMavenRepoRoot, GavSet gavSet, String version) {
        super();
        this.localMavenRepoRoot = localMavenRepoRoot;
        this.gavSet = gavSet;
        this.version = version;
        this.subtrees = gavSetToSubtrees(gavSet, version);
    }

    /**
     * Walk through the {@link GavtcPath}s belonging to the given {@link #gavSet}
     *
     * @param callback
     *                     the {@link Consumer} to notify
     * @throws IOException
     */
    public void walk(Consumer<GavtcPath> callback) throws IOException {
        FileVisitor<Path> visitor = new GavtcPathVisitor(localMavenRepoRoot, gavSet, version, callback);
        for (Path path : subtrees) {
            final Path start = localMavenRepoRoot.resolve(path).normalize();
            if (Files.exists(start)) {
                Files.walkFileTree(start, visitor);
            }
        }
    }

}
