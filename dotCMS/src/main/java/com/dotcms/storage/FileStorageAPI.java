package com.dotcms.storage;

import com.dotcms.storage.model.BasicMetadataFields;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Config;

import java.io.File;
import java.io.Serializable;
import java.util.Map;

/**
 * This Storage API is in charge of generating, removing and storing file metadata in dotCMS using
 * the File System as its storage volume.
 *
 * @author jsanca
 * @since Oct 19th, 2020
 */
public interface FileStorageAPI {

    int SIZE                       = 1024;
    int DEFAULT_META_DATA_MAX_SIZE = 5;

    ObjectReaderDelegate DEFAULT_OBJECT_READER_DELEGATE = new JsonReaderDelegate<>(Map.class);
    ObjectWriterDelegate DEFAULT_OBJECT_WRITER_DELEGATE = new JsonWriterDelegate();
    MetadataGenerator DEFAULT_METADATA_GENERATOR = new MetadataGeneratorImpl();

    /**
     * Returns the default configured max length
     * @return int
     */
    static long configuredMaxLength () {
        return Config.getLongProperty("META_DATA_MAX_SIZE",
                FileStorageAPI.DEFAULT_META_DATA_MAX_SIZE) * FileStorageAPI.SIZE;
    }

    /**
     * Gets the basic metadata from the binary, this method returns the raw metadata, does not stores anything on cache or disk neither filter anything
     * @param binary {@link File} file to get the information
     * @return Map with the metadata
     */
    Map<String, Serializable> generateRawBasicMetaData(final File binary);

    /**
     * Gets the full metadata from the binary, this method returns the raw metadata, does not stores anything on cache or disk neither filter anything
     * @param binary  {@link File} file to get the information
     * @param maxLength {@link Long} max length is used when parse the content, how many bytes do you want to parse.
     * @return Map with the metadata
     */
    Map<String, Serializable> generateRawFullMetaData(final File binary, final long maxLength);

    /**
     * Based on the configuration generates the metadata, this configuration is more specific could select between
     * full|basic metadata, stores or not in the File System (even reuse or overrides it), stores or not in the cache, etc.
     *
     * @param binary {@link File} file to get the information
     * @param configuration {@link GenerateMetadataConfig}
     * @return Map with the metadata
     */
    Map<String, Serializable> generateMetaData(final File binary, final GenerateMetadataConfig configuration)
            throws DotDataException;

    /**
     * Retrieves the metadata object from the configured Storage Provider. There are a couple of
     * aspects to take into consideration when calling this method:
     * <ul>
     *     <li>Depending on the configuration of the {@link FetchMetadataParams} parameter, the
     *     resulting metadata object may be cached.</li>
     *     <li>For performance reasons, the {@link BasicMetadataFields#EDITABLE_AS_TEXT} property
     *     is always calculated <b>UNLESS</b> it's already present in the metadata Map. This way,
     *     Files don't need to be re-indexed for it to be available.</li>
     * </ul>
     *
     * @param requestMetaData The {@link FetchMetadataParams} object specifying how the metadata
     *                        should be retrieved.
     *
     * @return A key/value Map with the expected metadata properties.
     *
     * @throws DotDataException An error occurred when retrieving the metadata from the Storage
     *                          Provider.
     */
    Map<String, Serializable> retrieveMetaData(final FetchMetadataParams requestMetaData) throws DotDataException;


    /**
     * Deletes all related metadata for the given contentlet
     * @param requestMetaData
     * @return a set with the different keyPaths removed
     * @throws DotDataException
     */
    boolean removeMetaData(final FetchMetadataParams requestMetaData) throws DotDataException;

    /**
     * Deletes only current version (inode) metadata for the given contentlet
     * @param requestMetaData
     * @return a set with the different keyPaths removed
     * @throws DotDataException
     */
    boolean removeVersionMetaData(final FetchMetadataParams requestMetaData) throws DotDataException;

    /**
     * Saves additional custom attributes into the metadata storage
     * @param requestMetadata
     * @param customAttributes
     * @throws DotDataException
     */
    void putCustomMetadataAttributes(final FetchMetadataParams requestMetadata,
            final Map<String, Serializable> customAttributes) throws DotDataException;


    /**
     * Saves additional custom attributes into the metadata storage
     * @param requestMetadata
     * @param metadata
     * @throws DotDataException
     */
    boolean setMetadata(FetchMetadataParams requestMetadata,
            final Map<String, Serializable> metadata) throws DotDataException;

}
