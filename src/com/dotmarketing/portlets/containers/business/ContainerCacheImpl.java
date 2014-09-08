package com.dotmarketing.portlets.containers.business;

import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotCacheAdministrator;
import com.dotmarketing.business.DotCacheException;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;

public class ContainerCacheImpl extends ContainerCache {
	
	private DotCacheAdministrator cache;
	
	private static String primaryGroup = "ContainerCache";
	private static String CONTAINER_LIVE = "live";
	private static String CONTAINER_WORKING = "working";
    
	// region's name for the cache
    private static String[] groupNames = {primaryGroup};

	public ContainerCacheImpl() {
        cache = CacheLocator.getCacheAdministrator();
	}

	@Override
	protected Container add(String identifier, Container container) {
		
		try{
			if(UtilMethods.isSet(container)){
				String key = primaryGroup + identifier;
				
				if(container.isLive()){
					cache.put(key + CONTAINER_LIVE, container, primaryGroup);
				} 
				if(container.isWorking()){
					cache.put(key += CONTAINER_WORKING, container, primaryGroup);
				}
			}
		} catch (Exception e) {
			Logger.debug(this, "Could not add entre to cache", e);
		}	
        
		return container;
	}
	
	@Override
	protected Container getWorking(String identifier) {
		String key = primaryGroup + identifier + CONTAINER_WORKING;
		
		Container container = null;
    	try{
    		container = (Container)cache.get(key,primaryGroup);
    	}catch (DotCacheException e) {
			Logger.debug(this, "Cache Entry not found", e);
		}
    	
        return container;	
	}
	
	@Override
	protected Container getLive(String identifier) {
		String key = primaryGroup + identifier + CONTAINER_LIVE;
		
		Container container = null;
    	try{
    		container = (Container)cache.get(key,primaryGroup);
    	}catch (DotCacheException e) {
			Logger.debug(this, "Cache Entry not found", e);
		}
        
    	return container;	
	}

    public void clearCache() {
        // Clear the cache for all group.
        cache.flushGroup(primaryGroup);
    }

    public void remove(String identifier){
    	String keyWorking = primaryGroup + identifier + CONTAINER_WORKING;
    	String keyLive = primaryGroup + identifier + CONTAINER_LIVE;
    	
    	try{
    		cache.remove(keyWorking, primaryGroup);
    		cache.remove(keyLive, primaryGroup);
    		
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
