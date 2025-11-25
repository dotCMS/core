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
     * The job is currently being canceled.
     */
    CANCELLING,

    /**
     * The job has finished executing successfully.
     */
    COMPLETED,

    /**
     * The job encountered an error and could not complete successfully.
     */
    FAILED,

    /**
     * The job was canceled before it could complete.
     */
    CANCELED
}