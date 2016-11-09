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

		if(f ==null || id ==null || ! UtilMethods.isSet(id.getId()) || ! UtilMethods.isSet(id.getPath())){
			return;
		}
		DotCacheAdministrator cache = CacheLocator.getCacheAdministrator();
		
		// Folder by Inode
		String inode = f.getInode();
		cache.put(getPrimaryGroup() + inode, f, getPrimaryGroup());

		// Folder by Path
		String folderPath = f.getHostId() + ":" + cleanPath(id.getPath()) ;
		cache.put(getPrimaryGroup() + folderPath, f, getPrimaryGroup());

	}
	// Folder by Inode
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

	// Folder by Path
	public Folder getFolderByPathAndHost(String path, Host host) {

		if(host==null) return null;

		DotCacheAdministrator cache = CacheLocator.getCacheAdministrator();
		String folderPath = host.getIdentifier() + ":" + cleanPath(path) ;
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
		
		// Folder by Inode
		cache.remove(getPrimaryGroup() + inode, getPrimaryGroup());
		
		try{
			// Folder by Path
			String folderPath = f.getHostId() + ":" + cleanPath(id.getPath()) ;
			cache.remove(getPrimaryGroup() + folderPath, getPrimaryGroup());
		}
		catch(NullPointerException npe){
			Logger.debug(FolderCache.class, "Cache Entry not found", npe);
		}
	}
	
	
	private String cleanPath(String path){
		return (path != null && path.length() >1) ? 
				(path.endsWith("/"))
					? path.substring(0,path.length()-1)
							: path
								: path;
								
		
	}
	
	
	
	

}
