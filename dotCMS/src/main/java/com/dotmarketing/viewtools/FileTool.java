package com.dotmarketing.viewtools;

import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.IdentifierAPI;
import com.dotmarketing.business.UserAPI;

import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ContentletVersionInfo;
import com.dotmarketing.portlets.fileassets.business.FileAsset;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;
import com.dotmarketing.portlets.fileassets.business.IFileAsset;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.util.StringPool;

import org.apache.velocity.tools.view.tools.ViewTool;

public class FileTool implements ViewTool {

	private static final UserAPI userAPI = APILocator.getUserAPI();
	private static final LanguageAPI languageAPI = APILocator.getLanguageAPI();
	private static final ContentletAPI contentletAPI = APILocator.getContentletAPI();
	private static final FileAssetAPI fileAssetAPI = APILocator.getFileAssetAPI();
	private static final IdentifierAPI identifierAPI = APILocator.getIdentifierAPI();

	public void init(Object initData) {

	}

	public IFileAsset getFile(String identifier, boolean live) throws DotStateException, DotDataException, DotSecurityException{
		return getFile(identifier, live, languageAPI.getDefaultLanguage().getId());
	}

	public IFileAsset getFile(String identifier, boolean live, long languageId) throws DotDataException, DotStateException, DotSecurityException{
		Identifier id = identifierAPI.find(identifier);
		ContentletVersionInfo  cvi = APILocator.getVersionableAPI().getContentletVersionInfo(id.getId(), languageId);
	    String conInode = (!live) ? cvi.getWorkingInode() : cvi.getLiveInode();
	    FileAsset file  = APILocator.getFileAssetAPI().fromContentlet(APILocator.getContentletAPI().find(conInode,  userAPI.getSystemUser(), false));
	    return file;
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
	
	public IFileAsset getNewFile(){
	    return new FileAsset(); 
	}

}
