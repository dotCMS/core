package com.dotcms.rest.api.v1.scheduler;

import com.dotcms.rest.ResponseEntityView;

/**
 * Wraps a saved {@link SchedulerJobForm} for {@link SchedulerJobResource}'s responses.
 */
public class ResponseEntitySchedulerJobView extends ResponseEntityView<SchedulerJobForm> {

    /**
     * @param entity the saved job
     */
    public ResponseEntitySchedulerJobView(final SchedulerJobForm entity) {
        super(entity);
    }
}
