package com.dotcms.management.filter;

import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import java.io.IOException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Filter to restrict management endpoints to the dedicated management port only.
 * This ensures that infrastructure monitoring endpoints (/dotmgt/*)
 * are only accessible on the management port for security and isolation.
 * 
 * When management port is disabled, this filter passes through all requests.
 * When management port is enabled, it only allows access on the configured port.
 */
public class ManagementPortFilter implements Filter {
    
    private boolean managementPortEnabled;
    private int managementPort;
    
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // Load configuration
        managementPortEnabled = Config.getBooleanProperty("management.port.enabled", true);
        managementPort = Config.getIntProperty("management.port.number", 8090);
        
        Logger.info(this, "ManagementPortFilter initialized - enabled: " + managementPortEnabled + ", port: " + managementPort);
    }
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) 
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        
        // If management port is disabled, allow all requests to pass through
        if (!managementPortEnabled) {
            chain.doFilter(request, response);
            return;
        }
        
        // Check if request is coming from the management port
        int requestPort = httpRequest.getServerPort();
        String requestURI = httpRequest.getRequestURI();
        
        Logger.debug(this, "ManagementPortFilter: Request to " + requestURI + " on port " + requestPort);
        
        // If request is on management port, allow it
        if (requestPort == managementPort) {
            Logger.debug(this, "ManagementPortFilter: Allowing request on management port " + requestPort);
            chain.doFilter(request, response);
            return;
        }
        
        // Request is not on management port - block it
        Logger.debug(this, "ManagementPortFilter: Blocking request to " + requestURI + " on port " + requestPort + " (management port is " + managementPort + ")");
        
        httpResponse.setStatus(HttpServletResponse.SC_NOT_FOUND);
        httpResponse.setContentType("text/plain");
        httpResponse.setCharacterEncoding("UTF-8");
        httpResponse.getWriter().write("Management endpoints are only available on the management port");
    }
    
    @Override
    public void destroy() {
        Logger.info(this, "ManagementPortFilter destroyed");
    }
}