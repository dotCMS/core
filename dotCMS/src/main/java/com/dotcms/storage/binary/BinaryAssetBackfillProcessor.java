package com.dotcms.storage.binary;

import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.jobs.business.error.JobProcessingException;
import com.dotcms.jobs.business.error.JobValidationException;
import com.dotcms.jobs.business.job.Job;
import com.dotcms.jobs.business.processor.Cancellable;
import com.dotcms.jobs.business.processor.ExponentialBackoffRetryPolicy;
import com.dotcms.jobs.business.processor.JobProcessor;
import com.dotcms.jobs.business.processor.Queue;
import com.dotcms.jobs.business.processor.Validator;
import com.dotcms.storage.AssetStorageFeature;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.enterprise.context.Dependent;

/** Admin-triggered migration using the existing job API, retries and persisted job parameters. */
@Dependent
@Queue(BinaryAssetBackfillProcessor.QUEUE)
@ExponentialBackoffRetryPolicy
public class BinaryAssetBackfillProcessor implements JobProcessor, Validator, Cancellable {
    public static final String QUEUE = "binaryAssetBackfill";
    private static final ObjectMapper JSON = new ObjectMapper();
    private final AtomicBoolean cancelled = new AtomicBoolean();
    private String afterInode = "";
    private long verifiedBinaries;
    private boolean complete;

    @Override
    @CloseDBIfOpened
    public void validate(final Map<String, Object> parameters) throws JobValidationException {
        if (!AssetStorageFeature.isEnabled()) {
            throw new JobValidationException("S3 asset storage is disabled");
        }
        try {
            final Object userId = parameters.get("userId");
            if (!(userId instanceof String) || ((String) userId).isBlank()) {
                throw new JobValidationException("An administrator userId is required");
            }
            final var user = APILocator.getUserAPI().loadUserById((String) userId);
            if (user == null || !user.isActive() || !user.isAdmin()) {
                throw new JobValidationException("Only an active administrator can backfill all assets");
            }
            final int size = batchSize(parameters);
            final String cursor = (String) parameters.getOrDefault("afterInode", "");
            if (size < 1 || size > 1000 || cursor == null || !cursor.matches("[A-Za-z0-9_-]*")) {
                throw new JobValidationException("Provide a valid inode cursor and a batch size between 1 and 1000");
            }
            if (Long.parseLong(parameters.getOrDefault("verifiedBinaries", 0).toString()) < 0) {
                throw new JobValidationException("The verified binary count cannot be negative");
            }
        } catch (JobValidationException failure) {
            throw failure;
        } catch (Exception failure) {
            throw new JobValidationException("Invalid backfill parameters or submitting user", failure);
        }
    }

    private static int batchSize(final Map<String, Object> parameters) {
        return Integer.parseInt(parameters.getOrDefault("batchSize", 250).toString());
    }

    @Override
    @CloseDBIfOpened
    public void process(final Job job) throws JobProcessingException {
        if (!AssetStorageFeature.isEnabled()) {
            throw new JobProcessingException(job.id(), "S3 asset storage is disabled");
        }
        try {
            if (DbConnectionFactory.inTransaction()) {
                throw new DotDataException("Backfill jobs require independent committed checkpoints");
            }
            // The manager may retry an older Job instance. Always reload the committed checkpoint.
            CacheLocator.getJobCache().remove(job);
            final Job current = APILocator.getJobQueueManagerAPI().getJob(job.id());
            if (current == null || !QUEUE.equals(current.queueName())) {
                throw new DotDataException("Backfill job does not exist");
            }
            validate(current.parameters());
            afterInode = (String) current.parameters().getOrDefault("afterInode", "");
            verifiedBinaries = Long.parseLong(current.parameters().getOrDefault("verifiedBinaries", 0).toString());
            complete = false;
            final int size = batchSize(current.parameters());
            while (!cancelled.get()) {
                if (Thread.currentThread().isInterrupted()) {
                    throw new DotDataException("Backfill interrupted before completion");
                }
                final var batch = BinaryAssetBackfill.runBatch(afterInode, size);
                final long verified = Math.addExact(verifiedBinaries, batch.binaries());
                final String checkpoint = JSON.writeValueAsString(Map.of(
                        "afterInode", batch.afterInode(), "verifiedBinaries", verified));
                // Only verified batches advance. A failed/uncertain save retries idempotent copies.
                // Compare the cursor so an overlapping worker cannot overwrite newer progress.
                final var saved = new DotConnect().setSQL("update job set parameters = parameters || ?::jsonb, "
                                + "updated_at = current_timestamp where id = ? and queue_name = ? "
                                + "and coalesce(parameters->>'afterInode', '') = ? returning id")
                        .addParam(checkpoint).addParam(job.id()).addParam(QUEUE).addParam(afterInode)
                        .loadObjectResults();
                if (saved.size() != 1) {
                    throw new DotDataException("Backfill checkpoint changed or job was removed");
                }
                CacheLocator.getJobCache().remove(job);
                afterInode = batch.afterInode();
                verifiedBinaries = verified;
                complete = batch.complete();
                if (complete) {
                    job.progressTracker().ifPresent(tracker -> tracker.updateProgress(1.0f));
                    return;
                }
            }
        } catch (Exception failure) {
            throw new JobProcessingException(job.id(), "Unable to backfill binary assets", failure);
        }
    }

    @Override
    public void cancel(final Job job) {
        cancelled.set(true);
    }

    @Override
    public Map<String, Object> getResultMetadata(final Job job) {
        return Map.of("afterInode", afterInode, "verifiedBinaries", verifiedBinaries, "complete", complete);
    }
}
