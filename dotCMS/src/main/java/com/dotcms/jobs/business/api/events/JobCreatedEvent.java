package com.dotcms.jobs.business.api.events;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Event fired when a new job is created and added to the queue.
 */
public class JobCreatedEvent {

    private final String jobId;
    private final String queueName;
    private final LocalDateTime createdAt;
    private final Map<String, Object> parameters;

    /**
     * Constructs a new JobCreatedEvent.
     *
     * @param jobId      The unique identifier of the created job.
     * @param queueName  The name of the queue the job was added to.
     * @param createdAt  The timestamp when the job was created.
     * @param parameters The parameters of the job.
     */
    public JobCreatedEvent(String jobId, String queueName, LocalDateTime createdAt,
            Map<String, Object> parameters) {
        this.jobId = jobId;
        this.queueName = queueName;
        this.createdAt = createdAt;
        this.parameters = parameters;
    }

    /**
     * @return The unique identifier of the created job.
     */
    public String getJobId() {
        return jobId;
    }

    /**
     * @return The name of the queue the job was added to.
     */
    public String getQueueName() {
        return queueName;
    }

    /**
     * @return The timestamp when the job was created.
     */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /**
     * @return The parameters of the job.
     */
    public Map<String, Object> getParameters() {
        return parameters;
    }

}
