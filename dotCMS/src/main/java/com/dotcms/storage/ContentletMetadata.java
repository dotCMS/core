package com.dotcms.storage;

import java.io.Serializable;
import java.util.Map;

/**
 * Encapsulates a collection with field with full metadata generation and
 * all fields with basic metadata
 */
public class ContentletMetadata {

    private final Map<String, Map<String, Serializable>> fullMetadataMap;
    private final Map<String, Map<String, Serializable>> basicMetadataMap;

    ContentletMetadata(final Map<String, Map<String, Serializable>> fullMetadataMap,
            final Map<String, Map<String, Serializable>> basicMetadataMap) {
        this.fullMetadataMap = fullMetadataMap;
        this.basicMetadataMap = basicMetadataMap;
    }

    public Map<String, Map<String, Serializable>> getFullMetadataMap() {
        return fullMetadataMap;
    }

    public Map<String, Map<String, Serializable>> getBasicMetadataMap() {
        return basicMetadataMap;
    }
}
