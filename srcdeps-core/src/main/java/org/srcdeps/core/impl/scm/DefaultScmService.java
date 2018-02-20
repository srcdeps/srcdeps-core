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
package org.srcdeps.core.impl.scm;

import java.nio.file.Path;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.srcdeps.core.BuildRequest;
import org.srcdeps.core.Scm;
import org.srcdeps.core.ScmException;
import org.srcdeps.core.ScmService;
import org.srcdeps.core.impl.DefaultBuildService;

/**
 * A {@link ScmService} based on a {@link Set} of {@link Scm}s.
 *
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 * @since 3.2.1
 */
@Named
@Singleton
public class DefaultScmService implements ScmService {
    private static final Logger log = LoggerFactory.getLogger(DefaultBuildService.class);
    private final Set<Scm> scms;

    @Inject
    public DefaultScmService(Set<Scm> scms) {
        super();
        this.scms = scms;
    }

    /** {@inheritDoc} */
    @Override
    public String checkout(BuildRequest request) throws ScmException {
        final Path dir = request.getProjectRootDirectory();
        final String firstUrl = request.getScmUrls().iterator().next();
        log.info("About to build request {}", request);
        for (Scm scm : scms) {
            if (scm.supports(firstUrl)) {
                log.info("About to use Scm implementation {} to check out URL {} to directory {}",
                        scm.getClass().getName(), firstUrl, dir);
                return scm.checkout(request);
            }
        }
        throw new ScmException(String.format("No Scm found for URL [%s]", firstUrl));
    }

}
