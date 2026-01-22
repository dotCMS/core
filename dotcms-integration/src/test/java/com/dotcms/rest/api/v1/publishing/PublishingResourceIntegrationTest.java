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
}
