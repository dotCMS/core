package com.dotcms.rest.api.v1.workflow;


import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.FieldDataGen;
import com.dotcms.datagen.RoleDataGen;
import com.dotcms.datagen.TestDataUtils;
import com.dotcms.datagen.WorkflowActionDataGen;
import com.dotcms.datagen.WorkflowDataGen;
import com.dotcms.datagen.WorkflowStepDataGen;
import com.dotcms.rest.EmptyHttpResponse;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.workflow.form.WorkflowActionForm;
import com.dotcms.workflow.form.WorkflowSchemeForm;
import com.dotcms.workflow.form.WorkflowSchemeImportObjectForm;
import com.dotcms.workflow.form.WorkflowStepAddForm;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.RoleAPI;
import com.dotmarketing.common.reindex.ReindexQueueAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.workflows.business.WorkflowAPI;
import com.dotmarketing.portlets.workflows.model.WorkflowAction;
import com.dotmarketing.portlets.workflows.model.WorkflowScheme;
import com.dotmarketing.portlets.workflows.model.WorkflowState;
import com.dotmarketing.portlets.workflows.model.WorkflowStep;
import com.dotmarketing.portlets.workflows.util.WorkflowImportExportUtil;
import com.dotmarketing.portlets.workflows.util.WorkflowSchemeImportExportObject;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UUIDGenerator;
import org.apache.commons.lang.RandomStringUtils;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.dotcms.util.CollectionsUtils.list;
import static com.dotmarketing.business.Role.ADMINISTRATOR;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

public abstract class WorkflowTestUtil {

    static final String ADMIN_DEFAULT_ID = "dotcms.org.1";
    static final String ADMIN_DEFAULT_MAIL = "admin@dotcms.com";
    static final String ADMIN_NAME = "User Admin";

    //Markers to identify all the garbage created by this Test.  So we can clean-up at the end.
    static final String SCHEME_NAME_PREFIX = "scheme::";
    static final String STEP_NAME_PREFIX = "step::";
    static final String ACTION_NAME_PREFIX = "action::";
    static final String CURRENT_STEP = "currentstep";

    public static final String SYSTEM_WORKFLOW = "System Workflow";
    public static final String DM_WORKFLOW = "Document Management";

    static final String SAVE_AS_DRAFT = "Save as Draft";
    static final String SEND_FOR_REVIEW = "Send for Review";
    static final String SEND_TO_LEGAL = "Send to Legal";
    static final String COPY = "Copy";
    static final String SAVE = "Save";
    static final String PUBLISH = "Publish";
    static final String REPUBLISH = "Republish";
    static final String UNPUBLISH = "Unpublish";
    static final String ARCHIVE = "Archive";
    static final String SAVE_PUBLISH = "Save / Publish";

    static void doCleanUp(final WorkflowAPI workflowAPI)
            throws Exception {
        workflowAPI.findSchemes(true).stream()
                .filter(scheme -> scheme.getName().startsWith(SCHEME_NAME_PREFIX))
                .forEach(workflowScheme -> {
                    try {
                        workflowScheme.setArchived(true);
                        workflowAPI.deleteScheme(workflowScheme,
                                APILocator.getUserAPI().getSystemUser()).get();
                    } catch (Exception e) {
                        Logger.warn("Error deleting scheme", e.getMessage(), e);
                    }
                });

    }

    public static String stepName() {
        return STEP_NAME_PREFIX + RandomStringUtils.random(20, true, true);
    }

    public static String schemeName() {
        return SCHEME_NAME_PREFIX + RandomStringUtils.random(20, true, true);
    }

    static String actionName() {
        return ACTION_NAME_PREFIX + RandomStringUtils.random(20, true, true);
    }

    /**
     * Creates dummy schemes
     */
    static WorkflowScheme createScheme(final WorkflowResource workflowResource) {
        final String randomSchemaName = schemeName();
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final WorkflowSchemeForm form = new WorkflowSchemeForm.Builder()
                .schemeName(randomSchemaName).schemeDescription("").schemeArchived(false).build();
        final Response saveResponse = workflowResource.saveScheme(request, new EmptyHttpResponse(),
                form);
        assertEquals(Response.Status.OK.getStatusCode(), saveResponse.getStatus());
        final ResponseEntityView savedEv = ResponseEntityView.class.cast(saveResponse.getEntity());
        return WorkflowScheme.class.cast(savedEv.getEntity());
    }

    /**
     * finds ALL schemes in the database
     */
    @SuppressWarnings("unchecked")
    static List<WorkflowScheme> findSchemes(final WorkflowResource workflowResource) {
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final Response findResponse = workflowResource.findSchemes(request, new EmptyHttpResponse(),
                null, true);
        assertEquals(Response.Status.OK.getStatusCode(), findResponse.getStatus());
        final ResponseEntityView listEv = ResponseEntityView.class.cast(findResponse.getEntity());
        return List.class.cast(listEv.getEntity());
    }

    /**
     * Adds-up a number of steps to the given scheme
     */
    static List<WorkflowStep> addSteps(final WorkflowResource workflowResource,
            final WorkflowScheme savedScheme, int num) {
        final List<WorkflowStep> workflowSteps = new ArrayList<>(2);
        for (int i = 0; i < num; i++) {
            final String randomStepName = stepName();
            final HttpServletRequest addStepRequest = mock(HttpServletRequest.class);
            final WorkflowStepAddForm workflowStepAddForm = new WorkflowStepAddForm.Builder()
                    .stepName(randomStepName).schemeId(savedScheme.getId()).enableEscalation(false)
                    .escalationTime("0").escalationAction("").stepResolved(false).build();
            final Response addStepResponse = workflowResource
                    .addStep(addStepRequest, new EmptyHttpResponse(), workflowStepAddForm);
            assertEquals(Response.Status.OK.getStatusCode(), addStepResponse.getStatus());
            final ResponseEntityView savedStepEntityView = ResponseEntityView.class
                    .cast(addStepResponse.getEntity());
            final WorkflowStep workflowStep = WorkflowStep.class
                    .cast(savedStepEntityView.getEntity());
            assertNotNull(workflowStep);
            workflowSteps.add(workflowStep);
        }
        return workflowSteps;
    }

    /**
     * @param savedScheme
     * @param roleId
     * @param workflowSteps
     * @return
     */
    static List<WorkflowAction> createWorkflowActions(final WorkflowResource workflowResource,
            final WorkflowScheme savedScheme,
            final String roleId, final List<WorkflowStep> workflowSteps) {
        final List<WorkflowAction> workflowActions = new ArrayList<>(2);
        final Set<WorkflowState> states = WorkflowState.toSet(WorkflowState.values());
        final ListIterator<WorkflowStep> iterator = workflowSteps.listIterator();
        while (iterator.hasNext()) {
            final WorkflowStep ws = iterator.next();
            final String randomActionName = actionName();
            final HttpServletRequest saveActionRequest = mock(HttpServletRequest.class);
            final WorkflowActionForm form = new WorkflowActionForm.Builder()
                    .schemeId(savedScheme.getId()).
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
            final Response saveActionResponse = workflowResource.saveAction(saveActionRequest,
                    new EmptyHttpResponse(), form);
            assertEquals(Response.Status.OK.getStatusCode(), saveActionResponse.getStatus());
            final ResponseEntityView savedActionEv = ResponseEntityView.class
                    .cast(saveActionResponse.getEntity());
            final WorkflowAction savedAction = WorkflowAction.class.cast(savedActionEv.getEntity());
            assertNotNull(savedAction);
            workflowActions.add(savedAction);
        }
        return workflowActions;
    }

    static private String nextStepIdIfAny(final ListIterator<WorkflowStep> iterator,
            final List<WorkflowStep> workflowSteps) {
        if (iterator.hasNext()) {
            return workflowSteps.get(iterator.nextIndex()).getId();
        }
        return CURRENT_STEP;
    }

    @SuppressWarnings("unchecked")
    static List<WorkflowStep> findSteps(final WorkflowResource workflowResource,
            final WorkflowScheme savedScheme) {
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final Response findResponse = workflowResource
                .findStepsByScheme(request, new EmptyHttpResponse(), savedScheme.getId());
        assertEquals(Response.Status.OK.getStatusCode(), findResponse.getStatus());
        final ResponseEntityView findResponseEv = ResponseEntityView.class
                .cast(findResponse.getEntity());
        return List.class.cast(findResponseEv.getEntity());
    }

    protected static Role roleAdmin() {

        //Creating a test role
        Role adminRole = null;
        try {

            adminRole = APILocator.getRoleAPI().loadRoleByKey(ADMINISTRATOR);
            if (adminRole == null) {
                adminRole = new RoleDataGen().key(ADMINISTRATOR).nextPersisted();
            }
        } catch (DotDataException e) {
            e.printStackTrace();
        }

        return adminRole;
    }

    static WorkflowSchemeImportObjectForm createImportExportObjectForm() throws Exception {

        final WorkflowSchemeImportExportObject workflowExportObject = new WorkflowSchemeImportExportObject();
        final List<Permission> permissions = new ArrayList<>();
        final List<WorkflowScheme> schemes = new ArrayList<>();
        final WorkflowScheme scheme = new WorkflowScheme();
        final List<WorkflowStep> steps = new ArrayList<>();
        final List<WorkflowAction> actions = new ArrayList<>();
        final List<Map<String, String>> actionSteps = new ArrayList<>();

        final RoleAPI roleAPI = APILocator.getRoleAPI();

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
        workflowAction1.setShowOn(WorkflowState.LOCKED, WorkflowState.PUBLISHED,
                WorkflowState.UNPUBLISHED, WorkflowState.EDITING);
        workflowAction1.setNextStep(workflowStep2.getId());
        workflowAction1.setNextAssign(roleAdmin().getId());
        workflowAction1.setSchemeId(scheme.getId());
        workflowAction1.setName("save");
        workflowAction1.setOrder(0);
        workflowAction1.setCommentable(true);
        actions.add(workflowAction1);

        final WorkflowAction workflowAction2 = new WorkflowAction();

        workflowAction2.setId(UUIDGenerator.generateUuid());
        workflowAction2.setShowOn(WorkflowState.LOCKED, WorkflowState.PUBLISHED,
                WorkflowState.UNPUBLISHED, WorkflowState.EDITING);
        workflowAction2.setNextStep(workflowStep2.getId());
        workflowAction2.setNextAssign(roleAdmin().getId());
        workflowAction2.setSchemeId(scheme.getId());
        workflowAction2.setName("save/publish");
        workflowAction2.setOrder(1);
        workflowAction2.setCommentable(true);
        actions.add(workflowAction2);

        final WorkflowAction workflowAction3 = new WorkflowAction();

        workflowAction3.setId(UUIDGenerator.generateUuid());
        workflowAction3.setShowOn(WorkflowState.LOCKED, WorkflowState.PUBLISHED,
                WorkflowState.EDITING);
        workflowAction3.setNextStep(WorkflowAction.CURRENT_STEP);
        workflowAction3.setNextAssign(roleAdmin().getId());
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

        final WorkflowSchemeImportObjectForm exportObjectForm =
                new WorkflowSchemeImportObjectForm(
                        new WorkflowSchemeImportExportObjectView(WorkflowResource.VERSION, schemes,
                                steps, actions,
                                actionSteps, Collections.emptyList(), Collections.emptyList(),
                                Collections.emptyList()),
                        permissions);

        return exportObjectForm;
    }

    static Map<ContentType, List<Contentlet>> findContentSamplesByType(
            final List<ContentType> contentTypes, final WorkflowScheme scheme, final int limit)
            throws Exception {
        final Map<ContentType, List<Contentlet>> contentTypeSamplesMap = new HashMap<>();

        for (final ContentType contentType : contentTypes) {

            final List<Contentlet> contentlets = APILocator.getContentletAPI()
                    .search("+contentType:" + contentType.variable() +
                                    " +wfscheme:" + scheme.getId() +
                                    " +languageId:1 +deleted:false ", limit, 0,
                            "modDate desc",
                            APILocator.systemUser(), false);

            contentTypeSamplesMap.computeIfAbsent(contentType, ct -> new ArrayList<>(limit))
                    .addAll(contentlets);

        }
        return contentTypeSamplesMap;
    }

    static Map<WorkflowScheme, Map<ContentType, List<Contentlet>>> collectSampleContent(
            final int limit) throws Exception {
        boolean savedAutoAssign = Config.getBooleanProperty("AUTO_ASSIGN_WORKFLOW", true);
        try {
            Config.setProperty("AUTO_ASSIGN_WORKFLOW", false);

            ReindexQueueAPI queueAPI = APILocator.getReindexQueueAPI();
            final WorkflowScheme workflowScheme_1 = new WorkflowDataGen().nextPersisted();
            TestDataUtils.assertEmptyQueue();
            final WorkflowStep workflowStep_1 = new WorkflowStepDataGen(
                    workflowScheme_1.getId()).nextPersisted();
            final WorkflowAction workflowAction_1 = new WorkflowActionDataGen(
                    workflowScheme_1.getId(), workflowStep_1.getId())
                    .nextPersisted();

            TestDataUtils.assertEmptyQueue();
            final ContentType contentType_1 = new ContentTypeDataGen()
                    .field(new FieldDataGen()
                            .name("title")
                            .velocityVarName("title").next()
                    )
                    .workflowId(workflowScheme_1.getId()).nextPersisted();
            TestDataUtils.assertEmptyQueue();
            final Contentlet contentlet_1 = new ContentletDataGen(contentType_1.id())
                    .setProperty("title", "content_1")
                    .nextPersisted();

            final WorkflowScheme workflowScheme_2 = new WorkflowDataGen().nextPersisted();
            TestDataUtils.assertEmptyQueue();
            final WorkflowStep workflowStep_2 = new WorkflowStepDataGen(
                    workflowScheme_2.getId()).nextPersisted();
            TestDataUtils.assertEmptyQueue();
            final WorkflowAction workflowAction_2 = new WorkflowActionDataGen(
                    workflowScheme_2.getId(), workflowStep_2.getId())
                    .nextPersisted();

            final ContentType contentType_2 = new ContentTypeDataGen()
                    .field(new FieldDataGen()
                            .name("title")
                            .velocityVarName("title").next()
                    )
                    .workflowId(workflowScheme_2.getId()).nextPersisted();
            TestDataUtils.assertEmptyQueue();
            final Contentlet contentlet_2 = new ContentletDataGen(contentType_2.id())
                    .setProperty("title", "content_2")
                    .nextPersisted();
            TestDataUtils.assertEmptyQueue();
            return Map.of(
                    workflowScheme_1, Map.of(contentType_1, list(contentlet_1)),
                    workflowScheme_2, Map.of(contentType_2, list(contentlet_2))
            );
        } finally {
            Config.setProperty("AUTO_ASSIGN_WORKFLOW", savedAutoAssign);
        }
    }

    static List<WorkflowAction> getAllWorkflowActions(final BulkWorkflowSchemeView workflowScheme) {
        final List<WorkflowAction> workflowActions = new ArrayList<>();
        final List<BulkWorkflowStepView> systemWorkflowSteps = workflowScheme.getSteps();

        for (final BulkWorkflowStepView stepView : systemWorkflowSteps) {
            final CountWorkflowStep countWorkflowStep = stepView.getStep();
            assertTrue(countWorkflowStep.getCount() > 0);
            final List<CountWorkflowAction> actions = stepView.getActions();
            workflowActions.addAll(
                    actions.stream().map(CountWorkflowAction::getWorkflowAction)
                            .collect(Collectors.toList())
            );
        }
        return workflowActions;
    }


}
