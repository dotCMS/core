package com.dotcms.listeners;


import com.dotcms.api.system.event.SystemEvent;
import com.dotcms.api.system.event.SystemEventType;
import com.dotcms.api.system.event.SystemEventsAPI;
import com.dotcms.api.system.event.UserSessionPayloadBuilder;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.rules.model.ConditionGroup;
import com.liferay.portal.model.User;
import org.junit.Test;

import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionEvent;

import static com.dotcms.cms.login.LoginServiceAPIFactory.LOG_OUT_ATTRIBUTE;
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
}
