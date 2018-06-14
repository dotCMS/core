package com.dotmarketing.portlets.workflows.business;

import static com.dotcms.rest.api.v1.workflow.WorkflowTestUtil.getWorkflowActions;
import static com.dotmarketing.business.Role.ADMINISTRATOR;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.contenttype.model.field.DataTypes;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.FieldBuilder;
import com.dotcms.contenttype.model.field.TextField;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.rest.ContentHelper;
import com.dotcms.rest.api.v1.workflow.BulkActionView;
import com.dotcms.rest.api.v1.workflow.BulkActionsResultView;
import com.dotcms.rest.api.v1.workflow.BulkWorkflowSchemeView;
import com.dotcms.rest.api.v1.workflow.WorkflowResource;
import com.dotcms.rest.api.v1.workflow.WorkflowTestUtil;
import com.dotcms.util.IntegrationTestInitService;
import com.dotcms.workflow.form.BulkActionForm;
import com.dotcms.workflow.form.FireBulkActionsForm;
import com.dotcms.workflow.helper.WorkflowHelper;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.RoleAPI;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.workflows.model.WorkflowAction;
import com.dotmarketing.portlets.workflows.model.WorkflowScheme;
import com.dotmarketing.portlets.workflows.util.WorkflowImportExportUtil;
import com.liferay.portal.model.User;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.BeforeClass;
import org.junit.Test;

public class BulkActionsTest extends BaseWorkflowIntegrationTest {


    private static WorkflowAPI workflowAPI;
    private static RoleAPI roleAPI;
    private static WorkflowResource workflowResource;
    private static ContentletAPI contentletAPI;
    private static ContentTypeAPI contentTypeAPI;
    private static WorkflowHelper workflowHelper;
    private static User systemUser;
    private static User adminUser;
    private static LanguageAPI languageAPI;
    private static HostAPI hostAPI;

    @BeforeClass
    public static void prepare() throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
        workflowAPI = APILocator.getWorkflowAPI();
        contentletAPI = APILocator.getContentletAPI();
        roleAPI = APILocator.getRoleAPI();
        ContentHelper contentHelper = ContentHelper.getInstance();
        PermissionAPI permissionAPI = APILocator.getPermissionAPI();
        WorkflowImportExportUtil workflowImportExportUtil = WorkflowImportExportUtil.getInstance();
        workflowHelper = new WorkflowHelper(workflowAPI, roleAPI, contentletAPI,
                permissionAPI,
                workflowImportExportUtil
        );

        contentTypeAPI = APILocator.getContentTypeAPI(APILocator.systemUser());

        contentletAPI = APILocator.getContentletAPI();
        contentTypeAPI = APILocator.getContentTypeAPI(APILocator.systemUser());

        systemUser = APILocator.systemUser();

        adminUser = APILocator.getUserAPI()
                .loadByUserByEmail("admin@dotcms.com", systemUser, false);

        languageAPI = APILocator.getLanguageAPI();
        hostAPI = APILocator.getHostAPI();
    }


    @Test
    public void Test_Fire_Action_Then_Validate_Step_Changed() throws Exception {
        ContentType contentType = null;
        Contentlet contentlet = null;
        try {

            final Host host = hostAPI.findDefaultHost(systemUser, false);
            final Role adminRole = roleAPI.loadRoleByKey(ADMINISTRATOR);

            final String ctPrefix = "TestContentType";
            final String newContentTypeName = ctPrefix + System.currentTimeMillis();

            // Create ContentType
            contentType = createContentTypeAndAssignPermissions(newContentTypeName,
                    BaseContentType.CONTENT, PermissionAPI.PERMISSION_READ, adminRole.getId());
            final WorkflowScheme systemWorkflow = workflowAPI.findSystemWorkflowScheme();
            final WorkflowScheme documentWorkflow = workflowAPI
                    .findSchemeByName(WorkflowTestUtil.DM_WORKFLOW);

            // Add fields to the contentType
            final Field field =
                    FieldBuilder.builder(TextField.class).name("requiredUniqueName").variable("requiredUniqueName")
                            .required(true)
                            .unique(true)
                            .contentTypeId(contentType.id()).dataType(DataTypes.TEXT).build();
            contentType = contentTypeAPI.save(contentType, Collections.singletonList(field));

            // Assign contentType to Workflow
            workflowAPI.saveSchemeIdsForContentType(contentType,
                    Arrays.asList(systemWorkflow.getId(), documentWorkflow.getId()));

            // Create a content sample
            contentlet = new Contentlet();
            // instruct the content with its own type
            contentlet.setStructureInode(contentType.inode());
            contentlet.setHost(host.getIdentifier());
            contentlet.setLanguageId(languageAPI.getDefaultLanguage().getId());

            // Save the content
            contentlet = contentletAPI.checkin(contentlet, systemUser, false);
            assertNotNull(contentlet.getInode());

            TimeUnit.SECONDS.sleep(2); // Wait for the new content to be indexed.
            assertTrue(contentletAPI.isInodeIndexed(contentlet.getInode()));

            //  Now Test BulkActions
            final BulkActionForm form = new BulkActionForm(
                    Collections.singletonList(contentlet.getInode()), null);
            final BulkActionView view = workflowHelper.findBulkActions(adminUser, form);

            final List<BulkWorkflowSchemeView> schemes = view.getSchemes();

            final Optional<BulkWorkflowSchemeView> documentManagementOptional = schemes.stream()
                    .filter(bulkWorkflowSchemeView -> WorkflowTestUtil.DM_WORKFLOW
                            .equals(bulkWorkflowSchemeView.getScheme().getName())).findFirst();
            final Optional<BulkWorkflowSchemeView> systemWorkflowOptional = schemes.stream()
                    .filter(bulkWorkflowSchemeView -> WorkflowTestUtil.SYSTEM_WORKFLOW
                            .equals(bulkWorkflowSchemeView.getScheme().getName())).findFirst();

            assertTrue(documentManagementOptional.isPresent());
            assertTrue(systemWorkflowOptional.isPresent());

            //Validate we have every possible action within the workflows.
            final BulkWorkflowSchemeView documentManagementScheme = documentManagementOptional.get();
            final List<WorkflowAction> documentActions = getWorkflowActions(documentManagementScheme);

            assertTrue(documentActions.stream()
                    .anyMatch(action -> "Save as Draft".equals(action.getName())));
            assertTrue(documentActions.stream()
                    .anyMatch(action -> "Send for Review".equals(action.getName())));
            assertTrue(documentActions.stream()
                    .anyMatch(action -> "Send to Legal".equals(action.getName())));
            assertTrue(documentActions.stream().anyMatch(action -> "Publish".equals(action.getName())));
            assertTrue(
                    documentActions.stream().anyMatch(action -> "Republish".equals(action.getName())));
            assertTrue(
                    documentActions.stream().anyMatch(action -> "Unpublish".equals(action.getName())));
            assertTrue(documentActions.stream().anyMatch(action -> "Archive".equals(action.getName())));
            assertTrue(documentActions.stream()
                    .anyMatch(action -> "Tweet This!".equals(action.getName())));

            final BulkWorkflowSchemeView systemWorkflowScheme = systemWorkflowOptional.get();
            final List<WorkflowAction> systemActions = getWorkflowActions(systemWorkflowScheme);

            assertTrue(systemActions.stream().anyMatch(action -> "Republish".equals(action.getName())));
            assertTrue(systemActions.stream().anyMatch(action -> "Unpublish".equals(action.getName())));
            assertTrue(systemActions.stream().anyMatch(action -> "Copy".equals(action.getName())));
            assertTrue(systemActions.stream().anyMatch(action -> "Save".equals(action.getName())));
            assertTrue(systemActions.stream()
                    .anyMatch(action -> "Save / Publish".equals(action.getName())));


            final FireBulkActionsForm actionsForm = new FireBulkActionsForm(null, Collections.singletonList(contentlet.getInode()), "");
            final Future<BulkActionsResultView> futureResultAfterSave = workflowHelper.fireBulkActions(actionsForm, systemUser);

        } finally {

            if(contentlet != null){
                contentletAPI.delete(contentlet, systemUser, false);
            }

            if (contentType != null) {
                contentTypeAPI.delete(contentType);
            }
        }

    }

}
