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
package org.srcdeps.core.impl.builder.gradle;

/**
 * A part source file that can span over multiple lines. Note that all indexes are zero based - i.e. the first line in a
 * file has index 0.
 *
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */
public class SourceSpan {
    final int endColumnIndex;
    final int endLineIndex;
    final int startColumnIndex;
    final int startLineIndex;

    public SourceSpan(int startLineIndex, int startColumnIndex, int endLineIndex, int endColumnIndex) {
        super();
        this.startLineIndex = startLineIndex;
        this.startColumnIndex = startColumnIndex;
        this.endLineIndex = endLineIndex;
        this.endColumnIndex = endColumnIndex;
    }

    /**
     * @param lineIndex
     *            a zero based line index
     * @return {@code true} if this {@link SourceSpan} overlaps with the given {@code lineIndex}; {@code false}
     *         otherwise
     */
    public boolean containsLineIndex(int lineIndex) {
        return startLineIndex <= lineIndex && lineIndex < endLineIndex;
    }

    /**
     * @return a zero based column index
     */
    public int getEndColumnIndex() {
        return endColumnIndex;
    }

    /**
     * @return a zero based line index
     */
    public int getEndLineIndex() {
        return endLineIndex;
    }

    /**
     * @return a zero based column index
     */
    public int getStartColumnIndex() {
        return startColumnIndex;
    }

    /**
     * @return a zero based line index
     */
    public int getStartLineIndex() {
        return startLineIndex;
    }

    @Override
    public String toString() {
        return "SourceSpan [endColumnIndex=" + endColumnIndex + ", endLineIndex=" + endLineIndex + ", startColumnIndex="
                + startColumnIndex + ", startLineIndex=" + startLineIndex + "]";
    }
}