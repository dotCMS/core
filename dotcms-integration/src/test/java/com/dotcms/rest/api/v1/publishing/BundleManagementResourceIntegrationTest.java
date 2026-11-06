package com.dotcms.rest.api.v1.publishing;

import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.TestUserUtils;
import com.dotcms.datagen.UserDataGen;
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
import com.dotcms.publisher.business.PublishQueueElement;
import com.dotcms.publisher.business.PublisherAPI;
import com.dotcms.rest.exception.BadRequestException;
import com.dotcms.rest.exception.ConflictException;
import com.dotcms.rest.exception.ForbiddenException;
import com.dotcms.rest.exception.NotFoundException;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
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

    private static User contributorUser;

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
        adminUser = TestUserUtils.getAdminUser();

        final Role backendRole = TestUserUtils.getBackendRole();
        APILocator.getRoleAPI().addRoleToUser(backendRole, adminUser);

        contributorUser = TestUserUtils.getJoeContributorUser();

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

    /**
     * Given: A contributor user (no publish permission) adding assets
     * When: Adding assets to a new bundle
     * Then: Assets appear in the errors list (permission denied), not as HTTP error
     */
    @Test
    public void test_addAssets_nonAdminNoPublishPermission_reportsPerAssetErrors() throws Exception {
        final Contentlet contentlet = createContentlet();

        final String bundleName = "contributor-test-" + System.currentTimeMillis();
        final AddAssetsToBundleForm form = new AddAssetsToBundleForm(
                null, bundleName, List.of(contentlet.getIdentifier()));

        final ResponseEntityAddAssetsToBundleView result =
                resource.addAssetsToBundle(
                        mockAuthRequest(contributorUser), mockResponse, form);

        final AddAssetsToBundleView view = result.getEntity();
        assertNotNull(view);
        createdBundleIds.add(view.bundleId());
        assertTrue("Bundle should have been created", view.created());
        assertFalse("Errors list should not be empty for unpermitted assets",
                view.errors().isEmpty());
        assertTrue("Error should mention permission denial",
                view.errors().get(0).contains("does not have Publish Permission"));
    }

    /**
     * Given: A bundle with an asset already added
     * When: Adding the same asset again
     * Then: Duplicate is silently skipped, no error, total=0
     */
    @Test
    public void test_addAssets_duplicateAssets_skippedSilently() throws Exception {
        final Contentlet contentlet = createContentlet();
        final Bundle bundle = createBundleWithAssets(
                "duplicate-test", List.of(contentlet.getIdentifier()));

        final AddAssetsToBundleForm form = new AddAssetsToBundleForm(
                bundle.getId(), null, List.of(contentlet.getIdentifier()));

        final ResponseEntityAddAssetsToBundleView result =
                resource.addAssetsToBundle(mockAuthRequest(), mockResponse, form);

        final AddAssetsToBundleView view = result.getEntity();
        assertNotNull(view);
        assertEquals(bundle.getId(), view.bundleId());
        assertFalse("Should not create new bundle", view.created());
        assertEquals("Duplicate should be skipped, total=0", 0, view.total());
        assertTrue("No errors expected for duplicates", view.errors().isEmpty());
    }

    /**
     * Given: A bundle with one existing asset
     * When: Adding a mix of new and duplicate assets
     * Then: Only new assets are processed, duplicates skipped
     */
    @Test
    public void test_addAssets_mixOfNewAndDuplicate_onlyNewProcessed() throws Exception {
        final Contentlet existing = createContentlet();
        final Contentlet newContent = createContentlet();
        final Bundle bundle = createBundleWithAssets(
                "mix-test", List.of(existing.getIdentifier()));

        final AddAssetsToBundleForm form = new AddAssetsToBundleForm(
                bundle.getId(), null,
                List.of(existing.getIdentifier(), newContent.getIdentifier()));

        final ResponseEntityAddAssetsToBundleView result =
                resource.addAssetsToBundle(mockAuthRequest(), mockResponse, form);

        final AddAssetsToBundleView view = result.getEntity();
        assertNotNull(view);
        assertEquals("Only the new asset should be counted", 1, view.total());
        assertTrue("No errors expected", view.errors().isEmpty());
    }

    /**
     * Given: A bundle in BUNDLING status (in-progress)
     * When: Adding assets
     * Then: 409 ConflictException
     */
    @Test(expected = ConflictException.class)
    public void test_addAssets_bundleInProgress_returns409() throws Exception {
        final Bundle bundle = createBundleWithStatus("add-in-progress", Status.BUNDLING);
        final Contentlet contentlet = createContentlet();

        final AddAssetsToBundleForm form = new AddAssetsToBundleForm(
                bundle.getId(), null, List.of(contentlet.getIdentifier()));

        resource.addAssetsToBundle(mockAuthRequest(), mockResponse, form);
    }

    /**
     * Given: A non-existent bundleId with a valid bundleName
     * When: Adding assets
     * Then: Falls through to name lookup / auto-creation with bundleName
     */
    @Test
    public void test_addAssets_bundleIdNotFound_fallsThroughToName() throws Exception {
        final String bundleName = "fallthrough-" + System.currentTimeMillis();
        final Contentlet contentlet = createContentlet();

        final AddAssetsToBundleForm form = new AddAssetsToBundleForm(
                "non-existent-bundle-id", bundleName,
                List.of(contentlet.getIdentifier()));

        final ResponseEntityAddAssetsToBundleView result =
                resource.addAssetsToBundle(mockAuthRequest(), mockResponse, form);

        final AddAssetsToBundleView view = result.getEntity();
        assertNotNull(view);
        createdBundleIds.add(view.bundleId());
        assertTrue("Should have created a new bundle", view.created());
        assertEquals(bundleName, view.bundleName());
    }

    /**
     * Given: A non-existent bundleId with no bundleName
     * When: Adding assets
     * Then: 404 NotFoundException
     */
    @Test(expected = NotFoundException.class)
    public void test_addAssets_bundleIdNotFound_noBundleName_returns404() {
        final AddAssetsToBundleForm form = new AddAssetsToBundleForm(
                "non-existent-bundle-id", null,
                List.of("some-asset-id"));

        resource.addAssetsToBundle(mockAuthRequest(), mockResponse, form);
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
    // Bundle ownership: only the owner or a CMS Administrator changes a bundle
    // =========================================================================

    /**
     * Given: a bundle owned by one back-end user, with two queued assets
     * When: a different back-end user, who is not a CMS Administrator, removes both assets
     * Then: 403 for the whole request, and both assets stay in the bundle
     */
    @Test
    public void test_removeAssets_nonOwner_returns403AndKeepsAssets() throws Exception {
        final User owner = newBackendUser();
        final User otherUser = newBackendUser();
        final Contentlet contentlet = createContentlet();
        final Contentlet secondContentlet = createContentlet();
        final Bundle bundle = createBundleWithAssets("remove-non-owner", owner,
                List.of(contentlet.getIdentifier(), secondContentlet.getIdentifier()));

        final RemoveAssetsFromBundleForm form = RemoveAssetsFromBundleForm.builder()
                .assetIds(List.of(contentlet.getIdentifier(), secondContentlet.getIdentifier()))
                .build();

        try {
            resource.removeAssetsFromBundle(
                    mockAuthRequest(otherUser), mockResponse, bundle.getId(), form);
            fail("A user who does not own the bundle must get 403");
        } catch (final ForbiddenException e) {
            // expected
        }

        final List<String> queued = queuedAssets(bundle.getId());
        assertTrue(queued.contains(contentlet.getIdentifier()));
        assertTrue(queued.contains(secondContentlet.getIdentifier()));
    }

    /**
     * Given: a bundle owned by a back-end user who is not a CMS Administrator, with one queued asset
     * When: the owner removes the asset
     * Then: the asset is removed
     */
    @Test
    public void test_removeAssets_byOwner_removesSuccessfully() throws Exception {
        final User owner = newBackendUser();
        final Contentlet contentlet = createContentlet();
        final Bundle bundle = createBundleWithAssets(
                "remove-owner", owner, List.of(contentlet.getIdentifier()));

        final RemoveAssetsFromBundleForm form = RemoveAssetsFromBundleForm.builder()
                .assetIds(List.of(contentlet.getIdentifier()))
                .build();

        final List<RemoveAssetResultView> results = resource.removeAssetsFromBundle(
                mockAuthRequest(owner), mockResponse, bundle.getId(), form).getEntity();

        assertEquals(1, results.size());
        assertTrue("Asset removal should succeed", results.get(0).success());
        assertFalse(queuedAssets(bundle.getId()).contains(contentlet.getIdentifier()));
    }

    /**
     * Given: a bundle owned by a back-end user, with one queued asset
     * When: a CMS Administrator who does not own the bundle removes the asset
     * Then: the asset is removed
     */
    @Test
    public void test_removeAssets_adminOnAnotherUsersBundle_removesSuccessfully() throws Exception {
        final User owner = newBackendUser();
        final Contentlet contentlet = createContentlet();
        final Bundle bundle = createBundleWithAssets(
                "remove-admin", owner, List.of(contentlet.getIdentifier()));

        final RemoveAssetsFromBundleForm form = RemoveAssetsFromBundleForm.builder()
                .assetIds(List.of(contentlet.getIdentifier()))
                .build();

        final List<RemoveAssetResultView> results = resource.removeAssetsFromBundle(
                mockAuthRequest(adminUser), mockResponse, bundle.getId(), form).getEntity();

        assertEquals(1, results.size());
        assertTrue("Asset removal should succeed", results.get(0).success());
        assertFalse(queuedAssets(bundle.getId()).contains(contentlet.getIdentifier()));
    }

    /**
     * Given: a bundle owned by one back-end user that is being published
     * When: a different back-end user, who is not a CMS Administrator, removes an asset
     * Then: 403, not 409
     */
    @Test
    public void test_removeAssets_nonOwnerOnInProgressBundle_returns403() throws Exception {
        final User owner = newBackendUser();
        final User otherUser = newBackendUser();
        final Bundle bundle = createBundleWithStatus("remove-non-owner-in-progress", owner, Status.PUBLISHING_BUNDLE);

        final RemoveAssetsFromBundleForm form = RemoveAssetsFromBundleForm.builder()
                .assetIds(List.of("some-asset"))
                .build();

        try {
            resource.removeAssetsFromBundle(
                    mockAuthRequest(otherUser), mockResponse, bundle.getId(), form);
            fail("A user who does not own the bundle must get 403");
        } catch (final ForbiddenException e) {
            // expected
        }
    }

    /**
     * Given: a bundle owned by one back-end user
     * When: a different back-end user, who is not a CMS Administrator, adds an asset to it by id
     * Then: 403, and nothing is added
     */
    @Test
    public void test_addAssets_nonOwnerBundleId_returns403AndAddsNothing() throws Exception {
        final User owner = newBackendUser();
        final User otherUser = newBackendUser();
        final Bundle bundle = createBundle("add-non-owner", owner);
        final Contentlet contentlet = createContentlet();

        final AddAssetsToBundleForm form = new AddAssetsToBundleForm(
                bundle.getId(), null, List.of(contentlet.getIdentifier()));

        try {
            resource.addAssetsToBundle(mockAuthRequest(otherUser), mockResponse, form);
            fail("A user who does not own the bundle must get 403");
        } catch (final ForbiddenException e) {
            // expected
        }

        assertTrue(queuedAssets(bundle.getId()).isEmpty());
    }

    /**
     * Given: a bundle owned by one back-end user
     * When: a different back-end user, who is not a CMS Administrator, sends its id plus a bundle name
     * Then: 403; no bundle is created under that name and nothing is added
     */
    @Test
    public void test_addAssets_nonOwnerBundleIdWithName_returns403() throws Exception {
        final User owner = newBackendUser();
        final User otherUser = newBackendUser();
        final Bundle bundle = createBundle("add-non-owner-named", owner);
        final Contentlet contentlet = createContentlet();
        final String bundleName = "add-non-owner-fallback-" + System.currentTimeMillis();

        final AddAssetsToBundleForm form = new AddAssetsToBundleForm(
                bundle.getId(), bundleName, List.of(contentlet.getIdentifier()));

        try {
            final AddAssetsToBundleView view = resource.addAssetsToBundle(
                    mockAuthRequest(otherUser), mockResponse, form).getEntity();
            createdBundleIds.add(view.bundleId());
            fail("A user who does not own the bundle must get 403");
        } catch (final ForbiddenException e) {
            // expected
        }

        assertTrue(queuedAssets(bundle.getId()).isEmpty());
        assertTrue(bundleAPI.getUnsendBundlesByName(otherUser.getUserId(), bundleName, 10, 0).isEmpty());
    }

    /**
     * Given: a bundle owned by one back-end user that is being published
     * When: a different back-end user, who is not a CMS Administrator, adds an asset to it by id
     * Then: 403, not 409, and nothing is added
     */
    @Test
    public void test_addAssets_nonOwnerOnInProgressBundle_returns403() throws Exception {
        final User owner = newBackendUser();
        final User otherUser = newBackendUser();
        final Bundle bundle = createBundleWithStatus("add-non-owner-in-progress", owner, Status.PUBLISHING_BUNDLE);
        final Contentlet contentlet = createContentlet();

        final AddAssetsToBundleForm form = new AddAssetsToBundleForm(
                bundle.getId(), null, List.of(contentlet.getIdentifier()));

        try {
            resource.addAssetsToBundle(mockAuthRequest(otherUser), mockResponse, form);
            fail("A user who does not own the bundle must get 403");
        } catch (final ForbiddenException e) {
            // expected
        }

        assertTrue(queuedAssets(bundle.getId()).isEmpty());
    }

    /**
     * Given: a bundle owned by a back-end user who is not a CMS Administrator, and an asset the
     * owner may publish
     * When: the owner adds the asset to the bundle by id
     * Then: the asset is added to the owner's bundle
     */
    @Test
    public void test_addAssets_byOwnerBundleId_addsAsset() throws Exception {
        final User owner = newBackendUser();
        final Bundle bundle = createBundle("add-owner", owner);
        final Contentlet contentlet = createContentlet();
        grantPublish(contentlet, owner);

        final AddAssetsToBundleForm form = new AddAssetsToBundleForm(
                bundle.getId(), null, List.of(contentlet.getIdentifier()));

        final AddAssetsToBundleView view = resource.addAssetsToBundle(
                mockAuthRequest(owner), mockResponse, form).getEntity();

        assertEquals(bundle.getId(), view.bundleId());
        assertFalse("Should not create new bundle", view.created());
        assertTrue("No asset should be refused: " + view.errors(), view.errors().isEmpty());
        assertTrue(queuedAssets(bundle.getId()).contains(contentlet.getIdentifier()));
    }

    /**
     * Given: a bundle owned by a back-end user
     * When: a CMS Administrator who does not own the bundle adds an asset to it by id
     * Then: the asset is added to that bundle
     */
    @Test
    public void test_addAssets_adminOnAnotherUsersBundleId_addsAsset() throws Exception {
        final User owner = newBackendUser();
        final Bundle bundle = createBundle("add-admin", owner);
        final Contentlet contentlet = createContentlet();

        final AddAssetsToBundleForm form = new AddAssetsToBundleForm(
                bundle.getId(), null, List.of(contentlet.getIdentifier()));

        final AddAssetsToBundleView view = resource.addAssetsToBundle(
                mockAuthRequest(adminUser), mockResponse, form).getEntity();

        assertEquals(bundle.getId(), view.bundleId());
        assertFalse("Should not create new bundle", view.created());
        assertTrue(queuedAssets(bundle.getId()).contains(contentlet.getIdentifier()));
    }

    // =========================================================================
    // Helper Methods
    // =========================================================================

    private HttpServletRequest mockAuthRequest() {
        return mockAuthRequest(adminUser);
    }

    private HttpServletRequest mockAuthRequest(final User user) {
        final MockHeaderRequest request = new MockHeaderRequest(
                new MockSessionRequest(
                        new MockAttributeRequest(
                                new MockHttpRequestIntegrationTest(
                                        "localhost", "/api/v1/bundles")
                                        .request())
                                .request())
                        .request());
        request.setAttribute(WebKeys.USER, user);
        return request;
    }

    private Bundle createBundle(final String name) throws DotDataException {
        final Bundle bundle = new Bundle(name, null, null, adminUser.getUserId());
        bundleAPI.saveBundle(bundle);
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

    private void grantPublish(final Contentlet contentlet, final User user) throws Exception {
        final Permission permission = new Permission(contentlet.getPermissionId(),
                APILocator.getRoleAPI().getUserRole(user).getId(),
                PermissionAPI.PERMISSION_READ | PermissionAPI.PERMISSION_EDIT
                        | PermissionAPI.PERMISSION_PUBLISH, true);
        APILocator.getPermissionAPI().save(permission, contentlet, APILocator.systemUser(), false);
    }

    private User newBackendUser() throws DotDataException {
        return new UserDataGen().roles(TestUserUtils.getBackendRole()).nextPersisted();
    }

    private Bundle createBundle(final String name, final User owner) throws DotDataException {
        final Bundle bundle = new Bundle(name, null, null, owner.getUserId());
        bundleAPI.saveBundle(bundle);
        createdBundleIds.add(bundle.getId());
        return bundle;
    }

    private Bundle createBundleWithAssets(final String name, final User owner,
            final List<String> assetIds) throws Exception {
        final Bundle bundle = createBundle(name, owner);
        publisherQueueAPI.saveBundleAssets(assetIds, bundle.getId(), adminUser);
        return bundle;
    }

    private Bundle createBundleWithStatus(final String name, final User owner, final Status status)
            throws Exception {
        final Bundle bundle = createBundle(name, owner);

        final PublishAuditStatus auditStatus = new PublishAuditStatus(bundle.getId());
        auditStatus.setStatus(status);
        auditStatus.setStatusUpdated(new Date());

        final PublishAuditHistory history = new PublishAuditHistory();
        auditStatus.setStatusPojo(history);

        publishAuditAPI.insertPublishAuditStatus(auditStatus);
        publishAuditAPI.updatePublishAuditStatus(bundle.getId(), status, history);

        return bundle;
    }

    private List<String> queuedAssets(final String bundleId) throws Exception {
        final List<String> assets = new ArrayList<>();
        for (final PublishQueueElement element : publisherQueueAPI.getQueueElementsByBundleId(bundleId)) {
            assets.add(element.getAsset());
        }
        return assets;
    }

}
