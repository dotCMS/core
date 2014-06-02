package com.dotmarketing.portlets.containers.action;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.PortletConfig;
import javax.portlet.WindowState;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.struts.Globals;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;

import com.dotmarketing.beans.ContainerStructure;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.WebAsset;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.PermissionAPI.PermissionableType;
import com.dotmarketing.cache.StructureCache;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.factories.InodeFactory;
import com.dotmarketing.factories.WebAssetFactory;
import com.dotmarketing.portal.struts.DotPortletAction;
import com.dotmarketing.portal.struts.DotPortletActionInterface;
import com.dotmarketing.portlets.containers.ajax.util.ContainerAjaxUtil;
import com.dotmarketing.portlets.containers.factories.ContainerFactory;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.containers.struts.ContainerForm;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.structure.factories.StructureFactory;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.services.ContainerServices;
import com.dotmarketing.util.ActivityLogger;
import com.dotmarketing.util.HostUtil;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.Validator;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.model.User;
import com.liferay.portal.struts.ActionException;
import com.liferay.portal.util.Constants;
import com.liferay.portlet.ActionRequestImpl;
import com.liferay.util.servlet.SessionMessages;

/**
 * Portlet action used to maintain the containers
 * @author Maria
 */
public class EditContainerAction extends DotPortletAction implements
		DotPortletActionInterface {

	protected HostAPI hostAPI = APILocator.getHostAPI();
	protected PermissionAPI permissionAPI = APILocator.getPermissionAPI();

    /**
     * Main method called by the portlet's container to trigger actions over the contrainers
     */
	public void processAction(ActionMapping mapping, ActionForm form,
			PortletConfig config, ActionRequest req, ActionResponse res)
			throws Exception {

		String cmd = req.getParameter(Constants.CMD);
		String referer = req.getParameter("referer");
		ContainerForm fm = (ContainerForm) form;

		// wraps request to get session object
		ActionRequestImpl reqImpl = (ActionRequestImpl) req;
		HttpServletRequest httpReq = reqImpl.getHttpServletRequest();

		if ((referer != null) && (referer.length() != 0)) {
			while(referer.startsWith("%")){
				referer = URLDecoder.decode(referer, "UTF-8");
			}
		}

		Logger.debug(this, "EditContainerAction cmd=" + cmd);

		// if we are poping up the "add variables" page
		if (com.dotmarketing.util.Constants.CONTAINER_ADD_VARIABLE.equals(cmd)) {
			setForward(req, "portlet.ext.containers.add_variables");
			return;
		}

		HibernateUtil.startTransaction();

		User user = _getUser(req);

		try {
			Logger.debug(this, "Calling Retrieve method");
			_retrieveWebAsset(req, res, config, form, user, Container.class,
					WebKeys.CONTAINER_EDIT);

		} catch (Exception ae) {
			_handleException(ae, req);
			return;
		}

		/*
		 * We are editing the container or reloading a current one
		 */
		if ((cmd != null) && (cmd.equals(Constants.EDIT) || cmd.equals(Constants.UPDATE)) ) {
			try {
				Logger.debug(this, "Calling Edit Method");
				_editWebAsset(req, res, config, form, user);

			} catch (Exception ae) {
				if ((referer != null) && (referer.length() != 0)) {
					if (ae.getMessage().equals(WebKeys.EDIT_ASSET_EXCEPTION)) {
						// The web asset edit threw an exception because it's
						// locked so it should redirect back with message
						java.util.Map<String, String[]> params = new java.util.HashMap<String, String[]>();
						params.put("struts_action",
								new String[] { "/ext/director/direct" });
						params.put("cmd", new String[] { "editContainer" });
						params.put("container", new String[] { req
								.getParameter("inode") });
						params.put("referer", new String[] { URLEncoder.encode(
								referer, "UTF-8") });

						String directorURL = com.dotmarketing.util.PortletURLUtil
								.getActionURL(httpReq, WindowState.MAXIMIZED
										.toString(), params);

						_sendToReferral(req, res, directorURL);
						return;
					}
				}
				_handleException(ae, req);
				return;
			}
		}

		/*
		 * If we are updating the container, copy the information from the
		 * struts bean to the hbm inode and run the update action and return to
		 * the list
		 */
		if ((cmd != null) && cmd.equals(Constants.ADD)) {
			try {
				String note = fm.getNotes();
				if (Validator.validate(req, form, mapping) && note.length()<=255) {

					Logger.debug(this, "Calling Save Method");
					_saveWebAsset(req, res, config, form, user);


					String subcmd = req.getParameter("subcmd");

					if ((subcmd != null)
							&& subcmd
									.equals(com.dotmarketing.util.Constants.PUBLISH)) {
						try {
							Logger.debug(this, "Calling Publish Method");
							_publishWebAsset(req, res, config, form, user,
									WebKeys.CONTAINER_FORM_EDIT);
						} catch (Exception ae) {
							_handleException(ae, req);
							return;
						}
					}

					Container cont=(Container)req.getAttribute(WebKeys.CONTAINER_EDIT);
					if(cont.isLocked())
					    APILocator.getVersionableAPI().setLocked(cont, false, user);

					try{


					    _sendToReferral(req, res, referer);
					return;
					}
					catch(Exception e){
                        java.util.Map<String,String[]> params = new java.util.HashMap<String,String[]>();
                        params.put("struts_action",new String[] { "/ext/containers/view_containers" });
                        String directorURL = com.dotmarketing.util.PortletURLUtil.getActionURL(httpReq, WindowState.MAXIMIZED.toString(), params);
                        _sendToReferral(req, res, directorURL);
                        return;

					}
				}
				if(note.length()>255){
					ActionErrors ae = new ActionErrors();
					ae.add(Globals.ERROR_KEY, new ActionMessage("Note field Contains more than 255 characters"));
					req.setAttribute(Globals.ERROR_KEY, ae);
				}
				_editWebAsset(req, res, config, form, user);

			} catch (Exception ae) {
				_handleException(ae, req);
			}

		}
		/*
		 * If we are deleteing the container, run the delete action and return
		 * to the list
		 *
		 */
		else if ((cmd != null) && cmd.equals(Constants.DELETE)) {
			try {
				Logger.debug(this, "Calling Delete Method");
				_deleteWebAsset(req, res, config, form, user,
						WebKeys.CONTAINER_EDIT);

			} catch (Exception ae) {
				_handleException(ae, req);
				return;
			}
			_sendToReferral(req, res, referer);
		}
		else if ((cmd != null) && cmd.equals(com.dotmarketing.util.Constants.FULL_DELETE))
		{
			try
			{
				Logger.debug(this,"Calling Full Delete Method");
				WebAsset webAsset = (WebAsset) req.getAttribute(WebKeys.CONTAINER_EDIT);
				APILocator.getContainerAPI().deleteContainerStructuresByContainer((Container)webAsset);

				if(WebAssetFactory.deleteAsset(webAsset,user)) {
					SessionMessages.add(httpReq, "message", "message." + webAsset.getType() + ".full_delete");
				} else {
					SessionMessages.add(httpReq, "error", "message." + webAsset.getType() + ".full_delete.error");
				}
			}
			catch(Exception ae)
			{
				_handleException(ae, req);
				return;
			}
			_sendToReferral(req, res, referer);
		}
		else if ((cmd != null) && cmd.equals(com.dotmarketing.util.Constants.FULL_DELETE_LIST))
		{
			try
			{
				Logger.debug(this,"Calling Full Delete Method");
				String [] inodes = req.getParameterValues("publishInode");
				boolean returnValue = true;
				for(String inode  : inodes)
				{
					WebAsset webAsset = (WebAsset) InodeFactory.getInode(inode,Container.class);
					APILocator.getContainerAPI().deleteContainerStructuresByContainer((Container)webAsset);
					returnValue &= WebAssetFactory.deleteAsset(webAsset,user);
				}
				if(returnValue)
				{
					SessionMessages.add(httpReq,"message","message.containers.full_delete");
				}
				else
				{
					SessionMessages.add(httpReq,"error","message.containers.full_delete.error");
				}
			}
			catch(Exception ae)
			{
				_handleException(ae, req);
				return;
			}
			_sendToReferral(req, res, referer);
		}
		/*
		 * If we are undeleting the container, run the undelete action and
		 * return to the list
		 *
		 */
		else if ((cmd != null)
				&& cmd.equals(com.dotmarketing.util.Constants.UNDELETE)) {
			try {
				Logger.debug(this, "Calling UnDelete Method");
				_undeleteWebAsset(req, res, config, form, user,
						WebKeys.CONTAINER_EDIT);

			} catch (Exception ae) {
				_handleException(ae, req);
				return;
			}
			_sendToReferral(req, res, referer);

		}
		/*
		 * If we are deleting the container version, run the deeleteversion
		 * action and return to the list
		 */
		else if ((cmd != null)
				&& cmd.equals(com.dotmarketing.util.Constants.DELETEVERSION)) {
			try {
				Logger.debug(this, "Calling Delete Version Method");
				_deleteVersionWebAsset(req, res, config, form, user,
						WebKeys.CONTAINER_EDIT);

			} catch (Exception ae) {
				_handleException(ae, req);
				return;
			}
			_sendToReferral(req, res, referer);
		}
		/*
		 * If we are unpublishing the container, run the unpublish action and
		 * return to the list
		 */
		else if ((cmd != null)
				&& cmd.equals(com.dotmarketing.util.Constants.UNPUBLISH)) {
			try {
				Logger.debug(this, "Calling Unpublish Method");
				_unPublishWebAsset(req, res, config, form, user,
						WebKeys.CONTAINER_EDIT);

			} catch (Exception ae) {
				_handleException(ae, req);
				return;
			}
			_sendToReferral(req, res, referer);

		}
		/*
		 * If we are getting the container version back, run the getversionback
		 * action and return to the list
		 */
		else if ((cmd != null)
				&& cmd.equals(com.dotmarketing.util.Constants.GETVERSIONBACK)) {
			try {
				Logger.debug(this, "Calling Get Version Back Method");
				_getVersionBackWebAsset(req, res, config, form, user);

			} catch (Exception ae) {
				_handleException(ae, req);
				return;
			}
			_sendToReferral(req, res, referer);
		}
		/*
		 * If we are getting the container versions, run the assetversions
		 * action and return to the list
		 */
		else if ((cmd != null)
				&& cmd.equals(com.dotmarketing.util.Constants.ASSETVERSIONS)) {
			try {
				Logger.debug(this, "Calling Get Versions Method");
				_getVersionsWebAsset(req, res, config, form, user,
						WebKeys.CONTAINER_EDIT, WebKeys.CONTAINER_VERSIONS);

			} catch (Exception ae) {
				_handleException(ae, req);
				return;
			}
		}
		/*
		 * If we are unlocking the container, run the unlock action and return
		 * to the list
		 */
		else if ((cmd != null)
				&& cmd.equals(com.dotmarketing.util.Constants.UNLOCK)) {
			try {
				Logger.debug(this, "Calling Unlock Method");
				_unLockWebAsset(req, res, config, form, user,
						WebKeys.CONTAINER_EDIT);

			} catch (Exception ae) {
				_handleException(ae, req);
				return;
			}
			_sendToReferral(req, res, referer);

		}
		/*
		 * If we are copying the container, run the copy action and return to
		 * the list
		 */
		else if ((cmd != null)
				&& cmd.equals(com.dotmarketing.util.Constants.COPY)) {
			try {
				Logger.debug(this, "Calling Copy Method");
				_copyWebAsset(req, res, config, form, user);
			} catch (Exception ae) {
				_handleException(ae, req);
			}
			_sendToReferral(req, res, referer);
		}
		/*
		 * If we are moving the container, run the copy action and return to the
		 * list
		 */
		else if ((cmd != null)
				&& cmd.equals(com.dotmarketing.util.Constants.MOVE)) {
			try {
				Logger.debug(this, "Calling Move Method");
				_moveWebAsset(req, res, config, form, user, Container.class,
						WebKeys.CONTAINER_EDIT);
			} catch (Exception ae) {
				_handleException(ae, req);
			}
			_sendToReferral(req, res, referer);
		} else
			Logger.debug(this, "Unspecified Action");

		HibernateUtil.commitTransaction();

		_setupEditContainerPage(reqImpl, res, config, form, user);
		setForward(req, "portlet.ext.containers.edit_container");
	}

	// /// ************** ALL METHODS HERE *************************** ////////

	private void _setupEditContainerPage(ActionRequest req, ActionResponse res, PortletConfig config, ActionForm form,
			User user) throws Exception {

		//Getting the host that can be assigned to the container
		PermissionAPI perAPI = APILocator.getPermissionAPI();
		Container container = (Container) req.getAttribute(WebKeys.CONTAINER_EDIT);

        Host host = hostAPI.findParentHost(container, user, false);

		List<Host> hosts = APILocator.getHostAPI().findAll(user, false);
		hosts.remove(APILocator.getHostAPI().findSystemHost(user, false));
		hosts = perAPI.filterCollection(hosts, PermissionAPI.PERMISSION_CAN_ADD_CHILDREN, false, user);
		if(host != null && !hosts.contains(host)) {
			hosts.add(host);
		}
		req.setAttribute(WebKeys.CONTAINER_HOSTS, hosts);
	}




    /**
     * Method called to load the edit attributes in the request
     */
	@SuppressWarnings("unchecked")
	public void _editWebAsset(ActionRequest req, ActionResponse res,
			PortletConfig config, ActionForm form, User user) throws Exception {

		// calls edit method from super class that returns parent folder
		super._editWebAsset(req, res, config, form, user, WebKeys.CONTAINER_EDIT);

		// setting parent folder path and inode on the form bean
		ContainerForm cf = (ContainerForm) form;
		ActionRequestImpl reqImpl = (ActionRequestImpl) req;
		HttpServletRequest httpReq = reqImpl.getHttpServletRequest();
		HttpSession session = httpReq.getSession();

		//Setting the default host = the selected crumbtrail host if it is a new container
        String hostId= (String)session.getAttribute(com.dotmarketing.util.WebKeys.CMS_SELECTED_HOST_ID);
        if(!hostId.equals("allHosts") && cf.getHostId() == null) {
        	Host crumbHost = hostAPI.find(hostId, user, false);
        	if(crumbHost != null && permissionAPI.doesUserHavePermission(crumbHost, PermissionAPI.PERMISSION_CAN_ADD_CHILDREN, user, false))
        		cf.setHostId(hostId);
        }

		// This can't be done on the WebAsset so it needs to be done here.
		Container container = (Container) req
				.getAttribute(WebKeys.CONTAINER_EDIT);

        if (UtilMethods.isSet(container.getLuceneQuery())) {
            cf.setDynamic(true);
        }

		// BEGIN GRAZIANO issue-12-dnd-template
        if(UtilMethods.isSet(container.getCode())){
			if(ContainerAjaxUtil.checkMetadataContainerCode(container.getCode()))
				container.setForMetadata(true);
			// END GRAZIANO issue-12-dnd-template
        }
		// Getting container structure
		if (!InodeUtils.isSet(cf.getStructureInode())) {
			Structure currentStructure;
			if (!InodeUtils.isSet(container.getInode())) {
				currentStructure = StructureFactory.getDefaultStructure();
			} else {
				currentStructure = StructureCache.getStructureByInode(container.getStructureInode());
				if (!InodeUtils.isSet(currentStructure.getInode()))
					currentStructure = StructureFactory.getDefaultStructure();
			}
			cf.setStructureInode(currentStructure.getInode());
		}

        //gets the container host
        Host host = hostAPI.findParentHost(container, user, false);
        if(host!= null)
        	cf.setHostId(host.getIdentifier());

		//Asset Versions to list in the versions tab
		req.setAttribute(WebKeys.VERSIONS_INODE_EDIT, container);

	}

    /**
     * Method called to save container in the system
     */
	public void _saveWebAsset(ActionRequest req, ActionResponse res,
			PortletConfig config, ActionForm form, User user) throws Exception {

		// wraps request to get session object
		ActionRequestImpl reqImpl = (ActionRequestImpl) req;
		HttpServletRequest httpReq = reqImpl.getHttpServletRequest();

		ContainerForm fm = (ContainerForm) form;

		// gets the new information for the container from the request object
		req.setAttribute(WebKeys.CONTAINER_FORM_EDIT,
				new com.dotmarketing.portlets.containers.model.Container());
		BeanUtils.copyProperties(req.getAttribute(WebKeys.CONTAINER_FORM_EDIT),	form);

		// gets the new information for the container from the request object
		Container container = (Container) req.getAttribute(WebKeys.CONTAINER_FORM_EDIT);

		// gets the current container being edited from the request object
		Container currentContainer = (Container) req.getAttribute(WebKeys.CONTAINER_EDIT);

		//Is a new container?
		boolean isNew = !InodeUtils.isSet(currentContainer.getInode());

		//Getting the container host
        Host host = hostAPI.find(fm.getHostId(), user, false);

		//Checking permissions
		if (!isNew) {
			_checkWritePermissions(currentContainer, user, httpReq);
		} else {
			//If the asset is new checking that the user has permission to add children to the parent host
			if(!permissionAPI.doesUserHavePermission(host, PermissionAPI.PERMISSION_CAN_ADD_CHILDREN, user, false)
					|| !permissionAPI.doesUserHavePermissions(PermissionableType.CONTAINERS, PermissionAPI.PERMISSION_EDIT, user)) {
				SessionMessages.add(httpReq, "message", "message.insufficient.permissions.to.save");
				throw new ActionException(WebKeys.USER_PERMISSIONS_EXCEPTION);
			}
		}

		// Current associated templates
		List<Template> currentTemplates = InodeFactory.getChildrenClass(currentContainer, Template.class);

		// gets user id from request for mod user
		String userId = user.getUserId();

		// Associating the current structure
		Structure currentStructure = null;
		if (!InodeUtils.isSet(fm.getStructureInode())) {
			currentStructure = StructureFactory.getDefaultStructure();
		} else {
			currentStructure = StructureCache.getStructureByInode(fm.getStructureInode());
		}
		container.setStructureInode(currentStructure.getInode());
		//container.addParent(currentStructure);

		// BEGIN GRAZIANO issue-12-dnd-template
		if(ContainerAjaxUtil.checkMetadataContainerCode(container.getCode()))
			container.setForMetadata(true);
		// END GRAZIANO issue-12-dnd-template

		// it saves or updates the asset
		if (InodeUtils.isSet(currentContainer.getInode())) {
			Identifier identifier = APILocator.getIdentifierAPI().find(currentContainer);
			WebAssetFactory.createAsset(container, userId, identifier, false);
			container = (Container) WebAssetFactory.saveAsset(container,
					identifier);
		} else {
			WebAssetFactory.createAsset(container, userId, host);
		}

		req.setAttribute(WebKeys.CONTAINER_FORM_EDIT, container);

		// Get templates of the old version so you can update the working
		// information to this new version.
		Iterator<Template> it = currentTemplates.iterator();

		// update templates to new version
		while (it.hasNext()) {
			Template parentInode = (Template) it.next();
			parentInode.addChild(container);
		}
		Identifier identifier = APILocator.getIdentifierAPI().find(container);

		//Saving the host of the container
		identifier.setHostId(host.getIdentifier());
		APILocator.getIdentifierAPI().save(identifier);
		
		// saving the structures
		if(container.getMaxContentlets()>0) {
			String structureId = req.getParameter("structureInode");

			List<ContainerStructure> csList = new LinkedList<ContainerStructure>();

			String code = req.getParameter("code_"+structureId);
			ContainerStructure cs = new ContainerStructure();
			cs.setContainerId(container.getIdentifier());
			cs.setStructureId(structureId);
			cs.setCode(code);
			csList.add(cs);
			
			APILocator.getContainerAPI().saveContainerStructures(csList);

		}

		SessionMessages.add(httpReq, "message", "message.containers.save");
		ActivityLogger.logInfo(this.getClass(), "Save WebAsset action", "User " + user.getPrimaryKey() + " saved " + container.getTitle(), HostUtil.hostNameUtil(req, _getUser(req)));
		// saves to working folder under velocity
		ContainerServices.invalidate(container, true);

		// copies the information back into the form bean
		BeanUtils.copyProperties(form, req
				.getAttribute(WebKeys.CONTAINER_FORM_EDIT));

		APILocator.getVersionableAPI().setWorking(container);




		HibernateUtil.flush();
	}

	public void _copyWebAsset(ActionRequest req, ActionResponse res,
			PortletConfig config, ActionForm form, User user) throws Exception {

		// wraps request to get session object
		ActionRequestImpl reqImpl = (ActionRequestImpl) req;
		HttpServletRequest httpReq = reqImpl.getHttpServletRequest();

		Logger.debug(this, "I'm copying the Container");

		// gets the current template being edited from the request object
		Container currentContainer = (Container) req
				.getAttribute(WebKeys.CONTAINER_EDIT);

		// Checking permissions
		_checkCopyAndMovePermissions(currentContainer, user, httpReq,
				"copy");

		// Calling the factory to execute the copy operation
		ContainerFactory.copyContainer(currentContainer);

		SessionMessages.add(httpReq, "message", "message.containers.copy");
	}

	public void _getVersionBackWebAsset(ActionRequest req, ActionResponse res,
			PortletConfig config, ActionForm form, User user) throws Exception {
		Container workingContainer = (Container) super._getVersionBackWebAsset(
				req, res, config, form, user, Container.class,
				WebKeys.CONTAINER_EDIT);
		ContainerServices.invalidate(workingContainer, true);
	}

}
