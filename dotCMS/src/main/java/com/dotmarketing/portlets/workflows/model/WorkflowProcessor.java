package com.dotmarketing.portlets.workflows.model;

import static com.dotmarketing.business.APILocator.getRoleAPI;
import static com.dotmarketing.business.APILocator.getWorkflowAPI;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Role;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ContentletDependencies;
import com.dotmarketing.portlets.workflows.business.DotWorkflowException;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.language.LanguageException;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.User;
import java.util.List;
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

	public ContentletDependencies getContentletDependencies() {
		return contentletDependencies;
	}

	public void setContentletDependencies(final ContentletDependencies contentletDependencies) {
		this.contentletDependencies = contentletDependencies;
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



	public WorkflowProcessor(Contentlet contentlet, User firingUser) {
		this.contentlet = contentlet;

		try {

			this.user = firingUser;

			WorkflowStep contentStep = getWorkflowAPI().findStepByContentlet(contentlet);
			if (null != contentStep) {
				scheme = getWorkflowAPI().findScheme(contentStep.getSchemeId());
			}
			task = getWorkflowAPI().findTaskByContentlet(contentlet);

			String workflowActionId = contentlet.getStringProperty(Contentlet.WORKFLOW_ACTION_KEY);
			if (UtilMethods.isSet(workflowActionId)) {
				action = findAction(contentlet, workflowActionId);
			}

			//If we found and action and we don't have a workflow we can search for it
			if (null != action && null == scheme) {
				scheme = getWorkflowAPI().findScheme(action.getSchemeId());
			}

			if (!UtilMethods.isSet(workflowActionId) && task.isNew() && null != scheme) {
				workflowActionId = scheme.getEntryActionId();
				if (UtilMethods.isSet(workflowActionId)) {
					action = findAction(contentlet, workflowActionId);
				}
			}

			if (null == action)
				return;

			if(action.requiresCheckout()){
				try {
					APILocator.getContentletAPI().canLock(contentlet, user);
				} catch (Exception ex) {
					throw new DotWorkflowException(LanguageUtil
							.get(user, "message.workflow.error.content.requires.lock")
							+ contentlet.getStructure().getName(), ex);
				}
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

			nextStep      = (action.isNextStepCurrentStep())?
					contentStep:getWorkflowAPI().findStep(action.getNextStep());
			step          = contentStep;
			actionClasses = getWorkflowAPI().findActionClasses(action);
			if(null == scheme) {
                scheme = getWorkflowAPI().findScheme(step.getSchemeId());
            }

			if(task != null && UtilMethods.isSet(task.getId())){
				history = getWorkflowAPI().findWorkflowHistory(task);
			}

		} catch (Exception e) {
			throw new DotWorkflowException(e.getMessage(),e);
		}
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
			} catch (Exception ex) {
				throw new DotWorkflowException(
						LanguageUtil.get(this.user, "message.workflow.error.invalid.action")
								+ contentlet.getStringProperty(Contentlet.WORKFLOW_ACTION_KEY), ex);
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
}
