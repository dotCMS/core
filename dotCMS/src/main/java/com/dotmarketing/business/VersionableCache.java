package com.dotmarketing.business;

import com.dotmarketing.beans.Identifier;

public abstract class VersionableCache implements Cachable {

  protected abstract void addVersionableToCache(Versionable id);

  protected abstract Versionable getVersionable(String identId);

  protected abstract Versionable getVersionable(Versionable versionable);

  protected abstract Versionable getWorkingVersionable(Versionable versionable);

  protected abstract Versionable getLiveVersionable(Versionable versionable);

  protected abstract void removFromCache(Versionable versionable);

  protected abstract void removeFromCache(Identifier inode);

  protected abstract void removeFromCache(String inode);

  public abstract void clearCache();

  public String[] getGroups() {
    String[] groups = {getPrimaryGroup()};
    return groups;
  }

  public String getPrimaryGroup() {
    return "VersionableCache";
  }
}
