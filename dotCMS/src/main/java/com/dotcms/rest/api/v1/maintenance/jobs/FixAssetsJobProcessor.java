package com.dotcms.rest.api.v1.maintenance.jobs;

import com.dotcms.jobs.business.error.JobProcessingException;
import com.dotcms.jobs.business.job.Job;
import com.dotcms.jobs.business.processor.JobProcessor;
import com.dotcms.jobs.business.processor.NoRetryPolicy;
import com.dotcms.jobs.business.processor.ProgressTracker;
import com.dotcms.jobs.business.processor.Queue;
import com.dotcms.rest.api.v1.maintenance.MaintenanceJobHelper;
import com.dotmarketing.fixtask.FixTasksExecutor;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.SecurityLogger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.enterprise.context.Dependent;

/**
 * Job processor that runs {@link FixTasksExecutor#execute(org.quartz.JobExecutionContext)}
 * under the {@link com.dotcms.jobs.business.api.JobQueueManagerAPI} framework.
 *
 * <p>Legacy behavior is preserved: the processor calls the existing singleton executor,
 * so DWR callers, Quartz-triggered runs, and any other invocation paths continue to work
 * unchanged. Only the REST entry point is migrated to the job queue.</p>
 *
 * <p>Single-in-flight semantics are enforced at job-creation time by
 * {@link MaintenanceJobHelper}; the queue itself guarantees single-node execution.</p>
 *
 * @author hassandotcms
 */
@Queue(MaintenanceJobHelper.FIX_ASSETS_QUEUE)
@NoRetryPolicy
@Dependent
@SuppressWarnings("rawtypes")
public class FixAssetsJobProcessor implements JobProcessor {

    private Map<String, Object> resultMetadata = new HashMap<>();

    @Override
    public void process(final Job job) throws JobProcessingException {

        final String userId = String.valueOf(job.parameters().get("userId"));
        final String remoteAddr = String.valueOf(job.parameters().get("remoteAddr"));

        SecurityLogger.logInfo(this.getClass(), String.format(
                "User '%s' running fix assets inconsistencies (jobId=%s) from ip: %s",
                userId, job.id(), remoteAddr));
        Logger.info(this, String.format(
                "Executing fix-assets job %s for user %s", job.id(), userId));

        final ProgressTracker progressTracker = job.progressTracker().orElseThrow(
                () -> new JobProcessingException(job.id(), "Progress tracker not found"));

        try {
            final FixTasksExecutor executor = FixTasksExecutor.getInstance();
            executor.execute(null);

            final List<Map> results = new ArrayList<>(executor.getTasksresults());
            final Map<String, Object> metadata = new HashMap<>();
            metadata.put("tasksRun", results.size());
            metadata.put("results", results);
            this.resultMetadata = metadata;

            progressTracker.updateProgress(1.0f);

            Logger.info(this, String.format(
                    "Fix-assets job %s completed; %d task(s) produced results",
                    job.id(), results.size()));
        } catch (final Exception e) {
            Logger.error(this, "Fix-assets job " + job.id() + " failed", e);
            throw new JobProcessingException(
                    job.id(), "Fix-assets failed: " + e.getMessage(), e);
        }
    }

    @Override
    public Map<String, Object> getResultMetadata(final Job job) {
        return resultMetadata.isEmpty() ? Collections.emptyMap() : resultMetadata;
    }
}
