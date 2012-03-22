package com.dotmarketing.webdav;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;
import java.util.Map;

import com.bradmcevoy.http.Auth;
import com.bradmcevoy.http.CollectionResource;
import com.bradmcevoy.http.FileItem;
import com.bradmcevoy.http.FileResource;
import com.bradmcevoy.http.LockInfo;
import com.bradmcevoy.http.LockResult;
import com.bradmcevoy.http.LockTimeout;
import com.bradmcevoy.http.LockToken;
import com.bradmcevoy.http.LockableResource;
import com.bradmcevoy.http.Range;
import com.bradmcevoy.http.Request;
import com.bradmcevoy.http.Resource;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import com.liferay.util.FileUtil;

/**
 * 
 * @author Jason Tesser
 */
public class TempFileResourceImpl implements FileResource, LockableResource {
    
	private DotWebdavHelper dotDavHelper;
	private final File file;
    private final String url;
    private User user;
    private boolean isAutoPub = false;
    private PermissionAPI perAPI; 
    
    public TempFileResourceImpl(File file, String url, boolean isAutoPub) {
        if( file.isDirectory() ){
        	Logger.error(this, "Trying to get a temp file which is actually a directory!!!");
        	throw new IllegalArgumentException("Static resource must be a file, this is a directory: " + file.getAbsolutePath());
        }
        dotDavHelper = new DotWebdavHelper();
        perAPI = APILocator.getPermissionAPI();
        this.file = file;
        this.url = url;
        this.isAutoPub = isAutoPub;
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
        FileInputStream fis = new FileInputStream(file);
        BufferedInputStream bin = new BufferedInputStream(fis);
        final byte[] buffer = new byte[ 1024 ];
        int n = 0;
        while( -1 != (n = bin.read( buffer )) ) {
            out.write( buffer, 0, n );
        }
    }

    
    public String getName() {
        return file.getName();
    }

    
    public Object authenticate(String username, String password) {
    	try {
			user =  dotDavHelper.authorizePrincipal(username, password);
			return user;
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
//        log.debug("static resource modified: " + dt);
        return dt;
    }

    
    public Long getContentLength() {
        return file.length();
    }

    
    public String getContentType(String accepts) {
//        String s = MimeUtil.getMimeType(file.getAbsolutePath());
//        s = MimeUtil.getPreferedMimeType(accepts,s);
//        return s;
    	
    	String mimeType = Config.CONTEXT.getMimeType(file.getName());
    	if (!UtilMethods.isSet(mimeType)) {
			mimeType = com.dotmarketing.portlets.files.model.File.UNKNOWN_MIME_TYPE;
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
		if(collRes instanceof TempFolderResourceImpl){
			TempFolderResourceImpl tr = (TempFolderResourceImpl)collRes;
			try {
				FileUtil.copyFile(file, new File(tr.getFolder().getPath() + File.separator + name));
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
				dotDavHelper.copyTempFileToStorage(file, p + name, user, isAutoPub);
			} catch (Exception e) {
				Logger.error(this, e.getMessage(), e);
			}
		}else if(collRes instanceof LanguageFolderResourceImpl){
			LanguageFolderResourceImpl lr = (LanguageFolderResourceImpl)collRes;
			try {
				ClassLoader classLoader = getClass().getClassLoader();
				File f = new File(classLoader.getResource("content").getFile() + File.separator + name);
				if(f.exists()){
					File folder = new File(classLoader.getResource("content").getFile() + File.separator + "archived" + File.separator + f.getName());
					folder.mkdirs();
					FileUtil.copyFile(f, new File(folder.getPath() + File.separator + new Date().toString()));
				}
				FileUtil.copyFile(file, f);
			} catch (Exception e) {
				Logger.error(this, e.getMessage(), e);
				return;
			}
		}
	}


	public void delete() {
		file.delete();
	}


	public void moveTo(CollectionResource collRes, String name) {
		if(collRes instanceof TempFolderResourceImpl){
			TempFolderResourceImpl tr = (TempFolderResourceImpl)collRes;
			try {
				FileUtil.copyFile(file, new File(tr.getFolder().getPath() + File.separator + name));
				file.delete();
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
				dotDavHelper.copyTempFileToStorage(file, p + name, user, isAutoPub);
				file.delete();
			} catch (Exception e) {
				Logger.error(this, e.getMessage(), e);
			}
		}else if(collRes instanceof LanguageFolderResourceImpl){
			LanguageFolderResourceImpl lr = (LanguageFolderResourceImpl)collRes;
			try {
				ClassLoader classLoader = getClass().getClassLoader();
				File f = new File(classLoader.getResource("content").getFile() + File.separator + name);
				if(f.exists()){
					File folder = new File(classLoader.getResource("content").getFile() + File.separator + "archived" + File.separator + f.getName());
					folder.mkdirs();
					FileUtil.copyFile(f, new File(folder.getPath() + File.separator + new Date().toString() + f.getName()));
				}
				FileUtil.copyFile(file, f);
				file.delete();
			} catch (Exception e) {
				Logger.error(this, e.getMessage(), e);
				return;
			}
		}
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
