/**
 * 
 */
package com.dotmarketing.webdav;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
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
import com.bradmcevoy.http.Range;
import com.bradmcevoy.http.Request;
import com.bradmcevoy.http.Resource;
import com.bradmcevoy.http.Request.Method;
import com.bradmcevoy.http.exceptions.NotAuthorizedException;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;
import com.liferay.util.FileUtil;

/**
 * @author jasontesser
 *
 */
public class TempFolderResourceImpl implements FolderResource, LockableResource, LockingCollectionResource{

	private DotWebdavHelper dotDavHelper;
	private File folder;
	private String path;
	private boolean isAutoPub = false;
	private User user;
	
	
	public TempFolderResourceImpl(String path, File folder, boolean isAutoPub) {
		dotDavHelper = new DotWebdavHelper();
		this.isAutoPub = isAutoPub;
		this.path = path;
		this.folder = folder;
	}
	
	/* (non-Javadoc)
	 * @see com.bradmcevoy.http.MakeCollectionableResource#createCollection(java.lang.String)
	 */
	public CollectionResource createCollection(String newName) {
		dotDavHelper.createTempFolder(folder.getPath() + File.separator + newName);
		File f = new File(folder.getPath() + File.separator + newName);
		TempFolderResourceImpl tr = new TempFolderResourceImpl(folder.getPath() + File.separator + newName,f, isAutoPub);
		return tr;
	}

	/* (non-Javadoc)
	 * @see com.bradmcevoy.http.CollectionResource#child(java.lang.String)
	 */
	public Resource child(String childName) {
		List<? extends Resource> children = getChildren();
		for (Resource resource : children) {
			if(resource instanceof TempFolderResourceImpl){
				String name = ((TempFolderResourceImpl)resource).getFolder().getName();
				if(name.equalsIgnoreCase(childName)){
					return resource;
				}
			}else{
				String name = ((TempFileResourceImpl)resource).getFile().getName();
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
		File[] children = folder.listFiles();
		List<Resource> result = new ArrayList<Resource>();
		for (File file : children) {
			if(file.isDirectory()){
				TempFolderResourceImpl tr = new TempFolderResourceImpl(file.getPath(), file, isAutoPub);
				result.add(tr);
			}else{
				TempFileResourceImpl tr = new TempFileResourceImpl(file,file.getPath(), isAutoPub);
				result.add(tr);
			}
		}
		return result;
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
		if(auth == null)
			return false;
		else{
			return true;
		}
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
		return null;
	}

	/* (non-Javadoc)
	 * @see com.bradmcevoy.http.Resource#getModifiedDate()
	 */
	public Date getModifiedDate() {
		return new Date(folder.lastModified());
	}

	/* (non-Javadoc)
	 * @see com.bradmcevoy.http.Resource#getRealm()
	 */
	public String getRealm() {
		return null;
	}

	/* (non-Javadoc)
	 * @see com.bradmcevoy.http.Resource#getUniqueId()
	 */
	public String getUniqueId() {
		return folder.hashCode() + "";
	}

	/* (non-Javadoc)
	 * @see com.bradmcevoy.http.PutableResource#createNew(java.lang.String, java.io.InputStream, java.lang.Long, java.lang.String)
	 */
	public Resource createNew(String newName, InputStream in, Long length, String contentType) throws IOException {
		File f = new File(folder.getPath() + File.separator + newName);
		if(!f.exists()){
			String p = f.getPath().substring(0,f.getPath().lastIndexOf(File.separator));
			File fe = new File(p);
			fe.mkdirs();
			f.createNewFile();
		}
		FileOutputStream fos = new FileOutputStream(f);
		byte[] buf = new byte[256];
        int read = -1;
		while ((read = in.read()) != -1) {
			fos.write(read);
		}
		TempFileResourceImpl tr = new TempFileResourceImpl(f, f.getPath(), isAutoPub);
		return tr;
	}

	/* (non-Javadoc)
	 * @see com.bradmcevoy.http.CopyableResource#copyTo(com.bradmcevoy.http.CollectionResource, java.lang.String)
	 */
	public void copyTo(CollectionResource collRes, String name) {
		if(collRes instanceof TempFolderResourceImpl){
			TempFolderResourceImpl tr = (TempFolderResourceImpl)collRes;
			try {
				String p = tr.getFolder().getPath();
				File dest = new File(tr.getFolder().getPath() + File.separator + name);
				FileUtil.copyDirectory(folder, dest);
			} catch (Exception e) {
				Logger.error(this, e.getMessage(), e);
				return;
			}
		}else if(collRes instanceof FolderResourceImpl){
			FolderResourceImpl fr = (FolderResourceImpl)collRes;
			String p = fr.getPath();
			if(!p.endsWith("/"))
				p = p + "/";
			try {
				dotDavHelper.copyTempDirToStorage(folder, p + name, user,isAutoPub);
			} catch (Exception e) {
				Logger.error(this, e.getMessage(), e);
			}
		}else if(collRes instanceof LanguageFolderResourceImpl){
			throw new RuntimeException("You cannot copy a language folder");
		}
	}

	/* (non-Javadoc)
	 * @see com.bradmcevoy.http.DeletableResource#delete()
	 */
	public void delete() {
		folder.delete();
	}

	/* (non-Javadoc)
	 * @see com.bradmcevoy.http.GetableResource#getMaxAgeSeconds()
	 */
	public Long getMaxAgeSeconds() {
		return new Long(60);
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
	public void moveTo(CollectionResource collRes, String name) {
		if(collRes instanceof TempFolderResourceImpl){
			TempFolderResourceImpl tr = (TempFolderResourceImpl)collRes;
			try {
				File dest = new File(tr.getFolder().getPath() + File.separator + name);
				FileUtil.copyDirectory(folder, dest);
				FileUtil.deltree(folder, true);
			} catch (Exception e) {
				Logger.error(this, e.getMessage(), e);
				return;
			}
		}else if(collRes instanceof FolderResourceImpl){
			FolderResourceImpl fr = (FolderResourceImpl)collRes;
			String p = fr.getPath();
			if(!p.endsWith("/"))
				p = p + "/";
			try {
				dotDavHelper.copyTempDirToStorage(folder, p + name, user, isAutoPub);
				FileUtil.deltree(folder, true);
			} catch (Exception e) {
				Logger.error(this, e.getMessage(), e);
			}
		}else if(collRes instanceof HostResourceImpl){
			HostResourceImpl hr = (HostResourceImpl)collRes;
			try {
				dotDavHelper.copyTempDirToStorage(folder, "/" + hr.getHost().getHostname() + "/"+ name, user, isAutoPub);
				FileUtil.deltree(folder, true);
			} catch (Exception e) {
				Logger.error(this, e.getMessage(), e);
			}
		}else if(collRes instanceof LanguageFolderResourceImpl){
			throw new RuntimeException("You cannot move a language folder");
		}
	}

	/* (non-Javadoc)
	 * @see com.bradmcevoy.http.PropFindableResource#getCreateDate()
	 */
	public Date getCreateDate() {
		 Date dt = new Date(folder.lastModified());
//       log.debug("static resource modified: " + dt);
       return dt;
	}

	public String getName() {
		return folder.getName();
	}

	public int compareTo(Object o) {
		// TODO Auto-generated method stub
		return 0;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public File getFolder() {
		return folder;
	}

	public void setFolder(File folder) {
		this.folder = folder;
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
