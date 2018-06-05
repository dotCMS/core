package com.dotmarketing.portlets.workflows.business;

import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.rest.api.v1.workflow.BulkActionsResultView;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.Permissionable;
import com.dotmarketing.business.Role;
import com.dotmarketing.exception.AlreadyExistException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.DotContentletValidationException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ContentletDependencies;
import com.dotmarketing.portlets.fileassets.business.IFileAsset;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.portlets.workflows.actionlet.WorkFlowActionlet;
import com.dotmarketing.portlets.workflows.model.WorkflowAction;
import com.dotmarketing.portlets.workflows.model.WorkflowActionClass;
import com.dotmarketing.portlets.workflows.model.WorkflowActionClassParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowComment;
import com.dotmarketing.portlets.workflows.model.WorkflowHistory;
import com.dotmarketing.portlets.workflows.model.WorkflowProcessor;
import com.dotmarketing.portlets.workflows.model.WorkflowScheme;
import com.dotmarketing.portlets.workflows.model.WorkflowSearcher;
import com.dotmarketing.portlets.workflows.model.WorkflowState;
import com.dotmarketing.portlets.workflows.model.WorkflowStep;
import com.dotmarketing.portlets.workflows.model.WorkflowTask;
import com.liferay.portal.model.User;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.function.Consumer;

public interface WorkflowAPI {

	public static final String SYSTEM_WORKFLOW_ID           = WorkFlowFactory.SYSTEM_WORKFLOW_ID;
	public static final Set<WorkflowState> DEFAULT_SHOW_ON = EnumSet.of(WorkflowState.LOCKED, WorkflowState.UNLOCKED);

	public boolean hasValidLicense();

    public void registerBundleService ();

	public WorkFlowActionlet newActionlet(String className) throws DotDataException;

	/**
	 * If the user is not allowed to modified workflow, will throw {@link WorkflowPortletAccessException}
	 * @param user
	 */
	void isUserAllowToModifiedWorkflow (final User user);

	public java.util.List<WorkflowTask> searchTasks(WorkflowSearcher searcher) throws DotDataException;

	public WorkflowTask findTaskByContentlet(Contentlet contentlet) throws DotDataException;

	/**
	 * This method will get a list with the current contentlet workflow step.
	 * If the contentlet doesn't have a workflow step associated, then it will
	 * display all the first workflow steps associated to the contentlet Content Type.
	 *
	 * @param contentlet The current contentlet
	 * @return A list of step available for the contentlet
	 * @throws DotDataException
	 */
	public List<WorkflowStep> findStepsByContentlet(Contentlet contentlet) throws DotDataException;

	/**
	 * Return the contentlet current step if there is one or null
	 * if the contentlet is not associated to one workflow step.
	 * @param contentlet The contentlet to check
	 * @return The WorkflowStep where the contentlet is or null if not
	 * @throws DotDataException
	 */
	public WorkflowStep findStepByContentlet(Contentlet contentlet) throws DotDataException;

	/**
	 * Check if the schemeId pass exist n the list of workflow scheme.
	 * @param schemeId WorkflowScheme ID to validate
	 * @param schemes List of WorkflowScheme to compare
	 * @return true if the scheme Id exist false if not
	 */
	public boolean existSchemeIdOnSchemesList(String schemeId, List<WorkflowScheme> schemes);

	/**
	 * Finds a workflow by id
	 *
	 * @param id
	 * @return
	 * @throws DotDataException
	 */
	public WorkflowTask findTaskById(String id) throws DotDataException;

	/**
	 * Finds comments on a workflow item
	 *
	 * @param task
	 * @return
	 * @throws DotDataException
	 */
	public List<WorkflowComment> findWorkFlowComments(WorkflowTask task) throws DotDataException;

	/**
	 * Saves comments on a workflow item
	 *
	 * @param comment
	 * @throws DotDataException
	 */
	public void saveComment(WorkflowComment comment) throws DotDataException;

	/**
	 * deletes a specific comment on a workflow item
	 *
	 * @param comment
	 * @param user
	 * @throws DotDataException
	 */
	public void deleteComment(WorkflowComment comment) throws DotDataException;

	/**
	 * gets history of a particular workflow item
	 *
	 * @param task
	 * @return
	 * @throws DotDataException
	 */
	public List<WorkflowHistory> findWorkflowHistory(WorkflowTask task) throws DotDataException;

	/**
	 * Saves a new history item for a workflow
	 *
	 * @param history
	 * @param user
	 * @throws DotDataException
	 */
	public void saveWorkflowHistory(WorkflowHistory history) throws DotDataException;

	/**
	 * deletes a history item from a workflow
	 *
	 * @param history
	 * @param user
	 * @throws DotDataException
	 */
	public void deleteWorkflowHistory(WorkflowHistory history) throws DotDataException;

	/**
	/**
	 * finds files associated with a workflow item
	 *
	 * @param task
	 * @return
	 * @throws DotDataException
	 */
	public List<IFileAsset> findWorkflowTaskFilesAsContent(WorkflowTask task, User user) throws DotDataException;

	/**
	 *
	 * @param task
	 * @param fileInode
	 * @throws DotDataException
	 */

	public void attachFileToTask(WorkflowTask task, String fileInode) throws DotDataException;

	/**
	 *
	 * @param task
	 * @param fileInode
	 * @throws DotDataException
	 */

	public void removeAttachedFile(WorkflowTask task, String fileInode) throws DotDataException;

	/**
	 *
	 * @param task
	 * @param user
	 * @throws DotDataException
	 */
	public void deleteWorkflowTask(WorkflowTask task, User user) throws DotDataException;

	/**
	 *
	 * @param task
	 * @throws DotDataException
	 */
	public void  saveWorkflowTask(WorkflowTask task, WorkflowProcessor processor) throws DotDataException;
	public void  saveWorkflowTask(WorkflowTask task) throws DotDataException;

	public List<WorkflowScheme> findSchemes(boolean showArchived) throws DotDataException;

	public WorkflowScheme findScheme(String id) throws DotDataException, DotSecurityException;

	public List<WorkflowScheme> findSchemesForStruct(Structure struct) throws DotDataException;

	/**
	 * Returns all the schemes associated to the content type
	 * @param contentType ContentType
	 * @return List
	 * @throws DotDataException
	 */
	public List<WorkflowScheme> findSchemesForContentType(ContentType contentType) throws DotDataException;

	/**
	 * find all content types associated to a workflow
	 * @param workflowScheme
	 * @return
	 * @throws DotDataException
	 */
	public List<ContentType> findContentTypesForScheme(WorkflowScheme workflowScheme);

	/**
	 * Save an scheme
	 * @param scheme {@link WorkflowScheme}
	 * @param user   {@link User}
	 * @throws DotDataException
	 * @throws AlreadyExistException
	 */
	public void saveScheme(WorkflowScheme scheme, User user) throws DotDataException, DotSecurityException ,AlreadyExistException;

	/**
	 * Delete the workflow scheme with all the steps, action, actionlets and
	 * associations with content types
	 * @param scheme the workflow scheme to delete
	 * @param user The current user
	 * @throws DotDataException
	 * @throws DotSecurityException
	 * @throws AlreadyExistException
	 */
	public Future<WorkflowScheme> deleteScheme(WorkflowScheme scheme, User user) throws DotDataException, DotSecurityException, AlreadyExistException;

	/**
	 * Find the steps associated to the scheme
	 * @param scheme {@link WorkflowScheme}
	 * @return List of {@link WorkflowStep}
	 * @throws DotDataException
	 */
	public List<WorkflowStep> findSteps(WorkflowScheme scheme) throws DotDataException;

	public void saveStep(WorkflowStep step, User user) throws DotDataException, AlreadyExistException;

    /**
     * Delete a step with all dependencies: actions, actionlets and tasks.
     * @param step WorkflowStep   from the step to delete the action
     * @param user The current User
     * @return Future {@link WorkflowStep} the process runs async, returns a future with the steps deleted.
     * @throws DotDataException
     */
	public Future<WorkflowStep> deleteStep(WorkflowStep step, User user) throws DotDataException;

	/**
	 * This method makes the reorder for the step, reordering the rest of the steps too.
	 * @param step   WorkflowStep   step to reorder
	 * @param order  int			Order for the action
	 * @throws DotDataException
	 * @throws AlreadyExistException
	 */
	public void reorderStep(WorkflowStep step, int order, User user) throws DotDataException, AlreadyExistException, DotSecurityException;

	/**
	 * This is a legacy method for reorder
	 * 
	 * @deprecated On release 4.3, replaced by {@link #reorderAction(WorkflowAction, WorkflowStep, User, int)}
	 * @param action WorkflowAction action you want to reorder, the getStepid has to be not empty and has to have the associated step to the action
	 * @param order  int			Order for the action
	 * @throws DotDataException
	 * @throws AlreadyExistException
	 */
	@Deprecated
	public void reorderAction(WorkflowAction action, int order) throws DotDataException, AlreadyExistException, DotSecurityException;

	/**
	 * This method makes the reorder for the action associated to the step, reordering the rest of the actions too.
	 * @param action WorkflowAction action you want to reorder
	 * @param step   WorkflowStep   step which the action are associated
	 * @param user   User           user that is executing the aciton
	 * @param order  int			Order for the action
	 * @throws DotDataException
	 * @throws AlreadyExistException
	 */
	public void reorderAction(final WorkflowAction action,
							  final WorkflowStep step,
							  final User user,
							  final int order) throws DotDataException, AlreadyExistException;



	/**
	 * Finds an action by Id and checking the user permissions over the workflow portlet.
	 * The action will be validated against the user permissions.
	 * @param id String action id
	 * @param user     User   the user that makes the request
	 * @return WorkflowAction
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	public WorkflowAction findAction(String id, User user) throws DotDataException, DotSecurityException;

	/**
	 * Finds an action by Id and checking the user permissions over the permissionable.
	 * The action will be validated against the user permissions.
	 * @param id String action id
	 * @param permissionable   Permissionable Content/Content Type against who is going to be validated the permissions
	 * @param user     User   the user that makes the request
	 * @return WorkflowAction
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	public WorkflowAction findActionRespectingPermissions(String id, Permissionable permissionable, User user) throws DotDataException, DotSecurityException;

	/**
	 * Finds an action associated to the steps and user permissions over the workflow portlet.
	 * The action will be validated against the user permissions.
	 * @param actionId String action id
	 * @param stepId   String step  id
	 * @param user     User   the user that makes the request
	 * @return WorkflowAction
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	public WorkflowAction findAction(String actionId, String stepId, User user) throws DotDataException, DotSecurityException;

	/**
	 * Finds an action associated to the steps and the user permissions over the permissionable.
	 * The action will be validated against the user permissions.
	 * @param actionId String action id
	 * @param stepId   String step  id
	 * @param permissionable Permissionable Content/Content Type against who is going to be validated the permissions
	 * @param user     User   the user that makes the request
	 * @return WorkflowAction
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	public WorkflowAction findActionRespectingPermissions(String actionId, String stepId, Permissionable permissionable, User user) throws DotDataException, DotSecurityException;

	/**
	 * Finds the available {@link WorkflowAction} for the contentlet to a user on any give
	 * piece of content, based on how and who has the content locked and what workflow step the content
	 * is in
	 * @param contentlet Contentlet
	 * @param user       User
	 * @return List of   WorkflowAction
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	public List<WorkflowAction> findAvailableActions(Contentlet contentlet, User user) throws DotDataException,
	DotSecurityException ;

	/**
	 * Find the list of Workflow Actions available for the current user on the specified workflow step
	 * @param step The current step
	 * @param user The current User
	 * @return List of workflow actions that the user have access
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	public List<WorkflowAction> findActions(WorkflowStep step, User user) throws DotDataException,
			DotSecurityException;

	/**
	 * Find the list of Workflow Actions available for the current user on the specified workflow step and permissionable
	 * @param step The current step
	 * @param user The current User
	 * @param permissionable The Contentlet or Content Type to validate special workflow roles
	 * @return List of workflow actions that the user have access
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	public List<WorkflowAction> findActions(WorkflowStep step, User user, Permissionable permissionable) throws DotDataException,
			DotSecurityException;


	/**
	 * Find the list of Workflow Actions available for the role that is passed
	 * @param step
	 * @param role
     * @param permissionable
	 * @return
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	public List<WorkflowAction> findActions(WorkflowStep step, Role role, Permissionable permissionable) throws DotDataException,
			DotSecurityException;

	/**
	 * Find the {@link WorkflowAction} associated to the {@link WorkflowScheme}
	 * @param scheme {@link WorkflowScheme}
	 * @param user   {@link User}
	 * @return List of WorkflowAction
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	public List<WorkflowAction> findActions(WorkflowScheme scheme, User user) throws DotDataException,
			DotSecurityException;

	/**
	 * Find the list of Workflow Actions available for the current user ont the list of steps
	 * @param steps List of workflow steps
	 * @param user The current User
	 * @return List of workflow actions that the user have access
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	public List<WorkflowAction> findActions(List<WorkflowStep> steps, User user) throws DotDataException,
			DotSecurityException;

	/**
	 * Find the list of Workflow Actions available for the current user ont the list of steps and permissionable
	 * @param steps List of workflow steps
	 * @param user The current User
	 * @param permissionable The Contentlet or Content Type to validate special workflow roles
	 * @return List of workflow actions that the user have access
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	public List<WorkflowAction> findActions(List<WorkflowStep> steps, User user, Permissionable permissionable) throws DotDataException,
			DotSecurityException;

	/**
	 * This method associate a list of Workflow Schemes to a Structure
	 *
	 * @param struc The Structure
	 * @param schemes List of Workflow Schemes to be associated to the Structure
	 * @throws DotDataException
	 */
	public void saveSchemesForStruct(Structure struc, List<WorkflowScheme> schemes) throws DotDataException;

	/**
	 * This method associate a list of Workflow Schemes to a Content Type
	 * @param contentType {@link ContentType}
	 * @param schemesIds {@link List} list of scheme ids
	 * @throws DotDataException
	 */
	public void saveSchemeIdsForContentType(final ContentType contentType,
											final List<String> schemesIds) throws DotDataException;

	/**
	 * Saves an single action the action is associated to the schema by default
	 * @param action WorkflowAction
	 * @param perms List of Permission
	 * @throws DotDataException
	 * @throws AlreadyExistException
	 */
	public void saveAction(WorkflowAction action, List<Permission> perms,
						   final User user) throws DotDataException, AlreadyExistException;

	/**
	 * Save (associated) the action into the step
	 * If any of them does not exists (action or step) throws DoesNotExistException
	 * @param actionId String
	 * @param stepId   String
	 * @param user     User
	 */
	void saveAction(String actionId, String stepId, User user);

	/**
	 * Save (associated) the action into the step
	 * If any of them does not exists (action or step) throws DoesNotExistException
	 * Will associated the action in the order desired
	 * @param actionId String
	 * @param stepId   String
	 * @param user     User
	 * @param order    int
	 */
	void saveAction(String actionId, String stepId, User user, int order);

	public WorkflowStep findStep(String id) throws DotDataException;

	/**
	 * Deletes the action associated to the scheme
	 * @param action WorkflowAction
	 * @param user   User
	 * @throws DotDataException
	 * @throws AlreadyExistException
	 */
	public void deleteAction(WorkflowAction action, User user) throws DotDataException, AlreadyExistException;

	/**
	 * Deletes the action reference to the step, but the action associated to the scheme will continue alive.
	 * @param action WorkflowAction action to delete
	 * @param step   WorkflowStep   from the step to delete the action
	 * @param user   User
	 */
	void deleteAction(WorkflowAction action, WorkflowStep step, User user) throws DotDataException, AlreadyExistException;

	public List<WorkflowActionClass> findActionClasses(WorkflowAction action) throws DotDataException;

	public WorkflowActionClass findActionClass(String id) throws DotDataException;

	public void deleteActionClass(WorkflowActionClass actionClass, User user) throws DotDataException, AlreadyExistException;

	public void saveActionClass(WorkflowActionClass actionClass, User user) throws DotDataException, AlreadyExistException;

	public void reorderActionClass(WorkflowActionClass actionClass, int order, User user) throws DotDataException, AlreadyExistException;

	public Map<String, WorkflowActionClassParameter> findParamsForActionClass(WorkflowActionClass actionClass) throws DotDataException;

	public List<WorkFlowActionlet> findActionlets() throws DotDataException;

	public WorkFlowActionlet findActionlet(String clazz) throws DotDataException;

	public void saveWorkflowActionClassParameters(List<WorkflowActionClassParameter> params, User user) throws DotDataException;


	/***
	 *
	 * This method will take a WorkflowActionId (set in the contentlet map) and
	 * fire that action using the mod_user on the contentlet
	 * @param contentlet
	 * @throws DotDataException
	 */
	public WorkflowProcessor fireWorkflowPreCheckin(Contentlet contentlet, User user) throws DotDataException,DotWorkflowException, DotContentletValidationException;
	public void fireWorkflowPostCheckin(WorkflowProcessor wflow) throws DotDataException,DotWorkflowException;

	/**
	 * Fires a workflow for a contentlet with a contentlet dependencies, returning the final contentlet processed.
	 * @param contentlet {@link Contentlet}
	 * @param dependencies {@link ContentletDependencies}
	 * @return Contentlet
	 */
	Contentlet fireContentWorkflow(Contentlet contentlet, ContentletDependencies dependencies) throws DotDataException;

	/**
	 * Validates if the Workflow Action the Contentlet is going to execute belongs to the step of the
	 * Contentlet and also if the action belongs to one of the schemes associated to the
	 * Content Type of the Contentlet.
	 * This method does not validate the permissions execution of the Workflow Action.
	 */
	public void validateActionStepAndWorkflow(final Contentlet contentlet, final User user)
			throws DotDataException;

	public WorkflowProcessor fireWorkflowNoCheckin(Contentlet contentlet, User user) throws DotDataException,DotWorkflowException, DotContentletValidationException;


	public int countTasks(WorkflowSearcher searcher)  throws DotDataException;

	public List<WorkflowActionClassParameter> copyWorkflowActionClassParameters(final Collection<WorkflowActionClassParameter> from, WorkflowActionClass to, final User user) throws DotDataException;
	public WorkflowActionClass copyWorkflowActionClass(WorkflowActionClass from, WorkflowAction to, final User user) throws DotDataException, AlreadyExistException;
	public WorkflowAction copyWorkflowAction(WorkflowAction from, WorkflowScheme to, final User user) throws DotDataException, AlreadyExistException, DotSecurityException;
	public WorkflowStep copyWorkflowStep(WorkflowStep from, WorkflowScheme to, final User user) throws DotDataException, AlreadyExistException;

	/**
	 * Do a deep copy of the scheme, copying the steps, actions, etc.
	 * @param from WorkflowScheme scheme from you want to do the copy
	 * @param user User user that is creating the copy
	 * @param optionalName Optional String  optional name for the scheme.
	 * @throws DotDataException
	 */
	public WorkflowScheme deepCopyWorkflowScheme(WorkflowScheme from, final User user, final Optional<String> optionalName) throws DotDataException, AlreadyExistException, DotSecurityException;

    public java.util.List<WorkflowTask> searchAllTasks(WorkflowSearcher searcher) throws DotDataException;

	public WorkflowHistory retrieveLastStepAction(String taskId) throws DotDataException, DotSecurityException;
    
	/**
	 * This method will return the entry action of a scheme based on the content's structure.
	 *
	 * @param Contentlet
	 * @param User
	 * @return WorkflowAction
	 * @throws DotDataException, DotSecurityException
	 */

    public WorkflowAction findEntryAction(Contentlet contentlet, User user)  throws DotDataException, DotSecurityException;
    
    /**
     * finds tasks that have been in a step (with escalation enabled) for more time that the 
     * configured
     * 
     * @return
     * @throws DotDataException
     * @throws DotSecurityException
     */
    public List<WorkflowTask> findExpiredTasks() throws DotDataException, DotSecurityException;

    /**
     * finds the schema with the specified name.
     * 
     * @param schemaName 
     * @return the schema with the specified name. null if it doesn't exists
     */
    public WorkflowScheme findSchemeByName(String schemaName) throws DotDataException;

    public void deleteWorkflowActionClassParameter(WorkflowActionClassParameter param) throws DotDataException, AlreadyExistException;

    /**
	 * Method will replace user references of the given userId in workflow, workflow_ action task and workflow comments
	 * with the replacement user id 
	 * @param userId User Identifier
	 * @param userRoleId The role id of the user
	 * @param replacementUserId The user id of the replacement user
	 * @param replacementUserRoleId The role Id of the replacemente user
	 * @throws DotDataException There is a data inconsistency
	 * @throws DotStateException There is a data inconsistency
	 * @throws DotSecurityException 
	 */
	public void updateUserReferences(String userId, String userRoleId, String replacementUserId, String replacementUserRoleId)throws DotDataException, DotSecurityException;

	/**
	 * Method will replace step references of the given stepId in workflow, workflow_action task and contentlets
	 * with the replacement step id 
	 * @param stepId Step Identifier
	 * @param replacementStepId The step id of the replacement step
	 * @throws DotDataException There is a data inconsistency
	 * @throws DotStateException There is a data inconsistency
	 * @throws DotSecurityException 
	 */
	public void updateStepReferences(String stepId, String replacementStepId) throws DotDataException, DotSecurityException;

	/**
	 * Return the list of available default workflow actions associated to a Content type. All the
	 * Workflow Actions are part of the first step of the Workflow Schemes associted to the Content
	 * Type
	 *
	 * @param contentType ContentType to be processed
	 * @param user The current User
	 * @return List<WorkflowAction>
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	public List<WorkflowAction> findAvailableDefaultActionsByContentType(ContentType contentType, User user) throws DotDataException, DotSecurityException;

	/**
	 * Return the list of available default workflow actions associated to a List of Workflow schemes.
	 * All the Workflow Actions are part of the first step of the given Workflow Schemes.
	 *
	 * @param schemes List of workflowScheme to be processes
	 * @param user The current User
	 * @return List<WorkflowAction>
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	public List<WorkflowAction> findAvailableDefaultActionsBySchemes(List<WorkflowScheme> schemes, User user) throws DotDataException, DotSecurityException;

	/**
	 * Return the list of available workflow actions associated to a Content type. All the
	 * Workflow Actions are part of the first step of the Workflow Schemes associated to the Content
	 * Type
	 *
	 * @param contentType ContentType to be processed
	 * @param user The current User
	 * @return List<WorkflowAction>
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	public List<WorkflowAction> findInitialAvailableActionsByContentType(ContentType contentType, User user) throws DotDataException, DotSecurityException;

	/**
	 * return all the workflow task on a particular step
	 * @param stepId Id of the step
	 * @return List of workflow tasks
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	public List<WorkflowTask> findTasksByStep(String stepId) throws DotDataException, DotSecurityException;

	/**
	 * Return the system workflow scheme
	 * @return The system workflow scheme
	 * @throws DotDataException
	 */
	public WorkflowScheme findSystemWorkflowScheme() throws DotDataException;

	/**
	 * Archive the specified workflow scheme
	 *
	 * @param scheme Workflow scheme to archive
	 */
	public void archive(WorkflowScheme scheme, User user)
			throws DotDataException, DotSecurityException, AlreadyExistException;

    /**
     * Fires a list of contentlets returned by the luceneQuery and using an action.
     * @param action {@link WorkflowAction}
     * @param user  {@link User}
     * @param luceneQuery luceneQuery
     * @return
     */
    Future<BulkActionsResultView> fireBulkActions(WorkflowAction action, User user, String luceneQuery) throws DotDataException;

	/**
	 * Fires a list of contentlets by using an action.
	 * It returns a list of a success, failed and skipped contentlets
	 * @param action {@link WorkflowAction}
	 * @param user   {@link User}
	 * @param contentletIds {@link List}
	 * @return Future BulkActionsResultView
	 */
	Future<BulkActionsResultView> fireBulkActions(WorkflowAction action, User user, List<String> contentletIds) throws DotDataException;
}
