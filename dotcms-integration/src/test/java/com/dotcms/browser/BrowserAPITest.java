package com.dotcms.browser;

import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.dotcms.IntegrationTestBase;
import com.dotcms.browser.BrowserAPIImpl.PaginatedContents;
import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.contenttype.model.field.StoryBlockField;
import com.dotcms.contenttype.model.field.TextAreaField;
import com.dotcms.contenttype.model.field.WysiwygField;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.DotAssetDataGen;
import com.dotcms.datagen.FieldDataGen;
import com.dotcms.datagen.FileAssetDataGen;
import com.dotcms.datagen.FolderDataGen;
import com.dotcms.datagen.HTMLPageDataGen;
import com.dotcms.datagen.LanguageDataGen;
import com.dotcms.datagen.LinkDataGen;
import com.dotcms.rest.api.v1.content.search.handlers.FieldContext;
import com.dotcms.rest.api.v1.content.search.strategies.GlobalSearchAttributeStrategy;
import com.dotcms.datagen.RoleDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.TemplateDataGen;
import com.dotcms.datagen.TestDataUtils;
import com.dotcms.datagen.TestUserUtils;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.datagen.VariantDataGen;
import com.dotcms.util.IntegrationTestInitService;
import com.dotcms.variant.model.Variant;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Role;
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
import com.dotmarketing.util.Config;
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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
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

    // Fixture for the MIME-type routing tests -- https://github.com/dotCMS/core/issues/36916
    static final String DOTPAGE_MIME_TYPE = "application/dotpage";
    static Host mimeHost;
    static Folder mimeFolder, mimeSubFolder;
    static HTMLPageAsset mimePage, mimePageAltLanguage, mimeSubFolderPage;
    static FileAsset mimeJpgFile, mimePdfFile, mimeTxtFile, mimeSubFolderJpgFile;
    static Link mimeLink;

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

        seedMimeTypeFixture();
    }

    /**
     * Seeds a dedicated Site and folder tree for the MIME-type routing tests of
     * <a href="https://github.com/dotCMS/core/issues/36916">issue #36916</a>. It is kept apart from the
     * fixture above so the assertions can be exact about which items a MIME-filtered browse returns.
     * <pre>
     * mimeFolder
     *      |_ mimePage             HTMLPAGE,  default language
     *      |_ mimePageAltLanguage  HTMLPAGE,  testLanguage
     *      |_ mimeJpgFile          FILEASSET, image/jpeg
     *      |_ mimePdfFile          FILEASSET, application/pdf
     *      |_ mimeTxtFile          FILEASSET, text/plain
     *      |_ mimeLink             LINK
     *      |_ mimeSubFolder
     *          |_ mimeSubFolderPage      HTMLPAGE,  default language
     *          |_ mimeSubFolderJpgFile   FILEASSET, image/jpeg
     * </pre>
     */
    private static void seedMimeTypeFixture() throws Exception {
        mimeHost = new SiteDataGen().nextPersisted();
        mimeFolder = new FolderDataGen().name("mimeFolder").site(mimeHost).nextPersisted();
        mimeSubFolder = new FolderDataGen().name("mimeSubFolder").parent(mimeFolder).nextPersisted();

        final Template template = new TemplateDataGen().host(mimeHost).nextPersisted();

        mimePage = new HTMLPageDataGen(mimeFolder, template).title("mimePage").pageURL("mime-page")
                .nextPersisted();
        mimePageAltLanguage = new HTMLPageDataGen(mimeFolder, template).title("mimePageAltLanguage")
                .pageURL("mime-page-alt-language").languageId(testLanguage.getId()).nextPersisted();
        mimeSubFolderPage = new HTMLPageDataGen(mimeSubFolder, template).title("mimeSubFolderPage")
                .pageURL("mime-sub-folder-page").nextPersisted();

        mimeJpgFile = persistFileAsset(mimeFolder, copyTestResource("/images/test.jpg", "mimeJpgFile", ".jpg"));
        mimePdfFile = persistFileAsset(mimeFolder, copyTestResource(
                "/com/dotmarketing/portlets/contentlet/business/test_files/test.pdf", "mimePdfFile", ".pdf"));
        mimeTxtFile = persistFileAsset(mimeFolder,
                FileUtil.createTemporaryFile("mimeTxtFile", ".txt", "this is a test!"));
        mimeSubFolderJpgFile = persistFileAsset(mimeSubFolder,
                copyTestResource("/images/test.jpg", "mimeSubFolderJpgFile", ".jpg"));

        mimeLink = new LinkDataGen().hostId(mimeHost.getIdentifier()).title("mimeLink").parent(mimeFolder)
                .target("https://google.com").linkType("EXTERNAL").nextPersisted();
    }

    /**
     * Copies a classpath test resource into a temporary file so that a File Asset can be generated from it. The
     * file extension matters here: the asset metadata -- and therefore the {@code contentType} the SQL MIME
     * predicate looks at -- is derived from the actual file contents on check-in.
     */
    private static File copyTestResource(final String resourcePath, final String prefix, final String suffix)
            throws IOException {
        final URL url = BrowserAPITest.class.getResource(resourcePath);
        assertNotNull("Test resource must exist in the classpath: " + resourcePath, url);
        final File tempFile = File.createTempFile(prefix, suffix);
        FileUtils.copyFile(new File(url.getFile()), tempFile);
        return tempFile;
    }

    private static FileAsset persistFileAsset(final Folder folder, final File file)
            throws DotDataException, DotSecurityException {
        return APILocator.getFileAssetAPI().fromContentlet(new FileAssetDataGen(file).folder(folder)
                .setPolicy(IndexPolicy.WAIT_FOR).nextPersisted());
    }

    /**
     * Runs a browse against the MIME-type fixture folder through {@link BrowserAPI#getFolderContentList(BrowserQuery)}
     * and returns the Identifiers that came back. This entry point deliberately has <b>no</b> in-memory MIME filter
     * (see research R2), so what it returns is what the SQL predicate itself selected.
     */
    private Set<String> browseIdentifiers(final Folder folder, final List<String> mimeTypes)
            throws DotSecurityException, DotDataException {
        return browserAPI.getFolderContentList(BrowserQuery.builder()
                        .withUser(APILocator.systemUser())
                        .withHostOrFolderId(folder.getIdentifier())
                        .showPages(true)
                        .showFiles(true)
                        .showDotAssets(true)
                        .showFolders(false)
                        .showWorking(true)
                        .showMimeTypes(mimeTypes)
                        .build()).stream()
                .map(Treeable::getIdentifier)
                .collect(Collectors.toSet());
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

        final Host host = testHost;
        final Folder folder = new FolderDataGen().site(host)
                .name("multilang-" + System.currentTimeMillis())
                .nextPersisted();

        final File file = FileUtil.createTemporaryFile("test", ".txt", "this is a test!");

        final Contentlet persisted = new FileAssetDataGen(file)
                .languageId(1)
                .host(host)
                .folder(folder)
                .setPolicy(IndexPolicy.WAIT_FOR).nextPersisted();

        final ContentletAPI contentletAPI = APILocator.getContentletAPI();

        List<Long> languages = new ArrayList<>();
        languages.add(persisted.getLanguageId());
        languages.add(testLanguage.getId());
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
        final Host host = testHost;
        final Folder folder = new FolderDataGen().site(host)
                .name("pagination-" + System.currentTimeMillis())
                .nextPersisted();

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
        assertTrue("Should contain dotraw title search", result.contains("title_dotraw:*test*"));
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
     *     <li><b>Given Scenario:</b> A free-text filter is provided.</li>
     *     <li><b>Expected Result:</b> The text clause is produced by the shared
     *     {@link GlobalSearchAttributeStrategy} — the same one the Content Search portlet uses — and
     *     no longer by a hand-rolled query string.</li>
     * </ul>
     */
    @Test
    public void test_buildBaseESQuery_delegatesToSharedGlobalSearchStrategy() {
        final BrowserAPIImpl browserAPIImpl = new BrowserAPIImpl();
        final String filter = "searchterm";

        final String result = browserAPIImpl.buildBaseESQuery(
                BrowserQuery.builder().withFilter(filter).build());

        // The free-text clause must be byte-identical to what the Content Search portlet builds,
        // so both surfaces always query the index the same way (issue #36688).
        final String expectedTextGroup = new GlobalSearchAttributeStrategy().generateQuery(
                new FieldContext.Builder()
                        .withFieldName("title")
                        .withFieldValue(filter)
                        .build());
        assertEquals("Text group must be exactly what the shared global-search strategy produces",
                " +(" + expectedTextGroup + ")", result);

        // Guards against regressing to the previous hand-rolled query, which used a broad,
        // unscoped leading-wildcard catchall (slow, matched unrelated body text). Whether the
        // strategy itself uses ' OR ' internally is its own concern — the assertEquals above
        // already pins this method to whatever it produces.
        assertFalse("Must not use a leading-wildcard catchall clause",
                result.contains("catchall:*"));
    }

    /**
     * <ul>
     *     <li><b>Method to Test:</b> {@link BrowserAPIImpl#buildBaseESQuery(BrowserQuery)}</li>
     *     <li><b>Given Scenario:</b> Verify the generated query complies with Lucene syntax.</li>
     *     <li><b>Expected Result:</b> Query uses valid Lucene field:value syntax.</li>
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
        final User owner = new UserDataGen().nextPersisted();
        // Create a test environment
        final Host host = new SiteDataGen().nextPersisted();
        final Folder parentFolder = new FolderDataGen().site(host).nextPersisted();

        // Create 25 folders
        final List<Folder> subFolders = new ArrayList<>();
        for (int i = 0; i < 25; i++) {
            final Folder subFolder = new FolderDataGen()
                    .name(String.format("folder_%02d", i))
                    .parent(parentFolder)
                    .owner(owner)
                    .nextPersisted();
            subFolders.add(subFolder);
        }

        // Create 30 contentlets
        for (int i = 0; i < 30; i++) {
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

        // Verify first 25 items are folders
        for (int i = 0; i < 25; i++) {
            final Map<String, Object> item = list.get(i);
            assertNotNull("Item should have name", item.get("name"));
            assertTrue("First 25 items should be folders",
                item.get("name").toString().startsWith("folder_"));
            assertEquals("Owner should be the same as parent folder",
                owner.getFullName(), item.get("owner"));
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
    public void test_SmartPaginationPage2_15Contentlets() throws Exception {
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

        // Create 25 contentlets
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
                .folderCursor(10) // Second page
                .contentCursor(10)
                .maxResults(20)
                .build();

        final PaginatedContents paginatedContents = browserAPI.getPaginatedContents(browserQuery);

        // Verify results
        assertNotNull("Result should not be null", paginatedContents);

        @SuppressWarnings("unchecked")
        final List<Map<String, Object>> list = paginatedContents.list;

        assertEquals("Should return exactly 15 items (15 contentlets, no folders)", 15, list.size());
        assertEquals("Folder count should be 0 the 10 folders where in the first page", 0, paginatedContents.folderCount);
        assertFalse("Should indicate NO more folders available", paginatedContents.hasMoreFolders);
        assertEquals("Content count should be 15", 15, paginatedContents.contentCount);
        assertFalse("Should indicate NO more content available", paginatedContents.hasMoreContent);
    }

    /**
     * Test Case: Smart Pagination - Page 3 (offset=52)
     * Expected: 26 more contentlets
     */
    @Test
    public void test_SmartPaginationPage3_16MoreContentlets() throws Exception {
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

        // Create 50 contentlets
        for (int i = 0; i < 50; i++) {
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
                .folderCursor(15)
                .contentCursor(17)  // Third page (16*2) = 32 (15 folders and 17 contents)
                .maxResults(16)
                .build();

        final PaginatedContents paginatedContents = browserAPI.getPaginatedContents(browserQuery);

        // Verify results
        assertNotNull("Result should not be null", paginatedContents);

        @SuppressWarnings("unchecked")
        final List<Map<String, Object>> list = paginatedContents.list;

        assertEquals("Should return exactly 16 items (16 contentlets)", 16, list.size());
        assertEquals("Folder count should be 0", 0, paginatedContents.folderCount);
        assertFalse("Should indicate NO more folders available", paginatedContents.hasMoreFolders);
        assertEquals("Content count should be 16", 16, paginatedContents.contentCount);
        assertTrue("Should indicate more content available", paginatedContents.hasMoreContent);
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
        assertEquals("Folder count should be 10", 10, paginatedContents.folderCount);
        assertTrue("Should indicate more folders available", paginatedContents.hasMoreFolders);

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
        // Create a test environment
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
        assertEquals("Should return no content items", 0, resultsNone.contentCount);
        assertEquals("Should return empty list", 0, resultsNone.list.size());
    }

    /**
     * Method to test <li><b>Method to Test:</b> {@link BrowserAPI#getPaginatedContents(BrowserQuery)}</li>
     * Given scenario: Here we test a similar situation as above, but we set limits in the pageSize
     * to verify that the total count accurately reflects the total items in existence reflected in the contentTotalCount
     * Expected result: We should expect 5 matches filling the first page and a universe of 10 items
     * @throws Exception
     */
    @Test
    public void test_getPaginatedContents_Fixed_Page_Size_Using_textFilter_Verify_contentTotalCount() throws Exception {
        // Create a test environment
        final Host host = new SiteDataGen().nextPersisted();
        final Folder folder = new FolderDataGen().site(host).nextPersisted();

        // Create custom ContentType with title field
        final var customContentType = new ContentTypeDataGen()
                .host(host)
                .folder(folder)
                .field(new FieldDataGen().name("title").velocityVarName("title").next())
                .nextPersisted();

        for(int i=0; i<10; i++) {
            new ContentletDataGen(customContentType)
                    .setProperty("title", String.format("SearchableItem %s",i))
                    .host(host)
                    .folder(folder)
                    .setPolicy(IndexPolicy.WAIT_FOR)
                    .nextPersisted();
        }

        final BrowserQuery query = BrowserQuery.builder()
                .withHostOrFolderId(folder.getIdentifier())
                .withFilter("Item")
                .showContent(true)
                .showFiles(false)
                .showFolders(false)
                .showLinks(false)
                .showDotAssets(false)
                .showWorking(true)
                .showArchived(false)
                .offset(0)
                .maxResults(5)
                .build();

        final PaginatedContents resultsOne = browserAPI.getPaginatedContents(query);

        assertNotNull("Results should not be null", resultsOne);
        assertEquals("Should return 5 matching item as we defined a pageSize of 5.", 5, resultsOne.contentCount);

    }

    /**
     * Method to test <li><b>Method to Test:</b> {@link BrowserAPI#getPaginatedContents(BrowserQuery)}</li>
     * Given scenario: We're creating content under a folder and giving read access to a user then we request such content
     * Expected Results: We should get back the requested content
     * @throws Exception
     */
    @Test
    public void test_getContent_Using_LimitedUser_WithRead_Permissions() throws Exception {
        final Host host = new SiteDataGen().nextPersisted(true);
        final Folder folder = new FolderDataGen().site(host).nextPersisted();
        final User limitedUser = TestUserUtils.getChrisPublisherUser(host);
        final PermissionAPI permissionAPI = APILocator.getPermissionAPI();
        //Give him access to the site and parent folder
        final Permission siteReadPermissions = new Permission(host.getPermissionId(),
                APILocator.getRoleAPI().getUserRole(limitedUser).getId(), PermissionAPI.PERMISSION_READ );
        permissionAPI.save(siteReadPermissions, host, APILocator.systemUser(), false);

        //We need to assign Chris Publisher view permissions to the parent folder.
        final Permission folderReadPermission = new Permission(folder.getPermissionId(),
                APILocator.getRoleAPI().getUserRole(limitedUser).getId(), PermissionAPI.PERMISSION_READ );
        permissionAPI.save(folderReadPermission, folder, APILocator.systemUser(), false);

        final File file = FileUtil.createTemporaryFile("content", ".txt", "content");
        final Contentlet contentlet = new FileAssetDataGen(file)
                .host(host)
                .folder(folder)
                .setPolicy(IndexPolicy.WAIT_FOR)
                .nextPersisted();
        assertNotNull(contentlet.getIdentifier());
        assertFalse(contentlet.isLive());

        final boolean hasReadPermission = permissionAPI.doesUserHavePermission(contentlet,
                PermissionAPI.PERMISSION_READ, limitedUser, false);
        assertTrue("This should have read Permissions", hasReadPermission);

        final BrowserQuery query = BrowserQuery.builder()
                .withHostOrFolderId(folder.getInode())
                .ignoreSiteForFolders(true)
                .respectFrontEndRoles(false) // <-- This is key for this test!
                .withUser(limitedUser)
                .forceSystemHost(false)
                .showContent(true)
                .showFiles(false)
                .showFolders(false)
                .showLinks(false)
                .showDotAssets(false)
                .showWorking(true)
                .showArchived(false)
                .offset(0)
                .maxResults(5)
                .build();
        final PaginatedContents results = browserAPI.getPaginatedContents(query);
        assertEquals("Should return 1 content item", 1, results.contentCount);
        assertEquals("Should return exactly 1 item in list", 1, results.list.size());
        assertEquals("Contentlet inode should match", contentlet.getInode(), results.list.get(0).get("inode"));
    }

    /**
     * Method to test exhaustive pagination with permission filtering using getContentUnderParentFromDB
     * <li><b>Method to Test:</b> {@link BrowserAPIImpl#getContentUnderParentFromDB(BrowserQuery, int)}</li>
     * Given scenario: Creating alternating content with and without read permissions in the same folder,
     * then testing that pagination system exhaustively collects enough accessible content to complete
     * the requested page size despite permission filtering reducing intermediate results.
     * Expected Results: The pagination system should return the requested page size by iteratively
     * searching for additional accessible content when permission filtering creates gaps.
     * @throws Exception
     */
    @Test
    public void test_exhaustive_pagination_with_permission_filtering() throws Exception {
        final Host host = new SiteDataGen().nextPersisted(true);
        final Folder folder = new FolderDataGen().site(host).nextPersisted();
        final PermissionAPI permissionAPI = APILocator.getPermissionAPI();
        // Create a minimal backend user with no Publisher role (avoids broad READ from Publisher role).
        // The Backend User role is required so the user can read non-live (working) contentlets.
        final Role restrictedRole = new RoleDataGen().nextPersisted();
        final User limitedUser = new UserDataGen()
                .roles(restrictedRole, TestUserUtils.getBackendRole())
                .nextPersisted();
        // A separate role (not assigned to limitedUser) used to break inheritance on odd contentlets
        final Role noAccessRole = new RoleDataGen().nextPersisted();

        // Give limited user access to the site and parent folder
        final Permission siteReadPermissions = new Permission(host.getPermissionId(),
                APILocator.getRoleAPI().getUserRole(limitedUser).getId(), PermissionAPI.PERMISSION_READ);
        permissionAPI.save(siteReadPermissions, host, APILocator.systemUser(), false);

        final Permission folderReadPermission = new Permission(folder.getPermissionId(),
                APILocator.getRoleAPI().getUserRole(limitedUser).getId(), PermissionAPI.PERMISSION_READ);
        permissionAPI.save(folderReadPermission, folder, APILocator.systemUser(), false);

        // Create alternating content: accessible and non-accessible
        // This ensures non-continuous distribution in the database
        final List<Contentlet> accessibleContentlets = new ArrayList<>();

        // Create 20 pieces of content, alternating permissions (10 accessible, 10 non-accessible)
        for (int i = 0; i < 20; i++) {
            final File file = FileUtil.createTemporaryFile("content(" + i + ")", ".txt", "content-" + i);
            final Contentlet contentlet = new FileAssetDataGen(file)
                    .host(host)
                    .folder(folder)
                    .setPolicy(IndexPolicy.WAIT_FOR)
                    .nextPersisted();

            // Give read permission to every other contentlet (even indices: 0, 2, 4, 6, ...)
            if (i % 2 == 0) {
                final Permission contentletReadPermission = new Permission(contentlet.getPermissionId(),
                        APILocator.getRoleAPI().getUserRole(limitedUser).getId(), PermissionAPI.PERMISSION_READ);
                permissionAPI.save(contentletReadPermission, contentlet, APILocator.systemUser(), false);
                accessibleContentlets.add(contentlet);
            } else {
                // Odd: break the inheritance chain by setting explicit permissions for noAccessRole only.
                // limitedUser does not have noAccessRole, so it cannot read these contentlets even
                // though the parent folder grants READ (this explicit entry overrides inheritance).
                final Permission noAccessPermission = new Permission(contentlet.getPermissionId(),
                        noAccessRole.getId(), PermissionAPI.PERMISSION_READ);
                permissionAPI.save(noAccessPermission, contentlet, APILocator.systemUser(), false);
            }
        }

        // Wait for indexing to complete
        await().atMost(Duration.ofSeconds(10)).until(() -> {
            // Verify that all contentlets have been indexed and permissions are properly applied
            return accessibleContentlets.stream().allMatch(contentlet -> {
                try {
                    return permissionAPI.doesUserHavePermission(contentlet, PermissionAPI.PERMISSION_READ, limitedUser, false);
                } catch (Exception e) {
                    return false;
                }
            });
        });

        // Verify permission setup: should have 10 accessible contentlets
        assertEquals("Should have created 10 accessible contentlets", 10, accessibleContentlets.size());

        // Test Case 1: Request page size of 5 - should get exactly 5 accessible items
        final BrowserQuery query1 = BrowserQuery.builder()
                .withHostOrFolderId(folder.getInode())
                .ignoreSiteForFolders(true)
                .respectFrontEndRoles(false)
                .withUser(limitedUser)
                .forceSystemHost(false)
                .showContent(true)
                .contentCursor(0)
                .showFiles(false)
                .showFolders(false)
                .showLinks(false)
                .showDotAssets(false)
                .showWorking(true)
                .showArchived(false)
                .build();

        // Using reflection to access the package-private method for direct testing
        final BrowserAPIImpl browserAPIImpl = (BrowserAPIImpl) browserAPI;

        final var results1 = browserAPIImpl.getContentUnderParentFromDB(query1, 5);
        assertEquals("Should return exactly 5 accessible contentlets", 5, results1.contentlets.size());
        assertTrue("Should indicate more pages available", results1.hasMore);

        // Test Case 2: Request page size of 8 - should get exactly 8 accessible items
        final var results2 = browserAPIImpl.getContentUnderParentFromDB(query1, 8);
        assertEquals("Should return exactly 8 accessible contentlets", 8, results2.contentlets.size());
        assertTrue("Should indicate more pages available", results2.hasMore);

        // Test Case 3: Request page size of 10 - should get all 10 accessible items
        final var results3 = browserAPIImpl.getContentUnderParentFromDB(query1, 10);
        assertEquals("Should return exactly 10 accessible contentlets", 10, results3.contentlets.size());
        assertFalse("Should indicate no more pages available", results3.hasMore);

        // Test Case 4: Request more than available - should get all 10 accessible items
        final var results4 = browserAPIImpl.getContentUnderParentFromDB(query1, 15);
        assertEquals("Should return all 10 accessible contentlets", 10, results4.contentlets.size());
        assertFalse("Should indicate no more pages available", results4.hasMore);

        // Test Case 5: Cursor-based pagination
        // first page returns 5 items and a cursor,
        // second page continues from that cursor and returns the remaining 5 items.
        final var results5 = browserAPIImpl.getContentUnderParentFromDB(query1, 5);
        assertEquals("Should return exactly 5 accessible contentlets on page 1", 5, results5.contentlets.size());
        assertTrue("Should indicate more pages available after page 1", results5.hasMore);

        // Build query2 from query1, advancing only the contentCursor
        final BrowserQuery query2 = BrowserQuery.from(query1)
                .contentCursor(results5.nextDbCursor)
                .build();
        final var results6 = browserAPIImpl.getContentUnderParentFromDB(query2, 8);
        assertEquals("Should return remaining 5 accessible contentlets on page 2", 5, results6.contentlets.size());
        assertFalse("Should indicate no more pages available after page 2", results6.hasMore);
 }

    /**
     * <ul>
     *     <li><b>Method to Test:</b> {@link BrowserAPI#getPaginatedContents(BrowserQuery)}</li>
     *     <li><b>Given Scenario:</b> The folder contains exactly N sub-folders and some contentlets.
     *     When the client requests a page of size N, folders fill the page completely, leaving
     *     {@code maxResults = 0} before the content block is reached.</li>
     *     <li><b>Expected Result:</b> Even though no contentlets are added to the current page,
     *     {@code hasMoreContent} must be {@code true} so the client knows a next page exists.
     *     The {@code nextContentCursor} must remain at 0 since no content was consumed yet.</li>
     * </ul>
     */
    @Test
    public void test_getPaginatedContents_foldersExactlyFillPage_hasMoreContentIsTrue()
            throws Exception {

        final Host host = new SiteDataGen().nextPersisted();
        final Folder parentFolder = new FolderDataGen().site(host).nextPersisted();

        // Create exactly 5 sub-folders
        final int folderCount = 5;
        for (int i = 0; i < folderCount; i++) {
            new FolderDataGen()
                    .name(String.format("folder_%02d", i))
                    .parent(parentFolder)
                    .nextPersisted();
        }

        // Create 3 contentlets — they must NOT appear in this page but must be detectable
        for (int i = 0; i < 3; i++) {
            new FileAssetDataGen(FileUtil.createTemporaryFile("content", ".txt", "content " + i))
                    .host(host)
                    .folder(parentFolder)
                    .setPolicy(IndexPolicy.WAIT_FOR)
                    .nextPersisted();
        }

        // Request exactly as many results as there are folders — page is filled by folders alone
        final BrowserQuery query = BrowserQuery.builder()
                .showFolders(true)
                .showContent(true)
                .showFiles(true)
                .showDotAssets(true)
                .showLinks(false)
                .withHostOrFolderId(parentFolder.getIdentifier())
                .folderCursor(0)
                .contentCursor(0)
                .maxResults(folderCount)
                .build();

        final PaginatedContents result = browserAPI.getPaginatedContents(query);

        assertNotNull("Result should not be null", result);
        assertEquals("Page should contain only the folders", folderCount, result.list.size());
        assertEquals("folderCount should equal the number of sub-folders", folderCount, result.folderCount);
        assertEquals("contentCount should be 0 — no content added to this page", 0, result.contentCount);
        assertFalse("hasMoreFolders should be false — all folders fit in this page", result.hasMoreFolders);
        assertTrue("hasMoreContent must be true — content exists but was not yet shown", result.hasMoreContent);
        assertEquals("nextContentCursor should stay at 0 — no content was consumed", 0, result.nextContentCursor);
    }

    /**
     * <ul>
     *     <li><b>Method to Test:</b> {@link BrowserAPI#getPaginatedContents(BrowserQuery)}</li>
     *     <li><b>Given Scenario:</b> A site contains 20 content items but
     *     {@code BROWSER_DB_MAX_SCAN_ROWS} is intentionally set to 15, lower than the total row
     *     count. The scan loop must stop as soon as the number of rows scanned exceeds the limit,
     *     preventing runaway queries on large sites with heavily restricted users.</li>
     *     <li><b>Expected Result:</b> The request completes without hanging or throwing an
     *     exception. The result contains whatever items were accumulated before the limit was hit,
     *     and {@code nextContentCursor} reflects how far the scan reached (&gt; 0).</li>
     * </ul>
     */
    @Test
    public void test_getPaginatedContents_scanLimitStopsLoop() throws Exception {
        final int scanLimit = 15;
        final int itemCount = 20;

        Config.setProperty(BrowserAPIImpl.BROWSER_DB_MAX_SCAN_ROWS_KEY, scanLimit);
        try {
            final Host host = new SiteDataGen().nextPersisted();
            final Folder folder = new FolderDataGen().site(host).nextPersisted();

            for (int i = 0; i < itemCount; i++) {
                final File file = FileUtil.createTemporaryFile("scan-limit-" + i, ".txt", "content " + i);
                new FileAssetDataGen(file).folder(folder).host(host).nextPersisted();
            }

            // Query against the site root (no folder filter) so all 20 items are in scope
            final BrowserQuery query = BrowserQuery.builder()
                    .withUser(APILocator.systemUser())
                    .withHostOrFolderId(host.getIdentifier())
                    .skipFolder(true)
                    .showFiles(true)
                    .showWorking(true)
                    .showFolders(false)
                    .maxResults(100)
                    .contentCursor(0)
                    .build();

            final PaginatedContents result = browserAPI.getPaginatedContents(query);

            assertNotNull("Result must not be null when scan limit is reached", result);
            // 20 items were accumulated before the scan limit fired (dbOffset 20 >= scanLimit 15)
            assertTrue("Should have returned items accumulated before the scan limit",
                    result.contentCount > 0);
            // Cursor must reflect how far into the DB the scan reached
            assertTrue("nextContentCursor should be > 0 since rows were scanned",
                    result.nextContentCursor > 0);
        } finally {
            Config.setProperty(BrowserAPIImpl.BROWSER_DB_MAX_SCAN_ROWS_KEY,
                    BrowserAPIImpl.BROWSER_DB_MAX_SCAN_ROWS_DEFAULT);
        }
    }

    /**
     * <ul>
     *     <li><b>Method to test:</b> {@link BrowserAPI#getFolderContentList(BrowserQuery)}</li>
     *     <li><b>Given Scenario:</b> A folder holding Pages, File Assets and a Link is browsed filtering by the
     *     synthetic {@code application/dotpage} MIME type -- the value the legacy redirect target picker sends.</li>
     *     <li><b>Expected Result:</b> The Pages under the folder are returned and no File Asset is. The synthetic
     *     MIME type is never persisted to {@code contentlet_as_json}, so it can only resolve by HTMLPAGE base
     *     type.</li>
     * </ul>
     * Contract case C1 -- AC-001 and AC-006 of <a href="https://github.com/dotCMS/core/issues/36916">#36916</a>.
     */
    @Test
    public void test_getFolderContentList_dotPageMimeType_returnsPages() throws Exception {
        final Set<String> identifiers = browseIdentifiers(mimeFolder, List.of(DOTPAGE_MIME_TYPE));

        assertTrue("Pages must be returned for an '" + DOTPAGE_MIME_TYPE + "' browse, but got: " + identifiers,
                identifiers.containsAll(
                        Set.of(mimePage.getIdentifier(), mimePageAltLanguage.getIdentifier())));
        assertFalse("The JPG File Asset must not be returned for an '" + DOTPAGE_MIME_TYPE + "' browse",
                identifiers.contains(mimeJpgFile.getIdentifier()));
        assertFalse("The PDF File Asset must not be returned for an '" + DOTPAGE_MIME_TYPE + "' browse",
                identifiers.contains(mimePdfFile.getIdentifier()));
        assertFalse("The TXT File Asset must not be returned for an '" + DOTPAGE_MIME_TYPE + "' browse",
                identifiers.contains(mimeTxtFile.getIdentifier()));
    }

    /**
     * <ul>
     *     <li><b>Method to test:</b> {@link BrowserAPI#getFolderContentList(BrowserQuery)}</li>
     *     <li><b>Given Scenario:</b> The same folder is browsed filtering by a real MIME type, {@code image/jpeg},
     *     with Pages enabled.</li>
     *     <li><b>Expected Result:</b> Only the matching File Asset comes back. Pages must be dropped by the SQL
     *     predicate itself, since two of the three consumers of the query have no in-memory MIME filter.</li>
     * </ul>
     * Contract case C2, invariant I2 -- AC-005 and AC-007 of
     * <a href="https://github.com/dotCMS/core/issues/36916">#36916</a>.
     */
    @Test
    public void test_getFolderContentList_realMimeType_returnsMatchingFilesAndNoPages() throws Exception {
        final Set<String> identifiers = browseIdentifiers(mimeFolder, List.of("image/jpeg"));

        assertTrue("The JPG File Asset must be returned for an 'image/jpeg' browse, but got: " + identifiers,
                identifiers.contains(mimeJpgFile.getIdentifier()));
        assertFalse("The PDF File Asset must not match 'image/jpeg'",
                identifiers.contains(mimePdfFile.getIdentifier()));
        assertFalse("The TXT File Asset must not match 'image/jpeg'",
                identifiers.contains(mimeTxtFile.getIdentifier()));
        assertFalse("Pages must never leak into a real MIME type browse",
                identifiers.contains(mimePage.getIdentifier()));
        assertFalse("Pages must never leak into a real MIME type browse",
                identifiers.contains(mimePageAltLanguage.getIdentifier()));
    }

    /**
     * <ul>
     *     <li><b>Method to test:</b> {@link BrowserAPI#getFolderContentList(BrowserQuery)}</li>
     *     <li><b>Given Scenario:</b> The folder is browsed filtering by two real MIME types at once.</li>
     *     <li><b>Expected Result:</b> Both matching File Assets come back and nothing else. This is the pure
     *     regression guard for the MIME filtering added by PR #34217, and it must behave identically before and
     *     after the fix.</li>
     * </ul>
     * Contract case C4, invariant I1 -- AC-005 of
     * <a href="https://github.com/dotCMS/core/issues/36916">#36916</a>.
     */
    @Test
    public void test_getFolderContentList_multipleRealMimeTypes_areUnchanged() throws Exception {
        final Set<String> identifiers = browseIdentifiers(mimeFolder, List.of("image/jpeg", "application/pdf"));

        assertTrue("The JPG File Asset must be returned, but got: " + identifiers,
                identifiers.contains(mimeJpgFile.getIdentifier()));
        assertTrue("The PDF File Asset must be returned, but got: " + identifiers,
                identifiers.contains(mimePdfFile.getIdentifier()));
        assertFalse("The TXT File Asset matches neither MIME type",
                identifiers.contains(mimeTxtFile.getIdentifier()));
        assertFalse("Pages must never leak into a real MIME type browse",
                identifiers.contains(mimePage.getIdentifier()));
    }

    /**
     * <ul>
     *     <li><b>Method to test:</b> {@link BrowserAPI#getFolderContentList(BrowserQuery)}</li>
     *     <li><b>Given Scenario:</b> The folder is browsed with a MIME type that merely starts with the synthetic
     *     one, {@code application/dotpage-foo}.</li>
     *     <li><b>Expected Result:</b> No Page comes back. Only the exact string {@code application/dotpage} may be
     *     routed to the base type branch; anything else goes through the asset metadata check.</li>
     * </ul>
     * Invariant I3 of <a href="https://github.com/dotCMS/core/issues/36916">#36916</a>.
     */
    @Test
    public void test_getFolderContentList_dotPageMimeTypePrefix_isNotRoutedToBaseType() throws Exception {
        final Set<String> identifiers = browseIdentifiers(mimeFolder, List.of(DOTPAGE_MIME_TYPE + "-foo"));

        assertFalse("Only the exact '" + DOTPAGE_MIME_TYPE + "' may resolve Pages by base type",
                identifiers.contains(mimePage.getIdentifier()));
        assertFalse("Only the exact '" + DOTPAGE_MIME_TYPE + "' may resolve Pages by base type",
                identifiers.contains(mimePageAltLanguage.getIdentifier()));
    }

    /**
     * <ul>
     *     <li><b>Method to test:</b> {@link BrowserAPI#getFolderContentList(BrowserQuery)}</li>
     *     <li><b>Given Scenario:</b> The folder is browsed asking for the synthetic Page MIME type and a real one
     *     at the same time -- a folder holding a mix of Pages, File Assets and a Link.</li>
     *     <li><b>Expected Result:</b> Both the Pages and the matching File Asset come back; the File Assets that
     *     match neither requested MIME type do not.</li>
     * </ul>
     * Contract case C3 -- AC-004 of <a href="https://github.com/dotCMS/core/issues/36916">#36916</a>.
     */
    @Test
    public void test_getFolderContentList_mixedMimeTypes_returnsPagesAndMatchingFiles() throws Exception {
        final Set<String> identifiers =
                browseIdentifiers(mimeFolder, List.of(DOTPAGE_MIME_TYPE, "image/jpeg"));

        assertTrue("Pages must be returned for a mixed browse, but got: " + identifiers,
                identifiers.containsAll(
                        Set.of(mimePage.getIdentifier(), mimePageAltLanguage.getIdentifier())));
        assertTrue("The JPG File Asset must be returned for a mixed browse, but got: " + identifiers,
                identifiers.contains(mimeJpgFile.getIdentifier()));
        assertFalse("The PDF File Asset matches neither requested MIME type",
                identifiers.contains(mimePdfFile.getIdentifier()));
        assertFalse("The TXT File Asset matches neither requested MIME type",
                identifiers.contains(mimeTxtFile.getIdentifier()));
    }

    /**
     * <ul>
     *     <li><b>Method to test:</b> {@link BrowserAPI#getFolderContentList(BrowserQuery)}</li>
     *     <li><b>Given Scenario:</b> The same mixed browse is requested twice with the MIME types in opposite
     *     order.</li>
     *     <li><b>Expected Result:</b> Both requests select exactly the same rows -- the requested MIME types are
     *     an unordered set as far as the generated predicate is concerned.</li>
     * </ul>
     * Invariant I5 of <a href="https://github.com/dotCMS/core/issues/36916">#36916</a>.
     */
    @Test
    public void test_getFolderContentList_mimeTypeOrder_doesNotChangeResults() throws Exception {
        final Set<String> pageFirst =
                browseIdentifiers(mimeFolder, List.of(DOTPAGE_MIME_TYPE, "image/jpeg"));
        final Set<String> imageFirst =
                browseIdentifiers(mimeFolder, List.of("image/jpeg", DOTPAGE_MIME_TYPE));

        assertEquals("The order of the requested MIME types must not change the result set", pageFirst,
                imageFirst);
    }

    /**
     * <ul>
     *     <li><b>Method to test:</b> {@link BrowserAPI#getFolderContentList(BrowserQuery)}</li>
     *     <li><b>Given Scenario:</b> A sub-folder holding a Page and a JPG File Asset is browsed, first by the
     *     synthetic Page MIME type and then by a real one.</li>
     *     <li><b>Expected Result:</b> Each browse returns only its own kind, and neither returns items from the
     *     parent folder. Folders themselves are listed separately by the Browser API and are out of scope here.</li>
     * </ul>
     * AC-004 of <a href="https://github.com/dotCMS/core/issues/36916">#36916</a>.
     */
    @Test
    public void test_getFolderContentList_subFolder_filtersEachTypeAsExpected() throws Exception {
        final Set<String> pages = browseIdentifiers(mimeSubFolder, List.of(DOTPAGE_MIME_TYPE));

        assertTrue("The sub-folder Page must be returned, but got: " + pages,
                pages.contains(mimeSubFolderPage.getIdentifier()));
        assertFalse("The sub-folder JPG File Asset must not match '" + DOTPAGE_MIME_TYPE + "'",
                pages.contains(mimeSubFolderJpgFile.getIdentifier()));
        assertFalse("A sub-folder browse must not return items from its parent folder",
                pages.contains(mimePage.getIdentifier()));

        final Set<String> images = browseIdentifiers(mimeSubFolder, List.of("image/jpeg"));

        assertTrue("The sub-folder JPG File Asset must be returned, but got: " + images,
                images.contains(mimeSubFolderJpgFile.getIdentifier()));
        assertFalse("Pages must never leak into a real MIME type browse",
                images.contains(mimeSubFolderPage.getIdentifier()));
    }

    /**
     * <ul>
     *     <li><b>Method to test:</b> {@link BrowserAPI#getFolderContentList(BrowserQuery)}</li>
     *     <li><b>Given Scenario:</b> Pages are browsed by the synthetic MIME type under a specific language, with
     *     and without the default language fallback the legacy dialog turns on.</li>
     *     <li><b>Expected Result:</b> With the fallback on, both the Page in the requested language and the one in
     *     the default language come back; with it off, only the Page in the requested language does. Language
     *     resolution is unchanged by the MIME routing.</li>
     * </ul>
     * AC-008 of <a href="https://github.com/dotCMS/core/issues/36916">#36916</a>.
     */
    @Test
    public void test_getFolderContentList_dotPageMimeType_honorsLanguageFallback() throws Exception {
        final Set<String> withFallback = browseIdentifiersByLanguage(testLanguage.getId(), true);

        assertTrue("The Page in the requested language must be returned, but got: " + withFallback,
                withFallback.contains(mimePageAltLanguage.getIdentifier()));
        assertTrue("The default language Page must be returned when the fallback is on, but got: " + withFallback,
                withFallback.contains(mimePage.getIdentifier()));

        final Set<String> withoutFallback = browseIdentifiersByLanguage(testLanguage.getId(), false);

        assertTrue("The Page in the requested language must be returned, but got: " + withoutFallback,
                withoutFallback.contains(mimePageAltLanguage.getIdentifier()));
        assertFalse("The default language Page must not be returned when the fallback is off",
                withoutFallback.contains(mimePage.getIdentifier()));
    }

    /**
     * Same browse as {@link #browseIdentifiers(Folder, List)} but scoped to a language, mirroring how the legacy
     * dialog resolves content -- see {@code BrowserAjax.getFolderContentWithDotAssets}.
     */
    private Set<String> browseIdentifiersByLanguage(final long languageId, final boolean showDefaultLangItems)
            throws DotSecurityException, DotDataException {
        return browserAPI.getFolderContentList(BrowserQuery.builder()
                        .withUser(APILocator.systemUser())
                        .withHostOrFolderId(mimeFolder.getIdentifier())
                        .showPages(true)
                        .showFiles(true)
                        .showFolders(false)
                        .showWorking(true)
                        .withLanguageId(languageId)
                        .showDefaultLangItems(showDefaultLangItems)
                        .showMimeTypes(List.of(DOTPAGE_MIME_TYPE))
                        .build()).stream()
                .map(Treeable::getIdentifier)
                .collect(Collectors.toSet());
    }

    /**
     * <ul>
     *     <li><b>Method to test:</b> {@link BrowserAPI#getFolderContent(BrowserQuery)}</li>
     *     <li><b>Given Scenario:</b> A folder tree holding File Assets -- including one whose name contains
     *     dots and one nested in a sub-folder -- and an HTML Page is browsed the way the file browser dialog
     *     does it.</li>
     *     <li><b>Expected Result:</b> Every row already exposes the asset's <b>full</b> path in {@code path},
     *     {@code url} and, for Pages, {@code pageURI}: the three agree with each other and each one ends with
     *     the asset name exactly once. Consumers must therefore use one of them as-is and never append
     *     {@code fileName} to them.</li>
     * </ul>
     * Regression guard for <a href="https://github.com/dotCMS/core/issues/37050">#37050</a>: the Vanity URL
     * "Forward To" picker used to build its value as {@code path + fileName}, which duplicated the file name.
     * It also pins the contract against the reverse change -- making {@code path} mean "parent path" in
     * {@code WebAssetStrategy.addPath} would silently break every consumer of this payload.
     */
    @Test
    public void test_getFolderContent_webAssetRows_carryTheFullPathExactlyOnce() throws Exception {
        final Host host = new SiteDataGen().nextPersisted();
        final Folder folder = new FolderDataGen().name("pathFolder").site(host).nextPersisted();
        final Folder subFolder = new FolderDataGen().name("pathSubFolder").parent(folder).nextPersisted();

        final FileAsset plainFile = persistFileAsset(folder, fileNamed("pathPlainFile.txt"));
        // Dots in the name are the interesting case: any extension-based trimming would mangle it
        final FileAsset dottedFile = persistFileAsset(subFolder, fileNamed("my.report.v2.txt"));
        final HTMLPageAsset page = new HTMLPageDataGen(folder,
                new TemplateDataGen().host(host).nextPersisted()).title("pathPage").pageURL("path-page")
                .nextPersisted();

        assertWebAssetRow(folder, plainFile.getIdentifier(), "/pathFolder/pathPlainFile.txt",
                plainFile.getFileName());
        assertWebAssetRow(subFolder, dottedFile.getIdentifier(), "/pathFolder/pathSubFolder/my.report.v2.txt",
                dottedFile.getFileName());
        // Pages resolve through the 'pageURI' branch of the picker, which must keep agreeing with the rest
        final Map<String, Object> pageRow = assertWebAssetRow(folder, page.getIdentifier(),
                "/pathFolder/path-page", null);
        assertEquals("A Page's 'pageURI' must agree with its 'url'", pageRow.get("url"),
                pageRow.get("pageURI"));
    }

    /**
     * Browses {@code folder} the way the file browser dialog does -- see
     * {@code BrowserAjax.getFolderContentWithDotAssets} -- and asserts that the hydrated row for
     * {@code identifier} exposes {@code expectedPath} in both {@code path} and {@code url}. When
     * {@code fileName} is given, it also asserts the path ends with it exactly once.
     */
    private Map<String, Object> assertWebAssetRow(final Folder folder, final String identifier,
            final String expectedPath, final String fileName) throws Exception {
        final Map<String, Object> results = browserAPI.getFolderContent(BrowserQuery.builder()
                .withUser(APILocator.systemUser())
                .withHostOrFolderId(folder.getIdentifier())
                .showPages(true)
                .showFiles(true)
                .showFolders(false)
                .showWorking(true)
                .build());
        final Map<String, Object> row =
                ((List<Map<String, Object>>) results.get("list")).stream()
                        .filter(item -> identifier.equals(item.get("identifier")))
                        .findFirst()
                        .orElseThrow(() -> new AssertionError(
                                "The browse of '" + folder.getName() + "' must return " + identifier));

        final String actualPath = (String) row.get("path");
        assertEquals("'path' must be the asset's full path", expectedPath, actualPath);
        assertEquals("'url' must agree with 'path'", expectedPath, row.get("url"));
        if (null != fileName) {
            assertTrue("'path' must end with the file name: " + actualPath,
                    actualPath.endsWith("/" + fileName));
            assertEquals("The file name must appear exactly once in 'path': " + actualPath, 1,
                    StringUtils.countMatches(actualPath, fileName));
        }
        return row;
    }

    /**
     * Creates a temporary file with an <b>exact</b> name. {@code File.createTempFile} appends random digits,
     * which would defeat the path assertions above.
     */
    private static File fileNamed(final String name) throws IOException {
        final File file = new File(Files.createTempDirectory("issue37050").toFile(), name);
        FileUtils.writeStringToFile(file, "this is a test!", StandardCharsets.UTF_8);
        return file;
    }

    // ------------------------------------------------------------------------------------------
    // issue #37185 -- long-text listing projection trim (blast-radius regression, US2).
    //
    // T030-T033 from specs/37185-content-drive-listing-longtext-projection/tasks.md. Pins the
    // generic-Content row shape from both getPaginatedContents (Content Drive) and
    // getFolderContent (Site Browser), which share dotContentMap.

    private static final String LTP_WYSIWYG_VAR = "ltpWysiwyg";
    private static final String LTP_TEXTAREA_VAR = "ltpTextArea";
    private static final String LTP_STORY_VAR = "ltpStory";

    /**
     * AC-002: every field the Content Drive grid/toolbar/action menu depend on, for a
     * generic-Content row. {@code mimeType}/{@code extension} are File Asset-specific and
     * legitimately absent here (found running this test against a generic content type).
     */
    private static final List<String> REQUIRED_LISTING_KEYS = List.of(
            "identifier", "inode", "title", "contentType", "baseType", "languageId", "live",
            "working", "archived", "hasLiveVersion", "modUser", "modUserName", "modDate",
            "permissions", "icon", "hasTitleImage", "owner");

    private static String storyBlockJson(final String text) {
        return "{\"type\":\"doc\",\"content\":[{\"type\":\"paragraph\",\"content\":"
                + "[{\"type\":\"text\",\"text\":\"" + text + "\"}]}]}";
    }

    private static ContentType createLongTextContentType(final String uniqueId) {
        final ContentType contentType = new ContentTypeDataGen()
                .name("ltpType_" + uniqueId)
                .velocityVarName("ltpType_" + uniqueId)
                .nextPersisted();
        new FieldDataGen().type(WysiwygField.class).name(LTP_WYSIWYG_VAR)
                .velocityVarName(LTP_WYSIWYG_VAR).contentTypeId(contentType.id())
                .searchable(true).indexed(true).nextPersisted();
        new FieldDataGen().type(TextAreaField.class).name(LTP_TEXTAREA_VAR)
                .velocityVarName(LTP_TEXTAREA_VAR).contentTypeId(contentType.id())
                .searchable(true).indexed(true).nextPersisted();
        new FieldDataGen().type(StoryBlockField.class).name(LTP_STORY_VAR)
                .velocityVarName(LTP_STORY_VAR).contentTypeId(contentType.id())
                .searchable(true).indexed(true).nextPersisted();
        return contentType;
    }

    private static void assertRequiredKeysPresent(final Map<String, Object> row) {
        for (final String key : REQUIRED_LISTING_KEYS) {
            assertTrue("Row must carry required key '" + key + "': " + row.keySet(),
                    row.containsKey(key));
        }
    }

    private static void assertLongTextValuesArePreviews(final Map<String, Object> row,
            final String rawHtmlBody) {
        for (final String var : List.of(LTP_WYSIWYG_VAR, LTP_TEXTAREA_VAR, LTP_STORY_VAR)) {
            final Object value = row.get(var);
            assertTrue("'" + var + "' must be a String preview", value instanceof String);
            final String preview = (String) value;
            assertTrue("'" + var + "' preview must be <=150 chars", preview.length() <= 150);
            assertFalse("'" + var + "' preview must not contain HTML markers",
                    preview.contains("<") || preview.contains(">"));
            assertFalse("'" + var + "' preview must not contain JSON structure",
                    preview.contains("{") || preview.contains("}"));
            assertTrue("'" + var + "' preview must be shorter than the raw stored value",
                    preview.length() < rawHtmlBody.length());
        }
    }

    /**
     * <ul>
     *     <li><b>Method to Test:</b> {@link BrowserAPIImpl#getPaginatedContents(BrowserQuery)}</li>
     *     <li><b>Given Scenario:</b> A generic-Content row with WYSIWYG/TextArea/Story Block field
     *     values, listed via the Content Drive path (T030, AC-001/AC-002).</li>
     *     <li><b>Expected Result:</b> Every AC-002 key is present AND every long-text field value
     *     is a &lt;=150-character plain-text preview, free of HTML/JSON structure.</li>
     * </ul>
     */
    @Test
    public void test_getPaginatedContents_longTextFields_arePreviewedAndRequiredKeysPresent()
            throws Exception {
        final String uniqueId = UUIDGenerator.shorty();
        final Host site = new SiteDataGen().nextPersisted();
        final Folder folder = new FolderDataGen().site(site).nextPersisted();
        final ContentType contentType = createLongTextContentType(uniqueId);

        final String rawHtmlBody = "<p>" + "word ".repeat(60) + "</p>";
        final Contentlet contentlet = new ContentletDataGen(contentType.id())
                .folder(folder)
                .setProperty("title", "ltpDoc_" + uniqueId)
                .setProperty(LTP_WYSIWYG_VAR, rawHtmlBody)
                .setProperty(LTP_TEXTAREA_VAR, rawHtmlBody)
                .setProperty(LTP_STORY_VAR, storyBlockJson("word ".repeat(60)))
                .languageId(1)
                .setPolicy(IndexPolicy.WAIT_FOR)
                .nextPersisted();

        final PaginatedContents result = browserAPI.getPaginatedContents(BrowserQuery.builder()
                .withUser(APILocator.systemUser())
                .withHostOrFolderId(folder.getIdentifier())
                .build());

        final Map<String, Object> row = result.list.stream()
                .filter(item -> contentlet.getIdentifier().equals(item.get("identifier")))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Must find the created contentlet in the listing"));

        assertRequiredKeysPresent(row);
        assertLongTextValuesArePreviews(row, rawHtmlBody);
    }

    /**
     * <ul>
     *     <li><b>Method to Test:</b> {@link BrowserAPIImpl#getFolderContent(BrowserQuery)}</li>
     *     <li><b>Given Scenario:</b> The same content type/data as above, listed via the Site
     *     Browser path (T031, AC-004).</li>
     *     <li><b>Expected Result:</b> Same required keys present, same reduced long-text values --
     *     Site Browser gets identical treatment to Content Drive since both share
     *     {@code dotContentMap}.</li>
     * </ul>
     */
    @Test
    public void test_getFolderContent_longTextFields_arePreviewedAndRequiredKeysPresent()
            throws Exception {
        final String uniqueId = UUIDGenerator.shorty();
        final Host site = new SiteDataGen().nextPersisted();
        final Folder folder = new FolderDataGen().site(site).nextPersisted();
        final ContentType contentType = createLongTextContentType(uniqueId);

        final String rawHtmlBody = "<p>" + "word ".repeat(60) + "</p>";
        final Contentlet contentlet = new ContentletDataGen(contentType.id())
                .folder(folder)
                .setProperty("title", "ltpSiteBrowserDoc_" + uniqueId)
                .setProperty(LTP_WYSIWYG_VAR, rawHtmlBody)
                .setProperty(LTP_TEXTAREA_VAR, rawHtmlBody)
                .setProperty(LTP_STORY_VAR, storyBlockJson("word ".repeat(60)))
                .languageId(1)
                .setPolicy(IndexPolicy.WAIT_FOR)
                .nextPersisted();

        @SuppressWarnings("unchecked")
        final Map<String, Object> results = browserAPI.getFolderContent(BrowserQuery.builder()
                .withUser(APILocator.systemUser())
                .withHostOrFolderId(folder.getIdentifier())
                .build());
        @SuppressWarnings("unchecked")
        final List<Map<String, Object>> list = (List<Map<String, Object>>) results.get("list");

        final Map<String, Object> row = list.stream()
                .filter(item -> contentlet.getIdentifier().equals(item.get("identifier")))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Must find the created contentlet in the listing"));

        assertRequiredKeysPresent(row);
        assertLongTextValuesArePreviews(row, rawHtmlBody);
    }

    /**
     * <ul>
     *     <li><b>Given Scenario:</b> A content type with a {@code listed} (Show In List) WYSIWYG
     *     field (T032, AC-003).</li>
     *     <li><b>Expected Result:</b> The grid column's cell value is present, a &lt;=150-character
     *     plain-text preview -- not the full body, not blank, not mid-tag garbage.</li>
     * </ul>
     */
    @Test
    public void test_getPaginatedContents_listedWysiwygField_rendersReadablePreview() throws Exception {
        final String uniqueId = UUIDGenerator.shorty();
        final Host site = new SiteDataGen().nextPersisted();
        final Folder folder = new FolderDataGen().site(site).nextPersisted();

        final ContentType contentType = new ContentTypeDataGen()
                .name("ltpListedType_" + uniqueId)
                .velocityVarName("ltpListedType_" + uniqueId)
                .nextPersisted();
        new FieldDataGen().type(WysiwygField.class).name(LTP_WYSIWYG_VAR)
                .velocityVarName(LTP_WYSIWYG_VAR).contentTypeId(contentType.id())
                .searchable(true).indexed(true).listed(true).nextPersisted();

        final String rawHtmlBody = "<div><p>" + "article body text ".repeat(30) + "</p></div>";
        final Contentlet contentlet = new ContentletDataGen(contentType.id())
                .folder(folder)
                .setProperty("title", "ltpListedDoc_" + uniqueId)
                .setProperty(LTP_WYSIWYG_VAR, rawHtmlBody)
                .languageId(1)
                .setPolicy(IndexPolicy.WAIT_FOR)
                .nextPersisted();

        final PaginatedContents result = browserAPI.getPaginatedContents(BrowserQuery.builder()
                .withUser(APILocator.systemUser())
                .withHostOrFolderId(folder.getIdentifier())
                .build());

        final Map<String, Object> row = result.list.stream()
                .filter(item -> contentlet.getIdentifier().equals(item.get("identifier")))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Must find the created contentlet in the listing"));

        final Object value = row.get(LTP_WYSIWYG_VAR);
        assertTrue("Listed WYSIWYG column must be present", row.containsKey(LTP_WYSIWYG_VAR));
        assertTrue(value instanceof String);
        final String preview = (String) value;
        assertFalse("Must not be blank", preview.isEmpty());
        assertTrue("Must be <=150 chars", preview.length() <= 150);
        assertFalse("Must not contain HTML tags", preview.contains("<") || preview.contains(">"));
    }

    /**
     * <ul>
     *     <li><b>Given Scenario:</b> A content type whose title-source field is itself a WYSIWYG
     *     field (its variable is literally {@code "title"}) (T033, AC-008).</li>
     *     <li><b>Expected Result:</b> The listing's {@code title} key is the correct, untruncated
     *     title -- not derived from the same map entry the long-text preview strategy truncates.</li>
     * </ul>
     */
    @Test
    public void test_getPaginatedContents_wysiwygTitleField_titleKeyStaysUntruncated() throws Exception {
        final String uniqueId = UUIDGenerator.shorty();
        final Host site = new SiteDataGen().nextPersisted();
        final Folder folder = new FolderDataGen().site(site).nextPersisted();

        final ContentType contentType = new ContentTypeDataGen()
                .name("ltpTitleType_" + uniqueId)
                .velocityVarName("ltpTitleType_" + uniqueId)
                .nextPersisted();
        // The title-source field: WYSIWYG, variable name "title" -- Contentlet#getTitle() nominates
        // the first field whose variable starts with "title" when no separate title is set.
        new FieldDataGen().type(WysiwygField.class).name("Title")
                .velocityVarName("title").contentTypeId(contentType.id())
                .searchable(true).indexed(true).nextPersisted();

        // Kept under 255 chars (raw HTML) -- the contentlet.title column is varchar(255) -- while
        // its stripped plain text (~220 chars) still comfortably exceeds the 150-char preview
        // bound, so an accidental truncation of this key would be caught.
        final String longTitleHtml = "<p>" + "TitleWord ".repeat(22) + "</p>";
        final Contentlet contentlet = new ContentletDataGen(contentType.id())
                .folder(folder)
                .setProperty("title", longTitleHtml)
                .languageId(1)
                .setPolicy(IndexPolicy.WAIT_FOR)
                .nextPersisted();

        final PaginatedContents result = browserAPI.getPaginatedContents(BrowserQuery.builder()
                .withUser(APILocator.systemUser())
                .withHostOrFolderId(folder.getIdentifier())
                .build());

        final Map<String, Object> row = result.list.stream()
                .filter(item -> contentlet.getIdentifier().equals(item.get("identifier")))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Must find the created contentlet in the listing"));

        assertEquals("The title key must equal Contentlet#getTitle(), untruncated",
                contentlet.getTitle(), row.get("title"));
    }
}
