package com.dotcms.publisher.assets.business;

import com.dotcms.IntegrationTestBase;
import com.dotcms.LicenseTestUtil;
import com.dotcms.publisher.assets.bean.PushedAsset;
import com.dotcms.publisher.bundle.bean.Bundle;
import com.dotcms.publisher.bundle.business.BundleAPI;
import com.dotcms.publisher.business.PublisherTestUtil;
import com.dotcms.publisher.endpoint.bean.PublishingEndPoint;
import com.dotcms.publisher.environment.bean.Environment;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.UUIDGenerator;
import com.liferay.portal.model.User;
import io.vavr.API;
import org.jetbrains.annotations.NotNull;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * Test the PushedAssetsAPI
 */
public class PushedAssetsAPITest extends IntegrationTestBase {

    @BeforeClass
    public static void prepare() throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
        LicenseTestUtil.getLicense();
    }

    /**
     * This method test the deletePushedAssetsByEnvironment method
     */
    @Test
    public void test_save_and_delete_pushed_assets_on_two_env() throws DotDataException, DotSecurityException {

        final PushedAssetsAPI  pushedAssetsAPI            = APILocator.getPushedAssetsAPI();
        final ContentletAPI    contentletAPI              = APILocator.getContentletAPI();
        final BundleAPI        bundleAPI                  = APILocator.getBundleAPI();
        Environment            environment1               = null;
        Environment            environment2               = null;
        PublishingEndPoint     endpoint1                  = null;
        PublishingEndPoint     endpoint2                  = null;
        Bundle                 bundle1                    = null;
        Bundle                 bundle2                    = null;
        User                   adminUser;

        try {

            // ---------------------------------------------------------
            // creating the envs, endpoints and bundles.
            adminUser = APILocator.getUserAPI().loadByUserByEmail("admin@dotcms.com", APILocator.getUserAPI().getSystemUser(), false);

            environment1 = PublisherTestUtil.createEnvironment(adminUser);
            environment2 = PublisherTestUtil.createEnvironment(adminUser);

            endpoint1    = PublisherTestUtil.createEndpoint(environment1);
            endpoint2    = PublisherTestUtil.createEndpoint(environment2);
            //Save the endpoint.

            bundle1           = new Bundle( "testBundle", null, null, adminUser.getUserId() );
            bundleAPI.saveBundle(bundle1);
            bundle2           = new Bundle( "testBundle2", null, null, adminUser.getUserId() );
            bundleAPI.saveBundle(bundle2);

            // ---------------------------------------------------------
            // ------------- test_save_and_delete_Pushed_Assets --------
            final List<PushedAsset> pushedAssets1 = getPushedAssets(pushedAssetsAPI, contentletAPI, environment1, endpoint1, bundle1, adminUser);
            final List<PushedAsset> pushedAssets2 = getPushedAssets(pushedAssetsAPI, contentletAPI, environment2, endpoint2, bundle2, adminUser);

            // testing assets were adding ok in both env
            for (final PushedAsset asset : pushedAssets1) {

                assertNotNull(pushedAssetsAPI.getLastPushForAsset(asset.getAssetId(), environment1.getId(), endpoint1.getId()));
            }

            for (final PushedAsset asset : pushedAssets2) {

                assertNotNull(pushedAssetsAPI.getLastPushForAsset(asset.getAssetId(), environment2.getId(), endpoint2.getId()));
            }

            // so remove the first env group
            for (final PushedAsset asset : pushedAssets1) {

                pushedAssetsAPI.deletePushedAssetsByEnvironment(asset.getAssetId(), environment1.getId());
            }

            // double check first group is gone but second one still there
            for (final PushedAsset asset : pushedAssets1) {

                final PushedAsset pushedAssetRecovery =
                        pushedAssetsAPI.getLastPushForAsset(asset.getAssetId(), environment1.getId(), endpoint1.getId());

                assertNull(pushedAssetRecovery);
            }

            for (final PushedAsset asset : pushedAssets2) {

                assertNotNull(pushedAssetsAPI.getLastPushForAsset(asset.getAssetId(), environment2.getId(), endpoint2.getId()));
            }
        } finally {

            PublisherTestUtil.cleanBundleEndpointEnv(bundle1, endpoint1, environment1);
            PublisherTestUtil.cleanBundleEndpointEnv(bundle2, endpoint2, environment2);
        }
    }

    @NotNull
    private List<PushedAsset> getPushedAssets(final PushedAssetsAPI pushedAssetsAPI,
                                              final ContentletAPI contentletAPI,
                                              final Environment environment1,
                                              final PublishingEndPoint endpoint1,
                                              final Bundle bundle1,
                                              final User adminUser) throws DotDataException {
        PushedAsset pushedAsset = null;
        final List<Contentlet> contentlets = contentletAPI.findAllContent
                ( 0, 30 ).stream().filter(Objects::nonNull).collect(Collectors.toList());
        final List<PushedAsset> pushedAssets = new ArrayList<>();
        final Date pushDate = new Date();
        for (final Contentlet contentlet : contentlets) {

            pushedAsset = new PushedAsset(bundle1.getId(), contentlet.getIdentifier(), "content", pushDate, environment1.getId(), endpoint1.getId(), adminUser.getNickName());
            pushedAssetsAPI.savePushedAsset(pushedAsset);
            pushedAssets.add(pushedAsset);
        }

        return pushedAssets;
    }

    @Test
    public void test_deletePushedAssetsByBundleId() throws DotSecurityException, DotDataException {
        final PushedAssetsAPI  pushedAssetsAPI = APILocator.getPushedAssetsAPI();
        final User adminUser = APILocator.systemUser();
        final String bundleId = UUIDGenerator.generateUuid();
        final String environmentId = UUIDGenerator.generateUuid();
        for(int i=0;i<3;i++) {
            final PushedAsset pushedAsset = new PushedAsset(bundleId, UUIDGenerator.generateUuid(),
                    "content", new Date(), environmentId,
                    UUIDGenerator.generateUuid(), adminUser.getNickName());
            pushedAssetsAPI.savePushedAsset(pushedAsset);
        }
        assertFalse(pushedAssetsAPI.getPushedAssetsByBundleIdAndEnvironmentId(bundleId,environmentId).isEmpty());

        pushedAssetsAPI.deletePushedAssetsByBundleId(bundleId);

        assertTrue(pushedAssetsAPI.getPushedAssetsByBundleIdAndEnvironmentId(bundleId,environmentId).isEmpty());
    }



}