package com.dotcms.jobs.business.api;

import com.dotcms.jobs.business.error.CircuitBreaker;
import com.dotcms.jobs.business.error.JobProcessorNotFoundException;
import com.dotcms.jobs.business.error.RetryStrategy;
import com.dotcms.jobs.business.job.Job;
import com.dotcms.jobs.business.job.JobPaginatedResult;
import com.dotcms.jobs.business.processor.JobProcessor;
import com.dotcms.jobs.business.queue.JobQueue;
import com.dotmarketing.exception.DotDataException;
import java.util.Map;
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
     * Registers a job processor for a specific queue.
     *
     * @param queueName The name of the queue
     * @param processor The job processor to register
     */
    void registerProcessor(String queueName, JobProcessor processor);

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
     * Retrieves a list of jobs.
     *
     * @param page     The page number
     * @param pageSize The number of jobs per page
     * @return A result object containing the list of active jobs and pagination information.
     * @throws DotDataException if there's an error fetching the jobs
     */
    JobPaginatedResult getJobs(int page, int pageSize) throws DotDataException;

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
     */
    void watchJob(String jobId, Consumer<Job> watcher);

    /**
     * Sets a retry strategy for a specific queue.
     *
     * @param queueName     The name of the queue
     * @param retryStrategy The retry strategy to set
     */
    void setRetryStrategy(String queueName, RetryStrategy retryStrategy);

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