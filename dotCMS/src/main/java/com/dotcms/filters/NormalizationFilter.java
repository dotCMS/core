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

/**
 * Filter created to wrap all the incoming requests to override the {@link
 * HttpServletRequest#getRequestURI()} method in order to normalize the requested URIs.
 */
public class NormalizationFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse,
            FilterChain filterChain) throws IOException, ServletException {

        HttpServletRequestWrapper requestWrapper = new HttpServletRequestWrapper(
                (HttpServletRequest) servletRequest) {

            @Override
            public String getRequestURI() {

                /* Normalization is the process of removing unnecessary "." and ".." segments from the path component of a hierarchical URI.
                 1. Each "." segment is simply removed.
                 2. A ".." segment is removed only if it is preceded by a non-".." segment.
                 3. Normalization has no effect upon opaque URIs. (mailto:a@b.com)
                 */
                String newNormal = URI.create(super.getRequestURI()).normalize().toString();
                
                while(newNormal.indexOf("//")>-1) {
                    newNormal = newNormal.replace("//", "/");
                }
                return newNormal;
            }

        };

        filterChain.doFilter(requestWrapper, servletResponse);
    }

    @Override
    public void destroy() {
    }

}