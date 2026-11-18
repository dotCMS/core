package com.dotcms.rest.api.v1.temp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dotcms.http.CircuitBreakerPool;
import com.dotcms.util.network.IPUtils;
import com.dotmarketing.util.Config;
import com.liferay.portal.model.User;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * Unit tests for {@link TempFileAPI}: URL validation, the role gate for downloads by URL and the
 * download name policy. The network tests run against a loopback server; the address checks are
 * switched off only for the cases that need to reach it, and restored after each test.
 */
@Timeout(60)
class TempFileAPITest {

    private static final List<String> DEFAULTS = TempFileAPI.DEFAULT_URL_DOWNLOAD_EXTENSIONS;
    private static final int CAP = TempFileAPI.URL_PROBE_MAX_BYTES;

    private final List<HttpServer> servers = new ArrayList<>();
    private TempFileAPI api;

    @BeforeEach
    void setUp() {
        IPUtils.disabledIpPrivateSubnet(false);
        CircuitBreakerPool.flushPool();
        api = new TempFileAPI();
    }

    @AfterEach
    void tearDown() {
        IPUtils.disabledIpPrivateSubnet(false);
        CircuitBreakerPool.flushPool();
        for (final HttpServer server : servers) {
            server.stop(0);
        }
        servers.clear();
    }

    // ---- validUrl ----

    /**
     * The validation probe stops reading after the limit and still accepts the 200: the server
     * tries to send far more than the limit and gets cut off early.
     */
    @Test
    void testValidUrl_WhenBodyExceedsProbeLimit_ShouldStillReturnTrue() throws Exception {
        final AtomicLong written = new AtomicLong();
        final HttpServer server = localServer(200, 1024 * CAP, written);
        IPUtils.disabledIpPrivateSubnet(true);

        assertTrue(api.validUrl(url(server, "/large")));
        assertTrue(written.get() < 256L * CAP, "the probe must stop early, server wrote " + written.get());
    }

    /**
     * Only a 200 is valid, whatever the body length: short and long error pages are refused, a
     * short and an empty 200 body are accepted.
     */
    @Test
    void testValidUrl_WhenRemoteAnswersNon200_ShouldReturnFalse() throws Exception {
        final HttpServer ok = localServer(200, 16, new AtomicLong());
        final HttpServer empty = localServer(200, 0, new AtomicLong());
        final HttpServer notFound = localServer(404, 16, new AtomicLong());
        final HttpServer largeNotFound = localServer(404, 3 * CAP, new AtomicLong());
        final HttpServer largeError = localServer(500, 3 * CAP, new AtomicLong());
        IPUtils.disabledIpPrivateSubnet(true);

        assertTrue(api.validUrl(url(ok, "/small")));
        assertTrue(api.validUrl(url(empty, "/empty")));
        assertFalse(api.validUrl(url(notFound, "/missing")));
        assertFalse(api.validUrl(url(largeNotFound, "/missing-large")));
        assertFalse(api.validUrl(url(largeError, "/error-large")));
    }

    /**
     * Probing the same large resource many times keeps working: the cut-off probe does not count
     * against the resource.
     */
    @Test
    void testValidUrl_WhenProbedRepeatedly_ShouldKeepAccepting() throws Exception {
        final HttpServer server = localServer(200, 1024 * CAP, new AtomicLong());
        IPUtils.disabledIpPrivateSubnet(true);
        final String url = url(server, "/large-again");

        for (int i = 0; i < 8; i++) {
            assertTrue(api.validUrl(url), "probe " + i);
        }
    }

    /**
     * Only http and https URLs are valid, and a string that is not a URL is not.
     */
    @Test
    void testValidUrl_WhenSchemeIsNotHttp_ShouldReturnFalse() {
        assertFalse(api.validUrl("ftp://example.com/a.pdf"));
        assertFalse(api.validUrl("not a url"));
    }

    /**
     * With the address checks on, a loopback server is refused before any request reaches it.
     */
    @Test
    void testValidUrl_WhenHostIsInternal_ShouldReturnFalseWithoutContact() throws Exception {
        final AtomicInteger hits = new AtomicInteger();
        final HttpServer server = localServer(200, 16, new AtomicLong(), hits);

        assertFalse(api.validUrl(url(server, "/internal")));
        assertFalse(api.validUrl("http://localhost:" + server.getAddress().getPort() + "/internal"));
        assertEquals(0, hits.get());
    }

    /**
     * Internal literals are refused and a public literal is accepted, without any request.
     */
    @Test
    void testIsAllowedRemoteUrl_ShouldRefuseInternalTargets() throws Exception {
        for (final String refused : new String[]{"http://127.0.0.1/", "http://[::1]/", "http://169.254.169.254/latest/",
                "http://0.0.0.0/", "http://localhost/", "http://10.0.0.5/", "http://a@127.0.0.1/", "ftp://93.184.216.34/"}) {
            assertFalse(api.isAllowedRemoteUrl(new URL(refused)), refused);
        }
        assertFalse(api.isAllowedRemoteUrl(null));
        assertTrue(api.isAllowedRemoteUrl(new URL("http://93.184.216.34/")));
        assertTrue(api.isAllowedRemoteUrl(new URL("https://93.184.216.34:8443/x")));
    }

    // ---- isUrlDownloadPermitted ----

    @Test
    void testIsUrlDownloadPermitted_WhenAdminOnlyAndUserIsNotAdmin_ShouldReturnFalse() {
        assertFalse(TempFileAPI.isUrlDownloadPermitted(user(false, false), true));
        assertFalse(TempFileAPI.isUrlDownloadPermitted(user(false, true), true));
    }

    @Test
    void testIsUrlDownloadPermitted_WhenAdminOnlyAndUserIsAdmin_ShouldReturnTrue() {
        assertTrue(TempFileAPI.isUrlDownloadPermitted(user(true, false), true));
    }

    /**
     * With the setting off, every user the request admitted may download, the anonymous user
     * included: whether anonymous requests are admitted is decided by the temp resource's own
     * anonymous setting, not here.
     */
    @Test
    void testIsUrlDownloadPermitted_WhenAdminOnlyIsOff_ShouldReturnTrue() {
        assertTrue(TempFileAPI.isUrlDownloadPermitted(user(false, false), false));
        assertTrue(TempFileAPI.isUrlDownloadPermitted(user(true, false), false));
        assertTrue(TempFileAPI.isUrlDownloadPermitted(user(false, true), false));
    }

    @Test
    void testIsUrlDownloadPermitted_WhenNoUser_ShouldReturnFalse() {
        assertFalse(TempFileAPI.isUrlDownloadPermitted(null, true));
        assertFalse(TempFileAPI.isUrlDownloadPermitted(null, false));
    }

    /**
     * The instance method reads the setting from the configuration.
     */
    @Test
    void testIsUrlDownloadPermitted_ShouldReadTheSetting() {
        final String key = "TEMP_RESOURCE_BY_URL_ADMIN_ONLY";
        final boolean before = Config.getBooleanProperty(key, false);
        try {
            Config.setProperty(key, true);
            assertFalse(api.isUrlDownloadPermitted(user(false, false)));
            assertTrue(api.isUrlDownloadPermitted(user(true, false)));
            Config.setProperty(key, false);
            assertTrue(api.isUrlDownloadPermitted(user(false, false)));
            assertFalse(api.isUrlDownloadPermitted(null));
        } finally {
            Config.setProperty(key, before);
        }
    }

    // ---- isAllowedDownloadName ----

    /**
     * Names with an allowed extension are accepted, whatever the letter case, when the content
     * type agrees, says nothing, or is absent.
     */
    @Test
    void testIsAllowedDownloadName_WhenExtensionIsAllowed_ShouldReturnTrue() {
        assertTrue(TempFileAPI.isAllowedDownloadName("photo.PNG", "image/png", DEFAULTS));
        assertTrue(TempFileAPI.isAllowedDownloadName("photo.png", "IMAGE/PNG", DEFAULTS));
        assertTrue(TempFileAPI.isAllowedDownloadName("photo.png", "image/png, text/html", DEFAULTS));
        assertTrue(TempFileAPI.isAllowedDownloadName("doc.pdf", "application/pdf", DEFAULTS));
        assertTrue(TempFileAPI.isAllowedDownloadName("doc.pdf", "application/pdf; charset=binary", DEFAULTS));
        assertTrue(TempFileAPI.isAllowedDownloadName("doc.pdf", null, DEFAULTS));
        assertTrue(TempFileAPI.isAllowedDownloadName("doc.pdf", "unknown", DEFAULTS));
        assertTrue(TempFileAPI.isAllowedDownloadName("photo.jpg", "application/octet-stream", DEFAULTS));
        assertTrue(TempFileAPI.isAllowedDownloadName("logo.svg", "image/svg+xml", DEFAULTS));
        assertTrue(TempFileAPI.isAllowedDownloadName(".png", "image/png", DEFAULTS));
        assertTrue(TempFileAPI.isAllowedDownloadName("evil.php.png", "image/png", DEFAULTS));
    }

    /**
     * The default list is images, SVG and PDF.
     */
    @Test
    void testDefaultExtensions_ShouldBeImagesAndPdf() {
        assertEquals(List.of("jpg", "jpeg", "png", "gif", "webp", "bmp", "tif", "tiff", "svg", "pdf"), DEFAULTS);
    }

    /**
     * A name with no extension cannot be refused before the download, and is accepted afterwards
     * only when the content type names an allowed kind of file.
     */
    @Test
    void testIsAllowedDownloadName_WhenNameHasNoExtension_ShouldFollowTheContentType() {
        assertTrue(TempFileAPI.isAllowedDownloadName("noextension", "image/jpeg", DEFAULTS));
        assertTrue(TempFileAPI.isAllowedDownloadName("noextension", "application/pdf; charset=binary", DEFAULTS));
        assertTrue(TempFileAPI.isAllowedDownloadName("noextension", "image/svg+xml", DEFAULTS));
        assertFalse(TempFileAPI.isAllowedDownloadName("noextension", null, DEFAULTS));
        assertFalse(TempFileAPI.isAllowedDownloadName("noextension", "unknown", DEFAULTS));
        assertFalse(TempFileAPI.isAllowedDownloadName("noextension", "application/octet-stream", DEFAULTS));
        assertFalse(TempFileAPI.isAllowedDownloadName("noextension", "application/zip", DEFAULTS));
        assertFalse(TempFileAPI.isAllowedDownloadName("noextension", "text/html", DEFAULTS));
        assertFalse(TempFileAPI.isAllowedDownloadName("noextension", "image/", DEFAULTS));
        assertFalse(TempFileAPI.isAllowedDownloadName("trailing.", "application/zip", DEFAULTS));
    }

    /**
     * Names whose extension is not on the list, or that have none, are refused.
     */
    @Test
    void testIsAllowedDownloadName_WhenExtensionIsNotAllowed_ShouldReturnFalse() {
        for (final String name : new String[]{"script.js", "archive.zip", "noextension", "page.html", "", "trailing.", "photo.png.exe"}) {
            assertFalse(TempFileAPI.isAllowedDownloadName(name, null, DEFAULTS), name);
        }
        assertFalse(TempFileAPI.isAllowedDownloadName(null, null, DEFAULTS));
    }

    /**
     * A name on the list does not make a reply of another kind acceptable.
     */
    @Test
    void testIsAllowedDownloadName_WhenContentTypeDisagrees_ShouldReturnFalse() {
        assertFalse(TempFileAPI.isAllowedDownloadName("photo.png", "text/html", DEFAULTS));
        assertFalse(TempFileAPI.isAllowedDownloadName("photo.png", "text/html; charset=utf-8", DEFAULTS));
        assertFalse(TempFileAPI.isAllowedDownloadName("photo.png", "application/pdf", DEFAULTS));
        assertFalse(TempFileAPI.isAllowedDownloadName("doc.pdf", "image/png", DEFAULTS));
        assertFalse(TempFileAPI.isAllowedDownloadName("doc.pdf", "text/plain", DEFAULTS));
        assertFalse(TempFileAPI.isAllowedDownloadName("notes.md", "text/html", List.of("md")));
        assertFalse(TempFileAPI.isAllowedDownloadName("notes.md", "image/png", List.of("md")));
        assertFalse(TempFileAPI.isAllowedDownloadName("data.bin", "application/javascript", List.of("bin")));
    }

    /**
     * A configured list replaces the default one.
     */
    @Test
    void testIsAllowedDownloadName_WhenListIsConfigured_ShouldUseIt() {
        final List<String> onlyMarkdown = List.of("md");

        assertTrue(TempFileAPI.isAllowedDownloadName("notes.md", null, onlyMarkdown));
        assertTrue(TempFileAPI.isAllowedDownloadName("notes.md", "text/markdown", onlyMarkdown));
        assertFalse(TempFileAPI.isAllowedDownloadName("photo.png", null, onlyMarkdown));

        // an extension with no known content-type family is accepted with any type except a web page or script
        final List<String> archives = List.of("zip");
        assertTrue(TempFileAPI.isAllowedDownloadName("bundle.zip", "application/zip", archives));
        assertFalse(TempFileAPI.isAllowedDownloadName("bundle.zip", "text/html", archives));
    }

    /**
     * Before the download, only a name with a disallowed extension can be refused.
     */
    @Test
    void testIsAllowedDownloadName_BeforeTheDownload_ShouldRefuseOnlyDisallowedExtensions() {
        assertTrue(api.isAllowedDownloadName("doc.pdf"));
        assertTrue(api.isAllowedDownloadName("noextension"));
        assertFalse(api.isAllowedDownloadName("script.js"));
        assertFalse(api.isAllowedDownloadName(""));
        assertFalse(api.isAllowedDownloadName(null));
    }

    /**
     * Configured entries are trimmed, lower-cased and stripped of a leading dot; a list that
     * names no extension means the default.
     */
    @Test
    void testParseExtensions_ShouldNormaliseEntries() {
        assertEquals(List.of("pdf", "png"), TempFileAPI.parseExtensions(new String[]{" .PDF", "png ", "", "."}));
        assertEquals(DEFAULTS, TempFileAPI.parseExtensions(new String[]{" ", "."}));
        assertEquals(DEFAULTS, TempFileAPI.parseExtensions(new String[0]));
        assertEquals(DEFAULTS, TempFileAPI.parseExtensions(null));
    }

    // ---- resolveDownloadFileName ----

    /**
     * The stored name is the requested name's file part, else the URL path's last segment when
     * it carries an extension, else a random name with no extension.
     */
    @Test
    void testResolveDownloadFileName_ShouldKeepOnlyTheFileNamePart() throws Exception {
        final URL url = new URL("http://93.184.216.34/a/b.pdf?x=1");

        assertEquals("x.png", api.resolveDownloadFileName("../../etc/x.png", url));
        assertEquals("evil.html", api.resolveDownloadFileName("a/b/evil.html", url));
        assertEquals("b.pdf", api.resolveDownloadFileName(null, url));
        assertEquals("y.PNG", api.resolveDownloadFileName(null, new URL("http://93.184.216.34/x/y.PNG?w=1")));

        final String random = api.resolveDownloadFileName(null, new URL("http://93.184.216.34/avatar"));
        assertFalse(random.isEmpty());
        assertFalse(random.contains("."));
    }

    // ---- helpers ----

    private static User user(final boolean admin, final boolean anonymous) {
        final User user = mock(User.class);
        when(user.isAdmin()).thenReturn(admin);
        when(user.isAnonymousUser()).thenReturn(anonymous);
        return user;
    }

    private static String url(final HttpServer server, final String path) {
        return "http://127.0.0.1:" + server.getAddress().getPort() + path;
    }

    private HttpServer localServer(final int status, final long bodyLength, final AtomicLong written) throws IOException {
        return localServer(status, bodyLength, written, new AtomicInteger());
    }

    /**
     * A loopback server answering every request with the given status and a body of the given
     * length, counting the bytes it was able to write and the requests it received. A write that
     * fails because the client went away ends the response quietly.
     */
    private HttpServer localServer(final int status, final long bodyLength, final AtomicLong written,
                                   final AtomicInteger hits) throws IOException {
        final HttpServer server = HttpServer.create(
                new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/", exchange -> {
            hits.incrementAndGet();
            final byte[] chunk = new byte[8192];
            Arrays.fill(chunk, (byte) 'x');
            exchange.getResponseHeaders().add("Content-Type", "application/octet-stream");
            exchange.sendResponseHeaders(status, bodyLength == 0 ? -1 : bodyLength);
            try (OutputStream out = exchange.getResponseBody()) {
                long remaining = bodyLength;
                while (remaining > 0) {
                    final int n = (int) Math.min(chunk.length, remaining);
                    out.write(chunk, 0, n);
                    written.addAndGet(n);
                    remaining -= n;
                }
            } catch (final IOException closedByTheClient) {
                // the probe stops reading once it has seen enough
            }
        });
        server.start();
        servers.add(server);
        return server;
    }
}
