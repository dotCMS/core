/*
 * CharsetEncodingFilter.java
 *
 * Created on 24 October 2007
 */
package com.dotmarketing.filters;

import static com.liferay.util.CookieUtil.COOKIES_SECURE_FLAG;

import com.dotmarketing.util.Config;
import com.liferay.util.CookieUtil;
import java.io.IOException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


public class CookiesFilter implements Filter {
	
	public void init(FilterConfig arg0) throws ServletException {
		if ( com.dotmarketing.util.CookieUtil.ALWAYS.equals(Config.getStringProperty(COOKIES_SECURE_FLAG, com.dotmarketing.util.CookieUtil.HTTPS))
				|| com.dotmarketing.util.CookieUtil.HTTPS.equals(Config.getStringProperty(COOKIES_SECURE_FLAG, com.dotmarketing.util.CookieUtil.HTTPS)) ){
			Config.CONTEXT.getSessionCookieConfig().setSecure(true);
		}
	}

	public void doFilter(ServletRequest request, ServletResponse response,
			FilterChain filterChain) throws IOException, ServletException {
		
		HttpServletRequest req = (HttpServletRequest) request;
		HttpServletResponse res = new CookieServletResponse(response);
		CookieUtil.setCookiesSecurityHeaders(req, res); 
        filterChain.doFilter(req, res);
	}
	
	public void destroy() {
	}
}
