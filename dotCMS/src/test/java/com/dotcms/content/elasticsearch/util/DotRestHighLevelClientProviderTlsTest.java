package com.dotcms.content.elasticsearch.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.dotcms.content.index.TlsTestServer;
import com.dotmarketing.util.Config;
import javax.net.ssl.SSLException;
import org.elasticsearch.client.Request;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Connection tests for the TLS settings of {@link DotRestHighLevelClientProvider}. Each case makes a
 * real handshake with a local HTTPS server that presents a self-signed certificate.
 */
public class DotRestHighLevelClientProviderTlsTest {

    private static TlsTestServer server;

    @BeforeClass
    public static void startServer() throws Exception {
        server = TlsTestServer.start("other.example", "dns:other.example", "{\"tagline\":\"test\"}");
    }

    @AfterClass
    public static void stopServer() throws Exception {
        server.close();
    }

    @Before
    public void pointClientAtServer() {
        Config.setProperty("ES_ENDPOINTS", "https://127.0.0.1:" + server.port());
        Config.setProperty("ES_AUTH_TYPE", "NONE");
    }

    @After
    public void restoreConfig() {
        Config.setProperty("ES_ENDPOINTS", null);
        Config.setProperty("ES_AUTH_TYPE", null);
        Config.setProperty("ES_TLS_CERT_REQUIRED", null);
    }

    private static int status() throws Exception {
        return new DotRestHighLevelClientProvider().getClient().getLowLevelClient()
                .performRequest(new Request("GET", "/")).getStatusLine().getStatusCode();
    }

    @Test
    public void testDefaultConfig_RefusesUntrustedCertificate() {
        try {
            status();
            fail("the connection should have been refused");
        } catch (Exception e) {
            boolean rejected = false;
            for (Throwable t = e; t != null; t = t.getCause()) {
                rejected |= t instanceof SSLException;
            }
            assertTrue("expected a certificate rejection but got: " + e, rejected);
        }
    }

    @Test
    public void testCertRequiredFalse_AcceptsUntrustedCertificate() throws Exception {
        Config.setProperty("ES_TLS_CERT_REQUIRED", "false");
        assertEquals(200, status());
    }
}
