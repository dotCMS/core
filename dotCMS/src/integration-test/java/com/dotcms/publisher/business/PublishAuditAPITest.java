package com.dotcms.publisher.business;

import static com.dotcms.util.CollectionsUtils.list;
import static com.dotcms.util.CollectionsUtils.map;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.BundleDataGen;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.publisher.bundle.bean.Bundle;
import com.dotcms.publisher.business.PublishAuditStatus.Status;
import com.dotcms.publisher.util.PusheableAsset;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.UUIDGenerator;
import com.liferay.portal.model.User;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;
import org.junit.BeforeClass;
import org.junit.Test;

public class PublishAuditAPITest {

    private static PublishAuditAPI publishAuditAPI;
    private static User adminUser;

    @BeforeClass
    public static void prepare() throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
        publishAuditAPI = APILocator.getPublishAuditAPI();
        adminUser = APILocator.systemUser();
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

        publishAuditAPI.deletePublishAuditStatus(bundleID);
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

        publishAuditAPI.deletePublishAuditStatus(listOfBundleIDS);
        publishAuditAPI.deletePublishAuditStatus(bundleID_FailedToPublish);
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

        publishAuditAPI.deletePublishAuditStatus(listOfBundleIDS);
        publishAuditAPI.deletePublishAuditStatus(bundleID_Success);
        publishAuditAPI.deletePublishAuditStatus(bundleID_PublishingBundle);
    }

    @Test
    public void test_getBundleIdByStatusFilterByOwner_StatusSuccess_OwnerNewUser()
            throws DotDataException, DotPublisherException {
        final User newUser = new UserDataGen().nextPersisted();
        final String newUserId = newUser.getUserId();
        final String adminUserId = adminUser.getUserId();
        final List<String> allBundleIdsList = new ArrayList<>();

        final String bundleID_Success_Owned_Admin = insertPublishingBundle(adminUserId,UUIDGenerator.generateUuid(),Status.SUCCESS);
        allBundleIdsList.add(bundleID_Success_Owned_Admin);
        final String bundleID_FailToPublish_Owned_Admin = insertPublishingBundle(adminUserId,UUIDGenerator.generateUuid(),Status.FAILED_TO_PUBLISH);
        allBundleIdsList.add(bundleID_FailToPublish_Owned_Admin);
        final String bundleID_Success_Owned_User = insertPublishingBundle(newUserId,UUIDGenerator.generateUuid(),Status.SUCCESS);
        allBundleIdsList.add(bundleID_Success_Owned_User);
        final String bundleID_FailToPublish_Owned_User = insertPublishingBundle(newUserId,UUIDGenerator.generateUuid(),Status.FAILED_TO_PUBLISH);
        allBundleIdsList.add(bundleID_FailToPublish_Owned_User);

        final List<String> listOfBundleIDS = publishAuditAPI.getBundleIdByStatusFilterByOwner(new ArrayList<>(Arrays.asList(Status.SUCCESS)),
                100,0,newUserId);

        assertFalse(listOfBundleIDS.isEmpty());
        assertFalse(listOfBundleIDS.contains(bundleID_FailToPublish_Owned_Admin));
        assertFalse(listOfBundleIDS.contains(bundleID_Success_Owned_Admin));
        assertFalse(listOfBundleIDS.contains(bundleID_FailToPublish_Owned_User));
        assertTrue(listOfBundleIDS.contains(bundleID_Success_Owned_User));

        deletePublishingBundle(allBundleIdsList);
    }

    @Test
    public void test_getBundleIdByStatusFilterByOwner_StatusFailAllTypes_OwnerNewUser()
            throws DotDataException, DotPublisherException {
        final User newUser = new UserDataGen().nextPersisted();
        final String newUserId = newUser.getUserId();
        final String adminUserId = adminUser.getUserId();
        final List<String> allBundleIdsList = new ArrayList<>();

        final String bundleID_Success_Owned_Admin = insertPublishingBundle(adminUserId,UUIDGenerator.generateUuid(),Status.SUCCESS);
        allBundleIdsList.add(bundleID_Success_Owned_Admin);
        final String bundleID_FailToPublish_Owned_Admin = insertPublishingBundle(adminUserId,UUIDGenerator.generateUuid(),Status.FAILED_TO_PUBLISH);
        allBundleIdsList.add(bundleID_FailToPublish_Owned_Admin);
        final String bundleID_Success_Owned_User = insertPublishingBundle(newUserId,UUIDGenerator.generateUuid(),Status.SUCCESS);
        allBundleIdsList.add(bundleID_Success_Owned_User);
        final String bundleID_FailToPublish_Owned_User = insertPublishingBundle(newUserId,UUIDGenerator.generateUuid(),Status.FAILED_TO_PUBLISH);
        allBundleIdsList.add(bundleID_FailToPublish_Owned_User);
        final String bundleID_FailToBundle_Owned_User = insertPublishingBundle(newUserId,UUIDGenerator.generateUuid(),Status.FAILED_TO_BUNDLE);
        allBundleIdsList.add(bundleID_FailToBundle_Owned_User);
        final String bundleID_FailToSendSome_Owned_User = insertPublishingBundle(newUserId,UUIDGenerator.generateUuid(),Status.FAILED_TO_SEND_TO_SOME_GROUPS);
        allBundleIdsList.add(bundleID_FailToSendSome_Owned_User);
        final String bundleID_FailToSendAll_Owned_Admin = insertPublishingBundle(adminUserId,UUIDGenerator.generateUuid(),Status.FAILED_TO_SEND_TO_ALL_GROUPS);
        allBundleIdsList.add(bundleID_FailToSendAll_Owned_Admin);
        final String bundleID_FailToSent_Owned_Admin = insertPublishingBundle(adminUserId,UUIDGenerator.generateUuid(),Status.FAILED_TO_SENT);
        allBundleIdsList.add(bundleID_FailToSent_Owned_Admin);


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

        deletePublishingBundle(allBundleIdsList);
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

    private void deletePublishingBundle(final List<String> listOfIds) throws DotDataException {
        for(final String bundleId : listOfIds){
            APILocator.getBundleAPI().deleteBundleAndDependencies(bundleId,adminUser);
        }
    }

    /**
     * Method to test: {@link PublishAuditAPI#getPublishAuditStatus(String, int)}
     * When: Create a {@link PublishAuditStatus} with 5 assets and call the methods with assetsLimit equals to -1
     * Should: return all the assets
     *
     * @throws DotPublisherException
     */
    @Test
    public void maxAsssetLimits() throws DotPublisherException {
        final ContentType contentType = new ContentTypeDataGen().nextPersisted();
        final Contentlet contentlet_1 = new ContentletDataGen(contentType).nextPersisted();
        final Contentlet contentlet_2 = new ContentletDataGen(contentType).nextPersisted();
        final Contentlet contentlet_3 = new ContentletDataGen(contentType).nextPersisted();
        final Contentlet contentlet_4 = new ContentletDataGen(contentType).nextPersisted();

        final Bundle bundle = new BundleDataGen()
                .addAssets(
                        list(contentType, contentlet_1, contentlet_2, contentlet_3, contentlet_4))
                .nextPersisted();

        final PublishAuditStatus publishAuditStatus = new PublishAuditStatus(bundle.getId());

        final PublishAuditHistory publishAuditHistory = new PublishAuditHistory();
        publishAuditHistory.setAssets(map(
                contentType.id(), PusheableAsset.CONTENT_TYPE.toString(),
                contentlet_1.getIdentifier(), PusheableAsset.CONTENTLET.toString(),
                contentlet_2.getIdentifier(), PusheableAsset.CONTENTLET.toString(),
                contentlet_3.getIdentifier(), PusheableAsset.CONTENTLET.toString(),
                contentlet_4.getIdentifier(), PusheableAsset.CONTENTLET.toString()
        ));
        publishAuditStatus.setStatusPojo(publishAuditHistory);
        publishAuditAPI.insertPublishAuditStatus(publishAuditStatus);

        PublishAuditStatus publishAuditStatusFromDB = publishAuditAPI.getPublishAuditStatus(bundle.getId(), -1);
        final PublishAuditHistory statusPojo = publishAuditStatusFromDB.getStatusPojo();
        assertEquals(5, statusPojo.getAssets().size());

        final Set<String> assetsIds = statusPojo.getAssets().keySet();
        assertTrue(assetsIds.contains(contentType.id()));
        assertTrue(assetsIds.contains(contentlet_1.getIdentifier()));
        assertTrue(assetsIds.contains(contentlet_2.getIdentifier()));
        assertTrue(assetsIds.contains(contentlet_3.getIdentifier()));
        assertTrue(assetsIds.contains(contentlet_4.getIdentifier()));
    }

    /**
     * Method to test: {@link PublishAuditAPI#getPublishAuditStatus(String, int)}
     * When: Create a {@link PublishAuditStatus} with 4 assets and call the methods with assetsLimit equals to 3
     * Should: return just three assets
     *
     * @throws DotPublisherException
     */
    @Test
    public void maxAsssetLimitsEqualsTo3() throws DotPublisherException {
        final ContentType contentType = new ContentTypeDataGen().nextPersisted();
        final Contentlet contentlet_1 = new ContentletDataGen(contentType).nextPersisted();
        final Contentlet contentlet_2 = new ContentletDataGen(contentType).nextPersisted();
        final Contentlet contentlet_3 = new ContentletDataGen(contentType).nextPersisted();
        final Contentlet contentlet_4 = new ContentletDataGen(contentType).nextPersisted();

        final List assets = list(contentlet_1, contentlet_2,
                contentlet_3, contentlet_4);

        final Bundle bundle = new BundleDataGen()
                .addAssets(assets)
                .nextPersisted();

        final PublishAuditStatus publishAuditStatus = new PublishAuditStatus(bundle.getId());

        final PublishAuditHistory publishAuditHistory = new PublishAuditHistory();
        publishAuditHistory.setAssets(map(
                contentlet_1.getIdentifier(), PusheableAsset.CONTENTLET.toString(),
                contentlet_2.getIdentifier(), PusheableAsset.CONTENTLET.toString(),
                contentlet_3.getIdentifier(), PusheableAsset.CONTENTLET.toString(),
                contentlet_4.getIdentifier(), PusheableAsset.CONTENTLET.toString()
        ));
        publishAuditStatus.setStatusPojo(publishAuditHistory);
        publishAuditAPI.insertPublishAuditStatus(publishAuditStatus);

        PublishAuditStatus publishAuditStatusFromDB = publishAuditAPI.getPublishAuditStatus(bundle.getId(), 3);
        final PublishAuditHistory statusPojo = publishAuditStatusFromDB.getStatusPojo();
        assertEquals(3, statusPojo.getAssets().size());

        final Set<String> assetsIds = statusPojo.getAssets().keySet();
        int count = 0;

        for (Object asset : assets) {
            final Contentlet contentlet = Contentlet.class.cast(asset);

            if (assetsIds.contains(contentlet.getIdentifier())){
                count++;
            }
        }

        assertEquals(3, count);
    }

    /**
     * Method to test: {@link PublishAuditAPI#getPublishAuditStatus(String, int)}
     * When: Create a {@link PublishAuditStatus} with 5 assets and call the methods with assetsLimit equals to 0
     * Should: return a empty collections
     *
     * @throws DotPublisherException
     */
    @Test
    public void notAssets() throws DotPublisherException {
        final ContentType contentType = new ContentTypeDataGen().nextPersisted();
        final Contentlet contentlet_1 = new ContentletDataGen(contentType).nextPersisted();
        final Contentlet contentlet_2 = new ContentletDataGen(contentType).nextPersisted();
        final Contentlet contentlet_3 = new ContentletDataGen(contentType).nextPersisted();
        final Contentlet contentlet_4 = new ContentletDataGen(contentType).nextPersisted();

        final Bundle bundle = new BundleDataGen()
                .addAssets(list(contentType, contentlet_1,contentlet_2, contentlet_3, contentlet_4))
                .nextPersisted();

        final PublishAuditStatus publishAuditStatus = new PublishAuditStatus(bundle.getId());

        final PublishAuditHistory publishAuditHistory = new PublishAuditHistory();
        publishAuditHistory.setAssets(map(
                contentType.id(), PusheableAsset.CONTENT_TYPE.toString(),
                contentlet_1.getIdentifier(), PusheableAsset.CONTENTLET.toString(),
                contentlet_2.getIdentifier(), PusheableAsset.CONTENTLET.toString(),
                contentlet_3.getIdentifier(), PusheableAsset.CONTENTLET.toString(),
                contentlet_4.getIdentifier(), PusheableAsset.CONTENTLET.toString()
        ));
        publishAuditStatus.setStatusPojo(publishAuditHistory);
        publishAuditAPI.insertPublishAuditStatus(publishAuditStatus);

        PublishAuditStatus publishAuditStatusFromDB = publishAuditAPI.getPublishAuditStatus(publishAuditStatus.getBundleId(), 0);
        final PublishAuditHistory statusPojo = publishAuditStatusFromDB.getStatusPojo();
        assertTrue(statusPojo.getAssets().isEmpty());
    }

    private PublishAuditStatus createPublishAuditStatus(Map<String, String> assetsMap, Object... assets) {
        final Bundle bundle = new BundleDataGen()
                .addAssets(List.of(assets))
                .nextPersisted();

        final PublishAuditStatus publishAuditStatus = new PublishAuditStatus(bundle.getId());

        final PublishAuditHistory publishAuditHistory = new PublishAuditHistory();
        publishAuditHistory.setAssets(assetsMap);
        publishAuditStatus.setStatusPojo(publishAuditHistory);

        try {
            publishAuditAPI.insertPublishAuditStatus(publishAuditStatus);
        } catch (DotPublisherException e) {
            throw new RuntimeException(e);
        }

        return publishAuditStatus;
    }

    /**
     * Method to test: {@link PublishAuditAPI#getAllPublishAuditStatus(int, int, int)}
     * When: Crate 4 PublishAuditStatus with: 5 assets, 2 assets, 1 assets, 0 assets and Call the methods with limitAssets equals to 2
     * Should: Return just two o less assets foe each {@link PublishAuditHistory}
     *
     * @throws DotPublisherException
     */
    @Test
    public void allThePublishAuditHistoryWithOneAssets() throws DotPublisherException {
        final ContentType contentType = new ContentTypeDataGen().nextPersisted();
        final Contentlet contentlet_1 = new ContentletDataGen(contentType).nextPersisted();
        final Contentlet contentlet_2 = new ContentletDataGen(contentType).nextPersisted();
        final Contentlet contentlet_3 = new ContentletDataGen(contentType).nextPersisted();
        final Contentlet contentlet_4 = new ContentletDataGen(contentType).nextPersisted();

        final Map<String, String> assetMap_1 = map(
                contentType.id(), PusheableAsset.CONTENT_TYPE.toString(),
                contentlet_1.getIdentifier(), PusheableAsset.CONTENTLET.toString(),
                contentlet_2.getIdentifier(), PusheableAsset.CONTENTLET.toString(),
                contentlet_3.getIdentifier(), PusheableAsset.CONTENTLET.toString(),
                contentlet_4.getIdentifier(), PusheableAsset.CONTENTLET.toString()
        );

        final PublishAuditStatus publishAuditStatus_1 = createPublishAuditStatus(assetMap_1,
                contentType, contentlet_1, contentlet_2, contentlet_3, contentlet_4);

        final Map<String, String> assetMap_2 = map(
                contentType.id(), PusheableAsset.CONTENT_TYPE.toString(),
                contentlet_1.getIdentifier(), PusheableAsset.CONTENTLET.toString()
        );

        final PublishAuditStatus publishAuditStatus_2 = createPublishAuditStatus(assetMap_2,
                contentType, contentlet_1);

        final Map<String, String> assetMap_3 = map(
                contentType.id(), PusheableAsset.CONTENT_TYPE.toString()
        );

        final PublishAuditStatus publishAuditStatus_3 = createPublishAuditStatus(assetMap_3,
                contentType);

        final PublishAuditStatus publishAuditStatus_4 = createPublishAuditStatus(map());

        final List<PublishAuditStatus> allPublishAuditStatus = publishAuditAPI
                .getAllPublishAuditStatus(-1, 0, -1);

        final List<String> bundleIDs = list(publishAuditStatus_1.getBundleId(),
                publishAuditStatus_2.getBundleId(),
                publishAuditStatus_3.getBundleId(), publishAuditStatus_4.getBundleId());

        final List<PublishAuditStatus> publishAuditStatuses = filter(allPublishAuditStatus, bundleIDs);
        assertEquals(4, publishAuditStatuses.size());

        for (PublishAuditStatus publishAuditStatus : publishAuditStatuses) {
            if (publishAuditStatus.getBundleId().equals(publishAuditStatus_1.getBundleId())) {
                assertEquals(5, publishAuditStatus.getStatusPojo().getAssets().size());
            } else if (publishAuditStatus.getBundleId().equals(publishAuditStatus_2.getBundleId())) {
                assertEquals(2, publishAuditStatus.getStatusPojo().getAssets().size());
            } else if (publishAuditStatus.getBundleId().equals(publishAuditStatus_3.getBundleId())) {
                assertEquals(1, publishAuditStatus.getStatusPojo().getAssets().size());
            } else if (publishAuditStatus.getBundleId().equals(publishAuditStatus_4.getBundleId())) {
                assertEquals(0, publishAuditStatus.getStatusPojo().getAssets().size());
            } else {
                throw new AssertionError("Not Expected");
            }
        }

        final List<PublishAuditStatus> allPublishAuditStatusWithAssetsLimit = publishAuditAPI
                .getAllPublishAuditStatus(-1, 0, 2);

        final List<PublishAuditStatus> publishAuditStatuses_2 = filter(allPublishAuditStatusWithAssetsLimit, bundleIDs);

        assertEquals(4, publishAuditStatuses.size());

        for (PublishAuditStatus publishAuditStatus : publishAuditStatuses_2) {
            if (publishAuditStatus.getBundleId().equals(publishAuditStatus_1.getBundleId())) {
                assertEquals(2, publishAuditStatus.getStatusPojo().getAssets().size());
            } else if (publishAuditStatus.getBundleId().equals(publishAuditStatus_2.getBundleId())) {
                assertEquals(2, publishAuditStatus.getStatusPojo().getAssets().size());
            } else if (publishAuditStatus.getBundleId().equals(publishAuditStatus_3.getBundleId())) {
                assertEquals(1, publishAuditStatus.getStatusPojo().getAssets().size());
            } else if (publishAuditStatus.getBundleId().equals(publishAuditStatus_4.getBundleId())) {
                assertEquals(0, publishAuditStatus.getStatusPojo().getAssets().size());
            } else {
                throw new AssertionError("Not Expected");
            }
        }

    }

    private List<PublishAuditStatus> filter(List<PublishAuditStatus> allPublishAuditStatus, List<String> bundleIDs) {
        return allPublishAuditStatus.stream()
                .filter(publishAuditStatus -> bundleIDs.contains(publishAuditStatus.getBundleId()))
                .collect(Collectors.toList());
    }
}
