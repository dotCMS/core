package com.dotmarketing.portlets.htmlpages.action;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.PortletConfig;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionMapping;

import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.factories.PreviewFactory;
import com.dotmarketing.portal.struts.DotPortletAction;
import com.dotmarketing.portlets.htmlpages.model.HTMLPage;
import com.dotmarketing.util.ActivityLogger;
import com.dotmarketing.util.HostUtil;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.model.User;
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

			Logger.debug(this, "Calling Retrieve method");
			_retrieveWebAsset(req, res, config, form, user, HTMLPage.class, WebKeys.HTMLPAGE_EDIT);

			// wraps request to get session object
			ActionRequestImpl reqImpl = (ActionRequestImpl) req;
			HttpServletRequest hreq = reqImpl.getHttpServletRequest();

			// gets the session object for the messages
			HttpSession session = hreq.getSession();

			session.setAttribute(com.dotmarketing.util.WebKeys.EDIT_MODE_SESSION, "true");
			session.setAttribute(com.dotmarketing.util.WebKeys.PREVIEW_MODE_SESSION, null);
			session.setAttribute(com.dotmarketing.util.WebKeys.ADMIN_MODE_SESSION, "true");

			HTMLPage htmlPage = _previewHTMLPages(req, user);

			ActivityLogger.logInfo(this.getClass(), "save HTMLpage action", "User " + user.getPrimaryKey() + " save page" + htmlPage.getTitle(), HostUtil.hostNameUtil(req, _getUser(req)));

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

	private HTMLPage _previewHTMLPages(ActionRequest req, User user) throws Exception {

		// gets html page being previewed
		HTMLPage htmlPage = (HTMLPage) req.getAttribute(WebKeys.HTMLPAGE_EDIT);

		String language = req.getParameter("language");
		if (!UtilMethods.isSet(language)) {
			language = req.getParameter(com.dotmarketing.util.WebKeys.HTMLPAGE_LANGUAGE);
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