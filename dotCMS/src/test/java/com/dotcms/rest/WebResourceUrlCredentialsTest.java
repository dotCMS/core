package com.dotcms.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.dotcms.UnitTestBase;
import com.dotcms.auth.dotAuth.rest.DotAuthSessionCredentialProcessorImpl;
import com.dotcms.auth.providers.jwt.JsonWebTokenAuthCredentialProcessor;
import com.dotcms.auth.providers.jwt.beans.ApiToken;
import com.dotcms.auth.providers.jwt.factories.ApiTokenAPI;
import com.dotcms.cms.login.LoginServiceAPI;
import com.dotcms.mock.request.MockAttributeRequest;
import com.dotcms.rest.exception.SecurityException;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.ApiProvider;
import com.dotmarketing.business.LayoutAPI;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.business.web.UserWebAPI;
import com.dotmarketing.util.CompanyUtils;
import com.dotmarketing.util.Config;
import com.liferay.portal.auth.PrincipalThreadLocal;
import com.liferay.portal.model.Company;
import com.liferay.portal.model.User;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.MockedStatic;

/** URL login is a separate opt-in, including when the URL password is a reusable API JWT. */
@RunWith(Parameterized.class)
public class WebResourceUrlCredentialsTest extends UnitTestBase {

    private static final String PROPERTY = "ALLOW_USERNAME_PASSWORD_AS_URL_PARAMS";
    private static final String USERNAME = "token-owner";
    private static final String PASSWORD = "a-password";
    private static final String JWT = "header.payload.signature";
    private final String mode;
    private final String enabled;
    private String previousMode;
    private String previousEnabled;
    private String previousPrincipal;
    private MockedStatic<APILocator> apis;
    private MockedStatic<CompanyUtils> companies;
    private MockedStatic<DotAuthSessionCredentialProcessorImpl> sessions;
    private final LoginServiceAPI login = mock(LoginServiceAPI.class);
    private final ApiTokenAPI tokens = mock(ApiTokenAPI.class);
    private final LayoutAPI layoutAPI = mock(LayoutAPI.class);
    private final UserWebAPI userWebAPI = mock(UserWebAPI.class);
    private final JsonWebTokenAuthCredentialProcessor bearer = mock(JsonWebTokenAuthCredentialProcessor.class);
    private final HttpServletRequest request =
            new MockAttributeRequest(mock(HttpServletRequest.class)).request();
    private final HttpServletResponse response = mock(HttpServletResponse.class);
    private final User owner = mock(User.class);
    private WebResource resource;

    /** Covers absent, explicit false, invalid and enabled values under every Basic policy. */
    @Parameterized.Parameters(name = "{index}: mode={0}, URL opt-in={1}")
    public static Collection<Object[]> configurations() {
        final List<Object[]> cases = new ArrayList<>();
        for (final String mode : new String[]{"NONE", "JWT_TOKEN", "ALL"}) {
            for (final String enabled : new String[]{null, "false", "invalid", "true"}) {
                cases.add(new Object[]{mode, enabled});
            }
        }
        return cases;
    }

    /** Selects the independent URL opt-in and Basic policy. */
    public WebResourceUrlCredentialsTest(final String mode, final String enabled) {
        this.mode = mode;
        this.enabled = enabled;
    }

    /** Configures valid password and API-token identities so a rejected login cannot pass by accident. */
    @Before
    public void setUpAuthentication() throws Exception {
        previousMode = Config.getStringProperty("ALLOW_BASIC_AUTH", null);
        previousEnabled = Config.getStringProperty(PROPERTY, null);
        previousPrincipal = PrincipalThreadLocal.getName();
        Config.setProperty("ALLOW_BASIC_AUTH", mode);
        Config.setProperty(PROPERTY, enabled);
        apis = mockStatic(APILocator.class);
        companies = mockStatic(CompanyUtils.class);
        sessions = mockStatic(DotAuthSessionCredentialProcessorImpl.class);
        sessions.when(DotAuthSessionCredentialProcessorImpl::getInstance)
                .thenReturn(mock(DotAuthSessionCredentialProcessorImpl.class));
        final UserAPI userAPI = mock(UserAPI.class);
        final User anonymous = mock(User.class);
        final User system = mock(User.class);
        when(anonymous.getUserId()).thenReturn(UserAPI.CMS_ANON_USER_ID);
        when(owner.getUserId()).thenReturn(USERNAME);
        when(userAPI.getAnonymousUserNoThrow()).thenReturn(anonymous);
        when(userAPI.getSystemUser()).thenReturn(system);
        when(userAPI.loadUserById(USERNAME, system, false)).thenReturn(owner);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(login.doActionLogin(USERNAME, PASSWORD, false, request, response)).thenReturn(true);
        final ApiToken token = mock(ApiToken.class);
        when(token.getActiveUser()).thenReturn(Optional.of(owner));
        when(tokens.fromJwt(JWT, "127.0.0.1")).thenReturn(Optional.of(token));
        final Company company = mock(Company.class);
        when(company.getAuthType()).thenReturn(Company.AUTH_TYPE_ID);
        companies.when(CompanyUtils::getDefaultCompany).thenReturn(company);
        apis.when(APILocator::getLoginServiceAPI).thenReturn(login);
        apis.when(APILocator::getApiTokenAPI).thenReturn(tokens);
        apis.when(APILocator::getUserAPI).thenReturn(userAPI);
        final ApiProvider provider = mock(ApiProvider.class);
        when(provider.userAPI()).thenReturn(userAPI);
        when(provider.layoutAPI()).thenReturn(layoutAPI);
        when(provider.userWebAPI()).thenReturn(userWebAPI);
        resource = new WebResource(provider, bearer);
    }

    /** Restores configuration, static mocks and the thread-local identity after every invocation. */
    @After
    public void restoreAuthentication() {
        sessions.close();
        companies.close();
        apis.close();
        Config.setProperty("ALLOW_BASIC_AUTH", previousMode);
        Config.setProperty(PROPERTY, previousEnabled);
        PrincipalThreadLocal.setName(previousPrincipal);
    }

    /** Neither valid passwords nor API JWTs in legacy parameter maps authenticate without opt-in. */
    @Test
    public void urlCredentialsRequireBothPolicies() {
        for (final String credential : new String[]{PASSWORD, JWT}) {
            final Map<String, String> params = WebResource.buildParamsMap(
                    "/user/" + USERNAME + "/password/" + credential);
            assertAuthentication(params, "true".equals(enabled) && !"NONE".equals(mode)
                    && (JWT.equals(credential) || "ALL".equals(mode)));
        }
        if (!"true".equals(enabled) || "NONE".equals(mode)) {
            verifyNoInteractions(login, tokens);
        } else if (!"ALL".equals(mode)) {
            verifyNoInteractions(login);
        }
    }

    /** URL policy does not alter Basic or DOTAUTH headers, which still obey ALLOW_BASIC_AUTH. */
    @Test
    public void headerCredentialsAreUnchanged() {
        final String encoded = Base64.getEncoder().encodeToString(
                (USERNAME + ":" + JWT).getBytes(StandardCharsets.UTF_8));
        when(request.getHeader("Authorization")).thenReturn("Basic " + encoded);
        assertAuthentication(Collections.emptyMap(), !"NONE".equals(mode));
        when(request.getHeader("Authorization")).thenReturn(null);
        when(request.getHeader("DOTAUTH")).thenReturn(encoded);
        assertAuthentication(Collections.emptyMap(), !"NONE".equals(mode));
        verifyNoInteractions(login);
    }

    /** Bearer credentials authenticate independently of both legacy credential settings. */
    @Test
    public void bearerAuthenticationIsUnchanged() {
        when(bearer.processAuthHeaderFromJWT(request)).thenReturn(owner);
        final Map<String, String> params = "true".equals(enabled) ? Collections.emptyMap()
                : Map.of("user", "unrelated-user", "password", "wrong-secret");
        assertAuthentication(params, true);
        verifyNoInteractions(login, tokens);
    }

    /** An existing browser session remains usable with URL credentials disabled. */
    @Test
    public void sessionAuthenticationIsUnchanged() throws Exception {
        when(userWebAPI.getLoggedInUser(request)).thenReturn(owner);
        final Map<String, String> params = "true".equals(enabled) ? Collections.emptyMap()
                : Map.of("user", "unrelated-user", "password", "wrong-secret");
        assertAuthentication(params, true);
        verifyNoInteractions(login, tokens);
    }

    /** Explicit form-body credentials retain their Basic policy independently of the URL opt-in. */
    @Test
    public void formBodyCredentialsAreIndependentOfTheUrlOptIn() {
        for (final String credential : new String[]{PASSWORD, JWT}) {
            request.removeAttribute(com.liferay.portal.util.WebKeys.USER);
            request.removeAttribute(com.liferay.portal.util.WebKeys.USER_ID);
            if (!"NONE".equals(mode) && (JWT.equals(credential) || "ALL".equals(mode))) {
                final InitDataObject initialized = resource.initWithFormCredentials(USERNAME, credential,
                        request, response, null);
                assertEquals(owner, initialized.getUser());
                assertTrue(initialized.getParamsMap().isEmpty());
            } else {
                final SecurityException denied = assertThrows(SecurityException.class,
                        () -> resource.initWithFormCredentials(USERNAME, credential, request, response, null));
                assertEquals(401, denied.getResponse().getStatus());
            }
        }
    }

    /** Body credentials cannot bypass the endpoint's existing portlet permission requirement. */
    @Test
    public void formBodyCredentialsStillRequirePortletPermission() throws Exception {
        final SecurityException denied = assertThrows(SecurityException.class,
                () -> resource.initWithFormCredentials(USERNAME, JWT, request, response, "configuration"));
        assertEquals(401, denied.getResponse().getStatus());
        if (!"NONE".equals(mode)) {
            verify(layoutAPI).doesUserHaveAccessToPortlet("configuration", owner);
        }
    }

    /** The parsed-map entry point must enforce SSL even before looking for an existing user. */
    @Test
    public void parsedMapInitializationStillRequiresSsl() {
        final String previous = Config.getStringProperty("FORCE_SSL_ON_RESP_API", null);
        try {
            Config.setProperty("FORCE_SSL_ON_RESP_API", true);
            final SecurityException denied = assertThrows(SecurityException.class,
                    () -> resource.initWithMap(Collections.emptyMap(), request, response,
                            AnonymousAccess.NONE, null));
            assertEquals(403, denied.getResponse().getStatus());
            final SecurityException formDenied = assertThrows(SecurityException.class,
                    () -> resource.initWithFormCredentials(USERNAME, PASSWORD, request, response, null));
            assertEquals(403, formDenied.getResponse().getStatus());
            verifyNoInteractions(login, tokens, userWebAPI);
        } finally {
            Config.setProperty("FORCE_SSL_ON_RESP_API", previous);
        }
    }

    /** Verifies the authenticated identity or the existing strict anonymous-access denial. */
    private void assertAuthentication(final Map<String, String> params, final boolean allowed) {
        if (allowed) {
            assertEquals(owner, resource.authenticate(request, response, params, AnonymousAccess.NONE,
                    WebResource.AuthCheckOptions.SKIP_CHECK_FORCE_SSL));
        } else {
            final SecurityException denied = assertThrows(SecurityException.class,
                    () -> resource.authenticate(request, response, params, AnonymousAccess.NONE,
                            WebResource.AuthCheckOptions.SKIP_CHECK_FORCE_SSL));
            assertEquals(401, denied.getResponse().getStatus());
        }
    }
}
