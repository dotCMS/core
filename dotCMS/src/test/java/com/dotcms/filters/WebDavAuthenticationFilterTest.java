package com.dotcms.filters;

import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.dotcms.mock.request.DotCMSMockRequest;
import com.dotmarketing.util.Config;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import javax.servlet.FilterChain;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

/** Covers the WebDAV credential gate; the helper still has to authenticate any forwarded claim. */
public class WebDavAuthenticationFilterTest {

    private static final String CREDENTIALS = "Basic YWRtaW5AZG90Y21zLmNvbTphZG1pbg==";
    private static final String TOKEN_CREDENTIALS = "Basic " + Base64.getEncoder().encodeToString(
            "dav-user@example.com:test-api-jwt".getBytes(StandardCharsets.UTF_8));
    private static final List<String> WEBDAV_METHODS = List.of(
            "PROPPATCH", "PROPFIND", "OPTIONS", "LOCK", "UNLOCK", "MKCOL", "MOVE", "COPY",
            "GET", "HEAD", "PUT", "DELETE");
    private String previousMode;

    /** Save the caller's configuration rather than imposing ALL on the entire suite. */
    @Before
    public void rememberMode() {
        previousMode = Config.getStringProperty("ALLOW_BASIC_AUTH", null);
    }

    /** Restore an absent property as absent as well as restoring explicit values. */
    @After
    public void restoreMode() {
        Config.setProperty("ALLOW_BASIC_AUTH", previousMode);
    }

    /** Every library method must challenge missing credentials without a container redirect. */
    @Test
    public void everyMethodIsChallengedWithoutCredentialsInEveryMode() throws Exception {
        for (final String mode : new String[]{null, "NONE", "JWT_TOKEN", "ALL"}) {
            Config.setProperty("ALLOW_BASIC_AUTH", mode);
            for (final String method : WEBDAV_METHODS) {
                final FilterChain chain = mock(FilterChain.class);
                final HttpServletResponse response = mock(HttpServletResponse.class);

                new WebDavAuthenticationFilter().doFilter(request(method, null), response, chain);

                verifyChallenged(response);
                verify(chain, never()).doFilter(any(), any());
            }
        }
    }

    /** A blank header must never open the servlet, even in a mode that permits credentials. */
    @Test
    public void blankAuthorizationIsChallengedInEveryMode() throws Exception {
        for (final String mode : new String[]{null, "NONE", "JWT_TOKEN", "ALL"}) {
            Config.setProperty("ALLOW_BASIC_AUTH", mode);
            for (final String header : List.of("", "   ")) {
                final FilterChain chain = mock(FilterChain.class);
                final HttpServletResponse response = mock(HttpServletResponse.class);

                new WebDavAuthenticationFilter()
                        .doFilter(request("PROPPATCH", header), response, chain);

                verifyChallenged(response);
                verify(chain, never()).doFilter(any(), any());
            }
        }
    }

    /** Explicit NONE, FALSE, empty and unrecognized values do not forward credential-bearing data requests. */
    @Test
    public void credentialsAreNotForwardedWhenBasicAuthenticationIsDisabled() throws Exception {
        for (final String mode : new String[]{"NONE", "false", "invalid-mode", ""}) {
            Config.setProperty("ALLOW_BASIC_AUTH", mode);
            for (final String method : List.of("PROPPATCH", "PROPFIND", "GET", "PUT")) {
                final FilterChain chain = mock(FilterChain.class);
                final HttpServletResponse response = mock(HttpServletResponse.class);

                new WebDavAuthenticationFilter()
                        .doFilter(request(method, CREDENTIALS), response, chain);

                verify(chain, never()).doFilter(any(), any());
                verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                verify(response, never()).sendError(anyInt());
                verify(response, never()).sendError(anyInt(), anyString());
            }
        }
    }

    /** ALL preserves the legacy username/password handshake; the filter grants no principal. */
    @Test
    public void allForwardsCredentialsWithoutAuthenticatingTheRequest() throws Exception {
        verifyForwarded("ALL", CREDENTIALS);
    }

    /** JWT_TOKEN uses DAV's username/JWT Basic pair, not a new Bearer authentication flow. */
    @Test
    public void jwtTokenForwardsTheBasicPairForHelperValidation() throws Exception {
        verifyForwarded("JWT_TOKEN", TOKEN_CREDENTIALS);
    }

    /**
     * The absent-property default resolves to JWT_TOKEN, so a DAV client that sends its
     * username/API-JWT pair works without configuration; the helper still validates the pair.
     */
    @Test
    public void absentDefaultForwardsTheBasicPairLikeJwtToken() throws Exception {
        Config.setProperty("ALLOW_BASIC_AUTH", null);
        final FilterChain chain = mock(FilterChain.class);
        final HttpServletResponse response = mock(HttpServletResponse.class);

        new WebDavAuthenticationFilter()
                .doFilter(request("PROPPATCH", TOKEN_CREDENTIALS), response, chain);

        verify(chain).doFilter(any(), any());
        verify(response, never()).setStatus(anyInt());
    }

    /** Nothing but HTTP reaches this mapping, but a non-HTTP request must not be swallowed. */
    @Test
    public void nonHttpRequestIsForwarded() throws Exception {
        final FilterChain chain = mock(FilterChain.class);
        final ServletRequest request = mock(ServletRequest.class);
        final ServletResponse response = mock(ServletResponse.class);

        new WebDavAuthenticationFilter().doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    /** Forwarding a credential claim must not create an authenticated servlet principal. */
    private static void verifyForwarded(final String mode, final String credentials) throws Exception {
        Config.setProperty("ALLOW_BASIC_AUTH", mode);
        final FilterChain chain = mock(FilterChain.class);
        final HttpServletResponse response = mock(HttpServletResponse.class);
        final DotCMSMockRequest request = request("PROPPATCH", credentials);

        new WebDavAuthenticationFilter().doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        org.junit.Assert.assertNull(request.getUserPrincipal());
        verify(response, never()).sendError(anyInt());
        verify(response, never()).sendError(anyInt(), anyString());
    }

    /** setStatus avoids web.xml's 401 error-page mapping, which otherwise turns this into 302. */
    private static void verifyChallenged(final HttpServletResponse response) throws Exception {
        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(response, never()).sendError(anyInt());
        verify(response, never()).sendError(anyInt(), anyString());
        final ArgumentCaptor<String> challenge = ArgumentCaptor.forClass(String.class);
        verify(response).setHeader(eq("WWW-Authenticate"), challenge.capture());
        assertTrue("DAV clients must receive a usable challenge",
                challenge.getValue().startsWith("Basic realm="));
    }

    /** Supply the one header the filter reads without mocking the filter itself. */
    private static DotCMSMockRequest request(final String method, final String authorization) {
        final DotCMSMockRequest request = new DotCMSMockRequest() {
            /** Headers have no setter on the mock request. */
            @Override
            public String getHeader(final String name) {
                return "authorization".equalsIgnoreCase(name) ? authorization : super.getHeader(name);
            }
        };
        request.setMethod(method);
        request.setRequestURI("/webdav/live/1/default");
        request.setRemoteAddr("127.0.0.1");
        return request;
    }
}
