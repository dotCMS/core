package com.dotcms.rest.api.v1.analytics.event;

import com.dotcms.IntegrationTestBase;
import com.dotcms.JUnit4WeldRunner;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.TestUserUtils;
import com.dotcms.experiments.business.ExperimentsAPI.Health;
import com.dotcms.mock.request.MockAttributeRequest;
import com.dotcms.mock.request.MockHeaderRequest;
import com.dotcms.mock.request.MockHttpRequestIntegrationTest;
import com.dotcms.mock.request.MockSessionRequest;
import com.dotcms.mock.response.MockHttpResponse;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.api.v1.analytics.content.util.ContentAnalyticsUtil;
import com.dotcms.security.apps.AppSecrets;
import com.dotcms.security.apps.AppsAPI;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.liferay.portal.model.User;
import com.liferay.util.Base64;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.enterprise.context.Dependent;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Integration tests for {@link EventAnalyticsProxyResource#analyticsHealth}.
 *
 * <p>Exercises the real {@link AppsAPI} round-trip so the tests prove that the
 * {@code /analytics/health} endpoint returns the correct {@link Health} state from actual
 * secrets store data, and enforces the auth and permission contracts.
 *
 * <p>Cases requiring a live CAEM backend ({@code health: "OK"} with backend reachable) are
 * not testable in the harness without an active CAEM instance — they are covered by unit
 * tests in {@code ContentAnalyticsUtilTest} and by manual validation (quickstart.md Scenario A–E).
 *
 * @author dotCMS
 * @since Oct 2026
 */
@Dependent
@RunWith(JUnit4WeldRunner.class)
public class EventAnalyticsProxyResourceIntegrationTest extends IntegrationTestBase {

    private static EventAnalyticsProxyResource resource;
    private static HttpServletResponse mockResponse;

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
        resource = new EventAnalyticsProxyResource(new WebResource());
        mockResponse = new MockHttpResponse();
    }

    // --- resolveAnalyticsHealth() via real AppsAPI ---

    /**
     * Method to test: {@link ContentAnalyticsUtil#resolveAnalyticsHealth(Host)}
     * Given scenario: No App secrets are configured for the site.
     * Expected result: {@link Health#NOT_CONFIGURED} — the endpoint returns 200 with
     *   {@code { "health": "NOT_CONFIGURED" }}, never 503.
     */
    @Test
    public void resolveAnalyticsHealth_appAbsent_returnsNotConfigured() throws Exception {
        final Host site = new SiteDataGen().nextPersisted(true);
        try {
            final Health health = ContentAnalyticsUtil.resolveAnalyticsHealth(site);
            assertEquals(
                    "Site with no App secrets must yield NOT_CONFIGURED",
                    Health.NOT_CONFIGURED, health);
        } finally {
            cleanupSite(site);
        }
    }

    /**
     * Method to test: {@link ContentAnalyticsUtil#resolveAnalyticsHealth(Host)}
     * Given scenario: App secrets exist but both credentials are blank.
     * Expected result: {@link Health#CONFIGURATION_ERROR} — credentials present but unusable.
     */
    @Test
    public void resolveAnalyticsHealth_blankCredentials_returnsConfigurationError()
            throws Exception {
        final Host site = new SiteDataGen().nextPersisted(true);
        final User systemUser = APILocator.getUserAPI().getSystemUser();
        try {
            final AppsAPI appsAPI = APILocator.getAppsAPI();
            final AppSecrets secrets = new AppSecrets.Builder()
                    .withKey(ContentAnalyticsUtil.CONTENT_ANALYTICS_APP_KEY)
                    .withHiddenSecret(ContentAnalyticsUtil.SITE_AUTH_KEY, "")
                    .withHiddenSecret(ContentAnalyticsUtil.BEARER_TOKEN_KEY, "")
                    .build();
            appsAPI.saveSecrets(secrets, site, systemUser);

            final Health health = ContentAnalyticsUtil.resolveAnalyticsHealth(site);
            assertEquals(
                    "Blank credentials must yield CONFIGURATION_ERROR",
                    Health.CONFIGURATION_ERROR, health);
        } finally {
            cleanupSite(site);
        }
    }

    // --- analyticsHealth() endpoint — auth and permission contracts ---

    /**
     * Method to test: {@link EventAnalyticsProxyResource#analyticsHealth}
     * Given scenario: Unauthenticated request (no user).
     * Expected result: A {@code SecurityException} is thrown, which JAX-RS maps to
     *   {@code 401 Unauthorized} — the endpoint requires a backend user.
     */
    @Test
    public void analyticsHealth_unauthenticated_throwsSecurityException() {
        final HttpServletRequest request = anonymousRequest();

        // Calling the resource directly bypasses JAX-RS exception mappers.
        // In production the SecurityException → 401; here we assert the exception itself.
        try {
            resource.analyticsHealth(request, mockResponse);
            org.junit.Assert.fail("Expected a SecurityException for unauthenticated access");
        } catch (final com.dotcms.rest.exception.SecurityException e) {
            // Expected — the JAX-RS mapper would convert this to 401 in production.
            assertTrue("Security exception message must be present",
                    e.getMessage() != null && !e.getMessage().isEmpty());
        }
    }

    /**
     * Method to test: {@link EventAnalyticsProxyResource#analyticsHealth}
     * Given scenario: Authenticated admin user with a valid site.
     * Expected result: {@code 200 OK} with a {@code health} field in the entity — the endpoint
     *   always returns 200, never 503.
     */
    @Test
    @SuppressWarnings("unchecked")
    public void analyticsHealth_authenticatedAdmin_returns200WithHealthField() throws Exception {
        final User adminUser = TestUserUtils.getAdminUser();
        final HttpServletRequest request = authenticatedRequest(adminUser);

        final Response result = resource.analyticsHealth(request, mockResponse);

        assertEquals("Authenticated request must return 200", 200, result.getStatus());
        assertNotNull("Response must have an entity", result.getEntity());
        final ResponseEntityView<Map<String, String>> body =
                (ResponseEntityView<Map<String, String>>) result.getEntity();
        assertNotNull("entity.health must be present", body.getEntity().get("health"));
    }

    // --- helpers ---

    private static HttpServletRequest anonymousRequest() {
        return new MockAttributeRequest(
                new MockSessionRequest(
                        new MockHttpRequestIntegrationTest("localhost", "/").request())
                        .request())
                .request();
    }

    private static HttpServletRequest authenticatedRequest(final User user) {
        final String credentials = Base64.encode(
                (user.getEmailAddress() + ":admin").getBytes());
        final MockHeaderRequest request = new MockHeaderRequest(
                new MockSessionRequest(
                        new MockAttributeRequest(
                                new MockHttpRequestIntegrationTest("localhost", "/").request())
                                .request())
                        .request());
        request.setHeader("Authorization", "Basic " + credentials);
        return request;
    }

    private static void cleanupSite(final Host site) {
        try {
            final User systemUser = APILocator.getUserAPI().getSystemUser();
            final HostAPI hostAPI = APILocator.getHostAPI();
            hostAPI.archive(site, systemUser, false);
            hostAPI.delete(site, systemUser, false);
        } catch (final Exception e) {
            com.dotmarketing.util.Logger.warn(
                    EventAnalyticsProxyResourceIntegrationTest.class,
                    "Failed to clean up test site: " + e.getMessage());
        }
    }
}