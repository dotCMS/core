package com.dotmarketing.portlets.contentlet.business;

import com.dotmarketing.business.Cachable;

//This interface should have default package access
public abstract class ContentletCache implements Cachable{

	public abstract com.dotmarketing.portlets.contentlet.model.Contentlet add(String key,com.dotmarketing.portlets.contentlet.model.Contentlet content);

	public abstract com.dotmarketing.portlets.contentlet.model.Contentlet get(String key);

	public abstract void clearCache();

	public abstract void remove(String key);

}