package com.dotmarketing.viewtools;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.velocity.context.Context;
import org.apache.velocity.tools.view.context.ViewContext;
import org.apache.velocity.tools.view.tools.ViewTool;

import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.fileassets.business.IFileAsset;
import com.dotmarketing.portlets.files.model.File;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;

public class VideoGalleryWebApi implements ViewTool{
	private static final UserAPI userAPI = APILocator.getUserAPI();
	Context ctx;
	private HttpServletRequest request;
	
	public void init(Object obj) {
		ViewContext context = (ViewContext) obj;
		ctx = context.getVelocityContext();
		this.request = context.getRequest();
	}
	
	public List<IFileAsset> getVideoGalleryByPath (String folderPath, Host host) {
	    return getVideoGalleryByPath (folderPath, host.getIdentifier());
	}

	@Deprecated
	public List<IFileAsset> getVideoGalleryByPath (String folderPath, long hostId) {
		return getVideoGalleryByPath (folderPath, String.valueOf(hostId));
	}
	
	public List<IFileAsset> getVideoGalleryByPath (String folderPath, String hostId) {
		folderPath = (folderPath == null)?"":folderPath;
		folderPath = folderPath.trim().endsWith("/")?folderPath.trim():folderPath.trim() + "/";
		Folder folder = new Folder();
		try {
			folder = APILocator.getFolderAPI().findFolderByPath(folderPath, hostId,userAPI.getSystemUser(),false);
		} catch (Exception e) {
			Logger.error(this,e.getMessage());
			throw new DotRuntimeException(e.getMessage(),e);
		}

        boolean ADMIN_MODE= (request.getSession().getAttribute(com.dotmarketing.util.WebKeys.ADMIN_MODE_SESSION) != null);
        boolean PREVIEW_MODE = ((request.getSession().getAttribute(com.dotmarketing.util.WebKeys.PREVIEW_MODE_SESSION) != null) && ADMIN_MODE);
        boolean EDIT_MODE = ((request.getSession().getAttribute(com.dotmarketing.util.WebKeys.EDIT_MODE_SESSION) != null) && ADMIN_MODE);
        
		List<IFileAsset> filesList = new ArrayList<IFileAsset>();
		try {
			if(PREVIEW_MODE || EDIT_MODE){
				filesList.addAll(APILocator.getFolderAPI().getWorkingFiles(folder, userAPI.getSystemUser(), false));
				filesList.addAll(APILocator.getFileAssetAPI().findFileAssetsByFolder(folder,"",false, true, userAPI.getSystemUser(), false));
			}else{
				filesList.addAll(APILocator.getFolderAPI().getLiveFiles(folder, userAPI.getSystemUser(), false));
				filesList.addAll(APILocator.getFileAssetAPI().findFileAssetsByFolder(folder,"",true, false, userAPI.getSystemUser(), false));
			}
		} catch (Exception e) {
			Logger.error(this, e.getMessage(), e);
		} 
		List<IFileAsset> videoList = new ArrayList<IFileAsset> ();
		for(IFileAsset file : filesList) {
			String ext = file.getExtension();
			if(ext.toLowerCase().endsWith("flv"))
				videoList.add(file);
		}
		return videoList;
	}
	
	@Deprecated
	public List<IFileAsset> getVideoImages (String videoURI, long hostId) {
		return getVideoImages (videoURI, String.valueOf(hostId));
	}
	
	public List<IFileAsset> getVideoImages (String videoURI, String hostId) {
		//String videoURI = videoFile.getURI();
		String imageURI = videoURI.substring(0,videoURI.length()-4) + ".jpg";
		IFileAsset img = null;
		try {
			Host host = APILocator.getHostAPI().find(hostId,userAPI.getSystemUser(),false);
			Identifier id = APILocator.getIdentifierAPI().find(host, imageURI);
			if(id!=null && InodeUtils.isSet(id.getId()) && id.getAssetType().equals("contentlet")){
				Contentlet cont = APILocator.getContentletAPI().findContentletByIdentifier(id.getId(), true, APILocator.getLanguageAPI().getDefaultLanguage().getId(), userAPI.getSystemUser(),false);
				if(cont!=null && InodeUtils.isSet(cont.getInode())){
					img = APILocator.getFileAssetAPI().fromContentlet(cont);
				}
			}else{
			  img = APILocator.getFileAPI().getFileByURI(imageURI, hostId, true, userAPI.getSystemUser(),false);
			}
		} catch (Exception e) {
			Logger.error(this, e.getMessage(), e);
		} 
		List<IFileAsset> videoList = new ArrayList<IFileAsset> ();
		if(img!=null){
			videoList.add(img);
		}
		return videoList;
	}
}
