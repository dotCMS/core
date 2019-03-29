/**
 * Copyright (c) 2000-2005 Liferay, LLC. All rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.liferay.portal.ejb;

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
