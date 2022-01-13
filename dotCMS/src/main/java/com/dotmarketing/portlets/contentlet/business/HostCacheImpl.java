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

	//Use only one to alias and primary
	final Map<String,Host> hostCacheMap = new ConcurrentHashMap<>();
	final String DEFAULT_HOST = "_dotCMSDefaultHost_";

    // region's name for the cache
    private String[] groupNames = {PRIMARY_GROUP};

	public HostCacheImpl() {}

	@Override
	protected Host add(final Host host) {
		Logger.info(this,"adding to cache");
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

		final List<String> aliases = APILocator.getHostAPI().parseHostAliases(host);
		for(final String alias : aliases ){
			addHostAlias(alias,host);
		}

		return host;
	}

	protected void addAll(final List<Host> hosts){
        for(final Host host:hosts){
           add(host);
        }
    }

	protected void addHostAlias(final String alias, final Host host){
		if(alias != null && host != null && UtilMethods.isSet(host.getIdentifier())){
			Logger.info(this,"adding alias cache");
			hostCacheMap.put(alias, host);
		}
	}

	protected Set<Host> getAllSites(){
		return new HashSet<>(hostCacheMap.values());
	}

	protected Host get(final String key) {
		Logger.info(this,"getting from cache " + key);
		return null != key && null != hostCacheMap.get(key) ? new Host(hostCacheMap.get(key)) : null;
	}

	protected Host getHostByAlias(final String key) {
		return get(key);
	}

	protected Host getDefaultHost(){
		return get(DEFAULT_HOST);
	}

	public void clearCache() {
		Logger.info(this,"cleaning cache");
        hostCacheMap.clear();
    }

    public String[] getGroups() {
    	return groupNames;
    }
    public String getPrimaryGroup() {
    	return PRIMARY_GROUP;
    }
}
