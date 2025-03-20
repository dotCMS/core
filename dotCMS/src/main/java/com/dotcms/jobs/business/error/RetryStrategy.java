package com.dotcms.jobs.business.error;

import com.dotcms.jobs.business.job.Job;
import java.util.Set;

/**
 * Defines the contract for retry strategies in the job processing system. Implementations of this
 * interface determine how and when failed jobs should be retried.
 */
public interface RetryStrategy {

    /**
     * Determines whether a job should be retried based on its current state and the exception that
     * caused the failure.
     *
     * @param job       The job that failed and is being considered for retry.
     * @param exceptionClass The class of the exception that caused the failure.
     * @return true if the job should be retried, false otherwise.
     */
    boolean shouldRetry(Job job, Class<? extends Throwable> exceptionClass);

    /**
     * Calculates the delay before the next retry attempt for a given job.
     *
     * @param job The job that is being retried.
     * @return The delay in milliseconds before the next retry attempt.
     */
    long nextRetryDelay(Job job);

    /**
     * Returns the maximum number of retry attempts allowed by this strategy.
     *
     * @return The maximum number of retries.
     */
    int maxRetries();

    /**
     * Determines whether a given exception is not retryable according to this strategy.
     *
     * @param exceptionClass The class of the exception to check.
     * @return true if the exception is not retryable, false otherwise.
     */
    boolean isNonRetryableException(Class<? extends Throwable> exceptionClass);

    /**
     * Adds an exception class to the set of non retryable exceptions.
     *
     * @param exceptionClass The exception class to be considered non retryable.
     */
    void addNonRetryableException(Class<? extends Throwable> exceptionClass);

    /**
     * Returns an unmodifiable set of the currently registered non retryable exceptions.
     *
     * @return An unmodifiable set of non retryable exception classes.
     */
    Set<Class<? extends Throwable>> getNonRetryableExceptions();

}