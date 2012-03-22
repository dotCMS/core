package com.dotmarketing.portlets.virtuallinks.business;

import com.dotmarketing.business.Cachable;
import com.dotmarketing.portlets.virtuallinks.model.VirtualLink;

//This interface should have default package access
public abstract class VirtualLinkCache implements Cachable {

	abstract protected VirtualLink add(String key, VirtualLink virtualLink);

	abstract protected VirtualLink get(String key);

	abstract public void clearCache();

	abstract protected void remove(String key);

}