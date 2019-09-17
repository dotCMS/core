package com.dotcms.workflow.helper;

import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.business.WrapInTransaction;
import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.contenttype.exception.NotFoundInDbException;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.enterprise.license.LicenseManager;
import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.repackage.javax.validation.constraints.NotNull;
import com.dotcms.rest.api.v1.workflow.*;
import com.dotcms.rest.exception.BadRequestException;
import com.dotcms.rest.exception.InternalServerException;
import com.dotcms.util.CollectionsUtils;
import com.dotcms.uuid.shorty.ShortyId;
import com.dotcms.workflow.form.*;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.RoleAPI;
import com.dotmarketing.exception.*;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.business.DotContentletStateException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.transform.ContentletToMapTransformer;
import com.dotmarketing.portlets.workflows.actionlet.NotifyAssigneeActionlet;
import com.dotmarketing.portlets.workflows.actionlet.WorkFlowActionlet;
import com.dotmarketing.portlets.workflows.business.DotWorkflowException;
import com.dotmarketing.portlets.workflows.business.SystemWorkflowConstants;
import com.dotmarketing.portlets.workflows.business.WorkflowAPI;
import com.dotmarketing.portlets.workflows.model.*;
import com.dotmarketing.portlets.workflows.util.WorkflowImportExportUtil;
import com.dotmarketing.portlets.workflows.util.WorkflowSchemeImportExportObject;
import com.dotmarketing.util.*;
import com.dotmarketing.util.web.VelocityWebUtil;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.liferay.portal.language.LanguageException;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.lang.time.StopWatch;
import org.apache.velocity.context.Context;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.StringTerms;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.StringWriter;
import java.util.*;
import java.util.concurrent.Future;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.dotcms.rest.api.v1.authentication.ResponseUtil.getFormattedMessage;
import static com.dotmarketing.db.HibernateUtil.addSyncCommitListener;


/**
 * Helper for Workflow Actions
 * @author jsanca
 */
public class WorkflowHelper {

    private static final String INVALID_RENDER_MODE = "Invalid Render Mode";
    private final WorkflowAPI   workflowAPI;
    private final RoleAPI       roleAPI;
    private final ContentletAPI contentletAPI;
    private final PermissionAPI permissionAPI;
    private final WorkflowImportExportUtil workflowImportExportUtil;

    private static final String ES_WFSTEP_AGGREGATES_QUERY = "{\n"
            + "    \"query\" : { \n"
            + "        \"query_string\" : {\n"
            + "            \"query\" : \"%s\"\n"
            + "        } \n"
            + "\n"
            + "    },\n"
            + "    \"aggs\" : {\n"
            + "        \"tag\" : {\n"
            + "            \"terms\" : {\n"
            + "                \"field\" : \"wfstep\",\n"
            + "                \"size\" : 1000  \n"
            + "            }\n"
            + "        }\n"
            + "    },\n"
            + "\t\"size\":0   \n"
            + "}";

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
                    this.findBulkActionByQuery(bulkActionForm.getQuery(), user);
    }


    /**
     * Finds the bulk actions based on the luceneQuery
     * @param luceneQuery
     * @param user
     * @return
     * @throws DotDataException
     * @throws DotSecurityException
     */
    private BulkActionView findBulkActionByQuery(final String luceneQuery,
            final User user) throws DotDataException {

        try {
            final String sanitizedQuery = LuceneQueryUtils
                    .sanitizeBulkActionsQuery(luceneQuery);

            final String query = String.format(ES_WFSTEP_AGGREGATES_QUERY, sanitizedQuery);
            //We should only be considering Working content.
            final SearchResponse response = LicenseManager.getInstance().isCommunity()?
                    this.contentletAPI
                            .esSearch(query.toLowerCase(), false, user, false).getResponse():
                    this.contentletAPI
                            .esSearchRaw(query.toLowerCase(), false, user, false);
            //Query must be sent lowercase. It's a must.

            Logger.debug(getClass(), () -> "luceneQuery: " + sanitizedQuery);
            return this.buildBulkActionView(response, user);
        } catch (Exception e) {
            throw new DotDataException(e);
        }
    }

    /**
     * Finds the bulk actions based on the contentletIds
     * @param contentletIds
     * @param user
     * @return
     * @throws DotSecurityException
     * @throws DotDataException
     */
    private BulkActionView findBulkActionByContentlets(final List<String> contentletIds,
                                                       final User     user) throws DotDataException {

        final String luceneQuery =  String.format("+inode:( %s ) ", String.join(StringPool.SPACE, contentletIds));
        return this.findBulkActionByQuery(luceneQuery, user);
    }

    /**
     * Computes a view out of the results returned bylucene
     * @param response
     * @param user
     * @return
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @CloseDBIfOpened
    private BulkActionView buildBulkActionView (final SearchResponse response,
                                                final User user) throws DotDataException, DotSecurityException {

        final Set<String> archivedSchemes = workflowAPI.findArchivedSchemes().stream().map(WorkflowScheme::getId).collect(Collectors.toSet());

        final Aggregations aggregations     = response.getAggregations();
        final Map<String, Long> stepCounts  = new HashMap<>();

        for (final Aggregation aggregation : aggregations.asList()) {

            if (aggregation instanceof StringTerms) {
                StringTerms.class.cast(aggregation)
                .getBuckets().forEach(
                    bucket -> stepCounts.put(bucket.getKeyAsString(), bucket.getDocCount())
                );
            }
        }

        final Map<CountWorkflowStep, List<WorkflowAction>> stepActionsMap = new HashMap<>();

        for (final Map.Entry<String, Long> stepCount : stepCounts.entrySet()) {

            try {
                final WorkflowStep workflowStep = this.workflowAPI.findStep(stepCount.getKey());
                if( archivedSchemes.contains(workflowStep.getSchemeId())){
                    Logger.info(getClass(),()-> "Step with id "+ stepCount.getKey() + " is linked with an Archived WF and will be skipped." );
                    continue;
                }

                final CountWorkflowStep step = new CountWorkflowStep(stepCount.getValue(),
                        workflowStep);
                stepActionsMap.put(step, this.workflowAPI.findBulkActions(step.getWorkflowStep(), user)
                        .stream().filter(WorkflowAction::shouldShowOnListing)
                        .collect(CollectionsUtils.toImmutableList()));
            }catch (Exception e){
                Logger.warn(getClass(),()-> "Unable to load step with id "+ stepCount.getKey() + " The index is slightly out of sync." );
            }
        }

        return (stepCounts.size() > 0 ?
            this.buildBulkActionView(stepActionsMap) :
            new BulkActionView(Collections.emptyMap())
        );
    }

    /**
     * Build a entity view out of the stepsActions map
     * @param stepActionsMap
     * @return
     * @throws DotSecurityException
     * @throws DotDataException
     */
    private BulkActionView buildBulkActionView (final Map<CountWorkflowStep, List<WorkflowAction>> stepActionsMap) throws DotSecurityException, DotDataException {

        final Map<String, WorkflowScheme> schemeByIdMap  = new HashMap<>();
        final Map<String, Long>           actionCountMap = new HashMap<>();
        Map<WorkflowScheme, Map<CountWorkflowStep, List<CountWorkflowAction>>> bulkActions = new HashMap<>();

        for (final Map.Entry<CountWorkflowStep, List<WorkflowAction>> entry : stepActionsMap.entrySet()) {

            if (!schemeByIdMap.containsKey(entry.getKey().getWorkflowStep().getSchemeId())) {
                schemeByIdMap.put(entry.getKey().getWorkflowStep().getSchemeId(),
                        this.workflowAPI.findScheme(entry.getKey().getWorkflowStep().getSchemeId()));
            }

            final Long stepCount = entry.getKey().getCount();
            for (final WorkflowAction action : entry.getValue()) {

                actionCountMap.put(action.getId(),
                        actionCountMap.getOrDefault(action.getId(), 0L) + stepCount);
            }
        }

        for (final Map.Entry<CountWorkflowStep, List<WorkflowAction>> entry : stepActionsMap.entrySet()) {

            final CountWorkflowStep step = entry.getKey();
            final WorkflowScheme scheme  = schemeByIdMap.get(step.getWorkflowStep().getSchemeId());
            final List<CountWorkflowAction> actions = entry.getValue().stream()
                    .map(action -> new CountWorkflowAction(actionCountMap.get(action.getId()), action))
                    .collect(Collectors.toList());

            final Map<CountWorkflowStep, List<CountWorkflowAction>> schemeActionsMap =
                        (bulkActions.containsKey(scheme))? bulkActions.get(scheme):new HashMap<>();

            schemeActionsMap.put(step, actions); //Need to find other occurrences of the action even if they occur on different steps

            bulkActions.put(scheme, schemeActionsMap);
        }

        bulkActions = normalizeBulkActions(bulkActions);
        return new BulkActionView(bulkActions);
    }


    /**
     * The same action might appear on different steps. This will keep the firs appearance and remove the rest of them.
     * @param bulkActions
     * @return
     */
    private Map<WorkflowScheme, Map<CountWorkflowStep, List<CountWorkflowAction>>> normalizeBulkActions(
            final Map<WorkflowScheme, Map<CountWorkflowStep, List<CountWorkflowAction>>> bulkActions) {
        final Set<WorkflowScheme> workflowSchemes = bulkActions.keySet();
        for (final WorkflowScheme scheme : workflowSchemes) {
            final Map<CountWorkflowAction, List<CountWorkflowAction>> dupeActionsMap = new HashMap<>();
            // Actions are only allowed to appear once per workflow. So we need to merge every other instance that might popup.
            final Map<CountWorkflowStep, List<CountWorkflowAction>> stepsAndActions = bulkActions
                    .get(scheme);

            final Set<CountWorkflowStep> steps = stepsAndActions.keySet();
            for (final CountWorkflowStep step : steps) {
                final List<CountWorkflowAction> countWorkflowActions = stepsAndActions.get(step);
                final Iterator<CountWorkflowAction> iterator = countWorkflowActions.iterator();
                while (iterator.hasNext()) {
                    final CountWorkflowAction action = iterator.next();
                    if (!dupeActionsMap.containsKey(action)) {
                        dupeActionsMap.put(action, countWorkflowActions);
                    } else {
                        iterator.remove();
                    }
                }
            }
        }

        return bulkActions;
    }

    /**
     * Try to find an action by name
     * @param actionName {@link String}
     * @param contentlet {@link Contentlet}
     * @param user       {@link User}
     * @return String
     * @throws DotSecurityException
     * @throws DotDataException
     */
    @CloseDBIfOpened
    public String getActionIdByName(final String actionName,
                                    final Contentlet contentlet,
                                    final User user) throws DotSecurityException, DotDataException {

        final List<WorkflowAction> availableActionsOnListing =
                APILocator.getWorkflowAPI().findAvailableActionsListing(contentlet, user);

        final List<WorkflowAction> availableActionsOnEditing =
                APILocator.getWorkflowAPI().findAvailableActionsEditing(contentlet, user);

        final Optional<WorkflowAction> foundAction =
                Stream.concat(availableActionsOnListing.stream(), availableActionsOnEditing.stream())
                        .filter(action -> action.getName().equalsIgnoreCase(actionName)).findFirst();

        return foundAction.isPresent()?foundAction.get().getId():null;
    }

    /**
     *
     * @param form
     * @param user
     * @return
     * @throws DotSecurityException
     * @throws DotDataException
     */
    @CloseDBIfOpened
    public BulkActionsResultView fireBulkActions(final FireBulkActionsForm form,
            final User user) throws DotSecurityException, DotDataException {

        final WorkflowAction action = this.workflowAPI.findAction(form.getWorkflowActionId(), user);
        if(null != action) {

            this.checkActionLicense(action);

            if(UtilMethods.isSet(form.getQuery())){
                return this.workflowAPI.fireBulkActions(action, user, form.getQuery(), form.getPopupParamsBean());
            }
            return this.workflowAPI.fireBulkActions(action, user, form.getContentletIds(), form.getPopupParamsBean());
        } else {
            throw new DoesNotExistException("Workflow-does-not-exists-action");
        }
    }


    private void checkActionLicense(final WorkflowAction action) {

        // if does not have license and the action is not system workflow action
        if (!workflowAPI.hasValidLicense() && !action.getSchemeId().equals(SystemWorkflowConstants.SYSTEM_WORKFLOW_ID)) {

            throw new InvalidLicenseException("Workflow-Schemes-License-required");
        }
    }

    /**
     * If the render mode is set, it should be a valid one
     * @param renderMode String renderMode pass by the user
     * @param user User   the user that makes the request
     * @param validRenderModeSet Set set of valid values for the render mode
     */
    public void checkRenderMode(final String renderMode, final User user, final Set<String> validRenderModeSet) {

        if (UtilMethods.isSet(renderMode) && !validRenderModeSet.contains(renderMode.toLowerCase())) {

            String message = INVALID_RENDER_MODE;

            try {
                message = LanguageUtil.get(user, "workflow.invalidrendermode");
            } catch (LanguageException e) {
                message = INVALID_RENDER_MODE;
            }

            throw new BadRequestException(message);
        }
    }

    @WrapInTransaction
    public SystemActionWorkflowActionMapping deleteSystemAction(final String identifier, final User user) throws DotDataException, DotSecurityException {

        final Optional<SystemActionWorkflowActionMapping> mapping = this.workflowAPI.findSystemActionByIdentifier(identifier, user);

        if (mapping.isPresent()) {
            if (!this.workflowAPI.deleteSystemAction(mapping.get()).isPresent()) {

                throw new InternalServerException("Could not delete the system action: " + identifier +
                        ", it may not exists");
            }
        } else {

            throw new NotFoundInDbException("Could not delete the system action: " + identifier + ", because it does not exists");
        }

        return mapping.get();
    }

    @WrapInTransaction
    public SystemActionWorkflowActionMapping deleteSystemAction(final WorkflowAPI.SystemAction systemAction, final ContentType contentType, final User user) throws DotDataException, DotSecurityException {

        final Optional<SystemActionWorkflowActionMapping> mapping = this.workflowAPI.findSystemActionByContentType(systemAction, contentType, user);

        if (mapping.isPresent()) {
            this.workflowAPI.deleteSystemAction(mapping.get()).isPresent();
            return mapping.get();
        }

        return null;
    }

    @CloseDBIfOpened
    public List<WorkflowAction> findActions(final Set<String> schemes, final WorkflowAPI.SystemAction systemAction, final User user)
            throws DotDataException, DotSecurityException {

        switch (systemAction) {

            case NEW:
                return findActionsOnNew (schemes, user);
            default:
                // todo: implement rest of them here
                break;
        }

        return Collections.emptyList();
    }

    private List<WorkflowAction> findActionsOnNew(final Set<String> schemes, final User user) throws DotDataException, DotSecurityException {

        final ImmutableList.Builder<WorkflowAction> actions = new ImmutableList.Builder<>();

        for (final String schemeId : schemes) {

            final Optional<WorkflowStep> workflowStep = this.workflowAPI.findFirstStep(schemeId);
            if (workflowStep.isPresent()) {
                actions.addAll(this.workflowAPI.findActions(workflowStep.get(), user));
            }
        }

        return actions.build();
    }

    public List<SystemActionWorkflowActionMapping> findSystemActionsByContentType(final ContentType contentType, final User user) throws DotDataException, DotSecurityException {
        return this.workflowAPI.findSystemActionsByContentType(contentType, user);
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
    public List<WorkflowAction> findAvailableActions(final String inode, final User user,
                                                     final WorkflowAPI.RenderMode renderMode) {
        try {
            Logger.debug(this, ()->"Asking for the available actions for the inode: " + inode);

            if (!UtilMethods.isSet(inode)) {
                throw new IllegalArgumentException("Missing required parameter inode.");
            }

            final Contentlet contentlet = this.contentletAPI.find(inode, user, true);
            if(contentlet == null){
               throw new DoesNotExistException(String.format("Contentlet identified by inode '%s' was Not found.",inode));
            }
            return this.workflowAPI.findAvailableActions(contentlet, user, renderMode);
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
     * Evaluates the action condition using the Velocity Template Engine
     * @param actionId
     * @param user
     * @return
     * @throws DotDataException
     * @throws DotSecurityException
     */
    public String evaluateActionCondition(final String actionId, final User user, final HttpServletRequest request, final HttpServletResponse response) throws DotDataException, DotSecurityException{

        if (!UtilMethods.isSet(actionId)) {
            final String exceptionMessage = getFormattedMessage(user.getLocale(),"Workflow-required-param-actionId",actionId);
            throw new IllegalArgumentException(exceptionMessage);
        }

        final WorkflowAction action = this.workflowAPI.findAction(actionId, user);
        if(action == null){
            final String exceptionMessage = getFormattedMessage(user.getLocale(),"Workflow-does-not-exists-action-by-actionId",actionId);
            throw new DoesNotExistException(exceptionMessage);
        }

        try {
            final Context ctx = VelocityWebUtil.getVelocityContext(request, response);
            final StringWriter out = new StringWriter();
            VelocityUtil.getEngine().evaluate(ctx, out, "WorkflowVelocity:" + action.getName(), action.getCondition());
            return out.toString();
        }catch (Exception e){
            throw new DotDataException(e);
        }

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
    @CloseDBIfOpened
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
    @WrapInTransaction
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
    @WrapInTransaction
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
    public WorkflowStep deleteAction(final String actionId,
                                     final String stepId,
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

    @WrapInTransaction
    public void saveSchemesByContentType(final String contentTypeId, final User user, final Set<String> workflowIds) {

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
    @CloseDBIfOpened
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
    @CloseDBIfOpened
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
    @CloseDBIfOpened
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
     * Save the mapping between the System Action and the Workflow Action for a Content Type or Scheme
     * @param workflowSystemActionForm {@link WorkflowSystemActionForm}
     * @param user {@link User}
     * @return SystemActionWorkflowActionMapping
     */
    @WrapInTransaction
    public SystemActionWorkflowActionMapping mapSystemActionToWorkflowAction(
            final WorkflowSystemActionForm workflowSystemActionForm, final User user) throws DotSecurityException, DotDataException {

        SystemActionWorkflowActionMapping mapping = null;
        final WorkflowAction workflowAction = this.workflowAPI.findAction(workflowSystemActionForm.getActionId(), user);

        if (null != workflowAction) {
            if (UtilMethods.isSet(workflowSystemActionForm.getSchemeId())) {

                final WorkflowScheme workflowScheme = this.workflowAPI.findScheme(workflowSystemActionForm.getSchemeId());
                mapping = this.workflowAPI.mapSystemActionToWorkflowActionForWorkflowScheme(workflowSystemActionForm.getSystemAction(),
                        workflowAction, workflowScheme);
            } else if (UtilMethods.isSet(workflowSystemActionForm.getContentTypeVariable())) {

                final ContentType contentType = this.findContentType(workflowSystemActionForm.getContentTypeVariable(), user);
                mapping = this.workflowAPI.mapSystemActionToWorkflowActionForContentType(workflowSystemActionForm.getSystemAction(),
                        workflowAction, contentType);
            } else {

                throw new BadRequestException("SchemeId or Content Type Variable are required");
            }
        } else {

            throw new DoesNotExistException("The workflow action with the id: " + workflowSystemActionForm.getActionId() + " does not exists");
        }


        return mapping;
    }

    private ContentType findContentType (final String variable, final User user)
            throws DotDataException, DotSecurityException {

        try {
            return APILocator.getContentTypeAPI(user)
                    .find(variable);
        } catch (NotFoundInDbException e) {
            throw new BadRequestException("The content type: " + variable + " does not exists");
        }
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
        newAction.setShowOn(workflowActionForm.getShowOn());
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
    public void saveActionletToAction(final WorkflowActionletActionBean workflowActionletActionBean, final User user)
            throws DotDataException, AlreadyExistException {

        final String actionId       = workflowActionletActionBean.getActionId();
        final String actionletClass = workflowActionletActionBean.getActionletClass();

        if (!UtilMethods.isSet(actionId) || !UtilMethods.isSet(actionletClass)) {
            throw new IllegalArgumentException("Missing required parameter actionId or actionletClass.");
        }

        final int    order          = workflowActionletActionBean.getOrder();
        final WorkflowActionClass workflowActionClass = new WorkflowActionClass();

        if (order > 0) {

            workflowActionClass.setOrder(order);
        } else {

            final WorkflowAction action = new WorkflowAction();
            action.setId(actionId);
            final List<WorkflowActionClass> classes = this.workflowAPI.findActionClasses(action);
            if (classes != null) {
                workflowActionClass.setOrder(classes.size());
            }
        }

        final WorkFlowActionlet actionlet = this.workflowAPI.findActionlet(actionletClass);

        if (null == actionlet) {

            throw new DoesNotExistException("The actionlet: " + actionletClass + ", does not exists");
        }

        final Map<String, String> userActionClassParametersMap =
                workflowActionletActionBean.getParameters();

        workflowActionClass.setClazz(actionletClass);
        workflowActionClass.setName(actionlet.getName());
        workflowActionClass.setActionId(actionId);
        this.workflowAPI.saveActionClass(workflowActionClass, user);

        // if parameters, so save them
        final List<WorkflowActionClassParameter> parameters = null;
        this.workflowAPI.saveWorkflowActionClassParameters(parameters, user);
        if (UtilMethods.isSet(userActionClassParametersMap)) {
            this.saveWorkflowActionClassParameters(workflowActionClass, actionlet,
                    userActionClassParametersMap, user);
        }
    }

    private void saveWorkflowActionClassParameters (final WorkflowActionClass workflowActionClass,
                                                   final WorkFlowActionlet actionlet,
                                                   final Map<String, String> userActionClassParametersMap,
                                                   final User user) throws DotDataException {

        final List<WorkflowActionletParameter> actionletParameters	  = actionlet.getParameters();
        final Map<String, WorkflowActionClassParameter> enteredParams = this.workflowAPI.findParamsForActionClass(workflowActionClass);
        final List<WorkflowActionClassParameter>        newParams 	  = new ArrayList<>();
        String userIds = null;

        for (final WorkflowActionletParameter expectedParam : actionletParameters) {

            final WorkflowActionClassParameter enteredParam =
                    enteredParams.computeIfAbsent(expectedParam.getKey(), key -> new WorkflowActionClassParameter());
            enteredParam.setActionClassId(workflowActionClass.getId());
            enteredParam.setKey(expectedParam.getKey());
            enteredParam.setValue(userActionClassParametersMap.getOrDefault(expectedParam.getKey(), StringPool.BLANK));
            newParams.add(enteredParam);
            userIds = enteredParam.getValue();
            //Validate userIds or emails
            final String errors = expectedParam.hasError(userIds);
            if(errors != null) {

                throw new IllegalArgumentException("The userIds, emails or roles are invalid: " + userIds);
            }
        }

        this.workflowAPI.saveWorkflowActionClassParameters(newParams, user);
    }

    @WrapInTransaction
    public void saveActionToStep(final WorkflowActionStepBean workflowActionStepBean, final User user) throws DotDataException, DotSecurityException {

        final String actionId = workflowActionStepBean.getActionId();
        final String stepId = workflowActionStepBean.getStepId();

        if (!UtilMethods.isSet(actionId) || !UtilMethods.isSet(stepId)) {
            throw new IllegalArgumentException("Missing required parameter actionId or stepId.");
        }

        final WorkflowAction action = this.workflowAPI.findAction(actionId, stepId, user);
        if(action != null) {
            WorkflowReorderBean bean = new WorkflowReorderBean.Builder()
            .actionId(actionId).stepId(stepId)
            .order(workflowActionStepBean.getOrder()).build();

            this.reorderAction(bean, user);
            
        } else {

            this.workflowAPI.saveAction(actionId,
                stepId, user, workflowActionStepBean.getOrder());
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
    @CloseDBIfOpened
    public List<WorkflowDefaultActionView> findAvailableDefaultActionsByContentType(final String contentTypeId, final User user) {
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
    @CloseDBIfOpened
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

            final List<WorkflowAction> editingActions =
                    UtilMethods.isSet(actions)?actions.stream().filter(WorkflowAction::shouldShowOnEdit)
                            .collect(Collectors.toList()) : Collections.emptyList();

            for (final WorkflowAction action : editingActions) {
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
    @WrapInTransaction
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
    @WrapInTransaction
    public Future<WorkflowScheme> delete(final String schemeId, final User user)
            throws AlreadyExistException, DotDataException, DotSecurityException {

        final WorkflowScheme scheme = this.workflowAPI.findScheme(schemeId);

        if(null != scheme) {
            return this.workflowAPI.deleteScheme(scheme, user);
        } else {
            throw new DoesNotExistException("Workflow-does-not-exists-scheme");
        }
    } // delete.

    /**
     * Figure out the contentlet by identifier (when not language) depending on the following rules:
     * If there is a contentlet associated to the current session language tries the id+session lang combination
     * If there is not a contentlet associated and the default language is diff to the session lang will tries this combination.
     * Otherwise will try to get the content on some language
     * @param identifier {@link String} shorty or long identifier
     * @param mode {@link PageMode} page mode
     * @param user {@link User} user
     * @param sessionLanguageSupplier {@link Supplier} supplier to get the session language in case needed
     * @return Optional contentlet
     * @throws DotSecurityException
     * @throws DotDataException
     */
    @CloseDBIfOpened
    public Optional<Contentlet> getContentletByIdentifier(final String identifier,
                                                           final PageMode mode,
                                                           final User     user,
                                                           final Supplier<Long> sessionLanguageSupplier) throws DotSecurityException, DotDataException {

        Contentlet contentlet = null;
        final Optional<ShortyId> shortyIdOptional = APILocator.getShortyAPI().getShorty(identifier);
        final String longIdentifier = shortyIdOptional.isPresent()? shortyIdOptional.get().longId:identifier;
        final long sessionLanguage  = sessionLanguageSupplier.get();

        if(sessionLanguage > 0) {

            contentlet = this.getContentletByIdentifier
                    (longIdentifier, mode.showLive, sessionLanguage, user, mode.respectAnonPerms);
        }

        if (null == contentlet) {

            final long defaultLanguage = APILocator.getLanguageAPI().getDefaultLanguage().getId();
            if (defaultLanguage != sessionLanguage) {


                contentlet = this.getContentletByIdentifier
                        (longIdentifier, mode.showLive, defaultLanguage, user, mode.respectAnonPerms);
            }
        }

        return null == contentlet?
                Optional.ofNullable(this.contentletAPI.findContentletByIdentifierAnyLanguage(longIdentifier)):
                Optional.ofNullable(contentlet);
    }

    public Contentlet getContentletByIdentifier(final String longIdentifier, final boolean showLive,
                                                final long languageId, final User user, final boolean respectAnonPerms) {

        try {
            return this.contentletAPI.findContentletByIdentifier
                    (longIdentifier, showLive, languageId, user, respectAnonPerms);
        } catch (DotContentletStateException | DotSecurityException | DotDataException e) {

            Logger.error(this, e.getMessage(), e);
            return null;
        }
    }

    /**
     * Used to call and transform a Contentlet
     * @param contentlet A Contentlet
     * @return Transformed Map
     */
    public Map<String, Object> contentletToMap(final Contentlet contentlet) {

        final ContentletToMapTransformer transformer = new ContentletToMapTransformer(contentlet);
        return transformer.toMaps().stream().findFirst().orElse(Collections.EMPTY_MAP);
    }
} // E:O:F:WorkflowHelper.
