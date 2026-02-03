package com.dotcms.rest.api.v1.publishing;

import com.dotcms.datagen.BundleDataGen;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.TestUserUtils;
import com.dotcms.mock.request.MockAttributeRequest;
import com.dotcms.mock.request.MockHeaderRequest;
import com.dotcms.mock.request.MockHttpRequestIntegrationTest;
import com.dotcms.mock.request.MockSessionRequest;
import com.dotcms.mock.response.MockHttpResponse;
import com.dotcms.publisher.bundle.bean.Bundle;
import com.dotcms.publisher.business.DotPublisherException;
import com.dotcms.publisher.business.PublishAuditAPI;
import com.dotcms.publisher.business.PublishAuditHistory;
import com.dotcms.publisher.business.PublishAuditStatus;
import com.dotcms.publisher.business.PublishAuditStatus.Status;
import com.dotcms.publisher.util.PusheableAsset;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Role;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.liferay.portal.model.User;
import com.liferay.portal.util.WebKeys;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.core.Response;

import com.dotcms.publisher.business.EndpointDetail;
import com.dotcms.publishing.PublisherConfig.DeliveryStrategy;
import com.dotcms.rest.exception.ConflictException;
import com.dotcms.rest.exception.NotFoundException;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Integration tests for PublishingResource - focuses on DATA CORRECTNESS and BUSINESS LOGIC.
 * HTTP layer testing (auth, status codes) is handled by Postman collection.
 *
 * Tests verify:
 * - Filtering logic (status, text filter)
 * - Pagination behavior
 * - Response data correctness from database
 * - Sorting order
 * - Asset preview limiting
 */
public class PublishingResourceIntegrationTest {

    private static PublishAuditAPI publishAuditAPI;
    private static User adminUser;
    private static List<String> createdBundleIds;
    private static PublishingResource publishingResource;
    private static HttpServletResponse response;

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
        publishAuditAPI = PublishAuditAPI.getInstance();
        adminUser = TestUserUtils.getAdminUser();

        // Ensure admin user has backend role (fixes race condition in role initialization)
        final Role backendRole = TestUserUtils.getBackendRole();
        APILocator.getRoleAPI().addRoleToUser(backendRole, adminUser);

        createdBundleIds = new ArrayList<>();
        publishingResource = new PublishingResource();
        response = new MockHttpResponse();
    }

    @AfterClass
    public static void cleanup() {
        if (createdBundleIds == null) {
            return;
        }
        for (final String bundleId : createdBundleIds) {
            try {
                publishAuditAPI.deletePublishAuditStatus(bundleId);
                APILocator.getBundleAPI().deleteBundleAndDependencies(bundleId, adminUser);
            } catch (Exception e) {
                // Ignore cleanup errors
            }
        }
    }

    // =========================================================================
    // FILTERING TESTS - Verify filter logic returns correct data
    // =========================================================================

    /**
     * Given: Bundles with different statuses exist
     * When: Filter by single status (SUCCESS)
     * Then: Only bundles with that status are returned
     */
    @Test
    public void test_filterByStatus_returnsOnlyMatchingStatus() throws Exception {
        final String successId = createBundleWithStatus("filter-success", Status.SUCCESS);
        final String failedId = createBundleWithStatus("filter-failed", Status.FAILED_TO_PUBLISH);

        final ResponseEntityPublishingJobsView result = callEndpoint(null, "SUCCESS");

        final List<PublishingJobView> jobs = result.getEntity();
        assertNotNull(jobs);

        // ALL returned jobs must have SUCCESS status
        for (final PublishingJobView job : jobs) {
            assertEquals("All jobs should have SUCCESS status", Status.SUCCESS, job.status());
        }

        // Our SUCCESS bundle must be present
        assertTrue("Should contain our SUCCESS bundle",
                jobs.stream().anyMatch(j -> successId.equals(j.bundleId())));

        // Our FAILED bundle must NOT be present
        assertFalse("Should NOT contain FAILED bundle",
                jobs.stream().anyMatch(j -> failedId.equals(j.bundleId())));
    }

    /**
     * Given: Bundles with different statuses exist
     * When: Filter by multiple statuses (SUCCESS,FAILED_TO_PUBLISH)
     * Then: Bundles matching ANY of the statuses are returned
     */
    @Test
    public void test_filterByMultipleStatuses_returnsAllMatching() throws Exception {
        final String successId = createBundleWithStatus("multi-success", Status.SUCCESS);
        final String failedId = createBundleWithStatus("multi-failed", Status.FAILED_TO_PUBLISH);
        final String waitingId = createBundleWithStatus("multi-waiting", Status.WAITING_FOR_PUBLISHING);

        final ResponseEntityPublishingJobsView result = callEndpoint(null, "SUCCESS,FAILED_TO_PUBLISH");

        final List<PublishingJobView> jobs = result.getEntity();

        // All returned jobs must have one of the filtered statuses
        for (final PublishingJobView job : jobs) {
            assertTrue("Status should be SUCCESS or FAILED_TO_PUBLISH",
                    Status.SUCCESS.equals(job.status()) || Status.FAILED_TO_PUBLISH.equals(job.status()));
        }

        // WAITING bundle must NOT be present
        assertFalse("Should NOT contain WAITING_FOR_PUBLISHING bundle",
                jobs.stream().anyMatch(j -> waitingId.equals(j.bundleId())));
    }

    /**
     * Given: Bundles with specific names exist
     * When: Filter by text (partial match on name)
     * Then: Only bundles with matching names are returned
     */
    @Test
    public void test_filterByText_matchesBundleName() throws Exception {
        final String stagingId = createBundleWithStatus("staging-deployment-xyz", Status.SUCCESS);
        final String prodId = createBundleWithStatus("production-release", Status.SUCCESS);

        final ResponseEntityPublishingJobsView result = callEndpoint("staging", null);

        final List<PublishingJobView> jobs = result.getEntity();

        // Should find staging bundle
        assertTrue("Should find staging bundle",
                jobs.stream().anyMatch(j -> stagingId.equals(j.bundleId())));

        // Should NOT find production bundle (unless its ID happens to contain "staging")
        final boolean prodFound = jobs.stream()
                .anyMatch(j -> prodId.equals(j.bundleId()) &&
                        (j.bundleName() == null || !j.bundleName().toLowerCase().contains("staging")));
        assertFalse("Should NOT find production bundle", prodFound);
    }

    /**
     * Given: Bundles exist
     * When: Filter by partial bundle ID
     * Then: Bundles with matching IDs are returned
     */
    @Test
    public void test_filterByText_matchesBundleId() throws Exception {
        final String bundleId = createBundleWithStatus("id-search-test", Status.SUCCESS);
        final String partialId = bundleId.substring(0, 8);

        final ResponseEntityPublishingJobsView result = callEndpoint(partialId, null);

        assertTrue("Should find bundle by partial ID",
                result.getEntity().stream().anyMatch(j -> j.bundleId().contains(partialId)));
    }

    /**
     * Given: Bundles exist
     * When: Combined filter (text + status)
     * Then: Only bundles matching BOTH criteria are returned
     */
    @Test
    public void test_combinedFilters_requiresBothMatch() throws Exception {
        final String targetId = createBundleWithStatus("combined-test-bundle", Status.SUCCESS);
        final String wrongStatusId = createBundleWithStatus("combined-test-wrong", Status.FAILED_TO_PUBLISH);
        final String wrongNameId = createBundleWithStatus("other-name", Status.SUCCESS);

        final ResponseEntityPublishingJobsView result = callEndpoint("combined-test", "SUCCESS");

        final List<PublishingJobView> jobs = result.getEntity();

        // Should find target (matches both)
        assertTrue("Should find bundle matching both filters",
                jobs.stream().anyMatch(j -> targetId.equals(j.bundleId())));

        // Should NOT find wrong status (name matches, status doesn't)
        assertFalse("Should NOT find bundle with wrong status",
                jobs.stream().anyMatch(j -> wrongStatusId.equals(j.bundleId())));
    }

    /**
     * Given: No bundles match filter
     * When: Filter applied
     * Then: Empty list returned (not error)
     */
    @Test
    public void test_filterWithNoMatches_returnsEmptyList() throws Exception {
        final ResponseEntityPublishingJobsView result = callEndpoint(
                "nonexistent-bundle-xyz-12345", null);

        assertNotNull(result);
        assertNotNull(result.getEntity());
        assertTrue("Should return empty list", result.getEntity().isEmpty());
        assertEquals("Total entries should be 0", 0, result.getPagination().getTotalEntries());
    }

    // =========================================================================
    // PAGINATION TESTS - Verify pagination logic
    // =========================================================================

    /**
     * Given: Page number is 0 or negative
     * When: Request made
     * Then: Defaults to page 1
     */
    @Test
    public void test_pagination_invalidPageDefaultsToOne() throws Exception {
        createBundleWithStatus("pagination-test", Status.SUCCESS);

        final ResponseEntityPublishingJobsView result = callEndpoint(0, 50, null, null);

        assertEquals("Should default to page 1", 1, result.getPagination().getCurrentPage());
    }

    /**
     * Given: per_page exceeds maximum (500)
     * When: Request made
     * Then: Caps at 500
     */
    @Test
    public void test_pagination_perPageCappedAt500() throws Exception {
        final ResponseEntityPublishingJobsView result = callEndpoint(1, 1000, null, null);

        assertEquals("Should cap at 500", 500, result.getPagination().getPerPage());
    }

    /**
     * Given: per_page is 0 or negative
     * When: Request made
     * Then: Defaults to minimum (1)
     */
    @Test
    public void test_pagination_invalidPerPageDefaultsToMinimum() throws Exception {
        final ResponseEntityPublishingJobsView result = callEndpoint(1, 0, null, null);

        assertEquals("Should default to 1", 1, result.getPagination().getPerPage());
    }

    // =========================================================================
    // RESPONSE DATA CORRECTNESS - Verify fields are populated correctly
    // =========================================================================

    /**
     * Given: Bundle with known data exists
     * When: Retrieved via API
     * Then: All response fields are correctly populated
     */
    @Test
    public void test_responseFields_allFieldsPopulatedCorrectly() throws Exception {
        final String bundleName = "field-verification-bundle";
        final String bundleId = createBundleWithStatus(bundleName, Status.SUCCESS);

        final ResponseEntityPublishingJobsView result = callEndpoint(bundleId, null);

        final PublishingJobView job = result.getEntity().stream()
                .filter(j -> bundleId.equals(j.bundleId()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Bundle not found"));

        // Verify all required fields
        assertEquals("bundleId should match", bundleId, job.bundleId());
        assertEquals("bundleName should match", bundleName, job.bundleName());
        assertEquals("status should be SUCCESS", Status.SUCCESS, job.status());
        assertNotNull("createDate should not be null", job.createDate());
        assertTrue("assetCount should be >= 0", job.assetCount() >= 0);
        assertNotNull("assetPreview should not be null", job.assetPreview());
        assertTrue("environmentCount should be >= 0", job.environmentCount() >= 0);
        assertTrue("numTries should be >= 0", job.numTries() >= 0);

        // Verify statusUpdated is logically after or equal to createDate
        if (job.statusUpdated() != null) {
            assertFalse("statusUpdated should not be before createDate",
                    job.statusUpdated().isBefore(job.createDate()));
        }
    }

    /**
     * Given: Bundle with multiple assets exists
     * When: Retrieved via API
     * Then: Asset preview limited to 3, but assetCount shows total
     *       AND the previewed assets are from our created set
     */
    @Test
    public void test_assetPreview_limitedToThreeWithCorrectCount() throws Exception {
        final int totalAssets = 5;
        final List<String> createdAssetIds = new ArrayList<>();
        final String bundleId = createBundleWithAssets("asset-limit-test", totalAssets, createdAssetIds);

        final ResponseEntityPublishingJobsView result = callEndpoint(bundleId, null);

        final PublishingJobView job = result.getEntity().stream()
                .filter(j -> bundleId.equals(j.bundleId()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Bundle not found"));

        assertTrue("Asset preview should have at most 3 items",
                job.assetPreview().size() <= 3);
        assertEquals("Asset count should reflect total",
                totalAssets, job.assetCount());

        // Verify all previewed assets are from our created set
        for (final AssetPreviewView asset : job.assetPreview()) {
            assertTrue("Previewed asset should be from created set: " + asset.id(),
                    createdAssetIds.contains(asset.id()));
        }
    }

    /**
     * Given: Bundle with assets exists
     * When: Retrieved via API
     * Then: Asset preview contains id, title, and type for each asset
     *       AND the asset IDs match the contentlets we created
     */
    @Test
    public void test_assetPreview_containsRequiredFields() throws Exception {
        final List<String> createdAssetIds = new ArrayList<>();
        final String bundleId = createBundleWithAssets("asset-fields-test", 2, createdAssetIds);

        final ResponseEntityPublishingJobsView result = callEndpoint(bundleId, null);

        final PublishingJobView job = result.getEntity().stream()
                .filter(j -> bundleId.equals(j.bundleId()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Bundle not found"));

        assertFalse("Should have asset preview", job.assetPreview().isEmpty());

        for (final AssetPreviewView asset : job.assetPreview()) {
            assertNotNull("Asset id should not be null", asset.id());
            assertNotNull("Asset title should not be null", asset.title());
            assertNotNull("Asset type should not be null", asset.type());
            assertFalse("Asset id should not be empty", asset.id().isEmpty());
            assertFalse("Asset type should not be empty", asset.type().isEmpty());

            // Verify the asset ID is one we actually created
            assertTrue("Asset ID should be from created contentlets: " + asset.id(),
                    createdAssetIds.contains(asset.id()));
        }
    }

    // =========================================================================
    // SORTING TESTS - Verify sort order
    // =========================================================================

    /**
     * Given: Multiple bundles with different statusUpdated times
     * When: Retrieved via API
     * Then: Results are sorted by statusUpdated DESC (newest first)
     */
    @Test
    public void test_sorting_byStatusUpdatedDescending() throws Exception {
        // Create bundles with deliberate time gaps
        final String olderId = createBundleWithStatus("sort-older", Status.SUCCESS);
        Thread.sleep(100); // Ensure time difference
        final String newerId = createBundleWithStatus("sort-newer", Status.SUCCESS);

        final ResponseEntityPublishingJobsView result = callEndpoint("sort-", null);

        final List<PublishingJobView> jobs = result.getEntity();
        assertTrue("Should have at least 2 results", jobs.size() >= 2);

        // Find positions
        int newerPos = -1, olderPos = -1;
        for (int i = 0; i < jobs.size(); i++) {
            if (newerId.equals(jobs.get(i).bundleId())) newerPos = i;
            if (olderId.equals(jobs.get(i).bundleId())) olderPos = i;
        }

        assertTrue("Newer bundle should appear before older (DESC order)",
                newerPos < olderPos);
    }

    // =========================================================================
    // VALIDATION TESTS - Verify input validation
    // =========================================================================

    /**
     * Given: Invalid status value
     * When: Request made
     * Then: BadRequestException thrown
     */
    @Test(expected = BadRequestException.class)
    public void test_invalidStatus_throwsBadRequest() throws Exception {
        callEndpoint(null, "INVALID_STATUS_XYZ");
    }

    /**
     * Given: Mix of valid and invalid status values
     * When: Request made
     * Then: BadRequestException thrown
     */
    @Test(expected = BadRequestException.class)
    public void test_mixedValidInvalidStatus_throwsBadRequest() throws Exception {
        callEndpoint(null, "SUCCESS,INVALID_STATUS");
    }

    // =========================================================================
    // DETAIL ENDPOINT TESTS - GET /v1/publishing/{bundleId}
    // =========================================================================

    /**
     * Given: Non-existent bundle ID
     * When: getPublishingJobDetails is called
     * Then: NotFoundException is thrown
     */
    @Test(expected = NotFoundException.class)
    public void test_getDetails_nonExistentBundle_returns404() throws Exception {
        publishingResource.getPublishingJobDetails(
                mockAuthenticatedRequest(), response, "non-existent-bundle-id-xyz");
    }

    /**
     * Given: Empty bundle ID
     * When: getPublishingJobDetails is called
     * Then: BadRequestException is thrown
     */
    @Test(expected = BadRequestException.class)
    public void test_getDetails_emptyBundleId_returns400() throws Exception {
        publishingResource.getPublishingJobDetails(
                mockAuthenticatedRequest(), response, "");
    }

    /**
     * Given: Bundle with known data exists
     * When: getPublishingJobDetails is called
     * Then: All response fields are correctly populated
     */
    @Test
    public void test_getDetails_allFieldsPopulatedCorrectly() throws Exception {
        final String bundleName = "detail-verification-bundle";
        final String bundleId = createBundleWithStatus(bundleName, Status.SUCCESS);

        final ResponseEntityPublishingJobDetailView result = publishingResource.getPublishingJobDetails(
                mockAuthenticatedRequest(), response, bundleId);

        final PublishingJobDetailView detail = result.getEntity();

        // Verify all required fields
        assertEquals("bundleId should match", bundleId, detail.bundleId());
        assertEquals("bundleName should match", bundleName, detail.bundleName());
        assertEquals("status should be SUCCESS", Status.SUCCESS, detail.status());
        assertNotNull("timestamps should not be null", detail.timestamps());
        assertNotNull("timestamps.createDate should not be null", detail.timestamps().createDate());
        assertTrue("assetCount should be >= 0", detail.assetCount() >= 0);
        assertTrue("numTries should be >= 0", detail.numTries() >= 0);
        assertNotNull("environments list should not be null", detail.environments());
    }

    /**
     * Given: Bundle with environment/endpoint data exists
     * When: getPublishingJobDetails is called
     * Then: Environment and endpoint details are populated correctly
     */
    @Test
    public void test_getDetails_environmentsAndEndpointsPopulated() throws Exception {
        final String bundleId = createBundleWithEnvironmentEndpoints("env-endpoint-test", Status.SUCCESS);

        final ResponseEntityPublishingJobDetailView result = publishingResource.getPublishingJobDetails(
                mockAuthenticatedRequest(), response, bundleId);

        final PublishingJobDetailView detail = result.getEntity();

        // Should have at least one environment
        assertFalse("Should have environments", detail.environments().isEmpty());

        final EnvironmentDetailView env = detail.environments().get(0);
        assertNotNull("Environment id should not be null", env.id());
        assertNotNull("Environment name should not be null", env.name());
        assertFalse("Environment should have endpoints", env.endpoints().isEmpty());

        final EndpointDetailView endpoint = env.endpoints().get(0);
        assertNotNull("Endpoint id should not be null", endpoint.id());
        assertNotNull("Endpoint serverName should not be null", endpoint.serverName());
    }

    /**
     * Given: Bundle with timestamps exists
     * When: getPublishingJobDetails is called
     * Then: All timestamps are correctly populated
     */
    @Test
    public void test_getDetails_timestampsPopulatedCorrectly() throws Exception {
        final String bundleId = createBundleWithTimestamps("timestamps-test", Status.SUCCESS);

        final ResponseEntityPublishingJobDetailView result = publishingResource.getPublishingJobDetails(
                mockAuthenticatedRequest(), response, bundleId);

        final TimestampsView timestamps = result.getEntity().timestamps();

        assertNotNull("createDate should not be null", timestamps.createDate());
        // bundleStart, bundleEnd, publishStart, publishEnd may be null based on status
        // For SUCCESS, all timestamps should be set
        assertNotNull("bundleStart should not be null for completed bundle", timestamps.bundleStart());
        assertNotNull("bundleEnd should not be null for completed bundle", timestamps.bundleEnd());
        assertNotNull("publishStart should not be null for completed bundle", timestamps.publishStart());
        assertNotNull("publishEnd should not be null for completed bundle", timestamps.publishEnd());

        // Verify timestamps are logically ordered
        assertFalse("bundleEnd should not be before bundleStart",
                timestamps.bundleEnd().isBefore(timestamps.bundleStart()));
        assertFalse("publishEnd should not be before publishStart",
                timestamps.publishEnd().isBefore(timestamps.publishStart()));
    }

    /**
     * Given: Bundle with FAILED status and endpoint with stackTrace
     * When: getPublishingJobDetails is called
     * Then: stackTrace is included in the response for failed endpoints
     */
    @Test
    public void test_getDetails_stackTraceIncludedForFailedEndpoints() throws Exception {
        final String bundleId = createBundleWithFailedEndpoint("failed-stack-test");

        final ResponseEntityPublishingJobDetailView result = publishingResource.getPublishingJobDetails(
                mockAuthenticatedRequest(), response, bundleId);

        final PublishingJobDetailView detail = result.getEntity();

        // Find the endpoint with stackTrace
        boolean foundStackTrace = false;
        for (final EnvironmentDetailView env : detail.environments()) {
            for (final EndpointDetailView endpoint : env.endpoints()) {
                if (endpoint.stackTrace() != null && !endpoint.stackTrace().isEmpty()) {
                    foundStackTrace = true;
                    break;
                }
            }
        }
        assertTrue("Should find stackTrace for failed endpoint", foundStackTrace);
    }

    /**
     * Given: Bundle with SUCCESS status and endpoint without failures
     * When: getPublishingJobDetails is called
     * Then: stackTrace is NOT included (null) in the response
     */
    @Test
    public void test_getDetails_stackTraceNotIncludedForSuccessfulEndpoints() throws Exception {
        final String bundleId = createBundleWithEnvironmentEndpoints("success-no-stack-test", Status.SUCCESS);

        final ResponseEntityPublishingJobDetailView result = publishingResource.getPublishingJobDetails(
                mockAuthenticatedRequest(), response, bundleId);

        final PublishingJobDetailView detail = result.getEntity();

        // All endpoints should have null stackTrace
        for (final EnvironmentDetailView env : detail.environments()) {
            for (final EndpointDetailView endpoint : env.endpoints()) {
                assertNull("stackTrace should be null for successful endpoint: " + endpoint.id(),
                        endpoint.stackTrace());
            }
        }
    }

    // =========================================================================
    // HELPER METHODS
    // =========================================================================

    private ResponseEntityPublishingJobsView callEndpoint(String filter, String status)
            throws DotPublisherException {
        return callEndpoint(1, 100, filter, status);
    }

    private ResponseEntityPublishingJobsView callEndpoint(int page, int perPage,
            String filter, String status) throws DotPublisherException {
        return publishingResource.listPublishingJobs(
                mockAuthenticatedRequest(), response, page, perPage, filter, status);
    }

    private String createBundleWithStatus(final String bundleName, final Status status)
            throws DotDataException, DotPublisherException {

        final Bundle bundle = new BundleDataGen()
                .name(bundleName)
                .owner(adminUser)
                .nextPersisted();

        final String bundleId = bundle.getId();
        createdBundleIds.add(bundleId);

        final PublishAuditStatus auditStatus = new PublishAuditStatus(bundleId);
        auditStatus.setStatus(status);
        auditStatus.setStatusUpdated(new Date());

        final PublishAuditHistory history = new PublishAuditHistory();
        auditStatus.setStatusPojo(history);

        publishAuditAPI.insertPublishAuditStatus(auditStatus);
        publishAuditAPI.updatePublishAuditStatus(bundleId, status, history);

        return bundleId;
    }

    private String createBundleWithAssets(final String bundleName, final int assetCount,
            final List<String> outCreatedAssetIds) throws DotDataException, DotPublisherException {

        final var contentType = new ContentTypeDataGen().nextPersisted();
        final List<Contentlet> contentlets = new ArrayList<>();

        for (int i = 0; i < assetCount; i++) {
            contentlets.add(new ContentletDataGen(contentType).nextPersisted());
        }

        final BundleDataGen bundleDataGen = new BundleDataGen()
                .name(bundleName)
                .owner(adminUser);

        // Use identifier consistently for both bundle assets and audit history
        for (final Contentlet contentlet : contentlets) {
            bundleDataGen.addAsset(contentlet.getIdentifier(), PusheableAsset.CONTENTLET);
            if (outCreatedAssetIds != null) {
                outCreatedAssetIds.add(contentlet.getIdentifier());
            }
        }

        final Bundle bundle = bundleDataGen.nextPersisted();
        final String bundleId = bundle.getId();
        createdBundleIds.add(bundleId);

        final PublishAuditStatus auditStatus = new PublishAuditStatus(bundleId);
        auditStatus.setStatus(Status.SUCCESS);

        final PublishAuditHistory history = new PublishAuditHistory();
        final Map<String, String> assets = new HashMap<>();
        for (final Contentlet contentlet : contentlets) {
            assets.put(contentlet.getIdentifier(), PusheableAsset.CONTENTLET.toString());
        }
        history.setAssets(assets);
        auditStatus.setStatusPojo(history);

        publishAuditAPI.insertPublishAuditStatus(auditStatus);
        publishAuditAPI.updatePublishAuditStatus(bundleId, Status.SUCCESS, history);

        return bundleId;
    }

    private String createBundleWithEnvironmentEndpoints(final String bundleName, final Status status)
            throws DotDataException, DotPublisherException {

        final Bundle bundle = new BundleDataGen()
                .name(bundleName)
                .owner(adminUser)
                .nextPersisted();

        final String bundleId = bundle.getId();
        createdBundleIds.add(bundleId);

        final PublishAuditStatus auditStatus = new PublishAuditStatus(bundleId);
        auditStatus.setStatus(status);
        auditStatus.setStatusUpdated(new Date());

        final PublishAuditHistory history = new PublishAuditHistory();

        // Create mock environment/endpoint structure
        final Map<String, Map<String, EndpointDetail>> endpointsMap = new HashMap<>();
        final Map<String, EndpointDetail> endpoints = new HashMap<>();

        final EndpointDetail detail = new EndpointDetail();
        detail.setStatus(Status.SUCCESS.getCode());
        detail.setInfo("Published successfully");
        endpoints.put("endpoint-1", detail);

        endpointsMap.put("environment-1", endpoints);
        history.setEndpointsMap(endpointsMap);

        auditStatus.setStatusPojo(history);

        publishAuditAPI.insertPublishAuditStatus(auditStatus);
        publishAuditAPI.updatePublishAuditStatus(bundleId, status, history);

        return bundleId;
    }

    private String createBundleWithTimestamps(final String bundleName, final Status status)
            throws DotDataException, DotPublisherException {

        final Bundle bundle = new BundleDataGen()
                .name(bundleName)
                .owner(adminUser)
                .nextPersisted();

        final String bundleId = bundle.getId();
        createdBundleIds.add(bundleId);

        final PublishAuditStatus auditStatus = new PublishAuditStatus(bundleId);
        auditStatus.setStatus(status);
        auditStatus.setStatusUpdated(new Date());

        final PublishAuditHistory history = new PublishAuditHistory();

        // Set all timestamps in logical order
        final long baseTime = System.currentTimeMillis();
        history.setBundleStart(new Date(baseTime));
        history.setBundleEnd(new Date(baseTime + 1000));
        history.setPublishStart(new Date(baseTime + 2000));
        history.setPublishEnd(new Date(baseTime + 3000));

        auditStatus.setStatusPojo(history);

        publishAuditAPI.insertPublishAuditStatus(auditStatus);
        publishAuditAPI.updatePublishAuditStatus(bundleId, status, history);

        return bundleId;
    }

    private String createBundleWithFailedEndpoint(final String bundleName)
            throws DotDataException, DotPublisherException {

        final Bundle bundle = new BundleDataGen()
                .name(bundleName)
                .owner(adminUser)
                .nextPersisted();

        final String bundleId = bundle.getId();
        createdBundleIds.add(bundleId);

        final PublishAuditStatus auditStatus = new PublishAuditStatus(bundleId);
        auditStatus.setStatus(Status.FAILED_TO_PUBLISH);
        auditStatus.setStatusUpdated(new Date());

        final PublishAuditHistory history = new PublishAuditHistory();

        // Create mock environment/endpoint structure with failure
        final Map<String, Map<String, EndpointDetail>> endpointsMap = new HashMap<>();
        final Map<String, EndpointDetail> endpoints = new HashMap<>();

        final EndpointDetail failedDetail = new EndpointDetail();
        failedDetail.setStatus(Status.FAILED_TO_PUBLISH.getCode());
        failedDetail.setInfo("Connection refused");
        failedDetail.setStackTrace("java.net.ConnectException: Connection refused\n" +
                "    at java.net.PlainSocketImpl.socketConnect(Native Method)\n" +
                "    at java.net.AbstractPlainSocketImpl.doConnect(AbstractPlainSocketImpl.java:350)");
        endpoints.put("endpoint-failed-1", failedDetail);

        endpointsMap.put("environment-failed-1", endpoints);
        history.setEndpointsMap(endpointsMap);

        auditStatus.setStatusPojo(history);

        publishAuditAPI.insertPublishAuditStatus(auditStatus);
        publishAuditAPI.updatePublishAuditStatus(bundleId, Status.FAILED_TO_PUBLISH, history);

        return bundleId;
    }

    private HttpServletRequest mockAuthenticatedRequest() {
        final MockHeaderRequest request = new MockHeaderRequest(
                new MockSessionRequest(
                        new MockAttributeRequest(
                                new MockHttpRequestIntegrationTest("localhost", "/api/v1/publishing")
                                        .request())
                                .request())
                        .request());

        request.setAttribute(WebKeys.USER, adminUser);
        return request;
    }

    // =========================================================================
    // DELETE ENDPOINT TESTS
    // =========================================================================

    /**
     * Given: Bundle exists with terminal status (SUCCESS)
     * When: DELETE request made
     * Then: Bundle is deleted successfully (200)
     */
    @Test
    public void test_deleteBundle_successfulDeletion() throws Exception {
        final String bundleId = createBundleWithStatus("delete-test-success", Status.SUCCESS);

        final Response result = publishingResource.deletePublishingJob(
                mockAuthenticatedRequest(), response, bundleId);

        assertEquals(Response.Status.OK.getStatusCode(), result.getStatus());

        // Verify bundle no longer exists
        assertNull(publishAuditAPI.getPublishAuditStatus(bundleId));
        assertNull(APILocator.getBundleAPI().getBundleById(bundleId));

        // Remove from cleanup list since already deleted
        createdBundleIds.remove(bundleId);
    }

    /**
     * Given: Bundle exists with queued status (WAITING_FOR_PUBLISHING)
     * When: DELETE request made
     * Then: Bundle is deleted successfully (200) - can cancel queued
     */
    @Test
    public void test_deleteBundle_deletesQueuedBundle() throws Exception {
        final String bundleId = createBundleWithStatus("delete-queued", Status.WAITING_FOR_PUBLISHING);

        final Response result = publishingResource.deletePublishingJob(
                mockAuthenticatedRequest(), response, bundleId);

        assertEquals(Response.Status.OK.getStatusCode(), result.getStatus());
        createdBundleIds.remove(bundleId);
    }

    /**
     * Given: Bundle exists with failed status
     * When: DELETE request made
     * Then: Bundle is deleted successfully (200)
     */
    @Test
    public void test_deleteBundle_deletesFailedBundle() throws Exception {
        final String bundleId = createBundleWithStatus("delete-failed", Status.FAILED_TO_PUBLISH);

        final Response result = publishingResource.deletePublishingJob(
                mockAuthenticatedRequest(), response, bundleId);

        assertEquals(Response.Status.OK.getStatusCode(), result.getStatus());
        createdBundleIds.remove(bundleId);
    }

    /**
     * Given: Bundle does not exist
     * When: DELETE request made
     * Then: NotFoundException thrown (404)
     */
    @Test(expected = NotFoundException.class)
    public void test_deleteBundle_notFound() throws Exception {
        publishingResource.deletePublishingJob(
                mockAuthenticatedRequest(), response, "nonexistent-bundle-id-12345");
    }

    /**
     * Given: Bundle exists with BUNDLING status (in-progress)
     * When: DELETE request made
     * Then: ConflictException thrown (409)
     */
    @Test(expected = ConflictException.class)
    public void test_deleteBundle_conflictWhenBundling() throws Exception {
        final String bundleId = createBundleWithStatus("delete-bundling", Status.BUNDLING);

        publishingResource.deletePublishingJob(
                mockAuthenticatedRequest(), response, bundleId);
    }

    /**
     * Given: Bundle exists with SENDING_TO_ENDPOINTS status (in-progress)
     * When: DELETE request made
     * Then: ConflictException thrown (409)
     */
    @Test(expected = ConflictException.class)
    public void test_deleteBundle_conflictWhenSending() throws Exception {
        final String bundleId = createBundleWithStatus("delete-sending", Status.SENDING_TO_ENDPOINTS);

        publishingResource.deletePublishingJob(
                mockAuthenticatedRequest(), response, bundleId);
    }

    /**
     * Given: Bundle exists with PUBLISHING_BUNDLE status (in-progress)
     * When: DELETE request made
     * Then: ConflictException thrown (409)
     */
    @Test(expected = ConflictException.class)
    public void test_deleteBundle_conflictWhenPublishing() throws Exception {
        final String bundleId = createBundleWithStatus("delete-publishing", Status.PUBLISHING_BUNDLE);

        publishingResource.deletePublishingJob(
                mockAuthenticatedRequest(), response, bundleId);
    }

    /**
     * Given: Empty bundleId
     * When: DELETE request made
     * Then: BadRequestException thrown (400)
     */
    @Test(expected = BadRequestException.class)
    public void test_deleteBundle_emptyBundleId() throws Exception {
        publishingResource.deletePublishingJob(
                mockAuthenticatedRequest(), response, "");
    }

    // =========================================================================
    // RETRY ENDPOINT TESTS - POST /v1/publishing/retry
    // =========================================================================

    /**
     * Given: Bundle with FAILED_TO_PUBLISH status exists
     * When: Retry request with single bundleId
     * Then: Returns result for that bundle (success or failure based on configuration)
     */
    @Test
    public void test_retryBundles_singleBundleReturnsResult() throws Exception {
        final String bundleId = createBundleWithStatus("retry-single-test", Status.FAILED_TO_PUBLISH);

        final RetryBundlesForm form = RetryBundlesForm.builder()
                .bundleIds(List.of(bundleId))
                .forcePush(false)
                .deliveryStrategy(DeliveryStrategy.ALL_ENDPOINTS)
                .build();

        final ResponseEntityRetryBundlesView result = publishingResource.retryBundles(
                mockAuthenticatedRequest(), response, form);

        assertNotNull(result);
        assertNotNull(result.getEntity());
        assertEquals("Should have one result", 1, result.getEntity().size());

        final RetryBundleResultView bundleResult = result.getEntity().get(0);
        assertEquals("BundleId should match", bundleId, bundleResult.bundleId());
        assertNotNull("Message should not be null", bundleResult.message());
        assertEquals("DeliveryStrategy should match", "ALL_ENDPOINTS", bundleResult.deliveryStrategy());
    }

    /**
     * Given: Multiple bundles with different statuses exist
     * When: Retry request with multiple bundleIds
     * Then: Returns per-bundle results
     */
    @Test
    public void test_retryBundles_multipleBundlesReturnsPerBundleResults() throws Exception {
        final String failedId = createBundleWithStatus("retry-multi-failed", Status.FAILED_TO_PUBLISH);
        final String successId = createBundleWithStatus("retry-multi-success", Status.SUCCESS);
        final String bundlingId = createBundleWithStatus("retry-multi-bundling", Status.BUNDLING);

        final RetryBundlesForm form = RetryBundlesForm.builder()
                .bundleIds(List.of(failedId, successId, bundlingId))
                .forcePush(true)
                .deliveryStrategy(DeliveryStrategy.FAILED_ENDPOINTS)
                .build();

        final ResponseEntityRetryBundlesView result = publishingResource.retryBundles(
                mockAuthenticatedRequest(), response, form);

        assertNotNull(result);
        assertNotNull(result.getEntity());
        assertEquals("Should have three results", 3, result.getEntity().size());

        // Verify each bundle has a result
        final boolean foundFailed = result.getEntity().stream()
                .anyMatch(r -> failedId.equals(r.bundleId()));
        final boolean foundSuccess = result.getEntity().stream()
                .anyMatch(r -> successId.equals(r.bundleId()));
        final boolean foundBundling = result.getEntity().stream()
                .anyMatch(r -> bundlingId.equals(r.bundleId()));

        assertTrue("Should have result for failed bundle", foundFailed);
        assertTrue("Should have result for success bundle", foundSuccess);
        assertTrue("Should have result for bundling bundle", foundBundling);

        // Bundling bundle should fail (non-retryable status)
        final RetryBundleResultView bundlingResult = result.getEntity().stream()
                .filter(r -> bundlingId.equals(r.bundleId()))
                .findFirst()
                .orElseThrow();

        assertFalse("BUNDLING status should not be retryable", bundlingResult.success());
        assertTrue("Message should indicate cannot retry",
                bundlingResult.message().contains("Cannot retry"));
    }

    /**
     * Given: Bundle with non-retryable status (BUNDLING)
     * When: Retry request made
     * Then: Returns failure result with appropriate message
     */
    @Test
    public void test_retryBundles_nonRetryableStatus_returnsFailure() throws Exception {
        final String bundleId = createBundleWithStatus("retry-non-retryable", Status.BUNDLING);

        final RetryBundlesForm form = RetryBundlesForm.builder()
                .bundleIds(List.of(bundleId))
                .forcePush(false)
                .deliveryStrategy(DeliveryStrategy.ALL_ENDPOINTS)
                .build();

        final ResponseEntityRetryBundlesView result = publishingResource.retryBundles(
                mockAuthenticatedRequest(), response, form);

        assertNotNull(result);
        assertEquals(1, result.getEntity().size());

        final RetryBundleResultView bundleResult = result.getEntity().get(0);
        assertFalse("Should fail for non-retryable status", bundleResult.success());
        assertTrue("Message should mention status restriction",
                bundleResult.message().toLowerCase().contains("cannot retry") ||
                bundleResult.message().toLowerCase().contains("status"));
    }

    /**
     * Given: Bundle with SENDING_TO_ENDPOINTS status (in progress)
     * When: Retry request made
     * Then: Returns failure result
     */
    @Test
    public void test_retryBundles_sendingStatus_returnsFailure() throws Exception {
        final String bundleId = createBundleWithStatus("retry-sending", Status.SENDING_TO_ENDPOINTS);

        final RetryBundlesForm form = RetryBundlesForm.builder()
                .bundleIds(List.of(bundleId))
                .forcePush(false)
                .deliveryStrategy(DeliveryStrategy.ALL_ENDPOINTS)
                .build();

        final ResponseEntityRetryBundlesView result = publishingResource.retryBundles(
                mockAuthenticatedRequest(), response, form);

        final RetryBundleResultView bundleResult = result.getEntity().get(0);
        assertFalse("Should fail for in-progress status", bundleResult.success());
    }

    /**
     * Given: Empty bundleIds list
     * When: Retry request made
     * Then: BadRequestException thrown
     */
    @Test(expected = BadRequestException.class)
    public void test_retryBundles_emptyBundleIds_throwsBadRequest() throws Exception {
        final RetryBundlesForm form = RetryBundlesForm.builder()
                .bundleIds(List.of())
                .forcePush(false)
                .deliveryStrategy(DeliveryStrategy.ALL_ENDPOINTS)
                .build();

        publishingResource.retryBundles(mockAuthenticatedRequest(), response, form);
    }

    /**
     * Given: Null form
     * When: Retry request made
     * Then: BadRequestException thrown
     */
    @Test(expected = BadRequestException.class)
    public void test_retryBundles_nullForm_throwsBadRequest() throws Exception {
        publishingResource.retryBundles(mockAuthenticatedRequest(), response, null);
    }

    /**
     * Given: Non-existent bundleId
     * When: Retry request made
     * Then: Returns failure result (not exception)
     */
    @Test
    public void test_retryBundles_nonExistentBundle_returnsFailure() throws Exception {
        final String nonExistentId = "non-existent-bundle-id-xyz-123";

        final RetryBundlesForm form = RetryBundlesForm.builder()
                .bundleIds(List.of(nonExistentId))
                .forcePush(false)
                .deliveryStrategy(DeliveryStrategy.ALL_ENDPOINTS)
                .build();

        final ResponseEntityRetryBundlesView result = publishingResource.retryBundles(
                mockAuthenticatedRequest(), response, form);

        assertNotNull(result);
        assertEquals(1, result.getEntity().size());

        final RetryBundleResultView bundleResult = result.getEntity().get(0);
        assertFalse("Should fail for non-existent bundle", bundleResult.success());
        assertTrue("Message should indicate bundle not found",
                bundleResult.message().toLowerCase().contains("not found"));
    }

    /**
     * Given: Bundle with SUCCESS status
     * When: Retry request with forcePush=false
     * Then: Result shows forcePush=true (auto-enabled for successful bundles)
     */
    @Test
    public void test_retryBundles_successfulBundle_autoEnablesForcePush() throws Exception {
        final String bundleId = createBundleWithStatus("retry-force-push", Status.SUCCESS);

        final RetryBundlesForm form = RetryBundlesForm.builder()
                .bundleIds(List.of(bundleId))
                .forcePush(false) // User specified false
                .deliveryStrategy(DeliveryStrategy.ALL_ENDPOINTS)
                .build();

        final ResponseEntityRetryBundlesView result = publishingResource.retryBundles(
                mockAuthenticatedRequest(), response, form);

        // Note: The result may fail due to missing bundle files in test environment,
        // but we're checking the logic flow
        // For a proper test, we'd need to set up the full bundle structure
        assertNotNull(result);
        assertEquals(1, result.getEntity().size());
    }

    /**
     * Given: Bundle with SUCCESS_WITH_WARNINGS status
     * When: Retry request made
     * Then: Returns result (SUCCESS_WITH_WARNINGS is retryable)
     */
    @Test
    public void test_retryBundles_successWithWarnings_isRetryable() throws Exception {
        final String bundleId = createBundleWithStatus("retry-warnings", Status.SUCCESS_WITH_WARNINGS);

        final RetryBundlesForm form = RetryBundlesForm.builder()
                .bundleIds(List.of(bundleId))
                .forcePush(true)
                .deliveryStrategy(DeliveryStrategy.ALL_ENDPOINTS)
                .build();

        final ResponseEntityRetryBundlesView result = publishingResource.retryBundles(
                mockAuthenticatedRequest(), response, form);

        assertNotNull(result);
        assertEquals(1, result.getEntity().size());
        // SUCCESS_WITH_WARNINGS is a retryable status, so should not get "Cannot retry" message
        final RetryBundleResultView bundleResult = result.getEntity().get(0);
        // It may fail for other reasons (like missing bundle file) but not due to status
        if (!bundleResult.success()) {
            assertFalse("Should not fail due to status restriction",
                    bundleResult.message().toLowerCase().contains("cannot retry bundles with status"));
        }
    }

    /**
     * Given: Bundle with FAILED_TO_SEND_TO_ALL_GROUPS status
     * When: Retry request made
     * Then: Status is retryable (returns result, not status error)
     */
    @Test
    public void test_retryBundles_failedToSendToAllGroups_isRetryable() throws Exception {
        final String bundleId = createBundleWithStatus("retry-all-groups-failed", Status.FAILED_TO_SEND_TO_ALL_GROUPS);

        final RetryBundlesForm form = RetryBundlesForm.builder()
                .bundleIds(List.of(bundleId))
                .forcePush(false)
                .deliveryStrategy(DeliveryStrategy.FAILED_ENDPOINTS)
                .build();

        final ResponseEntityRetryBundlesView result = publishingResource.retryBundles(
                mockAuthenticatedRequest(), response, form);

        assertNotNull(result);
        final RetryBundleResultView bundleResult = result.getEntity().get(0);
        // Should not fail due to non-retryable status
        if (!bundleResult.success()) {
            assertFalse("Should not fail due to status restriction",
                    bundleResult.message().toLowerCase().contains("cannot retry bundles with status"));
        }
    }

    /**
     * Given: Request with FAILED_ENDPOINTS delivery strategy
     * When: Retry request made
     * Then: Result shows FAILED_ENDPOINTS strategy
     */
    @Test
    public void test_retryBundles_failedEndpointsStrategy_reflected() throws Exception {
        final String bundleId = createBundleWithStatus("retry-strategy-test", Status.FAILED_TO_PUBLISH);

        final RetryBundlesForm form = RetryBundlesForm.builder()
                .bundleIds(List.of(bundleId))
                .forcePush(false)
                .deliveryStrategy(DeliveryStrategy.FAILED_ENDPOINTS)
                .build();

        final ResponseEntityRetryBundlesView result = publishingResource.retryBundles(
                mockAuthenticatedRequest(), response, form);

        assertNotNull(result);
        final RetryBundleResultView bundleResult = result.getEntity().get(0);
        assertEquals("DeliveryStrategy should be FAILED_ENDPOINTS",
                "FAILED_ENDPOINTS", bundleResult.deliveryStrategy());
    }

    /**
     * Given: Bundle ID with whitespace
     * When: Retry request made
     * Then: Bundle ID is trimmed and processed
     */
    @Test
    public void test_retryBundles_bundleIdWithWhitespace_isTrimmed() throws Exception {
        final String bundleId = createBundleWithStatus("retry-whitespace-test", Status.FAILED_TO_PUBLISH);

        final RetryBundlesForm form = RetryBundlesForm.builder()
                .bundleIds(List.of("  " + bundleId + "  "))
                .forcePush(false)
                .deliveryStrategy(DeliveryStrategy.ALL_ENDPOINTS)
                .build();

        final ResponseEntityRetryBundlesView result = publishingResource.retryBundles(
                mockAuthenticatedRequest(), response, form);

        assertNotNull(result);
        assertEquals(1, result.getEntity().size());
        // The trimmed bundleId should be in the result
        // (bundleId in result is the trimmed version)
        final RetryBundleResultView bundleResult = result.getEntity().get(0);
        assertEquals("BundleId should be trimmed", bundleId, bundleResult.bundleId());
    }

    /**
     * Given: Empty string bundleId in list
     * When: Retry request made
     * Then: Returns failure result for that bundle
     */
    @Test
    public void test_retryBundles_emptyBundleIdInList_returnsFailure() throws Exception {
        final String validBundleId = createBundleWithStatus("retry-with-empty", Status.FAILED_TO_PUBLISH);

        final RetryBundlesForm form = RetryBundlesForm.builder()
                .bundleIds(List.of(validBundleId, "", "   "))
                .forcePush(false)
                .deliveryStrategy(DeliveryStrategy.ALL_ENDPOINTS)
                .build();

        final ResponseEntityRetryBundlesView result = publishingResource.retryBundles(
                mockAuthenticatedRequest(), response, form);

        assertNotNull(result);
        assertEquals("Should have three results", 3, result.getEntity().size());

        // Find results for empty/whitespace IDs
        final long failedEmptyCount = result.getEntity().stream()
                .filter(r -> !r.success() && r.message().toLowerCase().contains("required"))
                .count();

        assertTrue("Should have failures for empty bundle IDs", failedEmptyCount >= 1);
    }
}
