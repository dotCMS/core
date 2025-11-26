package com.dotcms.jobs.business.queue.error;

/**
 * Exception thrown when a data-related error occurs during job queue operations. This could include
 * connection issues, query failures, or data integrity problems, regardless of the underlying
 * storage mechanism (e.g., database, in-memory store, distributed cache).
 */
public class JobQueueDataException extends JobQueueException {

    /**
     * Constructs a new JobQueueDataException with the specified detail message.
     *
     * @param message the detail message
     */
    public JobQueueDataException(String message) {
        super(message);
    }

    /**
     * Constructs a new JobQueueDataException with the specified detail message and cause.
     *
     * @param message the detail message
     * @param cause   the cause of this exception
     */
    public JobQueueDataException(String message, Throwable cause) {
        super(message, cause);
    }

}
