package com.dotcms.jobs.business.api;

import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.business.WrapInTransaction;
import com.dotcms.jobs.business.api.events.JobCancelRequestEvent;
import com.dotcms.jobs.business.api.events.JobCancellingEvent;
import com.dotcms.jobs.business.api.events.JobCompletedEvent;
import com.dotcms.jobs.business.api.events.JobCreatedEvent;
import com.dotcms.jobs.business.api.events.JobFailedEvent;
import com.dotcms.jobs.business.api.events.JobProgressUpdatedEvent;
import com.dotcms.jobs.business.api.events.JobStartedEvent;
import com.dotcms.jobs.business.api.events.JobWatcher;
import com.dotcms.jobs.business.api.events.RealTimeJobMonitor;
import com.dotcms.jobs.business.detector.AbandonedJobDetector;
import com.dotcms.jobs.business.error.CircuitBreaker;
import com.dotcms.jobs.business.error.ErrorDetail;
import com.dotcms.jobs.business.error.JobCancellationException;
import com.dotcms.jobs.business.error.JobProcessorNotFoundException;
import com.dotcms.jobs.business.error.JobValidationException;
import com.dotcms.jobs.business.error.RetryPolicyProcessor;
import com.dotcms.jobs.business.error.RetryStrategy;
import com.dotcms.jobs.business.job.Job;
import com.dotcms.jobs.business.job.JobPaginatedResult;
import com.dotcms.jobs.business.job.JobResult;
import com.dotcms.jobs.business.job.JobState;
import com.dotcms.jobs.business.processor.Cancellable;
import com.dotcms.jobs.business.processor.DefaultProgressTracker;
import com.dotcms.jobs.business.processor.DefaultRetryStrategy;
import com.dotcms.jobs.business.processor.JobProcessor;
import com.dotcms.jobs.business.processor.ProgressTracker;
import com.dotcms.jobs.business.processor.Queue;
import com.dotcms.jobs.business.processor.Validator;
import com.dotcms.jobs.business.queue.JobQueue;
import com.dotcms.jobs.business.queue.error.JobNotFoundException;
import com.dotcms.jobs.business.queue.error.JobQueueDataException;
import com.dotcms.jobs.business.queue.error.JobQueueException;
import com.dotcms.jobs.business.util.JobUtil;
import com.dotcms.shutdown.ShutdownCoordinator;
import com.dotcms.system.event.local.model.EventSubscriber;
import com.dotcms.util.AnnotationUtils;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DoesNotExistException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.google.common.annotations.VisibleForTesting;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;

/**
 * Manages the processing of jobs in a distributed job queue system. This class is responsible for
 * job creation, execution, monitoring, and error handling.
 * <pre>{@code
 *
 *     @Inject
 *     private JobQueueManagerAPI jobQueueManagerAPI;
 *
 *     public static void main(String[] args) {
 *
 *        // (Optional) Set up a retry strategy for content import jobs, if not set, the default retry strategy will be used
 *        RetryStrategy contentImportRetryStrategy = new ExponentialBackoffRetryStrategy(
 *                5000, 300000, 2.0, 3
 *        );
 *        contentImportRetryStrategy.addRetryableException(IOException.class);
 *        jobQueueManagerAPI.setRetryStrategy("contentImport", contentImportRetryStrategy);
 *
 *        // Register job processors
 *        jobQueueManagerAPI.registerProcessor("contentImport", ContentImportJobProcessor.class);
 *
 *        // Start the job queue manager
 *        jobQueueManagerAPI.start();
 *
 *        // Create a content import job (dummy example)
 *        Map<String, Object> jobParameters = new HashMap<>();
 *        jobParameters.put("filePath", "/path/to/import/file.csv");
 *        jobParameters.put("contentType", "Article");
 *        String jobId = jobQueueManagerAPI.createJob("contentImport", jobParameters);
 *
 *        // Optionally, watch the job progress
 *        jobQueueManagerAPI.watchJob(jobId, job -> {
 *            System.out.println("Job " + job.id() + " progress: " + job.progress() * 100 + "%");
 *        });
 *
 *        // When shutting down the application
 *        jobQueueManagerAPI.close();
 *     }
 * }</pre>
 */
@ApplicationScoped
public class JobQueueManagerAPIImpl implements JobQueueManagerAPI {

    private final AtomicBoolean isStarted = new AtomicBoolean(false);
    private CountDownLatch startLatch;
    private volatile boolean isShuttingDown = false;
    private volatile boolean isClosed = false;

    private final CircuitBreaker circuitBreaker;
    private final JobQueue jobQueue;
    private final AbandonedJobDetector abandonedJobDetector;
    private final Map<String, Class<? extends JobProcessor>> processors;
    private final Map<String, JobProcessor> processorInstancesByJobId;
    private final int threadPoolSize;
    private ExecutorService executorService;
    private final Map<String, RetryStrategy> retryStrategies;
    private final RetryStrategy defaultRetryStrategy;
    private final RetryPolicyProcessor retryPolicyProcessor;

    private final RealTimeJobMonitor realTimeJobMonitor;

    private final JobProcessorFactory jobProcessorFactory;

    // Cap to prevent overflow
    private static final int MAX_EMPTY_QUEUE_COUNT = 30;
    // Arbitrary threshold to reset
    private static final int EMPTY_QUEUE_RESET_THRESHOLD = Integer.MAX_VALUE - 1000;

    /**
     * Constructs a new JobQueueManagerAPIImpl. This constructor initializes the job queue manager
     * with all necessary dependencies and configurations.
     *
     * @param jobQueue             The JobQueue implementation to use for managing jobs.
     * @param jobQueueConfig       The JobQueueConfig implementation providing configuration
     *                             settings.
     * @param circuitBreaker       The CircuitBreaker implementation for fault tolerance.
     * @param defaultRetryStrategy The default retry strategy to use for failed jobs.
     * @param realTimeJobMonitor   The RealTimeJobMonitor for handling real-time job updates.
     * @param jobProcessorFactory  The JobProcessorFactory for creating job processors instances.
     * @param retryPolicyProcessor The RetryPolicyProcessor for processing retry policies.
     * @param abandonedJobDetector The AbandonedJobDetector for detecting abandoned jobs.
     * @param discovery            The JobProcessorDiscovery for discovering job processors.
     */
    @Inject
    public JobQueueManagerAPIImpl(@Named("queueProducer") JobQueue jobQueue,
            JobQueueConfig jobQueueConfig,
            CircuitBreaker circuitBreaker,
            @DefaultRetryStrategy RetryStrategy defaultRetryStrategy,
            RealTimeJobMonitor realTimeJobMonitor,
            JobProcessorFactory jobProcessorFactory,
            RetryPolicyProcessor retryPolicyProcessor,
            AbandonedJobDetector abandonedJobDetector,
            JobProcessorDiscovery discovery) {

        this.jobQueue = jobQueue;
        this.threadPoolSize = jobQueueConfig.getThreadPoolSize();
        this.processors = new ConcurrentHashMap<>();
        this.processorInstancesByJobId = new ConcurrentHashMap<>();
        this.retryStrategies = new ConcurrentHashMap<>();
        this.defaultRetryStrategy = defaultRetryStrategy;
        this.circuitBreaker = circuitBreaker;
        this.jobProcessorFactory = jobProcessorFactory;
        this.retryPolicyProcessor = retryPolicyProcessor;
        this.abandonedJobDetector = abandonedJobDetector;
        this.realTimeJobMonitor = realTimeJobMonitor;

        // Register discovered processors by CDI
        discovery.discoverJobProcessors().forEach(this::registerProcessor);

        APILocator.getLocalSystemEventsAPI().subscribe(
                JobCancelRequestEvent.class,
                (EventSubscriber<JobCancelRequestEvent>) this::onCancelRequestJob
        );
    }

    @Override
    public boolean isStarted() {
        return isStarted.get() && !isClosed;
    }

    @Override
    public boolean awaitStart(long timeout, TimeUnit unit) throws InterruptedException {
        return startLatch.await(timeout, unit);
    }

    @Override
    public void start() {

        if (isClosed) {
            Logger.warn(this, "Attempt to start JobQueue that has been closed. Ignoring.");
            return;
        }

        if (isStarted.compareAndSet(false, true)) {

            Logger.info(
                    this, "Starting JobQueue with " + threadPoolSize + " threads."
            );

            startLatch = new CountDownLatch(threadPoolSize);
            executorService = Executors.newFixedThreadPool(threadPoolSize, Thread.ofVirtual().factory());

            for (int i = 0; i < threadPoolSize; i++) {
                executorService.submit(() -> {
                    startLatch.countDown();
                    processJobs();
                });
            }

            // Start the abandoned job detector
            abandonedJobDetector.start();

            Logger.info(this,
                    "JobQueue and abandoned job detector have been successfully started.");
        } else {
            Logger.warn(this,
                    "Attempt to start JobQueue that is already running. Ignoring."
            );
        }
    }

    @Override
    public void close() throws Exception {

        if (isClosed) {
            Logger.warn(this, "JobQueue is already closed. Ignoring.");
            return;
        }

        if (isStarted.compareAndSet(true, false)) {

            isShuttingDown = true;
            Logger.info(this, "Closing JobQueue and stopping all job processing.");

            // Stop the abandoned job detector
            abandonedJobDetector.close();

            // Close existing services
            closeExecutorService(executorService);

            // Keep isShuttingDown true until fully closed to prevent race conditions
            // isShuttingDown = false; // Remove this to prevent race conditions
            isClosed = true;
            Logger.info(this, "JobQueue has been successfully closed.");
        } else {
            Logger.warn(this,
                    "Attempt to close JobQueue that is not running. Ignoring."
            );
        }
    }

    @Override
    public void registerProcessor(final Class<? extends JobProcessor> processor) {

        Queue queue = AnnotationUtils.getBeanAnnotation(processor, Queue.class);
        String queueName = Objects.nonNull(queue) ? queue.value() : processor.getName();
        registerProcessor(queueName, processor);
    }

    @Override
    public void registerProcessor(final String queueName, final Class<? extends JobProcessor> processor) {

        Logger.info(this, "Registering JobProcessor: " + processor.getName());

        final Class<? extends JobProcessor> jobProcessor = processors.get(queueName);
        if (null != jobProcessor) {
            Logger.warn(this, String.format(
                    "Job processor [%s] already registered for queue: [%s] is getting overridden.",
                    jobProcessor.getName(), queueName));
        }
        processors.put(queueName, processor);

        // Process the retry policy for the processor
        RetryStrategy retryStrategy = retryPolicyProcessor.processRetryPolicy(processor);
        if (retryStrategy != null) {
            setRetryStrategy(queueName, retryStrategy);
        }
    }

    @Override
    public Map<String,Class<? extends JobProcessor>> getQueueNames() {
        return Map.copyOf(processors);
    }

    @WrapInTransaction
    @Override
    public String createJob(final String queueName, final Map<String, Object> parameters)
            throws JobProcessorNotFoundException, DotDataException {

        // Validate a processor is registered for the queue
        final Class<? extends JobProcessor> clazz = processors.get(queueName);
        if (null == clazz) {
            final var error = new JobProcessorNotFoundException(queueName);
            Logger.error(JobQueueManagerAPIImpl.class, error);
            throw error;
        }

        //first attempt instantiating the processor, cuz if we cant no use to create an entry in the db
        final var processor = newProcessorInstance(queueName);

        // Validate job parameters if processor implements Validator
        if (processor instanceof Validator) {
            try {
                ((Validator) processor).validate(parameters);
            } catch (JobValidationException e) {
                Logger.error(this, "Job validation failed: " + e.getMessage(), e);
                throw e;
            }
        }

        try {

            // now that we know we can instantiate the processor, we can add it to the map of instances
            // But first we need the job id
            final String jobId = jobQueue.createJob(queueName, parameters);
            addInstanceRef(jobId, processor);

            // Send the job created events
            JobUtil.sendEvents(
                    new JobCreatedEvent(jobId, queueName, LocalDateTime.now(), parameters));
            return jobId;
        } catch (JobQueueException e) {
            throw new DotDataException("Error creating job", e);
        }
    }

    @CloseDBIfOpened
    @Override
    public Job getJob(final String jobId) throws DotDataException {
        try {
            return jobQueue.getJob(jobId);
        } catch (JobNotFoundException e) {
            throw new DoesNotExistException(e);
        } catch (JobQueueDataException e) {
            throw new DotDataException("Error fetching job", e);
        }
    }

    @CloseDBIfOpened
    @Override
    public JobPaginatedResult getActiveJobs(final String queueName, final int page, final int pageSize)
            throws DotDataException {
        try {
            return jobQueue.getActiveJobs(queueName, page, pageSize);
        } catch (JobQueueDataException e) {
            throw new DotDataException("Error fetching active jobs", e);
        }
    }

    @CloseDBIfOpened
    @Override
    public JobPaginatedResult getCompletedJobs(final String queueName, final int page, final int pageSize)
            throws DotDataException {
        try {
            return jobQueue.getCompletedJobs(queueName, page, pageSize);
        } catch (JobQueueDataException e) {
            throw new DotDataException("Error fetching completed jobs", e);
        }
    }


    @CloseDBIfOpened
    @Override
    public JobPaginatedResult getCanceledJobs(final String queueName, final int page, final int pageSize)
            throws DotDataException {
        try {
            return jobQueue.getCanceledJobs(queueName, page, pageSize);
        } catch (JobQueueDataException e) {
            throw new DotDataException("Error fetching completed jobs", e);
        }
    }

    @CloseDBIfOpened
    @Override
    public JobPaginatedResult getFailedJobs(final String queueName, final int page, final int pageSize)
            throws DotDataException {
        try {
            return jobQueue.getFailedJobs(queueName, page, pageSize);
        } catch (JobQueueDataException e) {
            throw new DotDataException("Error fetching failed jobs", e);
        }
    }

    @CloseDBIfOpened
    @Override
    public JobPaginatedResult getAbandonedJobs(final String queueName, final int page, final int pageSize)
            throws DotDataException {
        try {
            return jobQueue.getAbandonedJobs(queueName, page, pageSize);
        } catch (JobQueueDataException e) {
            throw new DotDataException("Error fetching abandoned jobs", e);
        }
    }

    @CloseDBIfOpened
    @Override
    public JobPaginatedResult getSuccessfulJobs(final String queueName, final int page, final int pageSize)
            throws DotDataException {
        try {
            return jobQueue.getSuccessfulJobs(queueName, page, pageSize);
        } catch (JobQueueDataException e) {
            throw new DotDataException("Error fetching successful jobs", e);
        }
    }

    @CloseDBIfOpened
    @Override
    public JobPaginatedResult getJobs(final String queueName, final int page, final int pageSize)
            throws DotDataException {
        try {
            return jobQueue.getJobs(queueName, page, pageSize);
        } catch (JobQueueDataException e) {
            throw new DotDataException("Error fetching jobs", e);
        }
    }

    @CloseDBIfOpened
    @Override
    public JobPaginatedResult getJobs(final int page, final int pageSize) throws DotDataException {
        try {
            return jobQueue.getJobs(page, pageSize);
        } catch (JobQueueDataException e) {
            throw new DotDataException("Error fetching jobs", e);
        }
    }

    @CloseDBIfOpened
    @Override
    public JobPaginatedResult getActiveJobs(final int page, final int pageSize)
            throws DotDataException {
        try {
            return jobQueue.getActiveJobs(page, pageSize);
        } catch (JobQueueDataException e) {
            throw new DotDataException("Error fetching active jobs", e);
        }
    }

    @CloseDBIfOpened
    @Override
    public JobPaginatedResult getCompletedJobs(final int page, final int pageSize)
            throws DotDataException {
        try {
            return jobQueue.getCompletedJobs(page, pageSize);
        } catch (JobQueueDataException e) {
            throw new DotDataException("Error fetching completed jobs", e);
        }
    }

    @CloseDBIfOpened
    @Override
    public JobPaginatedResult getSuccessfulJobs(final int page, final int pageSize)
            throws DotDataException {
        try {
            return jobQueue.getSuccessfulJobs(page, pageSize);
        } catch (JobQueueDataException e) {
            throw new DotDataException("Error fetching successful jobs", e);
        }
    }

    @CloseDBIfOpened
    @Override
    public JobPaginatedResult getCanceledJobs(final int page, final int pageSize)
            throws DotDataException {
        try {
            return jobQueue.getCanceledJobs(page, pageSize);
        } catch (JobQueueDataException e) {
            throw new DotDataException("Error fetching canceled jobs", e);
        }
    }

    @CloseDBIfOpened
    @Override
    public JobPaginatedResult getFailedJobs(final int page, final int pageSize)
            throws DotDataException {
        try {
            return jobQueue.getFailedJobs(page, pageSize);
        } catch (JobQueueDataException e) {
            throw new DotDataException("Error fetching failed jobs", e);
        }
    }

    @CloseDBIfOpened
    @Override
    public JobPaginatedResult getAbandonedJobs(final int page, final int pageSize) throws DotDataException {
        try {
            return jobQueue.getAbandonedJobs(page, pageSize);
        } catch (JobQueueDataException e) {
            throw new DotDataException("Error fetching abandoned jobs", e);
        }
    }

    @Override
    public void cancelJob(final String jobId) throws DotDataException {

        final Job job = getJob(jobId);

        if (isInCancelableState(job)) {
            handleJobCancelRequest(job);
        } else {
            Logger.warn(this, "Job " + job.id() + " is not in a cancellable state. "
                    + "Current state: " + job.state());
        }

    }

    /**
     * Handles the cancellation of a job based on the given JobCancelRequestEvent. Retrieves the job
     * and checks its state. If the job is in a PENDING or RUNNING state, attempts to cancel it by
     * leveraging the job's associated processor. Logs and throws exceptions if any issues occur
     * during the cancellation process.
     *
     * @param event The event that triggers the job cancellation request.
     */
    @VisibleForTesting
    void onCancelRequestJob(final JobCancelRequestEvent event) {

        try {

            final var job = getJob(event.getJob().id());
            if (isInCancelableState(job) || job.state() == JobState.CANCEL_REQUESTED) {

                final Optional<JobProcessor> instance = getInstance(job.id());
                if (instance.isPresent()) {
                    final var processor = instance.get();
                    if (processor instanceof Cancellable) {
                        handleJobCancelling(job, processor);
                    } else {
                        final var error = new JobCancellationException(
                                job.id(), "Job is not Cancellable");
                        Logger.error(JobQueueManagerAPIImpl.class, error);
                        throw error;
                    }
                } else {
                    // In a cluster, the job may be running on another server
                    Logger.debug(this,
                            "Job cancellation requested. No processor found for job " + job.id());
                }
            } else {
                Logger.warn(this, "Job " + job.id() + " is not in a cancellable state. "
                        + "Current state: " + job.state());
            }
        } catch (DotDataException e) {
            throw new JobCancellationException(event.getJob().id(), e);
        }

    }

    @Override
    public JobWatcher watchJob(final String jobId, final Consumer<Job> watcher) {
        return realTimeJobMonitor.registerWatcher(jobId, watcher);
    }

    @Override
    public void removeJobWatcher(final String jobId, final JobWatcher watcher) {
        realTimeJobMonitor.removeWatcher(jobId, watcher);
    }

    @Override
    public void removeAllJobWatchers(final String jobId) {
        realTimeJobMonitor.removeAllWatchers(jobId);
    }

    @Override
    public void setRetryStrategy(final String queueName, final RetryStrategy retryStrategy) {
        retryStrategies.put(queueName, retryStrategy);
    }

    @Override
    @VisibleForTesting
    public Optional<JobProcessor> getInstance(final String jobId) {
        return Optional.ofNullable(processorInstancesByJobId.get(jobId));
    }

    @Override
    @VisibleForTesting
    public CircuitBreaker getCircuitBreaker() {
        return this.circuitBreaker;
    }

    @Override
    @VisibleForTesting
    public JobQueue getJobQueue() {
        return this.jobQueue;
    }

    @Override
    @VisibleForTesting
    public int getThreadPoolSize() {
        return this.threadPoolSize;
    }

    @Override
    @VisibleForTesting
    public RetryStrategy getDefaultRetryStrategy() {
        return this.defaultRetryStrategy;
    }

    /**
     * Fetches the state of a job from the job queue using the provided job ID.
     *
     * @param jobId the unique identifier of the job whose state is to be fetched
     * @return the current state of the job
     * @throws DotDataException if there is an error accessing the job state data
     */
    @CloseDBIfOpened
    private JobState getJobState(final String jobId) throws DotDataException {
        try {
            return jobQueue.getJobState(jobId);
        } catch (JobNotFoundException e) {
            throw new DoesNotExistException(e);
        } catch (JobQueueDataException e) {
            throw new DotDataException("Error fetching job state", e);
        }
    }

    /**
     * Updates the progress of a job and notifies its watchers.
     *
     * @param job             The job whose progress to update.
     * @param progressTracker The processor progress tracker
     * @param previousProgress The previous progress value
     */
    private float updateJobProgress(final Job job, final ProgressTracker progressTracker,
            final float previousProgress) throws DotDataException {

        try {
            if (job != null) {

                float progress = progressTracker.progress();

                // Only update progress if it has changed
                if (progress > previousProgress) {

                    // Make sure we have the latest state, the job of the running processor won't
                    // be updated with changes on the state, like a cancel request.
                    final var latestState = getJobState(job.id());

                    Job updatedJob = job.withProgress(progress).withState(latestState);

                    jobQueue.updateJobProgress(job.id(), updatedJob.progress());

                    // Send the update job progress events
                    JobUtil.sendEvents(updatedJob, JobProgressUpdatedEvent::new);

                    return progress;
                }
            }
        } catch (JobQueueDataException e) {
            Logger.error(this, "Error updating job progress: " + e.getMessage(), e);
            throw new DotDataException("Error updating job progress", e);
        }

        return -1;
    }

    /**
     * The main job processing loop. This method continuously checks for and processes jobs.
     */
    private void processJobs() {

        int emptyQueueCount = 0;

        while (!Thread.currentThread().isInterrupted() && !isShuttingDown && !ShutdownCoordinator.isShutdownStarted()) {

            if (isCircuitBreakerOpen()) {
                continue;
            }

            try {

                final boolean jobProcessed = processNextJob();
                if (jobProcessed) {
                    emptyQueueCount = 0;
                } else {
                    // If no jobs were found, wait for a short time before checking again
                    // Implement exponential backoff when queue is repeatedly empty
                    long sleepTime = calculateBackoffTime(emptyQueueCount, MAX_EMPTY_QUEUE_COUNT);
                    
                    // During shutdown, use shorter sleep times to be more responsive
                    if (isShuttingDown) {
                        sleepTime = Math.min(sleepTime, 100); // Max 100ms during shutdown
                    }
                    
                    Thread.sleep(sleepTime);
                    emptyQueueCount = incrementAndResetEmptyQueueCount(
                            emptyQueueCount, MAX_EMPTY_QUEUE_COUNT, EMPTY_QUEUE_RESET_THRESHOLD
                    );
                }
            } catch (InterruptedException e) {
                Logger.debug(this, "Job processing thread interrupted during shutdown");
                Thread.currentThread().interrupt();
                break; // Exit the loop immediately on interruption
            } catch (Exception e) {
                // Check if this is a database-related error during shutdown
                if (isShuttingDown && ShutdownCoordinator.isShutdownRelated(e)) {
                    Logger.debug(this, "Database connection lost during shutdown (expected), stopping job processing");
                    break;
                }
                
                Logger.error(this, "Unexpected error in job processing loop: " + e.getMessage(), e);
                getCircuitBreaker().recordFailure();
                
                // During shutdown, don't continue processing on errors
                if (isShuttingDown) {
                    Logger.debug(this, "Stopping job processing due to error during shutdown");
                    break;
                }
            }
        }
        
        Logger.debug(this, "Job processing thread exiting (interrupted: " + 
            Thread.currentThread().isInterrupted() + ", shuttingDown: " + isShuttingDown + ")");
    }
    
    

    /**
     * Processes the next job in the queue.
     *
     * @return {@code true} if a job was processed, {@code false} if the queue is empty.
     */
    @CloseDBIfOpened
    private boolean processNextJob() throws DotDataException {

        try {
            // Skip database calls during shutdown to avoid blocking threads
            if (isShuttingDown || ShutdownCoordinator.isShutdownStarted()) {
                Logger.debug(this, "Skipping job processing during shutdown");
                return false;
            }
            
            Job job = jobQueue.nextJob();
            if (job != null) {
                // Double-check shutdown status before processing
                if (isShuttingDown) {
                    Logger.debug(this, "Shutdown detected, skipping job processing");
                    return false;
                }
                processJobWithRetry(job);
                return true;
            }

            return false;
        } catch (JobQueueException e) {
            Logger.error(this, "Error fetching next job: " + e.getMessage(), e);
            throw new DotDataException("Error fetching next job", e);
        }
    }

    /**
     * Closes an ExecutorService, shutting it down and waiting for any running jobs to complete.
     * Uses a shorter timeout during shutdown to be responsive to dotCMS shutdown coordination.
     *
     * @param executor The ExecutorService to close.
     */
    private void closeExecutorService(final ExecutorService executor) {

        Logger.info(this, "Shutting down job queue executor service");
        
        // During coordinated shutdown, be more aggressive to prevent delays
        if (ShutdownCoordinator.isShutdownStarted()) {
            Logger.debug(this, "System shutdown detected - using immediate executor termination");
            var cancelledTasks = executor.shutdownNow();
            Logger.debug(this, "Cancelled " + cancelledTasks.size() + " pending tasks during immediate shutdown");
        } else {
            executor.shutdown();
        }

        try {
            // Use shorter timeout to be more responsive to shutdown coordination
            // Most job processing threads should exit quickly due to shutdown checks
            int defaultTimeout = ShutdownCoordinator.isShutdownStarted() ? 2 : 3; // Less time during coordinated shutdown since we use shutdownNow
            int gracefulTimeoutSeconds = Config.getIntProperty("shutdown.jobqueue.executor.timeout.seconds", defaultTimeout);
            
            if (!executor.awaitTermination(gracefulTimeoutSeconds, TimeUnit.SECONDS)) {
                // During coordinated shutdown, this is expected behavior - use debug level
                if (ShutdownCoordinator.isShutdownStarted()) {
                    Logger.debug(this, "Job queue executor graceful shutdown timeout during system shutdown, forcing termination");
                } else {
                    Logger.warn(this, "Job queue executor did not terminate gracefully within " + 
                        gracefulTimeoutSeconds + " seconds, forcing shutdown");
                }
                
                // Interrupt all threads to break out of blocking operations
                var cancelledTasks = executor.shutdownNow();
                Logger.debug(this, "Cancelled " + cancelledTasks.size() + " pending tasks during forced shutdown");
                
                // Give forced shutdown a brief moment to complete
                if (!executor.awaitTermination(2, TimeUnit.SECONDS)) {
                    Logger.error(this, "Job queue executor did not terminate even after forced shutdown");
                } else {
                    Logger.info(this, "Job queue executor terminated after forced shutdown");
                }
            } else {
                Logger.info(this, "Job queue executor shut down gracefully");
            }
        } catch (InterruptedException e) {
            Logger.warn(this, "Job queue shutdown interrupted, forcing immediate termination");
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Checks if the circuit breaker is open and handles the waiting period if it is.
     *
     * @return true if the circuit breaker is open, false otherwise.
     */
    private boolean isCircuitBreakerOpen() {

        if (!getCircuitBreaker().allowRequest()) {

            Logger.warn(this, "Circuit breaker is open. Pausing job processing for a while.");

            try {
                // During shutdown, use shorter sleep intervals to be more responsive
                int sleepInterval = isShuttingDown ? 100 : 1000; // 100ms during shutdown, 1s normally
                int totalSleepTime = isShuttingDown ? 500 : 5000; // 500ms during shutdown, 5s normally
                
                for (int elapsed = 0; elapsed < totalSleepTime && !isShuttingDown; elapsed += sleepInterval) {
                    Thread.sleep(sleepInterval);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            return true;
        }

        return false;
    }

    /**
     * Processes a job, handling retries if necessary. This method determines whether a job should
     * be processed immediately, retried later, or handled as a non-retryable failure.
     *
     * @param job The job to be processed.
     */
    private void processJobWithRetry(final Job job) throws DotDataException {

        if (job.state() == JobState.FAILED || job.state() == JobState.ABANDONED) {

            if (canRetry(job)) {

                if (isReadyForRetry(job)) {
                    Logger.warn(this, "Retrying job " + job.id() + " after failure.");
                    processJob(job.incrementRetry());
                } else {

                    Logger.debug(this, "Job " + job.id() + " is not ready for retry, "
                            + "putting back in queue.");
                    try {
                        // Put the job back in the queue for later retry
                        jobQueue.putJobBackInQueue(job);
                    } catch (JobQueueDataException e) {
                        throw new DotDataException("Error re-queueing job", e);
                    }
                }
            } else {
                handleNonRetryableFailedJob(job);
            }
        } else {
            processJob(job);
        }
    }

    /**
     * Determines whether a job is ready for retry based on its retry strategy.
     * @param job The job to check for retry eligibility.
     * @return {@code true} if the job is ready for retry, {@code false} otherwise.
     */
    private boolean isReadyForRetry(Job job) throws DotDataException {
        long now = System.currentTimeMillis();
        long lastCompletedAt = job.completedAt()
                .orElseThrow(() -> new DotDataException("Job has not completed at"))
                .atZone(ZoneId.systemDefault()).toInstant()
                .toEpochMilli();
        long nextRetryTime = lastCompletedAt + nextRetryDelay(job);
        return now >= nextRetryTime;
    }

    /**
     * Handles a failed job that cannot be retried. This method logs a warning about the
     * non-retryable job, removes it from the active queue, and marks it as failed permanently.
     *
     * @param job The failed job that cannot be retried.
     */
    private void handleNonRetryableFailedJob(final Job job) throws DotDataException {

        Logger.warn(this, "Job " + job.id() + " has failed and cannot be retried.");

        Job finishedJob = job.markAsFailedPermanently();
        if (job.state() == JobState.ABANDONED) {
            finishedJob = job.markAsAbandonedPermanently();
        }

        // Giving the job a final state
        updateJobStatus(finishedJob);

        // Send the job completion events
        JobUtil.sendEvents(finishedJob, JobCompletedEvent::new);
    }

    /**
     * Processes a single job.
     *
     * @param job The job to process.
     */
    private void processJob(final Job job) throws DotDataException {

        // Skip job processing if shutdown is in progress
        if (isShuttingDown) {
            Logger.debug(this, "Skipping job processing for job " + job.id() + " due to shutdown");
            return;
        }

        final Optional<JobProcessor> optional = getProcessorInstance(job);
        if (optional.isPresent()) {
            final JobProcessor processor = optional.get();

            final ProgressTracker progressTracker = new DefaultProgressTracker();
            Job runningJob = job.markAsRunning().withProgressTracker(progressTracker);
            updateJobStatus(runningJob);
            // Send the job started events
            JobUtil.sendEvents(runningJob, JobStartedEvent::new);

            try (final CloseableScheduledExecutor closeableExecutor = new CloseableScheduledExecutor()) {

                // Start a separate thread to periodically update and persist progress
                AtomicReference<Float> currentProgress = new AtomicReference<>((float) 0);
                ScheduledExecutorService progressUpdater = closeableExecutor.getExecutorService();
                progressUpdater.scheduleAtFixedRate(() ->
                        {
                            try {
                                final var progress = updateJobProgress(
                                        runningJob, progressTracker, currentProgress.get()
                                );
                                if (progress >= 0) {
                                    currentProgress.set(progress);
                                }
                            } catch (DotDataException e) {
                                throw new DotRuntimeException("Error updating job progress", e);
                            }
                        }, 0, 3, TimeUnit.SECONDS
                );

                // Process the job
                processor.process(runningJob);

                // The job finished processing
                handleJobCompletion(runningJob, processor);
            } catch (Exception e) {

                Logger.error(this,
                        "Error processing job " + runningJob.id() + ": " + e.getMessage(), e
                );
                handleJobFailure(
                        runningJob, processor, e, "Job execution"
                );
            } finally {
                //Free up resources
                removeInstanceRef(runningJob.id());
            }
        } else {

            Logger.error(this, "No processor found for queue: " + job.queueName());
            handleJobFailure(job, null, new JobProcessorNotFoundException(job.queueName()),
                    "Processor selection");
        }
    }

    /**
     * Get an instance of a processor for a job.
     * If an instance already exists, it will be returned. otherwise a new instance will be created.
     * @param job The job to get the processor for
     * @return The processor instance
     */
    private Optional<JobProcessor> getProcessorInstance(final Job job) {
        try {
            return Optional.of(processorInstancesByJobId.computeIfAbsent(job.id(),
                    id -> newProcessorInstance(job.queueName()))
            );
        } catch (Exception e){
          Logger.error(this, "Error getting processor instance", e);
        }
        return Optional.empty();
    }


    /**
     * Creates a new instance of a JobProcessor for a specific queue.
     * @param queueName The name of the queue
     * @return An optional containing the new JobProcessor instance, or an empty optional if the processor could not be created.
     */
    private JobProcessor newProcessorInstance(final String queueName) {
        final var processorClass = processors.get(queueName);
        if (processorClass != null) {
            return jobProcessorFactory.newInstance(processorClass);
        } else {
            throw new JobProcessorNotFoundException(queueName);
        }
    }

    /**
     * Once we're sure a processor can be instantiated, we add it to the map of instances.
     * @param jobId The ID of the job
     * @param processor The processor to add
     * @return The processor instance
     */
    private void addInstanceRef(final String jobId, final JobProcessor processor) {
        //Get an instance and put it in the map
        processorInstancesByJobId.putIfAbsent(jobId, processor);
    }

    /**
     * Removes a processor instance from the map of instances.
     * @param jobId The ID of the job
     */
    private void removeInstanceRef(final String jobId) {
        processorInstancesByJobId.remove(jobId);
    }

    /**
     * Handles the completion of a job.
     *
     * @param job       The job that completed.
     * @param processor The processor that handled the job.
     */
    private void handleJobCompletion(final Job job, final JobProcessor processor)
            throws DotDataException {

        final var resultMetadata = processor.getResultMetadata(job);

        JobResult jobResult = null;
        if (resultMetadata != null && !resultMetadata.isEmpty()) {
            jobResult = JobResult.builder().metadata(resultMetadata).build();
        }

        final float progress = getJobProgress(job);

        try {

            Job completedJob = job.markAsSuccessful(jobResult).withProgress(progress);

            if (jobQueue.hasJobBeenInState(job.id(), JobState.CANCEL_REQUESTED, JobState.CANCELLING)) {
                completedJob = job.markAsCanceled(jobResult).withProgress(progress);
            }

            updateJobStatus(completedJob);
            // Send the job completed events
            JobUtil.sendEvents(completedJob, JobCompletedEvent::new);

        } catch (JobQueueDataException e) {
            final var errorMessage = "Error updating job status";
            Logger.error(this, errorMessage, e);
            throw new DotDataException(errorMessage, e);
        }
    }

    /**
     * Handles the request to cancel a job.
     *
     * @param job The job to cancel.
     */
    private void handleJobCancelRequest(final Job job) throws DotDataException {

        Job cancelJob = job.withState(JobState.CANCEL_REQUESTED);
        updateJobStatus(cancelJob);

        // Send the cancel request events
        JobUtil.sendEvents(cancelJob, JobCancelRequestEvent::new);
    }

    /**
     * Handles the cancellation of a job.
     *
     * @param job       The job that was canceled.
     * @param processor The processor that handled the job.
     */
    private void handleJobCancelling(final Job job, final JobProcessor processor) {

        try {
            Logger.info(this, "Cancelling job " + job.id());
            ((Cancellable) processor).cancel(job);

            Job cancelJob = job.withState(JobState.CANCELLING);
            updateJobStatus(cancelJob);
            // Send the job cancelling events
            JobUtil.sendEvents(cancelJob, JobCancellingEvent::new);
        } catch (DotDataException e) {
            final var error = new JobCancellationException(job.id(), e);
            Logger.error(this, error);
            throw error;
        }
    }

    /**
     * Handles the failure of a job
     *
     * @param job             The job that failed.
     * @param processor       The processor that handled the job.
     * @param exception       The exception that caused the failure.
     * @param processingStage The stage of processing where the failure occurred.
     */
    private void handleJobFailure(final Job job, final JobProcessor processor,
            final Exception exception, final String processingStage) throws DotDataException {

        var jobFailureException = exception;
        if (exception.getCause() != null) {
            jobFailureException = (Exception) exception.getCause();
        }

        handleJobFailure(
                job, processor, jobFailureException, jobFailureException.getMessage(),
                processingStage
        );
    }

    /**
     * Handles the failure of a job
     *
     * @param job             The job that failed.
     * @param processor       The processor that handled the job.
     * @param exception       The exception that caused the failure.
     * @param errorMessage    The error message to include in the job result.
     * @param processingStage The stage of processing where the failure occurred.
     */
    private void handleJobFailure(final Job job, final JobProcessor processor,
            final Exception exception, final String errorMessage, final String processingStage)
            throws DotDataException {

        if (exception == null) {
            throw new IllegalArgumentException("Exception cannot be null");
        }

        final var errorDetail = ErrorDetail.builder()
                .message(errorMessage)
                .stackTrace(stackTrace(exception))
                .exceptionClass(exception.getClass().getName())
                .processingStage(processingStage)
                .timestamp(LocalDateTime.now())
                .build();

        JobResult jobResult = JobResult.builder().errorDetail(errorDetail).build();

        if (processor != null) {
            final var resultMetadata = processor.getResultMetadata(job);
            if (resultMetadata != null && !resultMetadata.isEmpty()) {
                jobResult = jobResult.withMetadata(resultMetadata);
            }
        }

        final float progress = getJobProgress(job);

        final Job failedJob = job.markAsFailed(jobResult).withProgress(progress);
        updateJobStatus(failedJob);
        // Send the job failed events
        JobUtil.sendEvents(failedJob, JobFailedEvent::new);

        try {
            // Put the job back in the queue for later retry
            jobQueue.putJobBackInQueue(job);
        } catch (JobQueueDataException e) {
            throw new DotDataException("Error re-queueing job", e);
        }

        // Record the failure in the circuit breaker
        getCircuitBreaker().recordFailure();
    }

    /**
     * Updates the status of a job in the job queue.
     *
     * @param job The job to update.
     * @throws DotDataException if there's an error updating the job status.
     */
    private void updateJobStatus(final Job job) throws DotDataException {
        try {
            jobQueue.updateJobStatus(job);
        } catch (JobQueueDataException e) {
            throw new DotDataException("Error updating job status", e);
        }
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

        final var jobResult = job.result();

        Class<?> lastExceptionClass = null;
        if (jobResult.isPresent()) {

            final var errorDetailOptional = jobResult.get().errorDetail();

            if (errorDetailOptional.isPresent()) {
                final var errorDetail = errorDetailOptional.get();
                final var exceptionClass = errorDetail.exceptionClass();
                if (exceptionClass != null) {
                    try {
                        lastExceptionClass = Class.forName(errorDetail.exceptionClass());
                    } catch (ClassNotFoundException e) {
                        Logger.error(this, "Error loading exception class: " + e.getMessage(), e);
                    }
                }
            }
        }

        return retryStrategy.shouldRetry(job, (Class<? extends Throwable>) lastExceptionClass);
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
     * Generates and returns the stack trace of the exception as a string. This is a derived value
     * and will be computed only when accessed.
     *
     * @param exception The exception for which to generate the stack trace.
     * @return A string representation of the exception's stack trace, or null if no exception is
     * present.
     */
    private String stackTrace(final Throwable exception) {
        if (exception != null) {
            return Arrays.stream(exception.getStackTrace())
                    .map(StackTraceElement::toString)
                    .reduce((a, b) -> a + "\n" + b)
                    .orElse("");
        }
        return null;
    }

    /**
     * Gets the progress of a job, or the progress of the job's progress tracker if present.
     *
     * @param job The job to get the progress for.
     * @return The progress of the job, or the progress of the job's progress tracker if present.
     */
    private float getJobProgress(final Job job) {

        float progress = job.progress();
        var progressTracker = job.progressTracker();
        if (progressTracker.isPresent()) {
            progress = progressTracker.get().progress();
        }

        return progress;
    }

    /**
     * Calculates the backoff time based on the number of empty queue counts.
     *
     * @param emptyQueueCount    the current count of empty queue checks
     * @param maxEmptyQueueCount the maximum count of empty queue checks
     * @return the calculated backoff time in milliseconds, the result is capped at 30,000
     * milliseconds (30 seconds) to prevent excessively long sleep times.
     */
    @VisibleForTesting
    public long calculateBackoffTime(int emptyQueueCount, int maxEmptyQueueCount) {
        emptyQueueCount = Math.min(emptyQueueCount, maxEmptyQueueCount);
        return Math.min(1000L * (1L << emptyQueueCount), 30000L);
    }

    /**
     * Increments the empty queue count and resets it if it exceeds the reset threshold.
     *
     * @param emptyQueueCount    the current count of empty queue checks
     * @param maxEmptyQueueCount the maximum count of empty queue checks
     * @param resetThreshold     the threshold at which the empty queue count should be reset
     * @return the updated empty queue count
     */
    private int incrementAndResetEmptyQueueCount(
            int emptyQueueCount, int maxEmptyQueueCount, int resetThreshold) {
        emptyQueueCount++;
        if (emptyQueueCount > resetThreshold) {
            emptyQueueCount = maxEmptyQueueCount; // Reset to max to avoid wrap around
        }
        return emptyQueueCount;
    }

    /**
     * Verifies if a job state is in a cancellable state.
     *
     * @param job The job to check.
     * @return {@code true} if the job is in a cancellable state, {@code false} otherwise.
     */
    private boolean isInCancelableState(final Job job) {
        return job.state() == JobState.PENDING || job.state() == JobState.RUNNING
                || job.state() == JobState.FAILED || job.state() == JobState.ABANDONED
                || job.state() == JobState.ABANDONED_PERMANENTLY;
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
