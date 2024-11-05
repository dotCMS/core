package com.dotcms.jobs.business.api.events;

import com.dotcms.jobs.business.job.Job;
import java.time.LocalDateTime;

/**
 * Event fired when there is a request to cancel a job.
 */
public class JobCancelRequestEvent {

    private final Job job;
    private final LocalDateTime canceledAt;

    /**
     * Constructs a new JobCancelRequestEvent.
     *
     * @param job        The job to cancel.
     * @param canceledAt The timestamp when the cancel request was made.
     */
    public JobCancelRequestEvent(Job job, LocalDateTime canceledAt) {
        this.job = job;
        this.canceledAt = canceledAt;
    }

    /**
     * @return The job to cancel.
     */
    public Job getJob() {
        return job;
    }

    /**
     * @return The timestamp when the cancel request was made.
     */
    public LocalDateTime getCanceledAt() {
        return canceledAt;
    }
}
