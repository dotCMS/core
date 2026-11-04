package com.dotmarketing.webdav;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.auth.providers.jwt.beans.JWToken;
import com.dotcms.auth.providers.jwt.factories.ApiTokenAPI;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.cms.factories.PublicCompanyFactory;
import com.dotmarketing.cms.login.factories.LoginFactory;
import com.dotmarketing.exception.DotSecurityException;
import com.liferay.portal.model.Company;
import com.liferay.portal.model.User;
import com.liferay.portal.util.PropsUtil;
import java.util.Optional;
import java.util.Timer;
import javax.servlet.http.HttpServletRequest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;

/**
 * WebDAV authentication must apply the same account-status checks as the back-end login:
 * inactive users and expired passwords are rejected with the same response as a wrong password.
 */
public class DotWebdavHelperAccountStatusTest {

    private static final String USERNAME = "webdav-user";
    private static final String CREDENTIAL = "test-credential";
    private static final String REJECTED = "The user's passwords didn't match";

    private MockedStatic<APILocator> apis;
    private MockedStatic<PublicCompanyFactory> companies;
    private MockedStatic<LoginFactory> passwords;
    private MockedStatic<PropsUtil> properties;
    private HttpServletRequest previousRequest;
    private DotWebdavHelper helper;
    private User user;
    private ApiTokenAPI tokenAPI;

    @Before
    public void setUp() throws Exception {
        previousRequest = HttpServletRequestThreadLocal.INSTANCE.getRequest();
        HttpServletRequestThreadLocal.INSTANCE.setRequest(null);
        apis = mockStatic(APILocator.class);
        companies = mockStatic(PublicCompanyFactory.class);
        passwords = mockStatic(LoginFactory.class);
        properties = mockStatic(PropsUtil.class);

        final Company company = mock(Company.class);
        final UserAPI userAPI = mock(UserAPI.class);
        final User systemUser = mock(User.class);
        user = mock(User.class);
        tokenAPI = mock(ApiTokenAPI.class);
        companies.when(PublicCompanyFactory::getDefaultCompany).thenReturn(company);
        apis.when(APILocator::getUserAPI).thenReturn(userAPI);
        apis.when(APILocator::getApiTokenAPI).thenReturn(tokenAPI);
        when(company.getAuthType()).thenReturn(Company.AUTH_TYPE_ID);
        when(userAPI.getSystemUser()).thenReturn(systemUser);
        when(userAPI.loadUserById(USERNAME, systemUser, false)).thenReturn(user);
        when(user.getUserId()).thenReturn(USERNAME);
        when(user.isActive()).thenReturn(true);
        when(user.isPasswordExpired()).thenReturn(false);
        when(tokenAPI.fromJwt(CREDENTIAL, null)).thenReturn(Optional.empty());

        // Only authorizePrincipal is under test: skip the constructor and the cache-cleaner timer.
        try (MockedConstruction<Timer> timers = mockConstruction(Timer.class)) {
            helper = mock(DotWebdavHelper.class, CALLS_REAL_METHODS);
        }
    }

    @After
    public void tearDown() {
        HttpServletRequestThreadLocal.INSTANCE.setRequest(previousRequest);
        for (final MockedStatic<?> mocked : new MockedStatic<?>[]{properties, passwords, companies, apis}) {
            if (mocked != null) {
                mocked.close();
            }
        }
    }

    private void passwordMatches() {
        passwords.when(() -> LoginFactory.passwordMatch(CREDENTIAL, user)).thenReturn(true);
    }

    private void tokenMatches() {
        final JWToken token = mock(JWToken.class);
        when(token.getUserId()).thenReturn(USERNAME);
        when(tokenAPI.fromJwt(CREDENTIAL, null)).thenReturn(Optional.of(token));
    }

    private void assertRejected() {
        final DotSecurityException error = assertThrows(DotSecurityException.class,
                () -> helper.authorizePrincipal(USERNAME, CREDENTIAL));
        assertEquals(REJECTED, error.getMessage());
    }

    @Test
    public void activeUserWithValidPasswordIsAccepted() throws Exception {
        passwordMatches();
        assertSame(user, helper.authorizePrincipal(USERNAME, CREDENTIAL));
    }

    @Test
    public void inactiveUserWithValidPasswordIsRejected() {
        passwordMatches();
        when(user.isActive()).thenReturn(false);
        assertRejected();
    }

    @Test
    public void inactiveUserWithValidTokenIsRejected() {
        tokenMatches();
        when(user.isActive()).thenReturn(false);
        assertRejected();
    }

    @Test
    public void expiredPasswordIsRejected() {
        passwordMatches();
        when(user.isPasswordExpired()).thenReturn(true);
        assertRejected();
    }

    @Test
    public void validTokenIsAcceptedWhenPasswordHasExpired() throws Exception {
        tokenMatches();
        when(user.isPasswordExpired()).thenReturn(true);
        assertSame(user, helper.authorizePrincipal(USERNAME, CREDENTIAL));
    }

    @Test
    public void wrongPasswordIsRejected() {
        assertRejected();
    }
}
