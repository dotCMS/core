package com.dotmarketing.viewtools.navigation;

import com.dotcms.IntegrationTestBase;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpageasset.model.IHTMLPage;
import com.dotmarketing.viewtools.LanguageWebAPI;
import com.liferay.portal.model.User;

import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * NavToolTest
 * Created by Oscar Arrieta on 5/4/15.
 */
public class NavToolTest extends IntegrationTestBase{

    private static boolean DEFAULT_PAGE_TO_DEFAULT_LANGUAGE;

    @BeforeClass
    public static void prepare() throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
        DEFAULT_PAGE_TO_DEFAULT_LANGUAGE = LanguageWebAPI.canDefaultPageToDefaultLanguage();
    }

    @Test
    public void testAboutUs() throws Exception { // https://github.com/dotCMS/core/issues/7678

        //Using System User.
        User user = APILocator.getUserAPI().getSystemUser();

        //Using demo.dotcms.com host.
        Host demoHost = APILocator.getHostAPI().findByName("demo.dotcms.com", user, false);

        //Using about us Folder.
        Folder aboutUsFolder = APILocator.getFolderAPI().findFolderByPath("/about-us/", demoHost, user, false);

        //Using Identifier to get the path.
        Identifier aboutUsIdentifier=APILocator.getIdentifierAPI().find(aboutUsFolder);

        NavResult navResult = NavTool.getNav(demoHost, aboutUsIdentifier.getPath(), 1, user);
        assertNotNull(navResult);

        //We are expecting 3 children result for English Language.
        int englishResultChildren = navResult.getChildren().size();
        assertEquals(englishResultChildren, 3);

        navResult = NavTool.getNav(demoHost, aboutUsIdentifier.getPath(), 2, user);
        assertNotNull(navResult);

        int spanishResultChildren = navResult.getChildren().size();
        if (DEFAULT_PAGE_TO_DEFAULT_LANGUAGE) {
            //We are expecting 3 children result for Spanish Language.
            assertEquals(spanishResultChildren, 3);
        } else {
            //We are expecting 2 children result for Spanish Language.
            assertEquals(spanishResultChildren, 2);
        }

        List<IHTMLPage> liveHTMLPages = APILocator.getHTMLPageAssetAPI().getLiveHTMLPages(aboutUsFolder, user, false);

        //List of contentlets created for this test.
        List<Contentlet> contentletsCreated = new ArrayList<>();

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

            //Checkin and Publish.
            Contentlet working = APILocator.getContentletAPI().checkin(htmlPageContentlet, user, false);
            APILocator.getContentletAPI().publish(working, user, false);
            APILocator.getContentletAPI().isInodeIndexed(working.getInode(), true);

            contentletsCreated.add(working);
        }

        navResult = NavTool.getNav(demoHost, aboutUsIdentifier.getPath(), 2, user);
        assertNotNull(navResult);

        if (DEFAULT_PAGE_TO_DEFAULT_LANGUAGE) {
            //Now We are expecting more children result for Spanish Language than English Language.
            assertTrue(englishResultChildren <= navResult.getChildren().size());
        } else {
            //Now We are expecting same children result for Spanish Language and English Language.
            assertEquals(englishResultChildren, navResult.getChildren().size());
        }

        //Now remove all the pages that we created for this tests.
        APILocator.getContentletAPI().unpublish(contentletsCreated, user, false);
        APILocator.getContentletAPI().archive(contentletsCreated, user, false);
        APILocator.getContentletAPI().delete(contentletsCreated, user, false);

        //We should back to 2 in Spanish Nav.
        navResult = NavTool.getNav(demoHost, aboutUsIdentifier.getPath(), 2, user);
        assertNotNull(navResult);

        //Now We are expecting original amount children result for Spanish Language.
        assertEquals(spanishResultChildren, navResult.getChildren().size());

    }

}
