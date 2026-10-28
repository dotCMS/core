package com.dotcms.util;

import org.junit.Test;

import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link RedirectUtil}: the same-host post-login redirect check and the HTML redirect page
 * (issue #36591).
 *
 * The check decides whether a SAML RelayState (or the session's saved destination) may be followed after
 * login. Only a relative path, or an http(s) URL on the request's own host, is accepted; the result is
 * always a relative path with its dot segments resolved, so the back-end role check sees exactly the path
 * the browser will load.
 */
public class RedirectUtilTest {

    private static final String HOST = "site.example.com";

    /**
     * Method to test: {@link RedirectUtil#toSameHostRedirect(String, String)}
     * Given Scenario: same-host destinations, relative or absolute, including dot segments that stay at or
     * below the root
     * ExpectedResult: each one is accepted and comes back as a normalized relative path, with non-dot
     * segments, the query and the fragment left exactly as given
     */
    @Test
    public void toSameHostRedirect_acceptsSameHostDestinations() {

        final String[][] cases = {
                // candidate,                                          expected
                {"/members/report?id=42",                              "/members/report?id=42"},
                {"https://site.example.com/members/report?id=42",      "/members/report?id=42"},
                {"HTTPS://SITE.EXAMPLE.COM:8443/a",                    "/a"},
                {"http://site.example.com/a",                          "/a"},
                {"/dotAdmin/../members/report",                        "/members/report"},
                {"/members/../dotAdmin/",                              "/dotAdmin/"},
                {"/a/./b/",                                            "/a/b/"},
                {"/a/%2E%2E/b",                                        "/b"},
                {"/a/.%2e/b",                                          "/b"},
                {"/a%20b/c?q=x#frag",                                  "/a%20b/c?q=x#frag"},
                {"/dotAdmin/?contentType=Blog:detail",                 "/dotAdmin/?contentType=Blog:detail"},
                {"/",                                                  "/"},
                {"/a/..",                                              "/"},
        };

        for (final String[] testCase : cases) {
            assertEquals("candidate: " + testCase[0], Optional.of(testCase[1]),
                    RedirectUtil.toSameHostRedirect(testCase[0], HOST));
        }
    }

    /**
     * Method to test: {@link RedirectUtil#toSameHostRedirect(String, String)}
     * Given Scenario: every open-redirect, traversal, injection and non-URL shape listed in the spec (AC-003)
     * ExpectedResult: each one is rejected with an empty result and the method never throws
     */
    @Test
    public void toSameHostRedirect_rejectsUnsafeDestinations() {

        final String[] rejected = {
                null,
                "",
                "   ",
                // dot segments climbing above the root, plain and percent-encoded
                "/page1/../../other-path",
                "/%2e%2e/%2e%2e/other-path",
                "/..",
                // protocol-relative and backslash tricks, plain and encoded
                "//evil.com",
                "/\\evil.com",
                "/%5Cevil.com",
                "/%2F%2Fevil.com",
                // dot segments that collapse into a protocol-relative URL
                "/a/..//evil.com",
                // control and whitespace characters, plain and encoded
                "/\t//evil.com",
                "/%09//evil.com",
                "/%0Aevil",
                "/%22quoted",
                "/x\r\nSet-Cookie:a=1",
                "/path\u007f",
                "/a b",
                // other hosts, look-alike hosts, user-info, other schemes
                "https://evil.com/x",
                "https://site.example.com.evil.com/",
                "http://evil.com@site.example.com/",
                "http://user@site.example.com/a",
                "ftp://site.example.com/a",
                "javascript:alert(1)",
                "data:text/html,x",
                // not a path at all
                "companyCode=abc",
                "members/report",
                // characters that could break out of the HTML redirect page
                "/x'",
                "/x\"><script>",
                "/x<y",
                "/x`y",
                // malformed percent-encoding
                "/bad%zz",
                "/bad%2",
        };

        for (final String candidate : rejected) {
            try {
                assertEquals("candidate: " + candidate, Optional.empty(),
                        RedirectUtil.toSameHostRedirect(candidate, HOST));
            } catch (final RuntimeException e) {
                fail("candidate must be rejected without throwing: " + candidate + " -> " + e);
            }
        }
    }

    /**
     * Method to test: {@link RedirectUtil#rejectionReason(String, String)}
     * Given Scenario: one candidate of each rejection kind, plus accepted ones
     * ExpectedResult: each rejected candidate reports why, so log lines can say why without logging the
     * value; accepted candidates report no reason
     */
    @Test
    public void rejectionReason_namesWhyEachCandidateIsRejected() {

        final Object[][] cases = {
                {null,                                 RedirectUtil.RedirectRejection.BLANK},
                {"   ",                                RedirectUtil.RedirectRejection.BLANK},
                {"https://evil.com/x",                 RedirectUtil.RedirectRejection.OTHER_HOST},
                {"https://site.example.com.evil.com/", RedirectUtil.RedirectRejection.OTHER_HOST},
                {"http://evil.com@site.example.com/",  RedirectUtil.RedirectRejection.OTHER_HOST},
                {"javascript:alert(1)",                RedirectUtil.RedirectRejection.NOT_A_PATH},
                {"ftp://site.example.com/a",           RedirectUtil.RedirectRejection.NOT_A_PATH},
                {"companyCode=abc",                    RedirectUtil.RedirectRejection.NOT_A_PATH},
                {"//evil.com",                         RedirectUtil.RedirectRejection.PROTOCOL_RELATIVE},
                {"/%2F%2Fevil.com",                    RedirectUtil.RedirectRejection.PROTOCOL_RELATIVE},
                {"/a/..//evil.com",                    RedirectUtil.RedirectRejection.PROTOCOL_RELATIVE},
                {"/\\evil.com",                      RedirectUtil.RedirectRejection.FORBIDDEN_CHARACTER},
                {"/%5Cevil.com",                       RedirectUtil.RedirectRejection.FORBIDDEN_CHARACTER},
                {"/\t//evil.com",                     RedirectUtil.RedirectRejection.FORBIDDEN_CHARACTER},
                {"/x\"><script>",                     RedirectUtil.RedirectRejection.FORBIDDEN_CHARACTER},
                {"/bad%zz",                            RedirectUtil.RedirectRejection.MALFORMED_ENCODING},
                {"/members/..%2f..%2fdotAdmin/",       RedirectUtil.RedirectRejection.FORBIDDEN_CHARACTER},
                {"/members/%2e%2e%2FdotAdmin",         RedirectUtil.RedirectRejection.FORBIDDEN_CHARACTER},
                {"/page1/../../other-path",            RedirectUtil.RedirectRejection.ABOVE_ROOT},
                {"/%2e%2e/%2e%2e/other-path",          RedirectUtil.RedirectRejection.ABOVE_ROOT},
        };

        for (final Object[] testCase : cases) {
            assertEquals("candidate: " + testCase[0], Optional.of(testCase[1]),
                    RedirectUtil.rejectionReason((String) testCase[0], HOST));
        }
        assertEquals(Optional.empty(), RedirectUtil.rejectionReason("/members/report?id=42", HOST));
        assertEquals(Optional.empty(), RedirectUtil.rejectionReason("https://site.example.com/a", HOST));
    }

    /**
     * Method to test: {@link RedirectUtil#toSameHostRedirect(String, String)}
     * Given Scenario: a path whose slash is percent-encoded (%2f / %2F), which can hide a ".." segment from the
     * dot-segment resolution (PR #37984 review): "/members/..%2f..%2fdotAdmin/" decodes to
     * "/members/../../dotAdmin/"
     * ExpectedResult: rejected wherever the encoded slash is in the path, also on an absolute URL; an encoded
     * slash in the query string is still accepted
     */
    @Test
    public void encodedSlashInPath_isRejected() {

        final String[] rejected = {
                "/members/..%2f..%2fdotAdmin/",
                "/members/..%2F..%2FdotAdmin/",
                "/members/%2e%2e%2fdotAdmin",
                "/a%2fb",
                "https://site.example.com/members/..%2f..%2fdotAdmin/",
        };
        for (final String candidate : rejected) {
            assertEquals("candidate: " + candidate, Optional.empty(), RedirectUtil.toSameHostRedirect(candidate, HOST));
            assertEquals("candidate: " + candidate, Optional.of(RedirectUtil.RedirectRejection.FORBIDDEN_CHARACTER),
                    RedirectUtil.rejectionReason(candidate, HOST));
        }
        assertEquals(Optional.of("/search?u=%2Fa%2Fb"), RedirectUtil.toSameHostRedirect("/search?u=%2Fa%2Fb", HOST));
    }

    /**
     * Method to test: {@link RedirectUtil#toSameHostRedirect(String, String)}
     * Given Scenario: a path whose slash, backslash or dots are double-encoded (%252f, %255c, %252e), which a
     * second decode by a proxy or the container would turn into a traversal (PR #37984 review)
     * ExpectedResult: rejected; a double-encoded percent sign or space that hides nothing is still accepted
     */
    @Test
    public void doubleEncodedSeparatorsInPath_areRejected() {

        final String[] rejected = {
                "/members/..%252f..%252fdotAdmin/",
                "/members/..%252F..%252FdotAdmin/",
                "/members/%252e%252e/dotAdmin/",
                "/a%255cevil.com",
                "https://site.example.com/members/..%252f..%252fdotAdmin/",
        };
        for (final String candidate : rejected) {
            assertEquals("candidate: " + candidate, Optional.empty(), RedirectUtil.toSameHostRedirect(candidate, HOST));
            assertEquals("candidate: " + candidate, Optional.of(RedirectUtil.RedirectRejection.FORBIDDEN_CHARACTER),
                    RedirectUtil.rejectionReason(candidate, HOST));
        }
        assertEquals(Optional.of("/report/100%25"), RedirectUtil.toSameHostRedirect("/report/100%25", HOST));
        assertEquals(Optional.of("/images/my%2520photo.jpg"),
                RedirectUtil.toSameHostRedirect("/images/my%2520photo.jpg", HOST));
    }

    /**
     * Method to test: {@link RedirectUtil#toSameHostRedirect(String, java.util.Collection)} and
     * {@link RedirectUtil#rejectionReason(String, java.util.Collection)}
     * Given Scenario: the SAML paths pass the callback host plus the hostname and aliases of the site being
     * logged in to (upgrade compatibility: IdPs often send a default RelayState on another alias of the site)
     * ExpectedResult: any of those hosts is accepted; another site or a look-alike host is rejected as OTHER_HOST
     */
    @Test
    public void allowedHosts_acceptsSiteAliasesOnly() {

        final List<String> siteHosts = List.of("cms.customer.com", "WWW.Customer.com");

        assertEquals(Optional.of("/members/"),
                RedirectUtil.toSameHostRedirect("https://www.customer.com/members/", siteHosts));
        assertEquals(Optional.of("/a"),
                RedirectUtil.toSameHostRedirect("https://cms.customer.com/a", siteHosts));
        assertEquals(Optional.empty(), RedirectUtil.toSameHostRedirect("https://other-site.com/x", siteHosts));
        assertEquals(Optional.of(RedirectUtil.RedirectRejection.OTHER_HOST),
                RedirectUtil.rejectionReason("https://other-site.com/x", siteHosts));
        assertEquals(Optional.of(RedirectUtil.RedirectRejection.OTHER_HOST),
                RedirectUtil.rejectionReason("https://www.customer.com.evil.com/", siteHosts));
        assertEquals(Optional.empty(), RedirectUtil.rejectionReason("/members/", siteHosts));
    }

    /**
     * Method to test: {@link RedirectUtil#toSameHostRedirect(String, java.util.Collection)}
     * Given Scenario: no allowed hosts (empty or null collection)
     * ExpectedResult: every absolute URL is rejected; relative paths are still accepted
     */
    @Test
    public void allowedHosts_emptyRejectsAbsoluteUrls() {

        assertEquals(Optional.empty(), RedirectUtil.toSameHostRedirect("https://site.example.com/a", List.of()));
        assertEquals(Optional.empty(),
                RedirectUtil.toSameHostRedirect("https://site.example.com/a", (java.util.Collection<String>) null));
        assertEquals(Optional.of("/a"), RedirectUtil.toSameHostRedirect("/a", List.of()));
    }

    /**
     * Method to test: {@link RedirectUtil#toSameHostRedirect(String, String)}
     * Given Scenario: the original single-host methods, for every case of the acceptance matrix
     * ExpectedResult: they return exactly what the collection overload returns for that one host
     */
    @Test
    public void singleHostMethods_matchCollectionOverload() {

        final String[] candidates = {"/members/report?id=42", "https://site.example.com/a", "https://evil.com/x",
                "//evil.com", "/page1/../../x", "/x'", "companyCode=abc", null};
        for (final String candidate : candidates) {
            assertEquals("candidate: " + candidate,
                    RedirectUtil.toSameHostRedirect(candidate, List.of(HOST)),
                    RedirectUtil.toSameHostRedirect(candidate, HOST));
            assertEquals("candidate: " + candidate,
                    RedirectUtil.rejectionReason(candidate, List.of(HOST)),
                    RedirectUtil.rejectionReason(candidate, HOST));
        }
    }

    /**
     * Method to test: {@link RedirectUtil#toAllowedRedirect(String, java.util.Collection, java.util.Collection)}
     * Given Scenario: an explicitly allowed external host (auth.relaystate.allowed.hosts), next to the site's hosts
     * ExpectedResult: site hosts still become relative paths; an allowed external host keeps its absolute URL
     * (normalized, host lower-cased, port kept) so the browser actually goes there; anything else is rejected with
     * the same rules as before
     */
    @Test
    public void allowedExternalHosts_keepAbsoluteUrl_andApplyTheSameRules() {

        final List<String> site     = List.of(HOST);
        final List<String> external = List.of("partner.example.org");

        assertEquals(Optional.of("https://partner.example.org/welcome?x=1"),
                RedirectUtil.toAllowedRedirect("https://partner.example.org/a/../welcome?x=1", site, external));
        assertEquals(Optional.of("https://partner.example.org:8443/x"),
                RedirectUtil.toAllowedRedirect("https://PARTNER.example.org:8443/x", site, external));
        assertEquals(Optional.of("/a"),
                RedirectUtil.toAllowedRedirect("https://site.example.com/a", site, external));

        assertEquals(Optional.of(RedirectUtil.RedirectRejection.OTHER_HOST),
                RedirectUtil.rejectionReason("https://other.example.net/x", site, external));
        assertEquals(Optional.of(RedirectUtil.RedirectRejection.OTHER_HOST),
                RedirectUtil.rejectionReason("http://user@partner.example.org/x", site, external));
        assertEquals(Optional.of(RedirectUtil.RedirectRejection.ABOVE_ROOT),
                RedirectUtil.rejectionReason("https://partner.example.org/%2e%2e/%2e%2e/x", site, external));
        assertEquals(Optional.of(RedirectUtil.RedirectRejection.FORBIDDEN_CHARACTER),
                RedirectUtil.rejectionReason("https://partner.example.org/x\"><script>", site, external));
        assertEquals(Optional.empty(), RedirectUtil.toAllowedRedirect("https://other.example.net/x", site, external));
    }

    /**
     * Method to test: {@link RedirectUtil#toAllowedRedirect(String, java.util.Collection, java.util.Collection)}
     * Given Scenario: no external hosts allowed
     * ExpectedResult: exactly the same results as the site-hosts-only check
     */
    @Test
    public void noExternalHosts_matchesSameHostCheck() {

        final String[] candidates = {"/members/report?id=42", "https://site.example.com/a", "https://evil.com/x",
                "//evil.com", "/page1/../../x", "/x'", "companyCode=abc", null};
        for (final String candidate : candidates) {
            assertEquals("candidate: " + candidate,
                    RedirectUtil.toSameHostRedirect(candidate, List.of(HOST)),
                    RedirectUtil.toAllowedRedirect(candidate, List.of(HOST), List.of()));
            assertEquals("candidate: " + candidate,
                    RedirectUtil.rejectionReason(candidate, List.of(HOST)),
                    RedirectUtil.rejectionReason(candidate, List.of(HOST), List.of()));
        }
    }

    /**
     * Method to test: {@link RedirectUtil#toSameHostRedirect(String, String)}
     * Given Scenario: the request host is unknown (null)
     * ExpectedResult: absolute URLs are rejected because no host can match; relative paths are still accepted
     */
    @Test
    public void toSameHostRedirect_nullHostRejectsAbsoluteUrls() {

        assertEquals(Optional.empty(), RedirectUtil.toSameHostRedirect("https://site.example.com/a", (String) null));
        assertEquals(Optional.of("/a"), RedirectUtil.toSameHostRedirect("/a", (String) null));
    }

    /**
     * Method to test: {@link RedirectUtil#toSameHostRedirect(String, String)}
     * Given Scenario: every accepted result from the acceptance cases
     * ExpectedResult: it holds the invariants from the data model: starts with a single "/", contains no
     * forbidden character, and has no "." or ".." segment in plain or encoded form
     */
    @Test
    public void toSameHostRedirect_resultsHoldInvariants() {

        final String[] candidates = {"/a/./b/../c", "https://site.example.com/x/%2e/y", "/q?x=1#f"};
        for (final String candidate : candidates) {
            final String result = RedirectUtil.toSameHostRedirect(candidate, HOST).orElseThrow();
            assertTrue(result, result.startsWith("/") && !result.startsWith("//"));
            final String path = result.split("[?#]", 2)[0];
            for (final String segment : path.split("/")) {
                final String decoded = segment.replace("%2e", ".").replace("%2E", ".");
                assertFalse(result, ".".equals(decoded) || "..".equals(decoded));
            }
        }
    }

    /**
     * Method to test: {@link RedirectUtil#sendRedirectHTML(HttpServletResponse, String)}
     * Given Scenario: a valid URL containing "&"
     * ExpectedResult: the URL is HTML-attribute-encoded in both the meta-refresh and the link, which the
     * browser decodes back to the same URL, and the content type is still text/html
     */
    @Test
    public void sendRedirectHTML_encodesAmpersandInBothAttributes() throws Exception {

        final StringWriter body = new StringWriter();
        final HttpServletResponse response = responseWritingTo(body);

        RedirectUtil.sendRedirectHTML(response, "/a?x=1&y=2");

        final String html = body.toString();
        assertTrue(html, html.contains("URL='/a?x=1&amp;y=2'"));
        assertTrue(html, html.contains("href=\"/a?x=1&amp;y=2\""));
        verify(response).setContentType("text/html");
    }

    /**
     * Method to test: {@link RedirectUtil#sendRedirectHTML(HttpServletResponse, String)}
     * Given Scenario: a URL crafted to close the attribute and inject a script (AC-008)
     * ExpectedResult: quotes and angle brackets are written as entities, so the script never appears in the page
     */
    @Test
    public void sendRedirectHTML_cannotBreakOutOfAttributes() throws Exception {

        final StringWriter body = new StringWriter();
        final HttpServletResponse response = responseWritingTo(body);

        RedirectUtil.sendRedirectHTML(response, "/x'\"><script>alert(1)</script>");

        final String html = body.toString();
        assertFalse(html, html.contains("<script>alert(1)"));
        assertTrue(html, html.contains("/x&#39;&quot;&gt;&lt;script&gt;alert(1)&lt;/script&gt;"));
    }

    private static HttpServletResponse responseWritingTo(final StringWriter body) throws Exception {

        final HttpServletResponse response = mock(HttpServletResponse.class);
        when(response.getWriter()).thenReturn(new PrintWriter(body));
        return response;
    }
}
