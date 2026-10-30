package org.apache.velocity.tools.view.context;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dotcms.UnitTestBase;
import com.dotcms.rendering.velocity.viewtools.VelocityRequestWrapper;
import com.dotcms.rendering.velocity.viewtools.VelocitySessionWrapper;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.apache.velocity.VelocityContext;
import org.junit.Test;

/**
 * Unit tests for the request, response and session that {@link ChainedContext} exposes to
 * templates.
 */
public class ChainedContextTest extends UnitTestBase {

    private ChainedContext newContext(final HttpServletRequest request, final HttpServletResponse response) {
        return new ChainedContext(new VelocityContext(), null, request, response);
    }

    /**
     * The session exposed as {@code $session} is the template-safe wrapper, so it does not
     * return the servlet context, while still delegating attribute access.
     */
    @Test
    public void session_is_exposed_through_the_wrapper() {
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final HttpSession session = mock(HttpSession.class);
        when(request.getSession(false)).thenReturn(session);
        when(request.getSession()).thenReturn(session);
        when(session.getServletContext()).thenReturn(mock(ServletContext.class));
        when(session.getAttribute("visitor")).thenReturn("value");

        final ChainedContext context = newContext(request, mock(HttpServletResponse.class));
        final Object exposed = context.internalGet(ViewContext.SESSION);

        assertTrue(exposed instanceof VelocitySessionWrapper);
        assertNull(((HttpSession) exposed).getServletContext());
        assertEquals("value", ((HttpSession) exposed).getAttribute("visitor"));
        assertEquals("value", context.getAttribute("visitor"));
    }

    /**
     * With no session on the request, none is exposed and none is created.
     */
    @Test
    public void no_session_is_exposed_when_the_request_has_none() {
        final HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getSession(false)).thenReturn(null);

        final ChainedContext context = newContext(request, mock(HttpServletResponse.class));

        assertNull(context.internalGet(ViewContext.SESSION));
    }

    /**
     * The request is exposed through the template-safe wrapper and the response as given;
     * the servlet context is never exposed.
     */
    @Test
    public void request_is_wrapped_and_servlet_context_is_not_exposed() {
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final HttpServletResponse response = mock(HttpServletResponse.class);
        when(request.getParameter("q")).thenReturn("term");

        final ChainedContext context = newContext(request, response);
        final Object exposedRequest = context.internalGet(ViewContext.REQUEST);

        assertTrue(exposedRequest instanceof VelocityRequestWrapper);
        assertEquals("term", ((HttpServletRequest) exposedRequest).getParameter("q"));
        assertNull(((HttpServletRequest) exposedRequest).getServletContext());
        assertSame(response, context.internalGet(ViewContext.RESPONSE));
        assertNull(context.getServletContext());
    }
}
