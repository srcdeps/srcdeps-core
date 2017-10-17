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
package org.srcdeps.core;

import java.io.IOException;
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
import org.srcdeps.core.util.SrcdepsCoreUtils;

/**
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */
public class AbstractBuildServiceTest extends InjectedTest {

    public interface BuilderTransformer {
        BuildRequestBuilder transform(BuildRequestBuilder builder);
    }

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

    @Rule
    public TestRule watcher = new TestWatcher() {

        @Override
        protected void starting(Description description) {
            AbstractBuildServiceTest.this.currentTestName = description.getMethodName();
        }

    };

    protected void assertBuild(String gitRepoUri, String srcVersion, BuilderTransformer builderTransformer,
            String... gavtcTemplates) throws IOException, BuildException {
        Assert.assertNotNull("buildService not injected", buildService);
        log.info("Using {} as {}", buildService.getClass().getName(), BuildService.class.getName());

        final Path projectRoot = projectsDirectory.resolve(currentTestName);
        final Path projectBuildDirectory = projectRoot.resolve("build");

        final List<Path> paths = resolve(srcVersion, gavtcTemplates);

        SrcdepsCoreUtils.deleteDirectory(projectRoot);

        for (Path path : paths) {
            SrcdepsCoreUtils.deleteDirectory(path.getParent());
        }

        BuildRequestBuilder requestBuilder = BuildRequest.builder() //
                .scmUrl(gitRepoUri) //
                .srcVersion(SrcVersion.parse(srcVersion)).projectRootDirectory(projectBuildDirectory) //
                .buildArgument("-Dmaven.repo.local=" + mvnLocalRepo.getRootDirectory().toString()) //
                .versionsMavenPluginVersion(Maven.getDefaultVersionsMavenPluginVersion()) //
                ;

        builderTransformer.transform(requestBuilder);

        buildService.build(requestBuilder.build());

        for (Path path : paths) {
            assertExists(path);
        }

    }

    protected void assertBuild(String gitRepoUri, String srcVersion, String... gavtcTemplates)
            throws IOException, BuildException {
        assertBuild(gitRepoUri, srcVersion, NO_TRANSFORMER, gavtcTemplates);
    }

}
