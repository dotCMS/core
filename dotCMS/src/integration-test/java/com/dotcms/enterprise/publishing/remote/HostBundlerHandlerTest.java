package com.dotcms.enterprise.publishing.remote;

import com.dotcms.IntegrationTestBase;
import com.dotcms.LicenseTestUtil;
import com.dotcms.enterprise.publishing.remote.bundler.HostBundler;
import com.dotcms.enterprise.publishing.remote.handler.HostHandler;
import com.dotcms.publisher.pusher.PushPublisherConfig;
import com.dotcms.publishing.BundlerStatus;
import com.dotcms.publishing.PublisherConfig.Operation;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.contentlet.model.IndexPolicy;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.UUIDGenerator;
import com.liferay.portal.model.User;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

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
        originalHostSize = APILocator.getHostAPI().findAll(user, false).size();
    }

    /**
     * This test creates a host and add it to a bundle as an unpublish operation (push-remove option).
     * After a couple of seconds, the HostHandler handle this bundle and apply the operation (removes the host)
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

            contentSet = new HashSet();
            Host host = new Host();
            hostBundler = new HostBundler();
            status = new BundlerStatus(HostBundler.class.getName());

            //Creating host
            host.setHostname("hostUnpublish" + System.currentTimeMillis() + ".dotcms.com");
            host.setDefault(false);

            HibernateUtil.startTransaction();
            host.setIndexPolicy(IndexPolicy.FORCE);
            host = APILocator.getHostAPI().save(host, user, false);
            HibernateUtil.closeAndCommitTransaction();

            Assert.assertEquals(originalHostSize + 1, APILocator.getHostAPI().findAll(user, false).size());

            contentSet.add(host.getIdentifier());

            //Mocking Push Publish configuration
            config = Mockito.mock(PushPublisherConfig.class);
            Mockito.when(config.getHostSet()).thenReturn(contentSet);
            Mockito.when(config.isDownloading()).thenReturn(true);
            Mockito.when(config.getOperation()).thenReturn(Operation.UNPUBLISH);
            Mockito.when(config.getId()).thenReturn(UUIDGenerator.generateUuid());
            hostBundler.setConfig(config);

            //Creating temp bundle dir

            if (!tempDir.exists()) {
                tempDir.mkdirs();
            }

            hostBundler.generate(tempDir, status);
            Assert.assertEquals(1, status.getCount()); //Only 1 content in the bundler

            Thread.sleep(5000); //Let's wait a couple of seconds before running the Hanlder

            //Handler
            final HostHandler hostHandler = new HostHandler(config);
            hostHandler.handle(tempDir);

            Assert.assertEquals(originalHostSize, APILocator.getHostAPI().findAll(user, false).size());
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
        contentSet = new HashSet();
        Host host = new Host();

        try {

            hostBundler = new HostBundler();
            status = new BundlerStatus(HostBundler.class.getName());

            //Creating host
            host.setHostname("hostGenerate" + System.currentTimeMillis() + ".dotcms.com");
            host.setDefault(false);

            HibernateUtil.startTransaction();
            host.setIndexPolicy(IndexPolicy.FORCE);
            host = APILocator.getHostAPI().save(host, user, false);
            HibernateUtil.closeAndCommitTransaction();

            Assert.assertEquals(originalHostSize + 1, APILocator.getHostAPI().findAll(user, false).size());

            contentSet.add(host.getIdentifier());

            //Mocking Push Publish configuration
            config = Mockito.mock(PushPublisherConfig.class);
            Mockito.when(config.getHostSet()).thenReturn(contentSet);
            Mockito.when(config.isDownloading()).thenReturn(true);
            Mockito.when(config.getOperation()).thenReturn(Operation.PUBLISH);
            hostBundler.setConfig(config);

            //Creating temp bundle dir

            if (!tempDir.exists()) {
                tempDir.mkdirs();
            }

            hostBundler.generate(tempDir, status);
            Assert.assertEquals(1, status.getCount()); //Only 1 content in the bundler
        } finally {
            tempDir.delete();
            hostAPI.archive(host, user, false);
            hostAPI.delete(host, user, false);
        }
    }

}
