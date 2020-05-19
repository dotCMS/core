package com.dotcms.storage;

import java.util.Map;

/**
 * Encapsulates a collection with field with full metadata generation and
 * all fields with basic metadata
 */
public class ContentletMetadata {

    private final Map<String, Map<String, Object>> fullMetadataMap;
    private final Map<String, Map<String, Object>> basicMetadataMap;

    public ContentletMetadata(final Map<String, Map<String, Object>> fullMetadataMap,
                              final Map<String, Map<String, Object>> basicMetadataMap) {
        this.fullMetadataMap = fullMetadataMap;
        this.basicMetadataMap = basicMetadataMap;
    }

    public Map<String, Map<String, Object>> getFullMetadataMap() {
        return fullMetadataMap;
    }

    public Map<String, Map<String, Object>> getBasicMetadataMap() {
        return basicMetadataMap;
    }
}
