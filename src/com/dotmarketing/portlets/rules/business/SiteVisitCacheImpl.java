package com.dotmarketing.portlets.rules.business;

import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotCacheAdministrator;
import com.dotmarketing.business.DotCacheException;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;

/**
 * Implements the site visits caching functionality. The structure will allow
 * the system to keep track of the number of visits by site, which is generated
 * after Quartz for populating the analytics tables is finished.
 * 
 * @author Jose Castro
 * @version 1.0
 * @since 04-29-2015
 *
 */
public class SiteVisitCacheImpl extends SiteVisitCache {

	protected DotCacheAdministrator cache = null;
	protected static final String APPENDER = "##";

	/**
	 * Default constructor. Instantiates the {@link DotCacheAdministrator}
	 * object used to store the site visits counter.
	 */
	public SiteVisitCacheImpl() {
		this.cache = CacheLocator.getCacheAdministrator();
	}

	@Override
	public void clearCache() {
		for (String cacheGroup : getGroups()) {
			this.cache.flushGroup(cacheGroup);
		}
	}

	@Override
	public boolean setSiteVisits(String userId, String hostId, int visits) {
		if (UtilMethods.isSet(hostId)) {
			this.cache.put(userId + APPENDER + hostId, visits, PRIMARY_GROUP);
			return true;
		}
		return false;
	}

	@Override
	public int getSiteVisits(String userId, String hostId) {
		if (UtilMethods.isSet(hostId)) {
			try {
				Object result = this.cache.get(userId + APPENDER + hostId,
						PRIMARY_GROUP);
				if (UtilMethods.isSet(result)) {
					return (int) result;
				}
			} catch (DotCacheException e) {
				Logger.debug(this,
						"SiteVisitCache entry could not be retrieved for Host ID: "
								+ hostId + " and user " + userId);
			}
		}
		return -1;
	}

}
