package com.dotcms.storage;

import com.dotcms.contenttype.model.field.Field;
import com.dotmarketing.portlets.contentlet.model.Contentlet;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

/**
 * Encapsulates the generation of the contentlet metadata, in addition the interaction of the file system and cache to stores the metadata and the actual generation of the meta
 * is being done by the {@link FileStorageAPI}
 * @author jsanca
 */
public interface ContentletMetadataAPI {

    /**
     * Returns the full and basic metadata for the binaries passed in the parameters.
     * Keep in mind that the full metadata won't be stored on the cache, but it will be stored on the file system,
     * so full metadata for the fullBinaryFieldNameSet will stores on the cache the basic metadata instead of the full one (but the file system will keep the full one)
     *
     * Note: if the basicBinaryFieldNameSet has field which is also include on the fullBinaryFieldNameSet, it will be skipped.
     * @param contentlet Contentlet
     * @param basicBinaryFieldNameSet {@link Set} fields to generate basic metadata
     * @param fullBinaryFieldNameSet  {@link Set} fields to generate full metadata
     * @return ContentletMetadata
     */
    ContentletMetadata  generateContentletMetadata (Contentlet contentlet, Set<String> basicBinaryFieldNameSet, Set<String> fullBinaryFieldNameSet) throws IOException;

    /**
     * This generation use an strategy to make the choice of which binary fields will generates the full or basic metadata.
     * By default it takes the first indexable binary for full and all of them will generates the basic.
     * @param contentlet {@link Contentlet}
     * @return ContentletMetadata
     */
    ContentletMetadata  generateContentletMetadata (Contentlet contentlet) throws IOException;

    /**
     * Retrieves the metadata for the contentlet
     * @param contentlet  {@link Contentlet}
     * @param field       {@link Field}
     * @return Map
     */
    Map<String, Object> getMetadata(Contentlet contentlet, Field field);


    /**
     * Retrieves the metadata for the contentlet
     * @param contentlet          {@link Contentlet}
     * @param fieldVariableName  {@link String}
     * @return Map
     */
    Map<String, Object> getMetadata(Contentlet contentlet, String fieldVariableName);

    /**
     * Retrieves the metadata for the contentlet
     * @param contentlet          {@link Contentlet}
     * @param fieldVariableName  {@link String}
     * @return Map
     */
    Map<String, Object> getMetadataNoCache(Contentlet contentlet, String fieldVariableName);
}
