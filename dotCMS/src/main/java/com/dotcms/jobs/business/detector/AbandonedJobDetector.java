package com.dotcms.jobs.business.detector;

import com.dotcms.jobs.business.api.events.JobAbandonedEvent;
import com.dotcms.jobs.business.job.Job;
import com.dotcms.jobs.business.job.JobState;
import com.dotcms.jobs.business.queue.JobQueue;
import com.dotcms.jobs.business.queue.error.JobQueueDataException;
import com.dotcms.jobs.business.util.JobUtil;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Logger;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

/**
 * Detects and handles abandoned jobs in the job queue system. A job is considered abandoned if it
 * remains in certain states (RUNNING, CANCELLING, CANCEL_REQUESTED) without updates for longer than
 * a configured threshold.
 * <p>
 * Key features:
 * <ul>
 *   <li>Periodically scans for jobs that haven't been updated within the abandonment threshold</li>
 *   <li>Marks abandoned jobs and puts them back in queue for retry</li>
 *   <li>Publishes events when abandoned jobs are detected</li>
 *   <li>Configurable detection interval and abandonment threshold</li>
 * </ul>
 */
@ApplicationScoped
public class AbandonedJobDetector implements AutoCloseable {

    private static final JobState[] ABANDONMENT_CHECK_STATES = {
            JobState.RUNNING,
            JobState.CANCELLING,
            JobState.CANCEL_REQUESTED
    };

    private final JobQueue jobQueue;

    private final ScheduledExecutorService executor;

    private final long detectionIntervalMinutes;
    private final long abandonmentThresholdMinutes;

    private volatile boolean isRunning = false;

    // Required for CDI proxy
    protected AbandonedJobDetector() {
        this.jobQueue = null;
        this.executor = null;
        this.detectionIntervalMinutes = 0;
        this.abandonmentThresholdMinutes = 0;
    }

    /**
     * Creates a new abandoned job detector with the specified configuration.
     *
     * @param config   Configuration containing detection interval and threshold
     * @param jobQueue The job queue to monitor for abandoned jobs
     */
    @Inject
    public AbandonedJobDetector(AbandonedJobDetectorConfig config, JobQueue jobQueue) {

        this.jobQueue = jobQueue;
        this.executor = Executors.newSingleThreadScheduledExecutor();

        // Load configuration
        this.detectionIntervalMinutes = config.getDetectionIntervalMinutes();
        this.abandonmentThresholdMinutes = config.getAbandonmentThresholdMinutes();
    }

    /**
     * Starts the abandoned job detection process if not already running. Schedules periodic checks
     * based on the configured detection interval.
     */
    public void start() {

        if (!isRunning) {
            isRunning = true;
            executor.scheduleWithFixedDelay(
                    this::detectAbandonedJobs,
                    detectionIntervalMinutes,
                    detectionIntervalMinutes,
                    TimeUnit.MINUTES
            );
            Logger.info(this, String.format(
                    "Abandoned job detector started. Checking every %d minutes for jobs not "
                            + "updated in %d minutes",
                    detectionIntervalMinutes, abandonmentThresholdMinutes));
        }
    }

    /**
     * Checks for and handles abandoned jobs.
     * <p>
     * Abandoned jobs are:
     * <ol>
     *   <li>Marked as abandoned in the job queue</li>
     *   <li>Logged as a warning</li>
     *   <li>Put back in queue for retry</li>
     *   <li>Generate a JobAbandonedEvent</li>
     * </ol>
     */
    private void detectAbandonedJobs() {

        try {

            Optional<Job> abandonedJob;
            while ((abandonedJob = jobQueue.detectAndMarkAbandoned(
                    Duration.ofMinutes(abandonmentThresholdMinutes),
                    ABANDONMENT_CHECK_STATES)).isPresent()) {

                processAbandonedJob(abandonedJob.get());
            }
        } catch (Exception e) {
            final var errorMessage = "Error detecting abandoned jobs";
            Logger.error(this, errorMessage, e);
            throw new DotRuntimeException(errorMessage, e);
        }
    }

    /**
     * Processes an abandoned job by logging the event, putting it back in the queue for retry, and
     * generating a JobAbandonedEvent.
     */
    private void processAbandonedJob(final Job abandonedJob) throws JobQueueDataException {

        // Log the event
        Logger.warn(this, String.format(
                "Abandoned job found - Job ID: %s, Queue: %s, Last Updated: %s",
                abandonedJob.id(),
                abandonedJob.queueName(),
                abandonedJob.updatedAt().orElse(null)
        ));

        // Put the job back in the queue for later retry
        putJobBackInQueue(abandonedJob);

        // Send events
        JobUtil.sendEvents(abandonedJob, JobAbandonedEvent::new);
    }

    /**
     * Places an abandoned job back in the queue for retry.
     *
     * @param abandonedJob The job that was detected as abandoned
     * @throws JobQueueDataException if there is an error re-queueing the job
     */
    private void putJobBackInQueue(final Job abandonedJob) throws JobQueueDataException {

        // Put the job back in the queue for later retry
        jobQueue.putJobBackInQueue(abandonedJob);

        Logger.info(this,
                String.format("Abandoned job %s queued for retry", abandonedJob.id()));
    }

    /**
     * Stops the abandoned job detector and cleans up resources. Attempts graceful shutdown, forcing
     * shutdown if tasks don't complete within 60 seconds.
     */
    @Override
    public void close() {
        if (isRunning) {
            isRunning = false;
            executor.shutdown();
            try {
                if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
            Logger.info(this, "Abandoned job detector stopped");
        }
    }

    /**
     * @return true if the detector is currently running, false otherwise
     */
    public boolean isRunning() {
        return isRunning;
    }

}