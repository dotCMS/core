/*
*
* Copyright (c) 2025 dotCMS LLC
* Use of this software is governed by the Business Source License included
* in the LICENSE file found at in the root directory of software.
* SPDX-License-Identifier: BUSL-1.1
*
*/

package com.dotcms.enterprise.cluster.action.business;

import java.util.List;

import com.dotcms.enterprise.cluster.action.model.ServerActionBean;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotCacheAdministrator;

public class ServerActionBeanCacheImpl implements ServerActionBeanCache {

  protected final DotCacheAdministrator cache;

  protected final String primaryGroup = "ServerActionBeanCache";
  protected final String[] groupNames = {primaryGroup};

  public ServerActionBeanCacheImpl() {
    cache = CacheLocator.getCacheAdministrator();
  }

  public String getPrimaryGroup() {
    return primaryGroup;
  }

  public String[] getGroups() {
    return groupNames;
  }

  public void clearCache() {
    cache.flushGroup(primaryGroup);
  }

  @Override
  public void putServerActions(List<ServerActionBean> serverActionBeans) {
    cache.put("serverActionBeans", serverActionBeans, primaryGroup);
  }

  @Override
  public List<ServerActionBean> getServerActions() {
    return (List<ServerActionBean>) cache.getNoThrow("serverActionBeans", primaryGroup);
  }

  @Override
  public void clearServerActions() {
    cache.remove("serverActionBeans", primaryGroup);

  }

}
