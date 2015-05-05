package com.dotmarketing.portlets.rules.business;

import com.dotmarketing.business.Cachable;

/**
 * Provides caching capabilities for the number of visits in a given site.
 * 
 * @author Jose Castro
 * @version 1.0
 * @since 04-29-2015
 *
 */
public abstract class SiteVisitCache implements Cachable {

	protected static final String PRIMARY_GROUP = "RuleConditionletSiteVisitsCache";

	@Override
	public String getPrimaryGroup() {
		return PRIMARY_GROUP;
	}

	@Override
	public String[] getGroups() {
		return new String[] { PRIMARY_GROUP };
	}

	/**
	 * Sets the total number of user visits for a specific site.
	 * 
	 * @param hostId
	 *            - The ID of the host (site).
	 * @param visits
	 *            - The number of visits a site has received.
	 * @return If {@code true}, the cache entry was added successfully.
	 *         Otherwise, returns {@code false}.
	 */
	public abstract boolean setSiteVisits(String hostId, int visits);

	/**
	 * Adds one visit to the site visit counter of a specific site.
	 * 
	 * @param hostId
	 *            - The ID of the host (site).
	 * @return If {@code true}, the cache entry was successfully incremented by
	 *         one. Otherwise, returns {@code false}
	 */
	public abstract boolean addSiteVisit(String hostId);

	/**
	 * Returns the number of site visits of a specific site.
	 * 
	 * @param hostId
	 *            - The ID of the host (site).
	 * @return The number of visits to the site. If the cache record does not
	 *         exist yet, returns -1.
	 */
	public abstract int getSiteVisits(String hostId);

}
