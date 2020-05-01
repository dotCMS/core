package com.dotcms.filters;

import java.io.IOException;
import java.net.URI;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import com.dotmarketing.util.Logger;
import com.liferay.util.StringPool;

/**
 * Filter created to wrap all the incoming requests to override the {@link
 * HttpServletRequest#getRequestURI()} method in order to normalize the requested URIs.
 */
public class NormalizationFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    final static String SLASH=StringPool.FORWARD_SLASH;
    final static String DOUBLESLASH=StringPool.FORWARD_SLASH + StringPool.FORWARD_SLASH;
    final static String QUESTION=StringPool.QUESTION ;
    final static String DOUBLEPEROIDS=StringPool.PERIOD + StringPool.PERIOD;
    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse,
            FilterChain filterChain) throws IOException, ServletException {

        HttpServletRequestWrapper requestWrapper = new HttpServletRequestWrapper(
                (HttpServletRequest) servletRequest) {

            @Override
            public String getRequestURI() {
                try {
                    /* Normalization is the process of removing unnecessary "." and ".." segments from the path component of a hierarchical URI.
                     1. Each "." segment is simply removed.
                     2. A ".." segment is removed only if it is preceded by a non-".." segment.
                     3. Normalization has no effect upon opaque URIs. (mailto:a@b.com)
                     */

                    String newNormal = URI.create(super.getRequestURI()).normalize().toString();
                    newNormal = newNormal.startsWith(DOUBLEPEROIDS) ? newNormal.replace(DOUBLEPEROIDS, "") : newNormal;
                    newNormal = newNormal.startsWith(SLASH) ? newNormal : SLASH + newNormal;

                    // this should not happen 
                    newNormal = newNormal.indexOf(QUESTION )>-1 ? newNormal.substring(0,newNormal.indexOf(StringPool.QUESTION)) : newNormal;
                    while(newNormal.indexOf(DOUBLESLASH)>-1) {
                        newNormal = newNormal.replace(DOUBLESLASH, SLASH);
                    }
                    return newNormal;
                }
                catch(IllegalArgumentException ill) {
                    Logger.warnAndDebug(this.getClass(),ill);
                    HttpServletResponse response= (HttpServletResponse) servletResponse;
                    try {
                        response.sendError(HttpServletResponse.SC_FORBIDDEN);
                        response.flushBuffer();
                    }
                    catch(Exception e) {}
                    return "/";
                    
                }
            }

        };

        filterChain.doFilter(requestWrapper, servletResponse);
    }

    @Override
    public void destroy() {
    }

}