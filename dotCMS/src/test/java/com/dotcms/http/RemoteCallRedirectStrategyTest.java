package com.dotcms.http;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.ProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.protocol.BasicHttpContext;
import org.junit.Test;

/**
 * The redirect policy applied to outbound calls made for user-supplied URLs. Public targets
 * are given as address literals so no name resolution is needed.
 */
public class RemoteCallRedirectStrategyTest {

    private static final String PUBLIC_ORIGIN = "http://93.184.216.34/start";

    private static HttpResponse redirectTo(final String location) {
        final HttpResponse response = new BasicHttpResponse(HttpVersion.HTTP_1_1, 302, "Found");
        response.setHeader("Location", location);
        return response;
    }

    /**
     * A redirect to another public address is followed as before.
     */
    @Test
    public void redirectToPublicAddress_isFollowed() throws Exception {
        assertTrue(new RemoteCallRedirectStrategy().isRedirected(new HttpGet(PUBLIC_ORIGIN),
                redirectTo("http://93.184.216.35/next"), new BasicHttpContext()));
    }

    /**
     * A response that is not a redirect is reported as such, without any address check.
     */
    @Test
    public void plainResponse_isNotARedirect() throws Exception {
        final HttpResponse ok = new BasicHttpResponse(HttpVersion.HTTP_1_1, 200, "OK");

        assertFalse(new RemoteCallRedirectStrategy().isRedirected(new HttpGet(PUBLIC_ORIGIN), ok,
                new BasicHttpContext()));
    }

    /**
     * A redirect from a public address to a loopback, link-local or private one is refused.
     */
    @Test
    public void redirectToInternalAddress_isRefused() {
        for (final String internal : new String[] {"http://127.0.0.1:8080/x", "http://localhost:8080/x",
                "http://[::1]:8080/x", "http://169.254.169.254/latest/meta-data/", "http://10.0.0.5/x"}) {
            try {
                new RemoteCallRedirectStrategy().isRedirected(new HttpGet(PUBLIC_ORIGIN),
                        redirectTo(internal), new BasicHttpContext());
                fail("redirect to " + internal + " must be refused");
            } catch (final ProtocolException expected) {
                assertTrue(expected.getMessage(), expected.getMessage().contains("refused"));
            }
        }
    }
}
