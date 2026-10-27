package com.dotcms.auth.providers.saml.v1;

import com.dotcms.filters.interceptor.saml.SamlWebUtils;
import com.dotcms.rest.WebResource;
import com.dotcms.saml.DotSamlConstants;
import com.dotcms.saml.IdentityProviderConfiguration;
import com.dotcms.saml.IdentityProviderConfigurationFactory;
import com.dotcms.saml.SamlAuthenticationService;
import com.dotcms.saml.SamlConfigurationService;
import com.dotmarketing.business.web.UserWebAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.util.WebKeys;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the SAML callback's post-login destination decision,
 * {@link DotSamlResource#resolveLoginPath(HttpServletRequest, HttpSession, IdentityProviderConfiguration)}
 * (private issue #879).
 *
 * The callback follows the RelayState, then the session's saved destination, then the configured
 * {@code redirect.after.login}, then {@code /dotAdmin/}. The first two can carry values a visitor controls, so they
 * are only followed when they are a destination on this site (or on a host listed in
 * {@code auth.relaystate.allowed.hosts}); otherwise the next source is tried.
 */
public class DotSamlResourceLoginPathTest {

    private static final String HOST = "site.example.com";

    private DotSamlResource resource;
    private HttpServletRequest request;
    private HttpSession session;
    private IdentityProviderConfiguration idpConfig;

    @Before
    public void setUp() {

        try (MockedStatic<WebAPILocator> webApiLocator = mockStatic(WebAPILocator.class)) {
            webApiLocator.when(WebAPILocator::getUserWebAPI).thenReturn(mock(UserWebAPI.class));
            resource = spy(new DotSamlResource(mock(SamlConfigurationService.class), mock(SAMLHelper.class),
                    mock(SamlAuthenticationService.class), mock(IdentityProviderConfigurationFactory.class),
                    mock(WebResource.class)));
        }

        request   = mock(HttpServletRequest.class);
        session   = mock(HttpSession.class);
        idpConfig = mock(IdentityProviderConfiguration.class);
        when(request.getServerName()).thenReturn(HOST);
        // the real lookup loads the site's aliases through APILocator, which is not available in unit tests
        doReturn(List.of(HOST)).when(resource).allowedRedirectHosts(any(), any());
    }

    /**
     * Given Scenario: the IdP echoes a relative RelayState
     * ExpectedResult: the user is sent to exactly that page
     */
    @Test
    public void relayStateRelativePath_isFollowed() {

        when(request.getParameter("RelayState")).thenReturn("/members/report?id=42");

        assertEquals("/members/report?id=42", resource.resolveLoginPath(request, session, idpConfig));
    }

    /**
     * Given Scenario: the RelayState is an absolute URL on the callback's own host
     * ExpectedResult: it is followed, reduced to its relative path
     */
    @Test
    public void relayStateAbsoluteSameHost_isFollowedAsRelativePath() {

        when(request.getParameter("RelayState")).thenReturn("https://site.example.com/members/report");

        assertEquals("/members/report", resource.resolveLoginPath(request, session, idpConfig));
    }

    /**
     * Given Scenario: the RelayState points to another host, protocol-relative or absolute
     * ExpectedResult: it is ignored and the default back-end page is used
     */
    @Test
    public void relayStateOtherHost_isIgnored() {

        for (final String candidate : List.of("//evil.example.org/x", "https://evil.example.org/x",
                "https://site.example.com.evil.example.org/x", "https://user@site.example.com/x")) {

            when(request.getParameter("RelayState")).thenReturn(candidate);
            assertEquals(candidate, DotSamlConstants.DEFAULT_LOGIN_PATH,
                    resource.resolveLoginPath(request, session, idpConfig));
        }
    }

    /**
     * Given Scenario: the RelayState carries markup, script or a quote that could break out of an HTML attribute
     * ExpectedResult: it is ignored and the default back-end page is used
     */
    @Test
    public void relayStateWithMarkupOrScript_isIgnored() {

        for (final String candidate : List.of("/a\"><script>alert(1)</script>", "javascript:alert(1)",
                "/a'onmouseover='alert(1)", "/a<b>", "/a`b")) {

            when(request.getParameter("RelayState")).thenReturn(candidate);
            assertEquals(candidate, DotSamlConstants.DEFAULT_LOGIN_PATH,
                    resource.resolveLoginPath(request, session, idpConfig));
        }
    }

    /**
     * Given Scenario: an unsafe RelayState, with a safe destination saved in the session
     * ExpectedResult: the session destination is used, and it is removed from the session
     */
    @Test
    public void unsafeRelayState_fallsBackToSessionDestination() {

        when(request.getParameter("RelayState")).thenReturn("//evil.example.org/x");
        when(session.getAttribute(WebKeys.REDIRECT_AFTER_LOGIN)).thenReturn("/members/home");

        assertEquals("/members/home", resource.resolveLoginPath(request, session, idpConfig));
        verify(session).removeAttribute(WebKeys.REDIRECT_AFTER_LOGIN);
    }

    /**
     * Given Scenario: no RelayState, and an unsafe destination saved in the session (the original request's
     * referrer is attacker controlled), with {@code redirect.after.login} configured
     * ExpectedResult: the session value is dropped and the configured page is used
     */
    @Test
    public void unsafeSessionDestination_fallsBackToConfiguredRedirect() {

        when(session.getAttribute(WebKeys.REDIRECT_AFTER_LOGIN)).thenReturn("\"><img src=x onerror=alert(1)>");
        when(idpConfig.containsOptionalProperty(DotSamlResource.REDIRECT_AFTER_LOGIN_CONFIG)).thenReturn(true);
        when(idpConfig.getOptionalProperty(DotSamlResource.REDIRECT_AFTER_LOGIN_CONFIG)).thenReturn("/welcome");

        assertEquals("/welcome", resource.resolveLoginPath(request, session, idpConfig));
        verify(session).removeAttribute(WebKeys.REDIRECT_AFTER_LOGIN);
    }

    /**
     * Given Scenario: no source gives a destination
     * ExpectedResult: the default back-end page is used
     */
    @Test
    public void noDestination_usesDefaultLoginPath() {

        assertEquals(DotSamlConstants.DEFAULT_LOGIN_PATH, resource.resolveLoginPath(request, session, idpConfig));
        verify(session, never()).removeAttribute(WebKeys.REDIRECT_AFTER_LOGIN);
    }

    /**
     * Given Scenario: an admin listed a host in {@code auth.relaystate.allowed.hosts}, and the RelayState points to it
     * ExpectedResult: it is followed as an absolute URL; hosts not on the list are still ignored
     */
    @Test
    public void relayStateOnAllowedExternalHost_isFollowed() {

        when(idpConfig.containsOptionalProperty(SamlWebUtils.AUTH_RELAYSTATE_ALLOWED_HOSTS_KEY)).thenReturn(true);
        when(idpConfig.getOptionalProperty(SamlWebUtils.AUTH_RELAYSTATE_ALLOWED_HOSTS_KEY))
                .thenReturn("app.partner.example.net");

        when(request.getParameter("RelayState")).thenReturn("https://app.partner.example.net/landing");
        assertEquals("https://app.partner.example.net/landing", resource.resolveLoginPath(request, session, idpConfig));

        when(request.getParameter("RelayState")).thenReturn("https://other.partner.example.net/landing");
        assertEquals(DotSamlConstants.DEFAULT_LOGIN_PATH, resource.resolveLoginPath(request, session, idpConfig));
    }
}
