package com.dotcms.content.opensearch.util;

import com.dotmarketing.util.Logger;
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
public final class OpenSearchDefaultClientProvider {

    /**
     * Initialization-on-demand holder idiom for thread-safe singleton creation
     */
    private static final class SingletonHolder {
        private static final OpenSearchDefaultClientProvider INSTANCE = new OpenSearchDefaultClientProvider();
    }

    /**
     * Immutable internal provider - configured once at startup
     */
    private final ConfigurableOpenSearchProvider provider;

    /**
     * Private constructor to prevent direct instantiation
     * Creates immutable provider with default configuration from properties
     */
    private OpenSearchDefaultClientProvider() {
        this.provider = new ConfigurableOpenSearchProvider();
        Logger.info(this.getClass(), "OpenSearchClients initialized with default configuration");
    }

    /**
     * Get the singleton instance using initialization-on-demand holder idiom
     * This is thread-safe and lazy without synchronization overhead
     */
    public static OpenSearchDefaultClientProvider getInstance() {
        return SingletonHolder.INSTANCE;
    }

    /**
     * Get the OpenSearch client using default configuration from properties
     * Thread-safe access to immutable provider
     */
    public OpenSearchClient getClient() {
        return provider.getClient();
    }

    /**
     * Close resources when shutting down
     */
    public void shutdown() {
        Logger.info(this.getClass(), "Shutting down OpenSearch clients");
        try {
            provider.close();
        } catch (IOException e) {
            Logger.warn(this.getClass(), "Error closing OpenSearch provider: " + e.getMessage(), e);
        }
    }

    /**
     * Create a default test configuration for local OpenSearch
     * This is a convenience method for tests
     */
    public static OpenSearchClientConfig createLocalTestConfig() {
        return OpenSearchClientConfig.builder()
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
    public static OpenSearchClientConfig createProductionTestConfig() {
        return OpenSearchClientConfig.builder()
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
    public static OpenSearchClientConfig createClusterTestConfig(String... endpoints) {
        ImmutableOpenSearchClientConfig.Builder builder = OpenSearchClientConfig.builder()
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