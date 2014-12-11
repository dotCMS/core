/*
 * WebSessionFilter
 *
 * A filter that recognizes return users who have
 * chosen to have their login information remembered.
 * Creates a valid WebSession object and
 * passes it a contact to use to fill its information
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
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.dotmarketing.util.Config;

public class SessionCookieFilter implements Filter {

    public void destroy() {

    }
    

    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException,
            ServletException {
        HttpServletResponse response = (HttpServletResponse) res;
        HttpServletRequest request = (HttpServletRequest) req;

		String cookiesSecureFlag = Config.getStringProperty("COOKIES_SECURE_FLAG", "https");

		String cookiesHttpOnly = Config.getBooleanProperty("COOKIES_HTTP_ONLY", true)?"; HttpOnly;":"";
		
		Cookie[] cookies = request.getCookies();
		
		if(cookies!=null) {
		
			for(Cookie cookie : cookies){
					if(cookiesSecureFlag.equals("always") || (cookiesSecureFlag.equals("https") && req.isSecure())) {
						response.setHeader("SET-COOKIE", cookie.getName() + "=" + cookie.getValue() + "; Path=/; secure" + cookiesHttpOnly);
					} else { 
						response.setHeader("SET-COOKIE", cookie.getName() + "=" + cookie.getValue() + "; Path=/; " + cookiesHttpOnly);
					}
				}
		
		}
		

        chain.doFilter(req, response);
    }
    public void init(FilterConfig config) throws ServletException {
    }
}
