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

import org.junit.Assert;
import org.junit.Test;
import org.srcdeps.core.config.ScmRepositoryFinder.SelectionResolver;

public class SelectionResolverTest {
    @Test
    public void resolveAny() {
        SelectionResolver resolver = new SelectionResolver("*:*:*", null);

        Assert.assertTrue(resolver.matches("group", "artifact", "1.2.3"));
        Assert.assertTrue(resolver.matches("group1", "artifact", "1.2.3"));
        Assert.assertTrue(resolver.matches("group", "artifact1", "1.2.3"));
        Assert.assertTrue(resolver.matches("group", "artifact", "1.2.4"));

        resolver = new SelectionResolver("", null);

        Assert.assertTrue(resolver.matches("group", "artifact", "1.2.3"));
        Assert.assertTrue(resolver.matches("group1", "artifact", "1.2.3"));
        Assert.assertTrue(resolver.matches("group", "artifact1", "1.2.3"));
        Assert.assertTrue(resolver.matches("group", "artifact", "1.2.4"));

    }

    @Test
    public void resolveAnyArtifactOrVersion() {
        SelectionResolver resolver = new SelectionResolver("group:*:*", null);

        Assert.assertTrue(resolver.matches("group", "artifact", "1.2.3"));
        Assert.assertFalse(resolver.matches("group1", "artifact", "1.2.3"));
        Assert.assertTrue(resolver.matches("group", "artifact1", "1.2.3"));
        Assert.assertTrue(resolver.matches("group", "artifact", "1.2.4"));

        resolver = new SelectionResolver("group", null);

        Assert.assertTrue(resolver.matches("group", "artifact", "1.2.3"));
        Assert.assertFalse(resolver.matches("group1", "artifact", "1.2.3"));
        Assert.assertTrue(resolver.matches("group", "artifact1", "1.2.3"));
        Assert.assertTrue(resolver.matches("group", "artifact", "1.2.4"));

    }


    @Test
    public void resolveAnyVersion() {
        SelectionResolver resolver = new SelectionResolver("group:artifact:*", null);

        Assert.assertTrue(resolver.matches("group", "artifact", "1.2.3"));
        Assert.assertFalse(resolver.matches("group1", "artifact", "1.2.3"));
        Assert.assertFalse(resolver.matches("group", "artifact1", "1.2.3"));
        Assert.assertTrue(resolver.matches("group", "artifact", "1.2.4"));

        resolver = new SelectionResolver("group:artifact", null);

        Assert.assertTrue(resolver.matches("group", "artifact", "1.2.3"));
        Assert.assertFalse(resolver.matches("group1", "artifact", "1.2.3"));
        Assert.assertFalse(resolver.matches("group", "artifact1", "1.2.3"));
        Assert.assertTrue(resolver.matches("group", "artifact", "1.2.4"));

    }


    @Test
    public void resolveLiteral() {
        SelectionResolver resolver = new SelectionResolver("group:artifact:1.2.3", null);

        Assert.assertTrue(resolver.matches("group", "artifact", "1.2.3"));
        Assert.assertFalse(resolver.matches("group1", "artifact", "1.2.3"));
        Assert.assertFalse(resolver.matches("group", "artifact1", "1.2.3"));
        Assert.assertFalse(resolver.matches("group", "artifact", "1.2.4"));
    }
}
