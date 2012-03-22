package com.dotmarketing.portlets.files.action;

import static com.dotmarketing.business.PermissionAPI.PERMISSION_CAN_ADD_CHILDREN;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.PortletConfig;
import javax.servlet.http.HttpServletRequest;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionMapping;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.cache.StructureCache;
import com.dotmarketing.cache.WorkingCache;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.WebAssetException;
import com.dotmarketing.factories.PublishFactory;
import com.dotmarketing.factories.WebAssetFactory;
import com.dotmarketing.portal.struts.DotPortletAction;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;
import com.dotmarketing.portlets.files.business.FileAPI;
import com.dotmarketing.portlets.files.model.File;
import com.dotmarketing.portlets.files.struts.FileForm;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.User;
import com.liferay.portal.struts.ActionException;
import com.liferay.portal.util.Constants;
import com.liferay.portal.util.PortalUtil;
import com.liferay.portlet.ActionRequestImpl;
import com.liferay.util.ParamUtil;
import com.liferay.util.servlet.SessionMessages;
import com.liferay.util.servlet.UploadPortletRequest;

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

        HibernateUtil.startTransaction();

		User user = _getUser(req);

		try {
			Logger.debug(this, "Calling Retrieve method");
			_retrieveWebAsset(req, res, config, form, user, File.class, WebKeys.FILE_EDIT);

		} catch (Exception ae) {
			_handleException(ae, req);
			return;
		}

        try {
            Logger.debug(this, "Calling Edit Method");
			_editWebAsset(req, res, config, form, user);

        }
        catch (Exception e) {

        }

        if ((cmd != null) && cmd.equals(Constants.ADD)) {
            try {

                Logger.debug(this, "Calling Save Method");

				String subcmd = req.getParameter("subcmd");

				_saveFileAsset(req, res, config, form, user, subcmd);

				_sendToReferral(req,res,referer);

            } catch (ActionException ae) {
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
        Logger.debug(this, "Unspecified Action");
        HibernateUtil.commitTransaction();

		setForward(req, "portlet.ext.files.upload_multiple");
    }

	public void _editWebAsset(ActionRequest req, ActionResponse res,PortletConfig config,ActionForm form, User user)
	throws Exception {

		FileAPI fileAPI = APILocator.getFileAPI();
		FolderAPI folderAPI = APILocator.getFolderAPI();

		// calls edit method from super class that returns parent folder
		super._editWebAsset(req, res, config, form, user, WebKeys.FILE_EDIT);

		// This can't be done on the WebAsset so it needs to be done here.
		File file = (File) req.getAttribute(WebKeys.FILE_EDIT);

       Folder parentFolder = null;

		if(req.getParameter("parent") != null) {
			parentFolder = folderAPI.find(req.getParameter("parent"),user,false);
		} else {
			parentFolder = fileAPI.getFileFolder(file,WebAPILocator.getHostWebAPI().getCurrentHost(req), user, false);
		}

		// setting parent folder path and inode on the form bean
		if(parentFolder != null) {
			FileForm cf = (FileForm) form;
			cf.setSelectedparent(parentFolder.getName());
			cf.setParent(parentFolder.getInode());
			cf.setSelectedparentPath(APILocator.getIdentifierAPI().find(parentFolder).getPath());
			file.setParent(parentFolder.getInode());
		}



		req.setAttribute("PARENT_FOLDER",parentFolder);

	}

	public void _saveWebAsset(ActionRequest req, ActionResponse res,PortletConfig config,ActionForm form, User user, String subcmd)
		throws WebAssetException, Exception {

			//wraps request to get session object
			ActionRequestImpl reqImpl = (ActionRequestImpl)req;
			HttpServletRequest httpReq = reqImpl.getHttpServletRequest();

			try {
				UploadPortletRequest uploadReq = PortalUtil.getUploadPortletRequest(req);

				String parent = ParamUtil.getString(req, "parent");

				int fileCounter = 0;
				//parent folder
				//Folder folder = (Folder) InodeFactory.getInode(parent, Folder.class);
				Folder folder = (Folder) APILocator.getFolderAPI().find(parent, user, false);

				//check permissions
				_checkUserPermissions(folder, user, PERMISSION_CAN_ADD_CHILDREN);

				//gets user id from request for mod user
				String userId = user.getUserId();

				String fileNamesStr = ParamUtil.getString(req, "fileNames");
				if(!UtilMethods.isSet(fileNamesStr))
					throw new ActionException(LanguageUtil.get(user, "message.file_asset.alert.please.upload"));

				String[] fileNamesArray = fileNamesStr.split(WebKeys.CONTENTLET_FORM_NAME_VALUE_SEPARATOR);
				String customMessage = "Some file does not match the filters specified by the folder: ";
				boolean filterError = false;
				List<String> fileNames = new ArrayList<String>();
				for (int k=0;k<fileNamesArray.length;k++) {

					File file = new File();
					String fileName = fileNamesArray[k];
					String title = getFriendlyName(fileName);
					String friendlyName = getFriendlyName(fileName);



					Date publishDate = new Date();
					fileName = checkMACFileName(fileName);


					if(!APILocator.getFolderAPI().matchFilter(folder,fileName))
		            {
					   customMessage += fileName + ", ";
		               filterError = true;
		               continue;
		            }

					if (fileName.length()>0) {




						//checks if another identifier with the same name exists in the same folder
						if ((APILocator.getFileAPI().fileNameExists(folder,fileName))) {
							fileNames.add(fileName);
						}
						else {
							//gets the real path to the assets directory
							String mimeType = APILocator.getFileAPI().getMimeType(fileName);


							//gets file extension
							String suffix = UtilMethods.getFileExtension(fileName);
							//sets filename for this new file
							file.setTitle(title);
							file.setFileName(fileName);
							file.setFriendlyName(friendlyName);
							file.setPublishDate(publishDate);
							//persists the file
							file.setModUser(userId);
							file.setMimeType(mimeType);
							HibernateUtil.saveOrUpdate(file);

							//gets the real path to the assets directory
							String filePath = APILocator.getFileAPI().getRealAssetsRootPath();
							new java.io.File(filePath).mkdir();

							java.io.File uploadedFile = uploadReq.getFile(fileName);

							Logger.debug(this, "bytes" + uploadedFile.length());

							//sets bytes size from request uploaded object
							file.setSize((int)uploadedFile.length());
							file.setMimeType(mimeType);

							//creates the path where to save this file based on the inode
							String fileInodePath = String.valueOf(file.getInode());
							if (fileInodePath.length()==1) {
								fileInodePath = fileInodePath + "0";
							}
							//creates the path with inode{1} + inode{2}
							fileInodePath = fileInodePath.substring(0,1) + java.io.File.separator + fileInodePath.substring(1,2);
							//creates directory for first level directory in case it doesn't exist
							new java.io.File(filePath + java.io.File.separator + fileInodePath.substring(0,1)).mkdir();
							//creates directory for second level directory in case it doesn't exist
							new java.io.File(filePath + java.io.File.separator + fileInodePath).mkdir();

							//creates the new file as inode{1}/inode{2}/inode.file_extension
							java.io.File f = new java.io.File(filePath + java.io.File.separator + fileInodePath + java.io.File.separator + file.getInode() + "." + suffix);

							//gets outputstream from the file
							java.io.FileOutputStream fout = new java.io.FileOutputStream(f);
							//gets buffered outputstream to write the data in the file
							FileChannel outputChannel = fout.getChannel();
							FileChannel inputChannel =null;

							if(uploadedFile.length()!=0){
							 inputChannel = new java.io.FileInputStream(uploadedFile).getChannel();
							 }

							else
							    {
								inputChannel = new java.io.FileInputStream(f).getChannel();
							    }
							//writes all data from input in output
							outputChannel.transferFrom(inputChannel, 0, inputChannel.size());

							//closes all streams
							outputChannel.force(false);
							outputChannel.close();
							inputChannel.close();

							Logger.debug(this, "SaveFileAction New File in =" + filePath + java.io.File.separator + fileInodePath + java.io.File.separator + file.getInode() + "." + suffix);


							//checks if it's an image
							if (suffix.equals("jpg") || suffix.equals("gif") || suffix.equals("png")) {

								com.dotmarketing.util.ImageResizeUtils.generateThumbnail(filePath + java.io.File.separator + fileInodePath + java.io.File.separator,String.valueOf(file.getInode()),suffix);

								//gets image height
								int height = javax.imageio.ImageIO.read(f).getHeight();
								file.setHeight(height);
								Logger.debug(this, "File height=" + height);
								//gets image width
								int width = javax.imageio.ImageIO.read(f).getWidth();
								file.setWidth(width);
								Logger.debug(this, "File width=" + width);
								//gets image size

								/*if (file.getMaxSize()!=0) maxsize = file.getMaxSize();
								if (file.getMaxWidth()!=0) maxwidth = file.getMaxWidth();
								if (file.getMaxHeight()!=0) maxheight = file.getMaxHeight();
								if (file.getMinHeight()!=0) minheight = file.getMinHeight();

								if (width > maxwidth) {
									message = "The uploaded file " + fileName + " is too wide: " + width + "px. Maximum width allowed is: " + maxwidth + "px";
									f.delete();
									InodeFactory.deleteInode(file);
									Logger.debug(this, "message from File Upload=" + message);
									SessionMessages.add(req, "custommessage", message);
								}
								else if (height > maxheight) {
									message = "The uploaded file " + fileName + " is too tall: " + height + "px. Maximum height allowed is: " + maxheight + "px";
									f.delete();
									InodeFactory.deleteInode(file);
									Logger.debug(this, "message from File Upload=" + message);
									SessionMessages.add(req, "custommessage", message);
								}
								else {*/
									//creates a new file
									WebAssetFactory.createAsset(file,userId,folder);
								/*}*/
							}
							else {
									//it's a file
									//it saves or updates the asset
									WebAssetFactory.createAsset(file,userId,folder);
							}
							WorkingCache.removeAssetFromCache(file);
							WorkingCache.addToWorkingAssetToCache(file);

							fileCounter+=1;

							if ((subcmd != null) && subcmd.equals(com.dotmarketing.util.Constants.PUBLISH)) {
								try{
									PublishFactory.publishAsset(file,httpReq);
									if(fileCounter > 1){
						                SessionMessages.add(req, "message", "message.file_asset.save");
									}else{
										SessionMessages.add(req, "message", "message.fileupload.save");
									}
								}catch(WebAssetException wax){
									Logger.error(this, wax.getMessage(),wax);
									SessionMessages.add(req, "error", "message.webasset.published.failed");
								}
							}
						}

					}
				}

				if(!fileNames.isEmpty()){
					StringBuffer messageText = new StringBuffer();
					if(fileNames.size()>1){
						messageText.append("The uploaded files ");
					}else{
						messageText.append("The uploaded file ");
					}

					for(int i=0;i<fileNames.size();i++){
						if(i==0){
							messageText.append(fileNames.get(i));
						}else{
							messageText.append(", "  + fileNames.get(i));
						}
					}
					if(fileNames.size()>1){
						messageText.append(" already exist in this folder");
					}else{
						messageText.append(" already exists in this folder");
					}

					SessionMessages.add(req, "custommessage", messageText.toString());
				}


				if(filterError)
				{
					customMessage = customMessage.substring(0,customMessage.lastIndexOf(","));
					SessionMessages.add(req, "custommessage",customMessage);
				}

			}
			catch (IOException e) {
				Logger.error(this, "Exception saving file: " + e.getMessage());
				throw new ActionException(e.getMessage());
			}
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
	throws WebAssetException, Exception {

		try {
			UploadPortletRequest uploadReq = PortalUtil.getUploadPortletRequest(req);

			String parent = ParamUtil.getString(req, "parent");

			//parent folder
			Folder folder = (Folder) APILocator.getFolderAPI().find(parent, user, false);

			//check permissions
			_checkUserPermissions(folder, user, PERMISSION_CAN_ADD_CHILDREN);

			String fileNamesStr = ParamUtil.getString(req, "fileNames");
			if(!UtilMethods.isSet(fileNamesStr))
				throw new ActionException(LanguageUtil.get(user, "message.file_asset.alert.please.upload"));

			String selectedStructureInode = ParamUtil.getString(req, "selectedStructure");
			if(!UtilMethods.isSet(selectedStructureInode))
				selectedStructureInode = StructureCache.getStructureByVelocityVarName(FileAssetAPI.DEFAULT_FILE_ASSET_STRUCTURE_VELOCITY_VAR_NAME).getInode();

			String[] fileNamesArray = fileNamesStr.split(WebKeys.CONTENTLET_FORM_NAME_VALUE_SEPARATOR);
			String customMessage = LanguageUtil.get(user, "message.file_asset.error.filename.filters"+" : ");
			if(fileNamesArray.length > 2)
				SessionMessages.add(req, "custommessage", LanguageUtil.get(user, "message.contentlets.batch.reindexing.background"));
			boolean filterError = false;

			ContentletAPI conAPI = APILocator.getContentletAPI();
			List<String> existingFileNames = new ArrayList<String>();
			for (int k=0;k<fileNamesArray.length;k++) {

				Contentlet contentlet = new Contentlet();
				contentlet.setStructureInode(selectedStructureInode);
				contentlet.setHost(APILocator.getHostAPI().findParentHost(folder, user, false).getIdentifier());
				contentlet.setFolder(folder.getInode());
				String fileName = fileNamesArray[k];
				String title = getFriendlyName(fileName);

				fileName = checkMACFileName(fileName);

				if(!APILocator.getFolderAPI().matchFilter(folder,fileName))
	            {
				   customMessage += fileName + ", ";
	               filterError = true;
	               continue;
	            }

				if (fileName.length()>0) {

					//checks if another identifier with the same name exists in the same folder
					if ((APILocator.getFileAPI().fileNameExists(folder,fileName))) {
						existingFileNames.add(fileName);
					}
					else {
						//sets filename for this new file

						contentlet.setStringProperty("title", title);
						contentlet.setStringProperty("fileName", fileName);
						java.io.File uploadedFile = uploadReq.getFile(fileName);
						contentlet.setBinary("fileAsset", uploadedFile);
						try {
							contentlet = APILocator.getContentletAPI().checkin(contentlet, user, false);
							if ((subcmd != null) && subcmd.equals(com.dotmarketing.util.Constants.PUBLISH)) {
								APILocator.getVersionableAPI().setLive(contentlet);
							}
							HibernateUtil.commitTransaction();

							if(InodeUtils.isSet(contentlet.getInode()) && !conAPI.isInodeIndexed(contentlet.getInode())){
								Logger.error(this, "Timed Out waiting for index to return");
							}
						} catch (Exception e) {
							Logger.error(this, e.getMessage());
							SessionMessages.add(req, "error", e.getMessage());
						}
					}

				}
			}

			if(!existingFileNames.isEmpty()){
				StringBuffer messageText = new StringBuffer();
				if(existingFileNames.size()>1){
					messageText.append("The uploaded files ");
				}else{
					messageText.append("The uploaded file ");
				}

				for(int i=0;i<existingFileNames.size();i++){
					if(i==0){
						messageText.append(existingFileNames.get(i));
					}else{
						messageText.append(", "  + existingFileNames.get(i));
					}
				}
				if(existingFileNames.size()>1){
					messageText.append(" already exist in this folder");
				}else{
					messageText.append(" already exists in this folder");
				}

				SessionMessages.add(req, "custommessage", messageText.toString());
			}


			if(filterError)
			{
				customMessage = customMessage.substring(0,customMessage.lastIndexOf(","));
				SessionMessages.add(req, "custommessage",customMessage);
			}

		}
		catch (IOException e) {
			Logger.error(this, "Exception saving file: " + e.getMessage());
			throw new ActionException(e.getMessage());
		}
}
}