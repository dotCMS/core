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
    SUCCESS,

    /**
     * The job encountered an error and could not complete successfully.
     */
    FAILED,

    /**
     * The job encountered error and could not complete successfully. The error is permanent and the
     * job will not be retried.
     */
    FAILED_PERMANENTLY,

    /**
     * The job was abandoned before it could complete.
     */
    ABANDONED,

    /**
     * The job was abandoned before it could complete. The error is permanent and the job will not
     * be retried.
     */
    ABANDONED_PERMANENTLY,

    /**
     * The job is waiting to be canceled.
     */
    CANCEL_REQUESTED,

    /**
     * The job is currently being canceled.
     */
    CANCELLING,

    /**
     * The job was canceled before it could complete.
     */
    CANCELED
}