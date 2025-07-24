package com.dotcms.rest.api.v1.system;

import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.ResponseEntityMapView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.rest.annotation.SwaggerCompliant;
import com.dotcms.shutdown.ShutdownCoordinator;
import com.dotmarketing.util.Logger;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.DefaultValue;
import java.util.HashMap;
import java.util.Map;

/**
 * REST endpoint for testing request draining functionality during shutdown.
 * 
 * This endpoint provides long-running requests that can be used to test
 * how the system handles active requests during shutdown.
 * 
 * Usage:
 * 1. Start multiple long-running requests: GET /api/v1/system/request-draining-test/long-request?duration=10000
 * 2. Send SIGTERM to the process
 * 3. Observe how shutdown waits for these requests to complete
 * 
 * WARNING: This is for testing purposes only and should not be used in production.
 */
@SwaggerCompliant(value = "System administration and configuration APIs", batch = 4)
@Path("/v1/system/request-draining-test")
@Tag(name = "Testing")
public class RequestDrainingTestResource {
    
    private final WebResource webResource = new WebResource();
    
    /**
     * Simulates a long-running request for testing request draining.
     * 
     * @param duration Duration in milliseconds (default: 5000ms)
     * @return Response with timing information
     */
    @Operation(
        summary = "Simulate long-running request for shutdown testing",
        description = "Creates a long-running request to test graceful shutdown behavior. The request will sleep for the specified duration while monitoring for shutdown signals. This is for testing purposes only and should not be used in production."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", 
                    description = "Request completed with timing information",
                    content = @Content(mediaType = "application/json",
                                      schema = @Schema(implementation = ResponseEntityMapView.class))),
        @ApiResponse(responseCode = "401", 
                    description = "Unauthorized - authentication required",
                    content = @Content(mediaType = "application/json"))
    })
    @GET
    @Path("/long-request")
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    public ResponseEntityView<Map<String, Object>> longRunningRequest(
            @Context HttpServletRequest request,
            @Context HttpServletResponse response,
            @Parameter(description = "Duration in milliseconds for the long-running request", required = false)
            @QueryParam("duration") @DefaultValue("5000") long duration) {
        
        // Initialize the web resource (authentication, etc.)
        webResource.init(request, response, true);
        
        long startTime = System.currentTimeMillis();
        boolean shutdownDetected = false;
        long shutdownDetectedAt = 0;
        
        Logger.info(this, String.format("Starting long-running request (duration: %dms, active requests: %d)", 
            duration, ShutdownCoordinator.getCurrentActiveRequestCount()));
        
        try {
            // Simulate processing time
            while (System.currentTimeMillis() - startTime < duration) {
                // Check if shutdown is in progress
                if (!shutdownDetected && ShutdownCoordinator.isRequestDraining()) {
                    shutdownDetected = true;
                    shutdownDetectedAt = System.currentTimeMillis();
                    Logger.info(this, String.format("Request detected shutdown signal after %dms", 
                        shutdownDetectedAt - startTime));
                }
                
                Thread.sleep(100); // Check every 100ms
            }
            
        } catch (InterruptedException e) {
            Logger.info(this, "Long-running request interrupted during shutdown");
            Thread.currentThread().interrupt();
        }
        
        long endTime = System.currentTimeMillis();
        long actualDuration = endTime - startTime;
        
        Map<String, Object> result = new HashMap<>();
        result.put("requestedDuration", duration);
        result.put("actualDuration", actualDuration);
        result.put("shutdownDetected", shutdownDetected);
        result.put("shutdownDetectedAfter", shutdownDetected ? (shutdownDetectedAt - startTime) : null);
        result.put("activeRequestsAtStart", ShutdownCoordinator.getCurrentActiveRequestCount());
        result.put("shutdownStatus", ShutdownCoordinator.getShutdownStatus());
        result.put("message", shutdownDetected ? 
            "Request completed after shutdown signal detected" : 
            "Request completed normally");
        
        Logger.info(this, String.format("Long-running request completed (duration: %dms, shutdown detected: %s)", 
            actualDuration, shutdownDetected));
        
        return new ResponseEntityView<>(result);
    }
    
    /**
     * Gets the current shutdown status for monitoring.
     * 
     * @return Current shutdown status information
     */
    @Operation(
        summary = "Get shutdown status",
        description = "Retrieves the current shutdown status including active request counts and shutdown progress information. Used for monitoring system shutdown behavior."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", 
                    description = "Shutdown status retrieved successfully",
                    content = @Content(mediaType = "application/json",
                                      schema = @Schema(implementation = ResponseEntityMapView.class))),
        @ApiResponse(responseCode = "401", 
                    description = "Unauthorized - authentication required",
                    content = @Content(mediaType = "application/json"))
    })
    @GET
    @Path("/status")
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    public ResponseEntityView<Map<String, Object>> getShutdownStatus(
            @Context HttpServletRequest request,
            @Context HttpServletResponse response) {
        
        webResource.init(request, response, true);
        
        ShutdownCoordinator.ShutdownStatus status = ShutdownCoordinator.getShutdownStatus();
        
        Map<String, Object> result = new HashMap<>();
        result.put("shutdownInProgress", status.isShutdownInProgress());
        result.put("requestDrainingInProgress", status.isRequestDrainingInProgress());
        result.put("shutdownCompleted", status.isShutdownCompleted());
        result.put("activeRequestCount", status.getActiveRequestCount());
        result.put("timestamp", System.currentTimeMillis());
        
        return new ResponseEntityView<>(result);
    }
    
    /**
     * Manually increments the active request counter for testing.
     * This simulates having active requests without actually creating them.
     * 
     * @param count Number of requests to simulate (default: 1)
     * @return Current active request count
     */
    @Operation(
        summary = "Simulate active requests for testing",
        description = "Manually increments the active request counter to simulate having active requests without actually creating them. This allows testing of shutdown behavior under different load conditions."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", 
                    description = "Active requests simulated successfully",
                    content = @Content(mediaType = "application/json",
                                      schema = @Schema(implementation = ResponseEntityMapView.class))),
        @ApiResponse(responseCode = "401", 
                    description = "Unauthorized - authentication required",
                    content = @Content(mediaType = "application/json"))
    })
    @GET
    @Path("/simulate-active-requests")
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    public ResponseEntityView<Map<String, Object>> simulateActiveRequests(
            @Context HttpServletRequest request,
            @Context HttpServletResponse response,
            @Parameter(description = "Number of active requests to simulate", required = false)
            @QueryParam("count") @DefaultValue("1") int count) {
        
        webResource.init(request, response, true);
        
        for (int i = 0; i < count; i++) {
            ShutdownCoordinator.incrementActiveRequests();
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("simulatedRequests", count);
        result.put("totalActiveRequests", ShutdownCoordinator.getCurrentActiveRequestCount());
        result.put("message", String.format("Simulated %d active requests", count));
        
        Logger.info(this, String.format("Simulated %d active requests (total: %d)", 
            count, ShutdownCoordinator.getCurrentActiveRequestCount()));
        
        return new ResponseEntityView<>(result);
    }
} 