package com.dotcms.jobs.business.error;

import com.dotcms.jobs.business.processor.ExponentialBackoffRetryPolicy;
import com.dotcms.jobs.business.processor.JobProcessor;
import com.dotcms.jobs.business.processor.NoRetryPolicy;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

/**
 * Processes retry policies for job processors. This class is responsible for interpreting retry
 * policy annotations on job processor classes and creating appropriate RetryStrategy instances.
 */
@ApplicationScoped
public class RetryPolicyProcessor {

    private NoRetryStrategy noRetryStrategy;

    /**
     * Default constructor required for CDI proxy creation.
     */
    public RetryPolicyProcessor() {
        // Default constructor for CDI
    }

    @Inject
    public RetryPolicyProcessor(NoRetryStrategy noRetryStrategy) {
        this.noRetryStrategy = noRetryStrategy;
    }

    /**
     * Processes the retry policy for a given job processor class.
     * <p>
     * Currently supports ExponentialBackoffRetryPolicy and NoRetryPolicy.
     *
     * @param processorClass The class of the job processor to process.
     * @return A RetryStrategy based on the annotation present on the processor class, or null if no
     * supported annotation is found.
     *
     * @see ExponentialBackoffRetryPolicy
     * @see NoRetryPolicy
     */
    public RetryStrategy processRetryPolicy(Class<? extends JobProcessor> processorClass) {

        // Check for NoRetryPolicy
        if (processorClass.isAnnotationPresent(NoRetryPolicy.class)) {
            return noRetryStrategy;
        }

        // Check for ExponentialBackoffRetryPolicy
        if (processorClass.isAnnotationPresent(ExponentialBackoffRetryPolicy.class)) {
            return processExponentialBackoffPolicy(
                    processorClass.getAnnotation(ExponentialBackoffRetryPolicy.class)
            );
        }

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
