/*
 * Created on May 30, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package com.dotmarketing.business;

import java.util.List;

import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.VersionInfo;
import com.dotmarketing.portlets.contentlet.model.ContentletVersionInfo;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;

/**
 * @author David
 * 
 */
public class IdentifierCacheImpl extends IdentifierCache {

	DotCacheAdministrator cache = null;

	protected IdentifierCacheImpl() {

		cache = CacheLocator.getCacheAdministrator();
	}

	protected void addIdentifierToCache(Identifier id) {

		if (id == null || ! InodeUtils.isSet(id.getInode())) {
			return;
		}
		// Obtain the key for the new entrance
		String key = id.getHostId() + "-" + id.getURI();

		// Add the new entry to the cache
		cache.put(getPrimaryGroup() + key, id, getPrimaryGroup());
		cache.put(getPrimaryGroup() + id.getInode(), id, getPrimaryGroup());
		
	}
	
	protected Identifier getIdentifier(Host host,String URI) {
		if(host ==null) return null;
		return getIdentifier(host.getIdentifier(),URI);
	}

	/**
	 * This method find the identifier associated with the given URI this
	 * methods will try to find the identifier in memory but if it is not found
	 * in memory it'll be found in db and put in memory.
	 * 
	 * @param URI
	 *            uri of the identifier
	 * @param hostId
	 *            host where the identifier belongs
	 * @return The identifier or an empty (inode = 0) identifier if it wasn't
	 *         found in momory and db.
	 */
	protected Identifier getIdentifier( String URI, String hostId) {

		Identifier value = null;
		try {
			value = (Identifier) cache.get(getPrimaryGroup() + hostId + "-" + URI, getPrimaryGroup());
		} catch (DotCacheException e) {
			Logger.debug(IdentifierCacheImpl.class, "Cache Entry not found", e);
		}

		return value;
	}


	protected Identifier getIdentifier(String identId)  {

		Identifier value = null;
		try {
			value = (Identifier) cache.get(getPrimaryGroup() + identId, getPrimaryGroup());
		} catch (DotCacheException e) {
			Logger.debug(IdentifierCacheImpl.class, "Cache Entry not found", e);
		}

		return value;
	}



	protected Identifier getIdentifier(Versionable versionable)  {

		Identifier value = null;
		try {
			value = (Identifier) cache.get(getPrimaryGroup() + versionable.getVersionId(), getPrimaryGroup());
		} catch (DotCacheException e) {
			Logger.debug(IdentifierCacheImpl.class, "Cache Entry not found", e);
		}

		if (value == null) {
			value = new Identifier();
		}
		return value;
	}
	

	protected void removeFromCacheByIdentifier(Identifier id) {
		if(id==null) return;
		
		cache.remove(getPrimaryGroup() + id.getId(),  getPrimaryGroup());
		String key = id.getHostId() + "-" + id.getURI();
		cache.remove(getPrimaryGroup() + key, getPrimaryGroup());
		
		if(UtilMethods.isSet(id.getAssetType()) && id.getAssetType().equals("folder")) {
		    try {
		        List<Identifier> idents=APILocator.getIdentifierAPI().findByParentPath(id.getHostId(), id.getURI());
		        for(Identifier ii : idents)
		            removeFromCacheByIdentifier(ii);
		    }
		    catch(Exception ex) {
		        Logger.warn(this, ex.getMessage(),ex);
		    }
		}
	}
	
	protected void removeFromCacheByIdentifier(String ident) {
		
		Identifier id = getIdentifier(ident);
		if(id==null){
			id=new Identifier();
			id.setId(ident);
		}
		
		removeFromCacheByIdentifier(id);

	}
	


	protected void removeFromCacheByURI(String hostId,String URI) {
		Identifier id = getIdentifier(hostId,URI);
		String key = hostId + "-" + URI;
		cache.remove(getPrimaryGroup() + key, getPrimaryGroup());
		removeFromCacheByIdentifier(id);

	}

	 public void removeFromCacheByVersionable(Versionable versionable) {
		
		removeFromCacheByIdentifier(versionable.getVersionId());


	}
	
	
	public void clearCache() {
		// clear the cache
		cache.flushGroup(getPrimaryGroup());
		cache.flushGroup(getVersionInfoGroup());
	}

    @Override
    protected void addContentletVersionInfoToCache(ContentletVersionInfo contV) {
        String key=contV.getIdentifier()+"-lang:"+contV.getLang();
        cache.put(getVersionInfoGroup()+key, contV, getVersionInfoGroup());
    }

    @Override
    protected void addVersionInfoToCache(VersionInfo versionInfo) {
        String key=versionInfo.getIdentifier();
        cache.put(getVersionInfoGroup()+key, versionInfo, getVersionInfoGroup());
    }

    @Override
    protected ContentletVersionInfo getContentVersionInfo(String identifier, long lang) {
        ContentletVersionInfo contV = null;
        try {
            String key=contV.getIdentifier()+"-lang:"+contV.getLang();
            contV = (ContentletVersionInfo)cache.get(getVersionInfoGroup()+key, getVersionInfoGroup());
        }
        catch(Exception ex) {
            Logger.debug(this, identifier +" contentVersionInfo not found in cache");
        }
        return contV;
    }

    @Override
    protected VersionInfo getVersionInfo(String identifier) {
        VersionInfo vi = null;
        try {
            vi = (VersionInfo)cache.get(getVersionInfoGroup()+identifier, getVersionInfoGroup());
        }
        catch(Exception ex) {
            Logger.debug(this, identifier +" versionInfo not found in cache");
        }
        return vi;
    }

    @Override
    protected void removeContentletVersionInfoToCache(String identifier, long lang) {
        String key=identifier+"-lang:"+lang;
        cache.remove(getVersionInfoGroup()+key, getVersionInfoGroup());
    }

    @Override
    protected void removeVersionInfoFromCache(String identifier) {
        cache.remove(getVersionInfoGroup()+identifier, getVersionInfoGroup());
    }
}
