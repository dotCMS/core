package com.dotmarketing.business;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.dotcms.enterprise.LicenseUtil;
import com.dotmarketing.portlets.htmlpageasset.model.IHTMLPage;
import com.dotmarketing.util.Logger;

/**
 * Provides the caching implementation for HTML pages. This approach uses a main
 * key to retrieve a cached page, and a subkey to retrieve the different
 * versions of it. With this structure, during the removal of a page, all the 
 * different versions of it will also be deleted easily. So, basically:
 * <ul>
 * 	<li>
 * 		The main key is composed of:
 * 		<ul>
 * 		<li>The page Inode.</li>
 * 		<li>The page modification date in milliseconds.</li>
 * 		</ul>
 *  </li>
 *  <li>
 * 		The subkey is composed of:
 * 		<ul>
 * 		<li>The current user ID.</li>
 * 		<li>The currently selected language ID.</li>
 * 		<li>The URL map.</li>
 * 		<li>The query String in the URL.</li>
 * 		</ul>
 *  </li>
 * </ul>
 * 
 * @author Jose Castro
 * @version 1.0
 * @since 10-17-2014
 *
 */
public class BlockPageCacheImpl extends BlockPageCache {

	private boolean canCache = false;
	private DotCacheAdministrator cache = null;
	private static String primaryCacheGroup = "BlockDirectiveHTMLPageCache";

	/**
	 * Default constructor. Initializes the internal caching structures.
	 */
	public BlockPageCacheImpl() {
		this.cache = CacheLocator.getCacheAdministrator();
		this.canCache = LicenseUtil.getLevel() > 99;
	}

	@Override
	public String getPrimaryGroup() {
		return primaryCacheGroup;
	}

	@Override
	public String[] getGroups() {
		String[] groupNames = { primaryCacheGroup };
		return groupNames;
	}

	@Override
	public void clearCache() {
		cache.flushGroup(primaryCacheGroup);
	}

	@Override
	public void add(IHTMLPage page, String value,
			PageCacheParameters pageChacheParams) {
		if (page == null || pageChacheParams == null
				|| pageChacheParams == null) {
			return;
		}
		StringBuilder key = new StringBuilder();
		key.append(page.getInode());
		key.append("_" + page.getModDate().getTime());
		String subkey = pageChacheParams.getKey();
		BlockDirectiveCacheObject cto = new BlockDirectiveCacheObject(value,
				(int) page.getCacheTTL());
		synchronized (cache) {
			try {
				// Lookup the cached versions of a page
				List<Map<String, Object>> cachedPages = (List<Map<String, Object>>) this.cache
						.get(key.toString(), primaryCacheGroup);
				if (cachedPages != null) {
					boolean updatedEntry = false;
					// Update version of page based on userid, language and
					// urlmap
					for (Map<String, Object> pageInfo : cachedPages) {
						if (pageInfo.containsKey(subkey)) {
							pageInfo.put(subkey, cto);
							updatedEntry = true;
							break;
						}
					}
					// Or add a new one
					if (!updatedEntry) {
						Map<String, Object> pageInfo = new HashMap<String, Object>();
						pageInfo.put(subkey, cto);
						cachedPages.add(pageInfo);
					}
				}
			} catch (DotCacheException e) {
				// Key does not exist in cache, then add it
			}
			List<Map<String, Object>> versions = new ArrayList<Map<String, Object>>();
			Map<String, Object> pageInfo = new HashMap<String, Object>();
			pageInfo.put(subkey, cto);
			versions.add(pageInfo);
			this.cache.put(key.toString(), versions, primaryCacheGroup);
		}
	}

	@Override
	public String get(IHTMLPage page, PageCacheParameters pageChacheParams) {
		if (!canCache || page == null || pageChacheParams == null
				|| pageChacheParams == null) {
			return null;
		}
		StringBuilder key = new StringBuilder();
		key.append(page.getInode());
		key.append("_" + page.getModDate().getTime());
		String subkey = pageChacheParams.getKey();
		synchronized (cache) {
			// Lookup the cached versions of the page based on inode and moddate
			try {
				List<Map<String, Object>> cachedPages = (List<Map<String, Object>>) this.cache
						.get(key.toString(), primaryCacheGroup);
				BlockDirectiveCacheObject cto = null;
				if (cachedPages != null) {
					for (Map<String, Object> pageInfo : cachedPages) {
						// Lookup specific page with userid, language and urlmap
						if (pageInfo.containsKey(subkey)) {
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
						// Remove page from cache if expired and get new version
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
	public void remove(IHTMLPage page) {
		try {
			StringBuilder key = new StringBuilder();
			key.append(page.getInode());
			key.append("_" + page.getModDate().getTime());
			this.cache.remove(key.toString(), primaryCacheGroup);
		} catch (Exception e) {
			Logger.debug(this, "Cache not able to be removed", e);
		}
	}

}
