package com.dotcms.cdn;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dotcms.filters.interceptor.Result;
import com.dotmarketing.util.Config;
import java.util.Collections;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link CDNInterceptor}. The interceptor is registered for every request, so the
 * main thing worth proving is that it stays out of the request when its debug switch is off.
 */
class CDNInterceptorTest {

    private static final String DEBUG_HEADERS = "DOT_CDN_DEBUG_HEADERS";

    private final CDNInterceptor interceptor = new CDNInterceptor();
    private final HttpServletRequest request = mock(HttpServletRequest.class);
    private final HttpServletResponse response = mock(HttpServletResponse.class);

    @AfterEach
    void restoreConfig() {
        Config.setProperty(DEBUG_HEADERS, false);
    }

    /**
     * Reading a request parameter on a form-encoded POST makes the container consume the body, so
     * downstream code that reads the stream would get nothing. With the debug switch off (the
     * default) the interceptor must therefore not touch the parameters at all.
     */
    @Test
    void intercept_debugSwitchOff_doesNotReadRequestParameters() {
        Config.setProperty(DEBUG_HEADERS, false);

        final Result result = interceptor.intercept(request, response);

        assertEquals(Result.NEXT, result);
        verify(request, never()).getParameter(anyString());
        verify(request, never()).getHeaderNames();
    }

    @Test
    void intercept_debugSwitchOnWithoutTheParameter_doesNotRecordHeaders() {
        Config.setProperty(DEBUG_HEADERS, true);
        when(request.getParameter("recordDotHeaders")).thenReturn(null);

        final Result result = interceptor.intercept(request, response);

        assertEquals(Result.NEXT, result);
        verify(request, never()).getHeaderNames();
    }

    @Test
    void intercept_debugSwitchOnWithTheParameter_recordsHeadersAndRedactsSensitiveOnes() {
        Config.setProperty(DEBUG_HEADERS, true);
        when(request.getParameter("recordDotHeaders")).thenReturn("true");
        when(request.getHeaderNames())
                .thenReturn(Collections.enumeration(java.util.List.of("Authorization", "Accept")));
        when(request.getHeader("Accept")).thenReturn("text/html");

        final Result result = interceptor.intercept(request, response);

        assertEquals(Result.NEXT, result);
        verify(request).getHeader("Accept");
        verify(request, never()).getHeader("Authorization");
    }
}
