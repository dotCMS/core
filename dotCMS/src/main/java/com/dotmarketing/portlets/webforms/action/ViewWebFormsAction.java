package com.dotmarketing.portlets.webforms.action;

import com.dotcms.repackage.javax.portlet.PortletConfig;
import com.dotcms.repackage.javax.portlet.RenderRequest;
import com.dotcms.repackage.javax.portlet.RenderResponse;
import com.dotmarketing.util.Logger;
import com.liferay.portal.struts.PortletAction;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

public class ViewWebFormsAction extends PortletAction {

    public ActionForward render(ActionMapping mapping, ActionForm form, PortletConfig config, RenderRequest req,
            RenderResponse res) throws Exception {

        Logger.debug(this, "Going to: portlet.ext.webforms.view");
        return mapping.findForward("portlet.ext.webforms.view");
    }

}
