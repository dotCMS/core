package com.dotmarketing.portlets.contentlet.business;

import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotCacheAdministrator;
import com.dotmarketing.business.DotCacheException;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Jason Tesser
 * @since 1.9
 */
public class HostCacheImpl extends HostCache {
	
	final String DEFAULT_HOST = "_dotCMSDefaultHost_";
//	final String SITES = "_dotSites_";

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

        //All known aliases to the cache
		final List<String> aliases = APILocator.getHostAPI().parseHostAliases(host);
		for(final String alias : aliases ){
			addHostAlias(alias,host);
		}

		return host;
		
	}

	protected void addAll(final Iterable<Host> hosts){
        for(final Host host:hosts){
           add(host);
        }

//		cache.put(SITES, ImmutableSet.copyOf(hosts), PRIMARY_GROUP);
    }


	protected List<Host> getAllSites(){
//		return (Set<Host>) cache.getNoThrow(SITES, PRIMARY_GROUP);
		return new ArrayList<>(this.hostCacheMap.values());
	}

//	private void clearSitesList(){
//		hostCacheMap.remove(SITES);
//	}

	protected Host getHostByAlias(String key) {
//		Host host = null;
		//    		String hostId = (String) cache.get(key,ALIAS_GROUP);
//    		host = get(hostId);
//		Host host = hostCacheMap.get(key);
//		if(host == null){
//			hostCacheMap.remove(key);
//		}

		return get(key);
	}
	
	protected Host get(String key) {
		return null != key && null != hostCacheMap.get(key) ? new Host(hostCacheMap.get(key)) : null;
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
//		hostCacheMap.remove(DEFAULT_HOST);
//
//    	//remove aliases from host in cache
//    	Host h = get(host.getIdentifier());
//
//
//    	String key = host.getIdentifier();
//    	String key2 = host.getHostname();
//
//    	try{
//    		hostCacheMap.remove(key);
//    	}catch (Exception e) {
//			Logger.debug(this, "Cache not able to be removed", e);
//		}
//
//    	try{
//			hostCacheMap.remove(key2);
//    	}catch (Exception e) {
//			Logger.debug(this, "Cache not able to be removed", e);
//    	}
//
//    	clearAliasCache();
//    	clearSitesList();
		hostCacheMap.clear();
    	 
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
