package com.dotmarketing.webdav;

import com.dotcms.repackage.com.bradmcevoy.http.Auth;
import com.dotcms.repackage.com.bradmcevoy.http.CollectionResource;
import com.dotcms.repackage.com.bradmcevoy.http.FileItem;
import com.dotcms.repackage.com.bradmcevoy.http.FileResource;
import com.dotcms.repackage.com.bradmcevoy.http.LockInfo;
import com.dotcms.repackage.com.bradmcevoy.http.LockResult;
import com.dotcms.repackage.com.bradmcevoy.http.LockTimeout;
import com.dotcms.repackage.com.bradmcevoy.http.LockToken;
import com.dotcms.repackage.com.bradmcevoy.http.LockableResource;
import com.dotcms.repackage.com.bradmcevoy.http.Range;
import com.dotcms.repackage.com.bradmcevoy.http.Request;
import com.dotcms.repackage.com.bradmcevoy.http.Resource;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.portlets.fileassets.business.FileAsset;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import com.liferay.util.FileUtil;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.Date;
import java.util.Map;

/**
 * 
 * @author Jason Tesser
 */
public class LanguageFileResourceImpl implements FileResource, LockableResource {

	private DotWebdavHelper dotDavHelper;
	private final File file;

	public LanguageFileResourceImpl(String path) {
		if(path.contains("/")){
			String[] splitPath = path.split("/");
			path = "";
			for(int i = 0; i<splitPath.length ; i++)
				path = path + splitPath[i] + File.separator;

		}
		if(path.contains("null")){
			path = path.replace("null", "");
		}
		if(path.startsWith(File.separator)){
			try
			{
				path = path.replaceFirst(File.separator, "");
			}
			catch(Exception ex)
			{
				//This code above throw an exception on Widnwso 
				path = path.substring(File.separator.length());
			}
		}
		dotDavHelper = new DotWebdavHelper();
		file = new File(FileUtil.getRealPath("/assets/messages") + File.separator + path);

	}


	public String getUniqueId() {
		return file.hashCode() + "";
	}


	public int compareTo(Object o) {
		if( o instanceof Resource ) {
			Resource res = (Resource)o;
			return this.getName().compareTo(res.getName());
		} else {
			return -1;
		}
	}


	public void sendContent(OutputStream out, Range range, Map<String, String> params, String arg3) throws IOException {
		try (InputStream fis = Files.newInputStream(file.toPath())){
			BufferedInputStream bin = new BufferedInputStream(fis);
			final byte[] buffer = new byte[ 1024 ];
			int n = 0;
			while( -1 != (n = bin.read( buffer )) ) {
				out.write( buffer, 0, n );
			}
		}
	}


	public String getName() {
		return file.getName();
	}


	public Object authenticate(String username, String password) {
		try {
			User user =  dotDavHelper.authorizePrincipal(username, password);
			  
			//Get the Administrator Role to validate if the user has permission
			Role cmsAdminRole = com.dotmarketing.business.APILocator.getRoleAPI().loadCMSAdminRole();
			if(com.dotmarketing.business.APILocator.getRoleAPI().doesUserHaveRole(user,cmsAdminRole.getId())){
				return user;
			}else{
				return null;
			}
		} catch (Exception e) {
			Logger.error(this, e.getMessage(), e);
			return null;
		}
	}


	public boolean authorise(Request request, Request.Method method, Auth auth) {
		if(auth == null)
			return false;
		else{
			return true;
		}
	}


	public String getRealm() {
		return null;
	}


	public Date getModifiedDate() {        
		Date dt = new Date(file.lastModified());
//		log.debug("static resource modified: " + dt);
		return dt;
	}


	public Long getContentLength() {
		return file.length();
	}


	public String getContentType(String accepts) {
		String mimeType = Config.CONTEXT.getMimeType(file.getName());
		if (!UtilMethods.isSet(mimeType)) {
			mimeType = FileAsset.UNKNOWN_MIME_TYPE;
		}
		
		return mimeType;
	}



	public String checkRedirect(Request request) {
		return null;
	}


	public Long getMaxAgeSeconds() {
		return (long)60;
	}


	public void copyTo(CollectionResource collRes, String name) {
		throw new RuntimeException("Not allowed to implement copy");
	}


	public void delete() {
		file.delete();
		WebAPILocator.getLanguageWebAPI().clearCache();
	}


	public void moveTo(CollectionResource collRes, String name) {
		throw new RuntimeException("Not allowed to implement move");
	}


	public String processForm(Map<String, String> parameters, Map<String, FileItem> files) {
		// TODO Auto-generated method stub
		return null;
	}


	public Date getCreateDate() {
		// TODO Auto-generated method stub
		return null;
	}


	public File getFile() {
		return file;
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
