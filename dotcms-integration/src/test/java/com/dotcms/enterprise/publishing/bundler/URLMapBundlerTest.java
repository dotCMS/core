package com.dotcms.enterprise.publishing.bundler;

import static com.dotcms.util.CollectionsUtils.list;
import static org.junit.Assert.assertFalse;
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
import com.dotcms.datagen.LanguageDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.TemplateDataGen;
import com.dotcms.enterprise.publishing.bundlers.URLMapBundler;
import com.dotcms.enterprise.publishing.staticpublishing.StaticDependencyBundler;
import com.dotcms.publisher.business.PublishQueueElement;
import com.dotcms.publisher.util.PusheableAsset;
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
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashSet;
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

    /**
     * Method to test: {@link StaticDependencyBundler#generate(BundleOutput, BundlerStatus)} followed by
     * {@link URLMapBundler#generate(BundleOutput, BundlerStatus)}, the static Push Publish chain.
     * Given scenario: a URL-mapped content with a LIVE default-language version and a LIVE translation
     * at a different localized URL, where the translation was edited last, pushed to a static endpoint
     * with DEFAULT_CONTENT_TO_DEFAULT_LANGUAGE=true and a third language with no translation.
     * Expected result: each language folder gets its own localized file, the default-language content
     * is not duplicated into the translated language's folder, and the language without a translation
     * still gets the default-language fallback copy. See #36729.
     */
    @Test
    public void staticPushPublishesEveryLiveLanguage_translationEditedLast() throws Exception {
        assertStaticPushPublishesEveryLiveLanguage(false);
    }

    /**
     * Same as {@link #staticPushPublishesEveryLiveLanguage_translationEditedLast()} but with the
     * default-language version edited last, the case that used to duplicate it into every language
     * folder. See #36729.
     */
    @Test
    public void staticPushPublishesEveryLiveLanguage_defaultLanguageEditedLast() throws Exception {
        assertStaticPushPublishesEveryLiveLanguage(true);
    }

    /**
     * Runs the static Push Publish bundler chain for one URL-mapped content that is live in the
     * default language and in a second language, and checks what lands in each language folder.
     *
     * @param defaultLanguageEditedLast whether the default-language version is the most recently
     *                                  modified one (true) or the translation is (false)
     * @throws Exception if the fixtures or the bundlers fail
     */
    private void assertStaticPushPublishesEveryLiveLanguage(final boolean defaultLanguageEditedLast)
            throws Exception {
        final boolean originalFlag = Config.getBooleanProperty("DEFAULT_CONTENT_TO_DEFAULT_LANGUAGE", false);
        Host host = null;
        ContentType contentType = null;
        Language secondLanguage = null;
        Language thirdLanguage = null;
        DirectoryBundleOutput directoryBundleOutput = null;
        try {
            Config.setProperty("DEFAULT_CONTENT_TO_DEFAULT_LANGUAGE", true);
            final Language defaultLanguage = APILocator.getLanguageAPI().getDefaultLanguage();
            secondLanguage = new LanguageDataGen().nextPersisted();
            thirdLanguage = new LanguageDataGen().nextPersisted();
            host = new SiteDataGen().nextPersisted();

            final Field field = new FieldDataGen().type(TextField.class).indexed(true).next();
            final Template template = new TemplateDataGen().host(host)
                    .body("<html><body>$URLMapContent.get(\"" + field.variable() + "\")</body></html>")
                    .nextPersisted();
            final HTMLPageAsset detailPage = new HTMLPageDataGen(host, template).nextPersisted();
            ContentletDataGen.publish(detailPage);

            contentType = new ContentTypeDataGen().field(field)
                    .detailPage(detailPage.getIdentifier())
                    .urlMapPattern("/test/{" + field.variable() + "}")
                    .nextPersisted();

            final String defaultSlug = "default-" + System.currentTimeMillis();
            final String translatedSlug = "translated-" + System.currentTimeMillis();

            Contentlet defaultVersion = new ContentletDataGen(contentType).host(host)
                    .languageId(defaultLanguage.getId())
                    .setProperty(field.variable(), defaultSlug)
                    .nextPersisted();
            ContentletDataGen.publish(defaultVersion);

            // Translation: same identifier, other language, its own localized URL
            final Contentlet translation = ContentletDataGen.checkout(defaultVersion);
            translation.setLanguageId(secondLanguage.getId());
            translation.setProperty(field.variable(), translatedSlug);
            ContentletDataGen.publish(ContentletDataGen.checkin(translation));

            if (defaultLanguageEditedLast) {
                final Contentlet checkedOut = ContentletDataGen.checkout(defaultVersion);
                checkedOut.setProperty(field.variable(), defaultSlug);
                defaultVersion = ContentletDataGen.publish(ContentletDataGen.checkin(checkedOut));
            }

            final PublishQueueElement asset = new PublishQueueElement();
            asset.setAsset(defaultVersion.getIdentifier());
            asset.setType(PusheableAsset.CONTENTLET.getType());

            final PublisherConfig config = new PublisherConfig();
            config.setStatic(true);
            config.setHosts(list(host));
            config.setLanguages(new HashSet<>(list(
                    String.valueOf(defaultLanguage.getId()),
                    String.valueOf(secondLanguage.getId()),
                    String.valueOf(thirdLanguage.getId()))));
            config.setAssets(list(asset));

            directoryBundleOutput = new DirectoryBundleOutput(config);
            final BundlerStatus status = mock(BundlerStatus.class);

            final StaticDependencyBundler dependencyBundler = new StaticDependencyBundler();
            dependencyBundler.setConfig(config);
            dependencyBundler.generate(directoryBundleOutput, status);

            final URLMapBundler urlMapBundler = new URLMapBundler();
            urlMapBundler.setConfig(config);
            urlMapBundler.generate(directoryBundleOutput, status);

            final String liveBase = directoryBundleOutput.getFile().getAbsolutePath()
                    + File.separator + "live" + File.separator + host.getHostname() + File.separator;
            final File defaultFile = new File(liveBase + defaultLanguage.getId()
                    + File.separator + "test" + File.separator + defaultSlug);
            final File translatedFile = new File(liveBase + secondLanguage.getId()
                    + File.separator + "test" + File.separator + translatedSlug);
            final File duplicateInTranslatedFolder = new File(liveBase + secondLanguage.getId()
                    + File.separator + "test" + File.separator + defaultSlug);
            final File fallbackInThirdFolder = new File(liveBase + thirdLanguage.getId()
                    + File.separator + "test" + File.separator + defaultSlug);

            assertTrue("Default-language version must be published at its own URL", defaultFile.exists());
            assertTrue("Translation must be published at its own localized URL", translatedFile.exists());
            assertTrue("Translated file must render the translation",
                    new String(Files.readAllBytes(translatedFile.toPath()), StandardCharsets.UTF_8)
                            .contains(translatedSlug));
            assertFalse("Default-language content must not be duplicated into a language that has "
                    + "its own translation", duplicateInTranslatedFolder.exists());
            assertTrue("A language with no translation still gets the default-language fallback",
                    fallbackInThirdFolder.exists());
        } finally {
            Config.setProperty("DEFAULT_CONTENT_TO_DEFAULT_LANGUAGE", originalFlag);
            // Content type deletion cascades to all its contentlets, in every language
            if (contentType != null) {
                ContentTypeDataGen.remove(contentType);
            }
            if (secondLanguage != null) {
                LanguageDataGen.remove(secondLanguage);
            }
            if (thirdLanguage != null) {
                LanguageDataGen.remove(thirdLanguage);
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
