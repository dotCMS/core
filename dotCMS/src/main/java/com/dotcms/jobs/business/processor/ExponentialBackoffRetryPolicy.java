package com.dotcms.jobs.business.processor;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to specify an exponential backoff retry policy for job processors. This annotation
 * <p>
 * should be applied at the class level to define the retry behavior for the entire job processor.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface ExponentialBackoffRetryPolicy {

    /**
     * Specifies the maximum number of retry attempts.
     * <p>
     * If set to -1, the value will be taken from the configuration property
     * 'DEFAULT_RETRY_STRATEGY_MAX_RETRIES'.
     *
     * @return the maximum number of retries, or -1 to use the config value
     */
    int maxRetries() default -1;

    /**
     * Specifies the initial delay between retry attempts in milliseconds.
     * <p>
     * If set to -1, the value will be taken from the configuration property
     * 'DEFAULT_RETRY_STRATEGY_INITIAL_DELAY'.
     *
     * @return the initial delay in milliseconds, or -1 to use the config value
     */
    long initialDelay() default -1;

    /**
     * Specifies the maximum delay between retry attempts in milliseconds.
     * <p>
     * If set to -1, the value will be taken from the configuration property
     * 'DEFAULT_RETRY_STRATEGY_MAX_DELAY'.
     *
     * @return the maximum delay in milliseconds, or -1 to use the config value
     */
    long maxDelay() default -1;

    /**
     * Specifies the factor by which the delay increases with each retry attempt.
     * <p>
     * If set to -1, the value will be taken from the configuration property
     * 'DEFAULT_RETRY_STRATEGY_BACK0FF_FACTOR'.
     *
     * @return the backoff factor, or -1 to use the config value
     */
    double backoffFactor() default -1;

    /**
     * Specifies the exception types that should not be retried. If an empty array is provided, all
     * exceptions will be considered retryable.
     *
     * @return an array of Throwable classes representing non-retryable exceptions
     */
    Class<? extends Throwable>[] nonRetryableExceptions() default {};
}
