package com.dotcms.rest.api.v1.database;

import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotmarketing.db.DatabaseConnectionHealthManager;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;
import io.vavr.control.Try;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * REST endpoint for database connection health monitoring and management.
 * 
 * Provides real-time information about:
 * - Circuit breaker state and history
 * - Connection pool metrics (if available)
 * - Database connectivity status
 * - Connection leak detection
 * 
 * Also allows administrative control of the circuit breaker for maintenance scenarios.
 * 
 * @author dotCMS
 */
@Path("/v1/database")
public class DatabaseHealthResource {
    
    private final WebResource webResource = new WebResource();
    
    /**
     * Get comprehensive database health status.
     * 
     * @param request  HTTP request
     * @param response HTTP response
     * @return Database health status including circuit breaker state and connection pool metrics
     */
    @GET
    @Path("/health")
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    public Response getHealthStatus(@Context HttpServletRequest request, 
                                  @Context HttpServletResponse response) {
        
        try {
            // Initialize request - no authentication required for health checks in this implementation
            // In production, you may want to require admin authentication
            webResource.init(request, response, true);
            
            DatabaseConnectionHealthManager.HealthStatus health = 
                    DbConnectionFactory.getConnectionHealthStatus();
            
            Map<String, Object> healthData = new HashMap<>();
            healthData.put("healthy", health.isHealthy());
            healthData.put("circuitState", health.getCircuitState().toString());
            healthData.put("consecutiveFailures", health.getConsecutiveFailures());
            healthData.put("lastSuccessTime", health.getLastSuccessTime());
            healthData.put("lastFailureTime", health.getLastFailureTime());
            healthData.put("connectionLeakCount", health.getConnectionLeakCount());
            healthData.put("operationAllowed", DbConnectionFactory.isDatabaseOperationAllowed());
            healthData.put("timestamp", Instant.now());
            
            // Add connection pool metrics if available
            if (health.getActiveConnections() != null) {
                Map<String, Object> poolMetrics = new HashMap<>();
                poolMetrics.put("activeConnections", health.getActiveConnections());
                poolMetrics.put("idleConnections", health.getIdleConnections());
                poolMetrics.put("totalConnections", health.getTotalConnections());
                poolMetrics.put("threadsAwaitingConnection", health.getThreadsAwaitingConnection());
                
                // Calculate utilization percentage
                if (health.getTotalConnections() != null && health.getTotalConnections() > 0) {
                    double utilization = (double) health.getActiveConnections() / health.getTotalConnections() * 100;
                    poolMetrics.put("utilizationPercentage", Math.round(utilization * 100.0) / 100.0);
                }
                
                healthData.put("connectionPool", poolMetrics);
            }
            
            // Add database connectivity test
            boolean dbConnectivity = Try.of(() -> {
                return DbConnectionFactory.isDatabaseOperationAllowed() && 
                       DbConnectionFactory.dbAvailable();
            }).getOrElse(false);
            
            healthData.put("databaseConnectivity", dbConnectivity);
            
            return Response.ok(new ResponseEntityView<>(healthData)).build();
            
        } catch (Exception e) {
            Logger.error(this, "Failed to retrieve database health status", e);
            
            Map<String, Object> errorData = new HashMap<>();
            errorData.put("healthy", false);
            errorData.put("error", "Failed to retrieve health status: " + e.getMessage());
            errorData.put("timestamp", Instant.now());
            
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ResponseEntityView<>(errorData)).build();
        }
    }
    
    /**
     * Get simple health check for load balancer / monitoring systems.
     * Returns 200 OK if database is healthy, 503 Service Unavailable if not.
     * 
     * @param request  HTTP request
     * @param response HTTP response
     * @return Simple health status
     */
    @GET
    @Path("/health/simple")
    @Produces(MediaType.TEXT_PLAIN)
    @NoCache
    public Response getSimpleHealthStatus(@Context HttpServletRequest request, 
                                        @Context HttpServletResponse response) {
        
        try {
            boolean isHealthy = DbConnectionFactory.isDatabaseOperationAllowed() && 
                               DbConnectionFactory.getConnectionHealthStatus().isHealthy();
            
            if (isHealthy) {
                return Response.ok("healthy").build();
            } else {
                return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                        .entity("unhealthy").build();
            }
            
        } catch (Exception e) {
            Logger.error(this, "Failed to check simple health status", e);
            return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                    .entity("error").build();
        }
    }
    
    /**
     * Manually open the circuit breaker for maintenance or emergency situations.
     * Requires admin authentication.
     * 
     * @param request  HTTP request
     * @param response HTTP response
     * @param reason   Reason for opening the circuit breaker
     * @return Operation result
     */
    @POST
    @Path("/circuit-breaker/open")
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    public Response openCircuitBreaker(@Context HttpServletRequest request,
                                     @Context HttpServletResponse response,
                                     @FormParam("reason") String reason) {
        
        try {
            // Require admin authentication for circuit breaker control
            webResource.init(request, response, true);
            User user = webResource.getUser();
            
            if (!user.isAdmin()) {
                return Response.status(Response.Status.FORBIDDEN)
                        .entity(new ResponseEntityView<>("Admin access required")).build();
            }
            
            String effectiveReason = (reason != null && !reason.trim().isEmpty()) ? 
                    reason : "Manually opened via REST API by " + user.getUserId();
            
            DbConnectionFactory.openDatabaseCircuitBreaker(effectiveReason);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("action", "circuit_breaker_opened");
            result.put("reason", effectiveReason);
            result.put("timestamp", Instant.now());
            result.put("user", user.getUserId());
            
            Logger.info(this, "Database circuit breaker manually opened by " + user.getUserId() + 
                    ": " + effectiveReason);
            
            return Response.ok(new ResponseEntityView<>(result)).build();
            
        } catch (Exception e) {
            Logger.error(this, "Failed to open circuit breaker", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ResponseEntityView<>("Failed to open circuit breaker: " + e.getMessage())).build();
        }
    }
    
    /**
     * Manually close the circuit breaker after maintenance or for forced recovery.
     * Requires admin authentication.
     * 
     * @param request  HTTP request
     * @param response HTTP response
     * @param reason   Reason for closing the circuit breaker
     * @return Operation result
     */
    @POST
    @Path("/circuit-breaker/close")
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    public Response closeCircuitBreaker(@Context HttpServletRequest request,
                                      @Context HttpServletResponse response,
                                      @FormParam("reason") String reason) {
        
        try {
            // Require admin authentication for circuit breaker control
            webResource.init(request, response, true);
            User user = webResource.getUser();
            
            if (!user.isAdmin()) {
                return Response.status(Response.Status.FORBIDDEN)
                        .entity(new ResponseEntityView<>("Admin access required")).build();
            }
            
            String effectiveReason = (reason != null && !reason.trim().isEmpty()) ? 
                    reason : "Manually closed via REST API by " + user.getUserId();
            
            DbConnectionFactory.closeDatabaseCircuitBreaker(effectiveReason);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("action", "circuit_breaker_closed");
            result.put("reason", effectiveReason);
            result.put("timestamp", Instant.now());
            result.put("user", user.getUserId());
            
            Logger.info(this, "Database circuit breaker manually closed by " + user.getUserId() + 
                    ": " + effectiveReason);
            
            return Response.ok(new ResponseEntityView<>(result)).build();
            
        } catch (Exception e) {
            Logger.error(this, "Failed to close circuit breaker", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ResponseEntityView<>("Failed to close circuit breaker: " + e.getMessage())).build();
        }
    }
}