package com.dotcms.jobs.business.error;

import com.dotcms.jobs.business.job.Job;
import java.util.Collections;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

/**
 * Implements an exponential backoff retry strategy. This strategy increases the delay between retry
 * attempts exponentially, and adds a small random jitter to prevent synchronized retries in
 * distributed systems.
 */
public class ExponentialBackoffRetryStrategy implements RetryStrategy {

    private final long initialDelay;
    private final long maxDelay;
    private final double backoffFactor;
    private final int maxRetries;
    private final Set<Class<? extends Throwable>> retryableExceptions;
    private final Random random = new Random();

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

    @Override
    public boolean shouldRetry(Job job, Throwable exception) {
        return job.retryCount() < maxRetries && isRetryableException(exception);
    }

    @Override
    public long nextRetryDelay(Job job) {
        long delay = (long) (initialDelay * Math.pow(backoffFactor, job.retryCount()));
        delay = Math.min(delay, maxDelay);
        // Add jitter
        delay += (long) (random.nextDouble() * delay * 0.1);
        return delay;
    }

    @Override
    public int maxRetries() {
        return maxRetries;
    }

    @Override
    public boolean isRetryableException(Throwable exception) {
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
    public void addRetryableException(Class<? extends Throwable> exceptionClass) {
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