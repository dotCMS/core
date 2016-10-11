package com.dotmarketing.viewtools.navigation;

import com.dotcms.TestBase;

import org.junit.BeforeClass;
import org.junit.Test;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpageasset.model.IHTMLPage;
import com.dotmarketing.util.IntegrationTestInitService;
import com.liferay.portal.model.User;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * Created by Oscar Arrieta on 5/4/15.
 */
public class NavToolTest extends TestBase{
	
	@BeforeClass
    public static void prepare() throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
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

        NavTool navTool = new NavTool();

        NavResult navResult = navTool.getNav(demoHost, aboutUsIdentifier.getPath(), 1, user);

        //We are expecting 4 children result for English Language.
        int englishResultChildren = navResult.getChildren().size();

        navResult = navTool.getNav(demoHost, aboutUsIdentifier.getPath(), 2, user);

        //We are expecting 2 children result for Spanish Language.
        int spanishResultChildren = navResult.getChildren().size();

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
            htmlPageContentlet.getMap().put("languageId", new Long(2));

            //Checkin and Publish.
            Contentlet working = APILocator.getContentletAPI().checkin(htmlPageContentlet, user, false);
            APILocator.getContentletAPI().publish(working, user, false);
            APILocator.getContentletAPI().isInodeIndexed(working.getInode(), true);

            contentletsCreated.add(working);
        }

        navResult = navTool.getNav(demoHost, aboutUsIdentifier.getPath(), 2, user);

        //Now We are expecting same children result for Spanish Language that English Language.
        assertEquals(englishResultChildren, navResult.getChildren().size());

        //Now remove all the pages that we created for this tests.
        APILocator.getContentletAPI().unpublish(contentletsCreated, user, false);
        APILocator.getContentletAPI().archive(contentletsCreated, user, false);
        APILocator.getContentletAPI().delete(contentletsCreated, user, false);

        //We should back to 2 in Spanish Nav.
        navResult = navTool.getNav(demoHost, aboutUsIdentifier.getPath(), 2, user);

        //Now We are expecting original amount children result for Spanish Language.
        assertEquals(spanishResultChildren, navResult.getChildren().size());

    }

}
