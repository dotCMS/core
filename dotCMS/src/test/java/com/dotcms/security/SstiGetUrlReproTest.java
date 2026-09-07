package com.dotcms.security;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.dotmarketing.util.UtilMethods;
import com.sun.net.httpserver.HttpServer;
import java.io.File;
import java.io.FileWriter;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.velocity.util.introspection.SecureIntrospectorImpl;
import org.junit.Test;

/**
 * Regression guard for the SSTI → arbitrary-file-read + SSRF finding via
 * $UtilMethods.getURL() (pentest F1 / dotCMS/private-issues#668).
 *
 * $UtilMethods stays in the Velocity context and the SecureIntrospector denylist still permits
 * the getURL *method* (used by ~24 templates for other helpers, and the introspector is
 * class-based), so the defense lives inside getURL: it must refuse non-http(s) schemes and
 * non-routable hosts. This test fails if that guard is removed and the primitive returns.
 */
public class SstiGetUrlReproTest {

    private static final String[] BAD_CLASSES = {
            "java.lang.Class", "java.lang.ClassLoader", "java.lang.Runtime", "java.lang.Process",
            "java.lang.System", "java.lang.Thread", "java.net.Socket",
            "org.apache.velocity.app.VelocityEngine"
    };
    private static final String[] BAD_PACKAGES = { "java.lang.reflect" };

    @Test
    public void getUrl_is_hardened_against_file_read_and_ssrf() throws Exception {

        // ---- The sandbox is real: it blocks dangerous classes but is class-based, so it still
        // permits the getURL method. That is exactly why getURL itself must self-defend. ----
        final SecureIntrospectorImpl sandbox = new SecureIntrospectorImpl(BAD_CLASSES, BAD_PACKAGES);
        assertTrue("sandbox should block Runtime.exec",
                !sandbox.checkObjectExecutePermission(Runtime.class, "exec"));
        assertTrue("sandbox is class-based and still exposes getURL — the guard must be in getURL",
                sandbox.checkObjectExecutePermission(UtilMethods.class, "getURL"));

        // ---- (1) file:// arbitrary read must now be blocked (returns empty, no file contents) ----
        final File secret = File.createTempFile("dotcms-ssti-secret", ".txt");
        secret.deleteOnExit();
        final String marker = "DOT_INITIAL_ADMIN_PASSWORD=CANARY-" + System.nanoTime();
        try (FileWriter fw = new FileWriter(secret)) { fw.write(marker); }
        final String fileRead = String.valueOf(UtilMethods.getURL(secret.toURI().toString())).trim();
        System.out.println("[LFI]  file:// read now returns: '" + fileRead + "'");
        assertFalse("REGRESSION: file:// read leaked file contents", fileRead.contains(marker));
        assertTrue("REGRESSION: file:// read returned non-empty content", fileRead.isEmpty());

        // ---- (2) SSRF to a loopback/internal host must now be blocked (no request, empty body) ----
        final HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        final AtomicReference<String> hitMethod = new AtomicReference<>(null);
        final String canary = "SSRF-CANARY-" + System.nanoTime();
        server.createContext("/oob", ex -> {
            hitMethod.set(ex.getRequestMethod());
            final byte[] body = canary.getBytes(StandardCharsets.UTF_8);
            ex.sendResponseHeaders(200, body.length);
            ex.getResponseBody().write(body);
            ex.close();
        });
        server.start();
        try {
            final int port = server.getAddress().getPort();
            final String ssrf = String.valueOf(
                    UtilMethods.getURL("http://127.0.0.1:" + port + "/oob")).trim();
            System.out.println("[SSRF] loopback request hit method: " + hitMethod.get()
                    + " | body returned: '" + ssrf + "'");
            assertNull("REGRESSION: SSRF request reached the loopback listener", hitMethod.get());
            assertFalse("REGRESSION: SSRF returned the internal response body", ssrf.contains(canary));
            assertTrue("REGRESSION: SSRF returned non-empty content", ssrf.isEmpty());
        } finally {
            server.stop(0);
        }

        // ---- (3) other non-routable targets must also be refused (empty), per review ----
        for (final String url : new String[]{
                "http://[::1]:9999/x",         // IPv6 loopback
                "http://100.64.0.1/x",         // IPv4 CGNAT (RFC 6598)
                "http://[fd00::1]/x",          // IPv6 unique-local (ULA)
                "http://0.0.0.0/x",            // any-local
                "http://169.254.170.2/x"}) {   // ECS credentials endpoint (link-local)
            final String out = String.valueOf(UtilMethods.getURL(url)).trim();
            System.out.println("[BLOCK] " + url + " -> '" + out + "'");
            assertTrue("REGRESSION: " + url + " was not blocked", out.isEmpty());
        }

        System.out.println("\n==== getURL hardened: file:// blocked; loopback / IPv6 / CGNAT / ULA / "
                + "link-local SSRF all blocked (F1 fixed) ====");
    }
}
