package com.dotmarketing.scripting.util;

import java.io.IOException;
import java.net.URLDecoder;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

public class CMSFilter extends com.dotmarketing.filters.CMSFilter {

	@Override
	public void doFilter(ServletRequest req, ServletResponse res,
			FilterChain chain) throws IOException, ServletException {
		HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;
        HttpSession session = request.getSession(false);
        String uri = request.getRequestURI();

        uri = URLDecoder.decode(uri, "UTF-8");

        if(uri.endsWith(".php")){
        	chain.doFilter(request, response);
        	return;
        }
		super.doFilter(req, res, chain);
	}
	
}
