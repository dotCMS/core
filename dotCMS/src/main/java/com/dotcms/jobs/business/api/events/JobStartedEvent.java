package com.dotcms.jobs.business.api.events;

import java.time.LocalDateTime;

/**
 * Event fired when a job starts processing.
 */
public class JobStartedEvent {

    private final String jobId;
    private final LocalDateTime startedAt;

    /**
     * Constructs a new JobStartedEvent.
     *
     * @param jobId     The unique identifier of the started job.
     * @param startedAt The timestamp when the job started processing.
     */
    public JobStartedEvent(String jobId, LocalDateTime startedAt) {
        this.jobId = jobId;
        this.startedAt = startedAt;
    }

    /**
     * @return The unique identifier of the started job.
     */
    public String getJobId() {
        return jobId;
    }

    /**
     * @return The timestamp when the job started processing.
     */
    public LocalDateTime getStartedAt() {
        return startedAt;
    }
}
