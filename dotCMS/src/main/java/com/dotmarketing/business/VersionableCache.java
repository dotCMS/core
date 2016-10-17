package com.dotmarketing.business;

import com.dotmarketing.beans.Identifier;

public abstract class VersionableCache implements Cachable {

	abstract protected void addVersionableToCache(Versionable id);

	abstract protected Versionable getVersionable(String identId);


	abstract protected Versionable getVersionable(Versionable versionable);

	abstract protected Versionable getWorkingVersionable(Versionable versionable);
	
	abstract protected Versionable getLiveVersionable(Versionable versionable);
	
	abstract protected void removFromCache(Versionable versionable);

	abstract protected void removeFromCache(Identifier inode);

	abstract protected void removeFromCache(String inode);

	abstract public void clearCache();

	public String[] getGroups() {
		String[] groups = { getPrimaryGroup() };
		return groups;
	}

	public String getPrimaryGroup() {
		return "VersionableCache";
	}

}
