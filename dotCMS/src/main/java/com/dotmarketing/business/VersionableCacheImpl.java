/*
 * Created on May 30, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package com.dotmarketing.business;

import com.dotmarketing.beans.Identifier;


/**
 * @author David
 * 
 */
public class VersionableCacheImpl extends VersionableCache {

	@Override
	protected void addVersionableToCache(Versionable id) {
		// TODO Auto-generated method stub
		
	}



	@Override
	protected Versionable getVersionable(String identId) {
		// TODO Auto-generated method stub
		return null;
	}



	@Override
	protected Versionable getVersionable(Versionable versionable) {
		// TODO Auto-generated method stub
		return null;
	}



	@Override
	protected Versionable getWorkingVersionable(Versionable versionable) {
		// TODO Auto-generated method stub
		return null;
	}



	@Override
	protected Versionable getLiveVersionable(Versionable versionable) {
		// TODO Auto-generated method stub
		return null;
	}



	@Override
	protected void removFromCache(Versionable versionable) {
		// TODO Auto-generated method stub
		
	}



	@Override
	protected void removeFromCache(Identifier inode) {
		// TODO Auto-generated method stub
		
	}



	@Override
	protected void removeFromCache(String inode) {
		// TODO Auto-generated method stub
		
	}



	DotCacheAdministrator cache = null;

	protected VersionableCacheImpl() {

		cache = CacheLocator.getCacheAdministrator();
	}

	

	public void clearCache() {
		// clear the cache
		cache.flushGroup(getPrimaryGroup());
	}

}
