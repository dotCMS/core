package com.dotcms.jobs.business.job;

import com.dotmarketing.business.Cachable;

/**
 * Interface for caching job objects. This interface extends the {@link Cachable} interface and
 * provides methods for adding, retrieving, and removing jobs from the cache.
 */
public interface JobCache extends Cachable {

    /**
     * Adds a job to the cache.
     *
     * @param job the job to be added to the cache
     */
    void put(Job job);

    /**
     * Adds a job state to the cache.
     *
     * @param jobId    the ID of the job state to add to the cache
     * @param jobState the state of the job to add to the cache
     */
    void putState(String jobId, JobState jobState);

    /**
     * Retrieves a job from the cache by its ID.
     *
     * @param jobId the ID of the job to be retrieved
     * @return the job associated with the given ID, or null if no such job exists in the cache
     */
    Job get(String jobId);

    /**
     * Retrieves the state of a job from the cache by its ID.
     *
     * @param jobId the ID of the job whose state is to be retrieved
     * @return the state of the job associated with the given ID, or null if no such state exists in
     * the cache
     */
    JobState getState(String jobId);

    /**
     * Removes a job from the cache.
     *
     * @param job the job to be removed from the cache
     */
    void remove(Job job);

    /**
     * Removes a job from the cache by its ID.
     *
     * @param jobId the ID of the job to be removed
     */
    void remove(String jobId);

    /**
     * Removes a job state from the cache by its ID.
     *
     * @param jobId the ID of the job state to be removed
     */
    void removeState(final String jobId);

}
