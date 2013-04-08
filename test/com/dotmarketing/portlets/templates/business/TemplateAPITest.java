package com.dotmarketing.portlets.templates.business;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.dotcms.TestBase;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.cache.StructureCache;
import com.dotmarketing.portlets.AssetUtil;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.structure.model.Structure;
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
    
    @Test
    public void delete() throws Exception {
        
        User user=APILocator.getUserAPI().getSystemUser();
        Host host=APILocator.getHostAPI().findDefaultHost(user, false);
        
        // a container to use inside the template
        Container container = new Container();
        container.setCode("this is the code");
        container.setFriendlyName("test container");
        container.setTitle("his is the title");
        container.setMaxContentlets(5);
        container.setPreLoop("preloop code");
        container.setPostLoop("postloop code");
        Structure st=StructureCache.getStructureByVelocityVarName("host");
        container = APILocator.getContainerAPI().save(container, st, host, user, false);
        
        
        String body="<html><body> #parseContainer('"+container.getIdentifier()+"') </body></html>";
        String title="empty test template "+UUIDGenerator.generateUuid();
        
        Template template=new Template();
        template.setTitle(title);
        template.setBody(body);
        
        Template saved=APILocator.getTemplateAPI().saveTemplate(template, host, user, false);
        
        final String tInode=template.getInode(),tIdent=template.getIdentifier();
        
        APILocator.getTemplateAPI().delete(saved, user, false);
        
        AssetUtil.assertDeleted(tInode, tIdent, "template");
        
        APILocator.getContainerAPI().delete(container, user, false);
        
        AssetUtil.assertDeleted(container.getInode(),container.getIdentifier(),"containers");
    }
}
