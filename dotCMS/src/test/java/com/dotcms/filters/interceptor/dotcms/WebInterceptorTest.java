package com.dotcms.filters.interceptor.dotcms;

import com.dotcms.UnitTestBase;
import com.dotcms.filters.interceptor.Result;
import com.dotcms.filters.interceptor.WebInterceptor;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Testing the web interceptor states and wrappers
 *
 * @author jsanca
 */
public class WebInterceptorTest extends UnitTestBase {

    private static class MyTestWebInterceptor implements WebInterceptor {

        @Override
        public Result intercept(final HttpServletRequest request, HttpServletResponse response)
                throws IOException {

            Result result = Result.NEXT;
            final boolean isLoggedToBackend = "admin".equals(request.getParameter("user"));

            // if we are not logged in...
            if (!isLoggedToBackend) {

                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.sendRedirect("/login");

                result = Result.SKIP_NO_CHAIN; // needs to stop the filter chain.
            } else {

                if ("admin".equals(request.getParameter("pass"))) {
                    result = new Result.Builder().skip().
                            wrap(new WrapRequest(request)).wrap(new WrapResponse(response))
                            .build();
                }
            }

            return result; // if it is log in, continue!

        }
    }

    private static class WrapRequest extends HttpServletRequestWrapper {
        public WrapRequest(HttpServletRequest request) {
            super(request);
        }
    }

    private static class WrapResponse extends HttpServletResponseWrapper {
        public WrapResponse(HttpServletResponse response) {
            super(response);
        }
    }


    /**
     * Test the scenario the request parameter is empty so needs to be redirect to login
     */
    @Test
    public void intercept_is_skip_no_chain_true() throws Exception {

        final HttpServletRequest request = mock(HttpServletRequest.class);
        final HttpServletResponse response = mock(HttpServletResponse.class);

        final MyTestWebInterceptor myTestWebInterceptor =
                new MyTestWebInterceptor();

        myTestWebInterceptor.init();

        Result result = myTestWebInterceptor.intercept(request, response);
        assertEquals(result, Result.SKIP_NO_CHAIN);

        myTestWebInterceptor.destroy();
    }

    /**
     * Test the scenario when the user can not be logged b/c the credentials are wrong
     */
    @Test
    public void intercept_is_next_true() throws Exception {

        final HttpServletRequest request = mock(HttpServletRequest.class);
        final HttpServletResponse response = mock(HttpServletResponse.class);

        when(request.getParameter("user")).thenReturn("admin");
        when(request.getParameter("pass")).thenReturn("fail"); // wrong pass
        final MyTestWebInterceptor myTestWebInterceptor =
                new MyTestWebInterceptor();

        myTestWebInterceptor.init();

        Result result = myTestWebInterceptor.intercept(request, response);
        assertEquals(result, Result.NEXT);

        myTestWebInterceptor.destroy();
    }

    /**
     * Test the scenario when the user is logged in and skip the rest of the interceptors
     * also the response and request are wrapped
     */
    @Test
    public void intercept_is_skip_true() throws Exception {

        final HttpServletRequest request = mock(HttpServletRequest.class);
        final HttpServletResponse response = mock(HttpServletResponse.class);

        when(request.getParameter("user")).thenReturn("admin");
        when(request.getParameter("pass")).thenReturn("admin");
        final MyTestWebInterceptor myTestWebInterceptor =
                new MyTestWebInterceptor();

        myTestWebInterceptor.init();

        Result result = myTestWebInterceptor.intercept(request, response);
        assertEquals(result.getType(), Result.SKIP.getType());
        assertNotNull(result.getRequest());
        assertEquals(WrapRequest.class, result.getRequest().getClass());
        assertNotNull(result.getResponse());
        assertEquals(WrapResponse.class, result.getResponse().getClass());

        myTestWebInterceptor.destroy();
    }


}