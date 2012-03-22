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
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.time.FastDateFormat;

import com.dotmarketing.util.Constants;

public class CacheImagesFilter implements Filter {
	FastDateFormat df = FastDateFormat.getInstance(Constants.RFC2822_FORMAT, TimeZone.getTimeZone("GMT"), Locale.US);
    public void destroy() {

    }

    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException,
            ServletException {
        HttpServletResponse response = (HttpServletResponse) res;
        int _daysCache = 30;
        GregorianCalendar expiration = new GregorianCalendar();
        expiration.add(java.util.Calendar.DAY_OF_MONTH, _daysCache);
        int seconds = (_daysCache * 24 * 60 * 60);
        response.setHeader("dotCacheImages", "yes");
        response.setHeader("Expires", df.format(expiration.getTime()));
        response.setHeader("Cache-Control", "public, max-age=" + seconds);
        

        chain.doFilter(req, response);
    }

	public void init(FilterConfig arg0) throws ServletException {
		// TODO Auto-generated method stub
		
	}

}
