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
package org.srcdeps.config.yaml.writer;

/**
 * A few ways to configure the output to YAML format.
 *
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */
public class YamlWriterConfiguration {

    public static class Builder {
        private char indentChar = DEFAULT_INDENT_CHAR;
        private int indentLength = DEFAULT_INDENT_LENGTH;
        private String newLine = DEFAULT_NEWLINE;

        public YamlWriterConfiguration build() {
            return new YamlWriterConfiguration(indentLength, indentChar, newLine);
        }

        public Builder indentChar(char indentChar) {
            this.indentChar = indentChar;
            return this;
        }

        public Builder indentLength(int indentLength) {
            this.indentLength = indentLength;
            return this;
        }

        public Builder newLine(String newLine) {
            this.newLine = newLine;
            return this;
        }
    }

    private static final char DEFAULT_INDENT_CHAR = ' ';

    private static final int DEFAULT_INDENT_LENGTH = 2;

    private static final String DEFAULT_NEWLINE = "\n";

    public static Builder builder() {
        return new Builder();
    }

    public static char getDefaultIndentChar() {
        return DEFAULT_INDENT_CHAR;
    }

    public static int getDefaultIndentLength() {
        return DEFAULT_INDENT_LENGTH;
    }

    public static String getDefaultNewline() {
        return DEFAULT_NEWLINE;
    }

    private final char indentChar;

    private final int indentLength;

    private final String newLine;

    private YamlWriterConfiguration(int indentLength, char indentChar, String newLine) {
        super();
        this.indentLength = indentLength;
        this.indentChar = indentChar;
        this.newLine = newLine;
    }

    /**
     * @return the character to use for indentation, default is space
     */
    public char getIndentChar() {
        return indentChar;
    }

    /**
     * @return the number of indentation characters to use for one level of indentation
     */
    public int getIndentLength() {
        return indentLength;
    }

    /**
     * @return the new line String. The default is {@code "\n"}
     */
    public String getNewLine() {
        return newLine;
    }
}
