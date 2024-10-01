package com.dotcms.jobs.business.job;

import com.dotcms.jobs.business.processor.ProgressTracker;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import org.immutables.value.Value;
import org.immutables.value.Value.Default;

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

    Optional<String> executionNode();

    Optional<LocalDateTime> createdAt();

    Optional<LocalDateTime> startedAt();

    Optional<LocalDateTime> updatedAt();

    Optional<LocalDateTime> completedAt();

    Optional<JobResult> result();

    Map<String, Object> parameters();

    Optional<ProgressTracker> progressTracker();

    @Default
    default int retryCount() {
        return 0;
    }

    @Default
    default float progress() {
        return 0.0f;
    }

    /**
     * Creates a new Job with an incremented retry count and updated timestamp.
     *
     * @return A new Job instance with updated retry information.
     */
    default Job incrementRetry() {
        return Job.builder().from(this)
                .retryCount(retryCount() + 1)
                .build();
    }

    /**
     * Creates a new Job marked as failed with the result details.
     *
     * @param result The result details of the failed job.
     * @return A new Job instance marked as failed.
     */
    default Job markAsFailed(final JobResult result) {
        return Job.builder().from(this)
                .state(JobState.FAILED)
                .result(result)
                .completedAt(Optional.of(LocalDateTime.now()))
                .updatedAt(LocalDateTime.now())
                .build();
    }

    /**
     * Creates a new Job marked as running.
     *
     * @return A new Job instance marked as running.
     */
    default Job markAsRunning() {
        return Job.builder().from(this)
                .state(JobState.RUNNING)
                .result(Optional.empty())
                .startedAt(this.startedAt().orElse(LocalDateTime.now()))
                .updatedAt(LocalDateTime.now())
                .build();
    }

    /**
     * Creates a new Job with an updated state.
     *
     * @param newState The new state to set.
     * @return A new Job instance with the updated state.
     */
    default Job withState(final JobState newState) {
        return Job.builder().from(this)
                .state(newState)
                .updatedAt(LocalDateTime.now())
                .build();
    }

    /**
     * Creates a new Job marked as completed.
     *
     * @param result The result details of the completed job.
     *
     * @return A new Job instance marked as completed.
     */
    default Job markAsCompleted(final JobResult result) {

        return Job.builder().from(this)
                .state(JobState.COMPLETED)
                .completedAt(Optional.of(LocalDateTime.now()))
                .updatedAt(LocalDateTime.now())
                .result(result != null ? Optional.of(result) : Optional.empty())
                .build();
    }

    /**
     * Creates a new Job marked as canceled.
     *
     * @param result The result details of the canceled job.
     *
     * @return A new Job instance marked as canceled.
     */
    default Job markAsCanceled(final JobResult result) {

        return Job.builder().from(this)
                .state(JobState.CANCELED)
                .completedAt(Optional.of(LocalDateTime.now()))
                .updatedAt(LocalDateTime.now())
                .result(result != null ? Optional.of(result) : Optional.empty())
                .build();
    }

}
