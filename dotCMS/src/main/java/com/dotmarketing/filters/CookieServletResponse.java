/*
 * ServletResponseCharacterEncoding.java
 *
 * Created on 24 October 2007
 */
package com.dotmarketing.filters;

import static com.liferay.util.CookieUtil.COOKIES_HTTP_ONLY;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;


import com.dotmarketing.util.Config;


public class CookieServletResponse extends HttpServletResponseWrapper {


	public CookieServletResponse(HttpServletRequest request, HttpServletResponse response) {
		super(response);
	}

    public CookieServletResponse(ServletRequest request, ServletResponse response) {
        this((HttpServletRequest) request,(HttpServletResponse) response);
    }
    
    @Override
    public void addCookie(Cookie cookie) {


        if (Config.getBooleanProperty(COOKIES_HTTP_ONLY, false)) {
            cookie.setHttpOnly(true);          
        }
        
        
        super.addCookie(cookie);
    }

	

	
	
	
	
}
