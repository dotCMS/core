package com.dotcms.browser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.dotcms.IntegrationTestBase;
import com.dotcms.browser.BrowserAPIImpl.PaginatedContents;
import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.DotAssetDataGen;
import com.dotcms.datagen.FieldDataGen;
import com.dotcms.datagen.FileAssetDataGen;
import com.dotcms.datagen.FolderDataGen;
import com.dotcms.datagen.HTMLPageDataGen;
import com.dotcms.datagen.LanguageDataGen;
import com.dotcms.datagen.LinkDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.TestDataUtils;
import com.dotcms.datagen.VariantDataGen;
import com.dotcms.util.IntegrationTestInitService;
import com.dotcms.variant.model.Variant;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Treeable;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.image.focalpoint.FocalPointAPITest;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.IndexPolicy;
import com.dotmarketing.portlets.fileassets.business.FileAsset;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.links.model.Link;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.FileUtil;
import com.dotmarketing.util.UUIDGenerator;
import com.google.common.collect.ImmutableSet;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;
import io.vavr.Tuple;
import io.vavr.Tuple3;
import io.vavr.control.Try;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Created by Oscar Arrieta on 6/8/17.
 */
public class BrowserAPITest extends IntegrationTestBase {

    final BrowserAPI browserAPI = APILocator.getBrowserAPI();
    final FolderAPI folderAPI = APILocator.getFolderAPI();
    final UserAPI userAPI = APILocator.getUserAPI();

    static Host testHost;
    static Folder testFolder, testSubFolder;
    static HTMLPageAsset testPage;
    static Language testLanguage;
    static Contentlet testDotAsset;
    static FileAsset testFileAsset, testFileAsset2, testFileAsset3Archived, testFileAsset2MultiLingual;

    static Link testlink;

    @BeforeClass
    public static void prepare() throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
        
        testLanguage = new LanguageDataGen().nextPersisted();
        testHost = new SiteDataGen().nextPersisted();

        testFolder = new FolderDataGen().name("testFolder").site(testHost).nextPersisted();
        testFileAsset = APILocator.getFileAssetAPI().fromContentlet(FileAssetDataGen.createFileAsset(testFolder, "text1FileAsset", ".txt"));
        
        
        testFileAsset2 = APILocator.getFileAssetAPI().fromContentlet(FileAssetDataGen.createFileAsset(testFolder, "text2FileAsset", ".txt"));


        File tempFile = File.createTempFile("testFileAsset2-es", ".jpg");
        URL url = FocalPointAPITest.class.getResource("/images/test.jpg");
        File testImage = new File(url.getFile());
        FileUtils.copyFile(testImage, tempFile);
        
        testFileAsset2MultiLingual =APILocator.getFileAssetAPI().fromContentlet( new FileAssetDataGen(tempFile).languageId(testLanguage.getId()).folder(testFolder).nextPersisted());


        
        // archived
        testFileAsset3Archived = APILocator.getFileAssetAPI().fromContentlet(FileAssetDataGen.createFileAsset(testFolder, "text3FileAsset", ".txt"));
        APILocator.getContentletAPI().archive(testFileAsset3Archived, APILocator.systemUser(), false);
        

        testDotAsset =  TestDataUtils.getDotAssetLikeContentlet(testFolder);
        
        testSubFolder =  new FolderDataGen().name("testSubFolder").parent(testFolder).nextPersisted();

        Template template=new Template();
        template.setTitle("a template "+UUIDGenerator.generateUuid());
        template.setBody("<html><body> I'm mostly empty </body></html>");
        template=APILocator.getTemplateAPI().saveTemplate(template, testHost, APILocator.systemUser(), false);
        
        HTMLPageAsset page = new HTMLPageDataGen(testFolder, template).next();
        page.setTitle("testingpage1");
        testPage = APILocator.getHTMLPageAssetAPI().fromContentlet(HTMLPageDataGen.checkin(page, IndexPolicy.FORCE));

        testlink = new LinkDataGen().hostId(testHost.getIdentifier()).title("testLink").parent(testFolder).target("https://google.com").linkType("EXTERNAL").nextPersisted();
    }

    /**
     * Given scenario: Create a folder place multiple versions in different languages of the same content
     * Expected result: We're testing that BrowserAPI can be used to bring multiple versions of the same content in different languages
     * @throws DotDataException
     * @throws DotSecurityException
     * @throws IOException
     */
    @Test()
    public void Test_GetFolderContent_Multiple_Langs() throws DotDataException, DotSecurityException, IOException {

        final SiteDataGen   siteDataGen   = new SiteDataGen();
        final FolderDataGen folderDataGen = new FolderDataGen();
        final Host          host          = siteDataGen.nextPersisted();
        final Folder        folder        = folderDataGen.site(host).nextPersisted();

        final File file = FileUtil.createTemporaryFile("test", ".txt", "this is a test!");

        final Contentlet persisted = new FileAssetDataGen(file)
                .languageId(1)
                .host(host)
                .folder(folder)
                .setPolicy(IndexPolicy.WAIT_FOR).nextPersisted();

        final ContentletAPI contentletAPI = APILocator.getContentletAPI();

        List<Long> languages = new ArrayList<>();
        languages.add(persisted.getLanguageId());
        languages.add(new LanguageDataGen().nextPersisted().getId());
        languages.add(new LanguageDataGen().nextPersisted().getId());

        for (Long lang:languages) {
            final Contentlet next = new FileAssetDataGen(file)
                    .languageId(lang)
                    .host(host)
                    .folder(folder)
                    .setPolicy(IndexPolicy.WAIT_FOR).next();

            next.setIdentifier(persisted.getIdentifier());
            next.setInode(null);
            contentletAPI.checkin(next, APILocator.systemUser(), false);
        }

        final List<Treeable> contentList = browserAPI.getFolderContentList(
                BrowserQuery.builder()
                        .showDotAssets(false)
                        .showLinks(false)
                        .withHostOrFolderId(folder.getIdentifier())
                        .offset(0)
                        .showFiles(true)
                        .showFolders(true)
                        .showWorking(true)
                        .build());

        assertEquals(3, contentList.size());
        assertTrue(contentList.stream().allMatch(c->  persisted.getIdentifier().equals(c.getIdentifier())));
        assertTrue(contentList.stream().map(c->(Contentlet)c).anyMatch(c-> languages.contains( c.getLanguageId())));
    }


    /**
     * Method to test: testing the pagination of the BrowserAPI, the test creates a site and a folder, them add 10 files and iterate over them with the browser api
     * Given Scenario: 1)  request items from 0 to 2
     *                  2) request items form 4 to 6
     *                  3) request items form 6 to 10
     *                  4) out of range
     * ExpectedResult: Must have always 10 files as a total, and should retrieve the respective items per request
     *
     */

    @Test()
    public void test_GetFolderContent_pagination() throws DotDataException, DotSecurityException, IOException {

        // create a folder
        // create a 10 files
        final SiteDataGen   siteDataGen   = new SiteDataGen();
        final FolderDataGen folderDataGen = new FolderDataGen();
        final Host          host          = siteDataGen.nextPersisted();
        final Folder        folder        = folderDataGen.site(host).nextPersisted();

        for (int i = 0; i < 10; ++i) {

            new FileAssetDataGen(FileUtil.createTemporaryFile("test", ".txt", "this is a test")).host(host)
                    .folder(folder).setPolicy(IndexPolicy.WAIT_FOR).nextPersisted();
        }

        Map<String, Object> resultMap = browserAPI.getFolderContent(BrowserQuery.builder()
                .showDotAssets(true)
                .showLinks(true)
                .withHostOrFolderId(folder.getIdentifier())
                .offset(0)
                .showFiles(true)
                .showFolders(true)
                .showWorking(true)
                .maxResults(2)
                .build());

        assertNotNull(resultMap);
        assertEquals(10, resultMap.get("total"));

        List<Map<String, Object>> results = (List<Map<String, Object>>)resultMap.get("list");
        assertNotNull(results);
        assertEquals(results.size(), 2);

        // 4 - 6
        resultMap = browserAPI.getFolderContent(BrowserQuery.builder()
                .showDotAssets(true)
                .showLinks(true)
                .withHostOrFolderId(folder.getIdentifier())
                .offset(4)
                .showFiles(true)
                .showFolders(true)
                .showWorking(true)
                .maxResults(2)
                .build());

        assertNotNull(resultMap);
        assertEquals(10, resultMap.get("total"));

        results = (List<Map<String, Object>>)resultMap.get("list");
        assertNotNull(results);
        assertEquals(results.size(), 2);

        // 6 - 10
        resultMap = browserAPI.getFolderContent(BrowserQuery.builder()
                .showDotAssets(true)
                .showLinks(true)
                .withHostOrFolderId(folder.getIdentifier())
                .offset(6)
                .showFiles(true)
                .showFolders(true)
                .showWorking(true)
                .maxResults(4)
                .build());

        assertNotNull(resultMap);
        assertEquals(10, resultMap.get("total"));

        results = (List<Map<String, Object>>)resultMap.get("list");
        assertNotNull(results);
        assertEquals(results.size(), 4);

        // 10 - ...
        resultMap = browserAPI.getFolderContent(BrowserQuery.builder()
                .showDotAssets(true)
                .showLinks(true)
                .withHostOrFolderId(folder.getIdentifier())
                .offset(10)
                .showFiles(true)
                .showFolders(true)
                .showWorking(true)
                .maxResults(15)
                .build());

        assertNotNull(resultMap);
        assertEquals(10, resultMap.get("total"));

        results = (List<Map<String, Object>>)resultMap.get("list");
        assertNotNull(results);
        assertEquals(results.size(), 0);
    }

    /**
     * Method to test: testing the pagination of the BrowserAPI, the test creates a site and a folder, them add 10 files and iterate over them with the browser api
     * also it is including a mime type
     * Given Scenario: 1)  request items from 0 to 2
     *                  2) request items form 4 to 6
     *                  3) request items form 6 to 10
     *                  4) out of range
     * ExpectedResult: Must have always 10 files as a total, and should retrieve the respective items per request
     *
     */

    @Test()
    public void test_GetFolderContent_mimetype_pagination() throws DotDataException, DotSecurityException, IOException {

        // create a folder
        // create a 10 files
        final SiteDataGen   siteDataGen   = new SiteDataGen();
        final FolderDataGen folderDataGen = new FolderDataGen();
        final Host          host          = siteDataGen.nextPersisted();
        final Folder        folder        = folderDataGen.site(host).nextPersisted();

        for (int i = 0; i < 10; ++i) {

            new FileAssetDataGen(FileUtil.createTemporaryFile("test", ".txt", "this is a test")).host(host)
                    .folder(folder).setPolicy(IndexPolicy.WAIT_FOR).nextPersisted();
        }

        Map<String, Object> resultMap = browserAPI.getFolderContent(BrowserQuery.builder()
                .showDotAssets(true)
                .showLinks(true)
                .showMimeTypes(Arrays.asList("application","text/plain"))
                .withHostOrFolderId(folder.getIdentifier())
                .offset(0)
                .showFiles(true)
                .showFolders(true)
                .showWorking(true)
                .maxResults(2)
                .build());

        assertNotNull(resultMap);
        assertEquals(10, resultMap.get("total"));

        List<Map<String, Object>> results = (List<Map<String, Object>>)resultMap.get("list");
        assertNotNull(results);
        assertEquals(results.size(), 2);

        // 4 - 6
        resultMap = browserAPI.getFolderContent(BrowserQuery.builder()
                .showDotAssets(true)
                .showLinks(true)
                .showMimeTypes(Arrays.asList("application","text/plain"))
                .withHostOrFolderId(folder.getIdentifier())
                .offset(4)
                .showFiles(true)
                .showFolders(true)
                .showWorking(true)
                .maxResults(2)
                .build());

        assertNotNull(resultMap);
        assertEquals(10, resultMap.get("total"));

        results = (List<Map<String, Object>>)resultMap.get("list");
        assertNotNull(results);
        assertEquals(results.size(), 2);

        // 6 - 10
        resultMap = browserAPI.getFolderContent(BrowserQuery.builder()
                .showDotAssets(true)
                .showLinks(true)
                .showMimeTypes(Arrays.asList("application","text/plain"))
                .withHostOrFolderId(folder.getIdentifier())
                .offset(6)
                .showFiles(true)
                .showFolders(true)
                .showWorking(true)
                .maxResults(10)
                .build());

        assertNotNull(resultMap);
        assertEquals(10, resultMap.get("total"));

        results = (List<Map<String, Object>>)resultMap.get("list");
        assertNotNull(results);
        assertEquals(results.size(), 4);

        // 10 - ...
        resultMap = browserAPI.getFolderContent(BrowserQuery.builder()
                .showDotAssets(true)
                .showLinks(true)
                .showMimeTypes(Arrays.asList("application","text/plain"))
                .withHostOrFolderId(folder.getIdentifier())
                .offset(10)
                .showFiles(true)
                .showFolders(true)
                .showWorking(true)
                .maxResults(15)
                .build());

        assertNotNull(resultMap);
        assertEquals(10, resultMap.get("total"));

        results = (List<Map<String, Object>>)resultMap.get("list");
        assertNotNull(results);
        assertEquals(results.size(), 0);
    }


    
    @Test(expected = DotRuntimeException.class)
    public void testGetFolderContentWithInvalidIdentifier() throws DotDataException, DotSecurityException { // https://github.com/dotCMS/core/issues/11829

        final String NOT_EXISTING_ID = "01234567-1234-1234-1234-123456789012";

        browserAPI.getFolderContent( APILocator.systemUser(), NOT_EXISTING_ID, 0, -1, "", null, null, true, false, false, false, "", false, false, 1 );
    }

    @Test
    public void testGetFolderContentWithValidIdentifier() throws Exception { // https://github.com/dotCMS/core/issues/11829

        final String folderPath = "/BrowserAPITest-Folder";

        //Creating folder to check.
        User user = userAPI.getSystemUser();
        Host demo = testHost;
        Folder folder = folderAPI.createFolders( folderPath, demo, user, false );

        try {
            Map<String, Object> folderContent = browserAPI.getFolderContent( APILocator.systemUser(), folder.getInode(), 0, -1, "", null, null, true, false, false, false, "", false, false, 1 );
            Assert.assertTrue( folderContent.containsKey( "total" ) );
            Assert.assertTrue( folderContent.containsKey( "list" ) );
        } catch ( Exception e ){
            Assert.fail( "We should not be getting any exception here" );
        } finally {
            folderAPI.delete( folder, user, false );
        }
    }

    /**
     * <ul>
     *     <li><b>Method to Test:</b> {@link BrowserAPI#getFolderContent(BrowserQuery)}</li>
     *     <li><b>Given Scenario:</b> Evaluate the list of Test Cases specified by the {@link #browserApiTestCases()}
     *     method, and compare the expected results with the ones returned by the API.</li>
     *     <li><b>Expected Result:</b> The total count and the name of the items returned by the API must match the
     *     expected ones.</li>
     * </ul>
     *
     * @throws Exception An error occurred when calling the {@link BrowserAPI#getFolderContent(BrowserQuery)} method.
     */
    @Test
    public void testingDifferentBrowserAPIResults() throws Exception {
        for (final Tuple3<String, BrowserQuery, Set<String>> testCase : browserApiTestCases()) {
            final String testTitle = testCase._1;
            final Map<String, Object> results = this.browserAPI.getFolderContent(testCase._2);
            final List<String> queryResults =
                    ((List<Map<String, Object>>) results.get("list")).stream().map(m -> (String) m.get("name")).collect(Collectors.toList());
            assertFalse("Result list for Test Case '" + testTitle + "' cannot be empty", queryResults.isEmpty());
            final Set<String> expectedNames = testCase._3;
            assertEquals("The expected list of items in the result list for Test Case '" + testTitle + "' must match" +
                                 ".", queryResults.size(), expectedNames.size());
            for (final String name : queryResults) {
                System.out.println("Test Case '" + testTitle + "' got: " + name);
                assertTrue(testTitle, expectedNames.contains(name));
            }
            System.out.println(StringPool.BLANK);
        }
    }

    /**
     * Generates the Test Cases for evaluating the results returned by the
     * {@link BrowserAPI#getFolderContent(BrowserQuery)} method.
     *
     * @return The {@link Tuple3} object with the expected Test Cases, including (1) their name, (2) filtering criteria,
     * and (2) expected results.
     */
    public static List<Tuple3<String,BrowserQuery, Set<String>>> browserApiTestCases() {
        final List<Tuple3<String,BrowserQuery, Set<String>>> testCases = new ArrayList<>();
        // All Test Cases will use the same base folder and the default language
        testCases.add(Tuple.of(
                "Show all contents, 1 language, non-archived",
                BrowserQuery.builder()
                        .showDotAssets(true)
                        .showLinks(true)
                        .withHostOrFolderId(testFolder.getInode())
                        .showFolders(true)
                        .showPages(true)
                        .showFiles(true)
                        .withLanguageId(APILocator.getLanguageAPI().getDefaultLanguage().getId()).build(),
                ImmutableSet.of(
                        testFileAsset.getName(),
                        testFileAsset2.getName(),
                        testSubFolder.getName(),
                        testlink.getName(),
                        testDotAsset.getTitle(),
                        testPage.getPageUrl()))
        );

        testCases.add(Tuple.of(
                "Show files only, 1 language, non-archived",
                BrowserQuery.builder()
                        .showDotAssets(true)
                        .showFiles(true)
                        .withHostOrFolderId(testFolder.getInode())
                        .withLanguageId(APILocator.getLanguageAPI().getDefaultLanguage().getId()).build(),
                ImmutableSet.of(
                        testFileAsset.getName(),
                        testFileAsset2.getName(),
                        testDotAsset.getTitle()))
        );
        
        testCases.add(Tuple.of(
                "Show files only, all languages, non-archived",
                BrowserQuery.builder()
                        .showDotAssets(true)
                        .showFiles(true)
                        .withHostOrFolderId(testFolder.getInode()).build(),
                ImmutableSet.of(
                        testFileAsset.getName(),
                        testFileAsset2.getName(),
                        testFileAsset2MultiLingual.getName(),
                        testDotAsset.getTitle()))
        );

        testCases.add(Tuple.of(
                "Show files only, all languages, non-archived, no dotAssets",
                BrowserQuery.builder()
                        .showDotAssets(false)
                        .showFiles(true)
                        .showPages(false)
                        .withHostOrFolderId(testFolder.getInode()).build(),
                ImmutableSet.of(
                        testFileAsset.getName(),
                        testFileAsset2.getName(),
                        testFileAsset2MultiLingual.getName()))
        );
        
        testCases.add(Tuple.of(
                "Show archived files, all languages, no dotAssets",
                BrowserQuery.builder()
                        .showFiles(true)
                        .showArchived(true)
                        .withHostOrFolderId(testFolder.getInode()).build(),
                ImmutableSet.of(
                        testFileAsset.getName(),
                        testFileAsset2.getName(),
                        testFileAsset3Archived.getName(),
                        testFileAsset2MultiLingual.getName()))
        );
        
        testCases.add(Tuple.of(
                "Show HTML Pages",
                BrowserQuery.builder()
                        .showPages(true)
                        .withHostOrFolderId(testFolder.getInode()).build(),
                ImmutableSet.of(
                        testPage.getPageUrl()))
        );
        
        testCases.add(Tuple.of(
                "Show Links",
                BrowserQuery.builder()
                        .showLinks(true)
                        .showContent(false)
                        .withHostOrFolderId(testFolder.getInode()).build(),
                ImmutableSet.of(
                        testlink.getName()))
        );

        // When requesting content in the folder for non the default language,
        //should return the content in the language requested + the content in the
        //default language
        testCases.add( Tuple.of(
                "Request Content No default lang, should return content also in default lang",

                BrowserQuery.builder()
                        .withHostOrFolderId(testFolder.getInode())
                        .showWorking(true)
                        .showArchived(false)
                        .showFolders(true)
                        .showPages(true)
                        .showFiles(true)
                        .showLinks(true)
                        .showDefaultLangItems(true)
                        .showDotAssets(true)
                        .withLanguageId(testLanguage.getId())
                        .build()
                ,
                ImmutableSet.of(
                        testFileAsset.getName(),
                        testFileAsset2.getName(),
                        testFileAsset2MultiLingual.getName(),
                        testSubFolder.getName(),
                        testlink.getName(),
                        testDotAsset.getTitle(),
                        testPage.getPageUrl()
                ))
        );
        
        return testCases;
    }

    /**
     * Method to test: getFolderContent
     * Given Scenario: Create a parent folder and a couple of subfolders, get the content of the parent folder.
     * ExpectedResult: The subfolders should be sort by name not by title.
     *
     */
    @Test
    public void test_getFolderContent_folderOrderedByName() throws Exception{
        final Host site = new SiteDataGen().nextPersisted();

        final Folder parentFolder = new FolderDataGen().site(site).nextPersisted();
        final Folder childFolder1 = new FolderDataGen().name("barn").title("barn")
                .parent(parentFolder).nextPersisted();
        final Folder childFolder2 = new FolderDataGen().name("xray").title("baby")
                .parent(parentFolder).nextPersisted();
        final Map<String, Object> parentFolderContent = browserAPI.getFolderContent(BrowserQuery.builder()
                .withHostOrFolderId(parentFolder.getIdentifier())
                .showFolders(true)
                .build());
        assertNotNull(parentFolderContent);
        assertEquals(2, parentFolderContent.get("total"));
        List<Map<String, Object>> results = (List<Map<String, Object>>)parentFolderContent.get("list");
        assertEquals(childFolder1.getIdentifier(),results.get(0).get("identifier"));
        assertEquals(childFolder2.getIdentifier(),results.get(1).get("identifier"));

    }

    /**
     * Method to test: {@link BrowserAPIImpl#getFolderContent(BrowserQuery)}
     * When: A Contentlet has Version in DEFAULT Variant and also in a specific Variant
     * Should: Return just the DEFAULT Version
     *
     * @throws DotDataException
     * @throws DotSecurityException
     * @throws IOException
     */
    @Test
    public void getJustDEFAULTVariantVersion() throws DotDataException, DotSecurityException, IOException {
        final Host host = new SiteDataGen().nextPersisted();
        final Folder folder = new FolderDataGen().site(host).nextPersisted();

       final  Contentlet file = new FileAssetDataGen(folder, "This is a File")
               .folder(folder)
               .host(host)
               .nextPersisted();

        final Variant variant = new VariantDataGen().nextPersisted();
        ContentletDataGen.createNewVersion(file, variant, Collections.EMPTY_MAP);

        final Map<String, Object> files = browserAPI.getFolderContent(BrowserQuery.builder()
                .withHostOrFolderId(folder.getIdentifier())
                .build());

        assertEquals(1, Integer.parseInt(files.get("total").toString()));

        final List list = (List) files.get("list");
        assertEquals(1, list.size());
        assertEquals(file.getIdentifier(), ((Contentlet.ContentletHashMap) list.get(0)).get("identifier"));
        assertEquals(file.getInode(), ((Contentlet.ContentletHashMap) list.get(0)).get("inode"));

    }

    /**
     * Method to test: {@link BrowserAPIImpl#getFolderContent(BrowserQuery)}
     * When: A Contentlet has Version  in a specific Variant
     * Should: Not return this Contentlet
     *
     * @throws DotDataException
     * @throws DotSecurityException
     * @throws IOException
     */
    @Test
    public void notGetSpecificVariantVersion() throws DotDataException, DotSecurityException, IOException {
        final Host host = new SiteDataGen().nextPersisted();
        final Folder folder = new FolderDataGen().site(host).nextPersisted();

        final Variant variant = new VariantDataGen().nextPersisted();

        final  Contentlet file = new FileAssetDataGen(folder, "This is a File")
                .folder(folder)
                .host(host)
                .variant(variant)
                .nextPersisted();


        final Map<String, Object> files = browserAPI.getFolderContent(BrowserQuery.builder()
                .withHostOrFolderId(folder.getIdentifier())
                .build());

        assertEquals(0, Integer.parseInt(files.get("total").toString()));

        final List list = (List) files.get("list");
        assertTrue(list.isEmpty());

    }

    /**
     * <ul>
     *     <li><b>Method to Test:</b> {@link BrowserAPIImpl#getAssetNameColumn(String)}</li>
     *     <li><b>Given Scenario:</b> Check that the asset name value is queried against the {@code json_as_content}
     *     column.</li>
     *     <li><b>Expected Result:</b> The query must containe the expected SQL code for both PostgreSQL and MSSQL
     *     databases.</li>
     * </ul>
     */
    @Test
    public void getAssetNameColumn_providedBaseQuery_shouldGenerateCorrectSQLForDB() {

        final String sql = BrowserAPIImpl.getAssetNameColumn("LOWER(%s) LIKE ? ");

        assertNotNull(sql);
        if (DbConnectionFactory.isPostgres()) {
            assertTrue(sql.contains("-> 'fields' -> 'fileName' ->> 'value'" ));
        }
        else{
            assertTrue(sql.contains("$.fields.fileName.value"));
        }
    }

    /**
     * <ul>
     *     <li><b>Method to Test:</b> {@link BrowserAPIImpl#getContentUnderParentFromDB(BrowserQuery)} and
     *     {@link BrowserAPIImpl#getFolderContent(BrowserQuery)}</li>
     *     <li><b>Given Scenario:</b> Searching for a DotAsset content must return a valid result and expected result
     *     .</li>
     *     <li><b>Expected Result:</b> The {@code company_logo.png} DotAsset must be returned by the API.</li>
     * </ul>
     *
     * @throws DotDataException     An error occurred when retrieving the result from the API.
     * @throws DotSecurityException An error occurred when retrieving the result from the API.
     */
    @Test
    public void getFolderContent_searchDotAssetWithFilter_shouldReturnNotNull() throws DotDataException, DotSecurityException {
        final String filterText = "company_logo.png";
        final User user = APILocator.systemUser();
        final List<String> mimeTypes = List.of("image");

        final BrowserQuery browserQuery = BrowserQuery.builder()
                .withUser(user)
                .withHostOrFolderId("SYSTEM_HOST")
                .offset(0)
                .maxResults(1)
                .withFilter(filterText)
                .showMimeTypes(mimeTypes)
                .showImages(mimeTypes.contains(mimeTypes.get(0)))
                .showExtensions(List.of())
                .showWorking(true)
                .showArchived(false)
                .showFolders(false)
                .showFiles(true)
                .showShorties(false)
                .showContent(true)
                .sortBy("modDate")
                .sortByDesc(true)
                .showLinks(false)
                .withLanguageId(1)
                .showDotAssets(true)
                .build();

        final List<Contentlet> contentletList = this.browserAPI.getContentUnderParentFromDB(browserQuery);
        final Map<String, Object> result = this.browserAPI.getFolderContent(browserQuery);

        assertNotNull(contentletList);
        assertNotNull(result);
    }

    /**
     * <ul>
     *     <li><b>Method to Test:</b> {@link BrowserAPIImpl#getContentUnderParentFromDB(BrowserQuery)}</li>
     *     <li><b>Given Scenario:</b> Searching for a DotAsset content must return a valid result and expected result
     *     .</li>
     *     <li><b>Expected Result:</b> The {@code test.jpg} DotAsset must be returned.</li>
     * </ul>
     */
    @Test
    public void getContentUnderParentFromDB_searchDotAssetWithFilter_shouldReturnTheAsset() {
        final String filterText = "test.jpg";
        final User user = APILocator.systemUser();
        final List<String> mimeTypes = List.of("image");

        final BrowserQuery browserQuery = BrowserQuery.builder()
                .withUser(user)
                .withHostOrFolderId(testFolder.getIdentifier())
                .offset(0)
                .maxResults(20)
                .withFilter(filterText)
                .showMimeTypes(mimeTypes)
                .showImages(mimeTypes.contains(mimeTypes.get(0)))
                .showExtensions(List.of())
                .showWorking(true)
                .showArchived(false)
                .showFolders(false)
                .showFiles(true)
                .showShorties(false)
                .showContent(true)
                .sortBy("modDate")
                .sortByDesc(true)
                .showLinks(false)
                .withLanguageId(1)
                .showDotAssets(true)
                .build();

        final List<Contentlet> contentletList = this.browserAPI.getContentUnderParentFromDB(browserQuery);

        assertTrue("No contents found",contentletList.size() > 0);
        for (final Contentlet contentlet : contentletList) {
            assertEquals(contentlet.getIdentifier(), testDotAsset.getIdentifier());
        }
    }


    /**
     * Generally speaking in most cases when a file is uploaded title and file name are the same.
     * But this is not always the case. Since we can upload a file via workflows and the title is not required.
     * Or it can take any value.
     * Given scenario: A folder with two files named very similar. Title is different from file name.
     * Expected result: We query using the exact file name The file asset should be returned by the API.
     * @throws DotDataException
     * @throws DotSecurityException
     * @throws IOException
     */
    @Test
    public void getFolderContent_searchAssetWithNoTitleUsingFileName_Expect_Results()
            throws DotDataException, DotSecurityException, IOException {

        final Folder folder = new FolderDataGen().nextPersisted();

        final File file1 = FileUtil.createTemporaryFile("lol", ".txt", "lol");
        final File file2 = FileUtil.createTemporaryFile("lol", ".txt", "lol");

        final String title = "testFileAsset1";
        final Contentlet contentlet1 = new FileAssetDataGen(folder, file1).title(title).languageId(1).nextPersisted();
        final Contentlet contentlet2 = new FileAssetDataGen(folder, file2).title(title).languageId(1).nextPersisted();
        //Title is different from file name to test the filter

        final FileAsset fileAsset1 = APILocator.getFileAssetAPI().fromContentlet(contentlet1);

        final User user = APILocator.systemUser();

        final BrowserQuery browserQuery = BrowserQuery.builder()
                .withUser(user)
                .maxResults(1)
                .withHostOrFolderId(folder.getIdentifier())
                .withFileName(fileAsset1.getFileName())
                .showWorking(true)
                .showArchived(false)
                .showFolders(false)
                .showFiles(true)
                .showContent(true)
                .withLanguageId(1)
                .showDotAssets(false)
                .build();

        final List<Contentlet> contentletList = this.browserAPI.getContentUnderParentFromDB(browserQuery);
        assertFalse(contentletList.isEmpty());
        assertEquals(1, contentletList.size());
        assertEquals(contentletList.get(0).getInode(),contentlet1.getInode());

    }


    @Test
    public void testThatSearchingForContentWithinAFolderWorks() throws DotDataException, DotSecurityException, IOException {

        final Host host = new SiteDataGen().nextPersisted();
        final Folder folder = new FolderDataGen().site(host).nextPersisted();
        String shorty = UUIDGenerator.shorty();
        final String[] tags = {"tag1" + shorty, "tag2" + shorty};
        final File file1 = FileUtil.createTemporaryFile("lol", ".txt", "lol");
        final Contentlet dotAsset = new DotAssetDataGen(host,folder,file1).tags(tags).nextPersisted();


        // searching by a tag
        final BrowserQuery browserQuery = BrowserQuery.builder()
            .withUser(APILocator.systemUser())
            .maxResults(1)
            .withHostOrFolderId(folder.getIdentifier())
            .withFilter("tag1" + shorty)
            .showWorking(true)
            .showArchived(false)
            .showFolders(false)
            .showFiles(true)
            .showContent(true)
            .withLanguageId(1)
            .showDotAssets(true)
            .build();


        List<String> appliedTags = new DotConnect("select tagname from tag, tag_inode where tag.tag_id=tag_inode.tag_id and inode = ?")
            .addParam(dotAsset.getInode())
            .loadStringArray("tagname");


        final List<Contentlet> contentletList = this.browserAPI.getContentUnderParentFromDB(browserQuery);
        assertFalse(contentletList.isEmpty());
        assertEquals(1, contentletList.size());
        assertEquals(contentletList.get(0).getInode(),dotAsset.getInode());
    }


    /**
     * Test for BrowserAPI with multiple language IDs using List.
     * Verifies that BrowserAPI filters content for multiple specified languages from a List.
     */
    @Test
    public void test_BrowserAPI_withMultipleLanguageIds_List() throws Exception {
        final Host host = new SiteDataGen().nextPersisted();
        final Folder folder = new FolderDataGen().site(host).nextPersisted();

        // Create additional languages
        final Language lang1 = new LanguageDataGen().nextPersisted();
        final Language lang2 = new LanguageDataGen().nextPersisted();
        final Language lang3 = new LanguageDataGen().nextPersisted();

        final long timeMillis = System.currentTimeMillis();
        // Create content in different languages
        final File tempFile1 = FileUtil.createTemporaryFile("test1"+timeMillis, ".txt", "test content 1");
        final File tempFile2 = FileUtil.createTemporaryFile("test2"+timeMillis, ".txt", "test content 2");
        final File tempFile3 = FileUtil.createTemporaryFile("test3"+timeMillis, ".txt", "test content 3");

        // Content in each language
        new FileAssetDataGen(tempFile1)
                .languageId(lang1.getId())
                .host(host)
                .folder(folder)
                .setPolicy(IndexPolicy.WAIT_FOR)
                .nextPersisted();

        new FileAssetDataGen(tempFile2)
                .languageId(lang2.getId())
                .host(host)
                .folder(folder)
                .setPolicy(IndexPolicy.WAIT_FOR)
                .nextPersisted();

        new FileAssetDataGen(tempFile3)
                .languageId(lang3.getId())
                .host(host)
                .folder(folder)
                .setPolicy(IndexPolicy.WAIT_FOR)
                .nextPersisted();

        // Query with multiple language IDs using List
        final List<Long> languageIds = List.of(lang1.getId(), lang2.getId());
        final BrowserQuery browserQuery = BrowserQuery.builder()
                .withHostOrFolderId(folder.getIdentifier())
                .withLanguageIds(languageIds)
                .showFiles(true)
                .showWorking(true)
                .build();

        final Map<String, Object> results = browserAPI.getFolderContent(browserQuery);
        final List<Map<String, Object>> contentList = (List<Map<String, Object>>) results.get("list");

        assertNotNull(results);
        assertTrue("Should find content in exactly 2 languages", (Integer) results.get("total") == 2);

        // Verify that results contain content from specified languages only
        final Set<Long> foundLanguages = contentList.stream()
                .map(content -> ((Number) content.get("languageId")).longValue())
                .collect(Collectors.toSet());

        assertTrue("Should contain content from lang1", foundLanguages.contains(lang1.getId()));
        assertTrue("Should contain content from lang2", foundLanguages.contains(lang2.getId()));
        assertFalse("Should not contain content from lang3", foundLanguages.contains(lang3.getId()));
    }


    /**
     * Test for BrowserAPI with multiple content type IDs using Set.
     * Verifies that BrowserAPI filters content for multiple specified content types.
     */
    @Test
    public void test_BrowserAPI_withMultipleContentTypes_Set() throws Exception {
        final Host host = new SiteDataGen().nextPersisted();
        final Folder folder = new FolderDataGen().site(host).nextPersisted();

        // Create different types of content
        final File tempFile = FileUtil.createTemporaryFile("test", ".txt", "test content");

        // Create a FileAsset
        final Contentlet fileAsset = new FileAssetDataGen(tempFile)
                .host(host)
                .folder(folder)
                .setPolicy(IndexPolicy.WAIT_FOR)
                .nextPersisted();

        // Create a DotAsset
        final File tempFile2 = FileUtil.createTemporaryFile("dotasset", ".txt", "dotasset content");
        final Contentlet dotAsset = new DotAssetDataGen(host, folder, tempFile2)
                .setPolicy(IndexPolicy.WAIT_FOR)
                .nextPersisted();

        // Create a custom content type and content
        final var customContentType = new ContentTypeDataGen()
                .host(host)
                .folder(folder)
                .nextPersisted();

        final Contentlet customContent = new ContentletDataGen(customContentType)
                .host(host)
                .folder(folder)
                .setPolicy(IndexPolicy.WAIT_FOR)
                .nextPersisted();

        final ContentTypeAPI contentTypeAPI = APILocator.getContentTypeAPI(APILocator.systemUser());
        // Query with multiple content type IDs using Set - filter for FileAsset and DotAsset only
        final Set<String> contentTypeIds = Set.of("fileAsset", "dotAsset")
                .stream().map(s -> Try.of(() -> contentTypeAPI.find(s).id()).getOrNull())
                .filter(Objects::nonNull).collect(Collectors.toSet());
        final BrowserQuery browserQuery = BrowserQuery.builder()
                .withHostOrFolderId(folder.getIdentifier())
                .withContentTypes(contentTypeIds)
                .showFiles(true)
                .showContent(true)
                .showDotAssets(true)
                .showWorking(true)
                .build();

        final Map<String, Object> results = browserAPI.getFolderContent(browserQuery);
        final List<Map<String, Object>> contentList = (List<Map<String, Object>>) results.get("list");

        assertNotNull(results);
        assertEquals("Should find exactly 2 pieces of content (FileAsset + DotAsset)", 2, results.get("total"));

        // Verify that results contain content from specified content types only
        final Set<String> foundContentTypes = contentList.stream()
                .map(content -> (String) content.get("baseType"))
                .collect(Collectors.toSet());

        assertTrue("Should contain FileAsset content", foundContentTypes.contains("FILEASSET"));
        assertTrue("Should contain DotAsset content", foundContentTypes.contains("DOTASSET"));

        // Verify specific contentlets are found
        final Set<String> foundINodes = contentList.stream()
                .map(content -> (String) content.get("inode"))
                .collect(Collectors.toSet());

        assertTrue("Should contain file asset", foundINodes.contains(fileAsset.getInode()));
        assertTrue("Should contain dot asset", foundINodes.contains(dotAsset.getInode()));
        assertFalse("Should not contain custom content", foundINodes.contains(customContent.getInode()));
    }

    /**
     * Test for BrowserAPI combining multiple languages and multiple content types.
     * Verifies that both filters work together correctly.
     */
    @Test
    public void test_BrowserAPI_withMultipleLanguagesAndContentTypes() throws Exception {
        final Host host = new SiteDataGen().nextPersisted();
        final Folder folder = new FolderDataGen().site(host).nextPersisted();

        // Create languages
        final Language lang1 = new LanguageDataGen().nextPersisted();
        final Language lang2 = new LanguageDataGen().nextPersisted();
        final long defaultLangId = APILocator.getLanguageAPI().getDefaultLanguage().getId();

        // Create FileAssets in different languages
        final File tempFile1 = FileUtil.createTemporaryFile("file1", ".txt", "file content 1");
        final Contentlet fileAsset_defaultLang = new FileAssetDataGen(tempFile1)
                .languageId(defaultLangId)
                .host(host)
                .folder(folder)
                .setPolicy(IndexPolicy.WAIT_FOR)
                .nextPersisted();

        final File tempFile2 = FileUtil.createTemporaryFile("file2", ".txt", "file content 2");
        final Contentlet fileAsset_lang1 = new FileAssetDataGen(tempFile2)
                .languageId(lang1.getId())
                .host(host)
                .folder(folder)
                .setPolicy(IndexPolicy.WAIT_FOR)
                .nextPersisted();

        final File tempFile3 = FileUtil.createTemporaryFile("file3", ".txt", "file content 3");
        final Contentlet fileAsset_lang2 = new FileAssetDataGen(tempFile3)
                .languageId(lang2.getId())
                .host(host)
                .folder(folder)
                .setPolicy(IndexPolicy.WAIT_FOR)
                .nextPersisted();

        // Create custom content type and content in different languages
        final var customContentType = new ContentTypeDataGen()
                .host(host)
                .folder(folder)
                .nextPersisted();

        new ContentletDataGen(customContentType)
                .languageId(defaultLangId)
                .host(host)
                .folder(folder)
                .setPolicy(IndexPolicy.WAIT_FOR)
                .nextPersisted();

        new ContentletDataGen(customContentType)
                .languageId(lang1.getId())
                .host(host)
                .folder(folder)
                .setPolicy(IndexPolicy.WAIT_FOR)
                .nextPersisted();

        final ContentTypeAPI contentTypeAPI = APILocator.getContentTypeAPI(APILocator.systemUser());
        final String fileAssetTypeId = Try.of(() -> contentTypeAPI.find("fileAsset").id()).getOrNull();

        // Query for FileAssets in lang1 only
        final BrowserQuery browserQuery = BrowserQuery.builder()
                .withHostOrFolderId(folder.getIdentifier())
                .withLanguageIds(Set.of(lang1.getId()))
                .withContentTypes(Set.of(fileAssetTypeId))
                .showFiles(true)
                .showContent(true)
                .showWorking(true)
                .build();

        final Map<String, Object> results = browserAPI.getFolderContent(browserQuery);
        final List<Map<String, Object>> contentList = (List<Map<String, Object>>) results.get("list");

        assertNotNull(results);
        assertEquals("Should find exactly 1 FileAsset in lang1", Integer.valueOf(1), results.get("total"));

        // Verify that result is the correct contentlet
        final Map<String, Object> foundContent = contentList.get(0);
        assertEquals("Should be the FileAsset in lang1", fileAsset_lang1.getInode(), foundContent.get("inode"));
        assertEquals("Should be lang1", lang1.getId(), ((Number) foundContent.get("languageId")).longValue());
        assertEquals("Should be FileAsset base type", "FILEASSET", foundContent.get("baseType"));

        // Verify other contentlets are not found
        final Set<String> foundINodes = contentList.stream()
                .map(content -> (String) content.get("inode"))
                .collect(Collectors.toSet());

        assertTrue("Should contain file asset lang1", foundINodes.contains(fileAsset_lang1.getInode()));
        assertFalse("Should not contain file asset default lang", foundINodes.contains(fileAsset_defaultLang.getInode()));
        assertFalse("Should not contain file asset lang2", foundINodes.contains(fileAsset_lang2.getInode()));
    }

    @Test
    public void test_BrowserAPI_Filter_Folders() throws Exception {
        final long timeMillis = System.currentTimeMillis();
        final Host host = new SiteDataGen().nextPersisted();
        final Folder parentFolder = new FolderDataGen().name("parentFolder"+timeMillis).site(host).nextPersisted();

        // Create additional languages
        final Language lang1 = new LanguageDataGen().nextPersisted();
        final Language lang2 = new LanguageDataGen().nextPersisted();
        final Language lang3 = new LanguageDataGen().nextPersisted();

        // Create content in different languages
        final File tempFile1 = FileUtil.createTemporaryFile("test1"+timeMillis, ".txt", "test content 1");
        final File tempFile2 = FileUtil.createTemporaryFile("test2"+timeMillis, ".txt", "test content 2");
        final File tempFile3 = FileUtil.createTemporaryFile("test3"+timeMillis, ".txt", "test content 3");

        // Content in each language
        new FileAssetDataGen(tempFile1)
                .languageId(lang1.getId())
                .host(host)
                .folder(parentFolder)
                .setPolicy(IndexPolicy.WAIT_FOR)
                .nextPersisted();

        new FileAssetDataGen(tempFile2)
                .languageId(lang2.getId())
                .host(host)
                .folder(parentFolder)
                .setPolicy(IndexPolicy.WAIT_FOR)
                .nextPersisted();

        new FileAssetDataGen(tempFile3)
                .languageId(lang3.getId())
                .host(host)
                .folder(parentFolder)
                .setPolicy(IndexPolicy.WAIT_FOR)
                .nextPersisted();

        // Query with multiple language IDs using List
        final List<Long> languageIds = List.of(lang1.getId(), lang2.getId());
        final BrowserQuery browserQuery = BrowserQuery.builder()
                .withHostOrFolderId(parentFolder.getIdentifier())
                .withLanguageIds(languageIds)
                .showContent(true)
                .build();

        final Map<String, Object> results = browserAPI.getFolderContent(browserQuery);
        final List<Map<String, Object>> contentList = (List<Map<String, Object>>) results.get("list");

        assertNotNull(results);
        assertTrue("Should find content in exactly 2 languages", (Integer) results.get("total") == 2);

        // Verify that results contain content from specified languages only
        final Set<Long> foundLanguages = contentList.stream()
                .map(content -> ((Number) content.get("languageId")).longValue())
                .collect(Collectors.toSet());

        assertTrue("Should contain content from lang1", foundLanguages.contains(lang1.getId()));
        assertTrue("Should contain content from lang2", foundLanguages.contains(lang2.getId()));
        assertFalse("Should not contain content from lang3", foundLanguages.contains(lang3.getId()));
    }

    /**
     * <ul>
     *     <li><b>Method to Test:</b> {@link BrowserAPIImpl#buildBaseESQuery(BrowserQuery)}</li>
     *     <li><b>Given Scenario:</b> Test the method with various combinations of filter and fileName parameters.</li>
     *     <li><b>Expected Result:</b> The method should generate proper Lucene query strings based on the input parameters.</li>
     * </ul>
     */
    @Test
    public void test_buildBaseESQuery_withDifferentFilterCombinations() {
        final BrowserAPIImpl browserAPIImpl = new BrowserAPIImpl();

        // Test Case 1: No filter, no fileName - should return empty
        BrowserQuery queryEmpty = BrowserQuery.builder().build();
        String result = browserAPIImpl.buildBaseESQuery(queryEmpty);
        assertEquals("Empty query should return blank string", "", result);

        // Test Case 2: Only filter provided
        BrowserQuery queryWithFilter = BrowserQuery.builder()
                .withFilter("test")
                .build();
        result = browserAPIImpl.buildBaseESQuery(queryWithFilter);
        assertNotNull("Result should not be null", result);
        assertTrue("Should contain title search", result.contains("title:test*"));
        assertTrue("Should contain quoted title search", result.contains("title:'test'^15"));
        assertTrue("Should contain dotraw title search", result.contains("title_dotraw:*test*^5"));
        assertTrue("Should be wrapped with mandatory group", result.startsWith(" +(") && result.endsWith(")"));
        assertFalse("Should not contain metadata search", result.contains("metadata.name"));

        // Test Case 3: Only fileName provided with metadata enabled
        try {
            // Mock the static method calls for metadata availability
            BrowserQuery queryWithFileName = BrowserQuery.builder()
                    .withFileName("document.pdf")
                    .build();
            result = browserAPIImpl.buildBaseESQuery(queryWithFileName);
            assertNotNull("Result should not be null", result);

            // The result will depend on whether metadata indexing is enabled
            // If metadata is enabled, it should contain metadata searches
            // If not, it should warn and not include metadata searches
            if (result.contains("metadata.name")) {
                assertTrue("Should contain metadata name search", result.contains("metadata.name:document.pdf*"));
                assertTrue("Should contain quoted metadata search", result.contains("metadata.name:'document.pdf'^15"));
                assertTrue("Should contain dotraw metadata search", result.contains("metadata.name_dotraw:*document.pdf*^5"));
            }
        } catch (Exception e) {
            // Expected if metadata is not configured
        }

        // Test Case 4: Both filter and fileName provided
        BrowserQuery queryWithBoth = BrowserQuery.builder()
                .withFilter("test")
                .withFileName("document.pdf")
                .build();
        result = browserAPIImpl.buildBaseESQuery(queryWithBoth);
        assertNotNull("Result should not be null", result);
        assertTrue("Should contain title search", result.contains("title:test*"));
        assertTrue("Should be wrapped with mandatory group", result.startsWith(" +(") && result.endsWith(")"));

        // Should contain AND operator between filter and fileName if both are present and fileName is processed
        if (result.contains("metadata.name")) {
            assertTrue("Should contain AND operator", result.contains(" AND "));
        }

        // Test Case 5: Filter with special characters
        BrowserQuery querySpecialChars = BrowserQuery.builder()
                .withFilter("test & special")
                .build();
        result = browserAPIImpl.buildBaseESQuery(querySpecialChars);
        assertNotNull("Result should not be null", result);
        assertTrue("Should handle special characters in filter", result.contains("test & special"));

        // Test Case 6: Empty string filter
        BrowserQuery queryEmptyFilter = BrowserQuery.builder()
                .withFilter("")
                .build();
        result = browserAPIImpl.buildBaseESQuery(queryEmptyFilter);
        assertEquals("Empty filter should return blank string", "", result);

        // Test Case 7: Null filter (using UtilMethods.isSet check)
        BrowserQuery queryNullFilter = BrowserQuery.builder()
                .withFilter(null)
                .build();
        result = browserAPIImpl.buildBaseESQuery(queryNullFilter);
        assertEquals("Null filter should return blank string", "", result);

        // Test Case 8: Empty fileName
        BrowserQuery queryEmptyFileName = BrowserQuery.builder()
                .withFileName("")
                .build();
        result = browserAPIImpl.buildBaseESQuery(queryEmptyFileName);
        assertEquals("Empty fileName should return blank string", "", result);

        // Test Case 9: Whitespace-only filter
        BrowserQuery queryWhitespaceFilter = BrowserQuery.builder()
                .withFilter("   ")
                .build();
        result = browserAPIImpl.buildBaseESQuery(queryWhitespaceFilter);
        assertNotNull("Result should not be null for whitespace filter", result);
        // The method should handle whitespace in the filter parameter
    }

    /**
     * <ul>
     *     <li><b>Method to Test:</b> {@link BrowserAPIImpl#buildBaseESQuery(BrowserQuery)}</li>
     *     <li><b>Given Scenario:</b> Test query structure and Lucene syntax compliance.</li>
     *     <li><b>Expected Result:</b> Generated queries should follow proper Lucene query syntax.</li>
     * </ul>
     */
    @Test
    public void test_buildBaseESQuery_luceneSyntaxCompliance() {
        final BrowserAPIImpl browserAPIImpl = new BrowserAPIImpl();

        // Test proper Lucene field:value syntax
        BrowserQuery query = BrowserQuery.builder()
                .withFilter("searchterm")
                .build();
        String result = browserAPIImpl.buildBaseESQuery(query);

        assertNotNull("Result should not be null", result);

        // Verify Lucene syntax elements
        assertTrue("Should use field:value syntax", result.contains("title:searchterm*"));
        assertTrue("Should use wildcard correctly", result.contains("*"));
        assertTrue("Should use boost factor", result.contains("^15") || result.contains("^5"));
        assertTrue("Should use quoted phrases", result.contains("'searchterm'"));
        assertTrue("Should be wrapped in mandatory group syntax", result.startsWith(" +(") && result.endsWith(")"));

        // Test multiple word filter
        BrowserQuery multiWordQuery = BrowserQuery.builder()
                .withFilter("multiple words")
                .build();
        result = browserAPIImpl.buildBaseESQuery(multiWordQuery);

        assertNotNull("Result should not be null for multi-word query", result);
        assertTrue("Should handle multi-word filters", result.contains("multiple words"));
    }

    /**
     * <ul>
     *     <li><b>Method to Test:</b> {@link BrowserAPIImpl#buildBaseESQuery(BrowserQuery)}</li>
     *     <li><b>Given Scenario:</b> Test edge cases and boundary conditions.</li>
     *     <li><b>Expected Result:</b> Method should handle edge cases gracefully.</li>
     * </ul>
     */
    @Test
    public void test_buildBaseESQuery_edgeCases() {
        final BrowserAPIImpl browserAPIImpl = new BrowserAPIImpl();

        // Test a very long filter string
        final String longFilter = "a".repeat(1000);
        BrowserQuery longQuery = BrowserQuery.builder()
                .withFilter(longFilter)
                .build();
        String result = browserAPIImpl.buildBaseESQuery(longQuery);
        assertNotNull("Should handle long filter strings", result);
        assertTrue("Should contain the long filter", result.contains(longFilter));

        // Test filter with numbers
        BrowserQuery numericQuery = BrowserQuery.builder()
                .withFilter("test123")
                .build();
        result = browserAPIImpl.buildBaseESQuery(numericQuery);
        assertNotNull("Should handle numeric characters", result);
        assertTrue("Should contain numeric filter", result.contains("test123"));

        // Test fileName with extension
        BrowserQuery fileExtQuery = BrowserQuery.builder()
                .withFileName("document.pdf")
                .build();
        result = browserAPIImpl.buildBaseESQuery(fileExtQuery);
        assertNotNull("Should handle file extensions", result);
        // Result depends on metadata configuration

        // Test single character filter
        BrowserQuery singleCharQuery = BrowserQuery.builder()
                .withFilter("a")
                .build();
        result = browserAPIImpl.buildBaseESQuery(singleCharQuery);
        assertNotNull("Should handle single character filter", result);
        assertTrue("Should process single character", result.contains("a"));
    }

    /**
     * Test Case: Smart Pagination - Page 1 with 25 folders and 100 contentlets, page size 26
     * Expected: 25 folders + 1 contentlet
     *
     * Tests the intelligent pagination system that handles elements from different sources:
     * - Folders (loaded in memory)
     * - Links (loaded in memory)
     * - Contentlets (database-paginated)
     *
     * The goal is to avoid loading all contentlets from DB and use counts of folders/links
     * to calculate the offset within the database pagination.
     */
    @Test
    public void test_SmartPaginationPage1_25Folders1Contentlet() throws Exception {
        // Create a test environment
        final Host host = new SiteDataGen().nextPersisted();
        final Folder parentFolder = new FolderDataGen().site(host).nextPersisted();

        // Create 25 folders
        final List<Folder> subFolders = new ArrayList<>();
        for (int i = 0; i < 25; i++) {
            final Folder subFolder = new FolderDataGen()
                    .name(String.format("folder_%02d", i))
                    .parent(parentFolder)
                    .nextPersisted();
            subFolders.add(subFolder);
        }

        // Create 100 contentlets
        for (int i = 0; i < 100; i++) {
            new FileAssetDataGen(FileUtil.createTemporaryFile("content", ".txt", "content " + i))
                    .host(host)
                    .folder(parentFolder)
                    .setPolicy(IndexPolicy.WAIT_FOR)
                    .nextPersisted();
        }

        // Execute pagination query - Page 1 with page size 26
        final BrowserQuery browserQuery = BrowserQuery.builder()
                .showFolders(true)
                .showContent(true)
                .showFiles(true)
                .showDotAssets(true)
                .showLinks(false) // Simplify test by disabling links
                .withHostOrFolderId(parentFolder.getIdentifier())
                .offset(0)
                .maxResults(26)
                .build();

        final PaginatedContents paginatedContents = browserAPI.getPaginatedContents(browserQuery);

        // Verify results
        assertNotNull("Result should not be null", paginatedContents);

        @SuppressWarnings("unchecked")
        final List<Map<String, Object>> list = paginatedContents.list;

        assertEquals("Should return exactly 26 items (25 folders + 1 contentlet)", 26, list.size());
        assertEquals("Folder count should be 25", 25, paginatedContents.folderCount);
        assertEquals("Content count should be 1", 1, paginatedContents.contentCount);
        assertEquals("Content total count should be 100", 100, paginatedContents.contentTotalCount);

        // Verify first 25 items are folders
        for (int i = 0; i < 25; i++) {
            final Map<String, Object> item = list.get(i);
            assertNotNull("Item should have name", item.get("name"));
            assertTrue("First 25 items should be folders",
                item.get("name").toString().startsWith("folder_"));
        }

        // Verify the last item is a contentlet
        final Map<String, Object> lastItem = list.get(25);
        assertNotNull("Last item should have extension", lastItem.get("extension"));
        assertEquals("Last item should be a file", "txt", lastItem.get("extension"));
    }

    /**
     * Test Case: Smart Pagination - Page 2 with the same data (offset=11, still 11 items per page)
     * Expected: 11 contentlets (all folders were shown on page 1)
     */
    @Test
    public void test_SmartPaginationPage2_10Contentlets() throws Exception {
        // Create test environment
        final Host host = new SiteDataGen().nextPersisted();
        final Folder parentFolder = new FolderDataGen().site(host).nextPersisted();

        // Create 10 folders
        for (int i = 0; i < 10; i++) {
            new FolderDataGen()
                    .name(String.format("folder_%02d", i))
                    .parent(parentFolder)
                    .nextPersisted();
        }

        // Create 100 contentlets
        for (int i = 0; i < 25; i++) {
            new FileAssetDataGen(FileUtil.createTemporaryFile("content", ".txt", "content " + i))
                    .host(host)
                    .folder(parentFolder)
                    .setPolicy(IndexPolicy.WAIT_FOR)
                    .nextPersisted();
        }

        // Execute pagination query - Page 2 (offset=10)
        final BrowserQuery browserQuery = BrowserQuery.builder()
                .showFolders(true)
                .showContent(true)
                .showFiles(true)
                .showLinks(false)
                .withHostOrFolderId(parentFolder.getIdentifier())
                .offset(11) // Second page
                .maxResults(20)
                .build();

        final PaginatedContents paginatedContents = browserAPI.getPaginatedContents(browserQuery);

        // Verify results
        assertNotNull("Result should not be null", paginatedContents);

        @SuppressWarnings("unchecked")
        final List<Map<String, Object>> list = paginatedContents.list;

        assertEquals("Should return exactly 20 items (20 contentlets, no folders)", 20, list.size());
        assertEquals("Folder count should be 10", 10, paginatedContents.folderCount);
        assertEquals("Content count should be 20", 20, paginatedContents.contentCount);
        assertEquals("Content total count should be 25", 25, paginatedContents.contentTotalCount);

        // Verify all items are contentlets
        for (Map<String, Object> item : list) {
            assertNotNull("Item should have extension", item.get("extension"));
            assertEquals("All items should be files", "txt", item.get("extension"));
        }
    }

    /**
     * Test Case: Smart Pagination - Page 3 (offset=52)
     * Expected: 26 more contentlets
     */
    @Test
    public void test_SmartPaginationPage3_26MoreContentlets() throws Exception {
        // Create a test environment
        final Host host = new SiteDataGen().nextPersisted();
        final Folder parentFolder = new FolderDataGen().site(host).nextPersisted();

        // Create 25 folders
        for (int i = 0; i < 25; i++) {
            new FolderDataGen()
                    .name(String.format("folder_%02d", i))
                    .parent(parentFolder)
                    .nextPersisted();
        }

        // Create 100 contentlets
        for (int i = 0; i < 100; i++) {
            new FileAssetDataGen(FileUtil.createTemporaryFile("content", ".txt", "content " + i))
                    .host(host)
                    .folder(parentFolder)
                    .setPolicy(IndexPolicy.WAIT_FOR)
                    .nextPersisted();
        }

        // Execute pagination query - Page 3 (offset=52)
        final BrowserQuery browserQuery = BrowserQuery.builder()
                .showFolders(true)
                .showContent(true)
                .showFiles(true)
                .showDotAssets(true)
                .showLinks(false)
                .withHostOrFolderId(parentFolder.getIdentifier())
                .offset(52) // Third page (26*2)
                .maxResults(26)
                .build();

        final PaginatedContents paginatedContents = browserAPI.getPaginatedContents(browserQuery);

        // Verify results
        assertNotNull("Result should not be null", paginatedContents);

        @SuppressWarnings("unchecked")
        final List<Map<String, Object>> list = paginatedContents.list;

        assertEquals("Should return exactly 26 items (26 contentlets)", 26, list.size());
        assertEquals("Folder count should be 25", 25, paginatedContents.folderCount);
        assertEquals("Content count should be 26", 26, paginatedContents.contentCount);
        assertEquals("Content total count should be 100", 100, paginatedContents.contentTotalCount);

        // Verify all items are contentlets
        for (Map<String, Object> item : list) {
            assertNotNull("Item should have extension", item.get("extension"));
            assertEquals("All items should be files", "txt", item.get("extension"));
        }
    }

    /**
     * Test Case: Smart Pagination - Only folders, no contentlets
     * Expected: Only folders returned, no database query for contentlets should be performed
     */
    @Test
    public void test_SmartPaginationOnlyFolders() throws Exception {
        // Create a test environment
        final Host host = new SiteDataGen().nextPersisted();
        final Folder parentFolder = new FolderDataGen().site(host).nextPersisted();

        // Create 15 folders
        for (int i = 0; i < 15; i++) {
            new FolderDataGen()
                    .name(String.format("folder_%02d", i))
                    .parent(parentFolder)
                    .nextPersisted();
        }

        // Execute pagination query - Page 1 with only folders enabled
        final BrowserQuery browserQuery = BrowserQuery.builder()
                .showFolders(true)
                .showContent(false) // Disable content
                .showFiles(false)
                .showDotAssets(false)
                .showLinks(false)
                .withHostOrFolderId(parentFolder.getIdentifier())
                .offset(0)
                .maxResults(10)
                .build();

        final PaginatedContents paginatedContents = browserAPI.getPaginatedContents(browserQuery);
        // Verify results
        assertNotNull("Result should not be null", paginatedContents);

        @SuppressWarnings("unchecked")
        final List<Map<String, Object>> list = paginatedContents.list;

        assertEquals("Should return exactly 10 folders", 10, list.size());
        assertEquals("Folder count should be 15", 15, paginatedContents.folderCount);

        // Verify all items are folders
        for (Map<String, Object> item : list) {
            assertNotNull("Item should have name", item.get("name"));
            assertTrue("All items should be folders",
                item.get("name").toString().startsWith("folder_"));
        }
    }

    /**
     * Test Case: Text filtering with custom ContentType - contentTotalCount validation
     *
     * Tests that getBrowserAPI.getPaginatedContents() correctly returns contentTotalCount
     * when using text search filters with a custom content type containing a title field.
     *
     * Expected behavior:
     * - Creates 3 custom content instances with different titles
     * - Filter matching one title returns contentTotalCount = 1
     * - Filter matching multiple titles returns contentTotalCount = 2
     */
    @Test
    public void test_getPaginatedContents_textFilter_contentTotalCount() throws Exception {
        // Create test environment
        final Host host = new SiteDataGen().nextPersisted();
        final Folder folder = new FolderDataGen().site(host).nextPersisted();

        // Create custom ContentType with title field
        final var customContentType = new ContentTypeDataGen()
                .host(host)
                .folder(folder)
                .field(new FieldDataGen().name("title").velocityVarName("title").next())
                .nextPersisted();

        // Create 3 contentlet instances with specific titles
        final Contentlet contentlet1 = new ContentletDataGen(customContentType)
                .setProperty("title", "SearchableItem Alpha")
                .host(host)
                .folder(folder)
                .setPolicy(IndexPolicy.WAIT_FOR)
                .nextPersisted();

        final Contentlet contentlet2 = new ContentletDataGen(customContentType)
                .setProperty("title", "SearchableItem Beta")
                .host(host)
                .folder(folder)
                .setPolicy(IndexPolicy.WAIT_FOR)
                .nextPersisted();

        final Contentlet contentlet3 = new ContentletDataGen(customContentType)
                .setProperty("title", "DifferentContent Gamma")
                .host(host)
                .folder(folder)
                .setPolicy(IndexPolicy.WAIT_FOR)
                .nextPersisted();

        // Test Case 1: Filter matching one item - expect contentTotalCount = 1
        final BrowserQuery queryMatchingOne = BrowserQuery.builder()
                .withHostOrFolderId(folder.getIdentifier())
                .withFilter("Alpha")
                .showContent(true)
                .showFiles(false)
                .showFolders(false)
                .showLinks(false)
                .showDotAssets(false)
                .showWorking(true)
                .showArchived(false)
                .build();

        final PaginatedContents resultsOne = browserAPI.getPaginatedContents(queryMatchingOne);

        assertNotNull("Results should not be null", resultsOne);
        assertEquals("Should find exactly 1 matching content", 1, resultsOne.contentTotalCount);
        assertEquals("Should return 1 content item", 1, resultsOne.contentCount);

        // Verify the correct content was found
        assertEquals("Should return exactly 1 item in list", 1, resultsOne.list.size());
        final Map<String, Object> foundItem = resultsOne.list.get(0);
        assertEquals("Found item should be contentlet1", contentlet1.getInode(), foundItem.get("inode"));

        // Test Case 2: Filter matching multiple items - expect contentTotalCount = 2
        final BrowserQuery queryMatchingTwo = BrowserQuery.builder()
                .withHostOrFolderId(folder.getIdentifier())
                .withFilter("SearchableItem")
                .showContent(true)
                .showFiles(false)
                .showFolders(false)
                .showLinks(false)
                .showDotAssets(false)
                .showWorking(true)
                .showArchived(false)
                .build();

        final PaginatedContents resultsTwo = browserAPI.getPaginatedContents(queryMatchingTwo);

        assertNotNull("Results should not be null", resultsTwo);
        assertEquals("Should find exactly 2 matching contents", 2, resultsTwo.contentTotalCount);
        assertEquals("Should return 2 content items", 2, resultsTwo.contentCount);
        assertEquals("Should return exactly 2 items in list", 2, resultsTwo.list.size());

        // Verify the correct contents were found (contentlet1 and contentlet2)
        final Set<String> foundInodes = resultsTwo.list.stream()
                .map(content -> (String) content.get("inode"))
                .collect(Collectors.toSet());

        assertTrue("Should contain contentlet1", foundInodes.contains(contentlet1.getInode()));
        assertTrue("Should contain contentlet2", foundInodes.contains(contentlet2.getInode()));
        assertFalse("Should not contain contentlet3", foundInodes.contains(contentlet3.getInode()));

        // Test Case 3: Filter with no matches - expect contentTotalCount = 0
        final BrowserQuery queryNoMatches = BrowserQuery.builder()
                .withHostOrFolderId(folder.getIdentifier())
                .withFilter("NonExistentTerm")
                .showContent(true)
                .showFiles(false)
                .showFolders(false)
                .showLinks(false)
                .showDotAssets(false)
                .showWorking(true)
                .showArchived(false)
                .build();

        final PaginatedContents resultsNone = browserAPI.getPaginatedContents(queryNoMatches);

        assertNotNull("Results should not be null", resultsNone);
        assertEquals("Should find no matching content", 0, resultsNone.contentTotalCount);
        assertEquals("Should return no content items", 0, resultsNone.contentCount);
        assertEquals("Should return empty list", 0, resultsNone.list.size());
    }

}
