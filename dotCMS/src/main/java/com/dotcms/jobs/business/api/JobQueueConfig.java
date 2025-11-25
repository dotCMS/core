package com.dotcms.jobs.business.api;

/**
 * This class represents the configuration for the Job Queue system.
 */
public class JobQueueConfig {

    /**
     * The number of threads to use for job processing.
     */
    private final int threadPoolSize;

    // The interval in milliseconds to poll for job updates
    private final int pollJobUpdatesIntervalMilliseconds;

    /**
     * Constructs a new JobQueueConfig
     *
     * @param threadPoolSize The number of threads to use for job processing.
     * @param pollJobUpdatesIntervalMilliseconds The interval in milliseconds to poll for job updates.
     */
    public JobQueueConfig(int threadPoolSize, int pollJobUpdatesIntervalMilliseconds) {
        this.threadPoolSize = threadPoolSize;
        this.pollJobUpdatesIntervalMilliseconds = pollJobUpdatesIntervalMilliseconds;
    }

    /**
     * Gets the thread pool size for job processing.
     *
     * @return The number of threads to use for job processing.
     */
    public int getThreadPoolSize() {
        return threadPoolSize;
    }

    /**
     * Gets the interval in milliseconds to poll for job updates.
     *
     * @return The interval in milliseconds to poll for job updates.
     */
    public int getPollJobUpdatesIntervalMilliseconds() {
        return pollJobUpdatesIntervalMilliseconds;
    }

}