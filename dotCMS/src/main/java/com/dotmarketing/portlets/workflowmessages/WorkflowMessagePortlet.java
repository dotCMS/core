package com.dotmarketing.portlets.workflowmessages;


import java.io.IOException;

import com.dotcms.repackage.javax.portlet.PortletException;
import com.dotcms.repackage.javax.portlet.RenderRequest;
import com.dotcms.repackage.javax.portlet.RenderResponse;

import com.liferay.portal.struts.Action;
import com.liferay.portal.struts.ActionException;
import com.liferay.portal.util.InstancePool;
import com.liferay.portlet.JSPPortlet;

public class WorkflowMessagePortlet extends JSPPortlet {
	public void doView(RenderRequest req, RenderResponse res)
		throws IOException, PortletException {

		try {
			Action a =
				(Action) InstancePool.get(
					"com.dotmarketing.portlets.workflowmessages.c.a.ViewWorkflowMessagesPortletAction");
			a.run(req, res);
		}
		catch (ActionException ae) {
			throw new PortletException(ae);
		}


		super.doView(req, res);
	}

}
