package com.dotmarketing.business;

import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.VersionInfo;
import com.dotmarketing.portlets.contentlet.model.ContentletVersionInfo;

public abstract class IdentifierCache implements Cachable {

  protected abstract void addIdentifierToCache(Identifier id);

  protected abstract void addIdentifierToCache(Identifier id, Versionable v);

  protected abstract void addIdentifierToCache(String identifier, String inode);

  protected abstract Identifier getIdentifier(String identId);

  protected abstract Identifier getIdentifier(Host host, String URI);

  protected abstract Identifier getIdentifier(String hostId, String URI);

  protected abstract String getIdentifierFromInode(Versionable versionable);

  protected abstract String getIdentifierFromInode(String inode);

  protected abstract void removeFromCacheByURI(String hostId, String URI);

  public abstract void removeFromCacheByVersionable(Versionable versionable);

  public abstract void removeFromCacheByIdentifier(String inode);

  public abstract void removeFromCacheByInode(String inode);

  public abstract VersionInfo getVersionInfo(String identifier);

  protected abstract void addVersionInfoToCache(VersionInfo versionInfo);

  protected abstract void removeVersionInfoFromCache(String identifier);

  protected abstract ContentletVersionInfo getContentVersionInfo(String identifier, long lang);

  protected abstract void addContentletVersionInfoToCache(ContentletVersionInfo contV);

  public abstract void removeContentletVersionInfoToCache(String identifier, long lang);

  public abstract void clearCache();

  public String[] getGroups() {
    String[] groups = {getPrimaryGroup(), getVersionInfoGroup(), getVersionGroup(), get404Group()};
    return groups;
  }

  public String getPrimaryGroup() {
    return "IdentifierCache";
  }

  public String getVersionInfoGroup() {
    return "VersionInfoCache";
  }

  public String getVersionGroup() {
    return "VersionCache";
  }

  public String get404Group() {
    return "Identifier404Cache";
  }
}
