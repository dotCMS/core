package com.dotcms.publishing.output;

import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.jobs.business.error.JobProcessingException;
import com.dotcms.jobs.business.job.Job;
import com.dotcms.jobs.business.processor.ExponentialBackoffRetryPolicy;
import com.dotcms.jobs.business.processor.JobProcessor;
import com.dotcms.jobs.business.processor.Queue;
import com.dotcms.storage.AssetStorageFeature;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Logger;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import javax.enterprise.context.Dependent;

/** The existing transactional job queue retains archive deletion intent across storage failures. */
@Dependent
@Queue(BundleArchiveCleanupProcessor.QUEUE)
@ExponentialBackoffRetryPolicy
public class BundleArchiveCleanupProcessor implements JobProcessor {
    public static final String QUEUE = "bundleArchiveCleanup";

    public static void enqueue(final String id) throws DotDataException {
        if (!AssetStorageFeature.isEnabled()) return;
        BundleArchiveStorage.localArchive(id);
        if (!DbConnectionFactory.inTransaction()) {
            throw new DotDataException("Archive cleanup must be recorded in the bundle deletion transaction");
        }
        APILocator.getJobQueueManagerAPI().createJob(QUEUE, Map.of("bundleId", id));
    }

    /**
     * Deletes the archive of a deleted bundle. If a bundle row with the same id exists again, for
     * example because a receiver got the same bundle again after its history was deleted, the
     * archive belongs to that bundle and the job finishes without deleting anything. The row check
     * and the delete run under {@link BundleArchiveStorage#archiveLock(String)}, so an archive
     * stored on this node cannot be deleted between them.
     *
     * @param job the cleanup job, whose {@code bundleId} parameter names the bundle
     * @throws JobProcessingException if the flag is off or the archive cannot be deleted, so the
     *                                queue retries the job
     */
    @Override
    @CloseDBIfOpened
    public void process(final Job job) throws JobProcessingException {
        if (!AssetStorageFeature.isEnabled()) {
            throw new JobProcessingException(job.id(), "S3 asset storage is disabled");
        }
        final String id = (String) job.parameters().get("bundleId");
        final BundleArchiveStorage archives = BundleArchiveStorage.getInstance();
        final Lock lock = archives.archiveLock(id);
        lock.lock();
        try {
            if (!new DotConnect().setSQL("select id from publishing_bundle where id = ?")
                    .addParam(id).loadObjectResults().isEmpty()) {
                Logger.info(this, "Bundle " + id + " exists again, keeping its archive");
                return;
            }
            archives.delete(id);
        } catch (DotDataException | RuntimeException e) {
            throw new JobProcessingException(job.id(), "Unable to remove bundle archive " + id, e);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Map<String, Object> getResultMetadata(final Job job) {
        return Map.of("bundleId", job.parameters().get("bundleId"));
    }
}
