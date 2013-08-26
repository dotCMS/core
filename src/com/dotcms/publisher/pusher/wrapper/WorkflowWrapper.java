package com.dotcms.publisher.pusher.wrapper;

import java.util.HashMap;
import java.util.List;

import com.dotcms.publisher.pusher.PushPublisherConfig.Operation;
import com.dotmarketing.portlets.workflows.model.WorkflowAction;
import com.dotmarketing.portlets.workflows.model.WorkflowScheme;
import com.dotmarketing.portlets.workflows.model.WorkflowStep;

public class WorkflowWrapper {
	private WorkflowScheme scheme;
	private HashMap<WorkflowStep, List<WorkflowAction>> stepActionMap;
	private Operation operation;

	public WorkflowWrapper(WorkflowScheme scheme, HashMap<WorkflowStep, List<WorkflowAction>> stepActionMap) {
		this.scheme = scheme;
		this.stepActionMap = stepActionMap;
	}


	public WorkflowScheme getScheme() {
		return scheme;
	}


	public void setScheme(WorkflowScheme scheme) {
		this.scheme = scheme;
	}


	public HashMap<WorkflowStep, List<WorkflowAction>> getStepActionMap() {
		return stepActionMap;
	}


	public void setStepActionMap(
			HashMap<WorkflowStep, List<WorkflowAction>> stepActionMap) {
		this.stepActionMap = stepActionMap;
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
