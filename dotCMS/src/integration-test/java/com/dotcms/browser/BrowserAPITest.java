package com.dotcms.browser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.FileUtil;
import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import com.dotcms.IntegrationTestBase;
import com.dotcms.contenttype.exception.NotFoundInDbException;
import com.dotcms.datagen.FileAssetDataGen;
import com.dotcms.datagen.FolderDataGen;
import com.dotcms.datagen.HTMLPageDataGen;
import com.dotcms.datagen.LanguageDataGen;
import com.dotcms.datagen.LinkDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.TestDataUtils;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.image.focalpoint.FocalPointAPITest;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.IndexPolicy;
import com.dotmarketing.portlets.fileassets.business.FileAsset;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.links.model.Link;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.UUIDGenerator;
import com.google.common.collect.ImmutableSet;
import com.liferay.portal.model.User;
import io.vavr.Tuple;
import io.vavr.Tuple3;

/**
 * Created by Oscar Arrieta on 6/8/17.
 */

public class BrowserAPITest extends IntegrationTestBase {

    final BrowserAPI browserAPI = APILocator.getBrowserAPI();
    final FolderAPI folderAPI = APILocator.getFolderAPI();
    final UserAPI userAPI = APILocator.getUserAPI();
    final HostAPI hostAPI = APILocator.getHostAPI();

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
     * Method to test: testing the pagination of the BrowserAPI, the test creates a site and a folder, them add 100 files and iterate over them with the browser api
     * Given Scenario: 1)  request items from 0 to 10
     *                  2) request items form 10 to 60
     *                  3) request items form 60 to 100
     *                  4) out of range
     * ExpectedResult: Must have always 100 files as a total, and should retrieve the respective items per request
     *
     */

    @Test()
    public void test_GetFolderContent_pagination() throws DotDataException, DotSecurityException, IOException {

        // create a folder
        // create a 100 files
        // paginate 10 in 100
        final SiteDataGen   siteDataGen   = new SiteDataGen();
        final FolderDataGen folderDataGen = new FolderDataGen();
        final Host          host          = siteDataGen.nextPersisted();
        final Folder        folder        = folderDataGen.site(host).nextPersisted();

        for (int i = 0; i < 100; ++i) {

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
                .maxResults(10)
                .build());

        assertNotNull(resultMap);
        assertEquals(100l, resultMap.get("total"));

        List<Map<String, Object>> results = (List<Map<String, Object>>)resultMap.get("list");
        assertNotNull(results);
        assertEquals(results.size(), 10);

        // 10 - 60
        resultMap = browserAPI.getFolderContent(BrowserQuery.builder()
                .showDotAssets(true)
                .showLinks(true)
                .withHostOrFolderId(folder.getIdentifier())
                .offset(10)
                .showFiles(true)
                .showFolders(true)
                .showWorking(true)
                .maxResults(50)
                .build());

        assertNotNull(resultMap);
        assertEquals(100l, resultMap.get("total"));

        results = (List<Map<String, Object>>)resultMap.get("list");
        assertNotNull(results);
        assertEquals(results.size(), 50);

        // 60 - 100
        resultMap = browserAPI.getFolderContent(BrowserQuery.builder()
                .showDotAssets(true)
                .showLinks(true)
                .withHostOrFolderId(folder.getIdentifier())
                .offset(60)
                .showFiles(true)
                .showFolders(true)
                .showWorking(true)
                .maxResults(50)
                .build());

        assertNotNull(resultMap);
        assertEquals(100l, resultMap.get("total"));

        results = (List<Map<String, Object>>)resultMap.get("list");
        assertNotNull(results);
        assertEquals(results.size(), 40);

        // 100 - ...
        resultMap = browserAPI.getFolderContent(BrowserQuery.builder()
                .showDotAssets(true)
                .showLinks(true)
                .withHostOrFolderId(folder.getIdentifier())
                .offset(100)
                .showFiles(true)
                .showFolders(true)
                .showWorking(true)
                .maxResults(150)
                .build());

        assertNotNull(resultMap);
        assertEquals(100l, resultMap.get("total"));

        results = (List<Map<String, Object>>)resultMap.get("list");
        assertNotNull(results);
        assertEquals(results.size(), 0);
    }

    /**
     * Method to test: testing the pagination of the BrowserAPI, the test creates a site and a folder, them add 100 files and iterate over them with the browser api
     * also it is including a mime type
     * Given Scenario: 1)  request items from 0 to 10
     *                  2) request items form 10 to 60
     *                  3) request items form 60 to 100
     *                  4) out of range
     * ExpectedResult: Must have always 100 files as a total, and should retrieve the respective items per request
     *
     */

    @Test()
    public void test_GetFolderContent_mimetype_pagination() throws DotDataException, DotSecurityException, IOException {

        // create a folder
        // create a 100 files
        // paginate 10 in 100
        final SiteDataGen   siteDataGen   = new SiteDataGen();
        final FolderDataGen folderDataGen = new FolderDataGen();
        final Host          host          = siteDataGen.nextPersisted();
        final Folder        folder        = folderDataGen.site(host).nextPersisted();

        for (int i = 0; i < 100; ++i) {

            new FileAssetDataGen(FileUtil.createTemporaryFile("test", ".txt", "this is a test")).host(host)
                    .folder(folder).setPolicy(IndexPolicy.WAIT_FOR).nextPersisted();
        }

        Map<String, Object> resultMap = browserAPI.getFolderContent(BrowserQuery.builder()
                .showDotAssets(true)
                .showLinks(true)
                .showMimeTypes(Arrays.asList("application"))
                .withHostOrFolderId(folder.getIdentifier())
                .offset(0)
                .showFiles(true)
                .showFolders(true)
                .showWorking(true)
                .maxResults(10)
                .build());

        assertNotNull(resultMap);
        assertEquals(100l, resultMap.get("total"));

        List<Map<String, Object>> results = (List<Map<String, Object>>)resultMap.get("list");
        assertNotNull(results);
        assertEquals(results.size(), 10);

        // 10 - 60
        resultMap = browserAPI.getFolderContent(BrowserQuery.builder()
                .showDotAssets(true)
                .showLinks(true)
                .showMimeTypes(Arrays.asList("application"))
                .withHostOrFolderId(folder.getIdentifier())
                .offset(10)
                .showFiles(true)
                .showFolders(true)
                .showWorking(true)
                .maxResults(50)
                .build());

        assertNotNull(resultMap);
        assertEquals(100l, resultMap.get("total"));

        results = (List<Map<String, Object>>)resultMap.get("list");
        assertNotNull(results);
        assertEquals(results.size(), 50);

        // 60 - 100
        resultMap = browserAPI.getFolderContent(BrowserQuery.builder()
                .showDotAssets(true)
                .showLinks(true)
                .showMimeTypes(Arrays.asList("application"))
                .withHostOrFolderId(folder.getIdentifier())
                .offset(60)
                .showFiles(true)
                .showFolders(true)
                .showWorking(true)
                .maxResults(50)
                .build());

        assertNotNull(resultMap);
        assertEquals(100l, resultMap.get("total"));

        results = (List<Map<String, Object>>)resultMap.get("list");
        assertNotNull(results);
        assertEquals(results.size(), 40);

        // 100 - ...
        resultMap = browserAPI.getFolderContent(BrowserQuery.builder()
                .showDotAssets(true)
                .showLinks(true)
                .showMimeTypes(Arrays.asList("application"))
                .withHostOrFolderId(folder.getIdentifier())
                .offset(100)
                .showFiles(true)
                .showFolders(true)
                .showWorking(true)
                .maxResults(150)
                .build());

        assertNotNull(resultMap);
        assertEquals(100l, resultMap.get("total"));

        results = (List<Map<String, Object>>)resultMap.get("list");
        assertNotNull(results);
        assertEquals(results.size(), 0);
    }


    
    @Test(expected = NotFoundInDbException.class)
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
    
    
    
    
    @Test
    public void testingDifferentBrowserAPIResults() throws Exception{
        for(Tuple3<String,BrowserQuery, Set<String>> testCase : browserApiTestCases()) {
            String testTitle = testCase._1;
            Map<String,Object> results = browserAPI.getFolderContent(testCase._2);
            
            List<String> list = ((List<Map<String,Object>>) results.get("list")).stream().map(m->(String) m.get("name")).collect(Collectors.toList());
            
            assertTrue(testTitle, !list.isEmpty());
            
            Set<String> expectedNames = testCase._3;
            
            assertTrue(testTitle, list.size()==expectedNames.size());
            
            for(String name : list) {
                System.out.println(testTitle + " - got :" + name);
                assertTrue(testTitle, expectedNames.contains(name));
            }
            System.out.println("");
        }   
    }
    
    

    public static List<Tuple3<String,BrowserQuery, Set<String>>> browserApiTestCases() {
    
        List<Tuple3<String,BrowserQuery, Set<String>>> testCases = new ArrayList<>();
        

        
        // All in a folder in the default language
        testCases.add( Tuple.of(
                        "all content, 1 langauge, no archived",
                        
                            BrowserQuery.builder()
                            .showDotAssets(true)
                            .showLinks(true)
                            .inHostOrFolder(testFolder)
                            .showFolders(true)
                            .showPages(true)
                            .showFiles(true)
                            .withLanguageId(APILocator.getLanguageAPI().getDefaultLanguage().getId())
                            .build()
                                        ,
                            ImmutableSet.of(
                                testFileAsset.getName(),
                                testFileAsset2.getName(),
                                testSubFolder.getName(),
                                testlink.getName(),
                                testDotAsset.getTitle(),
                                testPage.getPageUrl()
                            ))

        );
        
        
        testCases.add( Tuple.of(
                        "only files, 1 langauge, no archived",
                        
                        BrowserQuery.builder()
                        .showDotAssets(true)
                        .showFiles(true)
                        .inHostOrFolder(testFolder)
                        .withLanguageId(APILocator.getLanguageAPI().getDefaultLanguage().getId())
                        .build(),
                        
                        ImmutableSet.of(
                            testFileAsset.getName(),
                            testFileAsset2.getName(),
                            testDotAsset.getTitle()
                        ))
        
        );
        
        testCases.add(Tuple.of(
                        "only files, all langauges, no archived",
                        
                        BrowserQuery.builder()
                        .showDotAssets(true)
                        .showFiles(true)
                        .inHostOrFolder(testFolder)
                        .build(),
                        
                        ImmutableSet.of(
                            testFileAsset.getName(),
                            testFileAsset2.getName(),
                            testFileAsset2MultiLingual.getName(),
                            testDotAsset.getTitle()
                        ))
        
        );

        
        testCases.add(Tuple.of(
                        "show archived files, all langauges, no dotAssets",
                        
                        BrowserQuery.builder()
                        .showFiles(true)
                        .showArchived(true)
                        .inHostOrFolder(testFolder)
                        .build(),
                        
                        ImmutableSet.of(
                            testFileAsset.getName(),
                            testFileAsset2.getName(),
                            testFileAsset3Archived.getName(),
                            testFileAsset2MultiLingual.getName()
                        ))
        
        );
        
        
        
        testCases.add(Tuple.of(
                        "show pages",
                        
                        BrowserQuery.builder()
                        .showPages(true)
                        .inHostOrFolder(testFolder)
                        .build(),
                        
                        ImmutableSet.of(
                            testPage.getPageUrl()
            
                        ))
        
        );
        
        
        testCases.add(Tuple.of(
                        "show links",
                        
                        BrowserQuery.builder()
                        .showLinks(true)
                        .inHostOrFolder(testFolder)
                        .build(),
                        
                        ImmutableSet.of(
                            testlink.getName()
            
                        ))
        
        );
        
        
        
        return testCases;
        
    }
    
    

    
    
    
    
    
    
    
    
}
