package com.dotmarketing.business;

public interface Cachable {

	/**
	 * Use to get the primary group/region for the concrete cache
	 * @return
	 */
	public String getPrimaryGroup();
	
	/**
	 * Use to get all groups the concrete cache belongs to
	 * @return
	 */
	public String[] getGroups();
	
	public void clearCache();
}
