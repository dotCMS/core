package com.dotcms.jobs.business.api;

/**
 * This class represents the configuration for the Job Queue system.
 */
public class JobQueueConfig {

    /**
     * The number of threads to use for job processing.
     */
    private final int threadPoolSize;

    /**
     * Constructs a new JobQueueConfig
     *
     * @param threadPoolSize The number of threads to use for job processing.
     */
    public JobQueueConfig(int threadPoolSize) {
        this.threadPoolSize = threadPoolSize;
    }

    /**
     * Gets the thread pool size for job processing.
     *
     * @return The number of threads to use for job processing.
     */
    public int getThreadPoolSize() {
        return threadPoolSize;
    }

}