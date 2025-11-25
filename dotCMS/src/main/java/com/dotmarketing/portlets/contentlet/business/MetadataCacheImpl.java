package com.dotmarketing.portlets.contentlet.business;

import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotCacheAdministrator;
import com.dotmarketing.util.UtilMethods;
import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class MetadataCacheImpl implements MetadataCache {

    private final DotCacheAdministrator cache;

    private String metadataGroup = "AssetMetadataCache";

    private String[] groupNames = {metadataGroup};

    MetadataCacheImpl(final DotCacheAdministrator cache) {
        this.cache = cache;
    }

    public MetadataCacheImpl() {
        this(CacheLocator.getCacheAdministrator());
    }

    @Override
    public String getPrimaryGroup() {
        return metadataGroup;
    }

    @Override
    public String[] getGroups() {
        return Arrays.copyOf(groupNames, groupNames.length);
    }

    @Override
    public void clearCache() {
        for(String group : groupNames){
            cache.flushGroup(group);
        }
    }

    @Override
    public void addMetadataMap(final String key, final Map<String, Serializable> metadataMap) {
        cache.put(key, UtilMethods.isSet(metadataMap)?
                metadataMap:EMPTY_METADATA_MAP, metadataGroup);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Serializable> getMetadataMap(final String key) {
        Map<String, Serializable> cachedMap = (Map<String, Serializable>)cache.getNoThrow(key, metadataGroup);
        // The cache is shared and if returning the cachedMap directly, the contents could be modified by the caller impacting other threads
        return cachedMap != null ? new HashMap<>(cachedMap) : null;
    }

    public void removeMetadata(final String key) {
        cache.remove(key, metadataGroup);
    }

}
