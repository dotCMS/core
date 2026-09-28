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
import java.util.Map;
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

    @Override
    @CloseDBIfOpened
    public void process(final Job job) throws JobProcessingException {
        if (!AssetStorageFeature.isEnabled()) {
            throw new JobProcessingException(job.id(), "S3 asset storage is disabled");
        }
        final String id = (String) job.parameters().get("bundleId");
        try {
            if (!new DotConnect().setSQL("select id from publishing_bundle where id = ?")
                    .addParam(id).loadObjectResults().isEmpty()) {
                throw new JobProcessingException(job.id(), "Bundle still exists: " + id);
            }
            BundleArchiveStorage.getInstance().delete(id);
        } catch (DotDataException | RuntimeException e) {
            throw new JobProcessingException(job.id(), "Unable to remove bundle archive " + id, e);
        }
    }

    @Override
    public Map<String, Object> getResultMetadata(final Job job) {
        return Map.of("bundleId", job.parameters().get("bundleId"));
    }
}
