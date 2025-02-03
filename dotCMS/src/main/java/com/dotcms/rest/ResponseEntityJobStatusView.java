package com.dotcms.rest;

import com.dotcms.rest.api.v1.job.JobStatusResponse;

public class ResponseEntityJobStatusView extends ResponseEntityView<JobStatusResponse> {
    public ResponseEntityJobStatusView(JobStatusResponse entity) {
        super(entity);
    }
}