package com.dotmarketing.portlets.links.action;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.List;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.PortletConfig;
import javax.portlet.WindowState;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionMapping;

import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.Inode;
import com.dotmarketing.beans.Tree;
import com.dotmarketing.beans.WebAsset;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.IdentifierAPI;
import com.dotmarketing.business.IdentifierCache;
import com.dotmarketing.business.IdentifierFactory;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.factories.InodeFactory;
import com.dotmarketing.factories.TreeFactory;
import com.dotmarketing.factories.WebAssetFactory;
import com.dotmarketing.menubuilders.RefreshMenus;
import com.dotmarketing.portal.struts.DotPortletAction;
import com.dotmarketing.portal.struts.DotPortletActionInterface;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.portlets.contentlet.business.Contentlet;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.links.factories.LinkFactory;
import com.dotmarketing.portlets.links.model.Link;
import com.dotmarketing.portlets.links.model.Link.LinkType;
import com.dotmarketing.portlets.links.struts.LinkForm;
import com.dotmarketing.services.ContentletMapServices;
import com.dotmarketing.services.ContentletServices;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.Validator;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.model.User;
import com.liferay.portal.util.Constants;
import com.liferay.portlet.ActionRequestImpl;
import com.liferay.util.servlet.SessionMessages;

/**
 * @author if(working==false){ author="Maria Ahues"; }else{ author="Rocco
 *         Maglio";
 */

public class EditLinkAction extends DotPortletAction implements
DotPortletActionInterface {

	public void processAction(ActionMapping mapping, ActionForm form,
			PortletConfig config, ActionRequest req, ActionResponse res)
	throws Exception {

		String cmd = req.getParameter(Constants.CMD);
		String referer = req.getParameter("referer");
		
		//wraps request to get session object
		ActionRequestImpl reqImpl = (ActionRequestImpl) req;
		HttpServletRequest httpReq = reqImpl.getHttpServletRequest();

		if ((referer != null) && (referer.length() != 0)) {
			referer = URLDecoder.decode(referer, "UTF-8");
		}

		Logger.debug(this, "EditLinkAction cmd=" + cmd);

		HibernateUtil.startTransaction();

		User user = _getUser(req);

		try {
			Logger.debug(this, "Calling Retrieve method");

			_retrieveWebAsset(req, res, config, form, user, Link.class,
					WebKeys.LINK_EDIT);

		} catch (Exception ae) {
			_handleException(ae, req);
			return;
		}

		/*
		 * We are editing the link
		 */
		if ((cmd != null) && cmd.equals(Constants.EDIT)) {
			try {
				Logger.debug(this, "Calling Edit method");
				_editWebAsset(req, res, config, form, user);

			} catch (Exception ae) {
				if (ae.getMessage().equals(WebKeys.EDIT_ASSET_EXCEPTION)) {

					//The web asset edit threw an exception because it's
					// locked so it should redirect back with message
					java.util.Map<String,String[]> params = new java.util.HashMap<String,String[]>();
					params.put("struts_action",new String[] { "/ext/director/direct" });
					params.put("cmd", new String[] { "editLink" });
					params.put("link", new String[] { req.getParameter("inode") });
					params.put("popup", new String[] { (req.getParameter("popup")!=null) ? req.getParameter("popup") : "" });
					params.put("child", new String[] { (req.getParameter("child")!=null) ? req.getParameter("child") : "" });
					params.put("browse", new String[] { (req.getParameter("browse")!=null) ? req.getParameter("browse") : "" });
					params.put("page_width", new String[] { (req.getParameter("page_width")!=null) ? req.getParameter("page_width") : ""});

					if (UtilMethods.isSet(referer)) {
						params.put("referer", new String[] { URLEncoder.encode(referer, "UTF-8") });
					}

					String directorURL = com.dotmarketing.util.PortletURLUtil
					.getActionURL(httpReq, WindowState.MAXIMIZED
							.toString(), params);

					_sendToReferral(req, res, directorURL);
					return;
				}
				_handleException(ae, req);
				return;
			}
		}

		/*
		 * If we are updating the link, copy the information from the struts
		 * bean to the hbm inode and run the update action and return to the
		 * list
		 */
		if ((cmd != null) && cmd.equals(Constants.ADD)) {
			try {

				if (Validator.validate(req, form, mapping)) {

					Logger.debug(this, "Calling Save method");
					_saveWebAsset(req, res, config, form, user);
					
					
					String subcmd = req.getParameter("subcmd");

					if ((subcmd != null)
							&& subcmd
							.equals(com.dotmarketing.util.Constants.PUBLISH)) {
						Logger.debug(this, "Calling Publish method");
						_publishWebAsset(req, res, config, form, user, WebKeys.LINK_FORM_EDIT);
					}
					
					_sendToReferral(req, res, referer);
				}

			} catch (Exception ae) {
				_handleException(ae, req);
				if (ae.getMessage().equals(WebKeys.USER_PERMISSIONS_EXCEPTION)) {
					setForward(req, "portlet.ext.links.edit_link");
					return;
				} 
				return;
			}

		}
		/*
		 * If we are deleteing the link, run the delete action and return to the
		 * list
		 *  
		 */
		else if ((cmd != null) && cmd.equals(Constants.DELETE)) {
			try {
				Logger.debug(this, "Calling Delete method");
				_deleteWebAsset(req, res, config, form, user, WebKeys.LINK_EDIT);

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
				WebAsset webAsset = (WebAsset) req.getAttribute(WebKeys.LINK_EDIT);
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
					WebAsset webAsset = (WebAsset) InodeFactory.getInode(inode,Link.class);
					returnValue &= WebAssetFactory.deleteAsset(webAsset,user);
				}
				if(returnValue)
				{
					SessionMessages.add(httpReq,"message","message.links.full_delete");
				}
				else
				{
					SessionMessages.add(httpReq,"error","message.links.full_delete.error");
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
		 * If we are undeleting the link, run the undelete action and return to
		 * the list
		 *  
		 */
		else if ((cmd != null)
				&& cmd.equals(com.dotmarketing.util.Constants.UNDELETE)) {
			try {
				Logger.debug(this, "Calling UnDelete method");
				_undeleteWebAsset(req, res, config, form, user,
						WebKeys.LINK_EDIT);

			} catch (Exception ae) {
				_handleException(ae, req);
				return;
			}
			_sendToReferral(req, res, referer);
			return;

		}
		/*
		 * If we are deleting the link version, run the deeleteversion action
		 * and return to the list
		 */
		else if ((cmd != null)
				&& cmd.equals(com.dotmarketing.util.Constants.DELETEVERSION)) {
			try {
				Logger.debug(this, "Calling Delete Version Method");
				_deleteVersionWebAsset(req, res, config, form, user,
						WebKeys.LINK_EDIT);

			} catch (Exception ae) {
				_handleException(ae, req);
				return;
			}
			_sendToReferral(req, res, referer);
		}
		/*
		 * If we are unpublishing the link, run the unpublish action and return
		 * to the list
		 */
		else if ((cmd != null)
				&& cmd.equals(com.dotmarketing.util.Constants.UNPUBLISH)) {
			try {
				Logger.debug(this, "Calling Unpublish Version Method");
				_unPublishWebAsset(req, res, config, form, user,
						WebKeys.LINK_EDIT);

			} catch (Exception ae) {
				_handleException(ae, req);
				return;
			}
			_sendToReferral(req, res, referer);

		}
		/*
		 * If we are getting the link version back, run the getversionback
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
		 * If we are getting the link versions, run the assetversions action and
		 * return to the list
		 */
		else if ((cmd != null)
				&& cmd.equals(com.dotmarketing.util.Constants.ASSETVERSIONS)) {
			try {
				Logger.debug(this, "Calling Get Versions Method");
				_getVersionsWebAsset(req, res, config, form, user,
						WebKeys.LINK_EDIT, WebKeys.LINK_VERSIONS);

			} catch (Exception ae) {
				_handleException(ae, req);
				return;
			}
		}
		/*
		 * If we are unlocking the link, run the unlock action and return to the
		 * list
		 */
		else if ((cmd != null)
				&& cmd.equals(com.dotmarketing.util.Constants.UNLOCK)) {
			try {
				Logger.debug(this, "Calling Unlock Method");
				_unLockWebAsset(req, res, config, form, user, WebKeys.LINK_EDIT);

			} catch (Exception ae) {
				_handleException(ae, req);
				return;
			}
			_sendToReferral(req, res, referer);

		}
		/*
		 * If we are copying the link, run the copy action and return to the
		 * list
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
		 * If we are moving the link, run the copy action and return to the list
		 */
		else if ((cmd != null)
				&& cmd.equals(com.dotmarketing.util.Constants.MOVE)) {
			try {
				Logger.debug(this, "Calling Move Method");
				_moveWebAsset(req, res, config, form, user, Link.class,WebKeys.LINK_EDIT);

			} catch (Exception ae) {
				_handleException(ae, req);
			}
			_sendToReferral(req, res, referer);
		} else
			Logger.debug(this, "Unspecified Action");

		HibernateUtil.commitTransaction();

		if ((cmd != null) && cmd.equals(Constants.ADD) ) 
		{      	
			//RefreshMenus.deleteMenus();        	
		}

		setForward(req, "portlet.ext.links.edit_link");
	}

	///// ************** ALL METHODS HERE *************************** ////////

	public void _editWebAsset(ActionRequest req, ActionResponse res,
			PortletConfig config, ActionForm form, User user) throws Exception {

		//calls edit method from super class that returns parent folder
		Folder parentFolder = super._editWebAsset(req, res, config, form, user,
				WebKeys.LINK_EDIT);

		//setting parent folder path and inode on the form bean
		LinkForm lf = (LinkForm) form;
		lf.setParent(parentFolder.getInode());

		//This can't be done on the WebAsset so it needs to be done here.
		Link link = (Link) req.getAttribute(WebKeys.LINK_EDIT);
		link.setParent(parentFolder.getInode());
		if (InodeUtils.isSet(link.getInode())) {
			if (InodeUtils.isSet(link.getInternalLinkIdentifier())) {
				lf.setInternalLinkIdentifier(link.getInternalLinkIdentifier());
			}
		} else {
			lf.setLinkType (LinkType.INTERNAL);
			link.setLinkType (LinkType.INTERNAL.toString());
		}

	}

	@SuppressWarnings("unchecked")
	public void _saveWebAsset(ActionRequest req, ActionResponse res,
			PortletConfig config, ActionForm form, User user) throws Exception {

		HostAPI hostAPI = APILocator.getHostAPI();
		IdentifierAPI identifierAPI = APILocator.getIdentifierAPI();
		
		//wraps request to get session object
		ActionRequestImpl reqImpl = (ActionRequestImpl) req;
		HttpServletRequest httpReq = reqImpl.getHttpServletRequest();

		LinkForm linkForm = (LinkForm) form;
		
		//gets the new information for the link from the request object
		Link link = new Link();
		link.setTitle(((LinkForm) form).getTitle());		
		BeanUtils.copyProperties(link,form);
		req.setAttribute(WebKeys.LINK_FORM_EDIT,link);
		
		boolean previousShowMenu = link.isShowOnMenu();

		//gets the current link being edited from the request object
		Link currentLink = (Link) req.getAttribute(WebKeys.LINK_EDIT);
		
		//parent folder or inode for this file
		Folder parent = APILocator.getFolderAPI().find(req.getParameter("parent"), user, false);
		//http://jira.dotmarketing.net/browse/DOTCMS-5899
		if(UtilMethods.isSet(currentLink.getInode())){
			Identifier id = APILocator.getIdentifierAPI().find(currentLink);
			String URI = id.getURI();
			String uriPath = URI.substring(0,URI.lastIndexOf("/")+1);
			if(!uriPath.equals(APILocator.getIdentifierAPI().find(parent).getPath())){
				id.setURI(APILocator.getIdentifierAPI().find(parent).getPath()+currentLink.getProtocal() + currentLink.getUrl());
				APILocator.getIdentifierAPI().save(id);
			}
		}
		
		//Checking permissions
		_checkPermissions(currentLink, parent, user, httpReq);

		//gets user id from request for mod user
		String userId = user.getUserId();

		// take care of internal links
		if (InodeUtils.isSet(linkForm.getInternalLinkIdentifier())) {

			Identifier internalLinkIdentifier = identifierAPI.findFromInode(linkForm.getInternalLinkIdentifier());
			//link.setLinkType(LinkType.INTERNAL.toString());
			link.setInternalLinkIdentifier(internalLinkIdentifier.getInode());
			link.setProtocal("http://");

			StringBuffer myURL = new StringBuffer();
			if(InodeUtils.isSet(internalLinkIdentifier.getHostId())) {
				Host host = hostAPI.find(internalLinkIdentifier.getHostId(), user, false);
				myURL.append(host.getHostname());
			}
			myURL.append(internalLinkIdentifier.getURI());
			link.setUrl(myURL.toString());
			
		}
		
		if(link.getLinkType().equals(LinkType.CODE.toString()))
			link.setProtocal("");		

		Link workingLink = null;
		//it saves or updates the asset
		if (InodeUtils.isSet(currentLink.getInode())) {
			Identifier identifier = APILocator.getIdentifierAPI().find(currentLink);
			WebAssetFactory
			.createAsset(link, userId, parent, identifier, false);

			workingLink = (Link) WebAssetFactory.saveAsset(link, identifier);
			currentLink = link;
			link = workingLink;
			req.setAttribute(WebKeys.LINK_FORM_EDIT,link);
			if (!currentLink.getTarget().equals(link.getTarget())) {
				//create new identifier, with the URI
				APILocator.getIdentifierAPI().updateIdentifierURI(workingLink,(Folder) parent);
			}
		} else {
			WebAssetFactory.createAsset(link, userId, parent);
			req.setAttribute(WebKeys.LINK_EDIT, link);
			workingLink = link;
		}

		// Get parents of the old version so you can update the working
		// information to this new version.


		List<Object> parents = (List<Object>) InodeFactory.getParentsOfClass(currentLink, Category.class);
		parents.addAll(InodeFactory.getParentsOfClass(currentLink, Contentlet.class));


		List<Inode> children = (List<Inode>) InodeFactory.getChildrenClass(currentLink, Category.class);
		children.addAll(InodeFactory.getChildrenClass(currentLink, Contentlet.class));


		java.util.Iterator parentsIterator = parents.iterator();

		//update parents to new version delete old versions parents if not
		// live.
		while (parentsIterator.hasNext()) {
			Object obj = parentsIterator.next();
			
			if(obj instanceof Inode){
				Inode parentInode = (Inode) obj;
				parentInode.addChild(workingLink);

				//to keep relation types from parent only if it exists
				Tree tree = com.dotmarketing.factories.TreeFactory.getTree(
						parentInode, currentLink);
				if ((tree.getRelationType() != null)
						&& (tree.getRelationType().length() != 0)) {
					Tree newTree = com.dotmarketing.factories.TreeFactory.getTree(
							parentInode, workingLink);
					newTree.setRelationType(tree.getRelationType());
					newTree.setTreeOrder(0);
					TreeFactory.saveTree(newTree);
				}
			}
		
			
			/*if(obj instanceof Identifier){
				Identifier parentIdentifier = (Identifier) obj;
				parentIdentifier.addChild(workingLink);

				//to keep relation types from parent only if it exists
				Tree tree = com.dotmarketing.factories.TreeFactory.getTree(
						parentIdentifier.getInode(), currentLink.getInode());
				if ((tree.getRelationType() != null)
						&& (tree.getRelationType().length() != 0)) {
					Tree newTree = com.dotmarketing.factories.TreeFactory.getTree(
							parentIdentifier.getInode(), workingLink.getInode());
					newTree.setRelationType(tree.getRelationType());
					newTree.setTreeOrder(0);
					TreeFactory.saveTree(newTree);
				}
				//checks type of parent and deletes child if not live version.
				if (!currentLink.isLive()) {
					if (!(parentIdentifier instanceof Identifier)) {
						parentIdentifier.deleteChild(currentLink);
					}
				}
			}
          */


//			//Republishing parents working contentlets
//			if (parentInode instanceof Contentlet) {
//				Contentlet cont = (Contentlet) parentInode;
//
//				if (cont.isWorking()) {
//					//calls the asset factory edit
//					ContentletServices.writeContentletToFile(cont, true);
//					ContentletMapServices.writeContentletMapToFile(cont, true);
//				}
//			}

			
		}
		if (req.getParameter("popup") != null) {
			req.setAttribute("inode", String.valueOf(workingLink.getInode()));
		}

		//Refreshing the menues
		if (!(!previousShowMenu && !link.isShowOnMenu()))
		{
			//existing folder with different show on menu ... need to regenerate menu
			RefreshMenus.deleteMenu(link);
		}

		SessionMessages.add(req, "message", "message.links.save");
	}

	public void _copyWebAsset(ActionRequest req, ActionResponse res,
			PortletConfig config, ActionForm form, User user) throws Exception {

		//wraps request to get session object
		ActionRequestImpl reqImpl = (ActionRequestImpl) req;
		HttpServletRequest httpReq = reqImpl.getHttpServletRequest();

		//gets the current template being edited from the request object
		Link currentLink = (Link) req.getAttribute(WebKeys.LINK_EDIT);

		//gets folder parent
		String parentInode = req.getParameter("parent");
		Folder parent = null;
		if (parentInode != null && parentInode.length() != 0
				&& !parentInode.equals("0")) {
			//the parent is being passed through the request
			parent = (Folder) InodeFactory.getInode(parentInode, Folder.class);
			Logger.debug(this, "Parent Folder=" + parent.getInode());
		} else {
			parent = APILocator.getFolderAPI().findParentFolder(currentLink, user,false);
			Logger.debug(this, "Parent Folder=" + parent.getInode());
		}

		//Checking permissions
		_checkCopyAndMovePermissions(currentLink, parent, user, httpReq, "copy");

		LinkFactory.copyLink(currentLink, parent);

		SessionMessages.add(req, "message", "message.link.copy");
	}

	@SuppressWarnings("unchecked")
	public void _getVersionBackWebAsset(ActionRequest req, ActionResponse res,
			PortletConfig config, ActionForm form, User user) throws Exception {

		Link workingLink = (Link) super._getVersionBackWebAsset(req, res,
				config, form, user, Link.class, WebKeys.LINK_EDIT);
		Link linkVersion = (Link) req.getAttribute(WebKeys.LINK_EDIT);

		// Get parents of the old version so you can update the working
		// information to this new version.
		List<Inode> parents = (List<Inode>) InodeFactory.getParentsOfClass(linkVersion, Category.class);
		parents.addAll(InodeFactory.getParentsOfClass(linkVersion, Contentlet.class));


		java.util.Iterator parentsIterator = parents.iterator();

		//update parents to new version delete old versions parents if not
		// live.
		while (parentsIterator.hasNext()) {
			Object obj = parentsIterator.next();
		    if(obj instanceof Inode){
			  Inode parentInode = (Inode) obj;
			  if(!InodeUtils.isSet(parentInode.getInode())){
				continue;
			 }
			 parentInode.addChild(workingLink);

			 //to keep relation types from parent only if it exists
			 Tree tree = com.dotmarketing.factories.TreeFactory.getTree(
					parentInode, linkVersion);
			 if ((tree.getRelationType() != null)
					&& (tree.getRelationType().length() != 0)) {
				Tree newTree = com.dotmarketing.factories.TreeFactory.getTree(
						parentInode, workingLink);
				newTree.setRelationType(tree.getRelationType());
				newTree.setTreeOrder(0);
				TreeFactory.saveTree(newTree);
			 }

			 // checks type of parent and deletes child if not live version.
			 if (!linkVersion.isLive()) {
				if (parentInode instanceof Inode) {
					parentInode.deleteChild(linkVersion);
				}
			 }
		   }
		}

		//Rewriting the parent�s contentlets of the link
		List<Contentlet> contentlets = (List<Contentlet>)InodeFactory.getParentsOfClass(workingLink,
				Contentlet.class);
		ContentletAPI conAPI = APILocator.getContentletAPI();

		for( Contentlet cont : contentlets ) {
			if (cont.isWorking()) {
				com.dotmarketing.portlets.contentlet.model.Contentlet newFormatContentlet = 
					conAPI.convertFatContentletToContentlet(cont);
				ContentletServices.invalidate(newFormatContentlet, true);
				//writes the contentlet object to a file
				ContentletMapServices.invalidate(newFormatContentlet, true);
			}
		}
	}

	public void _moveWebAsset(ActionRequest req, ActionResponse res, PortletConfig config, ActionForm form, User user, Class webAssetClass,
			String webKey) throws Exception 
			{
		Logger.debug(this, "I'm moving the link");

		// gets the current container being edited from the request object
		WebAsset webAsset = (WebAsset) req.getAttribute(webKey);		

		//gets folder parent
		String parentInode = req.getParameter("parent");

		if (parentInode != null && parentInode.length() != 0 && !parentInode.equalsIgnoreCase("")) 
		{
			Folder parent = (Folder) InodeFactory.getInode(parentInode, Folder.class); 
			Folder oldParent = APILocator.getFolderAPI().findParentFolder(webAsset, user, false);
			super._moveWebAsset(req, res, config, form, user, Link.class,WebKeys.LINK_EDIT);			
			RefreshMenus.deleteMenu(oldParent, parent);
		}
			}
}