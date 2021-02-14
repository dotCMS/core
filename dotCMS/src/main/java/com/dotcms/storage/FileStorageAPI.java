package com.dotcms.storage;

import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Config;

import java.io.File;
import java.io.Serializable;
import java.util.Map;
import java.util.function.Predicate;

/**
 * This class is in charge of resolve File (on diff repositories), metadata, etc.
 * @author jsanca
 */
public interface FileStorageAPI {

    int SIZE                       = 1024;
    int DEFAULT_META_DATA_MAX_SIZE = 5;

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
     * Gets the basic metadata from the binary, this method does not any stores but could do a filter anything
     * @param binary {@link File} file to get the information
     * @param metaDataKeyFilter  {@link Predicate} filter the meta data key for the map result generation
     * @return Map with the metadata
     */
    Map<String, Serializable> generateBasicMetaData(final File binary, Predicate<String> metaDataKeyFilter) ;

    /**
     * Gets the full metadata from the binary, this could involved a more expensive process such as Tika, this method does not any stores but could do a filter anything
     * @param binary  {@link File} file to get the information
     * @param metaDataKeyFilter  {@link Predicate} filter for the map result generation
     * @param maxLength {@link Long} max length is used when parse the content, how many bytes do you want to parse.
     * @return Map with the metadata
     */
    Map<String, Serializable> generateFullMetaData(final File binary, Predicate<String> metaDataKeyFilter, final long maxLength);

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
     * Retrieve the metadata
     * @param requestMetaData {@link RequestMetadata}
     * @return  Map with the metadata
     */
    Map<String, Serializable> retrieveMetaData(final RequestMetadata requestMetaData) throws DotDataException;


    /**
     * Delete metadata
     * @param requestMetaData
     * @return a set with the different keyPaths removed
     * @throws DotDataException
     */
    boolean removeMetaData(final RequestMetadata requestMetaData) throws DotDataException;


    /**
     * Saves additional custom attributes into the metadata storage
     * @param configuration
     * @param customAttributes
     * @throws DotDataException
     */
    void putCustomMetadataAttributes(final GenerateMetadataConfig configuration,
            final Map<String, Serializable> customAttributes) throws DotDataException;


    /**
     * Saves additional custom attributes into the metadata storage
     * @param configuration
     * @param metadata
     * @throws DotDataException
     */
    boolean setMetadata(final GenerateMetadataConfig configuration,
            final Map<String, Serializable> metadata) throws DotDataException;

}
