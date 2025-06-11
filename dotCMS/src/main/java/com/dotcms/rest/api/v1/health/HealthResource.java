package com.dotcms.rest.api.v1.health;

import com.dotcms.health.api.HealthService;
import com.dotcms.health.model.HealthCheckResult;
import com.dotcms.health.model.HealthResponse;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.rest.exception.SecurityException;
import com.dotcms.rest.exception.mapper.ExceptionMapperUtil;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * REST API for application health management and dashboard integration.
 * 
 * This resource provides endpoints for managing and monitoring health checks from within
 * the dotCMS application, typically for administrative dashboards and health management UI.
 * 
 * This is separate from infrastructure monitoring endpoints (/livez, /readyz, /health)
 * which are designed for read-only access by infrastructure tools like Kubernetes.
 * 
 * All endpoints require appropriate CMS permissions for security.
 * 
 * @author dotCMS
 */
@Path("/v1/health")
@RequestScoped
public class HealthResource {
    
    private final WebResource webResource;
    
    @Inject
    private HealthService healthService;
    
    public HealthResource() {
        this.webResource = new WebResource();
    }
    
    /**
     * Overall health status - includes all health checks
     * Administrative endpoint requiring CMS Admin role
     * 
     * @param request HTTP request
     * @param response HTTP response
     * @return ResponseEntityView with HealthResponse
     */
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public final ResponseEntityView<HealthResponse> getOverallHealth(
            @Context final HttpServletRequest request,
            @Context final HttpServletResponse response) {
        
        try {
            // Require admin permissions
            final InitDataObject initData = webResource.init(null, request, response, false, null);
            
            final User user = initData.getUser();
            if (!APILocator.getRoleAPI().doesUserHaveRole(user, APILocator.getRoleAPI().loadCMSAdminRole())) {
                return new ResponseEntityView(null);
            }
            
            HealthResponse health = healthService.getOverallHealth();
            return new ResponseEntityView(health);
            
        } catch (DotDataException e) {
            Logger.error(this, "Error retrieving overall health status", e);
            return new ResponseEntityView(null);
        } catch (SecurityException e) {
            Logger.error(this, "Security error retrieving overall health status", e);
            return new ResponseEntityView(null);
        }
    }
    
    /**
     * Application liveness health status for dashboard/management UI
     * Returns detailed JSON suitable for application consumption
     * 
     * @param request HTTP request
     * @param response HTTP response
     * @return ResponseEntityView with HealthResponse
     */
    @GET
    @Path("/liveness")
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public final ResponseEntityView<HealthResponse> getLivenessHealth(
            @Context final HttpServletRequest request,
            @Context final HttpServletResponse response) {
        
        try {
            HealthResponse health = healthService.getLivenessHealth();
            return new ResponseEntityView(health);
            
        } catch (Exception e) {
            Logger.error(this, "Error retrieving liveness health status", e);
            return new ResponseEntityView(null);
        }
    }
    
    /**
     * Application readiness health status for dashboard/management UI
     * Returns detailed JSON suitable for application consumption
     * 
     * @param request HTTP request
     * @param response HTTP response
     * @return ResponseEntityView with HealthResponse
     */
    @GET
    @Path("/readiness")
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public final ResponseEntityView<HealthResponse> getReadinessHealth(
            @Context final HttpServletRequest request,
            @Context final HttpServletResponse response) {
        
        try {
            HealthResponse health = healthService.getReadinessHealth();
            return new ResponseEntityView(health);
            
        } catch (Exception e) {
            Logger.error(this, "Error retrieving readiness health status", e);
            return new ResponseEntityView(null);
        }
    }
    
    /**
     * Get a specific health check result by name
     * 
     * @param checkName the name of the health check
     * @param request HTTP request
     * @param response HTTP response
     * @return ResponseEntityView with HealthCheckResult or 404 if not found
     */
    @GET
    @Path("/check/{checkName}")
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public final Response getHealthCheck(
            @PathParam("checkName") final String checkName,
            @Context final HttpServletRequest request,
            @Context final HttpServletResponse response) {
        
        try {
            // Require admin permissions
            final InitDataObject initData = webResource.init(null, request, response, false, null);
            
            final User user = initData.getUser();
            if (!APILocator.getRoleAPI().doesUserHaveRole(user, APILocator.getRoleAPI().loadCMSAdminRole())) {
                return ExceptionMapperUtil.createResponse("", "Forbidden", Response.Status.FORBIDDEN);
            }
            
            Optional<HealthCheckResult> result = healthService.getHealthCheck(checkName);
            if (result.isPresent()) {
                return Response.ok(new ResponseEntityView(result.get())).build();
            } else {
                return ExceptionMapperUtil.createResponse("", "Health check not found: " + checkName, Response.Status.NOT_FOUND);
            }
            
        } catch (DotDataException e) {
            Logger.error(this, "Error retrieving health check: " + checkName, e);
            return ExceptionMapperUtil.createResponse("", "Internal server error", Response.Status.INTERNAL_SERVER_ERROR);
        } catch (SecurityException e) {
            Logger.error(this, "Security error retrieving health check: " + checkName, e);
            return ExceptionMapperUtil.createResponse("", "Forbidden", Response.Status.FORBIDDEN);
        }
    }
    
    /**
     * Get all health check names
     * 
     * @param request HTTP request
     * @param response HTTP response
     * @return ResponseEntityView with list of health check names
     */
    @GET
    @Path("/checks")
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public final ResponseEntityView<List<String>> getHealthCheckNames(
            @Context final HttpServletRequest request,
            @Context final HttpServletResponse response) {
        
        try {
            // Require admin permissions
            final InitDataObject initData = webResource.init(null, request, response, false, null);
            
            final User user = initData.getUser();
            if (!APILocator.getRoleAPI().doesUserHaveRole(user, APILocator.getRoleAPI().loadCMSAdminRole())) {
                return new ResponseEntityView(null);
            }
            
            List<String> checkNames = healthService.getHealthCheckNames();
            return new ResponseEntityView(checkNames);
            
        } catch (DotDataException e) {
            Logger.error(this, "Error retrieving health check names", e);
            return new ResponseEntityView(null);
        } catch (SecurityException e) {
            Logger.error(this, "Security error retrieving health check names", e);
            return new ResponseEntityView(null);
        }
    }
    
    /**
     * Get system status summary (alive/ready flags)
     * 
     * @param request HTTP request
     * @param response HTTP response
     * @return ResponseEntityView with status summary
     */
    @GET
    @Path("/status")
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public final ResponseEntityView<Map<String, Boolean>> getSystemStatus(
            @Context final HttpServletRequest request,
            @Context final HttpServletResponse response) {
        
        try {
            // Require admin permissions
            final InitDataObject initData = webResource.init(null, request, response, false, null);
            
            final User user = initData.getUser();
            if (!APILocator.getRoleAPI().doesUserHaveRole(user, APILocator.getRoleAPI().loadCMSAdminRole())) {
                return new ResponseEntityView(null);
            }
            
            Map<String, Boolean> status = Map.of(
                "alive", healthService.isAlive(),
                "ready", healthService.isReady()
            );
            return new ResponseEntityView(status);
            
        } catch (DotDataException e) {
            Logger.error(this, "Error retrieving system status", e);
            return new ResponseEntityView(null);
        } catch (SecurityException e) {
            Logger.error(this, "Security error retrieving system status", e);
            return new ResponseEntityView(null);
        }
    }
    
    /**
     * Force refresh all health checks
     * 
     * @param request HTTP request
     * @param response HTTP response
     * @return ResponseEntityView with success message
     */
    @POST
    @Path("/refresh")
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public final ResponseEntityView<Map<String, String>> refreshHealthChecks(
            @Context final HttpServletRequest request,
            @Context final HttpServletResponse response) {
        
        try {
            // Require admin permissions
            final InitDataObject initData = webResource.init(null, request, response, false, null);
            
            final User user = initData.getUser();
            if (!APILocator.getRoleAPI().doesUserHaveRole(user, APILocator.getRoleAPI().loadCMSAdminRole())) {
                return new ResponseEntityView(null);
            }
            
            healthService.refreshHealthChecks();
            Map<String, String> result = Map.of("message", "Health checks refresh triggered");
            return new ResponseEntityView(result);
            
        } catch (DotDataException e) {
            Logger.error(this, "Error refreshing health checks", e);
            return new ResponseEntityView(null);
        } catch (SecurityException e) {
            Logger.error(this, "Security error refreshing health checks", e);
            return new ResponseEntityView(null);
        }
    }
    
    /**
     * Force refresh a specific health check
     * 
     * @param checkName the name of the health check to refresh
     * @param request HTTP request
     * @param response HTTP response
     * @return ResponseEntityView with success/failure message
     */
    @POST
    @Path("/refresh/{checkName}")
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public final Response refreshHealthCheck(
            @PathParam("checkName") final String checkName,
            @Context final HttpServletRequest request,
            @Context final HttpServletResponse response) {
        
        try {
            // Require admin permissions
            final InitDataObject initData = webResource.init(null, request, response, false, null);
            
            final User user = initData.getUser();
            if (!APILocator.getRoleAPI().doesUserHaveRole(user, APILocator.getRoleAPI().loadCMSAdminRole())) {
                return ExceptionMapperUtil.createResponse("", "Forbidden", Response.Status.FORBIDDEN);
            }
            
            boolean success = healthService.refreshHealthCheck(checkName);
            if (success) {
                Map<String, String> result = Map.of("message", "Health check refreshed: " + checkName);
                return Response.ok(new ResponseEntityView(result)).build();
            } else {
                return ExceptionMapperUtil.createResponse("", "Health check not found: " + checkName, Response.Status.NOT_FOUND);
            }
            
        } catch (DotDataException e) {
            Logger.error(this, "Error refreshing health check: " + checkName, e);
            return ExceptionMapperUtil.createResponse("", "Internal server error", Response.Status.INTERNAL_SERVER_ERROR);
        } catch (SecurityException e) {
            Logger.error(this, "Security error refreshing health check: " + checkName, e);
            return ExceptionMapperUtil.createResponse("", "Forbidden", Response.Status.FORBIDDEN);
        }
    }
} 