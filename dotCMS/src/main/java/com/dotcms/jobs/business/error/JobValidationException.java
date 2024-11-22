package com.dotcms.jobs.business.error;

/**
 * Exception thrown when a job fails validation before or during processing. This exception provides
 * information about which job failed validation, the reason for the validation failure, and the
 * underlying cause (if available).
 */
public class JobValidationException extends RuntimeException {

    /**
     * Constructs a new JobValidationException with the specified message and cause.
     *
     * @param message A description of the validation failure
     * @param cause   The underlying cause of the validation failure (can be null)
     */
    public JobValidationException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a new JobValidationException with the specified message
     *
     * @param message A description of the validation failure
     */
    public JobValidationException(String message) {
        super(message);
    }

    /**
     * Constructs a new JobValidationException with the specified job ID, reason, and cause.
     *
     * @param jobId  The ID of the job that failed validation
     * @param reason A description of why the validation failed
     * @param cause  The underlying cause of the validation failure (can be null)
     */
    public JobValidationException(String jobId, String reason, Throwable cause) {
        super("Error processing job " + jobId + ". Reason: " + reason, cause);
    }

    /**
     * Constructs a new JobValidationException with the specified job ID and reason.
     *
     * @param jobId  The ID of the job that failed validation
     * @param reason A description of why the validation failed
     */
    public JobValidationException(String jobId, String reason) {
        super("Error processing job " + jobId + ". Reason: " + reason);
    }

}