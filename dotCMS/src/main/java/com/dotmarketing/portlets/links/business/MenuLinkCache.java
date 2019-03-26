package com.dotmarketing.portlets.links.business;

import com.dotmarketing.business.Cachable;
import com.dotmarketing.portlets.links.model.Link;

// This interface should have default package access
public abstract class MenuLinkCache implements Cachable {

  protected abstract Link add(String key, Link menuLink);

  protected abstract Link get(String key);

  public abstract void clearCache();

  protected abstract void remove(String key);
}
