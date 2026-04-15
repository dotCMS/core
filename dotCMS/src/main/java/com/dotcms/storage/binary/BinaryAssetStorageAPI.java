package com.dotcms.storage.binary;

import com.dotmarketing.exception.DotDataException;

import java.io.File;
import java.io.InputStream;

/**
 * High-level API for managing binary file assets (the actual uploaded files, not metadata).
 *
 * <p>This API understands binary asset semantics (inodes, field variable names, filenames)
 * and delegates to the lower-level {@link com.dotcms.storage.StoragePersistenceAPI} for
 * actual storage operations.</p>
 *
 * <p>Storage path layout: {@code {inode[0]}/{inode[1]}/{inode}/{fieldVarName}/{fileName}}</p>
 *
 * @since 2026-04-15
 */
public interface BinaryAssetStorageAPI {

    /**
     * Group name used for binary asset storage in the underlying StoragePersistenceAPI.
     */
    String BINARY_ASSETS_GROUP = "binary-assets";

    /**
     * Retrieves a binary file for the given inode, field, and filename.
     *
     * @param inode        the contentlet inode
     * @param fieldVarName the field's velocity variable name (e.g., "fileAsset", "image")
     * @param fileName     the actual filename (e.g., "report.pdf")
     * @return the File, or {@code null} if the binary does not exist
     * @throws DotDataException if a storage error occurs
     */
    File getBinaryFile(String inode, String fieldVarName, String fileName) throws DotDataException;

    /**
     * Retrieves a binary file as an InputStream.
     *
     * <p><strong>Caller is responsible for closing the returned stream.</strong></p>
     *
     * @param inode        the contentlet inode
     * @param fieldVarName the field's velocity variable name
     * @param fileName     the actual filename
     * @return an InputStream for the file, or {@code null} if the binary does not exist
     * @throws DotDataException if a storage error occurs
     */
    InputStream getBinaryStream(String inode, String fieldVarName, String fileName) throws DotDataException;

    /**
     * Retrieves the binary file for a given inode and field, discovering the filename
     * automatically. Excludes generated files (dotGenerated_*) and hidden files (dot-prefixed).
     *
     * <p>This method exists because many callers (especially {@code Contentlet.getBinary()})
     * don't know the filename — only the inode and field variable name. The implementation
     * lists the field directory and returns the first non-generated, non-hidden file.</p>
     *
     * <p><strong>Known limitation:</strong> In Phase 2 (FILE_SYSTEM only), this method bypasses
     * {@code StoragePersistenceAPI} for the directory listing step and goes direct to filesystem.
     * Phase 3 must add prefix-based listing to the storage layer for S3 support.</p>
     *
     * @param inode        the contentlet inode
     * @param fieldVarName the field's velocity variable name (e.g., "fileAsset", "image")
     * @return the File, or {@code null} if no binary exists for this field
     * @throws DotDataException if a storage error occurs
     */
    File getBinaryFile(String inode, String fieldVarName) throws DotDataException;

    /**
     * Retrieves a binary file as an InputStream, discovering the filename automatically.
     * Excludes generated files (dotGenerated_*) and hidden files (dot-prefixed).
     *
     * <p><strong>Caller is responsible for closing the returned stream.</strong></p>
     *
     * @param inode        the contentlet inode
     * @param fieldVarName the field's velocity variable name
     * @return an InputStream for the file, or {@code null} if no binary exists for this field
     * @throws DotDataException if a storage error occurs
     */
    InputStream getBinaryStream(String inode, String fieldVarName) throws DotDataException;

    /**
     * Stores a binary file for the given inode and field.
     *
     * @param inode        the contentlet inode
     * @param fieldVarName the field's velocity variable name
     * @param fileName     the filename to store as
     * @param sourceFile   the file to store
     * @throws DotDataException if a storage error occurs
     */
    void storeBinary(String inode, String fieldVarName, String fileName, File sourceFile) throws DotDataException;

    /**
     * Stores a binary file with hard-link optimization support.
     *
     * <p>In FILE_SYSTEM mode with {@code hardLink=true}, uses filesystem hard-links instead
     * of byte-copying when possible (respects {@code CONTENT_VERSION_HARD_LINK} config).
     * In non-FILE_SYSTEM mode, the hardLink flag is ignored.</p>
     *
     * <p><strong>Known limitation (Phase 2):</strong> bypasses {@code StoragePersistenceAPI.pushFile}
     * in FILE_SYSTEM mode to preserve hard-links and file name case. Phase 3 must evolve this.</p>
     *
     * @param inode        the contentlet inode
     * @param fieldVarName the field's velocity variable name
     * @param fileName     the filename to store as
     * @param sourceFile   the file to store
     * @param hardLink     whether to attempt hard-link instead of byte-copy (FILE_SYSTEM only)
     * @throws DotDataException if a storage error occurs
     */
    void storeBinary(String inode, String fieldVarName, String fileName,
                     File sourceFile, boolean hardLink) throws DotDataException;

    /**
     * Copies a binary file from one inode to another (e.g., for content versioning).
     *
     * @param sourceInode  the source contentlet inode
     * @param destInode    the destination contentlet inode
     * @param fieldVarName the field's velocity variable name
     * @param fileName     the filename
     * @throws DotDataException if a storage error occurs
     */
    void copyBinary(String sourceInode, String destInode, String fieldVarName, String fileName) throws DotDataException;

    /**
     * Deletes all binary files for the given inode and field.
     * This removes the entire field directory.
     *
     * @param inode        the contentlet inode
     * @param fieldVarName the field's velocity variable name
     * @throws DotDataException if a storage error occurs
     */
    void deleteBinary(String inode, String fieldVarName) throws DotDataException;

    /**
     * Checks whether a binary field directory exists for the given inode and field.
     *
     * @param inode        the contentlet inode
     * @param fieldVarName the field's velocity variable name
     * @return {@code true} if the field directory exists
     * @throws DotDataException if a storage error occurs
     */
    boolean existsBinary(String inode, String fieldVarName) throws DotDataException;

    /**
     * Deletes all binary files for the given inode across all fields.
     * Removes the entire inode directory tree.
     *
     * <p><strong>Known limitation (Phase 2):</strong> In FILE_SYSTEM mode, this resolves
     * the inode directory path via {@link com.dotmarketing.util.ConfigUtils#getAssetPath()}
     * and deletes the tree. Phase 3 must add S3 prefix-based deletion.</p>
     *
     * @param inode the contentlet inode
     * @throws DotDataException if a storage error occurs
     */
    void deleteAllBinaries(String inode) throws DotDataException;

}
