package com.dotmarketing.portlets.roleadmin.action;

import javax.portlet.PortletConfig;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

import com.liferay.portal.struts.PortletAction;

public class ViewRolesAction extends PortletAction {

	@Override
	public ActionForward render(ActionMapping mapping, ActionForm form, PortletConfig config, RenderRequest req, RenderResponse res) throws Exception {
		return mapping.findForward("portlet.ext.roleadmin.view_roles");
	}

}
