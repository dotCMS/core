package com.dotcms.auth.dotAuth.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dotcms.auth.providers.oauth.OAuthSsrfGuard;
import com.dotcms.util.network.IPUtils;
import com.dotmarketing.util.Config;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import javax.ws.rs.BadRequestException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

/**
 * Stub-server tests for {@link DotAuthResource#fetchValidated}, the redirect loop behind the
 * SAML-metadata and OIDC-discovery proxy. Automatic redirects are off there, so these pin the
 * loop itself: every hop is validated before it is requested, the body of the first 2xx is
 * returned, a non-2xx final response is refused, and at most five redirects are followed.
 *
 * <p>The stub listens on loopback, which {@link OAuthSsrfGuard} rightly treats as internal. The
 * guard is told that the literal {@value #STUB_HOST}, and nothing else, is public, so the stub
 * stands in for a public IdP while {@code localhost} keeps its real verdict and serves as the
 * internal redirect target. The connection-level private-subnet check in
 * {@code CircuitBreakerUrl} is off for the same reason, so the internal hop below is refused by
 * {@code fetchValidated} alone. Redirects are switched on in the configuration, so if
 * {@code fetchValidated} stopped turning them off for its own requests, the client would follow
 * that hop itself and the internal path would be requested.
 */
class DotAuthResourceFetchValidatedTest {

    private static final String STUB_HOST = "127.0.0.1";
    private static final String ACCEPT = "application/json";
    private static final int MAX_BYTES = 1024 * 1024;
    private static final Map<String, Boolean> FLAGS = Map.of(
            "OAUTH_ALLOW_INSECURE_URLS", true,     // the stub speaks plain http
            "OAUTH_ALLOW_INTERNAL_HOSTS", false,   // keep the guard's internal-host check on
            "REMOTE_CALL_ALLOW_REDIRECTS", true);  // fetchValidated must turn redirects off itself

    private final Map<String, Integer> hits = new ConcurrentHashMap<>();
    private HttpServer server;
    private String base;
    private String internalBase;
    private MockedStatic<Config> config;
    private MockedStatic<OAuthSsrfGuard> guard;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress(STUB_HOST, 0), 0);
        server.start();
        base = "http://" + STUB_HOST + ":" + server.getAddress().getPort();
        internalBase = "http://localhost:" + server.getAddress().getPort();

        config = Mockito.mockStatic(Config.class, invocation ->
                "getBooleanProperty".equals(invocation.getMethod().getName())
                        && invocation.getArguments().length == 2
                        && FLAGS.containsKey(invocation.getArgument(0))
                        ? FLAGS.get(invocation.getArgument(0))
                        : invocation.callRealMethod());
        guard = Mockito.mockStatic(OAuthSsrfGuard.class, invocation ->
                "isInternalHost".equals(invocation.getMethod().getName())
                        && STUB_HOST.equals(invocation.getArgument(0))
                        ? Boolean.FALSE
                        : invocation.callRealMethod());
        IPUtils.disabledIpPrivateSubnet(true);
    }

    @AfterEach
    void tearDown() {
        IPUtils.disabledIpPrivateSubnet(false);
        guard.close();
        config.close();
        server.stop(0);
    }

    @Test
    void followsRelativeAndAbsoluteRedirects_andReturnsTheFinalBody() throws IOException {
        final AtomicReference<String> finalAccept = new AtomicReference<>();
        route("/start", redirect(302, "/next"));
        route("/next", redirect(301, base + "/final"));
        route("/final", exchange -> {
            finalAccept.set(exchange.getRequestHeaders().getFirst("Accept"));
            respond(200, "{\"issuer\":\"stub\"}").handle(exchange);
        });

        assertEquals("{\"issuer\":\"stub\"}",
                DotAuthResource.fetchValidated(base + "/start", ACCEPT, MAX_BYTES));
        assertEquals(1, hits("/start"));
        assertEquals(1, hits("/next"));
        assertEquals(1, hits("/final"));
        assertEquals(ACCEPT, finalAccept.get());
    }

    @Test
    void redirectToAnInternalHost_partwayThroughTheChain_isRefusedBeforeItIsRequested() {
        route("/start", redirect(302, "/next"));
        route("/next", redirect(302, internalBase + "/internal"));
        route("/internal", respond(200, "internal-only"));

        final BadRequestException refused = assertThrows(BadRequestException.class,
                () -> DotAuthResource.fetchValidated(base + "/start", ACCEPT, MAX_BYTES));
        // the guard's verdict, not a failed fetch
        assertEquals("host resolves to an internal/private address", refused.getMessage());
        assertEquals(1, hits("/start"));
        assertEquals(1, hits("/next"));
        assertEquals(0, hits("/internal"));
    }

    @Test
    void nonSuccessFinalResponse_isRejected() {
        route("/start", redirect(302, "/missing"));
        route("/missing", respond(404, "not here"));
        route("/broken", respond(500, "boom"));

        final BadRequestException notFound = assertThrows(BadRequestException.class,
                () -> DotAuthResource.fetchValidated(base + "/start", ACCEPT, MAX_BYTES));
        assertTrue(notFound.getMessage().contains(base + "/missing"), notFound.getMessage());
        assertTrue(notFound.getMessage().contains("[404]"), notFound.getMessage());

        final BadRequestException serverError = assertThrows(BadRequestException.class,
                () -> DotAuthResource.fetchValidated(base + "/broken", ACCEPT, MAX_BYTES));
        assertTrue(serverError.getMessage().contains("[500]"), serverError.getMessage());
    }

    @Test
    void followsAtMostFiveRedirects() throws IOException {
        // /cap/{redirects}/{n} redirects to n + 1 until n reaches {redirects}, then answers 200
        route("/cap/", exchange -> {
            final String[] parts = exchange.getRequestURI().getPath().split("/");
            final int redirects = Integer.parseInt(parts[2]);
            final int n = Integer.parseInt(parts[3]);
            (n < redirects
                    ? redirect(302, "/cap/" + redirects + "/" + (n + 1))
                    : respond(200, "after " + n + " redirects")).handle(exchange);
        });

        assertEquals("after 5 redirects",
                DotAuthResource.fetchValidated(base + "/cap/5/0", ACCEPT, MAX_BYTES));

        final BadRequestException tooMany = assertThrows(BadRequestException.class,
                () -> DotAuthResource.fetchValidated(base + "/cap/6/0", ACCEPT, MAX_BYTES));
        assertEquals("Too many redirects", tooMany.getMessage());
        // the first request plus five redirects; the sixth redirect target is never requested
        for (int n = 0; n <= 5; n++) {
            assertEquals(1, hits("/cap/6/" + n));
        }
        assertEquals(0, hits("/cap/6/6"));
    }

    /** Serves {@code path} (and everything under it), counting each request by its exact path. */
    private void route(final String path, final HttpHandler handler) {
        server.createContext(path, exchange -> {
            hits.merge(exchange.getRequestURI().getPath(), 1, Integer::sum);
            handler.handle(exchange);
        });
    }

    private int hits(final String path) {
        return hits.getOrDefault(path, 0);
    }

    private static HttpHandler redirect(final int status, final String location) {
        return exchange -> {
            exchange.getResponseHeaders().add("Location", location);
            exchange.sendResponseHeaders(status, -1);
            exchange.close();
        };
    }

    private static HttpHandler respond(final int status, final String body) {
        return exchange -> {
            final byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(status, bytes.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(bytes);
            }
        };
    }
}
