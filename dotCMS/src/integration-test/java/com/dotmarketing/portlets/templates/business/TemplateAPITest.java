package com.dotmarketing.portlets.templates.business;

import com.dotcms.IntegrationTestBase;
import com.dotcms.content.elasticsearch.business.ESContentFactoryImpl;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.datagen.HTMLPageDataGen;
import com.dotcms.datagen.TemplateDataGen;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.ContainerStructure;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.Inode;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.FactoryLocator;
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
import com.dotmarketing.portlets.templates.design.bean.ContainerUUID;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UUIDGenerator;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import java.util.Map;
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
    public void getContainersUUIDFromDrawTemplateBodyTest() throws Exception {

        final String templateBody = "  This is just test<br/>  \n" +
                                    "  #parseContainer   ('f4a02846-7ca4-4e08-bf07-a61366bbacbb','1552493847863')  \n" +
                                    "  <p>This is just test</p>  \n" +
                                    "  #parseContainer   ('/application/containers/test1/','1552493847864')  \n" +
                                    "#parseContainer('/application/containers/test2/','1552493847868')\n" +
                                    "#parseContainer('/application/containers/test3/'     ,'1552493847869'       )\n" +
                                    "#parseContainer(    '/application/containers/test4/',    '1552493847870')\n";

        final TemplateAPI templateAPI = APILocator.getTemplateAPI();
        final List<ContainerUUID> containerUUIDS = templateAPI.getContainersUUIDFromDrawTemplateBody(templateBody);

        assertNotNull(containerUUIDS);
        assertEquals(5, containerUUIDS.size());

        final ContainerUUID containerUUID1 = containerUUIDS.get(0);
        assertEquals("f4a02846-7ca4-4e08-bf07-a61366bbacbb", containerUUID1.getIdentifier());
        assertEquals("1552493847863", containerUUID1.getUUID());

        final ContainerUUID containerUUID2 = containerUUIDS.get(1);
        assertEquals("/application/containers/test1/", containerUUID2.getIdentifier());
        assertEquals("1552493847864", containerUUID2.getUUID());

        final ContainerUUID containerUUID3 = containerUUIDS.get(2);
        assertEquals("/application/containers/test2/", containerUUID3.getIdentifier());
        assertEquals("1552493847868", containerUUID3.getUUID());

        final ContainerUUID containerUUID4 = containerUUIDS.get(3);
        assertEquals("/application/containers/test3/", containerUUID4.getIdentifier());
        assertEquals("1552493847869", containerUUID4.getUUID());

        final ContainerUUID containerUUID5 = containerUUIDS.get(4);
        assertEquals("/application/containers/test4/", containerUUID5.getIdentifier());
        assertEquals("1552493847870", containerUUID5.getUUID());
    }

    @Test
    public void getContainersUUIDFromDrawTemplateBodyDoubleCommasTest() throws Exception {

        final String templateBody = "  This is just test<br/>  \n" +
                "  #parseContainer   (\"f4a02846-7ca4-4e08-bf07-a61366bbacbb\",\"1552493847863\")  \n" +
                "  <p>This is just test</p>  \n" +
                "  #parseContainer   (\"/application/containers/test1/\",'1552493847864\")  \n" +
                "#parseContainer(\"/application/containers/test2/\",'1552493847868')\n" +
                "#parseContainer('/application/containers/test3/'     ,'1552493847869'       )\n" +
                "#parseContainer(    '/application/containers/test4/',    '1552493847870')\n";

        final TemplateAPI templateAPI = APILocator.getTemplateAPI();
        final List<ContainerUUID> containerUUIDS = templateAPI.getContainersUUIDFromDrawTemplateBody(templateBody);

        assertNotNull(containerUUIDS);
        assertEquals(5, containerUUIDS.size());

        final ContainerUUID containerUUID1 = containerUUIDS.get(0);
        assertEquals("f4a02846-7ca4-4e08-bf07-a61366bbacbb", containerUUID1.getIdentifier());
        assertEquals("1552493847863", containerUUID1.getUUID());

        final ContainerUUID containerUUID2 = containerUUIDS.get(1);
        assertEquals("/application/containers/test1/", containerUUID2.getIdentifier());
        assertEquals("1552493847864", containerUUID2.getUUID());

        final ContainerUUID containerUUID3 = containerUUIDS.get(2);
        assertEquals("/application/containers/test2/", containerUUID3.getIdentifier());
        assertEquals("1552493847868", containerUUID3.getUUID());

        final ContainerUUID containerUUID4 = containerUUIDS.get(3);
        assertEquals("/application/containers/test3/", containerUUID4.getIdentifier());
        assertEquals("1552493847869", containerUUID4.getUUID());

        final ContainerUUID containerUUID5 = containerUUIDS.get(4);
        assertEquals("/application/containers/test4/", containerUUID5.getIdentifier());
        assertEquals("1552493847870", containerUUID5.getUUID());
    }

    @Test
    public void getContainersUUIDFromDrawTemplateBodyLegacyTest() throws Exception {

        final String templateBody = "  This is just test<br/>  \n" +
                "  #parseContainer   (\"f4a02846-7ca4-4e08-bf07-a61366bbacbb\")  \n" +
                "  <p>This is just test</p>  \n" +
                "  #parseContainer   (\"/application/containers/test1/\",'1552493847864\")  \n" +
                "#parseContainer(\"/application/containers/test2/\",'1552493847868')\n" +
                "#parseContainer('/application/containers/test3/'    )\n" +
                "#parseContainer(    '/application/containers/test4/',    '1552493847870')\n";

        final TemplateAPI templateAPI = APILocator.getTemplateAPI();
        final List<ContainerUUID> containerUUIDS = templateAPI.getContainersUUIDFromDrawTemplateBody(templateBody);

        assertNotNull(containerUUIDS);
        assertEquals(5, containerUUIDS.size());

        final ContainerUUID containerUUID1 = containerUUIDS.get(0);
        assertEquals("f4a02846-7ca4-4e08-bf07-a61366bbacbb", containerUUID1.getIdentifier());
        assertEquals(ContainerUUID.UUID_LEGACY_VALUE, containerUUID1.getUUID());

        final ContainerUUID containerUUID2 = containerUUIDS.get(1);
        assertEquals("/application/containers/test1/", containerUUID2.getIdentifier());
        assertEquals("1552493847864", containerUUID2.getUUID());

        final ContainerUUID containerUUID3 = containerUUIDS.get(2);
        assertEquals("/application/containers/test2/", containerUUID3.getIdentifier());
        assertEquals("1552493847868", containerUUID3.getUUID());

        final ContainerUUID containerUUID4 = containerUUIDS.get(3);
        assertEquals("/application/containers/test3/", containerUUID4.getIdentifier());
        assertEquals(ContainerUUID.UUID_LEGACY_VALUE, containerUUID4.getUUID());

        final ContainerUUID containerUUID5 = containerUUIDS.get(4);
        assertEquals("/application/containers/test4/", containerUUID5.getIdentifier());
        assertEquals("1552493847870", containerUUID5.getUUID());
    }


    @Test
    public void getContainersUUIDFromDrawTemplateBodyTestNullInput() throws Exception {

        final String templateBody = null;

        final TemplateAPI templateAPI = APILocator.getTemplateAPI();
        final List<ContainerUUID> containerUUIDS = templateAPI.getContainersUUIDFromDrawTemplateBody(templateBody);

        assertNotNull(containerUUIDS);
        assertEquals(0, containerUUIDS.size());
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

    /**
     * Method to test: publishTemplate
     * Given Scenario: Create a template, publish it
     * ExpectedResult: Template should be live true
     *
     */
    @Test
    public void publishTemplate_expects_live_true() throws Exception {

        final Host host    = hostAPI.findDefaultHost(user, false);
        final String body  = "<html><body> I'm mostly empty </body></html>";
        final String title = "empty test template "+UUIDGenerator.generateUuid();
        final Template template = new Template();
        template.setTitle(title);
        template.setBody(body);
        final Template templateSaved = templateAPI.saveTemplate(template, host, user, false);
        assertTrue(UtilMethods.isSet(templateSaved.getInode()));
        assertTrue(UtilMethods.isSet(templateSaved.getIdentifier()));
        assertEquals(templateSaved.getBody(), body);
        assertEquals(templateSaved.getTitle(), title);
        assertFalse(templateSaved.isLive());

        templateAPI.publishTemplate(templateSaved, user, false);
        assertTrue(templateSaved.isLive());
    }

    /**
     * Method to test: unpublishTemplate
     * Given Scenario: Create a template, publish and unpublish it
     * ExpectedResult: Template should be live false at the end
     *
     */
    @Test
    public void unpublishTemplate_expects_live_false() throws Exception {

        final Host host    = hostAPI.findDefaultHost(user, false);
        final String body  = "<html><body> I'm mostly empty </body></html>";
        final String title = "empty test template "+UUIDGenerator.generateUuid();
        final Template template = new Template();
        template.setTitle(title);
        template.setBody(body);
        final Template templateSaved = templateAPI.saveTemplate(template, host, user, false);
        assertTrue(UtilMethods.isSet(templateSaved.getInode()));
        assertTrue(UtilMethods.isSet(templateSaved.getIdentifier()));
        assertEquals(templateSaved.getBody(), body);
        assertEquals(templateSaved.getTitle(), title);
        assertFalse(templateSaved.isLive());

        templateAPI.publishTemplate(templateSaved, user, false);
        assertTrue(templateSaved.isLive());

        assertTrue(templateAPI.unpublishTemplate(templateSaved, user, false));
        assertFalse(templateSaved.isLive());
    }

    /**
     * Method to test: archive
     * Given Scenario: Create a template, archive
     * ExpectedResult: Template should be archive true
     *
     */
    @Test
    public void archiveTemplate_expects_archive_true() throws Exception {

        final Host host    = hostAPI.findDefaultHost(user, false);
        final String body  = "<html><body> I'm mostly empty </body></html>";
        final String title = "empty test template "+UUIDGenerator.generateUuid();
        final Template template = new Template();
        template.setTitle(title);
        template.setBody(body);
        final Template templateSaved = templateAPI.saveTemplate(template, host, user, false);
        assertTrue(UtilMethods.isSet(templateSaved.getInode()));
        assertTrue(UtilMethods.isSet(templateSaved.getIdentifier()));
        assertEquals(templateSaved.getBody(), body);
        assertEquals(templateSaved.getTitle(), title);
        assertFalse(templateSaved.isLive());

        templateAPI.archive(templateSaved, user, false);
        assertTrue(templateSaved.isArchived());
    }

    /**
     * Method to test:  unarchive
     * Given Scenario: Create a template, archive and unarchive
     * ExpectedResult: Template should be archive false
     *
     */
    @Test
    public void archiveTemplate_expects_unarchive_true() throws Exception {

        final Host host    = hostAPI.findDefaultHost(user, false);
        final String body  = "<html><body> I'm mostly empty </body></html>";
        final String title = "empty test template "+UUIDGenerator.generateUuid();
        final Template template = new Template();
        template.setTitle(title);
        template.setBody(body);
        final Template templateSaved = templateAPI.saveTemplate(template, host, user, false);
        assertTrue(UtilMethods.isSet(templateSaved.getInode()));
        assertTrue(UtilMethods.isSet(templateSaved.getIdentifier()));
        assertEquals(templateSaved.getBody(), body);
        assertEquals(templateSaved.getTitle(), title);
        assertFalse(templateSaved.isLive());

        templateAPI.archive(templateSaved, user, false);
        assertTrue(templateSaved.isArchived());

        templateAPI.unarchive(templateSaved,user);
        assertFalse(templateSaved.isArchived());
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

    /**
     * Method to test: {@link TemplateAPI#findLiveTemplate(String, User, boolean)}
     * Given Scenario: Saves a new template and checks if is Live (false), then publish the template
     * and check again if is live (true).
     * ExpectedResult: template after been published should be live = true
     */
    @Test
    public void findLiveTemplate() throws Exception {

        Template template=new Template();
        template.setTitle("empty test template "+UUIDGenerator.generateUuid());
        template.setBody("<html><body> I'm mostly empty </body></html>");
        template= templateAPI.saveTemplate(template, host, user, false);

        Template live = templateAPI
                .findLiveTemplate(template.getIdentifier(), user, false);
        assertNull(live);//template has not been published

        //Publish template
        versionableAPI.setLive(template);

        live = templateAPI.findLiveTemplate(template.getIdentifier(), user, false);
        assertNotNull(live);//template has been published
        assertEquals(template.getInode(),live.getInode());//inode live is the template inode
        assertFalse(live.getOwner().isEmpty());//check owner was pulled
        assertFalse(live.getIDate().toString().isEmpty());//check idate was pulled

        templateAPI.delete(template, user, false);
    }

    /**
     * Method to test: {@link TemplateAPI#findTemplates(User, boolean, Map, String, String, String, String, int, int, String)}
     * Given Scenario: Saves a new template and finds all the templates the user can use
     * ExpectedResult: list of templates the user can use, it must contain the template created for this test
     */
    @Test
    public void testFindTemplates() throws DotDataException, DotSecurityException {
        final String title = "testTemplate_" + System.currentTimeMillis();
        final Template template = new TemplateDataGen().title(title).nextPersisted();

        final List<Template> result = templateAPI
                .findTemplates(user, false, null, host.getIdentifier(), null, null, null, 0, -1,
                        null);

        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertTrue(result.contains(template));
        assertFalse(result.get(0).getOwner().isEmpty());//check owner was pulled
        assertFalse(result.get(0).getIDate().toString().isEmpty());//check idate was pulled

    }

    /**
     * Method to test: {@link TemplateFactory#findWorkingTemplateByName(String, Host)}
     * Given Scenario: Saves a new template and find the working version of it using the template.title and host
     * ExpectedResult: working version of the template
     */
    @Test
    public void testFindWorkingTemplateByName() throws Exception {
        final String title = "testTemplate_" + System.currentTimeMillis();
        final Template template_version1 = new TemplateDataGen().title(title).host(host).nextPersisted();
        Thread.sleep(1000);
        template_version1.setTitle(title + "_1");
        template_version1.setInode("");
        final Template template_version2 = TemplateDataGen.save(template_version1);

        final Template result = FactoryLocator.getTemplateFactory().findWorkingTemplateByName(title + "_1", host);
        assertNotNull(result);
        assertEquals(template_version2.getInode(), result.getInode());
        assertFalse(result.getOwner().isEmpty());//check owner was pulled
        assertFalse(result.getIDate().toString().isEmpty());//check idate was pulled
    }


    /**
     * Method to test: {@link TemplateAPI#findWorkingTemplate(String, User, boolean)}
     * Given Scenario: Tries to find a non-existent working template
     * ExpectedResult: null template
     */
    @Test
    public void testFindWorkingTemplateNoNPE() throws DotDataException, DotSecurityException {
        try {
            final Template template = templateAPI.findWorkingTemplate("NO_TEMPLATE",
                    APILocator.getUserAPI().getSystemUser(), false);

            assertNull(template);

        }
        catch(NullPointerException e) {
            Logger.error(this, "getting non-existent template should not throw an NPE", e);
            assertTrue("getting non-existent template should not throw an NPE", false);
        }
    }

    /**
     * Method to test: {@link TemplateAPI#findWorkingTemplate(String, User, boolean)}
     * Given Scenario: Tries to find a non-existent live template
     * ExpectedResult: null template
     */
    @Test
    public void testFindLiveTemplateNoNPE() throws DotDataException, DotSecurityException {


        try {
            final Template template = templateAPI.findLiveTemplate("NO_TEMPLATE",
                    APILocator.getUserAPI().getSystemUser(), false);

            assertNull(template);
            
        }
        catch(NullPointerException e) {
            Logger.error(this, "getting non-existent template should not throw an NPE", e);
            assertTrue("getting non-existent template should not throw an NPE", false);
        }
    }

    /**
     * Method to test: {@link TemplateAPI#findTemplatesAssignedTo(Host,boolean)}
     * Given Scenario: Finds all templates under a host
     * ExpectedResult: list of templates that live under a host
     */
    @Test
    public void testFindTemplatesAssignedTo() throws DotDataException {
        final String title = "testTemplate_" + System.currentTimeMillis();
        final Template template = new TemplateDataGen().title(title).nextPersisted();

        final List<Template> result = templateAPI.findTemplatesAssignedTo(host);
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertTrue(result.contains(template));
        assertFalse(result.get(0).getOwner().isEmpty());//check owner was pulled
        assertFalse(result.get(0).getIDate().toString().isEmpty());//check idate was pulled
    }

    /**
     * Method to test: {@link TemplateAPI#copy(Template, User)}
     * Given Scenario: Creates a template and then a copy of it
     * ExpectedResult: template copied successfully
     */
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

    /**
     * Method to test: {@link TemplateAPI#findTemplatesUserCanUse(User, String, String, boolean, int, int)}
     * Given Scenario: Finds all the templates an user can use that matches the filter
     * ExpectedResult: list of templates that live under a host
     */
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
            assertFalse(filteredTemplates.get(0).getOwner().isEmpty());//check owner was pulled
            assertFalse(filteredTemplates.get(0).getIDate().toString().isEmpty());//check idate was pulled

        } finally {
            if (template != null) {
                templateAPI.delete(template, user, false);
            }
            if (anotherTemplate != null) {
                templateAPI.delete(anotherTemplate, user, false);
            }
        }
    }

    /**
     * Method to test: {@link TemplateAPI#find(String, User, boolean)}
     * Given Scenario: find a template by inode
     * ExpectedResult: the template of the specific inode
     */
    @Test
    public void test_find_success() throws Exception{
        final String title = "testFindTemplate_" + System.currentTimeMillis();
        final Template template = new TemplateDataGen().title(title).nextPersisted();

        //Remove template cache so it's has to search in the DB
        CacheLocator.getTemplateCache().clearCache();

        final Template templateFound = templateAPI.find(template.getInode(),user,false);

        assertEquals(title,templateFound.getTitle());
        assertFalse(templateFound.getOwner().isEmpty());//check owner was pulled
        assertFalse(templateFound.getIDate().toString().isEmpty());//check idate was pulled
    }

    /**
     * Method to test: {@link TemplateAPI#find(String, User, boolean)}
     * Given Scenario: tries to find a template, but the inode does not exists.
     * ExpectedResult: null
     */
    @Test
    public void test_find_inode_not_exist_return_Null() throws Exception{
       final Template templateFound = templateAPI.find(UUIDGenerator.generateUuid(),user,false);
       assertNull(templateFound);
    }

    /**
     * Method to test: {@link TemplateAPI#findAllVersions(Identifier, User, boolean)}
     * Given Scenario: brings all the versions of a template, using the identifier as reference
     * ExpectedResult: list of templates, the size must be 3 since its the amount of versions
     */
    @Test
    public void test_findAllVersions_success() throws Exception {
        final String title = "testFindTemplate_" + System.currentTimeMillis();
        Template template = new TemplateDataGen().title(title).nextPersisted();
        template.setTitle(title + "_1");
        template.setInode("");
        template = TemplateDataGen.save(template);
        template.setTitle(title + "_2");
        template.setInode("");
        template = TemplateDataGen.save(template);

        final Identifier identifier = APILocator.getIdentifierAPI().find(template.getIdentifier());
        final List<Template> templateAllVersions = templateAPI.findAllVersions(identifier,user,false);
        assertNotNull(templateAllVersions);
        assertEquals(3,templateAllVersions.size());
        assertFalse(templateAllVersions.get(0).getOwner().isEmpty());//check owner was pulled
        assertFalse(templateAllVersions.get(0).getIDate().toString().isEmpty());//check idate was pulled
    }

    /**
     * Method to test: {@link TemplateAPI#findAllVersions(Identifier, User, boolean)}
     * Given Scenario: brings all the versions of a template, using the identifier as reference,
     * since the identifier does not belong to any template, the list must be empty
     * ExpectedResult: empty list of templates
     */
    @Test
    public void test_findAllVersions_IdentifierNotBelongToTemplate_returnEmptyList() throws Exception {
        final Identifier identifier = new Identifier();
        identifier.setId(UUIDGenerator.generateUuid());
        final List<Template> templateAllVersions = templateAPI.findAllVersions(identifier,user,false);
        assertTrue(templateAllVersions.isEmpty());
    }

    /**
     * Method to test: {@link TemplateAPI#saveTemplate(Template, Host, User, boolean)}
     * Given Scenario: Saves a new template and checks that the owner and create_date
     * is being save on the identifier table
     * ExpectedResult: owner and create_date columns on the identifier tables must have values
     */
    @Test
    public void test_newTemplate_checkIfSavesOwnerAndCreateDateOnIdentifier() throws Exception {
        final String title = "testTemplate_" + System.currentTimeMillis();
        final Template template = new TemplateDataGen().title(title).nextPersisted();
        final Identifier identifier = APILocator.getIdentifierAPI().find(template.getIdentifier());
        assertNotNull(identifier);
        assertNotNull(identifier.getOwner());
        assertNotNull(identifier.getCreateDate());
        assertFalse(identifier.getOwner().isEmpty());
        assertFalse(identifier.getCreateDate().toString().isEmpty());
    }

}
