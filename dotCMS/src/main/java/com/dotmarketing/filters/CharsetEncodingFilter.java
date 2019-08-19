/*
 * CharsetEncodingFilter.java
 *
 * Created on 24 October 2007
 */
package com.dotmarketing.filters;

import com.dotcms.repackage.org.apache.struts.Globals;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.ServletResponseCharacterEncoding;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import com.liferay.portal.util.PortalUtil;
import java.io.IOException;
import java.util.Locale;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * Ensures the proper encoding 
 * of <tt>ServletRequest</tt> and <tt>ServletResponse</tt>
 * for the sake of i18n.
 * 
 * @author Dimitris Zavaliadis
 * @version 1.0
 * 
 * @author David Torres
 * @version 2.0
 */

public class CharsetEncodingFilter implements Filter {
	
	private static LanguageAPI langAPI =  APILocator.getLanguageAPI();
	private String CHARSET = null;
	
	public void init(FilterConfig arg0) throws ServletException {
		Logger.debug(this, "Initializing Language filter...");
		CHARSET = UtilMethods.getCharsetConfiguration();
	}

	public void doFilter(ServletRequest request, ServletResponse response,
			FilterChain filterChain) throws IOException, ServletException {
		
		//Ensure the proper encoding of request parameters
        request.setCharacterEncoding(CHARSET);
		
		// Wrap the response object
        response = new ServletResponseCharacterEncoding( (HttpServletRequest) request, (HttpServletResponse)response);

        
        //Handling the language selection
        if(request instanceof HttpServletRequest) {
        	
        	HttpServletRequest httpRequest = (HttpServletRequest) request;
        	
	        HttpSession session = httpRequest.getSession(false);
	
	        if(session != null) {
	        	
	        	String languageId = null;
	        	
	        	//If backend the locale is defined by the user profile
	        	if(((HttpServletRequest)request).getRequestURI().startsWith("/c/")) {
	        		if(  session.getAttribute(Globals.LOCALE_KEY) ==null){
		        		try {
		        			User user = PortalUtil.getUser(((HttpServletRequest)request));
							if(user != null) {
								Locale userLocale = user.getLocale(); 
					            session.setAttribute(Globals.LOCALE_KEY, userLocale);
						        session.setAttribute(com.dotmarketing.util.WebKeys.LOCALE, userLocale);
							} else {
								Locale userLocale = APILocator.getUserAPI().getDefaultUser().getLocale();
					            session.setAttribute(Globals.LOCALE_KEY, userLocale);
						        session.setAttribute(com.dotmarketing.util.WebKeys.LOCALE, userLocale);
							}
						} catch (Exception e) {
							Logger.warn(this, "Unable to retrieve user locale", e);
						}
	        		}
	        	} else {
	        	    //if frontend the locale is defined by the dotCMS frontend language session variables
	        	    // ALL work should be done here
        	        WebAPILocator.getLanguageWebAPI().checkSessionLocale(httpRequest);
	        	}
	        }
	
        }
        
        filterChain.doFilter(request, response);
	}
	
	public void destroy() {
		Logger.info(this, "Destroying character encoding filter...");
	}
}
