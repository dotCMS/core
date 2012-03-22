/*
 * Created on Jun 25, 2004
 *
 */
package com.dotmarketing.portal.struts;

import static com.dotmarketing.business.PermissionAPI.PERMISSION_CAN_ADD_CHILDREN;
import static com.dotmarketing.business.PermissionAPI.PERMISSION_PUBLISH;
import static com.dotmarketing.business.PermissionAPI.PERMISSION_READ;
import static com.dotmarketing.business.PermissionAPI.PERMISSION_WRITE;

import java.util.ArrayList;
import java.util.List;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.PortletConfig;
import javax.portlet.RenderRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.jsp.PageContext;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

import antlr.Utils;
import bsh.util.Util;

import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.Inode;
import com.dotmarketing.beans.PermissionAsset;
import com.dotmarketing.beans.WebAsset;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.IdentifierFactory;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.Versionable;
import com.dotmarketing.cms.factories.PublicCompanyFactory;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.exception.WebAssetException;
import com.dotmarketing.factories.InodeFactory;
import com.dotmarketing.factories.PublishFactory;
import com.dotmarketing.factories.WebAssetFactory;
import com.dotmarketing.portlets.categories.business.CategoryAPI;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PaginatedArrayList;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.NoSuchUserException;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.Company;
import com.liferay.portal.model.User;
import com.liferay.portal.struts.ActionException;
import com.liferay.portal.struts.PortletAction;
import com.liferay.portal.util.Constants;
import com.liferay.portlet.ActionRequestImpl;
import com.liferay.util.servlet.SessionMessages;

/**
 * @author Maria
 *
 */
public class DotPortletAction extends PortletAction {

	private CategoryAPI categoryAPI = APILocator.getCategoryAPI();
	private HostAPI hostAPI = APILocator.getHostAPI();
	private final static int MAX_LIMIT_COUNT = 101;
	
	
	/**
	 * Generic method to delete a WebAsset version
	 *
	 * @param req
	 * @param res
	 * @param config
	 * @param form
	 * @param user
	 * @param webKeyEdit
	 * @throws Exception
	 */
	public void _deleteVersionWebAsset(ActionRequest req, ActionResponse res, PortletConfig config, ActionForm form, User user, String webKeyEdit)
	throws Exception {

		// wraps request to get session object
		ActionRequestImpl reqImpl = (ActionRequestImpl) req;
		HttpServletRequest httpReq = reqImpl.getHttpServletRequest();

		WebAsset webAsset = (WebAsset) req.getAttribute(webKeyEdit);
	
		PermissionAPI perAPI = APILocator.getPermissionAPI();

		// Checking permissions
		if (!perAPI.doesUserHavePermission(webAsset, PERMISSION_WRITE, user)) {
			Logger.debug(DotPortletAction.class, "_checkUserPermissions: user does not have permissions ( " + PERMISSION_WRITE + " ) over this asset: " + webAsset);
			List<Role> roles = perAPI.getRoles(webAsset.getPermissionId(), PermissionAPI.PERMISSION_PUBLISH, "CMS Owner", 0, -1);
			
			Role cmsOwner = APILocator.getRoleAPI().loadCMSOwnerRole();
			boolean isCMSOwner = false;
			if(roles.size() > 0){
				for (Role role : roles) {
					if(role == cmsOwner){
						isCMSOwner = true;
						break;
					}
				}
				if(!isCMSOwner){
					throw new ActionException(WebKeys.USER_PERMISSIONS_EXCEPTION);
				}
			}else{
				throw new ActionException(WebKeys.USER_PERMISSIONS_EXCEPTION);
			}	
					
		}

		// calls the Contentlet API delete the container version
		try{
			WebAssetFactory.deleteAssetVersion(webAsset);
			SessionMessages.add(httpReq, "message", "message.contentlet.delete");
		}catch(Exception e){
			SessionMessages.add(httpReq, "message", "message.contentlet.delete.live_or_working");
		}
	}

	/**
	 * Generic method to undelete a WebAsset
	 *
	 * @param req
	 * @param res
	 * @param config
	 * @param form
	 * @param user
	 * @param webKeyEdit
	 * @throws Exception
	 */
	public void _undeleteWebAsset(ActionRequest req, ActionResponse res, PortletConfig config, ActionForm form, User user, String webKeyEdit)
	throws Exception {

		// wraps request to get session object
		ActionRequestImpl reqImpl = (ActionRequestImpl) req;
		HttpServletRequest httpReq = reqImpl.getHttpServletRequest();

		WebAsset webAsset = (WebAsset) req.getAttribute(webKeyEdit);

		// Checking permissions
		_checkUserPermissions(webAsset, user, PERMISSION_WRITE);

		WebAssetFactory.unArchiveAsset(webAsset);
		SessionMessages.add(httpReq, "message", "message." + webAsset.getType() + ".undelete");

	}

	/**
	 * Generic method to delete a WebAsset
	 *
	 * @param req
	 * @param res
	 * @param config
	 * @param form
	 * @param user
	 * @param webKeyEdit
	 * @throws Exception
	 */
	public void _deleteWebAsset(ActionRequest req, ActionResponse res, PortletConfig config, ActionForm form, User user, String webKeyEdit)
	throws Exception {
		// wraps request to get session object
		ActionRequestImpl reqImpl = (ActionRequestImpl) req;
		HttpServletRequest httpReq = reqImpl.getHttpServletRequest();

		WebAsset webAsset = (WebAsset) req.getAttribute(webKeyEdit);

		// Checking permissions
		_checkUserPermissions(webAsset, user, PERMISSION_WRITE);

		if (WebAssetFactory.archiveAsset(webAsset, user.getUserId())) {
			SessionMessages.add(httpReq, "message", "message." + webAsset.getType() + ".delete");
		} else {
			SessionMessages.add(httpReq, "message", "message." + webAsset.getType() + ".delete.locked");
		}
	}

	/**
	 * Generic method to unpublish a WebAsset
	 *
	 * @param req
	 * @param res
	 * @param config
	 * @param form
	 * @param user
	 * @param webKeyEdit
	 * @throws Exception
	 */
	public void _unPublishWebAsset(ActionRequest req, ActionResponse res, PortletConfig config, ActionForm form, User user, String webKeyEdit)
	throws Exception {

		// wraps request to get session object
		ActionRequestImpl reqImpl = (ActionRequestImpl) req;
		HttpServletRequest httpReq = reqImpl.getHttpServletRequest();

		WebAsset webAsset = (WebAsset) req.getAttribute(webKeyEdit);

		// Checking permissions
		_checkUserPermissions(webAsset, user, PERMISSION_WRITE);

		Folder parent = APILocator.getFolderAPI().findParentFolder(webAsset,user,false);
		// gets user id from request for mod user
		String userId = user.getUserId();
		if (InodeUtils.isSet(webAsset.getInode())) {
			// calls the asset factory edit
			if (WebAssetFactory.unPublishAsset(webAsset, userId, parent)) {
				SessionMessages.add(httpReq, "message", "message." + webAsset.getType() + ".unpublished");
			} else {
				SessionMessages.add(httpReq, "message", "message." + webAsset.getType() + ".unpublish.notlive_or_locked");
			}
		}

	}

	/**
	 * Generic Method to Get Versions of a WebAsset
	 *
	 * @param req
	 * @param res
	 * @param config
	 * @param form
	 * @param user
	 * @param webKeyEdit
	 * @param webKeyVersions
	 * @throws Exception
	 */
	public void _getVersionsWebAsset(ActionRequest req, ActionResponse res, PortletConfig config, ActionForm form, User user, String webKeyEdit,
			String webKeyVersions) throws Exception {

		WebAsset webAsset = (WebAsset) req.getAttribute(webKeyEdit);

		if (InodeUtils.isSet(webAsset.getInode())) {
			// calls the asset factory to get the version
			req.setAttribute(webKeyVersions, WebAssetFactory.getAssetVersions(webAsset));
		}

	}

	/**
	 * Generic Method to unlock a WebAsset
	 *
	 * @param req
	 * @param res
	 * @param config
	 * @param form
	 * @param user
	 * @param webKey
	 * @throws Exception
	 */
	public void _unLockWebAsset(ActionRequest req, ActionResponse res, PortletConfig config, ActionForm form, User user, String webKey)
	throws Exception {

		// wraps request to get session object
		ActionRequestImpl reqImpl = (ActionRequestImpl) req;
		HttpServletRequest httpReq = reqImpl.getHttpServletRequest();

		WebAsset webAsset = (WebAsset) req.getAttribute(webKey);

		// Checking permissions
		_checkUserPermissions(webAsset, user, PERMISSION_READ);

		if (InodeUtils.isSet(webAsset.getInode())) {
			// calls the asset factory edit
			WebAssetFactory.unLockAsset(webAsset);
			SessionMessages.add(httpReq, "message", "message." + webAsset.getType() + ".unlocked");
		}

		httpReq.getSession().removeAttribute(WebKeys.CONTENTLET_RELATIONSHIPS_EDIT);

	}

	/**
	 * Generic method to move a WebAsset to another Folder
	 *
	 * @param req
	 * @param res
	 * @param config
	 * @param form
	 * @param user
	 * @param webAssetClass
	 * @param webKey
	 * @throws Exception
	 */
	public void _moveWebAsset(ActionRequest req, ActionResponse res, PortletConfig config, ActionForm form, User user, Class webAssetClass,
			String webKey) throws Exception {

		Logger.debug(this, "I'm moving the webasset");

		// wraps request to get session object
		ActionRequestImpl reqImpl = (ActionRequestImpl) req;
		HttpServletRequest httpReq = reqImpl.getHttpServletRequest();

		// gets the current container being edited from the request object
		WebAsset webAsset = (WebAsset) req.getAttribute(webKey);
		Identifier identifier = com.dotmarketing.business.APILocator.getIdentifierAPI().find(webAsset);

		// gets working container
		WebAsset workingWebAsset = (WebAsset) APILocator.getVersionableAPI().findWorkingVersion(identifier,APILocator.getUserAPI().getSystemUser(),false);
		// gets live container
		WebAsset liveWebAsset = (WebAsset) APILocator.getVersionableAPI().findLiveVersion(identifier,APILocator.getUserAPI().getSystemUser(),false);

		// gets folder parent
		String parentInode = req.getParameter("parent");

		if (parentInode != null && parentInode.length() != 0 && !parentInode.equals("")) {
			// the new parent is being passed through the request
			Folder parent = (Folder) InodeFactory.getInode(parentInode, Folder.class);

			// Checking permissions
			_checkCopyAndMovePermissions(webAsset, parent, user, httpReq, "move");

			// gets old parent
			Folder oldParent = APILocator.getFolderAPI().findParentFolder(workingWebAsset, user, false);
			Logger.debug(this, "Old Parent Folder=" + oldParent.getInode());
			oldParent.deleteChild(workingWebAsset);
			if ((liveWebAsset != null) && (InodeUtils.isSet(liveWebAsset.getInode()))) {
				oldParent.deleteChild(liveWebAsset);
			}

			// Adding to new parent
			Logger.debug(this, "Parent Folder=" + parent.getInode());
			parent.addChild(workingWebAsset);
			if ((liveWebAsset != null) && (InodeUtils.isSet(liveWebAsset.getInode()))) {
				parent.addChild(liveWebAsset);
			}

			// gets identifier for this webasset and changes the uri and
			// persists it
			Host newHost = hostAPI.findParentHost(parent, user, false);
			identifier.setHostId(newHost.getIdentifier());
			identifier.setURI(workingWebAsset.getURI(parent));
			APILocator.getIdentifierAPI().save(identifier);

			SessionMessages.add(httpReq, "message", "message." + webAsset.getType() + ".move");
		}

	}

	/**
	 * Generic method to publish a WebAsset
	 *
	 * @param req
	 * @param res
	 * @param config
	 * @param form
	 * @param user
	 * @param formWebKey
	 * @throws Exception
	 */
	public void _publishWebAsset(ActionRequest req, ActionResponse res, PortletConfig config, ActionForm form, User user, String formWebKey)
	throws WebAssetException, Exception {
		try
		{
			// wraps request to get session object
			ActionRequestImpl reqImpl = (ActionRequestImpl) req;
			HttpServletRequest httpReq = reqImpl.getHttpServletRequest();

			WebAsset webAsset = (WebAsset) req.getAttribute(formWebKey);
			Logger.debug(this, "WEB ASSET " + webAsset.getType() + " TO PUBLISH=" + webAsset.getInode());

			// Checking permissions
			_checkUserPermissions(webAsset, user, PERMISSION_PUBLISH);

			if (InodeUtils.isSet(webAsset.getInode())) {
				// calls the asset factory edit
				PublishFactory.publishAsset(webAsset, httpReq);
				SessionMessages.add(httpReq, "message", "message." + webAsset.getType() + ".published");
			}
		}
		catch(ActionException ae)
		{
			if (ae.getMessage().equals(WebKeys.USER_PERMISSIONS_EXCEPTION))
			{
				return;
			}
			else
			{
				throw ae;
			}
		}

	}

	/**
	 * Generic method to retrieve a WebAsset based on the inode on the request
	 *
	 * @param req
	 * @param res
	 * @param config
	 * @param form
	 * @param user
	 * @param myClass
	 * @param webkey
	 * @throws Exception
	 */
	public void _retrieveWebAsset(ActionRequest req, ActionResponse res, PortletConfig config, ActionForm form, User user, Class myClass,
			String webkey) throws Exception {
		WebAsset webAsset;
		
		String inode=req.getParameter("inode");
		
		if(InodeUtils.isSet(inode)) {
			// editing existing asset
			
			Identifier ident=APILocator.getIdentifierAPI().findFromInode(inode);
			webAsset = (WebAsset) APILocator.getVersionableAPI().findWorkingVersion(ident, user, false);
			if(!webAsset.getInode().equals(inode)){
				webAsset = (WebAsset)InodeFactory.getInode(inode, myClass);
			}
			
			// Checking permissions
			_checkUserPermissions(webAsset, user, PERMISSION_READ);

			Logger.debug(this, "webAsset:" + webAsset.toString());
			Logger.debug(this, "webAsset:" + webAsset.getInode());
		}
		else {
			// it is a new one
			webAsset = (WebAsset) myClass.newInstance(); 
		}
		
		req.setAttribute(webkey, webAsset);

		// Asset Versions to list in the versions tab
		req.setAttribute(WebKeys.VERSIONS_INODE_EDIT, webAsset);

	}

	/**
	 * Generic Edit WebAsset method. Still needs to be implemented on the class,
	 * so it's still part of the interface
	 *
	 * @param req
	 * @param res
	 * @param config
	 * @param form
	 * @param user
	 * @param webKey
	 * @return Folder: parent Folder for this WebAsset if the webasset is a
	 *         contentlet, a container or a template it returns a void (inode =
	 *         0) folder.
	 * @throws Exception
	 */
	protected Folder _editWebAsset(ActionRequest req, ActionResponse res, PortletConfig config, ActionForm form, User user, String webKey)
	throws Exception {

		// wraps request to get session object
		ActionRequestImpl reqImpl = (ActionRequestImpl) req;
		HttpServletRequest httpReq = reqImpl.getHttpServletRequest();

		WebAsset webAsset = (WebAsset) req.getAttribute(webKey);

		// Checking permissions
		_checkUserPermissions(webAsset, user, PERMISSION_READ);

		if (InodeUtils.isSet(webAsset.getInode())) {

			// calls the asset factory edit
			boolean editAsset = WebAssetFactory.editAsset(webAsset, user.getUserId());
			if (!editAsset) {
				
				User userMod = null;
				try{
					userMod = APILocator.getUserAPI().loadUserById(webAsset.getModUser(),APILocator.getUserAPI().getSystemUser(),false);
				}catch(Exception ex){
					if(ex instanceof NoSuchUserException){
						try {
							userMod = APILocator.getUserAPI().getSystemUser();
						} catch (DotDataException e) {
							Logger.error(this,e.getMessage(),e);
						}
					}
				}
				if(userMod!=null){
					webAsset.setModUser(userMod.getUserId());
				}

				try {
					Company comp = PublicCompanyFactory.getDefaultCompany();
					String message = LanguageUtil.get(comp.getCompanyId(), user.getLocale(), "message." + webAsset.getType() + ".edit.locked");
					message += " (" + userMod.getEmailAddress() + ")";
					SessionMessages.add(httpReq, "custommessage", message);
				} catch (Exception e) {
					SessionMessages.add(httpReq, "message", "message." + webAsset.getType() + ".edit.locked");
				}

				throw (new ActionException(WebKeys.EDIT_ASSET_EXCEPTION));
			}
		}

		Folder parentFolder = new Folder();
        String parent = req.getParameter("parent");
		if (!(WebAssetFactory.isAbstractAsset(webAsset))) {
			if (InodeUtils.isSet(webAsset.getInode())) {
				parentFolder = APILocator.getFolderAPI().findParentFolder(webAsset, user, false);
			} else if(UtilMethods.isSet(parent)){
				parentFolder = APILocator.getFolderAPI().find(parent, user, false);
			}
		}

		req.setAttribute(webKey, webAsset);

		BeanUtils.copyProperties(form, req.getAttribute(webKey));

		return parentFolder;
	}

	/**
	 * Generic method to get a version back from an asset. Needs to be
	 * implemented on each class for the relations between assets Set in the
	 * <b>webKey </b> the new version created with the working information.
	 *
	 * @param req
	 * @param res
	 * @param config
	 * @param form
	 * @param user
	 * @param className
	 * @param webKey
	 * @return The new working asset
	 * @throws Exception
	 */
	protected WebAsset _getVersionBackWebAsset(ActionRequest req, ActionResponse res, PortletConfig config, ActionForm form, User user,
			Class className, String webKey) throws Exception {

		WebAsset version = (WebAsset) InodeFactory.getInode(req.getParameter("inode_version"), className);

		// Checking permissions
		_checkUserPermissions(version, user, PERMISSION_WRITE);

		WebAsset workingAsset = (WebAsset) WebAssetFactory.getBackAssetVersion(version);

		version = (WebAsset) InodeFactory.getInode(version.getInode(), version.getClass());

		req.setAttribute(webKey, version);

		return workingAsset;

	}

	/**
	 * Generic method for View Assets Actions.
	 *
	 * @param req
	 * @param user
	 * @param className:
	 *            Asset Class
	 * @param tableName:
	 *            Table Name
	 * @param countWebKey:
	 *            WebKey to store the Count
	 * @param viewWebKey:
	 *            WebKey to store the Listings
	 * @throws Exception
	 */
	protected void _viewWebAssets(RenderRequest req, User user, Class className, String tableName, String countWebKey, String viewWebKey,
			String queryWebKey, String showDeletedWebKey, String hostChangedWebKey) throws Exception {

		_viewWebAssets(req, user, className, tableName, countWebKey, viewWebKey, queryWebKey, showDeletedWebKey, hostChangedWebKey, null);

	}

	@SuppressWarnings("unchecked")
	protected void _viewWebAssets(RenderRequest req, User user, Class className, String tableName, String countWebKey, String viewWebKey,
			String queryWebKey, String showDeletedWebKey, String hostChangedWebKey, String parent) throws Exception {

		com.liferay.portlet.RenderRequestImpl reqImpl = (com.liferay.portlet.RenderRequestImpl) req;
		HttpServletRequest httpReq = reqImpl.getHttpServletRequest();
		WebAssetFactory webAssetFactory = new WebAssetFactory();
		// gets the session object for the messages
		HttpSession session = httpReq.getSession();

		String hostId = (String) session.getAttribute(WebKeys.SEARCH_HOST_ID);
		//if (req.getParameter("host_id") != null)
		//	hostId = req.getParameter("host_id");

		boolean hostChanged = false;
		if (session.getAttribute(com.dotmarketing.util.WebKeys.CMS_SELECTED_HOST_ID) != null) {
			if ((hostId != null) && !hostId.equals(session.getAttribute(com.dotmarketing.util.WebKeys.CMS_SELECTED_HOST_ID)))
				hostChanged = true;
			hostId = (String) session.getAttribute(com.dotmarketing.util.WebKeys.CMS_SELECTED_HOST_ID);
		}

		if (hostId != null){

			Host host = null;
			try{
				host = hostAPI.find(hostId, user, false);
			}
			catch(Exception e){
				Logger.error(this.getClass(), "Can't find host for assets.  Looking for host " + hostId + ", error:  " + e);
			}
			if(host != null){
				session.setAttribute(WebKeys.SEARCH_HOST_ID, hostId);
			}
			else{
				// this is a rig to set the host to something that will not return any pages, files, etc...
				session.removeAttribute(WebKeys.SEARCH_HOST_ID);
				hostId="no host";
			}

		}

		String query = req.getParameter("query");
		String resetQuery = req.getParameter("resetQuery");

		PaginatedArrayList<PermissionAsset>  results = new PaginatedArrayList<PermissionAsset>();

		try {

			// Gets roles
			Role[] roles = (Role[]) com.dotmarketing.business.APILocator.getRoleAPI().loadRolesForUser(user.getUserId()).toArray(new Role[0]);
			Logger.debug(this, "Inside _viewWebAssets Roles=" + roles.length);

			String showDeleted = req.getParameter("showDeleted");
			boolean showDel;
			if ((showDeleted == null) && (resetQuery == null)) {
				showDeleted = (String) session.getAttribute(showDeletedWebKey);
			}
			if ((showDeleted != null) && (showDeleted.equals("true"))) {
				session.setAttribute(showDeletedWebKey, "true");
				showDel = true;
			} else {
				session.setAttribute(showDeletedWebKey, "false");
				showDel = false;
			}

			String orderBy = req.getParameter("orderby");
			int pageNumber = 1;

			if (!hostChanged && UtilMethods.isSet(req.getParameter("pageNumber"))) {
				pageNumber = Integer.parseInt(req.getParameter("pageNumber"));
			}

			int limit = com.dotmarketing.util.Config.getIntProperty("PER_PAGE");

			int offset = (pageNumber - 1) * limit;

			String show = req.getParameter("show");
			WebAssetFactory.Direction direction = null;
			if ((show == null) || show.equals("next"))
				direction = WebAssetFactory.Direction.NEXT;
			else
				direction = WebAssetFactory.Direction.PREVIOUS;

			if ((query == null) && (resetQuery == null)) {
				query = (String) session.getAttribute(queryWebKey);
			}
			if(InodeUtils.isSet(query)){
				query = query.trim();
			}
			session.setAttribute(queryWebKey, query);
			
			if(UtilMethods.isSet(resetQuery)){
				offset = 0;
			}

			results = webAssetFactory.getAssetsAndPermissions(hostId, roles, showDel, limit, offset, orderBy, tableName, parent, query, user);

			req.setAttribute(hostChangedWebKey, hostChanged);
			req.setAttribute(countWebKey, results.getTotalResults());
			req.setAttribute(viewWebKey, results);
		} catch (Exception e) {
			req.setAttribute(viewWebKey, results);
			Logger.error(this, "Exception e =" + e.getMessage(), e);
			throw new Exception(e.getMessage());
		}

	}

	/**
	 * Method to handle an exception. Rollbacks the transaction and forwards to
	 * an error page.
	 *
	 * @param e
	 * @param req
	 */
	protected void _handleException(Exception e, ActionRequest req) {
		Logger.warn(this, e.toString(), e);
		try {
			HibernateUtil.rollbackTransaction();
		} catch (DotHibernateException e1) {
			Logger.error(this, e.getMessage(), e);
		}
		req.setAttribute(PageContext.EXCEPTION, e);
		
		//This is a fix for the <%@ page isErrorPage="true" %> directive in Glassfish
		req.setAttribute("javax.servlet.error.status_code", HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		
		setForward(req, Constants.COMMON_ERROR);
	}

	/**
	 * Method to handle an exception. Rollbacks the transaction and forwards to
	 * an error page.
	 *
	 * @param e
	 * @param req
	 */
	protected void _handleException(Exception e, ActionRequest req, boolean showStackTrace) {
		if (!showStackTrace)
			Logger.info(this, e.toString());
		else
			Logger.warn(this, e.toString(), e);
		try {
			HibernateUtil.rollbackTransaction();
		} catch (DotHibernateException e1) {
			Logger.error(this, e.getMessage(), e);
		}
		req.setAttribute(PageContext.EXCEPTION, e);
		
		//This is a fix for the <%@ page isErrorPage="true" %> directive in Glassfish
		req.setAttribute("javax.servlet.error.status_code", HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		
		setForward(req, Constants.COMMON_ERROR);
	}

	/**
	 * Method to handle an exception. Rollbacks the transaction and forwards to
	 * an error page.
	 *
	 * @param e
	 * @param req
	 */
	protected ActionForward _handleException(Exception e, RenderRequest req, ActionMapping mapping) {
		Logger.warn(this, e.toString(), e);
		try {
			HibernateUtil.rollbackTransaction();
		} catch (DotHibernateException e1) {
			Logger.error(this, e.toString(), e);
		}
		req.setAttribute(PageContext.EXCEPTION, e);
		
		//This is a fix for the <%@ page isErrorPage="true" %> directive in Glassfish
		req.setAttribute("javax.servlet.error.status_code", HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		
		return mapping.findForward(Constants.COMMON_ERROR);
	}

	/**
	 * Method to redirect to the referer if set, if not to the redirect. Closes
	 * the transaction before redirecting.
	 *
	 * @param req
	 * @param res
	 * @param referer
	 * @throws Exception
	 */
	protected void _sendToReferral(ActionRequest req, ActionResponse res, String referer) throws Exception {

		String redirect = req.getParameter("redirect");

		if (UtilMethods.isSet(referer)) {
			Logger.debug(this, "\n\nGoing to redirect to referer: " + referer);
			res.sendRedirect(referer);
		} else if (UtilMethods.isSet(redirect)) {
			Logger.debug(this, "\n\nGoing to redirect to redirect : " + redirect);
			res.sendRedirect(redirect);
		}
		Logger.debug(this, "End of _sendToReferral");
	}

	/**
	 * Method to obtain the User from Liferay on an ActionRequest
	 *
	 * @param req
	 * @return
	 */
	protected User _getUser(ActionRequest req) {

		// get the user
		User user = null;
		try {
			user = com.liferay.portal.util.PortalUtil.getUser(req);
		} catch (Exception e) {
			_handleException(e, req);
		}
		return user;

	}

	/**
	 * Method to obtain the User from Liferay on a RenderRequest
	 *
	 * @param req
	 * @return
	 */
	protected User _getUser(RenderRequest req) {

		// get the user
		User user = null;
		try {
			user = com.liferay.portal.util.PortalUtil.getUser(req);
		} catch (Exception e) {
			req.setAttribute(PageContext.EXCEPTION, e);
			return null;
		}
		return user;

	}

	// To check if Forward is being set on an action... wasn't implemented on
	// Liferay's PortletAction
	public String getForward(ActionRequest req) {
		return getForward(req, null);
	}

	public String getForward(ActionRequest req, String defaultValue) {
		String forward = (String) req.getAttribute(com.liferay.portal.util.WebKeys.PORTLET_STRUTS_FORWARD);

		if (forward == null) {
			return defaultValue;
		} else {
			return forward;
		}
	}

	protected static void _checkUserPermissions(Inode webAsset, User user, int permission) throws ActionException, DotDataException {
		PermissionAPI perAPI = APILocator.getPermissionAPI();
		// Checking permissions
		if (!InodeUtils.isSet(webAsset.getInode()))
			return;
		if (!perAPI.doesUserHavePermission(webAsset, permission, user)) {
			Logger.debug(DotPortletAction.class, "_checkUserPermissions: user does not have permissions ( " + permission + " ) over this asset: " + webAsset);
			List<Role> rolesPublish = perAPI.getRoles(webAsset.getInode(), PermissionAPI.PERMISSION_PUBLISH, "CMS Owner", 0, -1);
			List<Role> rolesWrite = perAPI.getRoles(webAsset.getInode(), PermissionAPI.PERMISSION_WRITE, "CMS Owner", 0, -1);
			
			Role cmsOwner;
			try {
				cmsOwner = APILocator.getRoleAPI().loadCMSOwnerRole();
			} catch (DotDataException e) {
				Logger.error(DotPortletAction.class,e.getMessage(),e);
				throw new ActionException(e);
			}
			boolean isCMSOwner = false;
			if(rolesPublish.size() > 0 || rolesWrite.size() > 0){
				for (Role role : rolesPublish) {
					if(role.getId().equals(cmsOwner.getId())){
						isCMSOwner = true;
						break;
					}
				}
				if(!isCMSOwner){
					for (Role role : rolesWrite) {
						if(role.getId().equals(cmsOwner.getId())){
							isCMSOwner = true;
							break;
						}
					}
				}
				if(!isCMSOwner){
					throw new ActionException(WebKeys.USER_PERMISSIONS_EXCEPTION);
				}
			}else{
				throw new ActionException(WebKeys.USER_PERMISSIONS_EXCEPTION);
			}	
					
		}
	}

	public static void _checkPermissions(Inode webAsset, Folder parentFolder, User user, HttpServletRequest httpReq) throws Exception {
		String subcmd = httpReq.getParameter("subcmd");
		boolean publish = (subcmd != null) && subcmd.equals(com.dotmarketing.util.Constants.PUBLISH);

		try {
			if (InodeUtils.isSet(webAsset.getInode())) {
				_checkUserPermissions(webAsset, user, PERMISSION_WRITE);
				if (publish)
					_checkUserPermissions(webAsset, user, PERMISSION_PUBLISH);
			} else {
				_checkUserPermissions(parentFolder, user, PERMISSION_CAN_ADD_CHILDREN);
			}
		} catch (Exception ae) {
			if (ae.getMessage().equals(WebKeys.USER_PERMISSIONS_EXCEPTION)) {
				if (publish)
					SessionMessages.add(httpReq, "message", "message.insufficient.permissions.to.save.and.publish");
				else
					SessionMessages.add(httpReq, "message", "message.insufficient.permissions.to.save");
			}
			throw ae;
		}

	}

	protected void _checkPermissions(Inode webAsset, User user, HttpServletRequest httpReq) throws Exception {
		String subcmd = httpReq.getParameter("subcmd");
		boolean publish = (subcmd != null) && subcmd.equals(com.dotmarketing.util.Constants.PUBLISH);

		try {
			if (InodeUtils.isSet(webAsset.getInode())) {
				_checkUserPermissions(webAsset, user, PERMISSION_WRITE);
				if (publish)
					_checkUserPermissions(webAsset, user, PERMISSION_PUBLISH);
			}
		} catch (Exception ae) {
			if (ae.getMessage().equals(WebKeys.USER_PERMISSIONS_EXCEPTION)) {
				if (publish)
					SessionMessages.add(httpReq, "message", "message.insufficient.permissions.to.save.and.publish");
				else{}
					SessionMessages.add(httpReq, "message", "message.insufficient.permissions.to.save");
			}
			throw ae;
		}

	}

	protected void _checkWritePermissions(Inode inode, User user, HttpServletRequest httpReq) throws Exception {
		try {
			_checkUserPermissions(inode, user, PERMISSION_WRITE);
		} catch (Exception ae) {
			if (ae.getMessage().equals(WebKeys.USER_PERMISSIONS_EXCEPTION)) {
				SessionMessages.add(httpReq, "message", "message.insufficient.permissions.to.save");
			}
			throw ae;
		}

	}

	protected void _checkWritePermissions(Inode inode, User user, HttpServletRequest httpReq, ArrayList<String>adminRoles) throws Exception {
		List<Role> userRoles = com.dotmarketing.business.APILocator.getRoleAPI().loadRolesForUser(user.getUserId());
		for (Role role : userRoles) {
			if (adminRoles.contains(role.getName())){
				return;
			}
		}
		_checkWritePermissions(inode, user, httpReq);
	}

	protected void _checkReadPermissions(Inode inode, User user, HttpServletRequest httpReq) throws Exception {
		try {
			_checkUserPermissions(inode, user, PERMISSION_READ);
		} catch (Exception ae) {
			if (ae.getMessage().equals(WebKeys.USER_PERMISSIONS_EXCEPTION)) {
				SessionMessages.add(httpReq, "message", "message.insufficient.permissions.to.read");
			}
			throw ae;
		}
	}

	protected void _checkReadPermissions(Inode inode, User user, HttpServletRequest httpReq, ArrayList<String>adminRoles) throws Exception {
		List<Role> userRoles = com.dotmarketing.business.APILocator.getRoleAPI().loadRolesForUser(user.getUserId());
		for (Role role : userRoles) {
			if (adminRoles.contains(role.getName())){
				return;
			}
		}
		_checkReadPermissions(inode, user, httpReq);
	}

	protected void _checkDeletePermissions(Inode inode, User user, HttpServletRequest httpReq) throws Exception {
		try {
			_checkUserPermissions(inode, user, PERMISSION_WRITE);
		} catch (Exception ae) {
			if (ae.getMessage().equals(WebKeys.USER_PERMISSIONS_EXCEPTION)) {
				SessionMessages.add(httpReq, "message", "message.insufficient.permissions.to.delete");
			}
			throw ae;
		}

	}

	protected void _checkDeletePermissions(Inode inode, User user, HttpServletRequest httpReq, ArrayList<String>adminRoles) throws Exception {
		List<Role> userRoles = com.dotmarketing.business.APILocator.getRoleAPI().loadRolesForUser(user.getUserId());
		for (Role role : userRoles) {
			if (adminRoles.contains(role.getName())){
				return;
			}
		}
		_checkDeletePermissions(inode, user, httpReq);
	}

	protected void _checkCopyAndMovePermissions(Inode webAsset, Folder parentFolder, User user, HttpServletRequest httpReq, String action)
	throws Exception {

		try {
			_checkUserPermissions(webAsset, user, PERMISSION_WRITE);
			_checkUserPermissions(parentFolder, user, PERMISSION_WRITE);
		} catch (Exception ae) {
			if (ae.getMessage().equals(WebKeys.USER_PERMISSIONS_EXCEPTION)) {
				SessionMessages.add(httpReq, "message", "message.insufficient.permissions.to." + action);
			}
			throw ae;
		}
	}

	protected void _checkCopyAndMovePermissions(Inode webAsset, User user, HttpServletRequest httpReq, String action) throws Exception {

		try {
			_checkUserPermissions(webAsset, user, PERMISSION_WRITE);
		} catch (Exception ae) {
			if (ae.getMessage().equals(WebKeys.USER_PERMISSIONS_EXCEPTION)) {
				SessionMessages.add(httpReq, "message", "message.insufficient.permissions.to." + action);
			}
			throw ae;
		}
	}

	protected void _copyPermissions(Inode from, Inode to) throws ActionException, DotDataException {
		PermissionAPI perAPI = APILocator.getPermissionAPI();
		perAPI.copyPermissions(from, to);
	}



	public CategoryAPI getCategoryAPI() {
		return categoryAPI;
	}

	public void setCategoryAPI(CategoryAPI categoryAPI) {
		this.categoryAPI = categoryAPI;
	}

}