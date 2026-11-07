package com.dotcms.content.index.opensearch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.dotcms.content.index.TlsTestServer;
import java.util.List;
import javax.net.ssl.SSLException;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Connection tests for the TLS settings of {@link ConfigurableOpenSearchProvider}. Each case makes a
 * real handshake with a local HTTPS server that presents a self-signed certificate.
 */
public class ConfigurableOpenSearchProviderTlsTest {

    private static final String INFO_BODY = "{\"name\":\"node\",\"cluster_name\":\"test\","
            + "\"cluster_uuid\":\"uuid\",\"version\":{\"distribution\":\"opensearch\",\"number\":\"3.8.0\","
            + "\"build_type\":\"tar\",\"build_hash\":\"hash\",\"build_date\":\"2026-08-03T21:07:36.443334696Z\","
            + "\"build_snapshot\":false,\"lucene_version\":\"10.5.0\","
            + "\"minimum_wire_compatibility_version\":\"2.19.0\","
            + "\"minimum_index_compatibility_version\":\"2.0.0\"},\"tagline\":\"test\"}";

    private static TlsTestServer matchingName;
    private static TlsTestServer otherName;

    @BeforeClass
    public static void startServers() throws Exception {
        matchingName = TlsTestServer.start("localhost", "dns:localhost,ip:127.0.0.1", INFO_BODY);
        otherName = TlsTestServer.start("other.example", "dns:other.example", INFO_BODY);
    }

    @AfterClass
    public static void stopServers() throws Exception {
        matchingName.close();
        otherName.close();
    }

    private static ImmutableOSClientConfig.Builder config(final TlsTestServer server) {
        return OSClientConfig.builder().endpoints(List.of("https://127.0.0.1:" + server.port()));
    }

    private static String connect(final OSClientConfig config) throws Exception {
        return new ConfigurableOpenSearchProvider(config).getClient().info().version().number();
    }

    private static boolean isCertificateRejection(final Throwable error) {
        for (Throwable t = error; t != null; t = t.getCause()) {
            if (t instanceof SSLException) {
                return true;
            }
        }
        return false;
    }

    private static void assertRejectedForName(final OSClientConfig config) {
        try {
            connect(config);
            fail("the connection should have been refused");
        } catch (Exception e) {
            boolean nameMismatch = false;
            for (Throwable t = e; t != null; t = t.getCause()) {
                nameMismatch |= t instanceof SSLException && String.valueOf(t.getMessage())
                        .contains("subject alternative names");
            }
            assertTrue("expected a name mismatch but got: " + e, nameMismatch);
        }
    }

    private static void assertRejected(final OSClientConfig config) {
        try {
            connect(config);
            fail("the connection should have been refused");
        } catch (Exception e) {
            assertTrue("expected a certificate rejection but got: " + e, isCertificateRejection(e));
        }
    }

    @Test
    public void testCertRequired_DefaultsToTrue() {
        assertTrue(config(matchingName).build().certRequired());
    }

    @Test
    public void testDefaultConfig_RefusesUntrustedCertificate() {
        assertRejected(config(matchingName).build());
    }

    @Test
    public void testCertRequiredFalse_AcceptsUntrustedCertificate() throws Exception {
        assertEquals("3.8.0", connect(config(otherName).certRequired(false).build()));
    }

    @Test
    public void testCertRequired_WithCaCertificate_AcceptsMatchingCertificate() throws Exception {
        assertEquals("3.8.0", connect(config(matchingName).certRequired(true)
                .caCertPath(matchingName.certificatePem().toString()).build()));
    }

    @Test
    public void testCertRequired_WithCaCertificate_RefusesMismatchedName() {
        assertRejectedForName(config(otherName).certRequired(true)
                .caCertPath(otherName.certificatePem().toString()).build());
    }

    @Test
    public void testCertRequired_WithTrustSelfSigned_RefusesMismatchedName() {
        assertRejectedForName(config(otherName).certRequired(true).trustSelfSigned(true).build());
    }

    @Test
    public void testCertRequired_WithTrustSelfSigned_AcceptsMatchingCertificate() throws Exception {
        assertEquals("3.8.0", connect(config(matchingName).certRequired(true).trustSelfSigned(true).build()));
    }
}
