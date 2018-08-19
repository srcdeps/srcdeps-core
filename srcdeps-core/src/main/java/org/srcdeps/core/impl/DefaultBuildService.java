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
package org.srcdeps.core.impl;

import java.nio.file.Path;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.srcdeps.core.BuildException;
import org.srcdeps.core.BuildRequest;
import org.srcdeps.core.BuildService;
import org.srcdeps.core.Builder;

/**
 * The default implementation of {@link BuildService} that makes use of the {@link Builder}s injected by the DI
 * container.
 *
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */
@Named
@Singleton
public class DefaultBuildService implements BuildService {
    private static final Logger log = LoggerFactory.getLogger(DefaultBuildService.class);
    private final Set<Builder> builders;

    @Inject
    public DefaultBuildService(Set<Builder> builders) {
        super();
        this.builders = builders;
    }

    /** {@inheritDoc} */
    @Override
    public void build(BuildRequest request) throws BuildException {
        final Path dir = request.getProjectRootDirectory();

        for (Builder builder : builders) {
            if (builder.canBuild(dir)) {
                log.info("srcdeps: Building project in {} using Builder {}", dir, builder.getClass().getName());
                builder.setVersions(request);
                builder.build(request);
                return;
            }
        }
        throw new BuildException(String.format("No Builder found for directory [%s]", dir));
    }

}
