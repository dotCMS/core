package com.dotmarketing.portlets.links.business;

import java.util.Date;
import java.util.List;
import java.util.Map;

import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.WebAsset;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.BaseWebAssetAPI;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.factories.InodeFactory;
import com.dotmarketing.portlets.contentlet.business.DotContentletStateException;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.links.factories.LinkFactory;
import com.dotmarketing.portlets.links.model.Link;
import com.dotmarketing.portlets.templates.business.TemplateFactory;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.model.User;

public class MenuLinkAPIImpl extends BaseWebAssetAPI implements MenuLinkAPI {
	
	static PermissionAPI permissionAPI = APILocator.getPermissionAPI();
	static MenuLinkFactory menuLinkFactory = FactoryLocator.getMenuLinkFactory();
	
	public Link copy(Link sourceLink, Folder destination, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
		if (!permissionAPI.doesUserHavePermission(sourceLink, PermissionAPI.PERMISSION_READ, user, respectFrontendRoles)) {
			throw new DotSecurityException("You don't have permission to read the source file.");
		}
			
		if (!permissionAPI.doesUserHavePermission(destination, PermissionAPI.PERMISSION_WRITE, user, respectFrontendRoles)) {
			throw new DotSecurityException("You don't have permission to wirte in the destination folder.");
		}
		
		Link newLink = new Link();

        newLink.copy(sourceLink);
        
        // translating internal link if internal and different host than the target folder
        if(sourceLink.getLinkType().equals(Link.LinkType.INTERNAL.toString()) &&
                !APILocator.getIdentifierAPI().find(sourceLink).getHostId().equals(destination.getHostId())) {
            
            Host destHost=APILocator.getHostAPI().find(destination.getHostId(),user,false);
            if(sourceLink.getUrl()!=null && sourceLink.getUrl().contains("/")) {
                String assetPath=sourceLink.getUrl().substring(sourceLink.getUrl().indexOf('/'));
                newLink.setUrl(destHost.getHostname()+assetPath);
            }
            
            // using source internal link ident get URI on source host. Link to same asset in dest host 
            Identifier ident=APILocator.getIdentifierAPI().find(sourceLink.getInternalLinkIdentifier());
            if(ident!=null && UtilMethods.isSet(ident.getId())) {
                Identifier newTargetIdent=APILocator.getIdentifierAPI().find(destHost, ident.getURI());
                if(newTargetIdent!=null && UtilMethods.isSet(newTargetIdent.getId())) {
                    String newLinkIdent=newTargetIdent.getId();
                    newLink.setInternalLinkIdentifier(newLinkIdent);
                }
            }
        }
        

        newLink.setFriendlyName(sourceLink.getFriendlyName());
        newLink.setTitle(sourceLink.getTitle());
        newLink.setShowOnMenu(sourceLink.isShowOnMenu());
        newLink.setProtocal(sourceLink.getProtocal());
        newLink.setLinkCode(sourceLink.getLinkCode());
        newLink.setLinkType(sourceLink.getLinkType());
        //persists the webasset
        save(newLink, destination, user, respectFrontendRoles);


        if(sourceLink.isLive())
            APILocator.getVersionableAPI().setLive(newLink);
        
		//Copy permissions
        permissionAPI.copyPermissions(sourceLink, newLink);
		
		return newLink;
	}
	
	public void save(Link menuLink, Folder destination, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException {

		boolean isNew = false;
		if(!InodeUtils.isSet(menuLink.getIdentifier()))
			isNew = true;
		
		if (!isNew  && !permissionAPI.doesUserHavePermission(menuLink, PermissionAPI.PERMISSION_WRITE, user, respectFrontendRoles)) {
			throw new DotSecurityException("You don't have permission to write the link.");
		}
		
		if(!permissionAPI.doesUserHavePermission(destination, PermissionAPI.PERMISSION_WRITE, user, respectFrontendRoles)) {
			throw new DotSecurityException("You don't have permission to write on the given folder.");
		}
			
		menuLink.setModUser(user.getUserId());
		
		menuLinkFactory.save(menuLink, destination);
		
	}
	
	public void save(Link menuLink, User user, boolean respectFrontendRoles) throws DotDataException,
		DotSecurityException {
		if(!InodeUtils.isSet(menuLink.getIdentifier()))
			throw new DotContentletStateException("This method is meant to be called with already save links");
		Folder parentFolder = (Folder) APILocator.getFolderAPI().findParentFolder(menuLink, user, false);
		save(menuLink, parentFolder, user, respectFrontendRoles);
	}	
	
	protected void save(WebAsset webAsset) throws DotDataException, DotStateException, DotSecurityException {
		menuLinkFactory.save((Link) webAsset);
	}
	
	public boolean delete(Link menuLink, User user, boolean respectFrontendRoles) throws DotSecurityException, Exception {
		if(permissionAPI.doesUserHavePermission(menuLink, PermissionAPI.PERMISSION_WRITE, user, respectFrontendRoles)) {
			return deleteAsset(menuLink);
		} else {
			throw new DotSecurityException(WebKeys.USER_PERMISSIONS_EXCEPTION);
		}
	}

	@SuppressWarnings("unchecked")
	public List<Link> findFolderMenuLinks(Folder sourceFolder) throws DotStateException, DotDataException, DotSecurityException {
		return APILocator.getFolderAPI().getWorkingLinks(sourceFolder,APILocator.getUserAPI().getSystemUser(),false);
	}

	public Link findWorkingLinkById(String id, User user, boolean respectFrontendRoles) throws DotSecurityException, DotDataException {
		Link link = (Link) InodeFactory.getInodeOfClassByCondition(Link.class, "identifier = '" + id + "'");
		if(link == null || !UtilMethods.isSet(link))
			return null;
		if(permissionAPI.doesUserHavePermission(link, PermissionAPI.PERMISSION_READ, user, respectFrontendRoles)) {
			return link;
		} else {
			throw new DotSecurityException(WebKeys.USER_PERMISSIONS_EXCEPTION);
		}
	}

	public List<Link> findLinks(User user, boolean includeArchived,
			Map<String, Object> params, String hostId, String inode, String identifier, String parent,
			int offset, int limit, String orderBy) throws DotSecurityException,
			DotDataException {
		return menuLinkFactory.findLinks(user, includeArchived, params, hostId, inode, identifier, parent, offset, limit, orderBy);
	}

    @Override
    public int deleteOldVersions(Date assetsOlderThan) throws DotDataException, DotHibernateException {
        return deleteOldVersions(assetsOlderThan,"links");
    }
    
    
    @Override
    public Link find(String inode, User user, boolean respectFrontEndRoles) throws DotDataException, DotSecurityException{
    	
    	Link link = menuLinkFactory.load(inode);
    	
    	if(!APILocator.getPermissionAPI().doesUserHavePermission(link, PermissionAPI.PERMISSION_READ, user,respectFrontEndRoles)){
    		throw new DotSecurityException("User "+ user + " does not have permission to link " + inode);
    	}
    	return link;
    }

    @Override
    public boolean move(Link link, Host host, User user, boolean respectFrontEndRoles) throws DotSecurityException, DotDataException {
        if(!permissionAPI.doesUserHavePermission(link, PermissionAPI.PERMISSION_WRITE, user, respectFrontEndRoles)) {
            throw new DotSecurityException("User "+user+" does not have permissions to move link "+link.getInode());
        }
        if(!permissionAPI.doesUserHavePermissions(host, "PARENT:"+PermissionAPI.PERMISSION_CAN_ADD_CHILDREN+", LINKS:"+PermissionAPI.PERMISSION_WRITE, user, respectFrontEndRoles)) {
            throw new DotSecurityException("User "+user+" does not have permissions to move a link to host "+host.getHostname());
        }
        return LinkFactory.moveLink(link, host);
    }

    @Override
    public boolean move(Link link, Folder folder, User user, boolean respectFrontEndRoles) throws DotSecurityException, DotDataException {
        if(!permissionAPI.doesUserHavePermission(link, PermissionAPI.PERMISSION_WRITE, user, respectFrontEndRoles)) {
            throw new DotSecurityException("User "+user+" does not have permissions to move link "+link.getInode());
        }
        if(!permissionAPI.doesUserHavePermissions(folder, "PARENT:"+PermissionAPI.PERMISSION_CAN_ADD_CHILDREN+", LINKS:"+PermissionAPI.PERMISSION_WRITE, user, respectFrontEndRoles)) {
            throw new DotSecurityException("User "+user+" does not have permissions to move a link to folder "+folder.getPath());
        }
        return LinkFactory.moveLink(link, folder);
    }

}