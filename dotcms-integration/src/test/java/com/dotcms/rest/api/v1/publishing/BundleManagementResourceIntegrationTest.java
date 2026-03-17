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
import com.dotcms.publisher.bundle.business.BundleAPI;
import com.dotcms.publisher.business.PublishAuditAPI;
import com.dotcms.publisher.business.PublishAuditHistory;
import com.dotcms.publisher.business.PublishAuditStatus;
import com.dotcms.publisher.business.PublishAuditStatus.Status;
import com.dotcms.publisher.business.PublisherAPI;
import com.dotcms.rest.exception.BadRequestException;
import com.dotcms.rest.exception.ConflictException;
import com.dotcms.rest.exception.NotFoundException;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Role;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotcms.contenttype.model.type.ContentType;
import com.liferay.portal.model.User;
import com.liferay.portal.util.WebKeys;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Integration tests for BundleManagementResource.
 * Focuses on data correctness and business logic for adding
 * and removing assets from publishing bundles.
 *
 * @author hassandotcms
 * @since Mar 2026
 */
public class BundleManagementResourceIntegrationTest {

    private static User adminUser;
    private static List<String> createdBundleIds;
    private static BundleManagementResource resource;
    private static HttpServletResponse mockResponse;
    private static BundleAPI bundleAPI;
    private static PublishAuditAPI publishAuditAPI;
    private static PublisherAPI publisherQueueAPI;
    private static ContentType testContentType;

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
        adminUser = TestUserUtils.getAdminUser();

        final Role backendRole = TestUserUtils.getBackendRole();
        APILocator.getRoleAPI().addRoleToUser(backendRole, adminUser);

        createdBundleIds = new ArrayList<>();
        resource = new BundleManagementResource();
        mockResponse = new MockHttpResponse();
        bundleAPI = APILocator.getBundleAPI();
        publishAuditAPI = PublishAuditAPI.getInstance();
        publisherQueueAPI = PublisherAPI.getInstance();
        testContentType = new ContentTypeDataGen().nextPersisted();
    }

    @AfterClass
    public static void cleanup() {
        if (createdBundleIds != null) {
            for (final String bundleId : createdBundleIds) {
                try {
                    publishAuditAPI.deletePublishAuditStatus(bundleId);
                    bundleAPI.deleteBundleAndDependencies(bundleId, adminUser);
                } catch (Exception e) {
                    // Ignore cleanup errors
                }
            }
        }
    }

    // =========================================================================
    // POST /v1/bundles/assets — Add Assets to Bundle
    // =========================================================================

    /**
     * Given: An existing bundle with a known ID
     * When: Adding assets by bundle ID
     * Then: Assets are added, created=false, correct bundleId returned
     */
    @Test
    public void test_addAssets_existingBundleById_addsAssetsSuccessfully() throws Exception {
        final Bundle bundle = createBundle("add-by-id");
        final Contentlet contentlet = createContentlet();

        final AddAssetsToBundleForm form = new AddAssetsToBundleForm(
                bundle.getId(), null, List.of(contentlet.getIdentifier()));

        final ResponseEntityAddAssetsToBundleView result =
                resource.addAssetsToBundle(mockAuthRequest(), mockResponse, form);

        final AddAssetsToBundleView view = result.getEntity();
        assertNotNull(view);
        assertEquals(bundle.getId(), view.bundleId());
        assertFalse("Should not create new bundle", view.created());
        assertTrue("Should have added at least one asset", view.total() > 0);
    }

    /**
     * Given: An existing unsent bundle with a known name
     * When: Adding assets by bundle name (no bundleId)
     * Then: The existing bundle is found by name, created=false
     */
    @Test
    public void test_addAssets_existingBundleByName_findsCorrectBundle() throws Exception {
        final String bundleName = "add-by-name-" + System.currentTimeMillis();
        final Bundle bundle = createBundle(bundleName);
        final Contentlet contentlet = createContentlet();

        final AddAssetsToBundleForm form = new AddAssetsToBundleForm(
                null, bundleName, List.of(contentlet.getIdentifier()));

        final ResponseEntityAddAssetsToBundleView result =
                resource.addAssetsToBundle(mockAuthRequest(), mockResponse, form);

        final AddAssetsToBundleView view = result.getEntity();
        assertNotNull(view);
        assertEquals(bundle.getId(), view.bundleId());
        assertEquals(bundleName, view.bundleName());
        assertFalse("Should not create new bundle", view.created());
    }

    /**
     * Given: No matching bundle exists
     * When: Adding assets with a unique name
     * Then: A new bundle is created, created=true
     */
    @Test
    public void test_addAssets_noMatchingBundle_createsNewBundle() throws Exception {
        final String uniqueName = "auto-create-" + System.currentTimeMillis();
        final Contentlet contentlet = createContentlet();

        final AddAssetsToBundleForm form = new AddAssetsToBundleForm(
                null, uniqueName, List.of(contentlet.getIdentifier()));

        final ResponseEntityAddAssetsToBundleView result =
                resource.addAssetsToBundle(mockAuthRequest(), mockResponse, form);

        final AddAssetsToBundleView view = result.getEntity();
        assertNotNull(view);
        assertNotNull("Bundle ID should be set", view.bundleId());
        assertEquals(uniqueName, view.bundleName());
        assertTrue("Should have created a new bundle", view.created());
        createdBundleIds.add(view.bundleId());
    }

    /**
     * Given: Empty assetIds list
     * When: Adding assets
     * Then: 400 BadRequestException
     */
    @Test(expected = BadRequestException.class)
    public void test_addAssets_emptyAssetIds_returns400() {
        final AddAssetsToBundleForm form = new AddAssetsToBundleForm(
                null, "test-bundle", List.of());

        resource.addAssetsToBundle(mockAuthRequest(), mockResponse, form);
    }

    /**
     * Given: Null assetIds
     * When: Adding assets
     * Then: 400 BadRequestException
     */
    @Test(expected = BadRequestException.class)
    public void test_addAssets_nullAssetIds_returns400() {
        final AddAssetsToBundleForm form = new AddAssetsToBundleForm(
                null, "test-bundle", null);

        resource.addAssetsToBundle(mockAuthRequest(), mockResponse, form);
    }

    /**
     * Given: Null form body
     * When: Adding assets
     * Then: 400 BadRequestException
     */
    @Test(expected = BadRequestException.class)
    public void test_addAssets_nullForm_returns400() {
        resource.addAssetsToBundle(mockAuthRequest(), mockResponse, null);
    }

    // =========================================================================
    // DELETE /v1/bundles/{bundleId}/assets — Remove Assets from Bundle
    // =========================================================================

    /**
     * Given: A bundle with one asset in the queue
     * When: Removing that asset
     * Then: success=true, asset removed from queue
     */
    @Test
    public void test_removeAssets_singleAsset_removesSuccessfully() throws Exception {
        final Contentlet contentlet = createContentlet();
        final Bundle bundle = createBundleWithAssets(
                "remove-single", List.of(contentlet.getIdentifier()));

        final RemoveAssetsFromBundleForm form = RemoveAssetsFromBundleForm.builder()
                .assetIds(List.of(contentlet.getIdentifier()))
                .build();

        final ResponseEntityRemoveAssetsFromBundleView result =
                resource.removeAssetsFromBundle(
                        mockAuthRequest(), mockResponse, bundle.getId(), form);

        final List<RemoveAssetResultView> results = result.getEntity();
        assertNotNull(results);
        assertEquals(1, results.size());
        assertTrue("Asset removal should succeed", results.get(0).success());
        assertEquals(contentlet.getIdentifier(), results.get(0).assetId());
    }

    /**
     * Given: A bundle with multiple assets
     * When: Removing all assets
     * Then: Per-asset results, all success=true
     */
    @Test
    public void test_removeAssets_multipleAssets_allSucceed() throws Exception {
        final Contentlet c1 = createContentlet();
        final Contentlet c2 = createContentlet();
        final List<String> assetIds = List.of(
                c1.getIdentifier(), c2.getIdentifier());
        final Bundle bundle = createBundleWithAssets("remove-multiple", assetIds);

        final RemoveAssetsFromBundleForm form = RemoveAssetsFromBundleForm.builder()
                .assetIds(assetIds)
                .build();

        final ResponseEntityRemoveAssetsFromBundleView result =
                resource.removeAssetsFromBundle(
                        mockAuthRequest(), mockResponse, bundle.getId(), form);

        final List<RemoveAssetResultView> results = result.getEntity();
        assertNotNull(results);
        assertEquals(2, results.size());
        assertTrue("First asset removal should succeed", results.get(0).success());
        assertTrue("Second asset removal should succeed", results.get(1).success());
    }

    /**
     * Given: A bundle with one asset and an audit status
     * When: Removing the last asset
     * Then: Audit status is also cleaned up
     */
    @Test
    public void test_removeAssets_lastAsset_cleansUpAuditStatus() throws Exception {
        final Contentlet contentlet = createContentlet();
        final Bundle bundle = createBundleWithAssets(
                "remove-last", List.of(contentlet.getIdentifier()));

        // Create audit status for the bundle
        final PublishAuditStatus auditStatus = new PublishAuditStatus(bundle.getId());
        auditStatus.setStatus(Status.SUCCESS);
        auditStatus.setStatusUpdated(new Date());
        final PublishAuditHistory history = new PublishAuditHistory();
        auditStatus.setStatusPojo(history);
        publishAuditAPI.insertPublishAuditStatus(auditStatus);
        publishAuditAPI.updatePublishAuditStatus(bundle.getId(), Status.SUCCESS, history);

        // Verify audit status exists before removal
        assertNotNull("Audit status should exist before removal",
                publishAuditAPI.getPublishAuditStatus(bundle.getId()));

        final RemoveAssetsFromBundleForm form = RemoveAssetsFromBundleForm.builder()
                .assetIds(List.of(contentlet.getIdentifier()))
                .build();

        final ResponseEntityRemoveAssetsFromBundleView result =
                resource.removeAssetsFromBundle(
                        mockAuthRequest(), mockResponse, bundle.getId(), form);

        final List<RemoveAssetResultView> results = result.getEntity();
        assertEquals(1, results.size());
        assertTrue("Asset removal should succeed", results.get(0).success());

        // Verify audit status was cleaned up (last asset removed)
        assertNull("Audit status should be cleaned up after last asset removal",
                publishAuditAPI.getPublishAuditStatus(bundle.getId()));
    }

    /**
     * Given: Non-existent bundleId
     * When: Removing assets
     * Then: 404 NotFoundException
     */
    @Test(expected = NotFoundException.class)
    public void test_removeAssets_bundleNotFound_returns404() {
        final RemoveAssetsFromBundleForm form = RemoveAssetsFromBundleForm.builder()
                .assetIds(List.of("some-asset-id"))
                .build();

        resource.removeAssetsFromBundle(
                mockAuthRequest(), mockResponse, "non-existent-bundle-id-xyz", form);
    }

    /**
     * Given: A bundle in BUNDLING status (in-progress)
     * When: Removing assets
     * Then: 409 ConflictException
     */
    @Test(expected = ConflictException.class)
    public void test_removeAssets_bundleInProgress_returns409() throws Exception {
        final Bundle bundle = createBundleWithStatus("remove-in-progress", Status.BUNDLING);

        final RemoveAssetsFromBundleForm form = RemoveAssetsFromBundleForm.builder()
                .assetIds(List.of("some-asset-id"))
                .build();

        resource.removeAssetsFromBundle(
                mockAuthRequest(), mockResponse, bundle.getId(), form);
    }

    /**
     * Given: Empty assetIds
     * When: Removing assets
     * Then: 400 BadRequestException
     */
    @Test(expected = BadRequestException.class)
    public void test_removeAssets_emptyAssetIds_returns400() throws Exception {
        final Bundle bundle = createBundle("remove-empty");

        final RemoveAssetsFromBundleForm form = RemoveAssetsFromBundleForm.builder()
                .assetIds(List.of())
                .build();

        resource.removeAssetsFromBundle(
                mockAuthRequest(), mockResponse, bundle.getId(), form);
    }

    /**
     * Given: Null form body
     * When: Removing assets
     * Then: 400 BadRequestException
     */
    @Test(expected = BadRequestException.class)
    public void test_removeAssets_nullForm_returns400() throws Exception {
        final Bundle bundle = createBundle("remove-null-form");

        resource.removeAssetsFromBundle(
                mockAuthRequest(), mockResponse, bundle.getId(), null);
    }

    /**
     * Given: A bundle with one asset, request to remove a different asset ID
     * When: Removing asset not in bundle
     * Then: success=false with "Asset not found in bundle", other assets still processed
     */
    @Test
    public void test_removeAssets_assetNotInBundle_returnsFailureForMissing() throws Exception {
        final Contentlet contentlet = createContentlet();
        final Bundle bundle = createBundleWithAssets(
                "remove-not-found", List.of(contentlet.getIdentifier()));

        final String fakeAssetId = "non-existent-asset-id";
        final RemoveAssetsFromBundleForm form = RemoveAssetsFromBundleForm.builder()
                .assetIds(List.of(fakeAssetId, contentlet.getIdentifier()))
                .build();

        final ResponseEntityRemoveAssetsFromBundleView result =
                resource.removeAssetsFromBundle(
                        mockAuthRequest(), mockResponse, bundle.getId(), form);

        final List<RemoveAssetResultView> results = result.getEntity();
        assertNotNull(results);
        assertEquals(2, results.size());

        // First asset (not in bundle) should fail
        assertFalse("Non-existent asset should fail", results.get(0).success());
        assertEquals(fakeAssetId, results.get(0).assetId());
        assertTrue("Message should indicate not found",
                results.get(0).message().contains("not found"));

        // Second asset (in bundle) should succeed
        assertTrue("Existing asset should succeed", results.get(1).success());
        assertEquals(contentlet.getIdentifier(), results.get(1).assetId());
    }

    // =========================================================================
    // Helper Methods
    // =========================================================================

    private HttpServletRequest mockAuthRequest() {
        final MockHeaderRequest request = new MockHeaderRequest(
                new MockSessionRequest(
                        new MockAttributeRequest(
                                new MockHttpRequestIntegrationTest(
                                        "localhost", "/api/v1/bundles")
                                        .request())
                                .request())
                        .request());
        request.setAttribute(WebKeys.USER, adminUser);
        return request;
    }

    private Bundle createBundle(final String name) throws DotDataException {
        final Bundle bundle = new BundleDataGen()
                .name(name)
                .owner(adminUser)
                .nextPersisted();
        createdBundleIds.add(bundle.getId());
        return bundle;
    }

    private Contentlet createContentlet() {
        return new ContentletDataGen(testContentType).nextPersisted();
    }

    private Bundle createBundleWithAssets(final String name, final List<String> assetIds)
            throws Exception {
        final Bundle bundle = createBundle(name);
        publisherQueueAPI.saveBundleAssets(assetIds, bundle.getId(), adminUser);
        return bundle;
    }

    private Bundle createBundleWithStatus(final String name, final Status status)
            throws Exception {
        final Bundle bundle = createBundle(name);

        final PublishAuditStatus auditStatus = new PublishAuditStatus(bundle.getId());
        auditStatus.setStatus(status);
        auditStatus.setStatusUpdated(new Date());

        final PublishAuditHistory history = new PublishAuditHistory();
        auditStatus.setStatusPojo(history);

        publishAuditAPI.insertPublishAuditStatus(auditStatus);
        publishAuditAPI.updatePublishAuditStatus(bundle.getId(), status, history);

        return bundle;
    }

}
