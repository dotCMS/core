package com.dotmarketing.filters;
import java.io.IOException;
import java.net.URLDecoder;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
public class SVNFilter implements Filter{

	public void destroy() {
		
		
	}

	 public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {

		 	HttpServletRequest request = (HttpServletRequest) req;
	        HttpServletResponse response = (HttpServletResponse) res;
	        String uri = request.getRequestURI();

	        uri = URLDecoder.decode(uri, "UTF-8");

			if (uri.contains(".svn/")) {
		     	response.sendError(403, "Forbidden");
	        	return;
		    }
			 chain.doFilter(request, response);
		
	}

	public void init(FilterConfig arg0) throws ServletException {
		   
	}

}
