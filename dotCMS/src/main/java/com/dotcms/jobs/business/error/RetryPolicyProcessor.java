package com.dotcms.jobs.business.error;

import com.dotcms.jobs.business.processor.ExponentialBackoffRetryPolicy;
import com.dotcms.jobs.business.processor.JobProcessor;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import javax.enterprise.context.ApplicationScoped;

/**
 * Processes retry policies for job processors. This class is responsible for interpreting retry
 * policy annotations on job processor classes and creating appropriate RetryStrategy instances.
 */
@ApplicationScoped
public class RetryPolicyProcessor {

    /**
     * Processes the retry policy for a given job processor class.
     * <p>
     * Currently supports ExponentialBackoffRetryPolicy.
     *
     * @param processorClass The class of the job processor to process
     * @return A RetryStrategy based on the annotation present on the processor class, or null if no
     * supported annotation is found
     */
    public RetryStrategy processRetryPolicy(Class<? extends JobProcessor> processorClass) {

        // Check for ExponentialBackoffRetryPolicy
        if (processorClass.isAnnotationPresent(ExponentialBackoffRetryPolicy.class)) {
            return processExponentialBackoffPolicy(
                    processorClass.getAnnotation(ExponentialBackoffRetryPolicy.class)
            );
        }

        // Add checks for other retry policy annotations here in the future
        // For example:
        // if (processorClass.isAnnotationPresent(SomeOtherRetryPolicy.class)) {
        //     return processSomeOtherPolicy(processorClass.getAnnotation(SomeOtherRetryPolicy.class));
        // }

        return null;
    }

    /**
     * Processes an ExponentialBackoffRetryPolicy annotation and creates an
     * ExponentialBackoffRetryStrategy based on its parameters.
     *
     * @param policy The ExponentialBackoffRetryPolicy annotation to process
     * @return An ExponentialBackoffRetryStrategy configured based on the annotation
     */
    private RetryStrategy processExponentialBackoffPolicy(ExponentialBackoffRetryPolicy policy) {

        final long initialDelay = policy.initialDelay() != -1 ? policy.initialDelay()
                : RetryStrategyProducer.DEFAULT_RETRY_STRATEGY_INITIAL_DELAY;
        final long maxDelay = policy.maxDelay() != -1 ? policy.maxDelay() :
                RetryStrategyProducer.DEFAULT_RETRY_STRATEGY_MAX_DELAY;
        final double backoffFactor = policy.backoffFactor() != -1 ? policy.backoffFactor()
                : RetryStrategyProducer.DEFAULT_RETRY_STRATEGY_BACK0FF_FACTOR;
        final int maxRetries = policy.maxRetries() != -1 ? policy.maxRetries()
                : RetryStrategyProducer.DEFAULT_RETRY_STRATEGY_MAX_RETRIES;

        Set<Class<? extends Throwable>> nonRetryableExceptions = new HashSet<>(
                Arrays.asList(policy.nonRetryableExceptions())
        );

        return new ExponentialBackoffRetryStrategy(
                initialDelay,
                maxDelay,
                backoffFactor,
                maxRetries,
                nonRetryableExceptions
        );
    }

}
