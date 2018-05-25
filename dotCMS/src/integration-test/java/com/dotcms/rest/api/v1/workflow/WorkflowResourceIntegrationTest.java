package com.dotcms.rest.api.v1.workflow;

import static com.dotcms.rest.api.v1.workflow.WorkflowTestUtil.ADMIN_DEFAULT_ID;
import static com.dotcms.rest.api.v1.workflow.WorkflowTestUtil.ADMIN_DEFAULT_MAIL;
import static com.dotcms.rest.api.v1.workflow.WorkflowTestUtil.ADMIN_NAME;
import static com.dotcms.rest.api.v1.workflow.WorkflowTestUtil.CURRENT_STEP;
import static com.dotcms.rest.api.v1.workflow.WorkflowTestUtil.actionName;
import static com.dotcms.rest.api.v1.workflow.WorkflowTestUtil.addSteps;
import static com.dotcms.rest.api.v1.workflow.WorkflowTestUtil.createScheme;
import static com.dotcms.rest.api.v1.workflow.WorkflowTestUtil.createWorkflowActions;
import static com.dotcms.rest.api.v1.workflow.WorkflowTestUtil.doCleanUp;
import static com.dotcms.rest.api.v1.workflow.WorkflowTestUtil.findSchemes;
import static com.dotcms.rest.api.v1.workflow.WorkflowTestUtil.findSteps;
import static com.dotcms.rest.api.v1.workflow.WorkflowTestUtil.schemeName;
import static com.dotmarketing.business.Role.ADMINISTRATOR;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dotcms.mock.response.MockAsyncResponse;
import com.dotcms.repackage.javax.ws.rs.container.AsyncResponse;
import com.dotcms.repackage.javax.ws.rs.core.Response;
import com.dotcms.repackage.javax.ws.rs.core.Response.Status;
import com.dotcms.rest.ContentHelper;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.api.v1.authentication.ResponseUtil;
import com.dotcms.util.IntegrationTestInitService;
import com.dotcms.workflow.form.WorkflowActionForm;
import com.dotcms.workflow.form.WorkflowActionStepForm;
import com.dotcms.workflow.form.WorkflowSchemeForm;
import com.dotcms.workflow.form.WorkflowSchemeImportObjectForm;
import com.dotcms.workflow.form.WorkflowStepUpdateForm;
import com.dotcms.workflow.helper.WorkflowHelper;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.RoleAPI;
import com.dotmarketing.exception.AlreadyExistException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.workflows.business.WorkflowAPI;
import com.dotmarketing.portlets.workflows.model.WorkflowAction;
import com.dotmarketing.portlets.workflows.model.WorkflowScheme;
import com.dotmarketing.portlets.workflows.model.WorkflowState;
import com.dotmarketing.portlets.workflows.model.WorkflowStep;
import com.dotmarketing.portlets.workflows.util.WorkflowImportExportUtil;
import com.dotmarketing.util.UUIDGenerator;
import com.liferay.portal.model.User;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class WorkflowResourceIntegrationTest {


    private static WorkflowAPI workflowAPI;
    private static RoleAPI roleAPI;
    private static WorkflowResource workflowResource;

    @BeforeClass
    public static void prepare() throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
        workflowAPI =  APILocator.getWorkflowAPI();
        ContentletAPI contentletAPI = APILocator.getContentletAPI();
        roleAPI = APILocator.getRoleAPI();
        ContentHelper contentHelper = ContentHelper.getInstance();
        PermissionAPI permissionAPI = APILocator.getPermissionAPI();
        WorkflowImportExportUtil workflowImportExportUtil = WorkflowImportExportUtil.getInstance();
        WorkflowHelper workflowHelper = new WorkflowHelper(workflowAPI, roleAPI, contentletAPI, permissionAPI,
                workflowImportExportUtil);
        ResponseUtil responseUtil = ResponseUtil.INSTANCE;

        final User user = mock(User.class);
        when(user.getUserId()).thenReturn(ADMIN_DEFAULT_ID);
        when(user.getEmailAddress()).thenReturn(ADMIN_DEFAULT_MAIL);
        when(user.getFullName()).thenReturn(ADMIN_NAME);

        final WebResource webResource = mock(WebResource.class);
        final InitDataObject dataObject = mock(InitDataObject.class);
        when(dataObject.getUser()).thenReturn(user);
        when(webResource
                .init(anyString(), anyBoolean(), any(HttpServletRequest.class), anyBoolean(),
                        anyString())).thenReturn(dataObject);

        workflowResource = new WorkflowResource(workflowHelper, contentHelper, workflowAPI,
                contentletAPI, responseUtil, permissionAPI, workflowImportExportUtil, webResource);
    }

    @AfterClass
    public static void cleanup() throws Exception {

        doCleanUp(workflowResource, workflowAPI);
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
            workflowAction1.setShowOn(WorkflowState.LOCKED, WorkflowState.PUBLISHED, WorkflowState.UNPUBLISHED);
            workflowAction1.setNextStep(workflowStep2.getId());
            workflowAction1.setNextAssign(roleAPI.loadRoleByKey(ADMINISTRATOR).getId());
            workflowAction1.setSchemeId(scheme.getId());
            workflowAction1.setName("save");
            workflowAction1.setOrder(0);
            workflowAction1.setCommentable(true);
            actions.add(workflowAction1);

            final WorkflowAction workflowAction2 = new WorkflowAction();

            workflowAction2.setId(UUIDGenerator.generateUuid());
            workflowAction2.setShowOn(WorkflowState.LOCKED, WorkflowState.PUBLISHED, WorkflowState.UNPUBLISHED);
            workflowAction2.setNextStep(workflowStep2.getId());
            workflowAction2.setNextAssign(roleAPI.loadRoleByKey(ADMINISTRATOR).getId());
            workflowAction2.setSchemeId(scheme.getId());
            workflowAction2.setName("save/publish");
            workflowAction2.setOrder(1);
            workflowAction2.setCommentable(true);
            actions.add(workflowAction2);

            final WorkflowAction workflowAction3 = new WorkflowAction();

            workflowAction3.setId(UUIDGenerator.generateUuid());
            workflowAction3.setShowOn(WorkflowState.LOCKED, WorkflowState.PUBLISHED);
            workflowAction3.setNextStep(WorkflowAction.CURRENT_STEP);
            workflowAction3.setNextAssign(roleAPI.loadRoleByKey(ADMINISTRATOR).getId());
            workflowAction3.setSchemeId(scheme.getId());
            workflowAction3.setName("finish");
            workflowAction3.setOrder(2);
            workflowAction3.setCommentable(true);
            actions.add(workflowAction3);

            final Map<String, String> actionStep1 = new HashMap<>();
            actionStep1.put(WorkflowImportExportUtil.ACTION_ID, workflowAction1.getId());
            actionStep1.put(WorkflowImportExportUtil.STEP_ID, workflowStep1.getId());
            actionStep1.put(WorkflowImportExportUtil.ACTION_ORDER, "0");
            actionSteps.add(actionStep1);

            final Map<String, String> actionStep2 = new HashMap<>();
            actionStep2.put(WorkflowImportExportUtil.ACTION_ID, workflowAction2.getId());
            actionStep2.put(WorkflowImportExportUtil.STEP_ID, workflowStep1.getId());
            actionStep2.put(WorkflowImportExportUtil.ACTION_ORDER, "1");
            actionSteps.add(actionStep2);

            final Map<String, String> actionStep3 = new HashMap<>();
            actionStep3.put(WorkflowImportExportUtil.ACTION_ID, workflowAction3.getId());
            actionStep3.put(WorkflowImportExportUtil.STEP_ID, workflowStep2.getId());
            actionStep3.put(WorkflowImportExportUtil.ACTION_ORDER, "2");
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
                    workflowAPI.deleteScheme(schemeToRemove, APILocator.systemUser());
                } catch (DotDataException | DotSecurityException | AlreadyExistException e) {
                    e.printStackTrace();
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

}
