package com.dotmarketing.portlets.workflows.model;

import static com.dotmarketing.business.APILocator.getRoleAPI;
import static com.dotmarketing.business.APILocator.getWorkflowAPI;

import java.util.List;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Role;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.workflows.business.DotWorkflowException;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
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



	public WorkflowProcessor(Contentlet contentlet) {
		this.contentlet = contentlet;

		try {
			user = APILocator.getUserAPI().loadUserById(contentlet.getModUser(), APILocator.getUserAPI().getSystemUser(), false);
			scheme = getWorkflowAPI().findSchemeForStruct(contentlet.getStructure());
			task = getWorkflowAPI().findTaskByContentlet(contentlet);
			
			String workflowActionId = contentlet.getStringProperty(Contentlet.WORKFLOW_ACTION_KEY);
			if (!UtilMethods.isSet(workflowActionId) && task.isNew()){
				workflowActionId=scheme.getEntryActionId();
			}	
			
			
			
			
			if (!UtilMethods.isSet(workflowActionId)) {
				if (scheme.isMandatory() ) {
					throw new DotWorkflowException("A workflow action is manditory for content of type: " + contentlet.getStructure().getName());
				}
				
				return;
			}

			
			try{
				action = getWorkflowAPI().findAction(workflowActionId, user);
			}
			catch(Exception ex){
				throw new DotWorkflowException("invalid workflow action specified:" + contentlet.getStringProperty(Contentlet.WORKFLOW_ACTION_KEY));
			}
			
			
			if(action.requiresCheckout()){
				try{
					APILocator.getContentletAPI().canLock(contentlet, user);
				}
				catch(Exception ex){
					throw new DotWorkflowException("This workflow action requires a lock on the content before executing for content of type: " + contentlet.getStructure().getName());
				}
			}
			if (UtilMethods.isSet(contentlet.getStringProperty(Contentlet.WORKFLOW_ASSIGN_KEY))) {
				nextAssign = getRoleAPI().loadRoleById(contentlet.getStringProperty(Contentlet.WORKFLOW_ASSIGN_KEY));
			}else{
				nextAssign = getRoleAPI().loadRoleById(action.getNextAssign());
			}
			
			
			// if the action's next assign is the "System User", we assign to the user executing the workflow
			if(getRoleAPI().loadCMSAnonymousRole().getId().equals(nextAssign.getId())){
				nextAssign = getRoleAPI().loadRoleByKey(user.getUserId());
			}
			
			
			
			if(UtilMethods.isSet(Contentlet.WORKFLOW_COMMENTS_KEY)){
				workflowMessage = contentlet.getStringProperty(Contentlet.WORKFLOW_COMMENTS_KEY);
			}
			
			nextStep = getWorkflowAPI().findStep(action.getNextStep());
			step = getWorkflowAPI().findStep(action.getStepId());
			actionClasses = getWorkflowAPI().findActionClasses(action);

			if(task != null && UtilMethods.isSet(task.getId())){
				history = getWorkflowAPI().findWorkflowHistory(task);
			}
				
				




			


		} catch (Exception e) {
			throw new DotWorkflowException(e.getMessage());
		}
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
