package com.dotcms.jobs.business.api.events;

import java.time.LocalDateTime;

/**
 * Event fired when a job's progress is updated.
 */
public class JobProgressUpdatedEvent {

    private final String jobId;
    private final float progress;
    private final LocalDateTime updatedAt;

    /**
     * Constructs a new JobProgressUpdatedEvent.
     *
     * @param jobId     The unique identifier of the job.
     * @param progress  The current progress of the job (0.0 to 1.0).
     * @param updatedAt The timestamp when the progress was updated.
     */
    public JobProgressUpdatedEvent(String jobId, float progress, LocalDateTime updatedAt) {
        this.jobId = jobId;
        this.progress = progress;
        this.updatedAt = updatedAt;
    }

    /**
     * @return The unique identifier of the job.
     */
    public String getJobId() {
        return jobId;
    }

    /**
     * @return The current progress of the job (0.0 to 1.0).
     */
    public float getProgress() {
        return progress;
    }

    /**
     * @return The timestamp when the progress was updated.
     */
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
