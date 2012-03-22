package com.dotmarketing.cache;

import java.util.List;

import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotCacheAdministrator;
import com.dotmarketing.business.DotCacheException;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.MenuItem;

/**
 * 
 * This class is used to cache menu items for nav.jsp menu 
 * @author carlos
 * @author Jason Tesser
 *
 */
public class NavMenuCache {
    
    /**
     * To add a new DOM tree related to the portletId to the cache
     * @param portletId The value is used as a key to identify the DOM tree 
     * @param doc The DOM tree to be stored into the cache.
     */
    public static void add(String porletId, List<MenuItem> menus) {
    	DotCacheAdministrator cache = CacheLocator.getCacheAdministrator();
    	cache.put(getPrimaryGroup() + porletId, menus, getPrimaryGroup());
    }
    
    /**
     * To retrieve a DOM tree from the cache
     * @param portletId
     * @return
     */
    public static List<MenuItem> get(String porletId) {
    	DotCacheAdministrator cache = CacheLocator.getCacheAdministrator();
    	List<MenuItem> menus = null;
    	try{
    		menus = ( List<MenuItem> ) cache.get(getPrimaryGroup() + porletId,getPrimaryGroup());
    	}catch (DotCacheException e) {
			Logger.debug(NavMenuCache.class, "Cache Entry not found", e);
    	}
		return menus;
    }
    
    /**
     * To invalidate (remove) the DOM tree entry related to this portletId from the cache
     * @param portletId
     */
    public static void invalidate(String porletId) {
    	DotCacheAdministrator cache = CacheLocator.getCacheAdministrator();
    	cache.remove(getPrimaryGroup() + porletId,getPrimaryGroup());
    }
    
    public static void clearCache(){
		DotCacheAdministrator cache = CacheLocator.getCacheAdministrator();
	    //clear the cache
	    cache.flushGroup(getPrimaryGroup());
	}
	public static String[] getGroups() {
    	String[] groups = {getPrimaryGroup()};
    	return groups;
    }
    
    public static String getPrimaryGroup() {
    	return "NavMenuCache";
    }
}