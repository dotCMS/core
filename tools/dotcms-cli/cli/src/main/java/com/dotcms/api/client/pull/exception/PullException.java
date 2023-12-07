package com.dotcms.api.client.pull.exception;

/**
 * Represents an exception that is thrown when a pull operation fails.
 * <p>
 * This class extends the RuntimeException class, making it an unchecked exception.
 * </p>
 */
public class PullException extends RuntimeException {

    public PullException(String message) {
        super(message);
    }

    public PullException(String message, Throwable cause) {
        super(message, cause);
    }

}