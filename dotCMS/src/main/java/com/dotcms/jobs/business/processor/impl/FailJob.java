package com.dotcms.jobs.business.processor.impl;

import com.dotcms.jobs.business.job.Job;
import com.dotcms.jobs.business.processor.JobProcessor;
import com.dotcms.jobs.business.processor.Queue;
import com.dotmarketing.exception.DotRuntimeException;
import java.util.Map;

@Queue("fail")
public class FailJob implements JobProcessor {

    @Override
    public void process(Job job) {

        throw new DotRuntimeException( "Failed job !");
    }

    @Override
    public Map<String, Object> getResultMetadata(Job job) {
        return Map.of();
    }

}
