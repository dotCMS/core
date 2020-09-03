package com.dotcms.rendering.velocity.viewtools.navigation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import com.dotcms.IntegrationTestBase;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.FileAssetDataGen;
import com.dotcms.datagen.FolderDataGen;
import com.dotcms.datagen.HTMLPageDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.TemplateDataGen;
import com.dotcms.datagen.TestDataUtils;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.IndexPolicy;
import com.dotmarketing.portlets.fileassets.business.FileAsset;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;
import com.dotmarketing.portlets.fileassets.business.IFileAsset;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.htmlpageasset.model.IHTMLPage;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import com.liferay.util.FileUtil;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.velocity.tools.view.context.ViewContext;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.mockito.Mockito;

/**
 * NavToolTest
 * Created by Oscar Arrieta on 5/4/15.
 */
@RunWith(DataProviderRunner.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class NavToolTest extends IntegrationTestBase{

    private static boolean ORIGINAL_DEFAULT_PAGE_TO_DEFAULT_LANGUAGE;
    private static Folder folder;
    private static User user;
    private static Host site;
    private static Language spanishLanguage;

    @BeforeClass
    public static void prepare() throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
        ORIGINAL_DEFAULT_PAGE_TO_DEFAULT_LANGUAGE = APILocator.getLanguageAPI().canDefaultPageToDefaultLanguage();
        user = APILocator.getUserAPI().getSystemUser();
        site = new SiteDataGen().nextPersisted();
        spanishLanguage = TestDataUtils.getSpanishLanguage();
    }

    @AfterClass
    public static void restoreProperty() throws DotSecurityException, DotDataException {
        Config.setProperty("DEFAULT_PAGE_TO_DEFAULT_LANGUAGE", ORIGINAL_DEFAULT_PAGE_TO_DEFAULT_LANGUAGE);
        assertEquals(APILocator.getLanguageAPI().canDefaultPageToDefaultLanguage(), ORIGINAL_DEFAULT_PAGE_TO_DEFAULT_LANGUAGE);
        if(folder != null){
            APILocator.getFolderAPI().delete(folder,user,true);
        }
    }

    @Test
    public void testFolderDefaultPageToLanguageTrue() throws Exception { // https://github.com/dotCMS/core/issues/7678
        Config.setProperty("DEFAULT_PAGE_TO_DEFAULT_LANGUAGE", true);
        assertTrue(APILocator.getLanguageAPI().canDefaultPageToDefaultLanguage());

        createData();

        //Using Identifier to get the path.
        Identifier folderIdentifier = APILocator.getIdentifierAPI().find(folder);
        NavResult navResult = new NavTool().getNav(site, folderIdentifier.getPath(), 1, user);
        assertNotNull(navResult);

        //Find out how many show on menu items we currently have
        final int currentShowOnMenuItems = findShowOnMenuUnderFolder(folder, user);

        //Comparing what we found vs the result on the NavTool
        //Expected: 1 SubFolder and 1 Page (there is another page with Show on Menu but is in Spanish)
        int englishResultChildren = navResult.getChildren().size();
        assertEquals(currentShowOnMenuItems-1,englishResultChildren);

        navResult = new NavTool()
                .getNav(site, folderIdentifier.getPath(), spanishLanguage.getId(), user);
        assertNotNull(navResult);

        //Expected: 1 SubFolder and 2 Pages (DEFAULT_PAGE_TO_DEFAULT_LANGUAGE=true) should make the english page to return also
        int spanishResultChildren = navResult.getChildren().size();
        assertEquals(currentShowOnMenuItems,spanishResultChildren);
    }

    @Test
    public void testFolderDefaultPageToLanguageFalse() throws Exception { // https://github.com/dotCMS/core/issues/7678
        Config.setProperty("DEFAULT_PAGE_TO_DEFAULT_LANGUAGE", false);
        assertFalse(APILocator.getLanguageAPI().canDefaultPageToDefaultLanguage());

        createData();

        //Using Identifier to get the path.
        Identifier folderIdentifier = APILocator.getIdentifierAPI().find(folder);
        NavResult navResult = new NavTool().getNav(site, folderIdentifier.getPath(), 1, user);
        assertNotNull(navResult);

        //Find out how many show on menu items we currently have
        final int currentShowOnMenuItems = findShowOnMenuUnderFolder(folder, user);

        //Comparing what we found vs the result on the NavTool
        //Expected: 1 SubFolder and 1 Page (there is another page with Show on Menu but is in Spanish)
        int englishResultChildren = navResult.getChildren().size();
        assertEquals(currentShowOnMenuItems-1,englishResultChildren);

        navResult = new NavTool()
                .getNav(site, folderIdentifier.getPath(), spanishLanguage.getId(), user);
        assertNotNull(navResult);

        //Expected: 1 SubFolder and 1 Page (DEFAULT_PAGE_TO_DEFAULT_LANGUAGE=false) should NOT include english page
        int spanishResultChildren = navResult.getChildren().size();
        assertEquals(currentShowOnMenuItems-1,spanishResultChildren);

    }

    private void createData() throws Exception {

        //Create Folder
        folder = new FolderDataGen().site(site).nextPersisted();

        //New template
        final Template template = new TemplateDataGen().nextPersisted();

        //Create 2 Pages (One with show on Menu and one without) in English
        final HTMLPageAsset pageAsset1 = new HTMLPageDataGen(folder, template).showOnMenu(true).languageId(1).nextPersisted();
        final HTMLPageAsset pageAsset2 = new HTMLPageDataGen(folder, template).showOnMenu(false).languageId(1).nextPersisted();
        pageAsset1.setIndexPolicy(IndexPolicy.FORCE);
        pageAsset2.setIndexPolicy(IndexPolicy.FORCE);
        APILocator.getContentletAPI().publish(pageAsset1, user, true);
        APILocator.getContentletAPI().publish(pageAsset2, user, true);

        //Create 2 Pages (One with show on Menu and one without) in Spanish
        final HTMLPageAsset pageAsset3 = new HTMLPageDataGen(folder, template).showOnMenu(true)
                .languageId(spanishLanguage.getId()).nextPersisted();
        final HTMLPageAsset pageAsset4 = new HTMLPageDataGen(folder, template).showOnMenu(false)
                .languageId(spanishLanguage.getId()).nextPersisted();
        pageAsset3.setIndexPolicy(IndexPolicy.FORCE);
        pageAsset4.setIndexPolicy(IndexPolicy.FORCE);
        APILocator.getContentletAPI().publish(pageAsset3, user, true);
        APILocator.getContentletAPI().publish(pageAsset4, user, true);

        //Create 2 Folders (One with show on Menu and one without)
        final Folder subFolder1 = new FolderDataGen().parent(folder).showOnMenu(true).nextPersisted();
        final Folder subFolder2 = new FolderDataGen().parent(folder).showOnMenu(false).nextPersisted();
    }

    @Test
    public void testRootLevelNavigation_WhenOneFileAssetIsShownOnMenu() throws Exception {

        Contentlet fileAssetShown = null;
        Contentlet fileAssetNotShown = null;

        try {

            //Get SystemFolder
            final Folder systemFolder = APILocator.getFolderAPI().findSystemFolder();

            //Create a FileAsset In English With ShowOnMenu = true
            final File file = File.createTempFile("fileTestEngTrue", ".txt");
            FileUtil.write(file, "helloworld");
            fileAssetShown = new FileAssetDataGen(systemFolder, file)
                    .host(site).setProperty(FileAssetAPI.SHOW_ON_MENU, "true").nextPersisted();
            ContentletDataGen.publish(fileAssetShown);

            //Create a FileAsset In English With ShowOnMenu = false
            final File file2 = File.createTempFile("fileTestEngFalse", ".txt");
            FileUtil.write(file2, "helloworld");
            fileAssetNotShown = new FileAssetDataGen(systemFolder, file2)
                    .host(site).setProperty(FileAssetAPI.SHOW_ON_MENU, "false").nextPersisted();
            ContentletDataGen.publish(fileAssetNotShown);

            //Get the Nav at the Root Level
            final NavResult navResult = new NavTool()
                    .getNav(site, systemFolder.getPath(), 1, user);
            assertNotNull(navResult);

            /* method below `findShowOnMenuUnderFolder` expects a mutated version of SYSTEM_FOLDER with the hostId altered.
               So let's create a defensive copy of the SYSTEM_FOLDER, modify it and pass it to the method.
            */
            Folder modifiedSystemFolder = new Folder();
            BeanUtils.copyProperties(modifiedSystemFolder, systemFolder);
            modifiedSystemFolder.setHostId(site.getIdentifier());

            //Find out how many show on menu items we currently have
            final int currentShowOnMenuItems = findShowOnMenuUnderFolder(modifiedSystemFolder, user);

            assertNotNull(navResult.getChildren());
            assertEquals(currentShowOnMenuItems,navResult.getChildren().size());
        }finally {
            //Now remove all the pages that we created for this tests.
            if(fileAssetShown!=null) {
                APILocator.getContentletAPI().destroy(fileAssetShown, user, false );
            }
            if(fileAssetNotShown!=null) {
                APILocator.getContentletAPI().destroy(fileAssetNotShown, user, false );
            }
        }

    }

    @Test
    public void test_getNavLevelAsParameter() throws Exception {
        testgetNav(1);
    }

    @Test
    public void test_getNavWithoutParameters() throws Exception {
        testgetNav(null);
    }

    private void testgetNav(Integer level) throws Exception {
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final ViewContext viewContext = mock(ViewContext.class);

        createData();

        Mockito.when(request.getRequestURI()).thenReturn("/" + folder.getName());
        Mockito.when(request.getServerName()).thenReturn(site.getHostname());
        Mockito.when(viewContext.getRequest()).thenReturn(request);


        final NavTool navTool = new NavTool();
        navTool.init(viewContext);
        NavResult navResult = null;
        if (UtilMethods.isSet(level)) {
            navResult = navTool.getNav(level);
        } else {
            navResult = navTool.getNav();
        }
        assertNotNull(navResult);

        //Find out how many show on menu items we currently have
        final int currentShowOnMenuItems = findShowOnMenuUnderFolder(folder, user);

        //Comparing what we found vs the result on the NavTool
        final int resultChildren = navResult.getChildren().size();
        assertEquals(currentShowOnMenuItems-1, resultChildren);//1 SubFolder and 1 Page (there is another page with Show on Menu but is in Spanish)
    }

    @DataProvider
    public static Object[] dataProviderShouldAddFileInAnotherLang() throws Exception {

        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
        final Language spanishLanguage = TestDataUtils.getSpanishLanguage();

        final FileAsset fileAssetInSpanish = new FileAsset();
        fileAssetInSpanish.setIdentifier("mutiLangFileAsset");
        fileAssetInSpanish.setLanguageId(spanishLanguage.getId());

        final NavToolTestCase case1 = new NavToolTestCase();
        case1.menuItems = Collections.singletonList(fileAssetInSpanish);
        case1.itemFile = fileAssetInSpanish;
        case1.selectedLang = 1L;
        case1.expectedResult = false;

        final NavToolTestCase case2 = new NavToolTestCase();
        case2.menuItems = Collections.singletonList(fileAssetInSpanish);
        case2.itemFile = fileAssetInSpanish;
        case2.selectedLang = spanishLanguage.getId();
        case2.expectedResult = false;

        final FileAsset fileAssetInEnglish = new FileAsset();
        fileAssetInEnglish.setIdentifier("mutiLangFileAsset");
        fileAssetInEnglish.setLanguageId(1);

        final NavToolTestCase case3 = new NavToolTestCase();
        case3.menuItems = Collections.singletonList(fileAssetInEnglish);
        case3.itemFile = fileAssetInSpanish;
        case3.selectedLang = 1L;
        case3.expectedResult = false;

        final NavToolTestCase case4 = new NavToolTestCase();
        case4.menuItems = Collections.singletonList(fileAssetInSpanish);
        case4.itemFile = fileAssetInEnglish;
        case4.selectedLang = spanishLanguage.getId();
        case4.expectedResult = false;

        final NavToolTestCase case5 = new NavToolTestCase();
        case5.menuItems = new ArrayList<>();
        case5.itemFile = fileAssetInEnglish;
        case5.selectedLang = spanishLanguage.getId();
        case5.expectedResult = true;

        return new Object[] {
            case1,
            case2,
            case3,
            case4,
            case5
        };
    }

    private static class NavToolTestCase {
        List<IFileAsset> menuItems;
        FileAsset itemFile;
        Long selectedLang;
        Boolean expectedResult;
    }

    @Test
    @UseDataProvider("dataProviderShouldAddFileInAnotherLang")
    public void testShouldAddFileInAnotherLang(final NavToolTestCase testCase) {

        final NavTool navTool = new NavTool();
        assertEquals(testCase.expectedResult, navTool.shouldAddFileInAnotherLang(testCase.menuItems, testCase.itemFile,
            testCase.selectedLang));

    }

    /**
     * Create a copy of the spanish pages
     *
     * @param user                      The user
     * @param liveHTMLPages         The live HTML pages
     * @param contentletsCreated    The contentlets created
     * @throws Exception            Any Exception
     */
    private void createSpanishPagesCopy(User user, List<IHTMLPage> liveHTMLPages, List<Contentlet> contentletsCreated) throws Exception {
        //We need to create a new copy of pages for Spanish.
        for(IHTMLPage liveHTMLPage : liveHTMLPages){
            Contentlet htmlPageContentlet = APILocator.getContentletAPI().find( liveHTMLPage.getInode(), user, false );

            //As a copy we need to remove this info to do a clean checkin.
            htmlPageContentlet.getMap().remove("modDate");
            htmlPageContentlet.getMap().remove("lastReview");
            htmlPageContentlet.getMap().remove("owner");
            htmlPageContentlet.getMap().remove("modUser");

            htmlPageContentlet.getMap().put("inode", "");
            htmlPageContentlet.getMap().put("languageId", 2L);
            htmlPageContentlet.setIndexPolicy(IndexPolicy.FORCE);

            //Checkin and Publish.
            Contentlet working = APILocator.getContentletAPI().checkin(htmlPageContentlet, user, false);
            working.setIndexPolicy(IndexPolicy.FORCE);
            APILocator.getContentletAPI().publish(working, user, false);

            contentletsCreated.add(working);
        }
    }

    /**
     * Given a folder we return the number of items inside that folder marked with the Show On Menu
     * flag.
     */
    private int findShowOnMenuUnderFolder(Folder folder, User user)
            throws DotSecurityException, DotDataException {

        int showOnMenu = 0;

        List<IHTMLPage> liveHTMLPages = APILocator.getHTMLPageAssetAPI()
                .getLiveHTMLPages(folder, user, false);
        if (!liveHTMLPages.isEmpty()) {
            for (IHTMLPage page : liveHTMLPages) {
                if (page.isShowOnMenu()) {
                    showOnMenu++;
                }
            }

        }

        List<Folder> subFolders = APILocator.getFolderAPI().findSubFolders(folder, user, false);
        if(!subFolders.isEmpty()) {
            for (Folder subfolder : subFolders) {
                if (subfolder.isShowOnMenu()) {
                    showOnMenu++;
                }
            }
        }

        List<FileAsset> fileAssetsByFolder = APILocator.getFileAssetAPI().findFileAssetsByFolder(folder,"",true,user,true);
        if(!fileAssetsByFolder.isEmpty()){
            for(FileAsset fileAsset : fileAssetsByFolder){
                if(fileAsset.isShowOnMenu()){
                    showOnMenu++;
                }
            }
        }

        return showOnMenu;
    }

    @Test
    public void testNavTool_rootLevel_returnItemsOnlyForSite() throws Exception
    {
        //Get SystemFolder
        final Folder systemFolder = APILocator.getFolderAPI().findSystemFolder();

        //Create new Host
        final Host newHost1 = new SiteDataGen().nextPersisted();

        //Create contentlets on one host
        final File file = File.createTempFile("fileTestEngTrue", ".txt");
        FileUtil.write(file, "helloworld");
        final Contentlet fileAssetOneHost = new FileAssetDataGen(systemFolder, file)
                .host(newHost1).setProperty(FileAssetAPI.SHOW_ON_MENU, "true").nextPersisted();
        final Template template = new TemplateDataGen().nextPersisted();
        final Contentlet pageAssetOneHost = new HTMLPageDataGen(systemFolder, template)
                .showOnMenu(true).host(newHost1).nextPersisted();
        ContentletDataGen.publish(pageAssetOneHost);
        ContentletDataGen.publish(fileAssetOneHost);

        //Create new Host
        final Host newHost2 = new SiteDataGen().nextPersisted();

        //Create contentlets on default host
        final Contentlet fileAssetNewHost = new FileAssetDataGen(systemFolder, file)
                .host(newHost2).setProperty(FileAssetAPI.SHOW_ON_MENU, "true").nextPersisted();
        final Contentlet pageAssetNewHost = new HTMLPageDataGen(systemFolder, template)
                .showOnMenu(true).host(newHost2).nextPersisted();
        ContentletDataGen.publish(pageAssetNewHost);
        ContentletDataGen.publish(fileAssetNewHost);

        NavResult navResult = new NavTool().getNav(newHost2, systemFolder.getPath());
        assertNotNull(navResult);
        assertEquals(2,navResult.getChildren().size());

    }

    /**
     * Method to test: NavTool.getNav
     * Given scenario: get navigation for system folder ("/")
     * Expected result: getting the navigation should not change system folder's hostId
     * @throws Exception exception
     */

    @Test
    public void testNavTool_getNav_SystemFolder_ShouldNotChangeSystemFolderHostId() throws Exception
    {
        //Get SystemFolder
        Folder systemFolder = APILocator.getFolderAPI().findSystemFolder();

        //Create new Host
        final Host host = new SiteDataGen().nextPersisted();

        //Create contentlets on one host
        final File file = File.createTempFile("fileTestEngTrue", ".txt");
        FileUtil.write(file, "helloworld");
        final Contentlet fileAssetOneHost = new FileAssetDataGen(systemFolder, file)
                .host(host).setProperty(FileAssetAPI.SHOW_ON_MENU, "true").nextPersisted();
        final Template template = new TemplateDataGen().nextPersisted();
        final Contentlet pageAssetOneHost = new HTMLPageDataGen(systemFolder, template)
                .showOnMenu(true).host(host).nextPersisted();
        ContentletDataGen.publish(pageAssetOneHost);
        ContentletDataGen.publish(fileAssetOneHost);

        new NavTool().getNav(host, systemFolder.getPath());
        systemFolder = APILocator.getFolderAPI().findSystemFolder();
        assertEquals(Host.SYSTEM_HOST, systemFolder.getHostId());
    }

}
