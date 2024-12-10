package com.dotcms.jobs.business.error;

/**
 * Exception thrown when an error occurs during job processing. This exception provides information
 * about which job encountered an error, the reason for the error, and the underlying cause (if
 * available).
 */
public class JobProcessingException extends RuntimeException {

    /**
     * Constructs a new JobProcessingException with the specified message and cause.
     *
     * @param message A description of the error
     * @param cause  The underlying cause of the error (can be null)
     */
    public JobProcessingException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a new JobProcessingException with the specified job ID, reason, and cause.
     *
     * @param jobId  The ID of the job that encountered an error during processing
     * @param reason A description of why the error occurred
     * @param cause  The underlying cause of the error (can be null)
     */
    public JobProcessingException(String jobId, String reason, Throwable cause) {
        super("Error processing job " + jobId + ". Reason: " + reason, cause);
    }

    /**
     * Constructs a new JobProcessingException with the specified job ID, reason, and cause.
     *
     * @param jobId  The ID of the job that encountered an error during processing
     * @param reason A description of why the error occurred
     */
    public JobProcessingException(String jobId, String reason) {
        super("Error processing job " + jobId + ". Reason: " + reason);
    }

}