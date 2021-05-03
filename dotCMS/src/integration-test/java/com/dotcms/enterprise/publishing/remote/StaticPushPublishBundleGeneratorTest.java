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
import com.dotcms.enterprise.publishing.bundlers.HTMLPageAsContentBundler;
import com.dotcms.enterprise.publishing.remote.StaticPushPublishBundleGeneratorTest.TestCase.Condition;
import com.dotcms.enterprise.publishing.staticpublishing.StaticPublisher;
import com.dotcms.publisher.bundle.bean.Bundle;
import com.dotcms.publisher.business.PublishAuditAPI;
import com.dotcms.publisher.business.PublishQueueElement;
import com.dotcms.publisher.business.PublisherAPI;
import com.dotcms.publisher.pusher.PushPublisherConfig;
import com.dotcms.publisher.util.PublisherUtil;
import com.dotcms.publishing.BundlerStatus;
import com.dotcms.publishing.BundlerUtil;
import com.dotcms.publishing.IBundler;
import com.dotcms.publishing.Publisher;
import com.dotcms.publishing.PublisherConfig.Operation;
import com.dotcms.publishing.output.DirectoryBundleOutput;
import com.dotcms.util.IntegrationTestInitService;
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
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.commons.lang.RandomStringUtils;
import org.junit.Test;
import org.junit.runner.RunWith;


@RunWith(DataProviderRunner.class)
public class StaticPushPublishBundleGeneratorTest extends IntegrationTestBase {

   private static AtomicBoolean prepared = new AtomicBoolean(false);

   static class TestCase{

       enum Condition {
           EQ, GT, GTE, LT,LTE
       }

      final Class<? extends IBundler> bundler;
      final Contentlet contentlet;
      final String includePattern;
      final String excludePattern;
      final int expectedMatches;
      final Condition condition;

       public TestCase(final Class<? extends IBundler> bundler,
               final Contentlet contentlet, final String includePattern,final String excludePattern,
               final int expectedMatches, final Condition condition) {
           this.bundler = bundler;
           this.contentlet = contentlet;
           this.includePattern = includePattern;
           this.excludePattern = excludePattern;
           this.expectedMatches = expectedMatches;
           this.condition = condition;
       }

   }


    private static TestCase fileAssetInclusivePatternTest(final Folder folder, final String fileName,
            final long langId, final int expectedMatches, final Condition condition) throws Exception {
        return fileAssetInclusivePatternTest(folder, fileName, langId, null, expectedMatches, condition
        );
    }

    private static TestCase fileAssetInclusivePatternTest(final Folder folder, final String fileName,
            long langId, final String pattern, final int expectedMatches,
            final Condition condition)
            throws Exception {

        final Contentlet fileAsset = newFileAsset(folder, fileName, langId);
        final String path =
                pattern == null ? APILocator.getIdentifierAPI().find(fileAsset.getIdentifier())
                        .getPath() : pattern;

        return new TestCase(FileAssetBundler.class, fileAsset, path, null, expectedMatches, condition);
    }


    private static TestCase fileAssetExclusivePatternTest(final Folder folder, final String fileName,
            final long langId, final int expectedMatches, final Condition condition) throws Exception {
        return fileAssetExclusivePatternTest(folder, fileName, langId, null, expectedMatches, condition
        );
    }

    private static TestCase fileAssetExclusivePatternTest(final Folder folder, final String fileName,
            long langId, final String pattern, final int expectedMatches,
            final Condition condition)
            throws Exception {

        final Contentlet fileAsset = newFileAsset(folder, fileName, langId);
        final String path =
                pattern == null ? APILocator.getIdentifierAPI().find(fileAsset.getIdentifier())
                        .getPath() : pattern;

        return new TestCase(FileAssetBundler.class, fileAsset, null, path, expectedMatches, condition);
    }

    private static TestCase htmlPageInclusivePatternTest(final Folder folder, final String pageName,
            long langId, final int expectedMatches, final Condition condition)
            throws Exception{
        return htmlPageInclusivePatternTest(folder, pageName, langId, null, expectedMatches, condition);
    }


    private static TestCase htmlPageExclusivePatternTest(final Folder folder, final String pageName,
            long langId, final int expectedMatches, final Condition condition)
            throws Exception{
        return htmlPageExclusivePatternTest(folder, pageName, langId, null, expectedMatches, condition);
    }

    private static TestCase htmlPageInclusivePatternTest(final Folder folder, final String pageName,
            final long langId, final String pattern, final int expectedMatches,
            final Condition condition)
            throws Exception {

        final Contentlet htmlPage = newHtmlPage(folder, pageName, langId);
        final String path =
                pattern == null ? APILocator.getIdentifierAPI().find(htmlPage.getIdentifier())
                        .getPath() : pattern;

        return new TestCase(HTMLPageAsContentBundler.class, htmlPage, path, null, expectedMatches, condition);
    }

    private static TestCase htmlPageExclusivePatternTest(final Folder folder, final String pageName,
            final long langId, final String pattern, final int expectedMatches,
            final Condition condition)
            throws Exception {

        final Contentlet htmlPage = newHtmlPage(folder, pageName, langId);
        final String excludePattern =
                pattern == null ? APILocator.getIdentifierAPI().find(htmlPage.getIdentifier())
                        .getPath() : pattern;


        return new TestCase(HTMLPageAsContentBundler.class, htmlPage, null, excludePattern, expectedMatches, condition);
    }


    @DataProvider
    public static Object[] getTestCases() throws Exception {
        prepareIfNecessary();

        final Folder folder = new FolderDataGen().nextPersisted();
        final long langId = APILocator.getLanguageAPI().getDefaultLanguage().getId();

        return new Object[]{

                fileAssetInclusivePatternTest(folder,
                "test-file.txt", langId, 1, Condition.EQ),
                fileAssetInclusivePatternTest(folder,
                "test file blank.txt", langId, 1, Condition.EQ),
                fileAssetInclusivePatternTest(folder,
                "test-file.txt", langId, "/*", 1, Condition.GTE),

                htmlPageInclusivePatternTest(folder,
                "test-page.htm", langId, 1, Condition.EQ),
                htmlPageInclusivePatternTest(folder,
                "test page blank.htm", langId, 1, Condition.EQ),
                htmlPageInclusivePatternTest(folder,
                "test page blank.htm", langId, "/*", 1, Condition.GTE),


                fileAssetExclusivePatternTest(folder,
                        "any.htm", langId, "/*",0, Condition.GTE),

                htmlPageExclusivePatternTest(folder,
                        "any.htm", langId, "/*",0, Condition.GTE),


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

        final String contentletIdentifier = testCase.contentlet.getIdentifier();
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
        final BundlerStatus status = generateBundle(systemUser, bundleWithDefaultFilter.getId(), testCase.bundler, testCase.includePattern, testCase.excludePattern);

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

        final Publisher publisher = new StaticPublisher(
                PublishAuditAPI.getInstance(),
                APILocator.getEnvironmentAPI(),
                APILocator.getPublisherEndPointAPI(),
                APILocator.getPushedAssetsAPI(),
                PublisherAPI.getInstance(),
                APILocator.getLocalSystemEventsAPI(),
                ()->true
        );
        publisher.init(publisherConfig);

        final IBundler instance = bundleGenerator.newInstance();

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
                .pageURL(pageName).languageId(languageId)
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
