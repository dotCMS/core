package com.dotcms.jobs.business.api;

import com.dotcms.jobs.business.api.events.JobWatcher;
import com.dotcms.jobs.business.error.CircuitBreaker;
import com.dotcms.jobs.business.error.JobProcessorNotFoundException;
import com.dotcms.jobs.business.error.RetryStrategy;
import com.dotcms.jobs.business.job.Job;
import com.dotcms.jobs.business.job.JobPaginatedResult;
import com.dotcms.jobs.business.processor.JobProcessor;
import com.dotcms.jobs.business.queue.JobQueue;
import com.dotmarketing.exception.DotDataException;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Defines the contract for interacting with the job queue system. This interface provides methods
 * for managing jobs, processors, and the overall state of the job queue.
 */
public interface JobQueueManagerAPI {

    /**
     * Starts the job queue manager, initializing the thread pool for job processing.
     */
    void start();

    /**
     * Checks if the JobQueueManager has been started.
     *
     * @return {@code true} if the JobQueueManager has been started, {@code false} otherwise.
     */
    boolean isStarted();

    /**
     * Waits for the JobQueueManager to start up.
     *
     * @param timeout The maximum time to wait.
     * @param unit    The time unit of the timeout argument.
     * @return {@code true} if the JobQueueManager has started, {@code false} if the waiting time
     * elapsed before the JobQueueManager started.
     * @throws InterruptedException if the current thread is interrupted while waiting.
     */
    boolean awaitStart(long timeout, TimeUnit unit) throws InterruptedException;

    /**
     * Stops all job processing and releases resources. This method should be called when the job
     * queue manager is no longer needed.
     *
     * @throws Exception if an error occurs while closing the manager
     */
    void close() throws Exception;

    /**
     * Registers a job processor
     *
     * @param processor The job processor to register
     */
    void registerProcessor(Class<? extends JobProcessor> processor);

    /**
     * Registers a job processor for a specific queue.
     *
     * @param queueName The name of the queue
     * @param processor The job processor to register
     */
    void registerProcessor(String queueName, Class<? extends JobProcessor> processor);

    /**
     * Retrieves the job processors for all registered queues.
     *
     * @return A map of queue names to job processors
     */
    Map<String, Class<? extends JobProcessor>> getQueueNames();

    /**
     * Creates a new job in the specified queue.
     *
     * @param queueName  The name of the queue
     * @param parameters The parameters for the job
     * @return The ID of the created job
     * @throws JobProcessorNotFoundException if no processor is registered for the specified queue
     * @throws DotDataException              if there's an error creating the job
     */
    String createJob(String queueName, Map<String, Object> parameters)
            throws JobProcessorNotFoundException, DotDataException;

    /**
     * Retrieves a job by its ID.
     *
     * @param jobId The ID of the job
     * @return The Job object, or null if not found
     * @throws DotDataException if there's an error fetching the job
     */
    Job getJob(String jobId) throws DotDataException;

    /**
     * Retrieves a list of active jobs for a specific queue.
     *
     * @param queueName The name of the queue
     * @param page      The page number
     * @param pageSize  The number of jobs per page
     * @return A result object containing the list of active jobs and pagination information.
     * @throws DotDataException if there's an error fetching the jobs
     */
    JobPaginatedResult getActiveJobs(String queueName, int page, int pageSize)
            throws DotDataException;

    /**
     * Retrieves a list of completed jobs for a specific queue.
     *
     * @param queueName The name of the queue
     * @param page      The page number
     * @param pageSize  The number of jobs per page
     * @return A result object containing the list of completed jobs and pagination information.
     * @throws DotDataException if there's an error fetching the jobs
     */
    JobPaginatedResult getCompletedJobs(String queueName, int page, int pageSize)
            throws DotDataException;

    /**
     * Retrieves a list of canceled jobs for a specific queue.
     *
     * @param queueName The name of the queue
     * @param page      The page number
     * @param pageSize  The number of jobs per page
     * @return A result object containing the list of canceled jobs and pagination information.
     * @throws DotDataException if there's an error fetching the jobs
     */
    JobPaginatedResult getCanceledJobs(String queueName, int page, int pageSize)
            throws DotDataException;

    /**
     * Retrieves a list of failed jobs for a specific queue.
     *
     * @param queueName The name of the queue
     * @param page      The page number
     * @param pageSize  The number of jobs per page
     * @return A result object containing the list of failed jobs and pagination information.
     * @throws DotDataException if there's an error fetching the jobs
     */
    JobPaginatedResult getFailedJobs(String queueName, int page, int pageSize)
            throws DotDataException;

    /**
     * Retrieves a list of abandoned jobs for a specific queue.
     *
     * @param queueName The name of the queue
     * @param page      The page number
     * @param pageSize  The number of jobs per page
     * @return A result object containing the list of abandoned jobs and pagination information.
     * @throws DotDataException if there's an error fetching the jobs
     */
    JobPaginatedResult getAbandonedJobs(String queueName, int page, int pageSize)
            throws DotDataException;

    /**
     * Retrieves a list of successful jobs for a specific queue.
     *
     * @param queueName The name of the queue
     * @param page      The page number
     * @param pageSize  The number of jobs per page
     * @return A result object containing the list of successful jobs and pagination information.
     * @throws DotDataException if there's an error fetching the jobs
     */
    JobPaginatedResult getSuccessfulJobs(String queueName, int page, int pageSize)
            throws DotDataException;

    /**
     * Retrieves a list of all jobs for a specific queue.
     *
     * @param queueName The name of the queue
     * @param page      The page number
     * @param pageSize  The number of jobs per page
     * @return A result object containing the list of jobs and pagination information.
     * @throws DotDataException if there's an error fetching the jobs
     */
    JobPaginatedResult getJobs(String queueName, int page, int pageSize)
            throws DotDataException;

    /**
     * Retrieves a list of jobs.
     *
     * @param page     The page number
     * @param pageSize The number of jobs per page
     * @return A result object containing the list of active jobs and pagination information.
     * @throws DotDataException if there's an error fetching the jobs
     */
    JobPaginatedResult getJobs(int page, int pageSize) throws DotDataException;

    /**
     * Retrieves a list of active jobs, meaning jobs that are currently being processed.
     *
     * @param page     The page number
     * @param pageSize The number of jobs per page
     * @return A result object containing the list of active jobs and pagination information.
     * @throws DotDataException if there's an error fetching the jobs
     */
    JobPaginatedResult getActiveJobs(int page, int pageSize) throws DotDataException;

    /**
     * Retrieves a list of completed jobs
     *
     * @param page     The page number
     * @param pageSize The number of jobs per page
     * @return A result object containing the list of completed jobs and pagination information.
     * @throws DotDataException if there's an error fetching the jobs
     */
    JobPaginatedResult getCompletedJobs(int page, int pageSize) throws DotDataException;

    /**
     * Retrieves a list of successful jobs
     *
     * @param page     The page number
     * @param pageSize The number of jobs per page
     * @return A result object containing the list of successful jobs and pagination information.
     * @throws DotDataException if there's an error fetching the jobs
     */
    JobPaginatedResult getSuccessfulJobs(int page, int pageSize) throws DotDataException;

    /**
     * Retrieves a list of canceled jobs
     *
     * @param page     The page number
     * @param pageSize The number of jobs per page
     * @return A result object containing the list of canceled jobs and pagination information.
     * @throws DotDataException if there's an error fetching the jobs
     */
    JobPaginatedResult getCanceledJobs(int page, int pageSize) throws DotDataException;

    /**
     * Retrieves a list of failed jobs
     *
     * @param page     The page number
     * @param pageSize The number of jobs per page
     * @return A result object containing the list of failed jobs and pagination information.
     * @throws DotDataException if there's an error fetching the jobs
     */
    JobPaginatedResult getFailedJobs(int page, int pageSize) throws DotDataException;

    /**
     * Retrieves a list of abandoned jobs
     *
     * @param page     The page number
     * @param pageSize The number of jobs per page
     * @return A result object containing the list of abandoned jobs and pagination information.
     * @throws DotDataException if there's an error fetching the jobs
     */
    JobPaginatedResult getAbandonedJobs(int page, int pageSize) throws DotDataException;

    /**
     * Cancels a job.
     *
     * @param jobId The ID of the job to cancel
     * @throws DotDataException if there's an error cancelling the job
     */
    void cancelJob(String jobId) throws DotDataException;

    /**
     * Registers a watcher for a specific job.
     *
     * @param jobId   The ID of the job to watch
     * @param watcher The consumer to be notified of job updates
     * @return A JobWatcher instance representing the registered watcher
     */
    JobWatcher watchJob(String jobId, Consumer<Job> watcher);

    /**
     * Removes a watcher for a specific job.
     *
     * @param jobId   The ID of the job to unwatch
     * @param watcher The watcher to remove
     */
    void removeJobWatcher(String jobId, JobWatcher watcher);

    /**
     * Removes all watchers for a specific job.
     *
     * @param jobId The ID of the job
     */
    void removeAllJobWatchers(String jobId);

    /**
     * Sets a retry strategy for a specific queue.
     *
     * @param queueName     The name of the queue
     * @param retryStrategy The retry strategy to set
     */
    void setRetryStrategy(String queueName, RetryStrategy retryStrategy);

    /**
     * Retrieves the retry strategy for a specific queue.
     *
     * @param jobId The ID of the job
     * @return The processor instance, or an empty optional if not found
     */
    Optional<JobProcessor> getInstance(final String jobId);

    /**
     * @return The CircuitBreaker instance
     */
    CircuitBreaker getCircuitBreaker();

    /**
     * @return The JobQueue instance
     */
    JobQueue getJobQueue();

    /**
     * @return The size of the thread pool
     */
    int getThreadPoolSize();

    /**
     * @return The default retry strategy
     */
    RetryStrategy getDefaultRetryStrategy();

}