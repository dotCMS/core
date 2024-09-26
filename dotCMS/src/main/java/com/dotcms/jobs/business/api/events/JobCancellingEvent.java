package com.dotcms.jobs.business.api.events;

import com.dotcms.jobs.business.job.Job;
import java.time.LocalDateTime;

/**
 * Event fired when a job is being cancelled.
 */
public class JobCancellingEvent {

    private final Job job;
    private final LocalDateTime cancelledAt;

    /**
     * Constructs a new JobCancellingEvent.
     *
     * @param job         The cancelled job.
     * @param cancelledAt The timestamp when the job was cancelled.
     */
    public JobCancellingEvent(Job job, LocalDateTime cancelledAt) {
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
