package com.dotcms.jobs.business.api.events;

import com.dotcms.jobs.business.job.Job;
import com.dotcms.jobs.business.job.JobState;
import com.dotmarketing.util.Logger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Predicate;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;

/**
 * Manages real-time monitoring of jobs in the system. This class provides functionality to register
 * watchers for specific jobs and receive notifications about job state changes and progress updates.
 *
 * <p>Thread safety is ensured through a combination of {@link ConcurrentHashMap} for storing watchers
 * and synchronized {@link List}s for managing multiple watchers per job. This allows concurrent
 * registration and notification of watchers without compromising data consistency.</p>
 *
 * <p>The monitor supports filtered watching through predicates, allowing clients to receive only
 * the updates they're interested in. Common predicates are provided through the inner
 * {@link Predicates} class.</p>
 *
 * <h2>Usage Examples:</h2>
 *
 * <p>Watch all job updates:</p>
 * <pre>{@code
 * monitor.registerWatcher(jobId, job -> System.out.println("Job updated: " + job.id()));
 * }</pre>
 *
 * <p>Watch only completed jobs:</p>
 * <pre>{@code
 * monitor.registerWatcher(jobId,
 *     job -> handleCompletion(job),
 *     Predicates.isCompleted()
 * );
 * }</pre>
 *
 * <p>Watch progress changes with threshold:</p>
 * <pre>{@code
 * monitor.registerWatcher(jobId,
 *     job -> updateProgress(job),
 *     Predicates.progressChanged(0.1f) // Updates every 10% progress
 * );
 * }</pre>
 *
 * <p>Combine multiple conditions:</p>
 * <pre>{@code
 * monitor.registerWatcher(jobId,
 *     job -> handleUpdate(job),
 *     Predicates.hasState(JobState.RUNNING)
 *         .and(Predicates.progressChanged(0.05f))
 * );
 * }</pre>
 *
 * @see JobWatcher
 * @see Predicates
 */
@ApplicationScoped
public class RealTimeJobMonitor {

    private final Map<String, List<JobWatcher>> jobWatchers = new ConcurrentHashMap<>();

    /**
     * Registers a watcher for a specific job with optional filtering of updates. The watcher will
     * be notified of job updates that match the provided filter predicate. If no filter is provided
     * (null), the watcher receives all updates for the job.
     *
     * <p>Multiple watchers can be registered for the same job, and each watcher can have
     * its own filter predicate. Watchers are automatically removed when a job reaches a final state
     * (completed, cancelled, or removed).</p>
     *
     * @param jobId   The ID of the job to watch
     * @param watcher The consumer to be notified of job updates
     * @param filter  Optional predicate to filter job updates (null means receive all updates)
     * @throws IllegalArgumentException if jobId or watcher is null
     * @see Predicates for common filter predicates
     */
    public void registerWatcher(String jobId, Consumer<Job> watcher, Predicate<Job> filter) {
        jobWatchers.compute(jobId, (key, existingWatchers) -> {
            List<JobWatcher> watchers = Objects.requireNonNullElseGet(
                    existingWatchers,
                    () -> Collections.synchronizedList(new ArrayList<>())
            );

            final var jobWatcher = JobWatcher.builder()
                    .watcher(watcher)
                    .filter(filter != null ? filter : job -> true).build();

            watchers.add(jobWatcher);
            return watchers;
        });
    }

    /**
     * Registers a watcher for a specific job that receives all updates.
     * This is a convenience method equivalent to calling {@code registerWatcher(jobId, watcher, null)}.
     *
     * @param jobId   The ID of the job to watch
     * @param watcher The consumer to be notified of job updates
     * @throws IllegalArgumentException if jobId or watcher is null
     */
    public void registerWatcher(String jobId, Consumer<Job> watcher) {
        registerWatcher(jobId, watcher, null);
    }

    /**
     * Retrieves the set of job IDs currently being watched.
     * The returned set is a snapshot and may not reflect concurrent modifications.
     *
     * @return An unmodifiable set of job IDs with active watchers
     */
    public Set<String> getWatchedJobIds() {
        return jobWatchers.keySet();
    }

    /**
     * Updates watchers for a list of jobs.
     * Each job's watchers are notified according to their filter predicates.
     *
     * @param updatedJobs List of jobs that have been updated
     * @throws IllegalArgumentException if updatedJobs is null
     */
    public void updateWatchers(List<Job> updatedJobs) {
        for (Job job : updatedJobs) {
            updateWatchers(job);
        }
    }

    /**
     * Updates watchers for a single job. Removes watchers if the job has reached a final state.
     *
     * @param job The job that has been updated.
     */
    private void updateWatchers(Job job) {

        List<JobWatcher> watchers = jobWatchers.get(job.id());
        if (watchers != null) {
            watchers.forEach(jobWatcher -> {
                try {
                    if (jobWatcher.filter().test(job)) {
                        jobWatcher.watcher().accept(job);
                    }
                } catch (Exception e) {
                    Logger.error(this, "Error notifying job watcher for job " + job.id(), e);
                    watchers.remove(jobWatcher);
                }
            });
        }
    }

    /**
     * Removes the watcher associated with the specified job ID.
     *
     * @param jobId The ID of the job whose watcher is to be removed.
     */
    private void removeWatcher(String jobId) {
        jobWatchers.remove(jobId);
    }

    /**
     * Handles the job started event.
     *
     * @param event The JobStartedEvent.
     */
    public void onJobStarted(@Observes JobStartedEvent event) {
        updateWatchers(event.getJob());
    }

    /**
     * Handles the job cancelling event.
     *
     * @param event The JobCancellingEvent.
     */
    public void onJobCancelling(@Observes JobCancellingEvent event) {
        updateWatchers(event.getJob());
    }

    /**
     * Handles the job-canceled event.
     *
     * @param event The JobCanceledEvent.
     */
    public void onJobCanceled(@Observes JobCanceledEvent event) {
        updateWatchers(event.getJob());
        removeWatcher(event.getJob().id());
    }

    /**
     * Handles the job completed event.
     *
     * @param event The JobCompletedEvent.
     */
    public void onJobCompleted(@Observes JobCompletedEvent event) {
        updateWatchers(event.getJob());
        removeWatcher(event.getJob().id());
    }

    /**
     * Handles the job removed from queue event when failed and is not retryable.
     *
     * @param event The JobRemovedFromQueueEvent.
     */
    public void onJobRemovedFromQueueEvent(@Observes JobRemovedFromQueueEvent event) {
        removeWatcher(event.getJob().id());
    }

    /**
     * Handles the job failed event.
     *
     * @param event The JobFailedEvent.
     */
    public void onJobFailed(@Observes JobFailedEvent event) {
        updateWatchers(event.getJob());
    }

    /**
     * Handles the job progress updated event.
     *
     * @param event The JobProgressUpdatedEvent.
     */
    public void onJobProgressUpdated(@Observes JobProgressUpdatedEvent event) {
        updateWatchers(event.getJob());
    }

    /**
     * Common predicates for filtering job updates. These predicates can be used individually or
     * combined using {@link Predicate#and(Predicate)} and {@link Predicate#or(Predicate)} to create
     * more complex filtering conditions.
     */
    public static class Predicates {

        /**
         * Creates a predicate that matches jobs with any of the specified states.
         *
         * @param states One or more job states to match
         * @return A predicate that returns true if the job's state matches any of the specified
         * states
         * @throws IllegalArgumentException if states is null or empty
         */
        public static Predicate<Job> hasState(JobState... states) {
            return job -> Arrays.asList(states).contains(job.state());
        }

        /**
         * Creates a predicate that matches jobs whose progress has changed by at least the
         * specified threshold since the last notification.
         *
         * @param threshold The minimum progress change (0.0 to 1.0) required to match
         * @return A predicate that tracks and matches significant progress changes
         * @throws IllegalArgumentException if threshold is not between 0.0 and 1.0
         */
        public static Predicate<Job> progressChanged(float threshold) {
            return new Predicate<>() {
                private float lastProgress = 0;

                @Override
                public boolean test(Job job) {
                    float currentProgress = job.progress();
                    if (Math.abs(currentProgress - lastProgress) >= threshold) {
                        lastProgress = currentProgress;
                        return true;
                    }
                    return false;
                }
            };
        }

        /**
         * Creates a predicate that matches failed jobs with error details. The predicate only
         * matches if the job is in FAILED state and has error details available.
         *
         * @return A predicate for matching failed jobs
         */
        public static Predicate<Job> hasFailed() {
            return job -> job.state() == JobState.FAILED
                    && job.result().isPresent()
                    && job.result().get().errorDetail().isPresent();
        }

        /**
         * Creates a predicate that matches completed jobs. The predicate matches any job in the
         * COMPLETED state.
         *
         * @return A predicate for matching completed jobs
         */
        public static Predicate<Job> isCompleted() {
            return job -> job.state() == JobState.COMPLETED;
        }

        /**
         * Creates a predicate that matches canceled jobs. The predicate matches any job in the
         * CANCELED state.
         *
         * @return A predicate for matching canceled jobs
         */
        public static Predicate<Job> isCanceled() {
            return job -> job.state() == JobState.CANCELED;
        }

    }

}
