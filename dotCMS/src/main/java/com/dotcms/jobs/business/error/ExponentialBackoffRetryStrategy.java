package com.dotcms.jobs.business.error;

import com.dotcms.jobs.business.job.Job;
import java.security.SecureRandom;
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
    private final Set<Class<? extends Throwable>> nonRetryableExceptions;
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
     * @param initialDelay           The initial delay between retries in milliseconds.
     * @param maxDelay               The maximum delay between retries in milliseconds.
     * @param backoffFactor          The factor by which the delay increases with each retry.
     * @param maxRetries             The maximum number of retry attempts allowed.
     * @param nonRetryableExceptions A set of exception classes that are considered non retryable.
     */
    public ExponentialBackoffRetryStrategy(long initialDelay, long maxDelay, double backoffFactor,
            int maxRetries, Set<Class<? extends Throwable>> nonRetryableExceptions) {

        if (initialDelay <= 0 || maxDelay <= 0 || backoffFactor <= 1) {
            throw new IllegalArgumentException("Invalid retry strategy parameters");
        }

        this.initialDelay = initialDelay;
        this.maxDelay = maxDelay;
        this.backoffFactor = backoffFactor;
        this.maxRetries = maxRetries;
        this.nonRetryableExceptions = new HashSet<>(nonRetryableExceptions);
    }

    /**
     * Determines whether a job should be retried based on the provided job and exception.
     *
     * @param job       The job in question.
     * @param exceptionClass The class of the exception that caused the failure.
     * @return true if the job should be retried, false otherwise.
     */
    @Override
    public boolean shouldRetry(final Job job, final Class<? extends Throwable> exceptionClass) {
        return job.retryCount() < maxRetries && !isNonRetryableException(exceptionClass);
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

    @Override
    public int maxRetries() {
        return maxRetries;
    }

    @Override
    public boolean isNonRetryableException(final Class<? extends Throwable> exceptionClass) {
        if (exceptionClass == null) {
            return false;
        }
        return nonRetryableExceptions.stream()
                .anyMatch(clazz -> clazz.isAssignableFrom(exceptionClass));
    }

    @Override
    public void addNonRetryableException(final Class<? extends Throwable> exceptionClass) {
        nonRetryableExceptions.add(exceptionClass);
    }

    @Override
    public Set<Class<? extends Throwable>> getNonRetryableExceptions() {
        return Set.copyOf(nonRetryableExceptions);
    }

}