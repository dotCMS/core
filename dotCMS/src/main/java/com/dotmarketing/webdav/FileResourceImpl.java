package com.dotmarketing.webdav;

import com.dotcms.repackage.com.bradmcevoy.http.Auth;
import com.dotcms.repackage.com.bradmcevoy.http.CollectionResource;
import com.dotcms.repackage.com.bradmcevoy.http.FileItem;
import com.dotcms.repackage.com.bradmcevoy.http.FileResource;
import com.dotcms.repackage.com.bradmcevoy.http.HttpManager;
import com.dotcms.repackage.com.bradmcevoy.http.LockInfo;
import com.dotcms.repackage.com.bradmcevoy.http.LockResult;
import com.dotcms.repackage.com.bradmcevoy.http.LockTimeout;
import com.dotcms.repackage.com.bradmcevoy.http.LockToken;
import com.dotcms.repackage.com.bradmcevoy.http.LockableResource;
import com.dotcms.repackage.com.bradmcevoy.http.Range;
import com.dotcms.repackage.com.bradmcevoy.http.Request;
import com.dotcms.repackage.com.bradmcevoy.http.Request.Method;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Permissionable;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.fileassets.business.FileAsset;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;
import com.dotmarketing.portlets.fileassets.business.IFileAsset;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.Date;
import java.util.Map;

public class FileResourceImpl implements FileResource, LockableResource {

	private static final FileAssetAPI fileAssetAPI = APILocator.getFileAssetAPI();
	private DotWebdavHelper dotDavHelper;
	private IFileAsset file = new FileAsset();
	String path;
	private boolean isAutoPub = false;
	private PermissionAPI perAPI;

	public FileResourceImpl(IFileAsset file, String path) {
		perAPI = APILocator.getPermissionAPI();
		dotDavHelper = new DotWebdavHelper();
		this.isAutoPub = dotDavHelper.isAutoPub(path);
		this.path = path;
		this.file = file;

	}

	public void copyTo(CollectionResource collRes, String name) throws DotRuntimeException {
	    User user=(User)HttpManager.request().getAuthorization().getTag();
		if(collRes instanceof TempFolderResourceImpl){
			TempFolderResourceImpl tr = (TempFolderResourceImpl)collRes;
			try {
				dotDavHelper.copyFileToTemp(file, tr.getFolder());
			} catch (IOException e) {
				Logger.error(this, e.getMessage(), e);
				return;
			}
		}else if(collRes instanceof BasicFolderResourceImpl){
			BasicFolderResourceImpl fr = (BasicFolderResourceImpl)collRes;
			try {
				String p = fr.getPath();
				if(!p.endsWith("/"))
					p = p + "/";
				dotDavHelper.copyResource(this.getPath(), p+name, user, isAutoPub);
			} catch (Exception e) {
				Logger.error(this, e.getMessage(), e);
				throw new DotRuntimeException(e.getMessage(), e);
			}
		}
	}

	public Object authenticate(String username, String password) {
		try {
			return dotDavHelper.authorizePrincipal(username, password);
		} catch (Exception e) {
			Logger.error(this, e.getMessage(), e);
			return null;
		}
	}

	public boolean authorise(Request req, Method method, Auth auth) {
		try {

			if(auth == null)
				return false;
			else {
			    User user=(User)auth.getTag();
			    if(method.isWrite && isAutoPub){
    				return perAPI.doesUserHavePermission((Permissionable)file, PermissionAPI.PERMISSION_PUBLISH, user, false);
    			}else if(method.isWrite && !isAutoPub){
    				return perAPI.doesUserHavePermission((Permissionable)file, PermissionAPI.PERMISSION_EDIT, user, false);
    			}else if(!method.isWrite){
    				return perAPI.doesUserHavePermission((Permissionable)file, PermissionAPI.PERMISSION_READ, user, false);
    			}
			}
		} catch (DotDataException e) {
			Logger.error(FileResourceImpl.class, e.getMessage(), e);
			throw new DotRuntimeException(e.getMessage(), e);
		}
		return false;
	}

	public String checkRedirect(Request req) {
		return null;
	}

	public Long getContentLength() {
		java.io.File workingFile;
		try {
			workingFile = ((Contentlet)file).getBinary(FileAssetAPI.BINARY_FIELD);

		} catch (IOException e) {
			Logger.error(this, e.getMessage(), e);
			return new Long(0);
		}
		return workingFile.length();
	}

	public String getContentType(String accepts) {
		return fileAssetAPI.getMimeType(file.getFileName());
	}

	public Date getModifiedDate() {
		return file.getModDate();
	}

	public String getName() {
		return UtilMethods.escapeHTMLSpecialChars(file.getFileName());
	}

	public String getRealm() {
		return null;
	}

	public String getUniqueId() {
		return  file.getInode();
	}

	public void delete() throws DotRuntimeException {
	    User user=(User)HttpManager.request().getAuthorization().getTag();
	    try {
			dotDavHelper.removeObject(path, user);
		} catch (Exception e) {
			Logger.error(this, e.getMessage(), e);
			throw new DotRuntimeException(e.getMessage(), e);
		}
	}

	public Long getMaxAgeSeconds() {
		return new Long(0);
	}

	public void sendContent(OutputStream out, Range arg1, Map<String, String> arg2, String arg3) throws IOException {
		java.io.File f;
		try {
			f = ((Contentlet)file).getBinary(FileAssetAPI.BINARY_FIELD);
		} catch (IOException e) {
			Logger.error(this, e.getMessage(), e);
			return;
		}
		InputStream fis = Files.newInputStream(f.toPath());
        BufferedInputStream bin = new BufferedInputStream(fis);
        final byte[] buffer = new byte[ 1024 ];
        int n = 0;
        while( -1 != (n = bin.read( buffer )) ) {
            out.write( buffer, 0, n );
        }
	}

	public void moveTo(CollectionResource collRes, String name) throws RuntimeException {
	    User user=(User)HttpManager.request().getAuthorization().getTag();
		if(!name.contains(".")){
			// so far there are no indications of problems moving files without extension
			// the validation remains for possible problems to help out debugging.
			Logger.info(this, "You are moving a file without extesion");
		}
		if(collRes instanceof TempFolderResourceImpl){
			TempFolderResourceImpl tr = (TempFolderResourceImpl)collRes;
			try {
				dotDavHelper.copyFileToTemp(file, tr.getFolder());
				Logger.debug(this, "Webdav clients wants to move a file from dotcms to a tempory storage but we don't allow this in fear that the tranaction may break and delete a file from dotcms");
			} catch (IOException e) {
				Logger.error(this, e.getMessage(), e);
				return;
			}
		}
		else if(collRes instanceof BasicFolderResourceImpl) {
			try {
			    String p = ((BasicFolderResourceImpl)collRes).getPath();
				if(!p.endsWith("/"))
					p = p + "/";
				try {
					dotDavHelper.move(this.getPath(), p + name, user, isAutoPub);
				} catch (DotDataException e) {
					Logger.error(FileResourceImpl.class,e.getMessage(),e);
					throw new DotRuntimeException(e.getMessage(), e);
				}
			} catch (IOException e) {
				Logger.error(this, e.getMessage(), e);
			}
		}
	}

	public String processForm(Map<String, String> parameters,	Map<String, FileItem> files) {
		return null;
	}

	public Date getCreateDate() {
		return file.getModDate();
	}

	public int compareTo(Object o) {
		// TODO Auto-generated method stub
		return 0;
	}

	public IFileAsset getFile() {
		return file;
	}

	public void setFile(FileAsset file) {
		this.file = file;
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

}
