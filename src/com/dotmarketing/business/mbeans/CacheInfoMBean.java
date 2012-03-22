package com.dotmarketing.business.mbeans;


public interface CacheInfoMBean {

	public abstract String printRegionInfo();
	
	public abstract String printKeys(String group);
	/**
	 * Pass in a group and this will return
	 * the memory it was consuing.  The method has
	 * to flush the group to figure out memory usage.
	 * @param group
	 * @return
	 */
	public abstract String regionCacheMemorySize(String cacheRegion);
}