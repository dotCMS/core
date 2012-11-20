package com.dotmarketing.portlets.templates.business;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.dotcms.TestBase;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.UUIDGenerator;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;

public class TemplateAPITest extends TestBase {
    @Test
    public void saveTemplate() throws Exception {
        User user=APILocator.getUserAPI().getSystemUser();
        Host host=APILocator.getHostAPI().findDefaultHost(user, false);
        String body="<html><body> I'm mostly empty </body></html>";
        String title="empty test template "+UUIDGenerator.generateUuid();
        
        Template template=new Template();
        template.setTitle(title);
        template.setBody(body);
        template=APILocator.getTemplateAPI().saveTemplate(template, host, user, false);
        assertTrue(UtilMethods.isSet(template.getInode()));
        assertTrue(UtilMethods.isSet(template.getIdentifier()));
        assertEquals(template.getBody(), body);
        assertEquals(template.getTitle(), title);
        
        // now testing with existing inode and identifier
        String inode=UUIDGenerator.generateUuid();
        String identifier=UUIDGenerator.generateUuid();
        template=new Template();
        template.setTitle(title);
        template.setBody(body);
        template.setInode(inode);
        template.setIdentifier(identifier);
        template=APILocator.getTemplateAPI().saveTemplate(template, host, user, false);
        assertTrue(UtilMethods.isSet(template.getInode()));
        assertTrue(UtilMethods.isSet(template.getIdentifier()));
        assertEquals(template.getBody(), body);
        assertEquals(template.getTitle(), title);
        assertEquals(template.getInode(),inode);
        assertEquals(template.getIdentifier(),identifier);
        
        template=APILocator.getTemplateAPI().findWorkingTemplate(identifier, user, false);
        assertTrue(template!=null);
        assertEquals(template.getInode(),inode);
        assertEquals(template.getIdentifier(),identifier);
        
        // now update with existing inode 
        template.setBody("updated body!");
        String newInode=UUIDGenerator.generateUuid();
        template.setInode(newInode);
        template=APILocator.getTemplateAPI().saveTemplate(template, host, user, false);
        
        // same identifier now new inode
        template=APILocator.getTemplateAPI().findWorkingTemplate(identifier, user, false);
        assertTrue(template!=null);
        assertEquals(template.getInode(),newInode);
        assertEquals(template.getIdentifier(),identifier);
        assertEquals(template.getBody(),"updated body!"); // make sure it took our changes
    }
}
