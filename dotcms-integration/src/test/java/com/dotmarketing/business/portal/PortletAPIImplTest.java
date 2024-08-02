package com.dotmarketing.business.portal;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;

import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.repackage.org.apache.struts.Globals;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.liferay.portal.SystemException;
import com.liferay.portal.language.LanguageException;
import com.liferay.portal.model.Portlet;
import com.liferay.portal.model.User;
import com.liferay.portal.struts.MultiMessageResources;
import com.liferay.portal.struts.MultiMessageResourcesFactory;
import com.liferay.portlet.StrutsPortlet;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import javax.xml.parsers.ParserConfigurationException;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.xml.sax.SAXException;

@RunWith(DataProviderRunner.class)
public class PortletAPIImplTest {

    private static final String PORTLET_ID = "testCustomPortlet";
    private static PortletAPI portletApi;
    private static User systemUser;

    @BeforeClass
    public static void prepare() throws Exception {
        // Setting web app environment
        IntegrationTestInitService.getInstance().init();

        //portletApi = APILocator.getPortletAPI();
        final String[] portletXMLFilesnew = {
                PortletAPIImplTest.class.getResource("/WEB-INF/portlet.xml").getPath(),
                PortletAPIImplTest.class.getResource("/WEB-INF/portlet-ext.xml").getPath()};
        portletApi = new PortletAPIImpl(new PortletFactoryImpl(portletXMLFilesnew), Config.CONTEXT);
        systemUser = APILocator.systemUser();
        when(Config.CONTEXT.getAttribute(Globals.MESSAGES_KEY))
                .thenReturn(new MultiMessageResources(MultiMessageResourcesFactory.createFactory(),""));

    }

    private Portlet createCustomPortlet(final String name, final String portletId, final String baseTypes, final String contentTypes) throws LanguageException, DotDataException {
        return createCustomPortlet(name, portletId, baseTypes, contentTypes, "list");
    }

    private Portlet createCustomPortlet(final String name, final String portletId, final String baseTypes, final String contentTypes, final String dataViewMode)
            throws LanguageException, DotDataException {


        final DotPortlet newPortlet = DotPortlet.builder()
                .portletId(portletId)
                .portletClass(StrutsPortlet.class.getName())
                .putInitParam("view-action", "/ext/contentlet/view_contentlets")
                .putInitParam("name", name)
                .putInitParam("baseTypes", baseTypes)
                .putInitParam("contentTypes", contentTypes)
                .putInitParam("dataViewMode", dataViewMode)
                .build();

        return portletApi.savePortlet(newPortlet.toPortlet(), systemUser);
    }

    /**
     * Test case to ensure no portlets are returned from the mixed-portlets-test.xml file
     * It should handle without error  but ignore portlet elements within the portlets element
     * that do not contain a portlet-name and portlet-class
     */
    @Test
    public void test_portletsFromMixedPortletXml() throws Exception {
        String resourcePath = "/com/dotmarketing/business/portal/mixed-portlets-test.xml";
        try (InputStream stream = getClass().getResourceAsStream(resourcePath)) {
            assertNotNull("Resource not found: " + resourcePath, stream);
            Map<String, Portlet> portlets = new PortletFactoryImpl().xmlToPortlets(stream);
            assertEquals("Expecting exactly 1 valid portlet", 1, portlets.size());
        } catch (IOException | ParserConfigurationException | SAXException e) {
            Logger.error(this, "Error loading portlets from liferay-portlet.xml", e);
            Assert.fail("Exception occurred: " + e.getMessage());
        }
    }

    /**
     * Method to test: Use following method to create a custom portlet {@link PortletAPI#savePortlet(Portlet portlet,User user)}
     * Given Scenario: The user should be able to create a custom portlet.
     * Some of the fields are mandatory and some are optional.
     * ExpectedResult: Must create the new custom portlet
     */
    @Test
    @UseDataProvider("testCasesCreateCustomPortlet")
    public void test_createCustomPortlet(final testCaseCreateCustomPortlet testCase)
            throws LanguageException {
        Portlet portlet = null;
        try {
            portlet = createCustomPortlet(testCase.portletName, testCase.portletId, testCase.baseTypes, testCase.contentTypes, testCase.dataViewMode);
            assertTrue(testCase.createdSuccessfully);
            assertTrue("Expected: "+portlet.getPortletId()+ " provided:"+ testCase.portletId,
                    portlet.getPortletId().startsWith(PortletAPI.CONTENT_PORTLET_PREFIX)
                            && portlet.getPortletId().contains(PORTLET_ID));

            if (testCase.baseTypes != null) {
                final List<String> returnedBaseTypes = Arrays
                        .asList(portlet.getInitParams().get("baseTypes").toLowerCase().split(","));
                assertTrue(Arrays.asList(testCase.baseTypes.toLowerCase().split(",")).stream().allMatch(
                        baseType -> returnedBaseTypes.contains(baseType.trim())));
            }

            if (testCase.contentTypes != null){
                final List<String> returnedContentTypes = Arrays
                        .asList(portlet.getInitParams().get("contentTypes").toLowerCase().split(","));
                assertTrue(Arrays.asList(testCase.contentTypes.toLowerCase().split(",")).stream().allMatch(
                        contentType -> returnedContentTypes.contains(contentType.trim())));
            }
        }catch (DotDataException | IllegalArgumentException e){
            if (testCase.createdSuccessfully) {
                Logger.error(PortletAPIImplTest.class, "Did not expect Exception for test", e);
            } else {
                Logger.info(PortletAPIImplTest.class,"Expected Exception found: " + e.getMessage());
            }
            Assert.assertFalse(e.getMessage(),testCase.createdSuccessfully);
            return;
        }finally {
            if(portlet!=null){
                portletApi.deletePortlet(portlet.getPortletId());
            }
        }

    }

    /**
     * Method to test: Use following method to update an existing custom portlet {@link PortletAPI#savePortlet(Portlet portlet,User user)}
     * Given Scenario:
     * 1. id should not be editable
     * 2. User can edit all the resting fields:
        * Add/Remove Base types or content types
        * Mode View Type: Card or List
     * ExpectedResult: Must update the custom portlet
     */
    @Test
    @UseDataProvider("testCasesCreateCustomPortlet")
    public void test_updateCustomPortlet(final testCaseCreateCustomPortlet testCase)
            throws LanguageException {
        Portlet updatedPortlet = null;
        try {
            //create
            final Portlet initialPortlet = createCustomPortlet(PORTLET_ID, PORTLET_ID, "CONTENT", "webPageContent", "card");
            assertNotNull(initialPortlet);
            //update
            updatedPortlet = createCustomPortlet(testCase.portletName, testCase.portletId, testCase.baseTypes, testCase.contentTypes, testCase.dataViewMode);
            assertTrue(testCase.createdSuccessfully);
            assertNotNull(updatedPortlet);
            assertTrue("Expected: "+updatedPortlet.getPortletId()+ " provided:"+ testCase.portletId,
                    updatedPortlet.getPortletId().startsWith(PortletAPI.CONTENT_PORTLET_PREFIX)
                    && updatedPortlet.getPortletId().contains(PORTLET_ID));
            //group id should not be updated
            assertEquals(updatedPortlet.getGroupId(), initialPortlet.getGroupId());


            if (testCase.baseTypes != null) {
                final List<String> returnedBaseTypes = Arrays
                        .asList(updatedPortlet.getInitParams().get("baseTypes").toLowerCase().split(","));
                assertTrue(Arrays.asList(testCase.baseTypes.toLowerCase().split(",")).stream().allMatch(
                        baseType -> returnedBaseTypes.contains(baseType.trim())));
            }

            if (testCase.contentTypes != null){
                final List<String> returnedContentTypes = Arrays
                        .asList(updatedPortlet.getInitParams().get("contentTypes").toLowerCase().split(","));
                assertTrue(Arrays.asList(testCase.contentTypes.toLowerCase().split(",")).stream().allMatch(
                        contentType -> returnedContentTypes.contains(contentType.trim())));
            }

        }catch (DotDataException | IllegalArgumentException e){
            Assert.assertFalse(testCase.createdSuccessfully);
            return;
        }finally {
            if(updatedPortlet!=null){
                portletApi.deletePortlet(updatedPortlet.getPortletId());
            }
        }

    }


    private static class testCaseCreateCustomPortlet{
        String portletId, portletName, baseTypes, contentTypes, dataViewMode;
        boolean createdSuccessfully;

        testCaseCreateCustomPortlet(final String portletId, final String portletName,
                final String baseTypes, final String contentTypes,
                final boolean createdSuccessfully, final String dataViewMode) {
            this.portletId = portletId;
            this.portletName = portletName;
            this.baseTypes = baseTypes;
            this.contentTypes = contentTypes;
            this.createdSuccessfully = createdSuccessfully;
            this.dataViewMode = dataViewMode;
        }

        testCaseCreateCustomPortlet(final String portletId, final String portletName,
                                    final String baseTypes, final String contentTypes,
                                    final boolean createdSuccessfully) {
            this.portletId = portletId;
            this.portletName = portletName;
            this.baseTypes = baseTypes;
            this.contentTypes = contentTypes;
            this.createdSuccessfully = createdSuccessfully;
            this.dataViewMode = "list";
        }
    }

    @DataProvider
    public static Object[] testCasesCreateCustomPortlet() throws Exception {

        // Setting web app environment
        IntegrationTestInitService.getInstance().init();

        final ContentType contentType = new ContentTypeDataGen().nextPersisted();
        final ContentType contentType1 = new ContentTypeDataGen().nextPersisted();

        return new Object[]{
                //working cases
                new testCaseCreateCustomPortlet(PORTLET_ID, PORTLET_ID, "Persona",
                        "", true),
                new testCaseCreateCustomPortlet("c_"+PORTLET_ID, PORTLET_ID, "Persona",
                        "", true, "card"),
                new testCaseCreateCustomPortlet("C_"+PORTLET_ID, PORTLET_ID, "Persona",
                        "", true),
                new testCaseCreateCustomPortlet(PORTLET_ID, PORTLET_ID,
                        "Content, Persona", "", true, "card"),
                new testCaseCreateCustomPortlet(PORTLET_ID, PORTLET_ID, "",
                        contentType.variable(), true, "card"),
                new testCaseCreateCustomPortlet(PORTLET_ID, PORTLET_ID, "",
                        contentType.variable() + "," + contentType1.variable(), true),
                //failing cases
                new testCaseCreateCustomPortlet("", PORTLET_ID, "Persona", "", false),
                new testCaseCreateCustomPortlet(PORTLET_ID, "", "Persona", "", false),
                new testCaseCreateCustomPortlet(PORTLET_ID, PORTLET_ID, "", "",
                        false),
                new testCaseCreateCustomPortlet(PORTLET_ID, PORTLET_ID, "NoExist",
                        "", false),
                new testCaseCreateCustomPortlet(PORTLET_ID, PORTLET_ID, "",
                        "NoExist", false),
                new testCaseCreateCustomPortlet(PORTLET_ID, PORTLET_ID, "Persona", "", false,""),
        };
    }

    @Test
    public void test_findPortlet() throws LanguageException, DotDataException {
        Portlet portlet = null;
        try {
            portlet = createCustomPortlet(PORTLET_ID, PORTLET_ID, "Persona", "");
            final Portlet findPortlet = portletApi.findPortlet(portlet.getPortletId());
            Assert.assertEquals(portlet.getInitParams(),findPortlet.getInitParams());
            Assert.assertEquals(portlet.getPortletId(),findPortlet.getPortletId());
        }finally {
            if(portlet!=null){
                portletApi.deletePortlet(portlet.getPortletId());
            }
        }
    }
    
    @Test
    public void test_delete_portlet() throws LanguageException, DotDataException {
        Portlet portlet = null;

            final String id =PORTLET_ID + System.currentTimeMillis();
            portlet = createCustomPortlet(id, id, "Persona", "");
            final Portlet findPortlet = portletApi.findPortlet(portlet.getPortletId());
            Assert.assertEquals(portlet.getInitParams(),findPortlet.getInitParams());
            Assert.assertEquals(portlet.getPortletId(),findPortlet.getPortletId());
            portletApi.deletePortlet(portlet.getPortletId());
            final Portlet findPortletAgain = portletApi.findPortlet(portlet.getPortletId());
            Assert.assertNull(findPortletAgain);
            
            

    }
    
    @Test
    public void test_findAllPortlets() throws LanguageException, DotDataException, SystemException {
        Portlet portlet = null;
        try {
            Collection<Portlet> portlets =  portletApi.findAllPortlets();
            portlet = createCustomPortlet(PORTLET_ID, PORTLET_ID, "Persona", "");
            Assert.assertEquals(portlets.size()+1,portletApi.findAllPortlets().size());
        }finally {
            if(portlet!=null){
                portletApi.deletePortlet(portlet.getPortletId());
            }
        }
    }
}
