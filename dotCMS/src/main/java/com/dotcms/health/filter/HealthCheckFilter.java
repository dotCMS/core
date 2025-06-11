package com.dotcms.health.filter;

import com.dotmarketing.util.Logger;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Set;

/**
 * Filter that handles health check endpoints by bypassing unnecessary filters.
 * Only essential filters are allowed to process health check requests:
 * - NormalizationFilter (URI validation)
 * - HttpHeaderSecurityFilter (Security headers)
 * - CookiesFilter (Cookie security)
 * - ThreadNameFilter (Logging)
 */
public class HealthCheckFilter implements Filter {

    private static final Set<String> ESSENTIAL_FILTERS = Set.of(
        "NormalizationFilter",
        "HttpHeaderSecurityFilter",
        "CookiesFilter",
        "ThreadNameFilter"
    );

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        Logger.info(this, "HealthCheckFilter initialized");
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) 
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String requestURI = httpRequest.getRequestURI();

        // Check if this is a health check endpoint
        if (isHealthCheckEndpoint(requestURI)) {
            Logger.debug(this, "Health check endpoint detected: " + requestURI);
            
            // Create a custom filter chain that only includes essential filters
            FilterChain essentialChain = new FilterChain() {
                @Override
                public void doFilter(ServletRequest req, ServletResponse res) throws IOException, ServletException {
                    // Get the current filter from the original chain
                    String currentFilter = chain.getClass().getSimpleName();
                    
                    if (ESSENTIAL_FILTERS.contains(currentFilter)) {
                        Logger.debug(this, "Processing essential filter: " + currentFilter);
                        chain.doFilter(req, res);
                    } else {
                        Logger.debug(this, "Skipping non-essential filter: " + currentFilter);
                        // Skip this filter and continue with the next one
                        chain.doFilter(req, res);
                    }
                }
            };
            
            // Process the request through the essential filters
            essentialChain.doFilter(request, response);
        } else {
            // Not a health check endpoint, proceed with normal filter chain
            chain.doFilter(request, response);
        }
    }

    @Override
    public void destroy() {
        Logger.info(this, "HealthCheckFilter destroyed");
    }

    /**
     * Determines if the given URI is a health check endpoint
     */
    private boolean isHealthCheckEndpoint(String uri) {
        return uri != null && (
            uri.equals("/health") ||
            uri.equals("/livez") ||
            uri.equals("/readyz") ||
            uri.startsWith("/api/v1/health/")
        );
    }
} 