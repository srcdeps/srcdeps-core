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
package org.srcdeps.core.config.scalar;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * An URI-like combination of {@link Scheme} and some scheme specific {@code resource} identifier.
 * <ul>
 * <li>For {@code classpath} scheme, the {@code resource} is a path that will be resolved via
 * {@code getClass().getResource(resource)}. Example:
 * {@code classpath:/gradle/settings/srcdeps-model-transformer.gradle}</li>
 *
 * <li>For {@code file} scheme, the {@code resource} is a file system path relative to the outer project's root
 * directory. Example: {@code file:src/build/srcdeps/my-script.gradle}</li>
 *
 * <li>For {@code literal} scheme, the {@code resource} is a string literal to use verbatim as the resource content.
 * {@code literal: println "srcdeps rocks!"}
 * <ul>
 *
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */
public class CharStreamSource {

    public enum Scheme {
        classpath {
            @Override
            public Reader openReader(String resource, Charset encoding, Path resolveAgainst) {
                return new InputStreamReader(getClass().getResourceAsStream(resource), encoding);
            }
        }, //
        file {
            @Override
            public Reader openReader(String resource, Charset encoding, Path resolveAgainst) throws IOException {
                return Files.newBufferedReader(resolveAgainst.resolve(Paths.get(resource)), encoding);
            }
        }, //
        literal {
            @Override
            public Reader openReader(String resource, Charset encoding, Path resolveAgainst) {
                return new StringReader(resource);
            }
        };
        public abstract Reader openReader(String resource, Charset encoding, Path resolveAgainst) throws IOException;
    }

    private static final CharStreamSource DEFAULT_MODEL_TRANSFORMER = new CharStreamSource(Scheme.classpath,
            "/gradle/settings/srcdeps-model-transformer.gradle");

    /**
     * @return the {@link CharStreamSource} of the default Gradle model transformer
     */
    public static CharStreamSource defaultModelTransformer() {
        return DEFAULT_MODEL_TRANSFORMER;
    }

    public static CharStreamSource of(String value) {
        for (Scheme scheme : Scheme.values()) {
            final String schemeName = scheme.name();
            if (value.startsWith(schemeName)) {
                int len = schemeName.length();
                if (value.length() >= len && value.charAt(len) == ':') {
                    return new CharStreamSource(scheme, value.substring(len + 1));
                }
            }
        }
        throw new IllegalArgumentException(
                String.format("Cannot parse [%s] to a " + CharStreamSource.class.getSimpleName(), value));
    };

    private final String resource;

    private final Scheme scheme;

    public CharStreamSource(Scheme scheme, String resource) {
        super();
        this.scheme = scheme;
        this.resource = resource;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        CharStreamSource other = (CharStreamSource) obj;
        if (scheme != other.scheme)
            return false;
        if (resource == null) {
            if (other.resource != null)
                return false;
        } else if (!resource.equals(other.resource))
            return false;
        return true;
    }

    /**
     * @return a scheme specific resource identifier, see {@link CharStreamSource}.
     */
    public String getResource() {
        return resource;
    }

    /**
     * @return the {@link Scheme}
     */
    public Scheme getScheme() {
        return scheme;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((resource == null) ? 0 : resource.hashCode());
        result = prime * result + ((scheme == null) ? 0 : scheme.hashCode());
        return result;
    }

    /**
     * Opens a {@link Reader} based on the {@link #scheme} and {@link #resource} using
     * {@link Scheme#openReader(String, Charset, Path)}.
     *
     * @param encoding       the encoding to use when creating the reader. Required only for {@link Scheme#file}.
     * @param resolveAgainst the directory to resolve the {@link #resource} against. Required only for
     *                       {@link Scheme#file}.
     * @return a {@link Reader}
     * @throws IOException when the {@code cannot be opened}.
     */
    public Reader openReader(Charset encoding, Path resolveAgainst) throws IOException {
        return scheme.openReader(resource, encoding, resolveAgainst);
    }

    @Override
    public String toString() {
        return scheme + ":" + resource;
    }

}
