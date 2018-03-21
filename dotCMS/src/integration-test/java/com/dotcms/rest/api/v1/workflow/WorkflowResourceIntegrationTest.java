package com.dotcms.rest.api.v1.workflow;

import com.dotcms.repackage.javax.ws.rs.core.Response;
import com.dotcms.rest.ContentHelper;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.api.v1.authentication.ResponseUtil;
import com.dotcms.util.IntegrationTestInitService;
import com.dotcms.workflow.form.*;
import com.dotcms.workflow.helper.WorkflowHelper;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.RoleAPI;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.AlreadyExistException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.workflows.business.WorkflowAPI;
import com.dotmarketing.portlets.workflows.model.WorkflowAction;
import com.dotmarketing.portlets.workflows.model.WorkflowScheme;
import com.dotmarketing.portlets.workflows.model.WorkflowState;
import com.dotmarketing.portlets.workflows.model.WorkflowStep;
import com.dotmarketing.portlets.workflows.util.WorkflowImportExportUtil;
import com.dotmarketing.portlets.workflows.util.WorkflowSchemeImportExportObject;
import com.dotmarketing.util.UUIDGenerator;
import com.liferay.portal.model.User;
import org.apache.commons.lang.RandomStringUtils;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.stream.Collectors;

import static com.dotcms.util.CollectionsUtils.map;
import static com.dotmarketing.business.Role.ADMINISTRATOR;
import static org.junit.Assert.*;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WorkflowResourceIntegrationTest {

    private static WorkflowAPI workflowAPI;
    private static ContentletAPI contentletAPI;
    private static RoleAPI roleAPI;
    private static ContentHelper contentHelper;
    private static ResponseUtil responseUtil;
    private static WorkflowHelper workflowHelper;
    private static PermissionAPI permissionAPI;
    private static WorkflowImportExportUtil workflowImportExportUtil;
    private static WorkflowResource workflowResource;

    private static final String ADMIN_DEFAULT_ID = "dotcms.org.1";
    private static final String ADMIN_DEFAULT_MAIL = "admin@dotcms.com";
    private static final String ADMIN_NAME = "User Admin";

    //Markers to identify all the garbage created by this Test.  So we can clean-up at the end.
    private static final String SCHEME_NAME_PREFIX = "scheme::";
    private static final String STEP_NAME_PREFIX = "step::";
    private static final String ACTION_NAME_PREFIX = "action::";

    private static final String CURRENT_STEP = "currentstep";

    @BeforeClass
    public static void prepare() throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
        workflowAPI = APILocator.getWorkflowAPI();
        contentletAPI = APILocator.getContentletAPI();
        roleAPI = APILocator.getRoleAPI();
        contentHelper = ContentHelper.getInstance();
        permissionAPI = APILocator.getPermissionAPI();
        workflowImportExportUtil = WorkflowImportExportUtil.getInstance();
        workflowHelper = new WorkflowHelper(workflowAPI, roleAPI, contentletAPI, permissionAPI, workflowImportExportUtil);
        responseUtil = ResponseUtil.INSTANCE;

        final User user = mock(User.class);
        when(user.getUserId()).thenReturn(ADMIN_DEFAULT_ID);
        when(user.getEmailAddress()).thenReturn(ADMIN_DEFAULT_MAIL);
        when(user.getFullName()).thenReturn(ADMIN_NAME);

        final WebResource webResource = mock(WebResource.class);
        final InitDataObject dataObject = mock(InitDataObject.class);
        when(dataObject.getUser()).thenReturn(user);
        when(webResource.init(anyString(), anyBoolean(), any(HttpServletRequest.class), anyBoolean(), anyString())).thenReturn(dataObject);

        workflowResource = new WorkflowResource(workflowHelper, contentHelper, workflowAPI, contentletAPI, responseUtil, permissionAPI, workflowImportExportUtil, webResource);
    }


    String stepName(){
        return STEP_NAME_PREFIX + RandomStringUtils.random(20, true, true);
    }

    String schemeName(){
        return SCHEME_NAME_PREFIX + RandomStringUtils.random(20, true, true);
    }

    String actionName(){
        return ACTION_NAME_PREFIX + RandomStringUtils.random(20, true, true);
    }

    /**
     * Creates dummy schemes
     * @return
     */
    private WorkflowScheme createScheme(){
        final String randomSchemaName =  schemeName();
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final WorkflowSchemeForm form = new WorkflowSchemeForm.Builder().schemeName(randomSchemaName).schemeDescription("").schemeArchived(false).build();
        final Response saveResponse = workflowResource.save(request,form);
        assertEquals(Response.Status.OK.getStatusCode(), saveResponse.getStatus());
        final ResponseEntityView savedEv = ResponseEntityView.class.cast(saveResponse.getEntity());
        return WorkflowScheme.class.cast(savedEv.getEntity());
    }

    /**
     * finds ALL schemes in the database
     * @return
     */
    @SuppressWarnings("unchecked")
    private  List<WorkflowScheme> findSchemes(){
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final Response findResponse = workflowResource.findSchemes(request, null);
        assertEquals(Response.Status.OK.getStatusCode(), findResponse.getStatus());
        final ResponseEntityView listEv = ResponseEntityView.class.cast(findResponse.getEntity());
        return List.class.cast(listEv.getEntity());
    }

    /**
     * Adds-up a number of steps to the given scheme
     * @param savedScheme
     * @param num
     * @return
     */
    private List<WorkflowStep> addSteps(final WorkflowScheme savedScheme, int num) {
        final List<WorkflowStep> workflowSteps = new ArrayList<>(2);
        for (int i = 0; i < num; i++) {
            final String randomStepName = stepName();
            final HttpServletRequest addStepRequest = mock(HttpServletRequest.class);
            final WorkflowStepAddForm workflowStepAddForm = new WorkflowStepAddForm.Builder().stepName(randomStepName).schemeId(savedScheme.getId()).enableEscalation(false).escalationTime("0").escalationAction("").stepResolved(false).build();
            final Response addStepResponse = workflowResource.addStep(addStepRequest, workflowStepAddForm);
            assertEquals(Response.Status.OK.getStatusCode(), addStepResponse.getStatus());
            final ResponseEntityView savedStepEntityView = ResponseEntityView.class.cast(addStepResponse.getEntity());
            final WorkflowStep workflowStep = WorkflowStep.class.cast(savedStepEntityView.getEntity());
            assertNotNull(workflowStep);
            workflowSteps.add(workflowStep);
        }
        return workflowSteps;
    }

    /**
     * Performs a reorder command over the workflow resource
     * @param stepId
     * @param newPosition
     */
    private void reorderSteps(String stepId, int newPosition){
        final HttpServletRequest reorderStepRequest = mock(HttpServletRequest.class);
        final Response reorderStepResponse = workflowResource.reorderStep(reorderStepRequest, stepId, newPosition);
        assertEquals(Response.Status.OK.getStatusCode(), reorderStepResponse.getStatus());
        final ResponseEntityView reorderedStepEntityView = ResponseEntityView.class.cast(reorderStepResponse.getEntity());
        final String ok = String.class.cast(reorderedStepEntityView.getEntity());
        assertEquals(ResponseEntityView.OK,ok);
    }

    /**
     *
     * @param savedScheme
     * @param roleId
     * @param workflowSteps
     * @return
     */
    private List<WorkflowAction> createWorkflowActions(final WorkflowScheme savedScheme, final String roleId, final List<WorkflowStep> workflowSteps){
        final List<WorkflowAction> workflowActions = new ArrayList<>(2);
        final Set<WorkflowState> states = WorkflowState.toSet(WorkflowState.values());
        final ListIterator <WorkflowStep> iterator = workflowSteps.listIterator();
        while(iterator.hasNext()) {
            final WorkflowStep ws = iterator.next();
            final String randomActionName = actionName();
            final HttpServletRequest saveActionRequest = mock(HttpServletRequest.class);
            final WorkflowActionForm form = new WorkflowActionForm.Builder().schemeId(savedScheme.getId()).
                    stepId(ws.getId()).
                    actionName(randomActionName).
                    showOn(states).
                    actionNextStep(nextStepIdIfAny(iterator, workflowSteps)).
                    actionAssignable(false).
                    actionCommentable(false).
                    requiresCheckout(false).
                    actionNextAssign(roleId).
                    whoCanUse(Arrays.asList("")).
                    actionCondition("").
                    build();
            final Response saveActionResponse = workflowResource.save(saveActionRequest, form);
            assertEquals(Response.Status.OK.getStatusCode(), saveActionResponse.getStatus());
            final ResponseEntityView savedActionEv = ResponseEntityView.class.cast(saveActionResponse.getEntity());
            final WorkflowAction savedAction = WorkflowAction.class.cast(savedActionEv.getEntity());
            assertNotNull(savedAction);
            workflowActions.add(savedAction);
        }
        return workflowActions;
    }

    private String nextStepIdIfAny(final ListIterator <WorkflowStep> iterator, final List<WorkflowStep> workflowSteps){
           if(iterator.hasNext()){
              return workflowSteps.get(iterator.nextIndex()).getId();
           }
           return CURRENT_STEP;
    }

    @SuppressWarnings("unchecked")
    private List<WorkflowStep> findSteps(final WorkflowScheme savedScheme){
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final Response findResponse = workflowResource.findStepsByScheme(request, savedScheme.getId());
        assertEquals(Response.Status.OK.getStatusCode(), findResponse.getStatus());
        final ResponseEntityView findResponseEv = ResponseEntityView.class.cast(findResponse.getEntity());
        return List.class.cast(findResponseEv.getEntity());
    }

    private void validateOrderedSteps(final List<WorkflowStep> steps){
        int lastOrderValue = -1;
        for(WorkflowStep step:steps){
            assertTrue(lastOrderValue < step.getMyOrder());
            lastOrderValue = step.getMyOrder();
        }
    }


    @Test
    public void testImportScheme() throws DotDataException {

        final HttpServletRequest request = mock(HttpServletRequest.class);
        final WorkflowSchemeImportExportObject workflowExportObject = new WorkflowSchemeImportExportObject();
        final List<Permission> permissions                          = new ArrayList<>();
        final List<WorkflowScheme> schemes                          = new ArrayList<>();
        final WorkflowScheme       scheme                           = new WorkflowScheme();
        final List<WorkflowStep>   steps                            = new ArrayList<>();
        final List<WorkflowAction> actions                          = new ArrayList<>();
        final List<Map<String, String>> actionSteps                 = new ArrayList<>();

        try {

            scheme.setArchived(false);
            scheme.setName("scheme::TestImport" + UUIDGenerator.generateUuid());
            scheme.setModDate(new Date());
            scheme.setId(UUIDGenerator.generateUuid());
            schemes.add(scheme);

            workflowExportObject.setSchemes(schemes);

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

            workflowExportObject.setSteps(steps);

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

            workflowExportObject.setActions(actions);

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

            workflowExportObject.setActionSteps(actionSteps);

            workflowExportObject.setActionClasses(Collections.emptyList());
            workflowExportObject.setActionClassParams(Collections.emptyList());

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


            final WorkflowSchemeImportExportObjectForm exportObjectForm =
                    new WorkflowSchemeImportExportObjectForm(workflowExportObject, permissions);

            final Response importResponse = workflowResource.importScheme(request, exportObjectForm);
            assertEquals(Response.Status.OK.getStatusCode(), importResponse.getStatus());

            final Response exportResponse = workflowResource.exportScheme(request, scheme.getId());
            assertEquals(Response.Status.OK.getStatusCode(), importResponse.getStatus());
            final ResponseEntityView exportEntityView = ResponseEntityView.class.cast(exportResponse.getEntity());
            final Map importSchemeMap = Map.class.cast(exportEntityView.getEntity());
            assertNotNull(importSchemeMap);

            final WorkflowSchemeImportExportObject exportObject = (WorkflowSchemeImportExportObject) importSchemeMap.get("workflowImportObject");
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
                } catch (DotDataException | AlreadyExistException e) {
                    e.printStackTrace();
                }
            }
        }

    }

    @Test
    public void testAddSchemaThenFindIt(){
        final WorkflowScheme savedScheme = createScheme();
        assertNotNull(savedScheme);
        final List<WorkflowScheme> schemaList = findSchemes();
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
        final String updatedName = "Any!";
        final WorkflowScheme savedScheme = createScheme();
        assertNotNull(savedScheme);
        final HttpServletRequest request = mock(HttpServletRequest.class);
        WorkflowSchemeForm form = new WorkflowSchemeForm.Builder().schemeDescription("lol").schemeArchived(false).schemeName(updatedName).build();
        final Response updateResponse = workflowResource.update(request,savedScheme.getId(), form);
        assertEquals(Response.Status.OK.getStatusCode(), updateResponse.getStatus());
        final ResponseEntityView updateSchemeEntityView = ResponseEntityView.class.cast(updateResponse.getEntity());
        final WorkflowScheme updatedScheme = WorkflowScheme.class.cast(updateSchemeEntityView.getEntity());
        assertNotNull(updatedScheme);
    }

    @Test
    public void testUpdateSchemaUsingDupeName(){
        final WorkflowScheme savedScheme1 = createScheme();
        final WorkflowScheme savedScheme2 = createScheme();
        assertNotNull(savedScheme1);
        final String updatedName = savedScheme2.getName();
        final HttpServletRequest request = mock(HttpServletRequest.class);
        WorkflowSchemeForm form = new WorkflowSchemeForm.Builder().schemeDescription("lol").schemeArchived(false).schemeName(updatedName).build();
        final Response updateResponse = workflowResource.update(request,savedScheme1.getId(), form);
        assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), updateResponse.getStatus());
    }

    @Test
    public void testCreateSchemeThenAddStepsThenAddActions() throws Exception{
        final Role adminRole = roleAPI.loadRoleByKey(ADMINISTRATOR);
        final String adminRoleId = adminRole.getId();
        final WorkflowScheme savedScheme = createScheme();
        assertNotNull(savedScheme);
        final List<WorkflowStep> workflowSteps = addSteps(savedScheme,3);
        assertFalse(workflowSteps.isEmpty());
        final List<WorkflowAction> actions = createWorkflowActions(savedScheme, adminRoleId, workflowSteps);
        assertFalse(actions.isEmpty());
    }

    @Test
    public void testCreateSchemeThenAddStepsThenDeleteSteps() {
        final int numSteps = 5;
        final WorkflowScheme savedScheme = createScheme();
        assertNotNull(savedScheme);
        final List<WorkflowStep> workflowSteps = addSteps(savedScheme, numSteps);
        assertFalse(workflowSteps.isEmpty());
        final HttpServletRequest removeStepRequest = mock(HttpServletRequest.class);
        int count = 0;
        for(final WorkflowStep ws: workflowSteps){
            Response deleteStepResponse = workflowResource.deleteStep(removeStepRequest, ws.getId());
            assertEquals(Response.Status.OK.getStatusCode(), deleteStepResponse.getStatus());
            count++;
        }
        assertEquals(count,numSteps);
    }

    @Test
    public void testCreateSchemeThenFindStepsBySchemeId() {
        int numSteps = 4;
        final WorkflowScheme savedScheme = createScheme();
        final List<WorkflowStep> steps = findSteps(savedScheme);
        validateOrderedSteps(steps);
        assertTrue(steps.isEmpty());
        addSteps(savedScheme,numSteps);
        final List<WorkflowStep> afterAddingSteps = findSteps(savedScheme);
        assertEquals(numSteps, afterAddingSteps.size());
        validateOrderedSteps(afterAddingSteps);
    }

    @Test
    public void testReorderStep() {
        final int numSteps = 10;
        final Random random = new Random();
        final int randomEntry = random.nextInt(numSteps);
        final WorkflowScheme savedScheme = createScheme();
        final List<WorkflowStep> steps = findSteps(savedScheme);
        assertTrue(steps.isEmpty());
        final List<WorkflowStep> workflowStepsOriginalSetUp = addSteps(savedScheme, numSteps);
        final WorkflowStep firstWorkflowStep = workflowStepsOriginalSetUp.get(0);
        reorderSteps(firstWorkflowStep.getId(),randomEntry);

        final List<WorkflowStep> afterReorderingSteps = findSteps(savedScheme);
        assertEquals(numSteps, afterReorderingSteps.size());
        final WorkflowStep rearrangedPosition = afterReorderingSteps.get(randomEntry);

        assertEquals(rearrangedPosition.getMyOrder(),randomEntry);
        assertEquals(rearrangedPosition.getId(),firstWorkflowStep.getId());
        validateOrderedSteps(afterReorderingSteps);
    }

    @Test
    public void testUpdateStep(){
        final int numSteps = 3;
        final String updatedName = "LOL!";
        final int newOrder = numSteps + 10; //We're sending an index outside the boundaries. See if we can break it.
        final WorkflowScheme savedScheme = createScheme();
        assertNotNull(savedScheme);
        final List<WorkflowStep> workflowSteps = addSteps(savedScheme, numSteps);
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
        final WorkflowScheme savedScheme = createScheme();
        assertNotNull(savedScheme);
        final List<WorkflowStep> workflowSteps = addSteps(savedScheme,2);
        assertFalse(workflowSteps.isEmpty());
        assertEquals(2, workflowSteps.size());
        final List<WorkflowAction> actions = createWorkflowActions(savedScheme, adminRoleId, workflowSteps);
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
        final WorkflowScheme savedScheme = createScheme();
        assertNotNull(savedScheme);
        final List<WorkflowStep> workflowSteps = addSteps(savedScheme,2);
        assertFalse(workflowSteps.isEmpty());
        assertEquals(2, workflowSteps.size());
        final List<WorkflowAction> actions = createWorkflowActions(savedScheme, adminRoleId, workflowSteps);
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
        final WorkflowScheme savedScheme = createScheme();
        assertNotNull(savedScheme);
        final List<WorkflowStep> workflowSteps = addSteps(savedScheme,1);
        assertFalse(workflowSteps.isEmpty());
        assertEquals(1, workflowSteps.size());
        final List<WorkflowAction> actions = createWorkflowActions(savedScheme, adminRoleId, workflowSteps);
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

}
