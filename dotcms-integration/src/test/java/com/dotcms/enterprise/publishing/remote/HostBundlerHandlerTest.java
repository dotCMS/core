package com.dotcms.enterprise.publishing.remote;

import com.dotcms.IntegrationTestBase;
import com.dotcms.LicenseTestUtil;
import com.dotcms.datagen.TestDataUtils;
import com.dotcms.enterprise.publishing.remote.bundler.HostBundler;
import com.dotcms.enterprise.publishing.remote.handler.HostHandler;
import com.dotcms.publisher.pusher.PushPublisherConfig;
import com.dotcms.publishing.BundlerStatus;
import com.dotcms.publishing.PublisherConfig.Operation;
import com.dotcms.publishing.output.BundleOutput;
import com.dotcms.publishing.output.DirectoryBundleOutput;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.db.LocalTransaction;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.contentlet.model.IndexPolicy;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.UUIDGenerator;
import com.liferay.portal.model.User;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;

/**
 * Unit test class for testing the functionality of the Host Bundler and Handler operations. This
 * class verifies the integration points and behaviors of the HostBundler and HostHandler classes
 * when managing Sites (Hosts) in various scenarios like unpublishing and generating Host bundles.
 * It extends {@link IntegrationTestBase} to leverage the testing infrastructure for integration
 * tests.
 *
 * @author Steve Bolton
 * @since Aug 27th, 2018
 */
public class HostBundlerHandlerTest extends IntegrationTestBase {

    private static User user;
    private static HostAPI hostAPI;
    private static int originalHostSize;

    @BeforeClass
    public static void prepare() throws Exception {

        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
        LicenseTestUtil.getLicense();

        hostAPI = APILocator.getHostAPI();
        user = APILocator.getUserAPI().getSystemUser();
        originalHostSize = APILocator.getHostAPI().findAllFromDB(user,
                HostAPI.SearchType.INCLUDE_SYSTEM_HOST).size();
    }

    /**
     * <ul>
     *     <li><b>Method to test: </b>{@link HostBundler#generate(BundleOutput, BundlerStatus)}, 
     *     and {@link HostHandler#handle(File)}</li>
     *     <li><b>Given Scenario: </b>This test creates a Site and adds it to a bundle as an
     *     unpublish operation (push-remove option).</li>
     *     <li><b>Expected Result: </b>After a couple of seconds, the {@link HostHandler} handles
     *     this bundle and applies the operation (removes the Site).</li>
     * </ul>
     */
    @Test
    public void testBundlerHandler_UnpublishHost_Success() throws Exception {
        final BundlerStatus status;
        final String assetRealPath = Config.getStringProperty("ASSET_REAL_PATH", "test-resources");
        final File tempDir = new File(assetRealPath + "/bundles/" + System.currentTimeMillis());
        final HostBundler hostBundler;
        final PushPublisherConfig config;
        final Set<String> contentSet;

        try {
            contentSet = new HashSet<>();
            hostBundler = new HostBundler();
            status = new BundlerStatus(HostBundler.class.getName());

            final Host site2 = LocalTransaction.wrapReturn(() -> {
                Host site = new Host();
                // Creating site
                site.setHostname("siteUnpublish" + System.currentTimeMillis() + ".dotcms.com");
                site.setDefault(false);
                site.setLanguageId(APILocator.getLanguageAPI().getDefaultLanguage().getId());
                site.setIndexPolicy(IndexPolicy.FORCE);
                return APILocator.getHostAPI().save(site, user, false);
            });

            assertEquals("The total number of Sites should be one more, including the recently created test Site", originalHostSize + 1,
                    APILocator.getHostAPI().findAllFromDB(user,
                            HostAPI.SearchType.INCLUDE_SYSTEM_HOST).size());

            contentSet.add(site2.getIdentifier());

            //Mocking Push Publish configuration
            config = Mockito.mock(PushPublisherConfig.class);
            Mockito.when(config.getHostSet()).thenReturn(contentSet);
            Mockito.when(config.isDownloading()).thenReturn(true);
            Mockito.when(config.getOperation()).thenReturn(Operation.UNPUBLISH);
            Mockito.when(config.getId()).thenReturn(UUIDGenerator.generateUuid());
            hostBundler.setConfig(config);

            final DirectoryBundleOutput directoryBundleOutput = new DirectoryBundleOutput(config,
                    tempDir);

            // Creating the temp bundle dir
            if (!tempDir.exists()) {
                tempDir.mkdirs();
            }

            hostBundler.generate(directoryBundleOutput, status);
            assertEquals("Only one Contentlet (the test Site) is expected in the bundle", 1, status.getCount());

            TestDataUtils.assertEmptyQueue();

            //Handler
            final HostHandler hostHandler = new HostHandler(config);
            hostHandler.handle(tempDir);

            assertEquals("The test Site created in this test must NOT exist anymore", originalHostSize,
                    APILocator.getHostAPI().findAllFromDB(user,
                            HostAPI.SearchType.INCLUDE_SYSTEM_HOST).size());

            TestDataUtils.assertEmptyQueue();
        } finally {
            tempDir.delete();
        }
    }

    @Test
    public void testGenerate_success_when_liveContentletIsNotFound()
            throws Exception {
        final BundlerStatus status;
        final String assetRealPath = Config.getStringProperty("ASSET_REAL_PATH", "test-resources");
        final File tempDir = new File(assetRealPath + "/bundles/" + System.currentTimeMillis());
        final HostBundler hostBundler;
        final PushPublisherConfig config;
        final Set<String> contentSet;
        contentSet = new HashSet<>();
        Host site = new Host();

        try {
            hostBundler = new HostBundler();
            status = new BundlerStatus(HostBundler.class.getName());

            // Creating the test Site
            site.setHostname("siteGenerate" + System.currentTimeMillis() + ".dotcms.com");
            site.setDefault(false);
            site.setLanguageId(APILocator.getLanguageAPI().getDefaultLanguage().getId());
            HibernateUtil.startTransaction();
            try {
                site.setIndexPolicy(IndexPolicy.FORCE);
                site = APILocator.getHostAPI().save(site, user, false);
            } catch (final Exception e) {
                HibernateUtil.rollbackTransaction();
                throw e;
            } finally {
                HibernateUtil.closeAndCommitTransaction();
            }

            assertEquals("The total number of Sites should be one more, including the recently created test Site", originalHostSize + 1,
                    APILocator.getHostAPI().findAllFromDB(user,
                            HostAPI.SearchType.INCLUDE_SYSTEM_HOST).size());

            contentSet.add(site.getIdentifier());

            //Mocking Push Publish configuration
            config = Mockito.mock(PushPublisherConfig.class);
            Mockito.when(config.getHostSet()).thenReturn(contentSet);
            Mockito.when(config.isDownloading()).thenReturn(true);
            Mockito.when(config.getOperation()).thenReturn(Operation.PUBLISH);
            hostBundler.setConfig(config);

            final DirectoryBundleOutput directoryBundleOutput = new DirectoryBundleOutput(config,
                    tempDir);

            // Creating the temp bundle dir
            if (!tempDir.exists()) {
                tempDir.mkdirs();
            }

            hostBundler.generate(directoryBundleOutput, status);
            assertEquals("Only one Contentlet (the test Site) is expected in the bundle", 1, status.getCount()); //Only 1 content in the bundler
        } finally {
            tempDir.delete();
            hostAPI.archive(site, user, false);
            hostAPI.delete(site, user, false);
        }
    }

}
