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
package org.srcdeps.core;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.eclipse.sisu.launch.InjectedTest;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.srcdeps.core.BuildRequest.BuildRequestBuilder;
import org.srcdeps.core.config.Maven;
import org.srcdeps.core.config.scalar.CharStreamSource;
import org.srcdeps.core.shell.LineConsumer;
import org.srcdeps.core.util.SrcdepsCoreUtils;

/**
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */
public class AbstractBuildServiceTest extends InjectedTest {

    public interface BuilderTransformer {
        BuildRequestBuilder transform(BuildRequestBuilder builder);
    }

    private static final Path dependentProjectRootDirectory;

    private static final Logger log = LoggerFactory.getLogger(AbstractBuildServiceTest.class);
    private static final String mrmSettingsXmlPath = System.getProperty("mrm.settings.xml");
    protected static final MavenLocalRepository mvnLocalRepo;
    protected static final BuilderTransformer NO_TRANSFORMER = new BuilderTransformer() {

        @Override
        public BuildRequestBuilder transform(BuildRequestBuilder builder) {
            return builder;
        }
    };
    private static final Path projectsDirectory;

    private static final Path targetDirectory = Paths.get(System.getProperty("project.build.directory", "target"))
            .toAbsolutePath();

    static {
        projectsDirectory = targetDirectory.resolve("test-projects");
        dependentProjectRootDirectory = targetDirectory.resolve("classes");
        Path mvnLocalRepoPath = targetDirectory.resolve("mvn-local-repo");
        mvnLocalRepo = new MavenLocalRepository(mvnLocalRepoPath);
    }

    public static void assertExists(Path path) {
        Assert.assertTrue(String.format("File or directory does not exist [%s]", path.toString()), Files.exists(path));
    }

    @BeforeClass
    public static void beforeClass() throws IOException {

        SrcdepsCoreUtils.ensureDirectoryExistsAndEmpty(mvnLocalRepo.getRootDirectory());

        System.setProperty(Maven.getSrcdepsMavenSettingsProperty(), mrmSettingsXmlPath);

    }

    protected static List<Path> resolve(String srcVersion, String... gavtcPatterns) {
        final List<Path> paths = new ArrayList<>(gavtcPatterns.length * 2);
        for (String gavtcPattern : gavtcPatterns) {
            gavtcPattern = gavtcPattern.replace("${version}", srcVersion);
            for (Gavtc gavtc : Gavtc.ofPattern(gavtcPattern)) {
                final Path path = mvnLocalRepo.resolve(gavtc);
                paths.add(path);
            }
        }
        return paths;
    }

    @Inject
    private BuildService buildService;

    protected String currentTestName;

    @Inject
    private ScmService scmService;

    @Rule
    public TestRule watcher = new TestWatcher() {

        @Override
        protected void starting(Description description) {
            AbstractBuildServiceTest.this.currentTestName = description.getMethodName();
        }

    };

    protected void assertBuild(String gitRepoUri, String srcVersion, BuilderTransformer builderTransformer,
            int expectedLogFileCount, String... gavtcTemplates) throws IOException, BuildException {
        Assert.assertNotNull("buildService not injected", buildService);
        log.info("Using {} as {}", buildService.getClass().getName(), BuildService.class.getName());

        final Path projectRoot = projectsDirectory.resolve(currentTestName);
        final Path logPath = projectRoot.resolve("log.txt");
        final Path projectBuildDirectory = projectRoot.resolve("build");

        {
            Assert.assertFalse(Files.exists(logPath));
            int i = 1;
            Assert.assertFalse(Files.exists(projectRoot.resolve("log-" + (i++) + ".txt")));
            Assert.assertFalse(Files.exists(projectRoot.resolve("log-" + (i++) + ".txt")));
            Assert.assertFalse(Files.exists(projectRoot.resolve("log-" + (i++) + ".txt")));
        }

        final List<Path> paths = resolve(srcVersion, gavtcTemplates);

        SrcdepsCoreUtils.deleteDirectory(projectRoot);

        for (Path path : paths) {
            SrcdepsCoreUtils.deleteDirectory(path.getParent());
        }

        BuildRequestBuilder requestBuilder = BuildRequest.builder() //
                .scmRepositoryId(currentTestName) //
                .encoding(StandardCharsets.UTF_8) //
                .scmUrl(gitRepoUri) //
                .srcVersion(SrcVersion.parse(srcVersion)) //
                .projectRootDirectory(projectBuildDirectory) //
                .dependentProjectRootDirectory(dependentProjectRootDirectory) //
                .output(() -> LineConsumer.tee( //
                        LineConsumer.logger(currentTestName, log), //
                        LineConsumer.rotate(logPath, 4) //
                )) //
                .buildArgument("-Dmaven.repo.local=" + mvnLocalRepo.getRootDirectory().toString()) //
                .versionsMavenPluginVersion(Maven.getDefaultVersionsMavenPluginVersion()) //
                .gradleModelTransformer(CharStreamSource.defaultModelTransformer()) //
        ;

        builderTransformer.transform(requestBuilder);

        BuildRequest request = requestBuilder.build();

        scmService.checkout(request);
        buildService.build(request);

        for (Path path : paths) {
            assertExists(path);
        }
        {
            Assert.assertTrue(Files.exists(logPath));
            int i = 1;
            while (i < expectedLogFileCount) {
                final Path path = projectRoot.resolve("log-" + (i++) + ".txt");
                Assert.assertTrue("Should exist: " + path, Files.exists(path));
            }
            final Path path = projectRoot.resolve("log-" + (i++) + ".txt");
            Assert.assertFalse("Should not exist: " + path, Files.exists(projectRoot.resolve("log-" + (i++) + ".txt")));
        }

    }

    protected void assertBuild(String gitRepoUri, String srcVersion, String... gavtcTemplates)
            throws IOException, BuildException {
        assertBuild(gitRepoUri, srcVersion, NO_TRANSFORMER, 1, gavtcTemplates);
    }

}
