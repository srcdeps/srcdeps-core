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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.srcdeps.core.BuildException;
import org.srcdeps.core.BuildRequest;
import org.srcdeps.core.BuildRequest.Verbosity;
import org.srcdeps.core.impl.builder.gradle.BuildGradleEditor;

/**
 * A base for {@link GradleBuilder} and {@link GradlewBuilder}.
 *
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */
public abstract class AbstractGradleBuilder extends ShellBuilder {
    protected static final List<String> BUILD_GRADLE_FILE_NAMES = Collections.singletonList("build.gradle");
    protected static final List<String> GRADLE_DEFAULT_ARGS = Collections
            .unmodifiableList(Arrays.asList("clean", "install"));

    protected static final List<String> GRADLEW_FILE_NAMES = Collections
            .unmodifiableList(Arrays.asList("gradlew", "gradlew.bat"));

    // Maybe also "-x", "integTest" ?
    protected static final List<String> SKIP_TESTS_ARGS = Collections
            .unmodifiableList(Arrays.asList("-x", "test"));

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

    public AbstractGradleBuilder(String executable) {
        super(executable);
    }

    @Override
    protected List<String> getDefaultBuildArguments() {
        return GRADLE_DEFAULT_ARGS;
    }

    @Override
    protected List<String> getSkipTestsArguments(boolean skipTests) {
        return skipTests ? SKIP_TESTS_ARGS : Collections.<String> emptyList();
    }

    @Override
    protected List<String> getVerbosityArguments(Verbosity verbosity) {
        switch (verbosity) {
        case trace:
        case debug:
            return Collections.singletonList("--debug");
        case info:
            return Collections.emptyList();
        case warn:
        case error:
            return Collections.singletonList("--quiet");
        default:
            throw new IllegalStateException("Unexpected " + Verbosity.class.getName() + " value [" + verbosity + "]");
        }
    }

    @Override
    public void setVersions(BuildRequest request) throws BuildException {
        Path buildGradle = request.getProjectRootDirectory().resolve("build.gradle");
        if (Files.exists(buildGradle)) {
            try {
                new BuildGradleEditor(buildGradle).setVersion("'" + request.getSrcVersion().toString() + "'").store();
            } catch (IOException e) {
                throw new BuildException(String.format("Could not change the version in file [%s]", buildGradle), e);
            }
        } else {
            throw new BuildException(String.format("File not found [%s]", buildGradle));
        }
    }

}
