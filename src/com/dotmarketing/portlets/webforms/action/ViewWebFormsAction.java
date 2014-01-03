package com.dotmarketing.portlets.webforms.action;

import com.dotcms.repackage.portlet.javax.portlet.PortletConfig;
import com.dotcms.repackage.portlet.javax.portlet.RenderRequest;
import com.dotcms.repackage.portlet.javax.portlet.RenderResponse;

import com.dotcms.repackage.struts.org.apache.struts.action.ActionForm;
import com.dotcms.repackage.struts.org.apache.struts.action.ActionForward;
import com.dotcms.repackage.struts.org.apache.struts.action.ActionMapping;

import com.dotmarketing.util.Logger;
import com.liferay.portal.struts.PortletAction;

public class ViewWebFormsAction extends PortletAction {

    public ActionForward render(ActionMapping mapping, ActionForm form, PortletConfig config, RenderRequest req,
            RenderResponse res) throws Exception {

        Logger.debug(this, "Going to: portlet.ext.virtuallinks.view");
        return mapping.findForward("portlet.ext.webforms.view");
    }

}
