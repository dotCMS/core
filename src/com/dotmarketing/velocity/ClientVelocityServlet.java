/*
 * Created on Apr 7, 2005
 *
 */
package com.dotmarketing.velocity;

import javax.servlet.http.HttpServletRequest;

import org.apache.velocity.tools.view.context.ChainedContext;

import com.dotmarketing.util.Config;

/**
 * @author maria
 */
public class ClientVelocityServlet extends VelocityServlet {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

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
