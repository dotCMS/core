package com.dotcms.storage.binary;

import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.storage.AssetStorageFeature;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.ConfigUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Immutable object references stored with the binary field in the content transaction. */
public final class BinaryAssetReference {
    private static final ObjectMapper JSON = new ObjectMapper();

    private BinaryAssetReference() { }

    /** Stored binaries, including old Image/File filename values with an actual owned file. */
    public static Map<String, StoredBinary> fromContentJson(final String json, final String inode)
            throws DotDataException {
        if (!AssetStorageFeature.isEnabled()) {
            throw new IllegalStateException("S3 asset storage is disabled");
        }
        final Map<String, StoredBinary> references = new LinkedHashMap<>();
        Set<String> inventory = null;
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
                            || name.equals(".") || name.equals("..")) {
                        continue;
                    }
                    final String path = ownerPrefix(inode, entry.getKey()) + "/" + name;
                    if (inventory == null) {
                        inventory = new HashSet<>(APILocator
                                .getBinaryAssetStorageAPI().listBinaryPaths(inode));
                    }
                    // Modern linked asset IDs have no physical file owned by this field.
                    if (!inventory.contains(path)) {
                        continue;
                    }
                    reference = new StoredBinary(null, null, name);
                } else {
                    continue;
                }
                if (reference != null) {
                    references.put(entry.getKey(), reference);
                }
            }
            return references;
        } catch (IOException failure) {
            throw new DotDataException("Invalid content asset references for " + inode, failure);
        }
    }

    /**
     * Looks up the storage key that a contentlet row records for one binary field.
     *
     * @param inode the contentlet inode
     * @param field the field variable name
     * @return the immutable revision key; {@code null} when the field holds an older-layout binary or
     *         the row has no {@code contentlet_as_json}, so the caller must search the older layout;
     *         an empty string when the row's JSON records no binary for the field
     * @throws DotDataException if the row cannot be read or its JSON cannot be parsed
     */
    public static String find(final String inode, final String field) throws DotDataException {
        final String json = contentJson(inode);
        if (json == null) {
            return null;
        }
        final StoredBinary binary = fromContentField(json, inode, field);
        return binary == null ? "" : binary.storageKey();
    }

    /**
     * Reads the {@code contentlet_as_json} column of one contentlet row.
     *
     * <p>The column is empty when {@code SAVE_CONTENTLET_AS_JSON} is false or the row has not been
     * populated yet. That says nothing about whether the row has a binary, so callers fall back to
     * the older filesystem layout instead of treating it as "no binary".</p>
     *
     * @param inode the contentlet inode
     * @return the JSON text, or {@code null} when the row is missing or the column is null or blank
     * @throws DotDataException if the query fails
     */
    @CloseDBIfOpened
    static String contentJson(final String inode) throws DotDataException {
        final String json = new DotConnect()
                .setSQL("select contentlet_as_json from contentlet where inode = ?")
                .addParam(inode).getString("contentlet_as_json");
        return json == null || json.isBlank() ? null : json;
    }

    /**
     * Reads the binary reference recorded for one field in a contentlet's JSON.
     *
     * @param json  the row's {@code contentlet_as_json}, as returned by {@link #contentJson(String)}
     * @param inode the contentlet inode
     * @param field the field variable name
     * @return the stored reference, or {@code null} when the JSON records no binary for the field
     * @throws DotDataException if the JSON cannot be parsed
     */
    static StoredBinary fromContentField(final String json, final String inode, final String field)
            throws DotDataException {
        try {
            return fromJson(JSON.readTree(json).path("fields").path(field), inode, field);
        } catch (IOException e) {
            throw new DotDataException("Invalid binary reference for " + inode, e);
        }
    }

    /**
     * Builds the stored reference for one binary field from its node in {@code contentlet_as_json}.
     * The file name must be a single path segment and must match the last segment of the storage
     * key, and the resulting local path must stay inside the field's directory; anything else is
     * rejected.
     *
     * @param binary the field's JSON node
     * @param inode  the contentlet inode that owns the field
     * @param field  the field variable name
     * @return the reference, or {@code null} when the node has no file name
     * @throws IllegalArgumentException if the file name, storage key, metadata key or owner is not valid
     */
    public static StoredBinary fromJson(final JsonNode binary,
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
        /**
         * Resolves this reference to its file in the local asset cache. A reference without a
         * storage key maps to the older {@code <inode>/<field>/<fileName>} layout; one with a key
         * must be a revision key owned by the given inode and field, and a metadata key, if present,
         * must belong to that binary. Keys owned by anyone else are rejected.
         *
         * @param inode the contentlet inode that owns the field
         * @param field the field variable name
         * @return the local file, carrying the metadata key when the reference has one
         * @throws IllegalArgumentException if the owner, storage key or metadata key is not valid
         */
        public File localFile(final String inode, final String field) {
            final File file = storageKey == null
                    ? new File(ConfigUtils.getAssetPath(), ownerPrefix(inode, field) + "/" + fileName)
                    : BinaryAssetReference.localFile(inode, field, storageKey);
            return withMetadata(file, inode, field, metadataStorageKey);
        }
    }

    private static String ownerPrefix(final String inode, final String field) {
        if (!isValidOwner(inode, field)) {
            throw new IllegalArgumentException("Invalid binary owner");
        }
        return inode.charAt(0) + "/" + inode.charAt(1) + "/" + inode + "/" + field;
    }

    /**
     * Checks that an inode and field can name a directory in the asset layout without leaving it.
     *
     * @param inode the contentlet inode: at least two letters, digits, underscores or hyphens
     * @param field the field variable name, which must pass {@link #isValidName(String)}
     * @return true when both are safe to use as path segments
     */
    static boolean isValidOwner(final String inode, final String field) {
        return inode != null && inode.matches("[A-Za-z0-9_-]{2,}") && isValidName(field);
    }

    /**
     * Checks that a field or file name is a single path segment: not blank, no {@code /} or
     * {@code \}, and not {@code .} or {@code ..}.
     *
     * @param name the field variable name or file name
     * @return true when the name cannot move a path out of its parent directory
     */
    static boolean isValidName(final String name) {
        return name != null && !name.isBlank() && !name.contains("/") && !name.contains("\\")
                && !name.equals(".") && !name.equals("..");
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

    /**
     * Returns the metadata revision key pinned to a file by {@link #withMetadata}. Any other file,
     * including {@code null}, has no key.
     *
     * @param file the file to inspect
     * @return the metadata key, or {@code null} when the file carries none
     */
    public static String metadataKeyOf(final File file) {
        return file instanceof MetadataFile ? ((MetadataFile) file).metadataKey : null;
    }

    /**
     * Returns the metadata revision key pinned to a file only when that key belongs to the given
     * owner. A key that fails the {@link #withMetadata} checks for this inode and field, such as
     * one copied from another contentlet, is ignored rather than rejected.
     *
     * @param file  the file to inspect
     * @param inode the contentlet inode expected to own the key
     * @param field the field variable name expected to own the key
     * @return the metadata key, or {@code null} when the file carries none or it belongs elsewhere
     */
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

    /**
     * Creates a new metadata revision key for a binary: the binary's revision key when it is owned
     * by this inode and field, otherwise the field's directory, followed by {@code .}, a random UUID
     * and {@code -metadata.json}.
     *
     * @param file  the binary the metadata describes
     * @param inode the contentlet inode that owns the binary
     * @param field the field variable name
     * @return the new metadata key, starting with {@code /}
     * @throws IllegalArgumentException if the inode or field cannot name a directory in the asset layout
     */
    public static String newMetadataKey(final File file, final String inode, final String field) {
        final String binaryKey = keyOf(file, inode, field);
        return "/" + (binaryKey == null ? ownerPrefix(inode, field) : binaryKey)
                + "." + UUID.randomUUID() + "-metadata.json";
    }

    /**
     * Pins a metadata revision key to a binary file. The key must have the form produced by
     * {@link #newMetadataKey} for this binary, and an older-layout file must sit directly in the
     * field's directory in the asset root; any other key or file is rejected.
     *
     * @param file  the binary file
     * @param inode the contentlet inode that owns the binary
     * @param field the field variable name
     * @param key   the metadata revision key
     * @return a file carrying the key, or {@code file} unchanged when file or key is {@code null}
     * @throws IllegalArgumentException if the file or key does not belong to this owner
     */
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

    /**
     * Maps an immutable revision key to its file in the local asset cache, after checking that the
     * key belongs to the given owner and has the {@code .revisions/<uuid>/<fileName>} layout.
     *
     * @param inode the contentlet inode that owns the revision
     * @param field the field variable name that owns the revision
     * @param key   the revision key, relative to the asset root
     * @return the local cache file for the key
     * @throws IllegalArgumentException if the owner or the key is not valid
     */
    public static File localFile(final String inode, final String field, final String key) {
        if (!isValidOwner(inode, field)) {
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

    /** The contentlet inode and field variable name that own a revision key. */
    public record Owner(String inode, String field) { }

    /**
     * Reads the owner out of a revision key with the {@code i/n/<inode>/<field>/.revisions/<uuid>/<fileName>}
     * layout. This only parses the key; {@link #localFile(String, String, String)} still validates it.
     *
     * @param key the revision key, relative to the asset root
     * @return the owner named by the key, or null when the key does not have the revision layout
     */
    public static Owner ownerOf(final String key) {
        final String[] parts = key.split("/", -1);
        return parts.length == 7 && ".revisions".equals(parts[4]) ? new Owner(parts[2], parts[3]) : null;
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
        final Path relative = assetRelative(file);
        if (relative == null || relative.getNameCount() != 7 || !relative.getName(4).toString().equals(".revisions")) {
            return null;
        }
        final String key = relative.toString().replace(File.separatorChar, '/');
        localFile(relative.getName(2).toString(), relative.getName(3).toString(), key);
        return key;
    }

    /**
     * Reads the owner out of an older-layout binary path, {@code i/n/<inode>/<field>/<fileName>} under
     * the asset root. This only parses the path; it does not touch the disk or S3.
     *
     * @param file a binary file that may be stored in the older layout
     * @return the owner named by the path, or null when the file is not in that layout
     */
    public static Owner legacyOwnerOf(final File file) {
        final Path relative = assetRelative(file);
        if (relative == null || relative.getNameCount() != 5) {
            return null;
        }
        final String inode = relative.getName(2).toString();
        final String field = relative.getName(3).toString();
        return isValidOwner(inode, field) && isValidName(relative.getName(4).toString())
                && relative.getName(0).toString().equals(inode.substring(0, 1))
                && relative.getName(1).toString().equals(inode.substring(1, 2))
                ? new Owner(inode, field) : null;
    }

    /** The file's path relative to the asset root, or null when it is outside the root or no root is set. */
    private static Path assetRelative(final File file) {
        final String assetRoot = ConfigUtils.getAssetPath();
        if (assetRoot == null || assetRoot.isBlank()) {
            return null;
        }
        final Path root = Path.of(assetRoot).toAbsolutePath().normalize();
        final Path path = file.toPath().toAbsolutePath().normalize();
        return path.startsWith(root) ? root.relativize(path) : null;
    }
}
