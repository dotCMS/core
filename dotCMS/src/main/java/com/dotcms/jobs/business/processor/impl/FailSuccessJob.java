package com.dotcms.jobs.business.processor.impl;

import com.dotcms.jobs.business.job.Job;
import com.dotcms.jobs.business.processor.JobProcessor;
import com.dotcms.jobs.business.processor.Queue;
import com.dotmarketing.exception.DotRuntimeException;
import java.util.Map;
import javax.enterprise.context.Dependent;

@Queue("failSuccess")
@Dependent
public class FailSuccessJob implements JobProcessor {

    @Override
    public void process(Job job) {
        if (job.parameters().containsKey("fail")) {
            throw new DotRuntimeException("Failed job !");
        }
    }

    @Override
    public Map<String, Object> getResultMetadata(Job job) {
        return Map.of();
    }

}
