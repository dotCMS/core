package com.dotcms.content.index.opensearch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.dotcms.content.index.TlsTestServer;
import java.util.List;
import java.security.cert.CertificateException;
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
    private static TlsTestServer chainFromPrivateCa;
    private static TlsTestServer expiredName;
    private static TlsTestServer leafFromPrivateCa;

    @BeforeClass
    public static void startServers() throws Exception {
        matchingName = TlsTestServer.start("localhost", "dns:localhost,ip:127.0.0.1", INFO_BODY);
        otherName = TlsTestServer.start("other.example", "dns:other.example", INFO_BODY);
        chainFromPrivateCa = TlsTestServer.startSignedByCa("localhost", "dns:localhost,ip:127.0.0.1", INFO_BODY);
        expiredName = TlsTestServer.startExpired("localhost", "dns:localhost,ip:127.0.0.1", INFO_BODY);
        leafFromPrivateCa = TlsTestServer.startSignedByCaWithoutChain("localhost", "dns:localhost,ip:127.0.0.1",
                INFO_BODY);
    }

    @AfterClass
    public static void stopServers() throws Exception {
        matchingName.close();
        otherName.close();
        chainFromPrivateCa.close();
        expiredName.close();
        leafFromPrivateCa.close();
    }

    private static ImmutableOSClientConfig.Builder config(final TlsTestServer server) {
        return OSClientConfig.builder().endpoints(List.of("https://127.0.0.1:" + server.port()));
    }

    private static String connect(final OSClientConfig config) throws Exception {
        final ConfigurableOpenSearchProvider provider = new ConfigurableOpenSearchProvider(config);
        try {
            return provider.getClient().info().version().number();
        } finally {
            provider.close();
        }
    }

    /**
     * A certificate can be refused during the handshake, as an {@link SSLException}, or before it —
     * a trust anchor that is already expired is rejected while the TLS context is being built, and
     * surfaces as a {@link CertificateException} wrapped in the provider's runtime exception.
     *
     * @param error the failure to inspect
     * @return whether the failure was caused by a certificate being refused
     */
    private static boolean isCertificateRejection(final Throwable error) {
        for (Throwable t = error; t != null; t = t.getCause()) {
            if (t instanceof SSLException || t instanceof CertificateException) {
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
        assertRejected(config, null);
    }

    /**
     * Asserts the connection was refused, and — when {@code expectedDetail} is given — that the
     * reason survives the JSSE wrapping, since {@link com.dotcms.content.index.opensearch.OSIndexAPIImpl}
     * classifies the failure from that text.
     *
     * @param config the client configuration under test
     * @param expectedDetail a substring the failure must carry, or {@code null} to only require a
     *        certificate rejection
     */
    private static void assertRejected(final OSClientConfig config, final String expectedDetail) {
        try {
            connect(config);
            fail("the connection should have been refused");
        } catch (Exception e) {
            assertTrue("expected a certificate rejection but got: " + e, isCertificateRejection(e));
            if (expectedDetail != null) {
                boolean found = false;
                for (Throwable t = e; t != null; t = t.getCause()) {
                    found |= String.valueOf(t.getMessage()).contains(expectedDetail);
                }
                assertTrue("expected '" + expectedDetail + "' in the failure but got: " + e, found);
            }
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

    /**
     * With validation off the connection must go through even though this certificate is untrusted
     * <em>and</em> carries a different name, and it must keep doing so after an httpclient5
     * upgrade. Silencing the client-side verifier is not enough on its own: without
     * {@code HostnameVerificationPolicy.CLIENT} the JDK performs the name check itself during the
     * handshake from httpclient5 5.6 on, and this is the assertion that catches it on the bump.
     */
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
    public void testCertRequired_WithMissingCaFile_NamesTheFile() {
        try {
            connect(config(matchingName).certRequired(true).caCertPath("/no/such/ca.pem").build());
            fail("an unreadable CA file should have been reported");
        } catch (Exception e) {
            boolean named = false;
            for (Throwable t = e; t != null; t = t.getCause()) {
                named |= String.valueOf(t.getMessage()).contains("OS_TLS_CA_CERT file /no/such/ca.pem");
            }
            assertTrue("expected the file to be named but got: " + e, named);
        }
    }

    @Test
    public void testCertRequired_WithTrustSelfSigned_RefusesMismatchedName() {
        assertRejectedForName(config(otherName).certRequired(true).trustSelfSigned(true).build());
    }

    @Test
    public void testCertRequired_WithTrustSelfSigned_AcceptsMatchingCertificate() throws Exception {
        assertEquals("3.8.0", connect(config(matchingName).certRequired(true).trustSelfSigned(true).build()));
    }

    /**
     * An expired certificate used to be accepted in silence through either path, because PKIX does
     * not check the validity dates of a trust anchor. The default policy keeps the connection
     * working so that no existing installation breaks on upgrade.
     */
    @Test
    public void testExpiredCertificate_WarnPolicy_StillConnects() throws Exception {
        assertEquals("3.8.0", connect(config(expiredName).certRequired(true).trustSelfSigned(true)
                .expiredCertPolicy(ExpiredCertPolicy.WARN).build()));
    }

    @Test
    public void testExpiredCertificate_RejectPolicy_RefusesTheConnection() {
        assertRejected(config(expiredName).certRequired(true).trustSelfSigned(true)
                .expiredCertPolicy(ExpiredCertPolicy.REJECT).build(),
                "OS_TLS_ON_EXPIRED_CERT=REJECT");
    }

    /**
     * The refusal text is what the operator-facing diagnosis is built from, so the two sides are
     * pinned together: the message must reach the caller and must classify as an expiry problem,
     * not as an unknown issuer, whose remediation would send the operator to OS_TLS_CA_CERT.
     */
    @Test
    public void testExpiredCertificate_RejectPolicy_ClassifiesAsExpired() {
        try {
            connect(config(expiredName).certRequired(true).trustSelfSigned(true)
                    .expiredCertPolicy(ExpiredCertPolicy.REJECT).build());
            fail("the connection should have been refused");
        } catch (Exception e) {
            assertEquals(OSIndexAPIImpl.ConnectionFailureKind.TLS_CERT_EXPIRED,
                    OSIndexAPIImpl.classifyConnectionError(e));
        }
    }

    /**
     * A trust anchor's own validity dates are never checked by PKIX, which is the gap the policy
     * exists for. Pinning an expired certificate as the anchor must be refused before the handshake
     * even reaches the server.
     */
    @Test
    public void testExpiredAnchor_RejectPolicy_RefusesBeforeConnecting() {
        assertRejected(config(matchingName).certRequired(true)
                .caCertPath(expiredName.certificatePem().toString())
                .expiredCertPolicy(ExpiredCertPolicy.REJECT).build(),
                "OS_TLS_ON_EXPIRED_CERT=REJECT");
    }

    /**
     * A certificate signed by a private CA and sent without that CA is not self-signed, so
     * OS_TLS_TRUST_SELF_SIGNED no longer covers it — it used to, because httpclient5's strategy
     * only counted the chain length. The configuration that works for it is OS_TLS_CA_CERT.
     */
    @Test
    public void testCertRequired_WithTrustSelfSigned_RefusesLoneCertificateFromPrivateCa() {
        assertRejected(config(leafFromPrivateCa).certRequired(true).trustSelfSigned(true).build());
    }

    /**
     * The same refusal must apply when the expired certificate is the pinned trust anchor, which is
     * the case PKIX leaves unchecked on its own.
     */
    @Test
    public void testExpiredCertificate_RejectPolicy_RefusesPinnedAnchor() {
        assertRejected(config(expiredName).certRequired(true)
                .caCertPath(expiredName.certificatePem().toString())
                .expiredCertPolicy(ExpiredCertPolicy.REJECT).build());
    }

    @Test
    public void testExpiredCertificate_AllowPolicy_ConnectsWhenPinned() throws Exception {
        assertEquals("3.8.0", connect(config(expiredName).certRequired(true)
                .caCertPath(expiredName.certificatePem().toString())
                .expiredCertPolicy(ExpiredCertPolicy.ALLOW).build()));
    }

    @Test
    public void testExpiredCertPolicy_DefaultsToWarn() {
        assertEquals(ExpiredCertPolicy.WARN, config(matchingName).build().expiredCertPolicy());
    }

    @Test
    public void testExpiredCertPolicy_UnknownValue_FailsNamingTheValidOnes() {
        try {
            ExpiredCertPolicy.from("nope");
            fail("an unknown policy value should have been refused");
        } catch (Exception e) {
            assertTrue("expected the valid values to be named but got: " + e,
                    String.valueOf(e.getMessage()).contains("OS_TLS_ON_EXPIRED_CERT")
                            && String.valueOf(e.getMessage()).contains("REJECT"));
        }
    }

    /**
     * Sending the signing CA alongside the server certificate is the recommended server
     * configuration, and an operator who turned OS_TLS_TRUST_SELF_SIGNED on has already accepted
     * trusting whatever that node presents. The chain must therefore be accepted when it anchors
     * itself: the last certificate is self-signed and every certificate is signed by the next one.
     */
    @Test
    public void testCertRequired_WithTrustSelfSigned_AcceptsSelfAnchoredChain() throws Exception {
        assertEquals("3.8.0",
                connect(config(chainFromPrivateCa).certRequired(true).trustSelfSigned(true).build()));
    }
}
