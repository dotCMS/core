package com.dotcms.jobs.business.api.events;

import com.dotcms.jobs.business.job.Job;
import java.time.LocalDateTime;

/**
 * Event fired when a job starts processing.
 */
public class JobStartedEvent {

    private final Job job;
    private final LocalDateTime startedAt;

    /**
     * Constructs a new JobStartedEvent.
     *
     * @param job       The started job.
     * @param startedAt The timestamp when the job started processing.
     */
    public JobStartedEvent(Job job, LocalDateTime startedAt) {
        this.job = job;
        this.startedAt = startedAt;
    }

    /**
     * @return The started job.
     */
    public Job getJob() {
        return job;
    }

    /**
     * @return The timestamp when the job started processing.
     */
    public LocalDateTime getStartedAt() {
        return startedAt;
    }
}
