/*
 * CharsetEncodingFilter.java
 *
 * Created on 24 October 2007
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

import com.liferay.util.CookieUtil;


public class CookiesFilter implements Filter {
	
	public void init(FilterConfig arg0) throws ServletException {
	}

	public void doFilter(ServletRequest request, ServletResponse response,
			FilterChain filterChain) throws IOException, ServletException {
		
		HttpServletRequest req = (HttpServletRequest) request;
		HttpServletResponse res = (HttpServletResponse) response;
		CookieUtil.setCookiesSecurityHeaders(req, res); 
        filterChain.doFilter(request, response);
	}
	
	public void destroy() {
	}
}
