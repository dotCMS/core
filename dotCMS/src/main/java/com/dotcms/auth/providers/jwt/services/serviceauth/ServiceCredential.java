package com.dotcms.auth.providers.jwt.services.serviceauth;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import java.util.Optional;

/**
 * Represents a service identity for service-to-service authentication.
 * Each microservice that dotCMS communicates with should have a unique ServiceCredential.
 *
 * Example usage:
 * <pre>
 * ServiceCredential credential = ServiceCredential.builder()
 *     .serviceId("wa11y-checker")
 *     .displayName("Accessibility Checker Service")
 *     .baseUrl("https://wa11y.internal.example.com")
 *     .build();
 * </pre>
 *
 * @author dotCMS
 */
@Value.Immutable
@JsonSerialize(as = ImmutableServiceCredential.class)
@JsonDeserialize(as = ImmutableServiceCredential.class)
public abstract class ServiceCredential {

    /**
     * Unique identifier for the service (e.g., "wa11y-checker", "analytics-service")
     */
    public abstract String serviceId();

    /**
     * Human-readable name for the service
     */
    public abstract Optional<String> displayName();

    /**
     * Base URL for the service (e.g., "https://wa11y.internal.example.com")
     */
    public abstract String baseUrl();

    /**
     * Optional audience claim for JWT validation.
     * If not set, defaults to serviceId.
     */
    @Value.Default
    public String audience() {
        return serviceId();
    }

    /**
     * Whether this service credential is enabled
     */
    @Value.Default
    public boolean enabled() {
        return true;
    }

    /**
     * JWT expiration time in seconds for tokens issued to this service.
     * Default is 300 seconds (5 minutes) - keep short for security.
     */
    @Value.Default
    public int tokenExpirationSeconds() {
        return 300;
    }

    public static ImmutableServiceCredential.Builder builder() {
        return ImmutableServiceCredential.builder();
    }
}
