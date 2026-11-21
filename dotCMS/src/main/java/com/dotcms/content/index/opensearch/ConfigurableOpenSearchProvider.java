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
import org.apache.hc.client5.http.psl.PublicSuffixMatcherLoader;
import org.apache.hc.client5.http.ssl.DefaultHostnameVerifier;
import org.apache.hc.client5.http.ssl.HostnameVerificationPolicy;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.client5.http.ssl.TrustAllStrategy;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.ssl.SSLContexts;
import org.apache.hc.core5.ssl.TrustStrategy;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.json.jackson.JacksonJsonpMapper;
import org.opensearch.client.transport.OpenSearchTransport;
import org.opensearch.client.transport.httpclient5.ApacheHttpClient5TransportBuilder;

import javax.net.ssl.SSLContext;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.cert.CertificateFactory;
import java.util.Arrays;
import java.util.List;

/**
 * Internal configurable OpenSearch client provider implementation.
 * Package-private class used internally by OpenSearchClients and for test configurations.
 * Provides a straightforward way to configure and create OpenSearch clients.
 */
class ConfigurableOpenSearchProvider {

    /** Certificates already reported by {@link #warnOnce}, so a steady state is logged once. */
    private static final java.util.Set<String> WARNED_CERTIFICATES =
            java.util.concurrent.ConcurrentHashMap.newKeySet();

    /** Upper bound for {@link #WARNED_CERTIFICATES}, so a hostile peer cannot grow it without end. */
    private static final int MAX_WARNED_CERTIFICATES = 128;

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
        builder.certRequired(IndexConfigHelper.getBoolean(OSIndexProperty.TLS_CERT_REQUIRED, true));
        builder.trustSelfSigned(IndexConfigHelper.getBoolean(OSIndexProperty.TLS_TRUST_SELF_SIGNED, false));
        builder.expiredCertPolicy(ExpiredCertPolicy.from(
                IndexConfigHelper.getString(OSIndexProperty.TLS_ON_EXPIRED_CERT, null)));

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
                throw new DotRuntimeException("Failed to configure HTTP client: " + e.getMessage(), e);
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
     * Configures the async connection manager: TLS trust and hostname verification, plus the
     * connection pool limits.
     *
     * <p>TLS is applied whenever the config has {@code tlsEnabled=true} OR any endpoint uses the
     * {@code https} scheme. With {@code certRequired=true} (the default) the server certificate is
     * validated against the trust anchors chosen in {@link #buildSSLContext} and the server name
     * must match it. With {@code certRequired=false} neither is checked.
     *
     * @param httpClientBuilder the HTTP client being configured
     * @param config the resolved client configuration
     * @throws GeneralSecurityException if the trust material cannot be set up
     * @throws IOException if the configured CA file cannot be read
     */
    private void configureConnectionManager(
            org.apache.hc.client5.http.impl.async.HttpAsyncClientBuilder httpClientBuilder,
            OSClientConfig config) throws GeneralSecurityException, IOException {

        org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManagerBuilder connManagerBuilder =
            org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManagerBuilder.create()
                .setMaxConnTotal(config.maxConnections())
                .setMaxConnPerRoute(config.maxConnectionsPerRoute());

        if (needsTls(config)) {
            SSLContext sslContext = buildSSLContext(config);
            ClientTlsStrategyBuilder tlsBuilder = ClientTlsStrategyBuilder.create()
                .setSslContext(sslContext);
            if (config.certRequired()) {
                // Set explicitly on purpose: with no verifier, httpclient5 5.5 leaves the name check
                // to the JDK alone (HostnameVerificationPolicy BUILTIN), and that path does not
                // take effect on this async transport — the TLS parameters are mutated after they
                // have been handed to the engine — so the name would not be checked at all.
                tlsBuilder.setHostnameVerifier(new DefaultHostnameVerifier(PublicSuffixMatcherLoader.getDefault()));
            } else {
                // The policy must be set alongside the verifier: left to its default, httpclient5
                // asks the JDK to check the name during the handshake (HostnameVerificationPolicy
                // BUILTIN/BOTH), so silencing the client-side verifier alone stops working from
                // httpclient5 5.6 on, where that JDK path is no longer inert.
                tlsBuilder.setHostnameVerifier(NoopHostnameVerifier.INSTANCE)
                        .setHostnameVerificationPolicy(HostnameVerificationPolicy.CLIENT);
                Logger.warn(this.getClass(), "OpenSearch TLS server certificate and hostname verification"
                    + " is turned off (OS_TLS_CERT_REQUIRED=false). Set it to true, and set OS_TLS_CA_CERT"
                    + " when the server certificate is signed by a private CA.");
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

    /**
     * Chooses the trust anchors for the TLS connection.
     *
     * <p>With {@code certRequired=false} every certificate is accepted. Otherwise, in priority order:
     * the CA file in {@code caCertPath}, then {@code trustSelfSigned}, then the JVM truststore.</p>
     *
     * @param config the resolved client configuration
     * @return the {@link SSLContext} the client uses for its connections
     * @throws GeneralSecurityException if the trust material cannot be set up
     * @throws IOException if the configured CA file cannot be read
     */
    private SSLContext buildSSLContext(OSClientConfig config) throws GeneralSecurityException, IOException {
        if (!config.certRequired()) {
            return SSLContexts.custom()
                .loadTrustMaterial(TrustAllStrategy.INSTANCE)
                .build();
        }
        if (config.caCertPath().isPresent()) {
            final KeyStore anchors = loadCaKeyStore(config.caCertPath().get());
            // PKIX never checks the validity dates of a trust anchor, so the pinned certificates are
            // checked here, once, while the context is built.
            applyExpiredCertPolicyToAnchors(config, anchors);
            return SSLContexts.custom()
                .loadTrustMaterial(anchors, expiryPolicyStrategy(config))
                .build();
        }
        if (config.trustSelfSigned()) {
            return SSLContexts.custom()
                .loadTrustMaterial(selfAnchoredChainStrategy(config))
                .build();
        }
        // The JVM truststore path deliberately keeps the default context: building one here would
        // drop the key material the JVM installs from javax.net.ssl.keyStore, so a deployment that
        // presents a client certificate to OpenSearch that way would stop presenting it. Nothing is
        // lost by it — the anchors are the JVM's own CAs, and PKIX date-checks everything else.
        return SSLContext.getDefault();
    }

    /**
     * Trust decision for {@code OS_TLS_TRUST_SELF_SIGNED=true}: accept the chain the server presents
     * when it anchors itself, meaning the last certificate is self-signed and every certificate is
     * signed by the next one.
     *
     * <p>httpclient5's own {@code TrustSelfSignedStrategy} accepts a chain of exactly one
     * certificate and nothing else, so a node that sends its signing CA alongside its own
     * certificate — the recommended server configuration — was refused, while a certificate from an
     * unknown private CA that happened to be sent alone was accepted. This looks at what the chain
     * is instead of how long it is.</p>
     *
     * @param config the resolved client configuration, consulted for the expired-certificate policy
     * @return the trust strategy handed to the TLS context
     */
    private static TrustStrategy selfAnchoredChainStrategy(final OSClientConfig config) {
        return (chain, authType) -> {
            applyExpiredCertPolicy(config, chain);
            if (isSelfAnchored(chain)) {
                return true;
            }
            // Refusing here only hands the chain to PKIX, which will reject it when the issuer is a
            // private CA. Say so once, with the issuer and the way out, or the operator is left with
            // a bare "certification path" error on a connection that OS_TLS_TRUST_SELF_SIGNED
            // appeared to cover.
            if (chain != null && chain.length > 0) {
                warnOnce(chain[0], "The OpenSearch server certificate '"
                        + chain[0].getSubjectX500Principal().getName() + "' issued by '"
                        + chain[0].getIssuerX500Principal().getName() + "' is not self-signed, so"
                        + " OS_TLS_TRUST_SELF_SIGNED does not cover it and the usual validation"
                        + " applies. Set OS_TLS_CA_CERT to the PEM file of the issuing CA.");
            }
            return false;
        };
    }

    /**
     * Logs a TLS warning the first time it is seen for a given certificate and at DEBUG afterwards.
     * The trust decision runs on every handshake, and an expired or privately-signed certificate is
     * a steady state that lasts months, so an unguarded WARN would repeat for every connection the
     * pool opens.
     *
     * @param certificate the certificate the message is about
     * @param message the message to log
     */
    private static void warnOnce(final X509Certificate certificate, final String message) {
        final String key = certificate.getIssuerX500Principal().getName()
                + '|' + certificate.getSerialNumber();
        if (WARNED_CERTIFICATES.size() < MAX_WARNED_CERTIFICATES && WARNED_CERTIFICATES.add(key)) {
            Logger.warn(ConfigurableOpenSearchProvider.class, message);
        } else {
            Logger.debug(ConfigurableOpenSearchProvider.class, message);
        }
    }

    /**
     * Applies the expired-certificate policy to the trust anchors configured in
     * {@code OS_TLS_CA_CERT}, which PKIX itself never date-checks.
     *
     * @param config the resolved client configuration
     * @param anchors the trust store built from the configured PEM file
     * @throws GeneralSecurityException when the policy is {@code REJECT} and an anchor is expired
     */
    private static void applyExpiredCertPolicyToAnchors(final OSClientConfig config,
            final KeyStore anchors) throws GeneralSecurityException {
        if (ExpiredCertPolicy.ALLOW == config.expiredCertPolicy()) {
            return;
        }
        final java.util.Enumeration<String> aliases = anchors.aliases();
        while (aliases.hasMoreElements()) {
            final Certificate certificate = anchors.getCertificate(aliases.nextElement());
            if (certificate instanceof X509Certificate) {
                applyExpiredCertPolicy(config, new X509Certificate[]{(X509Certificate) certificate});
            }
        }
    }

    /**
     * Trust strategy that only applies {@code OS_TLS_ON_EXPIRED_CERT} and never grants trust by
     * itself, so the normal PKIX validation still decides whether to accept the chain. It is needed
     * because PKIX does not check the validity dates of a trust anchor, which is exactly what a
     * certificate pinned through {@code OS_TLS_CA_CERT} becomes.
     *
     * @param config the resolved client configuration
     * @return a strategy that always defers to PKIX
     */
    private static TrustStrategy expiryPolicyStrategy(final OSClientConfig config) {
        return (chain, authType) -> {
            applyExpiredCertPolicy(config, chain);
            return false;
        };
    }

    /**
     * Returns whether the chain closes on itself, which is what an operator opts into with
     * {@code OS_TLS_TRUST_SELF_SIGNED=true}: the last certificate is self-signed, and every other
     * certificate carries a signature made by the one after it.
     *
     * @param chain the certificate chain the server presented
     * @return {@code true} when the chain is self-anchored and internally consistent
     */
    private static boolean isSelfAnchored(final X509Certificate[] chain) {
        if (chain == null || chain.length == 0) {
            return false;
        }
        final X509Certificate root = chain[chain.length - 1];
        if (!root.getIssuerX500Principal().equals(root.getSubjectX500Principal())) {
            return false;
        }
        try {
            for (int i = 0; i < chain.length - 1; i++) {
                chain[i].verify(chain[i + 1].getPublicKey());
            }
            root.verify(root.getPublicKey());
        } catch (GeneralSecurityException e) {
            Logger.debug(ConfigurableOpenSearchProvider.class,
                    "The OpenSearch server chain is not self-anchored: " + e.getMessage());
            return false;
        }
        return true;
    }

    /**
     * Applies {@code OS_TLS_ON_EXPIRED_CERT} to every certificate the server presented.
     *
     * @param config the resolved client configuration
     * @param chain the certificate chain the server presented
     * @throws CertificateException when the policy is {@code REJECT} and a certificate is expired or
     *         not valid yet
     */
    private static void applyExpiredCertPolicy(final OSClientConfig config,
            final X509Certificate[] chain) throws CertificateException {
        if (chain == null || ExpiredCertPolicy.ALLOW == config.expiredCertPolicy()) {
            return;
        }
        for (final X509Certificate certificate : chain) {
            try {
                certificate.checkValidity();
            } catch (CertificateException e) {
                final String detail = "The OpenSearch server certificate '"
                        + certificate.getSubjectX500Principal().getName() + "' is not valid: "
                        + e.getMessage() + ".";
                if (ExpiredCertPolicy.REJECT == config.expiredCertPolicy()) {
                    throw new CertificateException(detail
                            + " The connection was refused because OS_TLS_ON_EXPIRED_CERT=REJECT.", e);
                }
                // Deliberately not phrased as "accepted": on the CA-file and truststore paths the
                // normal validation still runs afterwards and may reject this very certificate.
                warnOnce(certificate, detail
                        + " OS_TLS_ON_EXPIRED_CERT=WARN, so the expiry alone does not refuse the"
                        + " connection; set it to REJECT to refuse it.");
            }
        }
    }

    /**
     * Reads the PEM file at the given path into a trust store holding every certificate in it.
     *
     * @param caCertPath path of the PEM file given in {@code OS_TLS_CA_CERT}
     * @return a {@link KeyStore} usable as the set of trust anchors
     * @throws GeneralSecurityException if the file holds no valid certificate
     * @throws IOException if the file cannot be read
     */
    private static KeyStore loadCaKeyStore(String caCertPath) throws GeneralSecurityException, IOException {
        KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
        trustStore.load(null, null);
        try (InputStream in = Files.newInputStream(Paths.get(caCertPath))) {
            int index = 0;
            for (Certificate certificate : CertificateFactory.getInstance("X.509").generateCertificates(in)) {
                trustStore.setCertificateEntry("ca-" + index++, certificate);
            }
            if (index == 0) {
                throw new GeneralSecurityException("No certificate found in OS_TLS_CA_CERT file " + caCertPath);
            }
        } catch (IOException e) {
            throw new IOException("Unable to read the OS_TLS_CA_CERT file " + caCertPath + ": " + e, e);
        }
        return trustStore;
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