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
package org.srcdeps.core.shell;

import java.io.IOException;
import java.nio.file.Paths;

import org.junit.Assert;
import org.junit.Test;
import org.srcdeps.core.BuildException;
import org.srcdeps.core.util.SrcdepsCoreUtils;

public class ShellTest {

    @Test
    public void output() throws BadExitCodeException, CommandTimeoutException, BuildException, IOException {

        if (!SrcdepsCoreUtils.isWindows()) {
            final LineConsumer out = LineConsumer.string();
            final ShellCommand cmd = ShellCommand.builder() //
                    .id("myCommand") //
                    .executable("/bin/bash") //
                    .arguments("-c", "echo stdout-msg; >&2 echo stderr-msg") //
                    .output(() -> out) //
                    .workingDirectory(Paths.get("."))
                    .build();
            Shell.execute(cmd).assertSuccess();
            final String actual = out.toString();
            /* The messages may come in any order */
            Assert.assertTrue("stdout-msg\nstderr-msg\n".equals(actual) || "stderr-msg\nstdout-msg\n".equals(actual));
        }

    }
}
