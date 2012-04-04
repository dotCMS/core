package com.dotmarketing.business;

import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.VersionInfo;
import com.dotmarketing.portlets.contentlet.model.ContentletLangVersionInfo;
import com.dotmarketing.portlets.contentlet.model.ContentletVersionInfo;

public abstract class IdentifierCache implements Cachable {

	abstract protected void addIdentifierToCache(Identifier id);

	abstract protected Identifier getIdentifier(String identId);

	abstract protected Identifier getIdentifier(Host host, String URI);

	abstract protected Identifier getIdentifier(String hostId, String URI);

	abstract protected Identifier getIdentifier(Versionable versionable);

	abstract protected void removeFromCacheByURI(String URI, String hostId);

	abstract public void removeFromCacheByVersionable(Versionable versionable);

	abstract protected void removeFromCacheByIdentifier(String inode);
	
    abstract protected VersionInfo getVersionInfo(String identifier);    
    
    abstract protected void addVersionInfoToCache(VersionInfo versionInfo);
    
    abstract protected void removeVersionInfoFromCache(String identifier);
    
    abstract protected ContentletVersionInfo getContentVersionInfo(String identifier);
    
    abstract protected ContentletLangVersionInfo getContentLangVersionInfo(String identifier, long lang);
    
    abstract protected void addContentletVersionInfoToCache(ContentletVersionInfo contV);
    
    abstract protected void addContentletLangVersionInfoToCache(ContentletLangVersionInfo contL);
    
    abstract protected void removeContentletVersionInfoToCache(String identifier);
    
    abstract protected void removeContentletLangVersionInfoToCache(String identifier, long lang);

	abstract public void clearCache();

	public String[] getGroups() {
		String[] groups = { getPrimaryGroup(), getVersionInfoGroup() };
		return groups;
	}

	public String getPrimaryGroup() {
		return "IdentifierCache";
	}
	
	public String getVersionInfoGroup() {
	    return "VersionInfoCache";
	}

}
