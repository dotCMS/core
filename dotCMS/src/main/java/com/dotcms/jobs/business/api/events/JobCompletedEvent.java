package com.dotcms.jobs.business.api.events;

import java.time.LocalDateTime;

/**
 * Event fired when a job completes successfully.
 */
public class JobCompletedEvent {

    private final String jobId;
    private final LocalDateTime completedAt;

    /**
     * Constructs a new JobCompletedEvent.
     *
     * @param jobId       The unique identifier of the completed job.
     * @param completedAt The timestamp when the job completed.
     */
    public JobCompletedEvent(String jobId, LocalDateTime completedAt) {
        this.jobId = jobId;
        this.completedAt = completedAt;
    }

    /**
     * @return The unique identifier of the completed job.
     */
    public String getJobId() {
        return jobId;
    }

    /**
     * @return The timestamp when the job completed.
     */
    public LocalDateTime getCompletedAt() {
        return completedAt;
    }
}
