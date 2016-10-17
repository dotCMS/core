package com.dotmarketing.portlets.virtuallinks.business;

import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotCacheAdministrator;
import com.dotmarketing.business.DotCacheException;
import com.dotmarketing.portlets.virtuallinks.model.VirtualLink;
import com.dotmarketing.util.Logger;

/**
 * @author Jason Tesser
 * @since 1.6
 */
public class VirtualLinkCacheImpl extends VirtualLinkCache {
	
	private DotCacheAdministrator cache;
	
	private String primaryGroup = "VirtualLinksCache";
    // region's name for the cache
    private String[] groupNames = {primaryGroup};

	public VirtualLinkCacheImpl() {
        cache = CacheLocator.getCacheAdministrator();
	}

	@Override
	protected VirtualLink add(String key, VirtualLink virtualLink) {
		key = sanitizeKey(key);

        // Add the key to the cache
        cache.put(key, virtualLink, primaryGroup);


		return virtualLink;
		
	}
	
	@Override
	protected VirtualLink get(String key) {
		key = sanitizeKey(key);
		VirtualLink virtualLink = null;
    	try{
    		virtualLink = (VirtualLink) cache.get(key,primaryGroup);
    	}catch (DotCacheException e) {
			Logger.debug(this, "Cache Entry not found", e);
		}
        return virtualLink;	
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
    	key = sanitizeKey(key);
    	try{
    		cache.remove(key,primaryGroup);
    	}catch (Exception e) {
			Logger.debug(this, "Cache not able to be removed", e);
		} 
    }
    public String[] getGroups() {
    	return groupNames;
    }
    public String getPrimaryGroup() {
    	return primaryGroup;
    }
    
    
    private String sanitizeKey(String key){
    	return key.replace('/', '|');
    	
    }
    
    
}
