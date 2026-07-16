package com.dotcms.health.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Health response following draft RFC Health Check Response Format for HTTP APIs
 * (draft-inadarei-api-health-check-06)
 * 
 * This format is compatible with:
 * - Prometheus monitoring
 * - Kubernetes health checks  
 * - Standard monitoring tools
 * - Spring Boot Actuator format
 */
@Value.Immutable
@JsonDeserialize(builder = ImmutableHealthResponse.Builder.class)
@JsonInclude(JsonInclude.Include.NON_ABSENT) // Exclude null and empty Optional values
public interface HealthResponse {
    
    /**
     * (REQUIRED) Indicates whether the service status is acceptable or not.
     * 
     * Standard values:
     * - "pass": healthy (aliases: "ok", "up")
     * - "fail": unhealthy (aliases: "error", "down") 
     * - "warn": healthy with concerns
     */
    HealthStatus status();
    
    /**
     * (OPTIONAL) Public version of the service.
     * Compatible with semantic versioning (e.g., "1.2.3")
     */
    Optional<String> version();
    
    /**
     * (OPTIONAL) Release identifier separate from public version.
     * Used for deployment tracking (e.g., "build-12345", "v1.2.3-456")
     */
    @JsonProperty("releaseId")
    Optional<String> releaseId();
    
    /**
     * (OPTIONAL) Unique identifier of the service in application scope.
     * Useful for microservices and distributed systems.
     */
    @JsonProperty("serviceId") 
    Optional<String> serviceId();
    
    /**
     * (OPTIONAL) Human-friendly description of the service.
     */
    Optional<String> description();
    
    /**
     * Array of individual health check results
     */
    List<HealthCheckResult> checks();
    
    /**
     * Timestamp when the health check was performed
     */
    Instant timestamp();
    
    /**
     * (OPTIONAL) Array of notes relevant to current state of health
     */
    Optional<List<String>> notes();
    
    /**
     * (OPTIONAL) Raw error output for "fail" or "warn" states.
     * Should be omitted for "pass" state.
     */
    Optional<String> output();
    
    /**
     * (OPTIONAL) Links object containing URIs for additional information.
     * Compatible with web-linking standards (RFC 8288).
     * 
     * Common link relations:
     * - "self": Link to this health check endpoint
     * - "about": Link to service documentation  
     * - "metrics": Link to Prometheus metrics endpoint
     * - "logs": Link to service logs
     */
    Optional<Map<String, String>> links();
    
    static ImmutableHealthResponse.Builder builder() {
        return ImmutableHealthResponse.builder();
    }
} 