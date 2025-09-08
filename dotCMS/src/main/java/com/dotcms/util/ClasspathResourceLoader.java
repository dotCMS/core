package com.dotcms.util;


import com.liferay.util.StringPool;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

/**
 * Utility to load text/bytes from either an absolute/relative filesystem path
 * or, if not found, from the application classpath.
 *
 * Resolution order:
 * 1) Try filesystem path as-is.
 * 2) Fallback to classpath resource (using the same string).
 * @author jsanca
 */
public final class ClasspathResourceLoader {

    private ClasspathResourceLoader() {}

    /**
     * Reads the whole resource as UTF-8 text.
     *
     * @param pathOrResource filesystem path or classpath resource (e.g. "/sql/rag_schema.sql")
     * @return UTF-8 string content
     * @throws IllegalStateException if not found or cannot be read
     */
    public static String readTextOrThrow(final String pathOrResource) {
        final byte[] bytes = readBytesOrThrow(pathOrResource);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    /**
     * Reads the whole resource as bytes.
     *
     * @param pathOrResource filesystem path or classpath resource
     * @return all bytes
     * @throws IllegalStateException if not found or cannot be read
     */
    public static byte[] readBytesOrThrow(final String pathOrResource) {
        // 1) Try filesystem
        final Path path = Paths.get(pathOrResource);
        if (Files.exists(path) && path.toFile().canRead() && Files.isRegularFile(path)) {
            try {
                return Files.readAllBytes(path);
            } catch (IOException e) {
                throw new IllegalStateException("Failed to read file: " + path, e);
            }
        }
        // 2) Fallback to classpath
        try (InputStream in = resourceStreamOrNull(pathOrResource)) {

            if (in == null) {
                throw new IllegalStateException("Resource not found (FS or classpath): " + pathOrResource);
            }

            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            final byte[] buf = new byte[8192];
            int bytesRead;
            while ((bytesRead = in.read(buf)) != -1) {
                baos.write(buf, 0, bytesRead);
            }

            return baos.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read resource: " + pathOrResource, e);
        }
    }

    /**
     * Opens the resource as an InputStream. Caller must close it.
     * Tries filesystem first, then classpath.
     *
     * @param pathOrResource filesystem path or classpath resource
     * @return InputStream (never null)
     * @throws IllegalStateException if not found
     */
    public static InputStream openStreamOrThrow(final String pathOrResource) {
        // FS
        Path path = Paths.get(pathOrResource);
        if (Files.exists(path) && Files.isRegularFile(path)) {
            try {
                return Files.newInputStream(path, StandardOpenOption.READ);
            } catch (IOException e) {
                throw new IllegalStateException("Failed to open file: " + path, e);
            }
        }
        // CP
        final InputStream in = resourceStreamOrNull(pathOrResource);
        if (in == null) {
            throw new IllegalStateException("Resource not found (FS or classpath): " + pathOrResource);
        }
        return in;
    }

    private static InputStream resourceStreamOrNull(final String pathOrResource) {
        // Try as-is
        final InputStream in = ClasspathResourceLoader.class.getResourceAsStream(pathOrResource);
        if (in != null) return in;
        // Try without leading slash
        if (pathOrResource != null && pathOrResource.startsWith(StringPool.FORWARD_SLASH)) {
            return ClasspathResourceLoader.class.getResourceAsStream(pathOrResource.substring(1));
        }
        return null;
    }
}
