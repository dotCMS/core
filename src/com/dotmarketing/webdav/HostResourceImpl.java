package com.dotmarketing.webdav;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.bradmcevoy.http.Auth;
import com.bradmcevoy.http.CollectionResource;
import com.bradmcevoy.http.LockInfo;
import com.bradmcevoy.http.LockResult;
import com.bradmcevoy.http.LockTimeout;
import com.bradmcevoy.http.LockToken;
import com.bradmcevoy.http.LockingCollectionResource;
import com.bradmcevoy.http.MakeCollectionableResource;
import com.bradmcevoy.http.PropFindableResource;
import com.bradmcevoy.http.Request;
import com.bradmcevoy.http.Request.Method;
import com.bradmcevoy.http.Resource;
import com.bradmcevoy.http.exceptions.LockedException;
import com.bradmcevoy.http.exceptions.NotAuthorizedException;
import com.bradmcevoy.http.exceptions.PreConditionFailedException;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.portlets.fileassets.business.FileAsset;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;

public class HostResourceImpl implements Resource, CollectionResource, PropFindableResource, MakeCollectionableResource, LockingCollectionResource{

	private Host host;
	private DotWebdavHelper dotDavHelper;
	private String path;
	private User user;
	private boolean isAutoPub = false;
	private PermissionAPI perAPI;
	
	public HostResourceImpl(String path, Host host) {
		perAPI = APILocator.getPermissionAPI();
		dotDavHelper = new DotWebdavHelper();
		this.isAutoPub = isAutoPub;
		this.path = path;
		this.host = host;
	}
	
	public Object authenticate(String username, String password) {
		try {
			this.user =  dotDavHelper.authorizePrincipal(username, password);
			return user;
		} catch (Exception e) {
			Logger.error(this, e.getMessage(), e);
			return null;
		}
	}

	public boolean authorise(Request request, Method method, Auth auth) {
		try {
			
			if(auth == null)
				return false;
			else if(method.isWrite){
				return perAPI.doesUserHavePermission(host, PermissionAPI.PERMISSION_WRITE, user, false);
			}else{
				return perAPI.doesUserHavePermission(host, PermissionAPI.PERMISSION_READ, user, false);
			}

		} catch (DotDataException e) {
			Logger.error(HostResourceImpl.class, e.getMessage(),
					e);
			throw new DotRuntimeException(e.getMessage(), e);
		}
	}

	public String checkRedirect(Request request) {
		return null;
	}

	public Long getContentLength() {
		return new Long(0);
	}

	public String getContentType(String accepts) {
		return "";
	}

	public Date getModifiedDate() {
		return host.getModDate();
	}

	public String getName() {
		return host.getHostname();
	}

	public String getRealm() {
		return null;
	}

	public String getUniqueId() {
		return host.getIdentifier();
	}

	public int compareTo(Object o) {
		// TODO Auto-generated method stub
		return 0;
	}

	public Resource child(String childName) {
		List<Folder> folders = listFolders();
		for (Folder folder : folders) {
			if(childName.equalsIgnoreCase(folder.getName())){
				String folderPath;
				try {
					folderPath = APILocator.getIdentifierAPI().find(folder).getPath();
				} catch (Exception e) {
					Logger.error(this, e.getMessage(), e);
					throw new DotRuntimeException(e.getMessage(),e);
				}
				FolderResourceImpl fr = new FolderResourceImpl(folder,path + "/" + (folderPath.startsWith("/")?folderPath.substring(1):folderPath));
				return fr;
			}
		}
		return null;
	} 

	public List<? extends Resource> getChildren() {
		List<Folder> folders = listFolders();
		List<Resource> frs = new ArrayList<Resource>();
		for (Folder folder : folders) {
			String p = path;
			if(p.endsWith("/"))
				p = p.substring(0, path.length() - 1);
			String folderPath = "";
			try {
				folderPath = APILocator.getIdentifierAPI().find(folder).getPath();
			} catch (Exception e) {
				Logger.error(this, e.getMessage(), e);
				throw new DotRuntimeException(e.getMessage(),e);
			} 
			FolderResourceImpl fr = new FolderResourceImpl(folder, p + folderPath);
			frs.add(fr);
		}
		try {
			List<FileAsset> fas = APILocator.getFileAssetAPI().findFileAssetsByHost(host, user, false);
			for(FileAsset fa:fas){
				FileResourceImpl fr = new FileResourceImpl(fa, path + host.getHostname() + "/" + fa.getPath());
				frs.add(fr);
			}
		} catch (Exception e) {
			
		} 
		/**
		TemplateFolderResourceImpl tfrl = new TemplateFolderResourceImpl(path + "/_TEMPLATES", host);
		frs.add(tfrl);
		**/
		
		
		String prePath;
		if(isAutoPub){
			prePath = "/webdav/autopub/";
		}else{
			prePath = "/webdav/nonpub/";
		}
		java.io.File tempDir = new java.io.File(dotDavHelper.getTempDir().getPath() + java.io.File.separator + host.getHostname());
		if(tempDir.exists() && tempDir.isDirectory()){
			java.io.File[] files = tempDir.listFiles();
			for (java.io.File file : files) {
				String tp = prePath + host.getHostname() + "/" + file.getName();
				if(!dotDavHelper.isTempResource(tp)){
					continue;
				}
				if(file.isDirectory()){
					TempFolderResourceImpl tr = new TempFolderResourceImpl(tp,file,isAutoPub);
					frs.add(tr);
				}else{
					TempFileResourceImpl tr = new TempFileResourceImpl(file,tp,isAutoPub);
					frs.add(tr);
				}
			}
		}
		return frs;
	}

	public Host getHost() {
		return host;
	}

	public void setHost(Host host) {
		this.host = host;
	}

	private List<Folder> listFolders(){
		PermissionAPI perAPI = APILocator.getPermissionAPI();
		FolderAPI folderAPI = APILocator.getFolderAPI();
		List<Folder> folders = new ArrayList<Folder>();
		try {
			folders = folderAPI.findSubFolders(host,user,false);
		} catch (Exception e) {
			Logger.error(this, e.getMessage(), e);
			throw new DotRuntimeException(e.getMessage(),e);
		} 
		for (Folder folderAux : folders) {
//			if (perAPI.doesUserHavePermission(folderAux, PERMISSION_READ, user, false)) {
//				
//			}
			
		}
		return folders;
	}

	public Date getCreateDate() {
		return host.getModDate();
	}

	public CollectionResource createCollection(String newName) throws DotRuntimeException {
		if(dotDavHelper.isTempResource(newName)){
			File f = dotDavHelper.createTempFolder(File.separator + host.getHostname() + File.separator + newName);
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

	public LockToken createAndLock(String arg0, LockTimeout arg1, LockInfo arg2)
			throws NotAuthorizedException {
		// TODO Auto-generated method stub
		return null;
	}

    public LockToken getCurrentLock() {
        // TODO Auto-generated method stub
        return null;
    }

    public LockResult lock(LockTimeout arg0, LockInfo arg1)
            throws NotAuthorizedException, PreConditionFailedException,
            LockedException {
        // TODO Auto-generated method stub
        return null;
    }

    public LockResult refreshLock(String arg0) throws NotAuthorizedException,
            PreConditionFailedException {
        // TODO Auto-generated method stub
        return null;
    }

    public void unlock(String arg0) throws NotAuthorizedException,
            PreConditionFailedException {
        // TODO Auto-generated method stub
        
    }
}
