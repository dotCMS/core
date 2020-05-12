package com.dotcms.storage;

import com.dotcms.util.MimeTypeUtils;
import com.dotmarketing.util.Config;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.function.Predicate;

/**
 * This class is in charge of resolve File (on diff storages), metadata, etc.
 * @author jsanca
 */
public interface FileStorageAPI {

    int SIZE                       = 1024;
    int DEFAULT_META_DATA_MAX_SIZE = 5;

    String TITLE_META_KEY         = "title";
    String PATH_META_KEY          = "path";
    String LENGTH_META_KEY        = "length";
    String CONTENT_TYPE_META_KEY  = "contentType";
    String MOD_DATE_META_KEY      = "modDate";
    String SHA226_META_KEY        = "sha256";

    /**
     * Returns the default configured max length
     * @return int
     */
    static int configuredMaxLength () {
        return Config.getIntProperty("META_DATA_MAX_SIZE",
                FileStorageAPI.DEFAULT_META_DATA_MAX_SIZE) * FileStorageAPI.SIZE;
    }

    /**
     * Gets the basic metadata from the binary, this method returns the raw metadata, does not stores anything on cache or disk neither filter anything
     * @param binary {@link File} file to get the information
     * @return Map with the metadata
     */
    Map<String, Object> generateRawBasicMetaData(final File binary);

    /**
     * Gets the full metadata from the binary, this method returns the raw metadata, does not stores anything on cache or disk neither filter anything
     * @param binary  {@link File} file to get the information
     * @param maxLength {@link Integer} max length is used when parse the content, how many bytes do you want to parse.
     * @return Map with the metadata
     */
    Map<String, Object> generateRawFullMetaData(final File binary, final int maxLength);


    /**
     * Gets the basic metadata from the binary, this method does not any stores but could do a filter anything
     * @param binary {@link File} file to get the information
     * @param metaDataKeyFilter  {@link Predicate} filter the meta data key for the map result generation
     * @return Map with the metadata
     */
    Map<String, Object> generateBasicMetaData(final File binary, Predicate<String> metaDataKeyFilter) ;

    /**
     * Gets the full metadata from the binary, this could involved a more expensive process such as Tika, this method does not any stores but could do a filter anything
     * @param binary  {@link File} file to get the information
     * @param metaDataKeyFilter  {@link Predicate} filter for the map result generation
     * @param maxLength {@link Integer} max length is used when parse the content, how many bytes do you want to parse.
     * @return Map with the metadata
     */
    Map<String, Object> generateFullMetaData(final File binary, Predicate<String> metaDataKeyFilter, final int maxLength);

    /**
     * Based on the configuration generates the metadata, this configuration is more specific could select between
     * full|basic metadata, stores or not in the File System (even reuse or overrides it), stores or not in the cache, etc.
     *
     * @param binary {@link File} file to get the information
     * @param generateMetaDataConfiguration
     * @return Map with the metadata
     */
    Map<String, Object> generateMetaData(final File binary, GenerateMetaDataConfiguration generateMetaDataConfiguration);

    /**
     *
     * @param generateMetaDataConfiguration
     * @return
     */
    Map<String, Object> retrieveMetaData(GenerateMetaDataConfiguration generateMetaDataConfiguration);

}
