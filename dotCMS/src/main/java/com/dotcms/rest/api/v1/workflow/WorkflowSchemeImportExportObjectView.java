package com.dotcms.rest.api.v1.workflow;

import com.dotcms.repackage.com.fasterxml.jackson.annotation.JsonCreator;
import com.dotcms.repackage.com.fasterxml.jackson.annotation.JsonProperty;
import com.dotmarketing.portlets.workflows.model.*;
import com.dotmarketing.portlets.workflows.util.WorkflowSchemeImportExportObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * View for the WorkflowSchemeImportExportObject
 * @author jsanca
 */
public class WorkflowSchemeImportExportObjectView implements Serializable {

	private final String version;
	private final List<WorkflowScheme> schemes;
	private final List<WorkflowStep> steps;
	private final List<WorkflowAction> actions;
	private final List<Map<String, String>> actionSteps;
	private final List<WorkflowActionClass> actionClasses;
	private final List<WorkflowActionClassParameter> actionClassParams;

	@JsonCreator
	public WorkflowSchemeImportExportObjectView(@JsonProperty("version") 		   final String version,
												@JsonProperty("schemes") 		   final List<WorkflowScheme> 			    schemes,
												@JsonProperty("steps") 			   final List<WorkflowStep>				    steps,
												@JsonProperty("actions")		   final List<WorkflowAction> 		        actions,
												@JsonProperty("actionSteps") 	   final List<Map<String, String>> 		    actionSteps,
												@JsonProperty("actionClasses") 	   final List<WorkflowActionClass> 		    actionClasses,
												@JsonProperty("actionClassParams") final List<WorkflowActionClassParameter> actionClassParams) {

		this.version = version;
		this.schemes = schemes;
		this.steps = steps;
		this.actions = actions;
		this.actionSteps = actionSteps;
		this.actionClasses = actionClasses;
		this.actionClassParams = actionClassParams;
	}

	public WorkflowSchemeImportExportObjectView(final String version, final WorkflowSchemeImportExportObject workflowExportObject) {

		this (version, workflowExportObject.getSchemes(), workflowExportObject.getSteps(), workflowExportObject.getActions(),
				workflowExportObject.getActionSteps(), workflowExportObject.getActionClasses(), workflowExportObject.getActionClassParams());
	}

	public List<Map<String, String>> getActionSteps() {
		if(actionSteps != null){
			return actionSteps;
		}

		return new ArrayList<Map<String,String>>();
	}


	public String getVersion() {
		return version;
	}

	public List<WorkflowScheme> getSchemes() {
		return schemes;
	}


	public List<WorkflowStep> getSteps() {
		return steps;
	}


	public List<WorkflowAction> getActions() {
		return actions;
	}


	public List<WorkflowActionClass> getActionClasses() {
		return actionClasses;
	}


	public List<WorkflowActionClassParameter> getActionClassParams() {
		return actionClassParams;
	}

}
