package com.dotmarketing.portlets.contentlet.business;

import com.dotmarketing.beans.Host;
import com.dotmarketing.business.Cachable;

// This interface should have default package access
public abstract class HostCache implements Cachable {
  protected static String PRIMARY_GROUP = "HostCache";
  protected static String ALIAS_GROUP = "HostAliasCache";

  protected abstract Host add(Host host);

  protected abstract Host get(String key);

  public abstract void clearCache();

  protected abstract void remove(Host host);

  protected abstract Host getDefaultHost();

  protected abstract Host getHostByAlias(String alias);

  protected abstract void addHostAlias(String alias, Host host);

  protected abstract void clearAliasCache();
}
