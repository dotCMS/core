package com.dotmarketing.filters;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

/*
 * @depreciated - clickstreams should be explicity recorded by CMS Filter
 * by: Will 2007-08-02
 */
public class ClickstreamFilter implements Filter {

	protected FilterConfig filterConfig;

	public final static String FILTER_APPLIED = "_clickstream_filter_applied";

	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException,
	ServletException {
/*
		Logger.debug(ClickstreamFilter.class, "Into the ClickstreamFilter");
		// Ensure that clickstream is recorded only once per request.
		if (request.getAttribute(FILTER_APPLIED) == null) {
			request.setAttribute(FILTER_APPLIED, Boolean.TRUE);

			//Log only when the path is a page
			HttpServletRequest req = (HttpServletRequest) request;
			String uri = req.getRequestURI().trim();
			String pageExt = Config.getStringProperty("VELOCITY_PAGE_EXTENSION");
			String lastPath = uri.substring(uri.lastIndexOf("/"), uri.length());
			
			if(uri.endsWith("/") || uri.endsWith(pageExt) || lastPath.indexOf(".") == -1) {
			
				Logger.debug(ClickstreamFilter.class, "Recording Clickstream for uri: " + uri);
				
				HttpSession session = req.getSession(false);

				if (session != null && session.getAttribute(WebKeys.CMS_USER) != null) {
	
					User user = (User) session.getAttribute(WebKeys.CMS_USER);
					UserProxy userProxy = UserProxyFactory.getUserProxy(user);
	
					if(!userProxy.isNoclicktracking()){
						ClickstreamFactory.addRequest((HttpServletRequest) request, ((HttpServletResponse) response));
					} 
		
				} else {
					ClickstreamFactory.addRequest((HttpServletRequest) request, ((HttpServletResponse) response));
				}

			}

		}
*/
		// pass the request on
		chain.doFilter(request, response);
	}

	public void init(FilterConfig filterConfig) throws ServletException {
		this.filterConfig = filterConfig;
	}

	public void destroy() {
	}

}