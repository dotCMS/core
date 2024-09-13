package com.dotcms.jobs.business.api.events;

import com.dotcms.jobs.business.error.ErrorDetail;
import java.time.LocalDateTime;

/**
 * Event fired when a job fails during processing.
 */
public class JobFailedEvent {

    private final String jobId;
    private final ErrorDetail errorDetail;
    private final LocalDateTime failedAt;

    /**
     * Constructs a new JobFailedEvent.
     *
     * @param jobId       The unique identifier of the failed job.
     * @param errorDetail The details of the error that caused the job to fail.
     * @param failedAt    The timestamp when the job failed.
     */
    public JobFailedEvent(String jobId, ErrorDetail errorDetail, LocalDateTime failedAt) {
        this.jobId = jobId;
        this.errorDetail = errorDetail;
        this.failedAt = failedAt;
    }

    /**
     * @return The unique identifier of the failed job.
     */
    public String getJobId() {
        return jobId;
    }

    /**
     * @return The details of the error that caused the job to fail.
     */
    public ErrorDetail getErrorDetail() {
        return errorDetail;
    }

    /**
     * @return The timestamp when the job failed.
     */
    public LocalDateTime getFailedAt() {
        return failedAt;
    }
}
