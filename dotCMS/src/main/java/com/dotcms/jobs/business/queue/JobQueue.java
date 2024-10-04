package com.dotcms.jobs.business.queue;

import com.dotcms.jobs.business.job.Job;
import com.dotcms.jobs.business.job.JobPaginatedResult;
import com.dotcms.jobs.business.job.JobState;
import com.dotcms.jobs.business.queue.error.JobLockingException;
import com.dotcms.jobs.business.queue.error.JobNotFoundException;
import com.dotcms.jobs.business.queue.error.JobQueueDataException;
import com.dotcms.jobs.business.queue.error.JobQueueException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Defines the contract for a job queue system. This interface provides methods for adding,
 * retrieving, updating, and monitoring jobs.
 */
public interface JobQueue {

    /**
     * Creates a new job in the specified queue.
     *
     * @param queueName  The name of the queue to add the job to.
     * @param parameters The parameters for the job.
     * @return The ID of the newly created job.
     * @throws JobQueueException     if there's an error creating the job
     * @throws JobQueueDataException if there's a data storage error while creating the job
     */
    String createJob(String queueName, Map<String, Object> parameters)
            throws JobQueueException;

    /**
     * Retrieves a job by its ID.
     *
     * @param jobId The ID of the job to retrieve.
     * @return The job with the specified ID.
     * @throws JobNotFoundException  if the job with the given ID is not found
     * @throws JobQueueDataException if there's a data storage error while fetching the job
     */
    Job getJob(String jobId) throws JobNotFoundException, JobQueueDataException;

    /**
     * Retrieves a list of active jobs for a specific queue.
     *
     * @param queueName The name of the queue.
     * @param page      The page number (for pagination).
     * @param pageSize  The number of items per page.
     * @return A result object containing the list of active jobs and pagination information.
     * @throws JobQueueDataException if there's a data storage error while fetching the jobs
     */
    JobPaginatedResult getActiveJobs(String queueName, int page, int pageSize)
            throws JobQueueDataException;

    /**
     * Retrieves a list of completed jobs for a specific queue within a date range.
     *
     * @param queueName The name of the queue.
     * @param startDate The start date of the range.
     * @param endDate   The end date of the range.
     * @param page      The page number (for pagination).
     * @param pageSize  The number of items per page.
     * @return A result object containing the list of active jobs and pagination information.
     * @throws JobQueueDataException if there's a data storage error while fetching the jobs
     */
    JobPaginatedResult getCompletedJobs(String queueName, LocalDateTime startDate,
            LocalDateTime endDate, int page, int pageSize) throws JobQueueDataException;

    /**
     * Retrieves a list of all jobs.
     *
     * @param page     The page number (for pagination).
     * @param pageSize The number of items per page.
     * @return A result object containing the list of jobs and pagination information.
     * @throws JobQueueDataException if there's a data storage error while fetching the jobs
     */
    JobPaginatedResult getJobs(int page, int pageSize) throws JobQueueDataException;

    /**
     * Retrieves a list of failed jobs.
     *
     * @param page     The page number (for pagination).
     * @param pageSize The number of items per page.
     * @return A result object containing the list of failed jobs and pagination information.
     * @throws JobQueueDataException if there's a data storage error while fetching the jobs
     */
    JobPaginatedResult getFailedJobs(int page, int pageSize) throws JobQueueDataException;

    /**
     * Updates the status of a job.
     *
     * @param job The job with an updated status.
     * @throws JobQueueDataException if there's a data storage error while updating the job status
     */
    void updateJobStatus(Job job) throws JobQueueDataException;

    /**
     * Retrieves updates for specific jobs since a given time.
     *
     * @param jobIds The IDs of the jobs to check for updates
     * @param since  The time from which to fetch updates
     * @return A list of updated Job objects
     * @throws JobQueueDataException if there's a data storage error while fetching job updates
     */
    List<Job> getUpdatedJobsSince(Set<String> jobIds, LocalDateTime since)
            throws JobQueueDataException;

    /**
     * Puts a job back in the queue for retry.
     *
     * @param job The job to retry.
     * @throws JobQueueDataException if there's a data storage error while re-queueing the job
     */
    void putJobBackInQueue(Job job) throws JobQueueDataException;

    /**
     * Retrieves the next job in the queue.
     *
     * @return The next job in the queue, or null if the queue is empty.
     * @throws JobQueueDataException if there's a data storage error while fetching the next job
     * @throws JobLockingException   if there's an error acquiring a lock on the next job
     */
    Job nextJob() throws JobQueueDataException, JobLockingException;

    /**
     * Updates the progress of a job.
     *
     * @param jobId    The ID of the job to update.
     * @param progress The new progress value (between 0.0 and 1.0).
     * @throws JobQueueDataException if there's a data storage error while updating the job
     *                               progress
     */
    void updateJobProgress(String jobId, float progress) throws JobQueueDataException;

    /**
     * Removes a job from the queue. This method should be used for jobs that have permanently
     * failed and cannot be retried. Implementing classes should ensure that the job is completely
     * removed from the queue and any associated resources are cleaned up.
     *
     * @param jobId The ID of the job to remove.
     * @throws JobQueueDataException if there's a data storage error while removing the job
     */
    void removeJobFromQueue(String jobId) throws JobQueueDataException;

    /**
     * Checks if a job has ever been in a specific state.
     *
     * @param jobId The ID of the job to check.
     * @param state The state to check for.
     * @return true if the job has been in the specified state, false otherwise.
     * @throws JobQueueDataException if there's an error accessing the job data.
     */
    boolean hasJobBeenInState(String jobId, JobState state) throws JobQueueDataException;

}