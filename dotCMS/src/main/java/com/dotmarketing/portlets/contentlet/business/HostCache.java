package com.dotmarketing.portlets.contentlet.business;

import com.dotmarketing.beans.Host;
import com.dotmarketing.business.Cachable;
import java.util.List;
import java.util.Set;

//This interface should have default package access
public abstract class HostCache implements Cachable{
	protected static String PRIMARY_GROUP = "HostCache";
	
	abstract protected Host add(Host host);

	abstract protected void addHostAlias(final String alias, final Host host);

	abstract protected void addAll(final List<Host> hosts);

	abstract protected Host get(String key);

	abstract protected Host getHostByAlias(final String alias);

	abstract protected Host getDefaultHost();

	abstract protected Set<Host> getAllSites();

	abstract public void clearCache();
}