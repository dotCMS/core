/*
 * Created on Apr 7, 2005
 *
 */
package com.dotmarketing.velocity;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.dotcms.util.SecurityUtils;
import com.dotmarketing.util.PageRequestModeUtil;
import com.dotmarketing.util.URLEncoder;
import org.apache.jasper.security.SecurityUtil;
import org.apache.velocity.runtime.parser.node.BooleanPropertyExecutor;
import org.apache.velocity.tools.view.context.ChainedContext;

import com.dotmarketing.util.Config;

import java.io.IOException;

/**
 * @author maria
 */
public class ClientVelocityServlet extends VelocityServlet {

    private URLEncoder encoder = new URLEncoder();
    private static String IN_FRAME_PARAMETER_NAME = "in_frame";

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    protected void service(HttpServletRequest req, HttpServletResponse response) throws ServletException, IOException {
        boolean ADMIN_MODE = PageRequestModeUtil.isAdminMode( req.getSession() );
        boolean notRedirect = notRedirect( req );

        if ( ADMIN_MODE  && !notRedirect ){
            String pageUrl = req.getAttribute("javax.servlet.forward.request_uri").toString();

            if (pageUrl != null && pageUrl.indexOf(IN_FRAME_PARAMETER_NAME) == -1) {
                String redirectURL = getRedirectURL(pageUrl);
                response.sendRedirect(redirectURL);
                return;
            }
        }

        super.service(req, response);
    }

    public String getRedirectURL(String pageUrl){
        return String.format("/dotAdmin/#/c/site-browser?&url=%s", encoder.encode( pageUrl ));
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
        String refererValue = req.getHeader("Referer");

        return (refererValue != null && (refererValue.contains( "host_id=" )
                    || refererValue.contains( "fromAngular=true" )
                    || refererValue.contains( "dotAdmin" )
                    || refererValue.contains( "p_p_id=site-browser")));
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
