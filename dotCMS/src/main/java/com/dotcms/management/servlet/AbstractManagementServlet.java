package com.dotcms.management.servlet;

import com.dotcms.management.config.InfrastructureConstants;
import com.dotmarketing.util.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Abstract base class for all management servlets.
 * 
 * Ensures management servlets can only be accessed through the management path prefix.
 * Extend this class and implement doManagementGet() instead of doGet().
 */
public abstract class AbstractManagementServlet extends HttpServlet {

    @Override
    protected final void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (!validateManagementPath(request, response)) {
            return; // Exit early if validation failed
        }
        doManagementGet(request, response);
    }

    @Override
    protected final void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (!validateManagementPath(request, response)) {
            return; // Exit early if validation failed
        }
        doManagementPost(request, response);
    }

    @Override
    protected final void doPut(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (!validateManagementPath(request, response)) {
            return; // Exit early if validation failed
        }
        doManagementPut(request, response);
    }

    @Override
    protected final void doDelete(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (!validateManagementPath(request, response)) {
            return; // Exit early if validation failed
        }
        doManagementDelete(request, response);
    }

    /**
     * Validates that the request is coming through the management path.
     * @return true if request is valid and should proceed, false if request was rejected
     */
    private boolean validateManagementPath(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        String requestURI = request.getRequestURI();
        String servletPath = request.getServletPath();
        
        // Check if request is on management path
        boolean isManagementPath = (requestURI != null && requestURI.startsWith(InfrastructureConstants.MANAGEMENT_PATH_PREFIX)) ||
                                  (servletPath != null && servletPath.startsWith(InfrastructureConstants.MANAGEMENT_PATH_PREFIX));
        
        if (!isManagementPath) {
            Logger.error(this, "Management servlet accessed outside management path: " + requestURI);
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            response.setContentType("text/plain");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write("Not Found");
            return false;
        }
        return true;
    }

    /**
     * Override this method instead of doGet().
     * Guaranteed to only be called for requests on the management path.
     */
    protected void doManagementGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // Default: Method not allowed
        response.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
    }

    /**
     * Override this method instead of doPost().
     * Guaranteed to only be called for requests on the management path.
     */
    protected void doManagementPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // Default: Method not allowed
        response.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
    }

    /**
     * Override this method instead of doPut().
     * Guaranteed to only be called for requests on the management path.
     */
    protected void doManagementPut(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // Default: Method not allowed
        response.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
    }

    /**
     * Override this method instead of doDelete().
     * Guaranteed to only be called for requests on the management path.
     */
    protected void doManagementDelete(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // Default: Method not allowed
        response.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
    }
} 