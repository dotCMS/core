package com.dotcms.enterprise.publishing.bundler;

import static com.dotcms.util.CollectionsUtils.list;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.TextField;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.FieldDataGen;
import com.dotcms.datagen.HTMLPageDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.TemplateDataGen;
import com.dotcms.enterprise.publishing.bundlers.URLMapBundler;
import com.dotcms.publishing.BundlerStatus;
import com.dotcms.publishing.DotBundleException;
import com.dotcms.publishing.PublisherConfig;
import com.dotcms.publishing.output.BundleOutput;
import com.dotcms.publishing.output.DirectoryBundleOutput;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.Logger;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import org.apache.commons.io.FileUtils;
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

    /**
     * Method to test: {@link URLMapBundler#generate(BundleOutput, BundlerStatus)}
     * When:
     * - Create a detail page whose template body renders the URL-mapped content's text field via
     *   {@code $URLMapContent.get("fieldVar")}.
     * - Create a Content Type with a URL map pattern and two contentlets with different field values.
     * - Run the bundler.
     *
     * Should: Generate a distinct HTML file for each contentlet, where each file contains that
     * contentlet's unique field value — NOT the value from the first contentlet.
     */
    @Test
    public void urlMapGeneratesDifferentHTMLPerContentlet() throws DotBundleException, IOException {
        Host host = null;
        ContentType contentType = null;
        DirectoryBundleOutput directoryBundleOutput = null;

        try {
            host = new SiteDataGen().nextPersisted();

            // Create the field first so its variable name can be referenced in the template body
            final Field field = new FieldDataGen()
                    .type(TextField.class)
                    .indexed(true)
                    .next();

            // Template renders the URL-mapped contentlet's text field; this is what differentiates
            // the HTML output per contentlet
            final String templateBody = "<html><body>$URLMapContent.get(\""
                    + field.variable() + "\")</body></html>";

            final Template template = new TemplateDataGen()
                    .host(host)
                    .body(templateBody)
                    .nextPersisted();

            final HTMLPageAsset htmlPageAsset = new HTMLPageDataGen(host, template).nextPersisted();
            ContentletDataGen.publish(htmlPageAsset);

            contentType = new ContentTypeDataGen()
                    .field(field)
                    .detailPage(htmlPageAsset.getIdentifier())
                    .urlMapPattern("/test/{" + field.variable() + "}")
                    .nextPersisted();

            final Contentlet contentlet1 = new ContentletDataGen(contentType)
                    .host(host)
                    .setProperty(field.variable(), "UniqueValue_1")
                    .nextPersisted();
            ContentletDataGen.publish(contentlet1);

            final Contentlet contentlet2 = new ContentletDataGen(contentType)
                    .host(host)
                    .setProperty(field.variable(), "UniqueValue_2")
                    .nextPersisted();
            ContentletDataGen.publish(contentlet2);

            final PublisherConfig config = new PublisherConfig();
            config.setHosts(list(host));
            config.setIncludePatterns(list("/test/*"));

            directoryBundleOutput = new DirectoryBundleOutput(config);
            final BundlerStatus status = mock(BundlerStatus.class);

            final URLMapBundler urlMapBundler = new URLMapBundler();
            urlMapBundler.setConfig(config);
            urlMapBundler.generate(directoryBundleOutput, status);

            final long defaultLangId = APILocator.getLanguageAPI().getDefaultLanguage().getId();
            final String liveBase = directoryBundleOutput.getFile().getAbsolutePath()
                    + File.separator + "live"
                    + File.separator + host.getHostname()
                    + File.separator + defaultLangId
                    + File.separator + "test"
                    + File.separator;

            final File htmlFile1 = new File(liveBase + "UniqueValue_1");
            final File htmlFile2 = new File(liveBase + "UniqueValue_2");

            assertTrue("HTML file for contentlet 1 should exist", htmlFile1.exists());
            assertTrue("HTML file for contentlet 2 should exist", htmlFile2.exists());

            final String html1 = new String(Files.readAllBytes(htmlFile1.toPath()), StandardCharsets.UTF_8);
            final String html2 = new String(Files.readAllBytes(htmlFile2.toPath()), StandardCharsets.UTF_8);

            assertTrue("HTML for contentlet 1 should contain its own field value",
                    html1.contains("UniqueValue_1"));
            assertTrue("HTML for contentlet 2 should contain its own field value",
                    html2.contains("UniqueValue_2"));
            assertNotEquals(
                    "Each URL-mapped contentlet must produce unique HTML (regression: page cache collision)",
                    html1, html2);

        } finally {
            // Content type deletion cascades to all its contentlets
            if (contentType != null) {
                ContentTypeDataGen.remove(contentType);
            }
            if (host != null) {
                try {
                    APILocator.getHostAPI().archive(host, APILocator.systemUser(), false);
                    APILocator.getHostAPI().delete(host, APILocator.systemUser(), false);
                } catch (Exception e) {
                    Logger.error(URLMapBundlerTest.class, "Error cleaning up test host", e);
                }
            }
            if (directoryBundleOutput != null) {
                FileUtils.deleteDirectory(directoryBundleOutput.getFile());
            }
        }
    }
}
