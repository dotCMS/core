package com.dotcms.jobs.business.queue;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;

/**
 * This class is responsible for producing the JobQueue implementation used in the application. It
 * is application-scoped, meaning a single instance is created for the entire application.
 */
@ApplicationScoped
public class JobQueueProducer {

    /**
     * Produces a JobQueue instance. This method is called by the CDI container to create a JobQueue
     * instance when it is needed for dependency injection.
     *
     * @return A JobQueue instance
     */
    @Produces
    @ApplicationScoped
    public JobQueue produceJobQueue() {

        // Potential future implementation:
        // String queueType = System.getProperty("job.queue.type", "postgres");
        // if ("postgres".equals(queueType)) {
        //     return new PostgresJobQueue();
        // } else if ("redis".equals(queueType)) {
        //     return new RedisJobQueue();
        // }
        // throw new IllegalStateException("Unknown job queue type: " + queueType);

        //return new PostgresJobQueue();
        return null;
    }

}