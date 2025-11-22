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

import com.dotmarketing.beans.Host;
import com.dotmarketing.business.web.HostWebAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PageMode;
import java.io.IOException;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

public class ThreadNameFilter implements Filter {

    DateTimeFormatter df = DateTimeFormatter.ofPattern("MM-dd-yyyy hh:mm:ss z");

    private static final ThreadLocal<LocalDateTime> startDate = new ThreadLocal<LocalDateTime>() {
		@Override
        protected LocalDateTime initialValue() {
            return LocalDateTime.now();
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
			boolean adminMode =   PageMode.get(request) .isAdmin;
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
            sw.append(" | start:" + startDate.get().atZone(ZoneId.systemDefault()).format(df));
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
