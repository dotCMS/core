package com.dotcms.rest.api.v1.analytics.content.util;

import com.dotcms.experiments.business.ExperimentsAPI.Health;
import com.dotcms.rest.api.v1.analytics.event.EventAnalyticsProxyHelper;
import com.dotcms.security.apps.AppsAPI;
import com.dotcms.security.apps.AppSecrets;
import com.dotcms.security.apps.Secret;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.liferay.portal.model.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ContentAnalyticsUtil#isAppConfigured(Host)} and
 * {@link ContentAnalyticsUtil#resolveAnalyticsHealth(Host)}.
 *
 * <p>Both methods are used by the feature-gate logic in {@code ExperimentsResource} and
 * {@code EventAnalyticsProxyResource} to decide whether the Content Analytics app is
 * configured for a site before forwarding requests to the upstream event manager.
 *
 * @author dotCMS
 * @since Oct 2026
 */
public class ContentAnalyticsUtilTest {

    private MockedStatic<APILocator> apiLocatorMock;
    private MockedStatic<EventAnalyticsProxyHelper> proxyHelperMock;
    private AppsAPI appsAPI;
    private Host site;
    private User systemUser;

    @BeforeEach
    void setUp() {
        appsAPI = mock(AppsAPI.class);
        site = mock(Host.class);
        when(site.getIdentifier()).thenReturn("site-test-id");
        systemUser = mock(User.class);

        apiLocatorMock = mockStatic(APILocator.class);
        apiLocatorMock.when(APILocator::getAppsAPI).thenReturn(appsAPI);
        apiLocatorMock.when(APILocator::systemUser).thenReturn(systemUser);

        proxyHelperMock = mockStatic(EventAnalyticsProxyHelper.class);
    }

    @AfterEach
    void tearDown() {
        proxyHelperMock.close();
        apiLocatorMock.close();
    }

    // --- isAppConfigured(Host) ---

    /**
     * Method to test: {@link ContentAnalyticsUtil#isAppConfigured(Host)}
     * Given scenario: App secrets map is empty — the App is not installed for this site.
     * Expected result: {@code false}.
     */
    @Test
    void isAppConfigured_emptySecretsMap_returnsFalse() throws Exception {
        when(appsAPI.getSecrets(ContentAnalyticsUtil.CONTENT_ANALYTICS_APP_KEY, true, site, systemUser))
                .thenReturn(Optional.empty());

        assertFalse(ContentAnalyticsUtil.isAppConfigured(site),
                "Empty secrets map must return false");
    }

    /**
     * Method to test: {@link ContentAnalyticsUtil#isAppConfigured(Host)}
     * Given scenario: Secrets map present but {@code siteAuth} value is blank.
     * Expected result: {@code false} — a blank siteAuth is not a usable credential.
     */
    @Test
    void isAppConfigured_siteAuthBlank_returnsFalse() throws Exception {
        final Secret siteAuth = blankSecret();
        final Secret bearerToken = secretWithValue("valid-bearer");
        stubSecrets(Map.of(
                ContentAnalyticsUtil.SITE_AUTH_KEY, siteAuth,
                ContentAnalyticsUtil.BEARER_TOKEN_KEY, bearerToken));

        assertFalse(ContentAnalyticsUtil.isAppConfigured(site),
                "Blank siteAuth must return false");
    }

    /**
     * Method to test: {@link ContentAnalyticsUtil#isAppConfigured(Host)}
     * Given scenario: Secrets map present but {@code bearerToken} value is blank.
     * Expected result: {@code false} — a blank bearerToken is not a usable credential.
     */
    @Test
    void isAppConfigured_bearerTokenBlank_returnsFalse() throws Exception {
        final Secret siteAuth = secretWithValue("valid-site-auth");
        final Secret bearerToken = blankSecret();
        stubSecrets(Map.of(
                ContentAnalyticsUtil.SITE_AUTH_KEY, siteAuth,
                ContentAnalyticsUtil.BEARER_TOKEN_KEY, bearerToken));

        assertFalse(ContentAnalyticsUtil.isAppConfigured(site),
                "Blank bearerToken must return false");
    }

    /**
     * Method to test: {@link ContentAnalyticsUtil#isAppConfigured(Host)}
     * Given scenario: Both {@code siteAuth} and {@code bearerToken} are non-blank.
     * Expected result: {@code true} — all required credentials are present and usable.
     */
    @Test
    void isAppConfigured_bothCredentialsNonBlank_returnsTrue() throws Exception {
        stubValidCredentials();

        assertTrue(ContentAnalyticsUtil.isAppConfigured(site),
                "Both credentials non-blank must return true");
    }

    // --- resolveAnalyticsHealth(Host) ---

    /**
     * Method to test: {@link ContentAnalyticsUtil#resolveAnalyticsHealth(Host)}
     * Given scenario: Secrets map is empty.
     * Expected result: {@link Health#NOT_CONFIGURED}.
     */
    @Test
    void resolveAnalyticsHealth_emptySecrets_returnsNotConfigured() throws Exception {
        when(appsAPI.getSecrets(ContentAnalyticsUtil.CONTENT_ANALYTICS_APP_KEY, true, site, systemUser))
                .thenReturn(Optional.empty());

        assertEquals(Health.NOT_CONFIGURED,
                ContentAnalyticsUtil.resolveAnalyticsHealth(site),
                "Empty secrets must yield NOT_CONFIGURED");
    }

    /**
     * Method to test: {@link ContentAnalyticsUtil#resolveAnalyticsHealth(Host)}
     * Given scenario: Secrets present but credential values are blank.
     * Expected result: {@link Health#CONFIGURATION_ERROR}.
     */
    @Test
    void resolveAnalyticsHealth_credentialsBlank_returnsConfigurationError() throws Exception {
        stubSecrets(Map.of(
                ContentAnalyticsUtil.SITE_AUTH_KEY, blankSecret(),
                ContentAnalyticsUtil.BEARER_TOKEN_KEY, secretWithValue("valid")));

        assertEquals(Health.CONFIGURATION_ERROR,
                ContentAnalyticsUtil.resolveAnalyticsHealth(site),
                "Blank credentials must yield CONFIGURATION_ERROR");
    }

    /**
     * Method to test: {@link ContentAnalyticsUtil#resolveAnalyticsHealth(Host)}
     * Given scenario: Credentials present and non-blank, but the upstream health check fails.
     * Expected result: {@link Health#CONFIGURATION_ERROR} — credentials exist but backend is unreachable.
     */
    @Test
    void resolveAnalyticsHealth_credentialsPresent_healthCheckFails_returnsConfigurationError()
            throws Exception {
        stubValidCredentials();
        proxyHelperMock.when(EventAnalyticsProxyHelper::healthCheck).thenReturn(false);

        assertEquals(Health.CONFIGURATION_ERROR,
                ContentAnalyticsUtil.resolveAnalyticsHealth(site),
                "Failing health check with valid credentials must yield CONFIGURATION_ERROR");
    }

    /**
     * Method to test: {@link ContentAnalyticsUtil#resolveAnalyticsHealth(Host)}
     * Given scenario: Credentials present and non-blank, and the upstream health check passes.
     * Expected result: {@link Health#OK} — fully configured and backend reachable.
     */
    @Test
    void resolveAnalyticsHealth_credentialsPresent_healthCheckPasses_returnsOk()
            throws Exception {
        stubValidCredentials();
        proxyHelperMock.when(EventAnalyticsProxyHelper::healthCheck).thenReturn(true);

        assertEquals(Health.OK,
                ContentAnalyticsUtil.resolveAnalyticsHealth(site),
                "Passing health check with valid credentials must yield OK");
    }

    // --- helpers ---

    private void stubValidCredentials() throws Exception {
        stubSecrets(Map.of(
                ContentAnalyticsUtil.SITE_AUTH_KEY, secretWithValue("valid-site-auth"),
                ContentAnalyticsUtil.BEARER_TOKEN_KEY, secretWithValue("valid-bearer-token")));
    }

    private void stubSecrets(final Map<String, Secret> secretMap) throws Exception {
        final AppSecrets secrets = mock(AppSecrets.class);
        when(secrets.getSecrets()).thenReturn(secretMap);
        when(appsAPI.getSecrets(ContentAnalyticsUtil.CONTENT_ANALYTICS_APP_KEY, true, site, systemUser))
                .thenReturn(Optional.of(secrets));
    }

    private static Secret secretWithValue(final String value) {
        final Secret s = mock(Secret.class);
        when(s.getString()).thenReturn(value);
        return s;
    }

    private static Secret blankSecret() {
        return secretWithValue("");
    }
}