package com.dotmarketing.portlets.folder.business;

import java.util.List;

import com.dotcms.repackage.org.junit.Assert;
import com.dotcms.repackage.org.junit.Test;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpages.model.HTMLPage;
import com.dotmarketing.portlets.templates.model.Template;
import com.liferay.portal.model.User;

public class FolderAPITest {
    
    @Test
    public void renameFolder() throws Exception {
        User user = APILocator.getUserAPI().getSystemUser();
        Host demo = APILocator.getHostAPI().findByName("demo.dotcms.com", user, false);
        Folder ftest = APILocator.getFolderAPI().createFolders("/folderTest"+System.currentTimeMillis(), demo, user, false);
        Folder ftest1 = APILocator.getFolderAPI().createFolders(ftest.getPath()+"/ff1", demo, user, false);
        Folder ftest2 = APILocator.getFolderAPI().createFolders(ftest.getPath()+"/ff1/ff2", demo, user, false);
        Folder ftest3 = APILocator.getFolderAPI().createFolders(ftest.getPath()+"/ff1/ff2/ff3", demo, user, false);
        
        // get identifiers to cache
        APILocator.getIdentifierAPI().find(ftest);
        APILocator.getIdentifierAPI().find(ftest1);
        APILocator.getIdentifierAPI().find(ftest2);
        APILocator.getIdentifierAPI().find(ftest3);
        
        Assert.assertTrue(APILocator.getFolderAPI().renameFolder(ftest, "folderTestXX"+System.currentTimeMillis(), user, false));
        
        // those should be cleared from cache
        Assert.assertNull(APILocator.getIdentifierAPI().loadFromCache(ftest1.getIdentifier()));
        Assert.assertNull(APILocator.getIdentifierAPI().loadFromCache(ftest2.getIdentifier()));
        Assert.assertNull(APILocator.getIdentifierAPI().loadFromCache(ftest3.getIdentifier()));
        
        // make sure the rename is properly propagated on children (that's done in a db trigger)
        Identifier ident=APILocator.getIdentifierAPI().find(ftest),ident1=APILocator.getIdentifierAPI().find(ftest1),
                ident2=APILocator.getIdentifierAPI().find(ftest2),ident3=APILocator.getIdentifierAPI().find(ftest3);
        Assert.assertTrue(ident.getAssetName().startsWith("folderTestXX"));
        Assert.assertEquals(ident.getPath(),ident1.getParentPath());
        Assert.assertEquals(ident1.getPath(),ident2.getParentPath());
        Assert.assertEquals(ident2.getPath(),ident3.getParentPath());
        
    }
    
    /**
     * Test move folders with subfolders 
     * @throws Exception
     */
    @Test
    public void move() throws Exception {
    	User user = APILocator.getUserAPI().getSystemUser();
        Host demo = APILocator.getHostAPI().findByName("demo.dotcms.com", user, false);
        //create folders and assets
        Folder ftest = APILocator.getFolderAPI().createFolders("/folderSourceTest"+System.currentTimeMillis(), demo, user, false);
        Folder ftest1 = APILocator.getFolderAPI().createFolders(ftest.getPath()+"/ff1", demo, user, false);
        
        Folder ftest2 = APILocator.getFolderAPI().createFolders(ftest.getPath()+"/ff1/ff2", demo, user, false);
        HTMLPage page1 = new HTMLPage();
        String page1Str="page1";
        page1.setTitle(page1Str);
        page1.setFriendlyName(page1Str);
        page1.setPageUrl(page1Str);
        List<Template> templates = APILocator.getTemplateAPI().findTemplatesAssignedTo(demo);
        Template template =null;
        for(Template temp: templates){
        	if(temp.getTitle().equals("Quest - 1 Column")){
        		template=temp;
        		break;
        	}
        }
        page1 = APILocator.getHTMLPageAPI().saveHTMLPage(page1, template, ftest2, user, false);
        
        Folder ftest3 = APILocator.getFolderAPI().createFolders(ftest.getPath()+"/ff1/ff3", demo, user, false);
        HTMLPage page2 = new HTMLPage();
        String page2Str ="page2";
        page2.setTitle(page2Str);
        page2.setFriendlyName(page2Str);
        page2.setPageUrl(page2Str);
        APILocator.getHTMLPageAPI().saveHTMLPage(page2, template, ftest3, user, false);
        
        Folder destinationftest = APILocator.getFolderAPI().createFolders("/folderDestinationTest"+System.currentTimeMillis(), demo, user, false);
        
        
        APILocator.getFolderAPI().move(ftest1, destinationftest, user, false);
        
        //validate that the folder and assets were moved
        Folder  newftest1 = APILocator.getFolderAPI().findFolderByPath(destinationftest.getPath()+ftest1.getName(), demo, user, false);
        Assert.assertTrue("Folder ("+ftest1.getName()+") wasn't moved", newftest1 != null);
        
        Folder  newftest2 = APILocator.getFolderAPI().findFolderByPath(newftest1.getPath()+ftest2.getName(), demo, user, false);
        Assert.assertNotNull(newftest2);
        List<HTMLPage> pages = APILocator.getHTMLPageAPI().findWorkingHTMLPages(newftest2);
        Assert.assertTrue(pages.size()==1 && pages.get(0).getTitle().equals(page1Str));
        Folder  newftest3 = APILocator.getFolderAPI().findFolderByPath(newftest1.getPath()+ftest3.getName(), demo, user, false);
        Assert.assertNotNull(newftest3);
        pages = APILocator.getHTMLPageAPI().findWorkingHTMLPages(newftest3);
        Assert.assertTrue(pages.size()==1 && pages.get(0).getTitle().equals(page2Str));       
    }

}
