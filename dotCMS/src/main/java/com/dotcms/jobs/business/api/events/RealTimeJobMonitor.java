package com.dotcms.jobs.business.api.events;

import com.dotcms.jobs.business.job.Job;
import com.dotcms.jobs.business.job.JobState;
import com.dotcms.system.event.local.model.EventSubscriber;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.util.Logger;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.function.Predicate;
import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;

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

    public RealTimeJobMonitor() {
        // Default constructor required for CDI
    }

    @PostConstruct
    protected void init() {

        APILocator.getLocalSystemEventsAPI().subscribe(
                JobProgressUpdatedEvent.class,
                (EventSubscriber<JobProgressUpdatedEvent>) this::onJobProgressUpdated
        );

        APILocator.getLocalSystemEventsAPI().subscribe(
                JobFailedEvent.class,
                (EventSubscriber<JobFailedEvent>) this::onJobFailed
        );

        APILocator.getLocalSystemEventsAPI().subscribe(
                JobCompletedEvent.class,
                (EventSubscriber<JobCompletedEvent>) this::onJobCompleted
        );

        APILocator.getLocalSystemEventsAPI().subscribe(
                JobCancellingEvent.class,
                (EventSubscriber<JobCancellingEvent>) this::onJobCancelling
        );

        APILocator.getLocalSystemEventsAPI().subscribe(
                JobCancelRequestEvent.class,
                (EventSubscriber<JobCancelRequestEvent>) this::onJobCancelRequest
        );

        APILocator.getLocalSystemEventsAPI().subscribe(
                JobStartedEvent.class,
                (EventSubscriber<JobStartedEvent>) this::onJobStarted
        );

        APILocator.getLocalSystemEventsAPI().subscribe(
                JobAbandonedEvent.class,
                (EventSubscriber<JobAbandonedEvent>) this::onAbandonedJob
        );

    }

    /**
     * Registers a watcher for a specific job with optional filtering of updates. The watcher will
     * be notified of job updates that match the provided filter predicate. If no filter is provided
     * (null), the watcher receives all updates for the job.
     *
     * <p>Multiple watchers can be registered for the same job, and each watcher can have
     * its own filter predicate. Watchers are automatically removed when a job reaches a final state
     * (completed, cancelled, or removed).</p>
     *
     * <p>Thread Safety:</p>
     * <ul>
     *   <li>This method is thread-safe and can be called concurrently from multiple threads.</li>
     *   <li>Internally uses {@link CopyOnWriteArrayList} to store watchers, which provides:
     *     <ul>
     *       <li>Thread-safe reads without synchronization - all iteration operations use an immutable snapshot</li>
     *       <li>Thread-safe modifications - each modification creates a new internal copy</li>
     *       <li>Memory consistency effects - actions in a thread prior to placing an object into a
     *           CopyOnWriteArrayList happen-before actions subsequent to the access or removal
     *           of that element from the CopyOnWriteArrayList in another thread</li>
     *     </ul>
     *   </li>
     *   <li>The trade-off is that modifications (adding/removing watchers) are more expensive as they
     *       create a new copy of the internal array, but this is acceptable since:
     *     <ul>
     *       <li>Reads (notifications) are much more frequent than writes (registering/removing watchers)</li>
     *       <li>The number of watchers per job is typically small</li>
     *       <li>Registration/removal of watchers is not in the critical path</li>
     *     </ul>
     *   </li>
     * </ul>
     *
     * @param jobId   The ID of the job to watch
     * @param watcher The consumer to be notified of job updates
     * @param filter  Optional predicate to filter job updates (null means receive all updates)
     * @return A JobWatcher instance representing the registered watcher
     * @throws NullPointerException if jobId or watcher is null
     * @see Predicates for common filter predicates
     * @see CopyOnWriteArrayList for more details about the thread-safety guarantees
     */
    public JobWatcher registerWatcher(String jobId, Consumer<Job> watcher, Predicate<Job> filter) {

        Objects.requireNonNull(jobId, "jobId cannot be null");
        Objects.requireNonNull(watcher, "watcher cannot be null");

        final var jobWatcher = JobWatcher.builder()
                .watcher(watcher)
                .filter(filter != null ? filter : job -> true)
                .build();

        jobWatchers.compute(jobId, (key, existingWatchers) -> {
            List<JobWatcher> watchers = Objects.requireNonNullElseGet(
                    existingWatchers,
                    CopyOnWriteArrayList::new
            );

            watchers.add(jobWatcher);

            Logger.debug(this, String.format(
                    "Added watcher for job %s. Total watchers: %d", jobId, watchers.size()));

            return watchers;
        });

        return jobWatcher;
    }

    /**
     * Registers a watcher for a specific job. The watcher receives all updates for the job.
     *
     * <p>Multiple watchers can be registered for the same job. Watchers are automatically removed
     * when a job reaches a final state (completed, cancelled, or removed).</p>
     *
     * <p>Thread Safety:</p>
     * <ul>
     *   <li>This method is thread-safe and can be called concurrently from multiple threads.</li>
     *   <li>Internally uses {@link CopyOnWriteArrayList} to store watchers, which provides:
     *     <ul>
     *       <li>Thread-safe reads without synchronization - all iteration operations use an immutable snapshot</li>
     *       <li>Thread-safe modifications - each modification creates a new internal copy</li>
     *       <li>Memory consistency effects - actions in a thread prior to placing an object into a
     *           CopyOnWriteArrayList happen-before actions subsequent to the access or removal
     *           of that element from the CopyOnWriteArrayList in another thread</li>
     *     </ul>
     *   </li>
     *   <li>The trade-off is that modifications (adding/removing watchers) are more expensive as they
     *       create a new copy of the internal array, but this is acceptable since:
     *     <ul>
     *       <li>Reads (notifications) are much more frequent than writes (registering/removing watchers)</li>
     *       <li>The number of watchers per job is typically small</li>
     *       <li>Registration/removal of watchers is not in the critical path</li>
     *     </ul>
     *   </li>
     * </ul>
     *
     * @param jobId   The ID of the job to watch
     * @param watcher The consumer to be notified of job updates
     * @return A JobWatcher instance representing the registered watcher
     * @throws NullPointerException if jobId or watcher is null
     * @see CopyOnWriteArrayList for more details about the thread-safety guarantees
     */
    public JobWatcher registerWatcher(String jobId, Consumer<Job> watcher) {
        return registerWatcher(jobId, watcher, null);
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
     * Removes all the watchers associated with the specified job ID.
     *
     * @param jobId The ID of the job whose watchers are to be removed.
     */
    public void removeAllWatchers(final String jobId) {

        List<JobWatcher> removed = jobWatchers.remove(jobId);
        if (removed != null) {
            Logger.info(this,
                    String.format("Removed all watchers for job %s. Watchers removed: %d",
                            jobId, removed.size()));
        }
    }

    /**
     * Removes the watcher associated with the specified job ID.
     *
     * @param jobId The ID of the job whose watcher is to be removed.
     */
    public void removeWatcher(final String jobId, final JobWatcher watcher) {

        if (jobId == null || watcher == null) {
            return;
        }

        // Get the list of watchers for the job
        List<JobWatcher> watchers = jobWatchers.get(jobId);
        if (watchers != null) {
            removeWatcherFromList(jobId, watcher, watchers);
        }
    }

    /**
     * Handles the job started event.
     *
     * @param event The JobStartedEvent.
     */
    public void onJobStarted(JobStartedEvent event) {
        updateWatchers(event.getJob());
    }

    /**
     * Handles the job cancel request event.
     *
     * @param event The JobCancelRequestEvent.
     */
    public void onJobCancelRequest(JobCancelRequestEvent event) {
        updateWatchers(event.getJob());
    }

    /**
     * Handles the abandoned job event.
     *
     * @param event The JobAbandonedEvent.
     */
    public void onAbandonedJob(JobAbandonedEvent event) {
        updateWatchers(event.getJob());
    }

    /**
     * Handles the job cancelling event.
     *
     * @param event The JobCancellingEvent.
     */
    public void onJobCancelling(JobCancellingEvent event) {
        updateWatchers(event.getJob());
    }

    /**
     * Handles the job completed event.
     *
     * @param event The JobCompletedEvent.
     */
    public void onJobCompleted(JobCompletedEvent event) {
        updateWatchers(event.getJob());
        removeAllWatchers(event.getJob().id());
    }

    /**
     * Handles the job failed event.
     *
     * @param event The JobFailedEvent.
     */
    public void onJobFailed(JobFailedEvent event) {
        updateWatchers(event.getJob());
    }

    /**
     * Handles the job progress updated event.
     *
     * @param event The JobProgressUpdatedEvent.
     */
    public void onJobProgressUpdated(JobProgressUpdatedEvent event) {
        updateWatchers(event.getJob());
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

                    // Direct remove is thread-safe with CopyOnWriteArrayList
                    removeWatcherFromList(job.id(), jobWatcher, watchers);
                }
            });
        }
    }

    /**
     * Removes the watcher from the list of watchers for the specified job ID.
     *
     * @param jobId    The ID of the job whose watcher is to be removed.
     * @param watcher  The watcher to remove.
     * @param watchers The list of watchers for the job.
     */
    private void removeWatcherFromList(String jobId, JobWatcher watcher,
            List<JobWatcher> watchers) {

        // Remove the watcher from the list
        watchers.remove(watcher);

        // If this was the last watcher, clean up the map entry
        if (watchers.isEmpty()) {
            jobWatchers.remove(jobId);
        }
    }

    /**
     * Common predicates for filtering job updates. These predicates can be used individually or
     * combined using {@link Predicate#and(Predicate)} and {@link Predicate#or(Predicate)} to create
     * more complex filtering conditions.
     */
    public static class Predicates {

        private Predicates() {
            // Prevent instantiation
        }

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
            return job -> (job.state() == JobState.FAILED
                    || job.state() == JobState.FAILED_PERMANENTLY)
                    && job.result().isPresent()
                    && job.result().get().errorDetail().isPresent();
        }

        /**
         * Creates a predicate that matches any completed job.
         *
         * @return A predicate for matching completed jobs
         */
        public static Predicate<Job> isCompleted() {
            return job -> (job.state() == JobState.SUCCESS
                    || job.state() == JobState.CANCELED
                    || job.state() == JobState.ABANDONED_PERMANENTLY
                    || job.state() == JobState.FAILED_PERMANENTLY);
        }

        /**
         * Creates a predicate that matches successful jobs. The predicate matches any job in the
         * SUCCESS state.
         *
         * @return A predicate for matching successful jobs
         */
        public static Predicate<Job> isSuccessful() {
            return job -> job.state() == JobState.SUCCESS;
        }

        /**
         * Creates a predicate that matches completed jobs. The predicate matches any job in the
         * ABANDONED state.
         *
         * @return A predicate for matching completed jobs
         */
        public static Predicate<Job> isAbandoned() {
            return job -> (job.state() == JobState.ABANDONED
                    || job.state() == JobState.ABANDONED_PERMANENTLY);
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
