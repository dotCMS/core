package com.dotcms.storage.binary;

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
import java.util.Map;

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

    private final StoragePersistenceAPI storagePersistenceAPI;
    private volatile boolean groupInitialized = false;

    /**
     * Production constructor. Obtains the FILE_SYSTEM storage provider from
     * {@link StoragePersistenceProvider}.
     */
    public BinaryAssetStorageAPIImpl() {
        this(StoragePersistenceProvider.INSTANCE.get().getStorage(StorageType.FILE_SYSTEM));
    }

    /**
     * Test constructor. Accepts a pre-configured or mocked {@link StoragePersistenceAPI}.
     *
     * @param storagePersistenceAPI the storage provider to delegate to
     */
    public BinaryAssetStorageAPIImpl(final StoragePersistenceAPI storagePersistenceAPI) {
        this.storagePersistenceAPI = storagePersistenceAPI;
    }

    @Override
    public File getBinaryFile(final String inode, final String fieldVarName,
                              final String fileName) throws DotDataException {

        validateParams(inode, fieldVarName, fileName);
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
    }

    @Override
    public InputStream getBinaryStream(final String inode, final String fieldVarName,
                                       final String fileName) throws DotDataException {

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

    @Override
    public File getBinaryFile(final String inode,
                              final String fieldVarName) throws DotDataException {

        if (!UtilMethods.isSet(inode)) {
            throw new IllegalArgumentException("inode must not be null or empty");
        }
        if (!UtilMethods.isSet(fieldVarName)) {
            throw new IllegalArgumentException("fieldVarName must not be null or empty");
        }

        final File fieldDir = resolveFieldDirectory(inode, fieldVarName);
        if (fieldDir == null || !fieldDir.exists() || !fieldDir.isDirectory()) {
            return null;
        }

        final File[] files = fieldDir.listFiles(file ->
                !file.getName().contains(Config.GENERATED_FILE)
                        && !file.getName().startsWith("."));

        if (files != null && files.length > 0) {
            return files[0];
        }

        return null;
    }

    @Override
    public InputStream getBinaryStream(final String inode,
                                       final String fieldVarName) throws DotDataException {

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

    /**
     * Resolves the field directory on the local filesystem.
     *
     * <p><strong>Known limitation (Phase 2):</strong> This method goes direct to filesystem
     * via {@link ConfigUtils#getAssetPath()}. Phase 3 must replace this with prefix-based
     * listing through {@code StoragePersistenceAPI} to support S3.</p>
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
     * <p><strong>Known limitation (Phase 2):</strong> This method goes direct to filesystem
     * via {@link ConfigUtils#getAssetPath()}. Phase 3 must replace this with storage-layer
     * operations to support S3.</p>
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

        validateParams(inode, fieldVarName, fileName);
        ensureGroupExists();

        if (sourceFile == null || !sourceFile.exists()) {
            throw new IllegalArgumentException(
                    String.format("Source file is null or does not exist for inode '%s', field '%s', file '%s'",
                            inode, fieldVarName, fileName));
        }

        if (hardLink && storagePersistenceAPI instanceof FileSystemStoragePersistenceAPIImpl) {
            // Bypass StoragePersistenceAPI.pushFile for FILE_SYSTEM mode:
            // pushFile uses Apache Commons FileUtils.copyFile (no hard-links) and lowercases
            // paths (breaks file name case). FileUtil.copyFile supports hard-links and preserves case.
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
    }

    @Override
    public void storeBinary(final String inode, final String fieldVarName,
                            final String fileName, final File sourceFile) throws DotDataException {

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
    }

    @Override
    public void copyBinary(final String sourceInode, final String destInode,
                           final String fieldVarName, final String fileName) throws DotDataException {

        validateParams(sourceInode, fieldVarName, fileName);
        if (!UtilMethods.isSet(destInode)) {
            throw new IllegalArgumentException("destInode must not be null or empty");
        }
        ensureGroupExists();

        if (storagePersistenceAPI instanceof FileSystemStoragePersistenceAPIImpl) {
            // Bypass pullFile+pushFile for FILE_SYSTEM mode:
            // pullFile/pushFile lowercase paths (breaks file name case) and pushFile
            // uses Apache Commons copy (no hard-links). FileUtil.copyFile preserves case
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
            final File sourceFile = storagePersistenceAPI.pullFile(BINARY_ASSETS_GROUP, sourcePath);

            final String destPath = buildFilePath(destInode, fieldVarName, fileName);
            storagePersistenceAPI.pushFile(BINARY_ASSETS_GROUP, destPath, sourceFile,
                    Map.<String, Serializable>of());
        }
    }

    @Override
    public void deleteBinary(final String inode,
                             final String fieldVarName) throws DotDataException {

        if (!UtilMethods.isSet(inode)) {
            throw new IllegalArgumentException("inode must not be null or empty");
        }
        if (!UtilMethods.isSet(fieldVarName)) {
            throw new IllegalArgumentException("fieldVarName must not be null or empty");
        }
        ensureGroupExists();

        final String fieldPath = buildFieldPath(inode, fieldVarName);
        storagePersistenceAPI.deleteObjectAndReferences(BINARY_ASSETS_GROUP, fieldPath);
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

        final String fieldPath = buildFieldPath(inode, fieldVarName);
        return storagePersistenceAPI.existsObject(BINARY_ASSETS_GROUP, fieldPath);
    }

    @Override
    public void deleteAllBinaries(final String inode) throws DotDataException {

        if (!UtilMethods.isSet(inode)) {
            throw new IllegalArgumentException("inode must not be null or empty");
        }

        final String inodePath = inode.charAt(0) + File.separator
                + inode.charAt(1) + File.separator
                + inode;

        final File inodeDir = new File(ConfigUtils.getAssetPath(), inodePath);
        if (inodeDir.exists()) {
            try {
                FileUtil.deltree(inodeDir);
            } catch (final Exception e) {
                throw new DotDataException(String.format(
                        "Failed to delete inode directory for inode '%s': %s",
                        inode, e.getMessage()), e);
            }
        }
        Logger.debug(this, () -> String.format("Deleted all binaries for inode '%s'", inode));
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
     */
    private void ensureGroupExists() throws DotDataException {

        if (!groupInitialized) {
            synchronized (this) {
                if (!groupInitialized) {
                    if (!storagePersistenceAPI.existsGroup(BINARY_ASSETS_GROUP)) {
                        if (storagePersistenceAPI instanceof FileSystemStoragePersistenceAPIImpl) {
                            final File assetsDir = new File(ConfigUtils.getAssetPath());
                            if (!assetsDir.exists()) {
                                assetsDir.mkdirs();
                            }
                            ((FileSystemStoragePersistenceAPIImpl) storagePersistenceAPI)
                                    .addGroupMapping(BINARY_ASSETS_GROUP, assetsDir);
                            Logger.info(this, String.format(
                                    "Mapped binary-assets group to asset directory: %s", assetsDir));
                        } else {
                            storagePersistenceAPI.createGroup(BINARY_ASSETS_GROUP);
                            Logger.info(this, String.format(
                                    "Created binary-assets group on %s storage provider",
                                    storagePersistenceAPI.getClass().getSimpleName()));
                        }
                    }
                    groupInitialized = true;
                }
            }
        }
    }

}
