package com.dotcms.rest.api.v1.maintenance.jobs;

import com.dotcms.jobs.business.error.JobProcessingException;
import com.dotcms.jobs.business.job.Job;
import com.dotcms.jobs.business.processor.JobProcessor;
import com.dotcms.jobs.business.processor.NoRetryPolicy;
import com.dotcms.jobs.business.processor.Queue;
import com.dotcms.rest.api.v1.maintenance.MaintenanceJobHelper;
import com.dotmarketing.portlets.cmsmaintenance.util.CleanAssetsThread;
import com.dotmarketing.portlets.cmsmaintenance.util.CleanAssetsThread.BasicProcessStatus;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.SecurityLogger;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import javax.enterprise.context.Dependent;

/**
 * Job processor that runs the {@link CleanAssetsThread} walk under the
 * {@link com.dotcms.jobs.business.api.JobQueueManagerAPI} framework.
 *
 * <p>Legacy {@code CleanAssetsThread} is invoked via {@link Thread#run()} — which is already
 * {@code public} on the {@code Thread} class — executing the walk synchronously on the job
 * queue's worker thread. No new threads are spawned and no legacy code is modified. DWR
 * callers, which still invoke {@link CleanAssetsThread#start()}, are unaffected.</p>
 *
 * <p>A lightweight scheduled reporter bridges {@link BasicProcessStatus} progress (updated
 * inside the walk) into the job's {@code progressTracker} so the standard job-status
 * endpoint can surface progress.</p>
 *
 * @author hassandotcms
 */
@Queue(MaintenanceJobHelper.CLEAN_ASSETS_QUEUE)
@NoRetryPolicy
@Dependent
public class CleanAssetsJobProcessor implements JobProcessor {

    private Map<String, Object> resultMetadata = new HashMap<>();

    @Override
    public void process(final Job job) throws JobProcessingException {

        final String userId = String.valueOf(job.parameters().get("userId"));
        final String remoteAddr = String.valueOf(job.parameters().get("remoteAddr"));

        SecurityLogger.logInfo(this.getClass(), String.format(
                "User '%s' running clean orphan assets (jobId=%s) from ip: %s",
                userId, job.id(), remoteAddr));
        Logger.info(this, String.format(
                "Executing clean-assets job %s for user %s", job.id(), userId));

        final CleanAssetsThread worker = CleanAssetsThread.getInstance(true, true);
        final BasicProcessStatus status = worker.getProcessStatus();

        final ScheduledExecutorService reporter =
                Executors.newSingleThreadScheduledExecutor(r -> {
                    final Thread t = new Thread(r, "clean-assets-progress-" + job.id());
                    t.setDaemon(true);
                    return t;
                });
        final ScheduledFuture<?> reporterTask = reporter.scheduleAtFixedRate(() -> {
            final int total = status.getTotalFiles();
            if (total > 0) {
                final float pct = Math.min(
                        1.0f, Math.max(0.0f, status.getCurrentFiles() / (float) total));
                job.progressTracker().ifPresent(tracker -> tracker.updateProgress(pct));
            }
        }, 1, 1, TimeUnit.SECONDS);

        try {
            // Synchronous invocation of run() on THIS thread (the job-queue worker),
            // NOT Thread.start(). Executes deleteAssetsWithNoInode() in-line.
            worker.run();
        } catch (final Exception e) {
            Logger.error(this, "Clean-assets job " + job.id() + " failed", e);
            throw new JobProcessingException(
                    job.id(), "Clean-assets failed: " + e.getMessage(), e);
        } finally {
            reporterTask.cancel(false);
            reporter.shutdown();
        }

        final Map<String, Object> metadata = new HashMap<>();
        metadata.put("totalFiles", status.getTotalFiles());
        metadata.put("currentFiles", status.getCurrentFiles());
        metadata.put("deleted", status.getDeleted());
        metadata.put("finalStatus", status.getStatus());
        this.resultMetadata = metadata;

        job.progressTracker().ifPresent(tracker -> tracker.updateProgress(1.0f));

        Logger.info(this, String.format(
                "Clean-assets job %s completed; deleted=%d totalFiles=%d finalStatus=%s",
                job.id(), status.getDeleted(), status.getTotalFiles(), status.getStatus()));
    }

    @Override
    public Map<String, Object> getResultMetadata(final Job job) {
        return resultMetadata.isEmpty() ? Collections.emptyMap() : resultMetadata;
    }
}
