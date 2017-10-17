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
package org.srcdeps.core.impl.builder;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.srcdeps.core.BuildException;
import org.srcdeps.core.BuildRequest;
import org.srcdeps.core.BuildRequest.Verbosity;
import org.srcdeps.core.GavPattern;
import org.srcdeps.core.GavSet;
import org.srcdeps.core.SrcdepsInner;
import org.srcdeps.core.util.SrcdepsCoreUtils;

/**
 * A base for {@link GradleBuilder} and {@link GradlewBuilder}.
 *
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */
public abstract class AbstractGradleBuilder extends ShellBuilder {
    protected static final List<String> BUILD_GRADLE_FILE_NAMES = Collections.singletonList("build.gradle");

    protected static final Path SRCDEPS_TRANSFORM_GRADLE = Paths.get("srcdeps-transform.gradle");
    protected static final Pattern PACKAGE_PATTERN = Pattern.compile(".*package +[^;]+;", Pattern.DOTALL);

    protected static final List<String> GRADLE_DEFAULT_ARGS = Collections
            .unmodifiableList(Arrays.asList("clean", "install", "--no-daemon"));
    protected static final List<String> GRADLEW_FILE_NAMES = Collections
            .unmodifiableList(Arrays.asList("gradlew", "gradlew.bat"));

    protected static final List<String> INNER_CLASSES = Collections.unmodifiableList(Arrays.asList(//
            GavPattern.class.getSimpleName() + ".gradle", //
            GavSet.class.getSimpleName() + ".gradle", //
            SrcdepsInner.class.getSimpleName() + ".gradle" //
    ));

    protected static final List<String> SKIP_TESTS_ARGS = Collections.emptyList();

    /**
     * @return the list of file names that can store Gradle build scripts.
     */
    public static List<String> getBuildGradleFileNames() {
        return BUILD_GRADLE_FILE_NAMES;
    }

    /**
     * @return the default build arguments used in Gradle builds of source dependencies
     */
    public static List<String> getGradleDefaultArgs() {
        return GRADLE_DEFAULT_ARGS;
    }

    /**
     * @return the list of file names whose presence signals that the project can be built with {@link GradlewBuilder}
     */
    public static List<String> getGradlewFileNames() {
        return GRADLEW_FILE_NAMES;
    }

    /**
     * @return the {@link List} of arguments to use when no tests should be run during the build of a source dependency
     */
    public static List<String> getSkipTestsArgs() {
        return SKIP_TESTS_ARGS;
    }

    public static boolean hasBuildGradleFile(Path directory) {
        for (String fileName : BUILD_GRADLE_FILE_NAMES) {
            if (directory.resolve(fileName).toFile().exists()) {
                return true;
            }
        }
        return false;
    }

    public static boolean hasGradlewFile(Path directory) {
        for (String fileName : GRADLEW_FILE_NAMES) {
            if (directory.resolve(fileName).toFile().exists()) {
                return true;
            }
        }
        return false;
    }

    protected final Map<String, String> defaultBuildEnvironment;

    public AbstractGradleBuilder(String executable) {
        super(executable);
        this.defaultBuildEnvironment = Collections.emptyMap();
    }

    @Override
    protected List<String> getDefaultBuildArguments() {
        return GRADLE_DEFAULT_ARGS;
    }

    @Override
    protected Map<String, String> getDefaultBuildEnvironment() {
        return defaultBuildEnvironment;
    }

    @Override
    protected List<String> getSkipTestsArguments(boolean skipTests) {
        return skipTests ? SKIP_TESTS_ARGS : Collections.<String>emptyList();
    }

    @Override
    protected List<String> getVerbosityArguments(Verbosity verbosity) {
        switch (verbosity) {
        case trace:
            return Collections.unmodifiableList(Arrays.asList("--debug", "--full-stacktrace"));
        case debug:
            return Collections.unmodifiableList(Arrays.asList("--debug", "--stacktrace"));
        case info:
            return Collections.emptyList();
        case warn:
            return Collections.singletonList("--warn");
        case error:
            return Collections.singletonList("--quiet");
        default:
            throw new IllegalStateException("Unexpected " + Verbosity.class.getName() + " value [" + verbosity + "]");
        }
    }

    @Override
    protected List<String> mergeArguments(BuildRequest request) {
        List<String> result = new ArrayList<>(super.mergeArguments(request));
        result.add("-Dsrcdeps.inner.version=" + request.getSrcVersion().toString());

        GavSet gavSet = request.getGavSet();

        try {
            StringBuilder sb = new StringBuilder("-Dsrcdeps.inner.includes=");
            gavSet.appendIncludes(sb);
            result.add(sb.toString());

            sb.setLength(0);
            sb.append("-Dsrcdeps.inner.excludes=");
            gavSet.appendExcludes(sb);
            result.add(sb.toString());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return result;
    }

    @Override
    public void setVersions(BuildRequest request) throws BuildException {
        Path buildGradle = request.getProjectRootDirectory().resolve("build.gradle");
        try {
            if (!Files.exists(buildGradle)) {
                throw new BuildException(String.format("File not found [%s]", buildGradle));
            }

            final Path rootPath = request.getProjectRootDirectory();
            final StringBuilder settingsAppendix = new StringBuilder("\n");

            final char[] buf = new char[10240];

            for (String innerClass : INNER_CLASSES) {
                String srcdepsInnerSrc = SrcdepsCoreUtils.read( //
                        getClass().getResource("/gradle/settings/" + innerClass), //
                        buf //
                );
                srcdepsInnerSrc = PACKAGE_PATTERN.matcher(srcdepsInnerSrc).replaceFirst("");
                settingsAppendix.append(srcdepsInnerSrc).append("\n");
            }

            settingsAppendix.append("def srcdepsInner = new SrcdepsInner()\n");

            try (Reader r = request.getGradleModelTransformer().openReader(StandardCharsets.UTF_8,
                    request.getDependentProjectRootDirectory())) {
                String src = SrcdepsCoreUtils.read(r, buf);
                settingsAppendix.append(src).append("\n");
            }

            final Path settingsGradlePath = rootPath.resolve("settings.gradle");
            if (Files.exists(settingsGradlePath)) {
                Files.write(settingsGradlePath, settingsAppendix.toString().getBytes(StandardCharsets.UTF_8),
                        StandardOpenOption.APPEND);
            } else {
                Files.write(settingsGradlePath, settingsAppendix.toString().getBytes(StandardCharsets.UTF_8));
            }

        } catch (IOException e) {
            throw new BuildException(String.format("Could not change the version in file [%s]", buildGradle), e);
        }

    }

}
