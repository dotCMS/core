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
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

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
 * This is separate from infrastructure monitoring endpoints (/livez, /readyz)
 * which are designed for read-only access by infrastructure tools like Kubernetes.
 * 
 * All endpoints require appropriate CMS permissions for security.
 * 
 * @author dotCMS
 */
@Tag(name = "Health", description = "Health management and monitoring endpoints for administrative dashboards")
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
     * Checks if authentication is required for detailed health information.
     * Respects the health.detailed.authentication.required configuration.
     * 
     * @param request HTTP request
     * @param response HTTP response
     * @return true if access is allowed, false if authentication failed
     * @throws DotDataException if there's an error checking authentication
     */
    private boolean isAccessAllowed(HttpServletRequest request, HttpServletResponse response) throws DotDataException {
        // Check if authentication is disabled via configuration
        boolean authRequired = Config.getBooleanProperty("health.detailed.authentication.required", true);
        if (!authRequired) {
            Logger.debug(this, "Authentication disabled for detailed health endpoints via configuration");
            return true;
        }
        
        // Authentication is required - check for admin permissions
        final InitDataObject initData = webResource.init(null, request, response, false, null);
        final User user = initData.getUser();
        
        return APILocator.getRoleAPI().doesUserHaveRole(user, APILocator.getRoleAPI().loadCMSAdminRole());
    }
    
    /**
     * Overall health status - includes all health checks
     * Authentication requirements controlled by health.detailed.authentication.required configuration
     * 
     * @param request HTTP request
     * @param response HTTP response
     * @return ResponseEntityView with HealthResponse
     */
    @Operation(
            operationId = "getOverallHealth",
            summary = "Get overall health status",
            description = "Returns comprehensive health status including all registered health checks. " +
                    "Authentication requirements are controlled by the health.detailed.authentication.required configuration property.",
            tags = {"Health"},
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Successfully retrieved overall health status",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ResponseEntityView.class)
                            )
                    ),
                    @ApiResponse(responseCode = "403", description = "Forbidden - Authentication required"),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error")
            }
    )
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public Response getOverallHealth(
            @Context final HttpServletRequest request,
            @Context final HttpServletResponse response) {
        
        try {
            // Check authentication based on configuration
            if (!isAccessAllowed(request, response)) {
                return ExceptionMapperUtil.createResponse("", "Forbidden", Response.Status.FORBIDDEN);
            }
            
            HealthResponse health = healthService.getOverallHealth();
            return Response.ok(new ResponseEntityView<>(health)).build();
            
        } catch (DotDataException e) {
            Logger.error(this, "Error retrieving overall health status", e);
            return ExceptionMapperUtil.createResponse(e, Response.Status.INTERNAL_SERVER_ERROR);
        } catch (SecurityException e) {
            Logger.error(this, "Security error retrieving overall health status", e);
            return ExceptionMapperUtil.createResponse("", "Forbidden", Response.Status.FORBIDDEN);
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
    @Operation(
            operationId = "getLivenessHealth",
            summary = "Get liveness health status",
            description = "Returns liveness health checks suitable for application dashboards. " +
                    "This endpoint provides detailed JSON information about critical system components " +
                    "required for the application to be considered alive.",
            tags = {"Health"},
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Successfully retrieved liveness health status",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ResponseEntityView.class)
                            )
                    ),
                    @ApiResponse(responseCode = "403", description = "Forbidden - Authentication required"),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error")
            }
    )
    @GET
    @Path("/liveness")
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public Response getLivenessHealth(
            @Context final HttpServletRequest request,
            @Context final HttpServletResponse response) {
        
        try {
            // Check authentication based on configuration
            if (!isAccessAllowed(request, response)) {
                return ExceptionMapperUtil.createResponse("", "Forbidden", Response.Status.FORBIDDEN);
            }
            
            HealthResponse health = healthService.getLivenessHealth();
            return Response.ok(new ResponseEntityView<>(health)).build();
            
        } catch (Exception e) {
            Logger.error(this, "Error retrieving liveness health status", e);
            return ExceptionMapperUtil.createResponse(e, Response.Status.INTERNAL_SERVER_ERROR);
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
    @Operation(
            operationId = "getReadinessHealth",
            summary = "Get readiness health status",
            description = "Returns readiness health checks to determine if the application is ready to receive traffic. " +
                    "This endpoint provides detailed JSON information about system components " +
                    "required for the application to be considered ready to serve requests.",
            tags = {"Health"},
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Successfully retrieved readiness health status",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ResponseEntityView.class)
                            )
                    ),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error")
            }
    )
    @GET
    @Path("/readiness")
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public Response getReadinessHealth(
            @Context final HttpServletRequest request,
            @Context final HttpServletResponse response) {
        
        try {
            // Check authentication based on configuration
            if (!isAccessAllowed(request, response)) {
                return ExceptionMapperUtil.createResponse("", "Forbidden", Response.Status.FORBIDDEN);
            }

            HealthResponse health = healthService.getReadinessHealth();
            return Response.ok(new ResponseEntityView<>(health)).build();
            
        } catch (Exception e) {
            Logger.error(this, "Error retrieving readiness health status", e);
            return ExceptionMapperUtil.createResponse(e, Response.Status.INTERNAL_SERVER_ERROR);
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
    @Operation(
            operationId = "getHealthCheck",
            summary = "Get specific health check result",
            description = "Returns the result of a specific health check identified by name. " +
                    "Useful for monitoring individual components or debugging specific health issues.",
            tags = {"Health"},
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Successfully retrieved health check result",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ResponseEntityView.class)
                            )
                    ),
                    @ApiResponse(responseCode = "403", description = "Forbidden - Authentication required"),
                    @ApiResponse(responseCode = "404", description = "Health check not found"),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error")
            }
    )
    @GET
    @Path("/check/{checkName}")
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public Response getHealthCheck(
            @PathParam("checkName") @Parameter(
                    required = true,
                    description = "Name of the health check to retrieve",
                    schema = @Schema(type = "string")
            ) final String checkName,
            @Context final HttpServletRequest request,
            @Context final HttpServletResponse response) {
        
        try {
            // Check authentication based on configuration
            if (!isAccessAllowed(request, response)) {
                return ExceptionMapperUtil.createResponse("", "Forbidden", Response.Status.FORBIDDEN);
            }
            
            Optional<HealthCheckResult> result = healthService.getHealthCheck(checkName);
            if (result.isPresent()) {
                return Response.ok(new ResponseEntityView<>(result.get())).build();
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
     * @return Response with list of health check names
     */
    @Operation(
            operationId = "getHealthCheckNames",
            summary = "Get all health check names",
            description = "Returns a list of all registered health check names. " +
                    "Useful for discovering available health checks and building monitoring interfaces.",
            tags = {"Health"},
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Successfully retrieved health check names",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ResponseEntityView.class)
                            )
                    ),
                    @ApiResponse(responseCode = "403", description = "Forbidden - Authentication required"),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error")
            }
    )
    @GET
    @Path("/checks")
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public Response getHealthCheckNames(
            @Context final HttpServletRequest request,
            @Context final HttpServletResponse response) {
        
        try {
            // Check authentication based on configuration
            if (!isAccessAllowed(request, response)) {
                return ExceptionMapperUtil.createResponse("", "Forbidden", Response.Status.FORBIDDEN);
            }
            
            List<String> checkNames = healthService.getHealthCheckNames();
            return Response.ok(new ResponseEntityView<>(checkNames)).build();
            
        } catch (DotDataException e) {
            Logger.error(this, "Error retrieving health check names", e);
            return ExceptionMapperUtil.createResponse(e, Response.Status.INTERNAL_SERVER_ERROR);
        } catch (SecurityException e) {
            Logger.error(this, "Security error retrieving health check names", e);
            return ExceptionMapperUtil.createResponse("", "Forbidden", Response.Status.FORBIDDEN);
        }
    }
    
    /**
     * Get system status summary (alive/ready flags)
     * 
     * @param request HTTP request
     * @param response HTTP response
     * @return Response with status summary
     */
    @Operation(
            operationId = "getSystemStatus",
            summary = "Get system status summary",
            description = "Returns a simple boolean summary of system health status with alive and ready flags. " +
                    "Provides a quick overview of system health without detailed check information.",
            tags = {"Health"},
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Successfully retrieved system status summary",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ResponseEntityView.class)
                            )
                    ),
                    @ApiResponse(responseCode = "403", description = "Forbidden - Authentication required"),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error")
            }
    )
    @GET
    @Path("/status")
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public Response getSystemStatus(
            @Context final HttpServletRequest request,
            @Context final HttpServletResponse response) {
        
        try {
            // Check authentication based on configuration
            if (!isAccessAllowed(request, response)) {
                return ExceptionMapperUtil.createResponse("", "Forbidden", Response.Status.FORBIDDEN);
            }
            
            Map<String, Boolean> status = Map.of(
                "alive", healthService.isAlive(),
                "ready", healthService.isReady()
            );
            return Response.ok(new ResponseEntityView<>(status)).build();
            
        } catch (DotDataException e) {
            Logger.error(this, "Error retrieving system status", e);
            return ExceptionMapperUtil.createResponse(e, Response.Status.INTERNAL_SERVER_ERROR);
        } catch (SecurityException e) {
            Logger.error(this, "Security error retrieving system status", e);
            return ExceptionMapperUtil.createResponse("", "Forbidden", Response.Status.FORBIDDEN);
        }
    }
    
    /**
     * Force refresh all health checks
     * 
     * @param request HTTP request
     * @param response HTTP response
     * @return Response with success message
     */
    @Operation(
            operationId = "refreshHealthChecks",
            summary = "Force refresh all health checks",
            description = "Triggers an immediate refresh of all registered health checks, " +
                    "bypassing any caching mechanisms. Useful for getting up-to-date health status " +
                    "after configuration changes or system maintenance.",
            tags = {"Health"},
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Successfully triggered health checks refresh",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ResponseEntityView.class)
                            )
                    ),
                    @ApiResponse(responseCode = "403", description = "Forbidden - Authentication required"),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error")
            }
    )
    @POST
    @Path("/refresh")
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public Response refreshHealthChecks(
            @Context final HttpServletRequest request,
            @Context final HttpServletResponse response) {
        
        try {
            // Check authentication based on configuration
            if (!isAccessAllowed(request, response)) {
                return ExceptionMapperUtil.createResponse("", "Forbidden", Response.Status.FORBIDDEN);
            }
            
            healthService.refreshHealthChecks();
            Map<String, String> result = Map.of("message", "Health checks refresh triggered");
            return Response.ok(new ResponseEntityView<>(result)).build();
            
        } catch (DotDataException e) {
            Logger.error(this, "Error refreshing health checks", e);
            return ExceptionMapperUtil.createResponse(e, Response.Status.INTERNAL_SERVER_ERROR);
        } catch (SecurityException e) {
            Logger.error(this, "Security error refreshing health checks", e);
            return ExceptionMapperUtil.createResponse("", "Forbidden", Response.Status.FORBIDDEN);
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
    @Operation(
            operationId = "refreshHealthCheck",
            summary = "Force refresh a specific health check",
            description = "Triggers an immediate refresh of a specific health check identified by name, " +
                    "bypassing any caching mechanisms. Useful for testing individual components " +
                    "or getting up-to-date status after targeted maintenance.",
            tags = {"Health"},
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Successfully refreshed the health check",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ResponseEntityView.class)
                            )
                    ),
                    @ApiResponse(responseCode = "403", description = "Forbidden - Authentication required"),
                    @ApiResponse(responseCode = "404", description = "Health check not found"),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error")
            }
    )
    @POST
    @Path("/refresh/{checkName}")
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public Response refreshHealthCheck(
            @PathParam("checkName") @Parameter(
                    required = true,
                    description = "Name of the health check to refresh",
                    schema = @Schema(type = "string")
            ) final String checkName,
            @Context final HttpServletRequest request,
            @Context final HttpServletResponse response) {
        
        try {
            // Check authentication based on configuration
            if (!isAccessAllowed(request, response)) {
                return ExceptionMapperUtil.createResponse("", "Forbidden", Response.Status.FORBIDDEN);
            }
            
            boolean success = healthService.refreshHealthCheck(checkName);
            if (success) {
                Map<String, String> result = Map.of("message", "Health check refreshed: " + checkName);
                return Response.ok(new ResponseEntityView<>(result)).build();
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