package com.dotcms.rest.api.v1.maintenance.jobs;

import com.dotcms.jobs.business.error.JobCancellationException;
import com.dotcms.jobs.business.error.JobProcessingException;
import com.dotcms.jobs.business.job.Job;
import com.dotcms.jobs.business.processor.Cancellable;
import com.dotcms.jobs.business.processor.JobProcessor;
import com.dotcms.jobs.business.processor.NoRetryPolicy;
import com.dotcms.jobs.business.processor.ProgressTracker;
import com.dotcms.jobs.business.processor.Queue;
import com.dotcms.rest.api.v1.maintenance.MaintenanceJobHelper;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.SecurityLogger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.enterprise.context.Dependent;
import org.apache.commons.io.FileUtils;

/**
 * Walks the assets directory and deletes binary folders whose contentlet inode is no
 * longer in the database. Mirrors the legacy
 * {@link com.dotmarketing.portlets.cmsmaintenance.util.CleanAssetsThread#deleteAssetsWithNoInode()}
 * walk; if you fix a bug here, fix it there too (the legacy class is still used by DWR).
 *
 * <p>Implements {@link Cancellable} so an admin can cancel a long-running cleanup via the
 * standard job-queue cancel endpoint.</p>
 *
 * @author hassandotcms
 */
@Queue(MaintenanceJobHelper.CLEAN_ASSETS_QUEUE)
@NoRetryPolicy
@Dependent
public class CleanAssetsJobProcessor implements JobProcessor, Cancellable {

    private static final char[] HEX_CHARS = {
            '0', '1', '2', '3', '4', '5', '6', '7',
            '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
    private static final String DO_NOT_DELETE_SUFFIX = ".donotdelete.dat";
    private static final String STATUS_FINISHED = "Finished";
    private static final String STATUS_CANCELED = "Canceled";

    private final AtomicBoolean cancellationRequested = new AtomicBoolean(false);
    private Map<String, Object> resultMetadata = new HashMap<>();

    @Override
    public void process(final Job job) throws JobProcessingException {

        final String userId = Objects.toString(job.parameters().get("userId"), "<unknown>");
        final String remoteAddr = Objects.toString(job.parameters().get("remoteAddr"), "<unknown>");

        SecurityLogger.logInfo(this.getClass(), String.format(
                "User '%s' running clean orphan assets (jobId=%s) from ip: %s",
                userId, job.id(), remoteAddr));
        Logger.info(this, String.format(
                "Executing clean-assets job %s for user %s", job.id(), userId));

        final String assetsPath = APILocator.getFileAssetAPI().getRealAssetsRootPath();
        final User systemUser = resolveSystemUser(job);
        final ProgressTracker progressTracker = job.progressTracker().orElseThrow(
                () -> new JobProcessingException(job.id(), "Progress tracker not found"));

        final int totalFiles = countAssetEntries(assetsPath);
        final CleanState state = cancellationRequested.get()
                ? new CleanState(0, 0, true)
                : cleanAssetEntries(assetsPath, systemUser, totalFiles, progressTracker);

        recordResult(totalFiles, state);

        // Only complete the progress bar when we finished naturally; on cancel the last
        // reported progress reflects how far we got.
        if (!state.canceled) {
            progressTracker.updateProgress(1.0f);
        }

        Logger.info(this, String.format(
                "Clean-assets job %s %s; deleted=%d totalFiles=%d",
                job.id(), state.canceled ? "canceled" : "completed",
                state.deleted, totalFiles));
    }

    @Override
    public void cancel(final Job job) throws JobCancellationException {
        Logger.info(this, "Clean-assets job cancellation requested: " + job.id());
        cancellationRequested.set(true);
    }

    @Override
    public Map<String, Object> getResultMetadata(final Job job) {
        return resultMetadata.isEmpty() ? Collections.emptyMap() : resultMetadata;
    }

    private User resolveSystemUser(final Job job) throws JobProcessingException {
        try {
            return APILocator.getUserAPI().getSystemUser();
        } catch (final DotDataException e) {
            throw new JobProcessingException(
                    job.id(), "Clean-assets failed: " + e.getMessage(), e);
        }
    }

    /**
     * Counting phase — sums the immediate children of every {@code assets/X/Y} directory.
     * Returns early (with a partial total) on cancellation.
     */
    private int countAssetEntries(final String assetsPath) {
        int total = 0;
        for (final char x : HEX_CHARS) {
            for (final char y : HEX_CHARS) {
                if (cancellationRequested.get()) {
                    return total;
                }
                final File dir = new File(
                        assetsPath + File.separator + x + File.separator + y);
                if (dir.isDirectory()) {
                    final String[] children = dir.list();
                    if (children != null) {
                        total += children.length;
                    }
                }
            }
        }
        return total;
    }

    /**
     * Cleaning phase — for each child entry under {@code assets/X/Y}, deletes the directory
     * if its name (treated as a contentlet inode) does not resolve to a contentlet with an
     * identifier. Reports progress to {@link Job#progressTracker()} only when the rounded
     * integer percentage changes, to avoid hammering the framework with millions of
     * fine-grained updates on large filesystems. Returns early on cancellation.
     */
    private CleanState cleanAssetEntries(final String assetsPath,
                                          final User systemUser,
                                          final int totalFiles,
                                          final ProgressTracker progressTracker) {
        int currentFiles = 0;
        int deleted = 0;
        int lastReportedPct = -1;

        for (final char x : HEX_CHARS) {
            for (final char y : HEX_CHARS) {
                if (cancellationRequested.get()) {
                    return new CleanState(currentFiles, deleted, true);
                }
                final File dir = new File(
                        assetsPath + File.separator + x + File.separator + y);
                if (!dir.isDirectory()) {
                    continue;
                }
                final File[] children = dir.listFiles();
                if (children == null) {
                    continue;
                }
                for (final File ff : children) {
                    if (cancellationRequested.get()) {
                        return new CleanState(currentFiles, deleted, true);
                    }
                    currentFiles++;
                    if (totalFiles > 0) {
                        final int pct = (int) (currentFiles * 100L / totalFiles);
                        if (pct != lastReportedPct) {
                            final float fraction = Math.min(1.0f, pct / 100f);
                            progressTracker.updateProgress(fraction);
                            lastReportedPct = pct;
                        }
                    }
                    if (ff.getName().endsWith(DO_NOT_DELETE_SUFFIX)) {
                        continue;
                    }
                    if (!ff.isDirectory()) {
                        continue;
                    }
                    if (deleteIfOrphan(ff, systemUser)) {
                        deleted++;
                    }
                }
            }
        }
        return new CleanState(currentFiles, deleted, false);
    }

    private boolean deleteIfOrphan(final File ff, final User systemUser) {
        final String inode = ff.getName();
        try {
            final Contentlet cont = APILocator.getContentletAPI()
                    .find(inode, systemUser, false);
            if (cont == null || !UtilMethods.isSet(cont.getIdentifier())) {
                Logger.info(this,
                        "deleting orphan binary content " + ff.getAbsolutePath());
                return FileUtils.deleteQuietly(ff);
            }
        } catch (final Exception ex) {
            Logger.warn(this, ex.getMessage(), ex);
        }
        return false;
    }

    private void recordResult(final int totalFiles, final CleanState state) {
        final Map<String, Object> metadata = new HashMap<>();
        metadata.put("totalFiles", totalFiles);
        metadata.put("currentFiles", state.currentFiles);
        metadata.put("deleted", state.deleted);
        metadata.put("finalStatus", state.canceled ? STATUS_CANCELED : STATUS_FINISHED);
        this.resultMetadata = metadata;
    }

    private static final class CleanState {
        final int currentFiles;
        final int deleted;
        final boolean canceled;

        CleanState(final int currentFiles, final int deleted, final boolean canceled) {
            this.currentFiles = currentFiles;
            this.deleted = deleted;
            this.canceled = canceled;
        }
    }
}
