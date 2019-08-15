package com.dotmarketing.business.portal;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.repackage.org.apache.struts.Globals;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Config;
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
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

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

    private Portlet createCustomPortlet(final String name, final String portletId, final String baseTypes, final String contentTypes)
            throws LanguageException, DotDataException {

        final Map<String, String> initValues = new HashMap<>();

        initValues.put("view-action","/ext/contentlet/view_contentlets");
        initValues.put("name", name);
        initValues.put("baseTypes", baseTypes);
        initValues.put("contentTypes", contentTypes);

        final Portlet newPortlet = portletApi.savePortlet(new DotPortlet(portletId, StrutsPortlet.class.getName(), initValues),systemUser);

        return newPortlet;
    }

    @Test
    @UseDataProvider("testCasesCreateCustomPortlet")
    public void test_createCustomPortlet(final testCaseCreateCustomPortlet testCase)
            throws LanguageException {
        Portlet portlet = null;
        try {
            portlet = createCustomPortlet(testCase.portletName, testCase.portletId, testCase.baseTypes, testCase.contentTypes);
            assertTrue(testCase.createdSuccessfully);
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
            Assert.assertFalse(testCase.createdSuccessfully);
            return;
        }finally {
            if(portlet!=null){
                portletApi.deletePortlet(portlet.getPortletId());
            }
        }

    }


    private static class testCaseCreateCustomPortlet{
        String portletId, portletName, baseTypes, contentTypes;
        boolean createdSuccessfully;

        testCaseCreateCustomPortlet(final String portletId, final String portletName,
                final String baseTypes, final String contentTypes,
                final boolean createdSuccessfully) {
            this.portletId = portletId;
            this.portletName = portletName;
            this.baseTypes = baseTypes;
            this.contentTypes = contentTypes;
            this.createdSuccessfully = createdSuccessfully;
        }
    }

    @DataProvider
    public static Object[] testCasesCreateCustomPortlet() throws Exception {

        // Setting web app environment
        IntegrationTestInitService.getInstance().init();

        final ContentType contentType = new ContentTypeDataGen().nextPersisted();
        final ContentType contentType1 = new ContentTypeDataGen().nextPersisted();

        return new Object[]{
                new testCaseCreateCustomPortlet(PORTLET_ID, PORTLET_ID, "Persona",
                        "", true),
                new testCaseCreateCustomPortlet(PORTLET_ID, PORTLET_ID,
                        "Content, Persona", "", true),
                new testCaseCreateCustomPortlet(PORTLET_ID, PORTLET_ID, "",
                        contentType.variable(), true),
                new testCaseCreateCustomPortlet(PORTLET_ID, PORTLET_ID, "",
                        contentType.variable() + "," + contentType1.variable(), true),
                new testCaseCreateCustomPortlet("", PORTLET_ID, "Persona", "", false),
                new testCaseCreateCustomPortlet(PORTLET_ID, "", "Persona", "", false),
                new testCaseCreateCustomPortlet(PORTLET_ID, PORTLET_ID, "", "",
                        false),
                new testCaseCreateCustomPortlet(PORTLET_ID, PORTLET_ID, "NoExist",
                        "", false),
                new testCaseCreateCustomPortlet(PORTLET_ID, PORTLET_ID, "",
                        "NoExist", false),
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
