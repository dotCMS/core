package com.dotmarketing.portlets.htmlpages.business;

import java.util.List;

import org.junit.Test;
import static org.junit.Assert.*;
import com.dotcms.TestBase;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpages.model.HTMLPage;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.UUIDGenerator;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;

public class HTMLPageAPITest extends TestBase {
    @Test
    public void saveHTMLPage() throws Exception {
        User sysuser=APILocator.getUserAPI().getSystemUser();
        Host host=APILocator.getHostAPI().findDefaultHost(sysuser, false);
        String ext="."+Config.getStringProperty("VELOCITY_PAGE_EXTENSION");
        
        Template template=new Template();
        template.setTitle("a template "+UUIDGenerator.generateUuid());
        template.setBody("<html><body> I'm mostly empty </body></html>");
        template=APILocator.getTemplateAPI().saveTemplate(template, host, sysuser, false);
        
        Folder folder=APILocator.getFolderAPI().createFolders(
                "/test_junit/test_"+UUIDGenerator.generateUuid().replaceAll("-", "_"), host, sysuser, false);
        
        HTMLPage page=new HTMLPage();
        page.setPageUrl("testpage"+ext);
        page.setFriendlyName("testpage"+ext);
        page.setTitle("testpage"+ext);
        page=APILocator.getHTMLPageAPI().saveHTMLPage(page, template, folder, sysuser, false);
        assertTrue(page!=null);
        assertTrue(UtilMethods.isSet(page.getInode()));
        assertTrue(UtilMethods.isSet(page.getIdentifier()));
        
        List<HTMLPage> pages = APILocator.getHTMLPageAPI().findWorkingHTMLPages(folder);
        assertTrue(pages.size()==1);
        
        // now with existing inode/identifier
        String existingInode=UUIDGenerator.generateUuid();
        String existingIdentifier=UUIDGenerator.generateUuid();
        
        folder=APILocator.getFolderAPI().createFolders(
                "/test_junit/test_"+UUIDGenerator.generateUuid().replaceAll("-", "_"), host, sysuser, false);
        page=new HTMLPage();
        page.setPageUrl("testpage"+ext);
        page.setFriendlyName("testpage"+ext);
        page.setTitle("testpage"+ext);
        page.setInode(existingInode);
        page.setIdentifier(existingIdentifier);
        page=APILocator.getHTMLPageAPI().saveHTMLPage(page, template, folder, sysuser, false);
        assertEquals(existingInode,page.getInode());
        assertEquals(existingIdentifier,page.getIdentifier());
        
        pages = APILocator.getHTMLPageAPI().findWorkingHTMLPages(folder);
        assertTrue(pages.size()==1);
        page=pages.get(0);
        assertEquals(existingInode,page.getInode());
        assertEquals(existingIdentifier,page.getIdentifier());
        
        // now with existing inode but this time with an update
        HibernateUtil.getSession().clear();
        String newInode=UUIDGenerator.generateUuid();
        page.setInode(newInode);
        page.setTitle("other title");
        page=APILocator.getHTMLPageAPI().saveHTMLPage(page, template, folder, sysuser, false);
        assertEquals(newInode,page.getInode());
        assertEquals(existingIdentifier,page.getIdentifier());
        assertEquals("other title",page.getTitle());
    }
}
