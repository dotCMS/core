/*
 * Created on Apr 7, 2005
 *
 */
package com.dotmarketing.velocity;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


import com.dotcms.util.CollectionsUtils;
import com.dotcms.util.SecurityUtils;
import com.dotmarketing.util.*;
import org.apache.jasper.security.SecurityUtil;
import org.apache.velocity.runtime.parser.node.BooleanPropertyExecutor;
import org.apache.velocity.tools.view.context.ChainedContext;

import java.io.IOException;

/**
 * @author maria
 */
public class ClientVelocityServlet extends VelocityServlet {

    private final PortletURLUtil PORTLET_URL_UTIL = new PortletURLUtil();
    private static String IN_FRAME_PARAMETER_NAME = "in_frame";

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    protected void service(HttpServletRequest req, HttpServletResponse response) throws ServletException, IOException {
        boolean adminMode = PageRequestModeUtil.isAdminMode( req.getSession() );
        boolean notRedirect = notRedirect( req );

        if ( adminMode  && !notRedirect ){
            String pageUrl = req.getAttribute(Constants.ORIGINAL_REQUEST_URL_HTTP_HEADER).toString();

            if (pageUrl != null && pageUrl.indexOf(IN_FRAME_PARAMETER_NAME) == -1) {
                String redirectURL = PORTLET_URL_UTIL.getPortletUrl(PortletID.SITE_BROWSER,
                        CollectionsUtils.map("url", pageUrl));
                response.sendRedirect(redirectURL);
                return;
            }
        }

        super.service(req, response);
    }

    /**
     * Return true if:
     *
     * <ul>
     *     <li>The url content the dotAdmin String</li>
     *     <li>or If teh Referer header content the dotAdmin String</li>
     * </ul>
     *
     * @param req
     * @return
     */
    private boolean notRedirect(HttpServletRequest req) {
        String refererValue = req.getHeader(Constants.REFERER_URL_HTTP_HEADER);
        boolean containerParameter = Boolean.parseBoolean(req.getParameter("container"));

        return (refererValue != null && (refererValue.contains( "host_id=" )
                    || refererValue.contains( "container=true" )
                    || refererValue.contains(PortletURLUtil.URL_ADMIN_PREFIX )
                    || refererValue.contains( "p_p_id=" + PortletID.SITE_BROWSER )))
                || containerParameter;
    }

    //EACH CLIENT MAY HAVE ITS OWN VARIABLES
	public void _setClientVariablesOnContext(HttpServletRequest request, ChainedContext context) {
        String URI = request.getRequestURI();
        String serverName = request.getServerName();
        boolean inStore = false;
        if (serverName.equals(Config.getStringProperty("FSP_SERVER_NAME")))
        {
        	inStore = true;
        }
        context.put("inStore",inStore);
        
        String FSP_SERVER_NAME = Config.getStringProperty("FSP_SERVER_NAME");
        context.put("FSP_SERVER_NAME",FSP_SERVER_NAME);
	}
}
