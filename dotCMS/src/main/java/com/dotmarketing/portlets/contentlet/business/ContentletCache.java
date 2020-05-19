package com.dotmarketing.portlets.contentlet.business;

import com.dotcms.content.elasticsearch.business.ESContentFactoryImpl.TranslatedQuery;
import com.dotmarketing.business.Cachable;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.google.common.collect.ImmutableMap;

import java.util.Map;

//This interface should have default package access
public abstract class ContentletCache implements Cachable {

    public static final Map<String, Object> EMPTY_METADATA_MAP = ImmutableMap.of();
    public static final String EMPTY_METADATA = "**~~||%%EMPTY_METADATA%%||~~**";
    public static final String CACHED_METADATA = "**~~||%%CACHED_METADATA%%||~~**";

    /**
     * Prefix for the metadata stored as {@link Map} in the cache.
     */
    public static final String META_DATA_MAP_KEY = "FileAssetMetadataCacheMap";

	public abstract Contentlet add(String inode,Contentlet content);
    public abstract Contentlet add(Contentlet content);
	public abstract Contentlet get(String inode);

	public abstract void clearCache();

	public abstract void remove(String key);

    public abstract void addMetadataMap(String key, Map<String, Object> metadataMap);

    public abstract Map<String, Object> getMetadataMap(String key);

	public abstract void addMetadata(String key, Contentlet content);
	
    public abstract void addTranslatedQuery(String key, TranslatedQuery translatedQuery);

    public abstract TranslatedQuery getTranslatedQuery(String key);
    
    public abstract String getMetadata(String key);

    public abstract void addMetadata(String key, String metadata);
    
    
    public abstract void remove(Contentlet contentlet);
}
