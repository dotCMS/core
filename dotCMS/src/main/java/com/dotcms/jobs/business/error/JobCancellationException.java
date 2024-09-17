package com.dotcms.jobs.business.error;

/**
 * Exception thrown when an attempt to cancel a job fails. This exception provides information about
 * which job failed to cancel and why.
 */
public class JobCancellationException extends RuntimeException {

    /**
     * Constructs a new JobCancellationException with the specified job ID and reason.
     *
     * @param jobId  The ID of the job that failed to cancel
     * @param reason The reason why the cancellation failed
     */
    public JobCancellationException(String jobId, String reason) {
        super("Failed to cancel job " + jobId + ". Reason: " + reason);
    }
}