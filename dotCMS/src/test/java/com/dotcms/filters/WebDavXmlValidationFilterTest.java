package com.dotcms.filters;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.dotcms.mock.request.DotCMSMockRequest;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import javax.servlet.FilterChain;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

/**
 * Covers the WebDAV body validation: a document carrying a DTD must never reach the servlet, and
 * everything a real WebDAV client sends must still get through with its body intact.
 */
public class WebDavXmlValidationFilterTest {

    private static final String DOCTYPE_BODY = "<?xml version=\"1.0\"?>\n"
            + "<!DOCTYPE propertyupdate [ <!ENTITY leak SYSTEM \"file:///etc/hostname\"> ]>\n"
            + "<propertyupdate xmlns=\"DAV:\"><set><prop>"
            + "<displayname>&leak;</displayname></prop></set></propertyupdate>";

    private static final String LEGITIMATE_PROPFIND = "<?xml version=\"1.0\"?>\n"
            + "<propfind xmlns=\"DAV:\"><prop><getcontentlength/><displayname/></prop></propfind>";

    @Test
    public void doctypeBodyIsRejectedWithBadRequestAndNeverReachesTheServlet() throws Exception {
        final FilterChain chain = mock(FilterChain.class);
        final HttpServletResponse response = mock(HttpServletResponse.class);

        new WebDavXmlValidationFilter().doFilter(request("PROPPATCH", DOCTYPE_BODY), response, chain);

        verify(response).sendError(HttpServletResponse.SC_BAD_REQUEST);
        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    public void legitimateBodyPassesThroughAndIsStillReadableDownstream() throws Exception {
        final FilterChain chain = mock(FilterChain.class);
        final HttpServletResponse response = mock(HttpServletResponse.class);

        new WebDavXmlValidationFilter()
                .doFilter(request("PROPFIND", LEGITIMATE_PROPFIND), response, chain);

        verify(response, never()).sendError(anyInt());

        // The filter consumes the body to inspect it, so it must hand the servlet a replayable copy.
        final ArgumentCaptor<ServletRequest> forwarded = ArgumentCaptor.forClass(ServletRequest.class);
        verify(chain).doFilter(forwarded.capture(), any());
        assertArrayEquals("Body was not replayed intact to the servlet",
                LEGITIMATE_PROPFIND.getBytes(StandardCharsets.UTF_8),
                forwarded.getValue().getInputStream().readAllBytes());
    }

    /**
     * The servlet already tolerates a body it cannot parse: a PROPFIND whose XML is broken falls
     * back to allprop and still answers 207. Rejecting those here would change behaviour that has
     * nothing to do with DTDs, so a malformed body must be forwarded untouched.
     */
    @Test
    public void malformedBodyIsForwardedRatherThanRejected() throws Exception {
        final FilterChain chain = mock(FilterChain.class);
        final HttpServletResponse response = mock(HttpServletResponse.class);

        new WebDavXmlValidationFilter()
                .doFilter(request("PROPFIND", "<propfind xmlns=\"DAV:\"><prop>"), response, chain);

        verify(response, never()).sendError(anyInt());
        verify(chain).doFilter(any(), any());
    }

    /** PROPFIND with no body means allprop, and LOCK with no body is a refresh. Both are legal. */
    @Test
    public void emptyBodyIsAllowed() throws Exception {
        final FilterChain chain = mock(FilterChain.class);
        final HttpServletResponse response = mock(HttpServletResponse.class);

        new WebDavXmlValidationFilter().doFilter(request("PROPFIND", ""), response, chain);

        verify(response, never()).sendError(anyInt());
        verify(chain).doFilter(any(), any());
    }

    /** GET and PUT carry no XML for the servlet to parse, so they must not be buffered or inspected. */
    @Test
    public void nonXmlMethodsAreForwardedUntouched() throws Exception {
        final FilterChain chain = mock(FilterChain.class);
        final HttpServletResponse response = mock(HttpServletResponse.class);
        final HttpServletRequest request = request("PUT", DOCTYPE_BODY);

        new WebDavXmlValidationFilter().doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        verify(response, never()).sendError(anyInt());
    }

    @Test
    public void oversizedBodyIsRejected() throws Exception {
        final FilterChain chain = mock(FilterChain.class);
        final HttpServletResponse response = mock(HttpServletResponse.class);
        final String huge = "<propfind xmlns=\"DAV:\">"
                + "x".repeat(WebDavXmlValidationFilter.MAX_BODY_BYTES + 1) + "</propfind>";

        new WebDavXmlValidationFilter().doFilter(request("PROPFIND", huge), response, chain);

        verify(response).sendError(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE);
        verify(chain, never()).doFilter(any(), any());
    }

    /**
     * The reason detection is a parse rather than a byte search: a search for the literal
     * "DOCTYPE" misses a UTF-16 encoded body, where every character is two bytes. Asserting the
     * clean document is still accepted in the same encoding proves the rejection is caused by the
     * DOCTYPE and not merely by the parser disliking UTF-16.
     */
    @Test
    public void doctypeIsDetectedRegardlessOfEncoding() {
        final String clean = "<?xml version=\"1.0\"?>"
                + "<propertyupdate xmlns=\"DAV:\"><set><prop>"
                + "<displayname>harmless</displayname></prop></set></propertyupdate>";

        for (final Charset charset : List.of(StandardCharsets.UTF_8, StandardCharsets.UTF_16)) {
            assertTrue("A DOCTYPE slipped past detection encoded as " + charset,
                    WebDavXmlValidationFilter.declaresDoctype(DOCTYPE_BODY.getBytes(charset)));
            assertFalse("A clean document was reported as declaring a DOCTYPE in " + charset,
                    WebDavXmlValidationFilter.declaresDoctype(clean.getBytes(charset)));
        }
    }

    private static HttpServletRequest request(final String method, final String body) throws Exception {
        final DotCMSMockRequest request = new DotCMSMockRequest();
        request.setMethod(method);
        request.setRemoteAddr("203.0.113.7");
        request.setCharacterEncoding("UTF-8");
        request.setContent(body.getBytes(StandardCharsets.UTF_8));
        return request;
    }
}
