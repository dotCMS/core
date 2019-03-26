package com.dotmarketing.business;

import com.dotmarketing.beans.Permission;
import java.util.List;

// This interface should have default package access
public abstract class PermissionCache implements Cachable {

  protected abstract List<Permission> addToPermissionCache(
      String key, List<Permission> permissions);

  protected abstract List<Permission> getPermissionsFromCache(String key);

  public abstract void clearCache();

  protected abstract void remove(String key);
}
