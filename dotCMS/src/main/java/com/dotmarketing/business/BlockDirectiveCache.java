package com.dotmarketing.business;

import com.dotmarketing.business.Cachable;

import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Logger;

public abstract class BlockDirectiveCache implements Cachable {


	abstract  public void add(String key, String val, int ttl);

	abstract public String get(String key, int ttl);
	abstract public BlockDirectiveCacheObject get(String key);

	abstract  public void clearCache();
	abstract  public void remove(String key) ;

}
