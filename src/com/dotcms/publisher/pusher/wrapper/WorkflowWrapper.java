package com.dotcms.publisher.pusher.wrapper;

import java.util.List;
import java.util.Map;

import com.dotcms.publisher.pusher.PushPublisherConfig.Operation;
import com.dotmarketing.business.Role;
import com.dotmarketing.portlets.workflows.model.WorkflowAction;
import com.dotmarketing.portlets.workflows.model.WorkflowActionClass;
import com.dotmarketing.portlets.workflows.model.WorkflowActionClassParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowScheme;
import com.dotmarketing.portlets.workflows.model.WorkflowStep;

public class WorkflowWrapper {
	private WorkflowScheme scheme;
	private List<WorkflowStep> steps;
	private List<WorkflowAction> actions;
	private Map<WorkflowAction, List<Role>> actionRoles;
	private List<WorkflowActionClass> actionClasses;
	private List<WorkflowActionClassParameter> actionClassParams;
	private Operation operation;

	public WorkflowWrapper(WorkflowScheme scheme, List<WorkflowStep> steps, List<WorkflowAction> actions, Map<WorkflowAction,
	      List<Role>> actionRoles, List<WorkflowActionClass> actionClasses, List<WorkflowActionClassParameter> actionClassParams) {
		this.scheme = scheme;
		this.steps = steps;
		this.actions = actions;
		this.actionRoles = actionRoles;
		this.actionClasses = actionClasses;
		this.actionClassParams = actionClassParams;
	}


	public WorkflowScheme getScheme() {
		return scheme;
	}


	public void setScheme(WorkflowScheme scheme) {
		this.scheme = scheme;
	}

	public List<WorkflowStep> getSteps() {
		  return steps;
		}


	public void setSteps(List<WorkflowStep> steps) {
	  this.steps = steps;
	}


	public List<WorkflowAction> getActions() {
	  return actions;
	}


	public void setActions(List<WorkflowAction> actions) {
	  this.actions = actions;
	}
	

	public Map<WorkflowAction, List<Role>> getActionRoles() {
		return actionRoles;
	}


	public void setActionRoles(Map<WorkflowAction, List<Role>> actionRoles) {
		this.actionRoles = actionRoles;
	}

	public List<WorkflowActionClass> getActionClasses() {
		return actionClasses;
	}


	public void setActionClasses(List<WorkflowActionClass> actionClasses) {
		this.actionClasses = actionClasses;
	}

	public List<WorkflowActionClassParameter> getActionClassParams() {
		return actionClassParams;
	}


	public void setActionClassParams(
			List<WorkflowActionClassParameter> actionClassParams) {
		this.actionClassParams = actionClassParams;
	}



	/**
	 * @return the operation
	 */
	public Operation getOperation() {
		return operation;
	}

	/**
	 * @param operation the operation to set
	 */
	public void setOperation(Operation operation) {
		this.operation = operation;
	}
}