package com.dotcms.cms.login;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.dotcms.auth.providers.jwt.JsonWebTokenUtils;
import com.dotcms.auth.providers.jwt.factories.JsonWebTokenFactory;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.filters.interceptor.Result;
import com.dotcms.filters.interceptor.dotcms.DefaultAutoLoginWebInterceptor;
import com.dotcms.mock.request.MockHeaderRequest;
import com.dotcms.mock.request.MockHttpRequestIntegrationTest;
import com.dotcms.rest.api.v1.user.UserForm;
import com.dotcms.rest.api.v1.user.UserResource;
import com.dotcms.test.util.ApiTokenTestUtils;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.business.UserCache;
import com.dotmarketing.db.LocalTransaction;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Config;
import com.liferay.portal.ejb.UserLocalManagerUtil;
import com.liferay.portal.model.User;
import com.liferay.portal.util.CookieKeys;
import com.liferay.portal.util.WebKeys;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.ws.rs.core.Response;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

/**
 * A login token (the JWT set as the login cookie, also accepted as a Bearer header) must stop
 * working once its user logs out or has the password changed, while a token issued afterwards keeps
 * working. Each test drives the real logout and password APIs and then asks the token API whether
 * the earlier token still resolves to a user. A login without "remember me" issues a token that
 * expires after the configured session token lifetime.
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
     * A login without "remember me" sets a session cookie whose token expires after the default
     * session token lifetime (60 minutes) and schedules its renewal on the session.
     */
    @Test
    public void test_sessionLogin_tokenExpiresAfterSessionLifetime() throws Exception {
        final HttpServletRequest request =
                new MockHttpRequestIntegrationTest("localhost", "/api/v1/authentication").request();
        request.getSession(); // a login always has its HTTP session by the time the cookie is set
        final long before = System.currentTimeMillis();

        final Cookie cookie = loginCookie(request, newUser(), false);

        assertEquals("the session cookie has no max age", -1, cookie.getMaxAge());
        assertExpiresIn(cookie, before, TimeUnit.MINUTES.toMillis(
                LoginServiceAPI.JSON_WEB_TOKEN_SESSION_MAX_AGE_MINUTES_DEFAULT));
        final Object renewAt =
                request.getSession().getAttribute(LoginServiceAPI.SESSION_LOGIN_TOKEN_RENEW_AT);
        assertTrue("renewal must be scheduled within the token lifetime", renewAt instanceof Long
                && (Long) renewAt > before && (Long) renewAt < expiryMillis(cookie));
    }

    /**
     * The session token lifetime follows its configuration property.
     */
    @Test
    public void test_sessionLogin_tokenLifetimeIsConfigurable() throws Exception {
        Config.setProperty(LoginServiceAPI.JSON_WEB_TOKEN_SESSION_MAX_AGE_MINUTES, 15);
        try {
            final long before = System.currentTimeMillis();
            final Cookie cookie = loginCookie(new MockHttpRequestIntegrationTest("localhost",
                    "/api/v1/authentication").request(), newUser(), false);
            assertExpiresIn(cookie, before, TimeUnit.MINUTES.toMillis(15));
        } finally {
            Config.setProperty(LoginServiceAPI.JSON_WEB_TOKEN_SESSION_MAX_AGE_MINUTES,
                    LoginServiceAPI.JSON_WEB_TOKEN_SESSION_MAX_AGE_MINUTES_DEFAULT);
        }
    }

    /**
     * A "remember me" login keeps its persistent cookie and a token that lasts the configured
     * number of days, and clears any renewal scheduled on the session.
     */
    @Test
    public void test_rememberMeLogin_keepsDaysLifetime() throws Exception {
        final HttpServletRequest request =
                new MockHttpRequestIntegrationTest("localhost", "/api/v1/authentication").request();
        request.getSession().setAttribute(LoginServiceAPI.SESSION_LOGIN_TOKEN_RENEW_AT, 0L);
        final long before = System.currentTimeMillis();

        final Cookie cookie = loginCookie(request, newUser(), true);

        assertTrue("the remember-me cookie is persistent", cookie.getMaxAge() > 0);
        assertExpiresIn(cookie, before,
                TimeUnit.DAYS.toMillis(LoginServiceAPI.JWT_TOKEN_MAX_AGE_DAYS.get()));
        assertNull(request.getSession().getAttribute(LoginServiceAPI.SESSION_LOGIN_TOKEN_RENEW_AT));
    }

    /**
     * A plugin login that sets a persistent cookie through the days overload clears a renewal
     * scheduled earlier on the same session, so the persistent cookie is never replaced.
     */
    @Test
    public void test_persistentCookieFromDaysOverload_clearsScheduledRenewal() throws Exception {
        final HttpServletRequest request =
                new MockHttpRequestIntegrationTest("localhost", "/api/v1/authentication").request();
        request.getSession().setAttribute(LoginServiceAPI.SESSION_LOGIN_TOKEN_RENEW_AT, 0L);
        final HttpServletResponse response = mock(HttpServletResponse.class);

        APILocator.getLoginServiceAPI().doRememberMe(request, response, newUser(), 7);

        final ArgumentCaptor<Cookie> cookie = ArgumentCaptor.forClass(Cookie.class);
        verify(response).addCookie(cookie.capture());
        assertTrue("the cookie is persistent", cookie.getValue().getMaxAge() > 0);
        assertNull(request.getSession().getAttribute(LoginServiceAPI.SESSION_LOGIN_TOKEN_RENEW_AT));
    }

    /**
     * A session token lifetime below one minute is ignored and the default is used.
     */
    @Test
    public void test_sessionLogin_invalidLifetimeFallsBackToDefault() throws Exception {
        Config.setProperty(LoginServiceAPI.JSON_WEB_TOKEN_SESSION_MAX_AGE_MINUTES, 0);
        try {
            final long before = System.currentTimeMillis();
            final Cookie cookie = loginCookie(new MockHttpRequestIntegrationTest("localhost",
                    "/api/v1/authentication").request(), newUser(), false);
            assertExpiresIn(cookie, before, TimeUnit.MINUTES.toMillis(
                    LoginServiceAPI.JSON_WEB_TOKEN_SESSION_MAX_AGE_MINUTES_DEFAULT));
        } finally {
            Config.setProperty(LoginServiceAPI.JSON_WEB_TOKEN_SESSION_MAX_AGE_MINUTES,
                    LoginServiceAPI.JSON_WEB_TOKEN_SESSION_MAX_AGE_MINUTES_DEFAULT);
        }
    }

    /**
     * A login restored from a session login token cookie schedules the renewal of that cookie on
     * the new session, while one restored from a "remember me" cookie does not.
     */
    @Test
    public void test_cookieLogin_schedulesRenewalOnlyForSessionLoginToken() throws Exception {
        final User user = newUser();
        APILocator.getRoleAPI().addRoleToUser(APILocator.getRoleAPI().loadBackEndUserRole(), user);
        final String sessionToken = JsonWebTokenUtils.getInstance().createUserToken(user,
                Duration.ofMinutes(LoginServiceAPI.JSON_WEB_TOKEN_SESSION_MAX_AGE_MINUTES_DEFAULT));
        final String rememberMeToken = JsonWebTokenUtils.getInstance().createUserToken(user,
                LoginServiceAPI.JWT_TOKEN_MAX_AGE_DAYS.get());

        assertEquals(0L, sessionAfterCookieLogin(sessionToken)
                .getAttribute(LoginServiceAPI.SESSION_LOGIN_TOKEN_RENEW_AT));
        assertNull(sessionAfterCookieLogin(rememberMeToken)
                .getAttribute(LoginServiceAPI.SESSION_LOGIN_TOKEN_RENEW_AT));
    }

    /**
     * A back-end request on a logged-in session whose renewal time has passed, carrying a valid
     * session login token cookie, gets a new browser-session cookie whose token expires later, and
     * the renewal time moves into the future. A second request right after it renews nothing.
     */
    @Test
    public void test_dueBackEndRequest_renewsSessionLoginToken() throws Exception {
        final User user = newUser();
        final String original = JsonWebTokenUtils.getInstance().createUserToken(user, Duration.ofMinutes(30));
        final HttpServletRequest base =
                new MockHttpRequestIntegrationTest("localhost", "/api/v1/users/current").request();
        final HttpSession session = base.getSession();
        session.setAttribute(WebKeys.USER_ID, user.getUserId());
        session.setAttribute(LoginServiceAPI.SESSION_LOGIN_TOKEN_RENEW_AT, System.currentTimeMillis() - 1_000L);
        final DefaultAutoLoginWebInterceptor interceptor = new DefaultAutoLoginWebInterceptor();

        final HttpServletResponse firstResponse = mock(HttpServletResponse.class);
        assertEquals(Result.SKIP, interceptor.intercept(withLoginCookie(base, original), firstResponse));

        final ArgumentCaptor<Cookie> renewed = ArgumentCaptor.forClass(Cookie.class);
        verify(firstResponse).addCookie(renewed.capture());
        assertEquals(CookieKeys.REMEMBER_ME_COOKIE.get(), renewed.getValue().getName());
        assertEquals("the renewed cookie is a browser-session cookie", -1, renewed.getValue().getMaxAge());
        assertTrue("the renewed token must expire later than the one it replaces",
                expiryMillis(renewed.getValue()) > JsonWebTokenFactory.getInstance()
                        .getJsonWebTokenService().parseToken(original).getExpiresDate().getTime());
        final Object renewAt = session.getAttribute(LoginServiceAPI.SESSION_LOGIN_TOKEN_RENEW_AT);
        assertTrue("the renewal time must move into the future",
                renewAt instanceof Long && (Long) renewAt > System.currentTimeMillis());

        final HttpServletResponse secondResponse = mock(HttpServletResponse.class);
        assertEquals(Result.SKIP, interceptor.intercept(
                withLoginCookie(base, renewed.getValue().getValue()), secondResponse));
        verify(secondResponse, never()).addCookie(any(Cookie.class));
        assertEquals(renewAt, session.getAttribute(LoginServiceAPI.SESSION_LOGIN_TOKEN_RENEW_AT));
    }

    /**
     * Wraps the request so it carries the given token as its login cookie, keeping its session.
     */
    private static HttpServletRequest withLoginCookie(final HttpServletRequest request, final String token) {
        return new HttpServletRequestWrapper(request) {
            @Override
            public Cookie[] getCookies() {
                return new Cookie[]{new Cookie(CookieKeys.REMEMBER_ME_COOKIE.get(), token)};
            }
        };
    }

    /**
     * Sends a site page request with no session and the given token as its login cookie through
     * the auto-login interceptor, and returns the session it logged in.
     */
    private static HttpSession sessionAfterCookieLogin(final String token) {
        final HttpServletRequest request = new HttpServletRequestWrapper(
                new MockHttpRequestIntegrationTest("localhost", "/").request()) {
            @Override
            public Cookie[] getCookies() {
                return new Cookie[]{new Cookie(CookieKeys.REMEMBER_ME_COOKIE.get(), token)};
            }
        };
        assertEquals(Result.SKIP, new DefaultAutoLoginWebInterceptor()
                .intercept(request, mock(HttpServletResponse.class)));
        final HttpSession session = request.getSession(false);
        assertNotNull("the cookie must log the request in", session);
        return session;
    }

    /**
     * Issues the login cookie for the user through the login service and returns it.
     */
    private static Cookie loginCookie(final HttpServletRequest request, final User user,
            final boolean rememberMe) {
        final HttpServletResponse response = mock(HttpServletResponse.class);
        APILocator.getLoginServiceAPI().doRememberMe(request, response, user, rememberMe);
        final ArgumentCaptor<Cookie> cookie = ArgumentCaptor.forClass(Cookie.class);
        verify(response).addCookie(cookie.capture());
        assertEquals(CookieKeys.REMEMBER_ME_COOKIE.get(), cookie.getValue().getName());
        return cookie.getValue();
    }

    private static long expiryMillis(final Cookie cookie) {
        return JsonWebTokenFactory.getInstance().getJsonWebTokenService()
                .parseToken(cookie.getValue()).getExpiresDate().getTime();
    }

    /**
     * Asserts the cookie's token expires the given lifetime after {@code before}, within a minute.
     */
    private static void assertExpiresIn(final Cookie cookie, final long before, final long lifetime) {
        final long expiresIn = expiryMillis(cookie) - before;
        assertTrue("token lifetime was " + expiresIn + " ms, expected about " + lifetime + " ms",
                expiresIn >= lifetime - TimeUnit.MINUTES.toMillis(1)
                        && expiresIn <= lifetime + TimeUnit.MINUTES.toMillis(1));
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
}
