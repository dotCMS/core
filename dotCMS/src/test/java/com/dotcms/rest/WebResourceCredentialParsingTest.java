package com.dotcms.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dotcms.UnitTestBase;
import com.dotcms.rest.exception.SecurityException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;
import java.util.function.Supplier;
import javax.servlet.http.HttpServletRequest;
import org.junit.Test;

/**
 * Pins the credential-header parsing contract shared by {@code Authorization: Basic ...} and the
 * {@code DOTAUTH} header: the whole secret is preserved (so a colon inside a token, e.g. a JWT with
 * appended data, is not truncated), the scheme prefix is matched case-insensitively, and malformed
 * Base64, a missing separator and an empty secret still fail with the pre-existing {@code 400}.
 */
public class WebResourceCredentialParsingTest extends UnitTestBase {

    // A JWT-shaped secret followed by extra data after a colon: the old split(":") kept only
    // "header.payload.signature" and silently discarded the ":junk" suffix.
    private static final String COLON_SECRET =
            "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ1c2VyIn0.signature:junk";

    /** The Basic transport must preserve every character of the secret, colons included. */
    @Test
    public void colon_containing_secret_is_preserved_by_basic_transport() {
        final WebResource.UsernamePassword parsed =
                parseBasic("Basic " + encode("user@example.com:" + COLON_SECRET)).get();
        assertEquals("user@example.com", parsed.username);
        assertEquals(COLON_SECRET, parsed.password);
    }

    /** The DOTAUTH transport must preserve every character of the secret, colons included. */
    @Test
    public void colon_containing_secret_is_preserved_by_dot_auth_transport() {
        final WebResource.UsernamePassword parsed =
                parseDotAuth(encode("user@example.com:" + COLON_SECRET)).get();
        assertEquals("user@example.com", parsed.username);
        assertEquals(COLON_SECRET, parsed.password);
    }

    /** The Basic scheme prefix is case-insensitive per HTTP auth-scheme matching. */
    @Test
    public void basic_scheme_prefix_is_case_insensitive() {
        final String encoded = encode("user:pass");
        for (final String prefix : new String[]{"Basic ", "basic ", "BASIC ", "bAsIc "}) {
            final WebResource.UsernamePassword parsed = parseBasic(prefix + encoded).get();
            assertEquals("user", parsed.username);
            assertEquals("pass", parsed.password);
        }
    }

    /** Malformed Base64 stays a 400 on both transports. */
    @Test
    public void malformed_base64_is_rejected_with_bad_request_by_both_transports() {
        assertBadRequest(() -> parseBasic("Basic !!!not-base64!!!"));
        assertBadRequest(() -> parseDotAuth("!!!not-base64!!!"));
    }

    /** A body with no colon separator stays a 400 on both transports. */
    @Test
    public void missing_separator_is_rejected_with_bad_request_by_both_transports() {
        final String encoded = encode("user-without-colon");
        assertBadRequest(() -> parseBasic("Basic " + encoded));
        assertBadRequest(() -> parseDotAuth(encoded));
    }

    /** An empty secret stays a 400 on both transports. */
    @Test
    public void empty_secret_is_rejected_with_bad_request_by_both_transports() {
        final String encoded = encode("user:");
        assertBadRequest(() -> parseBasic("Basic " + encoded));
        assertBadRequest(() -> parseDotAuth(encoded));
    }

    private static Optional<WebResource.UsernamePassword> parseBasic(final String headerValue) {
        final HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("Authorization")).thenReturn(headerValue);
        return WebResource.getAuthCredentialsFromBasicAuth(request);
    }

    private static Optional<WebResource.UsernamePassword> parseDotAuth(final String headerValue) {
        final HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("DOTAUTH")).thenReturn(headerValue);
        return WebResource.getAuthCredentialsFromHeaderAuth(request);
    }

    private static String encode(final String credentials) {
        return Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
    }

    private static void assertBadRequest(
            final Supplier<Optional<WebResource.UsernamePassword>> parser) {
        final SecurityException denied = assertThrows(SecurityException.class, parser::get);
        assertEquals(400, denied.getResponse().getStatus());
    }
}
