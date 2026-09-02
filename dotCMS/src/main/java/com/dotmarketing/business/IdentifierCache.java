package com.dotmarketing.business;

import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.VersionInfo;
import com.dotmarketing.portlets.contentlet.model.ContentletVersionInfo;

import java.util.List;

public abstract class IdentifierCache implements Cachable {

	abstract protected void addIdentifierToCache(Identifier id);
	
    abstract protected void addIdentifierToCache(Identifier id, Versionable v) ;
    
    abstract protected void addIdentifierToCache(String identifier, String inode) ;

	abstract protected Identifier getIdentifier(String identId);

	abstract protected Identifier getIdentifier(Host host, String URI);

	abstract protected Identifier getIdentifier(String hostId, String URI);

	abstract protected String getIdentifierFromInode(Versionable versionable);
	
	abstract protected String getIdentifierFromInode(String inode);

	abstract public void removeFromCacheByURI(String hostId, String URI);

	/**
	 * Removes the UUID-keyed and URI-keyed cache entries for a single identifier
	 * without triggering the recursive child-folder eviction that
	 * {@link #removeFromCacheByIdentifier(String)} performs when the cached
	 * entry has {@code assetType = "folder"}.
	 * <p>
	 * Use this when the caller already iterates the full set of descendants
	 * (e.g. from a pre-loaded snapshot) and recursive re-discovery via
	 * {@code findByParentPath} would be redundant and cause O(F × depth)
	 * extra DB round-trips.
	 *
	 * @param id     UUID of the identifier to evict
	 * @param hostId site/host identifier (used to construct the URI cache key)
	 * @param uri    identifier URI ({@code parent_path + asset_name}) at the time of eviction
	 */
	abstract public void removeFromCacheDirect(String id, String hostId, String uri);

	abstract public void removeFromCacheByVersionable(Versionable versionable);

	abstract public void removeFromCacheByIdentifier(String inode);
	
	abstract public void removeFromCacheByInode(String inode);
	
    abstract public VersionInfo getVersionInfo(String identifier);
    
    abstract protected void addVersionInfoToCache(VersionInfo versionInfo);
    
    abstract protected void removeVersionInfoFromCache(String identifier);
    
    abstract protected ContentletVersionInfo getContentVersionInfo(String identifier, long lang);

	abstract protected ContentletVersionInfo getContentVersionInfo(String identifier, long lang, String variantId);

	abstract protected void addContentletVersionInfoToCache(ContentletVersionInfo contV);
    
    abstract public void removeContentletVersionInfoToCache(String identifier, long lang);

	abstract public void removeContentletVersionInfoToCache(String identifier, long lang, String variantId);

	abstract public void clearCache();

	public String[] getGroups() {
		String[] groups = { getPrimaryGroup(), getVersionInfoGroup(),getVersionGroup(),get404Group() };
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

	public abstract void putContentVersionInfos(String identifier,
												List<ContentletVersionInfo> cvis);

	public abstract List<ContentletVersionInfo> getContentVersionInfos(String identifier);

}
