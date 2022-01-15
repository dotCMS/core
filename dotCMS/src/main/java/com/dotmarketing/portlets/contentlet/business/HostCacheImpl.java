package com.dotmarketing.portlets.contentlet.business;

import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotCacheAdministrator;
import com.dotmarketing.business.DotCacheException;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.google.common.collect.ImmutableSet;
import java.util.HashSet;
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
    }


	protected Set<Host> getAllSites(){
		return new HashSet<>(hostCacheMap.values());
	}

	protected Host getHostByAlias(String key) {
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
		hostCacheMap.clear();
    }

    /* (non-Javadoc)
	 * @see com.dotmarketing.business.PermissionCache#remove(java.lang.String)
	 */
    protected void remove(Host host){
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
		hostCacheMap.clear();
    }
}
