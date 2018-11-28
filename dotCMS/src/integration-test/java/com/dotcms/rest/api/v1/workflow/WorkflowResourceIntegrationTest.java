package com.dotcms.rest.api.v1.workflow;

import static com.dotcms.rest.api.v1.workflow.WorkflowTestUtil.ADMIN_DEFAULT_ID;
import static com.dotcms.rest.api.v1.workflow.WorkflowTestUtil.ADMIN_DEFAULT_MAIL;
import static com.dotcms.rest.api.v1.workflow.WorkflowTestUtil.ADMIN_NAME;
import static com.dotcms.rest.api.v1.workflow.WorkflowTestUtil.CURRENT_STEP;
import static com.dotcms.rest.api.v1.workflow.WorkflowTestUtil.DM_WORKFLOW;
import static com.dotcms.rest.api.v1.workflow.WorkflowTestUtil.PUBLISH;
import static com.dotcms.rest.api.v1.workflow.WorkflowTestUtil.SAVE;
import static com.dotcms.rest.api.v1.workflow.WorkflowTestUtil.SAVE_AS_DRAFT;
import static com.dotcms.rest.api.v1.workflow.WorkflowTestUtil.SAVE_PUBLISH;
import static com.dotcms.rest.api.v1.workflow.WorkflowTestUtil.SEND_FOR_REVIEW;
import static com.dotcms.rest.api.v1.workflow.WorkflowTestUtil.SEND_TO_LEGAL;
import static com.dotcms.rest.api.v1.workflow.WorkflowTestUtil.SYSTEM_WORKFLOW;
import static com.dotcms.rest.api.v1.workflow.WorkflowTestUtil.actionName;
import static com.dotcms.rest.api.v1.workflow.WorkflowTestUtil.addSteps;
import static com.dotcms.rest.api.v1.workflow.WorkflowTestUtil.collectSampleContent;
import static com.dotcms.rest.api.v1.workflow.WorkflowTestUtil.createScheme;
import static com.dotcms.rest.api.v1.workflow.WorkflowTestUtil.createWorkflowActions;
import static com.dotcms.rest.api.v1.workflow.WorkflowTestUtil.doCleanUp;
import static com.dotcms.rest.api.v1.workflow.WorkflowTestUtil.findSchemes;
import static com.dotcms.rest.api.v1.workflow.WorkflowTestUtil.findSteps;
import static com.dotcms.rest.api.v1.workflow.WorkflowTestUtil.getAllWorkflowActions;
import static com.dotcms.rest.api.v1.workflow.WorkflowTestUtil.schemeName;
import static com.dotmarketing.business.Role.ADMINISTRATOR;
import static com.dotmarketing.portlets.workflows.util.WorkflowImportExportUtil.ACTION_ID;
import static com.dotmarketing.portlets.workflows.util.WorkflowImportExportUtil.ACTION_ORDER;
import static com.dotmarketing.portlets.workflows.util.WorkflowImportExportUtil.STEP_ID;
import static com.dotmarketing.portlets.workflows.util.WorkflowImportExportUtil.getInstance;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dotcms.business.WrapInTransaction;
import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.contenttype.business.FieldAPI;
import com.dotcms.contenttype.model.field.CategoryField;
import com.dotcms.contenttype.model.field.CheckboxField;
import com.dotcms.contenttype.model.field.CustomField;
import com.dotcms.contenttype.model.field.DataTypes;
import com.dotcms.contenttype.model.field.DateField;
import com.dotcms.contenttype.model.field.DateTimeField;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.FieldBuilder;
import com.dotcms.contenttype.model.field.ImageField;
import com.dotcms.contenttype.model.field.KeyValueField;
import com.dotcms.contenttype.model.field.MultiSelectField;
import com.dotcms.contenttype.model.field.RadioField;
import com.dotcms.contenttype.model.field.SelectField;
import com.dotcms.contenttype.model.field.TextAreaField;
import com.dotcms.contenttype.model.field.TextField;
import com.dotcms.contenttype.model.field.TimeField;
import com.dotcms.contenttype.model.field.WysiwygField;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.mock.response.MockAsyncResponse;
import com.dotcms.repackage.javax.ws.rs.container.AsyncResponse;
import com.dotcms.repackage.javax.ws.rs.core.Response;
import com.dotcms.repackage.javax.ws.rs.core.Response.Status;
import com.dotcms.rest.ContentHelper;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.api.v1.authentication.ResponseUtil;
import com.dotcms.util.CollectionsUtils;
import com.dotcms.util.IntegrationTestInitService;
import com.dotcms.workflow.form.BulkActionForm;
import com.dotcms.workflow.form.FireActionForm;
import com.dotcms.workflow.form.FireBulkActionsForm;
import com.dotcms.workflow.form.WorkflowActionForm;
import com.dotcms.workflow.form.WorkflowActionStepForm;
import com.dotcms.workflow.form.WorkflowSchemeForm;
import com.dotcms.workflow.form.WorkflowSchemeImportObjectForm;
import com.dotcms.workflow.form.WorkflowStepUpdateForm;
import com.dotcms.workflow.helper.WorkflowHelper;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.RoleAPI;
import com.dotmarketing.exception.AlreadyExistException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.IndexPolicy;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.workflows.business.BaseWorkflowIntegrationTest;
import com.dotmarketing.portlets.workflows.business.WorkflowAPI;
import com.dotmarketing.portlets.workflows.model.WorkflowAction;
import com.dotmarketing.portlets.workflows.model.WorkflowScheme;
import com.dotmarketing.portlets.workflows.model.WorkflowState;
import com.dotmarketing.portlets.workflows.model.WorkflowStep;
import com.dotmarketing.portlets.workflows.util.WorkflowImportExportUtil;
import com.dotmarketing.util.DateUtil;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UUIDGenerator;
import com.dotmarketing.util.UtilMethods;
import com.google.common.collect.ImmutableMap;
import com.liferay.portal.model.User;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.lang.RandomStringUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class WorkflowResourceIntegrationTest extends BaseWorkflowIntegrationTest {

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
    private static Host host;
    private static Role adminRole;
    private static Role systemRole;

    @BeforeClass
    public static void prepare() throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
        workflowAPI =  APILocator.getWorkflowAPI();
        contentletAPI = APILocator.getContentletAPI();
        roleAPI = APILocator.getRoleAPI();
        ContentHelper contentHelper = ContentHelper.getInstance();
        PermissionAPI permissionAPI = APILocator.getPermissionAPI();
        WorkflowImportExportUtil workflowImportExportUtil = getInstance();
        WorkflowHelper workflowHelper = new WorkflowHelper(workflowAPI, roleAPI, contentletAPI, permissionAPI,
                workflowImportExportUtil);
        ResponseUtil responseUtil = ResponseUtil.INSTANCE;

        final User user = mock(User.class);
        when(user.getUserId()).thenReturn(ADMIN_DEFAULT_ID);
        when(user.getEmailAddress()).thenReturn(ADMIN_DEFAULT_MAIL);
        when(user.getFullName()).thenReturn(ADMIN_NAME);
        when(user.getLocale()).thenReturn(Locale.getDefault());

        final WebResource webResource = mock(WebResource.class);
        final InitDataObject dataObject = mock(InitDataObject.class);
        when(dataObject.getUser()).thenReturn(user);
        when(webResource
                .init(anyString(), anyBoolean(), any(HttpServletRequest.class), anyBoolean(),
                        anyString())).thenReturn(dataObject);

        workflowResource = new WorkflowResource(workflowHelper, contentHelper, workflowAPI,
                contentletAPI, responseUtil, permissionAPI, workflowImportExportUtil, webResource);

        contentTypeAPI = APILocator.getContentTypeAPI(APILocator.systemUser());
        systemUser = APILocator.systemUser();

        adminUser = APILocator.getUserAPI()
                .loadByUserByEmail("admin@dotcms.com", systemUser, false);

        languageAPI = APILocator.getLanguageAPI();
        hostAPI = APILocator.getHostAPI();

        host = hostAPI.findDefaultHost(systemUser, false);
        adminRole = roleAPI.loadRoleByKey(ADMINISTRATOR);
        systemRole = roleAPI.loadRoleByKey(Role.SYSTEM);


    }

    //@AfterClass
    public static void cleanup() throws Exception {

        doCleanUp(workflowAPI);

    }

    @SuppressWarnings("unchecked")
    @Test
    public void testImportScheme() throws DotDataException {

        final HttpServletRequest request = mock(HttpServletRequest.class);
        final List<Permission> permissions                              = new ArrayList<>();
        final List<WorkflowScheme> schemes                              = new ArrayList<>();
        final WorkflowScheme       scheme                               = new WorkflowScheme();
        final List<WorkflowStep>   steps                                = new ArrayList<>();
        final List<WorkflowAction> actions                              = new ArrayList<>();
        final List<Map<String, String>> actionSteps                     = new ArrayList<>();

        try {

            scheme.setArchived(false);
            scheme.setName("scheme::TestImport" + UUIDGenerator.generateUuid());
            scheme.setModDate(new Date());
            scheme.setId(UUIDGenerator.generateUuid());
            schemes.add(scheme);

            final WorkflowStep workflowStep1 = new WorkflowStep();

            workflowStep1.setSchemeId(scheme.getId());
            workflowStep1.setCreationDate(new Date());
            workflowStep1.setResolved(false);
            workflowStep1.setMyOrder(0);
            workflowStep1.setName("Step1");
            workflowStep1.setId(UUIDGenerator.generateUuid());
            steps.add(workflowStep1);

            final WorkflowStep workflowStep2 = new WorkflowStep();

            workflowStep2.setSchemeId(scheme.getId());
            workflowStep2.setCreationDate(new Date());
            workflowStep2.setResolved(false);
            workflowStep2.setMyOrder(1);
            workflowStep2.setName("Step2");
            workflowStep2.setId(UUIDGenerator.generateUuid());
            steps.add(workflowStep2);

            final WorkflowAction workflowAction1 = new WorkflowAction();

            workflowAction1.setId(UUIDGenerator.generateUuid());
            workflowAction1.setShowOn(WorkflowState.LOCKED, WorkflowState.PUBLISHED, WorkflowState.UNPUBLISHED, WorkflowState.EDITING);
            workflowAction1.setNextStep(workflowStep2.getId());
            workflowAction1.setNextAssign(roleAPI.loadRoleByKey(ADMINISTRATOR).getId());
            workflowAction1.setSchemeId(scheme.getId());
            workflowAction1.setName("save");
            workflowAction1.setOrder(0);
            workflowAction1.setCommentable(true);
            actions.add(workflowAction1);

            final WorkflowAction workflowAction2 = new WorkflowAction();

            workflowAction2.setId(UUIDGenerator.generateUuid());
            workflowAction2.setShowOn(WorkflowState.LOCKED, WorkflowState.PUBLISHED, WorkflowState.UNPUBLISHED, WorkflowState.EDITING);
            workflowAction2.setNextStep(workflowStep2.getId());
            workflowAction2.setNextAssign(roleAPI.loadRoleByKey(ADMINISTRATOR).getId());
            workflowAction2.setSchemeId(scheme.getId());
            workflowAction2.setName("save/publish");
            workflowAction2.setOrder(1);
            workflowAction2.setCommentable(true);
            actions.add(workflowAction2);

            final WorkflowAction workflowAction3 = new WorkflowAction();

            workflowAction3.setId(UUIDGenerator.generateUuid());
            workflowAction3.setShowOn(WorkflowState.LOCKED, WorkflowState.PUBLISHED, WorkflowState.EDITING);
            workflowAction3.setNextStep(WorkflowAction.CURRENT_STEP);
            workflowAction3.setNextAssign(roleAPI.loadRoleByKey(ADMINISTRATOR).getId());
            workflowAction3.setSchemeId(scheme.getId());
            workflowAction3.setName("finish");
            workflowAction3.setOrder(2);
            workflowAction3.setCommentable(true);
            actions.add(workflowAction3);

            final Map<String, String> actionStep1 = new HashMap<>();
            actionStep1.put(ACTION_ID, workflowAction1.getId());
            actionStep1.put(STEP_ID, workflowStep1.getId());
            actionStep1.put(ACTION_ORDER, "0");
            actionSteps.add(actionStep1);

            final Map<String, String> actionStep2 = new HashMap<>();
            actionStep2.put(ACTION_ID, workflowAction2.getId());
            actionStep2.put(STEP_ID, workflowStep1.getId());
            actionStep2.put(ACTION_ORDER, "1");
            actionSteps.add(actionStep2);

            final Map<String, String> actionStep3 = new HashMap<>();
            actionStep3.put(ACTION_ID, workflowAction3.getId());
            actionStep3.put(STEP_ID, workflowStep2.getId());
            actionStep3.put(ACTION_ORDER, "2");
            actionSteps.add(actionStep3);

            final Permission permission1 = new Permission();
            permission1.setId(0);
            permission1.setInode(workflowAction1.getId());
            final String anyoneWhoCanEditRoleId = "617f7300-5c7b-463f-9554-380b918520bc";
            permission1.setRoleId(anyoneWhoCanEditRoleId);
            permission1.setPermission(1);
            permissions.add(permission1);

            final Permission permission2 = new Permission();
            permission2.setId(0);
            permission2.setInode(workflowAction2.getId());
            permission2.setRoleId(anyoneWhoCanEditRoleId);
            permission2.setPermission(1);
            permissions.add(permission2);


            final WorkflowSchemeImportObjectForm exportObjectForm =
                    new WorkflowSchemeImportObjectForm(
                            new WorkflowSchemeImportExportObjectView(WorkflowResource.VERSION,schemes,steps,actions,actionSteps,Collections.emptyList(),Collections.emptyList()),
                            permissions);

            final Response importResponse = workflowResource.importScheme(request, exportObjectForm);
            assertEquals(Response.Status.OK.getStatusCode(), importResponse.getStatus());

            final Response exportResponse = workflowResource.exportScheme(request, scheme.getId());
            assertEquals(Response.Status.OK.getStatusCode(), importResponse.getStatus());
            final ResponseEntityView exportEntityView = ResponseEntityView.class.cast(exportResponse.getEntity());
            final Map importSchemeMap = Map.class.cast(exportEntityView.getEntity());
            assertNotNull(importSchemeMap);

            final WorkflowSchemeImportExportObjectView exportObject = (WorkflowSchemeImportExportObjectView) importSchemeMap.get("workflowObject");
            final List<Permission> permissionsExported = (List<Permission>) importSchemeMap.get("permissions");

            assertNotNull(exportObject);
            assertNotNull(permissionsExported);

            assertNotNull(exportObject.getSchemes());
            assertEquals(1, exportObject.getSchemes().size());
            assertEquals(scheme.getId(), exportObject.getSchemes().get(0).getId());

            assertNotNull(exportObject.getSteps());
            assertEquals(2, exportObject.getSteps().size());
            assertEquals(workflowStep1.getId(), exportObject.getSteps().get(0).getId());
            assertEquals(workflowStep2.getId(), exportObject.getSteps().get(1).getId());

            assertNotNull(exportObject.getActions());
            assertEquals(3, exportObject.getActions().size());
            final Set<String> actionIdSet = exportObject.getActions().stream().map(WorkflowAction::getId).collect(Collectors.toSet());
            assertTrue(actionIdSet.contains(workflowAction1.getId()));
            assertTrue(actionIdSet.contains(workflowAction2.getId()));
            assertTrue(actionIdSet.contains(workflowAction3.getId()));

            assertNotNull(exportObject.getActionSteps());
            assertEquals(3, exportObject.getActionSteps().size());

            assertNotNull(permissionsExported);
            assertEquals(2, permissionsExported.size());
        } finally {

            if (null != scheme.getId()) {

                try {
                    final WorkflowScheme schemeToRemove = workflowAPI.findScheme(scheme.getId());
                    schemeToRemove.setArchived(true);
                    workflowAPI.saveScheme(schemeToRemove, APILocator.systemUser());
                    workflowAPI.deleteScheme(schemeToRemove, APILocator.systemUser()).get();
                } catch (Exception e) {
                    Logger.warn(getClass(), e.getMessage(), e);
                }
            }
        }

    }

    @Test
    public void testAddSchemaThenFindIt(){
        final WorkflowScheme savedScheme = createScheme(workflowResource);
        assertNotNull(savedScheme);
        final List<WorkflowScheme> schemaList = findSchemes(workflowResource);
        boolean found = false;
        for(WorkflowScheme scheme:schemaList){
            found = savedScheme.getId().equals(scheme.getId());
            if(found){
               break;
            }
        }
        assertTrue(found);
    }

    @Test
    public void testUpdateSchema(){
        final String updatedName = schemeName();
        final WorkflowScheme savedScheme = createScheme(workflowResource);
        assertNotNull(savedScheme);
        final HttpServletRequest request = mock(HttpServletRequest.class);
        WorkflowSchemeForm form = new WorkflowSchemeForm.Builder().schemeDescription("lol").schemeArchived(false).schemeName(updatedName).build();
        final Response updateResponse = workflowResource.updateScheme(request,savedScheme.getId(), form);
        assertEquals(Response.Status.OK.getStatusCode(), updateResponse.getStatus());
        final ResponseEntityView updateSchemeEntityView = ResponseEntityView.class.cast(updateResponse.getEntity());
        final WorkflowScheme updatedScheme = WorkflowScheme.class.cast(updateSchemeEntityView.getEntity());
        assertNotNull(updatedScheme);
    }

    @Test
    public void testUpdateSchemaUsingDupeName(){
        final WorkflowScheme savedScheme1 = createScheme(workflowResource);
        final WorkflowScheme savedScheme2 = createScheme(workflowResource);
        assertNotNull(savedScheme1);
        final String updatedName = savedScheme2.getName();
        final HttpServletRequest request = mock(HttpServletRequest.class);
        WorkflowSchemeForm form = new WorkflowSchemeForm.Builder().schemeDescription("lol").schemeArchived(false).schemeName(updatedName).build();
        final Response updateResponse = workflowResource.updateScheme(request,savedScheme1.getId(), form);
        assertEquals(Status.OK.getStatusCode(), updateResponse.getStatus());
    }

    @Test
    public void testCreateSchemeThenAddStepsThenAddActions() throws Exception{
        final Role adminRole = roleAPI.loadRoleByKey(ADMINISTRATOR);
        final String adminRoleId = adminRole.getId();
        final WorkflowScheme savedScheme = createScheme(workflowResource);
        assertNotNull(savedScheme);
        final List<WorkflowStep> workflowSteps = addSteps(workflowResource, savedScheme,3);
        assertFalse(workflowSteps.isEmpty());
        final List<WorkflowAction> actions = createWorkflowActions(workflowResource, savedScheme, adminRoleId, workflowSteps);
        assertFalse(actions.isEmpty());
    }

    @Test
    @WrapInTransaction
    public void testCreateSchemeThenAddStepsThenDeleteSteps() {
        final int numSteps = 5;
        final WorkflowScheme savedScheme = createScheme(workflowResource);
        assertNotNull(savedScheme);
        final List<WorkflowStep> workflowSteps = addSteps(workflowResource, savedScheme, numSteps);
        assertFalse(workflowSteps.isEmpty());
        final HttpServletRequest removeStepRequest = mock(HttpServletRequest.class);

        final CountDownLatch countDownLatch = new CountDownLatch(workflowSteps.size());
        final AtomicInteger count = new AtomicInteger(0);
        for(final WorkflowStep workflowStep: workflowSteps){
            final AsyncResponse asyncResponse = new MockAsyncResponse((arg) -> {

                countDownLatch.countDown();
                final Response deleteStepResponse = (Response)arg;
                assertEquals(Response.Status.OK.getStatusCode(), deleteStepResponse.getStatus());
                count.addAndGet(1);
                return true;
            }, arg -> {
                countDownLatch.countDown();
                fail("Error on deleting step");
                return true;
            });

            workflowResource.deleteStep(removeStepRequest, asyncResponse, workflowStep.getId());
            DateUtil.sleep(500);
        }

        try {
            countDownLatch.await();
            assertEquals(count.get(), numSteps);
        } catch (InterruptedException e) {
            fail(e.getMessage());
        }

    }

    @Test
    public void testCreateSchemeThenFindStepsBySchemeId() {
        int numSteps = 4;
        final WorkflowScheme savedScheme = createScheme(workflowResource);
        final List<WorkflowStep> steps = findSteps(workflowResource, savedScheme);
        validateOrderedSteps(steps);
        assertTrue(steps.isEmpty());
        addSteps(workflowResource, savedScheme,numSteps);
        final List<WorkflowStep> afterAddingSteps = findSteps(workflowResource, savedScheme);
        assertEquals(numSteps, afterAddingSteps.size());
        validateOrderedSteps(afterAddingSteps);
    }

    @Test
    public void testReorderStep() {
        final int numSteps = 10;
        final Random random = new Random();
        final int randomEntry = random.nextInt(numSteps);
        final WorkflowScheme savedScheme = createScheme(workflowResource);
        final List<WorkflowStep> steps = findSteps(workflowResource, savedScheme);
        assertTrue(steps.isEmpty());
        final List<WorkflowStep> workflowStepsOriginalSetUp = addSteps(workflowResource, savedScheme, numSteps);
        final WorkflowStep firstWorkflowStep = workflowStepsOriginalSetUp.get(0);
        reorderSteps(firstWorkflowStep.getId(),randomEntry);

        final List<WorkflowStep> afterReorderingSteps = findSteps(workflowResource, savedScheme);
        assertEquals(numSteps, afterReorderingSteps.size());
        final WorkflowStep rearrangedPosition = afterReorderingSteps.get(randomEntry);

        assertEquals(rearrangedPosition.getMyOrder(),randomEntry);
        assertEquals(rearrangedPosition.getId(),firstWorkflowStep.getId());
        validateOrderedSteps(afterReorderingSteps);
    }

    /**
     * Performs a reorder command over the workflow resource
     */
    private void reorderSteps(String stepId, int newPosition) {
        final HttpServletRequest reorderStepRequest = mock(HttpServletRequest.class);
        final Response reorderStepResponse = workflowResource
                .reorderStep(reorderStepRequest, stepId, newPosition);
        assertEquals(Response.Status.OK.getStatusCode(), reorderStepResponse.getStatus());
        final ResponseEntityView reorderedStepEntityView = ResponseEntityView.class
                .cast(reorderStepResponse.getEntity());
        final String ok = String.class.cast(reorderedStepEntityView.getEntity());
        assertEquals(ResponseEntityView.OK, ok);
    }

    private void validateOrderedSteps(final List<WorkflowStep> steps) {
        int lastOrderValue = -1;
        for (WorkflowStep step : steps) {
            assertTrue(lastOrderValue < step.getMyOrder());
            lastOrderValue = step.getMyOrder();
        }
    }

    @Test
    public void testUpdateStep(){
        final int numSteps = 3;
        final String updatedName = "LOL!";
        final int newOrder = numSteps + 10; //We're sending an index outside the boundaries. See if we can break it.
        final WorkflowScheme savedScheme = createScheme(workflowResource);
        assertNotNull(savedScheme);
        final List<WorkflowStep> workflowSteps = addSteps(workflowResource, savedScheme, numSteps);
        assertFalse(workflowSteps.isEmpty());
        final WorkflowStep workflowStep = workflowSteps.get(0);
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final WorkflowStepUpdateForm updatedValuesForm = new WorkflowStepUpdateForm.Builder().enableEscalation(false).escalationAction("").escalationTime("0").enableEscalation(false).stepName(updatedName).stepOrder(newOrder).build();
        final Response updateStepResponse = workflowResource.updateStep(request, workflowStep.getId(), updatedValuesForm);
        assertEquals(Response.Status.OK.getStatusCode(), updateStepResponse.getStatus());
        final ResponseEntityView updateResponseEv = ResponseEntityView.class.cast(updateStepResponse.getEntity());
        final WorkflowStep updatedWorkflowStep = WorkflowStep.class.cast(updateResponseEv.getEntity());
        assertEquals(updatedName, updatedWorkflowStep.getName());
        assertEquals(numSteps -1 ,updatedWorkflowStep.getMyOrder());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testSaveActionToStepThenFindActionsBySchemeThenFindByStep() throws Exception{
        final Role adminRole = roleAPI.loadRoleByKey(ADMINISTRATOR);
        final String adminRoleId = adminRole.getId();
        final WorkflowScheme savedScheme = createScheme(workflowResource);
        assertNotNull(savedScheme);
        final List<WorkflowStep> workflowSteps = addSteps(workflowResource, savedScheme,2);
        assertFalse(workflowSteps.isEmpty());
        assertEquals(2, workflowSteps.size());
        final List<WorkflowAction> actions = createWorkflowActions(workflowResource, savedScheme, adminRoleId, workflowSteps);
        assertEquals(2, actions.size());

        final WorkflowStep secondStep = workflowSteps.get(1);
        final WorkflowAction firstAction = actions.get(0);

        final HttpServletRequest request1 = mock(HttpServletRequest.class);
        //assign the first action to the second step
        final Response saveActionToStepResponse = workflowResource.saveActionToStep(
                request1, secondStep.getId(),
                new WorkflowActionStepForm.Builder().actionId(firstAction.getId()).build()
        );
        final ResponseEntityView updateResponseEv = ResponseEntityView.class.cast(saveActionToStepResponse.getEntity());
        assertEquals(ResponseEntityView.OK,updateResponseEv.getEntity());

        final HttpServletRequest request2 = mock(HttpServletRequest.class);
        final Response actionsBySchemeResponse = workflowResource.findActionsByScheme(request2, savedScheme.getId());
        final ResponseEntityView findActionsResponseEv = ResponseEntityView.class.cast(actionsBySchemeResponse.getEntity());
        final List<WorkflowAction> actionsByScheme = List.class.cast(findActionsResponseEv.getEntity());
        assertEquals(2, actionsByScheme.size());

        final HttpServletRequest request3 = mock(HttpServletRequest.class);
        //This returns 1 single action
        final Response actionsByStepResponse = workflowResource.findActionByStep(request3, secondStep.getId(), firstAction.getId());
        final ResponseEntityView findActionsByStepResponseEv = ResponseEntityView.class.cast(actionsByStepResponse.getEntity());
        final WorkflowAction actionByStep = WorkflowAction.class.cast(findActionsByStepResponseEv.getEntity());
        assertNotNull(actionByStep);
        assertEquals(firstAction.getId(),actionByStep.getId());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testFindActionsByStepThenDeleteThem() throws Exception{
        final Role adminRole = roleAPI.loadRoleByKey(ADMINISTRATOR);
        final String adminRoleId = adminRole.getId();
        final WorkflowScheme savedScheme = createScheme(workflowResource);
        assertNotNull(savedScheme);
        final List<WorkflowStep> workflowSteps = addSteps(workflowResource, savedScheme,2);
        assertFalse(workflowSteps.isEmpty());
        assertEquals(2, workflowSteps.size());
        final List<WorkflowAction> actions = createWorkflowActions(workflowResource, savedScheme, adminRoleId, workflowSteps);
        assertEquals(2, actions.size());

        final HttpServletRequest request1 = mock(HttpServletRequest.class);
        final Response actionsByStepResponse1 = workflowResource.findActionsByStep(request1,workflowSteps.get(0).getId());
        final ResponseEntityView findActionsByStepResponseEv1 = ResponseEntityView.class.cast(actionsByStepResponse1.getEntity());
        final List<WorkflowAction> actionsByStep1 = List.class.cast(findActionsByStepResponseEv1.getEntity());
        assertEquals(1, actionsByStep1.size());

        final HttpServletRequest request2 = mock(HttpServletRequest.class);
        //This returns a list actions
        final Response actionsByStepResponse2 = workflowResource.findActionsByStep(request2,workflowSteps.get(1).getId());
        final ResponseEntityView findActionsByStepResponseEv2 = ResponseEntityView.class.cast(actionsByStepResponse2.getEntity());
        final List<WorkflowAction> actionsByStep2 = List.class.cast(findActionsByStepResponseEv2.getEntity());
        assertEquals(1, actionsByStep2.size());

        final HttpServletRequest request3 = mock(HttpServletRequest.class);
        final Response deleteActionResponse1 = workflowResource.deleteAction(request3,actionsByStep1.get(0).getId());
        final ResponseEntityView deleteActionByResponseEv1 = ResponseEntityView.class.cast(deleteActionResponse1.getEntity());
        final Response deleteActionResponse2 = workflowResource.deleteAction(request3,actionsByStep2.get(0).getId());
        final ResponseEntityView deleteActionByResponseEv2 = ResponseEntityView.class.cast(deleteActionResponse2.getEntity());
        final String ok1 = String.class.cast(deleteActionByResponseEv1.getEntity());
        assertEquals(ResponseEntityView.OK,ok1);
        final String ok2 = String.class.cast(deleteActionByResponseEv2.getEntity());
        assertEquals(ResponseEntityView.OK,ok2);

        final HttpServletRequest request4 = mock(HttpServletRequest.class);
        final Response actionsBySchemeResponse = workflowResource.findActionsByScheme(request4, savedScheme.getId());
        final ResponseEntityView findActionsResponseEv = ResponseEntityView.class.cast(actionsBySchemeResponse.getEntity());
        final List<WorkflowAction> actionsByScheme = List.class.cast(findActionsResponseEv.getEntity());
        assertEquals(0, actionsByScheme.size());

    }

    @Test
    public void testUpdateAction() throws Exception{
        final Role adminRole = roleAPI.loadRoleByKey(ADMINISTRATOR);
        final String adminRoleId = adminRole.getId();
        final WorkflowScheme savedScheme = createScheme(workflowResource);
        assertNotNull(savedScheme);
        final List<WorkflowStep> workflowSteps = addSteps(workflowResource, savedScheme,1);
        assertFalse(workflowSteps.isEmpty());
        assertEquals(1, workflowSteps.size());
        final List<WorkflowAction> actions = createWorkflowActions(workflowResource, savedScheme, adminRoleId, workflowSteps);
        assertEquals(1, actions.size());
        final HttpServletRequest request = mock(HttpServletRequest.class);

        final Set<WorkflowState> states = WorkflowState.toSet(WorkflowState.values());
        final String actionNewName = actionName();

        final WorkflowActionForm form = new WorkflowActionForm.Builder().schemeId(savedScheme.getId()).
                stepId(workflowSteps.get(0).getId()).
                actionName(actionNewName).
                showOn(states).
                actionNextStep(CURRENT_STEP).
                actionAssignable(false).
                actionCommentable(false).
                requiresCheckout(false).
                actionNextAssign(adminRoleId).
                whoCanUse(Arrays.asList("")).
                actionCondition("").
                build();

        final String actionId = actions.get(0).getId();
        final Response updateResponse = workflowResource.updateAction(request, actionId, form);
        final ResponseEntityView updateResponseEv = ResponseEntityView.class.cast(updateResponse.getEntity());
        final WorkflowAction workflowAction = WorkflowAction.class.cast(updateResponseEv.getEntity());
        assertEquals(actionNewName,workflowAction.getName());
        final HttpServletRequest request2 = mock(HttpServletRequest.class);
        final Response findActionResponse = workflowResource.findAction(request2, actionId);
        final ResponseEntityView findActionResponseEv = ResponseEntityView.class.cast(findActionResponse.getEntity());
        final WorkflowAction wa = WorkflowAction.class.cast(findActionResponseEv.getEntity());
        assertNotNull(wa);
        assertEquals(actionNewName,wa.getName());
    }

    /**
     * Test the delete scheme resource
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testDeleteScheme() throws Exception{
        final Role adminRole = roleAPI.loadRoleByKey(ADMINISTRATOR);
        final String adminRoleId = adminRole.getId();
        final WorkflowScheme savedScheme = createScheme(workflowResource);
        assertNotNull(savedScheme);
        final List<WorkflowStep> workflowSteps = addSteps(workflowResource, savedScheme,2);
        assertFalse(workflowSteps.isEmpty());
        assertEquals(2, workflowSteps.size());
        final List<WorkflowAction> actions = createWorkflowActions(workflowResource, savedScheme, adminRoleId, workflowSteps);
        assertEquals(2, actions.size());

        final WorkflowStep secondStep = workflowSteps.get(1);
        final WorkflowAction firstAction = actions.get(0);

        final HttpServletRequest request1 = mock(HttpServletRequest.class);

        //assign the first action to the second step
        final Response saveActionToStepResponse = workflowResource.saveActionToStep(
                request1, secondStep.getId(),
                new WorkflowActionStepForm.Builder().actionId(firstAction.getId()).build()
        );
        final ResponseEntityView updateResponseEv = ResponseEntityView.class.cast(saveActionToStepResponse.getEntity());
        assertEquals(ResponseEntityView.OK,updateResponseEv.getEntity());

        final HttpServletRequest request2 = mock(HttpServletRequest.class);
        final Response actionsBySchemeResponse = workflowResource.findActionsByScheme(request2, savedScheme.getId());
        final ResponseEntityView findActionsResponseEv = ResponseEntityView.class.cast(actionsBySchemeResponse.getEntity());
        final List<WorkflowAction> actionsByScheme = List.class.cast(findActionsResponseEv.getEntity());
        assertEquals(2, actionsByScheme.size());

        final HttpServletRequest request3 = mock(HttpServletRequest.class);
        //This returns 1 single action
        final Response actionsByStepResponse = workflowResource.findActionByStep(request3, secondStep.getId(), firstAction.getId());
        final ResponseEntityView findActionsByStepResponseEv = ResponseEntityView.class.cast(actionsByStepResponse.getEntity());
        final WorkflowAction actionByStep = WorkflowAction.class.cast(findActionsByStepResponseEv.getEntity());
        assertNotNull(actionByStep);
        assertEquals(firstAction.getId(),actionByStep.getId());

        //test delete without archive
        final AsyncResponse asyncResponse = new MockAsyncResponse(
                (arg) -> {
            final Response deleteSchemeResponse = (Response)arg;
            assertEquals(Status.FORBIDDEN.getStatusCode(), deleteSchemeResponse.getStatus());

            //test archive scheme
            WorkflowSchemeForm form = new WorkflowSchemeForm.Builder().schemeDescription("Delete scheme").schemeArchived(true).schemeName(savedScheme.getName()).build();
            final HttpServletRequest request5 = mock(HttpServletRequest.class);
            final Response updateResponse = workflowResource.updateScheme(request5,savedScheme.getId(), form);
            assertEquals(Response.Status.OK.getStatusCode(), updateResponse.getStatus());

            //test delete scheme
            final AsyncResponse asyncResponse2 = new MockAsyncResponse(
                    arg2 -> {
                        final Response deleteSchemeResponse2 =(Response) arg2;
                        assertEquals(Status.OK.getStatusCode(), deleteSchemeResponse2.getStatus());
                        return true;
                    },
                    (arg2) -> true
            );
            final HttpServletRequest request6 = mock(HttpServletRequest.class);
            workflowResource.deleteScheme(request6, asyncResponse2, savedScheme.getId());

            return true;
        }, (arg) -> true);

        final HttpServletRequest request4 = mock(HttpServletRequest.class);
        workflowResource.deleteScheme(request4, asyncResponse, savedScheme.getId());
    }

    @Test
    public void Test_Get_Bulk_Actions_For_Query() throws Exception{
        final String luceneQuery = "-contentType:Host -baseType:3 +(conhost:48190c8c-42c4-46af-8d1a-0cd5db894797 conhost:SYSTEM_HOST) +languageId:1 +deleted:false +working:true";
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final BulkActionForm form = new BulkActionForm(null, luceneQuery);
        final Response response = workflowResource.getBulkActions(request, form);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        final ResponseEntityView entityView = ResponseEntityView.class.cast(response.getEntity());
        final BulkActionView wa = BulkActionView.class.cast(entityView.getEntity());
        assertNotNull(wa);
        final List<BulkWorkflowSchemeView> schemes = wa.getSchemes();
        assertNotNull(schemes);

        final Optional<BulkWorkflowSchemeView> documentManagementOptional = schemes.stream()
                .filter(bulkWorkflowSchemeView -> DM_WORKFLOW
                        .equals(bulkWorkflowSchemeView.getScheme().getName())).findFirst();
        final Optional<BulkWorkflowSchemeView> systemWorkflowOptional = schemes.stream()
                .filter(bulkWorkflowSchemeView -> SYSTEM_WORKFLOW
                        .equals(bulkWorkflowSchemeView.getScheme().getName())).findFirst();

        assertTrue(documentManagementOptional.isPresent());
        assertTrue(systemWorkflowOptional.isPresent());

        final BulkWorkflowSchemeView documentManagementScheme = documentManagementOptional.get();
        final List<WorkflowAction> documentActions = getAllWorkflowActions(documentManagementScheme);

        final Set<String> documentActionNames =
                workflowAPI.findActions(workflowAPI.findSchemeByName(DM_WORKFLOW), systemUser).stream().map(WorkflowAction::getName).collect(Collectors.toSet());

        documentActions.forEach(action -> {
            assertTrue(documentActionNames.contains(action.getName()));
        });

        final BulkWorkflowSchemeView systemWorkflowScheme = systemWorkflowOptional.get();
        final List<WorkflowAction> systemActions = getAllWorkflowActions(systemWorkflowScheme);

        final Set<String> systemActionNames =
                workflowAPI.findActions(workflowAPI.findSystemWorkflowScheme(), systemUser).stream().map(WorkflowAction::getName).collect(Collectors.toSet());

        systemActions.forEach(action -> {
            assertTrue(systemActionNames.contains(action.getName()));
        });
    }


    @Test
    public void Test_Get_Bulk_Actions_For_Contentlet_Ids() throws Exception {
        //This collects contents associated with a workflow.
        final Map<WorkflowScheme, Map<ContentType, List<Contentlet>>> sampleContent = collectSampleContent(10);
        final Set<WorkflowScheme> workflowSchemes = sampleContent.keySet();
        for (final WorkflowScheme scheme : workflowSchemes) {

            final List<WorkflowStep> steps = workflowAPI.findSteps(scheme);

            final Set<String> availableActions =
                    workflowAPI.findActions(steps, APILocator.systemUser()).stream().map(WorkflowAction::getId).collect(Collectors.toSet());

            final Map<ContentType, List<Contentlet>> contentsByType = sampleContent.get(scheme);
            final Set<ContentType> types = contentsByType.keySet();
            for (final ContentType contentType : types) {
                final List<Contentlet> contentlets = contentsByType.get(contentType);
                final List<String> contentletIds = contentlets.stream().map(Contentlet::getInode)
                        .collect(Collectors.toList());

                if(!UtilMethods.isSet(contentletIds)){
                    // for some reason no contetlets of the current type were found.
                   continue;
                }

                final HttpServletRequest request = mock(HttpServletRequest.class);
                final BulkActionForm form = new BulkActionForm(contentletIds, null);
                final Response response = workflowResource.getBulkActions(request, form);
                assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
                final ResponseEntityView entityView = ResponseEntityView.class
                        .cast(response.getEntity());
                final BulkActionView bulkActionView = BulkActionView.class.cast(entityView.getEntity());
                assertNotNull(bulkActionView);
                final List<BulkWorkflowSchemeView> schemes = bulkActionView.getSchemes();

                assertNotNull(schemes);
                if(Host.HOST_VELOCITY_VAR_NAME.equals(contentType.name())){

                    // if we're sending contentlets of type Host then we should get nothing back
                    assertTrue("Nothing should come back for CT Host ",schemes.isEmpty());

                } else {

                    assertFalse( "Everything else should have a WF. " , schemes.isEmpty());
                    // if the piece of content is associated with multiple WorkFlows we need to verify the response matches the one we're currently on.
                    boolean schemeMatches = false;
                    for (final BulkWorkflowSchemeView view : schemes) {
                        schemeMatches = (scheme.getId().equals(view.getScheme().getId()));
                        if (schemeMatches) {
                            assertFalse("Scheme is expected to have steps", view.getSteps().isEmpty());
                            assertEquals("Schema name does not match", scheme.getName(),
                                    view.getScheme().getName());
                            for (final BulkWorkflowStepView stepView : view.getSteps()) {
                                assertTrue("Expected step " + stepView.getStep().getWorkflowStep()
                                                .getName() + " Not found.",
                                        steps.contains(stepView.getStep().getWorkflowStep())
                                );
                                final List<CountWorkflowAction> workflowActions = stepView.getActions();
                                for (final CountWorkflowAction workflowAction : workflowActions) {
                                    assertTrue("Expected action " + workflowAction.getWorkflowAction()
                                                    .getName() + " Not found.",
                                            availableActions.contains(
                                                    workflowAction.getWorkflowAction().getId())
                                    );
                                }
                            }
                            break;
                        }
                    }

                    //At least one scheme was matched
                    assertTrue("no scheme was matched. ",schemeMatches);

                }
            }
        }
    }


    @Test
    public void Test_Fire_Bulk_Actions_For_Contentlet_Id() throws Exception {

        final Map<WorkflowScheme, Map<ContentType, List<Contentlet>>> sampleContent = collectSampleContent(3);
        final Set<WorkflowScheme> workflowSchemes = sampleContent.keySet();
        for (final WorkflowScheme scheme : workflowSchemes) {
            final Map<ContentType,List<Contentlet>> samples = sampleContent.get(scheme);
            for(final ContentType ct: samples.keySet()){
               final List<Contentlet> contentlets = samples.get(ct);
               if(UtilMethods.isSet(contentlets)){
                   final Contentlet contentlet = contentlets.get(0);
                   final List<WorkflowStep> steps = workflowAPI.findStepsByContentlet(contentlet);
                   final List<WorkflowAction> actions = workflowAPI.findActions(steps, APILocator.systemUser());

                   final WorkflowAction action = actions.stream().findAny().get();
                   final HttpServletRequest request = mock(HttpServletRequest.class);
                   final FireBulkActionsForm form = new FireBulkActionsForm(null,
                           Collections.singletonList(contentlet.getInode()), action.getId(), null
                   );

                   final AsyncResponse asyncResponse = new MockAsyncResponse((arg) -> {
                       final Response response = (Response) arg;
                       final int code = response.getStatus();
                       assertEquals(Status.OK.getStatusCode(), code);
                       final ResponseEntityView entityView = ResponseEntityView.class.cast(response.getEntity());
                       final BulkActionsResultView wa = BulkActionsResultView.class.cast(entityView.getEntity());
                       assertNotNull(wa);
                       return true;
                   }, o -> true);

                   Logger.info(getClass(), "Executing Action: '"+action.getName() + "' on contentlet: '" + contentlet.getName() + "'");
                   workflowResource.fireBulkActions(request, asyncResponse, form);
               }
            }
        }
    }

    private List<WorkflowAction> validateDocumentManagement(final BulkWorkflowSchemeView documentManagementScheme){

        final List<WorkflowAction> documentActions = getAllWorkflowActions(documentManagementScheme);

        //step 1 Document Management Actions.
        assertTrue(documentActions.stream()
                .anyMatch(action -> SAVE_AS_DRAFT.equals(action.getName())));
        assertTrue(documentActions.stream()
                .anyMatch(action -> SEND_FOR_REVIEW.equals(action.getName())));
        assertTrue(documentActions.stream()
                .anyMatch(action -> SEND_TO_LEGAL.equals(action.getName())));
        assertTrue(documentActions.stream().anyMatch(action -> PUBLISH.equals(action.getName())));

        return documentActions;
    }

    private List<WorkflowAction> validateSystemWorkflow(final BulkWorkflowSchemeView systemWorkflowScheme){

        final List<WorkflowAction> systemActions = getAllWorkflowActions(systemWorkflowScheme);

        //step 1 System Workflow Actions.
        assertTrue(systemActions.stream().anyMatch(action -> SAVE.equals(action.getName())));
        assertTrue(systemActions.stream()
               .anyMatch(action -> SAVE_PUBLISH.equals(action.getName())));
        return systemActions;
    }


    /**
     * Thread sleep that allows a few seconds for the index to finish indexing.
     * @throws Exception
     */
    private void indexNeedsToCatchup() {
        try {
            TimeUnit.SECONDS.sleep(8); // Wait for the new content to be indexed.
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     *
     * @param contentlet
     * @return
     * @throws Exception
     */
    private Contentlet findLatestWorkingContentlet(final Contentlet contentlet) {

        try {
            return contentletAPI.findContentletByIdentifier(
                    contentlet.getIdentifier(),false, contentlet.getLanguageId(), systemUser, false
            );
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Creates a custom contentType with a required field
     * @return
     * @throws Exception
     */
    private ContentType createSampleContentType() throws Exception{
        ContentType contentType;
        final String ctPrefix = "TestContentType";
        final String newContentTypeName = ctPrefix + System.currentTimeMillis();

        // Create ContentType
        contentType = createContentTypeAndAssignPermissions(newContentTypeName,
                BaseContentType.CONTENT, PermissionAPI.PERMISSION_READ, adminRole.getId());
        final WorkflowScheme systemWorkflow = workflowAPI.findSystemWorkflowScheme();
        final WorkflowScheme documentWorkflow = workflowAPI
                .findSchemeByName(DM_WORKFLOW);

        // Add fields to the contentType
        final Field field =
                FieldBuilder.builder(TextField.class).name(REQUIRED_TEXT_FIELD_NAME).variable(REQUIRED_TEXT_FIELD_NAME)
                        .required(true)
                        .contentTypeId(contentType.id()).dataType(DataTypes.TEXT).build();
        contentType = contentTypeAPI.save(contentType, Collections.singletonList(field));

        // Assign contentType to Workflows
        workflowAPI.saveSchemeIdsForContentType(contentType,
                Arrays.asList(
                        systemWorkflow.getId(), documentWorkflow.getId()
                )
        );

        return contentType;
    }

    /**
     * Creates a contentlet based on the content type passed
     * @param contentType
     * @return
     * @throws Exception
     */
    private Contentlet createSampleContent(final ContentType contentType) throws Exception{
        Contentlet contentlet = null;
        // Create a content sample
        contentlet = new Contentlet();
        // instruct the content with its own type
        contentlet.setStructureInode(contentType.inode());
        contentlet.setHost(host.getIdentifier());
        contentlet.setLanguageId(languageAPI.getDefaultLanguage().getId());

        contentlet.setStringProperty(REQUIRED_TEXT_FIELD_NAME,"anyValue");
        contentlet.setIndexPolicy(IndexPolicy.FORCE);

        // Save the content
        contentlet = contentletAPI.checkin(contentlet, systemUser, false);
        assertNotNull(contentlet.getInode());

        indexNeedsToCatchup();

        assertTrue(contentletAPI.isInodeIndexed(contentlet.getInode()));
        return contentlet;
    }

    @Test
    public void Test_Find_Bulk_Actions_Then_Fire_Bulk_Actions_On_Custom_Content_Type_Then_Verify_Workflow_Changed()
            throws Exception {

        // Prep Workflows, they must have at least one action visible on the first step.
        final WorkflowScheme sysWorkflow = workflowAPI.findSchemeByName(SYSTEM_WORKFLOW);
        final List<WorkflowStep> sysSteps = workflowAPI.findSteps(sysWorkflow);
        final Optional<WorkflowStep> newStep = sysSteps.stream()
                .filter(workflowStep -> "New".equals(workflowStep.getName())).findFirst();
        assertTrue(newStep.isPresent());
        final Map<String, Set<WorkflowState>> sysWorkflowShowOn = new HashMap<>();

        final List<WorkflowAction> sysWorkflowActions = workflowAPI
                .findActions(newStep.get(), systemUser);
        for (final WorkflowAction action : sysWorkflowActions) {
            sysWorkflowShowOn
                    .computeIfAbsent(action.getId(), s -> new HashSet<>(action.getShowOn()));
            action.setShowOn(WorkflowState.LISTING);
            workflowAPI.saveAction(action, null, adminUser);
        }

        final WorkflowScheme dmScheme = workflowAPI.findSchemeByName(DM_WORKFLOW);
        final List<WorkflowStep> docWorkflowSteps = workflowAPI.findSteps(dmScheme);
        final Optional<WorkflowStep> editingStep = docWorkflowSteps.stream()
                .filter(workflowStep -> "Editing".equals(workflowStep.getName())).findFirst();
        assertTrue(editingStep.isPresent());
        final Map<String, Set<WorkflowState>> docWorkflowShowOn = new HashMap<>();

        final List<WorkflowAction> dmWorkflowActions = workflowAPI
                .findActions(editingStep.get(), systemUser);
        for (final WorkflowAction action : dmWorkflowActions) {
            docWorkflowShowOn
                    .computeIfAbsent(action.getId(), s -> new HashSet<>(action.getShowOn()));
            action.setShowOn(WorkflowState.LISTING);
            workflowAPI.saveAction(action, null, adminUser);
        }

        try {

            // We create a contentType that is associated with the two workflows that come out of the box.
            final ContentType contentType = createSampleContentType();
            //Then we create an instance
            final Contentlet contentlet = createSampleContent(contentType);
            final String inode = contentlet.getInode();

            try {
                //  Now Test BulkActions
                final BulkActionForm form1 = new BulkActionForm(
                        Collections.singletonList(inode), null
                );
                final HttpServletRequest request = mock(HttpServletRequest.class);
                final Response bulkActionsResponse = workflowResource
                        .getBulkActions(request, form1);
                assertEquals(Response.Status.OK.getStatusCode(), bulkActionsResponse.getStatus());

                final ResponseEntityView beforeFireEntityView = ResponseEntityView.class
                        .cast(bulkActionsResponse.getEntity());
                final BulkActionView bulkActionView = BulkActionView.class
                        .cast(beforeFireEntityView.getEntity());
                assertNotNull(bulkActionView);
                final List<BulkWorkflowSchemeView> schemes1 = bulkActionView.getSchemes();

                final Optional<BulkWorkflowSchemeView> documentManagementOptional1 = schemes1
                        .stream()
                        .filter(bulkWorkflowSchemeView -> DM_WORKFLOW
                                .equals(bulkWorkflowSchemeView.getScheme().getName())).findFirst();

                final Optional<BulkWorkflowSchemeView> systemWorkflowOptional1 = schemes1.stream()
                        .filter(bulkWorkflowSchemeView -> SYSTEM_WORKFLOW
                                .equals(bulkWorkflowSchemeView.getScheme().getName())).findFirst();

                assertTrue(documentManagementOptional1.isPresent());
                assertTrue(systemWorkflowOptional1.isPresent());

                //Validate we have every possible action within the workflows.
                final BulkWorkflowSchemeView documentManagementScheme = documentManagementOptional1
                        .get();
                // Workflow has to have All available first steps for the current assigned WF.
                final List<WorkflowAction> documentManagementAction = validateDocumentManagement(
                        documentManagementScheme);

                // System Actions
                final BulkWorkflowSchemeView systemWorkflowScheme = systemWorkflowOptional1.get();
                final List<WorkflowAction> systemWorkflowActions = validateSystemWorkflow(
                        systemWorkflowScheme
                );

                //Now I should be able to choose from the two set of actions and start an execution path.

                final WorkflowAction saveAction = systemWorkflowActions.stream()
                        .filter(action -> SAVE.equals(action.getName())).findFirst().get();

                final HttpServletRequest request1 = mock(HttpServletRequest.class);
                final FireBulkActionsForm actionsForm1 = new FireBulkActionsForm(null,
                        Collections.singletonList(inode), saveAction.getId(), null);

                //This CountDownLatch prevents the finally block from getting reached until after the thread completes
                final CountDownLatch countDownLatch = new CountDownLatch(1);

                final AsyncResponse asyncResponse = new MockAsyncResponse((arg) -> {
                    try {
                        final Response response = (Response) arg;
                        final int code = response.getStatus();
                        assertEquals(Status.OK.getStatusCode(), code);
                        final ResponseEntityView fireEntityView = ResponseEntityView.class
                                .cast(response.getEntity());

                        final BulkActionsResultView bulkActionsResultView = BulkActionsResultView.class
                                .cast(fireEntityView.getEntity());
                        assertNotNull(bulkActionsResultView);

                        assertEquals(1, bulkActionsResultView.getSuccessCount().intValue());
                        assertEquals(0, bulkActionsResultView.getFails().size());
                        assertEquals(0, bulkActionsResultView.getSkippedCount().intValue());

                        indexNeedsToCatchup();

                        // If we try to find available actions for a  contentlet on which we have fired an action successfully we shouldn't get anything.
                        final HttpServletRequest request2 = mock(HttpServletRequest.class);
                        final BulkActionForm form2 = new BulkActionForm(
                                Collections.singletonList(inode), null
                        );

                        final Response response2 = workflowResource.getBulkActions(request2, form2);
                        assertEquals(Response.Status.OK.getStatusCode(), response2.getStatus());
                        final ResponseEntityView afterFireEntityView = ResponseEntityView.class
                                .cast(response2.getEntity());
                        final BulkActionView bulkActionView2 = BulkActionView.class
                                .cast(afterFireEntityView.getEntity());
                        assertNotNull(bulkActionView2);
                        final List<BulkWorkflowSchemeView> schemes2 = bulkActionView2.getSchemes();
                        assertTrue(schemes2.isEmpty());

                        //We need to get the latest working inode. Since it has changed after we fired an action successfully.
                        final Contentlet contentletAfterActionApplied = findLatestWorkingContentlet(
                                contentlet);

                        final HttpServletRequest request3 = mock(HttpServletRequest.class);
                        // If we try to find available actions for the latest inode then we should still see the workflow that owns the action we fired on this contentlet. But not the other ones.
                        final BulkActionForm bulkActionFormAfterSave = new BulkActionForm(
                                Collections.singletonList(contentletAfterActionApplied.getInode()),
                                null
                        );

                        final Response response3 = workflowResource
                                .getBulkActions(request3, bulkActionFormAfterSave);
                        assertEquals(Response.Status.OK.getStatusCode(), response3.getStatus());
                        final ResponseEntityView afterFireEntityViewNewInode = ResponseEntityView.class
                                .cast(response3.getEntity());
                        final BulkActionView bulkActionView3 = BulkActionView.class
                                .cast(afterFireEntityViewNewInode.getEntity());
                        assertNotNull(bulkActionView3);
                        final List<BulkWorkflowSchemeView> schemes3 = bulkActionView3.getSchemes();
                        assertFalse(schemes3.isEmpty());

                        final Optional<BulkWorkflowSchemeView> documentManagementOptional2 = schemes3
                                .stream()
                                .filter(bulkWorkflowSchemeView -> DM_WORKFLOW
                                        .equals(bulkWorkflowSchemeView.getScheme().getName()))
                                .findFirst();

                        final Optional<BulkWorkflowSchemeView> systemWorkflowOptional2 = schemes3
                                .stream()
                                .filter(bulkWorkflowSchemeView -> SYSTEM_WORKFLOW
                                        .equals(bulkWorkflowSchemeView.getScheme().getName()))
                                .findFirst();

                        assertTrue(systemWorkflowOptional2.isPresent());
                        assertFalse(documentManagementOptional2.isPresent());
                    } finally {
                        countDownLatch.countDown();
                    }
                    return true;
                }, o -> true);

                workflowResource.fireBulkActions(request1, asyncResponse, actionsForm1);
                countDownLatch.await(60, TimeUnit.SECONDS);

            } finally {

                if (contentlet != null) {
                    contentletAPI.archive(contentlet, systemUser, false);
                    contentletAPI.delete(contentlet, systemUser, false);
                }
                if (contentType != null) {
                    contentTypeAPI.delete(contentType);
                }
            }

        } finally {
            // Restore workflows to their original condition
            for (final WorkflowAction action : sysWorkflowActions) {
                action.setShowOn(sysWorkflowShowOn.get(action.getId()));
                workflowAPI.saveAction(action, null, adminUser);
            }

            for (final WorkflowAction action : dmWorkflowActions) {
                action.setShowOn(docWorkflowShowOn.get(action.getId()));
                workflowAPI.saveAction(action, null, adminUser);
            }
        }
    }

    @Test
    public void Test_Find_Bulk_Actions_Then_Fire_Bulk_Actions_On_Custom_Content_Type_with_Errors_Then_Verify_Workflow_Did_Not_Changed()
            throws Exception {

        // Prep Workflows, they must have at least one action visible on the first step.
        final WorkflowScheme sysWorkflow = workflowAPI.findSchemeByName(SYSTEM_WORKFLOW);
        final List<WorkflowStep> sysSteps = workflowAPI.findSteps(sysWorkflow);
        final Optional<WorkflowStep> newStep = sysSteps.stream()
                .filter(workflowStep -> "New".equals(workflowStep.getName())).findFirst();
        assertTrue(newStep.isPresent());
        final Map<String, Set<WorkflowState>> sysWorkflowShowOn = new HashMap<>();

        final List<WorkflowAction> sysWorkflowActions = workflowAPI
                .findActions(newStep.get(), systemUser);
        for (final WorkflowAction action : sysWorkflowActions) {
            sysWorkflowShowOn
                    .computeIfAbsent(action.getId(), s -> new HashSet<>(action.getShowOn()));
            action.setShowOn(WorkflowState.LISTING);
            workflowAPI.saveAction(action, null, adminUser);
        }

        final WorkflowScheme dmScheme = workflowAPI.findSchemeByName(DM_WORKFLOW);
        final List<WorkflowStep> docWorkflowSteps = workflowAPI.findSteps(dmScheme);
        final Optional<WorkflowStep> editingStep = docWorkflowSteps.stream()
                .filter(workflowStep -> "Editing".equals(workflowStep.getName())).findFirst();
        assertTrue(editingStep.isPresent());
        final Map<String, Set<WorkflowState>> docWorkflowShowOn = new HashMap<>();

        final List<WorkflowAction> dmWorkflowActions = workflowAPI
                .findActions(editingStep.get(), systemUser);
        for (final WorkflowAction action : dmWorkflowActions) {
            docWorkflowShowOn
                    .computeIfAbsent(action.getId(), s -> new HashSet<>(action.getShowOn()));
            action.setShowOn(WorkflowState.LISTING);
            workflowAPI.saveAction(action, null, adminUser);
        }

        try {

            //Hand picked Form-like Widgets with a mandatory title field
            final Contentlet candidate1 = contentletAPI.findContentletByIdentifier(
                    "b4ae5fd4-c4c7-4590-8299-00569a9f13be", false, 1, systemUser, false
            );

            final Contentlet candidate2 = contentletAPI.findContentletByIdentifier(
                    "2f180f39-59c3-4225-9cca-5daf778f3f3e", false, 1, systemUser, false
            );

            final List<Contentlet> contentlets = Arrays.asList(candidate1, candidate2);

            final CountDownLatch countDownLatch = new CountDownLatch(contentlets.size());

            for (final Contentlet contentlet : contentlets) {

                final String inode = contentlet.getInode();

                //  Now Test BulkActions
                final BulkActionForm form1 = new BulkActionForm(
                        Collections.singletonList(inode), null
                );
                final HttpServletRequest request = mock(HttpServletRequest.class);
                final Response findActionsResponse = workflowResource
                        .getBulkActions(request, form1);
                assertEquals(Response.Status.OK.getStatusCode(), findActionsResponse.getStatus());

                final ResponseEntityView beforeFireEntityView = ResponseEntityView.class
                        .cast(findActionsResponse.getEntity());
                final BulkActionView bulkActionView = BulkActionView.class
                        .cast(beforeFireEntityView.getEntity());
                assertNotNull(bulkActionView);
                final List<BulkWorkflowSchemeView> schemes1 = bulkActionView.getSchemes();

                final Optional<BulkWorkflowSchemeView> documentManagementOptional1 = schemes1
                        .stream()
                        .filter(bulkWorkflowSchemeView -> DM_WORKFLOW
                                .equals(bulkWorkflowSchemeView.getScheme().getName())).findFirst();

                final Optional<BulkWorkflowSchemeView> systemWorkflowOptional1 = schemes1.stream()
                        .filter(bulkWorkflowSchemeView -> SYSTEM_WORKFLOW
                                .equals(bulkWorkflowSchemeView.getScheme().getName())).findFirst();

                assertFalse(documentManagementOptional1.isPresent());
                assertTrue(systemWorkflowOptional1.isPresent());

                //Validate and get System WF Actions.
                final BulkWorkflowSchemeView systemWorkflowScheme = systemWorkflowOptional1.get();
                final List<WorkflowAction> systemWorkflowActions = validateSystemWorkflow(
                        systemWorkflowScheme
                );

                //Now I should be able to fire Save.

                final WorkflowAction saveAction = systemWorkflowActions.stream()
                        .filter(action -> SAVE.equals(action.getName())).findFirst().get();

                final HttpServletRequest request1 = mock(HttpServletRequest.class);
                final FireBulkActionsForm actionsForm1 = new FireBulkActionsForm(null,
                        Collections.singletonList(inode), saveAction.getId(), null);

                final AsyncResponse asyncResponse = new MockAsyncResponse((arg) -> {
                    try {
                        final Response response = (Response) arg;
                        final int code = response.getStatus();
                        assertEquals(Status.OK.getStatusCode(), code);
                        final ResponseEntityView fireEntityView = ResponseEntityView.class
                                .cast(response.getEntity());

                        final BulkActionsResultView bulkActionsResultView = BulkActionsResultView.class
                                .cast(fireEntityView.getEntity());
                        assertNotNull(bulkActionsResultView);

                        //This CTs We chose should  have a required field. And a failure is expected.

                        assertEquals(0, bulkActionsResultView.getSuccessCount().intValue());
                        assertEquals(1, bulkActionsResultView.getFails().size());
                        assertEquals(0, bulkActionsResultView.getSkippedCount().intValue());

                        //  Now Test BulkActions
                        final BulkActionForm form2 = new BulkActionForm(
                                Collections.singletonList(inode), null
                        );
                        final HttpServletRequest request2 = mock(HttpServletRequest.class);
                        final Response response2 = workflowResource.getBulkActions(request2, form2);
                        assertEquals(Response.Status.OK.getStatusCode(), response2.getStatus());

                        final ResponseEntityView afterFireEntityView = ResponseEntityView.class
                                .cast(response2.getEntity());
                        final BulkActionView bulkActionView2 = BulkActionView.class
                                .cast(afterFireEntityView.getEntity());
                        assertNotNull(bulkActionView2);
                        final List<BulkWorkflowSchemeView> schemes2 = bulkActionView2.getSchemes();

                        final Optional<BulkWorkflowSchemeView> documentManagementOptional2 = schemes2
                                .stream()
                                .filter(bulkWorkflowSchemeView -> DM_WORKFLOW
                                        .equals(bulkWorkflowSchemeView.getScheme().getName()))
                                .findFirst();

                        final Optional<BulkWorkflowSchemeView> systemWorkflowOptional2 = schemes2
                                .stream()
                                .filter(bulkWorkflowSchemeView -> SYSTEM_WORKFLOW
                                        .equals(bulkWorkflowSchemeView.getScheme().getName()))
                                .findFirst();

                        assertFalse(documentManagementOptional2.isPresent());
                        assertTrue(systemWorkflowOptional2.isPresent());
                    } finally {
                        countDownLatch.countDown();
                    }
                    return true;
                }, o -> true);

                workflowResource.fireBulkActions(request1, asyncResponse, actionsForm1);

            }

            countDownLatch.await(60, TimeUnit.SECONDS);

        } finally {
            // Restore workflows to their original condition
            for (final WorkflowAction action : sysWorkflowActions) {
                action.setShowOn(sysWorkflowShowOn.get(action.getId()));
                workflowAPI.saveAction(action, null, adminUser);
            }

            for (final WorkflowAction action : dmWorkflowActions) {
                action.setShowOn(docWorkflowShowOn.get(action.getId()));
                workflowAPI.saveAction(action, null, adminUser);
            }

        }

    }


    @Test
    public void Test_Fire_Save_Instance_Then_Fire_Update_Instance() throws Exception {
        final String saveAndPublishActionId = "b9d89c80-3d88-4311-8365-187323c96436";
        ContentType contentType = null;
        try {
            // We create a contentType that is associated with the two workflows that comes out of the box.
            contentType = createSampleContentType();
            Contentlet brandNewContentlet = null;
             try {
                 //Save Action
                 final FireActionForm.Builder builder1 = new FireActionForm.Builder();
                 final Map <String,Object>contentletFormData = new HashMap<>();
                 contentletFormData.put("stInode", contentType.inode());
                 contentletFormData.put(REQUIRED_TEXT_FIELD_NAME, "value-1");
                 builder1.contentletFormData(contentletFormData);

                final FireActionForm fireActionForm1 = new FireActionForm(builder1);
                final HttpServletRequest request1 = mock(HttpServletRequest.class);
                final Response response1 = workflowResource
                        .fireAction(request1, null, saveAndPublishActionId, fireActionForm1);

                final int statusCode1 = response1.getStatus();
                assertEquals(Status.OK.getStatusCode(), statusCode1);
                final ResponseEntityView fireEntityView1 = ResponseEntityView.class
                        .cast(response1.getEntity());
                brandNewContentlet = Contentlet.class.cast(fireEntityView1.getEntity());
                assertNotNull(brandNewContentlet);
                assertEquals("value-1", brandNewContentlet.getMap().get(REQUIRED_TEXT_FIELD_NAME));

                 //Update Action
                final FireActionForm.Builder builder2 = new FireActionForm.Builder();
                final Map <String,Object>contentletFormData2 = new HashMap<>();
                contentletFormData2.put("stInode", contentType.inode());
                contentletFormData2.put(REQUIRED_TEXT_FIELD_NAME, "value-2");
                builder2.contentletFormData(contentletFormData2);

                final FireActionForm fireActionForm2 = new FireActionForm(builder2);
                final HttpServletRequest request2 = mock(HttpServletRequest.class);
                final Response response2 = workflowResource
                     .fireAction(request2, brandNewContentlet.getInode(), saveAndPublishActionId, fireActionForm2);

                final int statusCode2 = response2.getStatus();
                assertEquals(Status.OK.getStatusCode(), statusCode2);
                final ResponseEntityView fireEntityView2 = ResponseEntityView.class
                         .cast(response2.getEntity());
                final Contentlet updatedContentlet = Contentlet.class.cast(fireEntityView2.getEntity());
                assertNotNull(updatedContentlet);

                assertEquals(brandNewContentlet.getIdentifier(),updatedContentlet.getIdentifier());
                assertEquals("value-2", updatedContentlet.getMap().get(REQUIRED_TEXT_FIELD_NAME));

             }finally {
                if(null != brandNewContentlet){
                     contentletAPI.archive(brandNewContentlet, APILocator.systemUser(), false);
                     contentletAPI.delete(brandNewContentlet, APILocator.systemUser(), false);
                }
            }

        }finally {
            if(null != contentType){
              contentTypeAPI.delete(contentType);
            }
        }
    }

    static ImmutableMap <Class, DataTypes> fieldTypesMetaDataMap = new ImmutableMap.Builder<Class, DataTypes>()
            //   .put(BinaryField.class, DataTypes.SYSTEM)
            //   .put(CategoryField.class, DataTypes.SYSTEM)
                .put(CheckboxField.class, DataTypes.TEXT)
                .put(CustomField.class, DataTypes.LONG_TEXT)
                .put(DateField.class, DataTypes.DATE)
                .put(DateTimeField.class, DataTypes.DATE)
            //   .put(FileField.class, DataTypes.TEXT)
            //   .put(HiddenField.class, DataTypes.SYSTEM)
            //   .put(HostFolderField.class,DataTypes.SYSTEM)
                .put(ImageField.class,DataTypes.TEXT)
                .put(KeyValueField.class,DataTypes.LONG_TEXT)
            //   .put(LineDividerField.class,DataTypes.SYSTEM)
                .put(MultiSelectField.class,DataTypes.LONG_TEXT)
            //   .put(PermissionTabField.class,DataTypes.SYSTEM)
                .put(RadioField.class,DataTypes.TEXT)
            //   .put(RelationshipField.class,DataTypes.SYSTEM)
            //   .put(RelationshipsTabField.class,  DataTypes.SYSTEM)
                .put(SelectField.class, DataTypes.TEXT)
            //   .put(TabDividerField.class, DataTypes.SYSTEM)
            //   .put(TagField.class, DataTypes.SYSTEM)
                .put(TextField.class, DataTypes.TEXT)
                .put(TextAreaField.class, DataTypes.LONG_TEXT)
                .put(TimeField.class, DataTypes.DATE)
                .put(WysiwygField.class, DataTypes.LONG_TEXT)
                .build();


    private static final String SAVE_ACTION_ID = "ceca71a0-deee-4999-bd47-b01baa1bcfc8";
    private static final String NON_REQUIRED_IMAGE_FIELD_NAME = "nonRequiredImageField";
    private static final String NON_REQUIRED_IMAGE_VALUE= "/path/to/the/image/random.jpg";

    private static final String REQUIRED_TEXT_FIELD_NAME = "requiredTextField";
    private static final String REQUIRED_TEXT_FIELD_VALUE= "This Value is Required";

    private static final String NON_REQUIRED_TEXT_FIELD_NAME = "nonRequiredTextField";
    private static final String NON_REQUIRED_TEXT_FIELD_VALUE= "This Value isn't required";

    private static final String REQUIRED_NUMERIC_TEXT_FIELD_NAME = "requiredNumericTextField";
    private static final String REQUIRED_NUMERIC_TEXT_FIELD_VALUE= "0";

    private static final String NON_REQUIRED_NUMERIC_TEXT_FIELD_NAME = "nonRequiredNumericTextField";
    private static final String NON_REQUIRED_NUMERIC_TEXT_FIELD_VALUE= "0";

    @Test
    public void Test_Fire_Save_Remove_Image_Then_Verify_Fields_Were_Cleared_Issue_15340() throws Exception {

        final User sysUser = APILocator.systemUser();
        final FieldAPI fieldAPI = APILocator.getContentTypeFieldAPI();

        ContentType contentType = null;
        try {
            // We create a contentType that is associated with the two workflows that come out of the box.
            contentType = createSampleContentType();
            Contentlet brandNewContentlet = null;
            try {

                //Lets add even more fields to the contentType.
                Field imageField =
                        FieldBuilder.builder(ImageField.class).name(NON_REQUIRED_IMAGE_FIELD_NAME).variable(NON_REQUIRED_IMAGE_FIELD_NAME)
                                .required(false)
                                .contentTypeId(contentType.id()).dataType(DataTypes.TEXT).build();

                imageField = fieldAPI.save(imageField, sysUser);
                final List<Field> fields = Stream.concat (
                        Stream.of(imageField),contentType.fields().stream()).collect(
                                CollectionsUtils.toImmutableList()
                        );

                contentType = contentTypeAPI.save(contentType, fields);

                //Save Action (Creates the initial content)
                final FireActionForm.Builder builder1 = new FireActionForm.Builder();
                final Map <String,Object>contentletFormData = new HashMap<>();
                contentletFormData.put("stInode", contentType.inode());
                contentletFormData.put(REQUIRED_TEXT_FIELD_NAME, REQUIRED_TEXT_FIELD_VALUE);
                contentletFormData.put(NON_REQUIRED_IMAGE_FIELD_NAME, NON_REQUIRED_IMAGE_VALUE);
                builder1.contentletFormData(contentletFormData);

                final FireActionForm fireActionForm1 = new FireActionForm(builder1);
                final HttpServletRequest request1 = mock(HttpServletRequest.class);
                final Response response1 = workflowResource
                        .fireAction(request1, null, SAVE_ACTION_ID, fireActionForm1);

                final int statusCode1 = response1.getStatus();
                assertEquals(Status.OK.getStatusCode(), statusCode1);
                final ResponseEntityView fireEntityView1 = ResponseEntityView.class
                        .cast(response1.getEntity());
                brandNewContentlet = Contentlet.class.cast(fireEntityView1.getEntity());
                assertNotNull(brandNewContentlet);
                assertEquals(REQUIRED_TEXT_FIELD_VALUE, brandNewContentlet.getMap().get(REQUIRED_TEXT_FIELD_NAME));
                assertEquals(NON_REQUIRED_IMAGE_VALUE, brandNewContentlet.getMap().get(NON_REQUIRED_IMAGE_FIELD_NAME));

                //Save Action (Creates the initial content)
                final FireActionForm.Builder builder2 = new FireActionForm.Builder();
                final Map <String,Object>contentletFormData2 = new HashMap<>();
                contentletFormData2.put("stInode", contentType.inode());
                // We're not sending this property here so we can verify it comes back without any modifications.
                //contentletFormData.put("requiredField", "value-1");
                //W're just marking the image null so we can get it removed.
                contentletFormData2.put(NON_REQUIRED_IMAGE_FIELD_NAME, null);
                builder2.contentletFormData(contentletFormData2);

                final FireActionForm fireActionForm2 = new FireActionForm(builder2);
                final HttpServletRequest request2 = mock(HttpServletRequest.class);
                final Response response2 = workflowResource
                        .fireAction(request2, brandNewContentlet.getInode(), SAVE_ACTION_ID, fireActionForm2);

                final int statusCode2 = response2.getStatus();
                assertEquals(Status.OK.getStatusCode(), statusCode2);
                final ResponseEntityView fireEntityView2 = ResponseEntityView.class
                        .cast(response2.getEntity());
                Contentlet fetchedContentlet = Contentlet.class.cast(fireEntityView2.getEntity());
                assertNotNull(fetchedContentlet);
                assertEquals(REQUIRED_TEXT_FIELD_VALUE, fetchedContentlet.getMap().get(REQUIRED_TEXT_FIELD_NAME));

                assertNull(fetchedContentlet.getMap().get(NON_REQUIRED_IMAGE_FIELD_NAME)); // Image.. still should be gone!

                final Contentlet found = APILocator.getContentletAPI().find(fetchedContentlet.getInode(), sysUser,false);
                assertNotNull(found);

                assertEquals(REQUIRED_TEXT_FIELD_VALUE, found.getMap().get(REQUIRED_TEXT_FIELD_NAME));
                assertNull(found.getMap().get(NON_REQUIRED_IMAGE_FIELD_NAME)); // Image.. still should be gone!
                //if we send null we should get back null.

            }finally {
                if(null != brandNewContentlet){
                    contentletAPI.archive(brandNewContentlet, APILocator.systemUser(), false);
                    contentletAPI.delete(brandNewContentlet, APILocator.systemUser(), false);
                }
            }

        }finally {
            if(null != contentType){
                contentTypeAPI.delete(contentType);
            }
        }
    }


    @Test
    public void Test_Set_Value_on_All_NonRequired_Fields_Then_Fire_Save_Set_Null_Then_Verify_Fields_Were_Cleared_Issue_15340()
            throws Exception {

        ContentType contentType = null;
        try {
            // We create a contentType that is associated with the two workflows that come out of the box.
            contentType = createLargeContentType(false);
            Contentlet brandNewContentlet = null;

            //Save Action (Creates the initial content)
            final FireActionForm.Builder builder1 = new FireActionForm.Builder();
            final Map <String,Object>contentletFormData = new HashMap<>();
            contentletFormData.put("stInode", contentType.inode());
            for(final Field field : contentType.fields()){
                final Object value = generateValue(field);
                if( null != value){
                    contentletFormData.put(field.name(), value);
                }
            }
            builder1.contentletFormData(contentletFormData);
            final FireActionForm fireActionForm1 = new FireActionForm(builder1);
            final HttpServletRequest request1 = mock(HttpServletRequest.class);
            final Response response1 = workflowResource
                    .fireAction(request1, null, SAVE_ACTION_ID, fireActionForm1);

            final int statusCode1 = response1.getStatus();
            assertEquals(Status.OK.getStatusCode(), statusCode1);
            final ResponseEntityView fireEntityView1 = ResponseEntityView.class
                    .cast(response1.getEntity());
            brandNewContentlet = Contentlet.class.cast(fireEntityView1.getEntity());
            assertNotNull(brandNewContentlet);

            for(final Field field : contentType.fields()){
                assertNotNull(brandNewContentlet.get(field.name()));
            }

            final FireActionForm.Builder builder2 = new FireActionForm.Builder();
            final Map <String,Object>contentletFormData2 = new HashMap<>();
            contentletFormData2.put("stInode", contentType.inode());
            for(final Field field : contentType.fields()){
                contentletFormData2.put(field.name(), null);
            }
            builder2.contentletFormData(contentletFormData2);
            final FireActionForm fireActionForm2 = new FireActionForm(builder2);
            final HttpServletRequest request2 = mock(HttpServletRequest.class);
            final Response response2 = workflowResource
                    .fireAction(request2, brandNewContentlet.getInode(), SAVE_ACTION_ID, fireActionForm2);

            final int statusCode2 = response2.getStatus();
            assertEquals(Status.OK.getStatusCode(), statusCode2);

            final ResponseEntityView fireEntityView2 = ResponseEntityView.class
                    .cast(response2.getEntity());
            final Contentlet fetchedContentlet = Contentlet.class.cast(fireEntityView2.getEntity());
            assertNotNull(fetchedContentlet);

            for(final Field field : contentType.fields()){
                assertNull(fetchedContentlet.get(field.name()));
            }

        } finally {
            if (null != contentType) {
                contentTypeAPI.delete(contentType);
            }
        }

    }


    /**
     * This Test demostrates that the workflow resource is capable of performing an update on a subset of fields.
     * @throws Exception
     */
    @Test
    public void Test_Create_Instance_Of_Content_With_Mandatory_Fields_Then_Send_Partial_Number_Of_Required_Fields_Issue_15340()
        throws Exception {
        ContentType contentType = null;
        try {
            // This CT has a mix of required and non-required fields.
            contentType = createMixedRequiredAndNonRequiredFieldsContentType();
            Contentlet brandNewContentlet = null;

            //Save Action (Creates the initial content)
            final FireActionForm.Builder builder1 = new FireActionForm.Builder();
            final Map <String,Object>contentletFormData = new HashMap<>();
            contentletFormData.put("stInode", contentType.inode());
            contentletFormData.put(REQUIRED_TEXT_FIELD_NAME, REQUIRED_TEXT_FIELD_VALUE);
            contentletFormData.put(NON_REQUIRED_TEXT_FIELD_NAME, NON_REQUIRED_TEXT_FIELD_VALUE);

            builder1.contentletFormData(contentletFormData);
            final FireActionForm fireActionForm1 = new FireActionForm(builder1);
            final HttpServletRequest request1 = mock(HttpServletRequest.class);
            final Response response1 = workflowResource
                    .fireAction(request1, null, SAVE_ACTION_ID, fireActionForm1);

            final int statusCode1 = response1.getStatus();
            assertEquals(Status.OK.getStatusCode(), statusCode1);
            final ResponseEntityView fireEntityView1 = ResponseEntityView.class
                    .cast(response1.getEntity());
            brandNewContentlet = Contentlet.class.cast(fireEntityView1.getEntity());
            assertNotNull(brandNewContentlet);

            //Once the content has been created lets send another request with half the fields

            final FireActionForm.Builder builder2 = new FireActionForm.Builder();

            final String NON_REQUIRED_UPDATED_VALUE = "Non required updated value.";

            final Map <String,Object>contentletFormData2 = new HashMap<>();
            contentletFormData2.put("stInode", contentType.inode());
            contentletFormData2.put(NON_REQUIRED_TEXT_FIELD_NAME, NON_REQUIRED_UPDATED_VALUE);
            builder2.contentletFormData(contentletFormData2);

            final FireActionForm fireActionForm2 = new FireActionForm(builder2);
            final HttpServletRequest request2 = mock(HttpServletRequest.class);
            final Response response2 = workflowResource
                    .fireAction(request2, brandNewContentlet.getInode(), SAVE_ACTION_ID, fireActionForm2);

            final int statusCode2 = response2.getStatus();
            assertEquals(Status.OK.getStatusCode(), statusCode2);

            //Now we need to test sending only required fields and not the nonRequired see if they get affected!!!

            final ResponseEntityView fireEntityView2 = ResponseEntityView.class
                    .cast(response2.getEntity());
            final Contentlet fetchedContentlet = Contentlet.class.cast(fireEntityView2.getEntity());
            assertNotNull(fetchedContentlet);

            //This one got changed. It is expected since we are using the resource to modify its value.
            assertEquals(NON_REQUIRED_UPDATED_VALUE, fetchedContentlet.getMap().get(NON_REQUIRED_TEXT_FIELD_NAME));
            //This one should remain unchanged. since it never got send on the form.
            assertEquals(REQUIRED_TEXT_FIELD_VALUE, fetchedContentlet.getMap().get(REQUIRED_TEXT_FIELD_NAME));


            //Meaning the endpoint is flexible to modify a subset of fields. No need to send them all.
        } finally {
            if (null != contentType) {
                contentTypeAPI.delete(contentType);
            }
        }
    }

    @Test
    public void Test_Create_Instance_Of_Content_With_Numeric_Fields_Verify_Message_When_Setting_Invalid_Values_Issue_15340()
            throws Exception {
        ContentType contentType = null;
        try {
            // This CT has a mix of required and non-required fields.
            contentType = createNumericRequiredAndNonRequiredFieldsContentType();
            Contentlet brandNewContentlet = null;

            //Save Action (Creates the initial content)
            final FireActionForm.Builder builder1 = new FireActionForm.Builder();
            final Map<String, Object> contentletFormData = new HashMap<>();
            contentletFormData.put("stInode", contentType.inode());
            contentletFormData.put(REQUIRED_NUMERIC_TEXT_FIELD_NAME, "0");
            contentletFormData.put(NON_REQUIRED_NUMERIC_TEXT_FIELD_NAME, "0");

            builder1.contentletFormData(contentletFormData);
            final FireActionForm fireActionForm1 = new FireActionForm(builder1);
            final HttpServletRequest request1 = mock(HttpServletRequest.class);
            final Response response1 = workflowResource
                    .fireAction(request1, null, SAVE_ACTION_ID, fireActionForm1);

            final int statusCode1 = response1.getStatus();
            assertEquals(Status.OK.getStatusCode(), statusCode1);

            final ResponseEntityView fireEntityView1 = ResponseEntityView.class
                    .cast(response1.getEntity());
            brandNewContentlet = Contentlet.class.cast(fireEntityView1.getEntity());
            assertNotNull(brandNewContentlet);

            final FireActionForm.Builder builder2 = new FireActionForm.Builder();
            final Map <String,Object>contentletFormData2 = new HashMap<>();
            contentletFormData2.put("stInode", contentType.inode());
            contentletFormData2.put(REQUIRED_NUMERIC_TEXT_FIELD_NAME, null);
            contentletFormData2.put(NON_REQUIRED_NUMERIC_TEXT_FIELD_NAME, "0");
            builder2.contentletFormData(contentletFormData2);

            final FireActionForm fireActionForm2 = new FireActionForm(builder2);
            final HttpServletRequest request2 = mock(HttpServletRequest.class);
            final Response response2 = workflowResource
                    .fireAction(request2, brandNewContentlet.getInode(), SAVE_ACTION_ID, fireActionForm2);

            final int statusCode2 = response2.getStatus();
            assertEquals(Status.BAD_REQUEST.getStatusCode(), statusCode2);
            final ResponseEntityView errorEntityView = ResponseEntityView.class.cast(response2.getEntity());
            assertEquals(1, errorEntityView.getErrors().stream().filter(errorEntity -> "required".equals(errorEntity.getErrorCode())).count());

        } finally {
            if (null != contentType) {
                contentTypeAPI.delete(contentType);
            }
        }
    }

    private ContentType createNumericRequiredAndNonRequiredFieldsContentType() throws Exception{
        ContentType contentType;
        final String ctPrefix = "NumericRequiredAndNonRequiredFieldsContentType";
        final String newContentTypeName = ctPrefix + System.currentTimeMillis();

        // Create ContentType
        contentType = createContentTypeAndAssignPermissions(newContentTypeName,
                BaseContentType.CONTENT, PermissionAPI.PERMISSION_READ, adminRole.getId());
        final WorkflowScheme systemWorkflow = workflowAPI.findSystemWorkflowScheme();
        final WorkflowScheme documentWorkflow = workflowAPI.findSchemeByName(DM_WORKFLOW);

        final Field requiredField = FieldBuilder.builder(TextField.class)
                .dataType(DataTypes.INTEGER)
                .name(REQUIRED_NUMERIC_TEXT_FIELD_NAME).variable(REQUIRED_NUMERIC_TEXT_FIELD_NAME)
                .required(true)
                .contentTypeId(contentType.id()).build();

        final Field nonRequiredField = FieldBuilder.builder(TextField.class)
                .dataType(DataTypes.INTEGER)
                .name(NON_REQUIRED_NUMERIC_TEXT_FIELD_NAME).variable(NON_REQUIRED_NUMERIC_TEXT_FIELD_NAME)
                .required(false)
                .contentTypeId(contentType.id()).build();

        final List<Field> fields = Arrays.asList(requiredField, nonRequiredField);
        contentType = contentTypeAPI.save(contentType, fields);

        // Assign contentType to Workflows
        workflowAPI.saveSchemeIdsForContentType(contentType,
                Arrays.asList(
                        systemWorkflow.getId(), documentWorkflow.getId()
                )
        );

        return contentType;
    }

    private ContentType createMixedRequiredAndNonRequiredFieldsContentType() throws Exception{
        ContentType contentType;
        final String ctPrefix = "MixedRequiredAndNonRequiredFieldsContentType";
        final String newContentTypeName = ctPrefix + System.currentTimeMillis();

        // Create ContentType
        contentType = createContentTypeAndAssignPermissions(newContentTypeName,
                BaseContentType.CONTENT, PermissionAPI.PERMISSION_READ, adminRole.getId());
        final WorkflowScheme systemWorkflow = workflowAPI.findSystemWorkflowScheme();
        final WorkflowScheme documentWorkflow = workflowAPI.findSchemeByName(DM_WORKFLOW);

        final Field requiredField = FieldBuilder.builder(TextField.class)
                    .dataType(DataTypes.TEXT)
                    .name(REQUIRED_TEXT_FIELD_NAME).variable(REQUIRED_TEXT_FIELD_NAME)
                    .required(true)
                    .contentTypeId(contentType.id()).build();

        final Field nonRequiredField = FieldBuilder.builder(TextField.class)
                .dataType(DataTypes.TEXT)
                .name(NON_REQUIRED_TEXT_FIELD_NAME).variable(NON_REQUIRED_TEXT_FIELD_NAME)
                .required(false)
                .contentTypeId(contentType.id()).build();

        final List<Field> fields = Arrays.asList(requiredField, nonRequiredField);
        contentType = contentTypeAPI.save(contentType, fields);

        // Assign contentType to Workflows
        workflowAPI.saveSchemeIdsForContentType(contentType,
                Arrays.asList(
                        systemWorkflow.getId(), documentWorkflow.getId()
                )
        );

        return contentType;
    }

    private ContentType createLargeContentType(final boolean required) throws Exception{
        ContentType contentType;
        final String ctPrefix = "LargeTestContentType";
        final String newContentTypeName = ctPrefix + System.currentTimeMillis();

        // Create ContentType
        contentType = createContentTypeAndAssignPermissions(newContentTypeName,
                BaseContentType.CONTENT, PermissionAPI.PERMISSION_READ, adminRole.getId());
        final WorkflowScheme systemWorkflow = workflowAPI.findSystemWorkflowScheme();
        final WorkflowScheme documentWorkflow = workflowAPI.findSchemeByName(DM_WORKFLOW);

        final List<Field> fields = new ArrayList<>(contentType.fields());

        for (final Class clazz : fieldTypesMetaDataMap.keySet()) {
            final String fieldName = "_" + clazz.getCanonicalName();
            final Field field = FieldBuilder.builder(clazz)
                    .dataType(fieldTypesMetaDataMap.get(clazz))
                    .name(fieldName).variable(fieldName)
                    .required(required)
                    .contentTypeId(contentType.id()
            ).build();
            fields.add(field);
        }
        contentType = contentTypeAPI.save(contentType, fields);

        // Assign contentType to Workflows
        workflowAPI.saveSchemeIdsForContentType(contentType,
                Arrays.asList(
                        systemWorkflow.getId(), documentWorkflow.getId()
                )
        );

        return contentType;
    }

    private Object generateValue(final Field field){

        if(field instanceof CategoryField){
            return "Any";
        }

        if(field instanceof KeyValueField){
            return "{key1:value, key2:value }";
        }

        final DataTypes dataType = field.dataType();
        if(DataTypes.DATE == dataType){
            return new Date();
        }
        if(DataTypes.LONG_TEXT == dataType) {
            return RandomStringUtils.random(2500, true, false);
        }
        if(DataTypes.TEXT == dataType) {
            return RandomStringUtils.random(100, true, false);
        }

        return null;

    }
}
