package com.dotmarketing.portlets.contentlet.business;

import com.dotmarketing.business.Cachable;
import com.dotmarketing.portlets.contentlet.model.Contentlet;

//This interface should have default package access
public abstract class ContentletCache implements Cachable {
    public static final String EMPTY_METADATA = "**~~||%%EMPTY_METADATA%%||~~**";
    public static final String CACHED_METADATA = "**~~||%%CACHED_METADATA%%||~~**";

	public abstract com.dotmarketing.portlets.contentlet.model.Contentlet add(String key,com.dotmarketing.portlets.contentlet.model.Contentlet content);

	public abstract com.dotmarketing.portlets.contentlet.model.Contentlet get(String key);

	public abstract void clearCache();

	public abstract void remove(String key);

    public abstract void addMetadata(String key, Contentlet content);

    public abstract String getMetadata(String key);

    public abstract void addMetadata(String key, String metadata);
}