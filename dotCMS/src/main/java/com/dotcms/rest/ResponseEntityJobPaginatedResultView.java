package com.dotcms.rest;

import com.dotcms.jobs.business.job.JobPaginatedResult;

public class ResponseEntityJobPaginatedResultView extends ResponseEntityView<JobPaginatedResult> {
    public ResponseEntityJobPaginatedResultView(JobPaginatedResult entity) {
        super(entity);
    }
}
