package com.dotcms.security.multipart;

import com.dotmarketing.util.SecurityLogger;

/**
 * Validates any traversal directory on the file name
 * @author jsanca
 */
public class IllegalTraversalFilePathValidator implements SecureFileValidator {


    @Override
    public void validate(final String fileName) {

        if (null == fileName || containsPathSeparators(fileName) || containsPathTraversal(fileName)) {

            SecurityLogger.logInfo(this.getClass(), "The filename: '" + fileName + "' is invalid");
            throw new IllegalArgumentException("Illegal Multipart Request");
        }
    }

    /**
     * Checks if the filename contains path separator characters
     */
    private boolean containsPathSeparators(final String fileName) {
        return fileName.contains("/") || fileName.contains("\\");
    }

    /**
     * Checks for actual path traversal patterns, not just any occurrence of ".."
     * Allows filenames like "kek .. lol.png" but blocks "../../file.png"
     */
    private boolean containsPathTraversal(final String fileName) {
        // Check for actual path traversal patterns with path separators
        return fileName.contains("../") || fileName.contains("..\\") ||
               fileName.startsWith("../") || fileName.startsWith("..\\") ||
               fileName.endsWith("/..") || fileName.endsWith("\\..") ||
               fileName.equals("..");
    }
}
