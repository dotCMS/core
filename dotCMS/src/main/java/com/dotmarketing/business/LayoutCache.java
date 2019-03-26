package com.dotmarketing.business;

import java.util.List;

/** @author Jason Tesser */
public abstract class LayoutCache implements Cachable {

  protected abstract Layout add(String key, Layout layout);

  protected abstract Layout get(String key);

  public abstract void clearCache();

  protected abstract void remove(Layout layout);

  protected abstract List<String> getPortlets(Layout layout);

  protected abstract List<String> addPortlets(Layout layout, List<String> portletIds);
}
