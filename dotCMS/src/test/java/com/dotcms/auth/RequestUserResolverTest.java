package com.dotcms.auth;

import com.dotmarketing.business.UserAPI;
import com.liferay.portal.model.User;
import com.liferay.portal.util.WebKeys;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.never;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

/**
 * Unit tests for {@link RequestUserResolver#resolveRequestUser}: a request or session that holds the built-in
 * account yields no user and nothing is written back; a server-side scope is the only way to get that account.
 * Sources that need the database (stored user id, token header) are covered by the integration tests.
 */
public class RequestUserResolverTest {

    private static User user(final String id) {
        final User user = new User();
        user.setUserId(id);
        return user;
    }

    /**
     * With a server-side scope open, the resolver answers with the built-in account and never reads the request.
     */
    @Test
    public void test_open_scope_is_the_source_and_the_request_is_not_read() {
        final HttpServletRequest request = org.mockito.Mockito.mock(HttpServletRequest.class);
        try (org.mockito.MockedStatic<com.dotmarketing.business.APILocator> locator =
                     org.mockito.Mockito.mockStatic(com.dotmarketing.business.APILocator.class);
             SystemUserScope ignored = SystemUserScope.open()) {
            locator.when(com.dotmarketing.business.APILocator::systemUser).thenReturn(user(UserAPI.SYSTEM_USER_ID));
            final User resolved = RequestUserResolver.resolveRequestUser(request);
            assertEquals(UserAPI.SYSTEM_USER_ID, resolved.getUserId());
        }
        verifyNoInteractions(request);
    }

    /**
     * Without a scope, a request attribute carrying the built-in account is refused and nothing is written.
     */
    @Test
    public void test_request_attribute_with_the_built_in_account_is_refused() {
        final Map<String, Object> attributes = new HashMap<>();
        attributes.put(WebKeys.USER, user(UserAPI.SYSTEM_USER_ID));
        final HttpServletRequest request = IdentityMocks.request(attributes, null);
        assertNull(RequestUserResolver.resolveRequestUser(request));
        verify(request, never()).setAttribute(anyString(), any());
    }

    /**
     * A cached session user that is the built-in account is refused and nothing is written back.
     */
    @Test
    public void test_cached_session_user_with_the_built_in_account_is_refused() {
        final Map<String, Object> sessionStore = new HashMap<>();
        sessionStore.put(WebKeys.USER, user(UserAPI.SYSTEM_USER_ID));
        final Map<String, Object> before = new HashMap<>(sessionStore);
        final javax.servlet.http.HttpSession session = IdentityMocks.session(sessionStore);
        final HttpServletRequest request = IdentityMocks.request(new HashMap<>(), session);
        assertNull(RequestUserResolver.resolveRequestUser(request));
        verify(request, never()).setAttribute(anyString(), any());
        verify(session, never()).setAttribute(anyString(), any());
        assertEquals(before, sessionStore);
    }

    /**
     * A cached real user wins over a stored id naming the built-in account.
     */
    @Test
    public void test_cached_real_user_wins_over_a_stored_built_in_id() {
        final User real = user("dotcms.org.1");
        final Map<String, Object> sessionStore = new HashMap<>();
        sessionStore.put(WebKeys.USER, real);
        sessionStore.put(WebKeys.USER_ID, UserAPI.SYSTEM_USER_ID);
        final HttpServletRequest request = IdentityMocks.request(new HashMap<>(), IdentityMocks.session(sessionStore));
        assertSame(real, RequestUserResolver.resolveRequestUser(request));
    }

    /**
     * Without a scope, a real user on the request is returned unchanged, and an attribute never acts as a scope.
     */
    @Test
    public void test_real_user_on_the_request_is_returned_and_no_scope_is_implied() {
        final User real = user("dotcms.org.1");
        final Map<String, Object> attributes = new HashMap<>();
        attributes.put(WebKeys.USER, real);
        final HttpServletRequest request = IdentityMocks.request(attributes, null);
        assertSame(real, RequestUserResolver.resolveRequestUser(request));
        assertTrue(!SystemUserScope.isActive());
    }
}
