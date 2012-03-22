package com.dotmarketing.portlets.contentratings.action;

import javax.portlet.PortletConfig;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

import com.dotmarketing.util.Logger;
import com.liferay.portal.struts.PortletAction;

public class ViewContentRatingsAction extends PortletAction {

    public ActionForward render(ActionMapping mapping, ActionForm form, PortletConfig config, RenderRequest req, RenderResponse res) throws Exception {
        Logger.debug(this, "Going to: portlet.ext.contentratings.view");
        return mapping.findForward("portlet.ext.contentratings.view");
    }
}