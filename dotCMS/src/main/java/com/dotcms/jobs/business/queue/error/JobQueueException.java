package com.dotcms.jobs.business.queue.error;

/**
 * Base exception class for all job queue related errors. This exception is the parent of all more
 * specific job queue exceptions.
 */
public class JobQueueException extends Exception {

    /**
     * Constructs a new JobQueueException with the specified detail message.
     *
     * @param message the detail message
     */
    public JobQueueException(String message) {
        super(message);
    }

    /**
     * Constructs a new JobQueueException with the specified detail message and cause.
     *
     * @param message the detail message
     * @param cause   the cause of this exception
     */
    public JobQueueException(String message, Throwable cause) {
        super(message, cause);
    }

}
