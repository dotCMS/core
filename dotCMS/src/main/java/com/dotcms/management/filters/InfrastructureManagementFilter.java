package com.dotcms.management.filters;

import com.dotcms.management.config.InfrastructureConstants;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Generic infrastructure management filter for any management endpoints.
 * 
 * This filter is SERVICE-AGNOSTIC and only handles:
 * 1. Port validation for requests under the management prefix
 * 2. Access control based on management port vs application port
 * 3. Forwarding valid requests to servlet mappings
 * 
 * DECOUPLING PRINCIPLE:
 * - This filter knows ONLY about the management path prefix (/dotmgt)
 * - This filter does NOT know about specific services (health, metrics, etc.)
 * - Specific services define their own endpoints under the shared prefix
 * - Servlet mappings handle routing to appropriate service servlets
 * 
 * This enables multiple management services without filter changes.
 */
public class InfrastructureManagementFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        Logger.info(this, "InfrastructureManagementFilter initialized");
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        String requestURI = httpRequest.getRequestURI();

        // Only process management requests - let everything else pass through
        if (requestURI != null && requestURI.contains(InfrastructureConstants.MANAGEMENT_PATH_PREFIX)) {
            Logger.debug(this, "Management endpoint detected: " + requestURI);
            
            if (isManagementAccessAuthorized(httpRequest)) {
                Logger.debug(this, "Port validation passed - continuing to servlet mapping");
                // Continue to servlet mapping - the servlet mappings will handle routing
                chain.doFilter(request, response);
            } else {
                Logger.debug(this, "Port validation failed - blocking access");
                sendManagementAccessDenied(httpResponse);
            }
            return; // Management endpoints handled (either forwarded or blocked)
        }

        // Continue with normal filter chain for all non-management requests
        chain.doFilter(request, response);
    }

    /**
     * Simple port validation for management endpoints.
     * Uses the same CMS_MANAGEMENT_PORT environment variable that server.xml uses.
     */
    private boolean isManagementAccessAuthorized(HttpServletRequest request) {
        int serverPort = request.getServerPort();
        int managementPort = getManagementPortFromEnvironment();
        
        // Check if on management port
        if (serverPort == managementPort) {
            return true;
        }
        
        // Check proxy headers for Docker/proxy scenarios
        String forwardedPort = request.getHeader(InfrastructureConstants.Headers.X_FORWARDED_PORT);
        if (forwardedPort != null) {
            try {
                int proxyPort = Integer.parseInt(forwardedPort);
                if (proxyPort == managementPort) {
                    return true;
                }
            } catch (NumberFormatException e) {
                // Invalid port header
            }
        }
        
        // Check alternative proxy headers
        String originalPort = request.getHeader(InfrastructureConstants.Headers.X_ORIGINAL_PORT);
        if (originalPort != null) {
            try {
                int proxyPort = Integer.parseInt(originalPort);
                if (proxyPort == managementPort) {
                    return true;
                }
            } catch (NumberFormatException e) {
                // Invalid port header
            }
        }
        
        // Check if management port enforcement is disabled (for development/testing)
        boolean strictChecking = Config.getBooleanProperty(InfrastructureConstants.Ports.STRICT_CHECK_PROPERTY, true);
        if (!strictChecking) {
            Logger.debug(this, "Management port strict checking disabled - allowing access");
            return true;
        }
        
        return false;
    }

    /**
     * Gets the management port from the same environment variable used by server.xml.
     * This ensures we use the exact same port configuration without duplication.
     * 
     * server.xml uses: port="${CMS_MANAGEMENT_PORT:-8090}"
     * We read the same variable here to stay in sync.
     */
    private int getManagementPortFromEnvironment() {
        // Read the exact same environment variable that server.xml uses
        String portEnv = System.getenv(InfrastructureConstants.Ports.MANAGEMENT_PORT_PROPERTY);
        if (portEnv != null && !portEnv.trim().isEmpty()) {
            try {
                return Integer.parseInt(portEnv.trim());
            } catch (NumberFormatException e) {
                Logger.warn(this, "Invalid CMS_MANAGEMENT_PORT environment variable: " + portEnv + 
                          ", using default: " + InfrastructureConstants.Ports.DEFAULT_MANAGEMENT_PORT);
            }
        }
        
        // Fallback: Check if it's available through Config (in case Config is enhanced later)
        int configPort = Config.getIntProperty(InfrastructureConstants.Ports.MANAGEMENT_PORT_PROPERTY, -1);
        if (configPort != -1) {
            return configPort;
        }
        
        // Final fallback: Use default (same as server.xml default)
        return InfrastructureConstants.Ports.DEFAULT_MANAGEMENT_PORT;
    }

    /**
     * Sends a standardized access denied response for management endpoints.
     */
    private void sendManagementAccessDenied(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        response.setContentType(InfrastructureConstants.Responses.CONTENT_TYPE_TEXT);
        response.setCharacterEncoding(InfrastructureConstants.Responses.CHARSET_UTF8);
        response.getWriter().write(InfrastructureConstants.Responses.ACCESS_DENIED_MESSAGE);
    }

    @Override
    public void destroy() {
        Logger.info(this, "InfrastructureManagementFilter destroyed");
        // CDI handles cleanup automatically
    }
} 