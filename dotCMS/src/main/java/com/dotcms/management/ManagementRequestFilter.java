package com.dotcms.management;

import java.io.IOException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.dotcms.management.ManagementAPI;
import com.dotcms.management.ManagementAPIImpl;

/**
 * This filter will only allow backend access to requests coming on a specific host
 *
 */
public class ManagementRequestFilter implements Filter {


    private static ManagementAPI managementApi = new ManagementAPIImpl();
    
    
    
    
    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
                    throws IOException, ServletException {

        final HttpServletRequest request = ((HttpServletRequest) servletRequest);
        final HttpServletResponse response = ((HttpServletResponse) servletResponse);
        
        
        if (managementApi.managementHostRequired(request) && !managementApi.isManagementHost(request)) {
            response.sendError(404);
            return;
        }
        
        if (managementApi.isManagementHost(request) ) {
            response.addHeader("X-Robots-Tag",  "noindex, nofollow");
        }


        filterChain.doFilter(servletRequest, servletResponse);
    }










    @Override
    public void init(FilterConfig filterConfig) throws ServletException { /* Nothing to do here */ }

    @Override
    public void destroy() { /* Nothing to do here */ }



}
