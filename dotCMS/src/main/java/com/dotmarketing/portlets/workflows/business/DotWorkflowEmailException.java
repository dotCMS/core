package com.dotmarketing.portlets.workflows.business;

import com.dotmarketing.business.DotStateException;

public class DotWorkflowEmailException extends DotStateException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public DotWorkflowEmailException(String x) {
		super(x);

	}

	public DotWorkflowEmailException(String x, Exception cause) {
		super(x, cause);

	}

}
