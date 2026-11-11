package com.dotcms.cms.login;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import com.dotcms.auth.providers.jwt.JsonWebTokenUtils;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.listeners.SessionMonitor;
import com.dotcms.mock.request.MockAttributeRequest;
import com.dotcms.mock.request.MockHeaderRequest;
import com.dotcms.mock.request.MockHttpRequestIntegrationTest;
import com.dotcms.mock.request.MockSession;
import com.dotcms.mock.request.MockSessionRequest;
import com.dotcms.rest.api.v1.user.UserForm;
import com.dotcms.rest.api.v1.user.UserResource;
import com.dotcms.test.util.ApiTokenTestUtils;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.business.UserCache;
import com.dotmarketing.cms.factories.PublicCompanyFactory;
import com.dotmarketing.db.LocalTransaction;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.UUIDGenerator;
import com.liferay.portal.ejb.UserLocalManagerUtil;
import com.liferay.portal.model.Company;
import com.liferay.portal.model.User;
import com.liferay.portal.util.CookieKeys;
import com.liferay.portal.util.PortalUtil;
import com.liferay.portal.util.WebKeys;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionBindingEvent;
import javax.ws.rs.core.Response;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

/**
 * A login token (the JWT set as the login cookie, also accepted as a Bearer header) must stop
 * working once its user logs out or has the password changed, while a token issued afterwards keeps
 * working. Each test drives the real logout and password APIs and then asks the token API whether
 * the earlier token still resolves to a user.
 */
public class LoginTokenInvalidationTest {

    @ClassRule
    public static final ApiTokenTestUtils TOKEN_UTILS = new ApiTokenTestUtils();

    private static final String IP_ADDRESS = "127.0.0.1";
    private static final String NEW_PASSWORD = "Neutral-Value-2026!";

    private static UserAPI userAPI;
    private static final List<User> createdUsers = new ArrayList<>();

    /**
     * Boots the integration test environment and resolves the user API.
     */
    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
        userAPI = APILocator.getUserAPI();
    }

    /**
     * Removes the users the tests created.
     */
    @AfterClass
    public static void cleanup() {
        createdUsers.forEach(UserDataGen::remove);
    }

    /** Ids of the users whose sessions a test registered with the session monitor. */
    private final List<String> trackedUserIds = new ArrayList<>();

    /**
     * Ends the sessions a test registered with the session monitor, so the other tests and
     * classes only see their own.
     */
    @After
    public void untrackSessions() {
        trackedUserIds.forEach(SessionMonitor::endSessionsOf);
        trackedUserIds.clear();
    }

    /**
     * Logging out through the login service rejects a login token issued before the logout, and a
     * token issued after it is accepted.
     */
    @Test
    public void test_logout_rejectsLoginTokenIssuedBeforeIt() throws Exception {
        final User user = newUser();
        final String token = loginToken(user);
        assertTrue("token must work before logout", resolves(token));

        final HttpServletRequest request =
                new MockHttpRequestIntegrationTest("localhost", "/api/v1/logout").request();
        request.getSession().setAttribute(WebKeys.USER_ID, user.getUserId());
        APILocator.getLoginServiceAPI().doActionLogout(request, mock(HttpServletResponse.class));

        assertFalse("token issued before logout must be rejected", resolves(token));
        assertTrue("token issued after logout must work",
                resolves(loginToken(userAPI.loadUserById(user.getUserId()))));
    }

    /**
     * Completing a password reset rejects a login token issued before it.
     */
    @Test
    public void test_updatePassword_rejectsLoginTokenIssuedBeforeIt() throws Exception {
        final User user = newUser();
        final String token = loginToken(user);
        assertTrue(resolves(token));

        final User toUpdate = userAPI.loadUserById(user.getUserId());
        userAPI.updatePassword(toUpdate, NEW_PASSWORD, APILocator.systemUser(), false);

        assertFalse("token issued before the password change must be rejected", resolves(token));
        assertTrue(resolves(loginToken(userAPI.loadUserById(user.getUserId()))));
        assertCurrentTokenId(toUpdate);
    }

    /**
     * Saving a user with a new password (the path used by the legacy user editing screen and the
     * current-user REST endpoint) rejects a login token issued before it.
     */
    @Test
    public void test_saveWithNewPassword_rejectsLoginTokenIssuedBeforeIt() throws Exception {
        final User user = newUser();
        final String token = loginToken(user);
        assertTrue(resolves(token));

        final User toSave = (User) userAPI.loadUserById(user.getUserId()).clone();
        toSave.setPassword(NEW_PASSWORD);
        userAPI.save(toSave, APILocator.systemUser(), true, false);

        assertFalse("token issued before the password change must be rejected", resolves(token));
        assertCurrentTokenId(toSave);
    }

    /**
     * Saving a {@link User} object loaded before a logout, for example one held in memory by a
     * screen that was open at the time, does not bring back the login token id the logout replaced,
     * so the token issued before the logout stays rejected.
     */
    @Test
    public void test_staleUserSavedAfterLogout_doesNotRestoreOldToken() throws Exception {
        final User user = newUser();
        final String token = loginToken(user);
        final User loadedBeforeLogout = (User) userAPI.loadUserById(user.getUserId()).clone();

        userAPI.invalidateLoginTokens(user.getUserId());
        loadedBeforeLogout.setFirstName("Edited");
        userAPI.save(loadedBeforeLogout, APILocator.systemUser(), false);

        assertNotEquals(user.getRememberMeToken(),
                userAPI.loadUserById(user.getUserId()).getRememberMeToken());
        assertFalse("a stale save must not bring back the token issued before logout", resolves(token));
    }

    /**
     * Saving a {@link User} object loaded before a logout through the legacy Liferay persistence
     * does not write the replaced login token id back.
     */
    @Test
    public void test_staleUserSavedThroughLegacyPathAfterLogout_doesNotRestoreOldToken() throws Exception {
        final User user = newUser();
        final String token = loginToken(user);
        final User loadedBeforeLogout = userAPI.loadUserById(user.getUserId());

        userAPI.invalidateLoginTokens(user.getUserId());
        loadedBeforeLogout.setFirstName("Edited");
        UserLocalManagerUtil.updateUser(loadedBeforeLogout);

        assertNotEquals(user.getRememberMeToken(),
                userAPI.loadUserById(user.getUserId()).getRememberMeToken());
        assertFalse("a stale legacy save must not bring back the token issued before logout",
                resolves(token));
    }

    /**
     * When the login token id changes inside a transaction, a copy of the user cached before the
     * commit, as another request reading the still-committed old value would leave behind, is
     * evicted once the transaction commits, so the earlier token is rejected afterwards.
     */
    @Test
    public void test_invalidateLoginTokensInTransaction_evictsUserCachedBeforeCommit() throws Exception {
        final User user = newUser();
        final String token = loginToken(user);
        final User loadedBefore = userAPI.loadUserById(user.getUserId());
        final UserCache userCache = CacheLocator.getUserCache();

        LocalTransaction.wrap(() -> {
            userAPI.invalidateLoginTokens(user.getUserId());
            userCache.add(user.getUserId(), loadedBefore);
        });

        assertNull("the cache must hold no entry for the user after the commit",
                userCache.get(user.getUserId()));
        assertFalse("token issued before the change must be rejected after the commit", resolves(token));
    }

    /**
     * When a user is deleted inside a transaction, a copy of the user cached before the commit, as
     * another request reading the still-committed row would leave behind, is evicted once the
     * transaction commits (the cache administrator repeats, after the commit, every removal made
     * inside the transaction), so a session of that user not reached by the delete resolves to no
     * user afterwards.
     */
    @Test
    public void test_deleteInTransaction_evictsUserCachedBeforeCommit() throws Exception {
        final User user = new UserDataGen().nextPersisted();
        final String userId = user.getUserId();
        final User loadedBefore = userAPI.loadUserById(userId);
        final UserCache userCache = CacheLocator.getUserCache();
        final HttpSession untrackedSession = new MockSession(UUIDGenerator.generateUuid());
        untrackedSession.setAttribute(WebKeys.USER_ID, userId);

        LocalTransaction.wrap(() -> {
            userAPI.delete(userAPI.loadUserById(userId), APILocator.systemUser(),
                    APILocator.systemUser(), false);
            userCache.add(userId, loadedBefore);
        });

        assertNull("the cache must hold no entry for the user after the commit", userCache.get(userId));
        assertNull("a session of the deleted user must resolve to no user",
                PortalUtil.getUser(untrackedSession));
    }

    /**
     * Invalidating the login tokens of a user id given in a different case or with surrounding
     * spaces changes that user and evicts the cached copy, so the earlier token is rejected right
     * away.
     */
    @Test
    public void test_invalidateLoginTokens_withNonCanonicalUserId_rejectsToken() throws Exception {
        final User user = newUser();
        final String token = loginToken(user);
        // load once so the user is cached under its stored id
        userAPI.loadUserById(user.getUserId());

        userAPI.invalidateLoginTokens("  " + user.getUserId().toUpperCase() + " ");

        assertFalse("the cached token id must be evicted", resolves(token));
    }

    /**
     * A blank user id is refused with a {@link DotDataException} instead of failing with a
     * {@link NullPointerException}.
     */
    @Test
    public void test_invalidateLoginTokens_withBlankUserId_throwsDotDataException() {
        assertThrows(DotDataException.class, () -> userAPI.invalidateLoginTokens(null));
        assertThrows(DotDataException.class, () -> userAPI.invalidateLoginTokens(" "));
    }

    /**
     * Saving a user without changing the password leaves its login tokens working, so ordinary
     * profile edits and the save done on every login do not end the user's sessions.
     */
    @Test
    public void test_saveWithoutPasswordChange_keepsLoginToken() throws Exception {
        final User user = newUser();
        final String token = loginToken(user);
        final String tokenIdBefore = userAPI.loadUserById(user.getUserId()).getRememberMeToken();

        final User toSave = (User) userAPI.loadUserById(user.getUserId()).clone();
        toSave.setFirstName("Renamed");
        userAPI.save(toSave, APILocator.systemUser(), false, false);

        assertEquals(tokenIdBefore, userAPI.loadUserById(user.getUserId()).getRememberMeToken());
        assertTrue("a save without a password change must not reject the token", resolves(token));
    }

    /**
     * Setting a new password through the admin user edit endpoint (PUT /api/v1/users) rejects a
     * login token issued before it.
     */
    @Test
    public void test_adminRestUserEdit_withNewPassword_rejectsLoginTokenIssuedBeforeIt() throws Exception {
        final User user = newUser();
        final String token = loginToken(user);
        assertTrue(resolves(token));

        final MockHeaderRequest request = new MockHeaderRequest(
                new MockHttpRequestIntegrationTest("localhost", "/api/v1/users").request());
        request.setHeader("Authorization",
                TOKEN_UTILS.adminBearerHeader());
        final Response response = new UserResource().update(request, mock(HttpServletResponse.class),
                new UserForm.Builder()
                        .userId(user.getUserId())
                        .firstName(user.getFirstName())
                        .lastName(user.getLastName())
                        .email(user.getEmailAddress())
                        .password(NEW_PASSWORD.toCharArray())
                        .build());
        assertEquals(200, response.getStatus());

        assertFalse("token issued before the password change must be rejected", resolves(token));
    }

    /**
     * Logging out while an administrator is logged in as another user rejects the administrator's
     * login token, which is the one the browser holds, and leaves the impersonated user's working.
     */
    @Test
    public void test_logoutDuringLoginAs_rejectsAdministratorTokenOnly() throws Exception {
        final User admin = newUser();
        final User target = newUser();
        final String adminToken = loginToken(admin);
        final String targetToken = loginToken(target);

        final HttpServletRequest request =
                new MockHttpRequestIntegrationTest("localhost", "/api/v1/logout").request();
        request.getSession().setAttribute(WebKeys.USER_ID, target.getUserId());
        request.getSession().setAttribute(WebKeys.PRINCIPAL_USER_ID, admin.getUserId());
        APILocator.getLoginServiceAPI().doActionLogout(request, mock(HttpServletResponse.class));

        assertFalse("the administrator's token must be rejected", resolves(adminToken));
        assertTrue("the impersonated user's token must keep working", resolves(targetToken));
    }

    /**
     * Logging out with no session, only a valid login cookie (for example after the session has
     * expired), rejects that cookie's token afterwards.
     */
    @Test
    public void test_logoutWithOnlyLoginCookie_rejectsThatToken() throws Exception {
        final User user = newUser();
        final String token = loginToken(user);
        assertTrue(resolves(token));

        APILocator.getLoginServiceAPI().doActionLogout(requestWithLoginCookie(token),
                mock(HttpServletResponse.class));

        assertFalse("the cookie's token must be rejected after logout", resolves(token));
    }

    /**
     * A login cookie that does not validate (altered signature, or a token id that is no longer
     * current) does not change any user's login token id on logout.
     */
    @Test
    public void test_logoutWithInvalidLoginCookie_changesNothing() throws Exception {
        final User user = newUser();
        final String staleToken = loginToken(user);
        userAPI.invalidateLoginTokens(user.getUserId());
        final User reloaded = userAPI.loadUserById(user.getUserId());
        final String currentToken = loginToken(reloaded);
        final String tokenIdBefore = reloaded.getRememberMeToken();

        final String altered = currentToken.substring(0, currentToken.length() - 4)
                + (currentToken.endsWith("AAAA") ? "BBBB" : "AAAA");
        APILocator.getLoginServiceAPI().doActionLogout(requestWithLoginCookie(altered),
                mock(HttpServletResponse.class));
        APILocator.getLoginServiceAPI().doActionLogout(requestWithLoginCookie(staleToken),
                mock(HttpServletResponse.class));

        assertEquals(tokenIdBefore, userAPI.loadUserById(user.getUserId()).getRememberMeToken());
        assertTrue("the current token must keep working", resolves(currentToken));
    }

    /**
     * The logout used by the SAML integration ({@code LoginServiceAPI#doLogout}) rejects a login
     * token issued before it.
     */
    @Test
    public void test_doLogout_rejectsLoginTokenIssuedBeforeIt() throws Exception {
        final User user = newUser();
        final String token = loginToken(user);
        final String tokenIdBefore = user.getRememberMeToken();
        assertTrue(resolves(token));

        final HttpServletRequest request =
                new MockHttpRequestIntegrationTest("localhost", "/dotsaml/logout").request();
        request.getSession().setAttribute(WebKeys.USER_ID, user.getUserId());
        APILocator.getLoginServiceAPI().doLogout(request, mock(HttpServletResponse.class));

        assertNotEquals(tokenIdBefore, userAPI.loadUserById(user.getUserId()).getRememberMeToken());
        assertFalse("token issued before logout must be rejected", resolves(token));
    }

    /**
     * Making a user inactive through {@link UserAPI#save} rejects a login token issued before the
     * change, ends the user's session tracked on this node, and leaves the saved object with the
     * stored token id. The old token stays rejected once the user is active again, which shows the
     * token id was replaced and not only the status checked.
     */
    @Test
    public void test_saveAsInactive_rejectsLoginTokenAndEndsTrackedSession() throws Exception {
        final User user = newUser();
        final String token = loginToken(user);
        final String tokenIdBefore = user.getRememberMeToken();
        final HttpSession session = trackedSessionOf(user);
        final String sessionId = session.getId();
        assertTrue(resolves(token));
        assertTrue(isTracked(sessionId));

        final User toSave = (User) userAPI.loadUserById(user.getUserId()).clone();
        toSave.setActive(false);
        userAPI.save(toSave, APILocator.systemUser(), false);

        assertFalse("token issued before the change must be rejected", resolves(token));
        final String tokenIdAfter = userAPI.loadUserById(user.getUserId()).getRememberMeToken();
        assertNotEquals(tokenIdBefore, tokenIdAfter);
        assertEquals("the saved object carries the stored token id", tokenIdAfter, toSave.getRememberMeToken());
        assertFalse("the tracked session must be ended", isTracked(sessionId));
        assertNull("the ended session no longer names the user", session.getAttribute(WebKeys.USER_ID));

        final User reactivated = (User) userAPI.loadUserById(user.getUserId()).clone();
        reactivated.setActive(true);
        userAPI.save(reactivated, APILocator.systemUser(), false);
        assertFalse("the old token stays rejected once the user is active again", resolves(token));
        assertTrue(resolves(loginToken(userAPI.loadUserById(user.getUserId()))));
    }

    /**
     * Saving a user that stays active keeps its login token and its tracked session, so profile
     * edits and the save made on every login do not end the user's sessions.
     */
    @Test
    public void test_saveWithoutStatusChange_keepsLoginTokenAndTrackedSession() throws Exception {
        final User user = newUser();
        final String token = loginToken(user);
        final HttpSession session = trackedSessionOf(user);
        final String sessionId = session.getId();

        final User toSave = (User) userAPI.loadUserById(user.getUserId()).clone();
        toSave.setLastName("Renamed");
        userAPI.save(toSave, APILocator.systemUser(), false);

        assertTrue("a save that keeps the user active must not reject the token", resolves(token));
        assertTrue("the tracked session must go on", isTracked(sessionId));
        assertEquals(user.getUserId(), session.getAttribute(WebKeys.USER_ID));
    }

    /**
     * Making an inactive user active again keeps its login token id: no new id is stored, and a
     * token issued once the user is active works. (A token cannot be issued while the user is
     * inactive, so the check is on the stored id.)
     */
    @Test
    public void test_saveAsActiveAgain_keepsLoginTokenId() throws Exception {
        final User user = newUser();
        final User inactive = (User) userAPI.loadUserById(user.getUserId()).clone();
        inactive.setActive(false);
        userAPI.save(inactive, APILocator.systemUser(), false);
        final String tokenIdWhileInactive = userAPI.loadUserById(user.getUserId()).getRememberMeToken();

        final User active = (User) userAPI.loadUserById(user.getUserId()).clone();
        active.setActive(true);
        userAPI.save(active, APILocator.systemUser(), false);

        final User reactivated = userAPI.loadUserById(user.getUserId());
        assertEquals("activating again keeps the token id", tokenIdWhileInactive, reactivated.getRememberMeToken());
        assertTrue("a token issued once the user is active works", resolves(loginToken(reactivated)));
    }

    /**
     * Deleting a user through {@link UserAPI#delete} rejects a login token issued before and ends
     * the user's session tracked on this node.
     */
    @Test
    public void test_delete_rejectsLoginTokenAndEndsTrackedSession() throws Exception {
        final User user = new UserDataGen().nextPersisted();
        final String token = loginToken(user);
        final HttpSession session = trackedSessionOf(user);
        final String sessionId = session.getId();
        assertTrue(resolves(token));

        userAPI.delete(userAPI.loadUserById(user.getUserId()), APILocator.systemUser(),
                APILocator.systemUser(), false);

        assertFalse("token of a deleted user must be rejected", resolves(token));
        assertFalse("the tracked session must be ended", isTracked(sessionId));
        assertNull("the ended session no longer names the user", session.getAttribute(WebKeys.USER_ID));
    }

    /**
     * A session is registered with the session monitor as soon as the login stores the user id in
     * it, before the browser makes another request, so the session tools list it and can end it
     * right away. The servlet container hands the session's attribute events to the listener
     * registered in web.xml; the session used here hands them over itself.
     */
    @Test
    public void test_doActionLogin_registersTheSessionWithTheSessionMonitor() throws Exception {
        final User user = new UserDataGen()
                .roles(APILocator.getRoleAPI().loadBackEndUserRole()).nextPersisted();
        createdUsers.add(user);
        userAPI.updatePassword(user, NEW_PASSWORD, APILocator.systemUser(), false);
        trackedUserIds.add(user.getUserId());
        final MockSessionRequest sessionRequest = new MockSessionRequest(
                new MockHttpRequestIntegrationTest("localhost", "/api/v1/authentication").request());
        sessionRequest.setSession(new ListenedSession());
        final HttpServletRequest request = new MockAttributeRequest(sessionRequest).request();

        assertTrue(APILocator.getLoginServiceAPI().doActionLogin(loginName(user), NEW_PASSWORD, false,
                request, mock(HttpServletResponse.class)));

        final HttpSession session = request.getSession(false);
        assertNotNull(session);
        assertEquals(user.getUserId(), session.getAttribute(WebKeys.USER_ID));
        assertTrue("the session must be listed right after the login", isTracked(session.getId()));
    }

    /**
     * The front controller copies the session's user id onto the request before the user is
     * resolved. When the session is refused because the administrator behind a Login As session is
     * no longer active, that copy names the refused session user and is not used as a second
     * source: the request ends up with no user and the copy is removed from the request.
     */
    @Test
    public void test_getUser_requestIdCopiedFromARejectedLoginAsSession_isNotUsed() throws Exception {
        final User admin = newUser();
        final User target = newUser();
        final User inactiveAdmin = (User) userAPI.loadUserById(admin.getUserId()).clone();
        inactiveAdmin.setActive(false);
        userAPI.save(inactiveAdmin, APILocator.systemUser(), false);

        final HttpServletRequest request =
                new MockHttpRequestIntegrationTest("localhost", "/c/portal/layout").request();
        request.getSession().setAttribute(WebKeys.USER_ID, target.getUserId());
        request.getSession().setAttribute(WebKeys.PRINCIPAL_USER_ID, admin.getUserId());
        request.setAttribute(WebKeys.USER_ID, target.getUserId());

        assertNull("the refused session user must not come back through the request's copy",
                PortalUtil.getUser(request));
        assertNull("the copied id must be removed from the request",
                request.getAttribute(WebKeys.USER_ID));
        assertNull("the refused session no longer names the user",
                request.getSession().getAttribute(WebKeys.USER_ID));
    }

    /**
     * A logout request with no session that carries the given token as its login cookie.
     */
    private static HttpServletRequest requestWithLoginCookie(final String token) {
        return new HttpServletRequestWrapper(
                new MockHttpRequestIntegrationTest("localhost", "/dotAdmin/logout").request()) {
            @Override
            public Cookie[] getCookies() {
                return new Cookie[]{new Cookie(CookieKeys.REMEMBER_ME_COOKIE.get(), token)};
            }
        };
    }

    private static User newUser() {
        final User user = new UserDataGen().nextPersisted();
        createdUsers.add(user);
        return user;
    }

    /**
     * Checks that the object a password change was made on carries the stored login token id, so
     * the change happened after the save and a JWT issued from that object works.
     */
    private static void assertCurrentTokenId(final User changed) throws Exception {
        assertEquals(userAPI.loadUserById(changed.getUserId()).getRememberMeToken(),
                changed.getRememberMeToken());
        assertTrue(resolves(loginToken(changed)));
    }

    private static String loginToken(final User user) {
        return JsonWebTokenUtils.getInstance().createUserToken(user, 1);
    }

    private static boolean resolves(final String token) {
        return APILocator.getApiTokenAPI().fromJwt(token, IP_ADDRESS).isPresent();
    }

    /**
     * A session of the user, registered with the session monitor as a login registers it.
     */
    private HttpSession trackedSessionOf(final User user) {
        final HttpSession session = new MockSession(UUIDGenerator.generateUuid());
        session.setAttribute(WebKeys.USER_ID, user.getUserId());
        SessionMonitor.track(session);
        trackedUserIds.add(user.getUserId());
        return session;
    }

    private static boolean isTracked(final String sessionId) {
        return new SessionMonitor().getUserSessions().containsKey(sessionId);
    }

    /**
     * The name the login takes for the user: its id or its email, as the company's authentication
     * type says.
     */
    private static String loginName(final User user) {
        return Company.AUTH_TYPE_ID.equals(PublicCompanyFactory.getDefaultCompany().getAuthType())
                ? user.getUserId() : user.getEmailAddress();
    }

    /**
     * A session that hands its attribute events to the session monitor, as the servlet container
     * does for the listener registered in web.xml.
     */
    private static final class ListenedSession extends MockSession {

        ListenedSession() {
            super(UUIDGenerator.generateUuid());
        }

        @Override
        public void setAttribute(final String name, final Object value) {
            final boolean replaced = getAttribute(name) != null;
            super.setAttribute(name, value);
            final HttpSessionBindingEvent event = new HttpSessionBindingEvent(this, name, value);
            if (replaced) {
                new SessionMonitor().attributeReplaced(event);
            } else {
                new SessionMonitor().attributeAdded(event);
            }
        }
    }
}
