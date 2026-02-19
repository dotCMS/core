package com.dotmarketing.business;

import com.dotmarketing.portlets.htmlpageasset.model.IHTMLPage;

/**
 * This cache will keep a set of different {@link HTMLPage} objects and their 
 * different versions, i.e., the specific user ID, language, etc., used when 
 * they were requested.
 * 
 * @author Jose Castro
 * @version 1.0
 * @since 10-17-2014
 *
 */
public abstract class StaticPageCache implements Cachable {

	@Override
	public abstract String getPrimaryGroup();

	@Override
	public abstract String[] getGroups();

	@Override
	public abstract void clearCache();

	/**
	 * Adds a new entry to the cache.
	 * 
	 * @param page
	 *            - The {@link IHTMLPage} object.
	 * @param value
	 *            - The String representation of the page.
	 * @param pageCacheParams
	 *            - Values used to cache a specific page.
	 */
	abstract public void add(IHTMLPage page, String value,
			PageCacheParameters pageCacheParams);

	/**
	 * Retrieves a page from the cache.
	 * 
	 * @param page
	 *            - The {@link IHTMLPage} object.
	 * @param pageCacheParams
	 *            - Values used to retrieve a specific page from the cache.
	 * @return
	 */
	abstract public String get(IHTMLPage page,
			PageCacheParameters pageCacheParams);

	/**
	 * Removes a page from the cache, along with all of its versions.
	 * 
	 * @param page
	 *            - The {@link IHTMLPage} object.
	 * @param pageCacheParams
	 *            - Values used to delete a specific page from the cache.
	 */
	abstract public void remove(IHTMLPage page,
			PageCacheParameters pageCacheParams);

}
