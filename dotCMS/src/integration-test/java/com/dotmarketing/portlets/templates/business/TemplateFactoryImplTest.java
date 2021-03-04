package com.dotmarketing.portlets.templates.business;


import com.dotcms.datagen.SiteDataGen;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import java.util.List;
import java.util.Map;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import com.dotcms.IntegrationTestBase;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.common.util.SQLUtilTest;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.UUIDGenerator;
import com.google.common.collect.ImmutableMap;
import com.liferay.portal.model.User;

public class TemplateFactoryImplTest extends IntegrationTestBase {

    private static HostAPI hostAPI;
    private static TemplateAPI templateAPI;
    private static User user;
    private static UserAPI userAPI;
    private static Host host;

    @BeforeClass
    public static void prepare() throws Exception {

        // Setting web app environment
        IntegrationTestInitService.getInstance().init();


        hostAPI = APILocator.getHostAPI();
        templateAPI = APILocator.getTemplateAPI();
        userAPI = APILocator.getUserAPI();
        user = userAPI.getSystemUser();

        host = hostAPI.findDefaultHost(user, false);
    }


    @Test
    public void testFindWorkingTemplateByName_SQL_INJECTIION() throws Exception {
        Template template = new Template();
        final TemplateFactory templateFactory = new TemplateFactoryImpl();
        template.setTitle(UUIDGenerator.generateUuid() + SQLUtilTest.MALICIOUS_SQL_CONDITION );
        template.setBody("<html><body> I'm mostly empty </body></html>");
        template.setOwner("template's owner");

        template = templateAPI.saveTemplate(template, host, user, false);

        final Template result = templateFactory.findWorkingTemplateByName(template.getTitle(), host);

        
        assert(result.getIdentifier().equals(template.getIdentifier()));

    }


    @Test
    public void test_find_templates_by_name_uses_parameterized_queries() throws Exception {
        Template template = null;
        Template anotherTemplate = null;

        template = new Template();
        final String uniqueString = UUIDGenerator.generateUuid() + SQLUtilTest.MALICIOUS_SQL_CONDITION;
        final String uniqueTitle = uniqueString + " This one will not show up";
        template.setTitle(uniqueTitle);
        template.setBody("<html><body> Empty Template </body></html>");
        templateAPI.saveTemplate(template, host, user, false);

        anotherTemplate = new Template();
        anotherTemplate.setTitle("I am not invited");
        anotherTemplate.setBody("<html><body> Empty Template </body></html>");
        templateAPI.saveTemplate(anotherTemplate, host, user, false);


        final TemplateFactory templateFactory = new TemplateFactoryImpl();

        List<Template> templates = templateFactory.
                findTemplates(user, false, ImmutableMap.of("filter", uniqueString), host.getIdentifier(), null, null, null, 0, 10, null);

        assert templates.size() ==1;
        
        template = new Template();
        template.setTitle(uniqueTitle);
        template.setBody("<html><body> Empty Template </body></html>");
        templateAPI.saveTemplate(template, host, user, false);

        anotherTemplate = new Template();
        anotherTemplate.setTitle("I am not invited");
        anotherTemplate.setBody("<html><body> Empty Template </body></html>");
        templateAPI.saveTemplate(anotherTemplate, host, user, false);
        
        
        templates = templateFactory.findTemplates(user, false, ImmutableMap.of("filter", uniqueString), host.getIdentifier(), null, null, null, 0, 10, SQLUtilTest.MALICIOUS_SQL_ORDER_BY);

        assert templates.size() ==2;

    }

    /**
     * Method to test: {@link TemplateFactoryImpl#findTemplates(User, boolean, Map, String, String, String, String, int, int, String)} ()}
     * Given Scenario: Try to get all the templates of a specific host, no other param is set.
     * ExpectedResult: empty list since there are no templates for that host
     *
     */
    @Test
    public void test_findTemplates_usingHostId_hostWithNoTemplates_returnEmptyList()
            throws DotDataException, DotSecurityException {
        final Host newHost = new SiteDataGen().nextPersisted();

        final TemplateFactory templateFactory = new TemplateFactoryImpl();

        final List<Template> templates = templateFactory.
                findTemplates(user, false, null, newHost.getIdentifier(), null, null, null, 0, 10, "title");

        Assert.assertTrue(templates.isEmpty());
    }


}
