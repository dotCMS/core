/**
 * 
 */
package com.dotcms.publisher.endpoint.business;

import java.util.*;
import com.dotcms.publisher.endpoint.bean.PublishingEndPoint;
import com.dotmarketing.business.Cachable;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotCacheAdministrator;
import com.dotmarketing.business.DotCacheException;
import com.dotmarketing.util.Logger;

/**
 * @author Brent Griffin
 *
 */
public class PublishingEndPointCacheImpl implements PublishingEndPointCache, Cachable {
	private final static String cacheGroup = "PublishingEndPointCache";
	private final static String[] cacheGroups = {cacheGroup};
	private final static String initialEntryKey = "_aaaa_";
	private PublishingEndPoint initialEntryObject = new PublishingEndPoint();
	private DotCacheAdministrator cache;
	private boolean isLoaded = false;

	public PublishingEndPointCacheImpl() {
		cache = CacheLocator.getCacheAdministrator();		
	}
	
	public synchronized boolean isLoaded()
	{	
		if(cache.getKeys(cacheGroup).size() == 0) {
			isLoaded = false;
			cache.put(initialEntryKey, initialEntryObject, cacheGroup);
		}
		return isLoaded;
	}
	
	public synchronized void setLoaded(boolean isLoaded) {
		this.isLoaded = isLoaded;
	}

	public synchronized List<PublishingEndPoint> getEndPoints() {
		List<PublishingEndPoint> endPoints = new ArrayList<PublishingEndPoint>();
		Set<String> keys = cache.getKeys(cacheGroup);
		for(String key : keys) {
			if(!key.equals(initialEntryKey)) {
				try {
					endPoints.add((PublishingEndPoint)cache.get(key, cacheGroup));
				}
				catch(DotCacheException e) {
					Logger.error(PublishingEndPointCacheImpl.class, "Cache does not contain object for key returned via getKeys().  Key = " + key, e);
				}
			}
		}
		return endPoints;
	}

	public synchronized PublishingEndPoint getEndPointById(String id) {
		PublishingEndPoint endPoint = null;
		try {
			endPoint = (PublishingEndPoint) cache.get(id, cacheGroup);
		}
		catch(DotCacheException e) {
			Logger.debug(this, "PublishingEndPoint cache entry not found for: " + id);
		}
		return endPoint;
	}

	public synchronized void add(PublishingEndPoint anEndPoint) {
		if(anEndPoint != null) {
			cache.put(anEndPoint.getId(), anEndPoint, cacheGroup);
		}
	}

	public synchronized void removeEndPointById(String id) {
		if(id != null)
			cache.remove(id, cacheGroup);
	}

	public String getPrimaryGroup() {
		return cacheGroup;
	}

	public String[] getGroups() {
		return cacheGroups;
	}

	public synchronized void clearCache() {
		cache.flushGroup(cacheGroup);
		isLoaded = false;
	}
}
