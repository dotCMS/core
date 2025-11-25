package com.dotcms.jobs.business.api.events;

import com.dotcms.jobs.business.job.Job;
import java.time.LocalDateTime;

/**
 * Event fired when a job is removed from the queue because failed and is not retryable.
 */
public class JobRemovedFromQueueEvent {

    private final Job job;
    private final LocalDateTime removedAt;

    /**
     * Constructs a new JobRemovedFromQueueEvent.
     *
     * @param job         The non-retryable job.
     * @param canceledAt The timestamp when the job was removed from the queue.
     */
    public JobRemovedFromQueueEvent(Job job, LocalDateTime canceledAt) {
        this.job = job;
        this.removedAt = canceledAt;
    }

    /**
     * @return The non-retryable job.
     */
    public Job getJob() {
        return job;
    }

    /**
     * @return The timestamp when the job removed from the queue.
     */
    public LocalDateTime getRemovedAt() {
        return removedAt;
    }
}
