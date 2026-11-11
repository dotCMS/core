package com.dotcms.filters.interceptor.saml;

import com.dotcms.cms.login.LoginServiceAPI;
import com.dotcms.saml.DotSamlProxyFactory;
import com.dotcms.saml.IdentityProviderConfiguration;
import com.dotcms.saml.IdentityProviderConfigurationFactory;
import com.dotcms.saml.SamlAuthenticationService;
import com.dotcms.saml.SamlConfigurationService;
import com.dotcms.saml.SamlName;
import com.dotcms.security.apps.AppsAPI;
import com.dotcms.util.SecurityUtils;
import com.dotcms.util.security.Encryptor;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.business.web.HostWebAPI;
import com.dotmarketing.filters.CMSUrlUtil;
import com.dotmarketing.util.WebKeys;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the default RelayState that {@link SamlWebInterceptor} sends when it starts a SAML login
 * (issue #36591).
 *
 * The default is the visitor's destination: the {@code referrer} parameter when present, otherwise the
 * intercepted URI and query string. It is only sent when it is a safe same-host destination, so an unsafe
 * referrer results in no default at all (today's behavior). The session attributes written before the
 * redirect to the IdP are unchanged.
 */
public class SamlWebInterceptorDefaultRelayStateTest {

    private static final String SITE_ID = "123";

    private HttpServletRequest request;
    private HttpServletResponse response;
    private HttpSession session;
    private SamlWebUtils samlWebUtils;
    private IdentityProviderConfiguration idpConfig;
    private SamlWebInterceptor interceptor;
    private SamlConfigurationService samlConfig;

    @Before
    public void setUp() {

        request      = mock(HttpServletRequest.class);
        response     = mock(HttpServletResponse.class);
        session      = mock(HttpSession.class);
        samlWebUtils = mock(SamlWebUtils.class);
        idpConfig    = mock(IdentityProviderConfiguration.class);

        final HostWebAPI hostWebAPI = mock(HostWebAPI.class);
        final IdentityProviderConfigurationFactory idpFactory = mock(IdentityProviderConfigurationFactory.class);
        final Host host = mock(Host.class);

        interceptor = spy(new SamlWebInterceptor(
                mock(Encryptor.class), mock(LoginServiceAPI.class), mock(UserAPI.class), hostWebAPI,
                mock(AppsAPI.class), samlWebUtils, mock(CMSUrlUtil.class), idpFactory));
        samlConfig = mock(SamlConfigurationService.class);
        interceptor.setSamlConfig(samlConfig);

        when(request.getSession(false)).thenReturn(session);
        when(request.getServerName()).thenReturn("site.example.com");
        when(hostWebAPI.getCurrentHostNoThrow(request)).thenReturn(host);
        when(host.getIdentifier()).thenReturn(SITE_ID);
        when(idpFactory.findIdentityProviderConfigurationById(SITE_ID)).thenReturn(idpConfig);
        when(idpConfig.isEnabled()).thenReturn(true);
        when(samlWebUtils.isNotLogged(request)).thenReturn(true);

        doReturn(true).when(interceptor).isAnySamlConfigurated();
        doReturn(false).when(interceptor).checkAccessFilters(anyString(), any(), any(), any());
        doReturn(true).when(interceptor).checkIncludePath(anyString(), any());
        // auto-login did not resolve a SAML user, so intercept() starts a new IdP login
        doReturn(new AutoLoginResult(session, false)).when(interceptor).autoLogin(any(), any(), any(), any());
        doNothing().when(interceptor).doRequestLoginSecurityLog(any(), any());
        // the real lookup loads the site's aliases through APILocator, which is not available in unit tests
        doReturn(List.of("site.example.com")).when(interceptor).allowedRedirectHosts(any(), any());
    }

    /**
     * Given Scenario: a signed-out visitor opens a front-end page with a query string
     * ExpectedResult: that page and query string are sent as the default RelayState (AC-001)
     */
    @Test
    public void originalRequest_isSentAsDefault() throws Exception {

        request("/members/report", "id=42");
        assertEquals("/members/report?id=42", defaultRelayStateSent());
    }

    /**
     * Given Scenario: a login page with a referrer parameter
     * ExpectedResult: the referrer is sent as the default RelayState (AC-009)
     */
    @Test
    public void referrer_isSentAsDefault() throws Exception {

        request("/dotCMS/login", "referrer=/members/other");
        when(request.getParameter(SamlWebInterceptor.REFERRER_PARAMETER_KEY)).thenReturn("/members/other");
        assertEquals("/members/other", defaultRelayStateSent());
    }

    /**
     * Given Scenario: a referrer pointing to another host
     * ExpectedResult: no default RelayState is sent (AC-009)
     */
    @Test
    public void offHostReferrer_sendsNoDefault() throws Exception {

        request("/dotCMS/login", "referrer=https://evil.com");
        when(request.getParameter(SamlWebInterceptor.REFERRER_PARAMETER_KEY)).thenReturn("https://evil.com");
        assertNull(defaultRelayStateSent());
    }

    /**
     * Given Scenario: a request URI whose dot segments climb above the root
     * ExpectedResult: no default RelayState is sent
     */
    @Test
    public void aboveRootPath_sendsNoDefault() throws Exception {

        request("/page1/../../x", null);
        assertNull(defaultRelayStateSent());
    }

    /**
     * Given Scenario: a destination longer than the 80-byte SAML RelayState limit
     * ExpectedResult: it is sent in full, not truncated (FR-007)
     */
    @Test
    public void longDestination_isSentInFull() throws Exception {

        final String query = "q=" + "a".repeat(120);
        request("/", query);
        assertEquals("/?" + query, defaultRelayStateSent());
    }

    /**
     * Given Scenario: any login start
     * ExpectedResult: the session still receives REDIRECT_AFTER_LOGIN and original_request exactly as before
     */
    @Test
    public void sessionAttributes_areUnchanged() throws Exception {

        request("/members/report", "id=42");
        defaultRelayStateSent();
        verify(session).setAttribute(WebKeys.REDIRECT_AFTER_LOGIN, "/members/report?id=42");
        verify(session).setAttribute(SamlWebInterceptor.ORIGINAL_REQUEST, "/members/report?id=42");
    }

    /**
     * Given Scenario: the usual front-end route: a 401 on /members/report?id=42 redirected to
     * /dotCMS/login?referrer=/members/report and kept the query string only in the session (Phase 8, AC-001)
     * ExpectedResult: the saved query string is reattached, so the IdP echoes the full destination back
     */
    @Test
    public void loginRoute_reattachesSavedQueryString() throws Exception {

        loginRoute("/members/report", "/members/report", "id=42");
        assertEquals("/members/report?id=42", defaultRelayStateSent());
    }

    /**
     * Given Scenario: the session holds a query string saved for another page
     * ExpectedResult: no stale query string is attached to the referrer
     */
    @Test
    public void loginRoute_staleQueryForOtherPath_isNotAttached() throws Exception {

        loginRoute("/members/report", "/other", "id=42");
        assertEquals("/members/report", defaultRelayStateSent());
    }

    /**
     * Given Scenario: the referrer already carries its own query string
     * ExpectedResult: the referrer's query wins and nothing is added
     */
    @Test
    public void loginRoute_referrerWithQuery_isKeptAsIs() throws Exception {

        loginRoute("/members/report?x=1", "/members/report?x=1", "id=42");
        assertEquals("/members/report?x=1", defaultRelayStateSent());
    }

    /**
     * Given Scenario: the 401 saved no query string
     * ExpectedResult: the referrer is sent as is
     */
    @Test
    public void loginRoute_noSavedQuery_sendsReferrer() throws Exception {

        loginRoute("/members/report", "/members/report", null);
        assertEquals("/members/report", defaultRelayStateSent());
    }

    /**
     * Given Scenario: the saved query string contains a forbidden character
     * ExpectedResult: the combined destination fails the same-host check, so no default is sent
     */
    @Test
    public void loginRoute_unsafeSavedQuery_sendsNoDefault() throws Exception {

        loginRoute("/members/report", "/members/report", "id=<x>");
        assertNull(defaultRelayStateSent());
    }

    /**
     * Given Scenario: the interceptor catches the protected page directly (include.path.values covers it)
     * ExpectedResult: the page and its query string are sent, as before
     */
    @Test
    public void directInterception_isUnchanged() throws Exception {

        request("/members/report", "id=42");
        assertEquals("/members/report?id=42", defaultRelayStateSent());
    }

    /**
     * Given Scenario: a signed-out visitor (e.g. an expired session) opens a logout URL, including one from
     * a configured logout.path.values (analysis C1)
     * ExpectedResult: no default RelayState, so the user is not sent back to the logout URL after signing in
     */
    @Test
    public void logoutPages_sendNoDefault() throws Exception {

        for (final String logoutUri : new String[] {"/dotCMS/logout", "/c/portal/logout", "/dotAdmin/logout", "/custom/logout"}) {
            setUp();
            when(samlConfig.getConfigAsString(any(), eq(SamlName.DOT_SAML_LOGOUT_PATH_VALUES))).thenReturn("/custom/logout");
            request(logoutUri, null);
            assertNull(logoutUri, defaultRelayStateSent());
        }
    }

    /**
     * Given Scenario: a signed-out visitor opens a login or logout page written with different letter case,
     * including a configured logout.path.values entry (PR #37984 review)
     * ExpectedResult: no default RelayState, so the user is not sent back to that page after signing in
     */
    @Test
    public void caseVariedLoginAndLogoutPages_sendNoDefault() throws Exception {

        for (final String uri : new String[] {"/DotCMS/Login", "/APPLICATION/login", "/C/Portal/Logout", "/Custom/Logout"}) {
            setUp();
            when(samlConfig.getConfigAsString(any(), eq(SamlName.DOT_SAML_LOGOUT_PATH_VALUES))).thenReturn("/custom/logout");
            request(uri, null);
            assertNull(uri, defaultRelayStateSent());
        }
    }

    /**
     * Given Scenario: a signed-out visitor opens a login page with no referrer (analysis C1)
     * ExpectedResult: no default RelayState; the existing fallbacks apply after the callback
     */
    @Test
    public void loginPages_sendNoDefault() throws Exception {

        for (final String loginUri : new String[] {"/dotCMS/login", "/html/portal/login", "/c/public/login",
                "/c/portal_public/login", "/application/login/login"}) {
            setUp();
            request(loginUri, null);
            assertNull(loginUri, defaultRelayStateSent());
        }
    }

    /**
     * Given Scenario: the referrer itself points at a logout URL
     * ExpectedResult: no default RelayState
     */
    @Test
    public void referrerToLogout_sendsNoDefault() throws Exception {

        request("/dotCMS/login", "referrer=/dotCMS/logout");
        when(request.getParameter(SamlWebInterceptor.REFERRER_PARAMETER_KEY)).thenReturn("/dotCMS/logout");
        assertNull(defaultRelayStateSent());
    }

    /**
     * Given Scenario: a signed-out admin opens the back-end
     * ExpectedResult: /dotAdmin/ is never excluded; it is still sent as the default
     */
    @Test
    public void backEnd_isNotExcluded() throws Exception {

        request("/dotAdmin/", null);
        assertEquals("/dotAdmin/", defaultRelayStateSent());
    }

    /**
     * Given Scenario: only the older javax.servlet.forward.query_string session attribute holds the query
     * (e.g. written by a custom 401 flow)
     * ExpectedResult: it is still reattached, as a fallback to the dedicated key
     */
    @Test
    public void loginRoute_fallsBackToForwardQueryStringAttribute() throws Exception {

        request("/dotCMS/login", "referrer=/members/report");
        when(request.getParameter(SamlWebInterceptor.REFERRER_PARAMETER_KEY)).thenReturn("/members/report");
        when(session.getAttribute(WebKeys.REDIRECT_AFTER_LOGIN)).thenReturn("/members/report");
        when(session.getAttribute(RequestDispatcher.FORWARD_QUERY_STRING)).thenReturn("id=42");
        assertEquals("/members/report?id=42", defaultRelayStateSent());
    }

    /**
     * Given Scenario: the query string was saved but not its path (e.g. a 401 raised before the path was
     * kept), while REDIRECT_AFTER_LOGIN holds the matching path (non-Redis sessions)
     * ExpectedResult: REDIRECT_AFTER_LOGIN is used for the match, and the query string is reattached
     */
    @Test
    public void loginRoute_fallsBackToRedirectAfterLoginForTheMatch() throws Exception {

        request("/dotCMS/login", "referrer=/members/report");
        when(request.getParameter(SamlWebInterceptor.REFERRER_PARAMETER_KEY)).thenReturn("/members/report");
        when(session.getAttribute(WebKeys.REDIRECT_AFTER_LOGIN)).thenReturn("/members/report");
        when(session.getAttribute(SecurityUtils.REDIRECT_AFTER_LOGIN_QUERY_STRING)).thenReturn("id=42");
        assertEquals("/members/report?id=42", defaultRelayStateSent());
    }

    /**
     * Given Scenario: the SAML app sets auth.relaystate.default=false (Phase 12 off switch)
     * ExpectedResult: no default RelayState is sent, exactly as before the fix
     */
    @Test
    public void offSwitch_false_sendsNoDefault() throws Exception {

        defaultRelayStateProperty("false");
        request("/members/report", "id=42");
        assertNull(defaultRelayStateSent());
    }

    /**
     * Given Scenario: the off switch with different case and surrounding spaces
     * ExpectedResult: still recognized as off
     */
    @Test
    public void offSwitch_caseAndSpaces_sendsNoDefault() throws Exception {

        defaultRelayStateProperty(" FALSE ");
        request("/members/report", "id=42");
        assertNull(defaultRelayStateSent());
    }

    /**
     * Given Scenario: the property is explicitly true
     * ExpectedResult: the default is sent
     */
    @Test
    public void offSwitch_true_sendsDefault() throws Exception {

        defaultRelayStateProperty("true");
        request("/members/report", "id=42");
        assertEquals("/members/report?id=42", defaultRelayStateSent());
    }

    /**
     * Given Scenario: the property is not set (every existing configuration)
     * ExpectedResult: the default is sent
     */
    @Test
    public void offSwitch_absent_sendsDefault() throws Exception {

        request("/members/report", "id=42");
        assertEquals("/members/report?id=42", defaultRelayStateSent());
    }

    /**
     * Given Scenario: the property holds an unexpected value (e.g. a typo)
     * ExpectedResult: only an explicit "false" turns the default off, so it is still sent
     */
    @Test
    public void offSwitch_unexpectedValue_sendsDefault() throws Exception {

        defaultRelayStateProperty("nope");
        request("/members/report", "id=42");
        assertEquals("/members/report?id=42", defaultRelayStateSent());
    }

    /**
     * Given Scenario: the off switch on the 401 route with a saved query string
     * ExpectedResult: no default is sent, and the usual session writes still happen
     */
    @Test
    public void offSwitch_loginRoute_sendsNoDefault_butKeepsSessionWrites() throws Exception {

        defaultRelayStateProperty("false");
        loginRoute("/members/report", "/members/report", "id=42");
        assertNull(defaultRelayStateSent());
        verify(session).setAttribute(WebKeys.REDIRECT_AFTER_LOGIN, "/members/report");
        verify(session).setAttribute(SamlWebInterceptor.ORIGINAL_REQUEST, "/dotCMS/login?referrer=/members/report");
    }

    private void defaultRelayStateProperty(final String value) {
        when(idpConfig.containsOptionalProperty(SamlWebUtils.AUTH_RELAYSTATE_DEFAULT_KEY)).thenReturn(true);
        when(idpConfig.getOptionalProperty(SamlWebUtils.AUTH_RELAYSTATE_DEFAULT_KEY)).thenReturn(value);
    }

    /**
     * Given Scenario: the referrer points to an external host listed in auth.relaystate.allowed.hosts
     * ExpectedResult: it is sent as the default RelayState, as an absolute URL
     */
    @Test
    public void referrerOnAllowedExternalHost_isSentAsDefault() throws Exception {

        when(idpConfig.containsOptionalProperty(SamlWebUtils.AUTH_RELAYSTATE_ALLOWED_HOSTS_KEY)).thenReturn(true);
        when(idpConfig.getOptionalProperty(SamlWebUtils.AUTH_RELAYSTATE_ALLOWED_HOSTS_KEY)).thenReturn("partner.example.org");
        request("/dotCMS/login", "referrer=https://partner.example.org/x");
        when(request.getParameter(SamlWebInterceptor.REFERRER_PARAMETER_KEY)).thenReturn("https://partner.example.org/x");
        assertEquals("https://partner.example.org/x", defaultRelayStateSent());
    }

    /**
     * Given Scenario: the same referrer with no allowed external hosts configured
     * ExpectedResult: no default RelayState
     */
    @Test
    public void referrerOnExternalHost_notListed_sendsNoDefault() throws Exception {

        request("/dotCMS/login", "referrer=https://partner.example.org/x");
        when(request.getParameter(SamlWebInterceptor.REFERRER_PARAMETER_KEY)).thenReturn("https://partner.example.org/x");
        assertNull(defaultRelayStateSent());
    }

    /** The 401 route: /dotCMS/login?referrer=..., with what the 401 saved in the session. */
    private void loginRoute(final String referrer, final String savedPath, final String savedQuery) {
        request("/dotCMS/login", "referrer=" + referrer);
        when(request.getParameter(SamlWebInterceptor.REFERRER_PARAMETER_KEY)).thenReturn(referrer);
        // as with Redis sessions: only what was saved where the 401 was raised persists, not the
        // error page's REDIRECT_AFTER_LOGIN
        when(session.getAttribute(SecurityUtils.REDIRECT_AFTER_LOGIN_QUERY_PATH)).thenReturn(savedPath);
        when(session.getAttribute(SecurityUtils.REDIRECT_AFTER_LOGIN_QUERY_STRING)).thenReturn(savedQuery);
    }

    private void request(final String uri, final String queryString) {
        when(request.getRequestURI()).thenReturn(uri);
        when(request.getQueryString()).thenReturn(queryString);
    }

    /** Runs intercept() up to the IdP redirect and returns the default RelayState handed to SamlWebUtils. */
    private String defaultRelayStateSent() throws Exception {

        final DotSamlProxyFactory proxyFactory = mock(DotSamlProxyFactory.class);
        when(proxyFactory.samlAuthenticationService()).thenReturn(mock(SamlAuthenticationService.class));
        try (MockedStatic<DotSamlProxyFactory> factory = mockStatic(DotSamlProxyFactory.class)) {
            factory.when(DotSamlProxyFactory::getInstance).thenReturn(proxyFactory);
            interceptor.intercept(request, response);
        }

        final ArgumentCaptor<String> defaultRelayState = ArgumentCaptor.forClass(String.class);
        verify(samlWebUtils).getRelayStateWithDefault(eq(request), eq(response), eq(idpConfig), eq(SITE_ID),
                defaultRelayState.capture());
        return defaultRelayState.getValue();
    }
}
