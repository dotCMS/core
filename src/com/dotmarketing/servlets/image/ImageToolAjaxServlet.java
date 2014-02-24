package com.dotmarketing.servlets.image;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.dotcms.enterprise.LicenseUtil;
import com.dotcms.util.SecurityUtils;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.cache.WorkingCache;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;
import com.dotmarketing.portlets.files.business.FileAPI;
import com.dotmarketing.portlets.files.model.File;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.servlets.AjaxFileUploadListener;
import com.dotmarketing.servlets.AjaxFileUploadListener.FileUploadStats;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Constants;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.model.User;
import com.liferay.util.FileUtil;

/**
 * This servlet resize an image proportionally without placing that image into a
 * box background. The image generated is with the .png extension
 * 
 * @author WE
 * 
 */
public class ImageToolAjaxServlet extends HttpServlet {
    /**
     * serialVersionUID
     */
    private static final long serialVersionUID = 1L;
    private static PermissionAPI permissionAPI = APILocator.getPermissionAPI();

    /* This is a thread safe date formatter */
       /**
     * resize an image proportionally without placing that image into a box
     * background. The image generated is with the .png extension
     */
    public void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    	
    	
    	
        User user = null;
		try {
			user = com.liferay.portal.util.PortalUtil.getUser(request);
		} catch (Exception e) {
			Logger.warn(this.getClass(), "Unauthorized access to ImageToolAjax from IP + "+ request.getRemoteAddr() +", no user found");
		} 
	    if(user ==null || "100".equals(LicenseUtil.getLevel())){
	    	response.getWriter().println("Unauthorized");
	    	return;
	    }

    	
    	
    	
    	String fileUrl = request.getParameter("fileUrl");
    	String inode = request.getParameter("inode");
    	String action = request.getParameter("action");
    	String fileName = request.getParameter("fileName");
    	String binaryFieldId = request.getParameter("binaryFieldId");
    	
    	
    	if("reset".equals(action)){
			request.getSession().removeAttribute(WebKeys.IMAGE_TOOL_SAVE_FILES);
    		return ;
    	}else if("download".equals(action)){
    		doDownload(fileUrl, response);
    		return ;
    	}
		else if("saveAs".equals(action)){
    		doSaveAs(request, response, inode, fileName);
    	}
    	else if("save".equals(action)){
    		doSave(request, response, inode, binaryFieldId, fileName);
    	}
    	else if("setClipboard".equals(action)){
    		setClipboard(fileUrl, request, response);
    	}
    	else if("getClipboard".equals(action)){
    		getClipboard(fileUrl, request, response);
    	}
   
    }
    
    
    private void setClipboard(String fileUrl, HttpServletRequest request, HttpServletResponse response) throws IOException{
    	
    	List<String> list = (List<String>) request.getSession().getAttribute(WebKeys.IMAGE_TOOL_CLIPBOARD);
    	if(list ==null){
    		list = new ArrayList<String>();
    	}
    	// we only show nine images in clipboard
    	if(list.size()>8){
    		list = list.subList(0, 8);
    	}

    	if(list.contains(fileUrl)){
    		list.remove(fileUrl);
    	}
    	list.add(0, fileUrl);
    	
    	
    	request.getSession().setAttribute(WebKeys.IMAGE_TOOL_CLIPBOARD, list);
    	

    	
    	response.getWriter().println("success");
    	response.getWriter().println(fileUrl);
		return;
    }
    private void getClipboard(String fileUrl, HttpServletRequest request, HttpServletResponse response) throws IOException{
    	List<String> list = (List<String>) request.getSession().getAttribute(WebKeys.IMAGE_TOOL_CLIPBOARD);
    	if(list ==null){
    		list = new ArrayList<String>();
    	}
		return;
    }
    
    private void doDownload(String fileUrl, HttpServletResponse response) throws IOException{
		fileUrl+=(fileUrl.indexOf("?") < 0) ? "?":"&"; 
		fileUrl+= "force_download=true&r" +new Random( 1756547574 ).nextInt();
		System.out.println(fileUrl);
		response.sendRedirect(SecurityUtils.stripReferer(fileUrl));
		return;
    	
    	
    	
    }
    
    private void doSave(HttpServletRequest request,HttpServletResponse response, String inode, String fieldId, String fileName) throws IOException{
    	
    	
		String userId = request.getSession().getAttribute("USER_ID").toString();
		//see if we have anything in session from the image tool
		@SuppressWarnings("unchecked")
		java.io.File binaryFile = null;
		Map<String, String> imgToolFile = (Map<String, String>) request.getSession().getAttribute(WebKeys.IMAGE_TOOL_SAVE_FILES);
    	if(imgToolFile != null){
			String x = imgToolFile.get(fieldId);
			if( x != null){
				binaryFile = new java.io.File(x);
				if(binaryFile != null && binaryFile.exists() && binaryFile.length()>0){
					java.io.File tempUserFolder = new java.io.File(APILocator.getFileAPI().getRealAssetPathTmpBinary() + java.io.File.separator + userId + 
							java.io.File.separator + fieldId);
					if (!tempUserFolder.exists())
						tempUserFolder.mkdirs();
					
					java.io.File dest = new java.io.File(tempUserFolder.getCanonicalPath() + java.io.File.separator + fileName);
					if(dest.exists())
						dest.delete();
					
					FileUtil.copyFile(binaryFile, dest);
					binaryFile.delete();
					AjaxFileUploadListener listener = new AjaxFileUploadListener(dest.length());
					FileUploadStats fus = listener.getFileUploadStats();
					fus.setBytesRead(dest.length());
					fus.setCurrentStatus("done");
					request.getSession().setAttribute("FILE_UPLOAD_STATS_" + fieldId, null);
					response.getWriter().println("success.  File Saved");
				}
			}
		}
    	
    	
    	
    	
    }
    
    private void doSaveAs(HttpServletRequest request, HttpServletResponse response, String inode, String fileName){
    	
		if(!UtilMethods.isSet(inode)){
			throw new DotStateException("Cannot find underlying file to 'save as' ");
		}
    	User user;
		try {
			
	    	Map<String, String> imgToolFile = (Map<String, String>) request.getSession().getAttribute(WebKeys.IMAGE_TOOL_SAVE_FILES);
			if(imgToolFile  ==null){
				throw new DotStateException("Cannot find underlying file to 'save as' ");
			}
			java.io.File saveAsIOFile = null;
			if(imgToolFile != null){
				
				for (Map.Entry<String, String> entry : imgToolFile.entrySet()){
					if(WebKeys.EDITED_IMAGE_FILE_ASSET.equals(entry.getKey())){
						saveAsIOFile = new java.io.File(entry.getValue());
						request.getSession().removeAttribute(WebKeys.IMAGE_TOOL_SAVE_FILES);
						break;
					}
				}
			}
			if(saveAsIOFile ==null || ! saveAsIOFile.exists() || saveAsIOFile.length() <60){
				response.getWriter().println("failure.  No Save as file found");
				return;
			}
			java.io.File temp = java.io.File.createTempFile(saveAsIOFile.getName(),UtilMethods.getFileExtension(saveAsIOFile.getName()));
			temp.deleteOnExit();
			FileUtil.copyFile(saveAsIOFile, temp);
			
			
			FileAPI fAPI = APILocator.getFileAPI();
			user = WebAPILocator.getUserWebAPI().getLoggedInUser(request);
			Identifier ident = APILocator.getIdentifierAPI().findFromInode(inode);
		    if(ident!=null && InodeUtils.isSet(ident.getId()) && ident.getAssetType().equals("contentlet")){
		    	Contentlet cont = APILocator.getContentletAPI().findContentletByIdentifier(ident.getId(), true, APILocator.getLanguageAPI().getDefaultLanguage().getId(), user, false);
		    	if(cont!=null && InodeUtils.isSet(cont.getInode())){
		    		Host h = APILocator.getHostAPI().find(cont.getHost(), user, false);
					Folder folder = APILocator.getFolderAPI().findFolderByPath(ident.getParentPath(), h, user, false);
					if(APILocator.getFileAssetAPI().fileNameExists(h, folder, fileName, cont.getIdentifier())){
						response.getWriter().println("failure.  fileAlreadyExists");
						return;
					}
					Contentlet fileAsset = new Contentlet();
					fileAsset.setStructureInode(folder.getDefaultFileType());
					fileAsset.setStringProperty(FileAssetAPI.TITLE_FIELD, UtilMethods.getFileName(fileName));
					fileAsset.setFolder(folder.getInode());
					fileAsset.setHost(h.getIdentifier());
					fileAsset.setBinary(FileAssetAPI.BINARY_FIELD, temp);
					APILocator.getContentletAPI().checkin(fileAsset, user,false);
		    	}
		    	
		    }else{
		    	File src = fAPI.get(inode,user, false );
		    	Identifier fileId = APILocator.getIdentifierAPI().find(src.getIdentifier());
				Host h = APILocator.getHostAPI().find(fileId.getHostId(), user, false);
				Folder folder = APILocator.getFileAPI().getFileFolder(src, h, user, false);
				File copiedFile = new File();
				copiedFile.setFileName(fileName);
				copiedFile.setAuthor(user.getFullName());
				copiedFile.setModUser(user.getUserId());
				copiedFile.setFriendlyName(src.getFriendlyName());
				copiedFile.setMimeType(APILocator.getFileAPI().getMimeType(fileName));
				String x = WorkingCache.getPathFromCache(APILocator.getIdentifierAPI().find(folder).getPath() + fileName, h);

				
				
				if(UtilMethods.isSet(x)){
					response.getWriter().println("failure.  fileAlreadyExists");
					
					return;
				}
				


				fAPI.saveFile(copiedFile, temp, folder, user, false);

		    }
		
	
			response.getWriter().println("success.  File Saved");
			
			return;
		} catch (Exception e) {
			Logger.error(this.getClass(), e.getMessage());
			Logger.debug(this.getClass(), e.getMessage(), e);
			throw new DotStateException("Error in 'save as' " +e.getMessage() );
		}
		finally{
			request.getSession().removeAttribute(WebKeys.IMAGE_TOOL_SAVE_FILES);
		}
		
    	
    }
}
