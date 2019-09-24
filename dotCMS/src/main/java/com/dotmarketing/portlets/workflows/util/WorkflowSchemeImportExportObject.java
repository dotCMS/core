package com.dotmarketing.portlets.workflows.util;

import com.dotmarketing.portlets.workflows.model.SystemActionWorkflowActionMapping;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.dotmarketing.portlets.workflows.model.WorkflowAction;
import com.dotmarketing.portlets.workflows.model.WorkflowActionClass;
import com.dotmarketing.portlets.workflows.model.WorkflowActionClassParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowScheme;
import com.dotmarketing.portlets.workflows.model.WorkflowStep;

public class WorkflowSchemeImportExportObject implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	List<WorkflowScheme> schemes;
	List<WorkflowStep> steps;
	List<WorkflowAction> actions;
	List<Map<String, String>> actionSteps;
	List<WorkflowActionClass> actionClasses;
	List<WorkflowActionClassParameter> actionClassParams;
	List<Map<String, String>> workflowStructures;
	List<SystemActionWorkflowActionMapping> schemeSystemActionWorkflowActionMappings;
	List<SystemActionWorkflowActionMapping> contentTpeSystemActionWorkflowActionMappings;


	public List<Map<String, String>> getActionSteps() {
		if(actionSteps != null){
			return actionSteps;
		}

		return new ArrayList<Map<String,String>>();
	}

	public void setActionSteps(List<Map<String, String>> actionSteps) {
		this.actionSteps = actionSteps;
	}

	public List<Map<String, String>> getWorkflowStructures() {
		if(workflowStructures != null){
			return workflowStructures;
		}
		else{
			return new ArrayList<Map<String,String>>();
		}
	}

	public void setWorkflowStructures(List<Map<String, String>> workflowStructures) {
		this.workflowStructures = workflowStructures;
	}

	public List<WorkflowScheme> getSchemes() {
		return schemes;
	}

	public void setSchemes(List<WorkflowScheme> schemes) {
		this.schemes = schemes;
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

	public List<WorkflowActionClass> getActionClasses() {
		return actionClasses;
	}

	public void setActionClasses(List<WorkflowActionClass> actionClasses) {
		this.actionClasses = actionClasses;
	}

	public List<WorkflowActionClassParameter> getActionClassParams() {
		return actionClassParams;
	}

	public void setActionClassParams(List<WorkflowActionClassParameter> actionClassParams) {
		this.actionClassParams = actionClassParams;
	}

	public List<SystemActionWorkflowActionMapping> getSchemeSystemActionWorkflowActionMappings() {
		return schemeSystemActionWorkflowActionMappings;
	}

	public void setSchemeSystemActionWorkflowActionMappings(
			List<SystemActionWorkflowActionMapping> schemeSystemActionWorkflowActionMappings) {
		this.schemeSystemActionWorkflowActionMappings = schemeSystemActionWorkflowActionMappings;
	}

	public List<SystemActionWorkflowActionMapping> getContentTpeSystemActionWorkflowActionMappings() {
		return contentTpeSystemActionWorkflowActionMappings;
	}

	public void setContentTpeSystemActionWorkflowActionMappings(
			List<SystemActionWorkflowActionMapping> contentTpeSystemActionWorkflowActionMappings) {
		this.contentTpeSystemActionWorkflowActionMappings = contentTpeSystemActionWorkflowActionMappings;
	}
}
