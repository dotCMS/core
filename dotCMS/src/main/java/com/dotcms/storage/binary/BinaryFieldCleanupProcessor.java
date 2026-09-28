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
import com.dotcms.storage.StoragePersistenceAPI;
import com.dotcms.storage.StoragePersistenceProvider;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.db.LocalTransaction;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Config;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.file.Path;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.enterprise.context.Dependent;

/** Field removal and its archived, exact cleanup inventory use the existing transactional queue. */
@Dependent
@Queue(BinaryFieldCleanupProcessor.QUEUE)
@ExponentialBackoffRetryPolicy
public class BinaryFieldCleanupProcessor implements JobProcessor, Validator {
    public static final String QUEUE = "binaryFieldCleanup";
    private static final ObjectMapper JSON = new ObjectMapper();

    @Override public void validate(Map<String, Object> parameters) throws JobValidationException {
        // The public job endpoint always injects userId; only the authorized field API records this work.
        if (!AssetStorageFeature.isEnabled() || parameters.containsKey("userId")) {
            throw new JobValidationException("Field cleanup is internal to enabled content field deletion");
        }
    }

    public static void enqueue(String type, String field, Date deletedBefore) throws DotDataException {
        requireTransaction();
        prefix(type, field);
        APILocator.getJobQueueManagerAPI().createJob(QUEUE,
                Map.of("type", type, "field", field, "deletedBefore", deletedBefore.getTime()));
    }

    private static void requireTransaction() throws DotDataException {
        if (!AssetStorageFeature.isEnabled() || !DbConnectionFactory.inTransaction()) {
            throw new DotDataException("S3 field cleanup requires a transaction and the feature flag");
        }
    }

    private static String prefix(String inode, String field) {
        new BinaryAssetReference.StoredBinary(null, null, "inventory").localFile(inode, field);
        return inode.charAt(0) + "/" + inode.charAt(1) + "/" + inode + "/" + field;
    }

    private static StoragePersistenceAPI metadataStorage() {
        return StoragePersistenceProvider.INSTANCE.get().getStorage(StoragePersistenceProvider.getStorageType());
    }

    private static String metadataGroup() {
        return Config.getStringProperty(StoragePersistenceProvider.METADATA_GROUP_NAME, FileMetadataAPI.DOT_METADATA);
    }

    /**
     * Archives the removed field across every version of a content type's contentlets, one row per
     * transaction. Each transaction locks only its row, rechecks that the row was not edited after the
     * field was removed, archives it, and advances the job's committed {@code afterInode} cursor, so a
     * retry resumes after the last archived row and no lock is held while other rows upload.
     *
     * @param job the per-type cleanup job, carrying {@code type}, {@code field} and {@code deletedBefore}
     * @throws DotDataException if a row cannot be archived or the cursor was changed by another worker
     */
    private static void cleanType(final Job job) throws Exception {
        if (DbConnectionFactory.inTransaction()) {
            throw new DotDataException("Field cleanup commits each row on its own and cannot join a transaction");
        }
        // The manager may retry an older Job instance. Always reload the committed cursor.
        CacheLocator.getJobCache().remove(job);
        final Job current = APILocator.getJobQueueManagerAPI().getJob(job.id());
        if (current == null || !QUEUE.equals(current.queueName())) {
            throw new DotDataException("Field cleanup job does not exist");
        }
        final var parameters = current.parameters();
        final String type = (String) parameters.get("type");
        final String field = (String) parameters.get("field");
        final Date deletedBefore = new Date(((Number) parameters.get("deletedBefore")).longValue());
        prefix(type, field);
        String after = (String) parameters.getOrDefault("afterInode", "");
        while (true) {
            final var candidates = new DotConnect().setSQL("select inode from contentlet where structure_inode = ? "
                            + "and inode > ? and mod_date <= ? order by inode limit 100")
                    .addParam(type).addParam(after).addParam(deletedBefore).loadObjectResults();
            if (candidates.isEmpty()) return;
            for (var candidate : candidates) {
                final String inode = candidate.get("inode").toString();
                final String previous = after;
                try {
                    LocalTransaction.wrapReturn(() -> {
                        // The lock and timestamp recheck protect an edit made since the candidate scan.
                        final var rows = new DotConnect().setSQL("select inode, identifier, contentlet_as_json "
                                        + "from contentlet where inode = ? and mod_date <= ? for update")
                                .addParam(inode).addParam(deletedBefore).loadObjectResults();
                        if (!rows.isEmpty()) archiveRow(rows.get(0), field);
                        // Compare the cursor so an overlapping worker cannot overwrite newer progress.
                        final var saved = new DotConnect().setSQL("update job set parameters = parameters || ?::jsonb, "
                                        + "updated_at = current_timestamp where id = ? and queue_name = ? "
                                        + "and coalesce(parameters->>'afterInode', '') = ? returning id")
                                .addParam(JSON.writeValueAsString(Map.of("afterInode", inode)))
                                .addParam(job.id()).addParam(QUEUE).addParam(previous).loadObjectResults();
                        if (saved.size() != 1) {
                            throw new DotDataException("Field cleanup cursor changed or job was removed");
                        }
                        return null;
                    });
                } catch (Exception failure) {
                    throw new DotDataException("Unable to preserve removed binary field " + inode + "/" + field, failure);
                }
                CacheLocator.getJobCache().remove(job);
                after = inode;
            }
        }
    }

    /**
     * Archives and clears the removed field on one contentlet row that the caller has locked in the
     * current transaction: it uploads a verified recovery ZIP of the row's field bytes and metadata,
     * removes the field from the row's JSON, and records the per-inode cleanup job.
     *
     * @param row   the locked row, with {@code inode}, {@code identifier} and {@code contentlet_as_json}
     * @param field the removed field's variable name
     * @throws Exception if the archive cannot be verified or the row cannot be updated
     */
    static void archiveRow(final Map<String, Object> row, final String field) throws Exception {
        final String inode = row.get("inode").toString();
        final Object value = row.get("contentlet_as_json");
        final String json = value == null || value.toString().isBlank()
                ? APILocator.getContentletJsonAPI().toJson(APILocator.getContentletAPI()
                        .find(inode, APILocator.systemUser(), false)) : value.toString();
        final ObjectNode document = (ObjectNode) JSON.readTree(json);
        final var storedField = document.path("fields").path(field);
        if (!storedField.isMissingNode() && !"Binary".equals(storedField.path("type").asText())) return;
        final String owner = prefix(inode, field);
        final var binaryAPI = APILocator.getBinaryAssetStorageAPI();
        final List<String> binaries = binaryAPI.listBinaryPaths(inode).stream()
                .filter(path -> path.startsWith(owner + "/")).toList();
        final var reference = BinaryAssetReference.fromJson(document.path("fields").path(field), inode, field);
        if (reference != null) {
            final String expected = reference.storageKey() == null
                    ? owner + "/" + reference.fileName() : reference.storageKey();
            if (!binaries.contains(expected)) throw new DotDataException("Missing binary before field backup: " + expected);
        }
        final Set<String> metadata = new LinkedHashSet<>();
        final Set<String> parents = new LinkedHashSet<>();
        final String legacyParent = "/" + owner.substring(0, owner.lastIndexOf('/') + 1);
        parents.add(legacyParent);
        for (String path : binaries) parents.add("/" + path.substring(0, path.lastIndexOf('/') + 1));
        for (String parent : parents) {
            // Local revision directories contain originals too. Their complete contents
            // are already in the physical inventory; only S3 can distinguish the groups.
            final var storage = parent.equals(legacyParent) ? metadataStorage()
                    : StoragePersistenceProvider.INSTANCE.get().getStorage(com.dotcms.storage.StorageType.S3);
            for (String path : storage.listObjectPaths(metadataGroup(), parent)) {
                final String absolute = path.startsWith("/") ? path : "/" + path;
                if (parent.equals(legacyParent) && absolute.substring(parent.length()).contains("/")) continue;
                if (ownsMetadata(owner, absolute)) metadata.add(absolute);
            }
        }
        if (reference != null && reference.metadataStorageKey() != null) metadata.add(reference.metadataStorageKey());
        if (binaries.isEmpty() && metadata.isEmpty() && reference == null) return;
        final String backup = ContentletBackupStorage.getInstance().storeField(
                row.get("identifier").toString(), inode, json, binaries, List.copyOf(metadata));
        ((ObjectNode) document.path("fields")).remove(field);
        new DotConnect().setSQL("update contentlet set contentlet_as_json = ?::jsonb where inode = ?")
                .addParam(JSON.writeValueAsString(document)).addParam(inode).loadResult();
        APILocator.getJobQueueManagerAPI().createJob(QUEUE, Map.of("inode", inode, "field", field,
                "backup", backup, "binaries", binaries, "metadata", List.copyOf(metadata)));
        CacheLocator.getContentletCache().remove(inode);
        HibernateUtil.addCommitListener(() -> CacheLocator.getContentletCache().remove(inode));
        final var snapshot = APILocator.getContentletAPI().find(inode, APILocator.systemUser(), false);
        new com.dotcms.rendering.velocity.services.ContentletLoader().invalidate(snapshot);
    }

    private static boolean ownsMetadata(String owner, String path) {
        // Filesystem metadata historically folds case; S3 metadata can retain its logical spelling.
        final String prefix = ("/" + owner).toLowerCase(java.util.Locale.ROOT);
        final String normalized = path.toLowerCase(java.util.Locale.ROOT);
        return path.endsWith(FileMetadataAPI.METADATA_JSON)
                && (normalized.startsWith(prefix + "/") || normalized.startsWith(prefix + ".")
                        || normalized.equals(prefix + FileMetadataAPI.METADATA_JSON))
                && Path.of(path).normalize().toString().equals(path);
    }

    /**
     * Runs a cleanup job. A per-type job archives the removed field one row per transaction (see
     * {@link #cleanType(Job)}); a per-inode job verifies its archived inventory and deletes it in a
     * single transaction.
     *
     * @param job the cleanup job
     * @throws JobProcessingException if archiving or deletion fails; the queue retries it
     */
    @Override
    @CloseDBIfOpened
    public void process(Job job) throws JobProcessingException {
        try {
            validate(job.parameters());
            if (job.parameters().containsKey("type")) {
                cleanType(job);
                return;
            }
            LocalTransaction.wrapReturn(() -> {
                cleanInode(job);
                return null;
            });
        } catch (Exception failure) {
            throw new JobProcessingException(job.id(), "Unable to clean removed binary field", failure);
        }
    }

    /**
     * Deletes one inode's archived field bytes and metadata once they are verified unreferenced and
     * their recovery archive is still present. Runs inside the caller's transaction.
     *
     * @param job the per-inode cleanup job, carrying the exact inventory recorded at archive time
     * @throws Exception if the inventory is invalid, still referenced, or cannot be deleted
     */
    private static void cleanInode(final Job job) throws Exception {
        requireTransaction();
        final var parameters = job.parameters();
        final String field = (String) parameters.get("field");
        final String inode = (String) parameters.get("inode");
        final String owner = prefix(inode, field);
        final List<String> binaries = strings(parameters.get("binaries"));
        final List<String> metadata = strings(parameters.get("metadata"));
        for (String path : binaries) {
            if (!path.startsWith(owner + "/") || path.contains("\\")
                    || !Path.of(path).normalize().toString().equals(path)) {
                throw new DotDataException("Invalid field cleanup inventory");
            }
        }
        for (String path : metadata) if (!ownsMetadata(owner, path)) throw new DotDataException("Invalid metadata inventory");
        final var rows = new DotConnect().setSQL("select contentlet_as_json from contentlet where inode = ? for update")
                .addParam(inode).loadObjectResults();
        for (var row : rows) {
            final var fields = JSON.readTree(row.get("contentlet_as_json").toString()).path("fields").fields();
            while (fields.hasNext()) {
                final var entry = fields.next();
                if (!"Binary".equals(entry.getValue().path("type").asText())) continue;
                final var reference = BinaryAssetReference.fromJson(entry.getValue(), inode, entry.getKey());
                if (reference == null) continue;
                final String key = reference.storageKey() == null
                        ? prefix(inode, entry.getKey()) + "/" + reference.fileName() : reference.storageKey();
                if (binaries.contains(key) || metadata.contains(reference.metadataStorageKey())) {
                    throw new DotDataException("Archived field bytes are referenced again; retain them");
                }
            }
        }
        final String backup = (String) parameters.get("backup");
        if (!backup.contains("/" + inode + "/")) throw new DotDataException("Archive owner does not match cleanup");
        // An operator may have removed the archive since enqueue; retain sources in that case.
        try (var ignored = ContentletBackupStorage.getInstance().open(backup)) { }
        for (String path : metadata) {
            metadataStorage().deleteObjectAndReferences(metadataGroup(), path);
            if (metadataStorage().existsObject(metadataGroup(), path)) throw new DotDataException("Metadata deletion incomplete");
            CacheLocator.getMetadataCache().removeMetadata(path);
        }
        final var binariesAPI = APILocator.getBinaryAssetStorageAPI();
        binariesAPI.deleteBinaryPaths(inode, field, binaries);
        binariesAPI.deleteGeneratedFiles(inode);
        final var legacyCache = new java.io.File(APILocator.getFileAssetAPI().getRealAssetsRootPath(), "cache/" + owner);
        com.liferay.util.FileUtil.deltree(legacyCache);
        if (legacyCache.exists()) throw new DotDataException("Legacy field cache deletion incomplete");
    }

    private static List<String> strings(Object value) {
        return ((List<?>) value).stream().map(String.class::cast).toList();
    }

    @Override public Map<String, Object> getResultMetadata(Job job) { return Map.of("field", job.parameters().get("field")); }
}
