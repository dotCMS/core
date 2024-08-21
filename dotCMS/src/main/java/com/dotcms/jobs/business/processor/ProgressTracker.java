package com.dotcms.jobs.business.processor;

/**
 * Interface for tracking the progress of a job. Implementations of this interface should provide
 * mechanisms to update and retrieve the current progress of a job.
 */
public interface ProgressTracker {

    /**
     * Updates the progress of the job.
     *
     * @param progress A float value between 0.0 and 1.0, inclusive, representing the job's
     *                 progress.
     * @throws IllegalArgumentException if progress is not between 0.0 and 1.0, inclusive.
     */
    void updateProgress(float progress);

    /**
     * Retrieves the current progress of the job.
     *
     * @return A float value between 0.0 and 1.0, inclusive, representing the job's current
     * progress.
     */
    float progress();
}