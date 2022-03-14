package com.dotcms.storage;

import static com.dotmarketing.util.UtilMethods.isSet;

import com.dotcms.contenttype.model.field.Field;
import com.dotcms.storage.model.ContentletMetadata;
import com.dotcms.storage.model.Metadata;
import com.dotcms.tika.TikaUtils;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;

import com.dotmarketing.util.Config;
import com.dotmarketing.util.StringUtils;
import com.dotmarketing.util.UtilMethods;
import com.liferay.util.StringPool;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.function.Supplier;

/**
 * Encapsulates the generation of the contentlet metadata, in addition the interaction of the file system and cache to stores the metadata and the actual generation of the meta
 * is being done by the {@link FileStorageAPI}
 * @author jsanca
 */
public interface FileMetadataAPI {

    String ALWAYS_REGENERATE_METADATA_ON_REINDEX = "always.regenerate.metadata.on.reindex";

    String BINARY_METADATA_VERSION = "BINARY_METADATA_VERSION";
    int CURRENT_BINARY_METADATA_VERSION = 20220201;

    String META_TMP = ".meta.tmp";
    String METADATA_GROUP_NAME = "METADATA_GROUP_NAME";
    String DEFAULT_STORAGE_TYPE = "DEFAULT_STORAGE_TYPE";
    String DOT_METADATA = "dotmetadata";
    String DEFAULT_METADATA_GROUP_NAME = DOT_METADATA;
    String METADATA_JSON = "-metadata.json";

    /**
     * Metadata file generator.
     * @param contentlet
     * @param fieldVariableName
     * @return
     */
    default String getFileName (final Contentlet contentlet, final String fieldVariableName) {

        final String inode        = contentlet.getInode();
        final String fileName     = fieldVariableName + METADATA_JSON;
        return StringUtils.builder(File.separator,
                inode.charAt(0), File.separator, inode.charAt(1), File.separator, inode, File.separator,
                fileName).toString();
    }

    /**
     * Reads INDEX_METADATA_FIELDS for  pre-configured metadata fields
     * @return
     */
    default Set<String> getConfiguredMetadataFields(){
        return TikaUtils.getConfiguredMetadataFields();
    }

    /**
     * Filters fields from a map given a set of fields to be kept
     * @param metaMap
     */
    default void filterMetadataFields(Map<String, ?> metaMap, Set<String> configFieldsSet){
        TikaUtils.filterMetadataFields(metaMap, configFieldsSet);
    }

    /**
     * This generation use an strategy to make the choice of which binary fields will be included on the generation of the full or basic metadata.
     * By default it takes the first indexable binary for full and all the rest will be used on the the basic.
     * @param contentlet {@link Contentlet}
     * @return ContentletMetadata
     */
    ContentletMetadata generateContentletMetadata (Contentlet contentlet)
            throws IOException, DotDataException;
    /**
     * Retrieves the basic metadata for the contentlet (a projection over the full MD)
     * @param contentlet          {@link Contentlet}
     * @param fieldVariableName  {@link String}
     * @return Map
     */
    Metadata getMetadata(Contentlet contentlet, String fieldVariableName)
            throws DotDataException;

    /**
     * Retrieves the basic metadata for the contentlet (a projection over the full MD)
     * If the MD has never been generated this will force it's generation
     * @param contentlet          {@link Contentlet}
     * @param fieldVariableName  {@link String}
     * @return Map
     */
     Metadata getOrGenerateMetadata(Contentlet contentlet, String fieldVariableName)
            throws DotDataException;

    /**
     * This number helps us to determine if we need to regenerate lazily the md if we want to include new attributes on the md
     * @return ver number follows the same pattern as our StartupTasks, eg, the date formatted like YYYYMMDD
     */
    default int getBinaryMetadataVersion(){
        //We should always return the hard coded val but I'm leaving it open so it can be override via props
        return Config.getIntProperty(BINARY_METADATA_VERSION,
                CURRENT_BINARY_METADATA_VERSION);
    }

    /**
     * Retrieves the full metadata for the contentlet
     * When we specify that we must not perform a cache read it means we will be loading the FM stored in disc
     * @param contentlet          {@link Contentlet}
     * @param fieldVariableName  {@link String}
     * @return Map
     */
    Metadata getFullMetadataNoCache(Contentlet contentlet, String fieldVariableName)
            throws DotDataException;

    /**
     * Retrieves the full metadata for the contentlet
     * if no MD exists this will force it's generation.
     * @param contentlet source content
     * @param fieldVariableName field
     * @return
     * @throws DotDataException
     */
    Metadata getOrGenerateFullMetadataNoCache(Contentlet contentlet,
            String fieldVariableName) throws DotDataException;

    /**
     * Look up the first indexed binary and returns such metadata
     * @param contentlet
     * @return
     */
    Optional<Metadata> getDefaultMetadata(Contentlet contentlet);

    /**
     * Look up the first indexed binary and returns such metadata
     * this forces the generation of the metadata in case it doesn't exist
     * @param contentlet
     * @return
     */
    Optional<Metadata> getOrGenerateDefaultMetadata(final Contentlet contentlet);

    /**
     * Removes All metadata for a given Contentlet
     * @param contentlet
     * @return @{@link Map} with the info of the entries removed
     */
    Map<String, Set<String>> removeMetadata(Contentlet contentlet);

    /**
     * Removes metadata for a given Contentlet inode. Meaning all other versions of the contentlet will get to keep their md.
     * @param contentlet
     * @return @{@link Map} with the info of the entries removed
     */
    Map<String, Set<String>> removeVersionMetadata(Contentlet contentlet);

    /**
     * Given a binary file this will rely on the metadata generator to read the file and get the full md associated
     * As a fallback we can also pass the contentlet in case of read md failure
     * @param binary
     * @param fallbackContentlet
     * @return
     * @throws DotDataException
     */
    Metadata getFullMetadataNoCache(File binary, Supplier<Contentlet> fallbackContentlet)
            throws DotDataException;


    /**
     * This is meant to put custom attributes into the metadata storage associated with the first indexed binary
     * They're associated with the first indexed-binary since that's pretty much all we store for the full-metadata
     * They're also reflected in the basic cached metadata
     * @param contentlet the contentlet we want to associate the md with
     * @param customAttributesByField the additional attributes organized by binary field
     * @throws DotDataException
     */
    void putCustomMetadataAttributes(Contentlet contentlet,
            final Map<String, Map<String, Serializable>> customAttributesByField) throws DotDataException;


    /**
     * Write custom metadata to linked to a temporary file
     * @param tempResourceId
     * @param customAttributesByField
     * @throws DotDataException
     */
    void putCustomMetadataAttributes(final String tempResourceId,
            final Map<String, Map<String,Serializable>> customAttributesByField) throws DotDataException;


    /**
     * Temp metadata retrieve method
     * @param tempResourceId
     * @return
     * @throws DotDataException
     */
    Optional<Metadata> getMetadata(final String tempResourceId)
            throws DotDataException;

    /**
     * Given a source contentlet this will grab all custom meta and copy it into the destination contentlet
     * assuming both are of the same CT
     * @param source
     * @param destination
     */
    void copyCustomMetadata(Contentlet source, Contentlet destination) throws DotDataException;

    /**
     * This forces the metadata into a contentlet. No validation type is performed
     * This means that a pdf file could end-up with something like "isImage:true"
     * So be careful.
     * This is pretty much meant for push publishing purposes
     * @param contentlet
     * @param binariesMetadata
     * @throws DotDataException
     */
    void setMetadata(final Contentlet contentlet, final Map<String, Metadata> binariesMetadata) throws DotDataException;
}
