package com.dotcms.rest;

import com.dotcms.rest.api.v1.job.JobStatusResponse;

/**
 * This class encapsulates the {@link javax.ws.rs.core.Response} object to include the expected {@link JobStatusResponse}
 * as the entity in the response.
 */
public class ResponseEntityJobStatusView extends ResponseEntityView<JobStatusResponse> {
    public ResponseEntityJobStatusView(JobStatusResponse entity) {
        super(entity);
    }
}