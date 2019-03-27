package com.dotmarketing.portlets.workflows.model;

import java.io.Serializable;

import com.dotmarketing.exception.DotRuntimeException;


public class WorkflowActionFailureException extends DotRuntimeException implements Serializable {

	public WorkflowActionFailureException(String x, Exception e) {
		super(x, e);
		
	}

	public WorkflowActionFailureException(String x) {
		super(x);
		
	}

}
