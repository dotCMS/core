package com.dotmarketing.viewtools;

import org.apache.velocity.tools.view.tools.ViewTool;

import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.cache.LiveCache;
import com.dotmarketing.cache.WorkingCache;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.fileassets.business.FileAsset;
import com.dotmarketing.portlets.fileassets.business.IFileAsset;
import com.dotmarketing.portlets.files.business.FileAPI;
import com.dotmarketing.portlets.files.model.File;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;

public class FileTool implements ViewTool {

	private static final FileAPI fileAPI = APILocator.getFileAPI();
	private static final UserAPI userAPI = APILocator.getUserAPI();

	public void init(Object initData) {

	}

	public File getNewFile(){
		return new File();
	}
	
	public IFileAsset getFile(String identifier, boolean live){
		Identifier id;
		String p = null;
		try {
			id = APILocator.getIdentifierAPI().find(identifier);
		    if(live){
			  p = LiveCache.getPathFromCache(id.getURI(), id.getHostId());
		    }else{
			  p = WorkingCache.getPathFromCache(id.getURI(), id.getHostId());
		    }
		} catch (Exception e1) {
			Logger.error(FileTool.class,e1.getMessage(),e1);
			return new File();
		}
        p = p.substring(5, p.lastIndexOf("."));
        IFileAsset file = null;
		try {
			if(id!=null && InodeUtils.isSet(id.getId()) && id.getAssetType().equals("contentlet")){
				Contentlet cont = APILocator.getContentletAPI().findContentletByIdentifier(id.getId(), true, APILocator.getLanguageAPI().getDefaultLanguage().getId(), APILocator.getUserAPI().getSystemUser(), false);
				if(cont!=null && InodeUtils.isSet(cont.getInode())){
					file = APILocator.getFileAssetAPI().fromContentlet(cont);
				}
			}else{
				file = fileAPI.find(p, userAPI.getSystemUser(), false);
			}

		} catch (Exception e) {
			Logger.error(FileTool.class,e.getMessage(),e);
		} 
		if(file == null){
			file = new File();
		}
		return file;
	}
	
	public String getURI(File file){
		if(file != null && InodeUtils.isSet(file.getIdentifier())){
			return UtilMethods.espaceForVelocity("/dotAsset/" + file.getIdentifier() + "." + file.getExtension());
		}else{
			return "";
		}
	}
	
	public String getURI(FileAsset file){
		if(file != null && InodeUtils.isSet(file.getIdentifier())){
			return UtilMethods.espaceForVelocity("/dotAsset/" + file.getIdentifier() + "." + file.getExtension());
		}else{
			return "";
		}
	}
	
}
