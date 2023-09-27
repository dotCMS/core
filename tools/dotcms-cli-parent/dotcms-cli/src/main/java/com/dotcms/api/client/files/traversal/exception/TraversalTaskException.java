package com.dotcms.api.client.files.traversal.exception;

/**
 * Exception thrown during traversal task execution.
 * <p>
 * This exception extends RuntimeException and is thrown to
 * indicate an error occurred during the execution of a
 * traversal task.
 */
public class TraversalTaskException extends RuntimeException {

    public TraversalTaskException(String message) {
        super(message);
    }

    public TraversalTaskException(String message, Throwable cause) {
        super(message, cause);
    }
}