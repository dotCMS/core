package com.dotcms.jobs.business.processor.impl;

import com.dotcms.jobs.business.job.Job;
import com.dotcms.jobs.business.processor.JobProcessor;
import java.util.Map;

// RED STUB (#37062 T025): duplicates nothing and reports nothing. Not bound to a queue yet, so the
// running server never picks it up. Replaced in T033.
public class FolderBulkDuplicateProcessor implements JobProcessor {

    @Override
    public void process(final Job job) {
        // RED STUB
    }

    @Override
    public Map<String, Object> getResultMetadata(final Job job) {
        return Map.of();
    }
}
