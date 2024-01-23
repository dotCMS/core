package com.dotcms.enterprise.publishing.bundler;

import static com.dotcms.util.CollectionsUtils.list;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.TextField;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.BundleDataGen;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.FieldDataGen;
import com.dotcms.datagen.FilterDescriptorDataGen;
import com.dotcms.datagen.HTMLPageDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.TemplateDataGen;
import com.dotcms.enterprise.publishing.bundlers.URLMapBundler;
import com.dotcms.enterprise.publishing.remote.bundler.ContentBundler;
import com.dotcms.publisher.pusher.PushPublisherConfig;
import com.dotcms.publisher.util.PusheableAsset;
import com.dotcms.publishing.BundlerStatus;
import com.dotcms.publishing.DotBundleException;
import com.dotcms.publishing.FilterDescriptor;
import com.dotcms.publishing.PublisherConfig;
import com.dotcms.publishing.output.BundleOutput;
import com.dotcms.publishing.output.DirectoryBundleOutput;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.templates.model.Template;
import com.liferay.util.StringPool;
import java.io.File;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class URLMapBundlerTest {

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
    }

    /**
     * Method to test: {@link URLMapBundler#generate(BundleOutput, BundlerStatus)}
     * When:
     * - Create a Page.
     * - Crete a ContentType and set the page created before as detail page and two contentlet, also set url map as /test/{field.variable()}
     * Create two contentlet
     * - Include the path /test/{field.variable()} into the bundle
     *
     * Should: Create the dotUrlMap.xml file for both Contentlet
     *
     * @throws DotBundleException
     */
    @Test
    public void urlMapWithInclude() throws DotBundleException {
        final Host host = new SiteDataGen().nextPersisted();
        final Template template = new TemplateDataGen().host(host).nextPersisted();
        final HTMLPageAsset htmlPageAsset = new HTMLPageDataGen(host, template).nextPersisted();
        ContentletDataGen.publish(htmlPageAsset);

        final Field field = new FieldDataGen()
                .type(TextField.class)
                .indexed(true)
                .unique(true)
                .next();
        final ContentType contentType = new ContentTypeDataGen()
                .field(field)
                .detailPage(htmlPageAsset.getIdentifier())
                .urlMapPattern("/test/{" + field.variable() + "}")
                .nextPersisted();

        final Contentlet contentlet = new ContentletDataGen(contentType)
                .host(host)
                .setProperty(field.variable(), "Testing_1")
                .nextPersisted();
        ContentletDataGen.publish(contentlet);

        final Contentlet contentlet_2 = new ContentletDataGen(contentType)
                .host(host)
                .setProperty(field.variable(), "Testing_2")
                .nextPersisted();
        ContentletDataGen.publish(contentlet_2);

        final PublisherConfig config = new PublisherConfig();
        config.setHosts(list(host));
        config.setIncludePatterns(list("/test/*"));

        final DirectoryBundleOutput directoryBundleOutput = new DirectoryBundleOutput(config);
        final BundlerStatus status = mock(BundlerStatus.class);

        URLMapBundler urlMapBundler = new URLMapBundler();
        urlMapBundler.setConfig(config);
        urlMapBundler.generate(directoryBundleOutput, status);

        final File urlMapFile = new File(
                directoryBundleOutput.getFile().getAbsolutePath() + File.separator + "live"
                        + File.separator + host.getHostname() + File.separator + "1"
                        + File.separator + "test" + File.separator + "Testing_1.dotUrlMap.xml");


        assertTrue(urlMapFile.exists());

        final File urlMapFile_2 = new File(
                directoryBundleOutput.getFile().getAbsolutePath() + File.separator + "live"
                        + File.separator + host.getHostname() + File.separator + "1"
                        + File.separator + "test" + File.separator + "Testing_2.dotUrlMap.xml");


        assertTrue(urlMapFile_2.exists());
    }

    /**
     * Method to test: {@link URLMapBundler#generate(BundleOutput, BundlerStatus)}
     * When:
     * - Create a Page.
     * - Crete a ContentType and set the page created before as detail page and two contentlet, also set url map as /test with blank space/{field.variable()}
     * Create two contentlet
     * - Include the path /test with blank space/{field.variable()} into the bundle
     *
     * Should: Create the dotUrlMap.xml file for both Contentlet
     *
     * @throws DotBundleException
     */
    @Test
    public void urlMapWithIncludeAndBlankSpace() throws DotBundleException {
        final Host host = new SiteDataGen().nextPersisted();
        final Template template = new TemplateDataGen().host(host).nextPersisted();
        final HTMLPageAsset htmlPageAsset = new HTMLPageDataGen(host, template).nextPersisted();
        ContentletDataGen.publish(htmlPageAsset);

        final Field field = new FieldDataGen()
                .type(TextField.class)
                .indexed(true)
                .unique(true)
                .next();
        final ContentType contentType = new ContentTypeDataGen()
                .field(field)
                .detailPage(htmlPageAsset.getIdentifier())
                .urlMapPattern("/test with blank space/{" + field.variable() + "}")
                .nextPersisted();

        final Contentlet contentlet = new ContentletDataGen(contentType)
                .host(host)
                .setProperty(field.variable(), "Testing_1")
                .nextPersisted();
        ContentletDataGen.publish(contentlet);

        final Contentlet contentlet_2 = new ContentletDataGen(contentType)
                .host(host)
                .setProperty(field.variable(), "Testing_2")
                .nextPersisted();
        ContentletDataGen.publish(contentlet_2);

        final PublisherConfig config = new PublisherConfig();
        config.setHosts(list(host));
        config.setIncludePatterns(list("/test with blank space/*"));

        final DirectoryBundleOutput directoryBundleOutput = new DirectoryBundleOutput(config);
        final BundlerStatus status = mock(BundlerStatus.class);

        URLMapBundler urlMapBundler = new URLMapBundler();
        urlMapBundler.setConfig(config);
        urlMapBundler.generate(directoryBundleOutput, status);

        final File urlMapFile = new File(
                directoryBundleOutput.getFile().getAbsolutePath() + File.separator + "live"
                        + File.separator + host.getHostname() + File.separator + "1"
                        + File.separator + "/test with blank space" + File.separator + "Testing_1.dotUrlMap.xml");


        assertTrue(urlMapFile.exists());

        final File urlMapFile_2 = new File(
                directoryBundleOutput.getFile().getAbsolutePath() + File.separator + "live"
                        + File.separator + host.getHostname() + File.separator + "1"
                        + File.separator + "/test with blank space" + File.separator + "Testing_2.dotUrlMap.xml");


        assertTrue(urlMapFile_2.exists());
    }
}
