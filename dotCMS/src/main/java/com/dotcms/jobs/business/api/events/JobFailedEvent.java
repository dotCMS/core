package com.dotcms.jobs.business.api.events;

import com.dotcms.jobs.business.job.Job;
import java.time.LocalDateTime;

/**
 * Event fired when a job fails during processing.
 */
public class JobFailedEvent {

    private final Job job;
    private final LocalDateTime failedAt;

    /**
     * Constructs a new JobFailedEvent.
     *
     * @param job      The failed job.
     * @param failedAt The timestamp when the job failed.
     */
    public JobFailedEvent(Job job, LocalDateTime failedAt) {
        this.job = job;
        this.failedAt = failedAt;
    }

    /**
     * @return The failed job.
     */
    public Job getJob() {
        return job;
    }

    /**
     * @return The timestamp when the job failed.
     */
    public LocalDateTime getFailedAt() {
        return failedAt;
    }
}
