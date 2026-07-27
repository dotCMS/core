package com.dotcms.filters.interceptor.saml;

import com.dotcms.cms.login.LoginServiceAPI;
import com.dotcms.filters.interceptor.Result;
import com.dotcms.saml.IdentityProviderConfiguration;
import com.dotcms.saml.IdentityProviderConfigurationFactory;
import com.dotcms.saml.SamlConfigurationService;
import com.dotcms.security.apps.AppsAPI;
import com.dotcms.util.security.Encryptor;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.business.web.HostWebAPI;
import com.dotmarketing.filters.CMSUrlUtil;
import com.dotmarketing.util.WebKeys;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the deterministic SAML auth-redirect loop-guard in
 * {@link SamlWebInterceptor#intercept(HttpServletRequest, HttpServletResponse)} (issue #32365).
 *
 * The guard breaks the infinite IdP redirect loop that occurs when an authenticated-but-unauthorized
 * user (e.g. a front-end-only SAML user sent to a back-end URL like {@code /dotAdmin}) would otherwise
 * be bounced to the IdP forever: when auto-login resolved a SAML user ({@code isAutoLogin}) yet the
 * request is still not logged in for its destination ({@code isNotLogged}), it returns a clean 403
 * instead of redirecting again.
 */
public class SamlWebInterceptorLoopGuardTest {

    /**
     * Builds a spy interceptor with the protected gate methods stubbed so {@code intercept()} reaches
     * the loop-guard branch: SAML is configured, the URL is include-path filtered (not access-filtered),
     * the user is not logged in for this destination, and auto-login resolves (or not) a SAML user.
     */
    private SamlWebInterceptor interceptorReachingGuard(final HttpServletRequest request,
                                                        final HttpServletResponse response,
                                                        final HttpSession session,
                                                        final boolean autoLoginResolvedUser) {

        final SamlWebUtils samlWebUtils      = mock(SamlWebUtils.class);
        final HostWebAPI hostWebAPI          = mock(HostWebAPI.class);
        final SamlConfigurationService samlConfigurationService = mock(SamlConfigurationService.class);
        final IdentityProviderConfigurationFactory idpFactory   = mock(IdentityProviderConfigurationFactory.class);
        final IdentityProviderConfiguration idpConfig           = mock(IdentityProviderConfiguration.class);
        final Host host                      = mock(Host.class);

        final SamlWebInterceptor interceptor = spy(new SamlWebInterceptor(
                mock(Encryptor.class), mock(LoginServiceAPI.class), mock(UserAPI.class), hostWebAPI,
                mock(AppsAPI.class), samlWebUtils, mock(CMSUrlUtil.class), idpFactory));
        interceptor.setSamlConfig(samlConfigurationService);

        when(request.getSession(false)).thenReturn(session);
        when(request.getRequestURI()).thenReturn("/dotAdmin/");
        when(hostWebAPI.getCurrentHostNoThrow(request)).thenReturn(host);
        when(host.getIdentifier()).thenReturn("123");
        when(idpFactory.findIdentityProviderConfigurationById("123")).thenReturn(idpConfig);
        when(idpConfig.isEnabled()).thenReturn(true);
        when(samlWebUtils.isNotLogged(request)).thenReturn(true);

        doReturn(true).when(interceptor).isAnySamlConfigurated();
        doReturn(false).when(interceptor).checkAccessFilters(anyString(), any(), any(), any());
        doReturn(true).when(interceptor).checkIncludePath(anyString(), any());
        doReturn(new AutoLoginResult(session, autoLoginResolvedUser)).when(interceptor)
                .autoLogin(any(), any(), any(), any());

        return interceptor;
    }

    /**
     * Method to test: {@link SamlWebInterceptor#intercept(HttpServletRequest, HttpServletResponse)}
     * Given Scenario: auto-login resolved a SAML user ({@code isAutoLogin=true}) but the request is
     * still not logged in for the destination (front-end-only user hitting a back-end URL).
     * Expected Result: the loop-guard returns HTTP 403 and {@code Result.SKIP_NO_CHAIN} instead of
     * redirecting to the IdP again.
     */
    @Test
    public void test_authenticated_but_unauthorized_breaks_loop_with_403() throws IOException {

        final HttpServletRequest request   = mock(HttpServletRequest.class);
        final HttpServletResponse response = mock(HttpServletResponse.class);
        final HttpSession session          = mock(HttpSession.class);
        when(response.isCommitted()).thenReturn(false);

        final SamlWebInterceptor interceptor = this.interceptorReachingGuard(request, response, session, true);

        final Result result = interceptor.intercept(request, response);

        assertEquals(Result.SKIP_NO_CHAIN, result);
        verify(response).sendError(HttpServletResponse.SC_FORBIDDEN);
        // The authenticated-403 path must clear any stale REDIRECT_AFTER_LOGIN (issue #36541/#32365),
        // so it can't resurface as an unwanted redirect on the next login — including for /api/* URLs
        // where the error page returns before its 403 branch would otherwise clear it.
        verify(session).removeAttribute(WebKeys.REDIRECT_AFTER_LOGIN);
    }

    /**
     * Method to test: {@link SamlWebInterceptor#intercept(HttpServletRequest, HttpServletResponse)}
     * Given Scenario: the response is already committed when the loop-guard fires.
     * Expected Result: no {@code sendError} is written (avoids IllegalStateException / double response),
     * and the interceptor still returns {@code Result.SKIP_NO_CHAIN}.
     */
    @Test
    public void test_loop_guard_does_not_write_when_response_committed() throws IOException {

        final HttpServletRequest request   = mock(HttpServletRequest.class);
        final HttpServletResponse response = mock(HttpServletResponse.class);
        final HttpSession session          = mock(HttpSession.class);
        when(response.isCommitted()).thenReturn(true);

        final SamlWebInterceptor interceptor = this.interceptorReachingGuard(request, response, session, true);

        final Result result = interceptor.intercept(request, response);

        assertEquals(Result.SKIP_NO_CHAIN, result);
        verify(response, never()).sendError(HttpServletResponse.SC_FORBIDDEN);
    }
}
