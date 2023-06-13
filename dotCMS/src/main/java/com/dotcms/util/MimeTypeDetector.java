package com.dotcms.util;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Implement this interface to provide a custom mime type detector.
 */
@FunctionalInterface
public interface MimeTypeDetector {

    /**
     * Detects the mime type of a file.
     */
    String detectMimeType(Path path)
            throws IOException;
}
