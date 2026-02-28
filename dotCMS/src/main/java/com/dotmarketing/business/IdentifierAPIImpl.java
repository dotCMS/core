package com.dotmarketing.business;

import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.business.WrapInTransaction;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.Inode;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.factories.InodeFactory;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import java.util.List;

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

		Identifier identifier = identifierFactory.loadFromCache(inodeOrIdentifier);

		if(identifier == null || !InodeUtils.isSet(identifier.getInode())){
			identifier = identifierFactory.loadFromCacheFromInode(inodeOrIdentifier);
		}
		
		if (identifier == null || !InodeUtils.isSet(identifier.getInode())) {
			try {
				Contentlet con = contentletAPI.find(inodeOrIdentifier, APILocator.getUserAPI().getSystemUser(), false);
				if (con != null && InodeUtils.isSet(con.getInode())) {
					identifier = identifierFactory.find(con.getIdentifier());
					return identifier;
				}
			} catch (Exception e) {
				Logger.debug(this, "Unable to find inodeOrIdentifier as content : ", e);
			}
		} else {
			return identifier;
		}

		try {
			identifier = identifierFactory.find(inodeOrIdentifier);
		} catch (DotHibernateException e) {
			Logger.debug(this, "Unable to find inodeOrIdentifier as identifier : ", e);
		}

		if (identifier == null || !InodeUtils.isSet(identifier.getInode())) {
			 identifier = identifierFactory.find(InodeFactory.getInode(inodeOrIdentifier, Inode.class));
		}
		
		if (identifier != null && InodeUtils.isSet(identifier.getId()) ) {
			CacheLocator.getIdentifierCache().addIdentifierToCache(identifier.getId(), inodeOrIdentifier);
		}
		
		return identifier;
		
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
		return identifierFactory.saveIdentifier(id);
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

	@Override
	public Identifier createNew(final Folder folder, final Treeable parent) throws DotDataException{
		return createNew(folder, parent,null);
	}


    @WrapInTransaction
	@Override
	public Identifier createNew(final Folder folder, final Treeable parent,
			final String existingId) throws DotDataException {

		Logger.debug(IdentifierAPIImpl.class, () -> String.format(
				"Creating new identifier for folder `%s` ", folder.getName()));

		if (UtilMethods.isNotSet(existingId)) {

			final String validId =
					APILocator.getDeterministicIdentifierAPI().generateDeterministicIdBestEffort(folder, parent);

			if (parent instanceof Folder) {
				return identifierFactory.createNewIdentifier(folder, (Folder) parent, validId);
			}

			if (parent instanceof Host) {
				return identifierFactory.createNewIdentifier(folder, (Host) parent, validId);
			}

		} else {

			if (parent instanceof Folder) {
				return identifierFactory.createNewIdentifier(folder, (Folder) parent, existingId);
			}

			if (parent instanceof Host) {
				return identifierFactory.createNewIdentifier(folder, (Host) parent, existingId);
			}
		}
		throw new DotStateException(
				"You can only create an identifier on a host of folder.  Trying: " + parent);

	}


	@WrapInTransaction
	public Identifier createNew(final Versionable asset, final Treeable parent,
			final String existingId) throws DotDataException {

		Logger.debug(IdentifierAPIImpl.class, () -> String.format(
				"Creating new identifier for versionable asset of type `%s` and title `%s` ",
				asset.getVersionType(), asset.getTitle()));

		if (UtilMethods.isNotSet(existingId)) {

			final String validId =
					APILocator.getDeterministicIdentifierAPI().generateDeterministicIdBestEffort(asset, parent);

			if (parent instanceof Folder) {
				return identifierFactory.createNewIdentifier(asset, (Folder) parent, validId);
			}

			if (parent instanceof Host) {
				return identifierFactory.createNewIdentifier(asset, (Host) parent, validId);
			}

		} else {

			if (parent instanceof Folder) {
				return identifierFactory.createNewIdentifier(asset, (Folder) parent, existingId);
			}

			if (parent instanceof Host) {
				return identifierFactory.createNewIdentifier(asset, (Host) parent, existingId);
			}
		}
		throw new DotStateException(
				"You can only create an identifier on a host of folder.  Trying: " + parent);

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

	@Override
	@WrapInTransaction
	public void updateUserReferences(final String userId, final String replacementUserId)
			throws DotDataException, DotSecurityException {
		identifierFactory.updateUserReferences(userId,replacementUserId);
	}

}
