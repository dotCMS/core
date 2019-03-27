/*
 * Created on May 30, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package com.dotmarketing.business;

import java.util.List;

import com.dotmarketing.beans.Permission;
import com.dotmarketing.util.Logger;

/**
 * 
 * @author salvador & david
 * @author Carlos Rivas
 * @author Jason Tesser
 */
public class PermissionCacheImpl extends PermissionCache {
	
	private DotCacheAdministrator cache;
	
	private String primaryGroup = "PermissionCache";
	private String secondaryGroup = "ParentPermissionableCache";

	// region's name for the cache
    private String[] groupNames = {primaryGroup, secondaryGroup};

	protected PermissionCacheImpl() {
        cache = CacheLocator.getCacheAdministrator();
	}

	/* (non-Javadoc)
	 * @see com.dotmarketing.business.PermissionCache#addToPermissionCache(java.lang.String, java.util.List)
	 */
	protected List<Permission> addToPermissionCache(String key, List<Permission> permissions) {
        key = primaryGroup + key;
        // Add the key to the cache
        cache.put(key, permissions,primaryGroup);

        return permissions;
    }

    /* (non-Javadoc)
	 * @see com.dotmarketing.business.PermissionCache#getPermissionsFromCache(java.lang.String)
	 */
    @SuppressWarnings("unchecked")
	protected List<Permission> getPermissionsFromCache(String key) {
    	key = primaryGroup + key;
    	List<Permission> perms = null;
    	try{
    		perms = (List<Permission>) cache.get(key, primaryGroup);
    	}catch (DotCacheException e) {
			Logger.debug(this,"Cache Entry not found", e);
		}
        return perms;
    }

    /* (non-Javadoc)
	 * @see com.dotmarketing.business.PermissionCache#clearCache()
	 */
    public void clearCache() {
        // clear the cache
        cache.flushGroup(primaryGroup);
        cache.flushGroup(secondaryGroup);
    }

    /* (non-Javadoc)
	 * @see com.dotmarketing.business.PermissionCache#remove(java.lang.String)
	 */
    protected void remove(String key){
    	key = primaryGroup + key;
    	try{
	        cache.remove(key,primaryGroup);
    	}catch (Exception e) {
			Logger.debug(this,e.getMessage(), e);
		} 
    	try{
	        cache.remove(key,secondaryGroup);
    	}catch (Exception e) {
			Logger.debug(this,e.getMessage(), e);
		} 
    }
    public String[] getGroups() {
    	return groupNames;
    }
    public String getPrimaryGroup() {
    	return primaryGroup;
    }
    public String getSecondaryGroup() {
    	return secondaryGroup;
    }

}
