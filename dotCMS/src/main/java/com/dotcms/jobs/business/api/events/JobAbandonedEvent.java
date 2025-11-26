package com.dotcms.jobs.business.api.events;

import com.dotcms.jobs.business.job.Job;
import java.time.LocalDateTime;

/**
 * Event fired when an abandoned job is detected.
 */
public class JobAbandonedEvent implements JobEvent {

    private final Job job;
    private final LocalDateTime detectedAt;

    /**
     * Constructs a new JobAbandonedEvent.
     *
     * @param job        The job.
     * @param detectedAt The timestamp when the abandoned job was detected.
     */
    public JobAbandonedEvent(Job job, LocalDateTime detectedAt) {
        this.job = job;
        this.detectedAt = detectedAt;
    }

    /**
     * @return The abandoned job.
     */
    public Job getJob() {
        return job;
    }

    /**
     * @return The timestamp when the abandoned job was detected.
     */
    public LocalDateTime getDetectedAt() {
        return detectedAt;
    }

}
