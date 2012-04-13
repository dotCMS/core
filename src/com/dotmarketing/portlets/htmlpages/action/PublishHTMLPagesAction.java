package com.dotmarketing.portlets.htmlpages.action;

import java.net.URLDecoder;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.PortletConfig;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionMapping;

import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.exception.WebAssetException;
import com.dotmarketing.factories.InodeFactory;
import com.dotmarketing.factories.PublishFactory;
import com.dotmarketing.portal.struts.DotPortletAction;
import com.dotmarketing.portlets.htmlpages.model.HTMLPage;
import com.dotmarketing.util.ActivityLogger;
import com.dotmarketing.util.HostUtil;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.model.User;
import com.liferay.portlet.ActionRequestImpl;
import com.liferay.util.servlet.SessionMessages;

/**
 * <a href="ViewQuestionsAction.java.html"><b><i>View Source</i></b></a>
 * 
 * @author Maria Ahues
 * @version $Revision: 1.3 $
 * 
 */
public class PublishHTMLPagesAction extends DotPortletAction {

	@SuppressWarnings("unchecked")
	public void processAction(ActionMapping mapping, ActionForm form, PortletConfig config, ActionRequest req, ActionResponse res) throws Exception {

		String cmd = req.getParameter("cmd");

		Logger.debug(this, "Running PublishHTMLPagesAction!!!! cmd=" + cmd);

		String referer = req.getParameter("referer");
		if ((referer != null) && (referer.length() != 0)) {
			referer = URLDecoder.decode(referer, "UTF-8");
		}

		try {
			// get the user
			User user = _getUser(req);

			if ((cmd != null) && cmd.equals(com.dotmarketing.util.Constants.PREPUBLISH)) {
				_prePublishHTMLPages(req, user);

			}
			java.util.List relatedAssets = (java.util.List) req.getAttribute(WebKeys.HTMLPAGE_RELATED_ASSETS);
			java.util.List relatedWorkflows = (java.util.List) req.getAttribute(WebKeys.HTMLPAGE_RELATED_WORKFLOWS);

			if (((cmd != null) && cmd.equals(com.dotmarketing.util.Constants.PUBLISH))
					|| ((relatedAssets == null || relatedAssets.size() == 0) && (relatedWorkflows == null || relatedWorkflows.size() == 0))) {
				_publishHTMLPages(req, user);

				if ((referer != null) && (referer.length() != 0)) {
					_sendToReferral(req, res, referer);
				}
			}

			setForward(req, "portlet.ext.htmlpages.publish_htmlpages");

		} catch (Exception e) {
			_handleException(e, req);
		}
	}

	@SuppressWarnings("unchecked")
	private void _prePublishHTMLPages(ActionRequest req, User user) throws Exception {

		String[] publishInode = req.getParameterValues("publishInode");

		if (publishInode == null)
			return;

		// calls the publish factory to get related assets
		java.util.List relatedAssets = new java.util.ArrayList();
		java.util.List relatedWorkflows = new java.util.ArrayList();

		for (int i = 0; i < publishInode.length; i++) {

			HTMLPage htmlPage = (HTMLPage) InodeFactory.getInode(publishInode[i], HTMLPage.class);

			if (InodeUtils.isSet(htmlPage.getInode())) {
				// calls the asset factory edit

				// relatedWorkflows.addAll(WorkflowMessageFactory.getWorkflowMessageByHTMLPageWaitingForPublish(htmlPage));
				relatedAssets = PublishFactory.getUnpublishedRelatedAssets(htmlPage, relatedAssets, true, user, false);

			}
		}
		req.setAttribute(WebKeys.HTMLPAGE_RELATED_WORKFLOWS, relatedWorkflows);
		req.setAttribute(WebKeys.HTMLPAGE_RELATED_ASSETS, relatedAssets);

	}

	private void _publishHTMLPages(ActionRequest req, User user) throws Exception {

		String[] publishInode = req.getParameterValues("publishInode");

		if (publishInode == null)
			return;

		ActionRequestImpl reqImpl = (ActionRequestImpl) req;

		for (int i = 0; i < publishInode.length; i++) {
			HTMLPage htmlpage = (HTMLPage) InodeFactory.getInode(publishInode[i], HTMLPage.class);

			if (InodeUtils.isSet(htmlpage.getInode())) {
				// calls the asset factory edit
				try {
					PublishFactory.publishAsset(htmlpage, reqImpl.getHttpServletRequest());
					ActivityLogger.logInfo(PublishFactory.class, "Publishing HTMLpage action", "User " + user + " publishing page " + htmlpage.getTitle(), HostUtil.hostNameUtil(req, _getUser(req)));
					SessionMessages.add(reqImpl.getHttpServletRequest(), "message", "message.htmlpage_list.published");
				} catch (WebAssetException wax) {
					Logger.error(this, wax.getMessage(), wax);
					SessionMessages.add(reqImpl.getHttpServletRequest(), "error", "message.webasset.published.failed");
				}
			}
		}

	}


}