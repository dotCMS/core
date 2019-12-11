package com.dotcms.publisher.bundle.business;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.dotcms.contenttype.exception.NotFoundInDbException;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.publisher.bundle.bean.Bundle;
import com.dotcms.publisher.business.DotPublisherException;
import com.dotcms.publisher.business.PublishAuditAPI;
import com.dotcms.publisher.business.PublishAuditHistory;
import com.dotcms.publisher.business.PublishAuditStatus;
import com.dotcms.publisher.business.PublishAuditStatus.Status;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.DateUtil;
import com.dotmarketing.util.UUIDGenerator;
import com.liferay.portal.model.User;
import java.util.Calendar;
import java.util.Date;
import java.util.Set;
import org.junit.BeforeClass;
import org.junit.Test;

public class BundleAPITest {

    private static User adminUser;
    private static BundleAPI bundleAPI;

    @BeforeClass
    public static void prepare() throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
        adminUser = APILocator.systemUser();
        bundleAPI = APILocator.getBundleAPI();
    }

    private String insertPublishingBundle(final String userId, final Date publishDate)
            throws DotDataException {
        final String uuid = UUIDGenerator.generateUuid();
        final Bundle bundle = new Bundle();
        bundle.setId(uuid);
        bundle.setName("testBundle"+System.currentTimeMillis());
        bundle.setForcePush(false);
        bundle.setOwner(userId);
        bundle.setPublishDate(publishDate);
        APILocator.getBundleAPI().saveBundle(bundle);

        return uuid;
    }

    private void insertPublishAuditStatus(final Status status, final String bundleID) throws DotPublisherException {
        final PublishAuditStatus publishAuditStatus = new PublishAuditStatus(bundleID);
        publishAuditStatus.setStatusPojo(new PublishAuditHistory());
        publishAuditStatus.setStatus(status);
        APILocator.getPublishAuditAPI().insertPublishAuditStatus(publishAuditStatus);
    }

    @Test
    public void test_deleteBundleAndDependencies_byAdmin()
            throws DotDataException, DotPublisherException {
        final User newUser = new UserDataGen().nextPersisted();
        final String bundleIdAdmin = insertPublishingBundle(adminUser.getUserId(),new Date());
        insertPublishAuditStatus(Status.FAILED_TO_BUNDLE,bundleIdAdmin);
        final String bundleIdUser = insertPublishingBundle(newUser.getUserId(),new Date());
        insertPublishAuditStatus(Status.FAILED_TO_BUNDLE,bundleIdUser);

        assertNotNull(bundleAPI.getBundleById(bundleIdAdmin));
        assertNotNull(bundleAPI.getBundleById(bundleIdUser));

        bundleAPI.deleteBundleAndDependencies(bundleIdAdmin,adminUser);
        bundleAPI.deleteBundleAndDependencies(bundleIdUser,adminUser);

        assertNull(bundleAPI.getBundleById(bundleIdAdmin));
        assertNull(bundleAPI.getBundleById(bundleIdUser));
    }

    @Test(expected = DotDataException.class)
    public void test_deleteBundleAndDependencies_byLimitedUser() throws DotDataException {
        final User newUser = new UserDataGen().nextPersisted();
        final String bundleIdAdmin = insertPublishingBundle(adminUser.getUserId(),new Date());
        final String bundleIdUser = insertPublishingBundle(newUser.getUserId(),new Date());

        assertNotNull(bundleAPI.getBundleById(bundleIdAdmin));
        assertNotNull(bundleAPI.getBundleById(bundleIdUser));

        bundleAPI.deleteBundleAndDependencies(bundleIdUser,newUser);
        assertNull(bundleAPI.getBundleById(bundleIdUser));

        bundleAPI.deleteBundleAndDependencies(bundleIdAdmin,newUser);
        assertNotNull(bundleAPI.getBundleById(bundleIdAdmin));
    }

    @Test(expected = NotFoundInDbException.class)
    public void test_deleteBundleAndDependencies_bundleIdDoesNotExist() throws DotDataException {
        bundleAPI.deleteBundleAndDependencies(UUIDGenerator.generateUuid(),adminUser);
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_deleteBundleAndDependenciesOlderThan_futureDate() throws DotDataException {
        bundleAPI.deleteBundleAndDependenciesOlderThan(DateUtil.addDate(new Date(),
                Calendar.MONTH,1),adminUser);
    }

    @Test
    public void test_deleteBundleAndDependenciesOlderThan_byAdmin() throws DotDataException {
        final User newUser = new UserDataGen().nextPersisted();
        final String bundleIdAdmin = insertPublishingBundle(adminUser.getUserId(),new Date());
        final String bundleIdUser = insertPublishingBundle(newUser.getUserId(),new Date());
        final String bundleIdAdmin_futureDate = insertPublishingBundle(adminUser.getUserId(),DateUtil.addDate(new Date(),
                Calendar.MONTH,1));

        assertNotNull(bundleAPI.getBundleById(bundleIdAdmin));
        assertNotNull(bundleAPI.getBundleById(bundleIdUser));
        assertNotNull(bundleAPI.getBundleById(bundleIdAdmin_futureDate));

        final Set<String> bundlesDeleted = bundleAPI.deleteBundleAndDependenciesOlderThan(new Date(),adminUser).getDeleteBundleSet();
        assertTrue(bundlesDeleted.contains(bundleIdAdmin));
        assertTrue(bundlesDeleted.contains(bundleIdUser));
        assertFalse(bundlesDeleted.contains(bundleIdAdmin_futureDate));

        assertNull(bundleAPI.getBundleById(bundleIdAdmin));
        assertNull(bundleAPI.getBundleById(bundleIdUser));
        assertNotNull(bundleAPI.getBundleById(bundleIdAdmin_futureDate));
    }

    @Test
    public void test_deleteBundleAndDependenciesOlderThan_byLimitedUser() throws DotDataException {
        final User newUser = new UserDataGen().nextPersisted();
        final String bundleIdAdmin = insertPublishingBundle(adminUser.getUserId(),new Date());
        final String bundleIdUser = insertPublishingBundle(newUser.getUserId(),new Date());
        final String bundleIdUser_futureDate = insertPublishingBundle(newUser.getUserId(),DateUtil.addDate(new Date(),
                Calendar.MONTH,1));

        assertNotNull(bundleAPI.getBundleById(bundleIdAdmin));
        assertNotNull(bundleAPI.getBundleById(bundleIdUser));
        assertNotNull(bundleAPI.getBundleById(bundleIdUser_futureDate));

        final Set<String> bundlesDeleted = bundleAPI.deleteBundleAndDependenciesOlderThan(new Date(),newUser).getDeleteBundleSet();
        assertTrue(bundlesDeleted.contains(bundleIdUser));
        assertFalse(bundlesDeleted.contains(bundleIdAdmin));
        assertFalse(bundlesDeleted.contains(bundleIdUser_futureDate));

        assertNull(bundleAPI.getBundleById(bundleIdUser));
        assertNotNull(bundleAPI.getBundleById(bundleIdAdmin));
        assertNotNull(bundleAPI.getBundleById(bundleIdUser_futureDate));
    }

    @Test
    public void test_deleteAllBundles_byAdmin() throws DotDataException, DotPublisherException {
        final User newUser = new UserDataGen().nextPersisted();
        final String bundleIdAdmin_success = insertPublishingBundle(adminUser.getUserId(),new Date());
        insertPublishAuditStatus(Status.SUCCESS,bundleIdAdmin_success);
        final String bundleIdUser_success = insertPublishingBundle(newUser.getUserId(),new Date());
        insertPublishAuditStatus(Status.SUCCESS,bundleIdUser_success);
        final String bundleIdAdmin_failedPublish = insertPublishingBundle(adminUser.getUserId(),new Date());
        insertPublishAuditStatus(Status.FAILED_TO_PUBLISH,bundleIdAdmin_failedPublish);
        final String bundleIdUser_failedBundle = insertPublishingBundle(newUser.getUserId(),new Date());
        insertPublishAuditStatus(Status.FAILED_TO_BUNDLE,bundleIdUser_failedBundle);

        assertNotNull(bundleAPI.getBundleById(bundleIdAdmin_success));
        assertNotNull(bundleAPI.getBundleById(bundleIdUser_success));
        assertNotNull(bundleAPI.getBundleById(bundleIdAdmin_failedPublish));
        assertNotNull(bundleAPI.getBundleById(bundleIdUser_failedBundle));

        final Set<String> bundlesDeleted = bundleAPI.deleteAllBundles(adminUser,Status.SUCCESS,Status.FAILED_TO_PUBLISH).getDeleteBundleSet();
        assertTrue(bundlesDeleted.contains(bundleIdAdmin_success));
        assertTrue(bundlesDeleted.contains(bundleIdUser_success));
        assertTrue(bundlesDeleted.contains(bundleIdAdmin_failedPublish));
        assertFalse(bundlesDeleted.contains(bundleIdUser_failedBundle));

        assertNull(bundleAPI.getBundleById(bundleIdAdmin_success));
        assertNull(bundleAPI.getBundleById(bundleIdUser_success));
        assertNull(bundleAPI.getBundleById(bundleIdAdmin_failedPublish));
        assertNotNull(bundleAPI.getBundleById(bundleIdUser_failedBundle));
    }

    @Test
    public void test_deleteAllBundles_byLimitedUser() throws DotDataException, DotPublisherException {
        final User newUser = new UserDataGen().nextPersisted();
        final String bundleIdAdmin_success = insertPublishingBundle(adminUser.getUserId(),new Date());
        insertPublishAuditStatus(Status.SUCCESS,bundleIdAdmin_success);
        final String bundleIdUser_success = insertPublishingBundle(newUser.getUserId(),new Date());
        insertPublishAuditStatus(Status.SUCCESS,bundleIdUser_success);
        final String bundleIdAdmin_failedPublish = insertPublishingBundle(adminUser.getUserId(),new Date());
        insertPublishAuditStatus(Status.FAILED_TO_PUBLISH,bundleIdAdmin_failedPublish);
        final String bundleIdUser_failedBundle = insertPublishingBundle(newUser.getUserId(),new Date());
        insertPublishAuditStatus(Status.FAILED_TO_BUNDLE,bundleIdUser_failedBundle);

        assertNotNull(bundleAPI.getBundleById(bundleIdAdmin_success));
        assertNotNull(bundleAPI.getBundleById(bundleIdUser_success));
        assertNotNull(bundleAPI.getBundleById(bundleIdAdmin_failedPublish));
        assertNotNull(bundleAPI.getBundleById(bundleIdUser_failedBundle));

        final Set<String> bundlesDeleted = bundleAPI.deleteAllBundles(newUser,Status.FAILED_TO_BUNDLE).getDeleteBundleSet();
        assertTrue(bundlesDeleted.contains(bundleIdUser_failedBundle));
        assertFalse(bundlesDeleted.contains(bundleIdAdmin_failedPublish));
        assertFalse(bundlesDeleted.contains(bundleIdAdmin_success));
        assertFalse(bundlesDeleted.contains(bundleIdUser_success));

        assertNotNull(bundleAPI.getBundleById(bundleIdAdmin_success));
        assertNotNull(bundleAPI.getBundleById(bundleIdUser_success));
        assertNotNull(bundleAPI.getBundleById(bundleIdAdmin_failedPublish));
        assertNull(bundleAPI.getBundleById(bundleIdUser_failedBundle));
    }
    /**
     * This test is for deleting a bundle by id, when that bundle was uploaded.
     * Since it was uploaded only lives on the publishing_queue_audit table.
     * @throws DotDataException
     * @throws DotPublisherException
     */
    @Test
    public void test_deleteBundleById_uploadedBundle() throws DotDataException, DotPublisherException {
        final String bundleId = UUIDGenerator.generateUuid();
        insertPublishAuditStatus(Status.SUCCESS,bundleId);
        final PublishAuditAPI publishAuditStatus = APILocator.getPublishAuditAPI();
        assertNotNull(publishAuditStatus.getPublishAuditStatus(bundleId));

        bundleAPI.deleteBundleAndDependencies(bundleId,adminUser);
        assertNull(publishAuditStatus.getPublishAuditStatus(bundleId));
    }
}
