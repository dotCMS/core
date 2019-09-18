package com.dotcms.publisher.pusher.wrapper;

import com.dotcms.publishing.PublisherConfig.Operation;
import com.dotmarketing.business.Role;
import com.dotmarketing.portlets.workflows.model.SystemActionWorkflowActionMapping;
import com.dotmarketing.portlets.workflows.model.WorkflowAction;
import com.dotmarketing.portlets.workflows.model.WorkflowActionClass;
import com.dotmarketing.portlets.workflows.model.WorkflowActionClassParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowScheme;
import com.dotmarketing.portlets.workflows.model.WorkflowStep;
import java.util.List;
import java.util.Map;

/**
 * Encapsulates the necessary object to represent a workflow to PP
 */
public class WorkflowWrapper {
	private WorkflowScheme scheme;
	private List<WorkflowStep> steps;
	private List<WorkflowAction> actions;
	private Map<WorkflowAction, List<Role>> actionRoles;
	private Map<String, String> actionNextAssignRolekeyMap;
	private List<WorkflowActionClass> actionClasses;
	private List<WorkflowActionClassParameter> actionClassParams;
	private Operation operation;
	private List<Map<String, String>>  actionStepsListMap;
	private List<SystemActionWorkflowActionMapping> systemActionMappings;

	public WorkflowWrapper(final WorkflowScheme scheme,
						   final List<WorkflowStep> steps,
						   final List<WorkflowAction> actions,
						   final Map<WorkflowAction, List<Role>> actionRoles,
						   final List<WorkflowActionClass> actionClasses,
						   final List<WorkflowActionClassParameter> actionClassParams,
						   final Map<String, String> actionNextAssignRolekeyMap,
						   final List<Map<String, String>> actionStepsListMap,
						   final List<SystemActionWorkflowActionMapping> systemActionMappings) {

		this.scheme = scheme;
		this.steps  = steps;
		this.actions     = actions;
		this.actionRoles = actionRoles;
		this.actionClasses     = actionClasses;
		this.actionClassParams = actionClassParams;
		this.actionNextAssignRolekeyMap = actionNextAssignRolekeyMap;
		this.actionStepsListMap         = actionStepsListMap;
		this.systemActionMappings       = systemActionMappings;
	}

	public List<SystemActionWorkflowActionMapping> getSystemActionMappings() {
		return systemActionMappings;
	}

	public void setSystemActionMappings(final List<SystemActionWorkflowActionMapping> systemActionMappings) {
		this.systemActionMappings = systemActionMappings;
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


	public Map<String, String> getActionNextAssignRolekeyMap() {
		return actionNextAssignRolekeyMap;
	}


	public void setActionsNextAssignRolekeyMap(
			Map<String, String> actionNextAssignRolekeyMap) {
		this.actionNextAssignRolekeyMap = actionNextAssignRolekeyMap;
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

	/**
	 * Set the action steps map
	 * @param actionStepsListMap List of action steps map
	 */
	public void setStepActions(List<Map<String, String>> actionStepsListMap) {
		this.actionStepsListMap = actionStepsListMap;
	}

	/**
	 * Get the List of related action steps map
	 * @return a List action steps map
	 */
	public List<Map<String, String>> getStepActions() {
		return actionStepsListMap;
	}
}
