package com.dotcms.jobs.business.error;

import com.dotcms.jobs.business.job.Job;
import java.security.SecureRandom;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Implements an exponential backoff retry strategy. This strategy increases the delay between retry
 * attempts exponentially and adds a small random jitter to prevent synchronized retries in
 * distributed systems.
 */
public class ExponentialBackoffRetryStrategy implements RetryStrategy {

    private final long initialDelay;
    private final long maxDelay;
    private final double backoffFactor;
    private final int maxRetries;
    private final Set<Class<? extends Throwable>> retryableExceptions;
    private final SecureRandom random = new SecureRandom();

    /**
     * Constructs an ExponentialBackoffRetryStrategy with the specified parameters.
     *
     * @param initialDelay  The initial delay between retries in milliseconds.
     * @param maxDelay      The maximum delay between retries in milliseconds.
     * @param backoffFactor The factor by which the delay increases with each retry.
     * @param maxRetries    The maximum number of retry attempts allowed.
     */
    public ExponentialBackoffRetryStrategy(long initialDelay, long maxDelay, double backoffFactor,
            int maxRetries) {
        this(initialDelay, maxDelay, backoffFactor, maxRetries, new HashSet<>());
    }

    /**
     * Constructs an ExponentialBackoffRetryStrategy with the specified parameters and retryable
     * exceptions.
     *
     * @param initialDelay        The initial delay between retries in milliseconds.
     * @param maxDelay            The maximum delay between retries in milliseconds.
     * @param backoffFactor       The factor by which the delay increases with each retry.
     * @param maxRetries          The maximum number of retry attempts allowed.
     * @param retryableExceptions A set of exception classes that are considered retryable.
     */
    public ExponentialBackoffRetryStrategy(long initialDelay, long maxDelay, double backoffFactor,
            int maxRetries, Set<Class<? extends Throwable>> retryableExceptions) {

        if (initialDelay <= 0 || maxDelay <= 0 || backoffFactor <= 1 || maxRetries <= 0) {
            throw new IllegalArgumentException("Invalid retry strategy parameters");
        }

        this.initialDelay = initialDelay;
        this.maxDelay = maxDelay;
        this.backoffFactor = backoffFactor;
        this.maxRetries = maxRetries;
        this.retryableExceptions = new HashSet<>(retryableExceptions);
    }

    /**
     * Determines whether a job should be retried based on the provided job and exception.
     *
     * @param job       The job in question.
     * @param exception The exception that occurred during the execution of the job.
     * @return true if the job should be retried, false otherwise.
     */
    @Override
    public boolean shouldRetry(final Job job, final Throwable exception) {
        return job.retryCount() < maxRetries && isRetryableException(exception);
    }

    /**
     * Calculates the delay for the next retry attempt for the given job based on the delay
     * parameters.
     *
     * @param job The job for which the next retry delay is calculated.
     * @return The delay for the next retry attempt in milliseconds.
     */
    @Override
    public long nextRetryDelay(final Job job) {

        long delay = (long) (initialDelay * Math.pow(backoffFactor, job.retryCount()));
        delay = Math.min(delay, maxDelay);

        // Add jitter (0-10% of delay)
        long jitter = (long) (delay * 0.1 * random.nextDouble());

        return delay + jitter;
    }

    /**
     * Returns the maximum number of retry attempts allowed by this strategy.
     *
     * @return The maximum number of retries.
     */
    @Override
    public int maxRetries() {
        return maxRetries;
    }

    /**
     * Determines whether a given exception is considered retryable according to the retry
     * strategy.
     *
     * @param exception The exception to check if it is retryable.
     * @return {@code true} if the exception is retryable, {@code false} otherwise.
     */
    @Override
    public boolean isRetryableException(final Throwable exception) {
        if (exception == null) {
            return false;
        }
        if (retryableExceptions.isEmpty()) {
            return true; // If no specific exceptions are set, all are retryable
        }
        return retryableExceptions.stream().anyMatch(clazz -> clazz.isInstance(exception));
    }

    /**
     * Adds an exception class to the set of retryable exceptions.
     *
     * @param exceptionClass The exception class to be considered retryable.
     */
    public void addRetryableException(final Class<? extends Throwable> exceptionClass) {
        retryableExceptions.add(exceptionClass);
    }

    /**
     * Returns an unmodifiable set of the currently registered retryable exceptions.
     *
     * @return An unmodifiable set of retryable exception classes.
     */
    public Set<Class<? extends Throwable>> getRetryableExceptions() {
        return Collections.unmodifiableSet(retryableExceptions);
    }

}