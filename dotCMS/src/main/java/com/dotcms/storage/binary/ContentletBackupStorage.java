package com.dotcms.storage.binary;

import com.dotcms.content.business.json.ContentletJsonHelper;
import com.dotcms.storage.AssetStorageFeature;
import com.dotcms.storage.FileMetadataAPI;
import com.dotcms.storage.StorageKey;
import com.dotcms.storage.StoragePersistenceAPI;
import com.dotcms.storage.StoragePersistenceProvider;
import com.dotcms.util.xstream.XStreamHandler;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/** Self-contained, immutable recovery archives, independent of normal binary cleanup. */
public final class ContentletBackupStorage {
    public static final String GROUP = "deleted-content-backups";
    /** Archive entry listing the files that were already gone when the backup was taken. */
    public static final String MISSING_MANIFEST = "missing.json";
    private StoragePersistenceAPI storage;
    private boolean initialized;

    private static class Holder {
        private static final ContentletBackupStorage INSTANCE = new ContentletBackupStorage(null);
    }

    public static ContentletBackupStorage getInstance() { return Holder.INSTANCE; }

    public ContentletBackupStorage(StoragePersistenceAPI storage) { this.storage = storage; }

    private synchronized StoragePersistenceAPI storage() throws DotDataException {
        if (!AssetStorageFeature.isEnabled()) throw new DotDataException("S3 asset storage is disabled");
        if (storage == null) storage = StoragePersistenceProvider.remoteObjectStorage();
        if (!initialized) {
            storage.createGroup(GROUP);
            initialized = true;
        }
        return storage;
    }

    private static void validateId(String id) {
        if (id == null || !id.matches("[A-Za-z0-9_-]{2,}")) throw new IllegalArgumentException("Invalid backup owner");
    }

    /**
     * Writes a recovery ZIP for one content version to S3 before that version is deleted. Runs inside
     * the deletion transaction and locks the version's row until the deletion commits. Source files
     * are only read, never moved or removed.
     *
     * <p>A file that is already gone does not stop the deletion. When a binary has no local copy and
     * no copy in S3, or a metadata file the row points to no longer exists, the archive leaves it
     * out, lists it in a {@value #MISSING_MANIFEST} entry and a warning is logged. The manifest is
     * only written when something is missing. Any other problem still fails the backup and so the
     * deletion: an S3 error while restoring a file, any other read error, a failure writing the
     * archive, or S3 not confirming the uploaded archive.</p>
     *
     * @param requested the content version about to be deleted
     * @return the S3 key of the stored archive
     * @throws DotDataException if the flag is off, no transaction is active, or the backup fails
     */
    public String store(Contentlet requested) throws DotDataException {
        if (!AssetStorageFeature.isEnabled() || !DbConnectionFactory.inTransaction()) {
            throw new DotDataException("S3 content backup requires the content deletion transaction");
        }
        validateId(requested.getInode());
        Path archive = null;
        try {
            final var rows = new DotConnect().setSQL("select contentlet_as_json from contentlet where inode = ? for update")
                    .addParam(requested.getInode()).loadObjectResults();
            if (rows.isEmpty()) throw new DotDataException("Content version disappeared before backup");
            final Object persisted = rows.getFirst().get("contentlet_as_json");
            final Contentlet content = persisted == null || persisted.toString().isBlank()
                    ? new Contentlet(APILocator.getContentletAPI().find(requested.getInode(), APILocator.systemUser(), false))
                    : APILocator.getContentletJsonAPI().toMutableContentlet(
                            ContentletJsonHelper.INSTANCE.get().immutableFromJson(persisted.toString()));
            validateId(content.getIdentifier());
            final String json = persisted == null || persisted.toString().isBlank()
                    ? APILocator.getContentletJsonAPI().toJson(content) : persisted.toString();
            final String key = content.getIdentifier() + "/" + content.getInode() + "/" + UUID.randomUUID() + ".zip";
            archive = Files.createTempFile("contentlet-backup-", ".zip");
            try (var lease = APILocator.getBinaryAssetStorageAPI().acquireCacheLease();
                 var zip = new ZipOutputStream(Files.newOutputStream(archive))) {
                text(zip, "contentlet.json", json);
                zip.putNextEntry(new ZipEntry("contentlet.xml"));
                XStreamHandler.newXStreamInstance().toXML(content, zip);
                zip.closeEntry();
                final var metadataAPI = APILocator.getFileMetadataAPI();
                final var mapper = new ObjectMapper();
                final List<Map<String, String>> missing = new ArrayList<>();
                for (final var field : BinaryAssetReference.fromContentJson(json, content.getInode()).entrySet()) {
                    final var reference = field.getValue();
                    final File binary = reference.localFile(content.getInode(), field.getKey());
                    final String revision = reference.storageKey();
                    final String path = revision == null
                            ? content.getInode().charAt(0) + "/" + content.getInode().charAt(1) + "/"
                                    + content.getInode() + "/" + field.getKey() + "/" + binary.getName()
                            : revision;
                    if (!archiveBinary(APILocator.getBinaryAssetStorageAPI(), binary, zip, "assets/" + path)) {
                        missing.add(missingEntry(content.getInode(), "binary", field.getKey(), "assets/" + path));
                    }
                    content.getMap().put(field.getKey(), binary);
                    final String metadataPath = metadataAPI.getFileName(content, field.getKey());
                    final var metadata = rawMetadata(metadataPath);
                    final String metadataEntry = "assets/" + metadataPath.substring(1).toLowerCase(java.util.Locale.ROOT);
                    if (metadata != null) {
                        text(zip, metadataEntry, mapper.writeValueAsString(metadata));
                    } else if (BinaryAssetReference.metadataKeyOf(binary) != null) {
                        missing.add(missingEntry(content.getInode(), "metadata", field.getKey(), metadataEntry));
                    }
                }
                if (!missing.isEmpty()) {
                    text(zip, MISSING_MANIFEST, mapper.writeValueAsString(missing));
                }
            }
            if (!storage().backfillFile(GROUP, key, archive.toFile())) {
                throw new DotDataException("Recovery archive was not verified in S3");
            }
            return key;
        } catch (Exception failure) {
            throw new DotDataException("Unable to back up content version " + requested.getInode(), failure);
        } finally {
            if (archive != null) {
                try { Files.deleteIfExists(archive); }
                catch (IOException failure) { com.dotmarketing.util.Logger.warn(ContentletBackupStorage.class, "Unable to remove backup staging file", failure); }
            }
        }
    }

    /**
     * Copies one binary into the archive under {@code entryName}. The entry is only created once the
     * file is open, so a skipped binary leaves nothing behind in the archive.
     *
     * @param binaries  the binary storage that opens, and if needed restores, the file
     * @param binary    the binary's local path
     * @param zip       the archive being written
     * @param entryName the archive entry for the binary's bytes
     * @return {@code true} when the bytes were archived, {@code false} when storage confirms the file
     *         is gone: it is not on local disk and S3 has no copy either
     * @throws IOException      for any other read or archive write failure
     * @throws DotDataException if restoring the file from S3 fails
     */
    static boolean archiveBinary(final BinaryAssetStorageAPI binaries, final File binary,
                                 final ZipOutputStream zip, final String entryName) throws IOException, DotDataException {
        final InputStream input;
        try {
            input = binaries.openLocalFile(binary);
        } catch (final NoSuchFileException absent) {
            return false;
        }
        try (input) {
            zip.putNextEntry(new ZipEntry(entryName));
            input.transferTo(zip);
            zip.closeEntry();
        }
        return true;
    }

    /**
     * Describes one file the archive could not include and logs a warning, so operators can see it
     * was already lost before the backup.
     *
     * @param inode the content version being backed up
     * @param kind  {@code binary} or {@code metadata}
     * @param field the field variable name
     * @param path  the archive entry the file would have had
     * @return the manifest entry
     */
    static Map<String, String> missingEntry(final String inode, final String kind, final String field,
                                                    final String path) {
        Logger.warn(ContentletBackupStorage.class, "Backup of content version " + inode + " leaves out the "
                + kind + " of field '" + field + "' because it no longer exists (" + path + ")");
        final Map<String, String> entry = new LinkedHashMap<>();
        entry.put("kind", kind);
        entry.put("field", field);
        entry.put("path", path);
        return entry;
    }

    private static void text(ZipOutputStream zip, String name, String value) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(value.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }

    /**
     * Reads one stored metadata file from the configured metadata chain, restoring it from S3 when
     * it is not local.
     *
     * @param path the metadata path, starting with {@code /}
     * @return the metadata, or {@code null} when it exists neither locally nor in S3
     * @throws DotDataException if the metadata storage fails
     */
    static Map<String, Serializable> rawMetadata(final String path) throws DotDataException {
        return APILocator.getFileStorageAPI().retrieveRawMetaData(new StorageKey.Builder()
                .group(Config.getStringProperty(StoragePersistenceProvider.METADATA_GROUP_NAME, FileMetadataAPI.DOT_METADATA))
                .path(path).storage(StoragePersistenceProvider.getStorageType()).build());
    }

    /**
     * Archives the exact inventory of a removed binary field while the caller holds the owner row's
     * lock, and uploads the archive to S3.
     *
     * <p>The caller decides what was already gone before the cleanup started and passes it in
     * {@code missing}; those entries are written to {@value #MISSING_MANIFEST}, which only exists when
     * the list is not empty. Every path in {@code binaries} and {@code metadata} came from a storage
     * listing, so if one of them cannot be read now it changed concurrently, and the archive fails
     * so the job retries and lists again. An S3 error, any other read or write error and an upload
     * S3 does not confirm also fail it.</p>
     *
     * @param identifier the content identifier
     * @param inode      the content version whose field is archived
     * @param json       the row's {@code contentlet_as_json} before the field is removed
     * @param binaries   listed binary paths to archive under {@code binary-assets/}
     * @param metadata   listed or referenced metadata paths to archive under {@code dotmetadata/}
     * @param missing    files the row references that no longer exist, as built by {@link #missingEntry}
     * @return the S3 key of the stored archive
     * @throws DotDataException if the flag is off, no transaction is active, or the archive fails
     */
    String storeField(String identifier, String inode, String json, List<String> binaries,
                      List<String> metadata, List<Map<String, String>> missing) throws DotDataException {
        if (!AssetStorageFeature.isEnabled() || !DbConnectionFactory.inTransaction()) {
            throw new DotDataException("Field backup requires the cleanup transaction");
        }
        validateId(identifier);
        validateId(inode);
        Path archive = null;
        try {
            archive = Files.createTempFile("binary-field-backup-", ".zip");
            try (var lease = APILocator.getBinaryAssetStorageAPI().acquireCacheLease();
                 var zip = new ZipOutputStream(Files.newOutputStream(archive))) {
                text(zip, "contentlet.json", json);
                for (String path : binaries) {
                    if (!archiveBinary(APILocator.getBinaryAssetStorageAPI(),
                            new File(com.dotmarketing.util.ConfigUtils.getAssetPath(), path), zip, "binary-assets/" + path)) {
                        throw new NoSuchFileException("Listed field binary disappeared during backup: " + path);
                    }
                }
                final var mapper = new ObjectMapper();
                for (String path : metadata) {
                    final var raw = rawMetadata(path);
                    if (raw == null) throw new IOException("Missing field metadata " + path);
                    text(zip, "dotmetadata/" + path.substring(1), mapper.writeValueAsString(raw));
                }
                if (!missing.isEmpty()) {
                    text(zip, MISSING_MANIFEST, mapper.writeValueAsString(missing));
                }
            }
            final String key = identifier + "/" + inode + "/" + UUID.randomUUID() + ".zip";
            if (!storage().backfillFile(GROUP, key, archive.toFile())) {
                throw new DotDataException("Field recovery archive was not verified in S3");
            }
            return key;
        } catch (Exception failure) {
            throw new DotDataException("Unable to archive binary field for " + inode, failure);
        } finally {
            if (archive != null) {
                try { Files.deleteIfExists(archive); }
                catch (IOException failure) { com.dotmarketing.util.Logger.warn(ContentletBackupStorage.class,
                        "Unable to remove field backup staging file", failure); }
            }
        }
    }

    public List<String> list(String identifier) throws DotDataException {
        validateId(identifier);
        return storage().listObjectPaths(GROUP, identifier + "/");
    }

    /** Closing the recovery stream releases its private download; no permanent local copy is required. */
    public InputStream open(String key) throws DotDataException, IOException {
        if (key == null || !key.matches("[A-Za-z0-9_-]{2,}/[A-Za-z0-9_-]{2,}/[a-f0-9-]{36}\\.zip")) {
            throw new IllegalArgumentException("Invalid recovery archive key");
        }
        final StoragePersistenceAPI remote = storage();
        final File download = remote.pullFile(GROUP, key);
        if (download == null) throw new IOException("Missing recovery archive");
        final InputStream input;
        try { input = Files.newInputStream(download.toPath()); }
        catch (IOException failure) { remote.releaseRetrievedFile(download); throw failure; }
        return new FilterInputStream(input) {
            @Override public void close() throws IOException {
                try { super.close(); }
                finally {
                    try { remote.releaseRetrievedFile(download); }
                    catch (DotDataException failure) { throw new IOException("Unable to release recovery download", failure); }
                }
            }
        };
    }
}
