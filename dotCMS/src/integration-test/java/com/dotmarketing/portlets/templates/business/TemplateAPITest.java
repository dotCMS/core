package com.dotmarketing.portlets.templates.business;

import com.dotcms.IntegrationTestBase;
import com.dotcms.datagen.HTMLPageDataGen;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.ContainerStructure;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Inode;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.business.VersionableAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.AssetUtil;
import com.dotmarketing.portlets.containers.business.ContainerAPI;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.UUIDGenerator;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class TemplateAPITest extends IntegrationTestBase {

    private static ContainerAPI containerAPI;
    private static HostAPI hostAPI;
    private static TemplateAPI templateAPI;
    private static User user;
    private static UserAPI userAPI;
    private static VersionableAPI versionableAPI;
    private static Host host;

    @BeforeClass
    public static void prepare () throws Exception {
    	
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();

        containerAPI   = APILocator.getContainerAPI();
        hostAPI        = APILocator.getHostAPI();
        templateAPI    = APILocator.getTemplateAPI();
        userAPI        = APILocator.getUserAPI();
        user           = userAPI.getSystemUser();
        versionableAPI = APILocator.getVersionableAPI();
        host           = hostAPI.findDefaultHost(user, false);
    }
	
    @Test
    public void saveTemplate() throws Exception {
        final Host host= hostAPI.findDefaultHost(user, false);
        String body="<html><body> I'm mostly empty </body></html>";
        String title="empty test template "+UUIDGenerator.generateUuid();

        Template template=new Template();
        template.setTitle(title);
        template.setBody(body);
        template= templateAPI.saveTemplate(template, host, user, false);
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
        template= templateAPI.saveTemplate(template, host, user, false);
        assertTrue(UtilMethods.isSet(template.getInode()));
        assertTrue(UtilMethods.isSet(template.getIdentifier()));
        assertEquals(template.getBody(), body);
        assertEquals(template.getTitle(), title);
        assertEquals(template.getInode(),inode);
        assertEquals(template.getIdentifier(),identifier);

        template= templateAPI.findWorkingTemplate(identifier, user, false);
        assertTrue(template!=null);
        assertEquals(template.getInode(),inode);
        assertEquals(template.getIdentifier(),identifier);

        // now update with existing inode
        template.setBody("updated body!");
        String newInode=UUIDGenerator.generateUuid();
        template.setInode(newInode);
        template= templateAPI.saveTemplate(template, host, user, false);

        // same identifier now new inode
        template= templateAPI.findWorkingTemplate(identifier, user, false);
        assertTrue(template!=null);
        assertEquals(template.getInode(),newInode);
        assertEquals(template.getIdentifier(),identifier);
        assertEquals(template.getBody(),"updated body!"); // make sure it took our changes
    }

    @Test
    public void delete() throws Exception {
        Host host= hostAPI.findDefaultHost(user, false);

        // a container to use inside the template
        Container container = new Container();
        container.setFriendlyName("test container");
        container.setTitle("his is the title");
        container.setMaxContentlets(5);
        container.setPreLoop("preloop code");
        container.setPostLoop("postloop code");
        Structure st=CacheLocator.getContentTypeCache().getStructureByVelocityVarName("host");

        List<ContainerStructure> csList = new ArrayList<ContainerStructure>();
        ContainerStructure cs = new ContainerStructure();
        cs.setStructureId(st.getInode());
        cs.setCode("this is the code");
        csList.add(cs);
        container = containerAPI.save(container, csList, host, user, false);


        String body="<html><body> #parseContainer('"+container.getIdentifier()+"') </body></html>";
        String title="empty test template "+UUIDGenerator.generateUuid();

        Template template=new Template();
        template.setTitle(title);
        template.setBody(body);

        final Template saved= templateAPI.saveTemplate(template, host, user, false);

        final String tInode=template.getInode(),tIdent=template.getIdentifier();

        templateAPI.delete(saved, user, false);

        AssetUtil.assertDeleted(tInode, tIdent, "template");

        containerAPI.delete(container, user, false);

        AssetUtil.assertDeleted(container.getInode(),container.getIdentifier(), Inode.Type.CONTAINERS.getValue());
    }

    @Test
    public void findPagesByTemplate() throws Exception {
        User user=APILocator.getUserAPI().getSystemUser();
        Host host=APILocator.getHostAPI().findDefaultHost(user, false);

        Template template = null;
        Folder folder = null;
        HTMLPageAsset page = null;

        try {
            //Create a Template
            template = new Template();
            template.setTitle("Title");
            template.setBody("Body");
            template = APILocator.getTemplateAPI().saveTemplate(template, host, user, false);

            //Create a Folder
            folder = APILocator.getFolderAPI().createFolders(
                    "/test_junit/test_" + UUIDGenerator.generateUuid().replaceAll("-", "_"), host,
                    user, false);

            //Create a Page inside the Folder assigned to the newly created Template
            page = new HTMLPageDataGen(folder, template).nextPersisted();


            //wait a second before attempting to search the pages with elastic search
            //APILocator.getContentletAPI().isInodeIndexed(page.getInode());

            //Find pages by template
            List<Contentlet> pages = APILocator.getHTMLPageAssetAPI()
                    .findPagesByTemplate(template, user, false);
            assertFalse(pages.isEmpty()); //Should contain dependencies
            assertTrue(pages.size() == 1); //Only one dependency, the page we just created
            assertEquals(page.getInode(), pages.get(0).getInode()); //Page inode should be the same

            //Delete the page
            HTMLPageDataGen.remove(page);
            page = null;
            //Now find again pages by template
            pages = APILocator.getHTMLPageAssetAPI().findPagesByTemplate(template, user, false);
            assertTrue(pages.isEmpty()); //Should NOT contain dependencies

        } finally {
            //Clean up
            if (page != null) {
                HTMLPageDataGen.remove(page);
            }
            if (template != null) {
                APILocator.getTemplateAPI().delete(template, user, false);
            }
            if (folder != null) {
                APILocator.getFolderAPI().delete(folder, user, false);
            }
        }

    }

    @Test
    public void findLiveTemplate() throws Exception {

        Template template=new Template();
        template.setTitle("empty test template "+UUIDGenerator.generateUuid());
        template.setBody("<html><body> I'm mostly empty </body></html>");
        template= templateAPI.saveTemplate(template, host, user, false);

        Template live = templateAPI
                .findLiveTemplate(template.getIdentifier(), user, false);
        assertTrue(live==null || !InodeUtils.isSet(live.getInode()));

        versionableAPI.setLive(template);

        live = templateAPI.findLiveTemplate(template.getIdentifier(), user, false);
        assertTrue(live!=null && InodeUtils.isSet(live.getInode()));
        assertEquals(template.getInode(),live.getInode());

        templateAPI.delete(template, user, false);
    }

    @Test
    public void testFindTemplates() throws DotDataException, DotSecurityException {
        final List<Template> result = templateAPI
                .findTemplates(user, false, null, host.getIdentifier(), null, null, null, 0, -1,
                        null);

        assertNotNull(result);
        assertTrue(!result.isEmpty());
    }

    @Test
    public void testFindWorkingTemplateByName() throws Exception {
        Template template = new Template();
        final TemplateFactory templateFactory = new TemplateFactoryImpl();
        template.setTitle("empty test template " + UUIDGenerator.generateUuid());
        template.setBody("<html><body> I'm mostly empty </body></html>");
        template.setOwner("template's owner");

        try {
            template = templateAPI.saveTemplate(template, host, user, false);

            final Template result = templateFactory.findWorkingTemplateByName(template.getTitle(), host);

            assertNotNull(result);
            assertEquals(template.getInode(), result.getInode());
            assertTrue(template.getOwner()!=null && template.getOwner().equals(result.getOwner()));
        } finally {
            if (template.getInode() != null) {
                templateAPI.delete(template, user, false);
            }
        }
    }

    @Test
    public void testFindTemplatesAssignedTo() throws DotDataException, DotSecurityException {
        final List<Template> result = templateAPI.findTemplatesAssignedTo(host);

        assertNotNull(result);
        assertTrue(!result.isEmpty());
    }

    @Test
    public void copyTemplate() throws Exception {
        Template oldTemplate = null;
        Template newTemplate = null;

        try {
            //Create a Template.
            oldTemplate = new Template();
            oldTemplate.setTitle("Title");
            oldTemplate.setBody("<html><body> I'm mostly empty </body></html>");
            oldTemplate = templateAPI.saveTemplate(oldTemplate, host, user, false);

            //Copy Template.
            newTemplate = templateAPI.copy(oldTemplate, user);

            //Body should be the same.
            Assert.assertEquals(oldTemplate.getBody(), newTemplate.getBody());

            //Name, identifier, inode and mod_date shouldn't be the same.
            Assert.assertNotEquals(oldTemplate.getTitle(), newTemplate.getTitle());
            Assert.assertNotEquals(oldTemplate.getIdentifier(), newTemplate.getIdentifier());
            Assert.assertNotEquals(oldTemplate.getInode(), newTemplate.getInode());
            Assert.assertNotEquals(oldTemplate.getModDate(), newTemplate.getModDate());

        } finally {
            //Clean up.
            if (oldTemplate != null) {
                templateAPI.delete(oldTemplate, user, false);
            }
            if (newTemplate != null) {
                templateAPI.delete(newTemplate, user, false);
            }
        }
    }

    @Test
    public void testFindTemplatesNoLayout () throws Exception {
        Template template = null;
        Template layout = null;
        try {

            template = new Template();
            template.setTitle("Template Title");
            template.setBody("<html><body> Empty Template </body></html>");
            template = templateAPI.saveTemplate(template, host, user, false);

            layout = new Template();
            //No title, this is a layout
            layout.setBody("<html><body> Empty Layout </body></html>");
            layout = templateAPI.saveTemplate(layout, host, user, false);

            //This method should only return Templates, no Layouts
            List<Template> templates = APILocator.getTemplateAPI().findTemplates(user, false, null, null, null,
                                                        null, null, 0, 1000, null);

            assertFalse(templates.isEmpty());
            for (final Template temp : templates) {
                assertTrue(temp.isTemplate());
            }

            //This method should only return Templates, no Layouts
            templates = templateAPI.findTemplatesUserCanUse(user, null, null, false,
                                                        0, 1000);

            assertFalse(templates.isEmpty());
            for (final Template temp : templates) {
                assertTrue(temp.isTemplate());
            }

        } finally {
            if (template != null) {
                templateAPI.delete(template, user, false);
            }
            if (layout != null) {
                templateAPI.delete(layout, user, false);
            }
        }
    }

    @Test
    public void testFindTemplatesUserCanUse_IncludeUniqueFilter_ShouldListOnlyOneResult() throws Exception {
        Template template = null;
        Template anotherTemplate = null;
        try {

            template = new Template();
            final String uniqueString = UUIDGenerator.generateUuid();
            final String uniqueTitle =  uniqueString + " This one will show up";
            template.setTitle(uniqueTitle);
            template.setBody("<html><body> Empty Template </body></html>");
            template = templateAPI.saveTemplate(template, host, user, false);

            anotherTemplate = new Template();
            anotherTemplate.setTitle("I am not invited");
            anotherTemplate.setBody("<html><body> Empty Template </body></html>");
            anotherTemplate = templateAPI.saveTemplate(anotherTemplate, host, user, false);

            final List<Template> filteredTemplates = APILocator.getTemplateAPI().findTemplatesUserCanUse(user, host.getIdentifier(), uniqueString, true,0, 1000);

            assertEquals(1, filteredTemplates.size());
            assertEquals(uniqueTitle, filteredTemplates.get(0).getTitle());

        } finally {
            if (template != null) {
                templateAPI.delete(template, user, false);
            }
            if (anotherTemplate != null) {
                templateAPI.delete(anotherTemplate, user, false);
            }
        }
    }
}
