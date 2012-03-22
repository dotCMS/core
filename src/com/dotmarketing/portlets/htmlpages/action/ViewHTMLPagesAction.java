package com.dotmarketing.portlets.htmlpages.action;

import javax.portlet.PortletConfig;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.servlet.jsp.PageContext;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

import com.dotmarketing.portal.struts.DotPortletAction;
import com.dotmarketing.portlets.htmlpages.model.HTMLPage;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.model.User;
import com.liferay.portal.util.Constants;

/**
 * <a href="ViewQuestionsAction.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Maria Ahues
 * @version $Revision: 1.4 $
 *
 */
public class ViewHTMLPagesAction extends DotPortletAction {

	/*
	 * @see com.liferay.portal.struts.PortletAction#render(org.apache.struts.action.ActionMapping, org.apache.struts.action.ActionForm, javax.portlet.PortletConfig, javax.portlet.RenderRequest, javax.portlet.RenderResponse)
	 */
	public ActionForward render(
			ActionMapping mapping, ActionForm form, PortletConfig config,
			RenderRequest req, RenderResponse res)
	throws Exception {

		Logger.debug(this, "Running ViewHTMLPagesAction!!!!");

		try {
			//gets the user
			User user = _getUser(req);

			
			com.liferay.portlet.RenderRequestImpl reqImpl = (com.liferay.portlet.RenderRequestImpl) req;
			HttpServletRequest httpReq = reqImpl.getHttpServletRequest();
			// gets the session object for the messages
			HttpSession session = httpReq.getSession();

			String template = (String) session.getAttribute(WebKeys.SEARCH_TEMPLATE_ID);
			if (req.getParameter("template") != null)
				template = req.getParameter("template");
			if (template != null)
				session.setAttribute(WebKeys.SEARCH_TEMPLATE_ID, template);
			if (!InodeUtils.isSet(template)) {
				template = "";
			}

			_viewWebAssets(req, user, HTMLPage.class, "htmlpage", WebKeys.HTMLPAGES_VIEW_COUNT, WebKeys.HTMLPAGES_VIEW, WebKeys.HTMLPAGE_QUERY, WebKeys.HTMLPAGE_SHOW_DELETED, WebKeys.HTMLPAGE_HOST_CHANGED, template);

			return mapping.findForward("portlet.ext.htmlpages.view_htmlpages");
		}
		catch (Exception e) {
			req.setAttribute(PageContext.EXCEPTION, e);
			return mapping.findForward(Constants.COMMON_ERROR);
		}
	}

}
