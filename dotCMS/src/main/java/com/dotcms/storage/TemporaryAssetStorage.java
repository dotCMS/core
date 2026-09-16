package com.dotcms.storage;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.ConfigUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Immutable temporary uploads, with access and original expiry checked before downloading bytes. */
public final class TemporaryAssetStorage {
    public static final String GROUP = "temporary-assets";
    public static final String MANAGED_MARKER = ".s3-upload";
    public static final String PERMISSIONS_FILE = "whoCanUse.tmp";
    private static final String RECEIPTS = ".receipts/";
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final JsonReaderDelegate<Map> READER = new JsonReaderDelegate<>(Map.class);
    private StoragePersistenceAPI remote;
    private FileSystemStoragePersistenceAPIImpl local;
    private Path root;

    private static class Holder {
        private static final TemporaryAssetStorage INSTANCE = new TemporaryAssetStorage(null, null);
    }

    public static TemporaryAssetStorage getInstance() {
        return Holder.INSTANCE;
    }

    public TemporaryAssetStorage(final StoragePersistenceAPI remote, final Path root) {
        this.remote = remote;
        this.root = root;
    }

    public static boolean validId(final String id) {
        return id != null && id.matches("temp_[A-Za-z0-9_-]{1,128}");
    }

    public static String metadataPath(final String id) {
        if (!validId(id)) throw new IllegalArgumentException("Invalid temporary resource id");
        return "/tmp_upload/" + id + "/" + id + FileMetadataAPI.META_TMP;
    }

    private Path root() {
        return root == null ? Path.of(ConfigUtils.getAssetTempPath()) : root;
    }

    /** Validates the upload's own directory before any producer creates directories or writes bytes. */
    public File file(final String id, final String name) throws IOException {
        if (!validId(id) || name == null || name.isBlank() || name.contains("\\")) {
            throw new IllegalArgumentException("Invalid temporary upload path");
        }
        final Path owner = root().toFile().getCanonicalFile().toPath().resolve(id);
        final Path requested = owner.resolve(name);
        if (!requested.startsWith(owner) || Files.isSymbolicLink(owner)) {
            throw new IllegalArgumentException("Invalid temporary upload directory");
        }
        Path component = owner;
        for (Path part : owner.relativize(requested)) {
            component = component.resolve(part);
            if (Files.isSymbolicLink(component)) {
                throw new IllegalArgumentException("Invalid temporary upload link");
            }
        }
        final Path target = requested.toFile().getCanonicalFile().toPath();
        if (!target.startsWith(owner) || target.equals(owner)) {
            throw new IllegalArgumentException("Temporary upload is outside its own directory");
        }
        for (Path part : owner.relativize(target)) {
            if (part.toString().startsWith(".") || part.toString().equalsIgnoreCase(PERMISSIONS_FILE)
                    || part.toString().endsWith(FileMetadataAPI.META_TMP)) {
                throw new IllegalArgumentException("Reserved temporary upload path");
            }
        }
        return target.toFile();
    }

    public boolean isManagedLocally(final String id) {
        return validId(id) && Files.exists(root().resolve(id).resolve(MANAGED_MARKER));
    }

    private synchronized StoragePersistenceAPI remote() throws DotDataException {
        if (remote == null) {
            remote = AmazonS3StoragePersistenceAPIImpl.withPlainPaths();
        }
        return remote;
    }

    private synchronized FileSystemStoragePersistenceAPIImpl local() throws IOException {
        if (local == null) {
            Files.createDirectories(root());
            local = new FileSystemStoragePersistenceAPIImpl();
            local.addGroupMapping(GROUP, root().toFile());
        }
        return local;
    }

    public record Receipt(String id, String name, long modifiedAt, List<String> allowed) {
        public boolean expired() {
            final long ageMillis = Config.getIntProperty("TEMP_RESOURCE_MAX_AGE_SECONDS", 1800) * 1000L;
            return modifiedAt <= System.currentTimeMillis() - ageMillis;
        }

        String prefix() {
            return "data/" + modifiedAt + "/" + id + "/";
        }

        String key() {
            return prefix() + name;
        }
    }

    public Optional<Receipt> receipt(final String id) throws DotDataException {
        if (!AssetStorageFeature.isEnabled() || !validId(id)) return Optional.empty();
        final Map<?, ?> record = (Map<?, ?>) remote().pullObject(GROUP, RECEIPTS + id + ".json", READER);
        if (record == null) return Optional.empty();
        try {
            final String name = (String) record.get("name");
            file(id, name);
            final long modified = ((Number) record.get("modifiedAt")).longValue();
            if (modified <= 0) throw new IllegalArgumentException("Invalid upload timestamp");
            final List<String> allowed = ((List<?>) record.get("allowed")).stream()
                    .map(value -> (String) value).toList();
            return Optional.of(new Receipt(id, name, modified, List.copyOf(allowed)));
        } catch (Exception invalid) {
            throw new DotDataException("Invalid temporary upload record for " + id, invalid);
        }
    }

    /** Called only after the upload stream has closed and its final filename has been resolved. */
    public void store(final String id, final File source) throws DotDataException {
        if (!AssetStorageFeature.isEnabled()) return;
        try {
            final Path owner = root().toFile().getCanonicalFile().toPath().resolve(id);
            final String name = owner.relativize(source.getCanonicalFile().toPath()).toString().replace(File.separatorChar, '/');
            final File validated = file(id, name);
            if (!validated.isFile()) throw new IOException("Temporary upload is incomplete");
            Files.writeString(owner.resolve(MANAGED_MARKER), "");
            final File permissions = new File(validated.getParentFile(), PERMISSIONS_FILE);
            if (Files.isSymbolicLink(permissions.toPath())) throw new IOException("Invalid upload permissions path");
            final List<String> allowed = List.copyOf(JSON.readValue(permissions, List.class));
            final Receipt record = receipt(id).orElseGet(() -> new Receipt(id, name, validated.lastModified(), allowed));
            if (!record.name().equals(name) || !record.allowed().equals(allowed) || record.expired()) {
                throw new DotDataException("Temporary upload changed or expired: " + id);
            }
            remote().createGroup(GROUP);
            // Record ownership/expiry first, so a failed upload is still discoverable for cleanup.
            final var data = new java.util.HashMap<String, Serializable>();
            data.put("name", record.name());
            data.put("modifiedAt", record.modifiedAt());
            data.put("allowed", new java.util.ArrayList<>(record.allowed()));
            if (!remote().backfillObject(GROUP, RECEIPTS + id + ".json", new JsonWriterDelegate(), READER, data)
                    || !remote().backfillFile(GROUP, record.key(), validated)) {
                throw new DotDataException("Temporary upload has no verified S3 copy: " + id);
            }
        } catch (IOException | RuntimeException failure) {
            throw new DotDataException("Unable to store temporary upload " + id, failure);
        }
    }

    public boolean exists(final Receipt record) throws DotDataException {
        return AssetStorageFeature.isEnabled() && !record.expired() && remote().existsObject(GROUP, record.key());
    }

    public Optional<File> retrieve(final Receipt record, final List<String> accessing) throws DotDataException {
        if (!AssetStorageFeature.isEnabled() || record.expired() || accessing == null
                || Collections.disjoint(record.allowed(), accessing)) return Optional.empty();
        try {
            final File target = file(record.id(), record.name());
            if (!target.isFile()) {
                final File downloaded = remote().pullFile(GROUP, record.key());
                if (downloaded == null) return Optional.empty();
                try {
                    // The marker prevents legacy fallback if an expired/deleted receipt disappears.
                    Files.createDirectories(root().resolve(record.id()));
                    Files.writeString(root().resolve(record.id()).resolve(MANAGED_MARKER), "");
                    local().pushFile(GROUP, record.id() + "/" + record.name(), downloaded, Map.of());
                    Files.setLastModifiedTime(target.toPath(), java.nio.file.attribute.FileTime.fromMillis(record.modifiedAt()));
                } finally {
                    remote().releaseRetrievedFile(downloaded);
                }
            }
            final Path permissions = target.toPath().getParent().resolve(PERMISSIONS_FILE);
            if (Files.isSymbolicLink(permissions)) throw new IOException("Invalid upload permissions path");
            if (!Files.exists(permissions)) JSON.writeValue(permissions.toFile(), record.allowed());
            // Restoration must not renew the logical TTL; it lives in the immutable receipt.
            return record.expired() ? Optional.empty() : Optional.of(target);
        } catch (IOException | RuntimeException failure) {
            throw new DotDataException("Unable to restore temporary upload " + record.id(), failure);
        }
    }

    private void removeMetadataAndRenditions(final String id) throws DotDataException {
        APILocator.getFileStorageAPI().removeMetaData(new FetchMetadataParams.Builder().cache(false)
                .storageKey(new StorageKey.Builder().storage(StorageType.S3)
                        .group(Config.getStringProperty(StoragePersistenceProvider.METADATA_GROUP_NAME, FileMetadataAPI.DOT_METADATA))
                        .path(metadataPath(id)).build()).build());
        APILocator.getBinaryAssetStorageAPI().deleteGeneratedFiles(id);
    }

    /** Timestamped payload prefixes also expose interrupted uploads after their receipt is gone. */
    public void cleanupExpired() throws DotDataException {
        if (!AssetStorageFeature.isEnabled()) return;
        for (String path : remote().listObjectPaths(GROUP, RECEIPTS)) {
            if (!path.startsWith(RECEIPTS) || !path.endsWith(".json")) continue;
            final String id = path.substring(RECEIPTS.length(), path.length() - ".json".length());
            final var record = receipt(id);
            if (record.isEmpty() || !record.get().expired()) continue;
            removeMetadataAndRenditions(id);
            delete(record.get().prefix());
            delete(path);
            try {
                final Path directory = file(id, "placeholder").toPath().getParent();
                com.liferay.util.FileUtil.deltree(directory.toFile());
                if (Files.exists(directory)) throw new IOException("Temporary cache directory remains");
            } catch (IOException failure) {
                throw new DotDataException("Unable to remove expired temporary cache " + id, failure);
            }
        }
        final var expired = new HashSet<String>();
        for (String path : remote().listObjectPaths(GROUP, "data/")) {
            final String[] parts = path.split("/", 4);
            if (parts.length != 4 || !parts[0].equals("data") || !validId(parts[2])) continue;
            try {
                final var record = new Receipt(parts[2], parts[3], Long.parseLong(parts[1]), List.of());
                if (record.expired()) expired.add(record.prefix());
            } catch (NumberFormatException ignored) {
                // Only this uploader's timestamped namespace belongs to this cleanup.
            }
        }
        for (String prefix : expired) delete(prefix);
    }

    private void delete(final String path) throws DotDataException {
        if (path.endsWith("/")) {
            for (String object : remote().listObjectPaths(GROUP, path)) {
                delete(object);
            }
            return;
        }
        if (!remote().deleteObjectAndReferences(GROUP, path)) {
            throw new DotDataException("Unable to remove expired temporary object " + path);
        }
    }
}
