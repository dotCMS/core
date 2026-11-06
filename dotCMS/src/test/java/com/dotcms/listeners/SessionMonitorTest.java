package com.dotcms.listeners;


import com.dotcms.api.system.event.SystemEvent;
import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.api.system.event.SystemEventType;
import com.dotcms.api.system.event.SystemEventsAPI;
import com.dotcms.api.system.event.UserSessionPayloadBuilder;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.exception.DotDataException;
import com.liferay.portal.util.WebKeys;
import org.junit.After;
import org.junit.Test;

import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionBindingEvent;
import javax.servlet.http.HttpSessionEvent;

import static com.dotcms.cms.login.LoginServiceAPIFactory.LOG_OUT_ATTRIBUTE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

public class SessionMonitorTest {

    /**
     * Method to Test: {@link SessionMonitor#sessionDestroyed(HttpSessionEvent)}
     * When: The session expired
     * Should: Send a {@link SystemEventType#SESSION_DESTROYED} notification
     */
    @Test
    public void testSessionDestroyedWithoutLogout() throws DotDataException {
        final HttpSession httpSession = mock(HttpSession.class);
        when(httpSession.getAttribute(com.liferay.portal.util.WebKeys.USER_ID)).thenReturn("user_id");
        when(httpSession.getAttribute(SessionMonitor.IGNORE_REMEMBER_ME_ON_INVALIDATION)).thenReturn(true);
        when(httpSession.getId()).thenReturn("session_id");

        final HttpSessionEvent event =  mock(HttpSessionEvent.class);
        when(event.getSession()).thenReturn(httpSession);

        final SystemEventsAPI systemEventsAPI = mock(SystemEventsAPI.class);

        final SessionMonitor sessionMonitor =  new SessionMonitor(systemEventsAPI);
        sessionMonitor.sessionDestroyed(event);

        final SystemEvent systemEvent = new SystemEvent
                (SystemEventType.SESSION_DESTROYED, UserSessionPayloadBuilder.build("user_id", "session_id"));
        verify(systemEventsAPI).push(systemEvent);

    }

    /**
     * Method to Test: {@link SessionMonitor#sessionDestroyed(HttpSessionEvent)}
     * When: The session expired after call the logout
     * Should: Not send a {@link SystemEventType#SESSION_DESTROYED} notification
     */
    @Test
    public void testSessionDestroyedWithLogout() throws DotDataException {
        final HttpSession httpSession = mock(HttpSession.class);
        when(httpSession.getAttribute(com.liferay.portal.util.WebKeys.USER_ID)).thenReturn("user_id");
        when(httpSession.getId()).thenReturn("session_id");
        when(httpSession.getAttribute(LOG_OUT_ATTRIBUTE)).thenReturn(true);
        when(httpSession.getAttribute(SessionMonitor.IGNORE_REMEMBER_ME_ON_INVALIDATION)).thenReturn(true);
        final HttpSessionEvent event =  mock(HttpSessionEvent.class);
        when(event.getSession()).thenReturn(httpSession);

        final SystemEventsAPI systemEventsAPI = mock(SystemEventsAPI.class);

        final SessionMonitor sessionMonitor =  new SessionMonitor(systemEventsAPI);
        sessionMonitor.sessionDestroyed(event);

        final SystemEvent systemEvent = new SystemEvent
                (SystemEventType.SESSION_LOGOUT,
                        UserSessionPayloadBuilder.build("user_id", "session_id"));
        verify(systemEventsAPI).push(systemEvent);
    }

    private static final String USER_A = "session_monitor_test_user_a";
    private static final String USER_B = "session_monitor_test_user_b";

    /**
     * The tracked map is static and shared by every test in the JVM, so each test ends the
     * sessions of the users it tracked. Invalidating a mock session is a no-op, so this only
     * removes the entries from the map.
     */
    @After
    public void untrackTestSessions() {
        SessionMonitor.endSessionsOf(USER_A);
        SessionMonitor.endSessionsOf(USER_B);
    }

    private static HttpSession trackedSession(final String sessionId, final String userId) {
        final HttpSession session = mock(HttpSession.class);
        when(session.getId()).thenReturn(sessionId);
        when(session.getAttribute(WebKeys.USER_ID)).thenReturn(userId);
        SessionMonitor.track(session);
        return session;
    }

    private static Map<String, HttpSession> trackedSessions() {
        return new SessionMonitor(mock(SystemEventsAPI.class)).getUserSessions();
    }

    /**
     * Method to Test: {@link SessionMonitor#track(HttpSession)}
     * When: a session that holds a USER_ID is registered at login time
     * Should: appear in getUserSessions() and carry the DOT_CLUSTER_SESSION flag
     */
    @Test
    public void testTrackRegistersAuthenticatedSession() {
        final HttpSession session = trackedSession("session_a", USER_A);

        assertTrue(trackedSessions().containsKey("session_a"));
        verify(session).setAttribute(SessionMonitor.DOT_CLUSTER_SESSION, true);
    }

    /**
     * Method to Test: {@link SessionMonitor#attributeAdded(HttpSessionBindingEvent)}
     * When: the USER_ID attribute is stored in a session (any login path)
     * Should: track the session at once; other attributes are ignored
     */
    @Test
    public void testAttributeAddedTracksTheSessionWhenTheUserIdIsStored() {
        final HttpSession session = mock(HttpSession.class);
        when(session.getId()).thenReturn("session_bound");
        when(session.getAttribute(WebKeys.USER_ID)).thenReturn(USER_A);
        final SessionMonitor sessionMonitor = new SessionMonitor(mock(SystemEventsAPI.class));

        sessionMonitor.attributeAdded(new HttpSessionBindingEvent(session, "SOME_OTHER_ATTRIBUTE", "x"));
        assertFalse(trackedSessions().containsKey("session_bound"));

        sessionMonitor.attributeAdded(new HttpSessionBindingEvent(session, WebKeys.USER_ID, USER_A));
        assertTrue(trackedSessions().containsKey("session_bound"));
    }

    /**
     * Method to Test: {@link SessionMonitor#track(HttpSession)}
     * When: the session belongs to the anonymous user
     * Should: be tracked but not flagged as a cluster session
     */
    @Test
    public void testTrackAnonymousSessionIsNotFlagged() {
        final HttpSession session = mock(HttpSession.class);
        when(session.getId()).thenReturn("session_anonymous");
        when(session.getAttribute(WebKeys.USER_ID)).thenReturn(UserAPI.CMS_ANON_USER_ID);
        try {
            SessionMonitor.track(session);

            assertTrue(trackedSessions().containsKey("session_anonymous"));
            verify(session, never()).setAttribute(eq(SessionMonitor.DOT_CLUSTER_SESSION), any());
        } finally {
            SessionMonitor.endSessionsOf(UserAPI.CMS_ANON_USER_ID);
        }
    }

    /**
     * Method to Test: {@link SessionMonitor#endSessionsOf(String)}
     * When: a tracked Login As session has the user as the administrator behind it
     * Should: end that session too
     */
    @Test
    public void testEndSessionsOfEndsLoginAsSessionsOfThePrincipal() {
        final HttpSession loginAs = trackedSession("session_login_as", USER_B);
        when(loginAs.getAttribute(WebKeys.PRINCIPAL_USER_ID)).thenReturn(USER_A);

        assertEquals(1, SessionMonitor.endSessionsOf(USER_A));
        verify(loginAs).invalidate();
        assertFalse(trackedSessions().containsKey("session_login_as"));
    }

    /**
     * Method to Test: {@link SessionMonitor#endSessionsOf(String)}
     * When: the user id differs from the tracked one only in case
     * Should: still end that user's session
     */
    @Test
    public void testEndSessionsOfMatchesUserIdIgnoringCase() {
        final HttpSession a1 = trackedSession("session_mixed_case", USER_A);

        assertEquals(1, SessionMonitor.endSessionsOf(USER_A.toUpperCase()));
        verify(a1).invalidate();
        assertFalse(trackedSessions().containsKey("session_mixed_case"));
    }

    /**
     * Method to Test: {@link SessionMonitor#track(HttpSession)}
     * When: the session has no USER_ID, or the session is null
     * Should: not be tracked and not throw
     */
    @Test
    public void testTrackIgnoresSessionWithoutUser() {
        final HttpSession anonymousSession = mock(HttpSession.class);
        when(anonymousSession.getId()).thenReturn("session_without_user");

        SessionMonitor.track(anonymousSession);
        SessionMonitor.track(null);

        assertFalse(trackedSessions().containsKey("session_without_user"));
        verify(anonymousSession, never()).setAttribute(eq(SessionMonitor.DOT_CLUSTER_SESSION), any());
    }

    /**
     * Method to Test: {@link SessionMonitor#endSessionsOf(String)}
     * When: two tracked sessions belong to user A and one to user B
     * Should: end only A's sessions (flag set, invalidate called, removed from the map), return 2, leave B tracked
     */
    @Test
    public void testEndSessionsOfEndsOnlyThatUsersSessions() {
        final HttpSession a1 = trackedSession("session_a1", USER_A);
        final HttpSession a2 = trackedSession("session_a2", USER_A);
        final HttpSession b1 = trackedSession("session_b1", USER_B);

        final int ended = SessionMonitor.endSessionsOf(USER_A);

        assertEquals(2, ended);
        for (final HttpSession session : new HttpSession[]{a1, a2}) {
            verify(session).setAttribute(SessionMonitor.IGNORE_REMEMBER_ME_ON_INVALIDATION, true);
            verify(session).invalidate();
        }
        final Map<String, HttpSession> tracked = trackedSessions();
        assertFalse(tracked.containsKey("session_a1"));
        assertFalse(tracked.containsKey("session_a2"));
        assertTrue(tracked.containsKey("session_b1"));
        verify(b1, never()).invalidate();
        verify(b1, never()).setAttribute(eq(SessionMonitor.IGNORE_REMEMBER_ME_ON_INVALIDATION), any());
    }

    /**
     * Method to Test: {@link SessionMonitor#endSessionsOf(String)}
     * When: no tracked session belongs to the user
     * Should: return 0 and invalidate nothing
     */
    @Test
    public void testEndSessionsOfUnknownUserEndsNothing() {
        final HttpSession b1 = trackedSession("session_b_only", USER_B);

        assertEquals(0, SessionMonitor.endSessionsOf("session_monitor_test_user_x"));
        verify(b1, never()).invalidate();
        assertTrue(trackedSessions().containsKey("session_b_only"));
    }

    /**
     * Method to Test: {@link SessionMonitor#endSessionsOf(String)}
     * When: the user id is null or blank
     * Should: return 0 and invalidate nothing
     */
    @Test
    public void testEndSessionsOfBlankUserIdEndsNothing() {
        final HttpSession a1 = trackedSession("session_blank_a", USER_A);
        final HttpSession b1 = trackedSession("session_blank_b", USER_B);

        assertEquals(0, SessionMonitor.endSessionsOf(null));
        assertEquals(0, SessionMonitor.endSessionsOf(""));
        verify(a1, never()).invalidate();
        verify(b1, never()).invalidate();
        assertTrue(trackedSessions().containsKey("session_blank_a"));
        assertTrue(trackedSessions().containsKey("session_blank_b"));
    }

    /**
     * Method to Test: {@link SessionMonitor#endSessionsOf(String)}
     * When: one of the user's tracked sessions was already ended (invalidate() throws IllegalStateException)
     * Should: still end the other session, drop the stale one from the map, and not throw
     */
    @Test
    public void testEndSessionsOfSkipsAlreadyEndedSession() {
        final HttpSession stale = trackedSession("session_stale", USER_A);
        // an invalidated session refuses every attribute read, which is the first call made on it
        when(stale.getAttribute(WebKeys.USER_ID)).thenThrow(new IllegalStateException("already invalidated"));
        final HttpSession a2 = trackedSession("session_live", USER_A);

        final int ended = SessionMonitor.endSessionsOf(USER_A);

        assertEquals(1, ended);
        verify(a2).invalidate();
        final Map<String, HttpSession> tracked = trackedSessions();
        assertFalse(tracked.containsKey("session_stale"));
        assertFalse(tracked.containsKey("session_live"));
    }

    /**
     * Method to Test: {@link SessionMonitor#attributeReplaced(HttpSessionBindingEvent)}
     * When: the USER_ID attribute of a session is replaced (a new login on an existing session)
     * Should: track the session; a null event and other attributes are ignored
     */
    @Test
    public void testAttributeReplacedTracksTheSessionWhenTheUserIdIsStored() {
        final HttpSession session = mock(HttpSession.class);
        when(session.getId()).thenReturn("session_rebound");
        when(session.getAttribute(WebKeys.USER_ID)).thenReturn(USER_A);
        final SessionMonitor sessionMonitor = new SessionMonitor(mock(SystemEventsAPI.class));

        sessionMonitor.attributeReplaced(null);
        sessionMonitor.attributeReplaced(new HttpSessionBindingEvent(session, "SOME_OTHER_ATTRIBUTE", "x"));
        assertFalse(trackedSessions().containsKey("session_rebound"));

        sessionMonitor.attributeReplaced(new HttpSessionBindingEvent(session, WebKeys.USER_ID, USER_A));
        assertTrue(trackedSessions().containsKey("session_rebound"));
    }

    /**
     * Method to Test: {@link SessionMonitor#attributeAdded(HttpSessionBindingEvent)}
     * When: the USER_ID attribute is stored while a request is in flight on the current thread
     * Should: record the request's remote address on the session, as the request listener does
     */
    @Test
    public void testAttributeAddedRecordsTheRemoteAddressOfTheCurrentRequest() {
        final HttpSession session = mock(HttpSession.class);
        when(session.getId()).thenReturn("session_addr");
        when(session.getAttribute(WebKeys.USER_ID)).thenReturn(USER_A);
        final HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn("10.0.0.7");
        final HttpServletRequest before = HttpServletRequestThreadLocal.INSTANCE.getRequest();
        HttpServletRequestThreadLocal.INSTANCE.setRequest(request);
        try {
            new SessionMonitor(mock(SystemEventsAPI.class))
                    .attributeAdded(new HttpSessionBindingEvent(session, WebKeys.USER_ID, USER_A));
        } finally {
            HttpServletRequestThreadLocal.INSTANCE.setRequest(before);
        }

        verify(session).setAttribute(SessionMonitor.USER_REMOTE_ADDR, "10.0.0.7");
        assertTrue(trackedSessions().containsKey("session_addr"));
    }

    /**
     * Method to Test: {@link SessionMonitor#endSessionsOf(String)}
     * When: ending one of the user's sessions fails with an unexpected error
     * Should: forget that session, still end the user's other sessions, and count only those
     */
    @Test
    public void testEndSessionsOfForgetsASessionThatFailsToEnd() {
        final HttpSession failing = trackedSession("session_failing", USER_A);
        doThrow(new RuntimeException("store unavailable")).when(failing).invalidate();
        final HttpSession live = trackedSession("session_live2", USER_A);

        final int ended = SessionMonitor.endSessionsOf(USER_A);

        assertEquals(1, ended);
        verify(live).invalidate();
        final Map<String, HttpSession> tracked = trackedSessions();
        assertFalse(tracked.containsKey("session_failing"));
        assertFalse(tracked.containsKey("session_live2"));
    }
}
