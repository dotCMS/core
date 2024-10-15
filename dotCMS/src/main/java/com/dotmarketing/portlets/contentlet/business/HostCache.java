package com.dotmarketing.portlets.contentlet.business;

import com.dotmarketing.beans.Host;
import com.dotmarketing.business.Cachable;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;

import java.util.Set;

//This interface should have default package access
public abstract class HostCache implements Cachable{
	protected static String PRIMARY_GROUP = "HostCache";
	protected static String ALIAS_GROUP = "HostAliasCache";
	protected static String HOST_LIVE_GROUP = "HostLiveCache";
	protected static String NOT_FOUND_BY_ID_GROUP = "Host404ByIdCache";
	protected static String NOT_FOUND_BY_NAME_GROUP = "Host404ByNameCache";

	public static final String CACHE_404_HOST = "CACHE_404_HOST";

	protected static final Host cache404Contentlet = new Host() {
		@Override
		public String getIdentifier() {
			return CACHE_404_HOST;
		}
	};

	abstract protected Host add(Host host) throws DotDataException, DotSecurityException;

	abstract protected Host getById(String id, boolean retrieveLiveVersion);

	abstract protected Host getByName(String name, boolean retrieveLiveVersion);

	abstract public void clearCache();

	abstract protected void remove(Host host);

	abstract protected void remove(Host host, boolean removeLiveVersion);

	abstract protected void addAll(Iterable<Host> hosts, Iterable<Host> liveHosts) throws DotDataException, DotSecurityException;

	abstract protected Set<Host> getAllSites(boolean retrieveLiveVersion);

	abstract protected Host getDefaultHost(boolean retrieveLiveVersion);
	
	abstract protected Host getHostByAlias(String alias, boolean retrieveLiveVersion);
	
	abstract protected void addHostAlias(String alias, Host host);

	abstract protected void add404HostById(String id);

	abstract protected void add404HostByName(String name);

	abstract protected  void clearAliasCache() ;
}