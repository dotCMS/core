package com.dotmarketing.webdav;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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
 * Covers WebDAV password and JWT validation without the obsolete pre-authentication hook.
 * The old property deliberately names a nonexistent class: it must not affect authentication.
 */
public class DotWebdavAuthenticationTest {

    private static final String USERNAME = "webdav-user";
    private static final String CREDENTIAL = "test-credential";

    private MockedStatic<APILocator> apis;
    private MockedStatic<PublicCompanyFactory> companies;
    private MockedStatic<LoginFactory> passwords;
    private MockedStatic<PropsUtil> properties;
    private DotWebdavHelper helper;
    private Company company;
    private User user;
    private User systemUser;
    private UserAPI userAPI;
    private ApiTokenAPI tokenAPI;
    private HttpServletRequest previousRequest;

    /** Sets up isolated APIs and avoids starting WebDAV's background cache-cleaner timer. */
    @Before
    public void setUp() throws Exception {
        previousRequest = HttpServletRequestThreadLocal.INSTANCE.getRequest();
        HttpServletRequestThreadLocal.INSTANCE.setRequest(null);
        apis = mockStatic(APILocator.class);
        companies = mockStatic(PublicCompanyFactory.class);
        passwords = mockStatic(LoginFactory.class);
        properties = mockStatic(PropsUtil.class);
        properties.when(() -> PropsUtil.get("auth.pipeline.pre"))
                .thenReturn("com.dotcms.removed.UnsupportedAuthenticator");

        company = mock(Company.class);
        user = mock(User.class);
        systemUser = mock(User.class);
        userAPI = mock(UserAPI.class);
        tokenAPI = mock(ApiTokenAPI.class);
        companies.when(PublicCompanyFactory::getDefaultCompany).thenReturn(company);
        apis.when(APILocator::getUserAPI).thenReturn(userAPI);
        apis.when(APILocator::getApiTokenAPI).thenReturn(tokenAPI);
        when(company.getAuthType()).thenReturn(Company.AUTH_TYPE_ID);
        when(userAPI.getSystemUser()).thenReturn(systemUser);
        when(userAPI.loadUserById(USERNAME, systemUser, false)).thenReturn(user);
        when(user.getUserId()).thenReturn(USERNAME);
        // WebDAV auth rejects inactive users, and an unstubbed mock reports inactive.
        when(user.isActive()).thenReturn(true);

        // Only the authentication methods are under test; skip API-dependent instance fields.
        try (MockedConstruction<Timer> timers = mockConstruction(Timer.class)) {
            helper = mock(DotWebdavHelper.class, CALLS_REAL_METHODS);
        }
    }

    /** Restores the request and releases thread-local static mocks even when a test fails. */
    @After
    public void tearDown() {
        HttpServletRequestThreadLocal.INSTANCE.setRequest(previousRequest);
        if (properties != null) {
            properties.close();
        }
        if (passwords != null) {
            passwords.close();
        }
        if (companies != null) {
            companies.close();
        }
        if (apis != null) {
            apis.close();
        }
    }

    /** A stale pre-authenticator setting must not prevent valid password authentication. */
    @Test
    public void testValidPasswordByUserIdIgnoresObsoletePreAuthenticator() throws Exception {
        passwords.when(() -> LoginFactory.passwordMatch(CREDENTIAL, user)).thenReturn(true);

        assertSame(user, helper.authorizePrincipal(USERNAME, CREDENTIAL));
        verify(userAPI).loadUserById(USERNAME, systemUser, false);
        verifyNoInteractions(tokenAPI);
    }

    /** Email-based lookup must still validate the password and return the requested user. */
    @Test
    public void testValidPasswordByEmail() throws Exception {
        when(company.getAuthType()).thenReturn(Company.AUTH_TYPE_EA);
        when(userAPI.loadByUserByEmail(USERNAME, systemUser, false)).thenReturn(user);
        passwords.when(() -> LoginFactory.passwordMatch(CREDENTIAL, user)).thenReturn(true);

        assertSame(user, helper.authorizePrincipal(USERNAME, CREDENTIAL));
        verify(userAPI).loadByUserByEmail(USERNAME, systemUser, false);
        verifyNoInteractions(tokenAPI);
    }

    /** A valid JWT is accepted only for the user resolved from the supplied username. */
    @Test
    public void testValidJwtForRequestedUser() throws Exception {
        final JWToken token = mock(JWToken.class);
        when(token.getUserId()).thenReturn(USERNAME);
        when(tokenAPI.fromJwt(CREDENTIAL, null)).thenReturn(Optional.of(token));

        assertSame(user, helper.authorizePrincipal(USERNAME, CREDENTIAL));
        passwords.verify(() -> LoginFactory.passwordMatch(CREDENTIAL, user));
    }

    /** A token belonging to a different user must not authorize the requested user. */
    @Test
    public void testJwtForAnotherUserIsRejected() throws Exception {
        final JWToken token = mock(JWToken.class);
        when(token.getUserId()).thenReturn("another-user");
        when(tokenAPI.fromJwt(CREDENTIAL, null)).thenReturn(Optional.of(token));

        final DotSecurityException error = assertThrows(DotSecurityException.class,
                () -> helper.authorizePrincipal(USERNAME, CREDENTIAL));

        assertEquals("The user's passwords didn't match", error.getMessage());
    }

    /** A failed password check and absent valid JWT must still fail closed. */
    @Test
    public void testInvalidPasswordAndJwtAreRejected() throws Exception {
        when(tokenAPI.fromJwt(CREDENTIAL, null)).thenReturn(Optional.empty());

        final DotSecurityException error = assertThrows(DotSecurityException.class,
                () -> helper.authorizePrincipal(USERNAME, CREDENTIAL));

        assertEquals("The user's passwords didn't match", error.getMessage());
    }

    /** An unresolved username must be rejected before any credential checks are attempted. */
    @Test
    public void testMissingUserIsRejected() throws Exception {
        when(userAPI.loadUserById(USERNAME, systemUser, false)).thenReturn(null);

        final DotSecurityException error = assertThrows(DotSecurityException.class,
                () -> helper.authorizePrincipal(USERNAME, CREDENTIAL));

        assertEquals("The user was returned NULL", error.getMessage());
        passwords.verifyNoInteractions();
        verifyNoInteractions(tokenAPI);
    }
}
