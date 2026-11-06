package com.dotcms.content.index.opensearch;

import com.dotcms.content.index.opensearch.ImmutableOSClientConfig.Builder;
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
public abstract class OSClientConfig {

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
     * Path to the PEM file of the CA that signed the server certificate. Used as the trust anchor
     * when {@link #certRequired()} is true; otherwise the JVM truststore applies.
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
     * Enforce TLS certificate and hostname verification.
     * Defaults to {@code true}. When the server certificate is signed by a private CA, set
     * {@code OS_TLS_CA_CERT} to that CA's PEM file. Set {@code OS_TLS_CERT_REQUIRED=false} only to
     * connect to a server whose certificate cannot be verified; the client then logs a warning.
     */
    @Value.Default
    public boolean certRequired() {
        return true;
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
        return ImmutableOSClientConfig.builder();
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