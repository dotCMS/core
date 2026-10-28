package com.dotcms.filters.interceptor.saml;

import com.dotcms.rendering.velocity.util.VelocityUtil;
import com.dotcms.saml.IdentityProviderConfiguration;
import com.dotmarketing.business.web.UserWebAPI;
import org.apache.velocity.tools.view.context.ChainedContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.MockedStatic;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for how {@link SamlWebUtils} picks the RelayState sent to the IdP when the SAML login starts
 * (issue #36591).
 *
 * The order is: a {@link RelayStateStrategy} registered for the site, then the site's
 * {@code auth.relaystate} template, then the default destination dotCMS computed for the visitor, then
 * nothing. A configured strategy or template always produces exactly what it produces today; the default is
 * only used when neither is configured.
 */
public class SamlWebUtilsRelayStateTest {

    private static final String SITE = "site-id-36591";

    private SamlWebUtils samlWebUtils;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private HttpSession session;
    private IdentityProviderConfiguration idpConfig;

    @Before
    public void setUp() {

        samlWebUtils = new SamlWebUtils(mock(UserWebAPI.class));
        request      = mock(HttpServletRequest.class);
        response     = mock(HttpServletResponse.class);
        session      = mock(HttpSession.class);
        idpConfig    = mock(IdentityProviderConfiguration.class);
        when(request.getSession()).thenReturn(session);
        // getRelayState hands the template to the strategy through a request attribute
        doAnswer(invocation -> {
            when(request.getAttribute(SamlWebUtils.AUTH_RELAYSTATE_KEY)).thenReturn(invocation.getArgument(1));
            return null;
        }).when(request).setAttribute(ArgumentMatchers.eq(SamlWebUtils.AUTH_RELAYSTATE_KEY), any());
    }

    @After
    public void tearDown() {
        SamlWebUtils.removeRelayStateStrategy(SITE);
    }

    /**
     * Given Scenario: no strategy and no template, with a default destination
     * ExpectedResult: the default is sent, and stored in the session as today
     */
    @Test
    public void noStrategyNoTemplate_sendsDefault() {

        assertEquals("/members/report?id=42", samlWebUtils.getRelayStateWithDefault(
                request, response, idpConfig, SITE, "/members/report?id=42"));
        verify(session).setAttribute(SamlWebUtils.AUTH_RELAYSTATE_KEY, "/members/report?id=42");
    }

    /**
     * Given Scenario: no strategy, no template and no default (the default was rejected)
     * ExpectedResult: no RelayState is sent, as today
     */
    @Test
    public void noStrategyNoTemplateNoDefault_sendsNothing() {

        assertNull(samlWebUtils.getRelayStateWithDefault(request, response, idpConfig, SITE, null));
    }

    /**
     * Given Scenario: a strategy registered for the site
     * ExpectedResult: the strategy's value is sent, not the default
     */
    @Test
    public void registeredStrategy_winsOverDefault() {

        SamlWebUtils.addRelayStateStrategy(SITE, (req, resp, config) -> "fromStrategy");
        assertEquals("fromStrategy", samlWebUtils.getRelayStateWithDefault(
                request, response, idpConfig, SITE, "/x"));
    }

    /**
     * Given Scenario: a strategy registered for the site that returns nothing
     * ExpectedResult: nothing is sent; the strategy owns the value, so the default must not leak in
     */
    @Test
    public void registeredStrategyReturningNull_doesNotFallBackToDefault() {

        SamlWebUtils.addRelayStateStrategy(SITE, (req, resp, config) -> null);
        assertNull(samlWebUtils.getRelayStateWithDefault(request, response, idpConfig, SITE, "/x"));
    }

    /**
     * Given Scenario: an auth.relaystate template is configured, with a default available
     * ExpectedResult: the evaluated template is sent, not the default
     */
    @Test
    public void configuredTemplate_winsOverDefault() {

        when(idpConfig.containsOptionalProperty(SamlWebUtils.AUTH_RELAYSTATE_KEY)).thenReturn(true);
        when(idpConfig.getOptionalProperty(SamlWebUtils.AUTH_RELAYSTATE_KEY)).thenReturn("/landing");

        final VelocityUtil velocityUtil = mock(VelocityUtil.class);
        when(velocityUtil.getContext(request, response)).thenReturn(mock(ChainedContext.class));
        try (MockedStatic<VelocityUtil> velocity = mockStatic(VelocityUtil.class)) {
            velocity.when(VelocityUtil::getInstance).thenReturn(velocityUtil);
            velocity.when(() -> VelocityUtil.eval(ArgumentMatchers.eq("/landing"), any())).thenReturn("/landing");

            assertEquals("/landing", samlWebUtils.getRelayStateWithDefault(
                    request, response, idpConfig, SITE, "/x"));
        }
    }

    /**
     * Given Scenario: the existing getRelayState methods, with no strategy and no template
     * ExpectedResult: they still send nothing; only the new method adds a default
     */
    @Test
    public void existingMethods_areUnchanged() {

        assertNull(samlWebUtils.getRelayState(request, response, idpConfig, SITE));
        assertNull(samlWebUtils.getRelayState(request, response, idpConfig, null, SITE));
    }

    /**
     * Method to test: {@link SamlWebUtils#configuredAllowedHosts(IdentityProviderConfiguration)}
     * Given Scenario: auth.relaystate.allowed.hosts lists two hosts with extra spaces and mixed case
     * ExpectedResult: both hosts, trimmed
     */
    @Test
    public void configuredAllowedHosts_parsesTheList() {

        allowedHostsProperty(" partner.example.org , Other.Example.NET ");
        assertEquals(List.of("partner.example.org", "Other.Example.NET"), SamlWebUtils.configuredAllowedHosts(idpConfig));
    }

    /**
     * Method to test: {@link SamlWebUtils#configuredAllowedHosts(IdentityProviderConfiguration)}
     * Given Scenario: the property is absent or blank
     * ExpectedResult: no external hosts
     */
    @Test
    public void configuredAllowedHosts_absentOrBlank_isEmpty() {

        assertTrue(SamlWebUtils.configuredAllowedHosts(idpConfig).isEmpty());
        allowedHostsProperty("   ");
        assertTrue(SamlWebUtils.configuredAllowedHosts(idpConfig).isEmpty());
    }

    /**
     * Method to test: {@link SamlWebUtils#configuredAllowedHosts(IdentityProviderConfiguration)}
     * Given Scenario: entries with wildcards, schemes, paths, spaces, user-info or ports
     * ExpectedResult: they are ignored, so the list can only name exact hosts
     */
    @Test
    public void configuredAllowedHosts_ignoresInvalidEntries() {

        allowedHostsProperty("*.example.org, https://x.org, x.org/path, a b, user@x.org, x.org:8443, ok.example.org");
        assertEquals(List.of("ok.example.org"), SamlWebUtils.configuredAllowedHosts(idpConfig));
    }

    /**
     * Method to test: {@link SamlWebUtils#isBackEndLoginPage(String)}
     * Given Scenario: a request URI naming the back-end with different letter case
     * ExpectedResult: the interceptor's check stays case-sensitive, so which sign-in flow a request gets is
     * unchanged; only the post-login redirect check ignores case (PR #37984 review)
     */
    @Test
    public void isBackEndLoginPage_staysCaseSensitive() {

        assertTrue(samlWebUtils.isBackEndLoginPage("/dotAdmin/"));
        assertFalse(samlWebUtils.isBackEndLoginPage("/DotAdmin/"));
        assertFalse(samlWebUtils.isBackEndLoginPage("/C/Portal/Logout"));
    }

    private void allowedHostsProperty(final String value) {
        when(idpConfig.containsOptionalProperty(SamlWebUtils.AUTH_RELAYSTATE_ALLOWED_HOSTS_KEY)).thenReturn(true);
        when(idpConfig.getOptionalProperty(SamlWebUtils.AUTH_RELAYSTATE_ALLOWED_HOSTS_KEY)).thenReturn(value);
    }
}
