package com.dotcms.rendering.velocity.viewtools;

import org.apache.velocity.tools.view.tools.ViewTool;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.IdentifierAPI;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ContentletVersionInfo;
import com.dotmarketing.portlets.fileassets.business.FileAsset;
import com.dotmarketing.portlets.fileassets.business.IFileAsset;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.UtilMethods;
import com.liferay.util.StringPool;

public class FileTool implements ViewTool {

	private static final UserAPI userAPI = APILocator.getUserAPI();
	private static final LanguageAPI languageAPI = APILocator.getLanguageAPI();
	private static final IdentifierAPI identifierAPI = APILocator.getIdentifierAPI();

	public void init(Object initData) {

	}

	public Contentlet getFile(String identifier, boolean live) throws DotStateException, DotDataException, DotSecurityException{
		return getFile(identifier, live, languageAPI.getDefaultLanguage().getId());
	}

	public Contentlet getFile(String identifier, boolean live, long languageId) throws DotDataException, DotStateException, DotSecurityException{
		final String conInode = getContentInode(identifier, live, languageId);
		Contentlet contentlet = APILocator.getContentletAPI().find(conInode,  userAPI.getSystemUser(), false);
		if(contentlet==null) {
		    return null;
		}
		if(contentlet.isFileAsset()) {
		    return  APILocator.getFileAssetAPI().fromContentlet(contentlet);
		}

	    return contentlet;
	}

	private String  getContentInode(String identifier, boolean live, long languageId)
			throws DotDataException {
		Identifier id = identifierAPI.find(identifier);
		ContentletVersionInfo cvi = APILocator.getVersionableAPI().getContentletVersionInfo(id.getId(),
				languageId);

		if (null == cvi || !UtilMethods.isSet(cvi.getIdentifier())) {
			throw new DotDataException("Can't find Content-version-info. Identifier: " + id.getId() + ". Lang:" + languageId);
		}
		String conInode = !live ? cvi.getWorkingInode() : cvi.getLiveInode();
		return conInode;
	}

	public Contentlet getFileAsContentlet(String identifier, boolean live, long languageId) throws DotDataException, DotStateException, DotSecurityException{
		try {
		    return getFile(identifier, live, languageId);
		} catch(DotStateException e) {
			final String conInode = getContentInode(identifier, live, languageId);
			return APILocator.getContentletAPI().find(conInode, userAPI.getSystemUser(), false);
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
	
	public String getURI(final Contentlet contentlet, long languageId){
		String uri = StringPool.BLANK;

		if (contentlet instanceof FileAsset) {
			uri = getURI((FileAsset) contentlet, languageId);
		} else if (contentlet.isDotAsset()){
			uri = UtilMethods.espaceForVelocity(
					String.format("dA/%s", contentlet.getIdentifier())
			);
		}
		return uri;
	}

	public IFileAsset getNewFile(){
	    return new FileAsset(); 
	}

}
