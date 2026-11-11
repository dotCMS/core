package com.dotcms.http;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.dotcms.util.network.IPUtils;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpServer;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.ProtocolException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpCoreContext;
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

    /**
     * Through {@link CircuitBreakerUrl} with redirects on, a redirect to another host drops the
     * credential headers and keeps the others, which shows that the client it builds follows
     * redirects through this strategy. The address check is switched off because the local
     * server is on the loopback interface; {@code 127.0.0.1} and {@code localhost} are
     * different hosts to the header rule.
     */
    @Test
    public void circuitBreakerUrl_redirectToAnotherHost_dropsCredentials() throws Exception {
        final AtomicReference<Headers> received = new AtomicReference<>();
        final HttpServer server = HttpServer.create(
                new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/start", exchange -> {
            exchange.getResponseHeaders().add("Location",
                    "http://localhost:" + server.getAddress().getPort() + "/final");
            exchange.sendResponseHeaders(302, -1);
            exchange.close();
        });
        server.createContext("/final", exchange -> {
            received.set(exchange.getRequestHeaders());
            final byte[] body = "done".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(body);
            }
        });
        server.start();
        IPUtils.disabledIpPrivateSubnet(true);
        try {
            final String body = CircuitBreakerUrl.builder()
                    .setUrl(url(server, "/start"))
                    .setHeaders(Map.of("Authorization", "Bearer token", "Accept", "application/json"))
                    .setAllowRedirects(true)
                    .build()
                    .doString();

            assertEquals("done", body);
            assertNull(received.get().getFirst("Authorization"));
            assertEquals("application/json", received.get().getFirst("Accept"));
        } finally {
            IPUtils.disabledIpPrivateSubnet(false);
            server.stop(0);
        }
    }

    @Test
    public void redirects_to_internal_addresses_are_refused() {
        for (final String target : new String[]{
                "http://127.0.0.1:8080/", "http://169.254.169.254/latest/meta-data/",
                "https://10.0.0.1/", "http://[::1]/", "http://192.168.1.10/"}) {
            final URI location = URI.create(target);
            assertThrows(target, ProtocolException.class,
                    () -> RemoteCallRedirectStrategy.checkRedirectTarget(location, false, location.getScheme()));
        }
    }

    @Test
    public void redirects_to_non_http_schemes_are_refused() {
        assertThrows(ProtocolException.class,
                () -> RemoteCallRedirectStrategy.checkRedirectTarget(URI.create("file:///etc/passwd"), true, "https"));
    }

    @Test
    public void internal_redirects_are_allowed_only_when_private_subnets_are_allowed() throws Exception {
        RemoteCallRedirectStrategy.checkRedirectTarget(URI.create("http://10.0.0.1/"), true, "http");
    }

    @Test
    public void redirects_to_public_addresses_are_allowed() throws Exception {
        RemoteCallRedirectStrategy.checkRedirectTarget(URI.create("https://8.8.8.8/"), false, "https");
    }

    @Test
    public void https_redirects_to_http_are_refused() {
        assertThrows(ProtocolException.class,
                () -> RemoteCallRedirectStrategy.checkRedirectTarget(URI.create("http://8.8.8.8/"), false, "https"));
    }

    @Test
    public void http_redirects_to_public_http_are_allowed() throws Exception {
        RemoteCallRedirectStrategy.checkRedirectTarget(URI.create("http://8.8.8.8/"), false, "http");
    }

    @Test
    public void redirect_to_same_host_and_port_keeps_all_headers() throws Exception {
        final HttpUriRequest redirect = redirectFrom(
                getWithCredentials("https://8.8.8.8/start"), originContext("https", "8.8.8.8", 443),
                "https://8.8.8.8/next");

        assertEquals("https://8.8.8.8/next", redirect.getURI().toString());
        assertEquals("Bearer token", redirect.getFirstHeader("Authorization").getValue());
        assertEquals("session=abc", redirect.getFirstHeader("Cookie").getValue());
        assertEquals("Basic cHJveHk=", redirect.getFirstHeader("Proxy-Authorization").getValue());
        assertEquals("application/json", redirect.getFirstHeader("Accept").getValue());
    }

    @Test
    public void redirect_to_explicit_default_port_is_the_same_origin() throws Exception {
        final HttpUriRequest redirect = redirectFrom(
                getWithCredentials("https://8.8.8.8/start"), originContext("https", "8.8.8.8", -1),
                "https://8.8.8.8:443/next");

        assertEquals("Bearer token", redirect.getFirstHeader("Authorization").getValue());
    }

    @Test
    public void redirect_to_another_host_drops_credentials_and_keeps_other_headers() throws Exception {
        final HttpUriRequest redirect = redirectFrom(
                getWithCredentials("https://8.8.8.8/start"), originContext("https", "8.8.8.8", 443),
                "https://1.1.1.1/next");

        assertCredentialsRemoved(redirect);
        assertEquals("application/json", redirect.getFirstHeader("Accept").getValue());
    }

    @Test
    public void redirect_to_another_port_on_the_same_host_drops_credentials() throws Exception {
        final HttpUriRequest redirect = redirectFrom(
                getWithCredentials("https://8.8.8.8/start"), originContext("https", "8.8.8.8", 443),
                "https://8.8.8.8:8443/next");

        assertCredentialsRemoved(redirect);
    }

    @Test
    public void relative_redirect_resolves_against_the_origin_and_keeps_headers() throws Exception {
        final HttpUriRequest redirect = redirectFrom(
                getWithCredentials("https://8.8.8.8/start"), originContext("https", "8.8.8.8", 443), "/next");

        assertEquals("8.8.8.8", redirect.getURI().getHost());
        assertEquals("/next", redirect.getURI().getPath());
        assertEquals("Bearer token", redirect.getFirstHeader("Authorization").getValue());
    }

    @Test
    public void cross_host_redirect_of_a_credentials_only_request_is_refused() {
        final HttpGet request = new HttpGet("https://8.8.8.8/start");
        request.addHeader("Authorization", "Bearer token");

        assertThrows(ProtocolException.class, () -> redirectFrom(
                request, originContext("https", "8.8.8.8", 443), "https://1.1.1.1/next"));
    }

    @Test
    public void host_names_are_compared_ignoring_case() {
        assertTrue(RemoteCallRedirectStrategy.sameOrigin(
                new HttpHost("Example.COM", -1, "https"), new HttpHost("example.com", 443, "https")));
        assertFalse(RemoteCallRedirectStrategy.sameOrigin(
                new HttpHost("example.com", -1, "https"), new HttpHost("example.org", -1, "https")));
        assertFalse(RemoteCallRedirectStrategy.sameOrigin(null, new HttpHost("example.com", -1, "https")));
    }

    private static HttpGet getWithCredentials(final String url) {
        final HttpGet request = new HttpGet(url);
        request.addHeader("Authorization", "Bearer token");
        request.addHeader("Cookie", "session=abc");
        request.addHeader("Proxy-Authorization", "Basic cHJveHk=");
        request.addHeader("Accept", "application/json");
        return request;
    }

    private static HttpCoreContext originContext(final String scheme, final String host, final int port) {
        final HttpCoreContext context = HttpCoreContext.create();
        context.setTargetHost(new HttpHost(host, port, scheme));
        return context;
    }

    private static HttpUriRequest redirectFrom(final HttpGet request, final HttpCoreContext context,
                                               final String location) throws ProtocolException {
        return new RemoteCallRedirectStrategy().getRedirect(request, redirectTo(location), context);
    }

    private static void assertCredentialsRemoved(final HttpUriRequest redirect) {
        for (final String name : new String[]{"Authorization", "Cookie", "Proxy-Authorization"}) {
            final Header header = redirect.getFirstHeader(name);
            assertNull(name + " must not follow a redirect to another origin", header);
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
