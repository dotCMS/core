package com.dotmarketing.portlets.contentlet.business;

import com.dotmarketing.beans.Host;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotCacheAdministrator;
import com.dotmarketing.business.DotCacheException;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;

/**
 * @author Jason Tesser
 * @since 1.9
 */
public class HostCacheImpl extends HostCache {
	
	final String DEFAULT_HOST = "_dotCMSDefaultHost_";
	
	private DotCacheAdministrator cache;
	

    // region's name for the cache
    private String[] groupNames = {PRIMARY_GROUP, ALIAS_GROUP};

	public HostCacheImpl() {
        cache = CacheLocator.getCacheAdministrator();
	}

	@Override
	protected Host add(Host host) {
		if(host == null){
			return null;
		}
		String key = host.getIdentifier();
		String key2 =host.getHostname();

        // Add the key to the cache
        cache.put(key, host,PRIMARY_GROUP);
        cache.put(key2, host,PRIMARY_GROUP);
        
        if(host.isDefault()){
    		String key3 =DEFAULT_HOST;
        	cache.put(key3,host,PRIMARY_GROUP);
        }
        


		return host;
		
	}
	
	protected Host getHostByAlias(String key) {
		Host host = null;
    	try{
    		String hostId = (String) cache.get(key,ALIAS_GROUP);
    		host = get(hostId);
    		if(host == null){
    			cache.remove(key, ALIAS_GROUP);
    		}
    	}catch (DotCacheException e) {
			Logger.debug(this, "Cache Entry not found", e);
		}

        return host;
	}
	
	protected Host get(String key) {
    	Host host = null;
    	try{
    		host = (Host) cache.get(key,PRIMARY_GROUP);
    	}catch (DotCacheException e) {
			Logger.debug(this, "Cache Entry not found", e);
		}

        return host;	
	}

    /* (non-Javadoc)
	 * @see com.dotmarketing.business.PermissionCache#clearCache()
	 */
	public void clearCache() {
        // clear the cache
        cache.flushGroup(PRIMARY_GROUP);
        cache.flushGroup(ALIAS_GROUP);
    }

    /* (non-Javadoc)
	 * @see com.dotmarketing.business.PermissionCache#remove(java.lang.String)
	 */
    protected void remove(Host host){
    	
    	// always remove default host
    	String _defaultHost =PRIMARY_GROUP +DEFAULT_HOST;
    	cache.remove(_defaultHost,PRIMARY_GROUP);

    	//remove aliases from host in cache
    	Host h = get(host.getIdentifier());

    	
    	String key = host.getIdentifier();
    	String key2 = host.getHostname();
    	
    	try{
    		cache.remove(key,PRIMARY_GROUP);
    	}catch (Exception e) {
			Logger.debug(this, "Cache not able to be removed", e);
		} 
    	
    	try{
    		cache.remove(key2,PRIMARY_GROUP);
    	}catch (Exception e) {
			Logger.debug(this, "Cache not able to be removed", e);
    	} 
    		        	
    	clearAliasCache();
    	 
    }

    public String[] getGroups() {
    	return groupNames;
    }
    public String getPrimaryGroup() {
    	return PRIMARY_GROUP;
    }
    
    
    protected Host getDefaultHost(){
    	return get(DEFAULT_HOST);
    }

    protected void addHostAlias(String alias, Host host){
    	if(alias != null && host != null && UtilMethods.isSet(host.getIdentifier())){
    		cache.put(alias, host.getIdentifier(),ALIAS_GROUP);
    	}
    }
    
    
	protected void clearAliasCache() {
        // clear the alias cache
        cache.flushGroup(ALIAS_GROUP);
    }
}
