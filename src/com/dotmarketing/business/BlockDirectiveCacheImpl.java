package com.dotmarketing.business;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.dotcms.enterprise.LicenseUtil;
import com.dotmarketing.portlets.htmlpages.model.HTMLPage;
import com.dotmarketing.util.Logger;

public class BlockDirectiveCacheImpl extends BlockDirectiveCache {

	private boolean canCache;
	private DotCacheAdministrator cache;

	private String group = "BlockDirectiveCache";
	private String secondaryGroup = "BlockDirectiveHTMLPageCache";

	// regions name for the cache
	private String[] groupNames = { group, secondaryGroup };
	
	@Override
	 public void add(String key, String value, int ttl) {
		if(key ==null || value == null){
			return;
		}
		BlockDirectiveCacheObject cto = new BlockDirectiveCacheObject(value, ttl);
		cache.put(key, cto, group);
	}
	
	/**
	 * 
	 * @param page
	 * @param pageChacheParams
	 */
	public void add(HTMLPage page, String value,
			Map<String, String> pageChacheParams) {
		if (page == null || pageChacheParams == null
				|| pageChacheParams.size() == 0) {
			return;
		}
		StringBuilder key = new StringBuilder();
		key.append(page.getInode());
		key.append("_" + page.getModDate().getTime());
		synchronized (cache) {
			StringBuilder subkey = new StringBuilder();
			subkey.append(pageChacheParams.get("userid"));
			subkey.append("_").append(pageChacheParams.get("language"));
			subkey.append("_").append(pageChacheParams.get("urlmap"));
			BlockDirectiveCacheObject cto = new BlockDirectiveCacheObject(
					value, (int) page.getCacheTTL());
			try {
				// Lookup the cached versions of a page
				List<Map<String, Object>> cachedPages = (List<Map<String, Object>>) this.cache
						.get(key.toString(), this.secondaryGroup);
				if (cachedPages != null) {
					boolean updatedEntry = false;
					for (Map<String, Object> pageInfo : cachedPages) {
						if (pageInfo.containsKey(subkey.toString())) {
							pageInfo.put(subkey.toString(), cto);
							updatedEntry = true;
							break;
						}
					}
					if (!updatedEntry) {
						Map<String, Object> pageInfo = new HashMap<String, Object>();
						pageInfo.put(subkey.toString(), cto);
						cachedPages.add(pageInfo);
					}
				}
			} catch (DotCacheException e) {
				// Key does not exist in cache
			}
			List<Map<String, Object>> versions = new ArrayList<Map<String, Object>>();
			Map<String, Object> pageInfo = new HashMap<String, Object>();
			pageInfo.put(subkey.toString(), cto);
			versions.add(pageInfo);
			this.cache.put(key.toString(), versions, this.secondaryGroup);
		}
	}

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
	 
	public void remove(HTMLPage page) {
		try {
			StringBuilder key = new StringBuilder();
			key.append(page.getInode());
			key.append("_" + page.getModDate().getTime());
			this.cache.remove(key.toString(), this.secondaryGroup);
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
	
	public String get(HTMLPage page, Map<String, String> pageChacheParams) {
		if (!canCache || page == null || pageChacheParams == null
				|| pageChacheParams.size() == 0) {
			return null;
		}
		StringBuilder key = new StringBuilder();
		key.append(page.getInode());
		key.append("_" + page.getModDate().getTime());
		synchronized (cache) {
			StringBuilder subkey = new StringBuilder();
			subkey.append(pageChacheParams.get("userid"));
			subkey.append("_").append(pageChacheParams.get("language"));
			subkey.append("_").append(pageChacheParams.get("urlmap"));
			// Lookup the cached versions of a page
			try {
				List<Map<String, Object>> cachedPages = (List<Map<String, Object>>) this.cache
						.get(key.toString(), this.secondaryGroup);
				BlockDirectiveCacheObject cto = null;
				if (cachedPages != null) {
					for (Map<String, Object> pageInfo : cachedPages) {
						if (pageInfo.containsKey(subkey.toString())) {
							cto = (BlockDirectiveCacheObject) pageInfo
									.get(subkey);
							break;
						}
					}
					if (cto != null
							&& cto.getCreated()
									+ ((int) page.getCacheTTL() * 1000) > System
										.currentTimeMillis()) {
						return cto.getValue();
					} else {
						remove(page);
					}
				}
			} catch (DotCacheException e) {
				Logger.error(this.getClass(), "cache entry :" + key.toString()
						+ " not found");

			}
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
