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
import com.dotcms.security.apps.AppDescriptor;
import com.dotcms.security.apps.AppSecrets;
import com.dotcms.security.apps.AppsAPI;
import com.dotcms.security.apps.ParamDescriptor;
import com.dotcms.security.apps.Type;
import com.dotcms.test.util.ApiTokenTestUtils;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.util.Config;
import com.liferay.portal.model.User;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.enterprise.context.Dependent;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Integration tests for {@link EventAnalyticsProxyResource#analyticsHealth} and for the
 * {@code allowedUrlParams} Content Analytics App field read by {@link UrlParamFilter}.
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

    @ClassRule
    public static final ApiTokenTestUtils TOKEN_UTILS = new ApiTokenTestUtils();

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

    // --- allowedUrlParams App field ---

    /**
     * Method to test: {@link AppsAPI#getAppDescriptor(String, User)}
     * Given scenario: the Content Analytics App descriptor is loaded from
     *   {@code dotContentAnalytics-config.yml} by a running instance.
     * Expected result: it exposes an optional {@code STRING} field named
     *   {@link ContentAnalyticsUtil#ALLOWED_URL_PARAMS_KEY} and an optional {@code BOOL} field named
     *   {@link ContentAnalyticsUtil#FILTER_URL_PARAMS_KEY}.
     */
    @Test
    public void appDescriptor_exposesOptionalUrlParamFields() throws Exception {
        final AppDescriptor descriptor = APILocator.getAppsAPI()
                .getAppDescriptor(ContentAnalyticsUtil.CONTENT_ANALYTICS_APP_KEY,
                        APILocator.getUserAPI().getSystemUser())
                .orElseThrow(() -> new AssertionError("Content Analytics App descriptor must load"));

        final ParamDescriptor param = descriptor.getParams().get(ContentAnalyticsUtil.ALLOWED_URL_PARAMS_KEY);
        assertNotNull("allowedUrlParams must be declared in the App descriptor", param);
        assertEquals(Type.STRING, param.getType());
        assertFalse("allowedUrlParams must be optional", param.isRequired());

        final ParamDescriptor filterParam = descriptor.getParams().get(ContentAnalyticsUtil.FILTER_URL_PARAMS_KEY);
        assertNotNull("filterUrlParams must be declared in the App descriptor", filterParam);
        assertEquals(Type.BOOL, filterParam.getType());
        assertFalse("filterUrlParams must be optional", filterParam.isRequired());
    }

    /**
     * Method to test: {@link UrlParamFilter#isEnabled} fed by
     *   {@link ContentAnalyticsUtil#getAppSecrets(Host)}
     * Given scenario: one site's Content Analytics App is saved through {@link AppsAPI} with
     *   {@code filterUrlParams=false}; another site is saved without that field.
     * Expected result: filtering is off for the first site only.
     */
    @Test
    public void isEnabled_siteFieldSavedThroughAppsApi_isPerSite() throws Exception {
        final Host site = new SiteDataGen().nextPersisted(true);
        final Host otherSite = new SiteDataGen().nextPersisted(true);
        final User systemUser = APILocator.getUserAPI().getSystemUser();
        try {
            APILocator.getAppsAPI().saveSecrets(new AppSecrets.Builder()
                    .withKey(ContentAnalyticsUtil.CONTENT_ANALYTICS_APP_KEY)
                    .withHiddenSecret(ContentAnalyticsUtil.SITE_AUTH_KEY, "site-auth-value")
                    .withSecret(ContentAnalyticsUtil.FILTER_URL_PARAMS_KEY, false)
                    .build(), site, systemUser);
            APILocator.getAppsAPI().saveSecrets(new AppSecrets.Builder()
                    .withKey(ContentAnalyticsUtil.CONTENT_ANALYTICS_APP_KEY)
                    .withHiddenSecret(ContentAnalyticsUtil.SITE_AUTH_KEY, "site-auth-value")
                    .build(), otherSite, systemUser);

            assertFalse("A site that unchecked the field must not be filtered",
                    UrlParamFilter.isEnabled(ContentAnalyticsUtil.getAppSecrets(site)));
            assertTrue("A site without the field must be filtered",
                    UrlParamFilter.isEnabled(ContentAnalyticsUtil.getAppSecrets(otherSite)));
        } finally {
            cleanupSite(site);
            cleanupSite(otherSite);
        }
    }

    /**
     * Method to test: {@link UrlParamFilter#resolveAllowedParams} fed by
     *   {@link ContentAnalyticsUtil#getAppSecrets(Host)}
     * Given scenario: a site's Content Analytics App is saved through {@link AppsAPI} with
     *   {@code allowedUrlParams=" Lang , page "}.
     * Expected result: the allowlist resolved from the stored secrets contains the defaults plus
     *   {@code lang} and {@code page}; another site without the field resolves to the defaults only.
     */
    @Test
    public void resolveAllowedParams_siteFieldSavedThroughAppsApi_isIncluded() throws Exception {
        final Host site = new SiteDataGen().nextPersisted(true);
        final Host otherSite = new SiteDataGen().nextPersisted(true);
        final User systemUser = APILocator.getUserAPI().getSystemUser();
        // Pin the instance-wide names to none, so the expected sets don't depend on the environment.
        final String instanceNames = Config.getStringProperty(UrlParamFilter.ALLOWED_PARAMS_PROPERTY, null);
        Config.setProperty(UrlParamFilter.ALLOWED_PARAMS_PROPERTY, "");
        try {
            final AppSecrets secrets = new AppSecrets.Builder()
                    .withKey(ContentAnalyticsUtil.CONTENT_ANALYTICS_APP_KEY)
                    .withHiddenSecret(ContentAnalyticsUtil.SITE_AUTH_KEY, "site-auth-value")
                    .withSecret(ContentAnalyticsUtil.ALLOWED_URL_PARAMS_KEY, " Lang , page ")
                    .build();
            APILocator.getAppsAPI().saveSecrets(secrets, site, systemUser);

            final Set<String> allowed =
                    UrlParamFilter.resolveAllowedParams(ContentAnalyticsUtil.getAppSecrets(site));
            assertTrue("Defaults must be kept", allowed.containsAll(UrlParamFilter.DEFAULT_ALLOWED_PARAMS));
            assertTrue("Site value must be trimmed and lower-cased", allowed.contains("lang"));
            assertTrue("Every listed name must be added", allowed.contains("page"));
            assertEquals(UrlParamFilter.DEFAULT_ALLOWED_PARAMS.size() + 2, allowed.size());

            assertEquals("A site without the field must resolve to the defaults only",
                    UrlParamFilter.DEFAULT_ALLOWED_PARAMS,
                    UrlParamFilter.resolveAllowedParams(ContentAnalyticsUtil.getAppSecrets(otherSite)));
        } finally {
            Config.setProperty(UrlParamFilter.ALLOWED_PARAMS_PROPERTY, instanceNames);
            cleanupSite(site);
            cleanupSite(otherSite);
        }
    }

    // --- helpers ---

    private static HttpServletRequest anonymousRequest() {
        return new MockAttributeRequest(
                new MockSessionRequest(
                        new MockHttpRequestIntegrationTest("localhost", "/").request())
                        .request())
                .request();
    }

    /** Builds a credential-only request carrying the given {@link User}'s API token. */
    private static HttpServletRequest authenticatedRequest(final User user) {
        final MockHeaderRequest request = new MockHeaderRequest(
                new MockSessionRequest(
                        new MockAttributeRequest(
                                new MockHttpRequestIntegrationTest("localhost", "/").request())
                                .request())
                        .request());
        request.setHeader("Authorization", TOKEN_UTILS.bearerHeader(user));
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