
package com.dotmarketing.business;

import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.VersionInfo;
import com.dotmarketing.cache.StructureCache;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ContentletVersionInfo;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class VersionableAPIImpl implements VersionableAPI {

	private VersionableFactory vfac;
	private PermissionAPI papi;

	public VersionableAPIImpl() {
		vfac = FactoryLocator.getVersionableFactory();
		papi = APILocator.getPermissionAPI();
	}

	public Versionable findWorkingVersion(Versionable inode, User user, boolean respectAnonPermissions) throws DotDataException,
			DotStateException, DotSecurityException {
		return findWorkingVersion(inode.getVersionId(), user, respectAnonPermissions);
	}

	public Versionable findWorkingVersion(Identifier id, User user, boolean respectAnonPermissions) throws DotDataException,
			DotStateException, DotSecurityException {
		return findWorkingVersion(id.getId(), user, respectAnonPermissions);
	}

	public Versionable findWorkingVersion(String id, User user, boolean respectAnonPermissions) throws DotDataException, DotStateException,
			DotSecurityException {

		Versionable asset = vfac.findWorkingVersion(id);
		if (!papi.doesUserHavePermission((Permissionable) asset, PermissionAPI.PERMISSION_READ, user, respectAnonPermissions)) {
			throw new DotSecurityException("User " + user + " does not have permission to read " + id);
		}

		return asset;
	}

	public Versionable findLiveVersion(Versionable inode, User user, boolean respectAnonPermissions) throws DotDataException,
			DotStateException, DotSecurityException {
		return findLiveVersion(inode.getVersionId(), user, respectAnonPermissions);
	}

	public Versionable findLiveVersion(Identifier id, User user, boolean respectAnonPermissions) throws DotDataException,
			DotStateException, DotSecurityException {
		return findLiveVersion(id.getId(), user, respectAnonPermissions);
	}

	public Versionable findLiveVersion(String id, User user, boolean respectAnonPermissions) throws DotDataException, DotStateException,
			DotSecurityException {
		Versionable asset = vfac.findLiveVersion(id);
		if (asset!=null && !papi.doesUserHavePermission((Permissionable) asset, PermissionAPI.PERMISSION_READ, user, respectAnonPermissions)) {
			throw new DotSecurityException("User " + user + " does not have permission to read " + id);
		}

		return asset;
	}

	public Versionable findDeletedVersion(Versionable inode, User user, boolean respectAnonPermissions) throws DotDataException,
			DotStateException, DotSecurityException {
		return findDeletedVersion(inode.getVersionId(), user, respectAnonPermissions);
	}

	public Versionable findDeletedVersion(Identifier id, User user, boolean respectAnonPermissions) throws DotDataException,
			DotStateException, DotSecurityException {
		return findDeletedVersion(id.getId(), user, respectAnonPermissions);
	}

	public Versionable findDeletedVersion(String id, User user, boolean respectAnonPermissions) throws DotDataException, DotStateException,
			DotSecurityException {

		Versionable asset = vfac.findDeletedVersion(id);
		if (!papi.doesUserHavePermission((Permissionable) asset, PermissionAPI.PERMISSION_READ, user, respectAnonPermissions)) {
			throw new DotSecurityException("User " + user + " does not have permission to read " + id);
		}

		return asset;
	}

	public List<Versionable> findAllVersions(Versionable inode) throws DotDataException, DotStateException, DotSecurityException {
		if (inode == null) {
			throw new DotStateException("Inode is null");
		}
		return findAllVersions(inode.getVersionId(), APILocator.getUserAPI().getSystemUser(), false);
	}

	public List<Versionable> findAllVersions(Identifier id) throws DotDataException, DotStateException, DotSecurityException {
		if (id == null) {
			throw new DotStateException("Inode is null");
		}
		return findAllVersions(id.getId(), APILocator.getUserAPI().getSystemUser(), false);
	}

	public List<Versionable> findAllVersions(String id) throws DotDataException, DotStateException, DotSecurityException {
		return findAllVersions(id, APILocator.getUserAPI().getSystemUser(), false);
	}

	public List<Versionable> findAllVersions(Versionable id, User user, boolean respectAnonPermissions) throws DotDataException,
			DotStateException, DotSecurityException {
		if (id == null) {
			throw new DotStateException("Versionable is null");
		}
		return findAllVersions(id.getVersionId(), user, respectAnonPermissions);
	}

	public List<Versionable> findAllVersions(Identifier id, User user, boolean respectAnonPermissions) throws DotDataException,
			DotStateException, DotSecurityException {
		if (id == null) {
			throw new DotStateException("Inode is null");
		}
		return findAllVersions(id.getId(), user, respectAnonPermissions);
	}

	public List<Versionable> findAllVersions(String id, User user, boolean respectAnonPermissions) throws DotDataException,
			DotStateException, DotSecurityException {
		List<Versionable> vass = vfac.findAllVersions(id);

		List<Permissionable> pass = new ArrayList<Permissionable>();
		for (Versionable v : vass) {
			if (v instanceof Permissionable) {
				pass.add((Permissionable) v);
			}
		}

		pass = papi.filterCollection(pass, PermissionAPI.PERMISSION_READ, respectAnonPermissions, user);
		vass = new ArrayList<Versionable>();
		for (Permissionable p : pass) {
			if (p instanceof Versionable) {
				vass.add((Versionable) p);
			}
		}
		return vass;

	}

    public boolean isDeleted(Versionable ver) throws DotDataException, DotStateException, DotSecurityException {
        if(!UtilMethods.isSet(ver) || !InodeUtils.isSet(ver.getVersionId()))
        	return false;
        Identifier ident = APILocator.getIdentifierAPI().find(ver.getVersionId());

        if(!UtilMethods.isSet(ident.getId()))
        	return false;

        if(ident.getAssetType().equals("contentlet")) {
            Contentlet cont=(Contentlet)ver;
            ContentletVersionInfo cinfo=vfac.getContentletVersionInfo(cont.getIdentifier(), cont.getLanguageId());
            return cinfo.isDeleted();
        }
        else {
            VersionInfo info = vfac.getVersionInfo(ver.getVersionId());
            if(!UtilMethods.isSet(info.getIdentifier()))
                throw new DotStateException("No version info. Call setWorking first");
            return info.isDeleted();
        }
    }

    public boolean isLive(Versionable versionable) throws DotDataException, DotStateException, DotSecurityException {
        if(!UtilMethods.isSet(versionable) || !InodeUtils.isSet(versionable.getVersionId()))
        	return false;
        Identifier ident = APILocator.getIdentifierAPI().find(versionable);
        if(ident==null || !UtilMethods.isSet(ident.getId()) || !UtilMethods.isSet(ident.getAssetType()))
            return false;
        String liveInode;
        if(ident.getAssetType().equals("contentlet")) {
            Contentlet cont = (Contentlet)versionable;
            ContentletVersionInfo info = vfac.getContentletVersionInfo(cont.getIdentifier(), cont.getLanguageId());
            if(info ==null || !UtilMethods.isSet(info.getIdentifier()))
                throw new DotStateException("No version info. Call setWorking first "+ident.getId());
            liveInode=info.getLiveInode();
        }
        else {
            VersionInfo info = vfac.getVersionInfo(versionable.getVersionId());
            if(!UtilMethods.isSet(info.getIdentifier()))
                throw new DotStateException("No version info. Call setWorking first");
            liveInode=info.getLiveInode();
        }
        return liveInode!=null && liveInode.equals(versionable.getInode());
    }

    public boolean isLocked(Versionable ver) throws DotDataException, DotStateException, DotSecurityException {
        if(!UtilMethods.isSet(ver) || !InodeUtils.isSet(ver.getVersionId()))
            return false;
        Identifier ident = APILocator.getIdentifierAPI().find(ver.getVersionId());
        if(ident.getAssetType().equals("contentlet")) {
            Contentlet cont=(Contentlet)ver;
            ContentletVersionInfo info = vfac.getContentletVersionInfo(cont.getIdentifier(),cont.getLanguageId());
            if(!UtilMethods.isSet(info.getIdentifier()))
                throw new DotStateException("No version info. Call setWorking first");
            return info.isLocked();
        }
        else {
            VersionInfo info = vfac.getVersionInfo(ver.getVersionId());
            if(!UtilMethods.isSet(info.getIdentifier()))
                throw new DotStateException("No version info. Call setWorking first");
            return info.isLocked();
        }
    }

    public boolean isWorking(Versionable versionable) throws DotDataException, DotStateException, DotSecurityException {
        if(!UtilMethods.isSet(versionable) || !InodeUtils.isSet(versionable.getVersionId()))
        	return false;
        Identifier ident = APILocator.getIdentifierAPI().find(versionable);
        if(ident==null || !UtilMethods.isSet(ident.getId()) || !UtilMethods.isSet(ident.getAssetType()))
            return false;
        String workingInode;
        if(ident.getAssetType().equals("contentlet")) {
            Contentlet cont = (Contentlet)versionable;
            ContentletVersionInfo info=vfac.getContentletVersionInfo(cont.getIdentifier(), cont.getLanguageId());
            if(info ==null || !UtilMethods.isSet(info.getIdentifier()))
                throw new DotStateException("No version info. Call setWorking first");
            workingInode=info.getWorkingInode();
        }
        else {
            VersionInfo info=vfac.getVersionInfo(versionable.getVersionId());
            if(!UtilMethods.isSet(info.getIdentifier()))
                throw new DotStateException("No version info. Call setWorking first");
            workingInode=info.getWorkingInode();
        }
        return workingInode.equals(versionable.getInode());
    }

    public void removeLive(String identifier) throws DotDataException, DotStateException, DotSecurityException {
        if(!UtilMethods.isSet(identifier))
            throw new DotStateException("invalid identifier");
        VersionInfo ver = vfac.getVersionInfo(identifier);
        if(!UtilMethods.isSet(ver.getIdentifier()))
            throw new DotStateException("No version info. Call setWorking first");
        ver.setLiveInode(null);
        vfac.saveVersionInfo(ver, true);
    }

    public void removeLive ( String identifier, long lang ) throws DotDataException, DotStateException, DotSecurityException {

        if ( !UtilMethods.isSet( identifier ) ) {
            throw new DotStateException( "invalid identifier" );
        }
        Identifier ident = APILocator.getIdentifierAPI().find( identifier );

        ContentletVersionInfo ver = vfac.getContentletVersionInfo( identifier, lang );
        if ( ver == null || !UtilMethods.isSet( ver.getIdentifier() ) ) {
            throw new DotStateException( "No version info. Call setLive first" );
        }

        if ( !UtilMethods.isSet( ver.getLiveInode() ) ) {
            throw new DotStateException( "No live version Contentlet. Call setLive first" );
        }

        Contentlet liveContentlet = APILocator.getContentletAPI().find( ver.getLiveInode(), APILocator.getUserAPI().getSystemUser(), false );
        if ( liveContentlet == null || !UtilMethods.isSet( liveContentlet.getIdentifier() ) ) {
            throw new DotStateException( "No live version Contentlet. Call setLive first" );
        }

        //Get the structure for this contentlet
        Structure structure = StructureCache.getStructureByInode( liveContentlet.getStructureInode() );

        if ( UtilMethods.isSet( structure.getExpireDateVar() ) ) {//Verify if the structure have a Expire Date Field set
            if ( UtilMethods.isSet( ident.getSysExpireDate() ) && ident.getSysExpireDate().after( new Date() ) ) {
                throw new PublishStateException( "message.contentlet.unpublish.expired" );
            }
        }

        ver.setLiveInode( null );
        vfac.saveContentletVersionInfo( ver );
    }

    public void setDeleted(Versionable ver, boolean deleted) throws DotDataException, DotStateException, DotSecurityException {
        if(!UtilMethods.isSet(ver.getVersionId()))
            throw new DotStateException("invalid identifier");
        Identifier ident = APILocator.getIdentifierAPI().find(ver.getVersionId());
        if(ident.getAssetType().equals("contentlet")) {
            Contentlet cont=(Contentlet)ver;
            ContentletVersionInfo info = vfac.getContentletVersionInfo(cont.getIdentifier(),cont.getLanguageId());
            if(!UtilMethods.isSet(info.getIdentifier()))
                throw new DotStateException("No version info. Call setWorking first");
            info.setDeleted(deleted);
            vfac.saveContentletVersionInfo(info);
        }
        else {
            VersionInfo info = vfac.getVersionInfo(ver.getVersionId());
            if(!UtilMethods.isSet(info.getIdentifier()))
                throw new DotStateException("No version info. Call setWorking first");
            info.setDeleted(deleted);
            vfac.saveVersionInfo(info, true);
        }
    }

    public void setLive ( Versionable versionable ) throws DotDataException, DotStateException, DotSecurityException {

        if ( !UtilMethods.isSet( versionable ) || !UtilMethods.isSet( versionable.getVersionId() ) ) {
            throw new DotStateException( "invalid identifier" );
        }

        Identifier ident = APILocator.getIdentifierAPI().find( versionable );
        if ( ident.getAssetType().equals( "contentlet" ) ) {

            Contentlet cont = (Contentlet) versionable;
            ContentletVersionInfo info = vfac.getContentletVersionInfo( cont.getIdentifier(), cont.getLanguageId() );
            if ( info == null || !UtilMethods.isSet( info.getIdentifier() ) ) {
                throw new DotStateException( "No version info. Call setWorking first" );
            }

            //Get the structure for this contentlet
            Structure structure = StructureCache.getStructureByInode( cont.getStructureInode() );

            if ( UtilMethods.isSet( structure.getPublishDateVar() ) ) {//Verify if the structure have a Publish Date Field set
                if ( UtilMethods.isSet( ident.getSysPublishDate() ) && ident.getSysPublishDate().after( new Date() ) ) {
                    throw new PublishStateException( "message.contentlet.publish.future.date" );
                }
            }
            if ( UtilMethods.isSet( structure.getExpireDateVar() ) ) {//Verify if the structure have a Expire Date Field set
                if ( UtilMethods.isSet( ident.getSysExpireDate() ) && ident.getSysExpireDate().before( new Date() ) ) {
                    throw new PublishStateException( "message.contentlet.expired" );
                }
            }

            info.setLiveInode( versionable.getInode() );
            vfac.saveContentletVersionInfo( info );
        } else {
            VersionInfo info = vfac.getVersionInfo( versionable.getVersionId() );
            if ( !UtilMethods.isSet( info.getIdentifier() ) ) {
                throw new DotStateException( "No version info. Call setWorking first" );
            }
            info.setLiveInode( versionable.getInode() );
            vfac.saveVersionInfo( info, true );
        }
    }

    public void setLocked(Versionable ver, boolean locked, User user) throws DotDataException, DotStateException, DotSecurityException {
        if(!UtilMethods.isSet(ver.getVersionId()))
            throw new DotStateException("invalid identifier");
        Identifier ident = APILocator.getIdentifierAPI().find(ver.getVersionId());
        if(ident.getAssetType().equals("contentlet")) {
            Contentlet cont=(Contentlet)ver;
            ContentletVersionInfo info = vfac.getContentletVersionInfo(cont.getIdentifier(),cont.getLanguageId());
            if(!UtilMethods.isSet(info.getIdentifier()))
                throw new DotStateException("No version info. Call setWorking first");
            if(locked)
                info.setLocked(user.getUserId());
            else
                info.unLock();
            vfac.saveContentletVersionInfo(info);
        }
        else {
            VersionInfo info = vfac.getVersionInfo(ver.getVersionId());
            if(!UtilMethods.isSet(info.getIdentifier()))
                throw new DotStateException("No version info. Call setWorking first");
            if(locked)
                info.setLocked(user.getUserId());
            else
                info.unLock();
            vfac.saveVersionInfo(info, false);
        }
    }

    public void setWorking(Versionable versionable) throws DotDataException, DotStateException, DotSecurityException {
        if(!UtilMethods.isSet(versionable) || !UtilMethods.isSet(versionable.getVersionId()))
            throw new DotStateException("invalid identifier");
        Identifier ident = APILocator.getIdentifierAPI().find(versionable);
        if(ident.getAssetType().equals("contentlet")) {
            Contentlet cont = (Contentlet)versionable;
            ContentletVersionInfo info = vfac.getContentletVersionInfo(cont.getIdentifier(), cont.getLanguageId());
            if(info ==null || !UtilMethods.isSet(info.getIdentifier())) {
                // Not yet created
                info = vfac.createContentletVersionInfo(ident, cont.getLanguageId(), versionable.getInode());
            }
            else {
                info.setWorkingInode(versionable.getInode());
                vfac.saveContentletVersionInfo(info);
            }
        }
        else {
            VersionInfo info = vfac.findVersionInfoFromDb(ident);

            if(info ==null || !UtilMethods.isSet(info.getIdentifier())) {
                // Not yet created
                vfac.createVersionInfo(ident, versionable.getInode());
            }
            else {
                info.setWorkingInode(versionable.getInode());
                vfac.saveVersionInfo(info, true);
            }
        }
    }

    public String getLockedBy(Versionable ver) throws DotDataException, DotStateException, DotSecurityException {
        if(!UtilMethods.isSet(ver.getVersionId()))
            throw new DotStateException("invalid identifier");
        Identifier ident = APILocator.getIdentifierAPI().find(ver.getVersionId());
        String userId;
        if(ident.getAssetType().equals("contentlet")) {
            Contentlet cont=(Contentlet)ver;
            ContentletVersionInfo vinfo=vfac.getContentletVersionInfo(cont.getIdentifier(),cont.getLanguageId());
            userId=vinfo.getLockedBy();
        }
        else {
            VersionInfo vinfo=vfac.getVersionInfo(ver.getVersionId());
            userId=vinfo.getLockedBy();
        }
        if(userId==null)
            throw new DotStateException("asset is not locked");
        return userId;
    }

    public Date getLockedOn(Versionable ver) throws DotDataException, DotStateException, DotSecurityException {
        if(!UtilMethods.isSet(ver.getVersionId()))
            throw new DotStateException("invalid identifier");
        Identifier ident = APILocator.getIdentifierAPI().find(ver.getVersionId());
        Date date;
        if(ident.getAssetType().equals("contentlet")) {
            Contentlet cont=(Contentlet)ver;
            ContentletVersionInfo vinfo=vfac.getContentletVersionInfo(cont.getIdentifier(),cont.getLanguageId());
            date=vinfo.getLockedOn();
        }
        else {
            VersionInfo vinfo=vfac.getVersionInfo(ident.getId());
            date=vinfo.getLockedOn();
        }
        if(date==null)
            throw new DotStateException("asset is not locked");
        return date;
    }

	public  VersionInfo getVersionInfo(String identifier) throws DotDataException, DotStateException{
		return vfac.getVersionInfo(identifier);
	}

	public ContentletVersionInfo getContentletVersionInfo(String identifier, long lang) throws DotDataException, DotStateException {
	    return vfac.getContentletVersionInfo(identifier, lang);
	}

	@Override
	public void saveVersionInfo(VersionInfo vInfo) throws DotDataException, DotStateException {
		vfac.saveVersionInfo(vInfo, true);
	}

	@Override
	public void saveContentletVersionInfo( ContentletVersionInfo cvInfo) throws DotDataException, DotStateException {
		ContentletVersionInfo info = vfac.findContentletVersionInfoInDB(cvInfo.getIdentifier(), cvInfo.getLang());
		if(info == null){
			vfac.saveContentletVersionInfo(cvInfo);
		}else{
			info.setDeleted(cvInfo.isDeleted());
			info.setLiveInode(cvInfo.getLiveInode());
			info.setLockedBy(cvInfo.getLockedBy());
			info.setLockedOn(cvInfo.getLockedOn());
			info.setWorkingInode(cvInfo.getWorkingInode());
			vfac.saveContentletVersionInfo(info);
		}
	}

	public void deleteVersionInfo(String identifier)throws DotDataException {
		vfac.deleteVersionInfo(identifier);
	}

	public void deleteContentletVersionInfo(String identifier, long lang) throws DotDataException {
	    vfac.deleteContentletVersionInfo(identifier, lang);
	}

	public boolean hasLiveVersion(Versionable ver)  throws DotDataException, DotStateException{
		if(ver instanceof Contentlet) {
			ContentletVersionInfo vi = this.getContentletVersionInfo(ver.getVersionId(), ((Contentlet) ver).getLanguageId());
			return (vi != null && UtilMethods.isSet(vi.getLiveInode()));
		}
		else{
			VersionInfo vi = this.getVersionInfo(ver.getVersionId());
			return (vi != null && UtilMethods.isSet(vi.getLiveInode()));
		}
	}

	@Override
	public void removeContentletVersionInfoFromCache(String identifier, long lang) {
		CacheLocator.getIdentifierCache().removeContentletVersionInfoToCache(identifier, lang);
	}

	@Override
	public void removeVersionInfoFromCache(String identifier) {
		CacheLocator.getIdentifierCache().removeVersionInfoFromCache(identifier);
	}

}
