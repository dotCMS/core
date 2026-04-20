package com.dotcms.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dotcms.UnitTestBase;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Unit tests for {@link BaseRestPortlet}.
 *
 * <p>Verifies that {@code WebApplicationException} thrown by a JSP (via the
 * {@code RequestDispatcher}) propagates out of {@code getJspResponse()} with its
 * HTTP status intact, while ordinary exceptions are caught and converted to an
 * error-HTML string.</p>
 */
@RunWith(MockitoJUnitRunner.class)
public class BaseRestPortletTest extends UnitTestBase {

    /** Minimal concrete subclass — BaseRestPortlet is abstract. */
    private static class TestPortlet extends BaseRestPortlet {
    }

    private TestPortlet portlet;
    private Method getJspResponse;
    private HttpServletRequest  mockRequest;
    private HttpServletResponse mockResponse;
    private RequestDispatcher   mockDispatcher;

    @Before
    public void setUp() throws Exception {
        portlet = new TestPortlet();

        // Expose the private getJspResponse method for white-box testing
        getJspResponse = BaseRestPortlet.class.getDeclaredMethod(
                "getJspResponse",
                HttpServletRequest.class,
                HttpServletResponse.class,
                String.class,
                String.class);
        getJspResponse.setAccessible(true);

        mockRequest    = mock(HttpServletRequest.class);
        mockResponse   = mock(HttpServletResponse.class);
        mockDispatcher = mock(RequestDispatcher.class);

        when(mockRequest.getRequestDispatcher(anyString())).thenReturn(mockDispatcher);
    }

    // -------------------------------------------------------------------------
    // WebApplicationException propagation
    // -------------------------------------------------------------------------

    @Test
    public void getJspResponse_propagates400WhenJspThrowsWebApplicationException()
            throws Exception {
        final WebApplicationException cause = new WebApplicationException(
                Response.status(Response.Status.BAD_REQUEST).entity("bad id").build());
        doThrow(cause).when(mockDispatcher).include(any(), any());

        try {
            getJspResponse.invoke(portlet, mockRequest, mockResponse, "rules", "include");
            fail("Expected WebApplicationException to propagate");
        } catch (final InvocationTargetException e) {
            assertTrue("Cause must be WebApplicationException",
                    e.getCause() instanceof WebApplicationException);
            assertEquals("HTTP status must be 400",
                    Response.Status.BAD_REQUEST.getStatusCode(),
                    ((WebApplicationException) e.getCause()).getResponse().getStatus());
        }
    }

    @Test
    public void getJspResponse_propagates404WhenJspThrowsWebApplicationException()
            throws Exception {
        final WebApplicationException cause = new WebApplicationException(
                Response.status(Response.Status.NOT_FOUND).entity("not found").build());
        doThrow(cause).when(mockDispatcher).include(any(), any());

        try {
            getJspResponse.invoke(portlet, mockRequest, mockResponse, "rules", "include");
            fail("Expected WebApplicationException to propagate");
        } catch (final InvocationTargetException e) {
            assertTrue("Cause must be WebApplicationException",
                    e.getCause() instanceof WebApplicationException);
            assertEquals("HTTP status must be 404",
                    Response.Status.NOT_FOUND.getStatusCode(),
                    ((WebApplicationException) e.getCause()).getResponse().getStatus());
        }
    }

    // -------------------------------------------------------------------------
    // Ordinary exceptions → error HTML (existing behaviour preserved)
    // -------------------------------------------------------------------------

    @Test
    public void getJspResponse_returnsErrorHtmlForGenericException() throws Exception {
        doThrow(new IOException("disk full")).when(mockDispatcher).include(any(), any());

        final Object result = getJspResponse.invoke(
                portlet, mockRequest, mockResponse, "rules", "include");

        assertTrue("Result must be a String", result instanceof String);
        final String html = (String) result;
        assertTrue("Error HTML must mention the JSP path", html.contains("rules"));
        assertTrue("Error HTML must include the exception message", html.contains("disk full"));
    }

    @Test
    public void getJspResponse_returnsErrorHtmlForRuntimeException() throws Exception {
        doThrow(new IllegalArgumentException("bad uuid"))
                .when(mockDispatcher).include(any(), any());

        final Object result = getJspResponse.invoke(
                portlet, mockRequest, mockResponse, "rules", "include");

        assertTrue("Result must be a String", result instanceof String);
        assertTrue("Error HTML must include the exception message",
                ((String) result).contains("bad uuid"));
    }
}
