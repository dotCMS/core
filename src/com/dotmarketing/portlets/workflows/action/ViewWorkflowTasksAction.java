package com.dotmarketing.portlets.workflows.action;

import com.dotcms.repackage.portlet.javax.portlet.PortletConfig;
import com.dotcms.repackage.portlet.javax.portlet.RenderRequest;
import com.dotcms.repackage.portlet.javax.portlet.RenderResponse;
import javax.servlet.jsp.PageContext;

import com.dotcms.repackage.struts.org.apache.struts.action.ActionForm;
import com.dotcms.repackage.struts.org.apache.struts.action.ActionForward;
import com.dotcms.repackage.struts.org.apache.struts.action.ActionMapping;

import com.dotmarketing.portal.struts.DotPortletAction;
import com.dotmarketing.util.Logger;

/**
 * 
 * @author David Torres
 * @version $Revision: 1.0 $ $Revision: 1.5 $
 * 
 */
public class ViewWorkflowTasksAction extends DotPortletAction {

	/*
	 * @see com.liferay.portal.struts.PortletAction#render(com.dotcms.repackage.struts.org.apache.struts.action.ActionMapping,
	 *      com.dotcms.repackage.struts.org.apache.struts.action.ActionForm, com.dotcms.repackage.portlet.javax.portlet.PortletConfig,
	 *      com.dotcms.repackage.portlet.javax.portlet.RenderRequest, com.dotcms.repackage.portlet.javax.portlet.RenderResponse)
	 */
	public ActionForward render(ActionMapping mapping, ActionForm form, PortletConfig config, RenderRequest req,
			RenderResponse res) throws Exception {

		Logger.debug(this, "Running ViewWorkflowTasksAction!!!!=" + req.getWindowState());

		try {

			return mapping.findForward("portlet.ext.workflows.view_workflow_tasks");

		} catch (Exception e) {
			req.setAttribute(PageContext.EXCEPTION, e);
			return mapping.findForward(com.liferay.portal.util.Constants.COMMON_ERROR);
		}
	}



}