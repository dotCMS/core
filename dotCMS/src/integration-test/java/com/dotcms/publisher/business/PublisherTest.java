package com.dotcms.publisher.business;

import com.dotcms.IntegrationTestBase;
import com.dotcms.LicenseTestUtil;
import com.dotcms.enterprise.publishing.remote.bundler.ContentBundler;
import com.dotcms.publisher.bundle.bean.Bundle;
import com.dotcms.publisher.endpoint.bean.PublishingEndPoint;
import com.dotcms.publisher.environment.bean.Environment;
import com.dotcms.publishing.BundlerStatus;
import com.dotcms.publishing.PublishStatus;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.AlreadyExistException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.util.Config;
import com.liferay.portal.model.User;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import org.apache.felix.framework.OSGIUtil;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.assertTrue;

/**
 * Test the PushedAssetsAPI
 */
@RunWith(DataProviderRunner.class)
public class PublisherTest extends IntegrationTestBase {


    @BeforeClass
    public static void prepare() throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
        LicenseTestUtil.getLicense();
        OSGIUtil.getInstance().initializeFramework(Config.CONTEXT);
    }

    /**
     * This method test the deletePushedAssetsByEnvironment method
     * 1) Creates a Folder and page
     * 2) Push the Page
     * 3) Remote remove of the folder
     * 4) Adds a new page to the folder
     * 5) Push the page
     */
    @Test
    public void test_Create_folderpage_Push_page_Remove_folderpush_Create_page_Push_page_SameBundle_shouldbe_created()
            throws Exception {

        User adminUser              = null;
        PPBean ppBean               = null;
        FolderPage folderPage       = null;
        PushResult    pushResult    = null;

        try {

            adminUser     = APILocator.getUserAPI().loadByUserByEmail("admin@dotcms.com", APILocator.getUserAPI().getSystemUser(), false);
            folderPage    = this.createFolderPage("testFolderPublisher"+System.currentTimeMillis(), adminUser);
            ppBean        = this.createPushPublishEnv(adminUser);
            pushResult    = this.pushFolderPage(folderPage, adminUser, ppBean);
            this.assertPushFolderPage(folderPage, pushResult);
            pushResult    = this.removePushFolder(folderPage, adminUser, ppBean);
            this.assertRemoveFolder(folderPage, pushResult);
            folderPage    = this.createNewPage(folderPage, adminUser);
            pushResult    = this.pushFolderPage(folderPage, adminUser, ppBean);
            this.assertPushFolderPage(folderPage, pushResult);
        } finally {

            if (null != ppBean) {
                PublisherTestUtil.cleanBundleEndpointEnv(null, ppBean.endPoint, ppBean.environment);
            }
        }
    }


    private FolderPage createNewPage (final  FolderPage folderPage, final User user) throws Exception {

        final HTMLPageAsset page = PublisherTestUtil.createPage(folderPage.folder, user);

        return new FolderPage(folderPage.folder, page);
    }


    private void assertPushFolderPage(final FolderPage folderPage, final PushResult pushResult) throws Exception {

        final User sysuser = APILocator.getUserAPI().getSystemUser();
        final Host host    = APILocator.getHostAPI().findDefaultHost(sysuser, false);
        final PublishStatus status                  = pushResult.publishStatus;
        final Optional<BundlerStatus> bundlerStatus = PublisherTestUtil.getBundleStatus(status.getBundlerStatuses(), ContentBundler.class);

        assertTrue(bundlerStatus.isPresent());
        assertTrue("We should have 1 page on: " + folderPage.folder, bundlerStatus.get().getCount() != 0);

        assertTrue(PublisherTestUtil.existsFolder(pushResult.bundlePath, folderPage.folder));
        assertTrue(PublisherTestUtil.existsPage(pushResult.bundlePath, host, 0, folderPage.folder, folderPage.page));
    }

    private void assertRemoveFolder(final FolderPage folderPage, final PushResult pushResult) throws Exception {

        assertTrue(PublisherTestUtil.existsFolder(pushResult.bundlePath, folderPage.folder));
    }



    private PPBean createPushPublishEnv(final User user) throws DotSecurityException, DotDataException {

        final Environment environment     = PublisherTestUtil.createEnvironment(user);
        final PublishingEndPoint endPoint = PublisherTestUtil.createEndpoint(environment);

        return new PPBean(environment, endPoint);
    }

    private PushResult pushFolderPage(final FolderPage folderPage, final User user, final PPBean ppBean)
            throws DotDataException, DotPublisherException, InstantiationException, IllegalAccessException, DotSecurityException {

        final PublisherAPI publisherAPI = PublisherAPI.getInstance();
        final Bundle bundle             = PublisherTestUtil.createBundle("folderPage1Test", user, ppBean.environment);
        final String assetRealPath      = Config.getStringProperty("ASSET_REAL_PATH", "test-resources");
        final File tempDir              = new File(assetRealPath + "/bundles/" + System.currentTimeMillis());

        //Creating temp bundle dir
        if (!tempDir.exists()) {
            tempDir.mkdirs();
        }

        final List<PublishQueueElement> assets = PublisherTestUtil.getAssets(bundle, folderPage.folder);

        publisherAPI.addContentsToPublish(
                Arrays.asList(folderPage.page.getIdentifier()),
                bundle.getId(), new Date(), user);

        return
            new PushResult(bundle, PublisherTestUtil.push (assets, bundle, user), assetRealPath + "/bundles/" + bundle.getId());
    }



    private FolderPage createFolderPage (final String folderName, final User user) throws Exception {

        final Folder folder      = PublisherTestUtil.createFolder(folderName);
        final HTMLPageAsset page = PublisherTestUtil.createPage(folder, user);

        return new FolderPage(folder, page);
    }

    private PushResult removePushFolder(final FolderPage folderPage, final User user, final PPBean ppBean)
            throws DotDataException, DotPublisherException, InstantiationException, IllegalAccessException, DotSecurityException {

        final PublisherAPI publisherAPI = PublisherAPI.getInstance();
        final Bundle bundle             = PublisherTestUtil.createBundle("removeFolderPage1Test", user, ppBean.environment);
        final String assetRealPath      = Config.getStringProperty("ASSET_REAL_PATH", "test-resources");

        final List<PublishQueueElement> assets = PublisherTestUtil.getAssets(bundle, folderPage.folder);

        publisherAPI.addContentsToUnpublish(
                Arrays.asList(folderPage.folder.getIdentifier()),
                bundle.getId(), new Date(), user);

        return
                new PushResult(bundle, PublisherTestUtil.remoteRemove (assets, bundle, user), assetRealPath + "/bundles/" + bundle.getId());
    }

    /**
     * Remove the content type and workflows created
     */
    @AfterClass
    public static void cleanup()
            throws DotDataException, DotSecurityException, InterruptedException, ExecutionException, AlreadyExistException {


    }

    class PushResult {

        final String bundlePath;
        final Bundle bundle;
        final PublishStatus publishStatus;

        public PushResult(final Bundle bundle, final PublishStatus publishStatus, final String bundlePath) {
            this.bundle        = bundle;
            this.publishStatus = publishStatus;
            this.bundlePath    = bundlePath;
        }
    }

    class PPBean {

        final Environment environment;
        final PublishingEndPoint endPoint;

        public PPBean(Environment environment, PublishingEndPoint endPoint) {
            this.environment = environment;
            this.endPoint    = endPoint;
        }
    }

    class FolderPage {

        final Folder folder;
        final HTMLPageAsset page;

        public FolderPage(final Folder folder, final HTMLPageAsset page) {
            this.folder = folder;
            this.page = page;
        }
    }
}