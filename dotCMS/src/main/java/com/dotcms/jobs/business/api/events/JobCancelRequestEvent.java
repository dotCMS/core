package com.dotcms.jobs.business.api.events;

import com.dotcms.jobs.business.job.Job;
import java.time.LocalDateTime;

/**
 * Event fired when there is a request to cancel a job.
 */
public class JobCancelRequestEvent implements JobEvent {

    private final Job job;
    private final LocalDateTime canceledOn;

    /**
     * Constructs a new JobCancelRequestEvent.
     *
     * @param job        The job to cancel.
     * @param canceledOn The timestamp when the cancel request was made.
     */
    public JobCancelRequestEvent(Job job, LocalDateTime canceledOn) {
        this.job = job;
        this.canceledOn = canceledOn;
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
    public LocalDateTime getCanceledOn() {
        return canceledOn;
    }
}
