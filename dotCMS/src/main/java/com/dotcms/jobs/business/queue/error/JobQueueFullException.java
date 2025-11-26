package com.dotcms.jobs.business.queue.error;

/**
 * Exception thrown when the job queue has reached its capacity and cannot accept new jobs. This may
 * occur if there's a limit on the number of pending jobs or if system resources are exhausted.
 */
public class JobQueueFullException extends JobQueueException {

    /**
     * Constructs a new JobQueueFullException with the specified detail message.
     *
     * @param message the detail message
     */
    public JobQueueFullException(String message) {
        super(message);
    }

}
