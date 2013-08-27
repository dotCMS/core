package com.dotmarketing.business;

import com.dotcms.enterprise.LicenseUtil;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotCacheAdministrator;
import com.dotmarketing.business.DotCacheException;
import com.dotmarketing.util.Logger;

public class BlockDirectiveCacheImpl extends BlockDirectiveCache {

	@Override
	 public void add(String key, String value, int ttl) {
		if(key ==null || value == null){
			return;
		}
		BlockDirectiveCacheObject cto = new BlockDirectiveCacheObject(value, ttl);
		cache.put(key, cto, group);

	}
	private boolean canCache;
	private DotCacheAdministrator cache;

	private String group = "BlockDirectiveCache";
	private String secondaryGroup = "BlockDirectiveHTMLPageCache";

	// regions name for the cache
	private String[] groupNames = { group, secondaryGroup };

	public BlockDirectiveCacheImpl() {
		cache = CacheLocator.getCacheAdministrator();
		//delete everything on startup
		//clearCache();
		canCache = LicenseUtil.getLevel() > 99;
		
		
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.dotmarketing.business.PermissionCache#clearCache()
	 */
	 public void clearCache() {
		// clear the cache
		cache.flushGroup(group);
		cache.flushGroup(secondaryGroup);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.dotmarketing.business.PermissionCache#remove(java.lang.String)
	 */
	 public void remove(String key) {
		try {
			cache.remove(key, group);
		} catch (Exception e) {
			Logger.debug(this, "Cache not able to be removed", e);
		}

	}

	public String[] getGroups() {
		return groupNames;
	}

	public String getPrimaryGroup() {
		return group;
	}

	@Override
	 public String get(String key, int ttl) {
		if(!canCache)return null;
		try {
			BlockDirectiveCacheObject cto = (BlockDirectiveCacheObject) cache.get(key, group);
			if (cto == null) {
				return null;
			}
			if (cto.getCreated() + (ttl * 1000) > System.currentTimeMillis()) {
				return cto.getValue();
			} else {
				remove(key);
			}

		} catch (DotCacheException e) {
			Logger.error(this.getClass(), "cache entry :" + key + " not found");
		}
		return null;
	}
	@Override
	 public BlockDirectiveCacheObject get(String key) {
		if(!canCache)return null;
		try {
			return (BlockDirectiveCacheObject) cache.get(key, group);
		
		} catch (DotCacheException e) {
			Logger.error(this.getClass(), "cache entry :" + key + " not found");
			return null;
		}
	}
	
}
