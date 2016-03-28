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
import java.io.StringWriter;
import java.util.Date;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import com.dotcms.repackage.org.apache.commons.lang.time.FastDateFormat;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.web.HostWebAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.util.Logger;

public class ThreadNameFilter implements Filter {
	FastDateFormat df = FastDateFormat.getInstance("MM-dd-yyyy hh:mm:ss z");

	private static final ThreadLocal<Date> startDate = new ThreadLocal<Date>() {
		@Override
		protected Date initialValue() {
			return new Date();
		}
	};

	public void destroy() {

	}

	public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
		HostWebAPI hapi = WebAPILocator.getHostWebAPI();
		String threadName = Thread.currentThread().getName();
		try{
			// Log only when the path is a page
			HttpServletRequest request = (HttpServletRequest) req;
			String uri = request.getRequestURI();
	
			Host host;
			try {
				host = hapi.getCurrentHost(request);
			} catch (Exception e) {
				Logger.error(this, "Unable to retrieve current request host for URI " + uri);
				throw new ServletException(e.getMessage(), e);
			}
			long languageId = WebAPILocator.getLanguageWebAPI().getLanguage(request).getId();
	
			String queryString = request.getQueryString();
			boolean adminMode = (request.getSession(false) != null && request.getSession().getAttribute(
					com.dotmarketing.util.WebKeys.ADMIN_MODE_SESSION) != null);
			String referer = request.getHeader("Referer");
			StringWriter sw = new StringWriter();
			sw.append("url:");
			sw.append(request.getMethod());
			sw.append("//");
			sw.append(host.getHostname());
			sw.append(uri);
			sw.append(" | lang:" + languageId);
			sw.append(" | ip:");
			sw.append(request.getRemoteAddr());
			sw.append(" | Admin:" + adminMode);
			sw.append(" | start:" + df.format(startDate.get()));
			if(referer!=null&& referer.length()>0){
				sw.append("  ref:");
				sw.append(referer.replace('"', '\''));
			}
			
			if(queryString!=null&& queryString.length()>0){
				sw.append("  ?");
				sw.append(queryString.replace('"', '\''));
			}
			
			
			
			Thread.currentThread().setName(sw.toString());
	
			chain.doFilter(req, res);
		}
		finally{
			Thread.currentThread().setName(threadName);
		}
	}

	public void init(FilterConfig arg0) throws ServletException {


	}

}
