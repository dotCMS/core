package com.dotmarketing.webdav;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bradmcevoy.http.Request;
import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.auth.providers.jwt.beans.ApiToken;
import com.dotcms.auth.providers.jwt.beans.TokenType;
import com.dotcms.auth.providers.jwt.beans.UserToken;
import com.dotcms.auth.providers.jwt.factories.ApiTokenAPI;
import com.dotcms.repackage.bsh.Interpreter;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.cms.factories.PublicCompanyFactory;
import com.dotmarketing.cms.login.factories.LoginFactory;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.CompanyUtils;
import com.dotmarketing.util.Config;
import com.liferay.portal.auth.Authenticator;
import com.liferay.portal.model.Company;
import com.liferay.portal.model.User;
import java.lang.reflect.Field;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.Timer;
import javax.servlet.http.HttpServletRequest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;

/** Tests real DAV authentication entry points without constructing content and folder services. */
public class DotWebdavAuthenticationTest {

    private static final String USERNAME = "dav-user@example.com";
    private static final String USER_ID = "dav-user";
    private static final String CREDENTIAL = "test-api-jwt";
    private static final String IP = "192.0.2.10";
    private String previousMode;
    private HttpServletRequest previousRequest;
    private MockedStatic<APILocator> locator;
    private MockedStatic<PublicCompanyFactory> companies;
    private MockedStatic<CompanyUtils> companyUtils;
    private MockedStatic<LoginFactory> passwords;
    private MockedConstruction<Timer> timers;
    private MockedConstruction<Interpreter> preAuthenticators;
    private UserAPI userAPI;
    private ApiTokenAPI tokens;
    private Company company;
    private User user;
    private DotWebdavHelper helper;

    /** Isolate static collaborators but call the existing authorizePrincipal implementation. */
    @Before
    public void setUp() throws Exception {
        previousMode = Config.getStringProperty("ALLOW_BASIC_AUTH", null);
        previousRequest = HttpServletRequestThreadLocal.INSTANCE.getRequest();
        locator = mockStatic(APILocator.class);
        companies = mockStatic(PublicCompanyFactory.class);
        companyUtils = mockStatic(CompanyUtils.class);
        passwords = mockStatic(LoginFactory.class);
        // Loading the helper must not leave its unrelated non-daemon cache-cleaner timer running.
        timers = mockConstruction(Timer.class);
        preAuthenticators = mockConstruction(Interpreter.class,
                (interpreter, context) -> when(interpreter.eval(anyString()))
                        .thenReturn(mock(Authenticator.class)));
        userAPI = mock(UserAPI.class);
        tokens = mock(ApiTokenAPI.class);
        company = mock(Company.class);
        user = mock(User.class);
        final User systemUser = mock(User.class);
        locator.when(APILocator::getUserAPI).thenReturn(userAPI);
        locator.when(APILocator::getApiTokenAPI).thenReturn(tokens);
        companies.when(PublicCompanyFactory::getDefaultCompany).thenReturn(company);
        companyUtils.when(CompanyUtils::getDefaultCompany).thenReturn(company);
        when(company.getAuthType()).thenReturn(Company.AUTH_TYPE_EA);
        when(userAPI.getSystemUser()).thenReturn(systemUser);
        when(userAPI.loadByUserByEmail(USERNAME, systemUser, false)).thenReturn(user);
        when(userAPI.loadUserById(USER_ID, systemUser, false)).thenReturn(user);
        when(userAPI.loadUserById(USER_ID)).thenReturn(user);
        when(user.getUserId()).thenReturn(USER_ID);
        when(user.getEmailAddress()).thenReturn(USERNAME);
        when(user.isActive()).thenReturn(true);
        when(tokens.fromJwt(CREDENTIAL, IP)).thenReturn(Optional.empty());
        final HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn(IP);
        HttpServletRequestThreadLocal.INSTANCE.setRequest(request);
        // Skip the constructor's unrelated folder/cache wiring, not the authentication method.
        helper = mock(DotWebdavHelper.class, CALLS_REAL_METHODS);
    }

    /** No mode, thread-local request, or static mock may leak into the next test. */
    @After
    public void tearDown() {
        if (preAuthenticators != null) {
            preAuthenticators.close();
        }
        if (timers != null) {
            timers.close();
        }
        if (passwords != null) {
            passwords.close();
        }
        if (companyUtils != null) {
            companyUtils.close();
        }
        if (companies != null) {
            companies.close();
        }
        if (locator != null) {
            locator.close();
        }
        HttpServletRequestThreadLocal.INSTANCE.setRequest(previousRequest);
        Config.setProperty("ALLOW_BASIC_AUTH", previousMode);
    }

    /** A correct password must not bypass explicit NONE, FALSE, empty or unrecognized values. */
    @Test
    public void disabledModesDenyBeforeCheckingPasswordsOrTokens() throws Exception {
        passwords.when(() -> LoginFactory.passwordMatch(CREDENTIAL, user)).thenReturn(true);
        for (final String mode : new String[]{"NONE", "false", "invalid-mode", ""}) {
            Config.setProperty("ALLOW_BASIC_AUTH", mode);
            assertThrows(DotSecurityException.class,
                    () -> helper.authorizePrincipal(USERNAME, CREDENTIAL));
        }
        passwords.verifyNoInteractions();
        companies.verifyNoInteractions();
        companyUtils.verifyNoInteractions();
        verify(tokens, never()).fromJwt(CREDENTIAL, IP);
    }

    /** JWT_TOKEN must not use a valid account password as a token fallback. */
    @Test
    public void jwtTokenRejectsPasswordWithoutCheckingIt() throws Exception {
        Config.setProperty("ALLOW_BASIC_AUTH", "JWT_TOKEN");
        passwords.when(() -> LoginFactory.passwordMatch(CREDENTIAL, user)).thenReturn(true);

        assertThrows(DotSecurityException.class,
                () -> helper.authorizePrincipal(USERNAME, CREDENTIAL));

        passwords.verifyNoInteractions();
        verify(tokens).fromJwt(CREDENTIAL, IP);
    }

    /** DAV's username/API-JWT pair succeeds without consulting password hashes. */
    @Test
    public void jwtTokenAcceptsAnApiTokenOwnedByTheNamedUser() throws Exception {
        Config.setProperty("ALLOW_BASIC_AUTH", "JWT_TOKEN");
        when(tokens.fromJwt(CREDENTIAL, IP)).thenReturn(Optional.of(apiToken(USER_ID)));

        assertSame(user, helper.authorizePrincipal(USERNAME, CREDENTIAL));

        verify(tokens).fromJwt(CREDENTIAL, IP);
        passwords.verifyNoInteractions();
        companies.verifyNoInteractions();
        assertTrue("Token-only authentication must not enter the pre-authentication pipeline",
                preAuthenticators.constructed().isEmpty());
    }

    /** Companies that log in by id keep that lookup in token-only mode. */
    @Test
    public void jwtTokenAcceptsTheCompanyUserIdLogin() throws Exception {
        Config.setProperty("ALLOW_BASIC_AUTH", "JWT_TOKEN");
        when(company.getAuthType()).thenReturn(Company.AUTH_TYPE_ID);
        when(tokens.fromJwt(CREDENTIAL, IP)).thenReturn(Optional.of(apiToken(USER_ID)));

        assertSame(user, helper.authorizePrincipal(USER_ID, CREDENTIAL));

        passwords.verifyNoInteractions();
    }

    /** A valid token is not authority to impersonate a different Basic username. */
    @Test
    public void jwtTokenRejectsAnotherUsersApiToken() throws Exception {
        Config.setProperty("ALLOW_BASIC_AUTH", "JWT_TOKEN");
        when(tokens.fromJwt(CREDENTIAL, IP)).thenReturn(Optional.of(apiToken("someone-else")));

        assertThrows(DotSecurityException.class,
                () -> helper.authorizePrincipal(USERNAME, CREDENTIAL));

        passwords.verifyNoInteractions();
    }

    /** fromJwt returns generic JWToken; a matching USER_TOKEN is not an API token. */
    @Test
    public void jwtTokenRejectsAUserTokenEvenWhenItsOwnerMatches() throws Exception {
        Config.setProperty("ALLOW_BASIC_AUTH", "JWT_TOKEN");
        final UserToken token = mock(UserToken.class);
        when(token.getUserId()).thenReturn(USER_ID);
        when(token.getTokenType()).thenReturn(TokenType.USER_TOKEN);
        when(token.getActiveUser()).thenReturn(Optional.of(user));
        when(tokens.fromJwt(CREDENTIAL, IP)).thenReturn(Optional.of(token));

        assertThrows(DotSecurityException.class,
                () -> helper.authorizePrincipal(USERNAME, CREDENTIAL));

        passwords.verifyNoInteractions();
    }

    /** An API token for an inactive owner must not become a DAV principal. */
    @Test
    public void jwtTokenRejectsAnInactiveOwner() throws Exception {
        Config.setProperty("ALLOW_BASIC_AUTH", "JWT_TOKEN");
        when(user.isActive()).thenReturn(false);
        when(tokens.fromJwt(CREDENTIAL, IP)).thenReturn(Optional.of(apiToken(USER_ID)));

        assertThrows(DotSecurityException.class,
                () -> helper.authorizePrincipal(USERNAME, CREDENTIAL));

        passwords.verifyNoInteractions();
    }

    /** ALL retains password authentication and need not parse a successful password as a JWT. */
    @Test
    public void allRetainsPasswordCompatibility() throws Exception {
        Config.setProperty("ALLOW_BASIC_AUTH", "ALL");
        passwords.when(() -> LoginFactory.passwordMatch(CREDENTIAL, user)).thenReturn(true);

        assertSame(user, helper.authorizePrincipal(USERNAME, CREDENTIAL));

        passwords.verify(() -> LoginFactory.passwordMatch(CREDENTIAL, user));
        verify(tokens, never()).fromJwt(CREDENTIAL, IP);
    }

    /** ALL retains the legacy JWT alternative after a password mismatch. */
    @Test
    public void allRetainsTokenCompatibility() throws Exception {
        Config.setProperty("ALLOW_BASIC_AUTH", "ALL");
        when(tokens.fromJwt(CREDENTIAL, IP)).thenReturn(Optional.of(apiToken(USER_ID)));

        assertSame(user, helper.authorizePrincipal(USERNAME, CREDENTIAL));

        passwords.verify(() -> LoginFactory.passwordMatch(CREDENTIAL, user));
        verify(tokens).fromJwt(CREDENTIAL, IP);
    }

    /** A representative resource must propagate refusal and deny anonymous data or discovery. */
    @Test
    public void fileResourceRefusesDeniedCredentialsAndAnonymousAccess() throws Exception {
        final DotWebdavHelper denied = mock(DotWebdavHelper.class);
        when(denied.authorizePrincipal(USERNAME, CREDENTIAL))
                .thenThrow(new DotSecurityException("denied by authentication policy"));
        final FileResourceImpl resource = mock(FileResourceImpl.class, CALLS_REAL_METHODS);
        final Field field = FileResourceImpl.class.getDeclaredField("dotDavHelper");
        field.setAccessible(true);
        field.set(resource, denied);

        assertNull(resource.authenticate(USERNAME, CREDENTIAL));
        final Request request = mock(Request.class);
        assertFalse(resource.authorise(request, Request.Method.PROPFIND, null));
        assertFalse(resource.authorise(request, Request.Method.PROPPATCH, null));
        assertFalse(resource.authorise(request, Request.Method.OPTIONS, null));
        verify(denied).authorizePrincipal(USERNAME, CREDENTIAL);
    }

    /** Build a genuinely typed API token rather than a generic mocked JWToken. */
    private static ApiToken apiToken(final String owner) {
        return ApiToken.builder().withId("api-test-token").withUserId(owner)
                .withExpires(Date.from(Instant.now().plusSeconds(3600)))
                .withIssueDate(Date.from(Instant.now().minusSeconds(60))).build();
    }
}
