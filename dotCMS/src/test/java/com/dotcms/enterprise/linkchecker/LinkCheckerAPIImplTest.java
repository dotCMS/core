package com.dotcms.enterprise.linkchecker;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.dotcms.UnitTestBase;
import com.dotcms.repackage.org.apache.commons.httpclient.HttpClient;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for {@link LinkCheckerAPIImpl#getExternalLinkStatus}.
 */
public class LinkCheckerAPIImplTest extends UnitTestBase {

    /** Only the "localhost" name is allowed; any other host (e.g. 127.0.0.1) is not. */
    private static final Predicate<String> ONLY_LOCALHOST_NAME = "localhost"::equals;

    /** Every resolved address is allowed, so only the host name check applies. */
    private static final Predicate<InetAddress> ANY_ADDRESS = address -> true;

    private HttpServer server;
    private int port;
    private final List<String> requestedPaths = new CopyOnWriteArrayList<>();

    @Before
    public void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        port = server.getAddress().getPort();
        server.createContext("/", exchange -> {
            final String path = exchange.getRequestURI().getPath();
            requestedPaths.add(path);
            switch (path) {
                case "/ok":
                    exchange.sendResponseHeaders(200, -1);
                    break;
                case "/redirect-same-host":
                    exchange.getResponseHeaders().add("Location", "/ok");
                    exchange.sendResponseHeaders(302, -1);
                    break;
                case "/redirect-other-host":
                    exchange.getResponseHeaders().add("Location", "http://127.0.0.1:" + port + "/target");
                    exchange.sendResponseHeaders(302, -1);
                    break;
                case "/redirect-other-scheme":
                    exchange.getResponseHeaders().add("Location", "file:///etc/hosts");
                    exchange.sendResponseHeaders(301, -1);
                    break;
                case "/loop":
                    exchange.getResponseHeaders().add("Location", "/loop");
                    exchange.sendResponseHeaders(302, -1);
                    break;
                default:
                    exchange.sendResponseHeaders(404, -1);
            }
            exchange.close();
        });
        server.start();
    }

    @After
    public void stopServer() {
        server.stop(0);
    }

    private Optional<Integer> check(final String host, final String path) {
        return LinkCheckerAPIImpl.getExternalLinkStatus(new HttpClient(),
                "http://" + host + ":" + port + path, null, ONLY_LOCALHOST_NAME, ANY_ADDRESS);
    }

    /**
     * When: the link points to a host that is not allowed
     * Should: return empty without sending any request
     */
    @Test
    public void notAllowedHost_isNotRequested() {
        assertFalse(check("127.0.0.1", "/ok").isPresent());
        assertTrue(requestedPaths.isEmpty());
    }

    /**
     * When: the link points to an allowed host
     * Should: return the status code of the response
     */
    @Test
    public void allowedHost_returnsStatus() {
        assertEquals(Optional.of(200), check("localhost", "/ok"));
        assertEquals(Optional.of(404), check("localhost", "/missing"));
    }

    /**
     * When: an allowed host redirects to another path of the same host
     * Should: follow the redirect and return the final status
     */
    @Test
    public void redirectToAllowedHost_isFollowed() {
        assertEquals(Optional.of(200), check("localhost", "/redirect-same-host"));
        assertEquals(List.of("/redirect-same-host", "/ok"), requestedPaths);
    }

    /**
     * When: an allowed host redirects to a host that is not allowed
     * Should: return empty without requesting the redirect location
     */
    @Test
    public void redirectToNotAllowedHost_isNotFollowed() {
        assertFalse(check("localhost", "/redirect-other-host").isPresent());
        assertEquals(List.of("/redirect-other-host"), requestedPaths);
    }

    /**
     * When: an allowed host redirects to a non HTTP location
     * Should: return empty
     */
    @Test
    public void redirectToOtherScheme_isNotFollowed() {
        assertFalse(check("localhost", "/redirect-other-scheme").isPresent());
    }

    /**
     * When: redirects never end
     * Should: stop after the maximum number of redirects and report a failure
     */
    @Test
    public void redirectLoop_stops() {
        assertEquals(Optional.of(-1), check("localhost", "/loop"));
        assertEquals(6, requestedPaths.size());
    }

    /**
     * When: the default host check is used
     * Should: reject the configured private subnets, e.g. the cloud metadata address
     */
    @Test
    public void defaultHostCheck_rejectsPrivateAddresses() {
        assertFalse(LinkCheckerAPIImpl.isHostAllowed("169.254.169.254"));
        assertFalse(LinkCheckerAPIImpl.isHostAllowed("127.0.0.1"));
        assertFalse(LinkCheckerAPIImpl.isHostAllowed("10.1.2.3"));
    }
    /**
     * When: the host of the link cannot be resolved
     * Should: report it as broken without checking whether the host is allowed
     */
    @Test
    public void unresolvableHost_isBroken() {
        final List<String> checkedHosts = new CopyOnWriteArrayList<>();
        final Optional<Integer> status = LinkCheckerAPIImpl.getExternalLinkStatus(new HttpClient(),
                "http://does-not-exist.invalid/page", null, host -> checkedHosts.add(host) && false,
                ANY_ADDRESS);
        assertEquals(Optional.of(-1), status);
        assertTrue(checkedHosts.isEmpty());
    }

    /**
     * When: the host name is allowed but, when the connection is opened, it resolves to an
     * address that is not allowed (as when the name's DNS answer changes after the first check)
     * Should: return empty without connecting, after checking the resolved address
     */
    @Test
    public void allowedHostResolvingToDisallowedAddress_isNotRequested() {
        final List<InetAddress> checkedAddresses = new CopyOnWriteArrayList<>();
        final Optional<Integer> status = LinkCheckerAPIImpl.getExternalLinkStatus(new HttpClient(),
                "http://localhost:" + port + "/ok", null, ONLY_LOCALHOST_NAME,
                address -> checkedAddresses.add(address) && false);

        assertFalse(status.isPresent());
        assertFalse(checkedAddresses.isEmpty());
        assertTrue(requestedPaths.isEmpty());
    }

    /**
     * When: the default address check is used
     * Should: reject loopback and the cloud metadata address, and accept a public address
     */
    @Test
    public void defaultAddressCheck_rejectsPrivateAddresses() throws Exception {
        assertFalse(LinkCheckerAPIImpl.isAddressAllowed(InetAddress.getLoopbackAddress()));
        assertFalse(LinkCheckerAPIImpl.isAddressAllowed(InetAddress.getByName("169.254.169.254")));
        assertTrue(LinkCheckerAPIImpl.isAddressAllowed(InetAddress.getByName("8.8.8.8")));
    }
}
