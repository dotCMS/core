package com.dotcms.jobs.business.processor;

import com.dotcms.jobs.business.error.JobCancellationException;
import com.dotcms.jobs.business.error.JobProcessingException;
import com.dotcms.jobs.business.job.Job;
import java.util.Map;

/**
 * Interface for processing jobs. Implementations of this interface should define how to process,
 * cancel, and track the progress of a job.
 */
public interface JobProcessor {

    /**
     * Processes the given job.
     *
     * @param job The job to process.
     * @throws JobProcessingException if an error occurs during processing.
     */
    void process(Job job) throws JobProcessingException;

    /**
     * Determines if the given job can be cancelled.
     *
     * @param job The job to check for cancellation capability.
     * @return true if the job can be cancelled, false otherwise.
     */
    boolean canCancel(Job job);

    /**
     * Cancels the given job.
     *
     * @param job The job to cancel.
     * @throws JobCancellationException if an error occurs during cancellation.
     */
    void cancel(Job job) throws JobCancellationException;

    /**
     * Returns metadata about the job execution. This metadata can be used to provide additional
     * information about the job's execution, such as statistics or other details useful for the
     * caller.
     *
     * @param job The job for which to provide metadata.
     * @return A map containing metadata about the job execution.
     */
    Map<String, Object> getResultMetadata(Job job);

    /**
     * Provides a progress tracker. The default implementation returns a new instance of
     * DefaultProgressTracker.
     *
     * @return A ProgressTracker instance
     */
    default ProgressTracker progressTracker() {
        return new DefaultProgressTracker();
    }

}