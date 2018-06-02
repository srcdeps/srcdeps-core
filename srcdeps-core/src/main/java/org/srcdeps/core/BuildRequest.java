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

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.srcdeps.core.config.scalar.CharStreamSource;
import org.srcdeps.core.shell.IoRedirects;
import org.srcdeps.core.util.DigestOutputStream;
import org.srcdeps.core.util.SrcdepsCoreUtils;

/**
 * A description of what and how should be built.
 *
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */
public class BuildRequest {

    /**
     *
     * A builder for {@link BuildRequest}s.
     *
     */
    public static class BuildRequestBuilder {

        private boolean addDefaultBuildArguments = true;
        private boolean addDefaultBuildEnvironment = true;
        private List<String> buildArguments = new ArrayList<>();
        private Map<String, String> buildEnvironment = new LinkedHashMap<>();
        private Path dependentProjectRootDirectory;
        private Set<String> forwardProperties = new LinkedHashSet<>();
        private GavSet gavSet = GavSet.includeAll();
        private CharStreamSource gradleModelTransformer;
        private IoRedirects ioRedirects = IoRedirects.inheritAll();
        private Path projectRootDirectory;
        private List<String> scmUrls = new ArrayList<>();
        private boolean skipTests = true;
        private SrcVersion srcVersion;
        private long timeoutMs = DEFAULT_TIMEOUT_MS;
        private Verbosity verbosity = Verbosity.info;
        private String version;
        private String versionsMavenPluginVersion;

        /**
         * @param addDefaultBuildArguments
         *                                     see {@link BuildRequest#isAddDefaultBuildArguments()}
         * @return this {@link BuildRequestBuilder}
         */
        public BuildRequestBuilder addDefaultBuildArguments(boolean addDefaultBuildArguments) {
            this.addDefaultBuildArguments = addDefaultBuildArguments;
            return this;
        }

        /**
         * @param addDefaultBuildEnvironment
         *                                       see {@link BuildRequest#isAddDefaultBuildEnvironment()}
         * @return this {@link BuildRequestBuilder}
         */
        public BuildRequestBuilder addDefaultBuildEnvironment(boolean addDefaultBuildEnvironment) {
            this.addDefaultBuildEnvironment = addDefaultBuildEnvironment;
            return this;
        }

        /**
         * @return a new {@link BuildRequest} based on the values stored in fields of this {@link BuildRequestBuilder}
         */
        public BuildRequest build() {
            final String useVersion = version == null ? srcVersion.toString() : version;
            return new BuildRequest(dependentProjectRootDirectory, projectRootDirectory, srcVersion, useVersion, gavSet,
                    Collections.unmodifiableList(scmUrls), Collections.unmodifiableList(buildArguments), skipTests,
                    addDefaultBuildArguments, Collections.unmodifiableSet(forwardProperties),
                    Collections.unmodifiableMap(buildEnvironment), addDefaultBuildEnvironment, verbosity, ioRedirects,
                    timeoutMs, versionsMavenPluginVersion, gradleModelTransformer);
        }

        /**
         * @param argument
         *                     the single build argument to add to {@link #buildArguments}; see
         *                     {@link BuildRequest#getBuildArguments()}
         * @return this {@link BuildRequestBuilder}
         */
        public BuildRequestBuilder buildArgument(String argument) {
            this.buildArguments.add(argument);
            return this;
        }

        /**
         * @param arguments
         *                      the arguments to add to {@link #buildArguments}; see
         *                      {@link BuildRequest#getBuildArguments()}
         * @return this {@link BuildRequestBuilder}
         */
        public BuildRequestBuilder buildArguments(List<String> buildArguments) {
            this.buildArguments.addAll(buildArguments);
            return this;
        }

        /**
         * @param arguments
         *                      the arguments to add to {@link #buildArguments}; see
         *                      {@link BuildRequest#getBuildArguments()}
         * @return this {@link BuildRequestBuilder}
         */
        public BuildRequestBuilder buildArguments(String... arguments) {
            for (String argument : arguments) {
                this.buildArguments.add(argument);
            }
            return this;
        }

        /**
         * @param buildEnvironment
         *                             see {@link BuildRequest#getBuildEnvironment()}
         * @return this {@link BuildRequestBuilder}
         */
        public BuildRequestBuilder buildEnvironment(Map<String, String> buildEnvironment) {
            this.buildEnvironment.putAll(buildEnvironment);
            return this;
        }

        /**
         * Add an environment variable of the given {@code name} and {@code value} to {@link #buildEnvironment}.
         *
         * @see BuildRequest#getBuildEnvironment()
         * @param name
         *                  the name of the environment variable to add to {@link #buildEnvironment}
         * @param value
         *                  the value of the environment variable to add to {@link #buildEnvironment}
         * @return this {@link BuildRequestBuilder}
         */
        public BuildRequestBuilder buildEnvironmentVariable(String name, String value) {
            this.buildEnvironment.put(name, value);
            return this;
        }

        /**
         * @param dependentProjectRootDirectory
         *                                          see {@link BuildRequest#getDependentProjectRootDirectory()}
         * @return this {@link BuildRequestBuilder}
         */
        public BuildRequestBuilder dependentProjectRootDirectory(Path dependentProjectRootDirectory) {
            this.dependentProjectRootDirectory = dependentProjectRootDirectory;
            return this;
        }

        /**
         * @see BuildRequest#getForwardProperties()
         * @param values
         *                   the property names or patterns to forward
         * @return this {@link BuildRequestBuilder}
         */
        public BuildRequestBuilder forwardProperties(Collection<String> values) {
            forwardProperties.addAll(values);
            return this;
        }

        /**
         * @see BuildRequest#getForwardProperties()
         * @param value
         *                  the property name or pattern to forward
         * @return this {@link BuildRequestBuilder}
         */
        public BuildRequestBuilder forwardProperty(String value) {
            forwardProperties.add(value);
            return this;
        }

        /**
         * @see BuildRequest#getGavSet()
         * @param gavSet
         *                   the {@link GavSet} to set
         * @return this builder
         */
        public BuildRequestBuilder gavSet(GavSet gavSet) {
            this.gavSet = gavSet;
            return this;
        }

        /**
         * @param gradleModelTransformer
         *                                   see {@link BuildRequest#getGradleModelTransformer()}
         * @return this {@link BuildRequestBuilder}
         */
        public BuildRequestBuilder gradleModelTransformer(CharStreamSource gradleModelTransformer) {
            this.gradleModelTransformer = gradleModelTransformer;
            return this;
        }

        /**
         * @param ioRedirects
         *                        see {@link BuildRequest#getIoRedirects()}
         * @return this {@link BuildRequestBuilder}
         */
        public BuildRequestBuilder ioRedirects(IoRedirects ioRedirects) {
            this.ioRedirects = ioRedirects;
            return this;
        }

        /**
         * @param projectRootDirectory
         *                                 see {@link BuildRequest#getProjectRootDirectory()}
         * @return this {@link BuildRequestBuilder}
         */
        public BuildRequestBuilder projectRootDirectory(Path projectRootDirectory) {
            this.projectRootDirectory = projectRootDirectory;
            return this;
        }

        /**
         * @param scmUrl
         *                   a SCM url to add to {@link #scmUrls}. See {@link BuildRequest#getScmUrls()}
         * @return this {@link BuildRequestBuilder}
         */
        public BuildRequestBuilder scmUrl(String scmUrl) {
            this.scmUrls.add(scmUrl);
            return this;
        }

        /**
         * @param scmUrls
         *                    SCM urls to add to {@link #scmUrls}. See {@link BuildRequest#getScmUrls()}
         * @return this {@link BuildRequestBuilder}
         */
        public BuildRequestBuilder scmUrls(Collection<String> scmUrls) {
            this.scmUrls.addAll(scmUrls);
            return this;
        }

        /**
         * @param skipTests
         *                      see {@link BuildRequest#isSkipTests()}
         * @return this {@link BuildRequestBuilder}
         */
        public BuildRequestBuilder skipTests(boolean skipTests) {
            this.skipTests = skipTests;
            return this;
        }

        /**
         * @param srcVersion
         *                       see {@link BuildRequest#getSrcVersion()}
         * @return this {@link BuildRequestBuilder}
         */
        public BuildRequestBuilder srcVersion(SrcVersion srcVersion) {
            this.srcVersion = srcVersion;
            return this;
        }

        /**
         * @param timeoutMs
         *                      see {@link BuildRequest#getTimeoutMs()}
         * @return this {@link BuildRequestBuilder}
         */
        public BuildRequestBuilder timeoutMs(long timeoutMs) {
            this.timeoutMs = timeoutMs;
            return this;
        }

        /**
         * @param verbosity
         *                      see {@link BuildRequest#getVerbosity()}
         * @return this {@link BuildRequestBuilder}
         */
        public BuildRequestBuilder verbosity(Verbosity verbosity) {
            this.verbosity = verbosity;
            return this;
        }

        /**
         * If {@link #version} is not set using this method, {@link #srcVersion}.toString() will be used.
         *
         * @param version
         *                    see {@link BuildRequest#getVerbosity()}
         * @return this {@link BuildRequestBuilder}
         */
        public BuildRequestBuilder version(String version) {
            this.version = version;
            return this;
        }

        /**
         * @param versionsMavenPluginVersion
         *                                       see {@link BuildRequest#getVersionsMavenPluginVersion()}
         * @return this {@link BuildRequestBuilder}
         */
        public BuildRequestBuilder versionsMavenPluginVersion(String versionsMavenPluginVersion) {
            this.versionsMavenPluginVersion = versionsMavenPluginVersion;
            return this;
        }
    }

    /**
     * The verbosity level the appropriate {@link Builder} should use when executing a {@link BuildRequest}. The
     * interpretation of the individual levels is up to the given {@link Builder} implementation. Some {@link Builder}s
     * may map the levels listed here to a distinct set of levels they support internally.
     */
    public enum Verbosity {
        debug, error, info, trace, warn;

        public static Verbosity fastValueOf(String level) {
            SrcdepsCoreUtils.assertArgNotNull(level, "Verbosity name");
            switch (level.toLowerCase(Locale.ROOT)) {
            case "trace":
                return trace;
            case "debug":
                return debug;
            case "info":
                return info;
            case "warn":
                return warn;
            case "error":
                return error;
            default:
                throw new IllegalStateException("No such " + Verbosity.class.getName() + " with name [" + level + "]");
            }
        }

    }

    /** {@link Long#MAX_VALUE} */
    private static final long DEFAULT_TIMEOUT_MS = Long.MAX_VALUE;

    /**
     * @return a new {@link BuildRequestBuilder}
     */
    public static BuildRequestBuilder builder() {
        return new BuildRequestBuilder();
    }

    /**
     * Computes a hash out of the given {@link BuildRequest} fields. The fields are the ones whose change should trigger
     * a rebuild of the underlying source repository.
     *
     * @param addDefaultBuildArguments
     * @param addDefaultBuildEnvironment
     * @param buildArguments
     * @param buildEnvironment
     * @param forwardProperties
     * @param gavSet
     * @param scmUrls
     * @param skipTests
     * @param srcVersion
     * @param version
     * @param timeoutMs
     * @param verbosity
     * @return a sha1 hash in hex form
     */
    public static String computeHash(boolean addDefaultBuildArguments, boolean addDefaultBuildEnvironment,
            List<String> buildArguments, Map<String, String> buildEnvironment, Set<String> forwardProperties,
            GavSet gavSet, List<String> scmUrls, boolean skipTests, SrcVersion srcVersion, String version,
            long timeoutMs, Verbosity verbosity) {

        try (DigestOutputStream digester = new DigestOutputStream(MessageDigest.getInstance("SHA-1"))) {
            try (ObjectOutputStream out = new ObjectOutputStream(digester)) {
                out.writeBoolean(addDefaultBuildArguments);
                out.writeBoolean(addDefaultBuildEnvironment);
                for (String e : buildArguments) {
                    out.writeUTF(e);
                }
                for (Map.Entry<String, String> e : buildEnvironment.entrySet()) {
                    out.writeUTF(e.getKey());
                    out.writeUTF(e.getValue());
                }
                for (String e : forwardProperties) {
                    out.writeUTF(e);
                }

                StringBuilder sb = new StringBuilder();
                gavSet.appendIncludes(sb);
                gavSet.appendExcludes(sb);
                out.writeUTF(sb.toString());
                for (String e : scmUrls) {
                    out.writeUTF(e);
                }
                out.writeBoolean(skipTests);
                out.writeUTF(srcVersion.toString());
                out.writeUTF(version);
                out.writeLong(timeoutMs);
                out.writeUTF(verbosity.name());
            }
            digester.flush();
            final byte[] sha1Bytes = digester.digest();
            return SrcdepsCoreUtils.bytesToHexString(sha1Bytes);
        } catch (IOException | NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }

    }

    /**
     * @return the default timeout in milliseconds for source dependency builds. The value is {@link Long#MAX_VALUE}.
     */
    public static long getDefaultTimeoutMs() {
        return DEFAULT_TIMEOUT_MS;
    }

    private final boolean addDefaultBuildArguments;
    private final boolean addDefaultBuildEnvironment;
    private final List<String> buildArguments;
    private final Map<String, String> buildEnvironment;
    private final Path dependentProjectRootDirectory;
    private final Set<String> forwardProperties;
    private final GavSet gavSet;
    private final CharStreamSource gradleModelTransformer;
    private final String hash;
    private final IoRedirects ioRedirects;
    private final Path projectRootDirectory;
    private final List<String> scmUrls;
    private final boolean skipTests;
    private final SrcVersion srcVersion;
    private final long timeoutMs;
    private final Verbosity verbosity;
    private final String version;
    private final String versionsMavenPluginVersion;

    private BuildRequest(Path dependentProjectRootDirectory, Path projectRootDirectory, SrcVersion srcVersion,
            String version, GavSet gavSet, List<String> scmUrls, List<String> buildArguments, boolean skipTests,
            boolean addDefaultBuildArguments, Set<String> forwardProperties, Map<String, String> buildEnvironment,
            boolean addDefaultBuildEnvironment, Verbosity verbosity, IoRedirects ioRedirects, long timeoutMs,
            String versionsMavenPluginVersion, CharStreamSource gradleModelTransformer) {
        super();

        SrcdepsCoreUtils.assertArgNotNull(dependentProjectRootDirectory, "dependentProjectRootDirectory");
        SrcdepsCoreUtils.assertArgNotNull(projectRootDirectory, "projectRootDirectory");
        SrcdepsCoreUtils.assertArgNotNull(srcVersion, "srcVersion");
        SrcdepsCoreUtils.assertArgNotNull(version, "version");
        SrcdepsCoreUtils.assertArgNotNull(scmUrls, "scmUrls");
        SrcdepsCoreUtils.assertCollectionNotEmpty(scmUrls, "scmUrls");
        SrcdepsCoreUtils.assertArgNotNull(buildArguments, "buildArguments");
        SrcdepsCoreUtils.assertArgNotNull(forwardProperties, "forwardProperties");
        SrcdepsCoreUtils.assertArgNotNull(buildEnvironment, "buildEnvironment");
        SrcdepsCoreUtils.assertArgNotNull(ioRedirects, "ioRedirects");
        SrcdepsCoreUtils.assertArgNotNull(versionsMavenPluginVersion, "versionsMavenPluginVersion");
        SrcdepsCoreUtils.assertArgNotNull(gradleModelTransformer, "gradleModelTransformer");

        this.dependentProjectRootDirectory = dependentProjectRootDirectory;
        this.projectRootDirectory = projectRootDirectory;
        this.srcVersion = srcVersion;
        this.version = version;
        this.gavSet = gavSet;
        this.scmUrls = scmUrls;
        this.buildArguments = buildArguments;
        this.skipTests = skipTests;
        this.buildEnvironment = buildEnvironment;
        this.addDefaultBuildEnvironment = addDefaultBuildEnvironment;
        this.verbosity = verbosity;
        this.timeoutMs = timeoutMs;
        this.addDefaultBuildArguments = addDefaultBuildArguments;
        this.forwardProperties = forwardProperties;
        this.ioRedirects = ioRedirects;
        this.versionsMavenPluginVersion = versionsMavenPluginVersion;
        this.gradleModelTransformer = gradleModelTransformer;
        this.hash = computeHash(addDefaultBuildArguments, addDefaultBuildEnvironment, buildArguments, buildEnvironment,
                forwardProperties, gavSet, scmUrls, skipTests, srcVersion, versionsMavenPluginVersion, timeoutMs,
                verbosity);
    }

    /**
     * @return a {@link List} of arguments that should be used by {@link Builder#build(BuildRequest)}. Cannot be
     *         {@code null}. The {@link Builder} will combine this list with its default list of arguments if
     *         {@link #isAddDefaultBuildArguments()} returns {@code true}.
     */
    public List<String> getBuildArguments() {
        return buildArguments;
    }

    /**
     * @return a {@link Map} of environment variables that should be used by {@link Builder#build(BuildRequest)}. Cannot
     *         be {@code null}. Note that these are just overlay variables - if the {@link Builder} spawns a new
     *         process, the environment is copied from the present process and the variables provided by the present
     *         method are overwritten or added.
     */
    public Map<String, String> getBuildEnvironment() {
        return buildEnvironment;
    }

    /**
     * @return an absolute {@link Path} to the root directory of the dependent (outer) project tree.
     */
    public Path getDependentProjectRootDirectory() {
        return dependentProjectRootDirectory;
    }

    /**
     * Used rarely, mostly for debugging. A list of property names that the top level builder A should pass as java
     * system properties to every dependency builder B using {@code -DmyProperty=myValue} style command line arguments.
     * Further, in case a child builder B spawns its own new child builder C, B must pass all these properties to C in
     * the very same manner as A did to B.
     *
     * A property name may end with asterisk {@code *} to denote that all properties starting with the part before the
     * asterisk should be forwared. E.g. {@code my.prop.*} would forward both {@code my.prop.foo} and
     * {@code my.prop.bar}.
     *
     * @return a {@link Set} of properties to forward
     */
    public Set<String> getForwardProperties() {
        return forwardProperties;
    }

    /**
     * Returns the {@link GavSet} that defines the set of artifacts that should be built by this {@link BuildRequest}.
     * {@link Builder} implementations should make their best to build just this set of artifacts and nothing else.
     *
     * @return the {@link GavSet}
     */
    public GavSet getGavSet() {
        return gavSet;
    }

    /**
     * @return a {@link CharStreamSource} from which the Gradle model transformer script will be loaded.
     */
    public CharStreamSource getGradleModelTransformer() {
        return gradleModelTransformer;
    }

    /**
     * @return a hash over the values of only those fields whose changes require a rebuild
     * @since 3.2.2
     */
    public String getHash() {
        return hash;
    }

    /**
     * @return the {@link IoRedirects} to use when the {@link Builder} spawns new {@link Process}es
     */
    public IoRedirects getIoRedirects() {
        return ioRedirects;
    }

    /**
     * @return the root directory of the dependency project's source tree that should be built
     */
    public Path getProjectRootDirectory() {
        return projectRootDirectory;
    }

    /**
     * @return a {@link List} of URLs that should be tried one after another to checkout the version determined by
     *         {@link #getSrcVersion()}. The URLs will be tried by the {@link BuildService} in the given order until the
     *         checkout is successful. See {@link Scm} for the information about the format of the URLs.
     */
    public List<String> getScmUrls() {
        return scmUrls;
    }

    /**
     * @return the {@link SrcVersion} to checkout
     */
    public SrcVersion getSrcVersion() {
        return srcVersion;
    }

    /**
     * @return the timeout in milliseconds for the {@link Builder#setVersions(BuildRequest)} and
     *         {@link Builder#build(BuildRequest)} operations.
     */
    public long getTimeoutMs() {
        return timeoutMs;
    }

    /**
     * @return the verbosity level to use when configuring the {@link Builder}
     */
    public Verbosity getVerbosity() {
        return verbosity;
    }

    /**
     * @return the version of dependency artifacts to install to Local Maven Repository
     */
    public String getVersion() {
        return version;
    }

    /**
     * @return the version of {@code versions-maven-plugin} to use when setting versions in an inner Maven build.
     */
    public String getVersionsMavenPluginVersion() {
        return versionsMavenPluginVersion;
    }

    /**
     * @return {@code true} if the given {@link Builder}'s default arguments should be combined with the arguments
     *         returned by {@link #getBuildArguments()}; {@code false} otherwise
     */
    public boolean isAddDefaultBuildArguments() {
        return addDefaultBuildArguments;
    }

    /**
     * @return {@code true} if the given {@link Builder}'s default environment variables should be combined with the
     *         environment returned by {@link #getBuildEnvironment()}; {@code false} otherwise
     */
    public boolean isAddDefaultBuildEnvironment() {
        return addDefaultBuildEnvironment;
    }

    /**
     * @return {@code true} if no tests should be run when building the dependency. For dependencies built with Maven,
     *         this accounts to adding {@code -DskipTests} to the {@code mvn} arguments.
     */
    public boolean isSkipTests() {
        return skipTests;
    }

    @Override
    public String toString() {
        return "BuildRequest [addDefaultBuildArguments=" + addDefaultBuildArguments + ", addDefaultBuildEnvironment="
                + addDefaultBuildEnvironment + ", buildArguments=" + buildArguments + ", buildEnvironment="
                + buildEnvironment + ", dependentProjectRootDirectory=" + dependentProjectRootDirectory
                + ", forwardProperties=" + forwardProperties + ", gavSet=" + gavSet + ", gradleModelTransformer="
                + gradleModelTransformer + ", id=" + hash + ", ioRedirects=" + ioRedirects + ", projectRootDirectory="
                + projectRootDirectory + ", scmUrls=" + scmUrls + ", skipTests=" + skipTests + ", srcVersion="
                + srcVersion + ", timeoutMs=" + timeoutMs + ", verbosity=" + verbosity + ", version=" + version
                + ", versionsMavenPluginVersion=" + versionsMavenPluginVersion + "]";
    }

}
