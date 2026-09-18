package com.dotcms.storage.binary;

import com.dotcms.content.business.json.ContentletJsonHelper;
import com.dotcms.storage.AmazonS3StoragePersistenceAPIImpl;
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
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/** Self-contained, immutable recovery archives, independent of normal binary cleanup. */
public final class ContentletBackupStorage {
    public static final String GROUP = "deleted-content-backups";
    private StoragePersistenceAPI storage;
    private boolean initialized;

    private static class Holder {
        private static final ContentletBackupStorage INSTANCE = new ContentletBackupStorage(null);
    }

    public static ContentletBackupStorage getInstance() { return Holder.INSTANCE; }

    public ContentletBackupStorage(StoragePersistenceAPI storage) { this.storage = storage; }

    private synchronized StoragePersistenceAPI storage() throws DotDataException {
        if (!AssetStorageFeature.isEnabled()) throw new DotDataException("S3 asset storage is disabled");
        if (storage == null) storage = AmazonS3StoragePersistenceAPIImpl.withPlainPaths();
        if (!initialized) {
            storage.createGroup(GROUP);
            initialized = true;
        }
        return storage;
    }

    private static void validateId(String id) {
        if (id == null || !id.matches("[A-Za-z0-9_-]{2,}")) throw new IllegalArgumentException("Invalid backup owner");
    }

    /** Lock the persisted snapshot until deletion commits; never move or remove its source files. */
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
                for (final var field : BinaryAssetReference.fromContentJson(json, content.getInode()).entrySet()) {
                    final var reference = field.getValue();
                    final File binary = reference.localFile(content.getInode(), field.getKey());
                    final String revision = reference.storageKey();
                    final String path = revision == null
                            ? content.getInode().charAt(0) + "/" + content.getInode().charAt(1) + "/"
                                    + content.getInode() + "/" + field.getKey() + "/" + binary.getName()
                            : revision;
                    zip.putNextEntry(new ZipEntry("assets/" + path));
                    try (var input = APILocator.getBinaryAssetStorageAPI().openLocalFile(binary)) { input.transferTo(zip); }
                    zip.closeEntry();
                    content.getMap().put(field.getKey(), binary);
                    final String metadataPath = metadataAPI.getFileName(content, field.getKey());
                    final var metadata = APILocator.getFileStorageAPI().retrieveRawMetaData(new StorageKey.Builder()
                            .group(Config.getStringProperty(StoragePersistenceProvider.METADATA_GROUP_NAME, FileMetadataAPI.DOT_METADATA))
                            .path(metadataPath).storage(StoragePersistenceProvider.getStorageType()).build());
                    if (metadata != null) {
                        text(zip, "assets/" + metadataPath.substring(1).toLowerCase(java.util.Locale.ROOT),
                                mapper.writeValueAsString(metadata));
                    } else if (BinaryAssetReference.metadataKeyOf(binary) != null) {
                        throw new IOException("Missing referenced backup metadata " + field.getKey());
                    }
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

    private static void text(ZipOutputStream zip, String name, String value) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(value.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }

    /** Archive the exact field inventory while its owner row is locked by the caller. */
    String storeField(String identifier, String inode, String json, List<String> binaries,
                      List<String> metadata) throws DotDataException {
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
                    zip.putNextEntry(new ZipEntry("binary-assets/" + path));
                    try (var input = APILocator.getBinaryAssetStorageAPI().openLocalFile(
                            new File(com.dotmarketing.util.ConfigUtils.getAssetPath(), path))) {
                        input.transferTo(zip);
                    }
                    zip.closeEntry();
                }
                for (String path : metadata) {
                    final var raw = APILocator.getFileStorageAPI().retrieveRawMetaData(new StorageKey.Builder()
                            .group(Config.getStringProperty(StoragePersistenceProvider.METADATA_GROUP_NAME, FileMetadataAPI.DOT_METADATA))
                            .path(path).storage(StoragePersistenceProvider.getStorageType()).build());
                    if (raw == null) throw new IOException("Missing field metadata " + path);
                    text(zip, "dotmetadata/" + path.substring(1), new ObjectMapper().writeValueAsString(raw));
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
