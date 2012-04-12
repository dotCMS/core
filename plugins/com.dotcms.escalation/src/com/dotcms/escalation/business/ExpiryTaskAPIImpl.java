package com.dotcms.escalation.business;

import java.util.Date;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.plugin.business.PluginAPI;
import com.dotmarketing.portlets.workflows.business.WorkflowAPI;
import com.dotmarketing.portlets.workflows.model.WorkflowTask;
import com.dotmarketing.util.Logger;

public class ExpiryTaskAPIImpl extends ExpiryTaskAPI {
	
	private static ExpiryTaskAPIImpl instance= null;

	public static ExpiryTaskAPIImpl getInstance() {
		if(instance == null) {
			instance = new ExpiryTaskAPIImpl();
		}

		return instance;
	}

	private static WorkflowAPI wAPI = APILocator.getWorkflowAPI();

	protected static String UPDATE_USER_ASSIGNTO_TASK = "update workflow_task set assigned_to = ? , mod_date = ? where id = ?";

	@Override
	public void escaleTask(String taskId, String roleId) throws Exception {

		WorkflowTask task = (WorkflowTask) wAPI.findTaskById(taskId);

		final DotConnect db = new DotConnect();
		try {

			db.setSQL(UPDATE_USER_ASSIGNTO_TASK);
			db.addParam(roleId);
			db.addParam(new Date());
			db.addParam(task.getId());

			db.loadResult();
			

		} catch (final Exception e) {
			Logger.debug(this.getClass(), e.getMessage(), e);
		}
		// wfAPI.escaleTask(task, roleId);

	}

}
