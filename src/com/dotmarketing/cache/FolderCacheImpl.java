package com.dotmarketing.cache;


import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotCacheAdministrator;
import com.dotmarketing.business.DotCacheException;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;

/**
 * @author David
 */
public class FolderCacheImpl extends FolderCache {

	public void addFolder(Folder f, Identifier id) {

		if(f ==null || id ==null || ! UtilMethods.isSet(id.getId())){
			return;
		}
		DotCacheAdministrator cache = CacheLocator.getCacheAdministrator();
		// we use the identifier uri for our mappings.
		String inode = f.getInode();

		String folderPath = f.getHostId() + ":" + id.getPath() ;
		cache.put(getPrimaryGroup() + inode, f, getPrimaryGroup());

		cache.put(getPrimaryGroup() + folderPath, f, getPrimaryGroup());

	}

	public Folder getFolder(String inode) {
		DotCacheAdministrator cache = CacheLocator.getCacheAdministrator();
		Folder f = null;
		try {
			f = (Folder) cache.get(getPrimaryGroup() + inode, getPrimaryGroup());
		} catch (DotCacheException e) {
			Logger.debug(FolderCache.class, "Cache Entry not found", e);
		}

		return f;
	}


	public Folder getFolderByPathAndHost(String path, Host host) {

		if(host==null) return null;

		DotCacheAdministrator cache = CacheLocator.getCacheAdministrator();
		String folderPath = host.getIdentifier() + ":" + path;
		Folder f = null;
		try {
			f = (Folder) cache.get(getPrimaryGroup() + folderPath, getPrimaryGroup());
		} catch (DotCacheException e) {
			Logger.debug(FolderCache.class, "Cache Entry not found", e);
		}


		return f;
	}

	public void removeFolder(Folder f, Identifier id) {

		DotCacheAdministrator cache = CacheLocator.getCacheAdministrator();
		String inode = f.getInode();

		cache.remove(getPrimaryGroup() + inode, getPrimaryGroup());
		try{
			String folderPath = f.getHostId() + ":" + id.getPath() ;
			cache.remove(getPrimaryGroup() + folderPath, getPrimaryGroup());
		}
		catch(NullPointerException npe){
			Logger.debug(FolderCache.class, "Cache Entry not found", npe);
		}
	}

}
