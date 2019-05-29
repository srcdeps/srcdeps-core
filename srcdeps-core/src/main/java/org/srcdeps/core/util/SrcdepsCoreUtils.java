/**
 * Copyright 2015-2019 Maven Source Dependencies
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
package org.srcdeps.core.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.AccessDeniedException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystemException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;

/**
 * The utilities.
 *
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */
public class SrcdepsCoreUtils {

    /** The number of attempts to try when creating a new directory */
    private static final int CREATE_RETRY_COUNT = 256;
    private static final long DELETE_RETRY_MILLIS = 5000L;

    private static final boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");

    public static void assertArgNotEmptyString(String value, String argName) {
        if (value.isEmpty()) {
            throw new IllegalArgumentException(String.format("Argument [%s] cannot be an empty String", argName));
        }
    }

    public static void assertArgNotNull(Object value, String argName) {
        if (value == null) {
            throw new IllegalArgumentException(String.format("Argument [%s] cannot be null", argName));
        }
    }

    public static void assertCollectionNotEmpty(Collection<?> value, String argName) {
        if (value.isEmpty()) {
            throw new IllegalArgumentException(String.format("Argument [%s] cannot be an empty collection", argName));
        }
    }

    /**
     * @param bytes the bytes to format
     * @return the given {@code bytes} formatted as a hex string
     */
    public static String bytesToHexString(byte[] bytes) {
        return bytesToHexString(bytes, 0, bytes.length);
    }

    /**
     * @param bytes the bytes to format
     * @param offset the offset to start at
     * @param length how many bytes from offset to format
     * @return the given {@code bytes} formatted as a hex string
     */
    public static String bytesToHexString(byte[] bytes, int offset, int length) {
        StringBuilder sb = new StringBuilder(length * 2);
        for (int i = offset; i < offset + length; i++) {
            byte b = bytes[i];
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    /**
     * Copy the given {@code src} directory to the given {@code destination} directory.
     *
     * @param src the directory to copy
     * @param destination where to copy
     * @throws IOException
     */
    public static void copyDirectory(final Path src, final Path destination) throws IOException {
        Files.walkFileTree(src, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(final Path dir, final BasicFileAttributes attrs)
                    throws IOException {
                Files.createDirectories(destination.resolve(src.relativize(dir)));
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
                Files.copy(file, destination.resolve(src.relativize(file)));
                return FileVisitResult.CONTINUE;
            }
        });
    }

    /**
     * Deletes a file or directory recursively if it exists.
     *
     * @param directory the directory to delete
     * @throws IOException
     */
    public static void deleteDirectory(Path directory) throws IOException {
        if (Files.exists(directory)) {
            Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    if (exc == null) {
                        Files.delete(dir);
                        return FileVisitResult.CONTINUE;
                    } else {
                        // directory iteration failed; propagate exception
                        throw exc;
                    }
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    if (isWindows) {
                        final long deadline = System.currentTimeMillis() + DELETE_RETRY_MILLIS;
                        FileSystemException lastException = null;
                        do {
                            try {
                                Files.delete(file);
                                return FileVisitResult.CONTINUE;
                            } catch (FileSystemException e) {
                                lastException = e;
                            }
                        } while (System.currentTimeMillis() < deadline);
                        throw new IOException(String.format("Could not delete file [%s] after retrying for %d ms", file,
                                DELETE_RETRY_MILLIS), lastException);
                    } else {
                        Files.delete(file);
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                    // try to delete the file anyway, even if its attributes
                    // could not be read, since delete-only access is
                    // theoretically possible
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }
            });
        }
    }

    /**
     * Makes sure that the given directory exists. Tries creating {@link #CREATE_RETRY_COUNT} times.
     *
     * @param dir the directory {@link Path} to check
     * @throws IOException if the directory could not be created or accessed
     */
    public static void ensureDirectoryExists(Path dir) throws IOException {
        Throwable toThrow = null;
        for (int i = 0; i < CREATE_RETRY_COUNT; i++) {
            try {
                Files.createDirectories(dir);
                if (Files.exists(dir)) {
                    return;
                }
            } catch (AccessDeniedException e) {
                toThrow = e;
                /* Workaround for https://bugs.openjdk.java.net/browse/JDK-8029608 */
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e1) {
                    Thread.currentThread().interrupt();
                    toThrow = e1;
                }
            } catch (IOException e) {
                toThrow = e;
            }
        }
        if (toThrow != null) {
            throw new IOException(String.format("Could not create directory [%s]", dir), toThrow);
        } else {
            throw new IOException(
                    String.format("Could not create directory [%s] attempting [%d] times", dir, CREATE_RETRY_COUNT));
        }

    }

    /**
     * If the given directory does not exist, creates it using {@link #ensureDirectoryExists(Path)}. Otherwise
     * recursively deletes all subpaths in the given directory.
     *
     * @param dir the directory to check
     * @throws IOException if the directory could not be created, accessed or its children deleted
     */
    public static void ensureDirectoryExistsAndEmpty(Path dir) throws IOException {
        if (Files.exists(dir)) {
            try (DirectoryStream<Path> subPaths = Files.newDirectoryStream(dir)) {
                for (Path subPath : subPaths) {
                    if (Files.isDirectory(subPath)) {
                        deleteDirectory(subPath);
                    } else {
                        Files.delete(subPath);
                    }
                }
            }
        } else {
            ensureDirectoryExists(dir);
        }
    }

    /**
     * @return the file system path to the Java binary that runs the current Java process
     */
    public static String getCurrentJavaExecutable() {
        if (isWindows) {
            return System.getProperties().getProperty("java.home") + File.separator + "bin" + File.separator
                    + "java.exe";
        } else {
            return System.getProperties().getProperty("java.home") + File.separator + "bin" + File.separator + "java";
        }
    }

    /**
     * @return {@code true} if the operating system the present JVM runs on is Windows; {@code false} otherwise
     */
    public static boolean isWindows() {
        return isWindows;
    }

    /**
     * For manual testing of {@link #sha1HexString(Path)} of files larger than the limit for reading the file all at
     * once.
     */
    public static void main(String[] args) throws NoSuchAlgorithmException, IOException {
        System.out.println(sha1HexString(Paths.get(args[0])));
    }

    /**
     * Returns the content of the given {@code reader} as string.
     *
     * @param reader the {@link Reader} to read from
     * @param buf a properly dimensioned buffer to use when reading
     * @return the content read from the given {@code reader}
     * @throws IOException
     */
    public static String read(Reader reader, char[] buf) throws IOException {
        StringBuilder result = new StringBuilder();
        int n;
        while ((n = reader.read(buf)) >= 0) {
            result.append(buf, 0, n);
        }
        return result.toString();
    }

    /**
     * Opens am {@link InputStream} out of the given {@code url} and returns the content as a UTF-8 string.
     *
     * @param url the {@link URL} to read from
     * @param buf a properly dimensioned buffer to use when reading
     * @return the content read from the given {@code url}
     * @throws IOException
     */
    public static String read(URL url, char[] buf) throws IOException {
        try (Reader in = new InputStreamReader(url.openStream(), StandardCharsets.UTF_8)) {
            return read(in, buf);
        }
    }

    /**
     * @param artifactPath the {@link Path} of the file whose sha1 should be computed
     * @return the sha1 of the file formatted as a hex string
     * @throws IOException
     * @throws NoSuchAlgorithmException
     */
    public static String sha1HexString(Path artifactPath) throws IOException, NoSuchAlgorithmException {
        if (Files.exists(artifactPath)) {
            final MessageDigest sha1Digest = MessageDigest.getInstance("SHA-1");
            final int bufferSize = 4096;
            final byte[] localMvnRepoArtifactSha1Bytes;
            if (Files.size(artifactPath) <= bufferSize) {
                localMvnRepoArtifactSha1Bytes = sha1Digest.digest(Files.readAllBytes(artifactPath));
            } else {
                try (InputStream in = Files.newInputStream(artifactPath);
                        DigestOutputStream out = new DigestOutputStream(sha1Digest)) {
                    byte[] buffer = new byte[bufferSize];
                    int len;
                    while ((len = in.read(buffer)) >= 0) {
                        out.write(buffer, 0, len);
                    }
                }
                localMvnRepoArtifactSha1Bytes = sha1Digest.digest();
            }
            return SrcdepsCoreUtils.bytesToHexString(localMvnRepoArtifactSha1Bytes);
        } else {
            return null;
        }
    }

    /**
     * @param anyPath a path with either slashes or backslashes
     * @return a file path with slashes
     */
    public static String toUnixPath(String anyPath) {
        if (anyPath == null || anyPath.isEmpty()) {
            return anyPath;
        }
        return anyPath.replace('\\', '/');
    }

    private SrcdepsCoreUtils() {
    }

}
