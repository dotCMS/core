package com.dotcms.content.index.opensearch;

import com.dotcms.content.index.opensearch.ImmutableOpenSearchClientConfig;
import com.dotcms.content.index.opensearch.ImmutableOpenSearchClientConfig.Builder;
import com.dotmarketing.util.UtilMethods;
import org.immutables.value.Value;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

/**
 * Configuration class for OpenSearch client using Builder pattern.
 * Provides a simple and flexible way to configure OpenSearch client settings.
 */
@Value.Immutable
public abstract class OpenSearchClientConfig {

    /**
     * List of OpenSearch endpoints (e.g., "https://localhost:9200")
     */
    public abstract List<String> endpoints();

    /**
     * Username for basic authentication
     */
    public abstract Optional<String> username();

    /**
     * Password for basic authentication
     */
    public abstract Optional<String> password();

    /**
     * JWT token for token-based authentication
     */
    public abstract Optional<String> jwtToken();

    /**
     * Path to client certificate for TLS authentication
     */
    public abstract Optional<String> clientCertPath();

    /**
     * Path to client private key for TLS authentication
     */
    public abstract Optional<String> clientKeyPath();

    /**
     * Path to CA certificate for TLS verification
     */
    public abstract Optional<String> caCertPath();

    /**
     * Whether TLS is enabled
     */
    @Value.Default
    public boolean tlsEnabled() {
        return false;
    }

    /**
     * Whether to trust self-signed certificates
     */
    @Value.Default
    public boolean trustSelfSigned() {
        return false;
    }

    /**
     * Connection timeout
     */
    @Value.Default
    public Duration connectionTimeout() {
        return Duration.ofSeconds(10);
    }

    /**
     * Socket timeout
     */
    @Value.Default
    public Duration socketTimeout() {
        return Duration.ofSeconds(30);
    }

    /**
     * Maximum number of connections
     */
    @Value.Default
    public int maxConnections() {
        return 100;
    }

    /**
     * Maximum connections per route
     */
    @Value.Default
    public int maxConnectionsPerRoute() {
        return 50;
    }

    /**
     * Create a new builder instance
     */
    public static Builder builder() {
        return ImmutableOpenSearchClientConfig.builder();
    }

    /**
     * Validation method to ensure configuration is valid
     */
    @Value.Check
    protected void check() {
        if (endpoints().isEmpty()) {
            throw new IllegalArgumentException("At least one endpoint must be specified");
        }

        for (String endpoint : endpoints()) {
            if (!UtilMethods.isSet(endpoint)) {
                throw new IllegalArgumentException("Endpoints cannot be null or empty");
            }
        }

        // Validate TLS configuration
        if (tlsEnabled()) {
            boolean hasCertAuth = clientCertPath().isPresent() && clientKeyPath().isPresent();
            boolean hasBasicAuth = username().isPresent() && password().isPresent();
            boolean hasJwtAuth = jwtToken().isPresent();

            if (!hasCertAuth && !hasBasicAuth && !hasJwtAuth && !trustSelfSigned()) {
                throw new IllegalArgumentException(
                    "TLS is enabled but no authentication method or trust-self-signed is configured");
            }
        }

        // Validate authentication methods are not conflicting
        int authMethods = 0;
        if (username().isPresent() || password().isPresent()) authMethods++;
        if (jwtToken().isPresent()) authMethods++;
        if (clientCertPath().isPresent() || clientKeyPath().isPresent()) authMethods++;

        if (authMethods > 1) {
            throw new IllegalArgumentException(
                "Only one authentication method should be configured (basic, JWT, or certificate)");
        }
    }
}