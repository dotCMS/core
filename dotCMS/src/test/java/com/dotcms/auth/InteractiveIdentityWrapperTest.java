package com.dotcms.auth;

import com.dotmarketing.business.UserAPI;
import com.liferay.portal.model.User;
import com.liferay.portal.util.WebKeys;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for {@link InteractiveIdentityWrapper}: the user keys of a wrapped request and session never
 * hold the built-in account, on write or on read. No database is involved.
 */
public class InteractiveIdentityWrapperTest {

    private static User user(final String id) {
        final User user = new User();
        user.setUserId(id);
        return user;
    }

    /**
     * Writing the built-in account to the user keys of the session is dropped, whatever the case.
     */
    @Test
    public void test_session_write_of_the_built_in_account_is_dropped() {
        final Map<String, Object> store = new HashMap<>();
        final HttpSession session = InteractiveIdentityWrapper.wrap(IdentityMocks.session(store));
        session.setAttribute(WebKeys.USER, user(UserAPI.SYSTEM_USER_ID));
        session.setAttribute(WebKeys.USER_ID, "SYSTEM");
        session.putValue(WebKeys.USER_ID, UserAPI.SYSTEM_USER_ID);
        assertTrue(store.isEmpty());
        assertNull(session.getAttribute(WebKeys.USER));
        assertNull(session.getAttribute(WebKeys.USER_ID));
    }

    /**
     * Writing the built-in account as a request attribute is dropped too: the token processors write there.
     */
    @Test
    public void test_request_write_of_the_built_in_account_is_dropped() {
        final Map<String, Object> store = new HashMap<>();
        final HttpServletRequest request = InteractiveIdentityWrapper.wrap(
                IdentityMocks.request(store, IdentityMocks.session(new HashMap<>())));
        request.setAttribute(WebKeys.USER, user(UserAPI.SYSTEM_USER_ID));
        request.setAttribute(WebKeys.USER_ID, UserAPI.SYSTEM_USER_ID);
        assertTrue(store.isEmpty());
        assertNull(request.getAttribute(WebKeys.USER));
    }

    /**
     * A real user and unrelated attributes pass through unchanged.
     */
    @Test
    public void test_real_user_and_other_attributes_pass_through() {
        final Map<String, Object> store = new HashMap<>();
        final HttpSession session = InteractiveIdentityWrapper.wrap(IdentityMocks.session(store));
        final User real = user("dotcms.org.1");
        session.setAttribute(WebKeys.USER, real);
        session.setAttribute(WebKeys.USER_ID, "dotcms.org.1");
        session.setAttribute("other", "value");
        assertSame(real, session.getAttribute(WebKeys.USER));
        assertEquals("dotcms.org.1", session.getAttribute(WebKeys.USER_ID));
        assertEquals("value", session.getAttribute("other"));
    }

    /**
     * A value already stored in the container session (written before or outside the wrapper) is hidden on
     * read, and its name is not listed either.
     */
    @Test
    public void test_pre_existing_value_is_hidden_on_read_and_in_the_names() {
        final Map<String, Object> store = new HashMap<>();
        store.put(WebKeys.USER, user(UserAPI.SYSTEM_USER_ID));
        store.put(WebKeys.USER_ID, UserAPI.SYSTEM_USER_ID);
        store.put("other", "value");
        final HttpSession session = InteractiveIdentityWrapper.wrap(IdentityMocks.session(store));
        assertNull(session.getAttribute(WebKeys.USER));
        assertNull(session.getValue(WebKeys.USER_ID));
        assertEquals("value", session.getAttribute("other"));
        assertEquals(Collections.singletonList("other"), Collections.list(session.getAttributeNames()));
    }

    /**
     * The session of a wrapped request is wrapped as well, so it cannot be used to write the account.
     */
    @Test
    public void test_session_of_a_wrapped_request_is_wrapped() {
        final Map<String, Object> sessionStore = new HashMap<>();
        final HttpServletRequest request = InteractiveIdentityWrapper.wrap(
                IdentityMocks.request(new HashMap<>(), IdentityMocks.session(sessionStore)));
        request.getSession().setAttribute(WebKeys.USER, user(UserAPI.SYSTEM_USER_ID));
        request.getSession(false).setAttribute(WebKeys.USER_ID, UserAPI.SYSTEM_USER_ID);
        assertFalse(sessionStore.containsKey(WebKeys.USER));
        assertFalse(sessionStore.containsKey(WebKeys.USER_ID));
    }

    /**
     * Asking the wrapped request for its session twice returns the same object, so identity-based use of
     * the session (equality, locking, map keys) keeps working.
     */
    @Test
    public void test_wrapped_request_returns_the_same_session_object_every_time() {
        final HttpServletRequest request = InteractiveIdentityWrapper.wrap(
                IdentityMocks.request(new HashMap<>(), IdentityMocks.session(new HashMap<>())));
        final HttpSession first = request.getSession();
        assertSame(first, request.getSession());
        assertSame(first, request.getSession(true));
        assertSame(first, request.getSession(false));
        assertEquals(first, request.getSession());
    }

    /** A new container session (the old one invalidated) gets a new wrapper, not the stale one. */
    @Test
    public void test_a_replaced_container_session_gets_a_fresh_wrapper() {
        final HttpSession old = IdentityMocks.session(new HashMap<>());
        final HttpSession replacement = IdentityMocks.session(new HashMap<>());
        final HttpServletRequest container = IdentityMocks.request(new HashMap<>(), old);
        final HttpServletRequest request = InteractiveIdentityWrapper.wrap(container);
        final HttpSession first = request.getSession();

        org.mockito.Mockito.when(container.getSession()).thenReturn(replacement);
        org.mockito.Mockito.when(container.getSession(org.mockito.ArgumentMatchers.anyBoolean()))
                .thenReturn(replacement);

        final HttpSession second = request.getSession();
        org.junit.Assert.assertNotSame(first, second);
        assertSame(second, request.getSession());
    }
}
