package com.dotmarketing.portlets.rules.business;

import com.dotmarketing.business.Cachable;

/**
 * Provides caching capabilities for the number of times users have visited a
 * given site.
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
	 * @param userId
	 *            - The ID of the currently logged in user.
	 * @param hostId
	 *            - The ID of the host (site).
	 * @param visits
	 *            - The number of visits a site has received.
	 * @return If {@code true}, the cache entry was added successfully.
	 *         Otherwise, returns {@code false}.
	 */
	public abstract boolean setSiteVisits(String userId, String hostId,
			int visits);

	/**
	 * Returns the number of times a specific user has visited a specific site.
	 * 
	 * @param userId
	 *            - The ID of the currently logged in user.
	 * @param hostId
	 *            - The ID of the host (site).
	 * @return The number of visits to the site. If the cache record does not
	 *         exist yet, returns -1.
	 */
	public abstract int getSiteVisits(String userId, String hostId);

}
