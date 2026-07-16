package com.dotcms.shutdown;

/**
 * Exception thrown when components attempt to access resources during system shutdown.
 * This is a special exception that indicates expected shutdown behavior, not an actual error.
 */
public class ShutdownException extends RuntimeException {

    public ShutdownException(String message) {
        super(message);
    }

    public ShutdownException(String message, Throwable cause) {
        super(message, cause);
    }
}