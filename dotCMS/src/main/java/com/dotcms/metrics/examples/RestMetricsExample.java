package com.dotcms.metrics.examples;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Timer;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Example REST resource showing how to add metrics to API endpoints.
 * 
 * This demonstrates various metric patterns:
 * - Request counters with tags
 * - Response time measurement
 * - Error rate tracking
 * - Custom business metrics
 */
@Path("/v1/example")
public class RestMetricsExample {
    
    // Define metrics as class fields for better performance
    private final Counter requestCounter = Metrics.counter("dotcms.api.requests.total", 
        "endpoint", "example");
    
    private final Counter errorCounter = Metrics.counter("dotcms.api.errors.total",
        "endpoint", "example");
    
    private final Timer requestTimer = Metrics.timer("dotcms.api.request.duration",
        "endpoint", "example");
    
    @GET
    @Path("/users/{userId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getUser(@PathParam("userId") String userId) {
        
        // Increment request counter
        requestCounter.increment();
        
        // Start timer  
        Timer.Sample sample = Timer.start(Metrics.globalRegistry);
        
        try {
            // Simulate some work
            Thread.sleep(100);
            
            // Track business metrics
            Metrics.counter("dotcms.business.user.lookups", 
                "source", "api",
                "type", "id")
                .increment();
            
            // Simulate response
            return Response.ok("{\"user\":\"" + userId + "\"}").build();
            
        } catch (Exception e) {
            // Increment error counter
            errorCounter.increment();
            
            // Tag errors by type
            Metrics.counter("dotcms.api.errors.by_type",
                "endpoint", "example", 
                "error_type", e.getClass().getSimpleName())
                .increment();
            
            return Response.status(500).entity("Error occurred").build();
            
        } finally {
            // Always record timing
            sample.stop(requestTimer);
        }
    }
    
    @GET
    @Path("/stats")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getStats() {
        
        // Track different operation
        Metrics.counter("dotcms.api.requests.total", 
            "endpoint", "example",
            "operation", "stats")
            .increment();
        
        // Use functional timer for simple timing
        try {
            return Metrics.timer("dotcms.api.request.duration", 
                "endpoint", "example", 
                "operation", "stats")
                .recordCallable(() -> {
                    // Your business logic here
                    return Response.ok("{\"status\":\"ok\"}").build();
                });
        } catch (Exception e) {
            return Response.status(500).entity("Error in stats").build();
        }
    }
}