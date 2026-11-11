package com.dotcms.jobs.business.job;

import java.util.Arrays;

/**
 * The job listings the job queue offers, each defined by the job states it includes. {@link #ALL}
 * includes every job regardless of its state.
 */
public enum JobListView {

    ALL(),
    ACTIVE(JobState.PENDING, JobState.RUNNING, JobState.FAILED, JobState.ABANDONED,
            JobState.CANCEL_REQUESTED, JobState.CANCELLING),
    COMPLETED(JobState.SUCCESS, JobState.CANCELED, JobState.ABANDONED_PERMANENTLY,
            JobState.FAILED_PERMANENTLY),
    SUCCESSFUL(JobState.SUCCESS),
    CANCELED(JobState.CANCEL_REQUESTED, JobState.CANCELLING, JobState.CANCELED),
    FAILED(JobState.FAILED, JobState.FAILED_PERMANENTLY),
    ABANDONED(JobState.ABANDONED, JobState.ABANDONED_PERMANENTLY);

    private final JobState[] states;

    JobListView(final JobState... states) {
        this.states = states;
    }

    /**
     * The job states this listing includes.
     *
     * @return a copy of the states; empty for {@link #ALL}, which does not filter by state
     */
    public JobState[] states() {
        return Arrays.copyOf(states, states.length);
    }
}
