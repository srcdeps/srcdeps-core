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

import java.util.List;

import org.srcdeps.core.config.ScmRepository;

/**
 * A composition of {@code scmRepoId} and {@code urls} having the purpose to identify a fetch from a remote repository.
 *
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 * @since 3.2.1
 */
public class FetchId {
    private final String scmRepoId;
    private final List<String> urls;

    public FetchId(String scmRepoId, List<String> urls) {
        super();
        this.scmRepoId = scmRepoId;
        this.urls = urls;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((scmRepoId == null) ? 0 : scmRepoId.hashCode());
        result = prime * result + ((urls == null) ? 0 : urls.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        FetchId other = (FetchId) obj;
        if (scmRepoId == null) {
            if (other.scmRepoId != null)
                return false;
        } else if (!scmRepoId.equals(other.scmRepoId))
            return false;
        if (urls == null) {
            if (other.urls != null)
                return false;
        } else if (!urls.equals(other.urls))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return scmRepoId + ":" + urls;
    }

    /**
     * @return see {@link ScmRepository#getId()}
     */
    public String getScmRepoId() {
        return scmRepoId;
    }

    /**
     * @return see {@link ScmRepository#getUrls()}
     */
    public List<String> getUrls() {
        return urls;
    }
}
