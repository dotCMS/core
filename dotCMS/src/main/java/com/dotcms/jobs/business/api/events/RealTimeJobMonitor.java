package com.dotcms.jobs.business.api.events;

import com.dotcms.jobs.business.job.Job;
import com.dotcms.jobs.business.job.JobState;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.ObservesAsync;

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
            if (watchers.isEmpty() || (job.state() == JobState.COMPLETED
                    || job.state() == JobState.FAILED
                    || job.state() == JobState.CANCELLED)) {
                jobWatchers.remove(job.id());
            }
        }
    }

    /**
     * Handles the job started event.
     *
     * @param event The JobStartedEvent.
     */
    public void onJobStarted(@ObservesAsync JobStartedEvent event) {
        updateWatchers(event.getJob());
    }

    /**
     * Handles the job cancelled event.
     *
     * @param event The JobCancelledEvent.
     */
    public void onCanceledJob(@ObservesAsync JobCancelledEvent event) {
        updateWatchers(event.getJob());
    }

    /**
     * Handles the job completed event.
     *
     * @param event The JobCompletedEvent.
     */
    public void onJobCompleted(@ObservesAsync JobCompletedEvent event) {
        updateWatchers(event.getJob());
    }

    /**
     * Handles the job failed event.
     *
     * @param event The JobFailedEvent.
     */
    public void onFailedJob(@ObservesAsync JobFailedEvent event) {
        updateWatchers(event.getJob());
    }

    /**
     * Handles the job progress updated event.
     *
     * @param event The JobProgressUpdatedEvent.
     */
    public void onJobProgressUpdated(@ObservesAsync JobProgressUpdatedEvent event) {
        updateWatchers(event.getJob());
    }

}
