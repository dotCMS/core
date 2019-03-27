package com.dotmarketing.business;

import com.dotmarketing.beans.UserProxy;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;

public class UserProxyCacheImpl extends UserProxyCache {

private DotCacheAdministrator cache;
	
	private String primaryGroup = "UserProxyCache";
	
	private String cookieGroup = "UserProxyLongLivedCache";
	
    // region's name for the cache
    private String[] groupNames = {primaryGroup, cookieGroup};
	
    protected UserProxyCacheImpl() {
        cache = CacheLocator.getCacheAdministrator();
	}
    
	@Override
	protected UserProxy addToUserProxyCache(UserProxy userProxy) {
		String key = userProxy.getUserId();
		key = primaryGroup + key;
		if(UtilMethods.isSet(userProxy.getLongLivedCookie())){
			String cKey = primaryGroup + userProxy.getLongLivedCookie();
			cache.put(cKey, userProxy,cookieGroup);
		}
        // Add the key to the cache
        cache.put(key, userProxy,primaryGroup);
        
        return userProxy;
	}

	@Override
	public void clearCache() {
		// clear the cache
        cache.flushGroup(primaryGroup);
	}

	@Override
	protected UserProxy getUserProxyFromUserId(String userId) {
		String key = primaryGroup + userId;
		UserProxy up = null;
    	try{
    		up = (UserProxy) cache.get(key, primaryGroup);
    	}catch (DotCacheException e) {
			Logger.debug(this,"Cache Entry not found", e);
		}
        return up;
	}

	@Override
	protected UserProxy getUserProxyFromLongCookie(String longLivedCookie) {
		String key = cookieGroup + longLivedCookie;
		UserProxy up = null;
    	try{
    		up = (UserProxy) cache.get(key, cookieGroup);
    	}catch (DotCacheException e) {
			Logger.debug(this,"Cache Entry not found", e);
		}
        return up;
	}
	
	@Override
	protected void remove(UserProxy userProxy) {
		String key = userProxy.getUserId();
		key = primaryGroup + key;
    	try{
	        cache.remove(key,primaryGroup);
	        if(UtilMethods.isSet(userProxy.getLongLivedCookie())){
	        	String cKey = primaryGroup + userProxy.getLongLivedCookie();
	        	cache.remove(cKey,cookieGroup);
	        }
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
