package com.dotcms.content.index.opensearch;

import com.dotcms.content.index.IndexConfigHelper;
import com.dotcms.content.index.opensearch.ImmutableOSClientConfig.Builder;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import java.net.URI;
import org.apache.hc.client5.http.auth.AuthScope;
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials;
import org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider;
import org.apache.hc.client5.http.ssl.ClientTlsStrategyBuilder;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.client5.http.ssl.TrustAllStrategy;
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
import java.util.Arrays;
import java.util.List;

/**
 * Internal configurable OpenSearch client provider implementation.
 * Package-private class used internally by OpenSearchClients and for test configurations.
 * Provides a straightforward way to configure and create OpenSearch clients.
 */
class ConfigurableOpenSearchProvider {

    private static final String OS_ENDPOINTS = "OS_ENDPOINTS";

    private static final String BASIC_AUTH_TYPE = "BASIC";
    private static final String JWT_AUTH_TYPE = "JWT";
    private static final String CERT_AUTH_TYPE = "CERT";

    private static final String HTTPS_PROTOCOL = "https";

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
    public ConfigurableOpenSearchProvider(OSClientConfig config) {
        buildClient(config);
    }

    /**
     * Build client using properties configuration
     */
    private void buildClient() {
        try {
            OSClientConfig config = configFromProperties();
            buildClient(config);
        } catch (Exception e) {
            Logger.error(this.getClass(), "Error building OpenSearch client from properties", e);
            throw new DotRuntimeException("Failed to build OpenSearch client", e);
        }
    }

    /**
     * Build client using provided configuration
     */
    private void buildClient(OSClientConfig config) {
        try {
            transport = createTransport(config);
            client = new OpenSearchClient(transport);

            // The human-readable, masked configuration banner is logged once at startup by
            // IndexStartupValidator (alongside the connection outcome). Here we keep only a quiet
            // DEBUG line so runtime (re)builds remain observable without duplicating the banner.
            Logger.debug(this.getClass(), "OpenSearch client built for endpoints: " + config.endpoints());
        } catch (Exception e) {
            Logger.error(this.getClass(), "Error building OpenSearch client", e);
            throw new DotRuntimeException("Failed to build OpenSearch client", e);
        }
    }

    /**
     * Resolves an {@link OSClientConfig} from dotCMS properties. Package-private and {@code static}
     * so {@link IndexStartupValidator} (same package) can re-resolve the configuration from the same
     * properties — for the startup banner and the endpoint-separation check — without exposing any
     * configuration accessor outside this package or widening the {@link OSClientProvider} contract.
     *
     * <p>Resolution is deterministic from properties: the validator's re-resolution matches what the
     * client is built with at startup. It is a re-resolution, not a readback of the live client's
     * stored config; the two could only differ if properties changed without a client rebuild.</p>
     */
    static OSClientConfig configFromProperties() {
        Builder builder = OSClientConfig.builder();

        // Load endpoints
        String[] endpoints = Config.getStringArrayProperty(OS_ENDPOINTS, getDefaultEndpoints());
        builder.endpoints(Arrays.asList(endpoints));

        // Load authentication settings
        String authType = IndexConfigHelper.getString(OSIndexProperty.AUTH_TYPE, BASIC_AUTH_TYPE);

        if (BASIC_AUTH_TYPE.equals(authType)) {
            String username = IndexConfigHelper.getString(OSIndexProperty.AUTH_BASIC_USER, null);
            String password = IndexConfigHelper.getString(OSIndexProperty.AUTH_BASIC_PASSWORD, null);
            if (UtilMethods.isSet(username) && UtilMethods.isSet(password)) {
                builder.username(username).password(password);
            }
        } else if (JWT_AUTH_TYPE.equals(authType)) {
            String token = IndexConfigHelper.getString(OSIndexProperty.AUTH_JWT_TOKEN, null);
            if (UtilMethods.isSet(token)) {
                builder.jwtToken(token);
            }
        } else if (CERT_AUTH_TYPE.equals(authType)) {
            String clientCert = IndexConfigHelper.getString(OSIndexProperty.TLS_CLIENT_CERT, null);
            String clientKey = IndexConfigHelper.getString(OSIndexProperty.TLS_CLIENT_KEY, null);
            if (UtilMethods.isSet(clientCert) && UtilMethods.isSet(clientKey)) {
                builder.clientCertPath(clientCert).clientKeyPath(clientKey);
            }
        }

        // Load TLS settings — trust flags are always loaded so they apply even when
        // OS_TLS_ENABLED=false but the endpoint scheme is https://
        boolean tlsEnabled = IndexConfigHelper.getBoolean(OSIndexProperty.TLS_ENABLED, false);
        builder.tlsEnabled(tlsEnabled);
        builder.certRequired(IndexConfigHelper.getBoolean(OSIndexProperty.TLS_CERT_REQUIRED, false));
        builder.trustSelfSigned(IndexConfigHelper.getBoolean(OSIndexProperty.TLS_TRUST_SELF_SIGNED, false));

        String caCert = IndexConfigHelper.getString(OSIndexProperty.TLS_CA_CERT, null);
        if (UtilMethods.isSet(caCert)) {
            builder.caCertPath(caCert);
        }

        // Load connection settings with defaults
        int connectionTimeout = IndexConfigHelper.getInt(OSIndexProperty.CONNECTION_TIMEOUT, 10000);
        int socketTimeout = IndexConfigHelper.getInt(OSIndexProperty.SOCKET_TIMEOUT, 30000);
        int maxConnections = IndexConfigHelper.getInt(OSIndexProperty.MAX_CONNECTIONS, 100);
        int maxConnectionsPerRoute = IndexConfigHelper.getInt(OSIndexProperty.MAX_CONNECTIONS_PER_ROUTE, 50);

        builder.connectionTimeout(java.time.Duration.ofMillis(connectionTimeout))
               .socketTimeout(java.time.Duration.ofMillis(socketTimeout))
               .maxConnections(maxConnections)
               .maxConnectionsPerRoute(maxConnectionsPerRoute);

        return builder.build();
    }

    /**
     * Get default endpoints if not configured
     */
    private static String[] getDefaultEndpoints() {
        String hostname = IndexConfigHelper.getString(OSIndexProperty.HOSTNAME, "localhost");
        String protocol = IndexConfigHelper.getString(OSIndexProperty.PROTOCOL, HTTPS_PROTOCOL);
        int port = IndexConfigHelper.getInt(OSIndexProperty.PORT, 9200);

        return new String[] { protocol + "://" + hostname + ":" + port };
    }

    /**
     * Create OpenSearch transport based on configuration
     */
    private OpenSearchTransport createTransport(OSClientConfig config) {
        HttpHost[] hosts = createHttpHosts(config.endpoints());

        ApacheHttpClient5TransportBuilder builder = ApacheHttpClient5TransportBuilder.builder(hosts);

        // Configure HTTP client
        builder.setHttpClientConfigCallback(httpClientBuilder -> {
            try {
                configureAuthentication(httpClientBuilder, config);
                configureRequestTimeouts(httpClientBuilder, config);
                configureConnectionManager(httpClientBuilder, config);
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
            // Catch IllegalArgumentException too: URI.create() rejects some malformed strings, and
            // toURL() throws "URI is not absolute" for scheme-less values like "invalid-url-format"
            // (issue #35636). Without this, the unclear raw exception surfaced instead of a
            // pointed, actionable message naming the offending OS_ENDPOINTS value.
            try {
                URL url = URI.create(endpoint).toURL();
                return new HttpHost(url.getProtocol(), url.getHost(), url.getPort());
            } catch (MalformedURLException | IllegalArgumentException e) {
                Logger.error(this.getClass(), "Invalid OS_ENDPOINTS URL: " + endpoint, e);
                throw new DotRuntimeException("Invalid OS_ENDPOINTS URL: '" + endpoint
                        + "'. It must be an absolute URL such as https://hostname:9200. Cause: "
                        + e.getMessage(), e);
            }
        }).toArray(HttpHost[]::new);
    }

    /**
     * Configure authentication for HTTP client
     */
    private void configureAuthentication(org.apache.hc.client5.http.impl.async.HttpAsyncClientBuilder httpClientBuilder,
                                       OSClientConfig config) {
        // Basic authentication
        if (config.username().isPresent() && config.password().isPresent()) {
            BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
            credentialsProvider.setCredentials(new AuthScope(null, -1),
                new UsernamePasswordCredentials(config.username().get(), config.password().get().toCharArray()));
            httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
            Logger.info(this.getClass(), "OpenSearch client configured with basic authentication");
        }

        // JWT authentication will be handled via headers in the request interceptor
        config.jwtToken().ifPresent(token -> {
            httpClientBuilder.addRequestInterceptorLast(
                (request, entity, context) -> request.setHeader("Authorization", "Bearer " + token));
            Logger.info(this.getClass(), "OpenSearch client configured with JWT authentication");
        });
    }

    /**
     * Configure request timeouts (connect + response).
     */
    private void configureRequestTimeouts(
            org.apache.hc.client5.http.impl.async.HttpAsyncClientBuilder httpClientBuilder,
            OSClientConfig config) {
        org.apache.hc.client5.http.config.RequestConfig requestConfig =
            org.apache.hc.client5.http.config.RequestConfig.custom()
                .setConnectTimeout(org.apache.hc.core5.util.Timeout.ofMilliseconds(config.connectionTimeout().toMillis()))
                .setResponseTimeout(org.apache.hc.core5.util.Timeout.ofMilliseconds(config.socketTimeout().toMillis()))
                .build();
        httpClientBuilder.setDefaultRequestConfig(requestConfig);
    }

    /**
     * Configure the async connection manager with TLS strategy and connection pool settings.
     *
     * <p>TLS is applied whenever the config has {@code tlsEnabled=true} OR any endpoint uses
     * the {@code https} scheme. The trust strategy is resolved in priority order:
     * skip-cert (default) → {@code trustSelfSigned} → JVM strict (when {@code certRequired=true}).
     */
    private void configureConnectionManager(
            org.apache.hc.client5.http.impl.async.HttpAsyncClientBuilder httpClientBuilder,
            OSClientConfig config) throws GeneralSecurityException {

        org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManagerBuilder connManagerBuilder =
            org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManagerBuilder.create()
                .setMaxConnTotal(config.maxConnections())
                .setMaxConnPerRoute(config.maxConnectionsPerRoute());

        if (needsTls(config)) {
            SSLContext sslContext = buildSSLContext(config);
            ClientTlsStrategyBuilder tlsBuilder = ClientTlsStrategyBuilder.create()
                .setSslContext(sslContext);
            if (!config.certRequired()) {
                tlsBuilder.setHostnameVerifier(NoopHostnameVerifier.INSTANCE);
            }
            connManagerBuilder.setTlsStrategy(tlsBuilder.build());
            Logger.info(this.getClass(), "OpenSearch TLS configured (certRequired=" + config.certRequired()
                + ", trustSelfSigned=" + config.trustSelfSigned() + ")");
        }

        httpClientBuilder.setConnectionManager(connManagerBuilder.build());
    }

    private boolean needsTls(OSClientConfig config) {
        if (config.tlsEnabled()) {
            return true;
        }
        return config.endpoints().stream()
            .anyMatch(e -> e.toLowerCase().startsWith("https://"));
    }

    private SSLContext buildSSLContext(OSClientConfig config) throws GeneralSecurityException {
        if (!config.certRequired()) {
            return SSLContexts.custom()
                .loadTrustMaterial(TrustAllStrategy.INSTANCE)
                .build();
        }
        if (config.trustSelfSigned()) {
            return SSLContexts.custom()
                .loadTrustMaterial(new TrustSelfSignedStrategy())
                .build();
        }
        return SSLContext.getDefault();
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
    public void rebuildClient(OSClientConfig config) {
        Logger.info(this.getClass(), "Rebuilding OpenSearch client with new configuration");
        try {
            close();
        } catch (IOException e) {
            Logger.warn(this.getClass(), "Error closing existing client during rebuild", e);
        }
        buildClient(config);
    }
}