package com.dotcms.jobs.business.api.events;

import com.dotcms.jobs.business.job.Job;
import java.time.LocalDateTime;

/**
 * Event fired when a job completes successfully.
 */
public class JobCompletedEvent implements JobEvent {

    private final Job job;
    private final LocalDateTime completedAt;

    /**
     * Constructs a new JobCompletedEvent.
     *
     * @param job       The completed job.
     * @param completedAt The timestamp when the job completed.
     */
    public JobCompletedEvent(Job job, LocalDateTime completedAt) {
        this.job = job;
        this.completedAt = completedAt;
    }

    /**
     * @return The completed job.
     */
    public Job getJob() {
        return job;
    }

    /**
     * @return The timestamp when the job completed.
     */
    public LocalDateTime getCompletedAt() {
        return completedAt;
    }
}
