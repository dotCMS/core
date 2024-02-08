package com.dotmarketing.portlets.workflows.business;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.workflows.ajax.WfStepAjax;
import com.liferay.portal.model.User;

/*
 * Needed to test WorkflowAPITest class
 */
public class TestableWfStepAjax extends WfStepAjax {
	@Override
	public User getUser() {
		
		try {
			return APILocator.getUserAPI().getSystemUser();
		} catch (DotDataException e) {
			return null;
		}
	}

}
