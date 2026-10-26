package com.dotcms.cms.login;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import com.dotcms.auth.providers.jwt.JsonWebTokenUtils;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.mock.request.MockHeaderRequest;
import com.dotcms.mock.request.MockHttpRequestIntegrationTest;
import com.dotcms.rest.api.v1.user.UserForm;
import com.dotcms.rest.api.v1.user.UserResource;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.UserAPI;
import com.liferay.portal.model.User;
import com.liferay.portal.util.WebKeys;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * A login token (the JWT set as the login cookie, also accepted as a Bearer header) must stop
 * working once its user logs out or has the password changed, while a token issued afterwards keeps
 * working. Each test drives the real logout and password APIs and then asks the token API whether
 * the earlier token still resolves to a user.
 */
public class LoginTokenInvalidationTest {

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

        userAPI.updatePassword(userAPI.loadUserById(user.getUserId()), NEW_PASSWORD,
                APILocator.systemUser(), false);

        assertFalse("token issued before the password change must be rejected", resolves(token));
        assertTrue(resolves(loginToken(userAPI.loadUserById(user.getUserId()))));
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
                "Basic " + Base64.getEncoder().encodeToString("admin@dotcms.com:admin".getBytes()));
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

    private static User newUser() {
        final User user = new UserDataGen().nextPersisted();
        createdUsers.add(user);
        return user;
    }

    private static String loginToken(final User user) {
        return JsonWebTokenUtils.getInstance().createUserToken(user, 1);
    }

    private static boolean resolves(final String token) {
        return APILocator.getApiTokenAPI().fromJwt(token, IP_ADDRESS).isPresent();
    }
}
