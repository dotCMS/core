package com.dotmarketing.portlets.contentlet.business;

import com.dotmarketing.beans.Host;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotCacheAdministrator;
import com.dotmarketing.business.DotCacheException;
import com.dotmarketing.util.Logger;

/**
 * @author Jason Tesser
 * @since 1.6
 */
public class ContentletCacheImpl extends ContentletCache {
	
	private DotCacheAdministrator cache;
	
	private String primaryGroup = "ContentletCache";
    // region's name for the cache
    private String[] groupNames = {primaryGroup, HostCache.PRIMARY_GROUP};

	public ContentletCacheImpl() {
        cache = CacheLocator.getCacheAdministrator();
	}

	@Override
	public com.dotmarketing.portlets.contentlet.model.Contentlet add(String key, com.dotmarketing.portlets.contentlet.model.Contentlet content) {
		key = primaryGroup + key;

        // Add the key to the cache
        cache.put(key, content,primaryGroup);


		return content;
		
	}
	
	@Override
	public com.dotmarketing.portlets.contentlet.model.Contentlet get(String key) {
		key = primaryGroup + key;
    	com.dotmarketing.portlets.contentlet.model.Contentlet content = null;
    	try{
    		content = (com.dotmarketing.portlets.contentlet.model.Contentlet)cache.get(key,primaryGroup);
    	}catch (DotCacheException e) {
			Logger.debug(this, "Cache Entry not found", e);
		}
        return content;	
	}

    /* (non-Javadoc)
	 * @see com.dotmarketing.business.PermissionCache#clearCache()
	 */
	public void clearCache() {
        // clear the cache
    	for(String group : groupNames){
    		cache.flushGroup(group);
    	}
    }

    /* (non-Javadoc)
	 * @see com.dotmarketing.business.PermissionCache#remove(java.lang.String)
	 */
	public void remove(String key){
    	
    	String myKey = primaryGroup + key;
    	try{
    		cache.remove(myKey,primaryGroup);
    	}catch (Exception e) {
			Logger.debug(this, "Cache not able to be removed", e);
		} 
    	Host h = CacheLocator.getHostCache().get(key);
    	if(h != null){ 
    		CacheLocator.getHostCache().remove(h);
    	}
    }
    public String[] getGroups() {
    	return groupNames;
    }
    public String getPrimaryGroup() {
    	return primaryGroup;
    }
}
