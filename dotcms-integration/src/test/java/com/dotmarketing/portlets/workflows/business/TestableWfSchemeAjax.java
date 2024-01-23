package com.dotmarketing.portlets.workflows.business;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.workflows.ajax.WfSchemeAjax;
import com.liferay.portal.model.User;

/*
 * Needed to test WorkflowAPITest class
 */
public class TestableWfSchemeAjax extends WfSchemeAjax {
	@Override
	public User getUser() {
		
		try {
			return APILocator.getUserAPI().getSystemUser();
		} catch (DotDataException e) {
			return null;
		}
	}

}
