package com.dotcms.rest.api.v1.maintenance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dotcms.cluster.bean.Server;
import com.dotmarketing.util.Config;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsServer;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import javax.ws.rs.core.StreamingOutput;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Unit tests for how {@link ClusterLogCollector} reaches peer servers: which scheme each value of
 * {@code CLUSTER_LOG_DOWNLOAD_SCHEME} selects, which port each peer is requested on, and how the
 * HTTPS peer certificate is checked. Peers are local in-process HTTP and HTTPS servers bound to
 * {@code 127.0.0.1}; certificates are generated for each run.
 */
class ClusterLogCollectorPeerRequestTest {

    private static final String SCHEME_KEY = "CLUSTER_LOG_DOWNLOAD_SCHEME";
    private static final String CONNECT_TIMEOUT_KEY = "CLUSTER_LOG_DOWNLOAD_CONNECT_TIMEOUT_SECONDS";
    private static final String TIMEOUT_KEY = "CLUSTER_LOG_DOWNLOAD_TIMEOUT_SECONDS";
    private static final String PORT_KEY = "CLUSTER_LOG_DOWNLOAD_PORT";
    private static final String VERIFY_KEY = "CLUSTER_LOG_DOWNLOAD_VERIFY_PEER_CERTS";
    private static final String HOSTNAME_PROPERTY = "jdk.internal.httpclient.disableHostnameVerification";

    private static final String LOOPBACK = "127.0.0.1";
    private static final String BEARER_TOKEN = "peer-request-test-token";
    private static final String LOG_FILE_NAME = "peer-request-test.log";
    private static final String LOCAL_LOG = "local log line\n";
    private static final String PEER_LOG = "peer log line\n";
    private static final String LOCAL_ENTRY = "local-node_" + LOG_FILE_NAME;
    private static final String PEER_ENTRY = "peer-node_" + LOG_FILE_NAME;
    private static final String ERRORS_ENTRY = "_errors.txt";
    private static final char[] KEY_PASSWORD = UUID.randomUUID().toString().toCharArray();
    private static final SecureRandom RANDOM = new SecureRandom();

    private static SSLContext contextTrustingTestCa;
    private static KeyStore caIssuedForLoopback;
    private static KeyStore caIssuedForOtherHost;
    private static KeyStore selfSignedForLoopback;

    private final Map<String, String> savedConfig = new HashMap<>();
    private final List<String> receivedAuthorization = new CopyOnWriteArrayList<>();
    private SSLContext savedDefaultContext;
    private String savedHostnameProperty;
    private HttpServer peer;
    private ServerSocket silentPeer;
    private int peerStatus = 200;
    private long peerDelayMillis;
    private File localLog;

    /**
     * Creates a test CA, a context that trusts it, and the key stores the HTTPS peers present.
     */
    @BeforeAll
    static void createCertificates() throws Exception {
        final KeyPair caKeys = rsaKeyPair();
        final X500Name caName = new X500Name("CN=Peer Request Test CA");
        final X509Certificate caCert = certificate(caName, caKeys.getPublic(), caName,
                caKeys.getPrivate(), true, null);

        final KeyPair loopbackKeys = rsaKeyPair();
        caIssuedForLoopback = keyStore(loopbackKeys.getPrivate(),
                certificate(new X500Name("CN=" + LOOPBACK), loopbackKeys.getPublic(), caName,
                        caKeys.getPrivate(), false, new GeneralName(GeneralName.iPAddress, LOOPBACK)),
                caCert);

        final KeyPair otherHostKeys = rsaKeyPair();
        caIssuedForOtherHost = keyStore(otherHostKeys.getPrivate(),
                certificate(new X500Name("CN=peer.invalid"), otherHostKeys.getPublic(), caName,
                        caKeys.getPrivate(), false, new GeneralName(GeneralName.dNSName, "peer.invalid")),
                caCert);

        final KeyPair selfSignedKeys = rsaKeyPair();
        final X500Name selfSignedName = new X500Name("CN=Self Signed Peer");
        selfSignedForLoopback = keyStore(selfSignedKeys.getPrivate(),
                certificate(selfSignedName, selfSignedKeys.getPublic(), selfSignedName,
                        selfSignedKeys.getPrivate(), false, new GeneralName(GeneralName.iPAddress, LOOPBACK)));

        final KeyStore trustStore = KeyStore.getInstance("PKCS12");
        trustStore.load(null, null);
        trustStore.setCertificateEntry("peer-request-test-ca", caCert);
        final TrustManagerFactory trustManagers =
                TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trustManagers.init(trustStore);
        contextTrustingTestCa = SSLContext.getInstance("TLS");
        contextTrustingTestCa.init(null, trustManagers.getTrustManagers(), null);
    }

    /**
     * Saves the global state the tests change and sets short timeouts with no scheme, port or
     * certificate verification configured.
     */
    @BeforeEach
    void setUp() throws Exception {
        for (final String key : List.of(SCHEME_KEY, CONNECT_TIMEOUT_KEY, TIMEOUT_KEY, PORT_KEY, VERIFY_KEY)) {
            savedConfig.put(key, Config.getStringProperty(key, null));
        }
        savedDefaultContext = SSLContext.getDefault();
        savedHostnameProperty = System.getProperty(HOSTNAME_PROPERTY);

        Config.setProperty(SCHEME_KEY, null);
        Config.setProperty(PORT_KEY, null);
        Config.setProperty(VERIFY_KEY, null);
        Config.setProperty(CONNECT_TIMEOUT_KEY, 2);
        Config.setProperty(TIMEOUT_KEY, 3);

        localLog = Files.createTempFile("peer-request-test-", ".log").toFile();
        Files.writeString(localLog.toPath(), LOCAL_LOG, StandardCharsets.UTF_8);
    }

    /**
     * Stops the peer and restores the configuration, the default SSL context and the system
     * property.
     */
    @AfterEach
    void tearDown() throws Exception {
        if (peer != null) {
            peer.stop(0);
        }
        if (silentPeer != null) {
            silentPeer.close();
        }
        savedConfig.forEach(Config::setProperty);
        SSLContext.setDefault(savedDefaultContext);
        if (savedHostnameProperty == null) {
            System.clearProperty(HOSTNAME_PROPERTY);
        } else {
            System.setProperty(HOSTNAME_PROPERTY, savedHostnameProperty);
        }
        Files.deleteIfExists(localLog.toPath());
    }

    /**
     * With no scheme configured, a peer that serves plain HTTP receives no request.
     */
    @Test
    void defaultScheme_plainHttpPeer_receivesNoRequest() throws Exception {
        final Map<String, String> zip = collectFrom(startHttpPeer());

        assertNoRequestAndLocalLogWithErrors(zip);
    }

    /**
     * The value {@code auto} selects HTTPS only, like the default.
     */
    @Test
    void autoScheme_plainHttpPeer_receivesNoRequest() throws Exception {
        Config.setProperty(SCHEME_KEY, "auto");

        final Map<String, String> zip = collectFrom(startHttpPeer());

        assertNoRequestAndLocalLogWithErrors(zip);
    }

    /**
     * A value that is not a known scheme selects HTTPS only.
     */
    @ParameterizedTest
    @ValueSource(strings = {"htps", "ftp", ""})
    void unrecognizedScheme_plainHttpPeer_receivesNoRequest(final String scheme) throws Exception {
        Config.setProperty(SCHEME_KEY, scheme);

        final Map<String, String> zip = collectFrom(startHttpPeer());

        assertNoRequestAndLocalLogWithErrors(zip);
    }

    /**
     * The value {@code http}, in any case and with surrounding spaces, fetches the peer log over
     * plain HTTP with the bearer token.
     */
    @ParameterizedTest
    @ValueSource(strings = {"http", "HTTP", " http "})
    void httpScheme_fetchesPeerLogOverHttp(final String scheme) throws Exception {
        Config.setProperty(SCHEME_KEY, scheme);

        final Map<String, String> zip = collectFrom(startHttpPeer());

        assertPeerLogFetched(zip);
    }

    /**
     * Peers are requested on the port given by {@code CLUSTER_LOG_DOWNLOAD_PORT}, not on the port
     * the local server listens on.
     */
    @Test
    void configuredPeerPort_overridesRequestLocalPort() throws Exception {
        Config.setProperty(SCHEME_KEY, "http");
        final int peerPort = startHttpPeer();

        final Map<String, String> zip = collectFrom(peerPort, 65534);

        assertPeerLogFetched(zip);
    }

    /**
     * Without {@code CLUSTER_LOG_DOWNLOAD_PORT}, HTTPS peers are requested on dotCMS's default
     * secure port, 8443.
     */
    @Test
    void resolvePeerPort_https_defaultsTo8443() {
        assertEquals(8443, ClusterLogCollector.resolvePeerPort("https", 1234),
                "Default HTTPS peer port");
    }

    /**
     * Without {@code CLUSTER_LOG_DOWNLOAD_PORT}, plain HTTP peers are requested on the local
     * server's own port.
     */
    @Test
    void resolvePeerPort_http_usesRequestPort() {
        assertEquals(1234, ClusterLogCollector.resolvePeerPort("http", 1234),
                "Default HTTP peer port");
    }

    /**
     * {@code CLUSTER_LOG_DOWNLOAD_PORT} wins over the scheme defaults for both schemes.
     */
    @Test
    void resolvePeerPort_configured_overridesSchemeDefaults() {
        Config.setProperty(PORT_KEY, "9443");

        assertEquals(9443, ClusterLogCollector.resolvePeerPort("https", 1234),
                "Configured HTTPS peer port");
        assertEquals(9443, ClusterLogCollector.resolvePeerPort("http", 1234),
                "Configured HTTP peer port");
    }

    /**
     * With nothing configured, a peer presenting a self-signed certificate is fetched over HTTPS:
     * peer certificates are accepted without verification by default.
     */
    @Test
    void defaultScheme_selfSignedPeerCertificate_fetchesPeerLog() throws Exception {
        final Map<String, String> zip = collectFrom(startHttpsPeer(selfSignedForLoopback));

        assertPeerLogFetched(zip);
    }

    /**
     * With nothing configured, a peer whose certificate names another host is still fetched:
     * host name checking is off unless {@code CLUSTER_LOG_DOWNLOAD_VERIFY_PEER_CERTS} is set.
     */
    @Test
    void defaultScheme_certificateForAnotherHost_fetchesPeerLog() throws Exception {
        final Map<String, String> zip = collectFrom(startHttpsPeer(caIssuedForOtherHost));

        assertPeerLogFetched(zip);
    }

    /**
     * With certificate verification on, a peer whose certificate is issued by a trusted CA for
     * its address is fetched over HTTPS.
     */
    @Test
    void verifyPeerCerts_trustedPeerCertificate_fetchesPeerLog() throws Exception {
        Config.setProperty(VERIFY_KEY, "true");
        SSLContext.setDefault(contextTrustingTestCa);

        final Map<String, String> zip = collectFrom(startHttpsPeer(caIssuedForLoopback));

        assertPeerLogFetched(zip);
    }

    /**
     * With certificate verification on, a peer whose certificate is not trusted by the JVM is
     * not sent the request, and its error entry reports the TLS handshake with the scheme
     * setting hint.
     */
    @Test
    void verifyPeerCerts_untrustedPeerCertificate_isRejected() throws Exception {
        Config.setProperty(VERIFY_KEY, "true");

        final Map<String, String> zip = collectFrom(startHttpsPeer(selfSignedForLoopback));

        assertNoRequestAndLocalLogWithErrors(zip);
        assertTlsFailureWithSchemeHint(zip);
    }

    /**
     * With certificate verification on, a peer whose trusted certificate names another host is
     * not sent the request, and its error entry reports the TLS handshake with the scheme
     * setting hint.
     */
    @Test
    void verifyPeerCerts_certificateForAnotherHost_isRejected() throws Exception {
        Config.setProperty(VERIFY_KEY, "true");
        SSLContext.setDefault(contextTrustingTestCa);

        final Map<String, String> zip = collectFrom(startHttpsPeer(caIssuedForOtherHost));

        assertNoRequestAndLocalLogWithErrors(zip);
        assertTlsFailureWithSchemeHint(zip);
    }

    /**
     * With no scheme configured, a peer whose port accepts no connection is reported as such,
     * without the scheme setting hint.
     */
    @Test
    void defaultScheme_closedPeerPort_errorEntryHasNoSchemeHint() throws Exception {
        final int closedPort = startHttpPeer();
        peer.stop(0);
        peer = null;

        final Map<String, String> zip = collectFrom(closedPort);

        assertTrue(zip.containsKey(ERRORS_ENTRY), "Expected " + ERRORS_ENTRY + " in " + zip.keySet());
        final String errors = zip.get(ERRORS_ENTRY);
        assertTrue(errors.contains("peer-node"), "Error entry should name the peer: " + errors);
        assertTrue(errors.contains("could not connect"), "Error entry should give the reason: " + errors);
        assertFalse(errors.contains(SCHEME_KEY), "Error entry should not name " + SCHEME_KEY + ": " + errors);
    }

    /**
     * With no scheme configured, a peer that accepts the connection but never answers is reported
     * as a connect timeout, with the scheme setting hint.
     */
    @Test
    void defaultScheme_peerNeverAnswers_errorEntryReportsConnectTimeoutWithSchemeHint() throws Exception {
        silentPeer = new ServerSocket(0, 1, InetAddress.getByName(LOOPBACK));

        final Map<String, String> zip = collectFrom(silentPeer.getLocalPort());

        assertTrue(zip.containsKey(ERRORS_ENTRY), "Expected " + ERRORS_ENTRY + " in " + zip.keySet());
        final String errors = zip.get(ERRORS_ENTRY);
        assertTrue(errors.contains("connect timed out"), "Error entry should give the reason: " + errors);
        assertTrue(errors.contains(SCHEME_KEY), "Error entry should name " + SCHEME_KEY + ": " + errors);
    }

    /**
     * Collecting from a self-signed peer leaves the JVM-wide HTTP client system properties as
     * they were: certificate acceptance is scoped to the collector's own client.
     */
    @Test
    void collect_doesNotSetJvmWideHttpClientProperties() throws Exception {
        System.clearProperty(HOSTNAME_PROPERTY);

        final Map<String, String> zip = collectFrom(startHttpsPeer(selfSignedForLoopback));

        assertPeerLogFetched(zip);
        assertNull(System.getProperty(HOSTNAME_PROPERTY),
                HOSTNAME_PROPERTY + " must not be set by the collector");
    }

    /**
     * When an HTTPS peer request fails, the error entry names the scheme setting.
     */
    @Test
    void failedHttpsPeer_errorEntryNamesSchemeSetting() throws Exception {
        final Map<String, String> zip = collectFrom(startHttpPeer());

        assertTrue(zip.containsKey(ERRORS_ENTRY), "Expected " + ERRORS_ENTRY + " in " + zip.keySet());
        final String errors = zip.get(ERRORS_ENTRY);
        assertTrue(errors.contains("peer-node"), "Error entry should name the peer: " + errors);
        assertTrue(errors.contains(SCHEME_KEY), "Error entry should name " + SCHEME_KEY + ": " + errors);
    }

    /**
     * With the {@code http} scheme, the error entry of a failed peer does not carry the scheme
     * setting hint.
     */
    @Test
    void httpScheme_failedPeer_errorEntryHasNoSchemeHint() throws Exception {
        Config.setProperty(SCHEME_KEY, "http");
        final int closedPort = startHttpPeer();
        peer.stop(0);
        peer = null;

        final Map<String, String> zip = collectFrom(closedPort);

        assertTrue(zip.containsKey(ERRORS_ENTRY), "Expected " + ERRORS_ENTRY + " in " + zip.keySet());
        final String errors = zip.get(ERRORS_ENTRY);
        assertTrue(errors.contains("peer-node"), "Error entry should name the peer: " + errors);
        assertFalse(errors.contains(SCHEME_KEY), "Error entry should not name " + SCHEME_KEY + ": " + errors);
    }

    /**
     * A verified HTTPS peer that does not answer within the request timeout is reported as timed out,
     * without the scheme setting hint.
     */
    @Test
    void defaultScheme_trustedPeerAnswersTooLate_errorEntryReportsTimeoutWithoutSchemeHint() throws Exception {
        SSLContext.setDefault(contextTrustingTestCa);
        Config.setProperty(TIMEOUT_KEY, 1);
        peerDelayMillis = 2_000;

        final Map<String, String> zip = collectFrom(startHttpsPeer(caIssuedForLoopback));

        assertTrue(zip.containsKey(ERRORS_ENTRY), "Expected " + ERRORS_ENTRY + " in " + zip.keySet());
        final String errors = zip.get(ERRORS_ENTRY);
        assertTrue(errors.contains("timed out"), "Error entry should give the reason: " + errors);
        assertFalse(errors.contains("connect timed out"), "Error entry should not report a connect timeout: " + errors);
        assertFalse(errors.contains(SCHEME_KEY), "Error entry should not name " + SCHEME_KEY + ": " + errors);
    }

    /**
     * With the {@code http} scheme, a peer that closes the connection without answering is reported
     * as a failed connection, without the scheme setting hint.
     */
    @Test
    void httpScheme_peerClosesConnection_errorEntryHasNoSchemeHint() throws Exception {
        Config.setProperty(SCHEME_KEY, "http");
        silentPeer = new ServerSocket(0, 1, InetAddress.getByName(LOOPBACK));
        final ServerSocket closingPeer = silentPeer;
        final Thread closer = new Thread(() -> {
            while (!closingPeer.isClosed()) {
                try {
                    closingPeer.accept().close();
                } catch (final IOException e) {
                    return;
                }
            }
        });
        closer.setDaemon(true);
        closer.start();

        final Map<String, String> zip = collectFrom(closingPeer.getLocalPort());

        assertTrue(zip.containsKey(ERRORS_ENTRY), "Expected " + ERRORS_ENTRY + " in " + zip.keySet());
        final String errors = zip.get(ERRORS_ENTRY);
        assertTrue(errors.contains("connection failed"), "Error entry should give the reason: " + errors);
        assertFalse(errors.contains(SCHEME_KEY), "Error entry should not name " + SCHEME_KEY + ": " + errors);
    }

    /**
     * A verified HTTPS peer that answers with an error status is reported with that status and
     * without the scheme setting hint.
     */
    @Test
    void httpsPeerErrorStatus_errorEntryNamesStatusWithoutSchemeHint() throws Exception {
        SSLContext.setDefault(contextTrustingTestCa);
        peerStatus = 500;

        final Map<String, String> zip = collectFrom(startHttpsPeer(caIssuedForLoopback));

        assertEquals(List.of("Bearer " + BEARER_TOKEN), receivedAuthorization, "Peer requests");
        assertFalse(zip.containsKey(PEER_ENTRY), "No peer log entry expected: " + zip.keySet());
        assertTrue(zip.containsKey(ERRORS_ENTRY), "Expected " + ERRORS_ENTRY + " in " + zip.keySet());
        final String errors = zip.get(ERRORS_ENTRY);
        assertTrue(errors.contains("peer-node"), "Error entry should name the peer: " + errors);
        assertTrue(errors.contains("HTTP 500"), "Error entry should give the status: " + errors);
        assertFalse(errors.contains(SCHEME_KEY), "Error entry should not name " + SCHEME_KEY + ": " + errors);
    }

    // ---- assertions ----

    private void assertNoRequestAndLocalLogWithErrors(final Map<String, String> zip) {
        assertTrue(receivedAuthorization.isEmpty(),
                "The peer should receive no request, got: " + receivedAuthorization);
        assertEquals(LOCAL_LOG, zip.get(LOCAL_ENTRY), "Local log entry");
        assertFalse(zip.containsKey(PEER_ENTRY), "No peer log entry expected: " + zip.keySet());
        assertTrue(zip.containsKey(ERRORS_ENTRY), "Expected " + ERRORS_ENTRY + " in " + zip.keySet());
    }

    private void assertTlsFailureWithSchemeHint(final Map<String, String> zip) {
        final String errors = zip.get(ERRORS_ENTRY);
        assertTrue(errors.contains("TLS handshake failed"), "Error entry should give the reason: " + errors);
        assertTrue(errors.contains(SCHEME_KEY), "Error entry should name " + SCHEME_KEY + ": " + errors);
    }

    private void assertPeerLogFetched(final Map<String, String> zip) {
        assertEquals(List.of("Bearer " + BEARER_TOKEN), receivedAuthorization, "Peer requests");
        assertEquals(LOCAL_LOG, zip.get(LOCAL_ENTRY), "Local log entry");
        assertEquals(PEER_LOG, zip.get(PEER_ENTRY), "Peer log entry");
        assertFalse(zip.containsKey(ERRORS_ENTRY), "No error entry expected: " + zip.get(ERRORS_ENTRY));
    }

    // ---- collector and peers ----

    private Map<String, String> collectFrom(final int peerPort) throws IOException {
        return collectFrom(peerPort, peerPort);
    }

    /**
     * Runs a collection whose peers are expected on {@code peerPort}, set as
     * {@code CLUSTER_LOG_DOWNLOAD_PORT}, while the collector is told the local server listens on
     * {@code localPort}.
     */
    private Map<String, String> collectFrom(final int peerPort, final int localPort) throws IOException {
        Config.setProperty(PORT_KEY, String.valueOf(peerPort));
        final Server peerServer = Server.builder().withServerId("peer-node").withIpAddress(LOOPBACK).build();
        final Server localServer = Server.builder().withServerId("local-node").withIpAddress(LOOPBACK).build();
        final ClusterLogCollector collector = new ClusterLogCollector(
                List.of(peerServer), LOG_FILE_NAME, BEARER_TOKEN, localPort, localLog, localServer);
        return readZip(collector.collect());
    }

    private int startHttpPeer() throws IOException {
        final HttpServer server = HttpServer.create(loopbackAddress(), 0);
        return start(server);
    }

    private int startHttpsPeer(final KeyStore keyStore) throws Exception {
        final KeyManagerFactory keyManagers =
                KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keyManagers.init(keyStore, KEY_PASSWORD);
        final SSLContext serverContext = SSLContext.getInstance("TLS");
        serverContext.init(keyManagers.getKeyManagers(), null, null);

        final HttpsServer server = HttpsServer.create(loopbackAddress(), 0);
        server.setHttpsConfigurator(new HttpsConfigurator(serverContext));
        return start(server);
    }

    private int start(final HttpServer server) {
        server.createContext("/", this::answer);
        server.start();
        peer = server;
        return server.getAddress().getPort();
    }

    private void answer(final HttpExchange exchange) throws IOException {
        receivedAuthorization.add(String.valueOf(exchange.getRequestHeaders().getFirst("Authorization")));
        if (peerDelayMillis > 0) {
            try {
                Thread.sleep(peerDelayMillis);
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        final byte[] body = PEER_LOG.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(peerStatus, body.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(body);
        }
    }

    private static InetSocketAddress loopbackAddress() throws IOException {
        return new InetSocketAddress(InetAddress.getByName(LOOPBACK), 0);
    }

    private static Map<String, String> readZip(final StreamingOutput output) throws IOException {
        final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        output.write(bytes);

        final Map<String, String> entries = new LinkedHashMap<>();
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                entries.put(entry.getName(), new String(zip.readAllBytes(), StandardCharsets.UTF_8));
            }
        }
        return entries;
    }

    // ---- certificates ----

    private static KeyPair rsaKeyPair() throws Exception {
        final KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048, RANDOM);
        return generator.generateKeyPair();
    }

    private static X509Certificate certificate(final X500Name subject, final PublicKey subjectKey,
            final X500Name issuer, final PrivateKey issuerKey, final boolean isCa,
            final GeneralName subjectAltName) throws Exception {

        final Instant now = Instant.now();
        final X509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(
                issuer, new BigInteger(64, RANDOM).add(BigInteger.ONE),
                Date.from(now.minus(1, ChronoUnit.DAYS)), Date.from(now.plus(1, ChronoUnit.DAYS)),
                subject, subjectKey);
        if (isCa) {
            builder.addExtension(Extension.basicConstraints, true, new BasicConstraints(true));
            builder.addExtension(Extension.keyUsage, true,
                    new KeyUsage(KeyUsage.keyCertSign | KeyUsage.cRLSign));
        }
        if (subjectAltName != null) {
            builder.addExtension(Extension.subjectAlternativeName, false, new GeneralNames(subjectAltName));
        }
        return new JcaX509CertificateConverter().getCertificate(
                builder.build(new JcaContentSignerBuilder("SHA256withRSA").build(issuerKey)));
    }

    private static KeyStore keyStore(final PrivateKey key, final X509Certificate... chain) throws Exception {
        final KeyStore keyStore = KeyStore.getInstance("PKCS12");
        keyStore.load(null, null);
        keyStore.setKeyEntry("peer", key, KEY_PASSWORD, chain);
        return keyStore;
    }
}
