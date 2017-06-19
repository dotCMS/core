package com.dotmarketing.portlets.workflows.business;

import java.util.List;
import java.util.Map;

import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.exception.AlreadyExistException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.DotContentletValidationException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
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
import com.dotmarketing.portlets.workflows.model.WorkflowStep;
import com.dotmarketing.portlets.workflows.model.WorkflowTask;
import com.liferay.portal.model.User;

public interface WorkflowAPI {

    public void registerBundleService ();

	public WorkFlowActionlet newActionlet(String className) throws DotDataException;


	public java.util.List<WorkflowTask> searchTasks(WorkflowSearcher searcher) throws DotDataException;

	public WorkflowTask findTaskByContentlet(Contentlet contentlet) throws DotDataException;

	public WorkflowStep findStepByContentlet(Contentlet contentlet) throws DotDataException;

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
	 * @throws DotDataException
	 */
	public void saveWorkflowHistory(WorkflowHistory history) throws DotDataException;

	/**
	 * deletes a history item from a workflow
	 *
	 * @param history
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
	 * @throws DotDataException
	 */
	public void deleteWorkflowTask(WorkflowTask task) throws DotDataException;

	/**
	 *
	 * @param task
	 * @throws DotDataException
	 */
	public void  saveWorkflowTask(WorkflowTask task, WorkflowProcessor processor) throws DotDataException;
	public void  saveWorkflowTask(WorkflowTask task) throws DotDataException;

	public List<WorkflowScheme> findSchemes(boolean showArchived) throws DotDataException;

	public WorkflowScheme findDefaultScheme() throws DotDataException;

	public boolean isDefaultScheme(WorkflowScheme scheme) throws DotDataException;

	public WorkflowScheme findScheme(String id) throws DotDataException;

	public WorkflowScheme findSchemeForStruct(Structure struct) throws DotDataException;

	public void saveScheme(WorkflowScheme scheme) throws DotDataException, AlreadyExistException;

	public void deleteScheme(WorkflowScheme scheme) throws DotDataException;

	public void activateScheme(WorkflowScheme scheme) throws DotDataException;

	public void deactivateScheme(WorkflowScheme scheme) throws DotDataException;

	public List<WorkflowStep> findSteps(WorkflowScheme scheme) throws DotDataException;

	public void saveStep(WorkflowStep step) throws DotDataException, AlreadyExistException;

	public void deleteStep(WorkflowStep step) throws DotDataException;

	public void reorderStep(WorkflowStep step, int order) throws DotDataException, AlreadyExistException;

	public void reorderAction(WorkflowAction action, int order) throws DotDataException, AlreadyExistException;

	public WorkflowAction findAction(String id, User user) throws DotDataException, DotSecurityException;

	public List<WorkflowAction> findAvailableActions(Contentlet contentlet, User user) throws DotDataException,
	DotSecurityException ;

	public List<WorkflowAction> findActions(WorkflowStep step, User user) throws DotDataException,
			DotSecurityException;

	public void saveSchemeForStruct(Structure struc, WorkflowScheme scheme) throws DotDataException;

	public void saveAction(WorkflowAction action, List<Permission> perms) throws DotDataException, AlreadyExistException;

	public WorkflowStep findStep(String id) throws DotDataException;

	public void deleteAction(WorkflowAction action) throws DotDataException, AlreadyExistException;

	public List<WorkflowActionClass> findActionClasses(WorkflowAction action) throws DotDataException;

	public WorkflowActionClass findActionClass(String id) throws DotDataException;

	public void deleteActionClass(WorkflowActionClass actionClass) throws DotDataException, AlreadyExistException;

	public void saveActionClass(WorkflowActionClass actionClass) throws DotDataException, AlreadyExistException;

	public void reorderActionClass(WorkflowActionClass actionClass, int order) throws DotDataException, AlreadyExistException;

	public Map<String, WorkflowActionClassParameter> findParamsForActionClass(WorkflowActionClass actionClass) throws DotDataException;

	public List<WorkFlowActionlet> findActionlets() throws DotDataException;

	public WorkFlowActionlet findActionlet(String clazz) throws DotDataException;

	public void saveWorkflowActionClassParameters(List<WorkflowActionClassParameter> params) throws DotDataException;


	/***
	 *
	 * This method will take a WorkflowActionId (set in the contentlet map) and
	 * fire that action using the mod_user on the contentlet
	 * @param contentlet
	 * @throws DotDataException
	 */
	public WorkflowProcessor fireWorkflowPreCheckin(Contentlet contentlet, User user) throws DotDataException,DotWorkflowException, DotContentletValidationException;
	public void fireWorkflowPostCheckin(WorkflowProcessor wflow) throws DotDataException,DotWorkflowException;


	public WorkflowProcessor fireWorkflowNoCheckin(Contentlet contentlet, User user) throws DotDataException,DotWorkflowException, DotContentletValidationException;


	public int countTasks(WorkflowSearcher searcher)  throws DotDataException;

	public void copyWorkflowActionClassParameter(WorkflowActionClassParameter from, WorkflowActionClass to) throws DotDataException;
	public void copyWorkflowActionClass(WorkflowActionClass from, WorkflowAction to) throws DotDataException;
	public void copyWorkflowAction(WorkflowAction from, WorkflowStep to) throws DotDataException;
	public void copyWorkflowStep(WorkflowStep from, WorkflowScheme to) throws DotDataException;

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
}
