package com.dotcms.jobs.business.processor.impl;

import com.dotcms.jobs.business.batch.BatchFailureReason;
import com.dotcms.jobs.business.batch.BatchItemResult;
import com.dotcms.jobs.business.batch.BatchItemStatus;
import com.dotcms.jobs.business.error.JobCancellationException;
import com.dotcms.jobs.business.error.JobProcessingException;
import com.dotcms.jobs.business.job.Job;
import com.dotcms.jobs.business.processor.Cancellable;
import com.dotcms.jobs.business.processor.JobProcessor;
import com.dotcms.jobs.business.processor.Queue;
import com.dotcms.rest.api.v1.asset.bulkduplicate.FolderBulkDuplicateHelper;
import com.dotcms.rest.api.v1.asset.WebAssetHelper;
import com.dotcms.rest.api.v1.asset.view.FolderView;
import com.dotcms.rest.api.v1.asset.view.WebAssetView;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import javax.enterprise.context.Dependent;

/**
 * Runs a bulk folder duplication: each submitted folder is duplicated in place, one at a time, in
 * its own transaction (#37062).
 * <p>
 * One folder failing never stops the run: its failure is recorded against its submitted path and
 * the next folder is attempted, so every submitted folder ends with exactly one outcome record.
 * Progress is reported once per completed folder, the finest step that can be observed.
 * <p>
 * The finer failure reasons, the rule that skips a folder inside another selected folder, retry,
 * heartbeat and notification arrive with their own stories.
 *
 * @author dotCMS
 */
@Dependent
@Queue(FolderBulkDuplicateHelper.QUEUE_NAME)
public class FolderBulkDuplicateProcessor implements JobProcessor, Cancellable {

    private final AtomicInteger successCount = new AtomicInteger();
    private final AtomicInteger failedCount = new AtomicInteger();
    private final AtomicInteger skippedCount = new AtomicInteger();
    private final AtomicInteger processedCount = new AtomicInteger();
    private final AtomicBoolean cancellationRequested = new AtomicBoolean();
    private final List<BatchItemResult> results = new CopyOnWriteArrayList<>();
    private volatile int total;

    /**
     * Duplicates every submitted folder, in submission order.
     *
     * @param job the run, carrying {@code assetPaths} and {@code userId}
     */
    @Override
    public void process(final Job job) {
        final List<String> paths = FolderBulkDuplicateHelper.pathsOf(job.parameters());
        this.total = paths.size();
        final User user = user(job.parameters());
        final WebAssetHelper webAssetHelper = WebAssetHelper.newInstance();
        final FolderDuplicator duplicator = new FolderDuplicator();

        Logger.info(this, String.format(
                "Bulk folder duplicate job [%s]: duplicating %d path(s) for user [%s]",
                job.id(), paths.size(), user.getUserId()));

        for (final String path : paths) {
            duplicateOne(webAssetHelper, duplicator, path, user);

            final int completed = this.processedCount.incrementAndGet();
            job.progressTracker().ifPresent(
                    tracker -> tracker.updateProgress(completed / (float) this.total));
        }

        Logger.info(this, String.format(
                "Bulk folder duplicate job [%s] finished: %d succeeded, %d failed, %d skipped",
                job.id(), this.successCount.get(), this.failedCount.get(),
                this.skippedCount.get()));
    }

    /**
     * Resolves and duplicates one folder, recording its outcome. Any failure is caught here: it is
     * this folder's outcome, and failing the run over it would discard every folder already
     * duplicated.
     */
    private void duplicateOne(final WebAssetHelper webAssetHelper,
            final FolderDuplicator duplicator, final String path, final User user) {
        try {
            final Folder source = resolve(webAssetHelper, path, user);
            duplicator.duplicate(source, user);
            record(path, BatchItemStatus.SUCCESS, null, null);
        } catch (final Exception e) {
            Logger.warn(this, String.format("Unable to duplicate folder [%s]: %s",
                    path, e.getMessage()), e);
            record(path, BatchItemStatus.FAILED, BatchFailureReason.UNCLASSIFIED, e.getMessage());
        }
    }

    /**
     * Resolves a site-qualified path to the folder it names, the way the shipped single-folder
     * actions do.
     *
     * @throws IllegalArgumentException the path resolves, but not to a folder
     */
    private Folder resolve(final WebAssetHelper webAssetHelper, final String path,
            final User user) throws Exception {
        final WebAssetView assetInfo = webAssetHelper.getAssetInfo(path, user);
        if (!(assetInfo instanceof FolderView)) {
            throw new IllegalArgumentException("The path [" + path + "] is not a folder");
        }
        final Folder folder = APILocator.getFolderAPI()
                .find(((FolderView) assetInfo).inode(), user, false);
        if (folder == null || !UtilMethods.isSet(folder.getInode())) {
            throw new IllegalArgumentException("The path [" + path + "] is not a folder");
        }
        return folder;
    }

    private void record(final String path, final BatchItemStatus status,
            final BatchFailureReason reason, final String message) {
        final BatchItemResult.Builder builder = BatchItemResult.builder().key(path).status(status);
        if (reason != null) {
            builder.reason(reason);
        }
        if (message != null) {
            builder.message(message);
        }
        this.results.add(builder.build());

        if (status == BatchItemStatus.SUCCESS) {
            this.successCount.incrementAndGet();
        } else if (status == BatchItemStatus.FAILED) {
            this.failedCount.incrementAndGet();
        } else {
            this.skippedCount.incrementAndGet();
        }
    }

    /**
     * Records that cancellation was asked for. Honouring it arrives with its own story.
     *
     * @param job the run
     * @throws JobCancellationException never
     */
    @Override
    public void cancel(final Job job) throws JobCancellationException {
        Logger.info(this, "Cancellation requested for bulk folder duplicate job " + job.id());
        this.cancellationRequested.set(true);
    }

    /**
     * The run's outcome: its counts and one record per submitted folder.
     *
     * @param job the run
     * @return {@code total}, {@code processed}, the three counts, and {@code results}
     */
    @Override
    public Map<String, Object> getResultMetadata(final Job job) {
        final Map<String, Object> metadata = new HashMap<>();
        metadata.put("total", this.total);
        metadata.put("processed", this.processedCount.get());
        metadata.put("successCount", this.successCount.get());
        metadata.put("failedCount", this.failedCount.get());
        metadata.put("skippedCount", this.skippedCount.get());
        metadata.put("results", List.copyOf(this.results));
        return metadata;
    }

    private User user(final Map<String, Object> parameters) {
        final String userId = String.valueOf(parameters.get("userId"));
        try {
            return APILocator.getUserAPI().loadUserById(userId);
        } catch (final Exception e) {
            throw new JobProcessingException("Unable to load the submitting user " + userId, e);
        }
    }
}
