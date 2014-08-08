package com.dotmarketing.osgi.portlet;

import com.dotcms.repackage.javax.portlet.PortletConfig;
import com.dotcms.repackage.javax.portlet.RenderRequest;
import com.dotcms.repackage.javax.portlet.RenderResponse;
import com.dotcms.repackage.javax.portlet.WindowState;
import com.dotcms.repackage.org.apache.struts.action.ActionForm;
import com.dotcms.repackage.org.apache.struts.action.ActionForward;
import com.dotcms.repackage.org.apache.struts.action.ActionMapping;
import com.dotmarketing.portal.struts.DotPortletAction;

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