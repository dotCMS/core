package com.dotcms.rest;

import com.dotcms.jobs.business.job.Job;

public class ResponseEntityJobView extends ResponseEntityView<Job> {
    public ResponseEntityJobView(Job entity) {
        super(entity);
    }
}
