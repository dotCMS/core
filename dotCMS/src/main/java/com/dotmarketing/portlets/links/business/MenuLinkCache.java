package com.dotmarketing.portlets.links.business;

import com.dotmarketing.business.Cachable;
import com.dotmarketing.portlets.links.model.Link;

//This interface should have default package access
public abstract class MenuLinkCache implements Cachable {

	abstract protected Link add(String key, Link menuLink);

	abstract protected Link get(String key);

	abstract public void clearCache();

	abstract protected void remove(String key);
}