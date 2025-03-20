package com.dotcms.jobs.business.error;

/**
 * Exception thrown when a job is detected to be abandoned in the job queue system. A job is
 * considered abandoned when it remains in an active state without updates beyond the configured
 * abandonment threshold.
 */
public class JobAbandonedException extends RuntimeException {

    /**
     * Creates a new JobAbandonedException with the specified message.
     *
     * @param message Details about why the job was considered abandoned
     */
    public JobAbandonedException(String message) {
        super(message);
    }

    /**
     * Creates a new JobAbandonedException with a message and underlying cause.
     *
     * @param message Details about why the job was considered abandoned
     * @param cause   The underlying exception that led to the job being abandoned
     */
    public JobAbandonedException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Creates a new JobAbandonedException with an underlying cause.
     *
     * @param cause The underlying exception that led to the job being abandoned
     */
    public JobAbandonedException(Throwable cause) {
        super(cause);
    }
}
