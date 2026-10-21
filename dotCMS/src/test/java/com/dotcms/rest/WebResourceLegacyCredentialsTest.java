package com.dotcms.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.dotcms.UnitTestBase;
import com.dotcms.auth.providers.jwt.beans.ApiToken;
import com.dotcms.auth.providers.jwt.factories.ApiTokenAPI;
import com.dotcms.cms.login.LoginServiceAPI;
import com.dotcms.rest.exception.SecurityException;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.util.CompanyUtils;
import com.dotmarketing.util.Config;
import com.liferay.portal.model.Company;
import com.liferay.portal.model.User;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;

/**
 * Pins the routing between API-JWT verification and the legacy password login when
 * {@code ALLOW_BASIC_AUTH=ALL}. Only credentials that can be a compact JWS (the shape the API token
 * factory issues: exactly two {@code '.'} separators) reach the JWT verifier, so an ordinary
 * password login no longer produces a spurious "JWT Failed" audit entry. A JWT-shaped credential is
 * still fully verified, and a verification failure still falls back to the password pipeline.
 */
public class WebResourceLegacyCredentialsTest extends UnitTestBase {

    private static final String USERNAME = "test-user";
    private static final String IP = "127.0.0.1";

    private String previousMode;

    /** Runs every case under the compatibility mode unless the case overrides it. */
    @Before
    public void saveState() {
        previousMode = Config.getStringProperty("ALLOW_BASIC_AUTH", null);
        Config.setProperty("ALLOW_BASIC_AUTH", "ALL");
    }

    /** Restores the global authentication mode even when an assertion fails. */
    @After
    public void restoreState() {
        Config.setProperty("ALLOW_BASIC_AUTH", previousMode);
    }

    /**
     * An ordinary password cannot be a compact JWS, so it must go straight to the password login
     * and never touch the JWT verifier (which would log a false "JWT Failed").
     */
    @Test
    public void ordinary_passwords_skip_the_jwt_verifier_and_use_password_login() throws Exception {
        final String[] passwords = {"a-password", "p@ss.word", "many.dots.in.a.password", "secret:with:colon"};
        for (final String password : passwords) {
            final LoginServiceAPI loginService = mock(LoginServiceAPI.class);
            final ApiTokenAPI tokens = mock(ApiTokenAPI.class);
            final UserAPI userAPI = mock(UserAPI.class);
            final User systemUser = mock(User.class);
            final User user = mock(User.class);
            final Company company = mock(Company.class);
            final HttpServletRequest request = mock(HttpServletRequest.class);
            final HttpServletResponse response = mock(HttpServletResponse.class);
            when(request.getRemoteAddr()).thenReturn(IP);
            when(userAPI.getSystemUser()).thenReturn(systemUser);
            when(userAPI.loadUserById(USERNAME, systemUser, false)).thenReturn(user);
            when(company.getAuthType()).thenReturn(Company.AUTH_TYPE_ID);
            when(loginService.doActionLogin(USERNAME, password, false, request, response)).thenReturn(true);

            try (MockedStatic<APILocator> apis = mockStatic(APILocator.class);
                    MockedStatic<CompanyUtils> companies = mockStatic(CompanyUtils.class)) {
                apis.when(APILocator::getLoginServiceAPI).thenReturn(loginService);
                apis.when(APILocator::getApiTokenAPI).thenReturn(tokens);
                companies.when(CompanyUtils::getDefaultCompany).thenReturn(company);

                assertEquals(user, WebResource.authenticateUser(USERNAME, password, request, response, userAPI));
            }

            verify(loginService).doActionLogin(USERNAME, password, false, request, response);
            // The regression: current code calls fromJwt for every password, logging a false JWT attack.
            verifyNoInteractions(tokens);
        }
    }

    /**
     * A compact JWS password is verified by the JWT verifier first; a valid owner is returned
     * without entering the password pipeline or minting a session/cookie.
     */
    @Test
    public void compact_api_token_owner_uses_jwt_verifier_without_password_login() throws Exception {
        final String token = "header.payload.signature";
        final LoginServiceAPI loginService = mock(LoginServiceAPI.class);
        final ApiTokenAPI tokens = mock(ApiTokenAPI.class);
        final UserAPI userAPI = mock(UserAPI.class);
        final User owner = mock(User.class);
        final ApiToken apiToken = mock(ApiToken.class);
        when(owner.getUserId()).thenReturn(USERNAME);
        when(apiToken.getActiveUser()).thenReturn(Optional.of(owner));
        when(tokens.fromJwt(token, IP)).thenReturn(Optional.of(apiToken));
        final Company company = mock(Company.class);
        when(company.getAuthType()).thenReturn(Company.AUTH_TYPE_ID);
        final HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn(IP);
        final HttpServletResponse response = mock(HttpServletResponse.class);

        final User result;
        try (MockedStatic<APILocator> apis = mockStatic(APILocator.class);
                MockedStatic<CompanyUtils> companies = mockStatic(CompanyUtils.class)) {
            apis.when(APILocator::getLoginServiceAPI).thenReturn(loginService);
            apis.when(APILocator::getApiTokenAPI).thenReturn(tokens);
            companies.when(CompanyUtils::getDefaultCompany).thenReturn(company);

            result = WebResource.authenticateUser(USERNAME, token, request, response, userAPI);
        }

        assertEquals(owner, result);
        verify(tokens).fromJwt(token, IP);
        verifyNoInteractions(loginService, response);
        verify(request, never()).getSession();
        verify(request, never()).getSession(true);
    }

    /**
     * A JWT-shaped but invalid candidate must still be verified by the JWT verifier and, in ALL
     * mode only, then fall back to the legacy password login (no silent downgrade).
     */
    @Test
    public void compact_invalid_candidate_is_verified_then_falls_back_to_password_login() throws Exception {
        final String candidate = "e30.e30.invalid";
        final LoginServiceAPI loginService = mock(LoginServiceAPI.class);
        final ApiTokenAPI tokens = mock(ApiTokenAPI.class);
        final UserAPI userAPI = mock(UserAPI.class);
        final User systemUser = mock(User.class);
        final User user = mock(User.class);
        when(userAPI.getSystemUser()).thenReturn(systemUser);
        when(userAPI.loadUserById(USERNAME, systemUser, false)).thenReturn(user);
        when(tokens.fromJwt(candidate, IP)).thenReturn(Optional.empty());
        final Company company = mock(Company.class);
        when(company.getAuthType()).thenReturn(Company.AUTH_TYPE_ID);
        final HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn(IP);
        final HttpServletResponse response = mock(HttpServletResponse.class);
        when(loginService.doActionLogin(USERNAME, candidate, false, request, response)).thenReturn(true);

        final User result;
        try (MockedStatic<APILocator> apis = mockStatic(APILocator.class);
                MockedStatic<CompanyUtils> companies = mockStatic(CompanyUtils.class)) {
            apis.when(APILocator::getLoginServiceAPI).thenReturn(loginService);
            apis.when(APILocator::getApiTokenAPI).thenReturn(tokens);
            companies.when(CompanyUtils::getDefaultCompany).thenReturn(company);

            result = WebResource.authenticateUser(USERNAME, candidate, request, response, userAPI);
        }

        assertEquals(user, result);
        verify(tokens).fromJwt(candidate, IP);
        verify(loginService).doActionLogin(USERNAME, candidate, false, request, response);
    }

    /**
     * JWT_TOKEN mode must keep routing every credential through the JWT verifier and reject a
     * non-token without ever entering the password pipeline.
     */
    @Test
    public void jwt_token_mode_routes_non_token_to_verifier_and_never_logs_in() throws Exception {
        Config.setProperty("ALLOW_BASIC_AUTH", "JWT_TOKEN");
        final String candidate = "not-a-jwt";
        final LoginServiceAPI loginService = mock(LoginServiceAPI.class);
        final ApiTokenAPI tokens = mock(ApiTokenAPI.class);
        final UserAPI userAPI = mock(UserAPI.class);
        when(tokens.fromJwt(candidate, IP)).thenReturn(Optional.empty());
        final HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn(IP);
        final HttpServletResponse response = mock(HttpServletResponse.class);

        try (MockedStatic<APILocator> apis = mockStatic(APILocator.class);
                MockedStatic<CompanyUtils> companies = mockStatic(CompanyUtils.class)) {
            apis.when(APILocator::getLoginServiceAPI).thenReturn(loginService);
            apis.when(APILocator::getApiTokenAPI).thenReturn(tokens);

            final SecurityException denied = assertThrows(SecurityException.class,
                    () -> WebResource.authenticateUser(USERNAME, candidate, request, response, userAPI));
            assertEquals(401, denied.getResponse().getStatus());
        }

        verify(tokens).fromJwt(candidate, IP);
        verifyNoInteractions(loginService, response);
        verify(request, never()).getSession();
        verify(request, never()).getSession(true);
    }
}
