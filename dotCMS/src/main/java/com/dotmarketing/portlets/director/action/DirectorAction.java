/**
 * Copyright (c) 2000-2004 Liferay, LLC. All rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.dotmarketing.portlets.director.action;

import static com.dotmarketing.business.PermissionAPI.PERMISSION_CAN_ADD_CHILDREN;
import static com.dotmarketing.business.PermissionAPI.PERMISSION_READ;
import static com.dotmarketing.business.PermissionAPI.PERMISSION_WRITE;

import java.net.URLDecoder;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import com.dotcms.repackage.javax.portlet.ActionRequest;
import com.dotcms.repackage.javax.portlet.ActionResponse;
import com.dotcms.repackage.javax.portlet.PortletConfig;
import com.dotcms.repackage.javax.portlet.WindowState;
import com.dotcms.repackage.org.apache.struts.action.ActionForm;
import com.dotcms.repackage.org.apache.struts.action.ActionMapping;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.MultiTree;
import com.dotmarketing.beans.WebAsset;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.factories.InodeFactory;
import com.dotmarketing.factories.MultiTreeFactory;
import com.dotmarketing.factories.WebAssetFactory;
import com.dotmarketing.portal.struts.DotPortletAction;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ContentletVersionInfo;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.htmlpageasset.model.IHTMLPage;
import com.dotmarketing.portlets.links.model.Link;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.portlets.user.factories.UserPreferencesFactory;
import com.dotmarketing.portlets.user.model.UserPreference;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.User;
import com.liferay.portlet.ActionRequestImpl;
import com.liferay.util.ParamUtil;

public class DirectorAction extends DotPortletAction {

	private ContentletAPI conAPI = APILocator.getContentletAPI();
	private PermissionAPI perAPI = APILocator.getPermissionAPI();
	
	protected IHTMLPage loadPage(String inode, User user) throws DotDataException, DotSecurityException {
	    Identifier ident=APILocator.getIdentifierAPI().findFromInode(inode);
	    return APILocator.getHTMLPageAssetAPI().fromContentlet(APILocator.getContentletAPI().find(inode, user, false));
	    
	}

	/**
	 * Updates the modification date of the page that has been recently
	 * modified, i.e., the version info of the page using the default language 
	 * in the system.
	 * 
	 * @param htmlPage
	 *            - The Legacy or Content Page that has changed.
	 * @param user
	 *            - The user performing this action.
	 * @throws DotStateException
	 * @throws DotDataException
	 *             An error occurred when persisting the changes in the
	 *             database.
	 */
	protected void updatePageModDate(IHTMLPage htmlPage, User user) throws DotStateException, DotDataException {
		updatePageModDate(htmlPage, user, htmlPage.getLanguageId());
	}
	
	/**
	 * Updates the modification date of the page that has been recently
	 * modified, i.e., the version info of the page.
	 * 
	 * @param htmlPage
	 *            - The Legacy or Content Page that has changed.
	 * @param user
	 *            - The user performing this action.
	 * @param languageId
	 *            - The language ID of the content being saved.
	 * @throws DotStateException
	 * @throws DotDataException
	 *             An error occurred when persisting the changes in the
	 *             database.
	 */
	protected void updatePageModDate(IHTMLPage htmlPage, User user,
			long languageId) throws DotStateException, DotDataException {
		if (htmlPage.isContent()) {
			ContentletVersionInfo versionInfo = APILocator.getVersionableAPI()
					.getContentletVersionInfo(htmlPage.getIdentifier(),
							languageId);
			versionInfo.setVersionTs(new Date());
			APILocator.getVersionableAPI().saveContentletVersionInfo(
					versionInfo);
		}
	}

	/**
	 * Represents the entry point to a series of actions that can be performed
	 * on contentlets, pages, file assets, templates, and so on.
	 * 
	 * @param mapping
	 *            - The Struts action mapping.
	 * @param form
	 *            - The HTML form that was submitted to this action.
	 * @param config
	 *            - The configuration of the portlet that triggered the request.
	 * @param req
	 *            - The HTTP request object. Contains important information such
	 *            as the command and/or sub-command to execute, the referrer
	 *            page, etc.
	 * @param res
	 *            - The HTTP response object.
	 * @throws Exception
	 *             An error occurred during the execution of a command.
	 */
	public void processAction(
			 ActionMapping mapping, ActionForm form, PortletConfig config,
			 ActionRequest req, ActionResponse res)
		 throws Exception {

			String cmd = req.getParameter("cmd");
			String subcmd = ParamUtil.getString(req,"subcmd");
			String referer = (req.getParameter("referer") != null) ? URLDecoder.decode(req.getParameter("referer"), "UTF-8"): "/c";

			Logger.debug(DirectorAction.class, "DirectorAction :: referer=" + referer);

			//wraps request to get session object
			ActionRequestImpl reqImpl = (ActionRequestImpl)req;
			HttpServletRequest httpReq = reqImpl.getHttpServletRequest();
			//gets the session object for the messages
			HttpSession session = httpReq.getSession();

			Logger.debug(DirectorAction.class, "I'm inside the Director cmd = " + cmd);
			Logger.debug(DirectorAction.class, "I'm inside the Director subcmd = " + subcmd);
			Logger.debug(DirectorAction.class, "I'm inside the Director referer = " + referer);

			//get the user
			User user = _getUser(req);

			//to order menu items
			if (cmd!=null && cmd.equals("orderMenu")) {

				Logger.debug(DirectorAction.class, "Director :: orderMenu");

				java.util.Map params = new java.util.HashMap();
				params.put("struts_action",new String[] {"/ext/folders/order_menu"});
				params.put("path",new String[] { req.getParameter("path")});
				params.put("pagePath",new String[] { req.getParameter("pagePath")});
				if (req.getParameter("openAll")!=null) {
					params.put("openAll",new String[] { req.getParameter("openAll")});
				}
				params.put("hostId",new String[] { req.getParameter("hostId")});
				params.put("referer",new String[] { referer });
				
				params.put("startLevel", new String[] {req.getParameter("startLevel")});
				params.put("depth", new String [] {req.getParameter("depth")});

				String af = com.dotmarketing.util.PortletURLUtil.getActionURL(httpReq,WindowState.MAXIMIZED.toString(),params);

				_sendToReferral(req, res, af);

				return;
			} 
			if (cmd!=null && cmd.equals("orderContentlets")) {

				Logger.debug(DirectorAction.class, "Director :: orderContentlet");

				Container container = (Container) InodeFactory.getInode(req.getParameter("containerId"), Container.class);
				IHTMLPage htmlPage = loadPage(req.getParameter("pageId"),user);
				boolean hasReadPermissionOnContainer = perAPI.doesUserHavePermission(container, PERMISSION_READ, user, false);
				boolean hasWritePermissionOnPage = perAPI.doesUserHavePermission(htmlPage, PERMISSION_WRITE, user, false);
				
				if(!hasReadPermissionOnContainer || !hasWritePermissionOnPage) {
					throw new DotSecurityException("User has no permission to reorder content on container = " + req.getParameter("container") + " on page = " + req.getParameter("htmlPage"));
				}

				java.util.Map params = new java.util.HashMap();
				params.put("struts_action",new String[] {"/ext/contentlet/order_contentlets"});
				params.put("containerId",new String[] { req.getParameter("containerId")});
				params.put("pageId",new String[] { req.getParameter("pageId")});
				params.put("referer",new String[] { referer });
				


				String af = com.dotmarketing.util.PortletURLUtil.getActionURL(httpReq,WindowState.MAXIMIZED.toString(),params);

				_sendToReferral(req, res, af);

				return;
			}


            if (cmd!=null && cmd.equals("newHTMLPage")) {

				Logger.debug(DirectorAction.class, "Director :: editHTMLPage");

				java.util.Map params = new java.util.HashMap();
				
				String type=req.getParameter("HTMLPageType");
				String language = UtilMethods.isSet(req.getParameter("language"))?req.getParameter("language")
						:Long.toString(APILocator.getLanguageAPI().getDefaultLanguage().getId());
				
				if(type!=null && !type.equals("0")) {
    				params.put("struts_action",new String[] {"/ext/contentlet/edit_contentlet"});
    				params.put("cmd",new String[] { "new" });
    				params.put("selectedStructure", new String[] { type });
    				params.put("lang", new String[] { language });
    				params.put("referer", new String[] { referer });
    				params.put("folder", new String[] { req.getParameter("folder") });

				}
				else {
				    params.put("struts_action",new String[] {"/ext/htmlpages/edit_htmlpage"});
	                params.put("cmd",new String[] { "edit" });
	                params.put("inode",new String[] { "0" });
	                params.put("referer", new String[] { referer });
				}

				String af = com.dotmarketing.util.PortletURLUtil.getActionURL(httpReq,WindowState.MAXIMIZED.toString(),params);

				_sendToReferral(req, res, af);
				return;
			}
            
			if (cmd!=null && cmd.equals("editHTMLPage")) {
			    // this is specific to legacy html page 
			    
				Logger.debug(DirectorAction.class, "Director :: editHTMLPage");

				IHTMLPage htmlpage = loadPage(req.getParameter("htmlPage"), user);
				
				if ("unlockHTMLPage".equals(subcmd)) {
					APILocator.getVersionableAPI().setLocked(htmlpage, false, user);
				}

				java.util.Map params = new java.util.HashMap();
				params.put("struts_action",new String[] {"/ext/contentlet/edit_contentlet"});
				params.put("cmd",new String[] { "edit" });
				params.put("inode",new String[] { htmlpage.getInode() });
				params.put("referer",new String[] { referer });

				String af = com.dotmarketing.util.PortletURLUtil.getActionURL(httpReq,WindowState.MAXIMIZED.toString(),params);

				_sendToReferral(req, res, af);
				return;
			}
			if (cmd!=null && cmd.equals("viewStatistics")) {

				Logger.debug(DirectorAction.class, "Director :: editHTMLPage");

				IHTMLPage htmlPage = loadPage(req.getParameter("htmlPage"), user);


				java.util.Map params = new java.util.HashMap();
				params.put("struts_action",new String[] {"/ext/htmlpageviews/view_htmlpage_views"});
				params.put("htmlpage",new String[] { htmlPage.getInode() + "" });
				params.put("referer",new String[] { referer });

				String af = com.dotmarketing.util.PortletURLUtil.getRenderURL(httpReq,WindowState.MAXIMIZED.toString(),params);

				_sendToReferral(req, res, af);
				return;
			}

			if (cmd!=null && cmd.equals("editFile")) {

				Logger.debug(DirectorAction.class, "Director :: editFile");
				
				String fileAssetInode = "";
				
				if(UtilMethods.isSet(req.getParameter("file")))
					fileAssetInode = req.getParameter("file");
				else
					return;
				
				Identifier identifier = APILocator.getIdentifierAPI().findFromInode(fileAssetInode);
				
				if(identifier.getAssetType().equals("contentlet")){
					try {
						Contentlet cont = APILocator.getContentletAPI().find(fileAssetInode, user, false);
						
						
						java.util.Map params = new java.util.HashMap();
						params.put("struts_action",new String[] {"/ext/contentlet/edit_contentlet"});
						params.put("cmd",new String[] { "edit" });
						params.put("inode",new String[] { cont.getInode() + "" });
						params.put("referer",new String[] { referer });
						
						String af = com.dotmarketing.util.PortletURLUtil.getActionURL(httpReq,WindowState.MAXIMIZED.toString(),params);
						
						_sendToReferral(req, res, af);
					} catch (DotSecurityException e) {
						Logger.error(this, e.getMessage());
						return;
					}
				}else{
					try {
						//gets the current working asset
						WebAsset workingFile = (WebAsset) APILocator.getVersionableAPI().findWorkingVersion(identifier,APILocator.getUserAPI().getSystemUser(),false);

						if ("unlockFile".equals(subcmd)) {
							WebAssetFactory.unLockAsset(workingFile);
						}

						if (workingFile.isLocked() && !workingFile.getModUser().equals(user.getUserId())) {
							req.setAttribute(WebKeys.FILE_EDIT, workingFile);
							setForward(req,"portlet.ext.director.unlock_file");
							return;
						}
						else if (workingFile.isLocked()) {
							//it's locked by the same user
							WebAssetFactory.unLockAsset(workingFile);
						}

						java.util.Map params = new java.util.HashMap();
						params.put("struts_action",new String[] {"/ext/files/edit_file"});
						params.put("cmd",new String[] { "edit" });
						params.put("inode",new String[] { workingFile.getInode() + "" });
						params.put("referer",new String[] { referer });
						
						String af = com.dotmarketing.util.PortletURLUtil.getActionURL(httpReq,WindowState.MAXIMIZED.toString(),params);
						
						_sendToReferral(req, res, af);
					} catch (DotStateException e) {
						Logger.error(this, e.getMessage());
						return;
					} catch (DotSecurityException e) {
						Logger.error(this, e.getMessage());
						return;
					}
				}
				
				return;
			}

			if (cmd!=null && cmd.equals("editTemplate")) {

				Logger.debug(DirectorAction.class, "Director :: editTemplate");

				IHTMLPage htmlPage=new HTMLPageAsset();
				WebAsset workingTemplate = new Template();
				if (req.getParameter("htmlPage")!=null) {
				    htmlPage = loadPage(req.getParameter("htmlPage"),user);
				    workingTemplate = APILocator.getTemplateAPI().findWorkingTemplate(
				            htmlPage.getTemplateId(),user,false);
				} else if (req.getParameter("template")!=null) {
					workingTemplate = (Template) InodeFactory.getInode(req.getParameter("template"), Template.class);
				}

				if ("unlockTemplate".equals(subcmd)) {
					WebAssetFactory.unLockAsset(workingTemplate);
				}

				if (workingTemplate.isLocked() && !workingTemplate.getModUser().equals(user.getUserId())) {
					req.setAttribute(WebKeys.HTMLPAGE_EDIT, htmlPage);
					req.setAttribute(WebKeys.TEMPLATE_EDIT, workingTemplate);
					setForward(req,"portlet.ext.director.unlock_template");
					return;
				}
				else if (workingTemplate.isLocked()) {
					//it's locked by the same user
					WebAssetFactory.unLockAsset(workingTemplate);
				}

				java.util.Map params = new java.util.HashMap();
				params.put("struts_action",new String[] {"/ext/templates/edit_template"});
				params.put("cmd",new String[] { "edit" });
				params.put("inode",new String[] { workingTemplate.getInode() });
				params.put("referer",new String[] { referer });

				String af = com.dotmarketing.util.PortletURLUtil.getActionURL(httpReq,WindowState.MAXIMIZED.toString(),params);

				_sendToReferral(req, res, af);
				return;
			}

			if (cmd!=null && cmd.equals("publishHTMLPage")) {

				Logger.debug(DirectorAction.class, "Director :: publishHTMLPage");

				java.util.Map params = new java.util.HashMap();
				params.put("struts_action",new String[] {"/ext/htmlpages/publish_htmlpages"});
				params.put("cmd",new String[] { "prepublish" });
				params.put("publishInode",new String[] { req.getParameter("htmlPage") });
				params.put("referer",new String[] { referer });

				String af = com.dotmarketing.util.PortletURLUtil.getActionURL(httpReq,WindowState.MAXIMIZED.toString(),params);

				_sendToReferral(req, res, af);
				return;
			}


			if (cmd!=null && cmd.equals("editContainer")) {

				Logger.debug(DirectorAction.class, "Director :: editContainer" + subcmd);

				Container container = (Container) InodeFactory.getInode(req.getParameter("container"), Container.class);

				Identifier identifier = APILocator.getIdentifierAPI().find(container);
				//gets the current working asset
				WebAsset workingContainer = (WebAsset) APILocator.getVersionableAPI().findWorkingVersion(identifier,APILocator.getUserAPI().getSystemUser(),false);

				if ("unlockContainer".equals(subcmd)) {
					WebAssetFactory.unLockAsset(workingContainer);
				}
				if (workingContainer.isLocked() && !workingContainer.getModUser().equals(user.getUserId())) {
					req.setAttribute(WebKeys.CONTAINER_EDIT, workingContainer);
					setForward(req,"portlet.ext.director.unlock_container");
					return;
				}
				else if (workingContainer.isLocked()) {
					//it's locked by the same user
					WebAssetFactory.unLockAsset(workingContainer);
				}
				java.util.Map params = new java.util.HashMap();
				params.put("struts_action",new String[] {"/ext/containers/edit_container"});
				params.put("cmd",new String[] { "edit" });
				params.put("inode",new String[] { workingContainer.getInode() + "" });
				params.put("referer",new String[] { referer });

				String af = com.dotmarketing.util.PortletURLUtil.getActionURL(httpReq,WindowState.MAXIMIZED.toString(),params);

				_sendToReferral(req, res, af);
				return;
			}

			if (cmd!=null && cmd.equals("editLink")) {

				Logger.debug(DirectorAction.class, "Director :: editLink");

				String popup = req.getParameter("popup");
				Link link = (Link) InodeFactory.getInode(req.getParameter("link"), Link.class);

				Identifier identifier = APILocator.getIdentifierAPI().find(link);
				//gets the current working asset
				WebAsset workingLink = (WebAsset) APILocator.getVersionableAPI().findWorkingVersion(identifier,APILocator.getUserAPI().getSystemUser(),false);

				if ("unlockLink".equals(subcmd)) {
					WebAssetFactory.unLockAsset(workingLink);
				}
				if (workingLink.isLocked() && !workingLink.getModUser().equals(user.getUserId())) {
					req.setAttribute(WebKeys.LINK_EDIT, workingLink);
					if (UtilMethods.isSet(popup)) {
						setForward(req,"portlet.ext.director.unlock_popup_link");
						return;
					}
					else {
						setForward(req,"portlet.ext.director.unlock_link");
						return;
					}
				}
				else if (workingLink.isLocked()) {
					//it's locked by the same user
					WebAssetFactory.unLockAsset(workingLink);
				}
				String popURL = "";
				if (UtilMethods.isSet(popup)){
					popURL = "_popup";
				}
				java.util.Map params = new java.util.HashMap();
				params.put("struts_action",new String[] {"/ext/links/edit_link"});
				params.put("cmd",new String[] { "edit" });
				params.put("inode",new String[] { workingLink.getInode() + "" });
				params.put("popup",new String[] { popup });
				params.put("referer",new String[] { referer });
                params.put("child", new String[] { (req.getParameter("child")!=null) ? req.getParameter("child") : "" });
                params.put("page_width", new String[] { (req.getParameter("page_width")!=null) ? req.getParameter("page_width") : ""});
                params.put("browse", new String[] { (req.getParameter("browse")!=null) ? req.getParameter("browse") : "" });

				String af = com.dotmarketing.util.PortletURLUtil.getActionURL(httpReq,WindowState.MAXIMIZED.toString(),params);

				_sendToReferral(req, res, af);
				return;
			}

			if (cmd!=null && cmd.equals("addChild")) {

				try {
					Logger.debug(DirectorAction.class, "Director :: addChild");
					
					HibernateUtil.startTransaction();
					
					Contentlet contentlet = new Contentlet();
					String cInode = req.getParameter("contentlet");
					if(InodeUtils.isSet(cInode)){
						contentlet = conAPI.find(cInode, user, true);	
					}
					Container container = (Container) InodeFactory.getInode(req.getParameter("container"), Container.class);
					IHTMLPage htmlPage = loadPage(req.getParameter("htmlPage"), user);
	
					boolean hasPermissionOnContainer = perAPI.doesUserHavePermission(container, PERMISSION_READ, user, false);
					if(Config.getBooleanProperty("SIMPLE_PAGE_CONTENT_PERMISSIONING", true))
						hasPermissionOnContainer = true;
					
					boolean hasPermissionsOnPage = perAPI.doesUserHavePermission(htmlPage, PERMISSION_CAN_ADD_CHILDREN, user, false);
					boolean duplicateContentCheck  = false;
					
					if(!hasPermissionOnContainer || !hasPermissionsOnPage) {
						throw new DotSecurityException("User has no permission to add content on container = " + req.getParameter("container") + " on page = " + req.getParameter("htmlPage"));
					}
	
					Identifier identifier = APILocator.getIdentifierAPI().find(contentlet);
	
					Identifier htmlPageIdentifier = APILocator.getIdentifierAPI().find(htmlPage);
					Identifier containerIdentifier = APILocator.getIdentifierAPI().find(container);
	
	                if (InodeUtils.isSet(identifier.getInode()) && InodeUtils.isSet(htmlPageIdentifier.getInode()) && InodeUtils.isSet(containerIdentifier.getInode())) {
	                    MultiTree mTree = new MultiTree(htmlPageIdentifier.getInode(),containerIdentifier.getInode(),identifier.getInode());
	                    java.util.List<MultiTree> treeList=  MultiTreeFactory.getMultiTree(htmlPage, container);
	                    for (int i = 0; i < treeList.size(); i++) {
	                    	if(treeList.get(i).getChild().equals(identifier.getInode())){
	                    	duplicateContentCheck = true;
	                    	session.setAttribute("duplicatedErrorMessage","Content already exists in the same container on the page");
	                    	}
	                    	
	                    }
	                    if(!duplicateContentCheck){
							if (htmlPage.isContent()) {
								ContentletVersionInfo versionInfo = APILocator
										.getVersionableAPI()
										.getContentletVersionInfo(
												htmlPage.getIdentifier(),
												contentlet.getLanguageId());
								if (versionInfo != null) {
									MultiTreeFactory.saveMultiTree(mTree,
											contentlet.getLanguageId());
									updatePageModDate(htmlPage, user,
											contentlet.getLanguageId());
								} else {
									// The language in the page and the 
									// contentlet do not match
									long contentletLang = contentlet
											.getLanguageId();
									String language = APILocator.getLanguageAPI()
											.getLanguage(contentletLang)
											.getLanguage();
									Logger.error(this,
											"Creating MultiTree failed: Contentlet with identifier "
													+ htmlPage.getIdentifier()
													+ " does not exist in "
													+ language);
									String msg = MessageFormat
											.format(LanguageUtil
													.get(user,
															"message.htmlpage.error.addcontent.invalidlanguage"),
													language);
									throw new DotRuntimeException(msg);
								}
							} else {
								MultiTreeFactory.saveMultiTree(mTree);
								updatePageModDate(htmlPage, user);
							}
	                    }
	
	                } else {
	                    Logger.error(this, "Error found trying to associate the contentlet inode: " + contentlet.getInode() + "(iden: " + identifier.getInode() + ") " +
	                            "to the container: " + container.getInode() + "(iden: " + containerIdentifier.getInode() + ") " +
	                                    "of the page: " + htmlPage.getInode() + "(iden: " + htmlPageIdentifier.getInode() + ") " +
	                                            "the system was unable to find some the identifiers (tree error?)!");
	                }

				} catch (DotRuntimeException e) {
					Logger.error(this, "Unable to add content to page", e);
				} finally {
					try {
                        HibernateUtil.closeAndCommitTransaction();
					}catch(Exception e){
						session.setAttribute("duplicatedErrorMessage","Content already exists in the same container on the page");
						//res.sendRedirect(referer);
					}				
				}
				_sendToReferral(req, res, referer);
				return;

			}

			if (cmd!=null && cmd.equals("removeChild")) {

				try {

					Logger.debug(DirectorAction.class, "Director :: removeChild");
	
					HibernateUtil.startTransaction();
	
					Contentlet contentlet = new Contentlet();
					String cInode = req.getParameter("contentlet");
					if(InodeUtils.isSet(cInode)){
						contentlet = conAPI.find(cInode, user, true);	
					}
					Container container = (Container) InodeFactory.getInode(req.getParameter("container"), Container.class);
					IHTMLPage htmlPage = loadPage(req.getParameter("htmlPage"), user);
	
					boolean hasPermissionOnPage = perAPI.doesUserHavePermission(htmlPage, PERMISSION_CAN_ADD_CHILDREN, user, false);
					boolean hasPermissionOnContainer = perAPI.doesUserHavePermission(container, PERMISSION_READ, user, false);
					if(Config.getBooleanProperty("SIMPLE_PAGE_CONTENT_PERMISSIONING", true))
						hasPermissionOnContainer = true;
					
					if(!hasPermissionOnContainer || !hasPermissionOnPage) {
						throw new DotSecurityException("User has no permission to remove content from container = " + req.getParameter("container") + " on page = " + req.getParameter("htmlPage"));
					}
	
					Identifier identifier = APILocator.getIdentifierAPI().find(contentlet);
					Logger.debug(DirectorAction.class, "Identifier of Contentlet to be removed=" + identifier.getInode());
	
					Contentlet contentletWorking = conAPI.findContentletByIdentifier(identifier.getInode(), false, contentlet.getLanguageId(), user, true);
					Contentlet liveContentlet = conAPI.findContentletByIdentifier(identifier.getInode(), false, contentlet.getLanguageId(), user, true);
					Logger.debug(DirectorAction.class, "\n\nContentlet Working to be removed=" + contentletWorking.getInode());
	
					Identifier htmlPageIdentifier = APILocator.getIdentifierAPI().find(htmlPage);
					Identifier containerIdentifier = APILocator.getIdentifierAPI().find(container);
					MultiTree multiTree = MultiTreeFactory.getMultiTree(htmlPageIdentifier,containerIdentifier,identifier);
					Logger.debug(DirectorAction.class, "multiTree=" + multiTree);
					MultiTreeFactory.deleteMultiTree(multiTree);
	
					updatePageModDate(htmlPage, user);
				} catch (DotRuntimeException e) {
					Logger.error(this, "Unable to remove content from page", e);
				} finally {
					HibernateUtil.closeAndCommitTransaction();
				}
				_sendToReferral(req, res, referer);
				return;

			}

			if (cmd!=null && cmd.equals("makeHomePage")) {

				Logger.debug(DirectorAction.class, "Director :: makeHomePage");

				if (InodeUtils.isSet(req.getParameter("htmlPage"))) {
					IHTMLPage htmlPage = loadPage(req.getParameter("htmlPage"), user);
					Folder folder = APILocator.getHTMLPageAssetAPI().getParentFolder(htmlPage);
					
					UserPreference up = UserPreferencesFactory.getUserPreferenceValue(user.getUserId(),WebKeys.USER_PREFERENCE_HOME_PAGE);

					if (up.getId()>0) {
						up.setValue(htmlPage.getURI(folder));

					}
					else {
						up.setUserId(user.getUserId());
						up.setPreference(WebKeys.USER_PREFERENCE_HOME_PAGE);
						up.setValue(htmlPage.getURI(folder));
					}
					UserPreferencesFactory.saveUserPreference(up);
				}
				else {
					//the user clicked on set with no page that means unsetting the page
					UserPreferencesFactory.deleteUserPreference(user.getUserId(),WebKeys.USER_PREFERENCE_HOME_PAGE);
				}

				_sendToReferral(req, res, referer);
				return;
			}

			if (cmd!=null && cmd.equals("moveUp")) {

				Logger.debug(DirectorAction.class, "Director :: moveUp");
				Contentlet contentlet = new Contentlet();
				String cInode = req.getParameter("contentlet");
				if(InodeUtils.isSet(cInode)){
					contentlet = conAPI.find(cInode, user, true);	
				}
				Container container = (Container) InodeFactory.getInode(req.getParameter("container"), Container.class);
				IHTMLPage htmlPage = loadPage(req.getParameter("htmlPage"), user);
				
				boolean hasPermissionOnPage = perAPI.doesUserHavePermission(htmlPage, PERMISSION_CAN_ADD_CHILDREN, user, false);
				boolean hasPermissionOnContainer = perAPI.doesUserHavePermission(container, PERMISSION_READ, user, false);
				if(Config.getBooleanProperty("SIMPLE_PAGE_CONTENT_PERMISSIONING", true))
					hasPermissionOnContainer = true;
				
				if(!hasPermissionOnContainer || !hasPermissionOnPage) {
					throw new DotSecurityException("User has no permission to reorder content on container = " + req.getParameter("container") + " on page = " + req.getParameter("htmlPage"));
				}
				
				String staticContainer = req.getParameter("static");

				Logger.debug(DirectorAction.class, "staticContainer=" + staticContainer);

				java.util.List cletList = new ArrayList();
				String sort = (container.getSortContentletsBy() == null) ? "tree_order" : container.getSortContentletsBy();

				Identifier idenHtmlPage = APILocator.getIdentifierAPI().find(htmlPage);
				Identifier idenContainer = APILocator.getIdentifierAPI().find(container);
				cletList = conAPI.findPageContentlets(idenHtmlPage.getInode(),idenContainer.getInode(), sort, true,contentlet.getLanguageId(), user, false);
				Logger.debug(DirectorAction.class, "Number of contentlets = " + cletList.size());

				int newPosition = cletList.indexOf(contentlet) -1;

				if( newPosition >= 0 ) {  

					idenContainer = APILocator.getIdentifierAPI().find(container);
					idenHtmlPage = APILocator.getIdentifierAPI().find(htmlPage);
					int x = 0;
					Iterator i = cletList.iterator();
	
					while (i.hasNext()) {
	
						Identifier iden;
						MultiTree multiTree;
						Contentlet c = (Contentlet) i.next();
						
						Logger.debug(DirectorAction.class, "Contentlet inode = " + c.getInode());
	
						if( newPosition == x ) {
							iden = APILocator.getIdentifierAPI().find(contentlet);
							multiTree = MultiTreeFactory.getMultiTree(idenHtmlPage,idenContainer,iden);
							multiTree.setTreeOrder(x);
							MultiTreeFactory.saveMultiTree(multiTree, htmlPage.getLanguageId());
							x++;
						}
	
						if (!c.getInode().equalsIgnoreCase(contentlet.getInode())) {
							iden = APILocator.getIdentifierAPI().find(c);
							multiTree = MultiTreeFactory.getMultiTree(idenHtmlPage,idenContainer,iden);
							multiTree.setTreeOrder(x);
							MultiTreeFactory.saveMultiTree(multiTree, htmlPage.getLanguageId());
							x++;
						}
	
					}
				}
				_sendToReferral(req, res, referer);
				return;
			}

			if (cmd!=null && cmd.equals("moveDown")) {

				Logger.debug(DirectorAction.class, "Director :: moveDown");
				Contentlet contentlet = new Contentlet();
				String cInode = req.getParameter("contentlet");
				if(InodeUtils.isSet(cInode)){
					contentlet = conAPI.find(cInode, user, true);	
				}
				Container container = (Container) InodeFactory.getInode(req.getParameter("container"), Container.class);
				IHTMLPage htmlPage = loadPage(req.getParameter("htmlPage"), user);
				String staticContainer = req.getParameter("static");

				boolean hasPermissionOnPage = perAPI.doesUserHavePermission(htmlPage, PERMISSION_CAN_ADD_CHILDREN, user, false);
				boolean hasPermissionOnContainer = perAPI.doesUserHavePermission(container, PERMISSION_READ, user, false);
				if(Config.getBooleanProperty("SIMPLE_PAGE_CONTENT_PERMISSIONING", true))
					hasPermissionOnContainer = true;
				
				if(!hasPermissionOnContainer || !hasPermissionOnPage) {
					throw new DotSecurityException("User has no permission to reorder content on container = " + req.getParameter("container") + " on page = " + req.getParameter("htmlPage"));
				}				
				Logger.debug(DirectorAction.class, "staticContainer=" + staticContainer);

				java.util.List cletList = new ArrayList();
				String sort = (container.getSortContentletsBy() == null) ? "tree_order" : container.getSortContentletsBy();

                Identifier idenHtmlPage = APILocator.getIdentifierAPI().find(htmlPage);
                Identifier idenContainer = APILocator.getIdentifierAPI().find(container);
				cletList = conAPI.findPageContentlets(idenHtmlPage.getInode(),idenContainer.getInode(), sort, true,contentlet.getLanguageId(),user,false);
				Logger.debug(DirectorAction.class, "Number of contentlets = " + cletList.size());

				int newPosition = cletList.indexOf(contentlet) + 1;

				if( newPosition < cletList.size() ) {  

					idenContainer = APILocator.getIdentifierAPI().find(container);
					idenHtmlPage = APILocator.getIdentifierAPI().find(htmlPage);
					int x = 0;
					Iterator i = cletList.iterator();
	
					while (i.hasNext()) {
	
						Identifier iden;
						MultiTree multiTree;
						Contentlet c = (Contentlet) i.next();
						
						Logger.debug(DirectorAction.class, "Contentlet inode = " + c.getInode());
	
						if (!c.getInode().equalsIgnoreCase(contentlet.getInode())) {
							iden = APILocator.getIdentifierAPI().find(c);
							multiTree = MultiTreeFactory.getMultiTree(idenHtmlPage,idenContainer,iden);
							multiTree.setTreeOrder(x);
							MultiTreeFactory.saveMultiTree(multiTree, htmlPage.getLanguageId());
							x++;
						}
 
						if( newPosition == x ) {
							iden = APILocator.getIdentifierAPI().find(contentlet);
							multiTree = MultiTreeFactory.getMultiTree(idenHtmlPage,idenContainer,iden);
							multiTree.setTreeOrder(x);
							MultiTreeFactory.saveMultiTree(multiTree, htmlPage.getLanguageId());
							x++;
						}
	
					}
				}

				_sendToReferral(req, res, referer);
				return;

			}

			if (cmd!=null && cmd.equals("unlock")) {

				Logger.debug(DirectorAction.class, "Director :: unlock Contentlet");

				Contentlet contentlet = new Contentlet();
				String cInode = req.getParameter("contentlet");
				if(InodeUtils.isSet(cInode)){
					contentlet = conAPI.find(cInode, user, true);	
				}
				conAPI.unlock(contentlet, user,true);
			}
			
			if (cmd!=null && cmd.equals("createForm")) {

				Logger.debug(DirectorAction.class, "Director :: createForrm");
				java.util.Map params = new java.util.HashMap();
				params.put("struts_action",new String[] {"/ext/structure/edit_structure"});
				params.put("structureType",new String[] {Integer.toString(Structure.STRUCTURE_TYPE_FORM)});
				params.put("cmd",new String[] {"null"});
	
				String af = com.dotmarketing.util.PortletURLUtil.getActionURL(httpReq,WindowState.MAXIMIZED.toString(),params);

				_sendToReferral(req, res, af);
				return;
			}
			
			/*if(cmd!=null && cmd.equals("migrate")) {
			    try {
			        HibernateUtil.startTransaction();
    			    HTMLPage htmlPage = (HTMLPage) HibernateUtil.load(HTMLPage.class, req.getParameter("htmlPage"));
    			    APILocator.getHTMLPageAssetAPI().migrateLegacyPage(htmlPage, user, false);
    			    HibernateUtil.closeAndCommitTransaction();
			    }
			    catch(Exception ex) {
			        HibernateUtil.rollbackTransaction();
			        Logger.error(this, "can't migrate page inode "+req.getParameter("htmlPage"),ex);
			    }
			    
			    _sendToReferral(req, res, referer);
                return;
			}*/
			

			Contentlet contentlet = new Contentlet();
			String cInode = req.getParameter("contentlet");
			if(InodeUtils.isSet(cInode)){
				contentlet = conAPI.find(cInode, user, true);	
			}
			if(contentlet == null){
				throw new DotStateException("Trying to edit an invalid contentlet - inode:" + cInode);
			}
			Container container = (Container) InodeFactory.getInode(req.getParameter("container"), Container.class);
			
			Logger.debug(DirectorAction.class, "contentlet=" + contentlet.getInode());

			String contentletInode = "";
			if (InodeUtils.isSet(contentlet.getInode())) {

				Identifier identifier = APILocator.getIdentifierAPI().find(contentlet);
				//gets the current working asset
				Contentlet workingContentlet = conAPI.findContentletByIdentifier(identifier.getInode(), false, contentlet.getLanguageId(), user, false);

				Logger.debug(DirectorAction.class, "workingContentlet=" + workingContentlet.getInode());
				Logger.debug(DirectorAction.class, "workingContentlet.getModUser()=" + workingContentlet.getModUser());
				Logger.debug(DirectorAction.class, "workingContentlet.isLocked()=" + workingContentlet.isLocked());

				contentletInode = workingContentlet.getInode();
			}
			else {
				contentletInode = contentlet.getInode();

			}

			Logger.debug(DirectorAction.class, "Director :: Edit Contentlet");

			java.util.Map params = new java.util.HashMap();
			params.put("struts_action",new String[] {"/ext/contentlet/edit_contentlet"});

			String cmdAux = (cmd.equals("newedit") ? cmd : "edit");

			params.put("cmd",new String[] { cmdAux });
			params.put("htmlpage_inode",new String[] { req.getParameter("htmlPage") });
			params.put("contentcontainer_inode",new String[] { container.getInode() + "" });
			params.put("inode",new String[] { contentletInode + "" });
			if(InodeUtils.isSet(req.getParameter("selectedStructure"))){
				params.put("selectedStructure",new String[] { req.getParameter("selectedStructure") + "" });
			}
			params.put("lang",new String[] { (req.getParameter("language")!=null) ? req.getParameter("language") : "" });
			params.put("referer",new String[] { referer });

			String af = com.dotmarketing.util.PortletURLUtil.getActionURL(httpReq,WindowState.MAXIMIZED.toString(),params);

			_sendToReferral(req, res, af);
			return;


	}

}

