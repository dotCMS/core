package com.dotmarketing.business;

import java.util.Date;
import java.util.List;

import com.dotcms.business.CloseDBIfOpened;
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

	private final ContentletAPI contentletAPI;
	private final IdentifierFactory identifierFactory;

	public IdentifierAPIImpl() {
		contentletAPI = APILocator.getContentletAPI();
		identifierFactory = FactoryLocator.getIdentifierFactory();
	}

	@Override
	public List<Identifier> findByURIPattern(String assetType, String uri,boolean hasLive, boolean onlyDeleted,boolean include,Host host) throws DotDataException {
		return identifierFactory.findByURIPattern(assetType,uri,hasLive,onlyDeleted,include, host);
	}
	
	@Override
	public List<Identifier> findByURIPattern(String assetType, String uri, boolean hasLive,boolean onlyDeleted, boolean include, Host host, Date startDate, Date endDate) throws DotDataException {
		return identifierFactory.findByURIPattern(assetType, uri, hasLive,onlyDeleted,include, host, startDate, endDate);
	}
	
	public Identifier findFromInode(String inodeOrIdentifier) throws DotDataException {
		Identifier ident = identifierFactory.loadFromCache(inodeOrIdentifier);

		if(ident == null || !InodeUtils.isSet(ident.getInode())){
			ident = identifierFactory.loadFromCacheFromInode(inodeOrIdentifier);
		}
		
		if (ident == null || !InodeUtils.isSet(ident.getInode())) {
			try {
				Contentlet con = contentletAPI.find(inodeOrIdentifier, APILocator.getUserAPI().getSystemUser(), false);
				if (con != null && InodeUtils.isSet(con.getInode())) {
					ident = identifierFactory.find(con.getIdentifier());
					return ident;
				}
			} catch (Exception e) {
				Logger.debug(this, "Unable to find inodeOrIdentifier as content : ", e);
			}
		} else {
			return ident;
		}

		try {
			ident = identifierFactory.find(inodeOrIdentifier);
		} catch (DotHibernateException e) {
			Logger.debug(this, "Unable to find inodeOrIdentifier as identifier : ", e);
		}

		
		if (ident == null || !InodeUtils.isSet(ident.getInode())) {
			 ident = identifierFactory.find(InodeFactory.getInode(inodeOrIdentifier, Inode.class));
		}
		
		if (ident != null && InodeUtils.isSet(ident.getId()) ) {
			CacheLocator.getIdentifierCache().addIdentifierToCache(ident.getId(), inodeOrIdentifier);
		}
		
		return ident;
		
	}

	@CloseDBIfOpened
	public Identifier find(final String identifier) throws DotDataException {
		return identifierFactory.find(identifier);

	}

	@CloseDBIfOpened
	public Identifier find(final Versionable versionable) throws DotDataException {

		if (versionable == null || (!InodeUtils.isSet(versionable.getVersionId()) && !InodeUtils.isSet(versionable.getInode()))) {

			throw new DotStateException("Versionable is null");
		}
		return this.identifierFactory.find(versionable);

	}

	public boolean isIdentifier(String identifierInode) throws DotDataException {
		return identifierFactory.isIdentifier(identifierInode);
	}

	public Identifier find(Host host, String uri) throws DotDataException, DotStateException {
		return identifierFactory.findByURI(host, uri);
	}

	public Identifier loadFromCache(Host host, String uri) throws DotDataException, DotStateException {
		return identifierFactory.loadByURIFromCache(host, uri);
	}

	public Identifier loadFromCache(Versionable version) throws DotDataException, DotStateException {
		return identifierFactory.loadFromCache(version);
	}

	public Identifier loadFromCache(String id) throws DotDataException, DotStateException {
		return identifierFactory.loadFromCache(id);
	}

	public Identifier loadFromDb(String id) throws DotDataException, DotStateException {
		return identifierFactory.loadFromDb(id);
	}

	public Identifier save(Identifier id) throws DotDataException, DotStateException {
		Identifier ident = identifierFactory.saveIdentifier(id);
		CacheLocator.getIdentifierCache().removeFromCacheByIdentifier(ident.getId());
		return ident;
	}

	public void delete(Identifier id) throws DotDataException, DotStateException {
		if(id==null || !UtilMethods.isSet(id.getId())){
			throw new DotStateException ("you cannot delete a null identifier");
		}
		identifierFactory.deleteIdentifier(id);
	}
	public Identifier createNew(Versionable asset, Treeable parent) throws DotDataException{
	    return createNew(asset,parent,null);
	}
	public Identifier createNew(Versionable asset, Treeable parent, String existingId) throws DotDataException{
		if(parent instanceof Folder){
		    if(UtilMethods.isSet(existingId))
		        return identifierFactory.createNewIdentifier(asset, (Folder) parent, existingId);
		    else
		        return identifierFactory.createNewIdentifier(asset, (Folder) parent);
		}else if(parent instanceof Host){
		    if(UtilMethods.isSet(existingId))
		        return identifierFactory.createNewIdentifier(asset, (Host) parent, existingId);
		    else
		        return identifierFactory.createNewIdentifier(asset, (Host) parent);
		}
		else{
			throw new DotStateException("You can only create an identifier on a host of folder.  Trying: " + parent);
		}
	}

	public void updateIdentifierURI(Versionable webasset, Folder folder) throws DotDataException {
		identifierFactory.updateIdentifierURI(webasset, folder);
	}
	
	public List<Identifier> findByParentPath(String hostId, String parent_path) throws DotHibernateException {
	    return identifierFactory.findByParentPath(hostId, parent_path);
	}

	public String getAssetTypeFromDB(String identifier) throws DotDataException{
		return identifierFactory.getAssetTypeFromDB(identifier);
	}

}
