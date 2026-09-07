package com.dotcms.jobs.business.processor.impl;

import com.dotcms.content.elasticsearch.business.ContentletIndexAPI;
import com.dotcms.jobs.business.batch.BatchFailureReason;
import com.dotcms.jobs.business.batch.BatchItemResult;
import com.dotcms.jobs.business.batch.BatchItemStatus;
import com.dotcms.jobs.business.batch.JobItemResultFactory;
import com.dotcms.jobs.business.error.JobCancellationException;
import com.dotcms.jobs.business.error.JobProcessingException;
import com.dotcms.jobs.business.job.Job;
import com.dotcms.jobs.business.processor.Cancellable;
import com.dotcms.jobs.business.processor.JobProcessor;
import com.dotcms.jobs.business.processor.ProgressTracker;
import com.dotcms.jobs.business.processor.Queue;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ContentletDependencies;
import com.dotmarketing.portlets.contentlet.model.IndexPolicy;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import java.io.File;
import java.util.Optional;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.enterprise.context.Dependent;

/**
 * Creates the assets of one bulk-upload batch (#37166).
 * <p>
 * <b>Not marked {@code @NoRetryPolicy}, deliberately.</b> The abandoned-job sweep re-queues a
 * stalled run without consulting the retry policy, so marking this no-retry would not prevent a
 * second attempt — it would only leave that attempt unprepared for one. The run is resumable
 * instead: each item's outcome is committed as it completes, and a re-queued run skips what already
 * succeeded rather than recreating it. A re-run without that would not duplicate data — the unique
 * index on the lower-cased path rejects the second create — but it would make the report lie, which
 * is worse: the author is told 30 files failed when all 30 are in the folder.
 *
 * @author dotCMS
 */
@Dependent
@Queue("assetBulkUpload")
public class BulkUploadProcessor implements JobProcessor, Cancellable {

    private final JobItemResultFactory itemResults = new JobItemResultFactory();
    private final AtomicBoolean cancellationRequested = new AtomicBoolean(false);

    @Override
    public void process(final Job job) throws JobProcessingException {

        final Map<String, Object> parameters = job.parameters();
        final User user = user(parameters);
        final List<Map<String, Object>> stagedFiles = stagedFiles(parameters);

        final ProgressTracker progressTracker = job.progressTracker().orElseThrow(
                () -> new JobProcessingException(job.id(), "Progress tracker not found"));

        // Resume: skip what a previous attempt already created rather than recreating it. Keyed by
        // seq and not by name, because a batch may legitimately contain two files of one name and
        // the two must remain distinguishable.
        final List<Integer> alreadyDone = completedSeqs(job);

        Logger.info(this, String.format(
                "Bulk upload job [%s]: %d file(s) for user [%s], %d already completed",
                job.id(), stagedFiles.size(), user.getUserId(), alreadyDone.size()));

        final List<String> createdInodes = new ArrayList<>();

        for (int seq = 0; seq < stagedFiles.size(); seq++) {

            if (alreadyDone.contains(seq)) {
                continue;
            }
            if (cancellationRequested.get()) {
                // Cancellation takes effect between files, never mid-file, so nothing is left
                // half-created. What was never reached is SKIPPED, which is a distinct outcome from
                // FAILED: those files were not rejected, they were simply not tried.
                recordRemainderAsSkipped(job, stagedFiles, seq, alreadyDone);
                break;
            }

            createOne(job, stagedFiles.get(seq), seq, user, createdInodes);
            progressTracker.updateProgress((seq + 1) / (float) stagedFiles.size());
        }

        // FR-008a — resolve index visibility ONCE for the batch, and before the completion signal.
        // Per-file WAIT_FOR would not merely block on a refresh; it also flushes the system-wide
        // query cache on every file, charging every other user for this batch. But DEFER alone only
        // enqueues into the reindex journal, so a run reporting "finished" would hand the author
        // files a text search cannot yet find. Content Drive lists folders from the database, so the
        // grid is fine either way — free-text and searchable-field criteria are what need this.
        resolveIndexVisibility(job, createdInodes);

        progressTracker.updateProgress(1.0f);
    }

    /**
     * Creates one asset and commits its outcome in the same breath.
     * <p>
     * The row is written inside the item's own transaction so the record commits with the work it
     * describes — a crash between the two would otherwise leave a file the resume path does not
     * know about, and recreate it as a collision.
     */
    private void createOne(final Job job, final Map<String, Object> file, final int seq,
                           final User user, final List<String> createdInodes) {

        final String fileName = String.valueOf(file.get("fileName"));
        final String tempFileId = String.valueOf(file.get("tempFileId"));

        try {
            final File binary = resolveStagedContent(job, tempFileId, user)
                    .orElseThrow(() -> new StagedContentUnavailableException(tempFileId));

            final Contentlet contentlet = new Contentlet();
            contentlet.setContentTypeId(contentTypeIdFor(job.parameters(), user));
            contentlet.setBinary(FileAssetAPI.BINARY_FIELD, binary);
            contentlet.setStringProperty(FileAssetAPI.TITLE_FIELD, fileName);
            contentlet.setStringProperty(FileAssetAPI.FILE_NAME_FIELD, fileName);
            applyTarget(contentlet, job.parameters(), user);

            final Contentlet created = APILocator.getWorkflowAPI().fireContentWorkflow(contentlet,
                    new ContentletDependencies.Builder()
                            .modUser(user)
                            .respectAnonymousPermissions(false)
                            // DEFER, never WAIT_FOR: the per-file wait also flushes the
                            // system-wide query cache, so a full batch would charge every other
                            // user one flush per file. The batch resolves visibility once, at the
                            // end, before the completion signal (FR-008a).
                            .indexPolicy(IndexPolicy.DEFER)
                            .indexPolicyDependencies(IndexPolicy.DEFER)
                            .build());

            createdInodes.add(created.getInode());
            record(job, seq, fileName, BatchItemStatus.SUCCESS, null, null,
                    created.getIdentifier());

        } catch (final StagedContentUnavailableException e) {
            // Not the author's fault: the content expired or could not be read. Named explicitly
            // so the copy never suggests they supplied a bad file (FR-032).
            record(job, seq, fileName, BatchItemStatus.FAILED,
                    BatchFailureReason.STAGED_CONTENT_UNAVAILABLE, e.getMessage(), null);
        } catch (final Exception e) {
            // US2 (T037) replaces this with the classified reasons — size, type, collision,
            // permission — derived from what staging measured rather than from exception text.
            record(job, seq, fileName, BatchItemStatus.FAILED,
                    BatchFailureReason.UNCLASSIFIED, e.getMessage(), null);
        }
    }

    /**
     * Retrieves the staged content <b>by the fingerprint captured at submission</b>, not through
     * the request.
     * <p>
     * <b>This is why the fingerprint is a job parameter.</b> The product's own binary-field
     * strategy resolves a temp id via {@code HttpServletRequestThreadLocal.INSTANCE.getRequest()},
     * which is null on a worker thread — a run has no HTTP request, and by the time it executes the
     * submitting one is long closed. Setting the temp id as a field value and letting that strategy
     * resolve it would therefore fail for every file in every batch. The {@code accessingList}
     * overload exists for exactly this, so the run resolves the file itself and hands the creation
     * path a real {@link File}.
     */
    private Optional<File> resolveStagedContent(final Job job, final String tempFileId,
                                                final User user) {
        final List<String> accessingList = new ArrayList<>();
        accessingList.add(user.getUserId());
        final Object fingerprint = job.parameters().get("requestFingerprint");
        if (fingerprint != null) {
            accessingList.add(String.valueOf(fingerprint));
        }
        return APILocator.getTempFileAPI().getTempFile(accessingList, tempFileId)
                .map(tempFile -> tempFile.file);
    }

    /** Raised when staged content cannot be retrieved, so it is reported as its own reason. */
    private static class StagedContentUnavailableException extends RuntimeException {
        StagedContentUnavailableException(final String tempFileId) {
            super("Staged content is no longer available: " + tempFileId);
        }
    }

    /**
     * Resolves the content type the batch creates into — one type for the whole batch, validated at
     * submission, so this never has to infer one per file.
     */
    private String contentTypeIdFor(final Map<String, Object> parameters, final User user)
            throws DotDataException, DotSecurityException {
        final String variable = "FILEASSET".equals(parameters.get("baseType"))
                ? FileAssetAPI.DEFAULT_FILE_ASSET_STRUCTURE_VELOCITY_VAR_NAME
                : "dotAsset";
        return APILocator.getContentTypeAPI(user).find(variable).id();
    }

    /** Places the asset in the folder, or at the site root when the batch targets a site. */
    private void applyTarget(final Contentlet contentlet, final Map<String, Object> parameters,
                             final User user) throws DotDataException, DotSecurityException {
        final Object folderId = parameters.get("folderId");
        if (folderId != null) {
            final Folder folder = APILocator.getFolderAPI()
                    .find(String.valueOf(folderId), user, false);
            contentlet.setHost(folder.getHostId());
            contentlet.setFolder(folder.getInode());
            return;
        }
        contentlet.setHost(String.valueOf(parameters.get("siteId")));
        contentlet.setFolder(Folder.SYSTEM_FOLDER);
    }

    /**
     * Makes every asset this run created visible to search, in one pass.
     */
    private void resolveIndexVisibility(final Job job, final List<String> createdInodes) {
        if (createdInodes.isEmpty()) {
            return;
        }
        try {
            Logger.info(this, String.format(
                    "Bulk upload job [%s]: resolving index visibility for %d asset(s)",
                    job.id(), createdInodes.size()));

            final List<Contentlet> created = APILocator.getContentletAPI()
                    .findContentlets(createdInodes);

            // One WAIT_FOR for the whole batch instead of N. This is the half that DEFER alone
            // cannot provide: DEFER only enqueues into the reindex journal, so without this a run
            // reporting "finished" would hand the author files a text search cannot yet find.
            created.forEach(contentlet -> contentlet.setIndexPolicy(IndexPolicy.WAIT_FOR));
            APILocator.getContentletIndexAPI().addContentToIndex(created);
        } catch (final Exception e) {
            Logger.error(this, String.format(
                    "Bulk upload job [%s]: could not resolve index visibility: %s",
                    job.id(), e.getMessage()), e);
        }
    }

    private void recordRemainderAsSkipped(final Job job, final List<Map<String, Object>> files,
                                          final int from, final List<Integer> alreadyDone) {
        for (int seq = from; seq < files.size(); seq++) {
            if (!alreadyDone.contains(seq)) {
                record(job, seq, String.valueOf(files.get(seq).get("fileName")),
                        BatchItemStatus.SKIPPED, null, null, null);
            }
        }
    }

    private void record(final Job job, final int seq, final String key,
                        final BatchItemStatus status, final BatchFailureReason reason,
                        final String message, final String refId) {
        try {
            itemResults.record(job.id(), seq, key, status, reason, message, refId);
        } catch (final DotDataException e) {
            Logger.error(this, String.format(
                    "Bulk upload job [%s]: could not record item %d (%s): %s",
                    job.id(), seq, key, e.getMessage()), e);
        }
    }

    private List<Integer> completedSeqs(final Job job) {
        try {
            return itemResults.findCompletedSeqs(job.id());
        } catch (final DotDataException e) {
            throw new JobProcessingException(job.id(),
                    "Could not read the run's checkpoint; refusing to restart from the first file "
                            + "and report already-created files as collisions", e);
        }
    }

    @Override
    public void cancel(final Job job) throws JobCancellationException {
        Logger.info(this, "Cancellation requested for bulk upload job " + job.id());
        cancellationRequested.set(true);
    }

    /**
     * The batch outcome, built from the durable per-item rows rather than from memory — which is
     * what lets it survive an interruption, and what bulk refresh's in-memory counters cannot do.
     */
    @Override
    public Map<String, Object> getResultMetadata(final Job job) {
        final Map<String, Object> metadata = new HashMap<>();
        try {
            final List<BatchItemResult> results = itemResults.findByJobId(job.id());
            long success = results.stream()
                    .filter(r -> r.status() == BatchItemStatus.SUCCESS).count();
            long failed = results.stream()
                    .filter(r -> r.status() == BatchItemStatus.FAILED).count();
            long skipped = results.stream()
                    .filter(r -> r.status() == BatchItemStatus.SKIPPED).count();

            metadata.put("total", results.size());
            metadata.put("processed", success + failed);
            metadata.put("successCount", success);
            metadata.put("failedCount", failed);
            metadata.put("skippedCount", skipped);
            metadata.put("results", results);
        } catch (final DotDataException e) {
            Logger.error(this, String.format(
                    "Bulk upload job [%s]: could not build the outcome: %s",
                    job.id(), e.getMessage()), e);
        }
        return metadata;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> stagedFiles(final Map<String, Object> parameters) {
        final Object files = parameters.get("stagedFiles");
        return files instanceof List ? (List<Map<String, Object>>) files : List.of();
    }

    private User user(final Map<String, Object> parameters) {
        try {
            return APILocator.getUserAPI()
                    .loadUserById(String.valueOf(parameters.get("userId")));
        } catch (final Exception e) {
            throw new IllegalStateException("Could not resolve the submitting user", e);
        }
    }
}
