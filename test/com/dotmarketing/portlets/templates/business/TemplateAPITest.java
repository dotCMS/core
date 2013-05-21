package com.dotmarketing.portlets.templates.business;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.dotcms.TestBase;
import com.dotmarketing.beans.ContainerStructure;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.cache.StructureCache;
import com.dotmarketing.portlets.AssetUtil;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.InodeUtils;
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
        container.setFriendlyName("test container");
        container.setTitle("his is the title");
        container.setMaxContentlets(5);
        container.setPreLoop("preloop code");
        container.setPostLoop("postloop code");
        Structure st=StructureCache.getStructureByVelocityVarName("host");

        List<ContainerStructure> csList = new ArrayList<ContainerStructure>();
        ContainerStructure cs = new ContainerStructure();
        cs.setStructureId(st.getInode());
        cs.setCode("this is the code");
        csList.add(cs);
        container = APILocator.getContainerAPI().save(container, csList, host, user, false);


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

    @Test
    public void findLiveTemplate() throws Exception {
        User user=APILocator.getUserAPI().getSystemUser();
        Host host=APILocator.getHostAPI().findDefaultHost(user, false);

        Template template=new Template();
        template.setTitle("empty test template "+UUIDGenerator.generateUuid());
        template.setBody("<html><body> I'm mostly empty </body></html>");
        template=APILocator.getTemplateAPI().saveTemplate(template, host, user, false);

        Template live = APILocator.getTemplateAPI().findLiveTemplate(template.getIdentifier(), user, false);
        assertTrue(live==null || !InodeUtils.isSet(live.getInode()));

        APILocator.getVersionableAPI().setLive(template);

        live = APILocator.getTemplateAPI().findLiveTemplate(template.getIdentifier(), user, false);
        assertTrue(live!=null && InodeUtils.isSet(live.getInode()));
        assertEquals(template.getInode(),live.getInode());

        APILocator.getTemplateAPI().delete(template, user, false);
    }
}
