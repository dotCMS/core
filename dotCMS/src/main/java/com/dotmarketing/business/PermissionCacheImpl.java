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

	// region's name for the cache
    private String[] groupNames = {primaryGroup};

	protected PermissionCacheImpl() {
        cache = CacheLocator.getCacheAdministrator();
	}

	/* (non-Javadoc)
	 * @see com.dotmarketing.business.PermissionCache#addToPermissionCache(java.lang.String, java.util.List)
	 */
	protected List<Permission> addToPermissionCache(final String key, List<Permission> permissions) {
	    if(permissions!=null && permissions.isEmpty()) {
	        Logger.warn(this.getClass(), ()->" !!! Putting an empty list of permissions in the cache for asset:" + key +". Every asset should have at least 1 permission (or inherited permission) associated with it");
	    }

        // Add the key to the cache
        cache.put(primaryGroup + key, permissions,primaryGroup);

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

    }
    public String[] getGroups() {
    	return groupNames;
    }
    public String getPrimaryGroup() {
    	return primaryGroup;
    }


}
