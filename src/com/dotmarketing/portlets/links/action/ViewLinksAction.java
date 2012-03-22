package com.dotmarketing.portlets.links.action;


import javax.portlet.PortletConfig;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;
import javax.servlet.jsp.PageContext;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

import com.dotmarketing.portal.struts.DotPortletAction;
import com.dotmarketing.portlets.links.model.Link;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.model.User;
import com.liferay.portal.util.Constants;

/**
 * <a href="ViewQuestionsAction.java.html"><b><i>View Source</i></b></a>
 *
 * @author  if(working==false){ author="Maria Ahues"; }else{ author="Rocco Maglio";
 * @version $Revision: 1.2 $
 *
 */
public class ViewLinksAction extends DotPortletAction {

	/*
	 * @see com.liferay.portal.struts.PortletAction#render(org.apache.struts.action.ActionMapping, org.apache.struts.action.ActionForm, javax.portlet.PortletConfig, javax.portlet.RenderRequest, javax.portlet.RenderResponse)
	 */
	public ActionForward render(
			ActionMapping mapping, ActionForm form, PortletConfig config,
			RenderRequest req, RenderResponse res)
		throws Exception {

		Logger.debug(this, "Running ViewLinksAction!!!!");

		try {
			//gets the user
			User user = _getUser(req);

			_viewWebAssets(req, user, Link.class, "link", WebKeys.LINKS_VIEW_COUNT, WebKeys.LINKS_VIEW, WebKeys.LINK_QUERY, WebKeys.LINK_SHOW_DELETED, WebKeys.LINK_HOST_CHANGED);
			return mapping.findForward("portlet.ext.links.view_links");
		}
		catch (Exception e) {
			req.setAttribute(PageContext.EXCEPTION, e);
			return mapping.findForward(Constants.COMMON_ERROR);
		}
	}
}