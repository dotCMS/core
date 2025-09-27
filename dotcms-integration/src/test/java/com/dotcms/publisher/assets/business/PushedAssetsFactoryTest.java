package com.dotcms.publisher.assets.business;

import com.dotcms.IntegrationTestBase;
import com.dotcms.LicenseTestUtil;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.*;
import com.dotcms.publisher.assets.bean.PushedAsset;
import com.dotcms.publisher.bundle.bean.Bundle;
import com.dotcms.publisher.endpoint.bean.impl.PushPublishingEndPoint;
import com.dotcms.publisher.environment.bean.Environment;
import com.dotcms.publisher.pusher.PushPublisher;
import com.dotcms.rest.api.v1.content.PushedAssetHistory;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.UUIDGenerator;
import com.liferay.portal.model.User;
import org.junit.BeforeClass;
import org.junit.Test;


import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class PushedAssetsFactoryTest  {

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
    }

    /**
     * Method to test: {@link PushedAssetsFactoryImpl#getPushedAssets(String)}
     * When: You have a content with a 3 long push history (all of them with different environment and bundle)
     * and called the method with the content id
     * Should: return all the history
     */
    @Test
    public void getPushedAssetsById() throws DotDataException {
        final ContentType contentType = new ContentTypeDataGen().nextPersisted();
        final Contentlet contentlet = new ContentletDataGen(contentType.id()).nextPersisted();

        final Environment[] environments =  {
                new EnvironmentDataGen().nextPersisted(),
                new EnvironmentDataGen().nextPersisted(),
                new EnvironmentDataGen().nextPersisted()
        } ;

        final Bundle[] bundles = {
                new BundleDataGen().nextPersisted(),
                new BundleDataGen().nextPersisted(),
                new BundleDataGen().nextPersisted()
        };

        createPushHistory(bundles[0], contentlet, environments[0], LocalDate.now());
        createPushHistory(bundles[1], contentlet, environments[1], LocalDate.now().plusDays(1));
        createPushHistory(bundles[2], contentlet, environments[2], LocalDate.now().plusDays(2));

        List<PushedAsset> pushedAssets = FactoryLocator.getPushedAssetsFactory().getPushedAssets(contentlet.getIdentifier());
        assertEquals(3, pushedAssets.size());
        checkPushAssets(pushedAssets, contentlet, environments, bundles, 0);
    }

    /**
     * Method to test: {@link PushedAssetsFactoryImpl#getPushedAssets(String)}
     * When: You have a content with a 3 long push history (all of them with different environment and bundle)
     * let test it with differents offset
     * and called the method with the content id
     * Should: return all the history
     */
    @Test
    public void getPushedAssetsByIdAndOffset() throws DotDataException, DotSecurityException {
        final ContentType contentType = new ContentTypeDataGen().nextPersisted();
        final Contentlet contentlet = new ContentletDataGen(contentType.id()).nextPersisted();

        final Environment[] environments =  {
                new EnvironmentDataGen().nextPersisted(),
                new EnvironmentDataGen().nextPersisted(),
                new EnvironmentDataGen().nextPersisted()
        } ;

        final Bundle[] bundles = {
                new BundleDataGen().nextPersisted(),
                new BundleDataGen().nextPersisted(),
                new BundleDataGen().nextPersisted()
        };

        createPushHistory(bundles[0], contentlet, environments[0], LocalDate.now());
        createPushHistory(bundles[1], contentlet, environments[1], LocalDate.now().plusDays(1));
        createPushHistory(bundles[2], contentlet, environments[2], LocalDate.now().plusDays(2));

        List<PushedAssetHistory> pushedAssets_offset_0 = FactoryLocator.getPushedAssetsFactory().getPushedAssets(
                contentlet.getIdentifier(), 0, -1);

        assertEquals(3, pushedAssets_offset_0.size());
        checkPushHistoryAssets(pushedAssets_offset_0, contentlet, environments, bundles, 0);

        List<PushedAssetHistory> pushedAssets_offset_1 = FactoryLocator.getPushedAssetsFactory().getPushedAssets(
                contentlet.getIdentifier(), 1, -1);

        assertEquals(2, pushedAssets_offset_1.size());
        checkPushHistoryAssets(pushedAssets_offset_1, contentlet, environments, bundles, 1);

        List<PushedAssetHistory> pushedAssets_offset_2 = FactoryLocator.getPushedAssetsFactory().getPushedAssets(
                contentlet.getIdentifier(), 2, -1);

        assertEquals(1, pushedAssets_offset_2.size());
        checkPushHistoryAssets(pushedAssets_offset_2, contentlet, environments, bundles, 2);

        List<PushedAssetHistory> pushedAssets_offset_3 = FactoryLocator.getPushedAssetsFactory().getPushedAssets(
                contentlet.getIdentifier(), 3, -1);

        assertTrue(pushedAssets_offset_3.isEmpty());
    }

    /**
     * Method to test: {@link PushedAssetsFactoryImpl#getPushedAssets(String)}
     * When: You have a content with a 3 long push history (all of them with different environment and bundle)
     * let test it with differents limits
     * and called the method with the content id
     * Should: return all the history
     */
    @Test
    public void getPushedAssetsByIdAndLimit() throws DotDataException, DotSecurityException {
        final ContentType contentType = new ContentTypeDataGen().nextPersisted();
        final Contentlet contentlet = new ContentletDataGen(contentType.id()).nextPersisted();

        final Environment[] environments =  {
                new EnvironmentDataGen().nextPersisted(),
                new EnvironmentDataGen().nextPersisted(),
                new EnvironmentDataGen().nextPersisted()
        } ;

        final Bundle[] bundles = {
                new BundleDataGen().owner(new UserDataGen().nextPersisted()).nextPersisted(),
                new BundleDataGen().owner(new UserDataGen().nextPersisted()).nextPersisted(),
                new BundleDataGen().owner(new UserDataGen().nextPersisted()).nextPersisted()
        };

        createPushHistory(bundles[0], contentlet, environments[0], LocalDate.now());
        createPushHistory(bundles[1], contentlet, environments[1], LocalDate.now().plusDays(1));
        createPushHistory(bundles[2], contentlet, environments[2], LocalDate.now().plusDays(2));

        List<PushedAssetHistory> pushedAssets_offset_0 = FactoryLocator.getPushedAssetsFactory().getPushedAssets(
                contentlet.getIdentifier(), 0, -1);

        assertEquals(3, pushedAssets_offset_0.size());
        checkPushHistoryAssets(pushedAssets_offset_0, contentlet, environments, bundles, 0);

        List<PushedAssetHistory> pushedAssets_limit_1 = FactoryLocator.getPushedAssetsFactory().getPushedAssets(
                contentlet.getIdentifier(), 0, 1);

        assertEquals(1, pushedAssets_limit_1.size());
        checkPushHistoryAssets(pushedAssets_limit_1, contentlet, environments, bundles, 0);

        List<PushedAssetHistory> pushedAssets_limit_2 = FactoryLocator.getPushedAssetsFactory().getPushedAssets(
                contentlet.getIdentifier(), 0, 2);

        assertEquals(2, pushedAssets_limit_2.size());
        checkPushHistoryAssets(pushedAssets_limit_2, contentlet, environments, bundles, 0);

        List<PushedAssetHistory> pushedAssets_limit_3 = FactoryLocator.getPushedAssetsFactory().getPushedAssets(
                contentlet.getIdentifier(), 0, 3);

        assertEquals(3, pushedAssets_limit_3.size());
        checkPushHistoryAssets(pushedAssets_limit_3, contentlet, environments, bundles, 0);

        List<PushedAssetHistory> pushedAssets_limit_4 = FactoryLocator.getPushedAssetsFactory().getPushedAssets(
                contentlet.getIdentifier(), 0, 4);

        assertEquals(3, pushedAssets_limit_3.size());
        checkPushHistoryAssets(pushedAssets_limit_3, contentlet, environments, bundles, 0);
    }

    /**
     * Method to test: {@link PushedAssetsFactoryImpl#getTotalPushedAssets(String)}
     * When: You have push asset from 2 differents contents
     * Should: return the count just for the contentid requested
     */
    @Test
    public void getTotalPushedAssets() throws DotDataException, DotSecurityException {
        final ContentType contentType_1 = new ContentTypeDataGen().nextPersisted();
        final Contentlet contentlet_1 = new ContentletDataGen(contentType_1.id()).nextPersisted();

        final Environment[] environments_1 =  {
                new EnvironmentDataGen().nextPersisted(),
                new EnvironmentDataGen().nextPersisted(),
                new EnvironmentDataGen().nextPersisted()
        } ;

        final Bundle[] bundles_1 = {
                new BundleDataGen().owner(new UserDataGen().nextPersisted()).nextPersisted(),
                new BundleDataGen().owner(new UserDataGen().nextPersisted()).nextPersisted(),
                new BundleDataGen().owner(new UserDataGen().nextPersisted()).nextPersisted()
        };

        createPushHistory(bundles_1[0], contentlet_1, environments_1[0], LocalDate.now());
        createPushHistory(bundles_1[1], contentlet_1, environments_1[1], LocalDate.now().plusDays(1));
        createPushHistory(bundles_1[2], contentlet_1, environments_1[2], LocalDate.now().plusDays(2));

        final ContentType contentType_2 = new ContentTypeDataGen().nextPersisted();
        final Contentlet contentlet_2 = new ContentletDataGen(contentType_2.id()).nextPersisted();

        final Environment[] environments_2 =  {
                new EnvironmentDataGen().nextPersisted(),
                new EnvironmentDataGen().nextPersisted()
        } ;

        final Bundle[] bundles_2 = {
                new BundleDataGen().owner(new UserDataGen().nextPersisted()).nextPersisted(),
                new BundleDataGen().owner(new UserDataGen().nextPersisted()).nextPersisted()
        };

        createPushHistory(bundles_2[0], contentlet_2, environments_2[0], LocalDate.now());
        createPushHistory(bundles_2[1], contentlet_2, environments_2[1], LocalDate.now().plusDays(1));

        assertEquals(3, APILocator.getPushedAssetsAPI().getTotalPushedAssets(contentlet_1.getIdentifier()));
        assertEquals(2, APILocator.getPushedAssetsAPI().getTotalPushedAssets(contentlet_2.getIdentifier()));

    }

    private static void checkPushAssets(List<PushedAsset> pushedAssets,
                                        Contentlet contentlet,
                                        Environment[] environments,
                                        Bundle[] bundles,
                                        int offset) {

        for (int i = 0; i < pushedAssets.size(); i++) {
            final PushedAsset pushedAsset = pushedAssets.get(i);
            assertEquals(contentlet.getIdentifier(), pushedAsset.getAssetId());
            assertEquals(environments[i + offset].getId(), pushedAsset.getEnvironmentId());
            assertEquals(bundles[i + offset].getId(), pushedAsset.getBundleId());
        }
    }

    private static void checkPushHistoryAssets(List<PushedAssetHistory> pushedAssetsHistory,
                                        Contentlet contentlet,
                                        Environment[] environments,
                                        Bundle[] bundles,
                                        int offset) throws DotDataException, DotSecurityException {

        for (int i = 0; i < pushedAssetsHistory.size(); i++) {
            final PushedAssetHistory pushedAsset = pushedAssetsHistory.get(i);
            assertEquals(environments[i + offset].getName(), pushedAsset.getEnvironment());
            assertEquals(bundles[i + offset].getId(), pushedAsset.getBundleId());

            User user = APILocator.getUserAPI().loadUserById(bundles[i + offset].getOwner());

            assertEquals(user.getFullName(), pushedAsset.getPushedBy());
        }
    }

    private static void createPushHistory(final Bundle bundle,
                                          final Contentlet contentlet,
                                          final Environment environment,
                                          final LocalDate currentDate) throws DotDataException {


        Date date = Date.from(currentDate
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant());

        final PushedAsset pushedAsset = new PushedAsset(
                bundle.getId(),
                contentlet.getIdentifier(),
                "CONTENT",
                date,
                environment.getId(),
                UUIDGenerator.generateUuid(),
                PushPublisher.class.getName());

        APILocator.getPushedAssetsAPI().savePushedAsset(pushedAsset);

    }
}
