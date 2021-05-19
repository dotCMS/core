package com.dotmarketing.business;

import com.dotmarketing.exception.DotSecurityException;
import java.io.StringWriter;
import java.util.List;
import java.util.UUID;
import org.apache.commons.codec.digest.DigestUtils;
import org.bouncycastle.crypto.digests.SHA256Digest;
import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.business.WrapInTransaction;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.Inode;
import com.dotmarketing.beans.WebAsset;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.factories.InodeFactory;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.fileassets.business.FileAsset;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpageasset.business.HTMLPageAssetAPI;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.personas.business.PersonaAPI;
import com.dotmarketing.portlets.personas.model.Persona;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UUIDGenerator;
import com.dotmarketing.util.UtilMethods;
import com.google.common.annotations.VisibleForTesting;
import io.vavr.control.Try;

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

	@VisibleForTesting
    String resolveAssetName(Versionable asset) {
	    
	    if(asset instanceof Contentlet){
	        final Contentlet contentlet = (Contentlet)asset;
            return Try.of(()-> 
                contentlet.isHost()
                    ? contentlet.getStringProperty(Host.HOST_NAME_KEY)
                    : contentlet.isFileAsset() 
                        ? contentlet.getBinary("fileAsset").getName()
                        : contentlet.isHTMLPage()            
                            ? contentlet.getStringProperty(HTMLPageAssetAPI.URL_FIELD)
                            : contentlet.isPersona()
                                ? contentlet.getStringProperty(PersonaAPI.KEY_TAG_FIELD)
                                : UUIDGenerator.generateUuid()
                    
            )
            .getOrElseThrow(e->new DotRuntimeException(e));
	    }
	    if(asset instanceof WebAsset) {
	        return ((WebAsset)asset).getTitle();
	    }
	    if(asset instanceof Folder) {
	        return ((Folder)asset).getName();
	    }
	    
	    return UUIDGenerator.generateUuid();

    }
	
    @VisibleForTesting
    String resolveAssetType(Versionable asset) {
        
        if(asset instanceof Contentlet){
            final Contentlet contentlet = (Contentlet)asset;
            return contentlet.getContentType().variable();
        }

        return asset.getClass().getSimpleName();



    }
	
	@VisibleForTesting
	@CloseDBIfOpened
    String generateKnownId(final Versionable asset, final Treeable parent) {

        final Host parentHost = (parent instanceof Host) ? (Host) parent : Try.of(()-> APILocator.getHostAPI().find(((Folder)parent).getHostId(),APILocator.systemUser(),false)).getOrElseThrow(e->new DotRuntimeException(e));
        final Folder parentFolder = (parent instanceof Folder) ? (Folder) parent : Try.of(()-> APILocator.getFolderAPI().findSystemFolder()).getOrElseThrow(e->new DotRuntimeException(e));
        final Language lang = (asset instanceof Contentlet) ? APILocator.getLanguageAPI().getLanguage(((Contentlet)asset).getLanguageId())
                        : APILocator.getLanguageAPI().getDefaultLanguage();


        final StringWriter writer = new StringWriter();
        writer.append(resolveAssetType(asset) + ":");
        writer.append(parentHost.getHostname());
        writer.append("/" + lang );
        writer.append(parentFolder.getPath());
        writer.append(resolveAssetName(asset));
        return writer.toString();

    }
	
	@VisibleForTesting
    @CloseDBIfOpened
    String bestEffortsKnowableId(final String stringToHash) {
        
        final String hashed =  DigestUtils.sha256Hex(stringToHash.toLowerCase()).substring(0,36);
        Logger.info(this.getClass(), "");
        Logger.info(this.getClass(), "hashing id: " + stringToHash + " to " + hashed);
        Logger.info(this.getClass(), "");
        if(new DotConnect()
                        .setSQL("select count(id) as test from identifier where id=?")
                        .addParam(hashed)
                        .getInt("test")>0) {
            return bestEffortsKnowableId(UUIDGenerator.generateUuid());
        }
        return hashed;
    }
    
    
    
	@WrapInTransaction
	public Identifier createNew(final Versionable asset, final Treeable parent,
								final String existingId) throws DotDataException {

	    final String validId = UtilMethods.isSet(existingId)
	                    ? existingId 
                        : bestEffortsKnowableId(generateKnownId(asset, parent));

		if(parent instanceof Folder){
		    return identifierFactory.createNewIdentifier(asset, (Folder) parent, validId);
		}
		
		if(parent instanceof Host){
		    return identifierFactory.createNewIdentifier(asset, (Host) parent, validId);
		}

		throw new DotStateException("You can only create an identifier on a host of folder.  Trying: " + parent);
		
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
