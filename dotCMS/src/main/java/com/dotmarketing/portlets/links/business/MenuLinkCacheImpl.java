package com.dotmarketing.portlets.links.business;

import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotCacheAdministrator;
import com.dotmarketing.business.DotCacheException;
import com.dotmarketing.portlets.links.model.Link;
import com.dotmarketing.util.Logger;

public class MenuLinkCacheImpl extends MenuLinkCache {
	
	private DotCacheAdministrator cache;
	
	private static String primaryGroup = "MenuLinkCache";
    // region's name for the cache
    private static String[] groupNames = {primaryGroup};

	public MenuLinkCacheImpl() {
        cache = CacheLocator.getCacheAdministrator();
	}

	@Override
	protected Link add(String key, Link menuLink) {
		key = primaryGroup + key;

        // Add the key to the cache
        cache.put(key, menuLink, primaryGroup);


		return menuLink;
		
	}
	
	@Override
	protected Link get(String key) {
		key = primaryGroup + key;
		Link menuLink = null;
    	try{
    		menuLink = (Link)cache.get(key,primaryGroup);
    	}catch (DotCacheException e) {
			Logger.debug(this, "Cache Entry not found", e);
		}
        return menuLink;	
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
			Logger.debug(this, "Cache not able to be removed", e);
		} 
    }
    public String[] getGroups() {
    	return groupNames;
    }
    public String getPrimaryGroup() {
    	return primaryGroup;
    }
}
