/*
 * Created on May 30, 2005
 *
 */
package com.dotmarketing.cache;

import java.util.Iterator;
import java.util.List;

import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotCacheAdministrator;
import com.dotmarketing.business.DotCacheException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.portlets.virtuallinks.factories.VirtualLinkFactory;
import com.dotmarketing.portlets.virtuallinks.model.VirtualLink;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.WebKeys;

/**
 * 
 * This cache is used to mapped the virtual link path to the redirection path
 * like /test path points to http://www.yahoo.com
 * @author David & Salvador
 * @author Jason Tesser
 *
 */
public class VirtualLinksCache {    

    /**
     * Find the given url in the cache
     * @param url
     * @return The redirection path used by the cms to properly redirect the virtual link
     */
    public static String getPathFromCache(String url) {
    	DotCacheAdministrator cache = CacheLocator.getCacheAdministrator();
    	String realLink = null;
    	try{
    		realLink = (String) cache.get(sanitizeKey(url),getPrimaryGroup());
    	}catch (DotCacheException e) {
			Logger.debug(VirtualLinksCache.class, "Cache Entry not found", e);
    	}
        
        if(realLink != null) {
        	if(realLink.equals(WebKeys.Cache.CACHE_NOT_FOUND)){
        		return null;
        	}
        	else{
        		return realLink;
        	}
        }
        
        VirtualLink vl = null;
        try{
        	vl = VirtualLinkFactory.getVirtualLinkByURL(url);
        }
        catch(DotHibernateException dhe){
        	Logger.debug(VirtualLinksCache.class, "failed to find: " + url);  
        }
        

		if(vl != null && InodeUtils.isSet(vl.getInode())) 
		{
			addPathToCache(vl);
		} else {

            cache.put(sanitizeKey(url), WebKeys.Cache.CACHE_NOT_FOUND, getPrimaryGroup());
            return null;
		}

        return vl.getUri();

    }
    
    /**
     * This maps the given virtual link in the cache also sends 
     * a cache invalidation message to force the other peers in the 
     * cluster to re-map the key as well
     * @param vl
     */
    public static void addPathToCache(VirtualLink vl){
    	DotCacheAdministrator cache = CacheLocator.getCacheAdministrator();
        if (vl != null && InodeUtils.isSet(vl.getInode())) 
        {
        	Logger.info(VirtualLinksCache.class, "mapping: " + vl.getUrl() + " -> " + vl.getUri());     	
            cache.put(sanitizeKey(vl.getUrl())  , vl.getUri(), getPrimaryGroup());
        }
    }

    /**
     * This method removes the given virtual link path from the cache
     * and also sends an invalidation 
     * @param url
     */
    public static void removePathFromCache(String url) {
    	DotCacheAdministrator cache = CacheLocator.getCacheAdministrator();
    	Logger.debug(VirtualLinksCache.class, "removePathFromCache: url = " + url);
    	cache.remove(sanitizeKey(url),getPrimaryGroup());
    } 
    
    public static void mapAllVirtualLinks() {
    	DotCacheAdministrator cache = CacheLocator.getCacheAdministrator();
    	List<VirtualLink> vls = VirtualLinkFactory.getVirtualLinks();
        Iterator<VirtualLink> iter = vls.iterator();
        Logger.debug(VirtualLinksCache.class, " mapping " + vls.size() + " virtual link(s) ");
        while (iter.hasNext()) {
            VirtualLink vl = (VirtualLink) iter.next();
            if (vl != null && InodeUtils.isSet(vl.getInode())) 
            {
            	Logger.debug(VirtualLinksCache.class, "mapping: " + vl.getUrl() + " -> " + vl.getUri());
                cache.put(sanitizeKey(vl.getUrl()), vl.getUri(), getPrimaryGroup());
            }
        }
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
    	return "VirtualLinksCache";
    } 
    
    private static String sanitizeKey(String key){
    	return key.replace('/', '|');
    	
    }
}
