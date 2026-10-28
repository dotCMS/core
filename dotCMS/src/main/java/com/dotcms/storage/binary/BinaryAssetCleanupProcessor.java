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
import com.dotmarketing.db.LocalTransaction;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Logger;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.liferay.util.FileUtil;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import javax.enterprise.context.Dependent;

/** Cleanup intent is committed with the content deletion and survives failed S3 requests/restarts. */
@Dependent
@Queue(BinaryAssetCleanupProcessor.QUEUE)
@ExponentialBackoffRetryPolicy
public class BinaryAssetCleanupProcessor implements JobProcessor, Validator {
    public static final String QUEUE = "binaryAssetCleanup";
    private static final String REVISIONS = "/.revisions/";
    private static final ObjectMapper JSON = new ObjectMapper();

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
        requireTransaction();
        enqueue(inode, storedPaths(inode));
    }

    /**
     * Records the cleanup of a deleted inode with paths listed earlier by {@link #storedPaths(String)}.
     * A caller that deletes inodes nothing else writes to, such as old versions, can list them before
     * it opens its deletion transaction, so the storage listing does not run while that transaction
     * holds its locks. With the flag off this does nothing.
     *
     * @param inode the deleted contentlet inode
     * @param paths the binary and metadata paths stored for the inode
     * @throws DotDataException if there is no transaction or the job cannot be recorded; the caller's
     *                          deletion then rolls back
     */
    public static void enqueue(final String inode, final StoredPaths paths) throws DotDataException {
        if (!AssetStorageFeature.isEnabled()) {
            return;
        }
        validateInode(inode);
        requireTransaction();
        APILocator.getJobQueueManagerAPI().createJob(QUEUE,
                Map.of("inode", inode, "binaries", paths.binaries(), "metadata", paths.metadata()));
    }

    /**
     * Lists the binary and metadata paths stored for an inode, for {@link #enqueue(String, StoredPaths)}.
     *
     * @param inode the contentlet inode
     * @return the paths stored for the inode at this moment
     * @throws DotDataException if storage cannot be listed
     */
    public static StoredPaths storedPaths(final String inode) throws DotDataException {
        validateInode(inode);
        final List<String> binaries = List.copyOf(APILocator.getBinaryAssetStorageAPI().listBinaryPaths(inode));
        return new StoredPaths(binaries,
                List.copyOf(APILocator.getFileMetadataAPI().listMetadataForInode(inode, binaries)));
    }

    /** The binary and metadata paths stored for one inode, as recorded in its cleanup job. */
    public record StoredPaths(List<String> binaries, List<String> metadata) { }

    private static void requireTransaction() throws DotDataException {
        if (!DbConnectionFactory.inTransaction()) {
            throw new DotDataException("Binary cleanup must be recorded in the content deletion transaction");
        }
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
     * Deletes the binaries and metadata recorded for a deleted inode. Metadata goes first, then each
     * recorded binary path, then the inode's renditions and legacy image cache. Nothing outside the
     * recorded inventory is deleted, so revisions uploaded after the deletion survive. Each step
     * tolerates paths that are already gone, so a retry after a partial failure finishes the work.
     *
     * <p>A content version with the same inode can exist again by the time the job runs, for example
     * when push publishing re-creates it with the sender's inode. The job then deletes only the
     * recorded revision files that the returned row does not reference and keeps everything else
     * (see {@link #cleanReturnedInode}), and it completes instead of retrying until it fails.</p>
     *
     * @param job the cleanup job, carrying {@code inode}, {@code binaries} and {@code metadata}
     * @throws JobProcessingException if the flag is off or a deletion fails; the queue retries it
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
                LocalTransaction.wrapReturn(() -> {
                    cleanReturnedInode(inode, binaryPaths, metadataPaths);
                    return null;
                });
                return;
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
        } catch (Exception e) {
            throw new JobProcessingException(job.id(), "Unable to clean binary assets for " + inode, e);
        }
    }

    /**
     * Cleans up after an inode that exists again. Locks the returned row for the rest of the
     * transaction, so a writer that is changing it right now finishes first and its references are
     * the ones read here, and no new reference can be written while the files are deleted. Then
     * deletes the recorded revision binaries ({@code .revisions/<uuid>/...}) and the recorded metadata
     * inside those revision folders, except any folder the row references. Recorded files outside
     * revision folders (older-layout binaries and metadata), renditions and the legacy image cache
     * are all kept, because the returned row may be using them.
     *
     * <p>Revision keys contain a fresh UUID and are never reused by a check-in, so a revision the
     * row does not reference now can only be referenced again if a row with that exact key is
     * restored by hand.</p>
     *
     * @param inode         the inode that exists again
     * @param binaryPaths   the binary paths recorded when the inode was deleted
     * @param metadataPaths the metadata paths recorded when the inode was deleted
     * @throws DotDataException if the row was deleted again meanwhile (the job retries and then takes
     *                          the whole-inode path) or a deletion fails
     */
    private static void cleanReturnedInode(final String inode, final List<String> binaryPaths,
                                           final List<String> metadataPaths) throws DotDataException {
        final var rows = new DotConnect().setSQL("select contentlet_as_json from contentlet where inode = ? for update")
                .addParam(inode).loadObjectResults();
        if (rows.isEmpty()) {
            throw new DotDataException("Content version " + inode + " was deleted again during cleanup");
        }
        final Object json = rows.getFirst().get("contentlet_as_json");
        final Set<String> referenced;
        try {
            referenced = json == null || json.toString().isBlank() ? null : referencedRevisionFolders(json.toString());
        } catch (IOException unreadable) {
            Logger.warn(BinaryAssetCleanupProcessor.class, "Content version " + inode
                    + " exists again but its stored references cannot be read; keeping all recorded files");
            return;
        }
        if (referenced == null) {
            Logger.warn(BinaryAssetCleanupProcessor.class, "Content version " + inode
                    + " exists again without stored references; keeping all recorded files");
            return;
        }
        final List<String> binaries = binaryPaths.stream()
                .filter(path -> isUnreferencedRevision(path, referenced)).toList();
        final List<String> metadata = metadataPaths.stream()
                .filter(path -> isUnreferencedRevision(path, referenced)).toList();
        Logger.info(BinaryAssetCleanupProcessor.class, "Content version " + inode + " exists again: deleting "
                + binaries.size() + " unreferenced revision file(s) and " + metadata.size() + " metadata file(s), keeping "
                + (binaryPaths.size() - binaries.size() + metadataPaths.size() - metadata.size())
                + " recorded file(s) it references or that are not revisions");
        APILocator.getFileMetadataAPI().removeMetadataPaths(inode, metadata);
        final BinaryAssetStorageAPI storage = APILocator.getBinaryAssetStorageAPI();
        for (final Map.Entry<String, List<String>> field : byField(inode, binaries).entrySet()) {
            storage.deleteBinaryPaths(inode, field.getKey(), field.getValue());
        }
    }

    /**
     * Collects the revision folders a row references through any field's {@code storageKey} or
     * {@code metadataStorageKey}. The JSON is scanned without validating field types, so a key the
     * row mentions anywhere is kept.
     *
     * @param json the row's {@code contentlet_as_json}
     * @return the referenced revision folders, in lower case
     * @throws IOException if the JSON cannot be parsed
     */
    private static Set<String> referencedRevisionFolders(final String json) throws IOException {
        final Set<String> folders = new HashSet<>();
        final var fields = JSON.readTree(json).path("fields").elements();
        while (fields.hasNext()) {
            final var field = fields.next();
            for (final String name : List.of("storageKey", "metadataStorageKey")) {
                final String folder = revisionFolder(field.path(name).asText(""));
                if (folder != null) {
                    folders.add(folder);
                }
            }
        }
        return folders;
    }

    /**
     * Tells whether a recorded path is a revision file whose folder the returned row does not
     * reference. Anything that is not inside a revision folder is kept.
     *
     * @param path       a recorded binary or metadata path
     * @param referenced the revision folders the row references, in lower case
     * @return {@code true} when the path may be deleted
     */
    private static boolean isUnreferencedRevision(final String path, final Set<String> referenced) {
        final String folder = revisionFolder(path);
        return folder != null && !referenced.contains(folder);
    }

    /**
     * Returns the revision folder ({@code a/b/<inode>/<field>/.revisions/<uuid>/}) a binary key or
     * metadata path belongs to. A revision's metadata files live in that same folder. The result is
     * in lower case because the filesystem stores metadata paths in lower case, and comparing that
     * way only ever keeps more.
     *
     * @param path a binary key, or a metadata path with or without a leading {@code /}
     * @return the folder in lower case, or {@code null} when the path is not inside a revision folder
     */
    private static String revisionFolder(final String path) {
        final String relative = path.startsWith("/") ? path.substring(1) : path;
        final int revisions = relative.indexOf(REVISIONS);
        if (revisions < 0) {
            return null;
        }
        final int end = relative.indexOf('/', revisions + REVISIONS.length());
        return end < 0 ? null : relative.substring(0, end + 1).toLowerCase(Locale.ROOT);
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
