package com.dotcms.storage;

import com.dotcms.contenttype.model.field.Field;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;

import java.io.IOException;
import java.io.Serializable;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;

/**
 * Encapsulates the generation of the contentlet metadata, in addition the interaction of the file system and cache to stores the metadata and the actual generation of the meta
 * is being done by the {@link FileStorageAPI}
 * @author jsanca
 */
public interface FileMetadataAPI {

    /**
     * Returns the full and basic metadata for the binaries passed in the parameters.
     * Keep in mind that the full metadata won't be stored on the cache, but it will be stored on the file system,
     * so full metadata for the fullBinaryFieldNameSet will stores on the cache the basic metadata instead of the full one (but the file system will keep the full one)
     *
     * Note: if the basicBinaryFieldNameSet has field which is also include on the fullBinaryFieldNameSet, it will be skipped.
     * @param contentlet Contentlet
     * @param basicBinaryFieldNameSet {@link SortedSet} fields to generate basic metadata
     * @param fullBinaryFieldNameSet  {@link SortedSet} fields to generate full metadata
     * @return ContentletMetadata
     */
    ContentletMetadata  generateContentletMetadata (Contentlet contentlet, SortedSet<String> basicBinaryFieldNameSet, SortedSet<String> fullBinaryFieldNameSet)
            throws IOException, DotDataException;

    /**
     * This generation use an strategy to make the choice of which binary fields will generates the full or basic metadata.
     * By default it takes the first indexable binary for full and all of them will generates the basic.
     * @param contentlet {@link Contentlet}
     * @return ContentletMetadata
     */
    ContentletMetadata  generateContentletMetadata (Contentlet contentlet)
            throws IOException, DotDataException;

    /**
     * Retrieves the basic metadata projection for the contentlet
     * @param contentlet  {@link Contentlet}
     * @param field       {@link Field}
     * @return Map
     */
    Map<String, Serializable> getMetadata(Contentlet contentlet, Field field) throws DotDataException;


    /**
     * Retrieves the basic metadata for the contentlet (a projection over the full MD)
     * @param contentlet          {@link Contentlet}
     * @param fieldVariableName  {@link String}
     * @return Map
     */
    Map<String, Serializable> getMetadata(Contentlet contentlet, String fieldVariableName)
            throws DotDataException;

    /**
     * Retrieves the full metadata for the contentlet
     * When we specify that we must not perform a cache read it means we will nbe loading the FM stored in disc
     * @param contentlet          {@link Contentlet}
     * @param fieldVariableName  {@link String}
     * @return Map
     */
    Map<String, Serializable> getFullMetadataNoCache(Contentlet contentlet, String fieldVariableName)
            throws DotDataException;

    /**
     * Compiles all metadata for the contentlet returning a natural ordered map.
     * @param contentlet {@link Contentlet}
     * @return Map
     */
    Map<String, Map<String, Serializable>> collectFieldsMetadata(final Contentlet contentlet);
}
