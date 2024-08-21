package com.dotcms.jobs.business.job;

/**
 * Represents the final result of a job execution.
 */
public enum JobResult {

    /**
     * Indicates that the job completed successfully.
     */
    SUCCESS,

    /**
     * Indicates that the job encountered an error during execution.
     */
    ERROR
}