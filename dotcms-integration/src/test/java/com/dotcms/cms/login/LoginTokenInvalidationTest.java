package com.dotcms.cms.login;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import com.dotcms.auth.providers.jwt.JsonWebTokenUtils;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.mock.request.MockHttpRequestIntegrationTest;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.UserAPI;
import com.liferay.portal.model.User;
import com.liferay.portal.util.WebKeys;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * A login token (the JWT set as the login cookie, also accepted as a Bearer header) must stop
 * working once its user logs out, while a token issued afterwards keeps working. Each test drives
 * the real logout API and then asks the token API whether the earlier token still resolves to a
 * user.
 */
public class LoginTokenInvalidationTest {

    private static final String IP_ADDRESS = "127.0.0.1";

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
