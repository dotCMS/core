package com.dotmarketing.viewtools;

import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.IdentifierAPI;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.cache.LiveCache;
import com.dotmarketing.cache.WorkingCache;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.fileassets.business.FileAsset;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;
import com.dotmarketing.portlets.fileassets.business.IFileAsset;
import com.dotmarketing.portlets.files.business.FileAPI;
import com.dotmarketing.portlets.files.model.File;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;

import org.apache.velocity.tools.view.tools.ViewTool;

public class FileTool implements ViewTool {

	private static final FileAPI fileAPI = APILocator.getFileAPI();
	private static final UserAPI userAPI = APILocator.getUserAPI();
	private static final LanguageAPI languageAPI = APILocator.getLanguageAPI();
	private static final ContentletAPI contentletAPI = APILocator.getContentletAPI();
	private static final FileAssetAPI fileAssetAPI = APILocator.getFileAssetAPI();
	private static final IdentifierAPI identifierAPI = APILocator.getIdentifierAPI();

	public void init(Object initData) {

	}

	public File getNewFile(){
		return new File();
	}

	public IFileAsset getFile(String identifier, boolean live){
		return getFile(identifier, live, -1);
	}

	public IFileAsset getFile(String identifier, boolean live, long languageId){
		Identifier id;
		String p = null;
		try {
			id = identifierAPI.find(identifier);
		    if(live){
			  p = LiveCache.getPathFromCache(id.getURI(), id.getHostId(), languageId != -1 ? languageId : null);
		    }else{
			  p = WorkingCache.getPathFromCache(id.getURI(), id.getHostId(), languageId != -1 ? languageId : null);
		    }
		} catch (Exception e1) {
			Logger.error(FileTool.class,e1.getMessage(),e1);
			return new File();
		}
        p = p.substring(5, p.lastIndexOf("."));
        IFileAsset file = null;
		try {
			if(id!=null && InodeUtils.isSet(id.getId()) && id.getAssetType().equals("contentlet")){
				Contentlet cont = contentletAPI.findContentletByIdentifier(
					id.getId(),
					live,
					languageId != -1 ? languageId : languageAPI.getDefaultLanguage().getId(),
					userAPI.getSystemUser(),
					false);
				if(cont!=null && InodeUtils.isSet(cont.getInode())){
					file = fileAssetAPI.fromContentlet(cont);
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
			return UtilMethods.espaceForVelocity("/contentAsset/raw-data/" + file.getIdentifier() + "/fileAsset");
		}else{
			return "";
		}
	}

	public String getURI(FileAsset file){
		return getURI(file, -1);
	}

	public String getURI(FileAsset file, long languageId){
		String langStr = languageId>0?"?language_id="+languageId:"";

		if(file != null && InodeUtils.isSet(file.getIdentifier())){
            return UtilMethods.espaceForVelocity("/contentAsset/raw-data/" + file.getIdentifier() + "/fileAsset" + langStr);
        }else{
			return "";
		}
	}

}
