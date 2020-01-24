package com.dotmarketing.portlets.workflows.model;

import static com.dotmarketing.business.APILocator.getRoleAPI;
import static com.dotmarketing.business.APILocator.getWorkflowAPI;

import com.dotmarketing.business.Role;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ContentletDependencies;
import com.dotmarketing.portlets.workflows.business.DotWorkflowException;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.language.LanguageException;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.User;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
public class WorkflowProcessor {

	Contentlet contentlet;
	WorkflowAction action;
	WorkflowStep step;
	List<WorkflowStep> steps;
	WorkflowScheme scheme;
	User user;
	Role nextAssign;
	Role previousAssign;
	WorkflowStep nextStep ;
	WorkflowTask task;
	List<WorkflowHistory> history;
	String workflowMessage;
	List<WorkflowActionClass> actionClasses;
	ContentletDependencies    contentletDependencies;
	private final AtomicBoolean abort  = new AtomicBoolean(false);
	private final Map<String, Object> contextMap = new HashMap<>();

	private ConcurrentMap<String,Object> actionsContext;

	/**
	 * True if the processor was aborted
	 * @return boolean
	 */
	public boolean abort () {
		return this.abort.get();
	}

	/**
	 * Be carefull on calling this method, it will abort the processor of the current workflow.
	 */
	public void abortProcessor () {
		this.abort.set(true);
	}

	public ContentletDependencies getContentletDependencies() {
		return contentletDependencies;
	}

	public void setContentletDependencies(final ContentletDependencies contentletDependencies) {
		this.contentletDependencies = contentletDependencies;
	}

	/**
	 * Get the context map for this processor.
	 * @return Map
	 */
	public Map<String, Object> getContextMap() {
		return contextMap;
	}

	public List<WorkflowActionClass> getActionClasses() {
		return actionClasses;
	}

	public void setActionClasses(List<WorkflowActionClass> actionClasses) {
		this.actionClasses = actionClasses;
	}

	public String getWorkflowMessage() {
		return workflowMessage;
	}

	public void setWorkflowMessage(String workflowMessage) {
		this.workflowMessage = workflowMessage;
	}


	public Role getNextAssign() {
		return nextAssign;
	}

	public void setNextAssign(Role nextAssign) {
		this.nextAssign = nextAssign;
	}


	public Role getPreviousAssign() {
		return previousAssign;
	}

	public void setPreviousAssign(Role previousAssign) {
		this.previousAssign = previousAssign;
	}

	public WorkflowStep getNextStep() {
		return nextStep;
	}

	public void setNextStep(WorkflowStep nextStep) {
		this.nextStep = nextStep;
	}

	public List<WorkflowHistory> getHistory() {
		return history;
	}

	public void setHistory(List<WorkflowHistory> history) {
		this.history = history;
	}

	public WorkflowProcessor(final Contentlet contentlet, final User firingUser) {
       this(contentlet, firingUser, null);
	}

	public WorkflowProcessor(final Contentlet contentlet, final User firingUser, final ConcurrentMap<String,Object> actionsContext) {
		this.contentlet = contentlet;

		try {

			this.user = firingUser;

			WorkflowStep contentStep = getWorkflowAPI().findStepByContentlet(contentlet);
			if (null != contentStep) {
				scheme = getWorkflowAPI().findScheme(contentStep.getSchemeId());
			}
			task = getWorkflowAPI().findTaskByContentlet(contentlet);

			String workflowActionId = contentlet.getActionId();
			if (UtilMethods.isSet(workflowActionId)) {
				action = findAction(contentlet, workflowActionId);
			}

			//If we found and action and we don't have a workflow we can search for it
			if (null != action && null == scheme) {
				scheme = getWorkflowAPI().findScheme(action.getSchemeId());
			}

			if (null == action) {
				Logger.error(this,"Contentlet Identifier ("+contentlet.getIdentifier()+") should not have a null workflow action");
				return;
			}

			if (UtilMethods.isSet(contentlet.getStringProperty(Contentlet.WORKFLOW_ASSIGN_KEY))) {
				nextAssign = getRoleAPI().loadRoleById(contentlet.getStringProperty(Contentlet.WORKFLOW_ASSIGN_KEY));
			}
			if(!UtilMethods.isSet(nextAssign)){
				nextAssign = getRoleAPI().loadRoleById(action.getNextAssign());
			}

			// if the action's next assign is the "System User", we assign to the user executing the workflow
			if((!UtilMethods.isSet(nextAssign)) || getRoleAPI().loadCMSAnonymousRole().getId().equals(nextAssign.getId())){
				nextAssign = getRoleAPI().loadRoleByKey(user.getUserId());
			}

			if(UtilMethods.isSet(Contentlet.WORKFLOW_COMMENTS_KEY)){
				workflowMessage = contentlet.getStringProperty(Contentlet.WORKFLOW_COMMENTS_KEY);
			}

			if (null == contentStep) {

				contentStep = this.findFirstStepByScheme (action.getSchemeId());
			}

			nextStep      = (action.isNextStepCurrentStep())?
					contentStep:getWorkflowAPI().findStep(action.getNextStep());
			step          = contentStep;
			actionClasses = getWorkflowAPI().findActionClasses(action);
			if(null == scheme) {
                scheme = getWorkflowAPI().findScheme(action.getSchemeId());
            }

			if(task != null && UtilMethods.isSet(task.getId())){
				history = getWorkflowAPI().findWorkflowHistory(task);
			}

			this.actionsContext = actionsContext;

		} catch (Exception e) {
			throw new DotWorkflowException(e.getMessage(),e);
		}
	}

	private WorkflowStep findFirstStepByScheme(final String schemeId) throws DotDataException, DotSecurityException {

		return getWorkflowAPI().findSteps(getWorkflowAPI().findScheme(schemeId)).stream().findFirst().orElse(null);
	}

	/**
	 * Searches and returns a WorkflowAction using a given workflow action id, if the Processor
	 * already have associated an action the existing action will be returned and no search will be
	 * executed.
	 */
	private WorkflowAction findAction(final Contentlet contentlet, final String workflowActionId)
			throws LanguageException {

		if (null == action) {
			try {
				action = getWorkflowAPI().findActionRespectingPermissions(workflowActionId, contentlet, this.user);
			} catch (DotSecurityException e) {
				throw new DotWorkflowException(e.getMessage(), e);
			} catch (Exception ex) {
				throw new DotWorkflowException(
						LanguageUtil.get(this.user, "message.workflow.error.invalid.action")
								+ contentlet.getActionId(), ex);
			}
		}

		return action;
	}

	public Contentlet getContentlet() {
		return contentlet;
	}

	public void setContentlet(Contentlet contentlet) {
		this.contentlet = contentlet;
	}

	public WorkflowAction getAction() {
		return action;
	}

	public void setAction(WorkflowAction action) {
		this.action = action;
	}

	public WorkflowStep getStep() {
		return step;
	}

	public void setStep(WorkflowStep step) {
		this.step = step;
	}

	public List<WorkflowStep> getSteps() {
		return steps;
	}

	public void setSteps(List<WorkflowStep> steps) {
		this.steps = steps;
	}

	public WorkflowScheme getScheme() {
		return scheme;
	}

	public void setScheme(WorkflowScheme scheme) {
		this.scheme = scheme;
	}

	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}

	public WorkflowTask getTask() {
		return task;
	}

	public void setTask(WorkflowTask task) {
		this.task = task;
	}

	public boolean inProcess(){
		return UtilMethods.isSet(action);
	}

	public boolean isRunningBulk(){
		final Boolean runningBulk = Boolean.class.cast(getContentlet().getMap().get(Contentlet.WORKFLOW_BULK_KEY));
		return runningBulk != null && runningBulk;
	}

	public ConcurrentMap<String,Object> getActionsContext() {
		return actionsContext;
	}
}
