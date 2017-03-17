/**
 * 
 */
package com.dotmarketing.velocity;

import java.io.File;


import org.apache.velocity.runtime.RuntimeServices;
import org.apache.velocity.runtime.resource.Resource;
import org.apache.velocity.runtime.resource.ResourceCache;
import org.apache.velocity.runtime.resource.ResourceManager;

import com.dotmarketing.business.Cachable;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotCacheAdministrator;
import com.dotmarketing.business.DotCacheException;
import com.dotmarketing.util.Logger;
import com.liferay.util.StringUtil;

/**
 * @author Jason Tesser
 * @author Andres Olarte
 * @since 1.6.5
 * The DotResourceCache was created to allow velocities cache to be distributed across nodes
 * in a cluster.  It also allows the dotCMS to set velocity to always cache and pull from cache
 * Our services methods which generate the velocity files will handle the filling and removing of 
 * The cache.  If something is not in cache though the DotResourceLoader will be called.  
 */
public class DotResourceCache implements ResourceCache,Cachable {


	private DotCacheAdministrator cache;
	
	private String primaryGroup = "VelocityCache";

    // region's name for the cache
    private String[] groupNames = {primaryGroup};
    
    public DotResourceCache() {
    	cache = CacheLocator.getCacheAdministrator();

	}

	/* (non-Javadoc)
	 * @see org.apache.velocity.runtime.resource.ResourceCache#get(java.lang.Object)
	 */
	public Resource get(Object resourceKey) {

		String cleanedResourceKey = cleanKey(resourceKey.toString());
		String group = primaryGroup;


		String key = group + cleanedResourceKey;
		ResourceWrapper rw = null;
		try {
			rw = (ResourceWrapper) cache.get(key, group);
		} catch ( DotCacheException e ) {
			Logger.debug(this, "Cache Entry not found", e);
		}
        return rw != null ? rw.getResource() : null;	
	}
	
	/* (non-Javadoc)
	 * @see org.apache.velocity.runtime.resource.ResourceCache#initialize(org.apache.velocity.runtime.RuntimeServices)
	 */
	public void initialize(RuntimeServices rs) {
		cache = CacheLocator.getCacheAdministrator();
	}

	public void addMiss(Object resourceKey) {

	}
	
	public boolean isMiss(Object resourceKey){
	  return false;
	}
	
	/* (non-Javadoc)
	 * @see org.apache.velocity.runtime.resource.ResourceCache#put(java.lang.Object, org.apache.velocity.runtime.resource.Resource)
	 */
	public Resource put(Object resourceKey, Resource resource) {

		ResourceWrapper rw = new ResourceWrapper(resource);
		String cleanedResourceKey = cleanKey(resourceKey.toString());
		String group = primaryGroup;



		String key = group + cleanedResourceKey;
		// Add the key to the cache
		cache.put(key, rw, group);

        return rw.getResource();

	}

	/* (non-Javadoc)
	 * @see org.apache.velocity.runtime.resource.ResourceCache#remove(java.lang.Object)
	 */
	public Resource remove(Object resourceKey) {

		String cleanedResourceKey = cleanKey(resourceKey.toString());
		String group = primaryGroup;


		String key = group + cleanedResourceKey;
		ResourceWrapper rw = null;
    	try{
	       cache.remove(key,group);
		} catch ( Exception e ) {
			Logger.debug(this, e.getMessage(), e);
		} 
    	return rw != null ? rw.getResource() : null;	
	}
	
	public void clearCache() {
        cache.flushGroup(primaryGroup);

    }
	@Deprecated
	public void clearMenuCache() {
	  
    }
	
	public String[] getGroups() {
    	return groupNames;
    }
    public String getPrimaryGroup() {
    	return primaryGroup;
    }
    
    private String cleanKey(String key) {
    	if(key.startsWith(ResourceManager.RESOURCE_TEMPLATE + ""))
    		key=key.substring((ResourceManager.RESOURCE_TEMPLATE+"").length());

    	if (key.startsWith(File.separatorChar +"")) {
    		key=key.substring(1);
    	}
    	if (key.startsWith("/")) {
    		key=key.substring(1);
    	}
    	key = StringUtil.replace(key, '\\', '/');
    	return key;
    }

    @Deprecated
	public String getMenuGroup() {
		return primaryGroup;
	}






}
