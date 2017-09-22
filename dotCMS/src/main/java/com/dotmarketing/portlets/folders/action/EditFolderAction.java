package com.dotmarketing.portlets.folders.action;

import static com.dotmarketing.business.PermissionAPI.PERMISSION_READ;

import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import com.dotcms.repackage.javax.portlet.ActionRequest;
import com.dotcms.repackage.javax.portlet.ActionResponse;
import com.dotcms.repackage.javax.portlet.PortletConfig;
import com.dotcms.repackage.javax.portlet.WindowState;
import com.dotcms.repackage.org.apache.commons.beanutils.BeanUtils;
import com.dotcms.repackage.org.apache.struts.action.ActionForm;
import com.dotcms.repackage.org.apache.struts.action.ActionMapping;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.Inode;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Treeable;
import com.dotmarketing.business.Versionable;

import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.factories.InodeFactory;
import com.dotmarketing.menubuilders.RefreshMenus;
import com.dotmarketing.portal.struts.DotPortletAction;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.folders.business.AddContentToFolderPermissionException;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.business.FolderFactory;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.folders.struts.FolderForm;
import com.dotmarketing.portlets.links.model.Link;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.User;
import com.liferay.portal.util.Constants;
import com.liferay.portlet.ActionRequestImpl;
import com.liferay.util.servlet.SessionMessages;

/**
 * This Action class provides utility methods to interact with {@link Folder}
 * objects from the back-end of dotCMS. Folder-related operations originated 
 * from the Site Browser portlet will make their way to this class.
 * 
 * @author root
 * @version 1.0
 * @since Mar 22, 2012
 *
 */
public class EditFolderAction extends DotPortletAction {

	private FolderAPI folderAPI = APILocator.getFolderAPI();
	private HostAPI hostAPI = APILocator.getHostAPI();
	
	private static final int MAX_FOLDER_PATH_LENGTH = 255;
	private static final int MAX_FOLDER_NAME_LENGTH = 255;

	/**
	 * 
	 */
	public void processAction(ActionMapping mapping, ActionForm form,
			PortletConfig config, ActionRequest req, ActionResponse res)
			throws Exception {

		// wraps request to get session object
		ActionRequestImpl reqImpl = (ActionRequestImpl) req;
		HttpServletRequest httpReq = reqImpl.getHttpServletRequest();

		String cmd = req.getParameter(Constants.CMD);

		User user = _getUser(req);

		// Referer
        String referer = "";
        if (UtilMethods.isSet(req.getParameter("referer"))) {
            referer = req.getParameter("referer");
        } else {
    		java.util.Map<String, String[]> params = new java.util.HashMap<String, String[]>();
    		params.put("struts_action",
    				new String[] { "/ext/folders/view_folders" });
    		params.put("openNodes", new String[] { req.getParameter("openNodes") });
    		params.put("view", new String[] { req.getParameter("view") });
    		params.put("content", new String[] { req.getParameter("content") });
    
    		referer = com.dotmarketing.util.PortletURLUtil.getActionURL(
    				httpReq, WindowState.MAXIMIZED.toString(), params);
        }
        
		Logger.debug(this, "EditFolderAction cmd=" + cmd);

		String forward = "portlet.ext.folders.edit_folder";



		try {
			_editFolder(req, res, config, form);

		} catch (Exception ae) {
			_handleException(ae, req);
			SessionMessages.add(req, "message", "message.folder.refresh");
			_sendToReferral(req, res, referer);
			return;
		}

		/*
		 * We are editing the folder
		 */
		if ((cmd != null) && cmd.equals(Constants.ADD)) {
			try {
				if (_updateFolder(req, res, config, form)) {
					HibernateUtil.closeAndCommitTransaction();
					_sendToReferral(req, res, referer);
					return;
				}

			} catch(AddContentToFolderPermissionException e){
				req.setAttribute(WebKeys.FOLDER_EDIT, req.getAttribute(WebKeys.PARENT_FOLDER));
				_sendToReferral(req, res, referer);
				return;
			}catch (Exception ae) {
				_handleException(ae, req);
				return;
			}
		}

		/*
		 * If we are deleting the folder, run the delete action and return
		 * to the referer
		 * 
		 */
		else if ((cmd != null) && cmd.equals(Constants.DELETE)) {
			try {
				_deleteFolder(req, res, config, form);
				_sendToReferral(req, res, referer);
			} catch (Exception ae) {
				_handleException(ae, req);
				return;
			}
			return;
		}
		/*
		 * If we are copying the folder, run the copy action and return to the
		 * list
		 */
		else if ((cmd != null)
				&& cmd.equals(com.dotmarketing.util.Constants.COPY)) {
			try {
				Logger.debug(this, "Calling Copy Method");
				_copyFolder(req, res, config, form, user);
			} catch (Exception ae) {
				_handleException(ae, req);
			}
			_sendToReferral(req, res, referer);
			return;
		}
		/*
		 * If we are moving the html page, run the copy action and return to the
		 * list
		 */
		else if ((cmd != null)
				&& cmd.equals(com.dotmarketing.util.Constants.MOVE)) {
			try {
				Logger.debug(this, "Calling Move Method");
				_moveFolder(req, res, config, form, user);
			} catch (Exception ae) {
				_handleException(ae, req);
			}
			_sendToReferral(req, res, referer);
			return;
		}
		else if ((cmd != null) && cmd.equals(com.dotmarketing.util.Constants.RENAME)) 
		{
			String newName = "newRenameNew";			
			Folder folder = APILocator.getFolderAPI().find(req.getParameter("inode"), user, false);
			folder.setName(newName);
			APILocator.getFolderAPI().save(folder, user, false);
			_sendToReferral(req, res, referer);
			return;
		}

		BeanUtils.copyProperties(form, req.getAttribute(WebKeys.FOLDER_EDIT));
		HibernateUtil.closeAndCommitTransaction();
		setForward(req, forward);
	}

	// /// ************** ALL METHODS HERE *************************** ////////

	/**
	 * 
	 * @param req
	 * @param res
	 * @param config
	 * @param form
	 * @throws Exception
	 */
	public void _editFolder(ActionRequest req, ActionResponse res,
			PortletConfig config, ActionForm form) throws Exception {
		User user=_getUser(req);
		
		String inode=req.getParameter("inode");
		
		Folder f;
		if(UtilMethods.isSet(inode)) {
			f = APILocator.getFolderAPI().find(inode, user, false);
		}
		else {
			// it is a new folder
			f = new Folder();
		}
		
		req.setAttribute(WebKeys.FOLDER_EDIT, f);

		// Checking permissions		
		_checkUserPermissions(f, user, PERMISSION_READ);

		// parent folder
		if (!InodeUtils.isSet(f.getInode())) {
			String parentFolderInode = req.getParameter("pfolderId");
			String parentHostId = req.getParameter("phostId");
			if(UtilMethods.isSet(parentFolderInode)) {
				Folder parent = folderAPI.find(parentFolderInode,user,false);
				req.setAttribute(WebKeys.FOLDER_PARENT, parent);
        		Host parentHost = hostAPI.findParentHost(parent, user, false);
				req.setAttribute(WebKeys.HOST_PARENT, parentHost);
			} else {
				req.setAttribute(WebKeys.FOLDER_PARENT, null);
				req.setAttribute(WebKeys.HOST_PARENT, hostAPI.find(parentHostId, user, false));
			}
		} else {
	        Folder parentFolder = folderAPI.findParentFolder(f,user,false); 
	        	//(Folder) InodeFactory.getParentOfClass(f, Folder.class);
	        
	        if(parentFolder != null && InodeUtils.isSet(parentFolder.getInode())) {
	        	req.setAttribute(WebKeys.FOLDER_PARENT, parentFolder);
        		Host parentHost = hostAPI.findParentHost(parentFolder, user, false);
				req.setAttribute(WebKeys.HOST_PARENT, parentHost);
	        } else {
				req.setAttribute(WebKeys.FOLDER_PARENT, null);
        		Host parentHost = hostAPI.findParentHost(f, user, false);
        		req.setAttribute(WebKeys.HOST_PARENT, parentHost);
	        }
		}
				
        //show on menu
		req.setAttribute(WebKeys.FOLDER_SHOWMENU, new Boolean(f.isShowOnMenu()));

	}

	/**
	 * Updates or adds the information and configuration parameters of a
	 * {@link Folder} object. There's a limitation of 255 characters for the
	 * folder name, and 255 characters for the parent path of a folder or asset.
	 * User and developers need to take this into consideration when adding or
	 * changing folder names in dotCMS.
	 * 
	 * @param req
	 *            - The Struts wrapper of the HTTP Request object.
	 * @param res
	 *            - The Struts wrapper of the HTTP Response object.
	 * @param config
	 *            - The configuration parameters of the Liferay portlet.
	 * @param form
	 *            - The Folder data that was added or updated by the user.
	 * @return If <code>true</code>, the Folder was successfully added or
	 *         updated. If an error occurred during the process, returns
	 *         <code>false</code>.
	 * @throws Exception
	 *             An error occurred when updating or adding the Folder
	 *             information.
	 */
	public boolean _updateFolder(ActionRequest req, ActionResponse res,PortletConfig config, ActionForm form) throws Exception 
	{
		Folder f = (Folder) req.getAttribute(WebKeys.FOLDER_EDIT);
		Folder parentFolder = (Folder) req.getAttribute(WebKeys.FOLDER_PARENT);
		Host parentHost = (Host) req.getAttribute(WebKeys.HOST_PARENT);
		User user = _getUser(req);

		PermissionAPI permAPI = APILocator.getPermissionAPI();
		if(!InodeUtils.isSet(f.getInode()) && parentFolder != null && !permAPI.doesUserHavePermission(parentFolder, PermissionAPI.PERMISSION_CAN_ADD_CHILDREN, user)){
			if(!InodeUtils.isSet(f.getInode()) && parentHost != null && !permAPI.doesUserHavePermission(parentHost, PermissionAPI.PERMISSION_CAN_ADD_CHILDREN, user)){
				throw new DotSecurityException("You don't have permissions to add this folder");
			}
		} else if(InodeUtils.isSet(f.getInode()) && !permAPI.doesUserHavePermission(f, PermissionAPI.PERMISSION_EDIT, user)){
			throw new DotSecurityException("You don't have permissions to edit this folder");
		}

		FolderForm folderForm = (FolderForm) form;
		// the folders is new or there are changes
		if(UtilMethods.isSet(f.getIdentifier()) && f.equals(folderForm)){
			// no changes to save, the message is shown and nothing saved.
			SessionMessages.add(req, "message", "message.folder.nochanges");
			return true;
		}
		String parentPath = parentFolder != null ? parentFolder.getPath() : "/";
		HibernateUtil.startTransaction();
		try {
			if (folderForm.getName().length() >= MAX_FOLDER_NAME_LENGTH) {
				SessionMessages.add(req, "message", "message.folder.namelength");
				return false;
			}
			String newPath = parentPath + folderForm.getName();
			if (newPath.length() >= MAX_FOLDER_PATH_LENGTH) {
				SessionMessages.add(req, "message", "message.folder.pathlength");
				return false;
			}
			if (InodeUtils.isSet(f.getInode()) && !folderForm.getName().equalsIgnoreCase(f.getName())) {
				if (!folderAPI.renameFolder(f,folderForm.getName(),user,false)) {
					// For messages to be displayed on messages page
					SessionMessages.add(req, "message", "message.folder.alreadyexists");
					return false;
				}
			}
			
			f.setName(folderForm.getName());
			f.setTitle(folderForm.getTitle());
			f.setFilesMasks(folderForm.getFilesMasks());
			f.setShowOnMenu(folderForm.isShowOnMenu());
			f.setSortOrder(folderForm.getSortOrder());
			f.setOwner(folderForm.getOwner());
			String defaultFileType = req.getParameter("defaultFileType");
			if(!InodeUtils.isSet(defaultFileType)){
				SessionMessages.add(req, "message", "message.folder.defaultfiletype.required");
				return false;
			}
			Structure defaultStr = CacheLocator.getContentTypeCache().getStructureByInode(defaultFileType);
			if(defaultStr==null || !InodeUtils.isSet(defaultStr.getInode())){
				SessionMessages.add(req, "message", "message.folder.defaultfiletype.required");
				return false;
			}
			f.setDefaultFileType(defaultFileType);
			
			java.util.List<String> reservedFolderNames = new java.util.ArrayList<String>();
			String[] reservedFolderNamesArray = Config.getStringArrayProperty("RESERVEDFOLDERNAMES");
			for(String name:reservedFolderNamesArray){
				reservedFolderNames.add(name.toUpperCase());
			}
			
			if (parentFolder == null && reservedFolderNames.contains(f.getName().toUpperCase())) {
				// For messages to be displayed on messages page
				SessionMessages.add(req, "message",
				"message.folder.save.reservedName");
			} else {
				if(!InodeUtils.isSet(f.getInode())) {
					// check if the new folder already exists
					Identifier prevId = null;
					if (parentFolder != null) {
						String uri=APILocator.getIdentifierAPI().find(parentFolder).getPath()+ f.getName();
						prevId = APILocator.getIdentifierAPI().find(parentHost, uri);
					} else {
						String uri="/" + f.getName();
						prevId = APILocator.getIdentifierAPI().find(parentHost, uri);
					}
				
					if (InodeUtils.isSet(prevId.getInode())) {
						// For messages to be displayed on messages page
						SessionMessages.add(req, "error",
						"message.named.asset.alreadyexists");
						return false;
					}
				}
				boolean previousShowMenu = ((Boolean) req
						.getAttribute(WebKeys.FOLDER_SHOWMENU)).booleanValue();
				
				if (parentFolder instanceof Folder){
					if(f.getName().equalsIgnoreCase("admin")){
							
					SessionMessages.add(req, "message",
					"message.folder.admin.doesnotallow");
					return false;
					}
				}					
				if(!InodeUtils.isSet(f.getInode())){
					f.setOwner(_getUser(req).getUserId());
				}
				
				//set hostId to folder to persist in Identifier table.
				f.setHostId(parentHost.getIdentifier());
				final boolean isNew=!UtilMethods.isSet(f.getIdentifier());
				if(isNew) {
					Treeable parent;
					if(UtilMethods.isSet(parentFolder))
						parent=parentFolder;
					else
						parent=parentHost;
					
					Identifier id=APILocator.getIdentifierAPI().createNew(f, parent);
					f.setIdentifier(id.getId());
				}
				
				folderAPI.save(f,user,false);
				if (!(!previousShowMenu && !f.isShowOnMenu())) 
				{
					//if the not, doesn't show before and doesn't show now, delete the menus
					RefreshMenus.deleteMenu(f);
					CacheLocator.getNavToolCache().removeNav(f.getHostId(), f.getInode());
					Identifier ident=APILocator.getIdentifierAPI().find(f);
					CacheLocator.getNavToolCache().removeNavByPath(ident.getHostId(), ident.getParentPath());
				}
				CacheLocator.getIdentifierCache().removeFromCacheByIdentifier(f.getIdentifier());
				// For messages to be displayed on messages page
				SessionMessages.add(req, "message", "message.folder.save");
				return true;
			}
			HibernateUtil.closeAndCommitTransaction();
		}catch(AddContentToFolderPermissionException e){
			HibernateUtil.rollbackTransaction();
			String message = LanguageUtil.format(user.getLocale(),
					"message.folder.save.error.permission",new String[]{e.getUserId(), e.getFolderPath()}, false);
			SessionMessages.add(req, "message", message);
			Logger.error(this, e.getMessage(), e);
			throw e;
		} catch(Exception ex) {
			HibernateUtil.rollbackTransaction();
			SessionMessages.add(req, "message", "message.folder.save.error");
			Logger.error(this, "ERROR: Cannot save folder '" + parentPath + folderForm.getName() + "'", ex);
		}
		finally {
			HibernateUtil.closeSession();
		}
		return false;
	}

	/**
	 * 
	 * @param req
	 * @param res
	 * @param config
	 * @param form
	 * @throws Exception
	 */
	public void _deleteFolder(ActionRequest req, ActionResponse res,
			PortletConfig config, ActionForm form) throws Exception {

		Folder f = (Folder) req.getAttribute(WebKeys.FOLDER_EDIT);

		// wraps request to get session object
		ActionRequestImpl reqImpl = (ActionRequestImpl) req;
		HttpServletRequest httpReq = reqImpl.getHttpServletRequest();
		// gets the session object for the messages
		HttpSession session = httpReq.getSession();

		String selectedFolder = ((String) session
				.getAttribute(com.dotmarketing.util.WebKeys.FOLDER_SELECTED) != null) ? (String) session
				.getAttribute(com.dotmarketing.util.WebKeys.FOLDER_SELECTED)
				: "";

				
				
				


		session.removeAttribute(com.dotmarketing.util.WebKeys.FOLDER_SELECTED);
		User user = _getUser(req);
		folderAPI.delete(f, user,false);


		// For messages to be displayed on messages page
		SessionMessages.add(req, "message", "message.folder.delete");

	}

	/**
	 * 
	 * @param folder
	 * @param objectsList
	 * @throws DotDataException
	 * @throws DotStateException
	 * @throws DotSecurityException
	 */
	public static void _deleteChildrenAssetsFromFolder(Folder folder,	Set<Inode> objectsList) throws DotDataException, DotStateException, DotSecurityException {

		PermissionAPI perAPI =  APILocator.getPermissionAPI();
		FolderAPI fapi = APILocator.getFolderAPI();
		
		/****** begin *************/

		List<Link> links = fapi.getLinks(folder,APILocator.getUserAPI().getSystemUser(),false);
		for (Link link: links) {		
			if (link.isWorking()) {

				Identifier identifier = APILocator.getIdentifierAPI().find(link);

                if(!InodeUtils.isSet(identifier.getInode())) {
                    Logger.warn(FolderFactory.class, "_deleteChildrenAssetsFromFolder: link inode = " + link.getInode() + 
                            " doesn't have a valid identifier associated.");
                    continue;
                }

                perAPI.removePermissions(link);

	            List<Versionable> versions =APILocator.getVersionableAPI().findAllVersions(identifier, APILocator.getUserAPI().getSystemUser(), false);
	            
	            for (Versionable version : versions) {
					new HibernateUtil().delete(version);
	            }
	            APILocator.getIdentifierAPI().delete(identifier);
			}
		}
		
		
		/************ end *****************/
	}

	// Copy action
	/**
	 * 
	 * @param req
	 * @param res
	 * @param config
	 * @param form
	 * @param user
	 * @return
	 * @throws Exception
	 */
	public boolean _copyFolder(ActionRequest req, ActionResponse res,
			PortletConfig config, ActionForm form, User user) throws Exception {
		String parentInode = req.getParameter("parent");
		String inode = req.getParameter("inode");
		Folder current = (Folder) InodeFactory.getInode(inode, Folder.class);

		if (APILocator.getFolderAPI().find(parentInode,user,false) != null) {
			Host parentHost = hostAPI.find(parentInode, user, false); 
			APILocator.getFolderAPI().copy(current, parentHost,user,false);
		} else {
			Folder parentFolder = folderAPI.find(parentInode,user,false);
			APILocator.getFolderAPI().copy(current, parentFolder,user,false);
		}
		return true;
	}

	// Move action
	/**
	 * 
	 * @param req
	 * @param res
	 * @param config
	 * @param form
	 * @param user
	 * @return
	 * @throws Exception
	 */
	public boolean _moveFolder(ActionRequest req, ActionResponse res,
			PortletConfig config, ActionForm form, User user) throws Exception {
		String parentInode = req.getParameter("parent");
		String inode = req.getParameter("inode");
		Folder current = (Folder) InodeFactory.getInode(inode, Folder.class);

		if (!folderAPI.exists(parentInode)) {
			Host parentHost = hostAPI.find(parentInode, user, false); 
			if (!folderAPI.move(current, parentHost,user,false)) {
				SessionMessages.add(req, "error",
						"message.folder.error.foldername.exists");
				return false;
			}
		} else {
			Folder parentFolder = folderAPI.find(parentInode,user,false);
			if (parentFolder.getInode().equalsIgnoreCase(current.getInode())) {
				SessionMessages.add(req, "message",
					"message.folder.isthesame");
				return false;
			}
			if (folderAPI.isChildFolder(parentFolder, current)) {
				SessionMessages.add(req, "message",
						"message.folder.ischildfolder");
				return false;
			}
			if (!folderAPI.move(current, parentFolder,user,false)) {
				SessionMessages.add(req, "error",
						"message.folder.error.foldername.exists");
				return false;
			}
		}
		return true;
	}

}
