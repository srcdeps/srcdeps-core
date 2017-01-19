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

import org.junit.Assert;
import org.junit.Test;

public class GavSetTest {

    @Test
    public void defaults() {
        GavSet set = GavSet.builder().build();
        Assert.assertTrue(set.contains("org.group1", "artifact1", "1.2.3"));
    }

    @Test
    public void excludeArtifact() {
        GavSet set = GavSet.builder() //
                .exclude("org.group1:artifact1") //
                .build();
        Assert.assertFalse(set.contains("org.group1", "artifact1", "1.2.3"));
        Assert.assertTrue(set.contains("org.group1", "artifact2", "2.3.4"));

        Assert.assertTrue(set.contains("org.group2", "artifact2", "5.6.7"));
        Assert.assertTrue(set.contains("org.group2", "artifact3", "6.7.8"));

        Assert.assertTrue(set.contains("com.group3", "artifact4", "5.6.7"));
    }

    @Test
    public void excludeGroups() {
        GavSet set = GavSet.builder() //
                .exclude("org.group1") //
                .exclude("org.group2") //
                .build();
        Assert.assertFalse(set.contains("org.group1", "artifact1", "1.2.3"));
        Assert.assertFalse(set.contains("org.group1", "artifact2", "2.3.4"));

        Assert.assertFalse(set.contains("org.group2", "artifact2", "5.6.7"));
        Assert.assertFalse(set.contains("org.group2", "artifact3", "6.7.8"));

        Assert.assertTrue(set.contains("com.group3", "artifact4", "5.6.7"));

    }

    @Test
    public void includeArtifact() {
        GavSet set = GavSet.builder() //
                .include("org.group1:artifact1") //
                .build();
        Assert.assertTrue(set.contains("org.group1", "artifact1", "1.2.3"));
        Assert.assertFalse(set.contains("org.group1", "artifact2", "2.3.4"));

        Assert.assertFalse(set.contains("org.group2", "artifact2", "5.6.7"));
        Assert.assertFalse(set.contains("org.group2", "artifact3", "6.7.8"));

        Assert.assertFalse(set.contains("com.group3", "artifact4", "5.6.7"));
    }

    @Test
    public void includeExcludeGroups() {
        GavSet set = GavSet.builder() //
                .include("org.group1") //
                .exclude("org.group2") //
                .build();
        Assert.assertTrue(set.contains("org.group1", "artifact1", "1.2.3"));
        Assert.assertTrue(set.contains("org.group1", "artifact2", "2.3.4"));

        Assert.assertFalse(set.contains("org.group2", "artifact2", "5.6.7"));
        Assert.assertFalse(set.contains("org.group2", "artifact3", "6.7.8"));

        Assert.assertFalse(set.contains("com.group3", "artifact4", "5.6.7"));

    }

    @Test
    public void includeGroup() {
        GavSet set = GavSet.builder() //
                .include("org.group1") //
                .build();
        Assert.assertTrue(set.contains("org.group1", "artifact1", "1.2.3"));
        Assert.assertTrue(set.contains("org.group1", "artifact2", "2.3.4"));

        Assert.assertFalse(set.contains("org.group2", "artifact2", "5.6.7"));
    }

    @Test
    public void includeGroups() {
        GavSet set = GavSet.builder() //
                .include("org.group1") //
                .include("org.group2") //
                .build();
        Assert.assertTrue(set.contains("org.group1", "artifact1", "1.2.3"));
        Assert.assertTrue(set.contains("org.group1", "artifact2", "2.3.4"));

        Assert.assertTrue(set.contains("org.group2", "artifact2", "5.6.7"));
        Assert.assertTrue(set.contains("org.group2", "artifact3", "6.7.8"));

        Assert.assertFalse(set.contains("com.group3", "artifact4", "5.6.7"));

    }

    @Test
    public void includeGroupsExcludeArtifact() {
        GavSet set = GavSet.builder() //
                .include("org.group1") //
                .include("org.group2") //
                .include("com.group3") //
                .exclude("org.group1:artifact2") //
                .exclude("org.group1:artifact3") //
                .exclude("org.group2:artifact2") //
                .exclude("org.group2:artifact3") //
                .build();
        Assert.assertTrue(set.contains("org.group1", "artifact1", "1.2.3"));
        Assert.assertFalse(set.contains("org.group1", "artifact2", "2.3.4"));
        Assert.assertFalse(set.contains("org.group1", "artifact3", "2.3.4"));

        Assert.assertTrue(set.contains("org.group2", "artifact1", "1.2.3"));
        Assert.assertFalse(set.contains("org.group2", "artifact2", "2.3.4"));
        Assert.assertFalse(set.contains("org.group2", "artifact3", "2.3.4"));

        Assert.assertTrue(set.contains("com.group3", "artifact1", "5.6.7"));
        Assert.assertTrue(set.contains("com.group3", "artifact2", "5.6.7"));
        Assert.assertTrue(set.contains("com.group3", "artifact3", "5.6.7"));
        Assert.assertTrue(set.contains("com.group3", "artifact4", "5.6.7"));

    }

}
