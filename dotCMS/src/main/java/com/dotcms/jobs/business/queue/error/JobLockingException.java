package com.dotcms.jobs.business.queue.error;

/**
 * Exception thrown when there's an error in acquiring or managing locks for job processing. This
 * could occur during attempts to atomically fetch and lock the next job for processing.
 */
public class JobLockingException extends JobQueueException {

    /**
     * Constructs a new JobLockingException with the specified detail message.
     *
     * @param message the detail message
     */
    public JobLockingException(String message) {
        super(message);
    }
    
}
