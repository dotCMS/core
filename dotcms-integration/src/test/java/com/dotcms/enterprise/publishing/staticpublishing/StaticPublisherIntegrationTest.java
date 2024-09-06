package com.dotcms.enterprise.publishing.staticpublishing;

import com.dotcms.datagen.BundleDataGen;
import com.dotcms.enterprise.publishing.staticpublishing.StaticPublisherIntegrationTestHelper.FileExpected;
import com.dotcms.enterprise.publishing.staticpublishing.StaticPublisherIntegrationTestHelper.TestCase;
import com.dotcms.publisher.bundle.bean.Bundle;
import com.dotcms.publisher.business.DotPublisherException;
import com.dotcms.publisher.business.PublishAuditAPI;
import com.dotcms.publisher.business.PublishAuditHistory;
import com.dotcms.publisher.business.PublishAuditStatus;
import com.dotcms.publisher.pusher.PushPublisherConfig;
import com.dotcms.publishing.BundlerUtil;
import com.dotcms.publishing.DotPublishingException;
import com.dotcms.publishing.PublishStatus;
import com.dotcms.publishing.Publisher;
import com.dotcms.publishing.PublisherAPIImpl;
import com.dotcms.publishing.PublisherConfig;
import com.dotcms.publishing.PublisherConfig.Operation;
import com.dotcms.test.util.FileTestUtil;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.exception.WebAssetException;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.util.UtilMethods;
import com.liferay.util.FileUtil;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.dotcms.enterprise.publishing.bundlers.HTMLPageAsContentBundler.HTMLPAGE_ASSET_EXTENSION;
import static com.dotcms.enterprise.publishing.staticpublishing.StaticPublisherIntegrationTestHelper.getContentTypeWithURlMap;
import static com.dotcms.enterprise.publishing.staticpublishing.StaticPublisherIntegrationTestHelper.getFolderWithLiveFileAssetAndPage;
import static com.dotcms.enterprise.publishing.staticpublishing.StaticPublisherIntegrationTestHelper.getHostWithLiveFileAssetAndPage;
import static com.dotcms.enterprise.publishing.staticpublishing.StaticPublisherIntegrationTestHelper.getLiveContentWithURlMap;
import static com.dotcms.enterprise.publishing.staticpublishing.StaticPublisherIntegrationTestHelper.getLiveFileAsset;
import static com.dotcms.enterprise.publishing.staticpublishing.StaticPublisherIntegrationTestHelper.getLiveFileAssetDifferentLang;
import static com.dotcms.enterprise.publishing.staticpublishing.StaticPublisherIntegrationTestHelper.getLiveFileAssetDifferentLangIncludingJustOneg;
import static com.dotcms.enterprise.publishing.staticpublishing.StaticPublisherIntegrationTestHelper.getLivePage;
import static com.dotcms.enterprise.publishing.staticpublishing.StaticPublisherIntegrationTestHelper.getLivePageWithDifferentLang;
import static com.dotcms.enterprise.publishing.staticpublishing.StaticPublisherIntegrationTestHelper.getLivePageWithDifferentLangIncludingJustOne;
import static com.dotcms.enterprise.publishing.staticpublishing.StaticPublisherIntegrationTestHelper.getPageWithCSS;
import static com.dotcms.enterprise.publishing.staticpublishing.StaticPublisherIntegrationTestHelper.getPageWithImage;
import static com.dotcms.enterprise.publishing.staticpublishing.StaticPublisherIntegrationTestHelper.getTwoPageDifferentHostSamePath;
import static com.dotcms.enterprise.publishing.staticpublishing.StaticPublisherIntegrationTestHelper.getURLMapPageWithImage;
import static com.dotcms.enterprise.publishing.staticpublishing.StaticPublisherIntegrationTestHelper.getWorkingContentWithURlMap;
import static com.dotcms.enterprise.publishing.staticpublishing.StaticPublisherIntegrationTestHelper.getWorkingFileAsset;
import static com.dotcms.enterprise.publishing.staticpublishing.StaticPublisherIntegrationTestHelper.getWorkingPage;
import static com.dotcms.util.CollectionsUtils.list;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;

@RunWith(DataProviderRunner.class)
public class StaticPublisherIntegrationTest {

    public static final String BUNDLE_METADA_FILE_NAME = "bundle.xml";
    

    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
    }

    /**
     * Cases:
     * - Add to the bundle a Live Page with no dependencies, Should create two files no matter the bundle operation:
     *   a xml file with the page properties, and no ext files with the html code
     * - Add to the bundle a Working page no dependencies should not create any files because just
     *   the LIVE page are include in a static bundle.
     * - Add to the bundle a Live Page with versions in two different lang, should create 4 files no matter the Bundle's Operation: a xml and a html for each page version.
     * - Add a Host to the bundle, this Host has:
     *      - Two Folders.
     *      - Two page in each folders, The first one LIVE and the second WORKING
     *      - Two File Asset for each folder, The first one LIVE and the second WORKING
     *      Should include into the bundle two files for each LIVE pages: xml and html files for each one
     *      Also should include one file for each File Asset with the content
     *      No matter the Bundle's Operation
     *  - Add into the bundle a Folder with:
     *      - Two pages, The first one LIVE and the second WORKING
     *      - Two Files Asset, The first one LIVE and the second WORKING
     *      Should include into the bundle two files for LIVE page: xml and html files for each one
     *      Also should include one file for the LIVE File Asset with the content
     *      No matter the Bundle's Operation
     * - Add into the bundle one LIVE FIle Asset, Should include one file into the bundle with the file's content no matter the Bundle's Operation
     * - Add into the bundle a WORKING File Asset, should not include any files in the bundle.
     * - Add a FileAsset with version in two different languages, Should include two files one for each languages
     * - Add into the bundle a LIVE page meanwhile there is anther page in a different hist with the same path
     * should include two files (just to the page that was add into the bundle directly): a xml file with the page property and another file with the page's content
     * No matter the Bundle's Operation
     * - Add a ContentType with an url Map into the bundle but there is not any contentlet from this ContentType, Should not generate any files into the bundle, No matter the Bundle's Operation
     * - Add into the bundle a WORKING Contentlet that is from a ContenTType with a URL MAP, should not generate any files into the bundle
     * - Add into the bundle a LIVE Contentlet that is from a ContenTType with a URL MAP, should generate two files into the bundle: a xml file with the COntentlet's properties and another file with the html content
     * - Create a File Image Contentlet and Add into the bundle a LIVE PAge with a Widget with the follow code:
     *  <code><img src="/dA/[File Image contentlet's ID]" style="width:33px;" class="img-circles border mr-2"></code>
     *  Should generate into the bundle three files when the Operation is equals to PUBLISH: two for the page (xml and page's html content) and another one for the Image
     *  Should generate into the bundle two files when the Operation is equals to UN_PUBLISH: just the page's files
     * - Create a Page with a Image (like the previous case), and Create a ContentType with the page as detail page, finally create a LIVE Contentlet and add it into the Bundle
     * Should create: thre file when the Operation is PUBLISH and two files when the operation is UN_PUBLISH (liek the previous case)
     * - Create a CSS File Asset, and Add into the bundle a LIVE PAge with a Widget with the follow code:
     *   <code><link rel="preload" as="style" href="[File asset's PATH]"></code>
     *  Should generate into the bundle three files when the Operation is equals to PUBLISH: two for the page (xml and page's html content) and another one for the CSS file
     *  Should generate into the bundle two files when the Operation is equals to UN_PUBLISH: just the page's files
     * - Add into the bundle a LIVE Page with version in two different languages but Add into the bundle just one of the lang: should include just two files for the page's version in the lang included
     * - Add into the bundle a LIVE FileAsset with version in two different languages but Add into the bundle just one of the lang: should include just one file for File's version in the lang included
     *
     * @return
     * @throws Exception
     */
    @DataProvider
    public static Object[] assets() throws Exception {
        prepare();

        final TestCase[] testCasesWithoutLangFilter = {
                getLivePage(),
                getWorkingPage(),
                getLivePageWithDifferentLang(),
                getHostWithLiveFileAssetAndPage(),
                getFolderWithLiveFileAssetAndPage(),
                getLiveFileAsset(),
                getWorkingFileAsset(),
                getLiveFileAssetDifferentLang(),
                getTwoPageDifferentHostSamePath(),
                getContentTypeWithURlMap(),
                getWorkingContentWithURlMap(),
                getLiveContentWithURlMap(),
                getPageWithImage(),
                getURLMapPageWithImage(),
                getPageWithCSS()
        };

       final TestCase[] testCasesWitLangFilter = {
               getLivePageWithDifferentLangIncludingJustOne(),
                getLiveFileAssetDifferentLangIncludingJustOneg()
        };

       final List<TestCase> testCaseWithEmptyLang = Arrays.stream(testCasesWithoutLangFilter)
                .map(testCase -> {
                    testCase.languages = Collections.emptyList();
                    return testCase;
                })
                .collect(Collectors.toList());

        return Stream.concat(
                    Stream.concat(
                        Stream.of(testCasesWitLangFilter),
                        Stream.of(testCasesWithoutLangFilter)
                    ),
                    testCaseWithEmptyLang.stream()
                ).toArray();
    }

    /**
     * Method to Test: {@link PublisherAPIImpl#publish(PublisherConfig)}
     * When: A {@link PushPublisherConfig}  is created with:
     * - A {@link StaticPublisher} setting in {@link PushPublisherConfig#setPublishers(List)}
     * - A Bundle with the assets in {@link TestCase#addToBundle}
     * - And all the languages in {@link TestCase#languages} are set in {@link PublisherConfig#setLanguages(Set)}}
     * should: Create a static bundle with all the files in {@link TestCase#filesExpected}
     *
     * @param testCase
     * @throws DotPublishingException
     * @throws DotPublisherException
     * @throws DotDataException
     * @throws IOException
     * @throws WebAssetException
     * @throws DotSecurityException
     */
    @Test
    @UseDataProvider("assets")
    public void createStaticBundle(final TestCase testCase)
            throws DotPublishingException, DotPublisherException, DotDataException, IOException, WebAssetException, DotSecurityException {

        final Class<? extends Publisher> publisher = StaticPublisher.class;

        final PublisherAPIImpl publisherAPI = new PublisherAPIImpl();

        final PushPublisherConfig config = new PushPublisherConfig();
        config.setPublishers(list(publisher));
        config.setOperation(PublisherConfig.Operation.PUBLISH);
        config.setLuceneQueries(list());
        config.setId("StaticPublisher" + System.currentTimeMillis());
        config.setStatic(true);

        config.setLanguages(
                testCase.languages.stream()
                        .map(language -> String.valueOf(language.getId()))
                        .collect(Collectors.toSet())
        );

        final Bundle bundle = new BundleDataGen()
                .pushPublisherConfig(config)
                .addAssets(list(testCase.addToBundle))
                .nextPersisted();

        final PublishAuditStatus status = new PublishAuditStatus(bundle.getId());

        final PublishAuditHistory historyPojo = new PublishAuditHistory();
        historyPojo.setAssets(testCase.assetsMap);
        status.setStatusPojo(historyPojo);
        PublishAuditAPI.getInstance().insertPublishAuditStatus(status);

        final PublishStatus publish = publisherAPI.publish(config);

        final File bundleRoot = BundlerUtil.getBundleRoot(config);
        final List<File> files = FileUtil.listFilesRecursively(bundleRoot)
                .stream()
                .filter(file -> !file.getName().equals(BUNDLE_METADA_FILE_NAME))
                .filter(file -> file.isFile())
                        .collect(Collectors.toList());


        assertEquals(testCase.filesExpected.size(), files.size());

        final File bundleXMLFile = new File(bundleRoot, BUNDLE_METADA_FILE_NAME);
        assertTrue(bundleXMLFile.exists());

        for (File file : files) {
            assertFile(testCase, file);
        }
    }

    private void assertFile(TestCase testCase, File file) throws IOException {
        final Optional<FileExpected> fileExpectedOptional = testCase.getFileExpected(
                file.getAbsolutePath());

        if (fileExpectedOptional.isEmpty()) {
            throw new AssertionError(String.format("File %s Expected", file.getAbsolutePath()));
        }

        final FileExpected fileExpected = fileExpectedOptional.get();

        if (UtilMethods.isSet(fileExpected.content)) {
            if (String.class.isInstance(fileExpected.content)) {
                String fileContent = FileTestUtil.removeSpace(
                        FileTestUtil.getFileContent(file));

                if (file.getAbsolutePath().endsWith(HTMLPAGE_ASSET_EXTENSION)) {
                    fileContent = FileTestUtil.removeContent(fileContent, getXMLFileToRemove());
                }

                Assert.assertEquals(fileExpected.content, fileContent);
            } else {
                FileTestUtil.compare(file, (File) fileExpected.content);
            }
        }
    }


    /**
     * Method to Test: {@link PublisherAPIImpl#publish(PublisherConfig)}
     * When: A {@link PushPublisherConfig}  is created with:
     * - A {@link StaticPublisher} setting in {@link PushPublisherConfig#setPublishers(List)}
     * - A Bundle with the assets in {@link TestCase#addToBundle}
     * - All the languages in {@link TestCase#languages} are set in {@link PublisherConfig#setLanguages(Set)}}
     * - Setting {@link Operation#UNPUBLISH}
     * should: Create a static bundle with all the files in {@link TestCase#filesExpected}
     *
     * @param testCase
     * @throws DotPublishingException
     * @throws DotPublisherException
     * @throws DotDataException
     * @throws IOException
     * @throws WebAssetException
     * @throws DotSecurityException
     */
    @Test
    @UseDataProvider("assets")
    public void createStaticBundleWithUnPublishOperation(final TestCase testCase)
            throws DotPublishingException, DotPublisherException, IOException {

        final Class<? extends Publisher> publisher = StaticPublisher.class;

        final PublisherAPIImpl publisherAPI = new PublisherAPIImpl();

        final PushPublisherConfig config = new PushPublisherConfig();
        config.setPublishers(list(publisher));
        config.setOperation(Operation.UNPUBLISH);
        config.setLuceneQueries(list());
        config.setId("StaticPublisher" + System.currentTimeMillis());
        config.setStatic(true);

        config.setLanguages(
                testCase.languages.stream()
                        .map(language -> String.valueOf(language.getId()))
                        .collect(Collectors.toSet())
        );

        final Bundle bundle = new BundleDataGen()
                .operation(Operation.UNPUBLISH)
                .pushPublisherConfig(config)
                .addAssets(list(testCase.addToBundle))
                .nextPersisted();

        final PublishAuditStatus status = new PublishAuditStatus(bundle.getId());

        final PublishAuditHistory historyPojo = new PublishAuditHistory();
        historyPojo.setAssets(testCase.assetsMap);
        status.setStatusPojo(historyPojo);
        PublishAuditAPI.getInstance().insertPublishAuditStatus(status);

        final PublishStatus publish = publisherAPI.publish(config);

        final File bundleRoot = BundlerUtil.getBundleRoot(config);
        final List<File> files = FileUtil.listFilesRecursively(bundleRoot)
                .stream()
                .filter(file -> !file.getName().equals(BUNDLE_METADA_FILE_NAME))
                .filter(file -> file.isFile())
                .collect(Collectors.toList());

        final Collection<FileExpected> bundleFiles = testCase.getAddToBundleFiles();

        assertEquals(bundleFiles.size(), files.size());

        final File bundleXMLFile = new File(bundleRoot, BUNDLE_METADA_FILE_NAME);
        assertTrue(bundleXMLFile.exists());

        for (File file : files) {
            assertFile(testCase, file);
        }
    }

    private static String getXMLPageFileExpectedContent(final HTMLPageAsset page)
            throws IOException, DotDataException {

        final Identifier identifier = APILocator.getIdentifierAPI().find(page);

        final File expectedFile = FileTestUtil.getFileInResources(
                "/bundlers-test/page/page.html.xml");
        final Map<String, Object>  arguments = new HashMap<>();

        arguments.put("id", page.getIdentifier());
        arguments.put("inode", page.getInode());
        arguments.put("lang", page.getLanguageId());
        arguments.put("template", page.getTemplateId());
        arguments.put("folder_inode", page.getFolder());
        arguments.put("host", page.getHost());
        arguments.put("friendly_name", page.getFriendlyName());
        arguments.put("title", page.getTitle());
        arguments.put("url", page.getPageUrl());
        arguments.put("content_type_inode", page.getContentTypeId());
        arguments.put("asset_name", identifier.getAssetName());
        arguments.put("parent_path", identifier.getParentPath());

        final List<String> toRemove = getXMLFileToRemove();

        final String fileContentExpected = FileTestUtil.getFormattedContentWithoutSpace(
                expectedFile,
                arguments);
        return FileTestUtil.removeContent(fileContentExpected, toRemove);
    }

    private static List<String> getXMLFileToRemove() {
        return list(
                "<lockedOn class=\"sql-timestamp\">.*</lockedOn>",
                "<versionTs class=\"sql-timestamp\">.*</versionTs>",
                "<sql-timestamp>.*</sql-timestamp>",
                "<createDate class=\"sql-timestamp\">.*/createDate>",
                "<deleted>false</deleted>"
        );
    }
}
