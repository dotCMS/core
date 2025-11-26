package com.dotcms.jobs.business.api.events;

import com.dotcms.jobs.business.job.Job;
import java.time.LocalDateTime;

/**
 * Event fired when a job's progress is updated.
 */
public class JobProgressUpdatedEvent implements JobEvent {

    private final Job job;
    private final LocalDateTime updatedAt;

    /**
     * Constructs a new JobProgressUpdatedEvent.
     *
     * @param job       The job.
     * @param updatedAt The timestamp when the progress was updated.
     */
    public JobProgressUpdatedEvent(Job job, LocalDateTime updatedAt) {
        this.job = job;
        this.updatedAt = updatedAt;
    }

    /**
     * @return The job.
     */
    public Job getJob() {
        return job;
    }

    /**
     * @return The timestamp when the progress was updated.
     */
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
