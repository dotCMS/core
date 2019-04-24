package com.dotmarketing.business.portal;

import static org.mockito.Mockito.when;

import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Config;
import com.liferay.portal.SystemException;
import com.liferay.portal.ejb.PortletManagerFactory;
import com.liferay.portal.language.LanguageException;
import com.liferay.portal.model.Portlet;
import com.liferay.portal.model.User;
import com.liferay.portal.struts.MultiMessageResources;
import com.liferay.portal.struts.MultiMessageResourcesFactory;
import com.liferay.portlet.StrutsPortlet;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.apache.struts.Globals;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(DataProviderRunner.class)
public class PortletAPIImplTest {

    private static PortletAPI portletApi;
    private static User systemUser;

    @BeforeClass
    public static void prepare() throws Exception {
        // Setting web app environment
        IntegrationTestInitService.getInstance().init();
        portletApi = APILocator.getPortletAPI();
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
            portlet = createCustomPortlet(testCase.portletName, testCase.porltedId, testCase.baseTypes, testCase.contentTypes);
            Assert.assertTrue(testCase.createdSuccessfully);
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
        String porltedId, portletName, baseTypes, contentTypes;
        boolean createdSuccessfully;

        testCaseCreateCustomPortlet(final String porltedId, final String portletName, final String baseTypes, final String contentTypes, final boolean createdSuccessfully){
            this.porltedId = porltedId;
            this.portletName = portletName;
            this.baseTypes = baseTypes;
            this.contentTypes = contentTypes;
            this.createdSuccessfully = createdSuccessfully;
        }
    }

    @DataProvider
    public static Object[] testCasesCreateCustomPortlet(){
        return new Object[] {
                new testCaseCreateCustomPortlet("testCustomPortlet","testCustomPortlet","Persona","",true),
                new testCaseCreateCustomPortlet("testCustomPortlet","testCustomPortlet","","news",true),
                new testCaseCreateCustomPortlet("","testCustomPortlet","Persona","",false),
                new testCaseCreateCustomPortlet("testCustomPortlet","","Persona","",false),
                new testCaseCreateCustomPortlet("testCustomPortlet","testCustomPortlet","","",false),
                new testCaseCreateCustomPortlet("testCustomPortlet","testCustomPortlet","NoExist","",false),
                new testCaseCreateCustomPortlet("testCustomPortlet","testCustomPortlet","","NoExist",false),
        };
    }

    @Test
    public void test_findPortlet() throws LanguageException, DotDataException {
        Portlet portlet = null;
        try {
            portlet = createCustomPortlet("testCustomPortlet", "testCustomPortlet", "Persona", "");
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
    public void test_findAllPortlets() throws LanguageException, DotDataException, SystemException {
        Portlet portlet = null;
        try {
            Collection<Portlet> portlets =  portletApi.findAllPortlets();
            portlet = createCustomPortlet("testCustomPortlet", "testCustomPortlet", "Persona", "");
            Assert.assertEquals(portlets.size()+1,portletApi.findAllPortlets().size());
        }finally {
            if(portlet!=null){
                portletApi.deletePortlet(portlet.getPortletId());
            }
        }
    }
}
