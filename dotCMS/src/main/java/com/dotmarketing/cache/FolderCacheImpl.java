package com.dotmarketing.cache;

import com.dotcms.exception.ExceptionUtil;
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

    @Override
	public void addFolder(final Folder folder, final Identifier id) {
		if (folder == null || id == null || !UtilMethods.isSet(id.getId()) || !UtilMethods.isSet(id.getPath())) {
			return;
		}
		final DotCacheAdministrator cache = CacheLocator.getCacheAdministrator();
		
		// Folder by Inode
		final String inode = folder.getInode();
		if (Folder.SYSTEM_FOLDER.equals(inode) && !Host.SYSTEM_HOST.equals(folder.getHostId())) {
		    // For SYSTEM_FOLDER, always make sure that it points to SYSTEM_HOST
            Logger.error(this, String.format("Host ID for SYSTEM_FOLDER must always be SYSTEM_HOST. Value '%s' was " +
                    "set.", folder.getHostId()));
            Logger.error(this, ExceptionUtil.getCurrentStackTraceAsString());
            folder.setHostId(Host.SYSTEM_HOST);
        }
		cache.put(getPrimaryGroup() + inode, folder, getPrimaryGroup());

		// Folder by Path
		final String folderPath = folder.getHostId() + ":" + cleanPath(id.getPath()) ;
		cache.put(getPrimaryGroup() + folderPath, folder, getPrimaryGroup());
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
