package com.dotcms.jobs.business.error;

/**
 * Exception thrown when an error occurs while executing a job's retry strategy. This could happen
 * if there's an issue incrementing the retry count, updating the job status, etc.
 */
public class RetryStrategyException extends RuntimeException {

    /**
     * Constructs a new RetryStrategyException with the specified job ID and reason.
     *
     * @param jobId  The ID of the job for which the retry strategy failed
     * @param reason A description of why the retry strategy failed
     */
    public RetryStrategyException(String jobId, String reason) {
        super("Retry strategy failed for job " + jobId + ". Reason: " + reason);
    }
}