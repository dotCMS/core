package com.dotmarketing.filters;

import com.dotcms.repackage.org.apache.commons.httpclient.HttpStatus;
import com.dotmarketing.exception.DotSecurityException;
import java.io.IOException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

/**
 * @author Andre Curione
 *
 * Filter class to catch exceptions and standardize responses throughout REST API Resources
 */
public class RESTAPIExceptionFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {

    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse,
            FilterChain filterChain) throws IOException, ServletException {
        try {
            //Execute chain
            filterChain.doFilter(servletRequest, servletResponse);

        } catch (Exception e) {
            //On Exception, get the root cause
            Throwable root = getRootCause(e);

            HttpServletResponse httpResponse = (HttpServletResponse) servletResponse;

            //Standardize Unauthorized status code = 401
            if (root instanceof DotSecurityException) {
                httpResponse.setStatus(HttpStatus.SC_UNAUTHORIZED);
            }
        }
    }

    @Override
    public void destroy() {

    }

    private Throwable getRootCause (Throwable e) {
        if (e != null) {
            if (e.getCause() != null && e.getCause() != e) {
                return getRootCause(e.getCause());
            } else {
                return e;
            }
        }
        return null;
    }
}
