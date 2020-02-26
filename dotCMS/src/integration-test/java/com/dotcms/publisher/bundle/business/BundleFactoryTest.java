package com.dotcms.publisher.bundle.business;

import com.dotcms.datagen.TestDataUtils;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.publisher.bundle.bean.Bundle;
import com.dotcms.publisher.business.DotPublisherException;
import com.dotcms.publisher.business.PublisherAPI;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.DateUtil;
import com.dotmarketing.util.UUIDGenerator;
import com.liferay.portal.model.User;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


public class BundleFactoryTest {

    private static BundleFactory bundleFactory;
    private static User adminUser;

    @BeforeClass
    public static void prepare() throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
        bundleFactory = FactoryLocator.getBundleFactory();
        adminUser = APILocator.systemUser();
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
        bundle.setFilterKey(null);
        APILocator.getBundleAPI().saveBundle(bundle);

        return uuid;
    }

    @Test
    public void test_findSentBundles_byAdminUser() throws DotDataException {
        //Create a few bundles that the owner is the Admin
        final ArrayList<String> bundleIdsAdmin = new ArrayList<>();
        final String systemUserId = adminUser.getUserId();
        for(int i=0;i<5;i++){
            bundleIdsAdmin.add(insertPublishingBundle(systemUserId,new Date()));
        }
        //Create a few bundles that the owner is a new User
        final User newUser = new UserDataGen().nextPersisted();
        final String newUserId = newUser.getUserId();
        for(int i=0;i<5;i++){
            bundleIdsAdmin.add(insertPublishingBundle(newUserId, new Date()));
        }

        final List<Bundle> bundlesSent = bundleFactory.findSentBundles(100,0);
        final List<String> bundlesSentIds = bundlesSent.stream().map(Bundle::getId).collect(Collectors.toList());

        //All bundles should be returned since the Admin can see all the bundles
        for(final String bundleId : bundleIdsAdmin){
            assertTrue(bundlesSentIds.contains(bundleId));
        }

        deletePublishingBundle(bundleIdsAdmin);
    }

    @Test
    public void test_findSentBundles_byLimitedUser() throws DotDataException {
        //Create a few bundles that the owner is the Admin
        final ArrayList<String> bundleIdsAdmin = new ArrayList<>();
        final String systemUserId = adminUser.getUserId();
        for(int i=0;i<5;i++){
            bundleIdsAdmin.add(insertPublishingBundle(systemUserId,new Date()));
        }
        //Create a few bundles that the owner is a new User
        final ArrayList<String> bundleIdsUser = new ArrayList<>();
        final User newUser = new UserDataGen().nextPersisted();
        final String newUserId = newUser.getUserId();
        for(int i=0;i<5;i++){
            bundleIdsUser.add(insertPublishingBundle(newUserId, new Date()));
        }

        final List<Bundle> bundlesSent = bundleFactory.findSentBundles(newUserId,100,0);
        final List<String> bundlesSentIds = bundlesSent.stream().map(Bundle::getId).collect(Collectors.toList());

        //Bundles that the owner is the Admin should not be returned
        for(final String bundleId : bundleIdsAdmin){
            assertFalse(bundlesSentIds.contains(bundleId));
        }
        //Bundles that the owner is the new User should be returned
        for(final String bundleId : bundleIdsUser){
            assertTrue(bundlesSentIds.contains(bundleId));
        }

        deletePublishingBundle(bundleIdsAdmin);
        deletePublishingBundle(bundleIdsUser);
    }

    @Test
    public void test_findSentBundlesOlderThan_byAdminUser() throws DotDataException {
        //Create a few bundles that the owner is the Admin
        final ArrayList<String> bundleIdsAdmin = new ArrayList<>();
        final String systemUserId = adminUser.getUserId();
        for(int i=1;i<5;i++){
            bundleIdsAdmin.add(insertPublishingBundle(systemUserId, DateUtil.addDate(new Date(),
                    Calendar.MONTH,-i)));
        }
        //Create a few bundles that the owner is a new User
        final User newUser = new UserDataGen().nextPersisted();
        final String newUserId = newUser.getUserId();
        for(int i=1;i<5;i++){
            bundleIdsAdmin.add(insertPublishingBundle(newUserId, DateUtil.addDate(new Date(),
                    Calendar.MONTH,-i)));
        }

        //Create a bundle with today date
        final String bundleTodayDate = insertPublishingBundle(systemUserId,new Date());

        final List<Bundle> bundlesSent = bundleFactory.findSentBundles(DateUtil.addDate(new Date(),
                Calendar.DAY_OF_MONTH,-1),100,0);
        final List<String> bundlesSentIds = bundlesSent.stream().map(Bundle::getId).collect(Collectors.toList());

        //All bundles on the list should be returned since the Admin can see all the bundles
        for(final String bundleId : bundleIdsAdmin){
            assertTrue(bundlesSentIds.contains(bundleId));
        }
        //Bundle with todays date should not be returned
        assertFalse(bundlesSentIds.contains(bundleTodayDate));

        bundleIdsAdmin.add(bundleTodayDate);
        deletePublishingBundle(bundleIdsAdmin);

    }

    @Test
    public void test_findSentBundlesOlderThan_byLimitedUser() throws DotDataException {
        //Create a few bundles that the owner is the Admin
        final ArrayList<String> bundleIdsAdmin = new ArrayList<>();
        final String systemUserId = adminUser.getUserId();
        for(int i=1;i<5;i++){
            bundleIdsAdmin.add(insertPublishingBundle(systemUserId, DateUtil.addDate(new Date(),
                    Calendar.MONTH,-i)));
        }
        //Create a few bundles that the owner is a new User
        final ArrayList<String> bundleIdsUser = new ArrayList<>();
        final User newUser = new UserDataGen().nextPersisted();
        final String newUserId = newUser.getUserId();
        for(int i=1;i<5;i++){
            bundleIdsUser.add(insertPublishingBundle(newUserId, DateUtil.addDate(new Date(),
                    Calendar.MONTH,-i)));
        }

        //Create a bundle with today date
        final String bundleTodayDate = insertPublishingBundle(newUserId,new Date());

        final List<Bundle> bundlesSent = bundleFactory.findSentBundles(DateUtil.addDate(new Date(),
                Calendar.DAY_OF_MONTH,-1),newUserId,100,0);
        final List<String> bundlesSentIds = bundlesSent.stream().map(Bundle::getId).collect(Collectors.toList());

        //Bundles that the owner is the Admin should not be returned
        for(final String bundleId : bundleIdsAdmin){
            assertFalse(bundlesSentIds.contains(bundleId));
        }
        //Bundles that the owner is the new User should be returned
        for(final String bundleId : bundleIdsUser){
            assertTrue(bundlesSentIds.contains(bundleId));
        }
        //Bundle with todays date should not be returned
        assertFalse(bundlesSentIds.contains(bundleTodayDate));

        bundleIdsAdmin.add(bundleTodayDate);
        deletePublishingBundle(bundleIdsAdmin);
        deletePublishingBundle(bundleIdsUser);
    }

    private void deletePublishingBundle(final List<String> listOfIds) throws DotDataException {
        for(final String bundleId : listOfIds){
            bundleFactory.deleteBundle(bundleId);
        }
    }

    @Test
    public void test_deleteAllAssetsFromBundle() throws DotDataException, DotPublisherException {
        final PublisherAPI publisherAPI = PublisherAPI.getInstance();
        final String bundleid = insertPublishingBundle(adminUser.getUserId(),new Date());

        final List<String> contentletList = new ArrayList<>();
        contentletList.add(TestDataUtils.getGenericContentContent(true, 1).getIdentifier());
        contentletList.add(TestDataUtils.getGenericContentContent(true, 1).getIdentifier());
        contentletList.add(TestDataUtils.getGenericContentContent(true, 1).getIdentifier());

        publisherAPI.addContentsToPublish(contentletList,bundleid,new Date(),adminUser);

        assertFalse(publisherAPI.getQueueElementsByBundleId(bundleid).isEmpty());

        bundleFactory.deleteAllAssetsFromBundle(bundleid);

        assertTrue(publisherAPI.getQueueElementsByBundleId(bundleid).isEmpty());

        bundleFactory.deleteBundle(bundleid);
    }

    @Test
    public void test_insertBundleWithFilterKey() throws DotDataException {
        final String uuid = UUIDGenerator.generateUuid();
        Bundle bundle = new Bundle();
        bundle.setId(uuid);
        bundle.setName("testBundle"+System.currentTimeMillis());
        bundle.setForcePush(false);
        bundle.setOwner(adminUser.getUserId());
        bundle.setPublishDate(new Date());
        bundle.setFilterKey("testFilter");
        APILocator.getBundleAPI().saveBundle(bundle);

        bundle = APILocator.getBundleAPI().getBundleById(uuid);
        assertEquals("testFilter",bundle.getFilterKey());
    }

    @Test
    public void test_insertBundleWithoutFilterKey() throws DotDataException {
        final String uuid = UUIDGenerator.generateUuid();
        Bundle bundle = new Bundle();
        bundle.setId(uuid);
        bundle.setName("testBundle"+System.currentTimeMillis());
        bundle.setForcePush(false);
        bundle.setOwner(adminUser.getUserId());
        bundle.setPublishDate(new Date());
        bundle.setFilterKey(null);
        APILocator.getBundleAPI().saveBundle(bundle);

        bundle = APILocator.getBundleAPI().getBundleById(uuid);
        assertTrue(bundle.getFilterKey().isEmpty());
    }
}
