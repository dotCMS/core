
package com.dotmarketing.business;

import static com.dotcms.util.CollectionsUtils.list;

import com.dotcms.api.system.event.message.MessageSeverity;
import com.dotcms.api.system.event.message.MessageType;
import com.dotcms.api.system.event.message.SystemMessageEventUtil;
import com.dotcms.api.system.event.message.builder.SystemMessageBuilder;
import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.business.WrapInTransaction;
import com.dotcms.cdi.CDIUtils;
import com.dotcms.concurrent.Debouncer;
import com.dotcms.contenttype.business.uniquefields.UniqueFieldValidationStrategyResolver;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.variant.model.Variant;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.VersionInfo;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DoesNotExistException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ContentletVersionInfo;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.util.ContentPublishDateUtil;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.google.common.collect.ImmutableList;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.User;
import com.rainerhahnekamp.sneakythrow.Sneaky;

import io.vavr.control.Try;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.commons.beanutils.BeanUtils;

public class VersionableAPIImpl implements VersionableAPI {

	private final VersionableFactory versionableFactory;
	private final PermissionAPI permissionAPI;
    final UniqueFieldValidationStrategyResolver uniqueFieldValidationStrategyResolver;

	public VersionableAPIImpl() {
        this.uniqueFieldValidationStrategyResolver = CDIUtils.getBeanThrows(UniqueFieldValidationStrategyResolver.class);
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
		List<Permissionable> pass  = new ArrayList<>();

		for (Versionable v : versions) {
			if (v instanceof Permissionable) {
				pass.add((Permissionable) v);
			}
		}

		pass = permissionAPI.filterCollection(pass, PermissionAPI.PERMISSION_READ, respectAnonPermissions, user);
		versions = new ArrayList<>();

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
    public boolean isDeleted(final Versionable versionable) throws DotDataException, DotStateException {

        if(!UtilMethods.isSet(versionable) || !InodeUtils.isSet(versionable.getVersionId()))
        	return false;

        final Identifier identifier = APILocator.getIdentifierAPI().find(versionable.getVersionId());

        if(!UtilMethods.isSet(identifier.getId()))
        	return false;

        if(identifier.getAssetType().equals("contentlet")) {
            final Contentlet contentlet = (Contentlet)versionable;
            final Optional<ContentletVersionInfo> cinfo = versionableFactory.
                    getContentletVersionInfo(contentlet.getIdentifier(), contentlet.getLanguageId(), contentlet.getVariantId());

            return cinfo.isPresent() && cinfo.get().isDeleted();
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
            final Optional<ContentletVersionInfo> info = contentlet.isHost() ?
                    getContentletVersionInfo(identifier, contentlet) :
                    getContentletVersionInfo(contentlet.getIdentifier(), contentlet.getLanguageId(),
                            contentlet.getVariantId());
            if (info.isEmpty())
                return false;
            liveInode=info.get().getLiveInode();
        } else {
            final VersionInfo info = versionableFactory.getVersionInfo(versionable.getVersionId());
            if(!UtilMethods.isSet(info.getIdentifier()))
                throw new DotStateException("No version info. Call setWorking first");

            liveInode = info.getLiveInode();
        }
        return liveInode!=null && liveInode.equals(versionable.getInode());
    }

    private Optional<ContentletVersionInfo> getContentletVersionInfo(final Identifier identifier,
            final Contentlet contentlet) throws DotDataException {
        final Optional<ContentletVersionInfo> info;

        if (contentlet.isHost()){
            info = versionableFactory.findAnyContentletVersionInfo(contentlet.getIdentifier());
        } else{
            info = versionableFactory.getContentletVersionInfo(
                    contentlet.getIdentifier(),
                    contentlet.getLanguageId(),
                    contentlet.getVariantId());
        }

        if(info.isEmpty())
            throw new DotStateException("No version info. Call setWorking first "+identifier.getId());
        return info;
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
            final Optional<ContentletVersionInfo> info = getContentletVersionInfo(identifier, contentlet);

            return info.get().isLocked();
        } else {
            final VersionInfo info = versionableFactory.getVersionInfo(versionable.getVersionId());
            if(!UtilMethods.isSet(info.getIdentifier()))
                throw new DotStateException("No version info. Call setWorking first");

            return info.isLocked();
        }
    }

    @CloseDBIfOpened
    @Override
    public boolean hasWorkingVersionInAnyOtherLanguage(Versionable versionable, final long versionableLanguageId) throws DotDataException, DotStateException, DotSecurityException {

        if(!UtilMethods.isSet(versionable) || !InodeUtils.isSet(versionable.getVersionId())) {
            return false;
        }

        final Identifier identifier = APILocator.getIdentifierAPI().find(versionable);
        if(identifier==null || !UtilMethods.isSet(identifier.getId()) || !UtilMethods.isSet(identifier.getAssetType())) {
            return false;
        }

        // only contents are multi language
        return "contentlet".equals(identifier.getAssetType())?
                !this.versionableFactory.getWorkingVersionsExcludingLanguage(identifier.getId(), versionableLanguageId).isEmpty():false;
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
            final Optional<ContentletVersionInfo> info = contentlet.isHost() ?
                    getContentletVersionInfo(identifier, contentlet) :
                    getContentletVersionInfo(contentlet.getIdentifier(), contentlet.getLanguageId(),
                            contentlet.getVariantId());

            if (info.isEmpty()) {
                return false;
            }
            
            workingInode = info.get().getWorkingInode();
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

        try {
            ContentletVersionInfo copy =  versionInfo instanceof ContentletVersionInfo  ?
                    (ContentletVersionInfo) BeanUtils.cloneBean(versionInfo) : null;

            versionInfo.setLiveInode(null);
            versionableFactory.saveVersionInfo(versionInfo, true);

            if (UtilMethods.isSet(copy)) {
                uniqueFieldValidationStrategyResolver.get().afterUnpublish(copy);
            }
        } catch (IllegalAccessException | InstantiationException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
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
        final Optional<ContentletVersionInfo> contentletVersionInfo =
                this.versionableFactory.getContentletVersionInfo( identifierId, lang, contentlet.getVariantId() );

        if ( contentletVersionInfo.isEmpty() ) {
            throw new DotStateException( "No version info. Call setLive first" );
        }

        if ( !UtilMethods.isSet( contentletVersionInfo.get().getLiveInode() ) ) {
            throw new DotStateException( "No live version Contentlet. Call setLive first" );
        }

        final Contentlet liveContentlet = APILocator.getContentletAPI()
                .find( contentletVersionInfo.get().getLiveInode(), APILocator.getUserAPI().getSystemUser(), false );

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
        final ContentletVersionInfo newInfo = Sneaky.sneak(()-> (ContentletVersionInfo) BeanUtils.cloneBean(contentletVersionInfo.get())) ;
        newInfo.setLiveInode( null );
        newInfo.setPublishDate(null);
        versionableFactory.saveContentletVersionInfo( newInfo, true );

        uniqueFieldValidationStrategyResolver.get().afterUnpublish(contentletVersionInfo.get());
    }

    @WrapInTransaction
    @Override
    public void setDeleted(final Versionable versionable, final boolean deleted) throws DotDataException, DotStateException, DotSecurityException {

        if(!UtilMethods.isSet(versionable.getVersionId()))
            throw new DotStateException("invalid identifier");

        final Identifier identifier = APILocator.getIdentifierAPI().find(versionable.getVersionId());

        if(identifier.getAssetType().equals("contentlet")) {
            final Contentlet contentlet      = (Contentlet)versionable;
            final Optional<ContentletVersionInfo> info = getContentletVersionInfo(identifier, contentlet);
            
            final ContentletVersionInfo newInfo = Sneaky.sneak(()-> (ContentletVersionInfo) BeanUtils.cloneBean(info.get())) ;
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
            final Optional<ContentletVersionInfo> info = getContentletVersionInfo(
                    contentlet.getIdentifier(), contentlet.getLanguageId(), contentlet.getVariantId());

            //Get the structure for this contentlet
            final ContentType contentType = contentlet.getContentType();
            if (ContentPublishDateUtil.notifyIfFuturePublishDate(contentType, identifier, versionable.getModUser())) {
                return;
            }
            if ( UtilMethods.isSet( contentType.expireDateVar() ) ) {//Verify if the structure have a Expire Date Field set
                if ( UtilMethods.isSet( identifier.getSysExpireDate() ) && identifier.getSysExpireDate().before( new Date() ) ) {
                    throw new ExpiredContentletPublishStateException( contentlet );
                }
            }
            final ContentletVersionInfo newInfo = Sneaky.sneak(()-> (ContentletVersionInfo) BeanUtils.cloneBean(info.get())) ;
            newInfo.setLiveInode(versionable.getInode());
            newInfo.setPublishDate(new Date());
            versionableFactory.saveContentletVersionInfo( newInfo, true );

            uniqueFieldValidationStrategyResolver.get().afterPublish(versionable.getInode());
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
            final Optional<ContentletVersionInfo> info = getContentletVersionInfo(identifier,
                    contentlet);
            final ContentletVersionInfo newInfo = Sneaky.sneak(()-> (ContentletVersionInfo) BeanUtils.cloneBean(info.get())) ;

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
    public void setWorking(final Versionable versionable) throws DotDataException, DotStateException {

        if(!UtilMethods.isSet(versionable) || !UtilMethods.isSet(versionable.getVersionId()))
            throw new DotStateException("invalid identifier");

        final Identifier identifier = APILocator.getIdentifierAPI().find(versionable);
        if(identifier.getAssetType().equals("contentlet")) {

            final Contentlet contentlet = (Contentlet)versionable;
            Optional<ContentletVersionInfo> info = versionableFactory
                    .getContentletVersionInfo(contentlet.getIdentifier(), contentlet.getLanguageId(),
                            contentlet.getVariantId());

            if(info.isEmpty()) {
                // Not yet created
                info = Optional.of(versionableFactory.createContentletVersionInfo(identifier,
                        contentlet.getLanguageId(), versionable.getInode(), contentlet.getVariantId()));
            }
            else {
                final ContentletVersionInfo oldInfo = info.get();
                final ContentletVersionInfo newInfo = Sneaky.sneak(()-> (ContentletVersionInfo) BeanUtils.cloneBean(oldInfo)) ;

                newInfo.setWorkingInode(versionable.getInode());
                versionableFactory.saveContentletVersionInfo(newInfo, true);
            }
            
            CacheLocator.getIdentifierCache().removeContentletVersionInfoToCache(info.get().getIdentifier(),contentlet.getLanguageId(), contentlet.getVariantId());
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
    public Optional<String> getLockedBy(Versionable versionable) throws DotDataException {

        if(!UtilMethods.isSet(versionable.getVersionId()))
            throw new DotStateException("invalid identifier");

        final Identifier identifier = APILocator.getIdentifierAPI().find(versionable.getVersionId());
        Optional<String> userId;

        if(identifier.getAssetType().equals("contentlet")) {

            final Contentlet contentlet = (Contentlet)versionable;
            final Optional<ContentletVersionInfo> vinfo = Try.of(()->versionableFactory
                    .getContentletVersionInfo(contentlet.getIdentifier()
                            ,contentlet.getLanguageId())).getOrElse(Optional.empty());

            if(vinfo.isPresent() && UtilMethods.isSet(vinfo.get().getLockedBy())) {
                userId = Optional.of(vinfo.get().getLockedBy());
            } else {
                userId = Optional.empty();
            }
        }
        else {
            final VersionInfo vinfo= versionableFactory.getVersionInfo(versionable.getVersionId());

            if(vinfo!=null && UtilMethods.isSet(vinfo.getLockedBy())) {
                userId = Optional.of(vinfo.getLockedBy());
            } else {
                userId = Optional.empty();
            }
        }
        return userId;
    }

    @CloseDBIfOpened
    @Override
    public Optional<Date> getLockedOn(Versionable versionable) throws DotDataException, DotStateException, DotSecurityException {

        if(!UtilMethods.isSet(versionable.getVersionId()))
            throw new DotStateException("invalid identifier");

        final Identifier identifier = APILocator.getIdentifierAPI().find(versionable.getVersionId());
        Optional<Date> date;

        if(identifier.getAssetType().equals("contentlet")) {
            final Contentlet contentlet = (Contentlet)versionable;
            final Optional<ContentletVersionInfo> vinfo= versionableFactory
                    .getContentletVersionInfo(contentlet.getIdentifier(),contentlet.getLanguageId());

            if(vinfo.isPresent() && UtilMethods.isSet(vinfo.get().getLockedOn())) {
                date = Optional.of(vinfo.get().getLockedOn());
            } else {
                date = Optional.empty();
            }

        } else {
            final VersionInfo vinfo= versionableFactory.getVersionInfo(identifier.getId());

            if(vinfo!=null && UtilMethods.isSet(vinfo.getLockedOn())) {
                date = Optional.of(vinfo.getLockedOn());
            } else {
                date = Optional.empty();
            }
        }

        return date;
    }

    @CloseDBIfOpened
	public  VersionInfo getVersionInfo(final String identifier) throws DotDataException, DotStateException{
		return versionableFactory.getVersionInfo(identifier);
	}

	@CloseDBIfOpened
	public Optional<ContentletVersionInfo> getContentletVersionInfo(final String identifier,
                                                          final long lang) {
	    return Try.of(()->versionableFactory.getContentletVersionInfo(identifier, lang))
                .getOrElse(Optional.empty());
	}

    @CloseDBIfOpened
    public Optional<ContentletVersionInfo> getContentletVersionInfo(final String identifier,
            final long lang, final String variantId) {
        return Try.of(()->versionableFactory.getContentletVersionInfo(identifier, lang, variantId))
                .getOrElse(Optional.empty());
    }
	
	@Override
	@CloseDBIfOpened
	public List<ContentletVersionInfo> findContentletVersionInfos(final String identifier) throws DotDataException, DotStateException {

        Logger.debug(this, "Finding the contentlet version infos, for the id: " + identifier);
	    return versionableFactory.findAllContentletVersionInfos(identifier);
	}

    @Override
    @CloseDBIfOpened
    public List<ContentletVersionInfo> findContentletVersionInfos(final String identifier, final String variantName)
            throws DotDataException, DotStateException {
        return versionableFactory.findAllContentletVersionInfos(identifier, variantName);
    }
	
	@Override
    @CloseDBIfOpened
    public List<ContentletVersionInfo> findAllByVariant(final Variant variant) throws DotDataException{
        return versionableFactory.findAllByVariant(variant);
    }

	@WrapInTransaction
	@Override
	public void saveVersionInfo(final VersionInfo vInfo) throws DotDataException, DotStateException {
		versionableFactory.saveVersionInfo(vInfo, true);
	}

	@WrapInTransaction
	@Override
	public void saveContentletVersionInfo(final ContentletVersionInfo contentletVersionInfo) throws DotDataException, DotStateException {

		final Optional<ContentletVersionInfo> contentletVersionInfoInDB = versionableFactory
                .findContentletVersionInfoInDB(contentletVersionInfo.getIdentifier(),
                        contentletVersionInfo.getLang(), contentletVersionInfo.getVariant());

		if(contentletVersionInfoInDB.isEmpty()) {
            if (UtilMethods.isSet(contentletVersionInfo.getLiveInode())
                    && !UtilMethods.isSet(contentletVersionInfo.getPublishDate())) {
                contentletVersionInfo.setPublishDate(
                        UtilMethods.isSet(contentletVersionInfo.getVersionTs()) ?
                                contentletVersionInfo.getVersionTs() :
                                new Date());
            }
			versionableFactory.saveContentletVersionInfo(contentletVersionInfo, true);
		} else {
		    final ContentletVersionInfo info = contentletVersionInfoInDB.get();
            info.setDeleted(contentletVersionInfo.isDeleted());
            info.setLiveInode(contentletVersionInfo.getLiveInode());
            info.setLockedBy(contentletVersionInfo.getLockedBy());
            info.setLockedOn(contentletVersionInfo.getLockedOn());
            info.setWorkingInode(contentletVersionInfo.getWorkingInode());
            info.setPublishDate(contentletVersionInfo.getPublishDate());
			versionableFactory.saveContentletVersionInfo(info, true);
		}
	}

	@WrapInTransaction
    @Override
	public void deleteVersionInfo(final String identifier)throws DotDataException {
		versionableFactory.deleteVersionInfo(identifier);
	}

	@WrapInTransaction
    @Override
	public void deleteContentletVersionInfoByLanguage(final Contentlet contentlet) throws DotDataException {
	    versionableFactory.deleteContentletVersionInfo(contentlet.getIdentifier(), contentlet.getLanguageId());

        APILocator.getLocalSystemEventsAPI().notify(new DeleteContentletVersionInfoEvent(contentlet));
    }

    @WrapInTransaction
    @Override
    public void deleteContentletVersionInfoByVariant(final Contentlet contentlet) throws DotDataException {
        versionableFactory.deleteContentletVersionInfo(contentlet.getIdentifier(), contentlet.getVariantId());

        APILocator.getLocalSystemEventsAPI().notify(new DeleteContentletVersionInfoEvent(contentlet, true));
    }

	@CloseDBIfOpened
    @Override
	public boolean hasLiveVersion(final Versionable versionable) throws DotDataException, DotStateException {

		if(versionable instanceof Contentlet) {

			final Optional<ContentletVersionInfo> contentletVersionInfo = this.getContentletVersionInfo
                    (versionable.getVersionId(), ((Contentlet) versionable).getLanguageId(), ((Contentlet) versionable).getVariantId());

			return contentletVersionInfo.isPresent()
                    && UtilMethods.isSet(contentletVersionInfo.get().getLiveInode());
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
