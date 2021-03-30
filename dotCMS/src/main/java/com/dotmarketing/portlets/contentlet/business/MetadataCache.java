package com.dotmarketing.portlets.contentlet.business;

import com.dotmarketing.business.Cachable;
import com.google.common.collect.ImmutableMap;
import java.io.Serializable;
import java.util.Map;

/**
 * Metadata cache
 */
public interface MetadataCache extends Cachable {

    Map<String, Object> EMPTY_METADATA_MAP = ImmutableMap.of();

    /**
     * Add the metadata as a map, to the cache
     * @param key {@link String}
     * @param metadataMap {@link Map}
     */
    void addMetadataMap(String key, Map<String, Serializable> metadataMap);

    /**
     * Gets the metadata as a map
     * @param key {@link String}
     * @return Map
     */
    Map<String, Serializable> getMetadataMap(String key);

    /**
     *
     * @param key
     */
    void removeMetadata(final String key);

}
