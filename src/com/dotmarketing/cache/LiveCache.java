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
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotCacheAdministrator;
import com.dotmarketing.business.DotCacheException;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.Treeable;
import com.dotmarketing.business.Versionable;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.factories.PublishFactory;
import com.dotmarketing.portlets.contentlet.business.DotContentletStateException;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.links.model.Link;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.model.User;

/**
 *
 * This cache is used to know when an asset is live, it doesn't store real valuable info in the cache
 *
 * @author David
 * @author Jason Tesser
 *
 */
public class LiveCache {

    /**
     * This method adds the given asset uri to the cache using the
     * host id + identifier uri as the key
     * This method also send a signal to the cluster to invalidate key cluster wide
     *
     * @param asset
     * @return
     */
    public static String addToLiveAssetToCache ( Versionable asset ) {
        return addToLiveAssetToCache( asset, null );
    }

    /**
     * This method adds the given asset uri to the cache using the
     * host id + identifier uri as the key
     * This method also send a signal to the cluster to invalidate key cluster wide
     *
     * @param asset
     * @param languageId
     * @return
     */
    public static String addToLiveAssetToCache ( Versionable asset, Long languageId ) {

        HostAPI hostAPI = APILocator.getHostAPI();

    	DotCacheAdministrator cache = CacheLocator.getCacheAdministrator();
        //The default value for velocity page extension
        String ext = Config.getStringProperty("VELOCITY_PAGE_EXTENSION");
		// we use the identifier uri for our mappings.
        String ret = null;
        try{
        	Identifier id = APILocator.getIdentifierAPI().find(asset);
        	//Obtain the host of the webassets
        	User systemUser = APILocator.getUserAPI().getSystemUser();
    		Host host = hostAPI.findParentHost((Treeable)asset, systemUser, false);
    		if(host == null) ret = null;

    		//Obtain the URI for future uses
    		String uri = id.getURI();
    		//Obtain the inode value of the host;
    		String hostId = host.getIdentifier();

    		//if this is an index page, map its directories to it
    		if (UtilMethods.isSet(uri))
    		{
    		  if(uri.endsWith("." + ext))
    		  {
    		    Logger.debug(LiveCache.class, "Mapping: " + uri + " to " + uri);

    		    //Add the entry to the cache
    			cache.put(getPrimaryGroup() + hostId + ":" + uri,uri, getPrimaryGroup() + "_" + hostId);

    			if(uri.endsWith("/index." + ext))
    			{
    			    //Add the entry to the cache
    			    Logger.debug(LiveCache.class, "Mapping: " + uri.substring(0,uri.lastIndexOf("/index." + ext)) + " to " + uri);
    				cache.put(getPrimaryGroup() + hostId + ":" + uri.substring(0,uri.lastIndexOf("/index." + ext)),uri, getPrimaryGroup() + "_" + hostId);
    				//Add the entry to the cache
    			    Logger.debug(LiveCache.class, "Mapping: " + uri.substring(0,uri.lastIndexOf("/index." + ext)) + " to " + uri);
    				cache.put(getPrimaryGroup() + hostId + ":" + uri.substring(0,uri.lastIndexOf("index." + ext)),uri, getPrimaryGroup() + "_" + hostId);
    			}
				ret = uri;
    		}
    		else if (asset instanceof Link) {
    			Folder parent = (Folder) APILocator.getFolderAPI().findParentFolder((Link)asset, APILocator.getUserAPI().getSystemUser(), false);
    			String path = ((Link)asset).getURI(parent);
    			//add the entry to the cache
    		    Logger.debug(LiveCache.class, "Mapping: " + uri + " to " + path);
    			cache.put(getPrimaryGroup() + hostId + ":" + uri,path, getPrimaryGroup() + "_" + hostId);
    			ret = path;
            } else if ( asset instanceof Contentlet ) {
                String path = APILocator.getFileAssetAPI().getRelativeAssetPath( APILocator.getFileAssetAPI().fromContentlet( (Contentlet) asset ) );
                //add the entry to the cache
                Logger.debug( LiveCache.class, "Mapping: " + uri + " to " + path );

                //For contentlet lets use the language to build the key for the cache
                cache.put( getPrimaryGroup() + hostId + ":" + uri, path, getPrimaryGroup() + "_" + hostId + "_" + languageId );
                ret = path;

            }else {
    			String path = APILocator.getFileAPI().getRelativeAssetPath((Inode)asset);
    			//add the entry to the cache
    		    Logger.debug(LiveCache.class, "Mapping: " + uri + " to " + path);
    			cache.put(getPrimaryGroup() + hostId + ":" + uri,path, getPrimaryGroup() + "_" + hostId);
    			ret = path;
    		}
    	  }
        } catch (DotDataException e) {
        	Logger.error(LiveCache.class,"Unable to retrieve identifier", e);
        	throw new DotRuntimeException(e.getMessage(), e);
		} catch (DotSecurityException e) {
        	Logger.error(LiveCache.class,"Unable to retrieve identifier", e);
        	throw new DotRuntimeException(e.getMessage(), e);
		}
		return ret;
    }

    /**
     * This method return the asset uri when the asset exists in the cache
     *
     * @param URI
     * @param host
     * @return null if the asset is not in the cache, the asset uri if the asset is in the cache
     * @throws DotSecurityException
     * @throws DotDataException
     * @throws DotStateException
     */
    public static String getPathFromCache ( String URI, Host host ) throws DotStateException, DotDataException, DotSecurityException {
        return getPathFromCache( URI, host, null );
    }

    /**
     * This method return the asset uri when the asset exists in the cache
     *
     * @param URI
     * @param host
     * @param languageId
     * @return null if the asset is not in the cache, the asset uri if the asset is in the cache
     * @throws DotSecurityException
     * @throws DotDataException
     * @throws DotStateException
     */
    public static String getPathFromCache ( String URI, Host host, Long languageId ) throws DotStateException, DotDataException, DotSecurityException {
        if ( URI.equals( "/" ) ) {
            String pointer = (String) VirtualLinksCache.getPathFromCache( host.getHostname() + ":/cmsHomePage" );
            if ( !UtilMethods.isSet( pointer ) ) {
                pointer = (String) VirtualLinksCache.getPathFromCache( "/cmsHomePage" );
            }
            if ( UtilMethods.isSet( pointer ) )
                URI = pointer;
        }
        return getPathFromCache( URI, host.getIdentifier(), languageId );
    }

    /**
     * This method return the asset uri when the asset exists in the cache
     *
     * @param URI
     * @param hostId
     * @return null if the asset is not in the cache, the asset uri if the asset is in the cache
     * @throws DotSecurityException
     * @throws DotDataException
     * @throws DotStateException
     */
    public static String getPathFromCache ( String URI, String hostId ) throws DotStateException, DotDataException, DotSecurityException {
        return getPathFromCache( URI, hostId, null );
    }

    /**
     * This method return the asset uri when the asset exists in the cache
     * @param URI
     * @param hostId
     * @param languageId
     * @return null if the asset is not in the cache, the asset uri if the asset is in the cache
     * @throws DotSecurityException
     * @throws DotDataException
     * @throws DotStateException
     */
	public static String getPathFromCache(String URI, String hostId, Long languageId) throws DotStateException, DotDataException, DotSecurityException{

		DotCacheAdministrator cache = CacheLocator.getCacheAdministrator();

        if ( languageId == null ) {
            languageId = APILocator.getLanguageAPI().getDefaultLanguage().getId();
        }

		String _uri = null;
        try {
            //First lets search in cache for a specific language
            _uri = (String) cache.get( getPrimaryGroup() + hostId + ":" + URI, getPrimaryGroup() + "_" + hostId + "_" + languageId );
            //If nothing found try without a language
            if ( _uri == null ) {
                _uri = (String) cache.get( getPrimaryGroup() + hostId + ":" + URI, getPrimaryGroup() + "_" + hostId );
            }
        } catch ( DotCacheException e ) {
            Logger.debug( LiveCache.class, "Cache Entry not found", e );
        }

		if(_uri != null)
		{
			if(_uri.equals(WebKeys.Cache.CACHE_NOT_FOUND))
				return null;
		    return _uri;
		}

		String ext = Config.getStringProperty("VELOCITY_PAGE_EXTENSION");
		if (URI.endsWith("/")) {
			//it's a folder path, so I add index.{pages ext} at the end
			URI += "index." + ext;

			// try again with an index page this time
			try{
				_uri = (String) cache.get(getPrimaryGroup() + hostId + ":" + URI,getPrimaryGroup() + "_" + hostId);
			}catch (DotCacheException e) {
				Logger.debug(LiveCache.class,"Cache Entry not found", e);
	    	}

			if(_uri != null)
			{
				if(_uri.equals(WebKeys.Cache.CACHE_NOT_FOUND))
					return null;
			    return _uri;
			}
		}


		// lets try to lazy get it.
		Host fake = new Host();
		fake.setIdentifier(hostId);
		Identifier id = APILocator.getIdentifierAPI().find( fake,URI);

		if(!InodeUtils.isSet(id.getInode()))
		{
			cache.put(getPrimaryGroup() + hostId + ":" + URI, WebKeys.Cache.CACHE_NOT_FOUND, getPrimaryGroup() + "_" + hostId);

			//it's a folder path, so I add index.html at the end
			URI += "/index." + ext;
			id = APILocator.getIdentifierAPI().find( fake, URI);
			if(!InodeUtils.isSet(id.getInode()))
			{
				cache.put(getPrimaryGroup() + hostId + ":" + URI, WebKeys.Cache.CACHE_NOT_FOUND, getPrimaryGroup() + "_" + hostId);
			    return null;
			}
		}

		Versionable asset;
		if(id.getAssetType().equals("contentlet")){

            User systemUser = APILocator.getUserAPI().getSystemUser();
            try {
                asset = APILocator.getContentletAPI().findContentletByIdentifier( id.getId(), true, languageId, systemUser, false );
            } catch ( DotContentletStateException e ) {

                Logger.debug( LiveCache.class, e.getMessage() );

                //If we did not find the asset with for given language lets try with the default language
                if ( !languageId.equals( APILocator.getLanguageAPI().getDefaultLanguage().getId() ) ) {
                    languageId = APILocator.getLanguageAPI().getDefaultLanguage().getId();
                    asset = APILocator.getContentletAPI().findContentletByIdentifier( id.getId(), true, languageId, systemUser, false );
                } else {
                    throw e;
                }
            }

        } else {
			asset =  APILocator.getVersionableAPI().findLiveVersion(id, APILocator.getUserAPI().getSystemUser(), false);
		}

		if(asset!=null && InodeUtils.isSet(asset.getInode()))
		{
		    Logger.debug(PublishFactory.class, "Lazy Mapping: " + id.getURI() + " to " + URI);
		    //The cluster entry doesn't need to be invalidated when loading the entry lazily,
		    //if the entry gets invalidated from the cluster in this case causes an invalidation infinite loop
            return addToLiveAssetToCache( asset, languageId );
        } else {
			//Identifier exists but the asset is not live
			cache.put(getPrimaryGroup() + hostId + ":" + URI, WebKeys.Cache.CACHE_NOT_FOUND, getPrimaryGroup() + "_" + hostId);
		    return null;
		}

	}

	/**
	 * This method removes the asset key from the cache and send an invalidation message
	 * to the cluster when the cms is in cluster
	 * @param asset
	 */
	public static void removeAssetFromCache(Versionable asset){
		DotCacheAdministrator cache = CacheLocator.getCacheAdministrator();
    	HostAPI hostAPI = APILocator.getHostAPI();

		try{
	    	User systemUser = APILocator.getUserAPI().getSystemUser();
	    	Host host = hostAPI.findParentHost((Treeable)asset, systemUser, false);
	    	if(host == null)
	    		return;
		    String hostId = host.getIdentifier();
			Identifier identifier = APILocator.getIdentifierAPI().find(asset);

			if(identifier.getAssetType().equals("contentlet")){
				Contentlet c = (Contentlet) asset;
				long languageId = UtilMethods.isSet(c.getLanguageId())?c.getLanguageId():APILocator.getLanguageAPI().getDefaultLanguage().getId();
				cache.remove(getPrimaryGroup() + hostId + ":" + identifier.getURI(),getPrimaryGroup() + "_" + hostId + "_" + languageId  );
			} else {
				cache.remove(getPrimaryGroup() + hostId + ":" + identifier.getURI(),getPrimaryGroup() + "_" + hostId);
			}

		}catch (Exception e) {
			Logger.error(LiveCache.class, "Unable to remove asset from live cache", e);
		}
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
    	return "LiveCache";
    }
}