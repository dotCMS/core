package com.dotmarketing.portlets.contentlet.business;

import com.dotmarketing.beans.Host;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotCacheAdministrator;
import com.dotmarketing.business.DotCacheException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.google.common.collect.ImmutableSet;

import java.util.HashSet;
import java.util.Set;
import org.apache.commons.lang.StringUtils;

/**
 * @author Jason Tesser
 * @since 1.9
 */
public class HostCacheImpl extends HostCache {
	
	final String DEFAULT_HOST = "_dotCMSDefaultHost_";
	final String SITES = "_dotSites_";

	private DotCacheAdministrator cache;
	

    // region's name for the cache
    private final String[] groupNames = {
		PRIMARY_GROUP, ALIAS_GROUP, HOST_LIVE_GROUP,
		NOT_FOUND_BY_ID_GROUP, NOT_FOUND_BY_NAME_GROUP
	};

	public HostCacheImpl() {
        cache = CacheLocator.getCacheAdministrator();
	}

	@Override
	protected Host add(final Host host) throws DotDataException, DotSecurityException {
		if(host == null){
			return null;
		}
		String key = host.getIdentifier();
		String key2 =host.getHostname();

		cache.remove(key, NOT_FOUND_BY_ID_GROUP);
		cache.remove(key2, NOT_FOUND_BY_NAME_GROUP);

        // Add the key to the cache
        final boolean isLive = host.isLive();
		final boolean onlyLiveVersion = isLive && !host.isWorking();

		if (!onlyLiveVersion) {
			cache.put(key, host, PRIMARY_GROUP);
			cache.put(key2, host, PRIMARY_GROUP);
		}

		if (isLive) {
			cache.put(key, host, HOST_LIVE_GROUP);
			cache.put(key2, host, HOST_LIVE_GROUP);
		}

        if(host.isDefault()){
			if (!onlyLiveVersion) {
				cache.put(DEFAULT_HOST, host, PRIMARY_GROUP);
			}
			if (isLive) {
				cache.put(DEFAULT_HOST, host, HOST_LIVE_GROUP);
			}
        }


		return host;
		
	}

	/**
	 * Add all the hosts to the cache
	 * @param hosts 		the hosts to add to the cache (for hosts with working and live versions
	 *                      this list should contain only the working version)
	 * @param liveHosts		the live hosts to add to the cache (for hosts with working and live versions
	 *                      this list should contain only the live version)
	 * @throws DotDataException An error occurred when accessing the data source.
	 * @throws DotSecurityException The specified user does not have the required permissions
	 * to perform this operation
	 */
	protected void addAll(final Iterable<Host> hosts,
						  final Iterable<Host> liveHosts) throws DotDataException, DotSecurityException {

		final Set<String> alreadyCached = new HashSet<>();
		for(final Host host : hosts){
           add(host);
		   alreadyCached.add(host.getInode());
        }
		for (final Host host : liveHosts) {
			if (!alreadyCached.contains(host.getInode())) {
				add(host);
			}
		}

		cache.put(SITES, ImmutableSet.copyOf(hosts), PRIMARY_GROUP);
		cache.put(SITES, ImmutableSet.copyOf(liveHosts), HOST_LIVE_GROUP);

    }


	/**
	 * Get all the sites
	 * @param retrieveLiveVersion if true, get the live version of the sites (if available)
	 * @return all the sites
	 */
	protected Set<Host> getAllSites(boolean retrieveLiveVersion){
		return (Set<Host>) cache.getNoThrow(SITES,
				retrieveLiveVersion ? HOST_LIVE_GROUP : PRIMARY_GROUP);
	}

	private void clearSitesList(){
		cache.remove(SITES, PRIMARY_GROUP);
		cache.remove(SITES, HOST_LIVE_GROUP);
	}

	protected Host getHostByAlias(final String key, final boolean retrieveLiveVersion) {
		Host host = null;
    	try{
    		String hostId = (String) cache.get(key,ALIAS_GROUP);
			if (CACHE_404_HOST.equals(hostId)) {
				return cache404Contentlet;
			}
            if (null != hostId) { // The NullCacheAdministrator can return null here
                host = get(hostId, NOT_FOUND_BY_ID_GROUP, retrieveLiveVersion);
                if (host == null) {
                    cache.remove(key, ALIAS_GROUP);
                }
            }
    	}catch (DotCacheException e) {
			Logger.debug(this, "Cache Entry not found", e);
		}

        return host;
	}
	
	private Host get(final String key, final String notFoundCacheGroup, final boolean retrieveLiveVersion) {
    	Host host = null;
    	try {
			if (UtilMethods.isSet(notFoundCacheGroup)) {
				host = (Host) cache.get(key, notFoundCacheGroup);
			}
			if (host != null && CACHE_404_HOST.equals(host.getIdentifier())) {
				return cache404Contentlet;
			}
			host = (Host) cache.get(key, PRIMARY_GROUP);
			if (host == null || retrieveLiveVersion) {
				final Host liveHost = (Host) cache.get(key, HOST_LIVE_GROUP);
				if (liveHost != null) {
					return liveHost;
				}
			}
		} catch (DotCacheException e) {
			Logger.debug(this, "Cache Entry not found", e);
		}

        return host;	
	}

	/**
	 * Get a host by id
	 *
	 * @param id                  the id of the host
	 * @param retrieveLiveVersion
	 * @return the host or 404 if the host is in the not found cache,
	 * null if the host is not found in the cache
	 */
	@Override
	protected Host getById(final String id, final boolean retrieveLiveVersion) {
		return get(id, NOT_FOUND_BY_ID_GROUP, retrieveLiveVersion);
	}

	/**
	 * Get a host by name
	 *
	 * @param name                the name of the host
	 * @param retrieveLiveVersion
	 * @return the host or 404 if the host is in the not found cache,
	 * null if the host is not found in the cache
	 */
	@Override
	protected Host getByName(final String name, boolean retrieveLiveVersion) {
		return get(name, NOT_FOUND_BY_NAME_GROUP, retrieveLiveVersion);
	}

	/* (non-Javadoc)
	 * @see com.dotmarketing.business.PermissionCache#clearCache()
	 */
	public void clearCache() {
        // clear the cache
        cache.flushGroup(PRIMARY_GROUP);
        cache.flushGroup(ALIAS_GROUP);
		cache.flushGroup(HOST_LIVE_GROUP);
		cache.flushGroup(NOT_FOUND_BY_ID_GROUP);
		cache.flushGroup(NOT_FOUND_BY_NAME_GROUP);
    }

    /* (non-Javadoc)
	 * @see com.dotmarketing.business.PermissionCache#remove(java.lang.String)
	 */
    protected void remove(final Host host){
		remove(host, true);
    }

	/**
	 * Remove a host from the cache
	 * @param host the host to remove
	 * @param removeLiveVersion if true, remove the live version of the host
	 */
	@Override
	protected void remove(final Host host, final boolean removeLiveVersion) {

		if(host == null || StringUtils.isBlank(host.getIdentifier())){
			return;
		}
		// always remove default host
		String _defaultHost =PRIMARY_GROUP +DEFAULT_HOST;
		cache.remove(_defaultHost,PRIMARY_GROUP);

		String key = host.getIdentifier();
		String key2 = host.getHostname();

		try{
			cache.remove(key,PRIMARY_GROUP);
			if (removeLiveVersion) {
				cache.remove(key,HOST_LIVE_GROUP);
			}
			cache.remove(key,NOT_FOUND_BY_ID_GROUP);
		}catch (Exception e) {
			Logger.debug(this, "Cache not able to be removed", e);
		}

		try{
			cache.remove(key2,PRIMARY_GROUP);
			if (removeLiveVersion) {
				cache.remove(key2,HOST_LIVE_GROUP);
			}
			cache.remove(key2,NOT_FOUND_BY_NAME_GROUP);
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
    
    
    protected Host getDefaultHost(final boolean retrieveLiveVersion){
    	return get(DEFAULT_HOST, null, retrieveLiveVersion);
    }

    protected void addHostAlias(String alias, Host host){
    	if(alias != null && host != null && UtilMethods.isSet(host.getIdentifier())){
    		cache.put(alias, host.getIdentifier(),ALIAS_GROUP);
    	}
    }

	/**
	 * Add the host id to the 404 (not found) cache
	 * @param id the id of the host
	 */
	@Override
	protected void add404HostById(String id) {
		if (id != null) {
			cache.put(id, cache404Contentlet, NOT_FOUND_BY_ID_GROUP);
		}
	}

	/**
	 * Add the host name to the 404 (not found) cache
	 * @param name the name of the host
	 */
	@Override
	protected void add404HostByName(String name) {
		if (name != null) {
			cache.put(name, cache404Contentlet, NOT_FOUND_BY_NAME_GROUP);
		}
	}

	protected void clearAliasCache() {
        // clear the alias cache
        cache.flushGroup(ALIAS_GROUP);
    }
}
