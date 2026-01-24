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
	 * @deprecated Use {@link #addBytes(IHTMLPage, byte[], PageCacheParameters)} for better performance
	 */
	@Deprecated
	abstract public void add(IHTMLPage page, String value,
			PageCacheParameters pageCacheParams);

	/**
	 * Adds a new entry to the cache using raw bytes.
	 * This is more efficient than {@link #add(IHTMLPage, String, PageCacheParameters)}
	 * as it avoids the byte[] → String → byte[] conversion roundtrip.
	 *
	 * @param page            - The {@link IHTMLPage} object.
	 * @param content         - The UTF-8 encoded byte array of the page content.
	 * @param pageCacheParams - Values used to cache a specific page.
	 */
	abstract public void addBytes(IHTMLPage page, byte[] content,
			PageCacheParameters pageCacheParams);

	/**
	 * Retrieves a page from the cache.
	 *
	 * @param page
	 *            - The {@link IHTMLPage} object.
	 * @param pageCacheParams
	 *            - Values used to retrieve a specific page from the cache.
	 * @return The cached page content as a String, or null if not found
	 * @deprecated Use {@link #getBytes(IHTMLPage, PageCacheParameters)} for better performance
	 */
	@Deprecated
	abstract public String get(IHTMLPage page,
			PageCacheParameters pageCacheParams);

	/**
	 * Retrieves a page from the cache as raw bytes.
	 * This is more efficient than {@link #get(IHTMLPage, PageCacheParameters)}
	 * as it avoids creating intermediate String objects.
	 *
	 * @param page            - The {@link IHTMLPage} object.
	 * @param pageCacheParams - Values used to retrieve a specific page from the cache.
	 * @return The cached page content as UTF-8 bytes, or null if not found
	 */
	abstract public byte[] getBytes(IHTMLPage page,
			PageCacheParameters pageCacheParams);

	/**
	 * Removes a page from the cache, along with all of its versions.
	 *
	 * @param page
	 *            - The {@link IHTMLPage} object.
	 */
	abstract public void remove(IHTMLPage page);

}
