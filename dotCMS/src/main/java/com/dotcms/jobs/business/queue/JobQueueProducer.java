package com.dotcms.jobs.business.queue;

import com.dotmarketing.util.Config;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;

/**
 * This class is responsible for producing the JobQueue implementation used in the application. It
 * is application-scoped, meaning a single instance is created for the entire application.
 */
@ApplicationScoped
public class JobQueueProducer {

    // The type of job queue implementation to use
    private static final String JOB_QUEUE_IMPLEMENTATION_TYPE = Config.getStringProperty(
            "JOB_QUEUE_IMPLEMENTATION_TYPE", "postgres"
    );

    /**
     * Produces a JobQueue instance. This method is called by the CDI container to create a JobQueue
     * instance when it is needed for dependency injection.
     *
     * @return A JobQueue instance
     */
    @Produces
    @ApplicationScoped
    public JobQueue produceJobQueue() {

        if (JOB_QUEUE_IMPLEMENTATION_TYPE.equals("postgres")) {
            return new PostgresJobQueue();
        }

        throw new IllegalStateException(
                "Unknown job queue implementation type: " + JOB_QUEUE_IMPLEMENTATION_TYPE
        );
    }

}