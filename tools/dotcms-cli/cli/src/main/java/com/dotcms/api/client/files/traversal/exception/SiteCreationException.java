package com.dotcms.api.client.files.traversal.exception;

/**
 * Exception thrown when a site creation process encounters an error.
 */
public class SiteCreationException extends RuntimeException {
    public SiteCreationException(String message) {
        super(message);
    }

    public SiteCreationException(String message, Throwable cause) {
        super(message, cause);
    }
}