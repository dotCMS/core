package com.dotmarketing.business;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.VersionInfo;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ContentletLangVersionInfo;
import com.dotmarketing.portlets.contentlet.model.ContentletVersionInfo;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;

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

    public boolean isDeleted(String identifier) throws DotDataException, DotStateException, DotSecurityException {
        if(!UtilMethods.isSet(identifier))
        	return false;
        Identifier ident = APILocator.getIdentifierAPI().find(identifier);
        if(ident.getAssetType().equals("contentlet")) {
            ContentletVersionInfo info = vfac.getContentletVersionInfo(identifier);
            if(!UtilMethods.isSet(info.getIdentifier()))
                throw new DotStateException("No version info. Call setWorking first");
            return info.isDeleted();
        }
        else {
            VersionInfo info = vfac.getVersionInfo(identifier);
            if(!UtilMethods.isSet(info.getIdentifier()))
                throw new DotStateException("No version info. Call setWorking first");
            return info.isDeleted();
        }
    }

    public boolean isLive(Versionable versionable) throws DotDataException, DotStateException, DotSecurityException {
        if(!UtilMethods.isSet(versionable) || !UtilMethods.isSet(versionable.getVersionId()))
        	return false;
        Identifier ident = APILocator.getIdentifierAPI().find(versionable);
        String liveInode;
        if(ident.getAssetType().equals("contentlet")) {
            Contentlet cont = (Contentlet)versionable;
            ContentletLangVersionInfo info = vfac.getContentletLangVersionInfo(cont.getIdentifier(), cont.getLanguageId());
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

    public boolean isLocked(String identifier) throws DotDataException, DotStateException, DotSecurityException {
        if(!UtilMethods.isSet(identifier))
            return false;
        Identifier ident = APILocator.getIdentifierAPI().find(identifier);
        if(ident.getAssetType().equals("contentlet")) {
            ContentletVersionInfo info = vfac.getContentletVersionInfo(identifier);
            if(!UtilMethods.isSet(info.getIdentifier()))
                throw new DotStateException("No version info. Call setWorking first");
            return info.isLocked();
        }   
        else {
            VersionInfo info = vfac.getVersionInfo(identifier);
            if(!UtilMethods.isSet(info.getIdentifier()))
                throw new DotStateException("No version info. Call setWorking first");
            return info.isLocked();
        }
    }

    public boolean isWorking(Versionable versionable) throws DotDataException, DotStateException, DotSecurityException {
        if(!UtilMethods.isSet(versionable) || !UtilMethods.isSet(versionable.getVersionId()))
        	return false;
        Identifier ident = APILocator.getIdentifierAPI().find(versionable);
        String workingInode;
        if(ident.getAssetType().equals("contentlet")) {
            Contentlet cont = (Contentlet)versionable;
            ContentletLangVersionInfo info=vfac.getContentletLangVersionInfo(cont.getIdentifier(), cont.getLanguageId());
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
        vfac.saveVersionInfo(ver);
    }
    
    public void removeLive(String identifier, long lang) throws DotDataException, DotStateException, DotSecurityException {
        if(!UtilMethods.isSet(identifier))
            throw new DotStateException("invalid identifier");
        ContentletLangVersionInfo ver = vfac.getContentletLangVersionInfo(identifier, lang);
        if(ver ==null || !UtilMethods.isSet(ver.getIdentifier()))
            throw new DotStateException("No version info. Call setWorking first");
        ver.setLiveInode(null);
        vfac.saveContentletLangVersionInfo(ver);
    }

    public void setDeleted(String identifier, boolean deleted) throws DotDataException, DotStateException, DotSecurityException {
        if(!UtilMethods.isSet(identifier))
            throw new DotStateException("invalid identifier");
        Identifier ident = APILocator.getIdentifierAPI().find(identifier);
        if(ident.getAssetType().equals("contentlet")) {
            ContentletVersionInfo info = vfac.getContentletVersionInfo(identifier);
            if(!UtilMethods.isSet(info.getIdentifier()))
                throw new DotStateException("No version info. Call setWorking first");
            info.setDeleted(deleted);
            vfac.saveContentletVersionInfo(info);
        }
        else {
            VersionInfo info = vfac.getVersionInfo(identifier);
            if(!UtilMethods.isSet(info.getIdentifier()))
                throw new DotStateException("No version info. Call setWorking first");
            info.setDeleted(deleted);
            vfac.saveVersionInfo(info);
        }
    }

    public void setLive(Versionable versionable) throws DotDataException, DotStateException, DotSecurityException {
        if(!UtilMethods.isSet(versionable) || !UtilMethods.isSet(versionable.getVersionId()))
            throw new DotStateException("invalid identifier");
        Identifier ident = APILocator.getIdentifierAPI().find(versionable);
        if(ident.getAssetType().equals("contentlet")) {
            Contentlet cont = (Contentlet)versionable;
            ContentletLangVersionInfo info = vfac.getContentletLangVersionInfo(cont.getIdentifier(), cont.getLanguageId());
            if(info ==null ||!UtilMethods.isSet(info.getIdentifier()))
                throw new DotStateException("No version info. Call setWorking first");
            info.setLiveInode(versionable.getInode());
            vfac.saveContentletLangVersionInfo(info);
        }
        else {
            VersionInfo info = vfac.getVersionInfo(versionable.getVersionId());
            if(!UtilMethods.isSet(info.getIdentifier()))
                throw new DotStateException("No version info. Call setWorking first");
            info.setLiveInode(versionable.getInode());
            vfac.saveVersionInfo(info);
        }
    }
    public void setLocked(String identifier, boolean locked, User user) throws DotDataException, DotStateException, DotSecurityException {
        if(!UtilMethods.isSet(identifier))
            throw new DotStateException("invalid identifier");
        Identifier ident = APILocator.getIdentifierAPI().find(identifier);
        if(ident.getAssetType().equals("contentlet")) {
            ContentletVersionInfo info = vfac.getContentletVersionInfo(identifier);
            if(!UtilMethods.isSet(info.getIdentifier()))
                throw new DotStateException("No version info. Call setWorking first");
            if(locked)
                info.setLocked(user.getUserId());
            else
                info.unLock();
            vfac.saveContentletVersionInfo(info);
        }
        else {
            VersionInfo info = vfac.getVersionInfo(identifier);
            if(!UtilMethods.isSet(info.getIdentifier()))
                throw new DotStateException("No version info. Call setWorking first");
            if(locked)
                info.setLocked(user.getUserId());
            else
                info.unLock();
            vfac.saveVersionInfo(info);
        }
    }

    public void setWorking(Versionable versionable) throws DotDataException, DotStateException, DotSecurityException {
        if(!UtilMethods.isSet(versionable) || !UtilMethods.isSet(versionable.getVersionId()))
            throw new DotStateException("invalid identifier");
        Identifier ident = APILocator.getIdentifierAPI().find(versionable);
        if(ident.getAssetType().equals("contentlet")) {
            Contentlet cont = (Contentlet)versionable;
            ContentletLangVersionInfo info = vfac.getContentletLangVersionInfo(cont.getIdentifier(), cont.getLanguageId());
            if(info ==null || !UtilMethods.isSet(info.getIdentifier())) {
                // Not yet created
                if(!UtilMethods.isSet(vfac.getContentletVersionInfo(ident.getId()).getIdentifier()))
                    vfac.createContentletVersionInfo(ident); // also ContentletVersionInfo should be created
                info = vfac.createContentletLangVersionInfo(ident, versionable.getInode(), cont.getLanguageId());
            }
            else {
                info.setWorkingInode(versionable.getInode());
                vfac.saveContentletLangVersionInfo(info);
            }
        }
        else {
            VersionInfo info = vfac.getVersionInfo(versionable.getVersionId());
            if(info ==null || !UtilMethods.isSet(info.getIdentifier())) {
                // Not yet created
                vfac.createVersionInfo(ident, versionable.getInode());
            }
            else {
                info.setWorkingInode(versionable.getInode());
                vfac.saveVersionInfo(info);
            }
        }
    }

    public String getLockedBy(String identifier) throws DotDataException, DotStateException, DotSecurityException {
        if(!UtilMethods.isSet(identifier))
            throw new DotStateException("invalid identifier");
        Identifier ident = APILocator.getIdentifierAPI().find(identifier);
        String userId;
        if(ident.getAssetType().equals("contentlet")) {
            ContentletVersionInfo vinfo=vfac.getContentletVersionInfo(identifier);
            userId=vinfo.getLockedBy();
        }
        else {
            VersionInfo vinfo=vfac.getVersionInfo(identifier);
            userId=vinfo.getLockedBy();
        }
        if(userId==null) 
            throw new DotStateException("asset is not locked");
        return userId;
    }

    public Date getLockedOn(String identifier) throws DotDataException, DotStateException, DotSecurityException {
        if(!UtilMethods.isSet(identifier))
            throw new DotStateException("invalid identifier");
        Identifier ident = APILocator.getIdentifierAPI().find(identifier);
        Date date;
        if(ident.getAssetType().equals("contentlet")) {
            ContentletVersionInfo vinfo=vfac.getContentletVersionInfo(identifier);
            date=vinfo.getLockedOn();
        }
        else {
            VersionInfo vinfo=vfac.getVersionInfo(identifier);
            date=vinfo.getLockedOn();
        }
        if(date==null) 
            throw new DotStateException("asset is not locked");
        return date;
    }
    

	public ContentletLangVersionInfo getContentletLangVersionInfo(String identifier, long lang) throws DotDataException,
	DotStateException{
		return vfac.getContentletLangVersionInfo(identifier, lang);
	}

	public  VersionInfo getVersionInfo(String identifier) throws DotDataException, DotStateException{
		return vfac.getVersionInfo(identifier);
	}

	public void deleteVersionInfo(String identifier)throws DotDataException {
		vfac.deleteVersionInfo(identifier);
	}
	
	public void deleteContentletVersionInfo(String identifier, long lang) throws DotDataException {
	    vfac.deleteContentletVersionInfo(identifier, lang);
	}
}
