package com.dotcms.jobs.business.api.events;

import com.dotcms.jobs.business.job.Job;
import java.time.LocalDateTime;

/**
 * Event fired when a job is cancelled.
 */
public class JobCancelledEvent {

    private final Job job;
    private final LocalDateTime cancelledAt;

    /**
     * Constructs a new JobCancelledEvent.
     *
     * @param job         The cancelled job.
     * @param cancelledAt The timestamp when the job was cancelled.
     */
    public JobCancelledEvent(Job job, LocalDateTime cancelledAt) {
        this.job = job;
        this.cancelledAt = cancelledAt;
    }

    /**
     * @return The cancelled job.
     */
    public Job getJob() {
        return job;
    }

    /**
     * @return The timestamp when the job was cancelled.
     */
    public LocalDateTime getCancelledAt() {
        return cancelledAt;
    }
}
