
package com.dotmarketing.business;

import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.business.WrapInTransaction;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.VersionInfo;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ContentletVersionInfo;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import com.rainerhahnekamp.sneakythrow.Sneaky;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.beanutils.BeanUtils;

public class VersionableAPIImpl implements VersionableAPI {

	private final VersionableFactory versionableFactory;
	private final PermissionAPI permissionAPI;

	public VersionableAPIImpl() {
		versionableFactory = FactoryLocator.getVersionableFactory();
		permissionAPI = APILocator.getPermissionAPI();
	}

	@Override
	public Versionable findWorkingVersion(final Versionable inode, final User user,
                                          final boolean respectAnonPermissions) throws DotDataException,
			                                DotStateException, DotSecurityException {

		return findWorkingVersion(inode.getVersionId(), user, respectAnonPermissions);
	}

    @Override
	public Versionable findWorkingVersion(final Identifier id, final User user,
                                          final boolean respectAnonPermissions) throws DotDataException,
			                                DotStateException, DotSecurityException {

		return findWorkingVersion(id.getId(), user, respectAnonPermissions);
	}

    @Override
	@CloseDBIfOpened
	public Versionable findWorkingVersion(final String id, final User user,
                                          final boolean respectAnonPermissions) throws DotDataException, DotStateException,
			                                    DotSecurityException {

		final Versionable asset = versionableFactory.findWorkingVersion(id);
		if (!permissionAPI.doesUserHavePermission((Permissionable) asset, PermissionAPI.PERMISSION_READ, user, respectAnonPermissions)) {
			throw new DotSecurityException("User " + user + " does not have permission to read " + id);
		}

		return asset;
	}

    @Override
	public Versionable findLiveVersion(final Versionable inode, final User user,
                                       final boolean respectAnonPermissions) throws DotDataException,
			                                DotStateException, DotSecurityException {

		return findLiveVersion(inode.getVersionId(), user, respectAnonPermissions);
	}

    @Override
	public Versionable findLiveVersion(final Identifier id, final User user,
                                       final boolean respectAnonPermissions) throws DotDataException,
			                                DotStateException, DotSecurityException {

		return findLiveVersion(id.getId(), user, respectAnonPermissions);
	}

    @Override
	@CloseDBIfOpened
	public Versionable findLiveVersion(final String id, final User user,
                                       final boolean respectAnonPermissions) throws DotDataException, DotStateException,
			                            DotSecurityException {

		final Versionable asset = versionableFactory.findLiveVersion(id);

		if (asset!=null && !permissionAPI.doesUserHavePermission((Permissionable) asset, PermissionAPI.PERMISSION_READ, user, respectAnonPermissions)) {
			throw new DotSecurityException("User " + user + " does not have permission to read " + id);
		}

		return asset;
	}

    @Override
	public Versionable findDeletedVersion(final Versionable inode, final User user,
                                          final boolean respectAnonPermissions) throws DotDataException,
			                                DotStateException, DotSecurityException {
		return findDeletedVersion(inode.getVersionId(), user, respectAnonPermissions);
	}

    @Override
	public Versionable findDeletedVersion(final Identifier id, final User user,
                                          final boolean respectAnonPermissions) throws DotDataException,
			                                    DotStateException, DotSecurityException {

		return findDeletedVersion(id.getId(), user, respectAnonPermissions);
	}

	@CloseDBIfOpened
    @Override
	public Versionable findDeletedVersion(final String id, final User user,
                                          final boolean respectAnonPermissions) throws DotDataException, DotStateException,
			                                    DotSecurityException {

		final Versionable asset = versionableFactory.findDeletedVersion(id);

		if (!permissionAPI.doesUserHavePermission((Permissionable) asset, PermissionAPI.PERMISSION_READ, user, respectAnonPermissions)) {
			throw new DotSecurityException("User " + user + " does not have permission to read " + id);
		}

		return asset;
	}

    @Override
	public List<Versionable> findAllVersions(final Versionable inode) throws DotDataException, DotStateException, DotSecurityException {
		if (inode == null) {
			throw new DotStateException("Inode is null");
		}
		return findAllVersions(inode.getVersionId(), APILocator.getUserAPI().getSystemUser(), false);
	}

	@Override
	public List<Versionable> findAllVersions(final Identifier id) throws DotDataException, DotStateException, DotSecurityException {
		if (id == null) {
			throw new DotStateException("Inode is null");
		}
		return findAllVersions(id.getId(), APILocator.getUserAPI().getSystemUser(), false);
	}

    public Optional<Versionable> findPreviousVersion(final String identifier)  throws DotDataException, DotStateException,DotSecurityException {
        if (identifier == null) {
            throw new DotStateException("Identifier is null");
        }
        return findPreviousVersion(identifier, APILocator.getUserAPI().getSystemUser(), false);
    }

    public Optional<Versionable> findPreviousVersion(final Identifier identifier)  throws DotDataException, DotStateException,DotSecurityException {
        if (identifier == null) {
            throw new DotStateException("Identifier is null");
        }
        return findPreviousVersion(identifier.getId(), APILocator.getUserAPI().getSystemUser(), false);
    }

	@Override
	@CloseDBIfOpened
	public List<Versionable> findAllVersions(final String id) throws DotDataException, DotStateException, DotSecurityException {
		return findAllVersions(id, APILocator.getUserAPI().getSystemUser(), false);
	}

	@Override
	public List<Versionable> findAllVersions(final Versionable id, final User user,
                                             final boolean respectAnonPermissions) throws DotDataException,
			                                    DotStateException, DotSecurityException {

		if (id == null) {
			throw new DotStateException("Versionable is null");
		}
		return findAllVersions(id.getVersionId(), user, respectAnonPermissions);
	}

	@Override
	public List<Versionable> findAllVersions(final Identifier id, final User user,
                                             final boolean respectAnonPermissions) throws DotDataException,
			                                    DotStateException, DotSecurityException {

		if (id == null) {
			throw new DotStateException("Inode is null");
		}
		return findAllVersions(id.getId(), user, respectAnonPermissions);
	}

	@CloseDBIfOpened
	@Override
	public List<Versionable> findAllVersions(final String id, final User user,
                                             final boolean respectAnonPermissions) throws DotDataException,
			                                    DotStateException, DotSecurityException {

		List<Versionable> versions = versionableFactory.findAllVersions(id);
		List<Permissionable> pass  = new ArrayList<Permissionable>();

		for (Versionable v : versions) {
			if (v instanceof Permissionable) {
				pass.add((Permissionable) v);
			}
		}

		pass = permissionAPI.filterCollection(pass, PermissionAPI.PERMISSION_READ, respectAnonPermissions, user);
		versions = new ArrayList<Versionable>();

		for (Permissionable p : pass) {
			if (p instanceof Versionable) {
				versions.add((Versionable) p);
			}
		}

		return versions;
	}

    @CloseDBIfOpened
    @Override
	public Optional<Versionable> findPreviousVersion(final String identifier, final User user,
                                                      final boolean respectAnonPermissions)  throws DotDataException, DotStateException, DotSecurityException {

        final List<Versionable> versions =
                versionableFactory.findAllVersions(identifier, Optional.of(2))
                .stream()
                .filter(versionable -> versionable instanceof  Permissionable &&
                        Sneaky.sneaked(()->permissionAPI.doesUserHavePermission((Permissionable)versionable,
                                PermissionAPI.PERMISSION_READ, user, respectAnonPermissions)).get())
                .collect(Collectors.toList());

        return versions.size() >= 2?
                Optional.ofNullable(versions.get(1)):
                Optional.empty();
    }

	@CloseDBIfOpened
    @Override
    public boolean isDeleted(final Versionable versionable) throws DotDataException, DotStateException, DotSecurityException {

        if(!UtilMethods.isSet(versionable) || !InodeUtils.isSet(versionable.getVersionId()))
        	return false;

        final Identifier identifier = APILocator.getIdentifierAPI().find(versionable.getVersionId());

        if(!UtilMethods.isSet(identifier.getId()))
        	return false;

        if(identifier.getAssetType().equals("contentlet")) {
            final Contentlet contentlet = (Contentlet)versionable;
            final ContentletVersionInfo cinfo = versionableFactory.
                    getContentletVersionInfo(contentlet.getIdentifier(), contentlet.getLanguageId());

            return (cinfo !=null && cinfo.isDeleted());
        } else {

            final VersionInfo info = versionableFactory.getVersionInfo(versionable.getVersionId());
            if(!UtilMethods.isSet(info.getIdentifier()))
                throw new DotStateException("No version info. Call setWorking first");

            return info.isDeleted();
        }
    }

    @CloseDBIfOpened
    @Override
    public boolean isLive(final Versionable versionable) throws DotDataException, DotStateException, DotSecurityException {

        if(!UtilMethods.isSet(versionable) || !InodeUtils.isSet(versionable.getVersionId()))
        	return false;

        final Identifier identifier = APILocator.getIdentifierAPI().find(versionable);
        if(identifier==null || !UtilMethods.isSet(identifier.getId()) || !UtilMethods.isSet(identifier.getAssetType()))
            return false;

        String liveInode;
        if(identifier.getAssetType().equals("contentlet")) {
            final Contentlet contentlet = (Contentlet)versionable;
            final ContentletVersionInfo info = versionableFactory
                    .getContentletVersionInfo(contentlet.getIdentifier(), contentlet.getLanguageId());

            if(info ==null || !UtilMethods.isSet(info.getIdentifier()))
                throw new DotStateException("No version info. Call setWorking first "+identifier.getId());

            liveInode=info.getLiveInode();
        } else {
            final VersionInfo info = versionableFactory.getVersionInfo(versionable.getVersionId());
            if(!UtilMethods.isSet(info.getIdentifier()))
                throw new DotStateException("No version info. Call setWorking first");

            liveInode = info.getLiveInode();
        }
        return liveInode!=null && liveInode.equals(versionable.getInode());
    }

    @CloseDBIfOpened
    @Override
    public boolean isLocked(final Versionable versionable) throws DotDataException, DotStateException, DotSecurityException {

        if(!UtilMethods.isSet(versionable) || !InodeUtils.isSet(versionable.getVersionId())){
            return false;
        }

        final Identifier identifier = APILocator.getIdentifierAPI().find(versionable.getVersionId());
        if(identifier==null || !UtilMethods.isSet(identifier.getId()) || !UtilMethods.isSet(identifier.getAssetType())) {
            return false;
        }
        if("contentlet".equals(identifier.getAssetType())) {
            final Contentlet contentlet = (Contentlet)versionable;
            final ContentletVersionInfo info = versionableFactory.getContentletVersionInfo(contentlet.getIdentifier(),contentlet.getLanguageId());

            if(!UtilMethods.isSet(info.getIdentifier()))
                throw new DotStateException("No version info. Call setWorking first");

            return info.isLocked();
        } else {
            final VersionInfo info = versionableFactory.getVersionInfo(versionable.getVersionId());
            if(!UtilMethods.isSet(info.getIdentifier()))
                throw new DotStateException("No version info. Call setWorking first");

            return info.isLocked();
        }
    }

    @CloseDBIfOpened
    @Override
    public boolean isWorking(final Versionable versionable) throws DotDataException, DotStateException, DotSecurityException {

        if(!UtilMethods.isSet(versionable) || !InodeUtils.isSet(versionable.getVersionId()))
        	return false;

        final Identifier identifier = APILocator.getIdentifierAPI().find(versionable);
        if(identifier==null || !UtilMethods.isSet(identifier.getId()) || !UtilMethods.isSet(identifier.getAssetType()))
            return false;

        String workingInode;
        if(identifier.getAssetType().equals("contentlet")) {

            final Contentlet contentlet = (Contentlet)versionable;
            final ContentletVersionInfo info= versionableFactory
                    .getContentletVersionInfo(contentlet.getIdentifier(), contentlet.getLanguageId());
            if(info ==null || !UtilMethods.isSet(info.getIdentifier()))
                throw new DotStateException("No version info. Call setWorking first");

            workingInode = info.getWorkingInode();
        } else {

            final VersionInfo info = versionableFactory.getVersionInfo(versionable.getVersionId());
            if(!UtilMethods.isSet(info.getIdentifier()))
                throw new DotStateException("No version info. Call setWorking first");

            workingInode = info.getWorkingInode();
        }

        return workingInode.equals(versionable.getInode());
    }

    @WrapInTransaction
    @Override
    public void removeLive(final String identifier) throws DotDataException, DotStateException, DotSecurityException {
        if(!UtilMethods.isSet(identifier))
            throw new DotStateException("invalid identifier");

        final VersionInfo versionInfo = versionableFactory.getVersionInfo(identifier);
        if(!UtilMethods.isSet(versionInfo.getIdentifier()))
            throw new DotStateException("No version info. Call setWorking first");

        versionInfo.setLiveInode(null);
        versionableFactory.saveVersionInfo(versionInfo, true);
    }

    @WrapInTransaction
    @Override
    public void removeLive (final Contentlet contentlet ) throws DotDataException, DotStateException, DotSecurityException {

    	final String identifierId = contentlet.getIdentifier();
    	final long lang         = contentlet.getLanguageId();
    	
    	if ( !UtilMethods.isSet( identifierId ) ) {
            throw new DotStateException( "invalid identifier" );
        }

        final Identifier identifier = APILocator.getIdentifierAPI().find( identifierId );
        final ContentletVersionInfo contentletVersionInfo =
                this.versionableFactory.getContentletVersionInfo( identifierId, lang );

        if ( contentletVersionInfo == null || !UtilMethods.isSet( contentletVersionInfo.getIdentifier() ) ) {
            throw new DotStateException( "No version info. Call setLive first" );
        }

        if ( !UtilMethods.isSet( contentletVersionInfo.getLiveInode() ) ) {
            throw new DotStateException( "No live version Contentlet. Call setLive first" );
        }

        final Contentlet liveContentlet = APILocator.getContentletAPI()
                .find( contentletVersionInfo.getLiveInode(), APILocator.getUserAPI().getSystemUser(), false );

        if ( liveContentlet == null || !UtilMethods.isSet( liveContentlet.getIdentifier() ) ) {
            throw new DotStateException( "No live version Contentlet. Call setLive first" );
        }

        //Get the structure for this contentlet
        final Structure structure = CacheLocator.getContentTypeCache().getStructureByInode( liveContentlet.getStructureInode() );

        if(contentlet.getMap().get(Contentlet.DONT_VALIDATE_ME) == null) {
        	if ( UtilMethods.isSet( structure.getExpireDateVar() ) &&
                UtilMethods.isSet( identifier.getSysExpireDate() ) &&
                identifier.getSysExpireDate().after( new Date() ) ) {//Verify if the structure have a Expire Date Field set

                throw new PublishStateException(
                    "Can't unpublish content that is scheduled to expire on a future date. Identifier: " + identifier.getId() );
            }
        }
        final ContentletVersionInfo newInfo = Sneaky.sneak(()-> (ContentletVersionInfo) BeanUtils.cloneBean(contentletVersionInfo)) ;
        newInfo.setLiveInode( null );
        versionableFactory.saveContentletVersionInfo( newInfo, true );
    }

    @WrapInTransaction
    @Override
    public void setDeleted(final Versionable versionable, final boolean deleted) throws DotDataException, DotStateException, DotSecurityException {

        if(!UtilMethods.isSet(versionable.getVersionId()))
            throw new DotStateException("invalid identifier");

        final Identifier identifier = APILocator.getIdentifierAPI().find(versionable.getVersionId());

        if(identifier.getAssetType().equals("contentlet")) {
            final Contentlet contentlet      = (Contentlet)versionable;
            final ContentletVersionInfo info = versionableFactory
                    .getContentletVersionInfo(contentlet.getIdentifier(),contentlet.getLanguageId());
            
            
            
            
            if(!UtilMethods.isSet(info.getIdentifier()))
                throw new DotStateException("No version info. Call setWorking first");
            
            final ContentletVersionInfo newInfo = Sneaky.sneak(()-> (ContentletVersionInfo) BeanUtils.cloneBean(info)) ;
            newInfo.setDeleted(deleted);
            versionableFactory.saveContentletVersionInfo(newInfo, true);
        }
        else {
            final VersionInfo info = versionableFactory.getVersionInfo(versionable.getVersionId());
            if(!UtilMethods.isSet(info.getIdentifier()))
                throw new DotStateException("No version info. Call setWorking first");

            info.setDeleted(deleted);
            versionableFactory.saveVersionInfo(info, true);
        }
    }

    @WrapInTransaction
    @Override
    public void setLive ( final Versionable versionable ) throws DotDataException, DotStateException, DotSecurityException {

        if ( !UtilMethods.isSet( versionable ) || !UtilMethods.isSet( versionable.getVersionId() ) ) {
            throw new DotStateException( "invalid identifier" );
        }

        final Identifier identifier = APILocator.getIdentifierAPI().find( versionable );
        if ( identifier.getAssetType().equals( "contentlet" ) ) {

            final Contentlet contentlet = (Contentlet) versionable;
            final ContentletVersionInfo info = versionableFactory
                    .getContentletVersionInfo( contentlet.getIdentifier(), contentlet.getLanguageId() );

            if ( info == null || !UtilMethods.isSet( info.getIdentifier() ) ) {
                throw new DotStateException( "No version info. Call setWorking first" );
            }

            //Get the structure for this contentlet
            final Structure structure = CacheLocator.getContentTypeCache().getStructureByInode( contentlet.getStructureInode() );

            if ( UtilMethods.isSet( structure.getPublishDateVar() ) ) {//Verify if the structure have a Publish Date Field set
                if ( UtilMethods.isSet( identifier.getSysPublishDate() ) && identifier.getSysPublishDate().after( new Date() ) ) {
                    throw new PublishStateException( "The content cannot be published because it is scheduled to be published on future date." );
                }
            }
            if ( UtilMethods.isSet( structure.getExpireDateVar() ) ) {//Verify if the structure have a Expire Date Field set
                if ( UtilMethods.isSet( identifier.getSysExpireDate() ) && identifier.getSysExpireDate().before( new Date() ) ) {
                    throw new PublishStateException( "The content cannot be published because the expire date has already passed." );
                }
            }
            final ContentletVersionInfo newInfo = Sneaky.sneak(()-> (ContentletVersionInfo) BeanUtils.cloneBean(info)) ;
            newInfo.setLiveInode(versionable.getInode());
            versionableFactory.saveContentletVersionInfo( newInfo, true );
        } else {

            final VersionInfo info = versionableFactory.getVersionInfo( versionable.getVersionId() );
            if ( !UtilMethods.isSet( info.getIdentifier() ) ) {
                throw new DotStateException( "No version info. Call setWorking first" );
            }

            info.setLiveInode( versionable.getInode() );
            this.versionableFactory.saveVersionInfo( info, true );
        }
    }

    @WrapInTransaction
    @Override
    public void setLocked(final Versionable versionable, final boolean locked,
                          final User user) throws DotDataException, DotStateException, DotSecurityException {

        if(!UtilMethods.isSet(versionable.getVersionId()))
            throw new DotStateException("invalid identifier");

        final Identifier identifier = APILocator.getIdentifierAPI().find(versionable.getVersionId());
        if(identifier.getAssetType().equals("contentlet")) {

            final Contentlet contentlet =(Contentlet)versionable;
            final ContentletVersionInfo info = versionableFactory
                    .getContentletVersionInfo(contentlet.getIdentifier(),contentlet.getLanguageId());

            if(!UtilMethods.isSet(info.getIdentifier()))
                throw new DotStateException("No version info. Call setWorking first");
            final ContentletVersionInfo newInfo = Sneaky.sneak(()-> (ContentletVersionInfo) BeanUtils.cloneBean(info)) ;

            if(locked) {
                newInfo.setLocked(user.getUserId());
            } else {
                newInfo.unLock();
            }

            versionableFactory.saveContentletVersionInfo(newInfo, false);
        } else {

            final VersionInfo info = versionableFactory.getVersionInfo(versionable.getVersionId());
            if(!UtilMethods.isSet(info.getIdentifier()))
                throw new DotStateException("No version info. Call setWorking first");

            if(locked) {
                info.setLocked(user.getUserId());
            } else {
                info.unLock();
            }

            versionableFactory.saveVersionInfo(info, false);
        }
    }

    @WrapInTransaction
    @Override
    public void setWorking(final Versionable versionable) throws DotDataException, DotStateException, DotSecurityException {

        if(!UtilMethods.isSet(versionable) || !UtilMethods.isSet(versionable.getVersionId()))
            throw new DotStateException("invalid identifier");

        final Identifier identifier = APILocator.getIdentifierAPI().find(versionable);
        if(identifier.getAssetType().equals("contentlet")) {

            final Contentlet contentlet = (Contentlet)versionable;
            ContentletVersionInfo info = versionableFactory
                    .getContentletVersionInfo(contentlet.getIdentifier(), contentlet.getLanguageId());

            if(info ==null || !UtilMethods.isSet(info.getIdentifier())) {
                // Not yet created
                info = versionableFactory.createContentletVersionInfo(identifier, contentlet.getLanguageId(), versionable.getInode());
            }
            else {
                final ContentletVersionInfo oldInfo = info;
                final ContentletVersionInfo newInfo = Sneaky.sneak(()-> (ContentletVersionInfo) BeanUtils.cloneBean(oldInfo)) ;

                newInfo.setWorkingInode(versionable.getInode());
                versionableFactory.saveContentletVersionInfo(newInfo, true);
            }
            
            CacheLocator.getIdentifierCache().removeContentletVersionInfoToCache(info.getIdentifier(),contentlet.getLanguageId());
        } else {

            final VersionInfo info = versionableFactory.findVersionInfoFromDb(identifier);

            if(info ==null || !UtilMethods.isSet(info.getIdentifier())) {
                // Not yet created
                versionableFactory.createVersionInfo(identifier, versionable.getInode());
            }
            else {
                info.setWorkingInode(versionable.getInode());
                versionableFactory.saveVersionInfo(info, true);
            }
        }
    }

    @CloseDBIfOpened
    @Override
    public String getLockedBy(Versionable versionable) throws DotDataException, DotStateException, DotSecurityException {

        if(!UtilMethods.isSet(versionable.getVersionId()))
            throw new DotStateException("invalid identifier");

        final Identifier identifier = APILocator.getIdentifierAPI().find(versionable.getVersionId());
        String userId;

        if(identifier.getAssetType().equals("contentlet")) {

            final Contentlet contentlet = (Contentlet)versionable;
            final ContentletVersionInfo vinfo = versionableFactory
                    .getContentletVersionInfo(contentlet.getIdentifier(),contentlet.getLanguageId());
            userId = vinfo.getLockedBy();
        }
        else {

            final VersionInfo vinfo= versionableFactory.getVersionInfo(versionable.getVersionId());
            userId = vinfo.getLockedBy();
        }

        if(userId==null)
            throw new DotStateException("asset is not locked");
        return userId;
    }

    @CloseDBIfOpened
    @Override
    public Date getLockedOn(Versionable versionable) throws DotDataException, DotStateException, DotSecurityException {

        if(!UtilMethods.isSet(versionable.getVersionId()))
            throw new DotStateException("invalid identifier");

        final Identifier identifier = APILocator.getIdentifierAPI().find(versionable.getVersionId());
        Date date;
        if(identifier.getAssetType().equals("contentlet")) {
            final Contentlet contentlet = (Contentlet)versionable;
            final ContentletVersionInfo vinfo= versionableFactory.getContentletVersionInfo(contentlet.getIdentifier(),contentlet.getLanguageId());
            date=vinfo.getLockedOn();
        } else {
            final VersionInfo vinfo= versionableFactory.getVersionInfo(identifier.getId());
            date=vinfo.getLockedOn();
        }

        if(date==null)
            throw new DotStateException("asset is not locked");
        return date;
    }

    @CloseDBIfOpened
	public  VersionInfo getVersionInfo(final String identifier) throws DotDataException, DotStateException{
		return versionableFactory.getVersionInfo(identifier);
	}

	@CloseDBIfOpened
	public ContentletVersionInfo getContentletVersionInfo(final String identifier,
                                                          final long lang) throws DotDataException, DotStateException {
	    return versionableFactory.getContentletVersionInfo(identifier, lang);
	}
	
	@Override
	@CloseDBIfOpened
	public List<ContentletVersionInfo> findContentletVersionInfos(final String identifier) throws DotDataException, DotStateException {
	  return versionableFactory.findAllContentletVersionInfos(identifier);
	}


	
	
	@WrapInTransaction
	@Override
	public void saveVersionInfo(final VersionInfo vInfo) throws DotDataException, DotStateException {
		versionableFactory.saveVersionInfo(vInfo, true);
	}

	@WrapInTransaction
	@Override
	public void saveContentletVersionInfo(final ContentletVersionInfo contentletVersionInfo) throws DotDataException, DotStateException {

		final ContentletVersionInfo contentletVersionInfoInDB = versionableFactory
                .findContentletVersionInfoInDB(contentletVersionInfo.getIdentifier(), contentletVersionInfo.getLang());

		if(contentletVersionInfoInDB == null || contentletVersionInfoInDB.getIdentifier() == null) {

			versionableFactory.saveContentletVersionInfo(contentletVersionInfo, true);
		} else {

			contentletVersionInfoInDB.setDeleted(contentletVersionInfo.isDeleted());
			contentletVersionInfoInDB.setLiveInode(contentletVersionInfo.getLiveInode());
			contentletVersionInfoInDB.setLockedBy(contentletVersionInfo.getLockedBy());
			contentletVersionInfoInDB.setLockedOn(contentletVersionInfo.getLockedOn());
			contentletVersionInfoInDB.setWorkingInode(contentletVersionInfo.getWorkingInode());
			versionableFactory.saveContentletVersionInfo(contentletVersionInfoInDB, true);
		}
	}

	@WrapInTransaction
    @Override
	public void deleteVersionInfo(final String identifier)throws DotDataException {
		versionableFactory.deleteVersionInfo(identifier);
	}

	@WrapInTransaction
    @Override
	public void deleteContentletVersionInfo(final String identifier, final long lang) throws DotDataException {
	    versionableFactory.deleteContentletVersionInfo(identifier, lang);
	}

	@CloseDBIfOpened
    @Override
	public boolean hasLiveVersion(final Versionable versionable) throws DotDataException, DotStateException {

		if(versionable instanceof Contentlet) {

			final ContentletVersionInfo contentletVersionInfo = this.getContentletVersionInfo
                    (versionable.getVersionId(), ((Contentlet) versionable).getLanguageId());

			return (contentletVersionInfo != null && UtilMethods.isSet(contentletVersionInfo.getLiveInode()));
		} else {

			final VersionInfo versionInfo = this.getVersionInfo(versionable.getVersionId());
			return (versionInfo != null && UtilMethods.isSet(versionInfo.getLiveInode()));
		}
	}

	@Override
	public void removeContentletVersionInfoFromCache(final String identifier, final long lang) {
		CacheLocator.getIdentifierCache().removeContentletVersionInfoToCache(identifier, lang);
	}

	@Override
	public void removeVersionInfoFromCache(final String identifier) {
		CacheLocator.getIdentifierCache().removeVersionInfoFromCache(identifier);
	}

}
