package com.dotcms.jobs.business.api;

import com.dotcms.jobs.business.error.CircuitBreaker;
import com.dotcms.jobs.business.error.ErrorDetail;
import com.dotcms.jobs.business.error.ExponentialBackoffRetryStrategy;
import com.dotcms.jobs.business.error.JobCancellationException;
import com.dotcms.jobs.business.error.ProcessorNotFoundException;
import com.dotcms.jobs.business.error.RetryStrategy;
import com.dotcms.jobs.business.job.Job;
import com.dotcms.jobs.business.job.JobState;
import com.dotcms.jobs.business.processor.JobProcessor;
import com.dotcms.jobs.business.processor.ProgressTracker;
import com.dotcms.jobs.business.queue.JobQueue;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import graphql.VisibleForTesting;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * Manages the processing of jobs in a distributed job queue system. This class is responsible for
 * job creation, execution, monitoring, and error handling.
 * <pre>{@code
 *     public static void main(String[] args) {
 *
 *         // Create the job queue
 *         JobQueue jobQueue = new PostgresJobQueue();
 *
 *         // Create and start the job queue manager
 *         JobQueueAPIImpl jobQueueAPI = new JobQueueAPIImpl(jobQueue, 5); // 5 threads
 *
 *         //(Optional) Set up a retry strategy for content import jobs
 *         RetryStrategy contentImportRetryStrategy = new ExponentialBackoffRetryStrategy(5000, 300000, 2.0, 3);
 *         contentImportRetryStrategy.addRetryableException(IOException.class);
 *         jobQueueAPI.setRetryStrategy("contentImport", contentImportRetryStrategy);
 *
 *         // Register job processors
 *         jobQueueAPI.registerProcessor("contentImport", new ContentImportJobProcessor());
 *
 *         // Start the job queue manager
 *         jobQueueAPI.start();
 *
 *         // Create a content import job (dummy example)
 *         Map<String, Object> jobParameters = new HashMap<>();
 *         jobParameters.put("filePath", "/path/to/import/file.csv");
 *         jobParameters.put("contentType", "Article");
 *         String jobId = jobQueueAPI.createJob("contentImport", jobParameters);
 *
 *         // Optionally, watch the job progress
 *         jobQueueAPI.watchJob(jobId, job -> {
 *             System.out.println("Job " + job.id() + " progress: " + job.progress() * 100 + "%");
 *         });
 *
 *         // When shutting down the application
 *         jobQueueAPI.close();
 *     }
 * }</pre>
 */
public class JobQueueAPIImpl implements JobQueueAPI {

    private final AtomicBoolean isStarted = new AtomicBoolean(false);

    private final CircuitBreaker circuitBreaker;
    private final JobQueue jobQueue;
    private final Map<String, JobProcessor> processors;
    private final int threadPoolSize;
    private ExecutorService executorService;
    private final Map<String, List<Consumer<Job>>> jobWatchers;
    private final Map<String, RetryStrategy> retryStrategies;
    private final RetryStrategy defaultRetryStrategy;

    static final int defaultThreadPoolSize = Config.getIntProperty(
            "JOB_QUEUE_THREAD_POOL_SIZE", 10
    );

    /**
     * Constructs a new JobQueueManager with the default job queue implementation and the default
     * number of threads.
     */
    public JobQueueAPIImpl() {
        // TODO: Use a job queue implementation
        this(null, defaultThreadPoolSize);
    }

    /**
     * Constructs a new JobQueueManager.
     *
     * @param jobQueue       The JobQueue implementation to use.
     * @param threadPoolSize The number of threads to use for job processing.
     */
    @VisibleForTesting
    public JobQueueAPIImpl(JobQueue jobQueue, int threadPoolSize) {
        this.jobQueue = jobQueue;
        this.threadPoolSize = threadPoolSize;
        this.processors = new ConcurrentHashMap<>();
        this.jobWatchers = new ConcurrentHashMap<>();
        this.retryStrategies = new ConcurrentHashMap<>();
        this.defaultRetryStrategy = new ExponentialBackoffRetryStrategy(
                1000, 60000, 2.0, 5
        );
        this.circuitBreaker = new CircuitBreaker(
                5, 60000
        ); // 5 failures within 1 minute
    }

    @Override
    public void start() {

        if (isStarted.compareAndSet(false, true)) {

            Logger.info(
                    this, "Starting JobQueueManager with " + threadPoolSize + " threads."
            );

            executorService = Executors.newFixedThreadPool(threadPoolSize);
            for (int i = 0; i < threadPoolSize; i++) {
                executorService.submit(this::processJobs);
            }

            Logger.info(this, "JobQueueManager has been successfully started.");
        } else {
            Logger.warn(this,
                    "Attempt to start JobQueueAPIImpl that is already running. Ignoring."
            );
        }
    }

    @Override
    public void close() throws Exception {

        if (isStarted.compareAndSet(true, false)) {

            Logger.info(this, "Closing JobQueueManager and stopping all job processing.");
            executorService.shutdown();

            try {
                if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                    if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                        Logger.error(this, "ExecutorService did not terminate");
                    }
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
                Logger.error(this, "Interrupted while waiting for jobs to complete", e);
            }

            Logger.info(this, "JobQueueManager has been successfully closed.");
        } else {
            Logger.warn(this,
                    "Attempt to close JobQueueAPIImpl that is not running. Ignoring."
            );
        }
    }

    @Override
    public void registerProcessor(final String queueName, final JobProcessor processor) {
        processors.put(queueName, processor);
    }

    @Override
    public String createJob(final String queueName, final Map<String, Object> parameters) {

        if (!processors.containsKey(queueName)) {
            final var error = new ProcessorNotFoundException(queueName);
            Logger.error(JobQueueAPIImpl.class, error);
            throw error;
        }

        return jobQueue.addJob(queueName, parameters);
    }

    @Override
    public Job getJob(final String jobId) {
        return jobQueue.job(jobId);
    }

    @Override
    public List<Job> getJobs(final int page, final int pageSize) {
        return jobQueue.jobs(page, pageSize);
    }

    @Override
    public void cancelJob(final String jobId) {

        Job job = jobQueue.job(jobId);
        if (job != null) {

            final var processor = processors.get(job.queueName());
            if (processor != null && processor.canCancel(job)) {

                try {

                    processor.cancel(job);
                    Job cancelledJob = job.withState(JobState.CANCELLED);
                    jobQueue.updateJobStatus(cancelledJob);
                    notifyJobWatchers(cancelledJob);
                } catch (Exception e) {
                    final var error = new JobCancellationException(jobId, e.getMessage());
                    Logger.error(JobQueueAPIImpl.class, error);
                    throw error;
                }
            } else {
                final var error = new JobCancellationException(jobId, "Job cannot be cancelled");
                Logger.error(JobQueueAPIImpl.class, error);
                throw error;
            }
        } else {
            final var error = new JobCancellationException(jobId, "Job not found");
            Logger.error(JobQueueAPIImpl.class, error);
            throw error;
        }
    }

    @Override
    public void watchJob(final String jobId, final Consumer<Job> watcher) {
        jobWatchers.computeIfAbsent(jobId, k -> new CopyOnWriteArrayList<>()).add(watcher);
        Job currentJob = jobQueue.job(jobId);
        watcher.accept(currentJob);
    }

    @Override
    public void setRetryStrategy(final String queueName, final RetryStrategy retryStrategy) {
        retryStrategies.put(queueName, retryStrategy);
    }

    @Override
    public void resetCircuitBreaker() {
        Logger.info(this, "Manually resetting CircuitBreaker");
        circuitBreaker.reset();
    }

    @Override
    public String getCircuitBreakerStatus() {
        return String.format("CircuitBreaker - Open: %b, Failure Count: %d, Last Failure: %s",
                circuitBreaker.isOpen(),
                circuitBreaker.getFailureCount(),
                circuitBreaker.getLastFailureTime() > 0
                        ? new Date(circuitBreaker.getLastFailureTime()).toString()
                        : "N/A");
    }

    /**
     * Notifies all registered watchers for a job of its current state.
     *
     * @param job The job whose watchers to notify.
     */
    private void notifyJobWatchers(final Job job) {
        List<Consumer<Job>> watchers = jobWatchers.get(job.id());
        if (watchers != null) {
            watchers.forEach(watcher -> watcher.accept(job));
            if (job.state() == JobState.COMPLETED
                    || job.state() == JobState.FAILED
                    || job.state() == JobState.CANCELLED) {
                jobWatchers.remove(job.id());
            }
        }
    }

    /**
     * Updates the progress of a job and notifies its watchers.
     *
     * @param job             The job whose progress to update.
     * @param progressTracker The processor progress tracker
     */
    private void updateJobProgress(final Job job, final ProgressTracker progressTracker) {
        if (job != null) {
            jobQueue.updateJobProgress(job.id(), progressTracker.progress());
            notifyJobWatchers(job);
        }
    }

    /**
     * The main job processing loop. This method continuously checks for and processes jobs.
     */
    private void processJobs() {

        while (!Thread.currentThread().isInterrupted()) {

            if (isCircuitBreakerOpen()) {
                continue;
            }

            try {

                Job job = fetchNextJob();
                if (job != null) {
                    processJobWithRetry(job);
                } else {
                    // If no jobs were found, wait for a short time before checking again
                    Thread.sleep(1000);
                }

            } catch (InterruptedException e) {
                Logger.error(this, "Job processing thread interrupted: " + e.getMessage(), e);
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                Logger.error(this, "Unexpected error in job processing loop: " + e.getMessage(), e);
                circuitBreaker.recordFailure();
            }
        }
    }

    /**
     * Checks if the circuit breaker is open and handles the waiting period if it is.
     *
     * @return true if the circuit breaker is open, false otherwise.
     */
    private boolean isCircuitBreakerOpen() {
        if (!circuitBreaker.allowRequest()) {
            Logger.warn(this, "Circuit breaker is open. Pausing job processing for a while.");
            try {
                Thread.sleep(5000); // Wait for 5 seconds before checking again
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return true;
        }
        return false;
    }

    /**
     * Fetches the next job to be processed, either pending or failed.
     *
     * @return The next job to be processed, or null if no job is available.
     */
    private Job fetchNextJob() {
        Job job = jobQueue.nextPendingJob();
        if (job == null) {
            job = jobQueue.nextFailedJob();
        }
        return job;
    }

    /**
     * Processes a job, handling retries if necessary. This method determines whether a job should
     * be processed immediately, retried later, or handled as a non-retryable failure.
     *
     * @param job The job to be processed.
     */
    private void processJobWithRetry(final Job job) {

        if (job.state() == JobState.FAILED) {
            if (canRetry(job)) {
                handleFailedJobWithRetry(job);
            } else {
                handleNonRetryableFailedJob(job);
            }
        } else {
            processJob(job);
        }
    }

    /**
     * Handles a failed job that is eligible for retry. If it's time for the next retry attempt, the
     * job is processed; otherwise, it's updated in the queue for a future retry.
     *
     * @param job The failed job that can be retried.
     */
    private void handleFailedJobWithRetry(final Job job) {

        long now = System.currentTimeMillis();
        long nextRetryTime = job.lastRetryTimestamp() + nextRetryDelay(job);
        if (now >= nextRetryTime) {
            processJob(job);
        } else {
            jobQueue.updateJobStatus(job); // Put the job back in the queue for later retry
        }
    }

    /**
     * Handles a failed job that cannot be retried. This method logs a warning about the
     * non-retryable job and removes it from the active queue.
     *
     * @param job The failed job that cannot be retried.
     */
    private void handleNonRetryableFailedJob(final Job job) {

        Logger.warn(this, "Job " + job.id() + " has failed and cannot be retried.");
        jobQueue.removeJob(job.id());
    }

    /**
     * Processes a single job.
     *
     * @param job The job to process.
     */
    private void processJob(final Job job) {

        JobProcessor processor = processors.get(job.queueName());
        if (processor != null) {

            Job runningJob = job.withState(JobState.RUNNING);
            jobQueue.updateJobStatus(runningJob);
            notifyJobWatchers(runningJob);

            try (final CloseableScheduledExecutor closeableExecutor = new CloseableScheduledExecutor()) {

                ScheduledExecutorService progressUpdater = closeableExecutor.getExecutorService();
                final ProgressTracker progressTracker = processor.progressTracker(runningJob);

                // Start a separate thread to periodically update and persist progress
                progressUpdater.scheduleAtFixedRate(() ->
                        updateJobProgress(runningJob, progressTracker), 0, 1, TimeUnit.SECONDS
                );

                try {
                    processor.process(runningJob);
                } finally {
                    // Ensure final progress is updated
                    updateJobProgress(runningJob, progressTracker);
                }

                Job completedJob = runningJob.markAsCompleted();
                jobQueue.updateJobStatus(completedJob);

                notifyJobWatchers(completedJob);
            } catch (Exception e) {

                Logger.error(this,
                        "Error processing job " + runningJob.id() + ": " + e.getMessage(), e);
                final var errorDetail = ErrorDetail.builder()
                        .message("Job processing failed")
                        .exception(e)
                        .processingStage("Job execution")
                        .timestamp(LocalDateTime.now())
                        .build();
                handleJobFailure(runningJob, errorDetail);
            }
        } else {

            Logger.error(this, "No processor found for queue: " + job.queueName());
            final var errorDetail = ErrorDetail.builder()
                    .message("No processor found for queue")
                    .processingStage("Processor selection")
                    .timestamp(LocalDateTime.now())
                    .build();
            Job failedJob = job.markAsFailed(errorDetail);
            jobQueue.updateJobStatus(failedJob);

            notifyJobWatchers(failedJob);
        }
    }

    /**
     * Handles the failure of a job, including retry logic.
     *
     * @param job         The job that failed.
     * @param errorDetail The details of the error that caused the failure.
     */
    private void handleJobFailure(final Job job, final ErrorDetail errorDetail) {

        Job updatedJob;
        if (canRetry(job)) {
            updatedJob = job.incrementRetry().
                    markAsFailed(errorDetail);
        } else {
            updatedJob = job.markAsFailed(errorDetail);
        }
        jobQueue.updateJobStatus(updatedJob);
        notifyJobWatchers(updatedJob);
    }

    /**
     * Gets the retry strategy for a specific queue.
     *
     * @param queueName The name of the queue.
     * @return The retry strategy for the queue, or the default strategy if none is set.
     */
    private RetryStrategy retryStrategy(final String queueName) {
        return retryStrategies.getOrDefault(queueName, defaultRetryStrategy);
    }

    /**
     * Determines whether a job is eligible for retry based on its retry strategy.
     *
     * @param job The job to check for retry eligibility.
     * @return {@code true} if the job is eligible for retry, {@code false} otherwise.
     */
    private boolean canRetry(final Job job) {
        final RetryStrategy retryStrategy = retryStrategy(job.queueName());
        return retryStrategy.shouldRetry(job, job.lastException());
    }

    /**
     * Calculates the next retry delay for a job based on its retry strategy.
     *
     * @param job The job for which to calculate the next retry delay.
     * @return The next retry delay for the job, in milliseconds.
     */
    private long nextRetryDelay(final Job job) {
        final RetryStrategy retryStrategy = retryStrategy(job.queueName());
        return retryStrategy.nextRetryDelay(job);
    }

    /**
     * A wrapper class that makes ScheduledExecutorService auto-closeable. This class is designed to
     * be used with try-with-resources to ensure that the ScheduledExecutorService is properly shut
     * down when it's no longer needed.
     *
     * <p>Usage example:</p>
     * <pre>
     * try (CloseableScheduledExecutor executor = new CloseableScheduledExecutor()) {
     *     ScheduledExecutorService service = executor.getExecutorService();
     *     // Use the service...
     * } // The executor service is automatically shut down here
     * </pre>
     */
    private static class CloseableScheduledExecutor implements AutoCloseable {

        private final ScheduledExecutorService executorService;

        /**
         * Constructs a new CloseableScheduledExecutor. This creates a new single-threaded
         * ScheduledExecutorService.
         */
        public CloseableScheduledExecutor() {
            this.executorService = Executors.newSingleThreadScheduledExecutor();
        }

        /**
         * Gets the wrapped ScheduledExecutorService.
         *
         * @return the ScheduledExecutorService
         */
        public ScheduledExecutorService getExecutorService() {
            return executorService;
        }

        /**
         * Closes the ScheduledExecutorService. This method attempts to perform an orderly shutdown,
         * waiting up to 60 seconds for submitted tasks to complete. If the shutdown doesn't
         * complete within this time, it forces an immediate shutdown.
         *
         * <p>This method is automatically called when used with try-with-resources.</p>
         *
         * @throws RuntimeException if the current thread is interrupted while waiting for the
         *                          executor service to terminate
         */
        @Override
        public void close() {
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException ie) {
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

}
