package com.dotcms.storage.binary;

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
import com.liferay.util.FileUtil;
import java.io.File;
import java.util.Map;
import javax.enterprise.context.Dependent;

/** Cleanup intent is committed with the content deletion and survives failed S3 requests/restarts. */
@Dependent
@Queue(BinaryAssetCleanupProcessor.QUEUE)
@ExponentialBackoffRetryPolicy
public class BinaryAssetCleanupProcessor implements JobProcessor {
    public static final String QUEUE = "binaryAssetCleanup";

    public static void enqueue(final String inode) throws DotDataException {
        if (!AssetStorageFeature.isEnabled()) {
            return;
        }
        validateInode(inode);
        if (!DbConnectionFactory.inTransaction()) {
            throw new DotDataException("Binary cleanup must be recorded in the content deletion transaction");
        }
        APILocator.getJobQueueManagerAPI().createJob(QUEUE, Map.of("inode", inode));
    }

    private static void validateInode(final String inode) {
        if (inode == null || !inode.matches("[A-Za-z0-9_-]{2,}")) {
            throw new IllegalArgumentException("Invalid binary cleanup inode");
        }
    }

    @Override
    @CloseDBIfOpened
    public void process(final Job job) throws JobProcessingException {
        if (!AssetStorageFeature.isEnabled()) {
            // Leave a failed/retryable job rather than losing pending S3 cleanup when disabled.
            throw new JobProcessingException(job.id(), "S3 asset storage is disabled");
        }
        final String inode = (String) job.parameters().get("inode");
        validateInode(inode);
        try {
            if (!new DotConnect().setSQL("select inode from contentlet where inode = ?")
                    .addParam(inode).loadObjectResults().isEmpty()) {
                throw new JobProcessingException(job.id(), "Content version still exists: " + inode);
            }
            final BinaryAssetStorageAPI binaries = APILocator.getBinaryAssetStorageAPI();
            // Keep source keys until metadata deletion succeeds, so a retry can reconstruct every key.
            APILocator.getFileMetadataAPI().removeMetadataForInode(inode, binaries.listBinaryPaths(inode));
            binaries.deleteAllBinaries(inode);
            final File legacyCache = new File(APILocator.getFileAssetAPI().getRealAssetsRootPath(),
                    "cache/" + inode.charAt(0) + "/" + inode.charAt(1) + "/" + inode);
            FileUtil.deltree(legacyCache);
            if (legacyCache.exists()) {
                throw new DotDataException("Unable to remove legacy cache for " + inode);
            }
        } catch (DotDataException | RuntimeException e) {
            throw new JobProcessingException(job.id(), "Unable to clean binary assets for " + inode, e);
        }
    }

    @Override
    public Map<String, Object> getResultMetadata(final Job job) {
        return Map.of("inode", job.parameters().get("inode"));
    }
}
