package com.dotmarketing.servlets.ajax;

import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.dotcms.UnitTestBase;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.exception.SecurityException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;
import org.junit.Test;

/** Authentication failures during Ajax initialization must keep their HTTP status, not become 500s. */
public class AjaxDirectorServletTest extends UnitTestBase {

    private static final String PATH = "/DotAjax/" + DeniedAjaxAction.class.getName()
            + "/user/actor/password/reusable-secret";

    /** A URL login rejected by WebResource must remain an unauthorized response at the servlet. */
    @Test
    public void deniedPathLoginReturnsUnauthorized() throws Exception {
        assertDeniedStatus(Response.Status.UNAUTHORIZED);
    }

    /** SSL or permission denials retain their forbidden status rather than becoming 401 or 500. */
    @Test
    public void forbiddenAuthenticationReturnsForbidden() throws Exception {
        assertDeniedStatus(Response.Status.FORBIDDEN);
    }

    /** Unexpected initialization failures still use the existing internal-server-error handler. */
    @Test
    public void unexpectedFailureStillReturnsInternalServerError() throws Exception {
        final HttpServletRequest request = request();
        final HttpServletResponse response = mock(HttpServletResponse.class);
        final WebResource webResource = mock(WebResource.class);
        when(webResource.init(PATH, true, request, true, null))
                .thenThrow(new IllegalStateException("Unexpected initialization failure"));

        new AjaxDirectorServlet(webResource).service(request, response);

        verify(response).sendError(500, "Unexpected initialization failure");
        verifyNoMoreInteractions(response);
    }

    /** Exercises the real reflective Ajax initialization without a live database or servlet container. */
    private void assertDeniedStatus(final Response.Status status) throws Exception {
        final HttpServletRequest request = request();
        final HttpServletResponse response = mock(HttpServletResponse.class);
        final WebResource webResource = mock(WebResource.class);
        when(webResource.init(PATH, true, request, true, null))
                .thenThrow(new SecurityException("Denied authentication", status));

        new AjaxDirectorServlet(webResource).service(request, response);

        verify(response).sendError(status.getStatusCode());
        verifyNoMoreInteractions(response);
    }

    /** Supplies a path-credential request with no body. */
    private HttpServletRequest request() throws IOException {
        final HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn(PATH);
        when(request.getReader()).thenReturn(new BufferedReader(new StringReader("")));
        return request;
    }

    /** Public fixture so the servlet can instantiate it through its existing reflection path. */
    public static class DeniedAjaxAction extends AjaxAction {

        /** The servlet injects WebResource before initialization; no global API lookup is needed. */
        public DeniedAjaxAction() {
            super(null);
        }

        /** Denied initialization must prevent command execution. */
        @Override
        public void action(final HttpServletRequest request, final HttpServletResponse response) {
            fail("A denied Ajax request must not execute an action");
        }

        /** Denied initialization must prevent HTTP-method dispatch too. */
        @Override
        public void service(final HttpServletRequest request, final HttpServletResponse response) {
            fail("A denied Ajax request must not reach service dispatch");
        }
    }
}
