package com.dotmarketing.portlets.contentlet.business;

import com.dotmarketing.beans.Host;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotCacheAdministrator;
import com.dotmarketing.business.DotCacheException;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.google.common.collect.ImmutableSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Jason Tesser
 * @since 1.9
 */
public class HostCacheImpl extends HostCache {
	
	final String DEFAULT_HOST = "_dotCMSDefaultHost_";
	final String SITES = "_dotSites_";

	private DotCacheAdministrator cache;
	private Map<String,Host> hostCacheMap;

    // region's name for the cache
    private String[] groupNames = {PRIMARY_GROUP, ALIAS_GROUP};

	public HostCacheImpl() {
        cache = CacheLocator.getCacheAdministrator();
        hostCacheMap = new ConcurrentHashMap<>();
	}

	@Override
	protected Host add(Host host) {
		if(host == null){
			return null;
		}
		String key = host.getIdentifier();
		String key2 =host.getHostname();

        // Add the key to the cache
		hostCacheMap.put(key, host);
		hostCacheMap.put(key2, host);
        
        if(host.isDefault()){
    		String key3 =DEFAULT_HOST;
			hostCacheMap.put(key3,host);
        }


		return host;
		
	}

	protected void addAll(final Iterable<Host> hosts){
        for(final Host host:hosts){
           add(host);
        }

//		cache.put(SITES, ImmutableSet.copyOf(hosts), PRIMARY_GROUP);
    }


	protected Set<Host> getAllSites(){
//		return (Set<Host>) cache.getNoThrow(SITES, PRIMARY_GROUP);
		return (Set<Host>) this.hostCacheMap.values();
	}

	private void clearSitesList(){
		hostCacheMap.remove(SITES);
	}

	protected Host getHostByAlias(String key) {
		Host host = null;
		//    		String hostId = (String) cache.get(key,ALIAS_GROUP);
//    		host = get(hostId);
		host = hostCacheMap.get(key);
		if(host == null){
			hostCacheMap.remove(key);
		}

		return host;
	}
	
	protected Host get(String key) {
    	Host host = null;
		host = (Host) hostCacheMap.get(key);

		return host;
	}

    /* (non-Javadoc)
	 * @see com.dotmarketing.business.PermissionCache#clearCache()
	 */
	public void clearCache() {
        // clear the cache
//        cache.flushGroup(PRIMARY_GROUP);
//        cache.flushGroup(ALIAS_GROUP);
		hostCacheMap.clear();
    }

    /* (non-Javadoc)
	 * @see com.dotmarketing.business.PermissionCache#remove(java.lang.String)
	 */
    protected void remove(Host host){
    	
    	// always remove default host
		hostCacheMap.remove(DEFAULT_HOST);

    	//remove aliases from host in cache
    	Host h = get(host.getIdentifier());

    	
    	String key = host.getIdentifier();
    	String key2 = host.getHostname();
    	
    	try{
    		hostCacheMap.remove(key);
    	}catch (Exception e) {
			Logger.debug(this, "Cache not able to be removed", e);
		} 
    	
    	try{
			hostCacheMap.remove(key2);
    	}catch (Exception e) {
			Logger.debug(this, "Cache not able to be removed", e);
    	} 
    		        	
    	clearAliasCache();
    	clearSitesList();
    	 
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
			hostCacheMap.put(alias, host);
    	}
    }
    
    
	protected void clearAliasCache() {
        // clear the alias cache
//        cache.flushGroup(ALIAS_GROUP);
		hostCacheMap.clear();
    }
}
