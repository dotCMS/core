package com.dotcms.publisher.endpoint.business;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.dotcms.publisher.endpoint.bean.PublishingEndPoint;
import com.dotmarketing.business.Cachable;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotCacheAdministrator;
import com.dotmarketing.business.DotCacheException;
import com.dotmarketing.util.Logger;

/**
 * Implementation class for the {@link PublishingEndPointCache}.
 * 
 * @author Brent Griffin
 * @since Jan 28, 2013
 * 
 */
public class PublishingEndPointCacheImpl implements PublishingEndPointCache, Cachable {

	private final static String cacheGroup = "PublishingEndPointCache";
	private final static String MAP_KEY = "publishing_endpoints_map";
	private final static String[] cacheGroups = {cacheGroup};
	private DotCacheAdministrator cache;
	private boolean isLoaded = false;

	/**
	 * Default class constructor.
	 */
	public PublishingEndPointCacheImpl() {
		cache = CacheLocator.getCacheAdministrator();		
	}

	@Override
	public synchronized boolean isLoaded()
	{	
		try {
			Object obj = cache.get(MAP_KEY, cacheGroup);
			if (obj != null) {
				isLoaded = true;
			} else {
				isLoaded = false;
			}
		} catch (DotCacheException e) {
			Logger.debug(this, "PublishingEndPoint cache not loaded yet");
			isLoaded = false;
		}
		return isLoaded;
	}

	@Override
	public synchronized void setLoaded(boolean isLoaded) {
		this.isLoaded = isLoaded;
	}

	@Override
	public synchronized List<PublishingEndPoint> getEndPoints() {
		List<PublishingEndPoint> endPoints = null;
		try {
			Map<String, PublishingEndPoint> endPointsMap = (Map<String, PublishingEndPoint>) cache
					.get(MAP_KEY, cacheGroup);
			if (endPointsMap != null) {
				endPoints = new ArrayList<PublishingEndPoint>();
				Set<String> keySet = endPointsMap.keySet();
				if (keySet != null && keySet.size() > 0) {
					for (String key : keySet) {
						endPoints.add((PublishingEndPoint) endPointsMap
								.get(key));
					}
				}
			}
		} catch (DotCacheException e) {
			Logger.debug(this, "PublishingEndPoint cache entry not found for: "
					+ MAP_KEY);
		}
		return endPoints;
	}

	@Override
	public synchronized void add(PublishingEndPoint anEndPoint) {
		cache.remove(MAP_KEY, cacheGroup);
		isLoaded = false;
	}

	@Override
	public synchronized void addAll(Map<String, PublishingEndPoint> endPoints) {
		cache.put(MAP_KEY, endPoints, cacheGroup);
	}

	@Override
	public synchronized void removeEndPointById(String id) {
		cache.remove(MAP_KEY, cacheGroup);
		isLoaded = false;
	}

	@Override
	public String getPrimaryGroup() {
		return cacheGroup;
	}

	@Override
	public String[] getGroups() {
		return cacheGroups;
	}

	@Override
	public synchronized void clearCache() {
		cache.remove(MAP_KEY, cacheGroup);
		cache.flushGroup(cacheGroup);
		isLoaded = false;
	}
	
}
