package com.dotcms.enterprise.publishing.remote.bundler;

import com.dotcms.IntegrationTestBase;
import com.dotcms.publisher.pusher.PushPublisherConfig;
import com.dotcms.publishing.BundlerStatus;
import com.dotcms.publishing.DotBundleException;
import com.dotcms.publishing.PublisherConfig;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.util.Config;
import com.liferay.portal.model.User;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by nollymar on 6/30/17.
 */
public class HostBundlerTest extends IntegrationTestBase {

    private static HostAPI hostAPI;
    private static User user;

    @BeforeClass
    public static void prepare() throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();

        hostAPI = APILocator.getHostAPI();
        user = APILocator.getUserAPI().getSystemUser();

    }

    @Test
    public void testGenerate_success_when_liveContentletIsNotFound()
        throws DotBundleException, DotDataException, DotSecurityException {
        BundlerStatus status;
        File tempDir;
        Host host;
        HostBundler hostBundler;
        PushPublisherConfig config;
        Set<String> contentSet;
        String assetRealPath;

        assetRealPath = Config.getStringProperty("ASSET_REAL_PATH", "test-resources");
        contentSet = new HashSet();
        host = new Host();
        hostBundler = new HostBundler();
        status = new BundlerStatus(HostBundler.class.getName());

        //Creating host
        host.setHostname("host" + System.currentTimeMillis() + ".dotcms.com");
        host.setDefault(false);

        HibernateUtil.startTransaction();
        host = hostAPI.save(host, user, false);
        HibernateUtil.closeAndCommitTransaction();

        contentSet.add(host.getIdentifier());

        //Mocking Push Publish configuration
        config = Mockito.mock(PushPublisherConfig.class);
        Mockito.when(config.getHostSet()).thenReturn(contentSet);
        Mockito.when(config.isDownloading()).thenReturn(true);
        Mockito.when(config.getOperation()).thenReturn(PublisherConfig.Operation.PUBLISH);
        hostBundler.setConfig(config);

        //Creating temp bundle dir
        tempDir = new File(assetRealPath + "/bundles/" + System.currentTimeMillis());
        if (!tempDir.exists()) {
            tempDir.mkdirs();
        }

        try {
            //Generating bundle
            hostBundler.generate(tempDir, status);
            Assert.assertEquals(status.getCount(), 1);
        } finally {
            tempDir.delete();
            hostAPI.delete(host, user, false);
        }
    }
}
