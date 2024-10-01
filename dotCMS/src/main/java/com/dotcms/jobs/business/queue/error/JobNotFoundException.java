package com.dotcms.jobs.business.queue.error;

/**
 * Exception thrown when a requested job cannot be found in the job queue. This typically occurs
 * when trying to retrieve, update, or process a job that no longer exists.
 */
public class JobNotFoundException extends JobQueueException {

    /**
     * Constructs a new JobNotFoundException with a message indicating the missing job's ID.
     *
     * @param jobId the ID of the job that was not found
     */
    public JobNotFoundException(String jobId) {
        super("Job with id: " + jobId + " not found");
    }

}

