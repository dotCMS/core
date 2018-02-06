package com.dotcms.vanityurl.action;

import com.dotcms.repackage.javax.portlet.PortletConfig;
import com.dotcms.repackage.javax.portlet.RenderRequest;
import com.dotcms.repackage.javax.portlet.RenderResponse;
import com.dotcms.repackage.javax.portlet.WindowState;
import com.dotmarketing.portal.struts.DotPortletAction;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

public class ViewVanityURLAction extends DotPortletAction {
	
	public ActionForward render(ActionMapping mapping, ActionForm form,
			PortletConfig config, RenderRequest req, RenderResponse res)
	throws Exception {
		
		if (req.getWindowState().equals(WindowState.NORMAL)) {
			return mapping.findForward("portlet.ext.virtuallinks.view");
		} else {			
			return mapping.findForward("portlet.ext.virtuallinks.view_virtuallinks");

		}
	}

}
