package com.dotcms.api.client.push.exception;

/**
 * Represents an exception that is thrown when a push operation fails.
 * <p>
 * This class extends the RuntimeException class, making it an unchecked exception.
 * </p>
 */
public class PushException extends RuntimeException {

    public PushException(String message) {
        super(message);
    }

    public PushException(String message, Throwable cause) {
        super(message, cause);
    }
}