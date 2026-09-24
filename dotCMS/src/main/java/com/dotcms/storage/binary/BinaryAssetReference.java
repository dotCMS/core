package com.dotcms.storage.binary;

import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.ConfigUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.nio.file.Path;

/** Immutable object references stored with the binary field in the content transaction. */
public final class BinaryAssetReference {
    private static final ObjectMapper JSON = new ObjectMapper();

    private BinaryAssetReference() { }

    /** Stored binaries, including old Image/File filename values with an actual owned file. */
    public static java.util.Map<String, StoredBinary> fromContentJson(final String json, final String inode)
            throws DotDataException {
        if (!com.dotcms.storage.AssetStorageFeature.isEnabled()) {
            throw new IllegalStateException("S3 asset storage is disabled");
        }
        final java.util.Map<String, StoredBinary> references = new java.util.LinkedHashMap<>();
        java.util.Set<String> inventory = null;
        try {
            final var fields = JSON.readTree(json).path("fields").fields();
            while (fields.hasNext()) {
                final var entry = fields.next();
                final var value = entry.getValue();
                final String type = value.path("type").asText();
                final StoredBinary reference;
                if ("Binary".equals(type)) {
                    reference = fromJson(value, inode, entry.getKey());
                } else if ("Image".equals(type) || "File".equals(type)) {
                    final String name = value.path("value").asText("");
                    if (name.isBlank() || name.contains("/") || name.contains("\\")
                            || name.equals(".") || name.equals("..")) continue;
                    final String path = ownerPrefix(inode, entry.getKey()) + "/" + name;
                    if (inventory == null) {
                        inventory = new java.util.HashSet<>(com.dotmarketing.business.APILocator
                                .getBinaryAssetStorageAPI().listBinaryPaths(inode));
                    }
                    // Modern linked asset IDs have no physical file owned by this field.
                    if (!inventory.contains(path)) continue;
                    reference = new StoredBinary(null, null, name);
                } else {
                    continue;
                }
                if (reference != null) references.put(entry.getKey(), reference);
            }
            return references;
        } catch (java.io.IOException failure) {
            throw new DotDataException("Invalid content asset references for " + inode, failure);
        }
    }

    /** Returns an immutable key, null for a legacy binary, or empty for an absent row/field. */
    public static String find(final String inode, final String field) throws DotDataException {
        final StoredBinary binary = findStored(inode, field);
        return binary == null ? "" : binary.storageKey();
    }

    @com.dotcms.business.CloseDBIfOpened
    public static StoredBinary findStored(final String inode, final String field) throws DotDataException {
        final String json = new DotConnect()
                .setSQL("select contentlet_as_json from contentlet where inode = ?")
                .addParam(inode).getString("contentlet_as_json");
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return fromJson(JSON.readTree(json).path("fields").path(field), inode, field);
        } catch (java.io.IOException e) {
            throw new DotDataException("Invalid binary reference for " + inode, e);
        }
    }

    public static StoredBinary fromJson(final com.fasterxml.jackson.databind.JsonNode binary,
                                        final String inode, final String field) {
        final String name = binary.path("value").asText("");
        if (name.isBlank()) {
            return null;
        }
        if (name.contains("/") || name.contains("\\") || name.equals(".") || name.equals("..")) {
            throw new IllegalArgumentException("Invalid binary filename");
        }
        final StoredBinary reference = new StoredBinary(binary.path("storageKey").asText(null),
                binary.path("metadataStorageKey").asText(null), name);
        if (reference.storageKey() != null && !reference.storageKey().endsWith("/" + name)) {
            throw new IllegalArgumentException("Binary reference filename does not match its stored name");
        }
        reference.localFile(inode, field); // Validate before using persisted paths.
        return reference;
    }

    public record StoredBinary(String storageKey, String metadataStorageKey, String fileName) {
        public File localFile(final String inode, final String field) {
            final File file = storageKey == null
                    ? new File(ConfigUtils.getAssetPath(), ownerPrefix(inode, field) + "/" + fileName)
                    : BinaryAssetReference.localFile(inode, field, storageKey);
            return withMetadata(file, inode, field, metadataStorageKey);
        }
    }

    private static String ownerPrefix(final String inode, final String field) {
        if (inode == null || !inode.matches("[A-Za-z0-9_-]{2,}") || field == null
                || field.isBlank() || field.contains("/") || field.contains("\\")
                || field.equals(".") || field.equals("..")) {
            throw new IllegalArgumentException("Invalid binary owner");
        }
        return inode.charAt(0) + "/" + inode.charAt(1) + "/" + inode + "/" + field;
    }

    /** A File still names the same binary bytes; the additional reference pins its metadata snapshot. */
    public static boolean isMetadataFile(final Class<?> type) {
        return type == MetadataFile.class;
    }

    private static final class MetadataFile extends File {
        private final String metadataKey;

        private MetadataFile(final File file, final String metadataKey) {
            super(file.getPath());
            this.metadataKey = metadataKey;
        }
    }

    public static String metadataKeyOf(final File file) {
        return file instanceof MetadataFile ? ((MetadataFile) file).metadataKey : null;
    }

    public static String metadataKeyOf(final File file, final String inode, final String field) {
        final String key = metadataKeyOf(file);
        if (key != null) {
            try {
                withMetadata(file, inode, field, key);
            } catch (IllegalArgumentException foreignOwner) {
                return null;
            }
        }
        return key;
    }

    public static String newMetadataKey(final File file, final String inode, final String field) {
        final String binaryKey = keyOf(file, inode, field);
        return "/" + (binaryKey == null ? ownerPrefix(inode, field) : binaryKey)
                + "." + java.util.UUID.randomUUID() + "-metadata.json";
    }

    public static File withMetadata(final File file, final String inode, final String field, final String key) {
        if (file == null || key == null) {
            return file;
        }
        final String binaryKey = keyOf(file, inode, field);
        if (binaryKey == null && !file.toPath().toAbsolutePath().normalize().equals(
                Path.of(ConfigUtils.getAssetPath(), ownerPrefix(inode, field), file.getName())
                        .toAbsolutePath().normalize())) {
            throw new IllegalArgumentException("Metadata reference does not belong to the binary");
        }
        final String prefix = "/" + (binaryKey == null ? ownerPrefix(inode, field) : binaryKey) + ".";
        if (!key.startsWith(prefix) || !key.substring(prefix.length()).matches("[a-f0-9-]{36}-metadata\\.json")) {
            throw new IllegalArgumentException("Invalid metadata revision path");
        }
        return new MetadataFile(file, key);
    }

    /** Restoration must not discard the metadata identity held by a content snapshot. */
    public static File preserveMetadata(final File restored, final File snapshot) {
        return restored != null && restored.equals(snapshot) && metadataKeyOf(snapshot) != null
                ? new MetadataFile(restored, metadataKeyOf(snapshot)) : restored;
    }

    public static File localFile(final String inode, final String field, final String key) {
        if (inode == null || !inode.matches("[A-Za-z0-9_-]{2,}") || field == null
                || field.isBlank() || field.contains("/") || field.contains("\\")
                || field.equals(".") || field.equals("..")) {
            throw new IllegalArgumentException("Invalid binary owner");
        }
        final String prefix = inode.charAt(0) + "/" + inode.charAt(1) + "/" + inode + "/" + field + "/.revisions/";
        if (!key.startsWith(prefix) || key.contains("\\") || key.contains("/../") || key.contains("/./")) {
            throw new IllegalArgumentException("Invalid binary revision path");
        }
        final String[] tail = key.substring(prefix.length()).split("/", -1);
        if (tail.length != 2 || !tail[0].matches("[a-f0-9-]{36}") || tail[1].isBlank()
                || tail[1].equals(".") || tail[1].equals("..")) {
            throw new IllegalArgumentException("Invalid binary revision path");
        }
        return new File(ConfigUtils.getAssetPath(), key);
    }

    /** Pure path conversion: JSON serialization must not stat NFS or download an object. */
    public static String keyOf(final File file, final String inode, final String field) {
        final String key = keyOf(file);
        if (key == null) {
            return null;
        }
        try {
            localFile(inode, field, key);
            return key;
        } catch (IllegalArgumentException foreignOwner) {
            // Check-in first saves the new inode, then copies the incoming source into its own revision.
            return null;
        }
    }

    /** Pure path conversion: JSON serialization must not stat NFS or download an object. */
    public static String keyOf(final File file) {
        final String assetRoot = ConfigUtils.getAssetPath();
        if (assetRoot == null || assetRoot.isBlank()) {
            return null;
        }
        final Path root = Path.of(assetRoot).toAbsolutePath().normalize();
        final Path path = file.toPath().toAbsolutePath().normalize();
        if (!path.startsWith(root)) {
            return null;
        }
        final Path relative = root.relativize(path);
        if (relative.getNameCount() != 7 || !relative.getName(4).toString().equals(".revisions")) {
            return null;
        }
        final String key = relative.toString().replace(File.separatorChar, '/');
        localFile(relative.getName(2).toString(), relative.getName(3).toString(), key);
        return key;
    }
}
