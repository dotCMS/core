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
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.RoleAPI;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.exception.AlreadyExistException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.workflows.business.BaseWorkflowIntegrationTest;
import com.dotmarketing.portlets.workflows.business.WorkflowAPI;
import com.dotmarketing.portlets.workflows.model.WorkflowActionClass;
import com.dotmarketing.portlets.workflows.model.WorkflowActionClassParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowActionletParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowProcessor;
import com.liferay.portal.model.User;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author Jose Castro
 * @version 4.3.0
 * @since Jan 10, 2018
 */
public class FourEyeApproverActionletTest extends BaseWorkflowIntegrationTest {

    private static RoleAPI roleAPI;
    private static WorkflowAPI workflowAPI;
    private static ContentletAPI contentletAPI;
    private static LanguageAPI languageAPI;
    private static ContentTypeAPI contentTypeAPI;
    private static UserAPI userAPI;

    private static Role publisherRole;
    private static User systemUser;

    private static CreateSchemeStepActionResult schemeStepActionResult = null;
    private static ContentType type = null;
    private static Contentlet contentlet = null;

    @BeforeClass
    public static void prepare() throws Exception {
        // Setting up the web app environment
        IntegrationTestInitService.getInstance().init();

        systemUser = APILocator.systemUser();
        roleAPI = APILocator.getRoleAPI();
        publisherRole = roleAPI.findRoleByName("Publisher / Legal", null);

        workflowAPI = APILocator.getWorkflowAPI();
        contentTypeAPI = APILocator.getContentTypeAPI(systemUser);
        contentletAPI = APILocator.getContentletAPI();
        languageAPI = APILocator.getLanguageAPI();
        userAPI = APILocator.getUserAPI();

        final long sysTime = System.currentTimeMillis();

        // creates the scheme and actions
        schemeStepActionResult = createSchemeStepActionActionlet
                ("itFourEyeApprovalScheme_" + sysTime, "step1", "action1",
                        CheckinContentActionlet.class);

        final List<Permission> permissions = new ArrayList<>();
        Permission permission = new Permission(schemeStepActionResult.getAction().getId(), publisherRole.getId(), PermissionAPI.PERMISSION_USE);
        permissions.add(permission);
        workflowAPI.saveAction(schemeStepActionResult.getAction(), permissions);

        WorkflowActionClass wac = new WorkflowActionClass();
        wac.setActionId(schemeStepActionResult.getAction().getId());
        wac.setClazz(FourEyeApproverActionlet.class.getName());
        wac.setName(WorkFlowActionlet.class.cast(FourEyeApproverActionlet.class.newInstance()).getName());
        wac.setOrder(1);
        workflowAPI.saveActionClass(wac);

        wac = new WorkflowActionClass();
        wac.setActionId(schemeStepActionResult.getAction().getId());
        wac.setClazz(PublishContentActionlet.class.getName());
        wac.setName(WorkFlowActionlet.class.cast(PublishContentActionlet.class.newInstance()).getName());
        wac.setOrder(2);
        workflowAPI.saveActionClass(wac);

        List<WorkflowActionClass> actionClasses = workflowAPI
                .findActionClasses(schemeStepActionResult.getAction());

        wac = actionClasses.get(1);
        WorkFlowActionlet actionlet = workflowAPI.findActionlet(wac.getClazz());
        List<WorkflowActionletParameter> actionletParams = actionlet.getParameters();

        List<WorkflowActionClassParameter> newParams = new ArrayList<>();
        WorkflowActionClassParameter testParam = new WorkflowActionClassParameter();
        testParam.setActionClassId(wac.getId());
        testParam.setKey(actionletParams.get(0).getKey());
        testParam.setValue("chris@dotcms.com,daniel@dotcms.com");
        newParams.add(testParam);

        testParam = new WorkflowActionClassParameter();
        testParam.setActionClassId(wac.getId());
        testParam.setKey(actionletParams.get(1).getKey());
        testParam.setValue("2");
        newParams.add(testParam);

        testParam = new WorkflowActionClassParameter();
        testParam.setActionClassId(wac.getId());
        testParam.setKey(actionletParams.get(2).getKey());
        testParam.setValue("'4 Eye' Approval Required");
        newParams.add(testParam);

        testParam = new WorkflowActionClassParameter();
        testParam.setActionClassId(wac.getId());
        testParam.setKey(actionletParams.get(3).getKey());
        testParam.setValue("Please review this content.");
        newParams.add(testParam);

        workflowAPI.saveWorkflowActionClassParameters(newParams);

        //Map<String, WorkflowActionClassParameter> enteredParams = workflowAPI.findParamsForActionClass(wac);

        // creates the type to trigger the scheme
        createTestType();

        // associated the scheme to the type
        workflowAPI.saveSchemesForStruct(new StructureTransformer(type).asStructure(),
                Arrays.asList(schemeStepActionResult.getScheme()));
    }

    /**
     * @throws DotDataException
     * @throws DotSecurityException
     */
    private static void createTestType()
            throws DotDataException, DotSecurityException {

        type = contentTypeAPI.save(
                ContentTypeBuilder.builder(BaseContentType.CONTENT.immutableClass())
                        .expireDateVar(null).folder(FolderAPI.SYSTEM_FOLDER).host(Host.SYSTEM_HOST)
                        .description("SaveContentActionletTest...")
                        .name("SaveContentActionletTest").owner(APILocator.systemUser().toString())
                        .variable("SaveContentActionletTest").build());

        final List<Field> fields = new ArrayList<>(type.fields());

        fields.add(FieldBuilder.builder(TextField.class).name("title").variable("title")
                .contentTypeId(type.id()).dataType(DataTypes.TEXT).indexed(true).build());
        fields.add(FieldBuilder.builder(TextField.class).name("txt").variable("txt")
                .contentTypeId(type.id()).dataType(DataTypes.TEXT).indexed(true).build());

        type = contentTypeAPI.save(type, fields);
    }

    @Test
    public void runActionlet() throws DotSecurityException, DotDataException, InterruptedException {
        User publisher1 = userAPI.getUsersByNameOrEmailOrUserID("chris@dotcms.com", 0, 2).get(0);
        User publisher2 = userAPI.getUsersByNameOrEmailOrUserID("daniel@dotcms.com", 0, 2).get(0);
        final long languageId = languageAPI.getDefaultLanguage().getId();
        Contentlet cont = new Contentlet();
        cont.setContentTypeId(type.id());
        cont.setOwner(APILocator.systemUser().toString());
        cont.setModDate(new Date());
        cont.setLanguageId(languageId);
        cont.setStringProperty("title", "Test Save");
        cont.setStringProperty("txt", "Test Save Text");
        cont.setHost("48190c8c-42c4-46af-8d1a-0cd5db894797");
        cont.setFolder("b37bed19-b1fd-497d-be5e-f8cc33c3fb8d");

        // first save
        final Contentlet contentlet1 = contentletAPI.checkin(cont, systemUser, false);
        boolean isLive = false;

        // triggering the save content action
        contentlet1.setStringProperty(Contentlet.WORKFLOW_ACTION_KEY,
                schemeStepActionResult.getAction().getId());
        contentlet1.setStringProperty("title", "Test Save");
        contentlet1.setStringProperty("txt", "Test Save Text");

        WorkflowProcessor processor =
                workflowAPI.fireWorkflowPreCheckin(contentlet1, publisher1);
        workflowAPI.fireWorkflowPostCheckin(processor);

        Thread.sleep(2000);
        final Contentlet contentlet2 = contentletAPI
                .findContentletByIdentifier(contentlet1.getIdentifier(),
                        false, languageId, systemUser, false);
        isLive = contentlet2.isLive();

        processor = workflowAPI.fireWorkflowPreCheckin(contentlet2, publisher2);
        workflowAPI.fireWorkflowPostCheckin(processor);

        Thread.sleep(2000);
        Contentlet contentlet3 = contentletAPI
                .findContentletByIdentifier(contentlet2.getIdentifier(),
                        false, languageId, systemUser, false);
        isLive = contentlet3.isLive();

        int counter = 0;
        contentlet = contentlet3;
    }

    /**
     * Remove the content type and workflows created
     */
    @AfterClass
    public static void cleanup()
            throws DotDataException, DotSecurityException, AlreadyExistException {
        try {
            if (null != contentlet) {
                contentletAPI.delete(contentlet, APILocator.systemUser(), false);
            }
        } finally {
            try {
                if (null != schemeStepActionResult) {
                    cleanScheme(schemeStepActionResult.getScheme());
                }
            } finally {
                if (null != type) {
                    contentTypeAPI.delete(type);
                }
            }
        }
    }

}
