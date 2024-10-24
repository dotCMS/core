package com.dotcms.jobs.business.error;

import com.dotcms.jobs.business.job.Job;
import java.util.Collections;
import java.util.Set;
import javax.enterprise.context.ApplicationScoped;

/**
 * Implements a no-retry strategy for job processors that should never retry failed jobs. This
 * strategy always returns false for shouldRetry and maintains an empty set of non-retryable
 * exceptions since retries are never attempted.
 */
@ApplicationScoped
public class NoRetryStrategy implements RetryStrategy {

    @Override
    public boolean shouldRetry(Job job, Class<? extends Throwable> exceptionClass) {
        return false; // Never retry
    }

    @Override
    public long nextRetryDelay(Job job) {
        return 0; // Not used since retries never occur
    }

    @Override
    public int maxRetries() {
        return 0;
    }

    @Override
    public boolean isNonRetryableException(Class<? extends Throwable> exceptionClass) {
        return true; // All exceptions are considered non-retryable
    }

    @Override
    public void addNonRetryableException(Class<? extends Throwable> exceptionClass) {
        // No-op since all exceptions are already non-retryable
    }

    @Override
    public Set<Class<? extends Throwable>> getNonRetryableExceptions() {
        return Collections.emptySet(); // No need to track specific exceptions
    }

}
