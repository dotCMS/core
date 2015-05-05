package com.dotmarketing.portlets.rules.business;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotCacheAdministrator;
import com.dotmarketing.business.DotCacheException;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;

/**
 * Implements the Visited URLs caching functionality. The structure will allow
 * the system to keep track of the URLs requested by a specific IP address to a
 * specific host.
 * 
 * @author Jose Castro
 * @version 1.0
 * @since 04-29-2015
 *
 */
public class VisitedUrlCacheImpl extends VisitedUrlCache {

	protected DotCacheAdministrator cache = null;

	/**
	 * Default constructor. Instantiates the {@link DotCacheAdministrator}
	 * object used to store URL information.
	 */
	public VisitedUrlCacheImpl() {
		cache = CacheLocator.getCacheAdministrator();
	}

	@Override
	public void clearCache() {
		for (String cacheGroup : getGroups()) {
			cache.flushGroup(cacheGroup);
		}
	}

	@Override
	public boolean addUrl(String ipAddress, String hostId, String url) {
		if (UtilMethods.isSet(ipAddress) && UtilMethods.isSet(url)) {
			try {
				Map<String, List<String>> hostMap = (Map<String, List<String>>) this.cache
						.get(ipAddress, PRIMARY_GROUP);
				List<String> urlList = null;
				if (hostMap == null) {
					hostMap = new HashMap<String, List<String>>();
					urlList = new ArrayList<String>();
				} else {
					urlList = hostMap.get(hostId);
				}
				if (!urlList.contains(url)) {
					urlList.add(url);
					hostMap.put(hostId, urlList);
					this.cache.put(ipAddress, hostMap, PRIMARY_GROUP);
					return true;
				}
			} catch (DotCacheException e) {
				Logger.debug(this,
						"VisitedUrlsCache entry could not be added for Host ID: "
								+ hostId + " and IP address: " + ipAddress);
			}
		}
		return false;
	}

	@Override
	public boolean addUrls(String ipAddress, String hostId, List<String> urls) {
		if (UtilMethods.isSet(ipAddress) && UtilMethods.isSet(urls)) {
			try {
				Map<String, List<String>> hostMap = (Map<String, List<String>>) this.cache
						.get(ipAddress, PRIMARY_GROUP);
				if (hostMap == null) {
					hostMap = new HashMap<String, List<String>>();
				}
				hostMap.put(hostId, urls);
				this.cache.put(ipAddress, hostMap, PRIMARY_GROUP);
				return true;
			} catch (DotCacheException e) {
				Logger.debug(this,
						"VisitedUrlsCache entry could not be added for Host ID: "
								+ hostId + " and IP address: " + ipAddress);
			}
		}
		return false;
	}

	@Override
	public List<String> getUrls(String ipAddress, String hostId) {
		if (UtilMethods.isSet(hostId)) {
			try {
				Map<String, List<String>> hostMap = (Map<String, List<String>>) this.cache
						.get(ipAddress, PRIMARY_GROUP);
				if (hostMap != null && hostMap.containsKey(hostId)) {
					return hostMap.get(hostId);
				}
			} catch (DotCacheException e) {
				Logger.debug(this,
						"VisitedUrlsCache entries not found for Host ID: "
								+ hostId + " and IP address: " + ipAddress);
			}
		}
		return null;
	}

}
