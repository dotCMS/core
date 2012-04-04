package com.dotmarketing.portlets.htmlpages.action;

import java.net.URLDecoder;
import java.net.URLEncoder;

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
import com.dotmarketing.beans.WebAsset;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.IdentifierCache;
import com.dotmarketing.business.IdentifierFactory;
import com.dotmarketing.cache.LiveCache;
import com.dotmarketing.cache.WorkingCache;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.factories.InodeFactory;
import com.dotmarketing.factories.WebAssetFactory;
import com.dotmarketing.menubuilders.RefreshMenus;
import com.dotmarketing.portal.struts.DotPortletAction;
import com.dotmarketing.portal.struts.DotPortletActionInterface;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.folders.business.FolderFactory;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpages.factories.HTMLPageFactory;
import com.dotmarketing.portlets.htmlpages.model.HTMLPage;
import com.dotmarketing.portlets.htmlpages.struts.HTMLPageForm;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.Validator;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.User;
import com.liferay.portal.struts.ActionException;
import com.liferay.portal.util.Constants;
import com.liferay.portlet.ActionRequestImpl;
import com.liferay.util.servlet.SessionMessages;

/**
 * @author Maria
 */

public class EditHTMLPageAction extends DotPortletAction implements
		DotPortletActionInterface {

	protected HostAPI hostAPI = APILocator.getHostAPI();

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

		Logger.debug(this, "EditHTMLPageAction cmd=" + cmd);

		HibernateUtil.startTransaction();

		User user = _getUser(req);

		try {
			Logger.debug(this, "Calling Retrieve method");
			_retrieveWebAsset(req, res, config, form, user, HTMLPage.class,
					WebKeys.HTMLPAGE_EDIT);

		} catch (Exception ae) {
			_handleException(ae, req);
			return;
		}

        /*
		 * We are editing the html page
		 */
		if ((cmd != null) && cmd.equals(Constants.EDIT)) {
			try {
				Logger.debug(this, "Calling Edit Method");
				_editWebAsset(req, res, config, form, user);

			} catch (Exception ae) {
				if ((referer != null) && (referer.length() != 0)) {
					if (ae.getMessage().equals(WebKeys.EDIT_ASSET_EXCEPTION)) {

                        //The web asset edit threw an exception because it's
                        // locked so it should redirect back with message
						java.util.Map<String,String[]> params = new java.util.HashMap<String,String[]>();
						params.put("struts_action",new String[] { "/ext/director/direct" });
						params.put("cmd", new String[] { "editHTMLPage" });
						params.put("htmlPage", new String[] { req.getParameter("inode") });
						params.put("referer", new String[] { URLEncoder.encode(referer, "UTF-8") });

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

		if ((cmd != null) && cmd.equals("newedit")) {
			try {

				Logger.debug(this, "Calling newEdit Method");
				String url = req.getParameter("url");
				String folderPath = url.substring(0, url.lastIndexOf("/"));
				String hostId = req.getParameter("hostId");
				Host host = APILocator.getHostAPI().find(hostId, user, false);
				Folder folder = APILocator.getFolderAPI().createFolders(folderPath,host,user,false);

				String page = url.substring(url.lastIndexOf("/")+1, url.length());
				String pageName = page.replaceAll(
						"." + Config.getStringProperty("VELOCITY_PAGE_EXTENSION"), "");

                String friendlyName  = (UtilMethods.isSet(req.getParameter("friendlyName")))
                        ? req.getParameter("friendlyName")
                                : pageName;


				HTMLPage htmlpage = new HTMLPage();
				htmlpage.setParent(folder.getInode());
				htmlpage.setPageUrl(url);
				htmlpage.setTitle(pageName);
				htmlpage.setFriendlyName(friendlyName);

				req.setAttribute(WebKeys.HTMLPAGE_EDIT,htmlpage);


				HTMLPageForm htmlForm = new HTMLPageForm();
				htmlForm.setParent(folder.getInode());
				htmlForm.setSelectedparent(folder.getName());
				htmlForm.setSelectedparentPath(APILocator.getIdentifierAPI().find(folder).getPath());
				htmlForm.setPageUrl(pageName);
				htmlForm.setTitle(pageName);
				htmlForm.setFriendlyName(friendlyName);



				BeanUtils.copyProperties(form, htmlpage);
				BeanUtils.copyProperties(form, htmlForm);

			} catch (Exception ae) {
				if ((referer != null) && (referer.length() != 0)) {
					if (ae.getMessage().equals(WebKeys.EDIT_ASSET_EXCEPTION)) {

                        //The web asset edit threw an exception because it's
                        // locked so it should redirect back with message
						java.util.Map<String,String[]> params = new java.util.HashMap<String,String[]>();
						params.put("struts_action",new String[] { "/ext/director/direct" });
						params.put("cmd", new String[] { "editHTMLPage" });
						params.put("htmlPage", new String[] { req.getParameter("inode") });
						params.put("referer", new String[] { URLEncoder.encode(referer, "UTF-8") });

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
		 * If we are updating the html page, copy the information from the
		 * struts bean to the hbm inode and run the update action and return to
		 * the list
		 */
		if ((cmd != null) && cmd.equals(Constants.ADD)) {
			try {

				if (Validator.validate(req, form, mapping)) {

					Logger.debug(this, "Calling Save Method");

					_saveWebAsset(req, res, config, form, user);
					String subcmd = req.getParameter("subcmd");

					if ((subcmd != null) && subcmd.equals(com.dotmarketing.util.Constants.PUBLISH))
					{
					    Logger.debug(this, "Calling Publish Method");
					    _publishWebAsset(req, res, config, form, user, WebKeys.HTMLPAGE_FORM_EDIT);
					}

					//Obtain the URL for the preview page
					// pasing null referer to force http://jira.dotmarketing.net/browse/DOTCMS-5971
					referer = getPreviewPageURL(req, null);
					
					HTMLPage htmlpage=(HTMLPage) req.getAttribute(WebKeys.HTMLPAGE_EDIT);
					if(htmlpage.isLocked())
					    APILocator.getVersionableAPI().setLocked(htmlpage, false, user);

					_sendToReferral(req, res, referer);
				}

			} catch (Exception ae) {
				_handleException(ae, req);
			}

		}

		/*
		 * If we are deleteing the html page, run the delete action and return
		 * to the list
		 *
		 */
		else if ((cmd != null) && cmd.equals(Constants.DELETE)) {
			try {
			    Logger.debug(this, "Calling Delete Method");

			    _deleteWebAsset(req, res, config, form, user,
						WebKeys.HTMLPAGE_EDIT);

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
				WebAsset webAsset = (WebAsset) req.getAttribute(WebKeys.HTMLPAGE_EDIT);
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
					WebAsset webAsset = (WebAsset) InodeFactory.getInode(inode,HTMLPage.class);
					returnValue &= WebAssetFactory.deleteAsset(webAsset,user);
				}
				if(returnValue)
				{
					SessionMessages.add(httpReq,"message","message.htmlpage.full_delete");
				}
				else
				{
					SessionMessages.add(httpReq,"error","message.htmlpage.full_delete.error");
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
		 * If we are undeleting the html page, run the undelete action and
		 * return to the list
		 *
		 */
		else if ((cmd != null)
				&& cmd.equals(com.dotmarketing.util.Constants.UNDELETE)) {
			try {
			    Logger.debug(this, "Calling UnDelete Method");

				_undeleteWebAsset(req, res, config, form, user,
						WebKeys.HTMLPAGE_EDIT);

			} catch (Exception ae) {
				_handleException(ae, req);
				return;
			}
			_sendToReferral(req, res, referer);

		}
		/*
		 * If we are deleting the html page version, run the deeleteversion
		 * action and return to the list
		 */
		else if ((cmd != null)
				&& cmd.equals(com.dotmarketing.util.Constants.DELETEVERSION)) {
			try {
			    Logger.debug(this, "Calling Delete Version Method");

			    _deleteVersionWebAsset(req, res, config, form, user,
						WebKeys.HTMLPAGE_EDIT);

			} catch (Exception ae) {
				_handleException(ae, req);
				return;
			}
			_sendToReferral(req, res, referer);
		}
		/*
		 * If we are unpublishing the html page, run the unpublish action and
		 * return to the list
		 */
		else if ((cmd != null)
				&& cmd.equals(com.dotmarketing.util.Constants.UNPUBLISH)) {
			try {
			    Logger.debug(this, "Calling Unpublish Method");

				_unPublishWebAsset(req, res, config, form, user,
						WebKeys.HTMLPAGE_EDIT);

			} catch (Exception ae) {
				_handleException(ae, req);
				return;
			}
			_sendToReferral(req, res, referer);

		}
		/*
		 * If we are getting the html page version back, run the getversionback
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
		 * If we are getting the html page versions, run the assetversions
		 * action and return to the list
		 */
		else if ((cmd != null)
				&& cmd.equals(com.dotmarketing.util.Constants.ASSETVERSIONS)) {
			try {
			    Logger.debug(this, "Calling Get Versions Method");

				_getVersionsWebAsset(req, res, config, form, user,
						WebKeys.HTMLPAGE_EDIT, WebKeys.HTMLPAGE_VERSIONS);

			} catch (Exception ae) {
				_handleException(ae, req);
				return;
			}
		}
		/*
		 * If we are unlocking the html page, run the unlock action and return
		 * to the list
		 */
		else if ((cmd != null)
				&& cmd.equals(com.dotmarketing.util.Constants.UNLOCK)) {
			try {
			    Logger.debug(this, "Calling Unlock Method");

				_unLockWebAsset(req, res, config, form, user,
						WebKeys.HTMLPAGE_EDIT);

			} catch (Exception ae) {
				_handleException(ae, req);
				return;
			}
			_sendToReferral(req, res, referer);

		}
		/*
		 * If we are copying the html page, run the copy action and return to
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
		 * If we are moving the html page, run the copy action and return to the
		 * list
		 */
		else if ((cmd != null)
				&& cmd.equals(com.dotmarketing.util.Constants.MOVE)) {
			try {
			    Logger.debug(this, "Calling Move Method");

			    _moveWebAsset(req, res, config, form, user);

			} catch (ActionException ae) {
			} catch (Exception ae) {
                _handleException(ae, req);
            }
			_sendToReferral(req, res, referer);
		} else
		    Logger.debug(this, "Unspecified Action");

		HibernateUtil.commitTransaction();

		setForward(req, "portlet.ext.htmlpages.edit_htmlpage");
	}

	///// ************** ALL METHODS HERE *************************** ////////

	protected String getPreviewPageURL(ActionRequest req, String referer) {
		//Obtain the URL for the preview page
		//Retreiving the current HTMLPage
		HTMLPage existingHTMLPage = (HTMLPage) req.getAttribute(WebKeys.HTMLPAGE_EDIT);
		if (!InodeUtils.isSet(existingHTMLPage.getInode()) || !UtilMethods.isSet(referer))
		{
		    existingHTMLPage = (HTMLPage) req.getAttribute(WebKeys.HTMLPAGE_FORM_EDIT);
		    Host host;
			try {
				host = hostAPI.findParentHost(existingHTMLPage, APILocator.getUserAPI().getSystemUser(), false);
			} catch (DotDataException e) {
				Logger.error(EditHTMLPageAction.class, e.getMessage(), e);
				throw new DotRuntimeException(e.getMessage(), e);
			} catch (DotSecurityException e) {
				Logger.error(EditHTMLPageAction.class, e.getMessage(), e);
				throw new DotRuntimeException(e.getMessage(), e);
			}
			if(host==null){
				Folder parentFolder = (Folder)req.getAttribute(WebKeys.PARENT_FOLDER);
				if(parentFolder!=null && UtilMethods.isSet(parentFolder.getHostId())){
					try {
						host = hostAPI.find(parentFolder.getHostId(), APILocator.getUserAPI().getSystemUser(), false);
					} catch (DotDataException e) {
						Logger.error(EditHTMLPageAction.class, e.getMessage(), e);
						throw new DotRuntimeException(e.getMessage(), e);
					} catch (DotSecurityException e) {
						Logger.error(EditHTMLPageAction.class, e.getMessage(), e);
						throw new DotRuntimeException(e.getMessage(), e);
					}
				}
			}
			if(host!=null){
				String host_id = host.getIdentifier();
				//go to preview mode for this page
				java.util.Map<String,String[]> params = new java.util.HashMap<String,String[]>();
				params.put("struts_action",new String[] {"/ext/htmlpages/preview_htmlpage"});
				params.put("inode",new String[] { existingHTMLPage.getInode() + "" });
				params.put("host_id",new String[] { host_id + "" });
				return com.dotmarketing.util.PortletURLUtil.getActionURL(req,WindowState.MAXIMIZED.toString(),params);
			}
		}
		return referer;
	}


	//this method is implemented again here because it has some differences
	// with cache updates!!!!
	public void _moveWebAsset(ActionRequest req, ActionResponse res,
			PortletConfig config, ActionForm form, User user) throws Exception {

        //wraps request to get session object
        ActionRequestImpl reqImpl = (ActionRequestImpl) req;
        HttpServletRequest httpReq = reqImpl.getHttpServletRequest();

        Logger.debug(this, "I'm moving the web page");

		//gets the current container being edited from the request object
		HTMLPage webAsset = (HTMLPage) req.getAttribute(WebKeys.HTMLPAGE_EDIT);

		String parentInode = req.getParameter("parent");

		if (parentInode != null && parentInode.length() != 0
				&& !parentInode.equals("0")) {
			//the new parent is being passed through the request
			Folder parent = (Folder) InodeFactory.getInode(parentInode,
					Folder.class);
			if(HTMLPageFactory.moveHTMLPage(webAsset, parent)) {
				SessionMessages.add(httpReq, "message", "message.htmlpage.move");
			} else {
                SessionMessages.add(httpReq, "error", "message.htmlpage.error.htmlpage.exists");
                throw new ActionException("Another page with the same page url exists in this folder");
			}
		}

	}

	public void _editWebAsset(ActionRequest req, ActionResponse res,
			PortletConfig config, ActionForm form, User user) throws Exception {

		//calls edit method from super class that returns parent folder
		Folder parentFolder = super._editWebAsset(req, res, config, form, user,
				WebKeys.HTMLPAGE_EDIT);

		//setting parent folder path and inode on the form bean
		HTMLPageForm hf = (HTMLPageForm) form;

		hf.setSelectedparent(parentFolder.getName());
		hf.setParent(parentFolder.getInode());
		try {
		hf.setSelectedparentPath(APILocator.getIdentifierAPI().find(parentFolder).getPath());
		} catch (Exception e) {
			Logger.info(this, e.getMessage());
		}
		//This can't be done on the WebAsset so it needs to be done here.
		HTMLPage htmlpage = (HTMLPage) req.getAttribute(WebKeys.HTMLPAGE_EDIT);
		htmlpage.setParent(parentFolder.getInode());

		//removes the extension .jsp from page url
		String pageName = htmlpage.getPageUrl();
		if (pageName != null) {
			pageName = pageName.replaceAll(
					"." + Config.getStringProperty("VELOCITY_PAGE_EXTENSION"), "");
		}
		//to remove the page extension on the bean
		hf.setPageUrl(pageName);

	}

	public void _saveWebAsset(ActionRequest req, ActionResponse res,
			PortletConfig config, ActionForm form, User user) throws Exception {

		//wraps request to get session object
		ActionRequestImpl reqImpl = (ActionRequestImpl) req;
		HttpServletRequest httpReq = reqImpl.getHttpServletRequest();

		//Retreiving the current HTMLPage
		HTMLPage existingHTMLPage = (HTMLPage) req.getAttribute(WebKeys.HTMLPAGE_EDIT);

        boolean previousShowMenu = existingHTMLPage.isShowOnMenu();

		//parent folder or inode for this file
		Folder parentFolder = APILocator.getFolderAPI().find(req.getParameter("parent"), user, false);
		//http://jira.dotmarketing.net/browse/DOTCMS-5899
		Identifier id = null;
		if(UtilMethods.isSet(existingHTMLPage.getIdentifier()))
			id=APILocator.getIdentifierAPI().find(existingHTMLPage);
		if(id!=null && UtilMethods.isSet(id.getInode())){
			String URI = id.getURI();
			String uriPath = URI.substring(0,URI.lastIndexOf("/")+1);
			if(!uriPath.equals(APILocator.getIdentifierAPI().find(parentFolder).getPath())){
				id.setURI(APILocator.getIdentifierAPI().find(parentFolder).getPath()+existingHTMLPage.getPageUrl());
				APILocator.getIdentifierAPI().save(id);
			}
		}

		req.setAttribute(WebKeys.PARENT_FOLDER, parentFolder);	// Since the above query is expensive, save it into request object

		String template = req.getParameter("template");

		if(!UtilMethods.isSet(template)){
			SessionMessages.add(httpReq, "error", "message.htmlpage.select.Template");
			throw new Exception(LanguageUtil.get(user,"message.htmlpage.select.Template"));
		}

		//Adds template children from selected box
		Identifier templateIdentifier = APILocator.getIdentifierAPI().find(template);

		// Checking permissions
		_checkPermissions(existingHTMLPage, parentFolder, user, httpReq);

		// parent identifier for this file
		Identifier identifier = null;
		if(UtilMethods.isSet(existingHTMLPage.getInode()))
			identifier=APILocator.getIdentifierAPI().find(existingHTMLPage);

		// Creating the new page
		HTMLPage newHtmlPage = new HTMLPage();
		req.setAttribute(WebKeys.HTMLPAGE_FORM_EDIT, newHtmlPage);
		BeanUtils.copyProperties(newHtmlPage, form);
		if (UtilMethods.isSet(newHtmlPage.getFriendlyName())) {
			newHtmlPage.setFriendlyName(newHtmlPage.getFriendlyName());
		} else {
			newHtmlPage.setFriendlyName(newHtmlPage.getTitle());
		}
		// add VELOCITY_PAGE_EXTENSION to the pagename
		if (!newHtmlPage.getPageUrl().endsWith(
				"." + Config.getStringProperty("VELOCITY_PAGE_EXTENSION"))) {
			newHtmlPage.setPageUrl(newHtmlPage.getPageUrl() + "."
					+ Config.getStringProperty("VELOCITY_PAGE_EXTENSION"));
		}

		// Some checks

		// Get asset host based on the parentFolder of the asset
		Host host = hostAPI.findParentHost(parentFolder, user, false);

		// get an identifier based on this new uri
		Identifier testIdentifier = APILocator.getIdentifierAPI().find(host, newHtmlPage.getURI(parentFolder));

		// if this is a new htmlpage and there is already an identifier with
		// this uri, return
		if (!InodeUtils.isSet(existingHTMLPage.getInode())
				&& InodeUtils.isSet(testIdentifier.getInode())) {
			existingHTMLPage.setParent(parentFolder.getInode());
			SessionMessages.add(httpReq, "error",
					"message.htmlpage.error.htmlpage.exists");
			throw new ActionException(
					"Another page with the same page url exists in this folder");
		}
		// if this is an existing htmlpage and there is already an identifier
		// with this uri, return
		else if (InodeUtils.isSet(existingHTMLPage.getInode())
				&& (!testIdentifier.getInode().equalsIgnoreCase(
						identifier.getInode()))
				&& InodeUtils.isSet(testIdentifier.getInode())) {
			SessionMessages.add(httpReq, "error",
					"message.htmlpage.error.htmlpage.exists");
			// when there is an error saving should unlock working asset
			WebAssetFactory.unLockAsset(existingHTMLPage);
			throw new ActionException(
					"Another page with the same page url exists in this folder");
		}

		if (UtilMethods.isSet(template)) {
			newHtmlPage.setTemplateId(templateIdentifier.getInode());
		}

		HTMLPage workingAsset = null;
		// Versioning
		if (InodeUtils.isSet(existingHTMLPage.getInode())) {
			// Creation the version asset
			WebAssetFactory.createAsset(newHtmlPage, user.getUserId(),
					parentFolder, identifier, false);
			HibernateUtil.flush();

			LiveCache.removeAssetFromCache(existingHTMLPage);
			workingAsset = (HTMLPage) WebAssetFactory.saveAsset(newHtmlPage,
					identifier);

			// if we need to update the identifier
			if (InodeUtils.isSet(parentFolder.getInode())
					&& !workingAsset.getURI(parentFolder).equals(
							identifier.getURI())) {

				// assets cache
				LiveCache.removeAssetFromCache(newHtmlPage);
				LiveCache.removeAssetFromCache(existingHTMLPage);
				LiveCache.clearCache(host.getIdentifier());
				WorkingCache.removeAssetFromCache(newHtmlPage);
				CacheLocator.getIdentifierCache().removeFromCacheByVersionable(newHtmlPage);

				CacheLocator.getIdentifierCache().removeFromCacheByVersionable(existingHTMLPage);

				APILocator.getIdentifierAPI().updateIdentifierURI(workingAsset,
						parentFolder);

			}

			CacheLocator.getHTMLPageCache().remove(workingAsset);


		} // Creating the new page
		else {
			WebAssetFactory.createAsset(newHtmlPage, user.getUserId(),
					parentFolder);
			workingAsset = newHtmlPage;
		}

		// Setting HTML Page relations
		/*if (UtilMethods.isSet(template)) {
			// Removing old template
			Identifier currentTemplateIdent = APILocator.getIdentifierAPI().find(workingAsset.getTemplateId());

			if (currentTemplateIdent == null
					|| !InodeUtils.isSet(currentTemplateIdent.getInode())
					|| (InodeUtils.isSet(currentTemplateIdent.getInode()) && (!currentTemplateIdent
							.getInode().equalsIgnoreCase(
									templateIdentifier.getInode())))) {
				// Updating the template that has changed
				for (Tree tree : TreeFactory.getTreesByChildAndRelationType(
						workingAsset, "parentPageTemplate")) {
					if (!tree.getParent().equalsIgnoreCase(
							currentTemplateIdent.getInode()))
						TreeFactory.deleteTree(tree);
				}
				Tree tree = TreeFactory.getTree(
						currentTemplateIdent.getInode(), workingAsset
								.getInode(), "parentPageTemplate");
				tree.setParent(templateIdentifier.getInode());
				tree.setChild(workingAsset.getInode());
				tree.setRelationType("parentPageTemplate");
				TreeFactory.saveTree(tree);
			}

		}*/

		HibernateUtil.flush();
		HibernateUtil.getSession().refresh(workingAsset);

		// Refreshing the menues
		if (previousShowMenu != workingAsset.isShowOnMenu()) {
			// existing folder with different show on menu ... need to
			// regenerate menu
			// RefreshMenus.deleteMenus();
			RefreshMenus.deleteMenu(workingAsset);
		}

		// Setting the new working page to publish tasks
		req.setAttribute(WebKeys.HTMLPAGE_FORM_EDIT, workingAsset);
		String this_page = workingAsset.getURI(parentFolder);
		req.setAttribute(WebKeys.HTMLPAGE_REFERER, this_page);

	}

	public void _copyWebAsset(ActionRequest req, ActionResponse res,
			PortletConfig config, ActionForm form, User user) throws Exception {

		//wraps request to get session object
		ActionRequestImpl reqImpl = (ActionRequestImpl) req;
		HttpServletRequest httpReq = reqImpl.getHttpServletRequest();

		//gets the current template being edited from the request object
		HTMLPage currentHTMLPage = (HTMLPage) req
				.getAttribute(WebKeys.HTMLPAGE_EDIT);

		//gets folder parent
		String parentInode = req.getParameter("parent");
		Folder parent = null;
		if (parentInode != null && parentInode.length() != 0
				&& !parentInode.equalsIgnoreCase("")) {
			//the parent is being passed through the request
			parent = (Folder) InodeFactory.getInode(parentInode, Folder.class);
		    Logger.debug(this, "Parent Folder=" + parent.getInode());
		} else {
			parent = APILocator.getFolderAPI().findParentFolder(currentHTMLPage, user,false);

		    Logger.debug(this, "Parent Folder=" + parent.getInode());
		}

		//Checking permissions
        _checkCopyAndMovePermissions(currentHTMLPage, parent, user, httpReq,"copy");

        HTMLPageFactory.copyHTMLPage (currentHTMLPage, parent);

		SessionMessages.add(httpReq, "message", "message.htmlpage.copy");
	}

	public void _getVersionBackWebAsset(ActionRequest req, ActionResponse res,
			PortletConfig config, ActionForm form, User user) throws Exception {

		super._getVersionBackWebAsset(req, res, config, form, user, HTMLPage.class, WebKeys.HTMLPAGE_EDIT);

	}
}
