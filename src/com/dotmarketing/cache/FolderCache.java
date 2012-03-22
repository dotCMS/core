package com.dotmarketing.cache;

import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.Cachable;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotCacheAdministrator;
import com.dotmarketing.portlets.folders.model.Folder;

/**
 * @author David
 */
public abstract class FolderCache implements Cachable {
    
    public abstract void addFolder(Folder f, Identifier id);
    

    public abstract Folder getFolder(String inode);
    

    public abstract Folder getFolderByPathAndHost(String path, Host host);

    public abstract void removeFolder(Folder f, Identifier id);

    public  void clearCache(){
		DotCacheAdministrator cache = CacheLocator.getCacheAdministrator();
	    //clear the cache
	    cache.flushGroup(getPrimaryGroup());
	}
	public  String[] getGroups() {
    	String[] groups = {getPrimaryGroup()};
    	return groups;
    }
    
    public  String getPrimaryGroup() {
    	return "FolderCache";
    }
}
