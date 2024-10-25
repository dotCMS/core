package com.dotmarketing.portlets.files.action;

import static com.dotmarketing.business.PermissionAPI.PERMISSION_CAN_ADD_CHILDREN;

import com.dotcms.api.system.event.message.MessageSeverity;
import com.dotcms.api.system.event.message.MessageType;
import com.dotcms.api.system.event.message.SystemMessageEventUtil;
import com.dotcms.api.system.event.message.builder.SystemMessageBuilder;
import com.dotcms.repackage.javax.portlet.ActionRequest;
import com.dotcms.repackage.javax.portlet.ActionResponse;
import com.dotcms.repackage.javax.portlet.PortletConfig;
import com.dotcms.repackage.org.apache.struts.action.ActionForm;
import com.dotcms.repackage.org.apache.struts.action.ActionMapping;
import com.dotcms.util.exceptions.DuplicateFileException;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.exception.WebAssetException;
import com.dotmarketing.portal.struts.DotPortletAction;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ContentletDependencies;
import com.dotmarketing.portlets.contentlet.model.IndexPolicyProvider;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;
import com.dotmarketing.portlets.files.struts.FileForm;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.structure.model.ContentletRelationships;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import com.google.common.collect.ImmutableList;
import com.liferay.portal.language.LanguageException;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.User;
import com.liferay.portal.struts.ActionException;
import com.liferay.portal.util.Constants;
import com.liferay.portal.util.PortalUtil;
import com.liferay.portlet.ActionRequestImpl;
import com.liferay.util.FileUtil;
import com.liferay.util.ParamUtil;
import com.liferay.util.StringPool;
import com.liferay.util.servlet.SessionMessages;
import com.liferay.util.servlet.UploadPortletRequest;
import java.io.IOException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

/**
 * @author Maria
 * @author David H Torres 2009
 */

public class UploadMultipleFilesAction extends DotPortletAction {


	public void processAction(
			 ActionMapping mapping, ActionForm form, PortletConfig config,
			 ActionRequest req, ActionResponse res)
		 throws Exception {

        String cmd = req.getParameter(Constants.CMD);
		String referer = req.getParameter("referer");

		//wraps request to get session object
		ActionRequestImpl reqImpl = (ActionRequestImpl)req;
		HttpServletRequest httpReq = reqImpl.getHttpServletRequest();

		if ((referer!=null) && (referer.length()!=0)) {
			referer = URLDecoder.decode(referer,"UTF-8");
		}
		Logger.debug(this, "UploadMultipleFilesAction cmd=" + cmd);

		User user = _getUser(req);
		if(cmd != null && cmd.equals(Constants.EDIT)) {
	        HibernateUtil.startTransaction();
			try {
				Logger.debug(this, "Calling Retrieve method");
	            Logger.debug(this, "Calling Edit Method");
				_editWebAsset(req, res, config, form, user);
			}
			catch (Exception ae) {
				_handleException(ae, req);
				return;
			}
	        HibernateUtil.closeAndCommitTransaction();
		}
		else if (cmd != null && cmd.equals(Constants.ADD)) {
            try {
                Logger.debug(this, "Calling Save Method");
				String subcmd = req.getParameter("subcmd");
				_saveFileAsset(req, res, config, form, user, subcmd);
				_sendToReferral(req,res,referer);
            }
            catch (ActionException ae) {
				_handleException(ae, req);
				if (ae.getMessage().equals("message.file_asset.error.filename.exists")) {
					_sendToReferral(req,res,referer);
				}
				else if (ae.getMessage().equals(WebKeys.USER_PERMISSIONS_EXCEPTION)) {
					SessionMessages.add(httpReq, "error", "message.insufficient.permissions.to.save");
					_sendToReferral(req,res,referer);
				}
            }
        }
		else {
			Logger.debug(this, "Unspecified Action");
		}
		setForward(req, "portlet.ext.files.upload_multiple");
    }

	public void _editWebAsset(ActionRequest req, ActionResponse res,PortletConfig config,ActionForm form, User user)
	throws Exception {

		FolderAPI folderAPI = APILocator.getFolderAPI();

       Folder parentFolder = null;

		if(req.getParameter("parent") != null) {
			parentFolder = folderAPI.find(req.getParameter("parent"),user,false);
		}

		// setting parent folder path and inode on the form bean
		if(parentFolder != null) {
			FileForm cf = (FileForm) form;
			cf.setSelectedparent(parentFolder.getName());
			cf.setParent(parentFolder.getInode());
			cf.setSelectedparentPath(APILocator.getIdentifierAPI().find(parentFolder.getIdentifier()).getPath());
		}

		req.setAttribute("PARENT_FOLDER",parentFolder);
	}

	private String checkMACFileName(String fileName)
	{
		if (UtilMethods.isSet(fileName)) {
    		if (fileName.contains("/"))
    			fileName = fileName.substring(fileName.lastIndexOf("/") + 1, fileName.length());
   		 	if (fileName.contains("\\"))
    		 	fileName = fileName.substring(fileName.lastIndexOf("\\") + 1, fileName.length());
    		fileName = fileName.replaceAll("'","");
    	}
		return fileName;
	}

	private String getFriendlyName(String fileName){

		String val = fileName;
		boolean test = false;
		String newVal = "";
		for(int i=0 ; i < val.length() ; i++){
			String c =     val.substring(i,i+1);
			 if(c == "_") {
			 	  c = " ";
			}
			if(test == true || i == 0){
				 test = false;
			     c = c.toUpperCase();
			}
			if(c == " "){
             	 test = true;
			}


		    if(c == ".") break;
			newVal = newVal +   c;
		}

		return newVal;
	}

	public void _saveFileAsset(ActionRequest req, ActionResponse res,PortletConfig config,ActionForm form, User user, String subcmd)
	throws WebAssetException, ActionException, DotDataException, DotSecurityException, LanguageException, IOException {

	    com.liferay.portlet.RenderRequestImpl reqImpl = (com.liferay.portlet.RenderRequestImpl) req;
        HttpServletRequest httpReq = reqImpl.getHttpServletRequest();
        HttpSession session = httpReq.getSession();

		UploadPortletRequest uploadReq = PortalUtil.getUploadPortletRequest(req);

		String parent = ParamUtil.getString(req, "parent");

		//parent folder
		Folder folder = (Folder) APILocator.getFolderAPI().find(parent, user, false);

		String hostId=folder.getHostId();
		boolean isRootHost=APILocator.getFolderAPI().findSystemFolder().equals(folder);
		if(isRootHost)
		    hostId=(String)session.getAttribute(com.dotmarketing.util.WebKeys.CMS_SELECTED_HOST_ID);

		Host host=APILocator.getHostAPI().find(hostId, user, false);

		//check permissions
		if(isRootHost) {
		    if(!APILocator.getPermissionAPI().doesUserHavePermission(host, PERMISSION_CAN_ADD_CHILDREN, user))
		        throw new ActionException(WebKeys.USER_PERMISSIONS_EXCEPTION);
		}
		else {
		    _checkUserPermissions(folder, user, PERMISSION_CAN_ADD_CHILDREN);
		}

		String fileNamesStr = ParamUtil.getString(req, "fileNames");
		if(!UtilMethods.isSet(fileNamesStr))
			throw new ActionException(LanguageUtil.get(user, "message.file_asset.alert.please.upload"));

		String selectedStructureInode;

		if (config.getPortletName().contains("site-browser")){
			selectedStructureInode = ParamUtil.getString(req, "selectedStructure");
		} else {
			selectedStructureInode = folder.getDefaultFileType();
		}
		if(!UtilMethods.isSet(selectedStructureInode))
			selectedStructureInode = CacheLocator.getContentTypeCache().getStructureByVelocityVarName(FileAssetAPI.DEFAULT_FILE_ASSET_STRUCTURE_VELOCITY_VAR_NAME).getInode();

		String[] fileNamesArray = fileNamesStr.split(WebKeys.CONTENTLET_FORM_NAME_VALUE_SEPARATOR);
		String customMessage = LanguageUtil.get(user, "message.file_asset.error.filename.filters") + ": ";
		if(fileNamesArray.length > 2)
			SessionMessages.add(req, "custommessage", LanguageUtil.get(user, "message.contentlets.batch.reindexing.background"));
		boolean filterError = false;

		List<String> existingFileNames = new ArrayList<>();
		final java.io.File tempFolder = java.io.File.createTempFile("temp", UUID.randomUUID().toString());
		tempFolder.delete();
		tempFolder.mkdirs();
		for (int k=0;k<fileNamesArray.length;k++) {
			try{
				HibernateUtil.startTransaction();
				Contentlet contentlet = new Contentlet();
				contentlet.setStructureInode(selectedStructureInode);
				contentlet.setHost(hostId);
				contentlet.setFolder(folder.getInode());
				long currentLang = 0;
				if (config.getPortletName().contains("site-browser")) {
					final long searchedLangId = Long.parseLong(session.getAttribute(WebKeys.LANGUAGE_SEARCHED).toString());
					final long defaultLanguageId = APILocator.getLanguageAPI().getDefaultLanguage().getId();
					currentLang = searchedLangId == 0 ? defaultLanguageId : searchedLangId;
				} else {
					if (session.getAttribute(WebKeys.CONTENT_SELECTED_LANGUAGE) != null) {
						currentLang = Long.parseLong(session.getAttribute(WebKeys.CONTENT_SELECTED_LANGUAGE).toString());
					} else {
						currentLang = Long.parseLong(session.getAttribute(WebKeys.HTMLPAGE_LANGUAGE).toString());
					}
				}
				if (currentLang != 0) {
					contentlet.setLanguageId(currentLang);
				}

				String fileName = fileNamesArray[k];
				String title = getFriendlyName(fileName);

				fileName = checkMACFileName(fileName);

				if(!APILocator.getFolderAPI().matchFilter(folder,fileName))
	            {
				   customMessage += "<strong>" + fileName + "</strong>, ";
	               filterError = true;
	               continue;
	            }


				if (fileName.length()>0) {

                    //sets filename for this new file
                    contentlet.setStringProperty("title", title);
                    contentlet.setStringProperty("fileName", fileName);

                    java.io.File uploadedFile = uploadReq.getFile(fileName);
                    java.io.File renamedFile = new java.io.File(tempFolder + java.io.File.separator + fileName);
                    
                    FileUtil.move(uploadedFile, renamedFile);
                    

                    
                    contentlet.setBinary("fileAsset", renamedFile);

                    if ( uploadedFile != null ) {

                        //checks if another identifier with the same name exists in the same folder
                        if (APILocator.getFileAssetAPI().fileNameExists(host, folder, fileName, "")) {
                            throw new DuplicateFileException(fileName);
                        }

                        // Now that we know that the host+folder+fileName+language doesn't exist, we need to find if
                        // we have an identifier with host+folder+fileName in order to create a new language.
                        Identifier folderId = APILocator.getIdentifierAPI().find(folder.getIdentifier());
                        String path = folder.getInode().equals(FolderAPI.SYSTEM_FOLDER) ?
                            java.io.File.separator + fileName :
                            folderId.getPath() + fileName;
                        Identifier identifier = APILocator.getIdentifierAPI().find(host, path);

                        // If we the identifier is found then the new FileAsset will be a new language
                        // for that contentlet that already exist.
                        if( identifier!=null && InodeUtils.isSet(identifier.getId()) ) {
                            contentlet.setIdentifier(identifier.getId());
                        }
						String wfActionId = ParamUtil.getString(req, "wfActionId");
						ContentletRelationships contentletRelationships = APILocator.getContentletAPI().getAllRelationships(contentlet);
						contentlet = APILocator.getWorkflowAPI().fireContentWorkflow(contentlet,
								new ContentletDependencies.Builder().respectAnonymousPermissions(Boolean.FALSE)
										.modUser(user)
										.relationships(contentletRelationships)
										.workflowActionId(wfActionId)
										.workflowActionComments(StringPool.BLANK)
										.workflowAssignKey(StringPool.BLANK)
										.categories(Collections.emptyList())
										.indexPolicy(IndexPolicyProvider.getInstance().forSingleContent())
										.generateSystemEvent(Boolean.FALSE).build());

                        HibernateUtil.closeAndCommitTransaction();

                    }

				}
			}
			catch (DuplicateFileException e){
                Logger.warn(UploadMultipleFilesAction.class, "File Asset already exist: " + e.getMessage());
				existingFileNames.add(e.getMessage());
				HibernateUtil.rollbackTransaction();
			}
			catch (IOException e) {
				Logger.error(this, "Exception saving file: " + e.getMessage());
				SessionMessages.add(req, "error", e.getMessage());
				HibernateUtil.rollbackTransaction();
			}
			catch (Exception e) {
				Logger.error(this, e.getMessage());
				SessionMessages.add(req, "error", e.getMessage());
				HibernateUtil.rollbackTransaction();
			}
		}
        FileUtil.deltree(tempFolder);
		if(!existingFileNames.isEmpty()){
			StringBuffer messageText = new StringBuffer();
			if(existingFileNames.size()>1){
				messageText.append(LanguageUtil.get(user, "The-following-uploaded-files-already-exist"));
			}else{
				messageText.append(LanguageUtil.get(user, "The-uploaded-file-already-exists"));
			}

			for(int i=0;i<existingFileNames.size();i++){
				if(i==0){
					messageText.append(existingFileNames.get(i));
				}else{
					messageText.append(", "  + existingFileNames.get(i));
				}
			}
			SessionMessages.add(req, "custommessage", messageText.toString());
		}

		if(filterError)
		{
			customMessage = customMessage.substring(0,customMessage.lastIndexOf(","));
			Logger.error(this, customMessage);
			final SystemMessageBuilder message = new SystemMessageBuilder()
					.setMessage(customMessage)
					.setSeverity(MessageSeverity.ERROR)
					.setType(MessageType.SIMPLE_MESSAGE)
					.setLife(3000);
			SystemMessageEventUtil.getInstance().pushMessage(message.create(), ImmutableList.of(user.getUserId()));
		}
	}
}