/*
 * WebSessionFilter
 * 
 * A filter that recognizes return users who have chosen to have their login
 * information remembered. Creates a valid WebSession object and passes it a
 * contact to use to fill its information
 *  
 */
package com.dotmarketing.filters;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.struts.Globals;
import org.apache.struts.action.ActionMessage;
import org.apache.struts.action.ActionMessages;

import com.dotmarketing.util.Logger;
import com.dotmarketing.util.WebKeys;

public class LoginRequiredFilter implements Filter {


    public void destroy() {

    }

    
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
       
    	HttpServletResponse response = (HttpServletResponse) res;
        HttpServletRequest request = (HttpServletRequest) req;
        HttpSession session = request.getSession(false);

        boolean ADMIN_MODE = (session.getAttribute(com.dotmarketing.util.WebKeys.ADMIN_MODE_SESSION) != null);

        // if we are not logged in, go to login page
        if (session.getAttribute(WebKeys.CMS_USER) == null && !ADMIN_MODE) {
        	Logger.warn(this.getClass(), 
                    "Doing LoginRequiredFilter for RequestURI: " + request.getRequestURI() + "?" + request.getQueryString());

            //if we don't have a redirect yet
            session.setAttribute(WebKeys.REDIRECT_AFTER_LOGIN, request.getRequestURI() + "?" + request.getQueryString());

            ActionMessages ams = new ActionMessages();
            ams.add(Globals.MESSAGE_KEY, new ActionMessage("message.login.required"));
            session.setAttribute(Globals.MESSAGE_KEY, ams);
            response.sendError(401);
            return;
        }

        chain.doFilter(req, response);
    }

    public void init(FilterConfig con) throws ServletException {

    }
}