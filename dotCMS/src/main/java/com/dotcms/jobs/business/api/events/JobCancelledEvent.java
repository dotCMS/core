package com.dotcms.jobs.business.api.events;

import java.time.LocalDateTime;

/**
 * Event fired when a job is cancelled.
 */
public class JobCancelledEvent {

    private final String jobId;
    private final LocalDateTime cancelledAt;

    /**
     * Constructs a new JobCancelledEvent.
     *
     * @param jobId       The unique identifier of the cancelled job.
     * @param cancelledAt The timestamp when the job was cancelled.
     */
    public JobCancelledEvent(String jobId, LocalDateTime cancelledAt) {
        this.jobId = jobId;
        this.cancelledAt = cancelledAt;
    }

    /**
     * @return The unique identifier of the cancelled job.
     */
    public String getJobId() {
        return jobId;
    }

    /**
     * @return The timestamp when the job was cancelled.
     */
    public LocalDateTime getCancelledAt() {
        return cancelledAt;
    }
}
