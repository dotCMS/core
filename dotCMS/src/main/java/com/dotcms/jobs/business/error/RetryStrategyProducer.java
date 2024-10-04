package com.dotcms.jobs.business.error;

import com.dotmarketing.util.Config;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;

/**
 * This class is responsible for producing the default RetryStrategy used in the application. It is
 * application-scoped, meaning a single instance is created for the entire application.
 */
@ApplicationScoped
public class RetryStrategyProducer {

    // The initial delay between retries in milliseconds
    static final int DEFAULT_RETRY_STRATEGY_INITIAL_DELAY = Config.getIntProperty(
            "DEFAULT_RETRY_STRATEGY_INITIAL_DELAY", 1000
    );

    // The maximum delay between retries in milliseconds
    static final int DEFAULT_RETRY_STRATEGY_MAX_DELAY = Config.getIntProperty(
            "DEFAULT_RETRY_STRATEGY_MAX_DELAY", 60000
    );

    // The factor by which the delay increases with each retry
    static final float DEFAULT_RETRY_STRATEGY_BACK0FF_FACTOR = Config.getFloatProperty(
            "DEFAULT_RETRY_STRATEGY_BACK0FF_FACTOR", 2.0f
    );

    // The maximum number of retry attempts allowed
    static final int DEFAULT_RETRY_STRATEGY_MAX_RETRIES = Config.getIntProperty(
            "DEFAULT_RETRY_STRATEGY_MAX_RETRIES", 3
    );

    /**
     * Produces a RetryStrategy instance. This method is called by the CDI container to create a
     * RetryStrategy instance when it is needed for dependency injection.
     *
     * @return An ExponentialBackoffRetryStrategy instance configured with the default values.
     */
    @Produces
    public RetryStrategy produceDefaultRetryStrategy() {
        return new ExponentialBackoffRetryStrategy(
                DEFAULT_RETRY_STRATEGY_INITIAL_DELAY,
                DEFAULT_RETRY_STRATEGY_MAX_DELAY,
                DEFAULT_RETRY_STRATEGY_BACK0FF_FACTOR,
                DEFAULT_RETRY_STRATEGY_MAX_RETRIES
        );
    }
}