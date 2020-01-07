/**
 * 
 */
package com.dotmarketing.webdav;

import com.dotcms.repackage.com.bradmcevoy.http.Auth;
import com.dotcms.repackage.com.bradmcevoy.http.CollectionResource;
import com.dotcms.repackage.com.bradmcevoy.http.FolderResource;
import com.dotcms.repackage.com.bradmcevoy.http.HttpManager;
import com.dotcms.repackage.com.bradmcevoy.http.LockInfo;
import com.dotcms.repackage.com.bradmcevoy.http.LockResult;
import com.dotcms.repackage.com.bradmcevoy.http.LockTimeout;
import com.dotcms.repackage.com.bradmcevoy.http.LockToken;
import com.dotcms.repackage.com.bradmcevoy.http.LockableResource;
import com.dotcms.repackage.com.bradmcevoy.http.LockingCollectionResource;
import com.dotcms.repackage.com.bradmcevoy.http.MakeCollectionableResource;
import com.dotcms.repackage.com.bradmcevoy.http.Request;
import com.dotcms.repackage.com.bradmcevoy.http.Request.Method;
import com.dotcms.repackage.com.bradmcevoy.http.Resource;
import com.dotcms.repackage.com.bradmcevoy.http.exceptions.BadRequestException;
import com.dotcms.repackage.com.bradmcevoy.http.exceptions.ConflictException;
import com.dotcms.repackage.com.bradmcevoy.http.exceptions.NotAuthorizedException;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.util.CompanyUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;

/**
 * @author Jason Tesser
 *
 */
public class FolderResourceImpl extends BasicFolderResourceImpl implements LockableResource, LockingCollectionResource, FolderResource , MakeCollectionableResource {

	private DotWebdavHelper dotDavHelper=new DotWebdavHelper();
	private Folder folder;
	private PermissionAPI perAPI;
	private HostAPI hostAPI;
	
	public FolderResourceImpl(Folder folder, String path) {
	    super(path);
		this.perAPI = APILocator.getPermissionAPI();
		this.folder = folder;
		this.hostAPI = APILocator.getHostAPI();
	}
	
	/* (non-Javadoc)
	 * @see com.dotcms.repackage.com.bradmcevoy.http.MakeCollectionableResource#createCollection(java.lang.String)
	 */
	public CollectionResource createCollection(String newName) throws DotRuntimeException {

	    User user=(User)HttpManager.request().getAuthorization().getTag();
		String folderPath ="";
		if(dotDavHelper.isTempResource(newName)){
			Host host;
			try {
				host = hostAPI.find(folder.getHostId(), user, false);
				folderPath = APILocator.getIdentifierAPI().find(folder).getPath();
			} catch (DotDataException e) {
				Logger.error(DotWebdavHelper.class, e.getMessage(), e);
				throw new DotRuntimeException(e.getMessage(), e);
			} catch (DotSecurityException e) {
				Logger.error(DotWebdavHelper.class, e.getMessage(), e);
				throw new DotRuntimeException(e.getMessage(), e);
			}

            final String hostFolderPath = new StringBuilder(File.separator).append(host.getHostname())
					.append(!folderPath.endsWith(File.separator)?folderPath + File.separator : folderPath).toString();

            dotDavHelper.createTempFolder(hostFolderPath + newName);
			File file = new File(File.separator + host.getHostname() + folderPath);
			TempFolderResourceImpl tempFolderResource = new TempFolderResourceImpl(file.getPath(),file ,isAutoPub);
			return tempFolderResource;
		}
		if(!path.endsWith("/")){
			path = path + "/";
		}
		try {
			Folder newfolder = dotDavHelper.createFolder(path + newName, user);
			FolderResourceImpl folderResource = new FolderResourceImpl(newfolder, path + newName + "/");
			return folderResource;
		} catch (Exception e) {
			Logger.error(this, e.getMessage(), e);
			throw new DotRuntimeException(e.getMessage(), e);
		}
	}

	/* (non-Javadoc)
	 * @see com.dotcms.repackage.com.bradmcevoy.http.CollectionResource#child(java.lang.String)
	 */
	public Resource child(String childName) {

		if(dotDavHelper.isSameTargetAndDestinationResourceOnMove(childName)){
		   //This a small hack that prevents Milton's MoveHandler from removing the destination folder when the source and destination are the same.
		   return null;
		}

	    final User user = (User)HttpManager.request().getAuthorization().getTag();
		List<Resource> children;
		try {
			children = dotDavHelper.getChildrenOfFolder(folder, user, isAutoPub, lang);
		} catch (IOException e) {
			Logger.error(FolderResourceImpl.class, e.getMessage(), e);
			throw new DotRuntimeException(e.getMessage(), e);
		}
		for (final Resource resource : children) {
			if(resource instanceof FolderResourceImpl){
				final String name = ((FolderResourceImpl)resource).getFolder().getName();
				if(name.equalsIgnoreCase(childName)){
					return resource;
				}
			}else if(resource instanceof TempFolderResourceImpl){
				final String name = ((TempFolderResourceImpl)resource).getFolder().getName();
				if(name.equalsIgnoreCase(childName)){
					return resource;
				}
			}else if(resource instanceof TempFileResourceImpl){
				final String name = ((TempFileResourceImpl)resource).getFile().getName();
				if(name.equalsIgnoreCase(childName)){
					return resource;
				}
			}else{
				final String name = ((FileResourceImpl)resource).getFile().getFileName();
				if(name.equalsIgnoreCase(childName)){
					return resource;
				}
			}
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see com.dotcms.repackage.com.bradmcevoy.http.CollectionResource#getChildren()
	 */
	public List<? extends Resource> getChildren() {
	    User user=(User)HttpManager.request().getAuthorization().getTag();
		List<Resource> children;
		try {
            children = dotDavHelper.getChildrenOfFolder( folder, user, isAutoPub, lang );
        } catch (IOException e) {
			Logger.error(FolderResourceImpl.class, e.getMessage(), e);
			throw new DotRuntimeException(e.getMessage(), e);
		}
		return children;
	}

	/* (non-Javadoc)
	 * @see com.dotcms.repackage.com.bradmcevoy.http.Resource#authenticate(java.lang.String, java.lang.String)
	 */
	public Object authenticate(String username, String password) {
		try {
			return dotDavHelper.authorizePrincipal(username, password);
		} catch (Exception e) {
			Logger.error(this, e.getMessage(), e);
			return null;
		}
	}

	/* (non-Javadoc)
	 * @see com.dotcms.repackage.com.bradmcevoy.http.Resource#authorise(com.dotcms.repackage.com.bradmcevoy.http.Request, com.dotcms.repackage.com.bradmcevoy.http.Request.Method, com.dotcms.repackage.com.bradmcevoy.http.Auth)
	 */
	public boolean authorise(Request req, Method method, Auth auth) {
		try {
			
			if(auth == null)
				return false;
			else {
			    User user=(User)auth.getTag();
			    if(method.isWrite){
    				return perAPI.doesUserHavePermission(folder, PermissionAPI.PERMISSION_CAN_ADD_CHILDREN, user, false);
    			}else if(!method.isWrite){
    				return perAPI.doesUserHavePermission(folder, PermissionAPI.PERMISSION_READ, user, false);
    			}
			}

		} catch (DotDataException e) {
			Logger.error(FolderResourceImpl.class, e.getMessage(),
					e);
			throw new DotRuntimeException(e.getMessage(), e);
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see com.dotcms.repackage.com.bradmcevoy.http.Resource#checkRedirect(com.dotcms.repackage.com.bradmcevoy.http.Request)
	 */
	public String checkRedirect(Request req) {
		return null;
	}

	/* (non-Javadoc)
	 * @see com.dotcms.repackage.com.bradmcevoy.http.Resource#getContentLength()
	 */
	public Long getContentLength() {
		return (long)0;
	}

	/* (non-Javadoc)
	 * @see com.dotcms.repackage.com.bradmcevoy.http.Resource#getContentType(java.lang.String)
	 */
	public String getContentType(String arg0) {
		return "folder";
	}

	/* (non-Javadoc)
	 * @see com.dotcms.repackage.com.bradmcevoy.http.Resource#getModifiedDate()
	 */
	public Date getModifiedDate() {
		return folder.getiDate();
	}

	/* (non-Javadoc)
	 * @see com.dotcms.repackage.com.bradmcevoy.http.Resource#getRealm()
	 */
	public String getRealm() {
		return CompanyUtils.getDefaultCompany().getName();
	}

	/* (non-Javadoc)
	 * @see com.dotcms.repackage.com.bradmcevoy.http.Resource#getUniqueId()
	 */
	public String getUniqueId() {
		return folder.getInode();
	}

	/* (non-Javadoc)
	 * @see com.dotcms.repackage.com.bradmcevoy.http.DeletableResource#delete()
	 */
	public void delete() throws DotRuntimeException{
	    User user=(User)HttpManager.request().getAuthorization().getTag();
		try {
			dotDavHelper.removeObject(path, user);
		} catch (Exception e) {
			Logger.error(this, e.getMessage(), e);
			throw new DotRuntimeException(e.getMessage(), e);
		}
	}

	/* (non-Javadoc)
	 * @see com.dotcms.repackage.com.bradmcevoy.http.GetableResource#getMaxAgeSeconds()
	 */
	public Long getMaxAgeSeconds() {
		return new Long(0);
	}
	
	@Override
    public void copyTo(CollectionResource collRes, String name) throws NotAuthorizedException, BadRequestException,ConflictException {
        User user=(User)HttpManager.request().getAuthorization().getTag();
        
        if(collRes instanceof TempFolderResourceImpl){
            TempFolderResourceImpl tr = (TempFolderResourceImpl)collRes;
            try {
                dotDavHelper.copyFolderToTemp(folder, tr.getFolder(), user, name, isAutoPub, lang);
            } catch (Exception e) {
                Logger.error(this, e.getMessage(), e);
                return;
            }
        }else if(collRes instanceof FolderResourceImpl){
            FolderResourceImpl fr = (FolderResourceImpl)collRes;
            try {
                String p = fr.getPath();
                if(!p.endsWith("/"))
                    p = p + "/";
                dotDavHelper.copyFolder(path, p+name, user, isAutoPub);
            } catch (Exception e) {
                Logger.error(this, e.getMessage(), e);
            }
        }else if(collRes instanceof HostResourceImpl){
            HostResourceImpl hr = (HostResourceImpl)collRes;
            String p = path;
            if(!p.endsWith("/"))
                p = p +"/";
            try {
                dotDavHelper.copyFolder(p, "/" + hr.getName() + "/"+name, user, isAutoPub);
            } catch (Exception e) {
                Logger.error(this, e.getMessage(), e);
            }
        }
    }

	/* (non-Javadoc)
	 * @see com.dotcms.repackage.com.bradmcevoy.http.MoveableResource#moveTo(com.dotcms.repackage.com.bradmcevoy.http.CollectionResource, java.lang.String)
	 */
	public void moveTo(CollectionResource collRes, String name) throws DotRuntimeException{
	    User user=(User)HttpManager.request().getAuthorization().getTag();
		if(collRes instanceof TempFolderResourceImpl){
			Logger.debug(this, "Webdav clients wants to move a file from dotcms to a temporary storage but we don't allow this in fear that the transaction may break and delete a file from dotcms");
			TempFolderResourceImpl tr = (TempFolderResourceImpl)collRes;
			try {
				dotDavHelper.copyFolderToTemp(folder, tr.getFolder(), user, name, isAutoPub, lang);
			} catch (IOException e) {
				Logger.error(this, e.getMessage(), e);
				return;
			}
		}else if(collRes instanceof FolderResourceImpl){
			FolderResourceImpl fr = (FolderResourceImpl)collRes;
			if(dotDavHelper.isTempResource(name)){
				Host host;
				String folderPath = "";
				try {
					host = hostAPI.find(fr.getFolder().getHostId(), user, false);
					folderPath = APILocator.getIdentifierAPI().find(fr.getFolder()).getPath();
				} catch (DotDataException e) {
					Logger.error(FolderResourceImpl.class, e.getMessage(), e);
					throw new DotRuntimeException(e.getMessage(), e);
				} catch (DotSecurityException e) {
					Logger.error(FolderResourceImpl.class, e.getMessage(), e);
					throw new DotRuntimeException(e.getMessage(), e);
				}
				dotDavHelper.createTempFolder(File.separator + host.getHostname() + folderPath + name);
				return;
			}
			try {
				String p = fr.getPath();
				if(!p.endsWith("/"))
					p = p + "/";
				dotDavHelper.move(this.getPath(), p + name, user, isAutoPub);
			} catch (Exception e) {
				Logger.error(this, e.getMessage(), e);
				throw new DotRuntimeException(e.getMessage(), e);
			}
		}else if(collRes instanceof HostResourceImpl){
			HostResourceImpl hr = (HostResourceImpl)collRes;
			if(dotDavHelper.isTempResource(name)){
				Host host = hr.getHost();
				dotDavHelper.createTempFolder(File.separator + host.getHostname());
				return;
			}
			try {
				String p = this.getPath();
				if(!p.endsWith("/"))
					p = p +"/";
				dotDavHelper.move(p, "/" + hr.getName() + "/" + name, user, isAutoPub);
			} catch (Exception e) {
				Logger.error(this, e.getMessage(), e);
				throw new DotRuntimeException(e.getMessage(), e);
			}
		}
	}

	/* (non-Javadoc)
	 * @see com.dotcms.repackage.com.bradmcevoy.http.PropFindableResource#getCreateDate()
	 */
	public Date getCreateDate() {
		return folder.getiDate();
	}

	public String getName() {
		return UtilMethods.escapeHTMLSpecialChars(folder.getName());
	}

	public int compareTo(Object o) {
		// TODO Auto-generated method stub
		return 0;
	}

	public Folder getFolder() {
		return folder;
	}

	public void setFolder(Folder folder) {
		this.folder = folder;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public LockResult lock(LockTimeout timeout, LockInfo lockInfo) {
		return dotDavHelper.lock(timeout, lockInfo, getUniqueId());
//		return dotDavHelper.lock(lockInfo, user, file.getIdentifier() + "");
	}

	public LockResult refreshLock(String token) {
		return dotDavHelper.refreshLock(getUniqueId());
//		return dotDavHelper.refreshLock(token);
	}

	public void unlock(String tokenId) {
		dotDavHelper.unlock(getUniqueId());
//		dotDavHelper.unlock(tokenId);
	}

	public LockToken getCurrentLock() {
		return dotDavHelper.getCurrentLock(getUniqueId());
	}

	public Long getMaxAgeSeconds(Auth arg0) {
		return (long)60;
	}


	public LockToken createAndLock(String name, LockTimeout timeout, LockInfo lockInfo)
			throws NotAuthorizedException {
		createCollection(name);
		return lock(timeout, lockInfo).getLockToken();
	}

}