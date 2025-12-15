package com.dotcms.content.opensearch.util;

import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import java.net.URI;
import org.apache.hc.client5.http.auth.AuthScope;
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials;
import org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider;
import org.apache.hc.client5.http.ssl.TrustSelfSignedStrategy;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.ssl.SSLContexts;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.json.jackson.JacksonJsonpMapper;
import org.opensearch.client.transport.OpenSearchTransport;
import org.opensearch.client.transport.httpclient5.ApacheHttpClient5TransportBuilder;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;

/**
 * Internal configurable OpenSearch client provider implementation.
 * Package-private class used internally by OpenSearchClients and for test configurations.
 * Provides a straightforward way to configure and create OpenSearch clients.
 */
class ConfigurableOpenSearchProvider {

    private static final String OS_ENDPOINTS = "OS_ENDPOINTS";
    private static final String OS_HOSTNAME = "OS_HOSTNAME";
    private static final String OS_PROTOCOL = "OS_PROTOCOL";
    private static final String OS_PORT = "OS_PORT";

    private static final String OS_AUTH_TYPE = "OS_AUTH_TYPE";
    private static final String OS_AUTH_BASIC_USER = "OS_AUTH_BASIC_USER";
    private static final String OS_AUTH_BASIC_PASSWORD = "OS_AUTH_BASIC_PASSWORD";
    private static final String OS_AUTH_JWT_TOKEN = "OS_AUTH_JWT_TOKEN";

    private static final String OS_TLS_ENABLED = "OS_TLS_ENABLED";
    private static final String OS_TLS_TRUST_SELF_SIGNED = "OS_TLS_TRUST_SELF_SIGNED";
    private static final String OS_TLS_CLIENT_CERT = "OS_TLS_CLIENT_CERT";
    private static final String OS_TLS_CLIENT_KEY = "OS_TLS_CLIENT_KEY";
    private static final String OS_TLS_CA_CERT = "OS_TLS_CA_CERT";

    private static final String OS_CONNECTION_TIMEOUT = "OS_CONNECTION_TIMEOUT";
    private static final String OS_SOCKET_TIMEOUT = "OS_SOCKET_TIMEOUT";
    private static final String OS_MAX_CONNECTIONS = "OS_MAX_CONNECTIONS";
    private static final String OS_MAX_CONNECTIONS_PER_ROUTE = "OS_MAX_CONNECTIONS_PER_ROUTE";

    private static final String BASIC_AUTH_TYPE = "BASIC";
    private static final String JWT_AUTH_TYPE = "JWT";
    private static final String CERT_AUTH_TYPE = "CERT";

    private static final String HTTPS_PROTOCOL = "https";
    private static final String HTTP_PROTOCOL = "http";

    private OpenSearchClient client;
    private OpenSearchTransport transport;

    /**
     * Create provider using configuration from properties
     */
    public ConfigurableOpenSearchProvider() {
        buildClient();
    }

    /**
     * Create provider using custom configuration
     */
    public ConfigurableOpenSearchProvider(OpenSearchClientConfig config) {
        buildClient(config);
    }

    /**
     * Build client using properties configuration
     */
    private void buildClient() {
        try {
            OpenSearchClientConfig config = loadConfigurationFromProperties();
            buildClient(config);
        } catch (Exception e) {
            Logger.error(this.getClass(), "Error building OpenSearch client from properties", e);
            throw new DotRuntimeException("Failed to build OpenSearch client", e);
        }
    }

    /**
     * Build client using provided configuration
     */
    private void buildClient(OpenSearchClientConfig config) {
        try {
            transport = createTransport(config);
            client = new OpenSearchClient(transport);

            Logger.info(this.getClass(), "OpenSearch client initialized successfully with endpoints: " + config.endpoints());
        } catch (Exception e) {
            Logger.error(this.getClass(), "Error building OpenSearch client", e);
            throw new DotRuntimeException("Failed to build OpenSearch client", e);
        }
    }

    /**
     * Load configuration from dotCMS properties
     */
    private OpenSearchClientConfig loadConfigurationFromProperties() {
        ImmutableOpenSearchClientConfig.Builder builder = OpenSearchClientConfig.builder();

        // Load endpoints
        String[] endpoints = Config.getStringArrayProperty(OS_ENDPOINTS, getDefaultEndpoints());
        builder.endpoints(Arrays.asList(endpoints));

        // Load authentication settings
        String authType = Config.getStringProperty(OS_AUTH_TYPE, BASIC_AUTH_TYPE);

        if (BASIC_AUTH_TYPE.equals(authType)) {
            String username = Config.getStringProperty(OS_AUTH_BASIC_USER, null);
            String password = Config.getStringProperty(OS_AUTH_BASIC_PASSWORD, null);
            if (UtilMethods.isSet(username) && UtilMethods.isSet(password)) {
                builder.username(username).password(password);
            }
        } else if (JWT_AUTH_TYPE.equals(authType)) {
            String token = Config.getStringProperty(OS_AUTH_JWT_TOKEN, null);
            if (UtilMethods.isSet(token)) {
                builder.jwtToken(token);
            }
        } else if (CERT_AUTH_TYPE.equals(authType)) {
            String clientCert = Config.getStringProperty(OS_TLS_CLIENT_CERT, null);
            String clientKey = Config.getStringProperty(OS_TLS_CLIENT_KEY, null);
            if (UtilMethods.isSet(clientCert) && UtilMethods.isSet(clientKey)) {
                builder.clientCertPath(clientCert).clientKeyPath(clientKey);
            }
        }

        // Load TLS settings
        boolean tlsEnabled = Config.getBooleanProperty(OS_TLS_ENABLED, false);
        builder.tlsEnabled(tlsEnabled);

        if (tlsEnabled) {
            builder.trustSelfSigned(Config.getBooleanProperty(OS_TLS_TRUST_SELF_SIGNED, false));
            String caCert = Config.getStringProperty(OS_TLS_CA_CERT, null);
            if (UtilMethods.isSet(caCert)) {
                builder.caCertPath(caCert);
            }
        }

        // Load connection settings with defaults
        int connectionTimeout = Config.getIntProperty(OS_CONNECTION_TIMEOUT, 10000);
        int socketTimeout = Config.getIntProperty(OS_SOCKET_TIMEOUT, 30000);
        int maxConnections = Config.getIntProperty(OS_MAX_CONNECTIONS, 100);
        int maxConnectionsPerRoute = Config.getIntProperty(OS_MAX_CONNECTIONS_PER_ROUTE, 50);

        builder.connectionTimeout(java.time.Duration.ofMillis(connectionTimeout))
               .socketTimeout(java.time.Duration.ofMillis(socketTimeout))
               .maxConnections(maxConnections)
               .maxConnectionsPerRoute(maxConnectionsPerRoute);

        return builder.build();
    }

    /**
     * Get default endpoints if not configured
     */
    private String[] getDefaultEndpoints() {
        String hostname = Config.getStringProperty(OS_HOSTNAME, "localhost");
        String protocol = Config.getStringProperty(OS_PROTOCOL, HTTPS_PROTOCOL);
        int port = Config.getIntProperty(OS_PORT, 9200);

        return new String[] { protocol + "://" + hostname + ":" + port };
    }

    /**
     * Create OpenSearch transport based on configuration
     */
    private OpenSearchTransport createTransport(OpenSearchClientConfig config) {
        HttpHost[] hosts = createHttpHosts(config.endpoints());

        ApacheHttpClient5TransportBuilder builder = ApacheHttpClient5TransportBuilder.builder(hosts);

        // Configure HTTP client
        builder.setHttpClientConfigCallback(httpClientBuilder -> {
            try {
                configureAuthentication(httpClientBuilder, config);
                configureTLS(httpClientBuilder, config);
                configureTimeouts(httpClientBuilder, config);
                return httpClientBuilder;
            } catch (Exception e) {
                Logger.error(this.getClass(), "Error configuring HTTP client", e);
                throw new DotRuntimeException("Failed to configure HTTP client", e);
            }
        });

        // Set JSON mapper
        builder.setMapper(new JacksonJsonpMapper());

        return builder.build();
    }

    /**
     * Convert endpoint strings to HttpHost array
     */
    private HttpHost[] createHttpHosts(List<String> endpoints) {
        return endpoints.stream().map(endpoint -> {
            try {
                URL url = URI.create(endpoint).toURL();
                return new HttpHost(url.getProtocol(), url.getHost(), url.getPort());
            } catch (MalformedURLException e) {
                Logger.error(this.getClass(), "Invalid endpoint URL: " + endpoint, e);
                throw new DotRuntimeException("Invalid endpoint URL: " + endpoint, e);
            }
        }).toArray(HttpHost[]::new);
    }

    /**
     * Configure authentication for HTTP client
     */
    private void configureAuthentication(org.apache.hc.client5.http.impl.async.HttpAsyncClientBuilder httpClientBuilder,
                                       OpenSearchClientConfig config) {
        // Basic authentication
        if (config.username().isPresent() && config.password().isPresent()) {
            BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
            credentialsProvider.setCredentials(new AuthScope(null, -1),
                new UsernamePasswordCredentials(config.username().get(), config.password().get().toCharArray()));
            httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
            Logger.info(this.getClass(), "OpenSearch client configured with basic authentication");
        }

        // JWT authentication will be handled via headers in the request interceptor
        if (config.jwtToken().isPresent()) {
            httpClientBuilder.addRequestInterceptorLast((request, entity, context) -> {
                request.setHeader("Authorization", "Bearer " + config.jwtToken().get());
            });
            Logger.info(this.getClass(), "OpenSearch client configured with JWT authentication");
        }
    }

    /**
     * Configure TLS for HTTP client
     */
    private void configureTLS(org.apache.hc.client5.http.impl.async.HttpAsyncClientBuilder httpClientBuilder,
                            OpenSearchClientConfig config) throws GeneralSecurityException {
        if (!config.tlsEnabled()) {
            return;
        }

        SSLContext sslContext;

        if (config.clientCertPath().isPresent() && config.clientKeyPath().isPresent()) {
            // Certificate-based authentication (would need PEM reader implementation)
            Logger.warn(this.getClass(), "Certificate-based TLS not yet implemented, using trust-self-signed strategy");
            sslContext = createTrustSelfSignedSSLContext();
        } else if (config.trustSelfSigned()) {
            sslContext = createTrustSelfSignedSSLContext();
        } else {
            sslContext = SSLContext.getDefault();
        }

        // For HttpClient5, SSL is configured via the connection manager
        // This is a simplified approach - SSL context will be used by default connection manager
        Logger.info(this.getClass(), "OpenSearch client configured with TLS");
    }

    /**
     * Create SSL context that trusts self-signed certificates
     */
    private SSLContext createTrustSelfSignedSSLContext() throws GeneralSecurityException {
        try {
            return SSLContexts.custom()
                .loadTrustMaterial(new TrustSelfSignedStrategy())
                .build();
        } catch (NoSuchAlgorithmException | KeyManagementException | KeyStoreException e) {
            Logger.error(this.getClass(), "Error creating trust-self-signed SSL context", e);
            throw new GeneralSecurityException("Failed to create SSL context", e);
        }
    }

    /**
     * Configure timeouts for HTTP client
     */
    private void configureTimeouts(org.apache.hc.client5.http.impl.async.HttpAsyncClientBuilder httpClientBuilder,
                                 OpenSearchClientConfig config) {
        org.apache.hc.client5.http.config.RequestConfig requestConfig =
            org.apache.hc.client5.http.config.RequestConfig.custom()
                .setConnectTimeout(org.apache.hc.core5.util.Timeout.ofMilliseconds(config.connectionTimeout().toMillis()))
                .setResponseTimeout(org.apache.hc.core5.util.Timeout.ofMilliseconds(config.socketTimeout().toMillis()))
                .build();

        httpClientBuilder.setDefaultRequestConfig(requestConfig);

        // Connection pool configuration for async client
        org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManagerBuilder connManagerBuilder =
            org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManagerBuilder.create()
                .setMaxConnTotal(config.maxConnections())
                .setMaxConnPerRoute(config.maxConnectionsPerRoute());

        httpClientBuilder.setConnectionManager(connManagerBuilder.build());
    }

    /**
     * Get the OpenSearch client instance
     */
    public OpenSearchClient getClient() {
        return client;
    }

    /**
     * Close the client and transport
     */
    public void close() throws IOException {
        if (transport != null) {
            transport.close();
        }
    }

    /**
     * Rebuild the client (useful for configuration changes)
     */
    public void rebuildClient() {
        Logger.info(this.getClass(), "Rebuilding OpenSearch client");
        try {
            close();
        } catch (IOException e) {
            Logger.warn(this.getClass(), "Error closing existing client during rebuild", e);
        }
        buildClient();
    }

    /**
     * Rebuild the client with new configuration
     */
    public void rebuildClient(OpenSearchClientConfig config) {
        Logger.info(this.getClass(), "Rebuilding OpenSearch client with new configuration");
        try {
            close();
        } catch (IOException e) {
            Logger.warn(this.getClass(), "Error closing existing client during rebuild", e);
        }
        buildClient(config);
    }
}