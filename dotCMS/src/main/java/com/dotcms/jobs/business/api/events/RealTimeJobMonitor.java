package com.dotcms.jobs.business.api.events;

import com.dotcms.jobs.business.job.Job;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;

/**
 * Manages real-time monitoring of jobs in the system. This class handles registration of job
 * watchers, updates watchers on job changes, and processes various job-related events.
 */
@ApplicationScoped
public class RealTimeJobMonitor {

    private final Map<String, List<Consumer<Job>>> jobWatchers = new ConcurrentHashMap<>();

    /**
     * Registers a watcher for a specific job.
     *
     * @param jobId   The ID of the job to watch.
     * @param watcher The consumer to be notified of job updates.
     */
    public void registerWatcher(String jobId, Consumer<Job> watcher) {
        jobWatchers.computeIfAbsent(jobId, k -> new CopyOnWriteArrayList<>()).add(watcher);
    }

    /**
     * Retrieves the set of job IDs currently being watched.
     *
     * @return A set of job IDs.
     */
    public Set<String> getWatchedJobIds() {
        return jobWatchers.keySet();
    }

    /**
     * Updates watchers for a list of jobs.
     *
     * @param updatedJobs List of jobs that have been updated.
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

        List<Consumer<Job>> watchers = jobWatchers.get(job.id());
        if (watchers != null) {
            watchers.forEach(watcher -> watcher.accept(job));
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

}
