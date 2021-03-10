package com.dotcms.enterprise.publishing.bundlers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import com.dotcms.datagen.BundleDataGen;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.FileAssetDataGen;
import com.dotcms.datagen.FolderDataGen;
import com.dotcms.datagen.HTMLPageDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.TemplateDataGen;
import com.dotcms.publisher.pusher.PushPublisherConfig;
import com.dotcms.publishing.BundlerStatus;
import com.dotcms.publishing.BundlerUtil;
import com.dotcms.publishing.DotBundleException;

import com.dotcms.publishing.PublisherConfig;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ContentletVersionInfo;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.FileUtil;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import org.junit.BeforeClass;
import org.junit.Test;

public class ShortyBundlerTest {

    @BeforeClass
    public static void prepare() throws Exception {

        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }

    /**
     * Method to test: {@link ShortyBundler#generate(File, BundlerStatus)} When: Exists a page
     * pointing to a Image that exists Should: Add the image into the bundle
     *
     * @throws DotBundleException
     * @throws IOException
     * @throws DotSecurityException
     * @throws DotDataException
     */
    @Test
    public void whenPagePointToImageThatExists()
            throws DotBundleException, IOException, DotSecurityException, DotDataException {

        final Host host = new SiteDataGen().nextPersisted();
        final Folder folder = new FolderDataGen().nextPersisted();

        final File image = new File(Thread.currentThread().getContextClassLoader()
                .getResource("images/test.jpg").getFile());
        final Contentlet contentlet = new FileAssetDataGen(folder, image)
                .host(host)
                .nextPersisted();
        ContentletDataGen.publish(contentlet);

        final BundlerStatus status = mock(BundlerStatus.class);
        final ShortyBundler bundler = new ShortyBundler();
        final File bundleRoot = FileUtil
                .createTemporaryDirectory("ShortyBundlerTest_whenPagePointToImageThatExists_");

        createPageAndFile(host, bundleRoot);

        final File htmlFile = new File(bundleRoot.getAbsolutePath() + File.separator + "test");

        final String imagePath = String
                .format("/dA/%s/fileAsset/1200w/50q/test.jpg", contentlet.getIdentifier());
        try (final FileOutputStream fileOutputStream = new FileOutputStream(htmlFile)) {
            fileOutputStream.write(
                    String.format("<div style=\"background-image: url('%s')\"/>", imagePath)
                            .getBytes()
            );
        }

        final PushPublisherConfig config = new PushPublisherConfig();
        config.setOperation(PublisherConfig.Operation.PUBLISH);

        new BundleDataGen()
                .pushPublisherConfig(config)
                .nextPersisted();

        bundler.setConfig(config);
        bundler.generate(bundleRoot, status);

        final String expectedFilePath = bundleRoot.getAbsolutePath() + File.separator +
                "/live/" + host.getHostname() + "/1/dA/" + contentlet.getIdentifier()
                + "/fileAsset/1200w/50q/test.jpg";
        final File file = new File(expectedFilePath);

        assertTrue(file.exists());
    }

    /**
     * Method to test: {@link ShortyBundler#generate(File, BundlerStatus)} When: Exists a page
     * pointing to a Image that not exists Should: Not add the image into the bundle
     *
     * @throws DotBundleException
     * @throws IOException
     * @throws DotSecurityException
     * @throws DotDataException
     */
    @Test
    public void whenPagePointToImageThatNotExists()
            throws DotBundleException, IOException, DotSecurityException, DotDataException {

        final Host host = new SiteDataGen().nextPersisted();

        final BundlerStatus status = mock(BundlerStatus.class);
        final ShortyBundler bundler = new ShortyBundler();
        final File bundleRoot = FileUtil
                .createTemporaryDirectory("ShortyBundlerTest_whenPagePointToImageThatNotExists_");

        createPageAndFile(host, bundleRoot);

        final File htmlFile = new File(bundleRoot.getAbsolutePath() + File.separator + "test");

        final String imagePath = "/dA/716dcfa9-537d-419a-837f-73084a499cf/fileAsset/1200w/50q/test.jpg";
        try (final FileOutputStream fileOutputStream = new FileOutputStream(htmlFile)) {
            fileOutputStream.write(
                    String.format("<div style=\"background-image: url('%s')\"/>", imagePath)
                            .getBytes()
            );
        }

        final PushPublisherConfig config = new PushPublisherConfig();
        config.setOperation(PublisherConfig.Operation.PUBLISH);

        new BundleDataGen()
                .pushPublisherConfig(config)
                .nextPersisted();

        bundler.setConfig(config);
        bundler.generate(bundleRoot, status);

        final String expectedFilePath = bundleRoot.getAbsolutePath() + File.separator +
                "/live/" + host.getHostname()
                + "/1/dA/716dcfa9-537d-419a-837f-73084a499cf/fileAsset/1200w/50q/test.jpg";
        final File file = new File(expectedFilePath);

        assertFalse(file.exists());

        final List<File> files = com.liferay.util.FileUtil.listFilesRecursively(bundleRoot);
        assertEquals(2, files.size());
    }

    /**
     * Method to test: {@link ShortyBundler#generate(File, BundlerStatus)} When: Exists a page
     * pointing to a Image that exists but Not have live version Should: Not add the image into the
     * bundle
     *
     * @throws DotBundleException
     * @throws IOException
     * @throws DotSecurityException
     * @throws DotDataException
     */
    @Test
    public void whenPagePointToImageThatExistsButNotHaveLiveVersion()
            throws DotBundleException, IOException, DotSecurityException, DotDataException {

        final Host host = new SiteDataGen().nextPersisted();
        final Folder folder = new FolderDataGen().nextPersisted();

        final File image = new File(Thread.currentThread().getContextClassLoader()
                .getResource("images/test.jpg").getFile());
        final Contentlet contentlet = new FileAssetDataGen(folder, image)
                .host(host)
                .nextPersisted();

        final BundlerStatus status = mock(BundlerStatus.class);
        final ShortyBundler bundler = new ShortyBundler();
        final File bundleRoot = FileUtil
                .createTemporaryDirectory("ShortyBundlerTest_whenPagePointToImageThatExists_");

        createPageAndFile(host, bundleRoot);

        final File htmlFile = new File(bundleRoot.getAbsolutePath() + File.separator + "test");

        final String imagePath = String
                .format("/dA/%s/fileAsset/1200w/50q/test.jpg", contentlet.getIdentifier());
        try (final FileOutputStream fileOutputStream = new FileOutputStream(htmlFile)) {
            fileOutputStream.write(
                    String.format("<div style=\"background-image: url('%s')\"/>", imagePath)
                            .getBytes()
            );
        }

        final PushPublisherConfig config = new PushPublisherConfig();
        config.setOperation(PublisherConfig.Operation.PUBLISH);

        new BundleDataGen()
                .pushPublisherConfig(config)
                .nextPersisted();

        bundler.setConfig(config);
        bundler.generate(bundleRoot, status);

        final String expectedFilePath = bundleRoot.getAbsolutePath() + File.separator +
                "/live/" + host.getHostname() + "/1/dA/" + contentlet.getIdentifier()
                + "/fileAsset/1200w/50q/test.jpg";
        final File file = new File(expectedFilePath);

        assertFalse(file.exists());

        final List<File> files = com.liferay.util.FileUtil.listFilesRecursively(bundleRoot);
        assertEquals(2, files.size());
    }

    private void createPageAndFile(final Host host, final File bundleRoot) throws DotDataException {
        final Template template = new TemplateDataGen()
                .host(host)
                .nextPersisted();
        final HTMLPageAsset htmlPageAsset = new HTMLPageDataGen(host, template)
                .nextPersisted();

        ContentletDataGen.publish(htmlPageAsset);

        final Identifier identifier = APILocator.getIdentifierAPI()
                .find(htmlPageAsset.getIdentifier());

        final List<ContentletVersionInfo> versions = APILocator.getVersionableAPI()
                .findContentletVersionInfos(htmlPageAsset.getIdentifier());

        final HTMLPageAsContentWrapper wrapper = new HTMLPageAsContentWrapper();
        wrapper.setInfo(versions.get(0));
        wrapper.setId(identifier);
        wrapper.setAsset(htmlPageAsset);

        final File contentFile = new File(
                bundleRoot.getAbsolutePath() + File.separator + "test.html.xml");
        BundlerUtil.objectToXML(wrapper, contentFile);
    }
}
