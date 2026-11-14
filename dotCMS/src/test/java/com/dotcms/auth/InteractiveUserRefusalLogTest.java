package com.dotcms.auth;

import com.dotmarketing.business.UserAPI;
import com.dotmarketing.util.SecurityLogger;
import com.liferay.portal.model.User;
import com.liferay.portal.util.WebKeys;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for the security-log entry written when an identity is refused: it is written at the first
 * refusal point a session reaches, once per session, with no session identifier or credential in it, and
 * the dedup state lives outside the session.
 */
public class InteractiveUserRefusalLogTest {

    private static final String SESSION_ID = "SESSION-ID-1234567890";

    private MockedStatic<SecurityLogger> securityLogger;
    private final List<String> entries = new ArrayList<>();

    @Before
    public void capture() {
        InteractiveUserRefusalLog.resetForTests();
        securityLogger = Mockito.mockStatic(SecurityLogger.class);
        securityLogger.when(() -> SecurityLogger.logWarn(Mockito.any(Class.class), Mockito.anyString()))
                .thenAnswer(i -> entries.add(i.getArgument(1)));
        securityLogger.when(() -> SecurityLogger.logInfo(Mockito.any(Class.class), Mockito.anyString()))
                .thenAnswer(i -> entries.add(i.getArgument(1)));
    }

    @After
    public void release() {
        securityLogger.close();
        InteractiveUserRefusalLog.resetForTests();
    }

    private static User system() {
        final User user = new User();
        user.setUserId(UserAPI.SYSTEM_USER_ID);
        return user;
    }

    private static HttpSession session(final String id, final Map<String, Object> store) {
        final HttpSession session = IdentityMocks.session(store);
        Mockito.when(session.getId()).thenReturn(id);
        return session;
    }

    /** Many refused writes on one session produce exactly one entry. */
    @Test
    public void test_many_dropped_writes_on_one_session_produce_one_entry() {
        final HttpSession session = InteractiveIdentityWrapper.wrap(session(SESSION_ID, new HashMap<>()));
        for (int i = 0; i < 10; i++) {
            session.setAttribute(WebKeys.USER, system());
            session.setAttribute(WebKeys.USER_ID, UserAPI.SYSTEM_USER_ID);
        }
        assertEquals(1, entries.size());
    }

    /** The entry is written at the first refusal point; later points on the same session add nothing. */
    @Test
    public void test_first_refusal_point_wins_across_write_read_and_resolver() {
        final Map<String, Object> store = new HashMap<>();
        final HttpSession container = session(SESSION_ID, store);
        final HttpServletRequest request = IdentityMocks.request(new HashMap<>(), container);
        final HttpServletRequest wrapped = InteractiveIdentityWrapper.wrap(request);
        store.put(WebKeys.USER, system());

        wrapped.getSession().getAttribute(WebKeys.USER);
        RequestUserResolver.resolveRequestUser(wrapped);
        wrapped.getSession().setAttribute(WebKeys.USER, system());

        assertEquals(1, entries.size());
    }

    /** Two sessions produce two entries. */
    @Test
    public void test_two_sessions_produce_two_entries() {
        InteractiveIdentityWrapper.wrap(session("SESSION-A-0001", new HashMap<>()))
                .setAttribute(WebKeys.USER, system());
        InteractiveIdentityWrapper.wrap(session("SESSION-B-0002", new HashMap<>()))
                .setAttribute(WebKeys.USER, system());
        assertEquals(2, entries.size());
    }

    /** With no session, every refused request produces its own entry. */
    @Test
    public void test_no_session_produces_one_entry_per_request() {
        for (int i = 0; i < 3; i++) {
            final Map<String, Object> attributes = new HashMap<>();
            attributes.put(WebKeys.USER, system());
            RequestUserResolver.resolveRequestUser(IdentityMocks.request(attributes, null));
        }
        assertEquals(3, entries.size());
    }

    /** A resolve that simply finds no user is not a refusal and writes nothing. */
    @Test
    public void test_resolving_no_user_logs_nothing() {
        RequestUserResolver.resolveRequestUser(IdentityMocks.request(new HashMap<>(), session(SESSION_ID, new HashMap<>())));
        assertTrue(entries.isEmpty());
    }

    /** The entry has the agreed text and contains neither the session id nor a credential marker. */
    @Test
    public void test_entry_text_has_no_session_id_and_no_credential() {
        final Map<String, Object> attributes = new HashMap<>();
        attributes.put(WebKeys.USER, system());
        final HttpSession container = session(SESSION_ID, new HashMap<>());
        final HttpServletRequest request = IdentityMocks.request(attributes, container);
        RequestUserResolver.resolveRequestUser(request);

        assertEquals(1, entries.size());
        final String entry = entries.get(0);
        assertTrue(entry.contains("user not eligible for interactive use"));
        assertFalse(entry.contains(SESSION_ID));
        assertFalse(entry.toLowerCase().contains("bearer"));
    }

    /** The set of tracked sessions is bounded. */
    @Test
    public void test_tracked_sessions_never_grow_past_the_maximum() {
        final int max = InteractiveUserRefusalLog.maxTrackedSessions();
        assertTrue("maximum must be a positive bound", max > 0);
        for (int i = 0; i < max + 50; i++) {
            InteractiveUserRefusalLog.record(null, session("SESSION-" + i, new HashMap<>()));
        }
        assertTrue(InteractiveUserRefusalLog.trackedSessions() <= max);
    }

    /** Nothing is written to the session to count entries. */
    @Test
    public void test_session_attributes_are_not_modified_by_logging() {
        final Map<String, Object> store = new HashMap<>();
        store.put("other", "value");
        final HttpSession container = session(SESSION_ID, store);
        final Map<String, Object> before = new HashMap<>(store);
        final HttpSession wrapped = InteractiveIdentityWrapper.wrap(container);
        wrapped.setAttribute(WebKeys.USER, system());
        wrapped.setAttribute(WebKeys.USER, system());
        assertEquals(before, store);
        Mockito.verify(container, Mockito.never()).setAttribute(Mockito.eq(WebKeys.USER), Mockito.any());
        assertNull(wrapped.getAttribute(WebKeys.USER));
    }
}
