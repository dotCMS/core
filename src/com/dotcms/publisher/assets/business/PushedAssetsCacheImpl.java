package com.dotcms.publisher.assets.business;

import com.dotcms.publisher.assets.bean.PushedAsset;
import com.dotmarketing.business.Cachable;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotCacheAdministrator;
import com.dotmarketing.business.DotCacheException;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;


public class PushedAssetsCacheImpl implements PushedAssetsCache, Cachable {
	private final static String cacheGroup = "PushedAssetsCache";
	private final static String[] cacheGroups = {cacheGroup};
	private DotCacheAdministrator cache;

	public PushedAssetsCacheImpl() {
		cache = CacheLocator.getCacheAdministrator();
	}


	public synchronized PushedAsset getPushedAsset(String assetId, String environmentId) {
		PushedAsset asset = null;
		try {
			asset = (PushedAsset) cache.get(assetId + "|" + environmentId, cacheGroup);
		}
		catch(DotCacheException e) {
			Logger.debug(this, "PublishingEndPoint cache entry not found for: " + assetId + "|" + environmentId);
		}
		return asset;
	}

	public synchronized void add(PushedAsset asset) {
		if(asset != null) {
			cache.put(asset.getAssetId() + "|" + asset.getEnvironmentId() , asset, cacheGroup);
		}
	}

	public synchronized void removePushedAssetById(String assetId, String environmentId) {
		if(UtilMethods.isSet(assetId) && UtilMethods.isSet(environmentId) )
			cache.remove(assetId + "|" + environmentId, cacheGroup);
	}

	public String getPrimaryGroup() {
		return cacheGroup;
	}

	public String[] getGroups() {
		return cacheGroups;
	}

	public synchronized void clearCache() {
		cache.flushGroup(cacheGroup);
	}

}
