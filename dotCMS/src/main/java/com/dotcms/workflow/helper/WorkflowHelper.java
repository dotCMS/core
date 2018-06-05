package com.dotcms.workflow.helper;

import static com.dotcms.rest.api.v1.authentication.ResponseUtil.getFormattedMessage;
import static com.dotmarketing.db.HibernateUtil.addSyncCommitListener;

import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.business.WrapInTransaction;
import com.dotcms.content.elasticsearch.business.ESSearchResults;
import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.repackage.javax.validation.constraints.NotNull;
import com.dotcms.rest.api.v1.workflow.BulkActionView;
import com.dotcms.rest.api.v1.workflow.BulkActionsResultView;
import com.dotcms.rest.api.v1.workflow.CountWorkflowAction;
import com.dotcms.rest.api.v1.workflow.CountWorkflowStep;
import com.dotcms.rest.api.v1.workflow.WorkflowDefaultActionView;
import com.dotcms.util.CollectionsUtils;
import com.dotcms.workflow.form.BulkActionForm;
import com.dotcms.workflow.form.FireBulkActionsForm;
import com.dotcms.workflow.form.IWorkflowStepForm;
import com.dotcms.workflow.form.WorkflowActionForm;
import com.dotcms.workflow.form.WorkflowActionStepBean;
import com.dotcms.workflow.form.WorkflowReorderBean;
import com.dotcms.workflow.form.WorkflowSchemeForm;
import com.dotcms.workflow.form.WorkflowStepAddForm;
import com.dotcms.workflow.form.WorkflowStepUpdateForm;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.RoleAPI;
import com.dotmarketing.exception.AlreadyExistException;
import com.dotmarketing.exception.DoesNotExistException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.exception.InvalidLicenseException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.workflows.actionlet.NotifyAssigneeActionlet;
import com.dotmarketing.portlets.workflows.business.DotWorkflowException;
import com.dotmarketing.portlets.workflows.business.WorkflowAPI;
import com.dotmarketing.portlets.workflows.model.WorkflowAction;
import com.dotmarketing.portlets.workflows.model.WorkflowActionClass;
import com.dotmarketing.portlets.workflows.model.WorkflowScheme;
import com.dotmarketing.portlets.workflows.model.WorkflowStep;
import com.dotmarketing.portlets.workflows.util.WorkflowImportExportUtil;
import com.dotmarketing.portlets.workflows.util.WorkflowSchemeImportExportObject;
import com.dotmarketing.util.DateUtil;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.StringUtils;
import com.dotmarketing.util.UtilMethods;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Future;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.lang.time.StopWatch;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.StringTerms;


/**
 * Helper for Workflow Actions
 * @author jsanca
 */
public class WorkflowHelper {

    private final WorkflowAPI   workflowAPI;
    private final RoleAPI       roleAPI;
    private final ContentletAPI contentletAPI;
    private final PermissionAPI permissionAPI;
    private final WorkflowImportExportUtil workflowImportExportUtil;

    /**
     * Finds the bulk actions based on the {@link BulkActionForm}
     * @param user {@link User}
     * @param bulkActionForm {@link BulkActionForm}
     * @return BulkActionView
     * @throws DotSecurityException
     * @throws DotDataException
     */
    public BulkActionView findBulkActions(final User user,
                                          final BulkActionForm bulkActionForm) throws DotSecurityException, DotDataException {

        return (UtilMethods.isSet(bulkActionForm.getContentletIds()))?
                    // 1) case with contentlet ids
                    this.findBulkActionByContentlets(bulkActionForm.getContentletIds(), user):
                    // 2) case when the user checks all (using a query)
                    this.findBulkActionByQuery      (bulkActionForm.getQuery(), user);
    }

    private BulkActionView findBulkActionByQuery(final String luceneQuery,
                                                 final User user) throws DotDataException, DotSecurityException {

        //We should only be considering Working content.
        final String query = "{\n"
                + "    \"query\" : { \n"
                + "        \"query_string\" : {\n"
                + "            \"query\" : \"" + luceneQuery + "\"\n"
                + "        } \n"
                + "\n"
                + "    },\n"
                + "    \"aggs\" : {\n"
                + "        \"tag\" : {\n"
                + "            \"terms\" : {\n"
                + "                \"field\" : \"wfstep_dotraw\",\n"
                + "                \"size\" : 1000  \n"
                + "            }\n"
                + "        }\n"
                + "    },\n"
                + "\t\"size\":0   \n"
                + "}";
        final ESSearchResults results = this.contentletAPI.esSearch(query, false, user, false);

        return this.buildBulkActionView(results, user);
    }

    private BulkActionView findBulkActionByContentlets(final List<String> contentletIds,
                                                       final User     user) throws DotSecurityException, DotDataException {

        final String luceneQuery = "+inode:(" + String.join(" ", contentletIds) + ")";
        return this.findBulkActionByQuery(luceneQuery, user);
    }

    private BulkActionView buildBulkActionView (final ESSearchResults results,
                                                final User user) throws DotDataException, DotSecurityException {

        final Aggregations aggregations     = results.getAggregations();
        final Map<String, Long> stepCounts  = new HashMap<>();

        for (final Aggregation aggregation : aggregations.asList()) {

            if (aggregation instanceof StringTerms) {
                StringTerms.class.cast(aggregation)
                        .getBuckets().forEach
                            (bucket -> stepCounts.put(bucket.getKeyAsString(), bucket.getDocCount()));
            }
        }

        final Map<CountWorkflowStep, List<WorkflowAction>> stepActionsMap = new HashMap<>();

        for (final Map.Entry<String, Long> stepCount : stepCounts.entrySet()) {

            final CountWorkflowStep step =
                    new CountWorkflowStep(stepCount.getValue(),
                            this.workflowAPI.findStep(stepCount.getKey()));

            stepActionsMap.put(step, this.workflowAPI.findActions(step.getWorkflowStep(), user));
        }

        return (stepCounts.size() > 0)?
            this.buildBulkActionView(stepActionsMap):
            new BulkActionView(Collections.emptyMap());
    }

    private BulkActionView buildBulkActionView (final Map<CountWorkflowStep, List<WorkflowAction>> stepActionsMap) throws DotSecurityException, DotDataException {

        final Map<String, WorkflowScheme> schemeByIdMap  = new HashMap<>();
        final Map<String, Long>           actionCountMap = new HashMap<>();
        final Map<WorkflowScheme, Map<CountWorkflowStep, List<CountWorkflowAction>>> bulkActions = new HashMap<>();

        for (final Map.Entry<CountWorkflowStep, List<WorkflowAction>> entry : stepActionsMap.entrySet()) {

            if (!schemeByIdMap.containsKey(entry.getKey().getWorkflowStep().getSchemeId())) {
                schemeByIdMap.put(entry.getKey().getWorkflowStep().getSchemeId(),
                        this.workflowAPI.findScheme(entry.getKey().getWorkflowStep().getSchemeId()));
            }

            final Long stepCount = entry.getKey().getCount();
            for (final WorkflowAction action : entry.getValue()) {

                actionCountMap.put(action.getId(),
                        actionCountMap.getOrDefault(action.getId(), 0l) + stepCount);
            }
        }

        for (final Map.Entry<CountWorkflowStep, List<WorkflowAction>> entry : stepActionsMap.entrySet()) {

            final CountWorkflowStep step = entry.getKey();
            final WorkflowScheme scheme  = schemeByIdMap.get(step.getWorkflowStep().getSchemeId());
            final List<CountWorkflowAction> actions = entry.getValue().stream()
                    .map(action -> new CountWorkflowAction(actionCountMap.get(action.getId()), action))
                    .collect(CollectionsUtils.toImmutableList());

            final Map<CountWorkflowStep, List<CountWorkflowAction>> schemeActionsMap =
                        (bulkActions.containsKey(scheme))? bulkActions.get(scheme):new HashMap<>();

            schemeActionsMap.put(step, actions);

            bulkActions.put(scheme, schemeActionsMap);
        }

        return new BulkActionView(bulkActions);
    }

    public Future<BulkActionsResultView> fireBulkActions(final FireBulkActionsForm form,
                                                         final User user) throws DotSecurityException, DotDataException {

        final WorkflowAction action = this.workflowAPI.findAction(form.getWorkflowActionId(), user);
        if(null != action) {
            if(UtilMethods.isSet(form.getQuery())){
                return this.workflowAPI.fireBulkActions(action, user, form.getQuery());
            }
            return this.workflowAPI.fireBulkActions(action, user, form.getContentletIds());
        } else {
            throw new DoesNotExistException("Workflow-does-not-exists-action");
        }
    }


    private static class SingletonHolder {
        private static final WorkflowHelper INSTANCE = new WorkflowHelper();
    }

    public static WorkflowHelper getInstance() {
        return WorkflowHelper.SingletonHolder.INSTANCE;
    }

    private WorkflowHelper() {
        this( APILocator.getWorkflowAPI(),
                APILocator.getRoleAPI(),
                APILocator.getContentletAPI(),
                APILocator.getPermissionAPI(),
                WorkflowImportExportUtil.getInstance());
    }


    @VisibleForTesting
    public WorkflowHelper(final WorkflowAPI      workflowAPI,
                             final RoleAPI       roleAPI,
                             final ContentletAPI contentletAPI,
                             final PermissionAPI permissionAPI,
                             final WorkflowImportExportUtil workflowImportExportUtil) {

        this.workflowAPI   = workflowAPI;
        this.roleAPI       = roleAPI;
        this.contentletAPI = contentletAPI;
        this.permissionAPI = permissionAPI;
        this.workflowImportExportUtil =
                workflowImportExportUtil;
    }

    @WrapInTransaction
    public void importScheme(final WorkflowSchemeImportExportObject workflowExportObject,
                             final List<Permission>                 permissions,
                             final User                             user) throws IOException,
                                DotSecurityException, DotDataException, AlreadyExistException {

        WorkflowAction action = null;

        final StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        Logger.debug(this, () -> "Starting the scheme import");

        final Set<String> schemeIds =
                this.checkIfSchemeExists (workflowExportObject.getSchemes());
        final Set<String> stepIds =
                this.checkSteps (schemeIds, workflowExportObject.getSteps());
        final Set<String> actionIds =
                this.checkActions (schemeIds, workflowExportObject.getActions(), user);
        this.checkActionSteps (workflowExportObject.getActionSteps(), stepIds, actionIds, user);

        this.workflowImportExportUtil.importWorkflowExport(workflowExportObject, user);

        stopWatch.stop();
        Logger.debug(this, () -> "Ended the scheme import, in: " + DateUtil.millisToSeconds(stopWatch.getTime()));

        stopWatch.reset();
        stopWatch.start();
        Logger.debug(this, () -> "Starting the action permissions import");
        for (final Permission permission: permissions) {

            action = this.workflowAPI.findAction(permission.getInode(), user);
            if (null != action) {

                this.permissionAPI.save(permission, action, APILocator.getUserAPI().getSystemUser(), false);
            } else {

                throw new DoesNotExistException("The action: " + action + " on the permission: "
                        + permission + ", does not exists");
            }
        }

        stopWatch.stop();
        Logger.debug(this, () -> "Ended the action permissions import, in: " + DateUtil.millisToSeconds(stopWatch.getTime()));
    } // importScheme.

    private void checkActionSteps(final List<Map<String, String>> actionSteps,
                                  final Set<String> stepIds,
                                  final Set<String> actionIds,
                                  final User user) throws AlreadyExistException {

        for (final Map<String, String> actionStep : actionSteps) {

            final String stepId   = actionStep.get("stepId");
            final String actionId = actionStep.get("actionId");

            WorkflowAction repeatAction = null;
            try {
                repeatAction =
                        this.workflowAPI.findAction(actionId, user);
            } catch (Exception e) {
                repeatAction = null;
            }

            if(null != repeatAction) {

                throw new AlreadyExistException("Already exist an action with the same id ("+repeatAction.getId()
                        +"). Create different action with the same id is not allowed. Please change your workflow action id.");
            }

            if (!actionIds.contains(actionId)) {

                throw new IllegalArgumentException("Does not exists the action " + actionId + " on the request ");
            }

            // step
            WorkflowStep repeatStep = null;
            try {
                repeatStep =
                        this.workflowAPI.findStep(stepId);
            } catch (Exception e) {
                repeatStep = null;
            }

            if(null != repeatStep) {

                throw new AlreadyExistException("Already exist a step with the same id ("+repeatStep.getId()
                        +"). Create different step with the same id is not allowed. Please change your workflow step id.");
            }

            if (!stepIds.contains(stepId)) {

                throw new IllegalArgumentException("Does not exists the step " + stepId + " on the request ");
            }
        }
    }

    private Set<String> checkActions(final Set<String> schemeIds, final List<WorkflowAction> actions, final User user) throws AlreadyExistException {

        final ImmutableSet.Builder<String> setBuilder = new ImmutableSet.Builder<>();

        for (final WorkflowAction action : actions) {

            WorkflowAction repeatAction = null;
            try {
                repeatAction =
                        this.workflowAPI.findAction(action.getId(), user);
            } catch (Exception e) {
                repeatAction = null;
            }

            if(null != repeatAction) {

                throw new AlreadyExistException("Already exist an action with the same id ("+repeatAction.getId()
                        +"). Create different action with the same id is not allowed. Please change your workflow action id.");
            }

            if (!schemeIds.contains(action.getSchemeId())) {

                throw new IllegalArgumentException("Does not exists the scheme " + action.getSchemeId() + " on the action "+ action.getId()
                        +". Create a action with a right scheme id.");
            }

            setBuilder.add(action.getId());
        }

        return setBuilder.build();
    }

    private Set<String> checkSteps(final Set<String> schemeIds, final List<WorkflowStep> steps) throws AlreadyExistException {

        final ImmutableSet.Builder<String> setBuilder = new ImmutableSet.Builder<>();

        for (final WorkflowStep step : steps) {

            WorkflowStep repeatStep = null;
            try {
                repeatStep =
                        this.workflowAPI.findStep(step.getId());
            } catch (Exception e) {
                repeatStep = null;
            }

            if(null != repeatStep) {

                throw new AlreadyExistException("Already exist a step with the same id ("+repeatStep.getId()
                        +"). Create different step with the same id is not allowed. Please change your workflow step id.");
            }

            if (!schemeIds.contains(step.getSchemeId())) {

                throw new IllegalArgumentException("Does not exists the scheme " + step.getSchemeId() + " on the step "+ step.getId()
                        +". Create a step with a right scheme id.");
            }

            setBuilder.add(step.getId());
        }

        return setBuilder.build();
    }

    private Set<String> checkIfSchemeExists(final List<WorkflowScheme> schemes) throws AlreadyExistException {


        final ImmutableSet.Builder<String> setBuilder = new ImmutableSet.Builder<>();

        for (final WorkflowScheme scheme : schemes) {

            WorkflowScheme repeatScheme = null;
            try {
                repeatScheme =
                        this.workflowAPI.findScheme(scheme.getId());
            } catch (Exception e) {
                repeatScheme = null;
            }

            if(null != repeatScheme) {

                throw new AlreadyExistException("Already exist a scheme with the same id ("+repeatScheme.getId()
                        +"). Create different schemes with the same id is not allowed.");
            }

            setBuilder.add(scheme.getId());
        }

        return setBuilder.build();
    }


    /**
     * Finds the available actions for an inode and user.
     * @param inode String
     * @param user  User
     * @return List of WorkflowAction
     */
    @CloseDBIfOpened
    public List<WorkflowAction> findAvailableActions(final String inode, final User user) {
        try {
            Logger.debug(this, ()->"Asking for the available actions for the inode: " + inode);

            if (!UtilMethods.isSet(inode)) {
                throw new IllegalArgumentException("Missing required parameter inode.");
            }

            final Contentlet contentlet = this.contentletAPI.find(inode, user, true);
            if(contentlet == null){
               throw new DoesNotExistException(String.format("Contentlet identified by inode '%s' was Not found.",inode));
            }
            return this.workflowAPI.findAvailableActions(contentlet, user);
        } catch (DotDataException  | DotSecurityException e) {
            Logger.error(this, e.getMessage());
            Logger.debug(this, e.getMessage(), e);
            throw new DotWorkflowException(e.getMessage(), e);
        }

    } // findAvailableActions.


    /**
     *
     * @param actionId
     * @param user
     * @return
     * @throws DotDataException
     * @throws DotSecurityException
     */
    public WorkflowAction findAction(final String actionId, final User user) throws DotDataException, DotSecurityException{

        if (!UtilMethods.isSet(actionId)) {
            final String exceptionMessage = getFormattedMessage(user.getLocale(),"Workflow-required-param-actionId",actionId);
            throw new IllegalArgumentException(exceptionMessage);
        }

        final WorkflowAction action = this.workflowAPI.findAction(actionId, user);
        if(action == null){
           final String exceptionMessage = getFormattedMessage(user.getLocale(),"Workflow-does-not-exists-action-by-actionId",actionId);
           throw new DoesNotExistException(exceptionMessage);
        }
        return action;
    }

    /**
     * Finds an action by step.
     * @param actionId String
     * @param stepId   String
     * @param user     User
     * @return WorkflowAction
     * @throws DotDataException
     * @throws DotSecurityException
     */
    public WorkflowAction findAction(final String actionId, final String stepId, final User user) throws DotDataException, DotSecurityException{

        if (!UtilMethods.isSet(actionId)) {
            final String exceptionMessage = getFormattedMessage(user.getLocale(),"Workflow-required-param-actionId",actionId);
            throw new IllegalArgumentException(exceptionMessage);
        }

        if (!UtilMethods.isSet(stepId)) {
            final String exceptionMessage = getFormattedMessage(user.getLocale(),"Workflow-required-param-stepId",stepId);
            throw new IllegalArgumentException(exceptionMessage);
        }

        final WorkflowAction action = this.workflowAPI.findAction(actionId, stepId, user);
        if(action == null){
            final String exceptionMessage = getFormattedMessage(user.getLocale(),"Workflow-does-not-exists-action-by-actionId-stepId",actionId, stepId);
            throw new DoesNotExistException(exceptionMessage);
        }
        return action;
    }

    /**
     * Get the {@link Permission}'s for the {@link WorkflowAction}'s. This is used by the export
     * @param workflowActions List of {@link WorkflowAction}
     * @return List {@link Permission}
     * @throws DotDataException
     */
    public List<Permission> getActionsPermissions (final List<WorkflowAction> workflowActions) throws DotDataException {

        final ImmutableList.Builder permissions =
                new ImmutableList.Builder();

        for (final WorkflowAction action : workflowActions) {
            permissions.addAll(this.permissionAPI.getPermissions(action));
        }

        return permissions.build();
    }

    /**
     * Reorder the action associated to the scheme.
     * @param workflowReorderActionStepForm WorkflowReorderActionStepForm
     */
    @WrapInTransaction
    public void reorderAction(final WorkflowReorderBean workflowReorderActionStepForm,
            final User user) throws DotSecurityException {

        WorkflowAction action = null;
        WorkflowStep step = null;

        try {
            Logger.debug(this, () -> "Looking for the actionId: "
                    + workflowReorderActionStepForm.getActionId());
            action = this.workflowAPI.findAction(workflowReorderActionStepForm.getActionId(), user);
        } catch (InvalidLicenseException e) {
            Logger.debug(this, e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            Logger.error(this, e.getMessage());
            Logger.debug(this, e.getMessage(), e);
        }

        try {
            Logger.debug(this, () -> "Looking for the stepId: "
                    + workflowReorderActionStepForm.getStepId());
            step = this.workflowAPI.findStep(workflowReorderActionStepForm.getStepId());
        } catch (Exception e) {
            Logger.error(this, e.getMessage());
            Logger.debug(this, e.getMessage(), e);
        }

        if (null == action) {
            throw new DoesNotExistException("Workflow-does-not-exists-action");
        }

        if (null == step) {
            throw new DoesNotExistException("Workflow-does-not-exists-step");
        }

        Logger.debug(this,  "Reordering the action: " + action.getId()
                + " for the stepId: " + step.getId() + ", order: " +
                workflowReorderActionStepForm.getOrder()
        );

        try {
            this.workflowAPI
                    .reorderAction(action, step, user, workflowReorderActionStepForm.getOrder());
        } catch (DotDataException | AlreadyExistException e) {

            Logger.error(this, e.getMessage());
            Logger.debug(this, e.getMessage(), e);
            throw new DotWorkflowException(e.getMessage(), e);
        }
    }  // reorderAction.


    /**
     * Reorder the action associated to the scheme.
     * @param stepId  String step id
     * @param order   int    order for the step
     * @param user   User    user that wants the reorder
     */
    @WrapInTransaction
    public void reorderStep(final String stepId,
                            final int order,
                            final User user)  {

        final WorkflowStep step;
        try {
            Logger.debug(this, () -> "Looking for the stepId: " + stepId);
            step = this.workflowAPI.findStep(stepId);
            Logger.debug(this, () -> "Reordering the stepId: "  + stepId +
                    ", order: " + order);
            this.workflowAPI.reorderStep(step, order, user);
        }catch (DoesNotExistException dne){
            Logger.error(this, dne.getMessage());
            Logger.debug(this, dne.getMessage(), dne);
            throw dne;
        } catch (DotDataException | AlreadyExistException | DotSecurityException e) {
            Logger.error(this, e.getMessage());
            Logger.debug(this, e.getMessage(), e);
            throw new DotWorkflowException(e.getMessage(), e);
        }
    }  // reorderAction.

    /**
     * copy contents from the form into the step
     * @param step
     * @param workflowStepUpdateForm
     * @return
     */
    private WorkflowStep populateStep(final WorkflowStep step, final IWorkflowStepForm workflowStepUpdateForm){
        if (workflowStepUpdateForm.isEnableEscalation()) {
            step.setEnableEscalation(true);
            step.setEscalationAction(workflowStepUpdateForm.getEscalationAction());
            step.setEscalationTime(Integer.parseInt(workflowStepUpdateForm.getEscalationTime()));
        } else {
            step.setEnableEscalation(false);
            step.setEscalationAction(null);
            step.setEscalationTime(0);
        }
        step.setName(workflowStepUpdateForm.getStepName());
        step.setResolved(workflowStepUpdateForm.isStepResolved());
        return step;
    }

    /**
     * Adds a brand new step to a workflow scheme
     * @param workflowStepUpdateForm
     * @param user
     * @return
     */
    public WorkflowStep addStep(final WorkflowStepAddForm workflowStepUpdateForm, final User user) {
        final String schemeId = workflowStepUpdateForm.getSchemeId();
        WorkflowStep step = new WorkflowStep();
        step = populateStep(step, workflowStepUpdateForm);

        try {
            final WorkflowScheme scheme    = workflowAPI.findScheme(schemeId);
            final List<WorkflowStep> steps = workflowAPI.findSteps(scheme);
            step.setSchemeId(scheme.getId());

            final Optional<WorkflowStep> optional = steps.stream().max(Comparator.comparing(WorkflowStep::getMyOrder));
            step.setMyOrder(
                    optional.map(workflowStep -> (workflowStep.getMyOrder() + 1)).orElse(0)
            );

            workflowAPI.saveStep(step, user);
        } catch (DotDataException | AlreadyExistException | DotSecurityException e) {
            Logger.error(this, e.getMessage());
            Logger.debug(this, e.getMessage(), e);
            throw new DotWorkflowException(e.getMessage(), e);
        }
        return step;
    }

    /**
     *
     * @param stepId
     * @return
     */
    public WorkflowStep findStepById(final String stepId) throws DotDataException, DotSecurityException{
        WorkflowStep step;
        try {
            step = workflowAPI.findStep(stepId);
        } catch (IndexOutOfBoundsException e){
            throw new DoesNotExistException("Workflow-does-not-exists-step", e);
        }
        return step;
    }

    /**
     *
     * @param stepId
     * @param workflowStepUpdateForm
     * @throws DotDataException
     * @throws AlreadyExistException
     */
    public WorkflowStep updateStep(final String stepId, final WorkflowStepUpdateForm workflowStepUpdateForm, final User user) throws DotDataException, AlreadyExistException, DotSecurityException {
        final WorkflowStep step;
        try {
            step = workflowAPI.findStep(stepId);
        } catch (DotDataException dde) {
            throw new DoesNotExistException(dde);
        }
        return updateStep(step, workflowStepUpdateForm, user);
    }

    /**
     *
     * @param step
     * @param workflowStepUpdateForm
     * @throws DotDataException
     * @throws AlreadyExistException
     */
    @WrapInTransaction
    public WorkflowStep updateStep(WorkflowStep step, final WorkflowStepUpdateForm workflowStepUpdateForm, final User user) throws DotDataException, AlreadyExistException {
        if (step.isNew()) {
            throw new DotWorkflowException("Cannot-edit-step");
        }
        final Integer order = workflowStepUpdateForm.getStepOrder();
        step = populateStep(step, workflowStepUpdateForm);
        step.setMyOrder(workflowStepUpdateForm.getStepOrder());
        try {
            workflowAPI.reorderStep(step, order, user);
        } catch (Exception e) {
            workflowAPI.saveStep(step, user);
        }
        return step;
    }

    /**
     * Deletes the step
     * @param stepId String
     */
    @WrapInTransaction
    public Future<WorkflowStep> deleteStep(final String stepId, final User user) throws DotSecurityException, DotDataException {

        if (!UtilMethods.isSet(stepId)) {
            throw new IllegalArgumentException("Missing required parameter stepId.");
        }

        WorkflowStep workflowStep = null;
        Logger.debug(this, "Looking for the stepId: " + stepId);
        try {
            workflowStep = this.workflowAPI.findStep(stepId);
        } catch (IndexOutOfBoundsException e) {
            throw new DoesNotExistException("Workflow-does-not-exists-step");
        }
        if (null != workflowStep) {
            try {
                Logger.debug(this, "deleting step: " + stepId);
                return this.workflowAPI.deleteStep(workflowStep, user);
            } catch (DotDataException e) {
                Logger.error(this, e.getMessage());
                Logger.debug(this, e.getMessage(), e);
                throw new DotWorkflowException(e.getMessage(), e);
            }
        } else {

            throw new DoesNotExistException("Workflow-does-not-exists-step");
        }
    } // deleteStep.

    /**
     * Deletes the action which is part of the step, but the action still being part of the scheme.
     * @param actionId String action id
     * @param user     User   the user that makes the request
     * @return WorkflowStep
     */
    @WrapInTransaction
    public void deleteAction(final @NotNull String actionId,
            final User user) throws DotSecurityException, DotDataException {

        if (!UtilMethods.isSet(actionId)) {
            throw new IllegalArgumentException("Missing required parameter actionId.");
        }

        Logger.debug(this, () -> "Looking for the action: " + actionId);
        final WorkflowAction action = this.workflowAPI.findAction(actionId, user);

        if (null != action) {

            try {
                Logger.debug(this, () -> "Deleting the action: " + actionId);
                this.workflowAPI.deleteAction(action, user);
            } catch (DotDataException | AlreadyExistException e) {

                Logger.error(this, e.getMessage());
                Logger.debug(this, e.getMessage(), e);
                throw new DotWorkflowException(e.getMessage(), e);
            }
        } else {

            throw new DoesNotExistException("Workflow-does-not-exists-action");
        }
    } // deleteAction.

    /**
     * Deletes the action which is part of the step, but the action still being part of the scheme.
     * @param actionId String action id
     * @param stepId   String step   id
     * @param user     User   the user that makes the request
     * @return WorkflowStep
     */
    @WrapInTransaction
    public WorkflowStep deleteAction(final @NotNull String actionId,
                                     final @NotNull String stepId,
                                     final User user) {

        if (!UtilMethods.isSet(actionId)) {
            throw new IllegalArgumentException("Missing required parameter actionId.");
        }

        if (!UtilMethods.isSet(stepId)) {
            throw new IllegalArgumentException("Missing required parameter stepId.");
        }

        WorkflowAction action = null;
        WorkflowStep step     = null;

        try {

            Logger.debug(this, () -> "Looking for the actionId: " + actionId);
            action =
                    this.workflowAPI.findAction(actionId, user);

            Logger.debug(this, () -> "Looking for the stepId: " + stepId);
            step =
                    this.workflowAPI.findStep(stepId);

            if (null == action) {
                throw new DoesNotExistException("Workflow-does-not-exists-action");
            }

            if (null == step) {
                throw new DoesNotExistException("Workflow-does-not-exists-step");
            }

            Logger.debug(this, () -> "Deleting the action: " + actionId
                    + " for the stepId: " + stepId);

            this.workflowAPI.deleteAction(action, step, user);
        } catch (DotDataException | DotSecurityException | AlreadyExistException e) {

            Logger.error(this, e.getMessage());
            Logger.debug(this, e.getMessage(), e);
            throw new DotWorkflowException(e.getMessage(), e);
        }

        return step;
    } // deleteAction.

    /**
     * Find Schemes by content type id
     * @param contentTypeId String
     * @param user          User   the user that makes the request
     * @return List
     */
    public List<WorkflowScheme> findSchemesByContentType(final String contentTypeId,
                                                         final User   user) {

        final ContentTypeAPI contentTypeAPI = APILocator.getContentTypeAPI(user);
        try {

            Logger.debug(this, () -> "Getting the schemes by content type: " + contentTypeId);

            return this.workflowAPI.findSchemesForContentType
                    (contentTypeAPI.find(contentTypeId));
        } catch (DotDataException | DotSecurityException e) {

            Logger.error(this, e.getMessage());
            Logger.debug(this, e.getMessage(), e);
            throw new DotWorkflowException(e.getMessage(), e);
        }

    } // findSchemesByContentType.

    public void saveSchemesByContentType(final String contentTypeId, final User user, final List<String> workflowIds) {

        final ContentTypeAPI contentTypeAPI = APILocator.getContentTypeAPI(user);

        try {

            Logger.debug(this, () -> String.format("Saving the schemes: %s by content type: %s",
                    String.join(",", workflowIds), contentTypeId));

            this.workflowAPI.saveSchemeIdsForContentType(contentTypeAPI.find(contentTypeId), workflowIds);
        } catch (DotDataException | DotSecurityException e) {

            Logger.error(this, e.getMessage());
            Logger.debug(this, e.getMessage(), e);
            throw new DotWorkflowException(e.getMessage(), e);
        }

    }

    public List<WorkflowScheme> findSchemes() {
        return findSchemes(false);
    }

    /**
     * Finds the all the schemes if showArchived is true, otherwise it return only non-archived schemes
     * @return List
     */
    public List<WorkflowScheme> findSchemes(boolean showArchived) {

        List<WorkflowScheme> schemes = null;

        try {

            Logger.debug(this, () -> "Getting all non-archived schemes");
            schemes =
                    this.workflowAPI.findSchemes(showArchived);
        } catch (DotDataException e) {

            Logger.error(this, e.getMessage());
            Logger.debug(this, e.getMessage(), e);
            throw new DotWorkflowException(e.getMessage(), e);
        }

        return schemes;
    } // findSchemes.

    /**
     * Finds the action associated to the stepId
     * @param stepId String
     * @param user   User
     * @return List of WorkflowAction
     */
    public List<WorkflowAction> findActions(final String stepId, final User user)
            throws DotSecurityException, DotDataException {

        List<WorkflowAction> actions = null;

        Logger.debug(this, () -> "Looking for the stepId: " + stepId);
        WorkflowStep workflowStep = this.workflowAPI.findStep(stepId);

        if (null != workflowStep) {

            try {

                Logger.debug(this, () -> "Looking for the actions associated to the step: " + stepId);
                actions = this.workflowAPI.findActions(workflowStep, user);
            } catch (DotDataException | DotSecurityException e) {

                Logger.error(this, e.getMessage());
                Logger.debug(this, e.getMessage(), e);
                throw new DotWorkflowException(e.getMessage(), e);
            }
        } else {
            throw new DoesNotExistException("Workflow-does-not-exists-step");
        }

        return (null == actions) ? Collections.emptyList() : actions;
    } // findActions.

    /**
     * Finds the steps by schemeId
     * @param schemeId String
     * @return List of WorkflowStep
     */
    public List<WorkflowStep> findSteps(final String schemeId)
            throws DotSecurityException, DotDataException {

        Logger.debug(this, () -> "Looking for the schemeId: " + schemeId);

        if (!UtilMethods.isSet(schemeId)) {
            throw new IllegalArgumentException("Missing required parameter schemeId.");
        }

        final WorkflowScheme workflowScheme = this.workflowAPI.findScheme(schemeId);
        final List<WorkflowStep> workflowSteps;
        if (null != workflowScheme) {

            try {
                workflowSteps = this.workflowAPI.findSteps(workflowScheme);
            } catch (DotDataException e) {

                Logger.error(this, e.getMessage());
                Logger.debug(this, e.getMessage(), e);
                throw new DotWorkflowException(e.getMessage(), e);
            }
        } else {

            throw new DoesNotExistException("Workflow-does-not-exists-scheme");
        }

        return workflowSteps;
    } // findSteps.

    /**
     * Returns if the action associated to the actionId parameter is new and clone the retrieved action into a new one.
     * @param actionId String
     * @return IsNewAndCloneItResult
     */
    private IsNewAndCloneItResult isNewAndCloneIt(final String actionId) {


        final WorkflowAction newAction = new WorkflowAction();
        boolean isNew = true;

        try {

            final WorkflowAction origAction = this.workflowAPI.findAction
                    (actionId, APILocator.getUserAPI().getSystemUser());
            BeanUtils.copyProperties(newAction, origAction);
            isNew = !(origAction !=null || !origAction.isNew());
        } catch (Exception e) {

            Logger.debug(this.getClass(), () -> "Unable to find action" + actionId);
        }

        return new IsNewAndCloneItResult(isNew, newAction);
    } // isNewAndCloneIt.

    /**
     * Resolve the role based on the id
     * @param id String
     * @return Role
     * @throws DotDataException
     */
    private Role resolveRole(final String id) throws DotDataException {

        Role role = null;
        final String newid = id.substring
                (id.indexOf("-") + 1, id.length());

        if(id.startsWith("user-")) {

            role = this.roleAPI.loadRoleByKey(newid);
        } else if(id.startsWith("role-")) {

            role = this.roleAPI.loadRoleById (newid);
        } else {

            role = this.roleAPI.loadRoleById (id);
        }

        return role;
    } // resolveRole.

    /**
     * Finds the actions by scheme
     * @param schemeId String
     * @param user     User
     * @return List of WorkflowAction
     */
    public List<WorkflowAction> findActionsByScheme(@NotNull final String schemeId,
                                                    final User user) {

        if (!UtilMethods.isSet(schemeId)) {
            throw new IllegalArgumentException("Missing required parameter schemeId.");
        }
        final List<WorkflowAction> actions;
        WorkflowScheme scheme = null;

        try {

            scheme  = this.workflowAPI.findScheme(schemeId);
            actions =
                    this.workflowAPI.findActions(scheme, (null == user)?
                            APILocator.getUserAPI().getSystemUser(): user);
        } catch (DotDataException | DotSecurityException e) {

            Logger.error(this.getClass(), e.getMessage());
            Logger.debug(this, e.getMessage(), e);
            throw new DotWorkflowException(e.getMessage(), e);
        }

        return actions;
    } // findActionsByScheme.

    /**
     * This method performs an additional check to verify if the action we're attempting to update truly exists
     * if it does not a new DoesNotExistException is thrown
     * if the action is valid the regular saveAction takes place
     * @param actionId String
     * @param workflowActionForm WorkflowActionForm
     * @param user User
     * @return WorkflowAction
     */
    @WrapInTransaction
    public WorkflowAction updateAction(final String actionId, final WorkflowActionForm workflowActionForm, final User user) {
        WorkflowAction action = null;
        try {
            if(workflowAPI.findAction(actionId, user) != null){
               action = saveAction(actionId, workflowActionForm, user);
            } else {
                throw new DoesNotExistException("Workflow-does-not-exists-action");
            }
        } catch (DotDataException | DotSecurityException e) {
            throw new DotWorkflowException(e.getMessage(), e);
        }
        return action;
    }

    /**
     * Save a WorkflowActionForm returning the WorkflowAction created.
     * A WorkflowActionForm can send a stepId in that case the Action will be associated to the Step in the same transaction.
     * @param actionId When present an update operation takes place otherwise an insert is executed
     * @param workflowActionForm WorkflowActionForm
     * @return WorkflowAction (workflow action created)
     */
    @WrapInTransaction
    public WorkflowAction saveAction(final String actionId, final WorkflowActionForm workflowActionForm, final User user) {

        String actionNextAssign     = workflowActionForm.getActionNextAssign();
        if (actionNextAssign != null && actionNextAssign.startsWith("role-")) {
            actionNextAssign  = actionNextAssign.replaceAll("role-", StringPool.BLANK);
        }

        final WorkflowHelper.IsNewAndCloneItResult isNewAndCloneItResult = this.isNewAndCloneIt(actionId);
        final WorkflowAction newAction = isNewAndCloneItResult.getAction();
        final boolean isNew            = isNewAndCloneItResult.isNew();

        newAction.setName       (workflowActionForm.getActionName());
        newAction.setAssignable (workflowActionForm.isActionAssignable());
        newAction.setCommentable(workflowActionForm.isActionCommentable());
        newAction.setIcon       (workflowActionForm.getActionIcon());
        newAction.setNextStep   (workflowActionForm.getActionNextStep());
        newAction.setSchemeId   (workflowActionForm.getSchemeId());
        newAction.setCondition  (workflowActionForm.getActionCondition());
        newAction.setRequiresCheckout(false);
        newAction.setShowOn((null != workflowActionForm.getShowOn() && !workflowActionForm.getShowOn().isEmpty())?
                workflowActionForm.getShowOn():WorkflowAPI.DEFAULT_SHOW_ON);
        newAction.setRoleHierarchyForAssign(workflowActionForm.isRoleHierarchyForAssign());

        try {

            newAction.setNextAssign(this.resolveRole(actionNextAssign).getId());
            if(!UtilMethods.isSet(newAction.getNextAssign())){
                newAction.setNextAssign(null);
            }

            final List<Permission> permissions = new ArrayList<>();

            for (final String permissionName : workflowActionForm.getWhoCanUse()) {

                if (UtilMethods.isSet(permissionName)) {

                    this.processPermission(newAction, permissions, permissionName);
                }
            }

            Logger.debug(this, () -> "Saving new Action: " + newAction.getName());
            this.workflowAPI.saveAction(newAction, permissions, user);

            if(isNew) {

                // if should be associated to a stepId right now
                if (UtilMethods.isSet(workflowActionForm.getStepId())) {

                    Logger.debug(this, () -> "The Action: " + newAction.getId() +
                            ", is going to be associated to the step: " + workflowActionForm.getStepId());
                    this.workflowAPI.saveAction(newAction.getId(),
                            workflowActionForm.getStepId(), user);
                }

                Logger.debug(this, () -> "Saving new WorkflowActionClass, for the Workflow action: "
                        + newAction.getId());

                addSyncCommitListener(() -> {
                    WorkflowActionClass workflowActionClass = new WorkflowActionClass();
                    workflowActionClass.setActionId(newAction.getId());
                    workflowActionClass.setClazz(NotifyAssigneeActionlet.class.getName());
                    try {
                        workflowActionClass.setName(NotifyAssigneeActionlet.class.newInstance().getName());
                        workflowActionClass.setOrder(0);
                        this.workflowAPI.saveActionClass(workflowActionClass, user);
                    } catch (Exception e) {
                        Logger.error(this.getClass(), e.getMessage());
                        Logger.debug(this, e.getMessage(), e);
                        throw new DotWorkflowException(e.getMessage(), e);
                    }
                });
            }
        } catch (Exception e) {
            Logger.error(this.getClass(), e.getMessage());
            Logger.debug(this, e.getMessage(), e);
            throw new DotWorkflowException(e.getMessage(), e);
        }

        return newAction;
    } // save.

    /**
     *
     * @param workflowActionForm
     * @param user
     * @return
     */
    public WorkflowAction saveAction(final WorkflowActionForm workflowActionForm, final User user) {
        final String actionId = workflowActionForm.getActionId();
        return saveAction(actionId, workflowActionForm, user);
    }

    @WrapInTransaction
    public void saveActionToStep(final WorkflowActionStepBean workflowActionStepForm, final User user) throws DotDataException, DotSecurityException {

        final String actionId = workflowActionStepForm.getActionId();
        final String stepId = workflowActionStepForm.getStepId();

        if (!UtilMethods.isSet(actionId) || !UtilMethods.isSet(stepId)) {
            throw new IllegalArgumentException("Missing required parameter actionId or stepId.");
        }

        final WorkflowAction action = this.workflowAPI.findAction(actionId, stepId, user);
        if(action != null) {
            WorkflowReorderBean bean = new WorkflowReorderBean.Builder()
            .actionId(actionId).stepId(stepId)
            .order(workflowActionStepForm.getOrder()).build();

            this.reorderAction(bean, user);
            
        }else {

        this.workflowAPI.saveAction(actionId,
                stepId, user, workflowActionStepForm.getOrder());
        }
    } // addActionToStep.



    private void processPermission(final WorkflowAction newAction,
                                   final List<Permission> permissions,
                                   final String permissionName) throws DotDataException {

        final Role role = this.resolveRole(permissionName);
        final Permission permission =
                new Permission(newAction.getId(), role.getId(), PermissionAPI.PERMISSION_USE);

        boolean exists = false;
        for (final Permission permissionItem : permissions) {
            exists = exists || permissionItem.getRoleId().equals(permission.getRoleId());
        }

        if (!exists) {
            permissions.add(permission);
        }
    } // processPermission.

    public class IsNewAndCloneItResult {

        final boolean        isNew;
        final WorkflowAction action;

        public IsNewAndCloneItResult(final boolean isNew,
                                     final WorkflowAction action) {
            this.isNew = isNew;
            this.action = action;
        }

        public boolean isNew() {
            return isNew;
        }

        public WorkflowAction getAction() {
            return action;
        }
    } // IsNewAndCloneItResult.

    /**
     * Find Availables actions that can be used as default action by content type id
     * @param contentTypeId String with the content type Id
     * @param user          User   the user that makes the request
     * @return List<WorkflowDefaultActionView>
     */
    public List<WorkflowDefaultActionView> findAvailableDefaultActionsByContentType(final String contentTypeId, final User   user) {
        final ContentTypeAPI contentTypeAPI = APILocator.getContentTypeAPI(user);
        final ImmutableList.Builder<WorkflowDefaultActionView> results = new ImmutableList.Builder<>();
        try {

            Logger.debug(this, () -> "Getting the available default workflows actions by content type: " + contentTypeId);
            //When no license is available. This should get the system workflow.
            final List<WorkflowAction> actions = this.workflowAPI.findAvailableDefaultActionsByContentType(contentTypeAPI.find(contentTypeId), user);
            for (final WorkflowAction action : actions){
                final WorkflowScheme scheme = this.workflowAPI.findScheme(action.getSchemeId());
                final WorkflowDefaultActionView value = new WorkflowDefaultActionView(scheme, action);
                results.add(value);
            }

        } catch (DotDataException | DotSecurityException e) {

            Logger.error(this, e.getMessage());
            Logger.debug(this, e.getMessage(), e);
            throw new DotWorkflowException(e.getMessage(), e);
        }

        return results.build();
    }

    /**
     * Find Available actions that can be used as default action by Workflow schemes
     * @param schemeIds     Comma separated list of workflow schemes Ids to process
     * @param user          User   the user that makes the request
     * @return List<WorkflowDefaultActionView>
     */
    public List<WorkflowDefaultActionView> findAvailableDefaultActionsBySchemes(
            final @NotNull String schemeIds, final User user) {

        if (!UtilMethods.isSet(schemeIds)) {
            throw new IllegalArgumentException("Missing required parameter schemeIds.");
        }

        final ImmutableList.Builder<WorkflowScheme> schemes = new ImmutableList.Builder<>();
        final ImmutableList.Builder<WorkflowDefaultActionView> results = new ImmutableList.Builder<>();
        try {
            Logger.debug(this,
                    () -> "Getting the available workflows default actions by schemeIds: " + schemeIds);

            //If no valid license is found we simply return the system wf.
            if (!workflowAPI.hasValidLicense()) {
                schemes.add(workflowAPI.findSystemWorkflowScheme());
            } else {
                for (final String id : schemeIds.split(",")) {
                    schemes.add(this.workflowAPI.findScheme(id));
                }
            }

            final List<WorkflowAction> actions = this.workflowAPI
                    .findAvailableDefaultActionsBySchemes(schemes.build(),
                            APILocator.getUserAPI().getSystemUser());
            for (final WorkflowAction action : actions) {
                final WorkflowScheme scheme = this.workflowAPI.findScheme(action.getSchemeId());
                final WorkflowDefaultActionView value = new WorkflowDefaultActionView(scheme,
                        action);
                results.add(value);
            }
        } catch (DotDataException | DotSecurityException e) {

            Logger.error(this, e.getMessage());
            Logger.debug(this, e.getMessage(), e);
            throw new DotWorkflowException(e.getMessage(), e);
        }

        return results.build();
    }

    /**
     * Finds the available actions of the initial/first step(s) of the workflow scheme(s) associated
     * with a content type Id and user.
     *
     * @param contentTypeId String Content Type Id
     * @param user User
     * @return List of WorkflowAction
     */
    @CloseDBIfOpened
    public List<WorkflowDefaultActionView> findInitialAvailableActionsByContentType(
            final @NotNull String contentTypeId, final User user) {

        if (!UtilMethods.isSet(contentTypeId)) {
            throw new IllegalArgumentException("Missing required parameter contentTypeId.");
        }

        final ContentTypeAPI contentTypeAPI = APILocator.getContentTypeAPI(user);
        final ImmutableList.Builder<WorkflowDefaultActionView> results = new ImmutableList.Builder<>();
        try {

            Logger.debug(this,
                    () -> "Asking for the available actions for the content type Id: " + contentTypeId);
            final ContentType contentType = contentTypeAPI.find(contentTypeId);

            if (null == contentType) {
                throw new DoesNotExistException("Workflow-does-not-exists-content-type");
            }

            final List<WorkflowAction> actions = this.workflowAPI
                    .findInitialAvailableActionsByContentType(contentType, user);

            for (final WorkflowAction action : actions) {
                final WorkflowScheme scheme = this.workflowAPI.findScheme(action.getSchemeId());
                final WorkflowDefaultActionView value = new WorkflowDefaultActionView(scheme, action);
                results.add(value);
            }

        } catch (DotDataException | DotSecurityException e) {

            Logger.error(this, e.getMessage());
            Logger.debug(this, e.getMessage(), e);
            throw new DotWorkflowException(e.getMessage(), e);
        }

        return results.build();
    } // findInitialAvailableActionsByContentType.


    /**
     * Saves an existing scheme or create a new one
     * @param schemeId a new scheme is created when null is passed otherwise attempts to update one
     * @param workflowSchemeForm
     * @param user
     * @return
     * @throws AlreadyExistException
     * @throws DotDataException
     * @throws DotSecurityException
     */
    public WorkflowScheme saveOrUpdate(final String schemeId, final WorkflowSchemeForm workflowSchemeForm, final User user) throws AlreadyExistException, DotDataException, DotSecurityException {

        final WorkflowScheme newScheme = new WorkflowScheme();
        if (StringUtils.isSet(schemeId)) {
            try {
                final WorkflowScheme origScheme = this.workflowAPI.findScheme(schemeId);
                BeanUtils.copyProperties(newScheme, origScheme);
            } catch (Exception e) {
                Logger.debug(this.getClass(), () -> "Unable to find scheme" + schemeId);
                throw new DoesNotExistException(e.getMessage(), e);
            }
        }

        newScheme.setArchived(workflowSchemeForm.isSchemeArchived());
        newScheme.setDescription(workflowSchemeForm.getSchemeDescription());
        newScheme.setName(workflowSchemeForm.getSchemeName());

        this.workflowAPI.saveScheme(newScheme, user);
        return newScheme;
    }

    /**
     * Delete an existing scheme
     * @param schemeId The id of the Scheme to be deleted
     * @param user The User that want to delete the scheme
     * @return Future {@link WorkflowScheme}
     * @throws AlreadyExistException
     * @throws DotDataException
     * @throws DotSecurityException
     */
    public Future<WorkflowScheme> delete(final String schemeId, final User user)
            throws AlreadyExistException, DotDataException, DotSecurityException {

        final WorkflowScheme scheme = this.workflowAPI.findScheme(schemeId);

        if(null != scheme) {
            return this.workflowAPI.deleteScheme(scheme, user);
        } else {
            throw new DoesNotExistException("Workflow-does-not-exists-scheme");
        }
    } // delete.


} // E:O:F:WorkflowHelper.