package com.dotcms.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.dotcms.UnitTestBase;
import com.dotcms.auth.dotAuth.rest.DotAuthSessionCredentialProcessorImpl;
import com.dotcms.auth.providers.jwt.JsonWebTokenAuthCredentialProcessor;
import com.dotcms.auth.providers.jwt.beans.ApiToken;
import com.dotcms.auth.providers.jwt.beans.UserToken;
import com.dotcms.auth.providers.jwt.factories.ApiTokenAPI;
import com.dotcms.cms.login.LoginServiceAPI;
import com.dotcms.rest.exception.SecurityException;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.ApiProvider;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.business.web.UserWebAPI;
import com.dotmarketing.util.CompanyUtils;
import com.dotmarketing.util.Config;
import com.liferay.portal.auth.PrincipalThreadLocal;
import com.liferay.portal.model.Company;
import com.liferay.portal.model.User;
import com.liferay.portal.util.WebKeys;
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

/** Verifies that restricted credential transports cannot enter the password login pipeline. */
@RunWith(Parameterized.class)
public class WebResourceAuthenticationTest extends UnitTestBase {

    private final String mode;
    private final String transport;
    private String previousMode;
    private String previousPrincipal;

    /**
     * Supplies every restrictive mode plus the absent-property default through every live
     * username/password transport. The default resolves to {@code JWT_TOKEN}: an API JWT as the
     * Basic password validates like a Bearer header, while a real password is still rejected.
     */
    @Parameterized.Parameters(name = "{index}: mode={0}, transport={1}")
    public static Collection<Object[]> restrictiveModes() {
        final List<Object[]> cases = new ArrayList<>();
        for (final String mode : new String[]{null, "NONE", "FALSE", "", "invalid", "JWT_TOKEN", " jwt_token "}) {
            for (final String transport : new String[]{"Basic", "DOTAUTH", "params"}) {
                cases.add(new Object[]{mode, transport});
            }
        }
        return cases;
    }

    /** Selects a configuration mode and a credential transport for this test invocation. */
    public WebResourceAuthenticationTest(final String mode, final String transport) {
        this.mode = mode;
        this.transport = transport;
    }

    /** Saves the configuration and principal before changing authentication policy. */
    @Before
    public void saveState() {
        previousMode = Config.getStringProperty("ALLOW_BASIC_AUTH", null);
        previousPrincipal = PrincipalThreadLocal.getName();
        Config.setProperty("ALLOW_BASIC_AUTH", mode);
    }

    /** Restores global state even when verifier-isolation assertions fail. */
    @After
    public void restoreState() {
        Config.setProperty("ALLOW_BASIC_AUTH", previousMode);
        PrincipalThreadLocal.setName(previousPrincipal);
    }

    /** A verified API token must match the company's login convention and never invoke login. */
    @Test
    public void api_jwt_requires_matching_owner_and_rejects_browser_tokens() throws Exception {
        final LoginServiceAPI loginService = mock(LoginServiceAPI.class);
        final ApiTokenAPI tokens = mock(ApiTokenAPI.class);
        final UserAPI userAPI = mock(UserAPI.class);
        final User owner = mock(User.class);
        when(owner.getUserId()).thenReturn("token-owner");
        when(owner.getEmailAddress()).thenReturn("owner@example.com");
        final ApiToken apiToken = mock(ApiToken.class);
        when(apiToken.getActiveUser()).thenReturn(Optional.of(owner));
        when(tokens.fromJwt("api-jwt", "127.0.0.1")).thenReturn(Optional.of(apiToken));
        when(tokens.fromJwt("browser-jwt", "127.0.0.1"))
                .thenReturn(Optional.of(mock(UserToken.class)));
        final HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        final HttpServletResponse response = mock(HttpServletResponse.class);
        final Company company = mock(Company.class);
        try (MockedStatic<APILocator> apis = mockStatic(APILocator.class);
                MockedStatic<CompanyUtils> companies = mockStatic(CompanyUtils.class)) {
            apis.when(APILocator::getApiTokenAPI).thenReturn(tokens);
            apis.when(APILocator::getLoginServiceAPI).thenReturn(loginService);
            companies.when(CompanyUtils::getDefaultCompany).thenReturn(company);
            for (final String authType : new String[]{Company.AUTH_TYPE_EA, Company.AUTH_TYPE_ID}) {
                when(company.getAuthType()).thenReturn(authType);
                final String username = Company.AUTH_TYPE_EA.equals(authType)
                        ? "OWNER@example.com" : owner.getUserId();
                // null exercises the absent-property default, which resolves to JWT_TOKEN.
                final boolean tokenMode = mode == null || "JWT_TOKEN".equalsIgnoreCase(mode.trim());
                if (tokenMode) {
                    assertEquals(owner, WebResource.authenticateUser(username, " api-jwt ",
                            request, response, userAPI));
                } else {
                    assertThrows(SecurityException.class, () -> WebResource.authenticateUser(
                            username, "api-jwt", request, response, userAPI));
                }
                assertThrows(SecurityException.class, () -> WebResource.authenticateUser(
                        "another-user", "api-jwt", request, response, userAPI));
                assertThrows(SecurityException.class, () -> WebResource.authenticateUser(
                        username, "browser-jwt", request, response, userAPI));
            }
            verifyNoInteractions(loginService, response);
            verify(request, never()).getSession();
            verify(request, never()).getSession(true);
        }
    }

    /** Passwords and rejected token candidates must not invoke login hooks or mint a session. */
    @Test
    public void restrictive_modes_never_enter_password_login() throws Exception {
        final LoginServiceAPI loginService = mock(LoginServiceAPI.class);
        final UserAPI userAPI = mock(UserAPI.class);
        final User anonymous = mock(User.class);
        when(anonymous.getUserId()).thenReturn(UserAPI.CMS_ANON_USER_ID);
        when(userAPI.getAnonymousUserNoThrow()).thenReturn(anonymous);
        final ApiProvider provider = mock(ApiProvider.class);
        when(provider.userAPI()).thenReturn(userAPI);
        when(provider.userWebAPI()).thenReturn(mock(UserWebAPI.class));

        try (MockedStatic<APILocator> apis = mockStatic(APILocator.class);
                MockedStatic<DotAuthSessionCredentialProcessorImpl> sessions =
                        mockStatic(DotAuthSessionCredentialProcessorImpl.class)) {
            apis.when(APILocator::getLoginServiceAPI).thenReturn(loginService);
            apis.when(APILocator::getUserAPI).thenReturn(userAPI);
            apis.when(APILocator::getApiTokenAPI).thenReturn(mock(ApiTokenAPI.class));
            sessions.when(DotAuthSessionCredentialProcessorImpl::getInstance)
                    .thenReturn(mock(DotAuthSessionCredentialProcessorImpl.class));
            final WebResource resource = new WebResource(provider,
                    mock(JsonWebTokenAuthCredentialProcessor.class));

            for (final String credential : new String[]{"a-password", "not-a-jwt", "e30.e30.invalid"}) {
                final HttpServletRequest request = mock(HttpServletRequest.class);
                final HttpServletResponse response = mock(HttpServletResponse.class);
                final String encoded = Base64.getEncoder().encodeToString(
                        ("test-user@example.com:" + credential).getBytes(StandardCharsets.UTF_8));
                Map<String, String> params = Collections.emptyMap();
                if ("params".equals(transport)) {
                    params = Map.of("user", "test-user@example.com", "password", credential);
                } else {
                    when(request.getHeader("Basic".equals(transport) ? "Authorization" : "DOTAUTH"))
                            .thenReturn("Basic".equals(transport) ? "Basic " + encoded : encoded);
                }
                final Map<String, String> credentials = params;
                try {
                    final SecurityException denied = assertThrows(SecurityException.class,
                            () -> resource.authenticate(request, response, credentials,
                                    AnonymousAccess.NONE, WebResource.AuthCheckOptions.SKIP_CHECK_FORCE_SSL));
                    assertEquals(401, denied.getResponse().getStatus());
                } finally {
                    // A 401 alone also passes when the password pipeline was incorrectly called.
                    verifyNoInteractions(loginService);
                    verifyNoInteractions(response);
                    verify(request, never()).setAttribute(eq(WebKeys.USER_ID), any());
                    verify(request, never()).getSession();
                    verify(request, never()).getSession(true);
                }
            }
        }
    }
}
