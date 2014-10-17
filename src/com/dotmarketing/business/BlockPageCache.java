package com.dotmarketing.business;

import com.dotcms.repackage.org.apache.commons.lang.StringUtils;
import com.dotmarketing.portlets.htmlpages.model.HTMLPage;

/**
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

		public PageCacheParameters(String userId, String language, String urlMap) {
			this.userId = userId;
			this.language = language;
			this.urlMap = urlMap;
		}

		public String getKey() {
			StringBuffer sb = new StringBuffer();
			sb.append(this.userId);
			sb.append("_").append(this.language);
			if (StringUtils.isNotBlank(urlMap)) {
				sb.append("_").append(this.urlMap);
			}
			return sb.toString();
		}

	}

	public abstract String getPrimaryGroup();

	public abstract String[] getGroups();

	public abstract void clearCache();

	/**
	 * Adds a new entry to the cache.
	 * 
	 * @param page
	 *            - The {@link HTMLPage} object.
	 * @param value
	 *            - The String representation of the page.
	 * @param pageChacheParams
	 *            - Values used to cache a specific page.
	 */
	abstract public void add(HTMLPage page, String value,
			PageCacheParameters pageChacheParams);

	/**
	 * Retrieves a page from the cache.
	 * 
	 * @param page
	 *            - The {@link HTMLPage} object.
	 * @param ageChacheParams
	 *            - Values used to retrieve a specific page from the cache.
	 * @return
	 */
	abstract public String get(HTMLPage page,
			PageCacheParameters ageChacheParams);

	/**
	 * Removes a page from the cache, along with all of its versions.
	 * 
	 * @param page
	 *            - The {@link HTMLPage} object.
	 */
	abstract public void remove(HTMLPage page);

}
