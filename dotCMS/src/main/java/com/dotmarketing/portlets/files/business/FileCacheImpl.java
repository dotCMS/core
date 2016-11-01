package com.dotmarketing.portlets.files.business;

import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotCacheAdministrator;
import com.dotmarketing.business.DotCacheException;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.cache.LiveCache;
import com.dotmarketing.cache.WorkingCache;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.files.model.File;
import com.dotmarketing.util.Logger;

public class FileCacheImpl extends FileCache {
	
	private DotCacheAdministrator cache;
	
	private static String primaryGroup = "FileCache";
    // region's name for the cache
    private static String[] groupNames = {primaryGroup};

	public FileCacheImpl() {
        cache = CacheLocator.getCacheAdministrator();
	}

	@Override
	protected File add(File file) {
		String key = file.getInode();
		key = primaryGroup + key;

        // Add the key to the cache
        cache.put(key, file, primaryGroup);


		return file;
		
	}
	
	@Override
	protected File get(String inode) {
		inode = primaryGroup + inode;
    	File file = null;
    	try{
    		file = (File)cache.get(inode,primaryGroup);
    	}catch (DotCacheException e) {
			Logger.debug(this, "Cache Entry not found", e);
		}
        return file;	
	}

    /* (non-Javadoc)
	 * @see com.dotmarketing.business.PermissionCache#clearCache()
	 */
    public void clearCache() {
        // clear the cache
        cache.flushGroup(primaryGroup);
    }

    /* (non-Javadoc)
	 * @see com.dotmarketing.business.PermissionCache#remove(java.lang.String)
	 */
    protected void remove(File file){
    	String key = file.getInode();
    	key = primaryGroup + key;
    	
        LiveCache.removeAssetFromCache(file);

        try {
			WorkingCache.removeAssetFromCache(file);
		} catch (DotStateException e1) {
			Logger.warn(this.getClass(), e1.getMessage(), e1);
		} catch (DotDataException e1) {
			Logger.warn(this.getClass(), e1.getMessage(), e1);
		}

    	try{
    		cache.remove(key,primaryGroup);
    	}catch (Exception e) {
			Logger.debug(this, "Cache not able to be removed", e);
		} 
    }
    public String[] getGroups() {
    	return groupNames;
    }
    public String getPrimaryGroup() {
    	return primaryGroup;
    }
}
