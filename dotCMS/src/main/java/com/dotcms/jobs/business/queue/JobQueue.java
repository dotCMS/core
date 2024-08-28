package com.dotcms.jobs.business.queue;

import com.dotcms.jobs.business.job.Job;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Defines the contract for a job queue system. This interface provides methods for adding,
 * retrieving, updating, and monitoring jobs.
 */
public interface JobQueue {

    /**
     * Adds a new job to the specified queue.
     *
     * @param queueName     The name of the queue to add the job to.
     * @param parameters    The parameters for the job.
     * @return The ID of the newly created job.
     */
    String addJob(String queueName, Map<String, Object> parameters);

    /**
     * Retrieves a job by its ID.
     *
     * @param jobId The ID of the job to retrieve.
     * @return The job with the specified ID, or null if not found.
     */
    Job getJob(String jobId);

    /**
     * Retrieves a list of active jobs for a specific queue.
     *
     * @param queueName The name of the queue.
     * @param page      The page number (for pagination).
     * @param pageSize  The number of items per page.
     * @return A list of active jobs.
     */
    List<Job> getActiveJobs(String queueName, int page, int pageSize);

    /**
     * Retrieves a list of completed jobs for a specific queue within a date range.
     *
     * @param queueName The name of the queue.
     * @param startDate The start date of the range.
     * @param endDate   The end date of the range.
     * @param page      The page number (for pagination).
     * @param pageSize  The number of items per page.
     * @return A list of completed jobs.
     */
    List<Job> getCompletedJobs(String queueName, LocalDateTime startDate, LocalDateTime endDate,
            int page, int pageSize);

    /**
     * Retrieves a list of all jobs.
     *
     * @param page     The page number (for pagination).
     * @param pageSize The number of items per page.
     * @return A list of all jobs.
     */
    List<Job> getJobs(int page, int pageSize);

    /**
     * Retrieves a list of failed jobs.
     *
     * @param page     The page number (for pagination).
     * @param pageSize The number of items per page.
     * @return A list of failed jobs.
     */
    List<Job> getFailedJobs(int page, int pageSize);

    /**
     * Updates the status of a job.
     *
     * @param job The job with an updated status.
     */
    void updateJobStatus(Job job);

    /**
     * Retrieves the next pending job.
     *
     * @return The next pending job, or null if no pending jobs are available.
     */
    Job nextPendingJob();

    /**
     * Retrieves the next failed job.
     *
     * @return The next failed job, or null if no failed jobs are available.
     */
    Job nextFailedJob();

    /**
     * Updates the progress of a job.
     *
     * @param jobId    The ID of the job to update.
     * @param progress The new progress value (between 0.0 and 1.0).
     */
    void updateJobProgress(String jobId, float progress);

    /**
     * Removes a job from the queue. This method should be used for jobs that have permanently
     * failed and cannot be retried. Implementing classes should ensure that the job is completely
     * removed from the queue and any associated resources are cleaned up.
     *
     * @param jobId The ID of the job to remove.
     * @throws IllegalArgumentException if the job with the given ID does not exist.
     */
    void removeJob(String jobId);

}