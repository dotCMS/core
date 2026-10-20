package com.dotcms.http;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.dotcms.util.network.IPUtils;
import com.sun.net.httpserver.HttpServer;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.ProtocolException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.protocol.BasicHttpContext;
import org.junit.Test;

/**
 * The redirect policy applied to outbound calls made for user-supplied URLs. The strategy
 * tests use address literals so no name resolution is needed; the client tests run a real
 * {@link HttpClientBuilder} client against a local server so the strategy is exercised the
 * way the client drives it, including its circular-redirect bookkeeping.
 */
public class RemoteCallRedirectStrategyTest {

    private static final String PUBLIC_ORIGIN = "http://93.184.216.34/start";

    private static HttpResponse redirectTo(final String location) {
        final HttpResponse response = new BasicHttpResponse(HttpVersion.HTTP_1_1, 302, "Found");
        response.setHeader("Location", location);
        return response;
    }

    /**
     * A redirect to another public address resolves to that address.
     */
    @Test
    public void redirectToPublicAddress_isFollowed() throws Exception {
        final HttpResponse response = redirectTo("http://93.184.216.35/next");

        assertTrue(new RemoteCallRedirectStrategy().isRedirected(new HttpGet(PUBLIC_ORIGIN), response,
                new BasicHttpContext()));
        assertEquals("http://93.184.216.35/next", new RemoteCallRedirectStrategy()
                .getLocationURI(new HttpGet(PUBLIC_ORIGIN), response, new BasicHttpContext()).toString());
    }

    /**
     * A redirect from a public address to a loopback, link-local or private one is refused.
     */
    @Test
    public void redirectToInternalAddress_isRefused() {
        for (final String internal : new String[] {"http://127.0.0.1:8080/x", "http://localhost:8080/x",
                "http://[::1]:8080/x", "http://169.254.169.254/latest/meta-data/", "http://10.0.0.5/x"}) {
            try {
                new RemoteCallRedirectStrategy().getLocationURI(new HttpGet(PUBLIC_ORIGIN),
                        redirectTo(internal), new BasicHttpContext());
                fail("redirect to " + internal + " must be refused");
            } catch (final ProtocolException expected) {
                assertTrue(expected.getMessage(), expected.getMessage().contains("refused"));
            }
        }
    }

    /**
     * Driven by a real client, an allowed redirect is followed to its end without tripping the
     * client's circular-redirect detection. The address check is switched off for this case
     * because the local server is on the loopback interface.
     */
    @Test
    public void realClient_allowedRedirect_isFollowed() throws Exception {
        final AtomicInteger finalHits = new AtomicInteger();
        final HttpServer server = redirectingServer(finalHits);
        IPUtils.disabledIpPrivateSubnet(true);
        try (final CloseableHttpClient client = HttpClientBuilder.create()
                .setRedirectStrategy(new RemoteCallRedirectStrategy()).build();
                final CloseableHttpResponse response = client.execute(new HttpGet(url(server, "/start")))) {

            assertEquals(200, response.getStatusLine().getStatusCode());
            assertEquals(1, finalHits.get());
        } finally {
            IPUtils.disabledIpPrivateSubnet(false);
            server.stop(0);
        }
    }

    /**
     * Driven by a real client, a redirect to an internal address ends the call before the
     * redirected request is sent.
     */
    @Test
    public void realClient_internalRedirect_endsTheCall() throws Exception {
        final AtomicInteger finalHits = new AtomicInteger();
        final HttpServer server = redirectingServer(finalHits);
        try (final CloseableHttpClient client = HttpClientBuilder.create()
                .setRedirectStrategy(new RemoteCallRedirectStrategy()).build()) {

            client.execute(new HttpGet(url(server, "/start"))).close();
            fail("the redirect to the loopback address must end the call");
        } catch (final ClientProtocolException expected) {
            assertEquals(0, finalHits.get());
        } finally {
            server.stop(0);
        }
    }

    private static String url(final HttpServer server, final String path) {
        return "http://127.0.0.1:" + server.getAddress().getPort() + path;
    }

    /**
     * A loopback server whose {@code /start} answers 302 to its own {@code /final}, which
     * answers 200 and counts the hits.
     */
    private static HttpServer redirectingServer(final AtomicInteger finalHits) throws Exception {
        final HttpServer server = HttpServer.create(
                new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/start", exchange -> {
            exchange.getResponseHeaders().add("Location", url(server, "/final"));
            exchange.sendResponseHeaders(302, -1);
            exchange.close();
        });
        server.createContext("/final", exchange -> {
            finalHits.incrementAndGet();
            final byte[] body = "done".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(body);
            }
        });
        server.start();
        return server;
    }
}
