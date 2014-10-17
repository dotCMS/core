package com.dotmarketing.business;

import java.util.Map;

import com.dotmarketing.portlets.htmlpages.model.HTMLPage;

public abstract class BlockDirectiveCache implements Cachable {

	abstract public void add(String key, String val, int ttl);

	abstract public void add(HTMLPage page, String value,
			Map<String, String> params);

	abstract public String get(String key, int ttl);
	
	abstract public String get(HTMLPage page, Map<String, String> pageChacheParams);

	abstract public BlockDirectiveCacheObject get(String key);

	abstract public void clearCache();

	abstract public void remove(String key);
	
	abstract public void remove(HTMLPage page);

}
