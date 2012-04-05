package com.dotmarketing.logConsole.business;

import java.util.List;
import java.util.Map;
import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.PortletConfig;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import com.dotmarketing.logConsole.model.LogMapperRow;
import com.dotmarketing.portal.struts.DotPortletAction;


public class LogConfigAction extends DotPortletAction {

	private static List<LogMapperRow> loggingCriteria = null;

	@Override
	public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest req, HttpServletResponse res) throws Exception {

		System.out.println("EXECUTE LOG-CONFIGURATION ACTION");
		return super.execute(mapping, form, req, res);
	}

	@Override
	public void processAction(ActionMapping mapping, ActionForm form, PortletConfig config, ActionRequest req, ActionResponse res) throws Exception {

		
	}

	@Override
	public ActionForward render(ActionMapping mapping, ActionForm form, PortletConfig config, RenderRequest req, RenderResponse res) throws Exception {

		return mapping.findForward("portlet.ext.plugins.logMan.struts");
	}

}
