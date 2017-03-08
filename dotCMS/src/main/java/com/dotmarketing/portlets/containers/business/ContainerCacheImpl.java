package com.dotmarketing.portlets.containers.business;

import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotCacheAdministrator;
import com.dotmarketing.business.DotCacheException;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;

public class ContainerCacheImpl extends ContainerCache {

  private DotCacheAdministrator cache;

  private static String primaryGroup = "ContainerCache";


  // region's name for the cache
  private static String[] groupNames = {primaryGroup};

  public ContainerCacheImpl() {
    cache = CacheLocator.getCacheAdministrator();
  }

  @Override
  public Container add(Container container) {

    try {
      if (UtilMethods.isSet(container)) {
        String key = primaryGroup + container.getInode();
        cache.put(key, container, primaryGroup);


      }
    } catch (Exception e) {
      Logger.debug(this, "Could not add entre to cache", e);
    }

    return container;
  }



  public void clearCache() {
    // Clear the cache for all group.
    cache.flushGroup(primaryGroup);
  }

  public void remove(String inode) {
    String key = primaryGroup + inode;


    try {
      cache.remove(key, primaryGroup);


    } catch (Exception e) {
      Logger.debug(this, "Cache not able to be removed", e);
    }
  }

  public String[] getGroups() {
    return groupNames;
  }

  public String getPrimaryGroup() {
    return primaryGroup;
  }

  @Override
  public Container get(String inode) {
    String key = primaryGroup + inode;
    try {
      return (Container) cache.get(key, primaryGroup);
    } catch (DotCacheException e) {
      Logger.debug(this, "Cache not able to be gotten", e);

    }
    return null;
  }
}
