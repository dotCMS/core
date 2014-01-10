package com.dotmarketing.portlets.browser.action;

import com.dotcms.repackage.portlet.javax.portlet.PortletConfig;
import com.dotcms.repackage.portlet.javax.portlet.RenderRequest;
import com.dotcms.repackage.portlet.javax.portlet.RenderResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.PageContext;

import com.dotcms.repackage.struts.org.apache.struts.action.ActionForm;
import com.dotcms.repackage.struts.org.apache.struts.action.ActionForward;
import com.dotcms.repackage.struts.org.apache.struts.action.ActionMapping;

import com.dotmarketing.factories.PreviewFactory;
import com.liferay.portal.struts.PortletAction;
import com.liferay.portal.util.Constants;
import com.liferay.portlet.RenderRequestImpl;

/**
 *
 * @author  David Torres
 *
 */
public class ViewBrowserAction extends PortletAction {

	public ActionForward render(
			ActionMapping mapping, ActionForm form, PortletConfig config,
			RenderRequest req, RenderResponse res)
		throws Exception {
	    
		try {
			HttpServletRequest hreq = ((RenderRequestImpl)req).getHttpServletRequest();
			PreviewFactory.setVelocityURLS(hreq);
			return mapping.findForward("portlet.ext.browser.view_browser");
		}
		catch (Exception e) {
			req.setAttribute(PageContext.EXCEPTION, e);
			return mapping.findForward(Constants.COMMON_ERROR);
		}
	}

}