package com.dotcms.storage.binary;

import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.jobs.business.error.JobProcessingException;
import com.dotcms.jobs.business.error.JobValidationException;
import com.dotcms.jobs.business.job.Job;
import com.dotcms.jobs.business.processor.ExponentialBackoffRetryPolicy;
import com.dotcms.jobs.business.processor.JobProcessor;
import com.dotcms.jobs.business.processor.Queue;
import com.dotcms.jobs.business.processor.Validator;
import com.dotcms.storage.AssetStorageFeature;
import com.dotcms.storage.FileMetadataAPI;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Logger;
import com.liferay.util.FileUtil;
import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.enterprise.context.Dependent;

/** Cleanup intent is committed with the content deletion and survives failed S3 requests/restarts. */
@Dependent
@Queue(BinaryAssetCleanupProcessor.QUEUE)
@ExponentialBackoffRetryPolicy
public class BinaryAssetCleanupProcessor implements JobProcessor, Validator {
    public static final String QUEUE = "binaryAssetCleanup";

    /**
     * Records the cleanup of a deleted inode's binaries and metadata in the caller's deletion
     * transaction. The job carries the exact binary and metadata paths stored for the inode at this
     * moment, so the worker later deletes only those. An inode can be re-created afterwards (push
     * publishing keeps the sender's inodes), and revisions it uploads must survive this cleanup.
     * With the flag off this does nothing.
     *
     * @param inode the deleted contentlet inode
     * @throws DotDataException if there is no transaction, storage cannot be listed, or the job
     *                          cannot be recorded; the caller's deletion then rolls back
     */
    public static void enqueue(final String inode) throws DotDataException {
        if (!AssetStorageFeature.isEnabled()) {
            return;
        }
        validateInode(inode);
        if (!DbConnectionFactory.inTransaction()) {
            throw new DotDataException("Binary cleanup must be recorded in the content deletion transaction");
        }
        final List<String> binaries = List.copyOf(APILocator.getBinaryAssetStorageAPI().listBinaryPaths(inode));
        final List<String> metadata = List.copyOf(APILocator.getFileMetadataAPI().listMetadataForInode(inode, binaries));
        APILocator.getJobQueueManagerAPI().createJob(QUEUE,
                Map.of("inode", inode, "binaries", binaries, "metadata", metadata));
    }

    /**
     * Registers a rollback listener that deletes a revision a check-in has just uploaded, together
     * with its revision metadata. The key carries a fresh UUID, so no other content version can
     * reference it, and an uncommitted inode is never reached by whole-inode cleanup. A failed delete
     * is logged and the object is left behind, because a rollback listener must not throw.
     * Outside a transaction no listener is registered.
     *
     * @param inode the inode the revision was uploaded under
     * @param field the binary field variable
     * @param key   the uploaded revision key, or {@code null} when the file is not a revision
     */
    public static void deleteRevisionOnRollback(final String inode, final String field, final String key) {
        if (key == null) {
            return;
        }
        HibernateUtil.addRollbackListener(() -> {
            try {
                APILocator.getFileMetadataAPI().removeMetadataPaths(inode,
                        List.of("/" + key + FileMetadataAPI.METADATA_JSON));
                APILocator.getBinaryAssetStorageAPI().deleteBinaryPaths(inode, field, List.of(key));
            } catch (Exception e) {
                Logger.warn(BinaryAssetCleanupProcessor.class,
                        "Unable to delete binary revision " + key + " after rollback: " + e.getMessage());
            }
        });
    }

    private static void validateInode(final String inode) {
        if (inode == null || !inode.matches("[A-Za-z0-9_-]{2,}")) {
            throw new IllegalArgumentException("Invalid binary cleanup inode");
        }
    }

    /**
     * Rejects submissions through the public job endpoint, which always injects {@code userId}.
     * Only content deletion records this work, through {@link #enqueue(String)}.
     *
     * @param parameters the job parameters
     * @throws JobValidationException if the flag is off or the job came from the public endpoint
     */
    @Override
    public void validate(final Map<String, Object> parameters) throws JobValidationException {
        if (!AssetStorageFeature.isEnabled() || parameters.containsKey("userId")) {
            throw new JobValidationException("Binary cleanup is internal to enabled content deletion");
        }
    }

    /**
     * Deletes the binaries and metadata recorded for a deleted inode, refusing while any content
     * version with that inode exists. Metadata goes first, then each recorded binary path, then the
     * inode's renditions and legacy image cache. Nothing outside the recorded inventory is deleted, so
     * revisions uploaded after the deletion survive. Each step tolerates paths that are already gone,
     * so a retry after a partial failure finishes the work.
     *
     * @param job the cleanup job, carrying {@code inode}, {@code binaries} and {@code metadata}
     * @throws JobProcessingException if the flag is off, the version exists, or a deletion fails;
     *                                the queue retries it
     */
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
            final List<String> binaryPaths = strings(job.parameters().get("binaries"));
            final List<String> metadataPaths = strings(job.parameters().get("metadata"));
            if (!new DotConnect().setSQL("select inode from contentlet where inode = ?")
                    .addParam(inode).loadObjectResults().isEmpty()) {
                throw new JobProcessingException(job.id(), "Content version still exists: " + inode);
            }
            final BinaryAssetStorageAPI binaries = APILocator.getBinaryAssetStorageAPI();
            APILocator.getFileMetadataAPI().removeMetadataPaths(inode, metadataPaths);
            for (final Map.Entry<String, List<String>> field : byField(inode, binaryPaths).entrySet()) {
                binaries.deleteBinaryPaths(inode, field.getKey(), field.getValue());
            }
            binaries.deleteGeneratedFiles(inode);
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

    /**
     * Groups recorded binary paths by their field folder, for the per-field delete API. Paths directly
     * under the inode folder have no field; on a local layer they are the legacy metadata files that
     * the metadata step has already deleted, so they are skipped. A path outside the inode is rejected.
     *
     * @param inode the deleted inode
     * @param paths the recorded binary paths
     * @return the paths keyed by field variable, in recorded order
     * @throws DotDataException if a path is not under the inode's folder
     */
    private static Map<String, List<String>> byField(final String inode, final List<String> paths)
            throws DotDataException {
        final String prefix = inode.charAt(0) + "/" + inode.charAt(1) + "/" + inode + "/";
        final Map<String, List<String>> fields = new LinkedHashMap<>();
        for (final String path : paths) {
            if (!path.startsWith(prefix)) {
                throw new DotDataException("Binary cleanup path escapes its inode: " + path);
            }
            final int slash = path.indexOf('/', prefix.length());
            if (slash > prefix.length()) {
                fields.computeIfAbsent(path.substring(prefix.length(), slash), field -> new ArrayList<>())
                        .add(path);
            }
        }
        return fields;
    }

    /**
     * Reads a recorded path list from the job parameters.
     *
     * @param value the parameter value
     * @return the paths
     * @throws DotDataException if the job carries no inventory, as jobs recorded before it did
     */
    private static List<String> strings(final Object value) throws DotDataException {
        if (!(value instanceof List)) {
            throw new DotDataException("Binary cleanup job has no recorded inventory");
        }
        return ((List<?>) value).stream().map(String.class::cast).toList();
    }

    @Override
    public Map<String, Object> getResultMetadata(final Job job) {
        return Map.of("inode", job.parameters().get("inode"));
    }
}
