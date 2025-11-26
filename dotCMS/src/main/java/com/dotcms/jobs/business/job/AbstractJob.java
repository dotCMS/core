package com.dotcms.jobs.business.job;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.time.LocalDateTime;
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
public interface AbstractJob extends JobContract {

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
     * Creates a new Job marked as abandoned with the result details.
     *
     * @param result The result details of the abandoned job.
     * @return A new Job instance marked as abandoned.
     */
    default Job markAsAbandoned(final JobResult result) {
        return Job.builder().from(this)
                .state(JobState.ABANDONED)
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
     * Creates a new Job marked as successful.
     *
     * @param result The result details of the successful job.
     *
     * @return A new Job instance marked as successful.
     */
    default Job markAsSuccessful(final JobResult result) {

        return Job.builder().from(this)
                .state(JobState.SUCCESS)
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

    /**
     * Creates a new Job marked as failed permanently.
     *
     * @return A new Job instance marked as failed permanently.
     */
    default Job markAsFailedPermanently() {

        return Job.builder().from(this)
                .state(JobState.FAILED_PERMANENTLY)
                .completedAt(Optional.of(LocalDateTime.now()))
                .updatedAt(LocalDateTime.now())
                .build();
    }

    /**
     * Creates a new Job marked as abandoned permanently.
     *
     * @return A new Job instance marked as abandoned permanently.
     */
    default Job markAsAbandonedPermanently() {

        return Job.builder().from(this)
                .state(JobState.ABANDONED_PERMANENTLY)
                .completedAt(Optional.of(LocalDateTime.now()))
                .updatedAt(LocalDateTime.now())
                .build();
    }

}
