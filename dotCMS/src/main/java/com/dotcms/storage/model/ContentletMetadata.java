package com.dotcms.storage.model;

import java.io.Serializable;
import java.util.Map;

/**
 * Encapsulates a collection with field with full metadata generation and
 * all fields with basic metadata
 */
public class ContentletMetadata implements Serializable {

    private final Map<String, Metadata> fullMetadataMap;

    private final Map<String, Metadata> basicMetadataMap;

    public ContentletMetadata(
           final Map<String, Metadata> fullMetadataMap,
           final Map<String, Metadata> basicMetadataMap) {
        this.fullMetadataMap = fullMetadataMap;
        this.basicMetadataMap = basicMetadataMap;
    }

    public Map<String, Metadata> getFullMetadataMap() {
        return fullMetadataMap;
    }

    public Map<String, Metadata> getBasicMetadataMap() {
        return basicMetadataMap;
    }
}
