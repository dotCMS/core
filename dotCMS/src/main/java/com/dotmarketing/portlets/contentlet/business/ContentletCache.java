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

	public abstract Contentlet add(String inode,Contentlet content);
    public abstract Contentlet add(Contentlet content);
	public abstract Contentlet get(String inode);

	public abstract void clearCache();

	public abstract void remove(String key);

    public abstract void addTranslatedQuery(String key, TranslatedQuery translatedQuery);

    public abstract TranslatedQuery getTranslatedQuery(String key);
    
    public abstract Map<String, Object> getMetadata(String key, String fieldVariable);

    public abstract void addMetadata(String key, Contentlet content);

    public abstract void addMetadata(String key, String fieldVariable, Map<String, Object> metadata);
    
    
    public abstract void remove(Contentlet contentlet);
}
