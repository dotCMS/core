package com.dotmarketing.portlets.contentlet.business;

import com.dotcms.content.elasticsearch.business.ESContentFactoryImpl.TranslatedQuery;
import com.dotmarketing.business.Cachable;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.google.common.collect.ImmutableMap;

import java.util.Map;

//This interface should have default package access
public abstract class ContentletCache implements Cachable {
    public static final Map<String, Object> EMPTY_METADATA  = ImmutableMap.of(); // "**~~||%%EMPTY_METADATA%%||~~**";
    public static final Map<String, Object> CACHED_METADATA = ImmutableMap.of(); //  "**~~||%%CACHED_METADATA%%||~~**"

    /**
     * Adds a contentlet per inode, to the cache, calculates the metadata too
     * @param inode {@link String}
     * @param content {@link Contentlet}
     * @return Contentlet
     */
	public abstract Contentlet add(String inode,Contentlet content);

    /**
     * Add a contentlet using the inode to add it
     * @param content {@link com.dotmarketing.portlets.contentlet.business.Contentlet}
     * @return Contentlet
     */
    public abstract Contentlet add(Contentlet content);

    /**
     * Get a Contentlet from cache, null if does not exists.
     * @param inode
     * @return
     */
	public abstract Contentlet get(String inode);

    /**
     * Clears the cache
     */
	public abstract void clearCache();

	public abstract void remove(String key);

    public abstract void addTranslatedQuery(String key, TranslatedQuery translatedQuery);

    public abstract TranslatedQuery getTranslatedQuery(String key);

    /**
     * Gets the metadata per key and field Variable
     * @param key {@link String}
     * @param fieldVariable {@link String}
     * @return Map
     */
    public abstract Map<String, Object> getMetadata(String key, String fieldVariable);

    /**
     * Adds the metadata for this contentlet, will take the metadata fields to stores in the cache.
     * @param key {@link String}
     * @param content {@link Contentlet}
     */
    public abstract void addMetadata(String key, Contentlet content);

    /**
     * Adds the metadata Map per key, field variable
     * @param key {@link String}
     * @param fieldVariable {@link String}
     * @param metadata {@link Map}
     */
    public abstract void addMetadata(String key, String fieldVariable, Map<String, Object> metadata);
    
    
    public abstract void remove(Contentlet contentlet);
}
