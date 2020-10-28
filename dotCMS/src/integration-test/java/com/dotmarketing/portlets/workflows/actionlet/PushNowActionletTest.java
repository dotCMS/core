package com.dotmarketing.portlets.workflows.actionlet;

import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.contenttype.model.field.DataTypes;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.FieldBuilder;
import com.dotcms.contenttype.model.field.TextField;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.ContentTypeBuilder;
import com.dotcms.contenttype.transform.contenttype.StructureTransformer;
import com.dotcms.datagen.TestUserUtils;
import com.dotcms.publisher.bundle.bean.Bundle;
import com.dotcms.publisher.environment.bean.Environment;
import com.dotcms.publisher.environment.business.EnvironmentAPI;
import com.dotcms.publishing.FilterDescriptor;
import com.dotcms.publishing.PublisherAPIImpl;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Role;
import com.dotmarketing.exception.AlreadyExistException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.IndexPolicy;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.workflows.business.BaseWorkflowIntegrationTest;
import com.dotmarketing.portlets.workflows.business.WorkflowAPI;
import com.dotmarketing.portlets.workflows.model.MultiKeyValue;
import com.dotmarketing.portlets.workflows.model.MultiSelectionWorkflowActionletParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowActionClass;
import com.dotmarketing.portlets.workflows.model.WorkflowActionletParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowProcessor;
import com.liferay.portal.model.User;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.validation.constraints.AssertTrue;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class PushNowActionletTest extends BaseWorkflowIntegrationTest {

    private static CreateSchemeStepActionResult schemeStepActionResult = null;
    private static WorkflowAPI workflowAPI = null;
    private static ContentletAPI contentletAPI = null;
    private static ContentTypeAPI contentTypeAPI = null;
    private static ContentType type = null;
    private static User adminUser;

    @BeforeClass
    public static void prepare() throws Exception {
        //Setting web app environment

        IntegrationTestInitService.getInstance().init();
        workflowAPI = APILocator.getWorkflowAPI();
        contentTypeAPI = APILocator.getContentTypeAPI(APILocator.systemUser());
        contentletAPI = APILocator.getContentletAPI();
        final long currentTime = System.currentTimeMillis();
        adminUser = TestUserUtils.getUser(TestUserUtils.getOrCreateAdminRole(),"testPushNow@test.com"+currentTime,"testPushNowActionlet"+currentTime,"testPushNowActionlet"+currentTime,"testPushNowActionlet"+currentTime);

    }

    private void createWorkflowWithPushNowActionlet(final String environmentNameParam, final String filterKeyParam)
            throws DotSecurityException, AlreadyExistException, DotDataException, InstantiationException, IllegalAccessException {
        // Create the scheme and actions. This method allows you to add just one sub-action
        final long sysTime = System.currentTimeMillis();
        schemeStepActionResult = createSchemeStepActionActionlet
                ("itPushNowScheme_" + sysTime, "step1", "action1",
                        CheckinContentActionlet.class);
        // Add the Push Now sub-action for this test
        addActionletToAction(schemeStepActionResult.getAction().getId(),
                PushNowActionlet.class, 1);
        // Add the required parameters of the sub-action
        final List<WorkflowActionClass> actionletClasses = getActionletsFromAction(
                schemeStepActionResult.getAction());
        WorkflowActionClass workflowActionClass = actionletClasses.get(1);
        addParameterValuesToActionlet(workflowActionClass,
                Arrays.asList(environmentNameParam,filterKeyParam));
        // Set the role ID of the people who can use the action
        addWhoCanUseToAction(schemeStepActionResult.getAction(),
                Collections.singletonList(TestUserUtils.getOrCreateAdminRole().getId()));
        // Associate the scheme to the content type
        workflowAPI.saveSchemesForStruct(new StructureTransformer(type).asStructure(),
                Collections.singletonList(schemeStepActionResult.getScheme()));
    }

    private static void createTestContentType()
            throws DotDataException, DotSecurityException {
        type = contentTypeAPI.save(
                ContentTypeBuilder.builder(BaseContentType.CONTENT.immutableClass())
                        .folder(FolderAPI.SYSTEM_FOLDER).host(Host.SYSTEM_HOST)
                        .description("Content Type for testing the Push Now actionlet.")
                        .name("PushNowActionletTest"+System.currentTimeMillis())
                        .variable("PushNowActionletTest"+System.currentTimeMillis()).build());
        final List<Field> fields = new ArrayList<>(type.fields());
        fields.add(FieldBuilder.builder(TextField.class).name("title").variable("title")
                .contentTypeId(type.id()).dataType(DataTypes.TEXT).indexed(true).build());
        fields.add(FieldBuilder.builder(TextField.class).name("txt").variable("txt")
                .contentTypeId(type.id()).dataType(DataTypes.TEXT).indexed(true).build());
        type = contentTypeAPI.save(type, fields);

        //Give Permissions to the Content Type
        final Permission permission = new Permission(type.id(),TestUserUtils.getOrCreateAdminRole().getId(),PermissionAPI.PERMISSION_WRITE);
        APILocator.getPermissionAPI().save(permission,type,APILocator.systemUser(),false);
    }

    private static void createFilterDescriptor(final String key, final boolean defaultFilter){
        final Map<String,Object> filtersMap = new HashMap<>();
        APILocator.getPublisherAPI().addFilterDescriptor(new FilterDescriptor(key,key,filtersMap,defaultFilter,"DOTCMS_BACK_END_USER"));
    }

    private static Environment createEnvironment (final String name) throws DotDataException, DotSecurityException {

        final EnvironmentAPI environmentAPI = APILocator.getEnvironmentAPI();
        final Environment environment = new Environment();
        final List<Permission> permissions = new ArrayList<>();
        permissions.add(new Permission(environment.getId(),
                APILocator.getRoleAPI().loadRoleByKey(adminUser.getUserId()).getId(),
                PermissionAPI.PERMISSION_USE));

        environment.setName(name);
        environment.setPushToAll(false);
        environmentAPI.saveEnvironment(environment, permissions);

        return environment;
    }

    /**
     * This test is for checking if the Push Now Actionlet creates the Bundle with the filter selected.
     * When the Workflow with the sub-action is invoked it creates a Bundle to PP the contentlet,
     * that Bundle that is created must have the selected filter as filter Key.
     */
    @Test
    public void test_PushNowActionlet_BundleUsesFilterKeySet()
            throws DotSecurityException, DotDataException, IllegalAccessException, AlreadyExistException, InstantiationException {
        //Create Content Type
        createTestContentType();
        //Create Filter
        final String defaultFilterKey = "testDefaultFilterKey.yml"+System.currentTimeMillis();
        createFilterDescriptor(defaultFilterKey,true);
        final String filterKey = "testFilterKey.yml"+System.currentTimeMillis();
        createFilterDescriptor(filterKey,false);
        //Create Environment
        final Environment environment = createEnvironment("TestEnvironment_" + System.currentTimeMillis());
        //Create Workflow and pass the env and the filterKey
        createWorkflowWithPushNowActionlet(environment.getName(),filterKey);
        //Create Contentlet
        final Contentlet cont = new Contentlet();
        cont.setContentTypeId(type.id());
        cont.setOwner(APILocator.systemUser().toString());
        cont.setModDate(new Date());
        cont.setLanguageId(1);
        cont.setStringProperty("title", "Test Save");
        cont.setStringProperty("txt", "Test Save Text");
        cont.setIndexPolicy(IndexPolicy.WAIT_FOR);
        cont.setBoolProperty(Contentlet.IS_TEST_MODE, true);
        Contentlet contentlet1 = contentletAPI.checkin(cont, adminUser, false);

        // Set the appropriate workflow action to the contentlet
        contentlet1.setActionId(
                schemeStepActionResult.getAction().getId());

        // Triggering the actionlets
        WorkflowProcessor processor =
                workflowAPI.fireWorkflowPreCheckin(contentlet1, adminUser);
        workflowAPI.fireWorkflowPostCheckin(processor);

        final Bundle bundleCreated = FactoryLocator.getBundleFactory().findSentBundles(adminUser.getUserId(),1,0).stream().findFirst().get();
        Assert.assertEquals(filterKey,bundleCreated.getFilterKey());

    }

    /**
     * This test is for the getParameters method.
     * In the Filter Param should come all the possible filters that the user have to select,
     * and the defaultValue should be a filter set as default = true.
     */
    @Test
    public void test_PushNowActionlet_getParameters_getFilters()
            throws DotSecurityException, DotDataException {
        //Create environment
        final String environmentName = "TestEnvironment_1_" + System.currentTimeMillis();
        createEnvironment(environmentName);

        //Clean all filters
        PublisherAPIImpl.class.cast(APILocator.getPublisherAPI()).getFilterDescriptorMap().clear();
        //Create 3 filters, one set as default
        final String defaulFilterKey = "defaultFilterKey"+System.currentTimeMillis();
        final String filterKey1 = "filterKey1"+System.currentTimeMillis();
        final String filterKey2 = "filterKey2"+System.currentTimeMillis();
        createFilterDescriptor(defaulFilterKey,true);
        createFilterDescriptor(filterKey1,false);
        createFilterDescriptor(filterKey2,false);

        //Get Params
        final PushNowActionlet pushNowActionlet = new PushNowActionlet();
        final List<WorkflowActionletParameter> parameters = pushNowActionlet.getParameters();
        Assert.assertFalse(parameters.isEmpty());
        Assert.assertEquals(defaulFilterKey,parameters.get(1).getDefaultValue());
        Assert.assertTrue(((MultiSelectionWorkflowActionletParameter) parameters.get(1)).getMultiValues().stream().anyMatch(multiKeyValue -> multiKeyValue.getKey().equalsIgnoreCase(defaulFilterKey)));
        Assert.assertTrue(((MultiSelectionWorkflowActionletParameter) parameters.get(1)).getMultiValues().stream().anyMatch(multiKeyValue -> multiKeyValue.getKey().equalsIgnoreCase(filterKey1)));
        Assert.assertTrue(((MultiSelectionWorkflowActionletParameter) parameters.get(1)).getMultiValues().stream().anyMatch(multiKeyValue -> multiKeyValue.getKey().equalsIgnoreCase(filterKey2)));
    }

}
