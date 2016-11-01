package com.dotmarketing.portlets.htmlpages.action;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import com.dotcms.repackage.javax.portlet.ActionRequest;
import com.dotcms.repackage.javax.portlet.ActionResponse;
import com.dotcms.repackage.javax.portlet.PortletConfig;
import com.dotcms.repackage.org.apache.struts.action.ActionForm;
import com.dotcms.repackage.org.apache.struts.action.ActionMapping;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.factories.PreviewFactory;
import com.dotmarketing.portal.struts.DotPortletAction;
import com.dotmarketing.portlets.contentlet.business.DotLockException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.htmlpageasset.model.IHTMLPage;
import com.dotmarketing.portlets.htmlpages.model.HTMLPage;
import com.dotmarketing.util.ActivityLogger;
import com.dotmarketing.util.HostUtil;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.User;
import com.liferay.portal.struts.ActionException;
import com.liferay.portlet.ActionRequestImpl;

/**
 * <a href="ViewQuestionsAction.java.html"><b><i>View Source</i></b></a>
 * 
 * @author Maria Ahues
 * @version $Revision: 1.5 $
 * 
 */
public class PreviewHTMLPageAction extends DotPortletAction {

	public void processAction(ActionMapping mapping, ActionForm form, PortletConfig config, ActionRequest req, ActionResponse res) throws Exception {

		Logger.debug(this, "Running PreviewHTMLPagesAction!!!!");

		try {

			// get the user
			User user = com.liferay.portal.util.PortalUtil.getUser(req);
			
			IHTMLPage webAsset;
			String inode=req.getParameter("inode");
            Identifier ident;
            try {
                ident = APILocator.getIdentifierAPI().findFromInode(inode);
            } catch (DotStateException e) {
                Logger.info(PreviewHTMLPageAction.class
                        , "Unable to find Identifier by Inode. Inode must have changed probably by conflicts fixing.");
                throw new ActionException(LanguageUtil.get(user,"message.htmlpage.inode.not.found"));

            }
            boolean contentLocked = false;
            boolean iCanLock = false;
            if("contentlet".equals(ident.getAssetType())) {
               
                Contentlet contentlet = APILocator.getHTMLPageAssetAPI().fromContentlet(APILocator.getContentletAPI().find(inode, user, false));
                
                try{
                	iCanLock = APILocator.getContentletAPI().canLock(contentlet, user);
                }
                catch(DotLockException e){
                	iCanLock=false;
                }
                	
                webAsset =(IHTMLPage) contentlet;
                
            }
            else {
                webAsset = (HTMLPage) APILocator.getVersionableAPI().findWorkingVersion(ident, user, false);
            }
            
            if(!APILocator.getPermissionAPI().doesUserHavePermission(webAsset, PermissionAPI.PERMISSION_READ, user)) {
                throw new ActionException(WebKeys.USER_PERMISSIONS_EXCEPTION);
            }
            
	        req.setAttribute(WebKeys.HTMLPAGE_EDIT, webAsset);	        
	        req.setAttribute(WebKeys.VERSIONS_INODE_EDIT, webAsset);
	        
			// wraps request to get session object
			ActionRequestImpl reqImpl = (ActionRequestImpl) req;
			HttpServletRequest hreq = reqImpl.getHttpServletRequest();

			// gets the session object for the messages
			HttpSession session = hreq.getSession();
			if(contentLocked && iCanLock){
				session.setAttribute(com.dotmarketing.util.WebKeys.EDIT_MODE_SESSION, "true");
				session.setAttribute(com.dotmarketing.util.WebKeys.PREVIEW_MODE_SESSION, null);
				session.setAttribute(com.dotmarketing.util.WebKeys.ADMIN_MODE_SESSION, "true");
			}
			else{
				session.setAttribute(com.dotmarketing.util.WebKeys.EDIT_MODE_SESSION, null);
				session.setAttribute(com.dotmarketing.util.WebKeys.PREVIEW_MODE_SESSION, "true");
				session.setAttribute(com.dotmarketing.util.WebKeys.ADMIN_MODE_SESSION, "true");
			}
			
			IHTMLPage htmlPage = _previewHTMLPages(req, user);

			ActivityLogger.logInfo(this.getClass(), "Preview HTMLpage action", "User " + user.getPrimaryKey() + " preview page " + htmlPage.getURI(), HostUtil.hostNameUtil(req, _getUser(req)));

			String previewPage = (String) req.getAttribute(WebKeys.HTMLPAGE_PREVIEW_PAGE);

			if ((previewPage != null) && (previewPage.length() != 0)) {
				_sendToReferral(req, res, previewPage);
			}

			PreviewFactory.setVelocityURLS(hreq);

			setForward(req, "portlet.ext.htmlpages.view_htmlpages");

		} catch (Exception e) {
			_handleException(e, req);
		}
	}

	private IHTMLPage _previewHTMLPages(ActionRequest req, User user) throws Exception {

		// gets html page being previewed
		IHTMLPage htmlPage = (IHTMLPage) req.getAttribute(WebKeys.HTMLPAGE_EDIT);

		Long language = null;
		if (htmlPage instanceof Contentlet) {
			language = ((Contentlet) htmlPage).getLanguageId();
		}

		Identifier identifier = APILocator.getIdentifierAPI().find(htmlPage);

		String livePage = (UtilMethods.isSet(req.getParameter("livePage")) && req.getParameter("livePage").equals("1")) ? "&livePage=1" : "";

		String identiferEncoded = identifier.getURI();
		identiferEncoded = UtilMethods.encodeURIComponent(identiferEncoded);

		if (UtilMethods.isSet(language)) {
			req.setAttribute(WebKeys.HTMLPAGE_PREVIEW_PAGE,
					identiferEncoded + "?" + com.dotmarketing.util.WebKeys.HTMLPAGE_LANGUAGE + "=" + language + "&host_id=" + identifier.getHostId() + livePage);
		} else {
			req.setAttribute(WebKeys.HTMLPAGE_PREVIEW_PAGE, identiferEncoded + "?host_id=" + identifier.getHostId() + livePage);
		}
		return htmlPage;
	}

	

}