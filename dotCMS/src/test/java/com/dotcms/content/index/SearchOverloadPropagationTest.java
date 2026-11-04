package com.dotcms.content.index;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.rendering.velocity.events.MethodExceptionEventHandlerImpl;
import com.dotcms.rest.api.v1.authentication.ResponseUtil;
import com.dotcms.rest.exception.mapper.DotDataExceptionMapper;
import com.dotcms.rest.exception.mapper.ExceptionMapperUtil;
import com.dotmarketing.exception.DotDataException;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;
import org.junit.After;
import org.junit.Test;

/** Temporary overload must survive wrappers and must never look like successful missing content. */
public class SearchOverloadPropagationTest {
    private final SearchUnavailableException failure = new SearchUnavailableException(
            SearchUnavailableException.Reason.REJECTED, new IllegalStateException("private query value"));

    /** Restores the existing request thread-local rather than adding another one. */
    @After
    public void cleanup() {
        HttpServletRequestThreadLocal.INSTANCE.setRequest(null);
    }

    /** Runtime and checked REST wrappers must use the same safe non-cacheable 503 response. */
    @Test
    public void wrappedFailuresHaveSafe503Responses() {
        for (Response response : new Response[]{
                ResponseUtil.mapExceptionResponse(new RuntimeException(failure)),
                ExceptionMapperUtil.createResponse(new DotDataException(failure), Response.Status.BAD_REQUEST),
                new DotDataExceptionMapper().toResponse(new DotDataException(failure))}) {
            try (response) {
                assertEquals(503, response.getStatus());
                assertEquals("no-store", response.getHeaderString("Cache-Control"));
                assertEquals("1", response.getHeaderString("Retry-After"));
                assertFalse(response.getEntity().toString().contains("private query value"));
            }
        }
    }

    /** Velocity must propagate overload instead of returning null for a failed viewtool call. */
    @Test
    public void velocityMethodHandlerRethrowsWrappedOverload() {
        assertSame(failure, assertThrows(SearchUnavailableException.class,
                () -> new MethodExceptionEventHandlerImpl().methodException(
                        String.class, "test", new RuntimeException(failure))));
    }

    /** A legacy helper swallowing an exception cannot make its partial output cacheable. */
    @Test
    public void swallowedFailureStillAbortsCacheFillButDoesNotLeakIntoNextRequest() {
        final Map<String, Object> attributes = new HashMap<>();
        final HttpServletRequest request = mock(HttpServletRequest.class);
        doAnswer(invocation -> attributes.put(invocation.getArgument(0), invocation.getArgument(1)))
                .when(request).setAttribute(anyString(), any());
        when(request.getAttribute(anyString())).thenAnswer(invocation -> attributes.get(invocation.getArgument(0)));
        HttpServletRequestThreadLocal.INSTANCE.setRequest(request);
        SearchRequestFailure.record(failure);
        assertSame(failure, assertThrows(SearchUnavailableException.class, SearchRequestFailure::check));
        HttpServletRequestThreadLocal.INSTANCE.setRequest(mock(HttpServletRequest.class));
        SearchRequestFailure.check();
    }

    /** The servlet writes a direct 503 rather than dispatching another search-dependent error page. */
    @Test
    public void servletOverloadResponseIsNonCacheableAndDoesNotDispatch() throws Exception {
        final HttpServletResponse response = mock(HttpServletResponse.class);
        when(response.getOutputStream()).thenReturn(mock(ServletOutputStream.class));
        SearchRequestFailure.respond(response);
        verify(response).resetBuffer();
        verify(response).setStatus(503);
        verify(response).setHeader("Cache-Control", "no-store");
        verify(response).setHeader("Retry-After", "1");
        verify(response, never()).sendError(anyInt());
    }

    /** Bytes already sent cannot be recalled; abort rather than completing a cacheable partial page. */
    @Test
    public void committedServletResponseIsAbortedWithoutWritingMoreBytes() throws Exception {
        final HttpServletResponse response = mock(HttpServletResponse.class);
        when(response.isCommitted()).thenReturn(true);
        assertThrows(java.io.IOException.class, () -> SearchRequestFailure.respond(response));
        verify(response, never()).resetBuffer();
        verify(response, never()).getOutputStream();
    }
}
