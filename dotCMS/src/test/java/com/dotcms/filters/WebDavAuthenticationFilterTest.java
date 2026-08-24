package com.dotcms.filters;

import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.dotcms.mock.request.DotCMSMockRequest;
import java.util.List;
import javax.servlet.FilterChain;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

/**
 * Covers the credentials requirement on the WebDAV endpoints: a request that carries none is
 * answered with a challenge and never reaches the servlet, whatever method or body it uses, and a
 * request that carries credentials is forwarded untouched.
 */
public class WebDavAuthenticationFilterTest {

    private static final String CREDENTIALS = "Basic YWRtaW5AZG90Y21zLmNvbTphZG1pbg==";

    /**
     * Every WebDAV method the servlet answers. The methods the library routes through its shared
     * path were already refused without credentials; PROPPATCH and OPTIONS were not. The filter
     * makes no distinction between them, and this asserts that rather than only the two that were
     * reported.
     */
    private static final List<String> WEBDAV_METHODS = List.of(
            "PROPPATCH", "PROPFIND", "OPTIONS", "LOCK", "UNLOCK", "MKCOL", "MOVE", "COPY",
            "GET", "HEAD", "PUT", "DELETE");

    @Test
    public void requestWithoutCredentialsIsChallengedAndNeverReachesTheServlet() throws Exception {
        final FilterChain chain = mock(FilterChain.class);
        final HttpServletResponse response = mock(HttpServletResponse.class);

        new WebDavAuthenticationFilter().doFilter(request("PROPPATCH", null), response, chain);

        verifyChallenged(response);
        verify(chain, never()).doFilter(any(), any());
    }

    /**
     * The body is what the reported cases varied, and it is exactly what must not matter: a
     * PROPPATCH naming no property was handled to completion because the check that refuses one
     * works by iterating the properties named.
     */
    @Test
    public void aBodyThatNamesNoPropertyIsStillRefused() throws Exception {
        for (final String body : List.of("", "<?xml version=\"1.0\"?><r/>",
                "<?xml version=\"1.0\"?><propertyupdate xmlns=\"DAV:\"/>")) {
            final FilterChain chain = mock(FilterChain.class);
            final HttpServletResponse response = mock(HttpServletResponse.class);

            final DotCMSMockRequest request = request("PROPPATCH", null);
            request.setContent(body);
            new WebDavAuthenticationFilter().doFilter(request, response, chain);

            verifyChallenged(response);
            verify(chain, never()).doFilter(any(), any());
        }
    }

    @Test
    public void everyMethodIsRefusedWithoutCredentials() throws Exception {
        for (final String method : WEBDAV_METHODS) {
            final FilterChain chain = mock(FilterChain.class);
            final HttpServletResponse response = mock(HttpServletResponse.class);

            new WebDavAuthenticationFilter().doFilter(request(method, null), response, chain);

            verifyChallenged(response);
            verify(chain, never()).doFilter(any(), any());
        }
    }

    /**
     * A refusal with no challenge leaves a client nothing to do but give up, so the header is part
     * of the behaviour and not decoration.
     */
    @Test
    public void theRefusalCarriesABasicChallenge() throws Exception {
        final HttpServletResponse response = mock(HttpServletResponse.class);

        new WebDavAuthenticationFilter()
                .doFilter(request("OPTIONS", null), response, mock(FilterChain.class));

        final ArgumentCaptor<String> challenge = ArgumentCaptor.forClass(String.class);
        verify(response).setHeader(eq("WWW-Authenticate"), challenge.capture());
        assertTrue("The challenge must name a scheme the client can use: " + challenge.getValue(),
                challenge.getValue().startsWith("Basic realm="));
    }

    @Test
    public void requestWithCredentialsIsForwardedUntouched() throws Exception {
        final FilterChain chain = mock(FilterChain.class);
        final HttpServletResponse response = mock(HttpServletResponse.class);
        final HttpServletRequest request = request("PROPPATCH", CREDENTIALS);

        new WebDavAuthenticationFilter().doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        verify(response, never()).sendError(anyInt());
    }

    /**
     * A header that is present but empty is not credentials. Treating it as some evidence of a
     * caller would reopen the path this closes.
     */
    @Test
    public void anEmptyAuthorizationHeaderIsNotCredentials() throws Exception {
        for (final String header : List.of("", "   ")) {
            final FilterChain chain = mock(FilterChain.class);
            final HttpServletResponse response = mock(HttpServletResponse.class);

            new WebDavAuthenticationFilter().doFilter(request("PROPPATCH", header), response, chain);

            verifyChallenged(response);
            verify(chain, never()).doFilter(any(), any());
        }
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

    /**
     * A refusal has to be the answer to the request, so the status is set rather than raised as a
     * container error: web.xml maps 401 to an error page that redirects, and a client handed a 302
     * to a login page cannot act on it. Caught live, where every refusal came back 302.
     */
    private static void verifyChallenged(final HttpServletResponse response) throws Exception {
        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(response, never()).sendError(anyInt());
    }

    private static DotCMSMockRequest request(final String method, final String authorization) {
        // DotCMSMockRequest holds headers but exposes no setter for them, so the one header this
        // filter reads is supplied here.
        final DotCMSMockRequest request = new DotCMSMockRequest() {
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
