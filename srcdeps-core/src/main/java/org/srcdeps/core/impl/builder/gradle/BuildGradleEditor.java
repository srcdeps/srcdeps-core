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

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import org.codehaus.groovy.ast.ASTNode;
import org.codehaus.groovy.ast.CodeVisitorSupport;
import org.codehaus.groovy.ast.builder.AstBuilder;
import org.codehaus.groovy.ast.expr.BinaryExpression;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.VariableExpression;
import org.codehaus.groovy.ast.stmt.BlockStatement;
import org.codehaus.groovy.ast.stmt.ExpressionStatement;
import org.codehaus.groovy.ast.stmt.Statement;

/**
 * A utility to edit {@code build.gradle} files.
 *
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */
public class BuildGradleEditor {

    /**
     * A utility to find the {@link SourceSpan} where the version number is defined in a {@code build.gradle} file.
     */
    private static class VersionVisitor extends CodeVisitorSupport {

        private static final int EXPECTED_VERSION_EXPRESSION_DEPTH = 1;
        private static final String VERSION_CONSTANT = "version";
        private int depth = 0;

        private final Path path;

        private SourceSpan result = null;

        private VersionVisitor(Path path) {
            super();
            this.path = path;
        }

        public SourceSpan assertFound() {
            if (result == null) {
                throw new IllegalStateException(String.format("Could not find %s in file %s", VERSION_CONSTANT, path));
            }
            return result;
        }

        @Override
        public void visitBlockStatement(BlockStatement node) {
            depth++;
            for (Statement child : node.getStatements()) {
                child.visit(this);
            }
            depth--;
        }

        @Override
        public void visitExpressionStatement(ExpressionStatement node) {
            if (depth == EXPECTED_VERSION_EXPRESSION_DEPTH) {
                final Expression expr = node.getExpression();
                if (expr instanceof BinaryExpression) {
                    final BinaryExpression binaryExpression = (BinaryExpression) expr;
                    final Expression left = binaryExpression.getLeftExpression();
                    if (left instanceof VariableExpression
                            && VERSION_CONSTANT.equals(((VariableExpression) left).getName())) {
                        final Expression right = binaryExpression.getRightExpression();
                        result = new SourceSpan(right.getLineNumber() - 1, right.getColumnNumber() - 1,
                                right.getLastLineNumber(), right.getLastColumnNumber());
                    }
                }
            }
        }
    }

    /** The path to the {@code build.gradle} file */
    private final Path path;

    /** The content of the {@code build.gradle} file */
    private String source;

    public BuildGradleEditor(Path path) throws IOException {
        this.path = path;
        this.source = new String(Files.readAllBytes(path), Charset.forName("utf-8"));
    }

    public BuildGradleEditor(String source) throws IOException {
        this.path = null;
        this.source = source;
    }

    /**
     * Replaces the given {@code span} with the given {@code replacement}
     *
     * @param span
     *            the span to replace
     * @param replacement
     *            the string to replace the {@code span} with
     * @return this {@link BuildGradleEditor}
     */
    public BuildGradleEditor replace(SourceSpan span, String replacement) {
        StringBuilder result = new StringBuilder(source.length() + replacement.length());

        String[] lines = source.split("\n");
        for (int i = 0; i < lines.length; i++) {
            final String line = lines[i];
            final int lineLength = line.length();
            if (span.containsLineIndex(i)) {
                int start = 0;
                int end = lineLength;
                if (i == span.startLineIndex) {
                    start = span.startColumnIndex;
                }
                if (i + 1 == span.endLineIndex) {
                    end = span.endColumnIndex;
                }
                boolean appended = false;
                if (start > 0) {
                    result.append(line, 0, start);
                    appended = true;
                }
                if (i == span.startLineIndex) {
                    /* append the replacement only once */
                    result.append(replacement);
                    if (replacement.length() > 0) {
                        appended = true;
                    }
                }
                if (end < lineLength) {
                    result.append(line, end, lineLength);
                    appended = true;
                }
                if (appended) {
                    result.append('\n');
                }
            } else {
                result.append(line).append('\n');
            }
        }
        source = result.toString();
        return this;
    }

    /**
     * Sets the version in {@link #source} to the given {@code newVersion}.
     *
     * @param newVersion
     *            the version to set
     * @return this {@link BuildGradleEditor}
     */
    public BuildGradleEditor setVersion(String newVersion) {
        List<ASTNode> nodes = new AstBuilder().buildFromString(source);
        VersionVisitor versionVisitor = new VersionVisitor(path);
        for (ASTNode node : nodes) {
            node.visit(versionVisitor);
        }
        replace(versionVisitor.assertFound(), newVersion);
        return this;
    }

    /**
     * Stores {@link #source} to {@link #path}.
     *
     * @return this {@link BuildGradleEditor}
     * @throws IOException
     */
    public BuildGradleEditor store() throws IOException {
        return store(path);
    }

    /**
     * Stores {@link #source} to the given {@code #path}.
     *
     * @param path
     *            where to store {@link #source}
     * @return this {@link BuildGradleEditor}
     * @throws IOException
     */
    public BuildGradleEditor store(Path path) throws IOException {
        Files.write(path, Collections.singletonList(source), Charset.forName("utf-8"));
        return this;
    }

    /** @return {@link #source} */
    @Override
    public String toString() {
        return source;
    }
}
