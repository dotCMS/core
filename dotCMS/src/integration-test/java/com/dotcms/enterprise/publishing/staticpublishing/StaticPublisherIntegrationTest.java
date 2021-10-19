package com.dotcms.enterprise.publishing.staticpublishing;

import static com.dotcms.enterprise.publishing.staticpublishing.StaticPublisherIntegrationTestHelper.getContentTypeWithURlMap;
import static com.dotcms.enterprise.publishing.staticpublishing.StaticPublisherIntegrationTestHelper.getDeletedContentWithURlMap;
import static com.dotcms.enterprise.publishing.staticpublishing.StaticPublisherIntegrationTestHelper.getFolderWithLiveFileAssetAndPage;
import static com.dotcms.enterprise.publishing.staticpublishing.StaticPublisherIntegrationTestHelper.getHostWithLiveFileAssetAndPage;
import static com.dotcms.enterprise.publishing.staticpublishing.StaticPublisherIntegrationTestHelper.getLiveContentWithURlMap;
import static com.dotcms.enterprise.publishing.staticpublishing.StaticPublisherIntegrationTestHelper.getLiveFileAsset;
import static com.dotcms.enterprise.publishing.staticpublishing.StaticPublisherIntegrationTestHelper.getLiveFileAssetDifferentLang;
import static com.dotcms.enterprise.publishing.staticpublishing.StaticPublisherIntegrationTestHelper.getLiveFileAssetDifferentLangIncludingJustOneg;
import static com.dotcms.enterprise.publishing.staticpublishing.StaticPublisherIntegrationTestHelper.getLivePage;
import static com.dotcms.enterprise.publishing.staticpublishing.StaticPublisherIntegrationTestHelper.getLivePageWithDifferentLang;
import static com.dotcms.enterprise.publishing.staticpublishing.StaticPublisherIntegrationTestHelper.getLivePageWithDifferentLangIncludingJustOne;
import static com.dotcms.enterprise.publishing.staticpublishing.StaticPublisherIntegrationTestHelper.getPageWithImage;
import static com.dotcms.enterprise.publishing.staticpublishing.StaticPublisherIntegrationTestHelper.getTwoPageDifferentHostSamePath;

import static com.dotcms.enterprise.publishing.staticpublishing.StaticPublisherIntegrationTestHelper.getURLMapPageWithImage;
import static com.dotcms.enterprise.publishing.staticpublishing.StaticPublisherIntegrationTestHelper.getWorkingContentWithURlMap;
import static com.dotcms.enterprise.publishing.staticpublishing.StaticPublisherIntegrationTestHelper.getWorkingFileAsset;
import static com.dotcms.enterprise.publishing.staticpublishing.StaticPublisherIntegrationTestHelper.getWorkingPage;
import static com.dotcms.util.CollectionsUtils.list;
import static com.dotcms.util.CollectionsUtils.map;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;

import com.dotcms.datagen.BundleDataGen;
import com.dotcms.datagen.FileAssetDataGen;
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
import com.dotcms.test.util.FileTestUtil;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.exception.WebAssetException;
import com.dotmarketing.image.focalpoint.FocalPointAPITest;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.util.UtilMethods;
import com.liferay.util.FileUtil;
import com.liferay.util.FileUtilTest;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(DataProviderRunner.class)
public class StaticPublisherIntegrationTest {

    public static final String BUNDLE_METADA_FILE_NAME = "bundle.xml";
    

    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
    }

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
                getURLMapPageWithImage()
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

            final Optional<FileExpected> fileExpectedOptional = testCase.getFileExpected(
                    file.getAbsolutePath());

            if (!fileExpectedOptional.isPresent()) {
                throw new AssertionError(String.format("File %s Expected", file.getAbsolutePath()));
            }

            final FileExpected fileExpected = fileExpectedOptional.get();

            if (UtilMethods.isSet(fileExpected.content)) {
                if (String.class.isInstance(fileExpected.content)) {
                    String fileContent = FileTestUtil.removeSpace(
                            FileTestUtil.getFileContent(file));
                    fileContent = FileTestUtil.removeContent(fileContent, getXMLFileToRemove());

                    Assert.assertEquals(fileExpected.content, fileContent);
                } else {
                    FileTestUtil.compare(file, (File) fileExpected.content);
                }
            }
        }
    }

    private static String getXMLPageFileExpectedContent(final HTMLPageAsset page)
            throws IOException, DotDataException {

        final Identifier identifier = APILocator.getIdentifierAPI().find(page);

        final File expectedFile = FileTestUtil.getFileInResources(
                "/bundlers-test/page/page.html.xml");
        final Map<String, Object>  arguments = map(
                "id", page.getIdentifier(),
                "inode", page.getInode(),
                "lang", page.getLanguageId(),
                "template", page.getTemplateId(),
                "folder_inode", page.getFolder(),
                "host", page.getHost(),
                "friendly_name", page.getFriendlyName(),
                "title", page.getTitle(),
                "url", page.getPageUrl(),
                "content_type_inode", page.getContentTypeId(),
                "asset_name", identifier.getAssetName()
        );

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
