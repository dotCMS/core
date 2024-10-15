package com.dotcms.jobs.business.api;

import com.dotmarketing.util.Config;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;

/**
 * This class is responsible for producing the configuration for the Job Queue system. It is
 * application-scoped, meaning a single instance is created for the entire application.
 */
@ApplicationScoped
public class JobQueueConfigProducer {

    // The number of threads to use for job processing.
    static final int DEFAULT_THREAD_POOL_SIZE = Config.getIntProperty(
            "JOB_QUEUE_THREAD_POOL_SIZE", 10
    );

    // The interval in milliseconds to poll for job updates.
    static final int DEFAULT_POLL_JOB_UPDATES_INTERVAL_MILLISECONDS = Config.getIntProperty(
            "JOB_QUEUE_POLL_JOB_UPDATES_INTERVAL_MILLISECONDS", 1000
    );

    /**
     * Produces a JobQueueConfig object. This method is called by the CDI container to create a
     * JobQueueConfig instance when it is necessary for dependency injection.
     *
     * @return A new JobQueueConfig instance
     */
    @Produces
    public JobQueueConfig produceJobQueueConfig() {
        return new JobQueueConfig(
                DEFAULT_THREAD_POOL_SIZE,
                DEFAULT_POLL_JOB_UPDATES_INTERVAL_MILLISECONDS
        );
    }

}