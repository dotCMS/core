package com.dotmarketing.business;

import java.util.Date;
import java.util.List;

import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.Inode;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.factories.InodeFactory;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;

public class IdentifierAPIImpl implements IdentifierAPI {

	private ContentletAPI conAPI;
	private IdentifierFactory ifac;

	public IdentifierAPIImpl() {
		conAPI = APILocator.getContentletAPI();
		ifac = FactoryLocator.getIdentifierFactory();
	}

	@Override
	public List<Identifier> findByURIPattern(String assetType, String uri,boolean hasLive, boolean onlyDeleted,boolean include,Host host) throws DotDataException {
		return ifac.findByURIPattern(assetType,uri,hasLive,onlyDeleted,include, host);
	}
	
	@Override
	public List<Identifier> findByURIPattern(String assetType, String uri, boolean hasLive,boolean onlyDeleted, boolean include, Host host, Date startDate, Date endDate) throws DotDataException {
		return ifac.findByURIPattern(assetType, uri, hasLive,onlyDeleted,include, host, startDate, endDate);
	}
	
	public Identifier findFromInode(String inodeOrIdentifier) throws DotDataException {
		Identifier ident = ifac.loadFromCache(inodeOrIdentifier);

		
		if(ident ==null){
			// try to load it from a id first, then by a proxy Inode
			Contentlet proxy = new Contentlet();
			proxy.setInode(inodeOrIdentifier);
			ident = ifac.loadFromCache(proxy);
		}
		
		if (ident == null || !InodeUtils.isSet(ident.getInode())) {
			try {
				Contentlet con = conAPI.find(inodeOrIdentifier, APILocator.getUserAPI().getSystemUser(), false);
				if (con != null && InodeUtils.isSet(con.getInode())) {
					ident = ifac.find(con.getIdentifier());
					return ident;
				}
			} catch (Exception e) {
				Logger.debug(this, "Unable to find inodeOrIdentifier as content : ", e);
			}
		} else {
			return ident;
		}

		try {
			ident = ifac.find(inodeOrIdentifier);
		} catch (DotHibernateException e) {
			Logger.debug(this, "Unable to find inodeOrIdentifier as identifier : ", e);
		}

		
		if (ident == null || !InodeUtils.isSet(ident.getInode())) {
			 ident = ifac.find(InodeFactory.getInode(inodeOrIdentifier, Inode.class));
		} 
		
		return ident;
		
	}

	public Identifier find(String identifier) throws DotDataException {
		return ifac.find(identifier);

	}

	public Identifier find(Versionable versionable) throws DotDataException {
		if (versionable == null || (!InodeUtils.isSet(versionable.getVersionId()) && !InodeUtils.isSet(versionable.getInode()))) {
			throw new DotStateException("Versionable is null");
		}
		return ifac.find(versionable);

	}

	public boolean isIdentifier(String identifierInode) throws DotDataException {
		return ifac.isIdentifier(identifierInode);
	}

	public Identifier find(Host host, String uri) throws DotDataException, DotStateException {
		return ifac.findByURI(host, uri);
	}

	public Identifier loadFromCache(Host host, String uri) throws DotDataException, DotStateException {
		return ifac.loadByURIFromCache(host, uri);
	}

	public Identifier loadFromCache(Versionable version) throws DotDataException, DotStateException {
		return ifac.loadFromCache(version);
	}

	public Identifier loadFromCache(String id) throws DotDataException, DotStateException {
		return ifac.loadFromCache(id);
	}

	public Identifier loadFromDb(String id) throws DotDataException, DotStateException {
		return ifac.loadFromDb(id);
	}

	public Identifier save(Identifier id) throws DotDataException, DotStateException {
		Identifier ident = ifac.saveIdentifier(id);
		CacheLocator.getIdentifierCache().removeFromCacheByIdentifier(id.getId());
		return ident;
	}

	public void delete(Identifier id) throws DotDataException, DotStateException {
		if(id==null || !UtilMethods.isSet(id.getId())){
			throw new DotStateException ("you cannot delete a null identifier");
		}
		ifac.deleteIdentifier(id);
	}
	public Identifier createNew(Versionable asset, Treeable parent) throws DotDataException{
	    return createNew(asset,parent,null);
	}
	public Identifier createNew(Versionable asset, Treeable parent, String existingId) throws DotDataException{
		if(parent instanceof Folder){
		    if(UtilMethods.isSet(existingId))
		        return ifac.createNewIdentifier(asset, (Folder) parent, existingId);
		    else
		        return ifac.createNewIdentifier(asset, (Folder) parent);
		}else if(parent instanceof Host){
		    if(UtilMethods.isSet(existingId))
		        return ifac.createNewIdentifier(asset, (Host) parent, existingId);
		    else
		        return ifac.createNewIdentifier(asset, (Host) parent);
		}
		else{
			throw new DotStateException("You can only create an identifier on a host of folder.  Trying: " + parent);
		}
	}

	public void updateIdentifierURI(Versionable webasset, Folder folder) throws DotDataException {
		ifac.updateIdentifierURI(webasset, folder);
	}
	
	public List<Identifier> findByParentPath(String hostId, String parent_path) throws DotHibernateException {
	    return ifac.findByParentPath(hostId, parent_path);
	}
}
