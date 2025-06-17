package com.dotcms.filters;

import com.dotcms.shutdown.ShutdownCoordinator;
import com.dotmarketing.util.Logger;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Filter that tracks active HTTP requests to support graceful shutdown.
 * 
 * This filter increments a counter when a request starts and decrements it
 * when the request completes (either successfully or with an error).
 * The ShutdownCoordinator uses this information to wait for active requests
 * to complete before shutting down dotCMS components.
 * 
 * The filter is designed to be lightweight and not interfere with normal
 * request processing. It only tracks the count - no request details are stored.
 * 
 * @author dotCMS Team
 */
public class RequestTrackingFilter implements Filter {
    
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        Logger.info(this, "RequestTrackingFilter initialized - active request tracking enabled for graceful shutdown");
    }
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) 
            throws IOException, ServletException {
        
        // Only track HTTP requests
        if (!(request instanceof HttpServletRequest)) {
            chain.doFilter(request, response);
            return;
        }
        
        // Increment active request count (with graceful degradation)
        boolean requestCounted = false;
        try {
            ShutdownCoordinator.incrementActiveRequests();
            requestCounted = true;
        } catch (Exception e) {
            Logger.debug(this, "Failed to increment request count, continuing without tracking: " + e.getMessage());
        }
        
        try {
            // Continue with the request processing
            chain.doFilter(request, response);
        } finally {
            // Always decrement the count if we successfully incremented it
            if (requestCounted) {
                try {
                    ShutdownCoordinator.decrementActiveRequests();
                } catch (Exception e) {
                    Logger.debug(this, "Failed to decrement request count: " + e.getMessage());
                }
            }
        }
    }
    
    @Override
    public void destroy() {
        Logger.info(this, "RequestTrackingFilter destroyed");
    }
} 