package com.dotmarketing.portlets.containers.business;

import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotCacheAdministrator;
import com.dotmarketing.business.DotCacheException;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.util.Logger;

public class ContainerCacheImpl extends ContainerCache {
	
	private DotCacheAdministrator cache;
	
	private static String primaryGroup = "ContainerCache";
    // region's name for the cache
    private static String[] groupNames = {primaryGroup};

	public ContainerCacheImpl() {
        cache = CacheLocator.getCacheAdministrator();
	}

	@Override
	protected Container add(String key, Container container) {
		key = primaryGroup + key;

        // Add the key to the cache
        cache.put(key, container, primaryGroup);


		return container;
		
	}
	
	@Override
	protected Container get(String key) {
		key = primaryGroup + key;
		Container container = null;
    	try{
    		container = (Container)cache.get(key,primaryGroup);
    	}catch (DotCacheException e) {
			Logger.debug(this, "Cache Entry not found", e);
		}
        return container;	
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
    public void remove(String key){
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
