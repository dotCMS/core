package com.dotcms.rendering.velocity.servlet;

import com.dotcms.util.SecurityUtils;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.model.User;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for how {@link VelocityServlet} answers a page the current user may not read
 * ({@link VelocityServlet#handleSecurityException(User, HttpServletRequest, HttpServletResponse)}, issue #36591).
 *
 * A signed-out visitor gets a 401, as before, and the page's query string is now saved in the session
 * ({@link SecurityUtils#REDIRECT_AFTER_LOGIN_QUERY_STRING}). The 401 error page that runs next can no longer see
 * it, and an SSO login needs it to return the visitor to the full URL. A signed-in user gets a 403, as before, and
 * any saved query string is cleared. Nothing else changes: the error page still records the page path in
 * {@link WebKeys#REDIRECT_AFTER_LOGIN}.
 */
public class VelocityServletSecurityHandlingTest {

    private User anonymous;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private HttpSession session;

    @Before
    public void setUp() {

        anonymous = mock(User.class);
        request   = mock(HttpServletRequest.class);
        response  = mock(HttpServletResponse.class);
        session   = mock(HttpSession.class);
        when(request.getSession()).thenReturn(session);
        when(request.getSession(false)).thenReturn(session);
        when(response.isCommitted()).thenReturn(false);
    }

    /**
     * Given Scenario: the anonymous user requests a protected page with ?id=42 (forwarded to the servlet)
     * ExpectedResult: the query string is saved, then a 401 is sent as before
     */
    @Test
    public void anonymous_savesQueryString_andSends401() throws Exception {

        when(request.getAttribute(RequestDispatcher.FORWARD_REQUEST_URI)).thenReturn("/members/report");
        when(request.getAttribute(RequestDispatcher.FORWARD_QUERY_STRING)).thenReturn("id=42");

        handle(anonymous);

        verify(session).setAttribute(SecurityUtils.REDIRECT_AFTER_LOGIN_QUERY_STRING, "id=42");
        verify(session).setAttribute(SecurityUtils.REDIRECT_AFTER_LOGIN_QUERY_PATH, "/members/report");
        verify(response).sendError(HttpServletResponse.SC_UNAUTHORIZED);
    }

    /**
     * Given Scenario: no user could be resolved
     * ExpectedResult: treated as signed out, a 401 as before
     */
    @Test
    public void nullUser_isTreatedAsAnonymous() throws Exception {

        handle(null);

        verify(response).sendError(HttpServletResponse.SC_UNAUTHORIZED);
    }

    /**
     * Given Scenario: the anonymous user requests a protected page without a query string
     * ExpectedResult: any stale saved query string is removed, then a 401 is sent
     */
    @Test
    public void anonymous_withoutQuery_removesSavedQueryString() throws Exception {

        handle(anonymous);

        verify(session).removeAttribute(SecurityUtils.REDIRECT_AFTER_LOGIN_QUERY_STRING);
        verify(response).sendError(HttpServletResponse.SC_UNAUTHORIZED);
    }

    /**
     * Given Scenario: a signed-in user lacks READ on the page
     * ExpectedResult: a 403 as before; the saved query string is cleared and REDIRECT_AFTER_LOGIN is not touched
     * (the error page's 403 branch keeps handling it)
     */
    @Test
    public void signedIn_clearsSavedQueryString_andSends403() throws Exception {

        handle(mock(User.class));

        verify(session).removeAttribute(SecurityUtils.REDIRECT_AFTER_LOGIN_QUERY_STRING);
        verify(session).removeAttribute(SecurityUtils.REDIRECT_AFTER_LOGIN_QUERY_PATH);
        verify(session, never()).removeAttribute(WebKeys.REDIRECT_AFTER_LOGIN);
        verify(session, never()).setAttribute(Mockito.eq(WebKeys.REDIRECT_AFTER_LOGIN), Mockito.any());
        verify(response).sendError(HttpServletResponse.SC_FORBIDDEN);
    }

    /**
     * Given Scenario: the response is already committed
     * ExpectedResult: nothing is written
     */
    @Test
    public void committedResponse_isNoOp() throws Exception {

        when(response.isCommitted()).thenReturn(true);

        handle(anonymous);

        verify(response, never()).sendError(Mockito.anyInt());
    }

    private void handle(final User user) throws Exception {

        final UserAPI userAPI = mock(UserAPI.class);
        when(userAPI.getAnonymousUserNoThrow()).thenReturn(anonymous);
        try (MockedStatic<APILocator> apiLocator = mockStatic(APILocator.class)) {
            apiLocator.when(APILocator::getUserAPI).thenReturn(userAPI);
            VelocityServlet.handleSecurityException(user, request, response);
        }
    }
}
