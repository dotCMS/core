
package com.dotmarketing.business.portal;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.dotmarketing.business.Cachable;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotCacheAdministrator;
import com.liferay.portal.model.Portlet;

public class PortletCache implements Cachable {
  final static String ALL_PORTLETS_KEY = "ALL_PORTLETS_KEY";

  public PortletCache() {
    cache = CacheLocator.getCacheAdministrator();
  }

  public void clear() {
    cache.flushGroup(primaryGroup);
  }

  public Portlet get(String portletId) {
    return getAllPortlets().get(portletId);
  }

  public void put(Portlet portlet) {
    getAllPortlets().put(portlet.getPortletId(), portlet);
  }

  public void remove(String portletId) {
    getAllPortlets().remove(portletId);
  }

  public void remove(Portlet portlet) {

    getAllPortlets().remove(portlet.getPortletId());
  }

  public String primaryGroup = "portletcache";

  // region's name for the cache
  public String[] groupNames = {primaryGroup};

  @Override
  public String getPrimaryGroup() {
    return primaryGroup;
  }

  public String[] getGroups() {
    return groupNames;
  }

  private DotCacheAdministrator cache = CacheLocator.getCacheAdministrator();

  public void clearCache() {
    clear();
  }

  public Map<String, Portlet> getAllPortlets() {
    ConcurrentHashMap<String, Portlet> map = (ConcurrentHashMap<String, Portlet>) cache.getNoThrow(ALL_PORTLETS_KEY, primaryGroup);
    if (map == null) {
      map = new ConcurrentHashMap<>();
      putAllPortlets(map);
    }
    return map;
  }

  public void putAllPortlets(Map<String, Portlet> allPortlets) {
    allPortlets = (allPortlets instanceof ConcurrentHashMap) ? allPortlets : new ConcurrentHashMap<>(allPortlets);

    cache.put(ALL_PORTLETS_KEY, allPortlets, primaryGroup);

  }

  public void removeAllPortlets() {
    cache.remove(ALL_PORTLETS_KEY, primaryGroup);

  }

}
