package com.dotcms.storage.binary;

import com.dotcms.storage.AssetStorageFeature;
import com.dotcms.storage.ChainableStoragePersistenceAPIBuilder;
import com.dotcms.storage.FileSystemStoragePersistenceAPIImpl;
import com.dotcms.storage.StoragePersistenceAPI;
import com.dotcms.storage.StoragePersistenceProvider;
import com.dotcms.storage.StorageType;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.ConfigUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Default implementation of {@link BinaryAssetStorageAPI} that delegates to
 * {@link StoragePersistenceAPI} for actual storage operations.
 *
 * <p>Storage path layout: {@code {inode[0]}/{inode[1]}/{inode}/{fieldVarName}/{fileName}}</p>
 *
 * <p>With the S3 feature flag off, the binary-assets group is mapped to the asset root
 * directory (same root used by the legacy filesystem layout). This means the storage paths
 * resolve to the same physical locations as the existing inode-based directory structure.</p>
 */
public class BinaryAssetStorageAPIImpl implements BinaryAssetStorageAPI {

    // Regular I/O remains concurrent, including NFS. Only eviction needs exclusive access.
    private final ReentrantReadWriteLock evictionLock = new ReentrantReadWriteLock();
    private final StoragePersistenceAPI storagePersistenceAPI;
    private final boolean resolveContentReferences;
    private volatile boolean groupInitialized = false;
    private volatile boolean generatedGroupInitialized = false;

    @Override
    public CacheLease acquireCacheLease() {
        if (!AssetStorageFeature.isEnabled()) {
            return BinaryAssetStorageAPI.super.acquireCacheLease();
        }
        // ponytail: one process-wide API lease defers eviction during consumption; per-file leases if cache pressure requires them.
        evictionLock.readLock().lock();
        return evictionLock.readLock()::unlock;
    }

    /**
     * Opens a local file. With the flag on, a missing file whose path is an owned binary or revision
     * under the asset root is restored from durable storage first. Paths outside that layout, or with
     * an invalid inode, field or file name, are opened as they are and are never restored.
     *
     * @param file the file to open; the caller has already checked access to it
     * @return an open stream on the file
     * @throws java.nio.file.NoSuchFileException if the file is not local and could not be restored
     *         because no durable copy exists
     * @throws IOException if the file cannot be opened
     * @throws DotDataException if the restore fails
     */
    @Override
    public InputStream openLocalFile(final File file) throws IOException, DotDataException {
        if (!AssetStorageFeature.isEnabled()) {
            return BinaryAssetStorageAPI.super.openLocalFile(file);
        }
        try (var lease = acquireCacheLease()) {
            if (!file.isFile()) {
                final Path root = new File(ConfigUtils.getAssetPath()).getCanonicalFile().toPath();
                final Path path = file.getCanonicalFile().toPath();
                if (path.startsWith(root)) {
                    final Path relative = root.relativize(path);
                    if (relative.getNameCount() == 5 || relative.getNameCount() == 7) {
                        final String inode = relative.getName(2).toString();
                        final String field = relative.getName(3).toString();
                        final String name = relative.getFileName().toString();
                        if (BinaryAssetReference.isValidOwner(inode, field) && BinaryAssetReference.isValidName(name)
                                && relative.getName(0).toString().equals(inode.substring(0, 1))
                                && relative.getName(1).toString().equals(inode.substring(1, 2))) {
                            final String key = relative.toString().replace(File.separatorChar, '/');
                            if (relative.getNameCount() == 7) {
                                BinaryAssetReference.localFile(inode, field, key); // Validate the revision layout.
                            }
                            ensureGroupExists();
                            storagePersistenceAPI.pullFile(BINARY_ASSETS_GROUP, key);
                        }
                    }
                }
            }
            markRead(file);
            return Files.newInputStream(file.toPath());
        }
    }

    /**
     * Production constructor. Resolves the storage provider based on config.
     *
     * @see #resolveBinaryStorageProvider()
     */
    public BinaryAssetStorageAPIImpl() {
        this(resolveBinaryStorageProvider(), true);
    }

    /** Selects the remote chain only when explicitly enabled; initialization failures must be visible. */
    private static StoragePersistenceAPI resolveBinaryStorageProvider() {
        final StoragePersistenceProvider provider = StoragePersistenceProvider.INSTANCE.get();
        if (!AssetStorageFeature.isEnabled()) {
            return provider.getStorage(StorageType.FILE_SYSTEM);
        }
        return new ChainableStoragePersistenceAPIBuilder()
                .add(provider.getStorage(StorageType.FILE_SYSTEM))
                .add(StoragePersistenceProvider.remoteObjectStorage())
                .get();
    }

    /**
     * Test constructor. Accepts a pre-configured or mocked {@link StoragePersistenceAPI}.
     *
     * @param storagePersistenceAPI the storage provider to delegate to
     */
    public BinaryAssetStorageAPIImpl(final StoragePersistenceAPI storagePersistenceAPI) {
        this(storagePersistenceAPI, false);
    }

    /** Test construction with real content references, or storage-only tests without a CMS database. */
    public BinaryAssetStorageAPIImpl(final StoragePersistenceAPI storagePersistenceAPI,
                                    final boolean resolveContentReferences) {
        this.storagePersistenceAPI = storagePersistenceAPI;
        this.resolveContentReferences = resolveContentReferences;
    }

    @Override
    public boolean backfillBinary(final String inode, final String field, final File file) throws DotDataException {
        if (!AssetStorageFeature.isEnabled()) {
            return false;
        }
        validateParams(inode, field, file.getName());
        evictionLock.readLock().lock();
        try {
            final String revision = BinaryAssetReference.keyOf(file, inode, field);
            final File expected = revision == null ? resolveFile(inode, field, file.getName())
                    : BinaryAssetReference.localFile(inode, field, revision);
            if (!expected.getCanonicalFile().equals(file.getCanonicalFile())) {
                throw new DotDataException("Backfill source does not belong to " + inode + "/" + field);
            }
            return storagePersistenceAPI.backfillFile(BINARY_ASSETS_GROUP,
                    revision == null ? buildFilePath(inode, field, file.getName()) : revision, file);
        } catch (IOException failure) {
            throw new DotDataException("Unable to backfill binary for " + inode + "/" + field, failure);
        } finally {
            evictionLock.readLock().unlock();
        }
    }

    @Override
    public File storeRevision(final String inode, final String field, final String fileName,
                              final File source) throws DotDataException {
        if (!AssetStorageFeature.isEnabled()) {
            throw new IllegalStateException("S3 asset storage is disabled");
        }
        validateParams(inode, field, fileName);
        if (inode.length() < 2) {
            throw new IllegalArgumentException("Invalid binary inode");
        }
        final String key = buildFieldPath(inode, field) + "/.revisions/" + java.util.UUID.randomUUID() + "/" + fileName;
        BinaryAssetReference.localFile(inode, field, key);
        evictionLock.readLock().lock();
        try {
            ensureGroupExists();
            storagePersistenceAPI.pushFile(BINARY_ASSETS_GROUP, key, source, Map.of());
            final File result = getRevisionFile(inode, field, key);
            if (result == null) {
                throw new DotDataException("Stored binary revision could not be retrieved: " + key);
            }
            return result;
        } finally {
            evictionLock.readLock().unlock();
        }
    }

    /**
     * Returns the local file for an immutable revision, restoring it from durable storage when it is not
     * cached, and records the read for cache eviction.
     *
     * @param inode      the contentlet inode that owns the revision
     * @param field      the binary field's variable name
     * @param storageKey the revision key, relative to the asset root
     * @return the local file, or {@code null} when durable storage has no copy
     * @throws IllegalStateException if the flag is off
     * @throws IllegalArgumentException if the owner or key is not valid
     * @throws DotDataException if the restore fails
     */
    @Override
    public File getRevisionFile(final String inode, final String field, final String storageKey) throws DotDataException {
        if (!AssetStorageFeature.isEnabled()) {
            throw new IllegalStateException("S3 asset storage is disabled");
        }
        final File local = BinaryAssetReference.localFile(inode, field, storageKey);
        evictionLock.readLock().lock();
        try {
            if (local.isFile()) {
                return markRead(local);
            }
            ensureGroupExists();
            final File restored = storagePersistenceAPI.pullFile(BINARY_ASSETS_GROUP, storageKey);
            if (restored == null) {
                return null;
            }
            if (!local.isFile()) {
                throw new DotDataException("Binary revision was not restored to the local cache: " + storageKey);
            }
            return markRead(local);
        } finally {
            evictionLock.readLock().unlock();
        }
    }

    /**
     * Returns the local file for a binary named by a content row. An immutable revision is looked up by
     * its storage key; an older-layout binary is pulled from {@code inode/field/fileName}. When the
     * restored file is the snapshot's own path, the snapshot's metadata key is kept on the result.
     *
     * @param inode     the contentlet inode
     * @param field     the binary field's variable name
     * @param reference the binary recorded in the row's {@code contentlet_as_json}
     * @return the local file, or {@code null} when durable storage has no copy
     * @throws DotDataException if the restore fails
     */
    private File getReferencedFile(final String inode, final String field,
                                   final BinaryAssetReference.StoredBinary reference) throws DotDataException {
        final File snapshot = reference.localFile(inode, field);
        if (reference.storageKey() != null) {
            return BinaryAssetReference.preserveMetadata(getRevisionFile(inode, field, reference.storageKey()), snapshot);
        }
        ensureGroupExists();
        return BinaryAssetReference.preserveMetadata(markRead(storagePersistenceAPI.pullFile(BINARY_ASSETS_GROUP,
                buildFilePath(inode, field, reference.fileName()))), snapshot);
    }

    /**
     * Records that a cached file was handed to a reader, so the eviction job, which evicts the
     * least-recently-read files first, keeps it longer. The access time is set explicitly because
     * {@code noatime} mounts never update it on reads; the modification time is left alone because
     * HTTP ETags and Last-Modified headers are built from it. Best effort: a failure is only logged.
     * Does nothing with the flag off.
     *
     * @param file the file being read, or {@code null}
     * @return the same file
     */
    private File markRead(final File file) {
        if (file == null || !AssetStorageFeature.isEnabled()) {
            return file;
        }
        try {
            Files.getFileAttributeView(file.toPath(), BasicFileAttributeView.class)
                    .setTimes(null, FileTime.fromMillis(System.currentTimeMillis()), null);
        } catch (final IOException e) {
            Logger.debug(this, () -> "Unable to record read of cached binary " + file + ": " + e.getMessage());
        }
        return file;
    }

    /**
     * Returns the local file for one binary, restoring it from durable storage when needed.
     *
     * <p>With the flag on and content references enabled, the binary named by the row's
     * {@code contentlet_as_json} is returned. A row without that JSON falls back to the older
     * {@code inode/field/fileName} layout, the same lookup used with the flag off.</p>
     *
     * @param inode        the contentlet inode
     * @param fieldVarName the binary field's variable name
     * @param fileName     the binary's file name
     * @return the local file, or {@code null} when the binary does not exist
     * @throws IllegalArgumentException if any argument is empty or would leave the asset layout
     * @throws DotDataException if the lookup or restore fails
     */
    @Override
    public File getBinaryFile(final String inode, final String fieldVarName,
                              final String fileName) throws DotDataException {
        try (var lease = acquireCacheLease()) {
            validateParams(inode, fieldVarName, fileName);
            if (resolveContentReferences && AssetStorageFeature.isEnabled()) {
                final String json = BinaryAssetReference.contentJson(inode);
                if (json != null) {
                    final var reference = BinaryAssetReference.fromContentField(json, inode, fieldVarName);
                    return reference == null || !fileName.equals(reference.fileName()) ? null
                            : getReferencedFile(inode, fieldVarName, reference);
                }
            }
            ensureGroupExists();

            final String path = buildFilePath(inode, fieldVarName, fileName);

            try {
                return markRead(storagePersistenceAPI.pullFile(BINARY_ASSETS_GROUP, path));
            } catch (final IllegalArgumentException e) {
                // File does not exist — return null to match existing Contentlet.getBinary() contract
                Logger.debug(this, () -> String.format(
                        "Binary not found for inode '%s', field '%s', file '%s': %s",
                        inode, fieldVarName, fileName, e.getMessage()));
                return null;
            }
        }
    }

    /**
     * Opens a stream on one binary. The cache lease is held only until the stream is open.
     *
     * @param inode        the contentlet inode
     * @param fieldVarName the binary field's variable name
     * @param fileName     the binary's file name
     * @return an open stream, or {@code null} when the binary does not exist
     * @throws DotDataException if the lookup or restore fails
     */
    @Override
    public InputStream getBinaryStream(final String inode, final String fieldVarName,
                                       final String fileName) throws DotDataException {
        try (var lease = acquireCacheLease()) {
            final File file = getBinaryFile(inode, fieldVarName, fileName);
            if (file == null) {
                return null;
            }

            try {
                return new FileInputStream(file);
            } catch (final FileNotFoundException e) {
                Logger.debug(this, () -> String.format(
                        "Binary file disappeared between pullFile and stream open for inode '%s', field '%s', file '%s'",
                        inode, fieldVarName, fileName));
                return null;
            }
        }
    }

    /**
     * Returns the local file for a field's binary when the file name is not known.
     *
     * <p>With the flag on and content references enabled, the binary named by the row's
     * {@code contentlet_as_json} is returned. A row without that JSON falls back to listing the
     * field directory locally and then in durable storage, the same lookup used with the flag off.</p>
     *
     * @param inode        the contentlet inode
     * @param fieldVarName the binary field's variable name
     * @return the local file, or {@code null} when the field has no binary
     * @throws IllegalArgumentException if an argument is empty or would leave the asset layout
     * @throws DotDataException if the lookup or restore fails
     */
    @Override
    public File getBinaryFile(final String inode,
                              final String fieldVarName) throws DotDataException {
        try (var lease = acquireCacheLease()) {
            validateOwner(inode, fieldVarName);

            if (resolveContentReferences && AssetStorageFeature.isEnabled()) {
                final String json = BinaryAssetReference.contentJson(inode);
                if (json != null) {
                    final var reference = BinaryAssetReference.fromContentField(json, inode, fieldVarName);
                    return reference == null ? null : getReferencedFile(inode, fieldVarName, reference);
                }
            }
            // Strategy 1: Try local FS directory listing (fast path — works when cached).
            // Covers flag-off filesystem/NFS mode and a warm S3 cache.
            final File fieldDir = resolveFieldDirectory(inode, fieldVarName);
            if (fieldDir != null && fieldDir.exists() && fieldDir.isDirectory()) {
                final File[] files = fieldDir.listFiles(file ->
                        file.isFile() && !file.getName().contains(Config.GENERATED_FILE)
                                && !file.getName().startsWith("."));

                if (files != null && files.length > 0) {
                    return markRead(files[0]);
                }
            }

            // Strategy 2: If not found locally, discover filename through the chain.
            // Covers a cold S3 cache (file in S3 but not local FS).
            if (!(storagePersistenceAPI instanceof FileSystemStoragePersistenceAPIImpl)) {
                ensureGroupExists();
                final String fieldPath = buildFieldPath(inode, fieldVarName);
                final List<String> objectPaths = storagePersistenceAPI.listObjectPaths(
                        BINARY_ASSETS_GROUP, fieldPath);

                for (final String objectPath : objectPaths) {
                    if (!objectPath.startsWith(fieldPath + File.separator)
                            || objectPath.substring(fieldPath.length() + 1).contains(File.separator)) {
                        continue;
                    }
                    // Extract filename from the full object path
                    final String fileName = objectPath.substring(
                            objectPath.lastIndexOf(File.separator) + 1);
                    // Skip generated and hidden files (same filter as local FS listing)
                    if (fileName.contains(Config.GENERATED_FILE) || fileName.startsWith(".")) {
                        continue;
                    }
                    // Pull through chain (downloads from S3, backfills to FS cache)
                    return getBinaryFile(inode, fieldVarName, fileName);
                }
            }

            return null;
        }
    }

    /**
     * Opens a stream on a field's binary when the file name is not known. The cache lease is held
     * only until the stream is open.
     *
     * @param inode        the contentlet inode
     * @param fieldVarName the binary field's variable name
     * @return an open stream, or {@code null} when the field has no binary
     * @throws DotDataException if the lookup or restore fails
     */
    @Override
    public InputStream getBinaryStream(final String inode,
                                       final String fieldVarName) throws DotDataException {
        try (var lease = acquireCacheLease()) {
            final File file = getBinaryFile(inode, fieldVarName);
            if (file == null) {
                return null;
            }

            try {
                return new FileInputStream(file);
            } catch (final FileNotFoundException e) {
                Logger.debug(this, () -> String.format(
                        "Binary file disappeared between lookup and stream open for inode '%s', field '%s'",
                        inode, fieldVarName));
                return null;
            }
        }
    }

    /**
     * Resolves the field directory on the local filesystem.
     *
     * <p>Used for the local cache fast path; cold-cache discovery lists storage providers.</p>
     *
     * @param inode        the contentlet inode
     * @param fieldVarName the field's velocity variable name
     * @return a File representing the field directory
     */
    private File resolveFieldDirectory(final String inode, final String fieldVarName) {

        final String fieldPath = buildFieldPath(inode, fieldVarName);
        return new File(ConfigUtils.getAssetPath(), fieldPath);
    }

    /**
     * Resolves the full file path on the local filesystem.
     *
     * <p>Used to check that a backfill source is the binary it claims to be.</p>
     *
     * @param inode        the contentlet inode
     * @param fieldVarName the field's velocity variable name
     * @param fileName     the actual filename
     * @return a File representing the full file path
     */
    private File resolveFile(final String inode, final String fieldVarName,
                             final String fileName) {

        final String filePath = buildFilePath(inode, fieldVarName, fileName);
        return new File(ConfigUtils.getAssetPath(), filePath);
    }

    @Override
    public void deleteBinaryPaths(final String inode, final String field, final List<String> paths)
            throws DotDataException {
        if (!AssetStorageFeature.isEnabled()) {
            throw new DotDataException("S3 asset storage is disabled");
        }
        new BinaryAssetReference.StoredBinary(null, null, "inventory").localFile(inode, field);
        final String prefix = buildFieldPath(inode, field) + "/";
        for (String path : paths) {
            if (!path.startsWith(prefix) || path.contains("\\")
                    || !Path.of(path).normalize().toString().equals(path)) {
                throw new IllegalArgumentException("Binary cleanup path escapes its field");
            }
        }
        try (var lease = acquireCacheLease()) {
            ensureGroupExists();
            for (String path : paths) {
                storagePersistenceAPI.deleteObjectAndReferences(BINARY_ASSETS_GROUP, path);
                if (storagePersistenceAPI.existsObject(BINARY_ASSETS_GROUP, path)) {
                    throw new DotDataException("Binary remains after deletion: " + path);
                }
            }
        }
    }

    @Override
    public List<String> listBinaryPaths(final String inode) throws DotDataException {
        if (inode == null || !inode.matches("[A-Za-z0-9_-]{2,}")) {
            throw new IllegalArgumentException("Invalid binary inode");
        }
        ensureGroupExists();
        return storagePersistenceAPI.listObjectPaths(BINARY_ASSETS_GROUP,
                inode.charAt(0) + "/" + inode.charAt(1) + "/" + inode + "/");
    }

    /**
     * Builds the full file path: {@code {inode[0]}/{inode[1]}/{inode}/{fieldVarName}/{fileName}}
     */
    private String buildFilePath(final String inode, final String fieldVarName,
                                 final String fileName) {

        return inode.charAt(0) + File.separator
                + inode.charAt(1) + File.separator
                + inode + File.separator
                + fieldVarName + File.separator
                + fileName;
    }

    /**
     * Builds the field directory path: {@code {inode[0]}/{inode[1]}/{inode}/{fieldVarName}}
     */
    private String buildFieldPath(final String inode, final String fieldVarName) {

        return inode.charAt(0) + File.separator
                + inode.charAt(1) + File.separator
                + inode + File.separator
                + fieldVarName;
    }

    /**
     * Validates that inode, fieldVarName, and fileName are set and are safe path segments, so the
     * built path stays inside {@code inode/field}.
     *
     * @param inode        the contentlet inode
     * @param fieldVarName the binary field's variable name
     * @param fileName     the binary's file name
     * @throws IllegalArgumentException if any value is empty, the inode has characters other than
     *         letters, digits, underscores and hyphens, or a name contains a separator or is a dot segment
     */
    private void validateParams(final String inode, final String fieldVarName,
                                final String fileName) {

        validateOwner(inode, fieldVarName);
        if (!UtilMethods.isSet(fileName)) {
            throw new IllegalArgumentException("fileName must not be null or empty");
        }
        if (!BinaryAssetReference.isValidName(fileName)) {
            throw new IllegalArgumentException("Invalid binary file name");
        }
    }

    /**
     * Validates that an inode and field are set and are safe path segments, using the same rules as
     * {@link BinaryAssetReference}.
     *
     * @param inode        the contentlet inode
     * @param fieldVarName the binary field's variable name
     * @throws IllegalArgumentException if either value is empty or would leave the asset layout
     */
    private void validateOwner(final String inode, final String fieldVarName) {
        if (!UtilMethods.isSet(inode)) {
            throw new IllegalArgumentException("inode must not be null or empty");
        }
        if (!UtilMethods.isSet(fieldVarName)) {
            throw new IllegalArgumentException("fieldVarName must not be null or empty");
        }
        if (!BinaryAssetReference.isValidOwner(inode, fieldVarName)) {
            throw new IllegalArgumentException("Invalid binary owner");
        }
    }

    /**
     * Ensures the binary-assets group is properly initialized. Thread-safe via double-checked
     * locking on the volatile {@link #groupInitialized} flag.
     *
     * <p>For FILE_SYSTEM mode, this maps the binary-assets group to the asset root directory
     * via {@link FileSystemStoragePersistenceAPIImpl#addGroupMapping(String, File)}.</p>
     *
     * <p>The chain forwards the directory mapping to its actual filesystem provider and
     * creates the binary group in the configured S3 bucket. The bucket must already exist.</p>
     */
    private void ensureGroupExists() throws DotDataException {
        if (!groupInitialized) {
            synchronized (this) {
                if (!groupInitialized) {
                    if (!storagePersistenceAPI.existsGroup(BINARY_ASSETS_GROUP)) {
                        final File assetsDir = new File(ConfigUtils.getAssetPath());
                        if (!assetsDir.exists() && !assetsDir.mkdirs()) {
                            throw new DotDataException("Unable to create binary asset directory " + assetsDir);
                        }
                        storagePersistenceAPI.createGroup(BINARY_ASSETS_GROUP,
                                Map.of(FileSystemStoragePersistenceAPIImpl.GROUP_DIRECTORY, assetsDir));
                    }
                    groupInitialized = true;
                }
            }
        }
    }

    /**
     * Maps the generated-assets group to the dotGenerated directory the first time a rendition call
     * needs it. Later calls only read the volatile flag, so they do not contend on this object's monitor.
     *
     * @throws DotDataException if the directory cannot be created or the group cannot be registered
     */
    private void ensureGeneratedGroupExists() throws DotDataException {
        if (generatedGroupInitialized) {
            return;
        }
        synchronized (this) {
            if (!generatedGroupInitialized) {
                final File directory = new File(ConfigUtils.getDotGeneratedPath());
                if (!directory.isDirectory() && !directory.mkdirs()) {
                    throw new DotDataException("Unable to create generated asset directory " + directory);
                }
                storagePersistenceAPI.createGroup(GENERATED_ASSETS_GROUP,
                        Map.of(FileSystemStoragePersistenceAPIImpl.GROUP_DIRECTORY, directory));
                generatedGroupInitialized = true;
            }
        }
    }

    /**
     * Reports whether a file name has the form of a finished rendition,
     * {@code dotGenerated_<filter>_<hash>.<extension>}. Temporary and partial outputs do not match.
     *
     * @param file the file whose name is checked
     * @return true when the name is a completed rendition name
     */
    static boolean isCompletedRendition(final File file) {
        return file.getName().matches("dotGenerated_[a-z0-9]*_[a-f0-9]{1,32}\\.[a-zA-Z0-9]+");
    }

    /**
     * Returns the storage key of a completed rendition: its path relative to the dotGenerated root,
     * which must be {@code <char>/<char>/<inode>/<rendition>} with the inode starting with those two
     * characters.
     *
     * @param file the rendition file
     * @return the rendition's path relative to the dotGenerated root
     * @throws IllegalArgumentException if the file is outside dotGenerated or is not a completed rendition
     * @throws DotDataException if the path cannot be resolved
     */
    private String generatedPath(final File file) throws DotDataException {
        try {
            final Path root = new File(ConfigUtils.getDotGeneratedPath()).getCanonicalFile().toPath();
            final Path path = file.getCanonicalFile().toPath();
            if (!path.startsWith(root)) {
                throw new IllegalArgumentException("Rendition is outside dotGenerated: " + file);
            }
            final Path relative = root.relativize(path);
            if (relative.getNameCount() != 4 || relative.getName(0).toString().length() != 1
                    || relative.getName(1).toString().length() != 1
                    || !relative.getName(2).toString().matches("[A-Za-z0-9_-]{2,}")
                    || !relative.getName(2).toString().startsWith(relative.getName(0).toString() + relative.getName(1))
                    || !isCompletedRendition(file)) {
                throw new IllegalArgumentException("Not a completed rendition: " + file);
            }
            return relative.toString();
        } catch (final IOException e) {
            throw new DotDataException("Unable to resolve rendition " + file, e);
        }
    }

    /**
     * Returns a completed rendition at its local dotGenerated path, restoring it from durable storage
     * when it is not cached, and records the read for cache eviction. Filesystem/NFS mode returns the
     * local file only if it exists.
     *
     * @param localFile the rendition's local path
     * @return the local file, or {@code null} when no copy exists
     * @throws IllegalArgumentException if the file is not a completed rendition under dotGenerated
     * @throws DotDataException if the restore fails
     */
    @Override
    public File getGeneratedFile(final File localFile) throws DotDataException {
        if (!AssetStorageFeature.isEnabled() || storagePersistenceAPI instanceof FileSystemStoragePersistenceAPIImpl) {
            return BinaryAssetStorageAPI.super.getGeneratedFile(localFile);
        }
        evictionLock.readLock().lock();
        try {
            final String path = generatedPath(localFile);
            ensureGeneratedGroupExists();
            return markRead(storagePersistenceAPI.pullFile(GENERATED_ASSETS_GROUP, path));
        } finally {
            evictionLock.readLock().unlock();
        }
    }

    @Override
    public void storeGeneratedFile(final File localFile) throws DotDataException {
        if (!AssetStorageFeature.isEnabled() || storagePersistenceAPI instanceof FileSystemStoragePersistenceAPIImpl) {
            return;
        }
        evictionLock.readLock().lock();
        try {
            final String path = generatedPath(localFile);
            ensureGeneratedGroupExists();
            // Only completed outputs reach this method; warm reads do not republish them.
            if (!storagePersistenceAPI.hasDurableCopy(GENERATED_ASSETS_GROUP, path, localFile)) {
                storagePersistenceAPI.pushFile(GENERATED_ASSETS_GROUP, path, localFile, Map.of());
            }
        } finally {
            evictionLock.readLock().unlock();
        }
    }

    @Override
    public void deleteGeneratedFiles(final String inode) throws DotDataException {
        if (!AssetStorageFeature.isEnabled() || storagePersistenceAPI instanceof FileSystemStoragePersistenceAPIImpl) {
            return; // Existing filesystem/NFS thumbnail cleanup remains responsible for this mode.
        }
        if (inode == null || !inode.matches("[a-zA-Z0-9_-]{2,}")) {
            throw new IllegalArgumentException("Invalid inode");
        }
        evictionLock.readLock().lock();
        try {
            ensureGeneratedGroupExists();
            final String prefix = inode.charAt(0) + "/" + inode.charAt(1) + "/" + inode + "/";
            for (final String path : storagePersistenceAPI.listObjectPaths(GENERATED_ASSETS_GROUP, prefix)) {
                if (path.startsWith(prefix) && !path.substring(prefix.length()).contains("/")
                        && isCompletedRendition(new File(path))) {
                    storagePersistenceAPI.deleteObjectAndReferences(GENERATED_ASSETS_GROUP, path);
                }
            }
        } finally {
            evictionLock.readLock().unlock();
        }
    }

    /**
     * Migrates an inode's completed renditions to the durable layout. Each rendition found in durable
     * storage is restored locally and backfilled at its current key. Filesystem/NFS mode does nothing.
     *
     * @param inode the contentlet inode
     * @throws IllegalArgumentException if the inode is not a valid inode
     * @throws DotDataException if a rendition has no durable copy or the backfill fails
     */
    @Override
    public void backfillGeneratedFiles(final String inode) throws DotDataException {
        if (!AssetStorageFeature.isEnabled() || storagePersistenceAPI instanceof FileSystemStoragePersistenceAPIImpl) {
            return;
        }
        if (inode == null || !inode.matches("[a-zA-Z0-9_-]{2,}")) {
            throw new IllegalArgumentException("Invalid inode");
        }
        try (var lease = acquireCacheLease()) {
            ensureGeneratedGroupExists();
            final String prefix = inode.charAt(0) + "/" + inode.charAt(1) + "/" + inode + "/";
            for (String path : storagePersistenceAPI.listObjectPaths(GENERATED_ASSETS_GROUP, prefix)) {
                if (!path.startsWith(prefix) || path.substring(prefix.length()).contains("/")
                        || !isCompletedRendition(new File(path))) {
                    continue;
                }
                final File local = new File(ConfigUtils.getDotGeneratedPath(), path);
                generatedPath(local); // Apply the same ownership/path validation used by normal rendering.
                final File restored = storagePersistenceAPI.pullFile(GENERATED_ASSETS_GROUP, path);
                if (restored == null || !storagePersistenceAPI.backfillFile(GENERATED_ASSETS_GROUP, path, restored)) {
                    throw new DotDataException("No durable rendition copy for " + path);
                }
            }
        }
    }

    /** Only owned originals/revisions and completed renditions belong to the eviction budget. */
    static boolean isEvictableAssetPath(final Path relative, final boolean generated) {
        final int depth = relative.getNameCount();
        if (relative.isAbsolute() || (generated ? depth != 4 : depth != 5 && depth != 7)) {
            return false;
        }
        final String inode = relative.getName(2).toString();
        if (relative.getName(0).toString().length() != 1 || relative.getName(1).toString().length() != 1
                || !inode.matches("[A-Za-z0-9_-]{2,}")
                || !inode.startsWith(relative.getName(0).toString() + relative.getName(1))) {
            return false;
        }
        if (generated) {
            return isCompletedRendition(relative.toFile());
        }
        final String field = relative.getName(3).toString();
        final String name = relative.getFileName().toString();
        if (field.isBlank() || field.startsWith(".") || field.contains("\\") || field.equals("metaData")
                || name.isBlank() || name.startsWith(".") || name.contains("\\")
                || name.matches("dotGenerated_[a-z0-9]*_[a-f0-9]{1,32}\\..*")) {
            return false;
        }
        return depth == 5 || relative.getName(4).toString().equals(".revisions")
                && relative.getName(5).toString().matches("[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}");
    }

    /**
     * Deletes a local cache file when durable storage holds a verified copy of the same bytes.
     *
     * <p>Verification can download the object from S3, so it runs with no lock held and never blocks
     * readers. If a lease or storage operation is active when the method starts, verification is
     * skipped entirely and the file is kept, because the final lock attempt would fail anyway. The
     * file's attributes are recorded before verification. Afterwards the write lock is taken without
     * waiting, only to repeat the path checks, confirm that the size, modification time and file key
     * are unchanged, and delete. If a lease or storage operation is active at that moment, or the file
     * changed, the file is kept.</p>
     *
     * @param file a file under the asset root or the dotGenerated root
     * @return true if the file was deleted, false if it was kept for any reason
     * @throws DotDataException if the file cannot be inspected or the durable copy cannot be verified
     */
    @Override
    public boolean evictLocalFile(final File file) throws DotDataException {
        if (!AssetStorageFeature.isEnabled()) {
            return false;
        }
        try {
            final EvictionCandidate candidate = evictionCandidate(file);
            if (candidate == null) {
                return false;
            }
            // Verification can download the whole object; skip it when the final tryLock would fail anyway.
            if (evictionLock.getReadLockCount() > 0) {
                return false;
            }
            final BasicFileAttributes before = Files.readAttributes(candidate.path(), BasicFileAttributes.class,
                    LinkOption.NOFOLLOW_LINKS);
            if (!before.isRegularFile()
                    || !storagePersistenceAPI.hasDurableCopy(candidate.group(), candidate.key(), file)) {
                return false;
            }
            // Never queue an eviction writer behind a long response: that would also stall new readers.
            if (!evictionLock.writeLock().tryLock()) {
                return false;
            }
            try {
                if (!candidate.equals(evictionCandidate(file))) {
                    return false;
                }
                final BasicFileAttributes after = Files.readAttributes(candidate.path(), BasicFileAttributes.class,
                        LinkOption.NOFOLLOW_LINKS);
                if (before.size() != after.size() || !before.lastModifiedTime().equals(after.lastModifiedTime())
                        || !Objects.equals(before.fileKey(), after.fileKey())) {
                    return false;
                }
                return Files.deleteIfExists(candidate.path());
            } catch (final NoSuchFileException removed) {
                return false; // Deleted by another operation while it was being verified.
            } finally {
                evictionLock.writeLock().unlock();
            }
        } catch (final IOException e) {
            throw new DotDataException("Unable to evict binary cache file " + file, e);
        }
    }

    /** The real path of an evictable file, with the storage group and key that hold its durable copy. */
    private record EvictionCandidate(Path path, String group, String key) { }

    /**
     * Applies the eviction path checks: the supplied file and every directory below the configured
     * root must not be symbolic links, and the real path must be an owned binary or completed
     * rendition under its root.
     *
     * @param file the file proposed for eviction
     * @return the candidate, or {@code null} when the file must not be evicted
     * @throws IOException if the path cannot be resolved
     */
    private EvictionCandidate evictionCandidate(final File file) throws IOException {
        if (Files.isSymbolicLink(file.toPath())) {
            return null;
        }
        final Path path = file.toPath().toRealPath();
        final String generatedRoot = ConfigUtils.getDotGeneratedPath();
        final boolean generated = generatedRoot != null
                && path.startsWith(new File(generatedRoot).getCanonicalFile().toPath());
        final Path root = Path.of(generated ? generatedRoot : ConfigUtils.getAssetPath()).toRealPath();
        final String group = generated ? GENERATED_ASSETS_GROUP : BINARY_ASSETS_GROUP;
        final Path configuredRoot = Path.of(generated ? generatedRoot : ConfigUtils.getAssetPath())
                .toAbsolutePath().normalize();
        final Path suppliedPath = file.toPath().toAbsolutePath().normalize();
        // Providers may return the canonical path when the configured root itself is an alias.
        final Path suppliedRoot = suppliedPath.startsWith(configuredRoot) ? configuredRoot : root;
        if (!suppliedPath.startsWith(suppliedRoot)) {
            return null;
        }
        Path descendant = suppliedRoot;
        for (final Path component : suppliedRoot.relativize(suppliedPath)) {
            descendant = descendant.resolve(component);
            if (Files.isSymbolicLink(descendant)) {
                return null;
            }
        }
        if (!path.startsWith(root) || !isEvictableAssetPath(root.relativize(path), generated)) {
            return null;
        }
        return new EvictionCandidate(path, group, root.relativize(path).toString());
    }

}
