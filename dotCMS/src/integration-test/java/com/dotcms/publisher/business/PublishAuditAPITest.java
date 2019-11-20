package com.dotcms.publisher.business;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.dotcms.datagen.UserDataGen;
import com.dotcms.publisher.bundle.bean.Bundle;
import com.dotcms.publisher.business.PublishAuditStatus.Status;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.UUIDGenerator;
import com.liferay.portal.model.User;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class PublishAuditAPITest {

    private static PublishAuditAPI publishAuditAPI;

    @BeforeClass
    public static void prepare() throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
        publishAuditAPI = APILocator.getPublishAuditAPI();
    }

    @Test
    public void test_insertPublishAuditStatus() throws DotPublisherException {
        final String bundleID = UUIDGenerator.generateUuid();
        final PublishAuditStatus publishAuditStatus = new PublishAuditStatus(bundleID);
        publishAuditStatus.setStatusPojo(new PublishAuditHistory());
        publishAuditAPI.insertPublishAuditStatus(publishAuditStatus);

        PublishAuditStatus publishAuditStatus1 = publishAuditAPI.getPublishAuditStatus(bundleID);
        assertNotNull(publishAuditStatus1.getStatus());
        assertNotNull(publishAuditStatus1.getCreateDate());
        assertEquals(bundleID,publishAuditStatus.getBundleId());
    }

    @Test
    public void test_getBundleIdByStatus_StatusSuccess()
            throws DotPublisherException, DotDataException {
        final String bundleID_Success = insertPublishAuditStatus(Status.SUCCESS,UUIDGenerator.generateUuid());
        final String bundleID_FailedToPublish = insertPublishAuditStatus(Status.FAILED_TO_PUBLISH,UUIDGenerator.generateUuid());

        final List<String> listOfBundleIDS = publishAuditAPI.getBundleIdByStatus(new ArrayList<>(
                Collections.singleton(Status.SUCCESS)),100,0);

        assertFalse(listOfBundleIDS.isEmpty());
        assertTrue(listOfBundleIDS.contains(bundleID_Success));
        assertFalse(listOfBundleIDS.contains(bundleID_FailedToPublish));
    }

    @Test
    public void test_getBundleIdByStatus_StatusFailAllTypes()
            throws DotPublisherException, DotDataException {
        final String bundleID_Success = insertPublishAuditStatus(Status.SUCCESS,UUIDGenerator.generateUuid());
        final String bundleID_FailedToPublish = insertPublishAuditStatus(Status.FAILED_TO_PUBLISH,UUIDGenerator.generateUuid());
        final String bundleID_PublishingBundle = insertPublishAuditStatus(Status.PUBLISHING_BUNDLE,UUIDGenerator.generateUuid());
        final String bundleID_FailedToPublish2 = insertPublishAuditStatus(Status.FAILED_TO_PUBLISH,UUIDGenerator.generateUuid());
        final String bundleID_FailedToSendALL = insertPublishAuditStatus(Status.FAILED_TO_SEND_TO_ALL_GROUPS,UUIDGenerator.generateUuid());
        final String bundleID_FailedToSendSome = insertPublishAuditStatus(Status.FAILED_TO_SEND_TO_SOME_GROUPS,UUIDGenerator.generateUuid());
        final String bundleID_FailedToBundle = insertPublishAuditStatus(Status.FAILED_TO_BUNDLE,UUIDGenerator.generateUuid());
        final String bundleID_FailedToSent = insertPublishAuditStatus(Status.FAILED_TO_SENT,UUIDGenerator.generateUuid());


        final List<String> listOfBundleIDS = publishAuditAPI.getBundleIdByStatus(new ArrayList<>(
                Arrays.asList(Status.FAILED_TO_BUNDLE, Status.FAILED_TO_PUBLISH,
                        Status.FAILED_TO_SEND_TO_ALL_GROUPS,
                        Status.FAILED_TO_SEND_TO_SOME_GROUPS, Status.FAILED_TO_SENT)),100,0);

        assertFalse(listOfBundleIDS.isEmpty());
        assertFalse(listOfBundleIDS.contains(bundleID_Success));
        assertFalse(listOfBundleIDS.contains(bundleID_PublishingBundle));
        assertTrue(listOfBundleIDS.contains(bundleID_FailedToPublish));
        assertTrue(listOfBundleIDS.contains(bundleID_FailedToPublish2));
        assertTrue(listOfBundleIDS.contains(bundleID_FailedToSendALL));
        assertTrue(listOfBundleIDS.contains(bundleID_FailedToSendSome));
        assertTrue(listOfBundleIDS.contains(bundleID_FailedToBundle));
        assertTrue(listOfBundleIDS.contains(bundleID_FailedToSent));
    }

    @Test
    public void test_getBundleIdByStatusFilterByOwner_StatusSuccess_OwnerNewUser()
            throws DotDataException, DotPublisherException {
        final User newUser = new UserDataGen().nextPersisted();
        final String newUserId = newUser.getUserId();
        final String adminUserId = APILocator.systemUser().getUserId();

        final String bundleID_Success_Owned_Admin = insertPublishingBundle(adminUserId,UUIDGenerator.generateUuid(),Status.SUCCESS);
        final String bundleID_FailToPublish_Owned_Admin = insertPublishingBundle(adminUserId,UUIDGenerator.generateUuid(),Status.FAILED_TO_PUBLISH);
        final String bundleID_Success_Owned_User = insertPublishingBundle(newUserId,UUIDGenerator.generateUuid(),Status.SUCCESS);
        final String bundleID_FailToPublish_Owned_User = insertPublishingBundle(newUserId,UUIDGenerator.generateUuid(),Status.FAILED_TO_PUBLISH);

        final List<String> listOfBundleIDS = publishAuditAPI.getBundleIdByStatusFilterByOwner(new ArrayList<>(Arrays.asList(Status.SUCCESS)),
                100,0,newUserId);

        assertFalse(listOfBundleIDS.isEmpty());
        assertFalse(listOfBundleIDS.contains(bundleID_FailToPublish_Owned_Admin));
        assertFalse(listOfBundleIDS.contains(bundleID_Success_Owned_Admin));
        assertFalse(listOfBundleIDS.contains(bundleID_FailToPublish_Owned_User));
        assertTrue(listOfBundleIDS.contains(bundleID_Success_Owned_User));
    }

    @Test
    public void test_getBundleIdByStatusFilterByOwner_StatusFailAllTypes_OwnerNewUser()
            throws DotDataException, DotPublisherException {
        final User newUser = new UserDataGen().nextPersisted();
        final String newUserId = newUser.getUserId();
        final String adminUserId = APILocator.systemUser().getUserId();

        final String bundleID_Success_Owned_Admin = insertPublishingBundle(adminUserId,UUIDGenerator.generateUuid(),Status.SUCCESS);
        final String bundleID_FailToPublish_Owned_Admin = insertPublishingBundle(adminUserId,UUIDGenerator.generateUuid(),Status.FAILED_TO_PUBLISH);
        final String bundleID_Success_Owned_User = insertPublishingBundle(newUserId,UUIDGenerator.generateUuid(),Status.SUCCESS);
        final String bundleID_FailToPublish_Owned_User = insertPublishingBundle(newUserId,UUIDGenerator.generateUuid(),Status.FAILED_TO_PUBLISH);
        final String bundleID_FailToBundle_Owned_User = insertPublishingBundle(newUserId,UUIDGenerator.generateUuid(),Status.FAILED_TO_BUNDLE);
        final String bundleID_FailToSendSome_Owned_User = insertPublishingBundle(newUserId,UUIDGenerator.generateUuid(),Status.FAILED_TO_SEND_TO_SOME_GROUPS);
        final String bundleID_FailToSendAll_Owned_Admin = insertPublishingBundle(adminUserId,UUIDGenerator.generateUuid(),Status.FAILED_TO_SEND_TO_ALL_GROUPS);
        final String bundleID_FailToSent_Owned_Admin = insertPublishingBundle(adminUserId,UUIDGenerator.generateUuid(),Status.FAILED_TO_SENT);


        final List<String> listOfBundleIDS = publishAuditAPI.getBundleIdByStatusFilterByOwner(new ArrayList<>(
                        Arrays.asList(Status.FAILED_TO_BUNDLE, Status.FAILED_TO_PUBLISH,
                                Status.FAILED_TO_SEND_TO_ALL_GROUPS,
                                Status.FAILED_TO_SEND_TO_SOME_GROUPS, Status.FAILED_TO_SENT)), 100,0,newUserId);

        assertFalse(listOfBundleIDS.isEmpty());
        assertFalse(listOfBundleIDS.contains(bundleID_FailToPublish_Owned_Admin));
        assertFalse(listOfBundleIDS.contains(bundleID_Success_Owned_Admin));
        assertFalse(listOfBundleIDS.contains(bundleID_Success_Owned_User));
        assertFalse(listOfBundleIDS.contains(bundleID_FailToSendAll_Owned_Admin));
        assertFalse(listOfBundleIDS.contains(bundleID_FailToSent_Owned_Admin));
        assertTrue(listOfBundleIDS.contains(bundleID_FailToBundle_Owned_User));
        assertTrue(listOfBundleIDS.contains(bundleID_FailToPublish_Owned_User));
        assertTrue(listOfBundleIDS.contains(bundleID_FailToSendSome_Owned_User));
    }

    private String insertPublishAuditStatus(final Status status, final String bundleID) throws DotPublisherException {
        final PublishAuditStatus publishAuditStatus = new PublishAuditStatus(bundleID);
        publishAuditStatus.setStatusPojo(new PublishAuditHistory());
        publishAuditStatus.setStatus(status);
        publishAuditAPI.insertPublishAuditStatus(publishAuditStatus);

        return bundleID;
    }

    private String insertPublishingBundle(final String userId, final String bundleId, final Status status)
            throws DotDataException, DotPublisherException {
        final Bundle bundle = new Bundle();
        bundle.setId(bundleId);
        bundle.setName("testBundle"+System.currentTimeMillis());
        bundle.setForcePush(false);
        bundle.setOwner(userId);
        APILocator.getBundleAPI().saveBundle(bundle);

        insertPublishAuditStatus(status,bundleId);

        return bundleId;
    }

    @AfterClass
    public static void cleanup() throws DotDataException {
        APILocator.getBundleAPI().deleteAllBundles(APILocator.systemUser());
    }
}
