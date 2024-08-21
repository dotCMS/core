package com.dotcms.jobs.business.job;

import com.dotcms.jobs.business.error.ErrorDetail;
import com.dotcms.jobs.business.error.RetryStrategy;
import com.dotcms.jobs.business.processor.ProgressTracker;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import org.immutables.value.Value;

/**
 * Abstract interface for an immutable Job class. This interface defines the structure for job
 * information in the job processing system. The concrete implementation will be generated as an
 * immutable class named Job.
 */
@Value.Style(typeImmutable = "*", typeAbstract = "Abstract*")
@Value.Immutable
@JsonSerialize(as = Job.class)
@JsonDeserialize(as = Job.class)
public interface AbstractJob {

    String id();

    String queueName();

    JobState state();

    LocalDateTime createdAt();

    LocalDateTime updatedAt();

    Optional<LocalDateTime> completedAt();

    Optional<JobResult> result();

    Map<String, Object> parameters();

    ProgressTracker progressTracker();

    String executionNode();

    Throwable lastException();

    ErrorDetail errorDetail();

    int retryCount();

    long lastRetryTimestamp();

    RetryStrategy retryStrategy();

    @Value.Derived
    default float progress() {
        return progressTracker() != null ? progressTracker().progress() : 0.0f;
    }

    @Value.Derived
    default int maxRetries() {
        return retryStrategy().maxRetries();
    }

    @Value.Derived
    default boolean canRetry() {
        return retryStrategy().shouldRetry(Job.copyOf(this), lastException());
    }

    @Value.Derived
    default long nextRetryDelay() {
        return retryStrategy().nextRetryDelay(Job.copyOf(this));
    }

    /**
     * Creates a new Job with an incremented retry count and updated timestamp.
     *
     * @return A new Job instance with updated retry information.
     */
    default Job incrementRetry() {
        return AbstractJob.builder().from(this)
                .retryCount(retryCount() + 1)
                .lastRetryTimestamp(System.currentTimeMillis())
                .build();
    }

    /**
     * Creates a new Job marked as failed with the given error detail.
     *
     * @param errorDetail The error detail to set.
     * @return A new Job instance marked as failed.
     */
    default Job markAsFailed(ErrorDetail errorDetail) {
        return AbstractJob.builder().from(this)
                .state(JobState.FAILED)
                .result(JobResult.ERROR)
                .errorDetail(errorDetail)
                .lastException(errorDetail.exception())
                .build();
    }

    /**
     * Creates a new Job with an updated state.
     *
     * @param newState The new state to set.
     * @return A new Job instance with the updated state.
     */
    default Job withState(JobState newState) {
        return AbstractJob.builder().from(this)
                .state(newState)
                .updatedAt(LocalDateTime.now())
                .build();
    }

    /**
     * Creates a new Job with an updated result.
     *
     * @param newResult The new result to set.
     * @return A new Job instance with the updated result.
     */
    default Job withResult(JobResult newResult) {
        return AbstractJob.builder().from(this)
                .result(Optional.of(newResult))
                .updatedAt(LocalDateTime.now())
                .build();
    }

    /**
     * Creates a new Job marked as completed.
     *
     * @return A new Job instance marked as completed.
     */
    default Job markAsCompleted() {
        return AbstractJob.builder().from(this)
                .state(JobState.COMPLETED)
                .completedAt(Optional.of(LocalDateTime.now()))
                .updatedAt(LocalDateTime.now())
                .build();
    }

    class Builder extends Job.Builder {

    }

    static Builder builder() {
        return new Builder();
    }

}
