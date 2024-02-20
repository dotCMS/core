package com.dotcms.util;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Implement this interface to provide a custom MIME Type detector.
 *
 * @author Jonathan Sanchez
 */
@FunctionalInterface
public interface MimeTypeDetector {

    /**
     * Detects the MIME Type of the provided file.
     *
     * @param path The {@link Path} to the file whose MIME Type must be retrieved.
     *
     * @return The detected MIME Type.
     *
     * @throws IOException An error occurred when reading the file's metadata.
     */
    String detectMimeType(final Path path) throws IOException;

}
