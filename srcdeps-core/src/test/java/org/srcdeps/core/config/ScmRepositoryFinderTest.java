/**
 * Copyright 2015-2016 Maven Source Dependencies
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

import java.nio.file.Paths;

import org.junit.Assert;
import org.junit.Test;
import org.srcdeps.core.BuildRequest.Verbosity;
import org.srcdeps.core.config.ScmRepository.Builder;
import org.srcdeps.core.config.tree.walk.DefaultsAndInheritanceVisitor;

public class ScmRepositoryFinderTest {

    @Test
    public void find() {

        Configuration config = Configuration.builder() //
                .configModelVersion("2.0").forwardProperty("myProp1") //
                .forwardProperty("myProp2") //
                .builderIo( //
                        BuilderIo.builder() //
                                .stdin("read:/path/to/input/file") //
                                .stdout("write:/path/to/output/file") //
                                .stderr("err2out") //
                ) //
                .skip(true) //
                .sourcesDirectory(Paths.get("/home/me/.m2/srcdeps")) //
                .verbosity(Verbosity.debug) //
                .repository(repo1()) //
                .repository(repo2()) //
                .accept(new DefaultsAndInheritanceVisitor()) //
                .build();

        ScmRepositoryFinder finder = new ScmRepositoryFinder(config);

        Assert.assertEquals("repo1", finder.findRepository("group1", "whatever", "whatever").getId());
        try {
            finder.findRepository("group2", "whatever", "whatever");
            Assert.fail(RuntimeException.class.getName() + " expected");
        } catch (RuntimeException expected) {
        }
        Assert.assertEquals("repo1", finder.findRepository("group2", "artifact2", "whatever").getId());
        Assert.assertEquals("repo2", finder.findRepository("group3", "artifact3", "whatever").getId());
        Assert.assertEquals("repo2", finder.findRepository("group4", "artifact4", "1.2.3").getId());

        try {
            finder.findRepository("group4", "artifact4", "1.2");
            Assert.fail(RuntimeException.class.getName() + " expected");
        } catch (RuntimeException expected) {
        }

    }

    private Builder repo1() {
        return ScmRepository.builder().id("repo1").selector("group1").selector("group2:artifact2:*").url("url1")
                .url("url2").buildArgument("-arg1").buildArgument("-arg2").addDefaultBuildArguments(false)
                .skipTests(false);
    }

    private Builder repo2() {
        return ScmRepository.builder().id("repo2").selector("group3:artifact3").selector("group4:artifact4:1.2.3")
                .url("url3").url("url4").buildArgument("arg3").addDefaultBuildArguments(false).skipTests(false);
    }
}
