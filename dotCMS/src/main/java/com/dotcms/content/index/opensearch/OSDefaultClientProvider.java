package com.dotcms.content.index.opensearch;

import com.dotcms.content.index.opensearch.ImmutableOSClientConfig.Builder;
import com.dotmarketing.util.Logger;
import com.google.common.annotations.VisibleForTesting;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;
import org.opensearch.client.opensearch.OpenSearchClient;

import java.io.IOException;

/**
 * Public immutable singleton for OpenSearch client access.
 * Provides a single client instance throughout the application configured once at startup
 * using default configuration from properties.
 *
 * This class uses the Initialization-on-demand holder idiom for thread-safe singleton
 * creation. The singleton is immutable after initialization.
 *
 * For custom configurations in tests, use ConfigurableOpenSearchProvider directly
 * with the OpenSearchClientConfig builder pattern.
 *
 * @author fabrizio
 */
@ApplicationScoped
@Default
public class OSDefaultClientProvider implements OSClientProvider {

    /**
     * Internal provider. Built lazily from properties on first {@link #getClient()} when this bean
     * is CDI-constructed (no-arg), or eagerly when an explicit {@link OSClientConfig} is supplied.
     *
     * <p><strong>Why lazy (issue #35636):</strong> the delegate's construction parses the configured
     * {@code OS_ENDPOINTS} URLs and can throw on a malformed value. Weld's client-proxy constructor
     * invokes this bean's no-arg constructor, so building eagerly here made a malformed
     * {@code OS_ENDPOINTS} crash dotCMS startup before the phase-aware {@code IndexStartupValidator}
     * could run. Deferring the build to {@code getClient()} moves that failure into the validator's
     * (and {@code OSIndexAPIImpl.waitUtilIndexReady()}'s) existing try/catch, so phases 1/2 fall back
     * to ES-only and phase 3 aborts — instead of an uncaught startup crash.</p>
     */
    private volatile ConfigurableOpenSearchProvider provider;

    /**
     * CDI constructor — does NOT build the client. The delegate is created from dotCMS properties
     * lazily on the first {@link #getClient()} call (see {@link #provider}).
     */
    public OSDefaultClientProvider() {
        // Intentionally empty: defer client construction to first getClient() (issue #35636).
    }

    /**
     * Constructor for direct (non-CDI) test use with an explicit {@link OSClientConfig}.
     * Builds eagerly to preserve the existing fail-fast contract for explicitly-configured clients.
     */
    @VisibleForTesting
    public OSDefaultClientProvider(OSClientConfig config) {
        this.provider = new ConfigurableOpenSearchProvider(config);
        Logger.info(this.getClass(), "OpenSearchClients initialized with custom configuration: " + config.endpoints());
    }

    /**
     * Returns the underlying provider, building it from properties on first use.
     * Thread-safe via double-checked locking on the {@code volatile} field.
     */
    private ConfigurableOpenSearchProvider provider() {
        ConfigurableOpenSearchProvider local = provider;
        if (local == null) {
            synchronized (this) {
                local = provider;
                if (local == null) {
                    local = new ConfigurableOpenSearchProvider();
                    provider = local;
                    Logger.info(this.getClass(), "OpenSearchClients initialized with default configuration");
                }
            }
        }
        return local;
    }

    /**
     * Get the OpenSearch client using default configuration from properties.
     * The client is built lazily on the first call (issue #35636).
     */
    public OpenSearchClient getClient() {
        return provider().getClient();
    }

    /**
     * Close resources when shutting down. No-op if the client was never built.
     */
    public void shutdown() {
        final ConfigurableOpenSearchProvider local = provider;
        if (local == null) {
            return;
        }
        Logger.info(this.getClass(), "Shutting down OpenSearch clients");
        try {
            local.close();
        } catch (IOException e) {
            Logger.warn(this.getClass(), "Error closing OpenSearch provider: " + e.getMessage(), e);
        }
    }

    /**
     * Create a default test configuration for local OpenSearch
     * This is a convenience method for tests
     */
    @VisibleForTesting
    public static OSClientConfig createLocalTestConfig() {
        return OSClientConfig.builder()
            .addEndpoints("http://localhost:9201")  // Local OpenSearch port
            .tlsEnabled(false)                      // Security disabled
            .trustSelfSigned(true)
            .connectionTimeout(java.time.Duration.ofSeconds(10))
            .socketTimeout(java.time.Duration.ofSeconds(10))
            .maxConnections(50)
            .maxConnectionsPerRoute(25)
            .build();
    }

    /**
     * Create a default production-like configuration
     * This is a convenience method for different test scenarios
     */
    @VisibleForTesting
    public static OSClientConfig createProductionTestConfig() {
        return OSClientConfig.builder()
            .addEndpoints("https://opensearch.prod.com:9200")
            .username("admin")
            .password("secure_password")
            .tlsEnabled(true)
            .trustSelfSigned(false)
            .connectionTimeout(java.time.Duration.ofSeconds(15))
            .socketTimeout(java.time.Duration.ofSeconds(30))
            .maxConnections(100)
            .maxConnectionsPerRoute(50)
            .build();
    }

    /**
     * Create a cluster configuration for testing
     * This is a convenience method for cluster test scenarios
     */
    @VisibleForTesting
    public static OSClientConfig createClusterTestConfig(String... endpoints) {
        Builder builder = OSClientConfig.builder()
            .tlsEnabled(false)
            .trustSelfSigned(true)
            .connectionTimeout(java.time.Duration.ofSeconds(15))
            .socketTimeout(java.time.Duration.ofSeconds(30))
            .maxConnections(100)
            .maxConnectionsPerRoute(30);

        for (String endpoint : endpoints) {
            builder.addEndpoints(endpoint);
        }

        return builder.build();
    }
}