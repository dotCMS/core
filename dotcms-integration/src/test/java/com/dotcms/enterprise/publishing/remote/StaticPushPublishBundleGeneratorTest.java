package com.dotcms.enterprise.publishing.remote;

import static com.dotcms.enterprise.publishing.remote.PushPublishBundleGeneratorTest.createBundle;
import static com.dotcms.enterprise.publishing.remote.PushPublishBundleGeneratorTest.defaultFilterKey;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;

import com.dotcms.IntegrationTestBase;
import com.dotcms.LicenseTestUtil;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.FileAssetDataGen;
import com.dotcms.datagen.FolderDataGen;
import com.dotcms.datagen.HTMLPageDataGen;
import com.dotcms.datagen.TemplateDataGen;
import com.dotcms.enterprise.publishing.bundlers.FileAssetBundler;
import com.dotcms.enterprise.publishing.bundlers.FileAssetWrapper;
import com.dotcms.enterprise.publishing.bundlers.HTMLPageAsContentBundler;
import com.dotcms.enterprise.publishing.remote.StaticPushPublishBundleGeneratorTest.TestCase.Condition;
import com.dotcms.enterprise.publishing.staticpublishing.StaticPublisher;
import com.dotcms.publisher.bundle.bean.Bundle;
import com.dotcms.publisher.business.PublishAuditAPI;
import com.dotcms.publisher.business.PublishQueueElement;
import com.dotcms.publisher.business.PublisherAPI;
import com.dotcms.publisher.pusher.PushPublisherConfig;
import com.dotcms.publisher.receiver.BundlePublisher;
import com.dotcms.publisher.util.PublisherUtil;
import com.dotcms.publishing.*;
import com.dotcms.publishing.PublisherConfig.Operation;
import com.dotcms.publishing.output.BundleOutput;
import com.dotcms.publishing.output.DirectoryBundleOutput;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.google.common.collect.ImmutableList;
import com.google.common.io.Files;
import com.liferay.portal.model.User;
import com.rainerhahnekamp.sneakythrow.Sneaky;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import io.vavr.Lazy;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.commons.lang.RandomStringUtils;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;


@RunWith(DataProviderRunner.class)
public class StaticPushPublishBundleGeneratorTest extends IntegrationTestBase {

   private static AtomicBoolean prepared = new AtomicBoolean(false);
    private static Folder folder;
    private static long langId;

    static class TestCase{

       enum Condition {
           EQ, GT, GTE, LT,LTE
       }

       final  boolean isInclude;
      final Class<? extends IBundler> bundler;
      final String fileName;
      final String pattern;
      final int expectedMatches;
      final Condition condition;

       public TestCase(boolean isInclude, final Class<? extends IBundler> bundler,
               final String fileName, final String pattern,
               final int expectedMatches, final Condition condition) {
           this.isInclude = isInclude;
           this.bundler = bundler;
           this.fileName = fileName;
           this.pattern = pattern;
           this.expectedMatches = expectedMatches;
           this.condition = condition;
       }

       @Override
       public String toString() {
           return "TestCase{" +
                   "bundler=" + bundler +
                   ", fileName=" + fileName +
                   ", isInclude=" + isInclude +
                   ", pattern='" + pattern + '\'' +
                   ", expectedMatches=" + expectedMatches +
                   ", condition=" + condition +
                   '}';
       }
   }


    @BeforeClass
    public static void setup() throws Exception {
        prepareIfNecessary();
        folder = new FolderDataGen().nextPersisted();
        langId = APILocator.getLanguageAPI().getDefaultLanguage().getId();

    }
    @DataProvider
    public static Object[] getTestCases() throws Exception {


        return new Object[]{

                new TestCase(true,FileAssetBundler.class,
                "static-push-test-file.txt",null,1, Condition.EQ),
                new TestCase(true,FileAssetBundler.class,
                "test file blank.txt",null, 1, Condition.EQ),
                new TestCase(true,FileAssetBundler.class,
                "test-file.txt", "/*", 1, Condition.GTE),

                new TestCase(true,HTMLPageAsContentBundler.class,
                "static-push-test-page.htm", null,1, Condition.EQ),
                new TestCase(true,HTMLPageAsContentBundler.class,
                "test page blank.htm",null, 1, Condition.EQ),
                new TestCase(true,HTMLPageAsContentBundler.class,
                "test page blank.htm", "/*", 1, Condition.GTE),


                new TestCase(false,FileAssetBundler.class,
                        "any.htm","/*",0, Condition.GTE),

                new TestCase(false,HTMLPageAsContentBundler.class,
                        "any.htm","/*",0, Condition.GTE),


        };
    }


    private static void prepareIfNecessary() throws Exception {
        if(!prepared.getAndSet(true)) {
            IntegrationTestInitService.getInstance().init();
            LicenseTestUtil.getLicense();
        }
    }


    /**
     * Given Scenario: We feed StaticPublisher with one of these two Bundlers (HTMLPageAsContentBundler, FileAssetBundler) and a set of contentlets
     * The combination of pattern and asset-name renders different results.
     * Before when using blank spaces within an asset or page name. The query would render several inappropriate matches when in reality it only had to return 1 match
     * See https://github.com/dotCMS/core/issues/20295
     * Expected Result: We expect Bundler to internally build a query and return matching results.
     * Queries involving names with spaces will be enclosed within double-quotes. But not for regular names
     *
     * @param testCase
     * @throws Exception
     */
    @Test
    @UseDataProvider("getTestCases")
    public void Test_Static_Publish_Bundle_Generate(final TestCase testCase)
            throws Exception {

        Contentlet contentlet = null;
        if (testCase.bundler.equals(HTMLPageAsContentBundler.class)) {
            Logger.info(StaticPushPublishBundleGeneratorTest.class, "Testing HTMLPageAsContentBundler");
            contentlet = newHtmlPage(folder, testCase.fileName, langId);

        } else {
            Logger.info(StaticPushPublishBundleGeneratorTest.class, "Testing FileAssetBundler");
            contentlet = newFileAsset(folder, testCase.fileName, langId);
        }


        final String contentletIdentifier = contentlet.getIdentifier();
        Logger.info(StaticPushPublishBundleGeneratorTest.class, "Identifier: " + contentletIdentifier);
        final Identifier identifier = APILocator.getIdentifierAPI().find(contentletIdentifier);
        Logger.info(StaticPushPublishBundleGeneratorTest.class, "Path is: " + identifier.getPath());

        final User systemUser = APILocator.getUserAPI().getSystemUser();

        //Create bundle with DefaultFilter
        final Bundle bundleWithDefaultFilter = createBundle(
                "TestBundle" + System.currentTimeMillis(), false, defaultFilterKey);
        //Add assets to the bundle

        final List<String> identifiers = ImmutableList.of(contentletIdentifier);
        final PublisherAPI publisherAPI = PublisherAPI.getInstance();
        publisherAPI.saveBundleAssets(identifiers, bundleWithDefaultFilter.getId(), systemUser);

        //Generate Bundle, will return several dependencySet with the assets that will be added to the bundle
        String pattern = testCase.pattern==null ? APILocator.getIdentifierAPI().find(contentletIdentifier).getPath() : testCase.pattern;
        String excludePattern = testCase.isInclude ? null : pattern;
        String includePattern = testCase.isInclude ? pattern : null;

        final BundlerStatus status = generateBundle(systemUser, bundleWithDefaultFilter.getId(), testCase.bundler, includePattern, excludePattern);

            switch (testCase.condition){
                case EQ : {
                    assertEquals(testCase.expectedMatches, status.getCount());
                    break;
                }
                case GT : {
                    assertTrue(status.getCount() > testCase.expectedMatches );
                    break;
                }
                case LT : {
                    assertTrue(status.getCount() < testCase.expectedMatches );
                    break;
                }
                case GTE:  {
                    assertTrue( status.getCount() >= testCase.expectedMatches);
                    break;
                }
                case LTE:  {
                    assertTrue(status.getCount() <= testCase.expectedMatches );
                    break;
                }
            }

    }

    /**
     * Given Scenario: A FileAsset with an uppercase extension is created and published
     * Expected Result: It should be possible to generate a bundle with the file asset without errors
     */
    @Test
    public void Test_Static_Publish_File_With_Uppercase_Extension()
            throws Exception {

        Contentlet contentlet = FileAssetDataGen.createFileAsset(folder, "example", ".PNG");
        ContentletDataGen.publish(contentlet);


        final String contentletIdentifier = contentlet.getIdentifier();

        final User systemUser = APILocator.getUserAPI().getSystemUser();

        //Create bundle with DefaultFilter
        final Bundle bundleWithDefaultFilter = createBundle(
                "TestBundle" + System.currentTimeMillis(), false, defaultFilterKey);
        //Add assets to the bundle

        final List<String> identifiers = ImmutableList.of(contentletIdentifier);
        final PublisherAPI publisherAPI = PublisherAPI.getInstance();
        publisherAPI.saveBundleAssets(identifiers, bundleWithDefaultFilter.getId(), systemUser);


        generateBundle(systemUser, bundleWithDefaultFilter.getId(), FileAssetBundler.class, null, null);


    }

    private static BundlerStatus generateBundle(final User user, final String bundleId, Class<? extends IBundler> bundleGenerator, final String includePatterns, final String excludePatterns) throws Exception{
        final PushPublisherConfig publisherConfig = new PushPublisherConfig();
        final PublisherAPI publisherAPI = PublisherAPI.getInstance();

        final List<PublishQueueElement> tempBundleContents = publisherAPI
                .getQueueElementsByBundleId(bundleId);

        final List<PublishQueueElement> assetsToPublish = new ArrayList<>(tempBundleContents);

        publisherConfig.setDownloading(true);
        publisherConfig.setOperation(Operation.PUBLISH);

        publisherConfig.setAssets(assetsToPublish);
        //Queries creation
        publisherConfig.setLuceneQueries(PublisherUtil.prepareQueries(tempBundleContents));
        publisherConfig.setId(bundleId);
        publisherConfig.setUser(user);
        if (null != includePatterns) {
            publisherConfig.setIncludePatterns(ImmutableList.of(includePatterns));
        }
        if (null != excludePatterns) {
            publisherConfig.setExcludePatterns(ImmutableList.of(excludePatterns));
        }

        final File bundleRoot = BundlerUtil.getBundleRoot(publisherConfig);
        final DirectoryBundleOutput directoryBundleOutput = new DirectoryBundleOutput(publisherConfig, bundleRoot);

        final Publisher publisher = new StaticPublisher();
        publisher.init(publisherConfig);

        final IBundler instance = bundleGenerator.getDeclaredConstructor().newInstance();

        instance.setConfig(publisherConfig);
        instance.setPublisher(publisher);
        final BundlerStatus bundlerStatus = new BundlerStatus(instance.getClass().getName());
        //Fire bundle generation
        instance.generate(directoryBundleOutput, bundlerStatus);
        return bundlerStatus;
    }

    private static Contentlet newFileAsset(final Folder folder, final String fileName, final long languageId) throws Exception {

        final ContentletDataGen fileAssetDataGen = new FileAssetDataGen(folder,
                newTestFile(fileName))
                .languageId(languageId);

        return ContentletDataGen.publish(fileAssetDataGen.nextPersisted());
    }

    private static Contentlet newHtmlPage(final Folder folder, final String pageName,
            final long languageId) throws Exception {
        final Template template = new TemplateDataGen().nextPersisted();
        APILocator.getVersionableAPI().setLive(template);
        HTMLPageAsset dummyPage = new HTMLPageDataGen(folder, template).friendlyName(pageName)
                .pageURL(pageName + System.currentTimeMillis()).languageId(languageId)
                .title(pageName).nextPersisted();
        return HTMLPageDataGen.publish(dummyPage);
    }

    private static File newTestFile(String fileName) throws Exception {
        final String extension = UtilMethods.getFileExtension(fileName);
        final File testImage = new File(Files.createTempDir(), String.format("%s_%s.%s",fileName,System.currentTimeMillis(),extension));
        try (FileOutputStream output = new FileOutputStream(testImage, true)) {
            output.write(RandomStringUtils.randomAlphanumeric(100).getBytes());
        }
        return testImage;
    }

}
