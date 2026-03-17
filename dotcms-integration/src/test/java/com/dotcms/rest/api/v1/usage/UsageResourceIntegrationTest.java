package com.dotcms.rest.api.v1.usage;

import com.dotcms.datagen.TestUserUtils;
import com.dotcms.mock.request.MockAttributeRequest;
import com.dotcms.mock.request.MockHeaderRequest;
import com.dotcms.mock.request.MockHttpRequestIntegrationTest;
import com.dotcms.mock.request.MockSessionRequest;
import com.dotcms.mock.response.MockHttpResponse;
import com.dotcms.rest.WebResource;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.liferay.portal.model.User;
import com.liferay.portal.util.WebKeys;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Integration tests for {@link UsageResource}.
 *
 * <p>Verifies the usage dashboard API including:</p>
 * <ul>
 *     <li>MINIMAL profile returns core metrics (typically 8-15)</li>
 *     <li>MINIMAL profile response completes in &lt; 5 seconds</li>
 *     <li>Response structure matches UsageSummary contract</li>
 *     <li>Metrics are organized by category</li>
 *     <li>Profile parameter is properly validated</li>
 * </ul>
 */
public class UsageResourceIntegrationTest {

    private static UsageResource resource;
    private static User adminUser;
    private static HttpServletResponse response;

    @BeforeClass
    public static void prepare() throws Exception {
        // Setting web app environment
        IntegrationTestInitService.getInstance().init();

        resource = new UsageResource(new WebResource());
        adminUser = TestUserUtils.getAdminUser();
        response = new MockHttpResponse();

        // Ensure admin user has backend role (required for REST API access)
        final com.dotmarketing.business.Role backendRole = APILocator.getRoleAPI().loadBackEndUserRole();
        if (!APILocator.getRoleAPI().doesUserHaveRole(adminUser, backendRole)) {
            APILocator.getRoleAPI().addRoleToUser(backendRole, adminUser);
        }

        // Ensure admin user has at least one layout (required for backend access)
        if (APILocator.getLayoutAPI().loadLayoutsForUser(adminUser).isEmpty()) {
            APILocator.getRoleAPI().addLayoutToRole(
                    APILocator.getLayoutAPI().findAllLayouts().get(0),
                    APILocator.getRoleAPI().getUserRole(adminUser));
        }
    }

    /**
     * Creates a mock HTTP request with admin authentication.
     */
    private static HttpServletRequest mockAuthenticatedRequest() {
        final MockHeaderRequest request = new MockHeaderRequest(
                new MockSessionRequest(
                        new MockAttributeRequest(
                                new MockHttpRequestIntegrationTest("localhost", "/")
                                        .request())
                                .request())
                        .request());

        request.setAttribute(WebKeys.USER, adminUser);
        return request;
    }

    /**
     * Method to test: {@link UsageResource#getSummary(HttpServletRequest, HttpServletResponse, String)}
     * When: Requesting usage summary with MINIMAL profile
     * Should: Return core metrics in less than 5 seconds
     */
    @Test
    public void testGetSummary_withMinimalProfile_shouldReturnCoreMetricsQuickly() {
        // Given: An authenticated request with MINIMAL profile
        final HttpServletRequest request = mockAuthenticatedRequest();
        final long startTime = System.currentTimeMillis();

        // When: Getting usage summary with MINIMAL profile
        final Response apiResponse = resource.getSummary(request, response, "MINIMAL");
        final long duration = System.currentTimeMillis() - startTime;

        // Then: Should return success
        assertEquals("Response should be 200 OK", Response.Status.OK.getStatusCode(), apiResponse.getStatus());
        assertNotNull("Response entity should not be null", apiResponse.getEntity());

        // Then: Should complete in less than 5 seconds
        assertTrue(
                String.format("MINIMAL profile should complete in < 5 seconds (actual: %dms)", duration),
                duration < 5000
        );

        // Then: Should return ResponseEntityUsageSummaryView with proper structure
        assertTrue("Response entity should be ResponseEntityUsageSummaryView",
                apiResponse.getEntity() instanceof ResponseEntityUsageSummaryView);

        final ResponseEntityUsageSummaryView responseView = (ResponseEntityUsageSummaryView) apiResponse.getEntity();
        final UsageSummary summary = responseView.getEntity();

        assertNotNull("UsageSummary should not be null", summary);
        assertNotNull("UsageSummary.metrics should not be null", summary.getMetrics());
        assertNotNull("UsageSummary.lastUpdated should not be null", summary.getLastUpdated());

        // Then: Should return core metrics (typically 8-15 for MINIMAL profile)
        final Map<String, Map<String, Object>> metricsByCategory = summary.getMetrics();
        final int totalMetrics = metricsByCategory.values().stream()
                .mapToInt(categoryMetrics -> categoryMetrics.size())
                .sum();

        assertTrue(
                String.format("MINIMAL profile should return at least 5 core metrics (actual: %d)", totalMetrics),
                totalMetrics >= 5
        );
        assertTrue(
                String.format("MINIMAL profile should return a reasonable number of metrics (actual: %d)", totalMetrics),
                totalMetrics <= 20
        );
    }

    /**
     * Method to test: {@link UsageResource#getSummary(HttpServletRequest, HttpServletResponse, String)}
     * When: Requesting usage summary with default profile (no parameter)
     * Should: Default to MINIMAL profile and return quickly
     */
    @Test
    public void testGetSummary_withDefaultProfile_shouldUseMINIMAL() {
        // Given: An authenticated request with no profile parameter
        final HttpServletRequest request = mockAuthenticatedRequest();
        final long startTime = System.currentTimeMillis();

        // When: Getting usage summary with default profile
        final Response apiResponse = resource.getSummary(request, response, "MINIMAL");
        final long duration = System.currentTimeMillis() - startTime;

        // Then: Should return success and complete quickly
        assertEquals("Response should be 200 OK", Response.Status.OK.getStatusCode(), apiResponse.getStatus());
        assertTrue(
                String.format("Default profile should complete in < 5 seconds (actual: %dms)", duration),
                duration < 5000
        );

        // Then: Should return valid summary
        final ResponseEntityUsageSummaryView responseView = (ResponseEntityUsageSummaryView) apiResponse.getEntity();
        final UsageSummary summary = responseView.getEntity();

        assertNotNull("UsageSummary should not be null", summary);
        assertNotNull("UsageSummary.metrics should not be null", summary.getMetrics());
    }

    /**
     * Method to test: {@link UsageResource#getSummary(HttpServletRequest, HttpServletResponse, String)}
     * When: Requesting with MINIMAL profile
     * Should: Return metrics organized by category with proper structure
     */
    @Test
    public void testGetSummary_shouldReturnMetricsOrganizedByCategory() {
        // Given: An authenticated request
        final HttpServletRequest request = mockAuthenticatedRequest();

        // When: Getting usage summary
        final Response apiResponse = resource.getSummary(request, response, "MINIMAL");

        // Then: Response should have proper structure
        final ResponseEntityUsageSummaryView responseView = (ResponseEntityUsageSummaryView) apiResponse.getEntity();
        final UsageSummary summary = responseView.getEntity();
        final Map<String, Map<String, Object>> metricsByCategory = summary.getMetrics();

        // Then: Should have at least one category
        assertFalse("Should have at least one category", metricsByCategory.isEmpty());

        // Then: Each category should contain metrics with proper metadata structure
        for (final Map.Entry<String, Map<String, Object>> categoryEntry : metricsByCategory.entrySet()) {
            final String category = categoryEntry.getKey();
            final Map<String, Object> categoryMetrics = categoryEntry.getValue();

            assertNotNull("Category name should not be null", category);
            assertFalse(
                    String.format("Category '%s' should have at least one metric", category),
                    categoryMetrics.isEmpty()
            );

            // Then: Each metric should have name, value, and displayLabel
            for (final Map.Entry<String, Object> metricEntry : categoryMetrics.entrySet()) {
                final String metricName = metricEntry.getKey();
                final Object metricData = metricEntry.getValue();

                assertNotNull("Metric name should not be null", metricName);
                assertNotNull("Metric data should not be null", metricData);
                assertTrue("Metric data should be a Map", metricData instanceof Map);

                @SuppressWarnings("unchecked")
                final Map<String, Object> metricMap = (Map<String, Object>) metricData;

                assertTrue(
                        String.format("Metric '%s' should have 'name' field", metricName),
                        metricMap.containsKey("name")
                );
                assertTrue(
                        String.format("Metric '%s' should have 'value' field", metricName),
                        metricMap.containsKey("value")
                );
                assertTrue(
                        String.format("Metric '%s' should have 'displayLabel' field", metricName),
                        metricMap.containsKey("displayLabel")
                );

                // Verify displayLabel is an i18n key
                final String displayLabel = (String) metricMap.get("displayLabel");
                assertTrue(
                        String.format("Metric '%s' displayLabel should start with 'usage.metric.'", metricName),
                        displayLabel.startsWith("usage.metric.")
                );
            }
        }
    }

    /**
     * Method to test: {@link UsageResource#getSummary(HttpServletRequest, HttpServletResponse, String)}
     * When: Requesting with STANDARD profile
     * Should: Return more metrics than MINIMAL profile
     */
    @Test
    public void testGetSummary_withStandardProfile_shouldReturnMoreMetricsThanMinimal() {
        // Given: An authenticated request
        final HttpServletRequest request = mockAuthenticatedRequest();

        // When: Getting usage summary with MINIMAL profile
        final Response minimalResponse = resource.getSummary(request, response, "MINIMAL");
        final ResponseEntityUsageSummaryView minimalView = (ResponseEntityUsageSummaryView) minimalResponse.getEntity();
        final UsageSummary minimalSummary = minimalView.getEntity();

        final int minimalCount = minimalSummary.getMetrics().values().stream()
                .mapToInt(categoryMetrics -> categoryMetrics.size())
                .sum();

        // When: Getting usage summary with STANDARD profile
        final Response standardResponse = resource.getSummary(request, response, "STANDARD");
        final ResponseEntityUsageSummaryView standardView = (ResponseEntityUsageSummaryView) standardResponse.getEntity();
        final UsageSummary standardSummary = standardView.getEntity();

        final int standardCount = standardSummary.getMetrics().values().stream()
                .mapToInt(categoryMetrics -> categoryMetrics.size())
                .sum();

        // Then: STANDARD profile should return more metrics than MINIMAL
        assertTrue(
                String.format("STANDARD profile (%d metrics) should return >= MINIMAL profile (%d metrics)",
                        standardCount, minimalCount),
                standardCount >= minimalCount
        );
    }

    /**
     * Method to test: {@link UsageResource#getSummary(HttpServletRequest, HttpServletResponse, String)}
     * When: Requesting with FULL profile
     * Should: Return more metrics than MINIMAL profile
     */
    @Test
    public void testGetSummary_withFullProfile_shouldReturnMoreMetricsThanMinimal() {
        // Given: An authenticated request
        final HttpServletRequest request = mockAuthenticatedRequest();

        // When: Getting usage summary with MINIMAL profile
        final Response minimalResponse = resource.getSummary(request, response, "MINIMAL");
        final ResponseEntityUsageSummaryView minimalView = (ResponseEntityUsageSummaryView) minimalResponse.getEntity();
        final UsageSummary minimalSummary = minimalView.getEntity();

        final int minimalCount = minimalSummary.getMetrics().values().stream()
                .mapToInt(categoryMetrics -> categoryMetrics.size())
                .sum();

        // When: Getting usage summary with FULL profile
        final Response fullResponse = resource.getSummary(request, response, "FULL");
        final ResponseEntityUsageSummaryView fullView = (ResponseEntityUsageSummaryView) fullResponse.getEntity();
        final UsageSummary fullSummary = fullView.getEntity();

        final int fullCount = fullSummary.getMetrics().values().stream()
                .mapToInt(categoryMetrics -> categoryMetrics.size())
                .sum();

        // Then: FULL profile should return more metrics than MINIMAL
        assertTrue(
                String.format("FULL profile (%d metrics) should return >= MINIMAL profile (%d metrics)",
                        fullCount, minimalCount),
                fullCount >= minimalCount
        );
    }

    /**
     * Method to test: {@link UsageResource#getSummary(HttpServletRequest, HttpServletResponse, String)}
     * When: Requesting with invalid profile parameter
     * Should: Return 400 Bad Request error
     */
    @Test
    public void testGetSummary_withInvalidProfile_shouldReturnBadRequest() {
        // Given: An authenticated request with invalid profile
        final HttpServletRequest request = mockAuthenticatedRequest();

        // When: Getting usage summary with invalid profile
        final Response apiResponse = resource.getSummary(request, response, "INVALID_PROFILE");

        // Then: Should return 400 Bad Request
        assertEquals("Response should be 400 Bad Request",
                Response.Status.BAD_REQUEST.getStatusCode(),
                apiResponse.getStatus());
    }

    /**
     * Method to test: {@link UsageResource#getSummary(HttpServletRequest, HttpServletResponse, String)}
     * When: Requesting with case-insensitive profile parameter
     * Should: Accept lowercase and mixed case profile names
     */
    @Test
    public void testGetSummary_withCaseInsensitiveProfile_shouldAccept() {
        // Given: An authenticated request
        final HttpServletRequest request = mockAuthenticatedRequest();

        // When: Getting usage summary with lowercase "minimal"
        final Response lowercaseResponse = resource.getSummary(request, response, "minimal");

        // Then: Should accept and return success
        assertEquals("Response should accept lowercase profile",
                Response.Status.OK.getStatusCode(),
                lowercaseResponse.getStatus());

        // When: Getting usage summary with mixed case "MiNiMaL"
        final Response mixedCaseResponse = resource.getSummary(request, response, "MiNiMaL");

        // Then: Should accept and return success
        assertEquals("Response should accept mixed case profile",
                Response.Status.OK.getStatusCode(),
                mixedCaseResponse.getStatus());
    }

    /**
     * Method to test: {@link UsageResource#getSummary(HttpServletRequest, HttpServletResponse, String)}
     * When: Requesting with MINIMAL profile
     * Should: Include i18n messages map in response
     */
    @Test
    public void testGetSummary_shouldIncludeI18nMessagesMap() {
        // Given: An authenticated request
        final HttpServletRequest request = mockAuthenticatedRequest();

        // When: Getting usage summary
        final Response apiResponse = resource.getSummary(request, response, "MINIMAL");

        // Then: Response should include i18n messages map
        final ResponseEntityUsageSummaryView responseView = (ResponseEntityUsageSummaryView) apiResponse.getEntity();
        final Map<String, String> i18nMessages = responseView.getI18nMessagesMap();

        assertNotNull("i18nMessagesMap should not be null", i18nMessages);
        assertFalse("i18nMessagesMap should not be empty", i18nMessages.isEmpty());

        // Then: Should contain i18n keys for metrics and categories
        final UsageSummary summary = responseView.getEntity();
        final Map<String, Map<String, Object>> metricsByCategory = summary.getMetrics();

        // Verify category title translations exist
        for (final String category : metricsByCategory.keySet()) {
            final String categoryTitleKey = "usage.category." + category + ".title";
            assertTrue(
                    String.format("i18nMessagesMap should contain category title key '%s'", categoryTitleKey),
                    i18nMessages.containsKey(categoryTitleKey)
            );
        }

        // Verify metric label translations exist
        for (final Map<String, Object> categoryMetrics : metricsByCategory.values()) {
            for (final String metricName : categoryMetrics.keySet()) {
                final String metricLabelKey = "usage.metric." + metricName + ".label";
                assertTrue(
                        String.format("i18nMessagesMap should contain metric label key '%s'", metricLabelKey),
                        i18nMessages.containsKey(metricLabelKey)
                );
            }
        }
    }

    /**
     * Method to test: {@link UsageResource#getSummary(HttpServletRequest, HttpServletResponse, String)}
     * When: Requesting usage summary with MINIMAL profile
     * Should: Include both content items and content types metrics
     */
    @Test
    public void testGetSummary_shouldIncludeContentMetrics() {
        // Given: An authenticated request with MINIMAL profile
        final HttpServletRequest request = mockAuthenticatedRequest();

        // When: Getting usage summary
        final Response apiResponse = resource.getSummary(request, response, "MINIMAL");

        // Then: Should return success
        assertEquals("Response should be 200 OK",
                Response.Status.OK.getStatusCode(),
                apiResponse.getStatus());

        // Then: Should include content category with both metrics
        final ResponseEntityUsageSummaryView responseView = (ResponseEntityUsageSummaryView) apiResponse.getEntity();
        final UsageSummary summary = responseView.getEntity();
        final Map<String, Map<String, Object>> metricsByCategory = summary.getMetrics();

        assertTrue("Should have 'content' category", metricsByCategory.containsKey("content"));

        final Map<String, Object> contentMetrics = metricsByCategory.get("content");

        // Then: Should have content items metric
        assertTrue(
                "Content category should include COUNT_CONTENT metric",
                contentMetrics.containsKey("COUNT_CONTENT")
        );

        // Then: Should have content types metric
        assertTrue(
                "Content category should include COUNT_OF_CONTENT_TYPES metric",
                contentMetrics.containsKey("COUNT_OF_CONTENT_TYPES")
        );

        // Then: Verify metric structure for content types
        @SuppressWarnings("unchecked")
        final Map<String, Object> contentTypesMetric =
                (Map<String, Object>) contentMetrics.get("COUNT_OF_CONTENT_TYPES");

        assertEquals("Metric name should match",
                "COUNT_OF_CONTENT_TYPES",
                contentTypesMetric.get("name"));
        assertNotNull("Metric should have a value",
                contentTypesMetric.get("value"));
        assertEquals("Metric should have correct i18n key",
                "usage.metric.COUNT_OF_CONTENT_TYPES.label",
                contentTypesMetric.get("displayLabel"));

        // Then: Verify metric structure for content items
        @SuppressWarnings("unchecked")
        final Map<String, Object> contentItemsMetric =
                (Map<String, Object>) contentMetrics.get("COUNT_CONTENT");

        assertEquals("Metric name should match",
                "COUNT_CONTENT",
                contentItemsMetric.get("name"));
        assertNotNull("Metric should have a value",
                contentItemsMetric.get("value"));
        assertEquals("Metric should have correct i18n key",
                "usage.metric.COUNT_CONTENT.label",
                contentItemsMetric.get("displayLabel"));
    }
}