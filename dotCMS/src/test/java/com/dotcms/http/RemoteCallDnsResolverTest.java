package com.dotcms.http;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import com.dotcms.util.network.IPUtils;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.Test;

/**
 * The resolver the outbound client connects with. Literal addresses are used so no real name
 * resolution is needed; the client tests run a real {@link HttpClientBuilder} client against a
 * local server to show the refusal happens at connection time.
 */
public class RemoteCallDnsResolverTest {

    /**
     * A public address resolves as usual.
     */
    @Test
    public void publicAddress_isResolved() throws Exception {
        final InetAddress[] addresses = new RemoteCallDnsResolver().resolve("93.184.216.34");

        assertEquals(1, addresses.length);
        assertEquals("93.184.216.34", addresses[0].getHostAddress());
    }

    /**
     * A loopback, link-local or private address is reported as unknown.
     */
    @Test
    public void internalAddress_isRefused() {
        for (final String internal : new String[] {"127.0.0.1", "localhost", "::1",
                "169.254.169.254", "10.0.0.5"}) {
            try {
                new RemoteCallDnsResolver().resolve(internal);
                fail(internal + " must be refused");
            } catch (final UnknownHostException expected) {
                assertEquals(true, expected.getMessage().contains("refused"));
            }
        }
    }

    /**
     * Driven by a real client, a request to a loopback server fails before any connection is
     * made: the server records no hit.
     */
    @Test
    public void realClient_internalTarget_neverConnects() throws Exception {
        final AtomicInteger hits = new AtomicInteger();
        final HttpServer server = localServer(hits);
        try (final CloseableHttpClient client = HttpClientBuilder.create()
                .setDnsResolver(new RemoteCallDnsResolver()).build()) {

            client.execute(new HttpGet(url(server))).close();
            fail("the loopback target must be refused");
        } catch (final IOException expected) {
            assertEquals(0, hits.get());
        } finally {
            server.stop(0);
        }
    }

    /**
     * With the address check switched off, the same client reaches the loopback server, so
     * the resolver itself does not get in the way of allowed targets.
     */
    @Test
    public void realClient_checkDisabled_connects() throws Exception {
        final AtomicInteger hits = new AtomicInteger();
        final HttpServer server = localServer(hits);
        IPUtils.disabledIpPrivateSubnet(true);
        try (final CloseableHttpClient client = HttpClientBuilder.create()
                .setDnsResolver(new RemoteCallDnsResolver()).build();
                final CloseableHttpResponse response = client.execute(new HttpGet(url(server)))) {

            assertEquals(200, response.getStatusLine().getStatusCode());
            assertEquals(1, hits.get());
        } finally {
            IPUtils.disabledIpPrivateSubnet(false);
            server.stop(0);
        }
    }

    private static String url(final HttpServer server) {
        return "http://127.0.0.1:" + server.getAddress().getPort() + "/x";
    }

    private static HttpServer localServer(final AtomicInteger hits) throws Exception {
        final HttpServer server = HttpServer.create(
                new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/", exchange -> {
            hits.incrementAndGet();
            final byte[] body = "ok".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(body);
            }
        });
        server.start();
        return server;
    }
}
