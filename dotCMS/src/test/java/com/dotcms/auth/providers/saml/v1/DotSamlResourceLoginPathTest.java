package com.dotcms.auth.providers.saml.v1;

import com.dotcms.filters.interceptor.saml.SamlWebUtils;
import com.dotcms.rest.WebResource;
import com.dotcms.saml.IdentityProviderConfiguration;
import com.dotcms.saml.IdentityProviderConfigurationFactory;
import com.dotcms.saml.SamlAuthenticationService;
import com.dotcms.saml.SamlConfigurationService;
import com.dotcms.util.RedirectUtil;
import com.dotmarketing.business.web.UserWebAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.SecurityLogger;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.model.User;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Answers;
import org.mockito.MockedStatic;
import org.mockito.invocation.InvocationOnMock;

import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the SAML callback's post-login destination decision,
 * {@link DotSamlResource#resolveLoginPath(HttpServletRequest, HttpSession, IdentityProviderConfiguration, User)}
 * (issue #36591).
 *
 * The callback follows the RelayState, then the session's saved destination, then the configured
 * {@code redirect.after.login}, then {@code /dotAdmin/}. The first two can carry values a visitor
 * controls, so they are only followed when they are a same-host destination; otherwise the next source is
 * tried and the rejection is security-logged without the rejected value. The back-end role check runs on
 * the normalized path, so dot segments cannot slip a front-end-only user into the back-end or deny them a
 * front-end page.
 *
 * Log assertions capture every {@link Logger} and {@link SecurityLogger} call through static mocks and
 * resolve any {@link Supplier} arguments, so a rejected value cannot reach a log line by any route.
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
     * Given Scenario: the IdP echoes a relative same-host RelayState
     * ExpectedResult: the user is sent to exactly that page
     */
    @Test
    public void relayStateRelativePath_isFollowed() {

        relayState("/members/report?id=42");
        assertRedirect("/members/report?id=42", resolve(admin()));
    }

    /**
     * Given Scenario: the RelayState is an absolute URL on the callback's own host
     * ExpectedResult: it is followed, reduced to its relative path
     */
    @Test
    public void relayStateAbsoluteSameHost_isFollowedAsRelativePath() {

        relayState("https://site.example.com/members/report");
        assertRedirect("/members/report", resolve(admin()));
    }

    /**
     * Given Scenario: a protocol-relative RelayState, with a safe destination saved in the session
     * ExpectedResult: the RelayState is ignored and the session destination is used
     */
    @Test
    public void relayStateProtocolRelative_fallsBackToSession() {

        relayState("//evil.com");
        sessionRedirect("/members/a");
        assertRedirect("/members/a", resolve(admin()));
    }

    /**
     * Given Scenario: an off-host RelayState, nothing in the session, and redirect.after.login configured
     * ExpectedResult: the configured destination is used
     */
    @Test
    public void relayStateOtherHost_fallsBackToConfig() {

        relayState("https://evil.com");
        redirectAfterLoginConfig("/landing");
        assertRedirect("/landing", resolve(admin()));
    }

    /**
     * Given Scenario: a RelayState that climbs above the root, and no other destination
     * ExpectedResult: the built-in /dotAdmin/ default is used for an admin
     */
    @Test
    public void relayStateAboveRoot_fallsBackToDefault() {

        relayState("/page1/../../x");
        assertRedirect("/dotAdmin/", resolve(admin()));
    }

    /**
     * Given Scenario: the session holds an off-host destination (e.g. from a crafted referrer parameter)
     * ExpectedResult: it is not followed, and the session attribute is still cleared
     */
    @Test
    public void sessionOtherHost_isNotFollowedAndIsCleared() {

        sessionRedirect("https://evil.com");
        final DotSamlResource.LoginDestination destination = resolve(admin());
        assertFalse(destination.isDenied());
        assertFalse(destination.path(), destination.path().contains("evil.com"));
        verify(session).removeAttribute(WebKeys.REDIRECT_AFTER_LOGIN);
    }

    /**
     * Given Scenario: the session holds a safe destination
     * ExpectedResult: it is followed and the session attribute is cleared
     */
    @Test
    public void sessionSameHost_isFollowedAndCleared() {

        sessionRedirect("/members/a");
        assertRedirect("/members/a", resolve(admin()));
        verify(session).removeAttribute(WebKeys.REDIRECT_AFTER_LOGIN);
    }

    /**
     * Given Scenario: the destination already carries the query string the session also kept (AC-007)
     * ExpectedResult: the query string appears exactly once
     */
    @Test
    public void forwardQueryString_isNotDuplicated() {

        relayState("/members/report?id=42");
        when(session.getAttribute(RequestDispatcher.FORWARD_QUERY_STRING)).thenReturn("id=42");
        assertRedirect("/members/report?id=42", resolve(admin()));
    }

    /**
     * Given Scenario: the destination has no query string and the session kept one
     * ExpectedResult: the kept query string is appended, as today
     */
    @Test
    public void forwardQueryString_isAppendedWhenDestinationHasNone() {

        relayState("/members/report");
        when(session.getAttribute(RequestDispatcher.FORWARD_QUERY_STRING)).thenReturn("id=42");
        assertRedirect("/members/report?id=42", resolve(admin()));
    }

    /**
     * Given Scenario: a front-end-only user whose RelayState uses ".." to reach the back-end (AC-005)
     * ExpectedResult: the path is treated as /dotAdmin/ and the login is denied
     */
    @Test
    public void frontEndUser_dotSegmentsIntoBackEnd_isDenied() {

        relayState("/members/../dotAdmin/");
        assertTrue(resolve(frontEndOnly()).isDenied());
    }

    /**
     * Given Scenario: a front-end-only user whose RelayState hides ".." behind an encoded slash to reach the
     * back-end: "/members/..%2f..%2fdotAdmin/" (PR #37984 review)
     * ExpectedResult: the RelayState is not followed; with no other destination the user lands on "/"
     */
    @Test
    public void frontEndUser_encodedSlashIntoBackEnd_isNotFollowed() {

        relayState("/members/..%2f..%2fdotAdmin/");
        assertRedirect("/", resolve(frontEndOnly()));
    }

    /**
     * Given Scenario: a front-end-only user whose RelayState names the back-end with different letter case
     * (PR #37984 review); a proxy or CDN that normalizes case would still route it to the back-end
     * ExpectedResult: the path counts as back-end and the login is denied
     */
    @Test
    public void frontEndUser_caseVariedBackEndPath_isDenied() {

        relayState("/DotAdmin/");
        assertTrue(resolve(frontEndOnly()).isDenied());
    }

    /**
     * Given Scenario: a front-end-only user whose RelayState is a back-end path followed by a query that
     * holds ".." segments and a stray "%" (PR #37984 review)
     * ExpectedResult: the path is back-end, so the login is denied
     */
    @Test
    public void frontEndUser_queryHidesBackEndPath_isDenied() {

        relayState("/dotAdmin?/../%");
        assertTrue(resolve(frontEndOnly()).isDenied());
    }

    /**
     * Given Scenario: a front-end-only user whose RelayState starts with /dotAdmin but resolves to a
     * front-end page (AC-005)
     * ExpectedResult: the user is sent to the front-end page, not denied
     */
    @Test
    public void frontEndUser_dotSegmentsOutOfBackEnd_isAllowed() {

        relayState("/dotAdmin/../members/report");
        assertRedirect("/members/report", resolve(frontEndOnly()));
    }

    /**
     * Given Scenario: an admin whose RelayState is the back-end
     * ExpectedResult: the admin goes to /dotAdmin/
     */
    @Test
    public void admin_backEnd_isAllowed() {

        relayState("/dotAdmin/");
        assertRedirect("/dotAdmin/", resolve(admin()));
    }

    /**
     * Given Scenario: an IdP-initiated login (no RelayState, no session, no config) by a front-end-only
     * user with front-end SSO enabled
     * ExpectedResult: unchanged behavior, the user lands on "/"
     */
    @Test
    public void frontEndUser_intentUnknown_frontEndEnabled_goesToRoot() {

        when(idpConfig.containsOptionalProperty("enableFrontend")).thenReturn(true);
        when(idpConfig.getOptionalProperty("enableFrontend")).thenReturn("true");
        assertRedirect("/", resolve(frontEndOnly()));
    }

    /**
     * Given Scenario: an IdP-initiated login (no destination known) by a front-end-only user, with enableFrontend
     * not set (configuration A, how most existing customers are configured)
     * ExpectedResult: the user lands on the site home, a safe page they can use, instead of a 403 for a back-end
     * page they never asked for (Phase 14)
     */
    @Test
    public void frontEndUser_intentUnknown_frontEndDisabled_goesToRoot() {

        assertRedirect("/", resolve(frontEndOnly()));
    }

    /**
     * Given Scenario: a front-end-only user whose RelayState is rejected (another host), nothing else to go by,
     * enableFrontend not set (manual scenario M3)
     * ExpectedResult: the rejected value is ignored and the user lands on the site home, not a 403
     */
    @Test
    public void frontEndUser_rejectedRelayState_goesToRoot() {

        relayState("https://evil.com");
        assertRedirect("/", resolve(frontEndOnly()));
    }

    /**
     * Given Scenario: the same with enableFrontend=true (configuration B)
     * ExpectedResult: the site home, as before
     */
    @Test
    public void frontEndUser_rejectedRelayState_frontEndEnabled_goesToRoot() {

        when(idpConfig.containsOptionalProperty("enableFrontend")).thenReturn(true);
        when(idpConfig.getOptionalProperty("enableFrontend")).thenReturn("true");
        relayState("https://evil.com");
        assertRedirect("/", resolve(frontEndOnly()));
    }

    /**
     * Given Scenario: a front-end-only user who explicitly asked for the back-end (manual scenario M4)
     * ExpectedResult: still denied: the destination is known and not allowed for this user
     */
    @Test
    public void frontEndUser_knownBackEndDestination_isDenied() {

        relayState("/dotAdmin/");
        assertTrue(resolve(frontEndOnly()).isDenied());
    }

    /**
     * Given Scenario: a front-end-only user, no RelayState, and the admin configured redirect.after.login to a
     * back-end path
     * ExpectedResult: denied: an admin-configured back-end destination is a known intent, not a fallback
     */
    @Test
    public void frontEndUser_configuredBackEndDestination_isDenied() {

        redirectAfterLoginConfig("/dotAdmin/");
        assertTrue(resolve(frontEndOnly()).isDenied());
    }

    /**
     * Given Scenario: an admin with no destination known
     * ExpectedResult: the back-end, as before
     */
    @Test
    public void admin_intentUnknown_goesToBackEnd() {

        assertRedirect("/dotAdmin/", resolve(admin()));
    }

    /**
     * Given Scenario: unsafe values in both the RelayState and the session
     * ExpectedResult: no log line, debug or security, contains either rejected value; each rejection is
     * still recorded in the security log, with the reason it was rejected
     */
    @Test
    public void rejectedValues_areNeverLogged() {

        final String badRelayState = "https://evil.com/steal-relay";
        final String badSession    = "//evil.com/steal-session";
        relayState(badRelayState);
        sessionRedirect(badSession);

        final List<String> logged = new ArrayList<>();
        final List<String> securityLogged = new ArrayList<>();
        try (MockedStatic<Logger> logger = mockStatic(Logger.class, invocation -> record(invocation, logged));
             MockedStatic<SecurityLogger> securityLogger = mockStatic(SecurityLogger.class,
                     invocation -> record(invocation, securityLogged))) {
            resolve(admin());
        }

        final List<String> all = new ArrayList<>(logged);
        all.addAll(securityLogged);
        for (final String line : all) {
            assertFalse(line, line.contains("steal-relay") || line.contains("steal-session"));
        }
        assertFalse("rejections must be security-logged", securityLogged.isEmpty());
        // each rejection says why: the RelayState pointed to another host, the session value was protocol-relative
        final String allSecurityLogs = String.join("\n", securityLogged);
        assertTrue(allSecurityLogs, allSecurityLogs.contains(RedirectUtil.RedirectRejection.OTHER_HOST.name()));
        assertTrue(allSecurityLogs, allSecurityLogs.contains(RedirectUtil.RedirectRejection.PROTOCOL_RELATIVE.name()));
    }

    /**
     * Given Scenario: the IdP sends a RelayState on another alias of the site being logged in to
     * (e.g. the IdP's default RelayState), while the callback arrives on the main hostname
     * ExpectedResult: it is followed, reduced to its relative path (upgrade compatibility)
     */
    @Test
    public void relayStateOnSiteAlias_isFollowed() {

        doReturn(List.of(HOST, "www.example.com")).when(resource).allowedRedirectHosts(any(), any());
        relayState("https://www.example.com/members/report");
        assertRedirect("/members/report", resolve(admin()));
    }

    /**
     * Given Scenario: the RelayState points at a host that is not the site or one of its aliases
     * ExpectedResult: it is not followed, and the security log names OTHER_HOST
     */
    @Test
    public void relayStateOnOtherSite_fallsBackAndIsLogged() {

        doReturn(List.of(HOST, "www.example.com")).when(resource).allowedRedirectHosts(any(), any());
        relayState("https://other.example.org/x");

        final List<String> securityLogged = new ArrayList<>();
        final DotSamlResource.LoginDestination destination;
        try (MockedStatic<SecurityLogger> securityLogger = mockStatic(SecurityLogger.class,
                invocation -> record(invocation, securityLogged))) {
            destination = resolve(admin());
        }

        assertRedirect("/dotAdmin/", destination);
        final String allSecurityLogs = String.join("\n", securityLogged);
        assertTrue(allSecurityLogs, allSecurityLogs.contains(RedirectUtil.RedirectRejection.OTHER_HOST.name()));
    }

    /**
     * Given Scenario: the RelayState points to an external host listed in auth.relaystate.allowed.hosts
     * ExpectedResult: the user is sent to that absolute URL (backward-compatibility valve)
     */
    @Test
    public void relayStateOnAllowedExternalHost_isFollowedAsAbsoluteUrl() {

        allowedHostsProperty("partner.example.org");
        relayState("https://partner.example.org/welcome");
        assertRedirect("https://partner.example.org/welcome", resolve(admin()));
    }

    /**
     * Given Scenario: the same RelayState, but no allowed external hosts configured
     * ExpectedResult: rejected as another host; the fallback is used and the rejection is security-logged
     */
    @Test
    public void relayStateOnExternalHost_notListed_fallsBack() {

        relayState("https://partner.example.org/welcome");

        final List<String> securityLogged = new ArrayList<>();
        final DotSamlResource.LoginDestination destination;
        try (MockedStatic<SecurityLogger> securityLogger = mockStatic(SecurityLogger.class,
                invocation -> record(invocation, securityLogged))) {
            destination = resolve(admin());
        }

        assertRedirect("/dotAdmin/", destination);
        assertTrue(String.join("\n", securityLogged).contains(RedirectUtil.RedirectRejection.OTHER_HOST.name()));
    }

    /**
     * Given Scenario: the session's saved destination is on an allowed external host
     * ExpectedResult: it is followed as an absolute URL
     */
    @Test
    public void sessionOnAllowedExternalHost_isFollowed() {

        allowedHostsProperty("partner.example.org");
        sessionRedirect("https://partner.example.org/w");
        assertRedirect("https://partner.example.org/w", resolve(admin()));
    }

    private void allowedHostsProperty(final String value) {
        when(idpConfig.containsOptionalProperty(SamlWebUtils.AUTH_RELAYSTATE_ALLOWED_HOSTS_KEY)).thenReturn(true);
        when(idpConfig.getOptionalProperty(SamlWebUtils.AUTH_RELAYSTATE_ALLOWED_HOSTS_KEY)).thenReturn(value);
    }

    /**
     * Method to test: {@link DotSamlResource#isBackEndLogin(String)}
     * Given Scenario: paths whose dot segments change which side they point to (trusted
     * redirect.after.login values are not validated, so they must still be normalized here)
     * ExpectedResult: the decision follows the normalized path
     */
    @Test
    public void isBackEndLoginNormalizes() {

        assertTrue(resource.isBackEndLogin("/members/../dotAdmin/"));
        assertFalse(resource.isBackEndLogin("/dotAdmin/../members/report"));
    }

    /**
     * Method to test: {@link DotSamlResource#isBackEndLogin(String)}
     * Given Scenario: a trusted redirect.after.login value whose ".." segments are hidden behind encoded slashes
     * or encoded dots (PR #37984 review); the decision must follow the decoded, normalized path
     * ExpectedResult: "/members/..%2f..%2fdotAdmin/" and "/members/%2e%2e/dotAdmin/" count as back-end
     */
    @Test
    public void isBackEndLoginDecodesBeforeNormalizing() {

        assertTrue(resource.isBackEndLogin("/members/..%2f..%2fdotAdmin/"));
        assertTrue(resource.isBackEndLogin("/members/x/..%2F..%2FdotAdmin/"));
        assertTrue(resource.isBackEndLogin("/members/%2e%2e/dotAdmin/"));
        assertFalse(resource.isBackEndLogin("/members/report%2Fid"));
    }

    /**
     * Method to test: {@link DotSamlResource#isBackEndLogin(String)}
     * Given Scenario: back-end paths written with different letter case (PR #37984 review)
     * ExpectedResult: they count as back-end, so the no-access role check applies; a front-end page whose
     * name merely contains a back-end segment does not
     */
    @Test
    public void isBackEndLoginIgnoresCase() {

        assertTrue(resource.isBackEndLogin("/DotAdmin/"));
        assertTrue(resource.isBackEndLogin("/DOTADMIN/#/c/content"));
        assertTrue(resource.isBackEndLogin("/members/../dotadmin/"));
        assertTrue(resource.isBackEndLogin("/C/Portal/Logout"));
        assertFalse(resource.isBackEndLogin("/members/DotAdmin/"));
    }

    /**
     * Method to test: {@link DotSamlResource#isBackEndLogin(String)}
     * Given Scenario: a back-end path followed by a query or fragment that holds ".." segments, including one
     * whose stray "%" made the old parser fall back to the raw string (PR #37984 review)
     * ExpectedResult: only the path decides; the query and fragment never make a path back-end or front-end
     */
    @Test
    public void isBackEndLoginIgnoresQueryAndFragment() {

        assertTrue(resource.isBackEndLogin("/dotAdmin?/../%"));
        assertTrue(resource.isBackEndLogin("/dotAdmin#/../%"));
        assertTrue(resource.isBackEndLogin("/dotAdmin/?x=/../../members"));
        assertFalse(resource.isBackEndLogin("/members?next=/dotAdmin/"));
    }

    /**
     * Method to test: {@link DotSamlResource#isBackEndLogin(String)}
     * Given Scenario: a path with a broken percent-escape (only a trusted redirect.after.login can carry one;
     * RelayState and session values are rejected earlier as MALFORMED_ENCODING)
     * ExpectedResult: treated as back-end, so the role check applies (fail closed)
     */
    @Test
    public void isBackEndLoginMalformedPath_failsClosed() {

        assertTrue(resource.isBackEndLogin("/members/report%"));
        assertTrue(resource.isBackEndLogin("/members/%zz"));
    }

    private DotSamlResource.LoginDestination resolve(final User user) {
        return resource.resolveLoginPath(request, session, idpConfig, user);
    }

    private void relayState(final String value) {
        when(request.getParameter("RelayState")).thenReturn(value);
    }

    private void sessionRedirect(final String value) {
        when(session.getAttribute(WebKeys.REDIRECT_AFTER_LOGIN)).thenReturn(value);
    }

    private void redirectAfterLoginConfig(final String value) {
        when(idpConfig.containsOptionalProperty(DotSamlResource.REDIRECT_AFTER_LOGIN_CONFIG)).thenReturn(true);
        when(idpConfig.getOptionalProperty(DotSamlResource.REDIRECT_AFTER_LOGIN_CONFIG)).thenReturn(value);
    }

    private static void assertRedirect(final String expected, final DotSamlResource.LoginDestination destination) {
        assertFalse("expected a redirect to " + expected + " but the login was denied", destination.isDenied());
        assertEquals(expected, destination.path());
    }

    private static User admin() {
        final User user = mock(User.class);
        when(user.isAdmin()).thenReturn(true);
        when(user.isBackendUser()).thenReturn(true);
        return user;
    }

    private static User frontEndOnly() {
        final User user = mock(User.class);
        when(user.isFrontendUser()).thenReturn(true);
        return user;
    }

    private static Object record(final InvocationOnMock invocation, final List<String> sink) throws Throwable {
        for (final Object argument : invocation.getArguments()) {
            if (argument instanceof Supplier) {
                sink.add(String.valueOf(((Supplier<?>) argument).get()));
            } else if (argument instanceof String) {
                sink.add((String) argument);
            }
        }
        return Answers.RETURNS_DEFAULTS.answer(invocation);
    }
}
