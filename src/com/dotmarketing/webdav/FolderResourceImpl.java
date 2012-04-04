/**
 * 
 */
package com.dotmarketing.webdav;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.bradmcevoy.http.Auth;
import com.bradmcevoy.http.CollectionResource;
import com.bradmcevoy.http.FolderResource;
import com.bradmcevoy.http.LockInfo;
import com.bradmcevoy.http.LockResult;
import com.bradmcevoy.http.LockTimeout;
import com.bradmcevoy.http.LockToken;
import com.bradmcevoy.http.LockableResource;
import com.bradmcevoy.http.LockingCollectionResource;
import com.bradmcevoy.http.MakeCollectionableResource;
import com.bradmcevoy.http.Range;
import com.bradmcevoy.http.Request;
import com.bradmcevoy.http.Resource;
import com.bradmcevoy.http.Request.Method;
import com.bradmcevoy.http.exceptions.NotAuthorizedException;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.fileassets.business.IFileAsset;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.util.CompanyUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;

/**
 * @author Jason Tesser
 *
 */
public class FolderResourceImpl implements LockableResource, LockingCollectionResource, FolderResource , MakeCollectionableResource {

	private DotWebdavHelper dotDavHelper;
	private Folder folder;
	private String path;
	private User user;
	private boolean isAutoPub = false;
	private PermissionAPI perAPI;
	private HostAPI hostAPI;
	
	public FolderResourceImpl(Folder folder, String path) {
		this.perAPI = APILocator.getPermissionAPI();
		this.dotDavHelper = new DotWebdavHelper();
		this.isAutoPub = dotDavHelper.isAutoPub(path);
		this.path = path;
		this.folder = folder;
		this.hostAPI = APILocator.getHostAPI();
	}
	
	/* (non-Javadoc)
	 * @see com.bradmcevoy.http.MakeCollectionableResource#createCollection(java.lang.String)
	 */
	public CollectionResource createCollection(String newName) throws DotRuntimeException {
		String folderPath ="";
		if(dotDavHelper.isTempResource(newName)){
			Host host;
			try {
				host = hostAPI.find(folder.getHostId(), APILocator.getUserAPI().getSystemUser(), false);
				folderPath = APILocator.getIdentifierAPI().find(folder).getPath();
			} catch (DotDataException e) {
				Logger.error(DotWebdavHelper.class, e.getMessage(), e);
				throw new DotRuntimeException(e.getMessage(), e);
			} catch (DotSecurityException e) {
				Logger.error(DotWebdavHelper.class, e.getMessage(), e);
				throw new DotRuntimeException(e.getMessage(), e);
			}			
			
			dotDavHelper.createTempFolder(File.separator + host.getHostname() + folderPath + File.separator + newName);
			File f = new File(File.separator + host.getHostname() + folderPath);
			TempFolderResourceImpl tr = new TempFolderResourceImpl(f.getPath(),f ,isAutoPub);
			return tr;
		}
		if(!path.endsWith("/")){
			path = path + "/";
		}
		try {
			Folder f = dotDavHelper.createFolder(path + newName, user);
			FolderResourceImpl fr = new FolderResourceImpl(f, path + newName + "/");
			return fr;
		} catch (Exception e) {
			Logger.error(this, e.getMessage(), e);
			throw new DotRuntimeException(e.getMessage(), e);
		}
	}

	/* (non-Javadoc)
	 * @see com.bradmcevoy.http.CollectionResource#child(java.lang.String)
	 */
	public Resource child(String childName) {
		List<Resource> children;
		try {
			children = dotDavHelper.getChildrenOfFolder(folder, isAutoPub);
		} catch (IOException e) {
			Logger.error(FolderResourceImpl.class, e.getMessage(), e);
			throw new DotRuntimeException(e.getMessage(), e);
		}
		for (Resource resource : children) {
			if(resource instanceof FolderResourceImpl){
				String name = ((FolderResourceImpl)resource).getFolder().getName();
				if(name.equalsIgnoreCase(childName)){
					return resource;
				}
			}else if(resource instanceof TempFolderResourceImpl){
				String name = ((TempFolderResourceImpl)resource).getFolder().getName();
				if(name.equalsIgnoreCase(childName)){
					return resource;
				}
			}else if(resource instanceof TempFileResourceImpl){
				String name = ((TempFileResourceImpl)resource).getFile().getName();
				if(name.equalsIgnoreCase(childName)){
					return resource;
				}
			}else{
				String name = ((FileResourceImpl)resource).getFile().getFileName();
				if(name.equalsIgnoreCase(childName)){
					return resource;
				}
			}
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see com.bradmcevoy.http.CollectionResource#getChildren()
	 */
	public List<? extends Resource> getChildren() {
		List<Resource> children;
		try {
			children = dotDavHelper.getChildrenOfFolder(folder, isAutoPub);
		} catch (IOException e) {
			Logger.error(FolderResourceImpl.class, e.getMessage(), e);
			throw new DotRuntimeException(e.getMessage(), e);
		}
		return children;
	}

	/* (non-Javadoc)
	 * @see com.bradmcevoy.http.Resource#authenticate(java.lang.String, java.lang.String)
	 */
	public Object authenticate(String username, String password) {
		try {
			this.user =  dotDavHelper.authorizePrincipal(username, password);
			return user;
		} catch (Exception e) {
			Logger.error(this, e.getMessage(), e);
			return null;
		}
	}

	/* (non-Javadoc)
	 * @see com.bradmcevoy.http.Resource#authorise(com.bradmcevoy.http.Request, com.bradmcevoy.http.Request.Method, com.bradmcevoy.http.Auth)
	 */
	public boolean authorise(Request req, Method method, Auth auth) {
		try {
			
			if(auth == null)
				return false;
			else if(method.isWrite){
				return perAPI.doesUserHavePermission(folder, PermissionAPI.PERMISSION_CAN_ADD_CHILDREN, user, false);
			}else if(!method.isWrite){
				return perAPI.doesUserHavePermission(folder, PermissionAPI.PERMISSION_READ, user, false);
			}

		} catch (DotDataException e) {
			Logger.error(FolderResourceImpl.class, e.getMessage(),
					e);
			throw new DotRuntimeException(e.getMessage(), e);
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see com.bradmcevoy.http.Resource#checkRedirect(com.bradmcevoy.http.Request)
	 */
	public String checkRedirect(Request req) {
		return null;
	}

	/* (non-Javadoc)
	 * @see com.bradmcevoy.http.Resource#getContentLength()
	 */
	public Long getContentLength() {
		return (long)0;
	}

	/* (non-Javadoc)
	 * @see com.bradmcevoy.http.Resource#getContentType(java.lang.String)
	 */
	public String getContentType(String arg0) {
		return "folder";
	}

	/* (non-Javadoc)
	 * @see com.bradmcevoy.http.Resource#getModifiedDate()
	 */
	public Date getModifiedDate() {
		return folder.getiDate();
	}

	/* (non-Javadoc)
	 * @see com.bradmcevoy.http.Resource#getRealm()
	 */
	public String getRealm() {
		return CompanyUtils.getDefaultCompany().getName();
	}

	/* (non-Javadoc)
	 * @see com.bradmcevoy.http.Resource#getUniqueId()
	 */
	public String getUniqueId() {
		return folder.getInode();
	}

	/* (non-Javadoc)
	 * @see com.bradmcevoy.http.PutableResource#createNew(java.lang.String, java.io.InputStream, java.lang.Long, java.lang.String)
	 */
	public Resource createNew(String newName, InputStream in, Long length, String contentType) throws IOException, DotRuntimeException {
		if(!path.endsWith("/")){
			path = path + "/";
		}
		if(!dotDavHelper.isTempResource(newName)){
			try {
				dotDavHelper.createResource(path + newName, isAutoPub, user);
			} catch (DotDataException e) {
				Logger.error(FolderResourceImpl.class,e.getMessage(),e);
				throw new DotRuntimeException(e.getMessage(), e);
			}
			IFileAsset f = null;
			try {
				dotDavHelper.setResourceContent(path + newName, in, contentType, null, java.util.Calendar.getInstance().getTime(), user, isAutoPub);
				f = dotDavHelper.loadFile(path + newName);
			} catch (Exception e) {
				Logger.error(this, e.getMessage(), e);
			}
			FileResourceImpl fr = new FileResourceImpl(f, f.getFileName());
			return fr;
		}
		String folderPath = "";
		Host host;
		try {
			host = hostAPI.find(folder.getHostId(), APILocator.getUserAPI().getSystemUser(), false);
			folderPath = APILocator.getIdentifierAPI().find(folder).getPath(); 
		} catch (DotDataException e) {
			Logger.error(DotWebdavHelper.class, e.getMessage(), e);
			throw new IOException(e.getMessage());
		} catch (DotSecurityException e) {
			Logger.error(DotWebdavHelper.class, e.getMessage(), e);
			throw new IOException(e.getMessage());
		}
		String p = folderPath;
		if(!p.endsWith("/")){
			p = p + "/";
		}
		File f = dotDavHelper.createTempFile("/" + host.getHostname() + p + newName);
		FileOutputStream fos = new FileOutputStream(f);
		byte[] buf = new byte[256];
        int read = -1;
		while ((read = in.read()) != -1) {
			fos.write(read);
		}
		TempFileResourceImpl tr = new TempFileResourceImpl(f, path + newName, isAutoPub);
		return tr;
	}

	/* (non-Javadoc)
	 * @see com.bradmcevoy.http.CopyableResource#copyTo(com.bradmcevoy.http.CollectionResource, java.lang.String)
	 */
	public void copyTo(CollectionResource collRes, String name) {
		if(collRes instanceof TempFolderResourceImpl){
			TempFolderResourceImpl tr = (TempFolderResourceImpl)collRes;
			try {
				dotDavHelper.copyFolderToTemp(folder, tr.getFolder(), name, isAutoPub);
			} catch (IOException e) {
				Logger.error(this, e.getMessage(), e);
				return;
			}
		}else if(collRes instanceof FolderResourceImpl){
			FolderResourceImpl fr = (FolderResourceImpl)collRes;
			try {
				String p = fr.getPath();
				if(!p.endsWith("/"))
					p = p + "/";
				dotDavHelper.copyFolder(this.getPath(), p+name, user, isAutoPub);
			} catch (Exception e) {
				Logger.error(this, e.getMessage(), e);
			}
		}else if(collRes instanceof HostResourceImpl){
			HostResourceImpl hr = (HostResourceImpl)collRes;
			String p = this.getPath();
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
	 * @see com.bradmcevoy.http.DeletableResource#delete()
	 */
	public void delete() throws DotRuntimeException{
		try {
			dotDavHelper.removeObject(path, user);
		} catch (Exception e) {
			Logger.error(this, e.getMessage(), e);
			throw new DotRuntimeException(e.getMessage(), e);
		}
	}

	/* (non-Javadoc)
	 * @see com.bradmcevoy.http.GetableResource#getMaxAgeSeconds()
	 */
	public Long getMaxAgeSeconds() {
		return new Long(0);
	}

	/* (non-Javadoc)
	 * @see com.bradmcevoy.http.GetableResource#sendContent(java.io.OutputStream, com.bradmcevoy.http.Range, java.util.Map)
	 */
	public void sendContent(OutputStream arg0, Range arg1, Map<String, String> arg2, String arg3) throws IOException {
		return;
	}

	/* (non-Javadoc)
	 * @see com.bradmcevoy.http.MoveableResource#moveTo(com.bradmcevoy.http.CollectionResource, java.lang.String)
	 */
	public void moveTo(CollectionResource collRes, String name) throws DotRuntimeException{
		if(collRes instanceof TempFolderResourceImpl){
			Logger.debug(this, "Webdav clients wants to move a file from dotcms to a tempory storage but we don't allow this in fear that the tranaction may break and delete a file from dotcms");
			TempFolderResourceImpl tr = (TempFolderResourceImpl)collRes;
			try {
				dotDavHelper.copyFolderToTemp(folder, tr.getFolder(), name, isAutoPub);
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
					host = hostAPI.find(fr.getFolder().getHostId(), APILocator.getUserAPI().getSystemUser(), false);
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
	 * @see com.bradmcevoy.http.PropFindableResource#getCreateDate()
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

	public String getPath() {
		return path;
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


	public LockToken createAndLock(String arg0, LockTimeout arg1, LockInfo arg2)
			throws NotAuthorizedException {
		// TODO Auto-generated method stub
		return null;
	}

	
}
