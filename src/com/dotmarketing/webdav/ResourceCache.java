package com.dotmarketing.webdav;

import com.dotmarketing.business.Cachable;

//This interface should have default package access
public abstract class ResourceCache implements Cachable {

	abstract protected Long add(String key, Long timeOfPublishing);

	abstract protected Long get(String key);

	abstract public void clearCache();

	abstract public void remove(String key);
}