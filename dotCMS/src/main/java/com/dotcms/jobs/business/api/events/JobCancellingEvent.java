package com.dotcms.jobs.business.api.events;

import com.dotcms.jobs.business.job.Job;
import java.time.LocalDateTime;

/**
 * Event fired when a job is being canceled.
 */
public class JobCancellingEvent {

    private final Job job;
    private final LocalDateTime canceledAt;

    /**
     * Constructs a new JobCancellingEvent.
     *
     * @param job         The canceled job.
     * @param canceledAt The timestamp when the job was canceled.
     */
    public JobCancellingEvent(Job job, LocalDateTime canceledAt) {
        this.job = job;
        this.canceledAt = canceledAt;
    }

    /**
     * @return The canceled job.
     */
    public Job getJob() {
        return job;
    }

    /**
     * @return The timestamp when the job was canceled.
     */
    public LocalDateTime getCanceledAt() {
        return canceledAt;
    }
}
