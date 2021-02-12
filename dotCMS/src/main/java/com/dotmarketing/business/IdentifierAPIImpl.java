package com.dotmarketing.business;

import java.util.List;

import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.business.WrapInTransaction;
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

	@CloseDBIfOpened
	@Override
	public List<Identifier> findByURIPattern(final String assetType, final String uri,
											 final boolean include,  final Host host) throws DotDataException {

		return this.identifierFactory.findByURIPattern(assetType, uri, include, host);
	}

	@CloseDBIfOpened
	public Identifier findFromInode(final String inodeOrIdentifier) throws DotDataException {

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

	@CloseDBIfOpened
	public boolean isIdentifier(final String identifierInode) throws DotDataException {
		return identifierFactory.isIdentifier(identifierInode);
	}

	@CloseDBIfOpened
	public Identifier find(final Host host, final String uri) throws DotDataException, DotStateException {
		return identifierFactory.findByURI(host, uri);
	}

	public Identifier loadFromCache(final Host host, final String uri) throws DotDataException, DotStateException {
		return identifierFactory.loadByURIFromCache(host, uri);
	}

	public Identifier loadFromCache(final Versionable version) throws DotDataException, DotStateException {
		return identifierFactory.loadFromCache(version);
	}

	public Identifier loadFromCache(final String id) throws DotDataException, DotStateException {
		return identifierFactory.loadFromCache(id);
	}

	@CloseDBIfOpened
	public Identifier loadFromDb(final String id) throws DotDataException, DotStateException {
		return identifierFactory.loadFromDb(id);
	}

	@WrapInTransaction
	public Identifier save(final Identifier id) throws DotDataException, DotStateException {
		final Identifier ident = identifierFactory.saveIdentifier(id);
		return ident;
	}

	@WrapInTransaction
	public void delete(Identifier id) throws DotDataException, DotStateException {
		if(id==null || !UtilMethods.isSet(id.getId())){
			throw new DotStateException ("you cannot delete a null identifier");
		}
		identifierFactory.deleteIdentifier(id);
	}


	public Identifier createNew(final Versionable asset, final Treeable parent) throws DotDataException{
	    return createNew(asset,parent,null);
	}

	@WrapInTransaction
	public Identifier createNew(final Versionable asset, final Treeable parent,
								final String existingId) throws DotDataException {

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

	@WrapInTransaction
	public void updateIdentifierURI(final Versionable webasset, final Folder folder) throws DotDataException {

		identifierFactory.updateIdentifierURI(webasset, folder);
	}

	@CloseDBIfOpened
	public List<Identifier> findByParentPath(final String hostId, final String parentPath) throws DotDataException {

	    return identifierFactory.findByParentPath(hostId, parentPath);
	}

	@CloseDBIfOpened
	public String getAssetTypeFromDB(final String identifier) throws DotDataException {

		return identifierFactory.getAssetTypeFromDB(identifier);
	}

}