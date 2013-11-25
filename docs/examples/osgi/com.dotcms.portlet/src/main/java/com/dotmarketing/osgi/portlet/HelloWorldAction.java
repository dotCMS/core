package com.dotmarketing.osgi.portlet;

import com.dotmarketing.portal.struts.DotPortletAction;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

import javax.portlet.PortletConfig;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;
import javax.portlet.WindowState;

public class HelloWorldAction extends DotPortletAction {

    public ActionForward render ( ActionMapping mapping, ActionForm form, PortletConfig config, RenderRequest req, RenderResponse res ) throws Exception {

        req.setAttribute( "hello", "Hello World" );

        if ( req.getWindowState().equals( WindowState.NORMAL ) ) {
            return mapping.findForward( "portlet.ext.plugins.hello.world.struts" );
        } else {
            return mapping.findForward( "portlet.ext.plugins.hello.world.struts.max" );
        }
    }

}