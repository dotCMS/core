package com.dotmarketing.portlets.workflows.business;

import java.util.List;
import java.util.Map;

import com.dotmarketing.business.DotStateException;
import com.dotmarketing.exception.AlreadyExistException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.fileassets.business.IFileAsset;
import com.dotmarketing.portlets.workflows.model.WorkflowAction;
import com.dotmarketing.portlets.workflows.model.WorkflowActionClass;
import com.dotmarketing.portlets.workflows.model.WorkflowActionClassParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowComment;
import com.dotmarketing.portlets.workflows.model.WorkflowHistory;
import com.dotmarketing.portlets.workflows.model.WorkflowScheme;
import com.dotmarketing.portlets.workflows.model.WorkflowSearcher;
import com.dotmarketing.portlets.workflows.model.WorkflowStep;
import com.dotmarketing.portlets.workflows.model.WorkflowTask;
import com.liferay.portal.model.User;

public interface WorkFlowFactory {

	public void deleteComment(WorkflowComment comment) throws DotDataException;

	public void deleteWorkflowHistory(WorkflowHistory history) throws DotDataException;

	public void deleteWorkflowTask(WorkflowTask task) throws DotDataException;

	public WorkflowTask findTaskByContentlet(Contentlet contentlet) throws DotDataException;

	public WorkflowTask findWorkFlowTaskById(String id) throws DotDataException;

	public List<WorkflowTask> searchTasks(WorkflowSearcher searcher) throws DotDataException;

	public WorkflowComment findWorkFlowCommentById(String id) throws DotDataException;

	public WorkflowHistory findWorkFlowHistoryById(String id) throws DotDataException;

	public List<WorkflowHistory> findWorkflowHistory(WorkflowTask task) throws DotDataException;

	public List<IFileAsset> findWorkflowTaskFiles(WorkflowTask task) throws DotDataException;

	public List<Contentlet> findWorkflowTaskFilesAsContent(WorkflowTask task, User user) throws DotDataException;

	public void attachFileToTask(WorkflowTask task, String fileInode) throws DotDataException;

	public void removeAttachedFile(WorkflowTask task, String fileInode) throws DotDataException;

	public List<WorkflowComment> findWorkFlowComments(WorkflowTask task) throws DotDataException;

	public WorkflowStep findStepByContentlet(Contentlet contentlet) throws DotDataException;

	public void saveComment(WorkflowComment comment) throws DotDataException;

	public void saveWorkflowHistory(WorkflowHistory history) throws DotDataException;

	public void saveWorkflowTask(WorkflowTask task) throws DotDataException;

	public List<WorkflowScheme> findSchemes(boolean showArchived) throws DotDataException;

	public WorkflowScheme findScheme(String id) throws DotDataException;

	public WorkflowScheme findSchemeForStruct(String id) throws DotDataException;

	public void deleteSchemeForStruct(String struc) throws DotDataException;

	public void saveSchemeForStruct(String struc, WorkflowScheme scheme) throws DotDataException;

	public void saveScheme(WorkflowScheme scheme) throws DotDataException, AlreadyExistException;

	public WorkflowScheme findDefaultScheme() throws DotDataException;

	public List<WorkflowStep> findSteps(WorkflowScheme scheme) throws DotDataException;

	public void saveStep(WorkflowStep step) throws DotDataException, AlreadyExistException;

	public List<WorkflowAction> findActions(WorkflowStep step) throws DotDataException;

	public WorkflowAction findAction(String id) throws DotDataException;

	public void saveAction(WorkflowAction action) throws DotDataException, AlreadyExistException;

	public WorkflowStep findStep(String id) throws DotDataException;

	public void deleteAction(WorkflowAction action) throws DotDataException, AlreadyExistException;

	public void deleteStep(WorkflowStep step) throws DotDataException, AlreadyExistException;
	
	public int getCountContentletsReferencingStep(WorkflowStep step) throws DotDataException;

	public List<WorkflowActionClass> findActionClasses(WorkflowAction action) throws DotDataException;

	public WorkflowActionClass findActionClass(String id) throws DotDataException;

	public void deleteActionClass(WorkflowActionClass actionClass) throws DotDataException, AlreadyExistException;

	public void saveActionClass(WorkflowActionClass actionClass) throws DotDataException, AlreadyExistException;

	public Map<String, WorkflowActionClassParameter> findParamsForActionClass(WorkflowActionClass actionClass) throws DotDataException;

	public void saveWorkflowActionClassParameter(WorkflowActionClassParameter param) throws DotDataException, AlreadyExistException;

	public void deleteWorkflowActionClassParameters(WorkflowActionClass actionClass) throws DotDataException, AlreadyExistException;

	public int countTasks(WorkflowSearcher searcher) throws DotDataException;

	public void copyWorkflowActionClassParameter(WorkflowActionClassParameter from, WorkflowActionClass actionClass) throws DotDataException;

	public void copyWorkflowActionClass(WorkflowActionClass from, WorkflowAction action) throws DotDataException;

	public void copyWorkflowAction(WorkflowAction from, WorkflowStep step) throws DotDataException;

	public void copyWorkflowStep(WorkflowStep from, WorkflowScheme scheme) throws DotDataException;

	// christian escalation
	public List<WorkflowTask> searchAllTasks(WorkflowSearcher searcher) throws DotDataException;

	public WorkflowHistory retrieveLastStepAction(String taskId) throws DotDataException;
	// christian escalation

    public List<WorkflowTask> findExpiredTasks() throws DotDataException, DotSecurityException;

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
}
