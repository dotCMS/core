package com.dotcms.cms.login;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.dotmarketing.business.LoginAsSession;
import com.dotmarketing.business.NoSuchUserException;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.Config;
import com.liferay.portal.model.User;
import com.liferay.portal.util.WebKeys;
import java.util.Date;
import java.util.Optional;
import javax.servlet.http.HttpSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the resolution of the user that an HTTP session belongs to. The resolver reads the
 * user cached in the session, or the user id stored in it, and re-checks the user's current status
 * through the user API on every call, so a session only ever yields a user that still exists and is
 * active.
 */
class SessionUserResolverTest {

    private static final String USER_ID = "user_a";

    private UserAPI userAPI;
    private HttpSession session;
    private SessionUserResolver resolver;
    private boolean inactiveLoginsAllowedBefore;

    @BeforeEach
    void setUp() {
        userAPI = mock(UserAPI.class);
        session = mock(HttpSession.class);
        resolver = new SessionUserResolver(userAPI);
        inactiveLoginsAllowedBefore = Config.getBooleanProperty(
                SessionUserResolver.ALLOW_INACTIVE_ACCOUNTS_TO_LOGIN, false);
    }

    @AfterEach
    void restoreConfig() {
        Config.setProperty(SessionUserResolver.ALLOW_INACTIVE_ACCOUNTS_TO_LOGIN, inactiveLoginsAllowedBefore);
    }

    private static User user(final String userId, final boolean active) {
        final User user = new User();
        user.setUserId(userId);
        user.setActive(active);
        return user;
    }

    @Test
    void nullSession_resolvesToNoUser() {
        final Optional<User> resolved = resolver.resolve(null);

        assertFalse(resolved.isPresent());
        verifyNoInteractions(userAPI);
    }

    @Test
    void sessionWithoutUserId_resolvesToNoUser() {
        final Optional<User> resolved = resolver.resolve(session);

        assertFalse(resolved.isPresent());
        verifyNoInteractions(userAPI);
        verify(session, never()).setAttribute(eq(WebKeys.USER), any());
    }

    @Test
    void userIdOnly_activeUser_isResolvedAndCached() throws DotDataException, DotSecurityException {
        final User activeUser = user(USER_ID, true);
        when(session.getAttribute(WebKeys.USER_ID)).thenReturn(USER_ID);
        when(userAPI.loadUserById(USER_ID)).thenReturn(activeUser);

        final Optional<User> resolved = resolver.resolve(session);

        assertTrue(resolved.isPresent());
        assertSame(activeUser, resolved.get());
        verify(session).setAttribute(WebKeys.USER, activeUser);
    }

    @Test
    void cachedUser_notActiveAnyMore_resolvesToNoUserAndDropsCachedCopy() throws DotDataException, DotSecurityException {
        final User cachedCopy = user(USER_ID, true);
        when(session.getAttribute(WebKeys.USER)).thenReturn(cachedCopy);
        when(session.getAttribute(WebKeys.USER_ID)).thenReturn(USER_ID);
        when(userAPI.loadUserById(USER_ID)).thenReturn(user(USER_ID, false));

        final Optional<User> resolved = resolver.resolve(session);

        assertFalse(resolved.isPresent());
        verify(session).removeAttribute(WebKeys.USER);
        verify(session).removeAttribute(WebKeys.USER_ID);
        verify(session, never()).setAttribute(eq(WebKeys.USER), any());
    }

    @Test
    void inactiveUser_whenInactiveLoginsAllowed_resolvesToUser() throws DotDataException, DotSecurityException {
        final User inactiveRecord = user(USER_ID, false);
        when(session.getAttribute(WebKeys.USER_ID)).thenReturn(USER_ID);
        when(userAPI.loadUserById(USER_ID)).thenReturn(inactiveRecord);
        Config.setProperty(SessionUserResolver.ALLOW_INACTIVE_ACCOUNTS_TO_LOGIN, true);

        final Optional<User> resolved = resolver.resolve(session);

        assertTrue(resolved.isPresent());
        assertSame(inactiveRecord, resolved.get());
        verify(session, never()).removeAttribute(WebKeys.USER);
    }

    @Test
    void inactiveUser_whenInactiveLoginsNotAllowed_resolvesToNoUser() throws DotDataException, DotSecurityException {
        when(session.getAttribute(WebKeys.USER_ID)).thenReturn(USER_ID);
        when(userAPI.loadUserById(USER_ID)).thenReturn(user(USER_ID, false));
        Config.setProperty(SessionUserResolver.ALLOW_INACTIVE_ACCOUNTS_TO_LOGIN, false);

        assertFalse(resolver.resolve(session).isPresent());
        verify(session).removeAttribute(WebKeys.USER);
    }

    @Test
    void cachedUserOnly_withoutUserId_isResolvedByTheCachedUsersId() throws DotDataException, DotSecurityException {
        final User cachedCopy = user(USER_ID, true);
        final User currentRecord = user(USER_ID, true);
        when(session.getAttribute(WebKeys.USER)).thenReturn(cachedCopy);
        when(userAPI.loadUserById(USER_ID)).thenReturn(currentRecord);

        final Optional<User> resolved = resolver.resolve(session);

        assertTrue(resolved.isPresent());
        assertSame(currentRecord, resolved.get());
    }

    @Test
    void blankUserId_resolvesToNoUser() {
        when(session.getAttribute(WebKeys.USER_ID)).thenReturn("");

        assertFalse(resolver.resolve(session).isPresent());
        verifyNoInteractions(userAPI);
    }

    @Test
    void lookupReturningNoRecord_resolvesToNoUserAndDropsCachedCopy() throws DotDataException, DotSecurityException {
        when(session.getAttribute(WebKeys.USER)).thenReturn(user(USER_ID, true));
        when(session.getAttribute(WebKeys.USER_ID)).thenReturn(USER_ID);
        when(userAPI.loadUserById(USER_ID)).thenReturn(null);

        assertFalse(resolver.resolve(session).isPresent());
        verify(session).removeAttribute(WebKeys.USER);
    }

    @Test
    void cachedUser_equivalentCopyOfCurrentRecord_isNotStoredAgain() throws DotDataException, DotSecurityException {
        final User cachedCopy = user(USER_ID, true);
        cachedCopy.setModificationDate(new Date(1_000L));
        final User currentRecord = user(USER_ID, true);
        currentRecord.setModificationDate(new Date(1_000L));
        when(session.getAttribute(WebKeys.USER)).thenReturn(cachedCopy);
        when(session.getAttribute(WebKeys.USER_ID)).thenReturn(USER_ID);
        when(userAPI.loadUserById(USER_ID)).thenReturn(currentRecord);

        assertSame(currentRecord, resolver.resolve(session).get());
        verify(session, never()).setAttribute(eq(WebKeys.USER), any());
    }

    @Test
    void cachedUser_sameInstanceAsCurrentRecord_isNotStoredAgain() throws DotDataException, DotSecurityException {
        final User sameInstance = user(USER_ID, true);
        when(session.getAttribute(WebKeys.USER)).thenReturn(sameInstance);
        when(session.getAttribute(WebKeys.USER_ID)).thenReturn(USER_ID);
        when(userAPI.loadUserById(USER_ID)).thenReturn(sameInstance);

        assertSame(sameInstance, resolver.resolve(session).get());
        verify(session, never()).setAttribute(eq(WebKeys.USER), any());
    }

    @Test
    void resolveActive_blankOrNullId_resolvesToNoUserWithoutLookup() {
        assertFalse(resolver.resolveActive(null).isPresent());
        assertFalse(resolver.resolveActive("").isPresent());
        verifyNoInteractions(userAPI);
    }

    @Test
    void resolveActive_activeUser_isResolved() throws DotDataException, DotSecurityException {
        final User activeUser = user(USER_ID, true);
        when(userAPI.loadUserById(USER_ID)).thenReturn(activeUser);

        assertSame(activeUser, resolver.resolveActive(USER_ID).get());
    }

    @Test
    void resolveActive_inactiveUser_resolvesToNoUser() throws DotDataException, DotSecurityException {
        when(userAPI.loadUserById(USER_ID)).thenReturn(user(USER_ID, false));

        assertFalse(resolver.resolveActive(USER_ID).isPresent());
    }

    @Test
    void resolveActive_missingUser_resolvesToNoUser() throws DotDataException, DotSecurityException {
        when(userAPI.loadUserById(USER_ID)).thenThrow(new NoSuchUserException(USER_ID));

        assertFalse(resolver.resolveActive(USER_ID).isPresent());
    }

    @Test
    void resolveActive_lookupError_resolvesToNoUser() throws DotDataException, DotSecurityException {
        when(userAPI.loadUserById(USER_ID)).thenThrow(new DotDataException("lookup failed"));

        assertFalse(resolver.resolveActive(USER_ID).isPresent());
    }

    @Test
    void cachedUser_noLongerExists_resolvesToNoUserAndDropsCachedCopy() throws DotDataException, DotSecurityException {
        when(session.getAttribute(WebKeys.USER)).thenReturn(user(USER_ID, true));
        when(session.getAttribute(WebKeys.USER_ID)).thenReturn(USER_ID);
        when(userAPI.loadUserById(USER_ID)).thenThrow(new NoSuchUserException(USER_ID));

        final Optional<User> resolved = resolver.resolve(session);

        assertFalse(resolved.isPresent());
        verify(session).removeAttribute(WebKeys.USER);
        verify(session).removeAttribute(WebKeys.USER_ID);
        verify(session, never()).setAttribute(eq(WebKeys.USER), any());
    }

    @Test
    void cachedUser_stillActive_resolvesToLiveUser() throws DotDataException, DotSecurityException {
        final User staleCopy = user(USER_ID, true);
        staleCopy.setModificationDate(new Date(1_000L));
        final User currentRecord = user(USER_ID, true);
        currentRecord.setModificationDate(new Date(2_000L));
        when(session.getAttribute(WebKeys.USER)).thenReturn(staleCopy);
        when(session.getAttribute(WebKeys.USER_ID)).thenReturn(USER_ID);
        when(userAPI.loadUserById(USER_ID)).thenReturn(currentRecord);

        final Optional<User> resolved = resolver.resolve(session);

        assertTrue(resolved.isPresent());
        assertSame(currentRecord, resolved.get());
        verify(session).setAttribute(WebKeys.USER, currentRecord);
        verify(session, never()).removeAttribute(WebKeys.USER);
    }

    @Test
    void cachedUser_lookupError_resolvesToNoUserForThisCall() throws DotDataException, DotSecurityException {
        when(session.getAttribute(WebKeys.USER)).thenReturn(user(USER_ID, true));
        when(session.getAttribute(WebKeys.USER_ID)).thenReturn(USER_ID);
        when(userAPI.loadUserById(USER_ID)).thenThrow(new DotDataException("lookup failed"));

        final Optional<User> resolved = resolver.resolve(session);

        assertFalse(resolved.isPresent());
        verify(session).removeAttribute(WebKeys.USER);
        verify(session, never()).removeAttribute(WebKeys.USER_ID);
        verify(session, never()).setAttribute(any(), any());
    }

    @Test
    void loginAsSession_principalNotActiveAnyMore_resolvesToNoUser() throws DotDataException, DotSecurityException {
        when(session.getAttribute(WebKeys.USER_ID)).thenReturn(USER_ID);
        when(session.getAttribute(WebKeys.PRINCIPAL_USER_ID)).thenReturn("admin_a");
        when(userAPI.loadUserById(USER_ID)).thenReturn(user(USER_ID, true));
        when(userAPI.loadUserById("admin_a")).thenReturn(user("admin_a", false));

        assertFalse(resolver.resolve(session).isPresent());
        verify(session).removeAttribute(WebKeys.USER);
        verify(session).removeAttribute(WebKeys.USER_ID);
        verify(session).removeAttribute(WebKeys.PRINCIPAL_USER_ID);
        verify(session).removeAttribute(LoginAsSession.SESSION_KEY);
    }

    @Test
    void loginAsSession_principalActive_resolvesToImpersonatedUser() throws DotDataException, DotSecurityException {
        final User impersonated = user(USER_ID, true);
        when(session.getAttribute(WebKeys.USER_ID)).thenReturn(USER_ID);
        when(session.getAttribute(WebKeys.PRINCIPAL_USER_ID)).thenReturn("admin_a");
        when(userAPI.loadUserById(USER_ID)).thenReturn(impersonated);
        when(userAPI.loadUserById("admin_a")).thenReturn(user("admin_a", true));

        assertSame(impersonated, resolver.resolve(session).get());
    }

    @Test
    void anonymousUserId_isResolvedLikeAnyActiveUser() throws DotDataException, DotSecurityException {
        final User anonymous = user(UserAPI.CMS_ANON_USER_ID, true);
        when(session.getAttribute(WebKeys.USER_ID)).thenReturn(UserAPI.CMS_ANON_USER_ID);
        when(userAPI.loadUserById(UserAPI.CMS_ANON_USER_ID)).thenReturn(anonymous);

        final Optional<User> resolved = resolver.resolve(session);

        assertTrue(resolved.isPresent());
        assertSame(anonymous, resolved.get());
    }

    /**
     * The user id stored at login names the session user; a {@link WebKeys#USER} copy of another
     * user (left by an earlier login on the same session) is replaced by the session user's record.
     */
    @Test
    void cachedUser_ofAnotherUserThanTheSessionUserId_isReplacedByTheSessionUsersRecord() throws DotDataException, DotSecurityException {
        final User otherUsersCopy = user("user_b", true);
        final User currentRecord = user(USER_ID, true);
        when(session.getAttribute(WebKeys.USER)).thenReturn(otherUsersCopy);
        when(session.getAttribute(WebKeys.USER_ID)).thenReturn(USER_ID);
        when(userAPI.loadUserById(USER_ID)).thenReturn(currentRecord);

        final Optional<User> resolved = resolver.resolve(session);

        assertSame(currentRecord, resolved.get());
        verify(session).setAttribute(WebKeys.USER, currentRecord);
        verify(userAPI, never()).loadUserById("user_b");
    }

    /**
     * A principal id equal to the session user's id (any letter case) is not a Login As session:
     * the user is resolved with a single lookup.
     */
    @Test
    void principalEqualToTheSessionUser_isNotLookedUpAgain() throws DotDataException, DotSecurityException {
        final User current = user(USER_ID, true);
        when(session.getAttribute(WebKeys.USER_ID)).thenReturn(USER_ID);
        when(session.getAttribute(WebKeys.PRINCIPAL_USER_ID)).thenReturn(USER_ID.toUpperCase());
        when(userAPI.loadUserById(USER_ID)).thenReturn(current);

        assertSame(current, resolver.resolve(session).get());
        verify(userAPI, times(1)).loadUserById(any());
        verify(session, never()).removeAttribute(any());
    }
}
