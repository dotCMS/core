/*
 * Created on May 30, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package com.dotmarketing.cache;

import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.Inode;
import com.dotmarketing.beans.WebAsset;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotCacheAdministrator;
import com.dotmarketing.business.DotCacheException;
import com.dotmarketing.business.DotIdentifierStateException;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.Versionable;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.filters.CMSFilter;
import com.dotmarketing.filters.CmsUrlUtil;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.links.model.Link;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;

/**
 * @author David
 * @author Jason Tesser
 *
 */


@Deprecated
public class WorkingCache {
    
    public static String addToWorkingAssetToCache(Versionable asset) throws DotIdentifierStateException, DotDataException{
    	DotCacheAdministrator cache = CacheLocator.getCacheAdministrator();
		// we use the identifier uri for our mappings.
		Identifier id = APILocator.getIdentifierAPI().find(asset);
		//Velocity Page Extension

		//Obtain the URI
		String uri = id.getURI(); 		
		//Obtain the INODE
		String hostId = id.getHostId();
		String ret = null;
		if (UtilMethods.isSet(uri)) 
		{		    
			Logger.debug(WorkingCache.class, "Mapping Working: " + uri + " to " + uri);
			if(CmsUrlUtil.getInstance().isPageAsset(asset))
			{
			    //add it to the cache
				//for now we are adding the page URI
				cache.put(getPrimaryGroup() + hostId + "-" + uri,uri, getPrimaryGroup() + "_" + hostId);

				//if this is an index page, map its directories to it
				if(id.getURI().endsWith("/" + CMSFilter.CMS_INDEX_PAGE))
				{
					Logger.debug(WorkingCache.class, "Mapping Working: " + uri.substring(0,uri.lastIndexOf("/" + CMSFilter.CMS_INDEX_PAGE)) + " to " + uri);
					cache.put(getPrimaryGroup() + hostId + "-" + uri.substring(0,uri.lastIndexOf("/" +CMSFilter.CMS_INDEX_PAGE)),uri, getPrimaryGroup() + "_" + hostId);
					Logger.debug(WorkingCache.class, "Mapping Working: " + id.getURI().substring(0,id.getURI().lastIndexOf(CMSFilter.CMS_INDEX_PAGE)) + " to " + uri);
					cache.put(getPrimaryGroup() + hostId + "-" + uri.substring(0,uri.lastIndexOf(CMSFilter.CMS_INDEX_PAGE)), uri, getPrimaryGroup() + "_" + hostId);
				}
				ret = uri;
			}
			else if (asset instanceof Link) {
				Folder parent;
				try {
					parent = (Folder) APILocator.getFolderAPI().findParentFolder((Link)asset, APILocator.getUserAPI().getSystemUser(), false);
					String path = ((Link)asset).getURI(parent);
					//add the entry to the cache
					Logger.debug(WorkingCache.class, "Mapping: " + uri + " to " + path);
					cache.put(getPrimaryGroup() + hostId + "-" + uri,path, getPrimaryGroup() + "_" + hostId);
					ret = path;
				} catch (DotSecurityException e) {
					Logger.error(WorkingCache.class, "Unable to get Folder for Link", e);
				}
			}else if(asset instanceof com.dotmarketing.portlets.contentlet.model.Contentlet){
				String path = APILocator.getFileAssetAPI().getRelativeAssetPath(APILocator.getFileAssetAPI().fromContentlet((com.dotmarketing.portlets.contentlet.model.Contentlet)asset));
				//add the entry to the cache
				Logger.debug(WorkingCache.class, "Mapping: " + uri + " to " + path);
				cache.put(getPrimaryGroup() + hostId + "-" + uri,path, getPrimaryGroup() + "_" + hostId);
				ret = path;
			}else {
				String path = APILocator.getFileAPI().getRelativeAssetPath((Inode)asset);
				//add the entry to the cache
				Logger.debug(WorkingCache.class, "Mapping: " + uri + " to " + path);
				cache.put(getPrimaryGroup() + hostId + "-" + uri,path, getPrimaryGroup() + "_" + hostId);
				ret = path;
			}	


		}
		return ret;
		
	}
    
    public static String getPathFromCache(String URI, Host host) throws DotStateException, DotDataException, DotSecurityException{
	    return getPathFromCache (URI, host.getIdentifier());
	}

    public static String getPathFromCache(String URI, Host host, Long langId) throws DotStateException, DotDataException, DotSecurityException{
	    return getPathFromCache (URI, host.getIdentifier(), langId);
	}
    
    public static String getPathFromCache(String URI, String hostId) throws DotStateException, DotDataException, DotSecurityException{
	    return getPathFromCache (URI, hostId, null);
	}

	    //Working cache methods
	public static String getPathFromCache(String URI, String hostId, Long langId) throws DotStateException, DotDataException, DotSecurityException{
		DotCacheAdministrator cache = CacheLocator.getCacheAdministrator();
		String _uri = null;
		try{
			_uri = (String) cache.get(getPrimaryGroup() + hostId + "-" + URI,getPrimaryGroup() + "_" + hostId);
		}catch (DotCacheException e) {
			Logger.debug(WorkingCache.class, "Cache Entry not found", e);
    	}

		if(_uri != null) return _uri;
		


		if (URI.endsWith("/")) {
			//it's a folder path, so I add index.html at the end
			URI += CMSFilter.CMS_INDEX_PAGE;
		}

		// lets try to lazy get it.
		Host fake = new Host();
		fake.setIdentifier(hostId);
		Identifier id = APILocator.getIdentifierAPI().find( fake,URI);

		if(!InodeUtils.isSet(id.getInode())) 
		{
			//it's a folder path, so I add index.html at the end
			URI += "/" + CMSFilter.CMS_INDEX_PAGE;
			id = APILocator.getIdentifierAPI().find( fake,URI);
			if(!InodeUtils.isSet(id.getInode()))
			{
			    return null;
			}
		}

		if(id.getAssetType().equals("contentlet")){
			com.dotmarketing.portlets.contentlet.model.Contentlet cont;
			if(!UtilMethods.isSet(langId))
				cont =  APILocator.getContentletAPI().findContentletByIdentifier(id.getId(), false, APILocator.getLanguageAPI().getDefaultLanguage().getId(), APILocator.getUserAPI().getSystemUser(), false);
			else
				cont =  APILocator.getContentletAPI().findContentletByIdentifier(id.getId(), false, langId, APILocator.getUserAPI().getSystemUser(), false);
		   if(cont!=null && InodeUtils.isSet(cont.getInode()))
			{
				Logger.debug(WorkingCache.class, "Lazy Preview Mapping: " + id.getURI() + " to " + URI);
			   return addToWorkingAssetToCache((Versionable)cont);
			}
		
		}else{
			WebAsset asset = null;
			asset = (WebAsset) APILocator.getVersionableAPI().findWorkingVersion(id, APILocator.getUserAPI().getSystemUser(), false);
			// add to cache now
			if(asset!=null && InodeUtils.isSet(asset.getInode()))
			{
				Logger.debug(WorkingCache.class, "Lazy Preview Mapping: " + id.getURI() + " to " + URI);
			   return addToWorkingAssetToCache(asset);
			}
		}

	

		return null; 
    	
	}

	public static void removeURIFromCache(String URI, Host host){
	    removeURIFromCache (URI, host.getIdentifier());
	}
	
	public static void removeURIFromCache(String URI, String hostId){
		DotCacheAdministrator cache = CacheLocator.getCacheAdministrator();
			cache.remove(getPrimaryGroup() + hostId + "-" + URI,getPrimaryGroup() + "_" + hostId);	
	}

	public static void removeAssetFromCache(Versionable asset) throws DotStateException, DotDataException{
		DotCacheAdministrator cache = CacheLocator.getCacheAdministrator();
		Identifier identifier = APILocator.getIdentifierAPI().find(asset);
		cache.remove(getPrimaryGroup() + identifier.getHostId() + "-" + identifier.getURI(),getPrimaryGroup() + "_" + identifier.getHostId());
	}
	
	public static void clearCache(String hostId){
		DotCacheAdministrator cache = CacheLocator.getCacheAdministrator();
	    //clear the cache
	    cache.flushGroup(getPrimaryGroup() + "_" + hostId);
	}
	public static String[] getGroups() {
    	String[] groups = {getPrimaryGroup()};
    	return groups;
    }
    
    public static String getPrimaryGroup() {
    	return "WorkingCache";
    }    
}
