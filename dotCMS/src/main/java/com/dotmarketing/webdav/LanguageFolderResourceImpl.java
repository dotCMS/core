/**
 * 
 */
package com.dotmarketing.webdav;

import com.dotcms.repackage.com.bradmcevoy.http.Auth;
import com.dotcms.repackage.com.bradmcevoy.http.CollectionResource;
import com.dotcms.repackage.com.bradmcevoy.http.FolderResource;
import com.dotcms.repackage.com.bradmcevoy.http.LockInfo;
import com.dotcms.repackage.com.bradmcevoy.http.LockResult;
import com.dotcms.repackage.com.bradmcevoy.http.LockTimeout;
import com.dotcms.repackage.com.bradmcevoy.http.LockToken;
import com.dotcms.repackage.com.bradmcevoy.http.LockingCollectionResource;
import com.dotcms.repackage.com.bradmcevoy.http.Range;
import com.dotcms.repackage.com.bradmcevoy.http.Request;
import com.dotcms.repackage.com.bradmcevoy.http.Request.Method;
import com.dotcms.repackage.com.bradmcevoy.http.Resource;
import com.dotcms.repackage.com.bradmcevoy.http.exceptions.NotAuthorizedException;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import com.liferay.util.FileUtil;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * @author jasontesser
 *
 */
public class LanguageFolderResourceImpl implements FolderResource, LockingCollectionResource {

	private DotWebdavHelper dotDavHelper;
	private File folder;
	private String path = "";
	private boolean isLanguageRoot = false;

	/**
	 * Pass path as null or empty to specify root
	 * @param path
	 */
	public LanguageFolderResourceImpl(String path) {
		dotDavHelper = new DotWebdavHelper();
		this.path = path;
		if(!UtilMethods.isSet(path)){
			isLanguageRoot = true;
			folder = new File(FileUtil.getRealPath("/assets/messages"));
			path = "";
		}else{
			if(path.contains("/")){
				if(path.contains("/")){
					String[] splitPath = path.split("/");
					path = "";
					for(int i = 0; i<splitPath.length ; i++)
						path = path + splitPath[i] + File.separator;
				}

			}
			if(path.contains("null")){
				path = path.replace("null", "");
			}
			if(path.startsWith(File.separator)){
				try {
					path = path.replaceFirst(File.separator, "");
				} catch (Exception e) {
					//This code above throw an exception on Windows 
					path = path.substring(File.separator.length());
				}
			}
			isLanguageRoot = false;
			folder = new File(FileUtil.getRealPath("/assets/messages") + File.separator + path);
		}
	}

	/* (non-Javadoc)
	 * @see com.dotcms.repackage.com.bradmcevoy.http.MakeCollectionableResource#createCollection(java.lang.String)
	 */
	public CollectionResource createCollection(String newName) {
		if(dotDavHelper.isTempResource(newName) && isLanguageRoot){
			dotDavHelper.createTempFolder(File.separator + "system" + File.separator + "languages" + File.separator + newName);
			File f = new File(File.separator + "system" + File.separator + "languages");
			TempFolderResourceImpl tr = new TempFolderResourceImpl(f.getPath(),f ,true);
			return tr;
		}else{
			return null;
		}
	}

	/* (non-Javadoc)
	 * @see com.dotcms.repackage.com.bradmcevoy.http.CollectionResource#child(java.lang.String)
	 */
	public Resource child(String childName) {
		List<? extends Resource> children = getChildren();
		for (Resource resource : children) {
			if(resource instanceof LanguageFolderResourceImpl){
				String name = ((LanguageFolderResourceImpl)resource).getFolder().getName();
				if(name.equalsIgnoreCase(childName)){
					return resource;
				}
			}else{
				String name = ((LanguageFileResourceImpl)resource).getFile().getName();
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
		File[] children = folder.listFiles();
		List<Resource> result = new ArrayList<Resource>();
		for (File file : children) {
			if(file.getName().endsWith(".properties") || file.getName().endsWith(".native") || file.getName().equals("archived") || folder.getName().equals("archived")){
				if(file.isDirectory()){
					LanguageFolderResourceImpl tr = new LanguageFolderResourceImpl(path + File.separator + file.getName());
					result.add(tr);
				}else{
					LanguageFileResourceImpl tr = new LanguageFileResourceImpl(path + File.separator + file.getName());
					result.add(tr);
				}
			}
		}
		File tempDir = dotDavHelper.getTempDir();
		File f = new File(tempDir.getPath() + File.separator + "system" + File.separator + "languages" + path);
		File[] c = f.listFiles();
		if(c != null){
			for (File file : c) {
				String p = path;
				if(p.contains(File.separator)){
					p = path.replace(File.separator, "/");
				}
				
				if(Config.getBooleanProperty("WEBDAV_LEGACY_PATHING", false)){
					if(file.isDirectory()){
						TempFolderResourceImpl tr = new TempFolderResourceImpl("/webdav/autopub/system/languages/" + p,file,true);
						result.add(tr);
					}else{
						TempFileResourceImpl tr = new TempFileResourceImpl(file,"/webdav/autopub/system/languages/" + p,true);
						result.add(tr);
					}
				}else{
					try {
						dotDavHelper.stripMapping(path);
					} catch (IOException e) {
						Logger.error( this, "Error happened with uri: [" + path + "]", e);
					}
					String pathFolder = "/webdav/live/" + dotDavHelper.getLanguage() + "/system/languages/";
					if(file.isDirectory()){
						TempFolderResourceImpl tr = new TempFolderResourceImpl(pathFolder + p,file,true);
						result.add(tr);
					}else{
						TempFileResourceImpl tr = new TempFileResourceImpl(file,pathFolder + p,true);
						result.add(tr);
					}
				}
				
			}
		}
		return result;
	}

	/* (non-Javadoc)
	 * @see com.dotcms.repackage.com.bradmcevoy.http.Resource#authenticate(java.lang.String, java.lang.String)
	 */
	public Object authenticate(String username, String password) {
		try {
			User user =  dotDavHelper.authorizePrincipal(username, password);
			//Get the Administrator Role to validate if the user has permission  
			Role cmsAdminRole = com.dotmarketing.business.APILocator.getRoleAPI().loadCMSAdminRole();
			if(com.dotmarketing.business.APILocator.getRoleAPI().doesUserHaveRole(user, cmsAdminRole.getId())){
				return user;
			}else{
				return null;
			}
		} catch (Exception e) {
			Logger.error(this, e.getMessage(), e);
			return null;
		}
	}

	/* (non-Javadoc)
	 * @see com.dotcms.repackage.com.bradmcevoy.http.Resource#authorise(com.dotcms.repackage.com.bradmcevoy.http.Request, com.dotcms.repackage.com.bradmcevoy.http.Request.Method, com.dotcms.repackage.com.bradmcevoy.http.Auth)
	 */
	public boolean authorise(Request req, Method method, Auth auth) {
		if(auth == null)
			return false;
		else{
			return true;
		}
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
		return null;
	}

	/* (non-Javadoc)
	 * @see com.dotcms.repackage.com.bradmcevoy.http.Resource#getModifiedDate()
	 */
	public Date getModifiedDate() {
		return new Date(folder.lastModified());
	}

	/* (non-Javadoc)
	 * @see com.dotcms.repackage.com.bradmcevoy.http.Resource#getRealm()
	 */
	public String getRealm() {
		return null;
	}

	/* (non-Javadoc)
	 * @see com.dotcms.repackage.com.bradmcevoy.http.Resource#getUniqueId()
	 */
	public String getUniqueId() {
		return folder.hashCode() + "";
	}

	/* (non-Javadoc)
	 * @see com.dotcms.repackage.com.bradmcevoy.http.PutableResource#createNew(java.lang.String, java.io.InputStream, java.lang.Long, java.lang.String)
	 */
	public Resource createNew(String newName, InputStream in, Long length, String contentType) throws IOException {
		if(!isLanguageRoot){
			throw new RuntimeException("You cannot add/edit languages files here");
		}
		if(!(newName.endsWith(".properties") || newName.endsWith(".native"))){
			throw new RuntimeException("You cannot add/edit languages files that are not properties files.");
		}
		File f = new File(FileUtil.getRealPath("/assets/messages") + File.separator + newName);
		if(f.exists()){
			File folder = new File(FileUtil.getRealPath("/assets/messages") + File.separator + "archived" + File.separator + f.getName());
			folder.mkdirs();
			String date = new Date().toString();
			date = date.replace(":", "");
			FileUtil.copyFile(f, new File(folder.getPath() + File.separator + date + f.getName()));
		}

		if(!f.exists()){
			f.createNewFile();
		}
		try (OutputStream fos = Files.newOutputStream(f.toPath());){
			byte[] buf = new byte[256];
			int read = -1;
			while ((read = in.read()) != -1) {
				fos.write(read);
			}
		}

		WebAPILocator.getLanguageWebAPI().clearCache();
		LanguageFileResourceImpl lfr = new LanguageFileResourceImpl(f.getName());
		return lfr;
	}

	/* (non-Javadoc)
	 * @see com.dotcms.repackage.com.bradmcevoy.http.CopyableResource#copyTo(com.dotcms.repackage.com.bradmcevoy.http.CollectionResource, java.lang.String)
	 */
	public void copyTo(CollectionResource collRes, String name) {
		throw new RuntimeException("Not allowed to implement copy");
	}

	/* (non-Javadoc)
	 * @see com.dotcms.repackage.com.bradmcevoy.http.DeletableResource#delete()
	 */
	public void delete() {
		folder.delete();
	}

	/* (non-Javadoc)
	 * @see com.dotcms.repackage.com.bradmcevoy.http.GetableResource#getMaxAgeSeconds()
	 */
	public Long getMaxAgeSeconds() {
		return new Long(60);
	}

	/* (non-Javadoc)
	 * @see com.dotcms.repackage.com.bradmcevoy.http.GetableResource#sendContent(java.io.OutputStream, com.dotcms.repackage.com.bradmcevoy.http.Range, java.util.Map)
	 */
	public void sendContent(OutputStream arg0, Range arg1, Map<String, String> arg2, String arg3) throws IOException {
		return;
	}

	/* (non-Javadoc)
	 * @see com.dotcms.repackage.com.bradmcevoy.http.MoveableResource#moveTo(com.dotcms.repackage.com.bradmcevoy.http.CollectionResource, java.lang.String)
	 */
	public void moveTo(CollectionResource collRes, String name) {
		throw new RuntimeException("Not allowed to implement move");
	}

	/* (non-Javadoc)
	 * @see com.dotcms.repackage.com.bradmcevoy.http.PropFindableResource#getCreateDate()
	 */
	public Date getCreateDate() {
		Date dt = new Date(folder.lastModified());
//		log.debug("static resource modified: " + dt);
		return dt;
	}

	public String getName() {
		if(isLanguageRoot)
			return "languages";
		else
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

	public LockToken createAndLock(String name, LockTimeout timeout, LockInfo lockInfo)
			throws NotAuthorizedException {
		createCollection(name);
		return lock(timeout, lockInfo).getLockToken();
	}

}
