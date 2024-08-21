package com.dotcms.jobs.business.job;

/**
 * Represents the various states a job can be in during its lifecycle.
 */
public enum JobState {

    /**
     * The job is waiting to be processed.
     */
    PENDING,

    /**
     * The job is currently being executed.
     */
    RUNNING,

    /**
     * The job has finished executing successfully.
     */
    COMPLETED,

    /**
     * The job encountered an error and could not complete successfully.
     */
    FAILED,

    /**
     * The job was cancelled before it could complete.
     */
    CANCELLED
}