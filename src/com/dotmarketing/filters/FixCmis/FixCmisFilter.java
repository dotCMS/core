package com.dotmarketing.filters.FixCmis;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.dotmarketing.util.Logger;

public class FixCmisFilter implements Filter {

	public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException,
			ServletException {
		if (req instanceof HttpServletRequest) {
			HttpServletRequest request = (HttpServletRequest) req;
			HttpServletResponse response = (HttpServletResponse) res;

		      if (request.getRequestURI().startsWith("/cmis/")) {
		          Logger.debug(this, "CMIS Filter Activated.");
		          FixCmisResponseWrapper wrappedResponse = new FixCmisResponseWrapper(response);
		          chain.doFilter(req, wrappedResponse);
		          wrappedResponse.finishResponse();
		          return;
		        }
			chain.doFilter(req, res);
		}
	}

	public void init(FilterConfig filterConfig) {
	}

	public void destroy() {
	}
}
