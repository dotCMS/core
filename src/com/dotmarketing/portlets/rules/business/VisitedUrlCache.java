package com.dotmarketing.portlets.rules.business;

import java.util.List;

import com.dotmarketing.business.Cachable;

/**
 * Provides caching capabilities to store the URLs a client (IP address) has
 * visited in a specific site.
 * 
 * @author Jose Castro
 * @version 1.0
 * @since 04-29-2015
 *
 */
public abstract class VisitedUrlCache implements Cachable {

	protected static final String PRIMARY_GROUP = "RuleConditionletVisitedUrlsCache";

	@Override
	public String getPrimaryGroup() {
		return PRIMARY_GROUP;
	}

	@Override
	public String[] getGroups() {
		return new String[] { PRIMARY_GROUP };
	}

	/**
	 * Adds the URL under a specific site that has been visited by a specific IP
	 * address.
	 * 
	 * @param ipAddress
	 *            - The client (IP address) that visited the URL.
	 * @param hostId
	 *            - The ID of the site (host).
	 * @param url
	 *            - The visited URL.
	 * @return If the cache entry was added successfully, return {@code true}.
	 *         Otherwise, returns {@code false}.
	 */
	public abstract boolean addUrl(String ipAddress, String hostId, String url);

	/**
	 * Adds the {@code List<String>} of URLs under a specific site that have
	 * been visited by a specific IP address.
	 * 
	 * @param ipAddress
	 *            - The client (IP address) that visited the URLs.
	 * @param hostId
	 *            - The ID of the site (host).
	 * @param urls
	 *            - The list of visited URL.
	 * @return If the cache entry was added successfully, return {@code true}.
	 *         Otherwise, returns {@code false}.
	 */
	public abstract boolean addUrls(String ipAddress, String hostId,
			List<String> urls);

	/**
	 * Returns the {@code List<String>} of URLs under a specific site that have
	 * been visited by a specific IP address.
	 * 
	 * @param ipAddress
	 *            - The client (IP address) that visited the URLs.
	 * @param hostId
	 *            - The ID of the site (host).
	 * @return The {@code List<String>} of visited URLs.
	 */
	public abstract List<String> getUrls(String ipAddress, String hostId);

}
