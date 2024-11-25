package com.dotmarketing.business.cache.provider.timedcache;

import com.dotmarketing.business.cache.provider.caffine.CaffineCache;
/**
 * Provides a timed cache implementation using Caffeine.
 * @deprecated
 * This class is deprecated and will be removed in a future version - use CaffineCache instead as it
 * provides a more robust and efficient implementation.
 * <p> Use {@link com.dotmarketing.business.cache.provider.caffine.CaffineCache} instead.
 */
@Deprecated(since = "24.12", forRemoval = false)
public class TimedCacheProvider extends CaffineCache {


	private static final long serialVersionUID = 1L;

	@Override
	public String getName() {
		return "Timed Cache Provider";
	}

	@Override
	public String getKey() {
		return "Timed Cache Provider";
	}





}
