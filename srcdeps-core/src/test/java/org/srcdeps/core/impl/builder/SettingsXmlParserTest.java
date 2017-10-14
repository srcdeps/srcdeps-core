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
import java.io.Reader;
import java.io.StringReader;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.xml.parsers.ParserConfigurationException;

import org.junit.Assert;
import org.junit.Test;
import org.xml.sax.SAXException;

/**
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 *
 */
public class SettingsXmlParserTest {

    @Test
    public void parseWithLocalRepositorySet() throws SAXException, IOException, ParserConfigurationException {
        String settingsXml = "<?xml version=\"1.0\"?>\n" +
                "\n" +
                "<settings xmlns=\"http://maven.apache.org/SETTINGS/1.0.0\"\n" +
                "    xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
                "    xsi:schemaLocation=\"http://maven.apache.org/SETTINGS/1.0.0\n" +
                "                              http://maven.apache.org/xsd/settings-1.0.0.xsd\">\n" +
                "    <localRepository>/my/local-repository</localRepository>\n" +
                "\n" +
                "    <servers>\n" +
                "        <server>\n" +
                "            <id>ossrh</id>\n" +
                "            <username>foo</username>\n" +
                "            <password>secret</password>\n" +
                "        </server>\n" +
                "    </servers>\n" +
                "</settings>";
        try (Reader in = new StringReader(settingsXml)) {
            Path actual = new MavenLocalRepository.SettingsXmlParser().parse(in, null);
            Assert.assertEquals(Paths.get("/my/local-repository"), actual);
        }

    }

    @Test
    public void parseWithoutLocalRepository() throws SAXException, IOException, ParserConfigurationException {
        String settingsXml = "<?xml version=\"1.0\"?>\n" +
                "\n" +
                "<settings xmlns=\"http://maven.apache.org/SETTINGS/1.0.0\"\n" +
                "    xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
                "    xsi:schemaLocation=\"http://maven.apache.org/SETTINGS/1.0.0\n" +
                "                              http://maven.apache.org/xsd/settings-1.0.0.xsd\">\n" +
                "    <servers>\n" +
                "        <server>\n" +
                "            <id>ossrh</id>\n" +
                "            <username>foo</username>\n" +
                "            <password>secret</password>\n" +
                "        </server>\n" +
                "    </servers>\n" +
                "</settings>";
        try (Reader in = new StringReader(settingsXml)) {
            Path actual = new MavenLocalRepository.SettingsXmlParser().parse(in, Paths.get("/my/default-repository"));
            Assert.assertEquals(Paths.get("/my/default-repository"), actual);
        }

    }

}
