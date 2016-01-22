package com.dotmarketing.business;

import com.dotcms.repackage.org.apache.commons.lang.StringUtils;
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
public abstract class BlockPageCache implements Cachable {

	/**
	 * Utility class used to keep the parameters used to identify a page cache.
	 * 
	 * @author Jose Castro
	 * @version 1.0
	 * @since 10-17-2014
	 *
	 */
	public static class PageCacheParameters {
		private String userId = null;
		private String language = null;
		private String urlMap = null;
		private String queryString = null;
		private String persona = null;
		/**
		 * Creates an object with a series of page-specific parameters to try 
		 * to uniquely identify a page request.
		 * 
		 * @param userId
		 *            - The ID of the current user.
		 * @param language
		 *            - The language ID.
		 * @param urlMap
		 * @param queryString
		 *            - The current query String in the page URL.
		 */
		public PageCacheParameters(String userId, String language,
				String urlMap, String queryString, String persona) {
			this.userId = userId;
			this.language = language;
			this.urlMap = urlMap;
			this.queryString = queryString;
			this.persona = persona;
		}

		/**
		 * Generates the page subkey that will be used in the page cache. This
		 * key will represent a specific version of the page.
		 * 
		 * @return The subkey which is specific for a page.
		 */
		public String getKey() {
			StringBuffer sb = new StringBuffer();
			sb.append(this.userId);
			sb.append("_").append(this.language);
			if (StringUtils.isNotBlank(this.urlMap)) {
				sb.append("_").append(this.urlMap);
			}
			if (StringUtils.isNotBlank(this.queryString)) {
				sb.append("_").append(this.queryString);
			}
			if (StringUtils.isNotBlank(this.persona)) {
				sb.append("_").append(this.persona);
			}
			return sb.toString();
		}

	}

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
	 * @param pageChacheParams
	 *            - Values used to cache a specific page.
	 */
	abstract public void add(IHTMLPage page, String value,
			PageCacheParameters pageChacheParams);

	/**
	 * Retrieves a page from the cache.
	 * 
	 * @param page
	 *            - The {@link IHTMLPage} object.
	 * @param pageChacheParams
	 *            - Values used to retrieve a specific page from the cache.
	 * @return
	 */
	abstract public String get(IHTMLPage page,
			PageCacheParameters pageChacheParams);

	/**
	 * Removes a page from the cache, along with all of its versions.
	 * 
	 * @param page
	 *            - The {@link IHTMLPage} object.
	 */
	abstract public void remove(IHTMLPage page);

}
