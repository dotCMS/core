package com.dotcms.browser;

import com.dotcms.IntegrationTestBase;
import com.dotcms.datagen.*;
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
import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

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

    private static BrowserAPIImpl browserAPIImpl;

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

}
