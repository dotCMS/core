package com.dotcms.storage.binary;

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
import com.liferay.util.FileUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.LinkOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Default implementation of {@link BinaryAssetStorageAPI} that delegates to
 * {@link StoragePersistenceAPI} for actual storage operations.
 *
 * <p>Storage path layout: {@code {inode[0]}/{inode[1]}/{inode}/{fieldVarName}/{fileName}}</p>
 *
 * <p>In the default FILE_SYSTEM mode, the binary-assets group is mapped to the asset root
 * directory (same root used by the legacy filesystem layout). This means the storage paths
 * resolve to the same physical locations as the existing inode-based directory structure.</p>
 */
public class BinaryAssetStorageAPIImpl implements BinaryAssetStorageAPI {

    /**
     * Config property to select binary asset storage mode.
     * Values: "FILE_SYSTEM" (default) or "BINARY_CHAIN" (local FS cache → S3 durable).
     */
    static final String BINARY_ASSET_STORAGE_TYPE_PROP = "BINARY_ASSET_STORAGE_TYPE";

    // Regular I/O remains concurrent, including NFS. Only eviction needs exclusive access.
    private final ReentrantReadWriteLock evictionLock = new ReentrantReadWriteLock();
    private final StoragePersistenceAPI storagePersistenceAPI;
    private final boolean resolveContentReferences;
    private volatile boolean groupInitialized = false;
    private volatile boolean generatedGroupInitialized = false;

    @Override
    public CacheLease acquireCacheLease() {
        if (!com.dotcms.storage.AssetStorageFeature.isEnabled()) {
            return BinaryAssetStorageAPI.super.acquireCacheLease();
        }
        // ponytail: one process-wide API lease defers eviction during consumption; per-file leases if cache pressure requires them.
        evictionLock.readLock().lock();
        return evictionLock.readLock()::unlock;
    }

    @Override
    public InputStream openLocalFile(final File file) throws IOException, DotDataException {
        if (!com.dotcms.storage.AssetStorageFeature.isEnabled()) {
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
                        validateParams(inode, field, name);
                        if (inode.length() >= 2 && relative.getName(0).toString().equals(inode.substring(0, 1))
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

    /** Selects the S3 chain only when explicitly enabled; initialization failures must be visible. */
    private static StoragePersistenceAPI resolveBinaryStorageProvider() {
        final StoragePersistenceProvider provider = StoragePersistenceProvider.INSTANCE.get();
        if (!com.dotcms.storage.AssetStorageFeature.isEnabled()) {
            return provider.getStorage(StorageType.FILE_SYSTEM);
        }
        return new ChainableStoragePersistenceAPIBuilder()
                .add(provider.getStorage(StorageType.FILE_SYSTEM))
                .add(com.dotcms.storage.AmazonS3StoragePersistenceAPIImpl.withPlainPaths())
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
        if (!com.dotcms.storage.AssetStorageFeature.isEnabled()) {
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
        if (!com.dotcms.storage.AssetStorageFeature.isEnabled()) {
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

    @Override
    public File getRevisionFile(final String inode, final String field, final String storageKey) throws DotDataException {
        if (!com.dotcms.storage.AssetStorageFeature.isEnabled()) {
            throw new IllegalStateException("S3 asset storage is disabled");
        }
        final File local = BinaryAssetReference.localFile(inode, field, storageKey);
        evictionLock.readLock().lock();
        try {
            if (local.isFile()) {
                return local;
            }
            ensureGroupExists();
            final File restored = storagePersistenceAPI.pullFile(BINARY_ASSETS_GROUP, storageKey);
            if (restored == null) {
                return null;
            }
            if (!local.isFile()) {
                throw new DotDataException("Binary revision was not restored to the local cache: " + storageKey);
            }
            return local;
        } finally {
            evictionLock.readLock().unlock();
        }
    }

    private File getReferencedFile(final String inode, final String field,
                                   final BinaryAssetReference.StoredBinary reference) throws DotDataException {
        final File snapshot = reference.localFile(inode, field);
        if (reference.storageKey() != null) {
            return BinaryAssetReference.preserveMetadata(getRevisionFile(inode, field, reference.storageKey()), snapshot);
        }
        ensureGroupExists();
        return BinaryAssetReference.preserveMetadata(storagePersistenceAPI.pullFile(BINARY_ASSETS_GROUP,
                buildFilePath(inode, field, reference.fileName())), snapshot);
    }

    @Override
    public File getBinaryFile(final String inode, final String fieldVarName,
                              final String fileName) throws DotDataException {
        evictionLock.readLock().lock();
        try {
            validateParams(inode, fieldVarName, fileName);
            if (resolveContentReferences && com.dotcms.storage.AssetStorageFeature.isEnabled()) {
                final var reference = BinaryAssetReference.findStored(inode, fieldVarName);
                return reference == null || !fileName.equals(reference.fileName()) ? null
                        : getReferencedFile(inode, fieldVarName, reference);
            }
            ensureGroupExists();

            final String path = buildFilePath(inode, fieldVarName, fileName);

            try {
                return storagePersistenceAPI.pullFile(BINARY_ASSETS_GROUP, path);
            } catch (final IllegalArgumentException e) {
                // File does not exist — return null to match existing Contentlet.getBinary() contract
                Logger.debug(this, () -> String.format(
                        "Binary not found for inode '%s', field '%s', file '%s': %s",
                        inode, fieldVarName, fileName, e.getMessage()));
                return null;
            }
        } finally {
            evictionLock.readLock().unlock();
        }
    }

    @Override
    public InputStream getBinaryStream(final String inode, final String fieldVarName,
                                       final String fileName) throws DotDataException {
        evictionLock.readLock().lock();
        try {
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
        } finally {
            evictionLock.readLock().unlock();
        }
    }

    @Override
    public File getBinaryFile(final String inode,
                              final String fieldVarName) throws DotDataException {
        evictionLock.readLock().lock();
        try {
            if (!UtilMethods.isSet(inode)) {
                throw new IllegalArgumentException("inode must not be null or empty");
            }
            if (!UtilMethods.isSet(fieldVarName)) {
                throw new IllegalArgumentException("fieldVarName must not be null or empty");
            }

            if (resolveContentReferences && com.dotcms.storage.AssetStorageFeature.isEnabled()) {
                final var reference = BinaryAssetReference.findStored(inode, fieldVarName);
                return reference == null ? null : getReferencedFile(inode, fieldVarName, reference);
            }
            // Strategy 1: Try local FS directory listing (fast path — works when cached).
            // Covers FILE_SYSTEM mode and BINARY_CHAIN with warm cache.
            final File fieldDir = resolveFieldDirectory(inode, fieldVarName);
            if (fieldDir != null && fieldDir.exists() && fieldDir.isDirectory()) {
                final File[] files = fieldDir.listFiles(file ->
                        file.isFile() && !file.getName().contains(Config.GENERATED_FILE)
                                && !file.getName().startsWith("."));

                if (files != null && files.length > 0) {
                    return files[0];
                }
            }

            // Strategy 2: If not found locally, discover filename through the chain.
            // Covers BINARY_CHAIN with cold cache (file in S3 but not local FS).
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
        } finally {
            evictionLock.readLock().unlock();
        }
    }

    @Override
    public InputStream getBinaryStream(final String inode,
                                       final String fieldVarName) throws DotDataException {
        evictionLock.readLock().lock();
        try {
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
        } finally {
            evictionLock.readLock().unlock();
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
     * <p>Used for filesystem-only hard-link operations.</p>
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
    public void storeBinary(final String inode, final String fieldVarName,
                            final String fileName, final File sourceFile,
                            final boolean hardLink) throws DotDataException {
        evictionLock.readLock().lock();
        try {
            validateParams(inode, fieldVarName, fileName);
            ensureGroupExists();

            if (sourceFile == null || !sourceFile.exists()) {
                throw new IllegalArgumentException(
                        String.format("Source file is null or does not exist for inode '%s', field '%s', file '%s'",
                                inode, fieldVarName, fileName));
            }

            if (hardLink && storagePersistenceAPI instanceof FileSystemStoragePersistenceAPIImpl) {
                // FileUtil supports hard-links; the filesystem provider's regular copy does not.
                final File dest = resolveFile(inode, fieldVarName, fileName);
                if (dest.getParentFile() != null && !dest.getParentFile().exists()) {
                    dest.getParentFile().mkdirs();
                }
                try {
                    FileUtil.copyFile(sourceFile, dest, true);
                } catch (final IOException e) {
                    throw new DotDataException(String.format(
                            "Failed to store binary with hard-link for inode '%s', field '%s', file '%s': %s",
                            inode, fieldVarName, fileName, e.getMessage()), e);
                }
            } else {
                // Non-FILE_SYSTEM mode or hardLink=false: delegate to existing storeBinary
                storeBinary(inode, fieldVarName, fileName, sourceFile);
            }
        } finally {
            evictionLock.readLock().unlock();
        }
    }

    @Override
    public void storeBinary(final String inode, final String fieldVarName,
                            final String fileName, final File sourceFile) throws DotDataException {
        evictionLock.readLock().lock();
        try {
            validateParams(inode, fieldVarName, fileName);
            ensureGroupExists();

            if (sourceFile == null || !sourceFile.exists()) {
                throw new IllegalArgumentException(
                        String.format("Source file is null or does not exist for inode '%s', field '%s', file '%s'",
                                inode, fieldVarName, fileName));
            }

            final String path = buildFilePath(inode, fieldVarName, fileName);
            storagePersistenceAPI.pushFile(BINARY_ASSETS_GROUP, path, sourceFile,
                    Map.<String, Serializable>of());
        } finally {
            evictionLock.readLock().unlock();
        }
    }

    @Override
    public void copyBinary(final String sourceInode, final String destInode,
                           final String fieldVarName, final String fileName) throws DotDataException {
        evictionLock.readLock().lock();
        try {
            validateParams(sourceInode, fieldVarName, fileName);
            if (!UtilMethods.isSet(destInode)) {
                throw new IllegalArgumentException("destInode must not be null or empty");
            }
            ensureGroupExists();

            if (storagePersistenceAPI instanceof FileSystemStoragePersistenceAPIImpl) {
                // Bypass pullFile+pushFile for FILE_SYSTEM mode:
                // pushFile uses Apache Commons copy (no hard-links). FileUtil.copyFile preserves case
                // and uses config-driven hard-link default (CONTENT_VERSION_HARD_LINK).
                final File source = resolveFile(sourceInode, fieldVarName, fileName);
                final File dest = resolveFile(destInode, fieldVarName, fileName);
                if (dest.getParentFile() != null && !dest.getParentFile().exists()) {
                    dest.getParentFile().mkdirs();
                }
                try {
                    FileUtil.copyFile(source, dest);
                } catch (final IOException e) {
                    throw new DotDataException(String.format(
                            "Failed to copy binary from inode '%s' to '%s', field '%s', file '%s': %s",
                            sourceInode, destInode, fieldVarName, fileName, e.getMessage()), e);
                }
            } else {
                // Non-FILE_SYSTEM mode: use storage API
                final String sourcePath = buildFilePath(sourceInode, fieldVarName, fileName);
                final File sourceFile = resolveContentReferences && com.dotcms.storage.AssetStorageFeature.isEnabled()
                        ? getBinaryFile(sourceInode, fieldVarName, fileName)
                        : storagePersistenceAPI.pullFile(BINARY_ASSETS_GROUP, sourcePath);

                final String destPath = buildFilePath(destInode, fieldVarName, fileName);
                storagePersistenceAPI.pushFile(BINARY_ASSETS_GROUP, destPath, sourceFile,
                        Map.<String, Serializable>of());
            }
        } finally {
            evictionLock.readLock().unlock();
        }
    }

    @Override
    public void deleteBinary(final String inode,
                             final String fieldVarName) throws DotDataException {
        evictionLock.readLock().lock();
        try {
            if (!UtilMethods.isSet(inode)) {
                throw new IllegalArgumentException("inode must not be null or empty");
            }
            if (!UtilMethods.isSet(fieldVarName)) {
                throw new IllegalArgumentException("fieldVarName must not be null or empty");
            }
            ensureGroupExists();

            final String fieldPath = buildFieldPath(inode, fieldVarName);
            for (final String path : storagePersistenceAPI.listObjectPaths(BINARY_ASSETS_GROUP, fieldPath)) {
                storagePersistenceAPI.deleteObjectAndReferences(BINARY_ASSETS_GROUP, path);
            }
            storagePersistenceAPI.deleteObjectAndReferences(BINARY_ASSETS_GROUP, fieldPath);
        } finally {
            evictionLock.readLock().unlock();
        }
        if (com.dotcms.storage.AssetStorageFeature.isEnabled()) {
            deleteGeneratedFiles(inode);
        }
    }

    @Override
    public void deleteBinaryPaths(final String inode, final String field, final List<String> paths)
            throws DotDataException {
        if (!com.dotcms.storage.AssetStorageFeature.isEnabled()) {
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
    public boolean existsBinary(final String inode,
                                final String fieldVarName) throws DotDataException {

        if (!UtilMethods.isSet(inode)) {
            throw new IllegalArgumentException("inode must not be null or empty");
        }
        if (!UtilMethods.isSet(fieldVarName)) {
            throw new IllegalArgumentException("fieldVarName must not be null or empty");
        }
        ensureGroupExists();

        if (resolveContentReferences && com.dotcms.storage.AssetStorageFeature.isEnabled()) {
            final String key = BinaryAssetReference.find(inode, fieldVarName);
            if (key != null) {
                return !key.isEmpty() && storagePersistenceAPI.existsObject(BINARY_ASSETS_GROUP, key);
            }
        }

        final String fieldPath = buildFieldPath(inode, fieldVarName);
        return !storagePersistenceAPI.listObjectPaths(BINARY_ASSETS_GROUP, fieldPath).isEmpty();
    }

    @Override
    public void deleteAllBinaries(final String inode) throws DotDataException {
        evictionLock.readLock().lock();
        try {
            if (!UtilMethods.isSet(inode)) {
                throw new IllegalArgumentException("inode must not be null or empty");
            }

            final String inodePath = inode.charAt(0) + File.separator
                    + inode.charAt(1) + File.separator
                    + inode;

            if (!com.dotcms.storage.AssetStorageFeature.isEnabled()) {
                deleteLocalInodeDirectory(inode, inodePath);
            }

            // If using a chain, also delete from remote providers (e.g., S3).
            if (!(storagePersistenceAPI instanceof FileSystemStoragePersistenceAPIImpl)) {
                ensureGroupExists();
                final List<String> objectPaths = storagePersistenceAPI.listObjectPaths(
                        BINARY_ASSETS_GROUP, inodePath);
                final List<String> failedPaths = new ArrayList<>();
                for (final String objectPath : objectPaths) {
                    try {
                        storagePersistenceAPI.deleteObjectAndReferences(
                                BINARY_ASSETS_GROUP, objectPath);
                    } catch (final Exception e) {
                        Logger.warn(this, String.format(
                                "Failed to delete binary '%s' for inode '%s' from chain: %s",
                                objectPath, inode, e.getMessage()));
                        failedPaths.add(objectPath);
                    }
                }
                if (!failedPaths.isEmpty()) {
                    throw new DotDataException(String.format(
                            "Failed to delete %d of %d binaries for inode '%s' from chain. Failed paths: %s",
                            failedPaths.size(), objectPaths.size(), inode, failedPaths));
                }
            }

            if (com.dotcms.storage.AssetStorageFeature.isEnabled()) {
                deleteLocalInodeDirectory(inode, inodePath);
            }

            Logger.debug(this, () -> String.format("Deleted all binaries for inode '%s'", inode));
        } finally {
            evictionLock.readLock().unlock();
        }
        if (com.dotcms.storage.AssetStorageFeature.isEnabled()) {
            deleteGeneratedFiles(inode);
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

    private void deleteLocalInodeDirectory(final String inode, final String inodePath) throws DotDataException {
        final File inodeDir = new File(ConfigUtils.getAssetPath(), inodePath);
        if (inodeDir.exists()) {
            try {
                FileUtil.deltree(inodeDir);
                if (com.dotcms.storage.AssetStorageFeature.isEnabled() && inodeDir.exists()) {
                    throw new DotDataException("Unable to remove local inode directory " + inode);
                }
            } catch (final Exception e) {
                throw new DotDataException(String.format(
                        "Failed to delete inode directory for inode '%s': %s",
                        inode, e.getMessage()), e);
            }
        }
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
     * Validates that inode, fieldVarName, and fileName are non-null and non-empty.
     */
    private void validateParams(final String inode, final String fieldVarName,
                                final String fileName) {

        if (!UtilMethods.isSet(inode)) {
            throw new IllegalArgumentException("inode must not be null or empty");
        }
        if (!UtilMethods.isSet(fieldVarName)) {
            throw new IllegalArgumentException("fieldVarName must not be null or empty");
        }
        if (!UtilMethods.isSet(fileName)) {
            throw new IllegalArgumentException("fileName must not be null or empty");
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

    private synchronized void ensureGeneratedGroupExists() throws DotDataException {
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

    static boolean isCompletedRendition(final File file) {
        return file.getName().matches("dotGenerated_[a-z0-9]*_[a-f0-9]{1,32}\\.[a-zA-Z0-9]+");
    }

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

    @Override
    public File getGeneratedFile(final File localFile) throws DotDataException {
        if (!com.dotcms.storage.AssetStorageFeature.isEnabled() || storagePersistenceAPI instanceof FileSystemStoragePersistenceAPIImpl) {
            return BinaryAssetStorageAPI.super.getGeneratedFile(localFile);
        }
        evictionLock.readLock().lock();
        try {
            final String path = generatedPath(localFile);
            ensureGeneratedGroupExists();
            return storagePersistenceAPI.pullFile(GENERATED_ASSETS_GROUP, path);
        } finally {
            evictionLock.readLock().unlock();
        }
    }

    @Override
    public void storeGeneratedFile(final File localFile) throws DotDataException {
        if (!com.dotcms.storage.AssetStorageFeature.isEnabled() || storagePersistenceAPI instanceof FileSystemStoragePersistenceAPIImpl) {
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
        if (!com.dotcms.storage.AssetStorageFeature.isEnabled() || storagePersistenceAPI instanceof FileSystemStoragePersistenceAPIImpl) {
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

    @Override
    public void backfillGeneratedFiles(final String inode) throws DotDataException {
        if (!com.dotcms.storage.AssetStorageFeature.isEnabled() || storagePersistenceAPI instanceof FileSystemStoragePersistenceAPIImpl) return;
        if (inode == null || !inode.matches("[a-zA-Z0-9_-]{2,}")) throw new IllegalArgumentException("Invalid inode");
        try (var lease = acquireCacheLease()) {
            ensureGeneratedGroupExists();
            final String prefix = inode.charAt(0) + "/" + inode.charAt(1) + "/" + inode + "/";
            for (String path : storagePersistenceAPI.listObjectPaths(GENERATED_ASSETS_GROUP, prefix)) {
                if (!path.startsWith(prefix) || path.substring(prefix.length()).contains("/")
                        || !isCompletedRendition(new File(path))) continue;
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

    @Override
    public boolean evictLocalFile(final File file) throws DotDataException {
        if (!com.dotcms.storage.AssetStorageFeature.isEnabled()) {
            return false;
        }
        // Never queue an eviction writer behind a long response: that would also stall new readers.
        if (!evictionLock.writeLock().tryLock()) {
            return false;
        }
        try {
            if (Files.isSymbolicLink(file.toPath())) {
                return false;
            }
            try {
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
                    return false;
                }
                Path descendant = suppliedRoot;
                for (final Path component : suppliedRoot.relativize(suppliedPath)) {
                    descendant = descendant.resolve(component);
                    if (Files.isSymbolicLink(descendant)) {
                        return false;
                    }
                }
                if (!path.startsWith(root) || !isEvictableAssetPath(root.relativize(path), generated)) {
                    return false;
                }
                final BasicFileAttributes before = Files.readAttributes(path, BasicFileAttributes.class,
                        LinkOption.NOFOLLOW_LINKS);
                if (!before.isRegularFile() || !storagePersistenceAPI.hasDurableCopy(
                        group, root.relativize(path).toString(), file)) {
                    return false;
                }
                final BasicFileAttributes after = Files.readAttributes(path, BasicFileAttributes.class,
                        LinkOption.NOFOLLOW_LINKS);
                if (before.size() != after.size() || !before.lastModifiedTime().equals(after.lastModifiedTime())
                        || !java.util.Objects.equals(before.fileKey(), after.fileKey())) {
                    return false;
                }
                return Files.deleteIfExists(path);
            } catch (final IOException e) {
                throw new DotDataException("Unable to evict binary cache file " + file, e);
            }
        } finally {
            evictionLock.writeLock().unlock();
        }
    }

}
