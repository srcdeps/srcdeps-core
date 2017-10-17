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
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Deque;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * A utility able to transform a {@link Gavtc} into the corresponding {@link Path} in the Local Maven Repository. You
 * will not need this class as long as you have Maven's Core libraries in the class path. This class is rather thought
 * for other build tools such as Gradle.
 *
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */
public class MavenLocalRepository {

    /**
     * A simple SAX parser to extract {@code <localRepository>} out of a Maven {@code settings.xml} file.
     */
    static class SettingsXmlParser extends DefaultHandler {

        private StringBuilder charBuffer;
        private String localRepositoryPath;
        /** The element stack */
        private Deque<String> stack = new java.util.ArrayDeque<String>();

        @Override
        public void characters(char[] ch, int start, int length) throws SAXException {
            if (charBuffer != null && localRepositoryPath == null) {
                charBuffer.append(ch, start, length);
            }
        }

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
            if (charBuffer != null) {
                stack.pop();
                if (stack.size() == 1 && "settings".equals(stack.peek())) {
                    localRepositoryPath = charBuffer.toString().trim();
                    charBuffer = null;
                }
            }
        }

        /**
         * Finds {@code <localRepository>} in the given {@code settingsXmlReader}, if found retruns it otherwise returns
         * {@code defaultResult}.
         *
         * @param settingsXmlReader
         * @param defaultResult
         * @return a {@link Path} to the Local Maven Repository on the current machine
         * @throws SAXException
         * @throws IOException
         * @throws ParserConfigurationException
         */
        Path parse(Reader settingsXmlReader, Path defaultResult)
                throws SAXException, IOException, ParserConfigurationException {
            SAXParser saxParser = SAXParserFactory.newInstance().newSAXParser();
            saxParser.parse(new InputSource(settingsXmlReader), this);
            if (localRepositoryPath != null) {
                return Paths.get(localRepositoryPath);
            } else {
                return defaultResult;
            }
        }

        @Override
        public void startDocument() throws SAXException {
            stack.clear();
            charBuffer = null;
            localRepositoryPath = null;
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes)
                throws SAXException {
            if (localRepositoryPath == null) {
                if ("localRepository".equals(qName)) {
                    charBuffer = new StringBuilder();
                }
                stack.push(qName);
            }
        }

    }

    public static final String MAVEN_REPO_LOCAL_PROP = "maven.repo.local";

    /**
     * Tries to figure out where is the Local Maven Repository on the current machine.
     * <p>
     * First checks
     *
     * @return a new {@link MavenLocalRepository}
     * @throws IOException
     */
    public static MavenLocalRepository autodetect() {
        final Path rootDirectory;
        final String repoPath = System.getProperty(MAVEN_REPO_LOCAL_PROP);
        if (repoPath != null && !repoPath.isEmpty()) {
            rootDirectory = Paths.get(repoPath);
        } else {
            final Path m2Directory = Paths.get(System.getProperty("user.home")).resolve(".m2");
            final Path settingsXmlPath = m2Directory.resolve("settings.xml");
            final Path defaultLocalRepoPath = m2Directory.resolve("repository");
            if (Files.exists(settingsXmlPath)) {
                try (Reader in = Files.newBufferedReader(settingsXmlPath, StandardCharsets.UTF_8)) {
                    rootDirectory = new SettingsXmlParser().parse(in, defaultLocalRepoPath);
                } catch (IOException | ParserConfigurationException | SAXException e) {
                    throw new RuntimeException("Could not parse " + settingsXmlPath, e);
                }
            } else {
                rootDirectory = defaultLocalRepoPath;
            }
        }
        return new MavenLocalRepository(rootDirectory);
    }

    private Path rootDirectory;

    public MavenLocalRepository(Path rootDirectory) {
        super();
        this.rootDirectory = rootDirectory;
    }

    public Path getRootDirectory() {
        return rootDirectory;
    }

    /**
     * @param gav
     *            the {@link Gavtc} to resolve
     * @return a {@link Path} under which the given artifact should exist in this {@link MavenLocalRepository}
     */
    public Path resolve(Gavtc gav) {
        return resolveGroup(gav.getGroupId()).resolve(gav.getArtifactId()).resolve(gav.getVersion())
                .resolve(gav.getArtifactId() + "-" + gav.getVersion()
                        + (gav.getClassifier() == null ? "" : "-" + gav.getClassifier()) + "." + gav.getType());
    }

    /**
     * @param groupId
     *            the {@code groupId} to resolve
     * @return a {@link Path} under which the given {@code groupId} lives in this {@link MavenLocalRepository}
     */
    public Path resolveGroup(String groupId) {
        return rootDirectory.resolve(groupId.replace('.', '/'));
    }

}
