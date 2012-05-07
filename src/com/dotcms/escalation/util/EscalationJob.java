package com.dotcms.escalation.util;

import java.util.Date;
import java.util.List;

import org.quartz.JobExecutionContext;
import org.quartz.StatefulJob;

import com.dotcms.escalation.business.ExpiryTaskAPI;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Role;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.workflows.business.WorkflowAPI;
import com.dotmarketing.portlets.workflows.model.WorkflowAction;
import com.dotmarketing.portlets.workflows.model.WorkflowHistory;
import com.dotmarketing.portlets.workflows.model.WorkflowTask;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.DateUtil;
import com.dotmarketing.util.Logger;

public class EscalationJob implements StatefulJob {

	private String maintenanceRoleKey = "com.dotcms.escalation";
	private WorkflowAPI wAPI = APILocator.getWorkflowAPI();
	private ExpiryTaskAPI expAPI = ExpiryTaskAPI.getInstance();

	private String name = "*";

	public void execute(JobExecutionContext arg0) {

		Logger.info(EscalationJob.class, "Running Escalation  Job ---- CHRI -----");

		try {
			String d = Config.getStringProperty("escalation.job.java.expiryTime", maintenanceRoleKey);
			int days = Integer.parseInt(d);
			
			String roleKeyToEscale = Config.getStringProperty("escalation.job.java.roleToEscale", maintenanceRoleKey);

			try {

				List<Role> rolesL = (List<Role>) APILocator.getRoleAPI().findAllAssignableRoles(false);

				List<WorkflowTask> tasks = APILocator.getWorkflowAPI().searchAllTasks(null);

				if(tasks.size() > 0) {
					for (WorkflowTask workflowTask : tasks) {

						if((DateUtil.diffDates(workflowTask.getModDate(), new Date())).get("diffHours") > (days != 0 ? days : 0)) {


							WorkflowHistory whistory;
							try {
								whistory = (WorkflowHistory) wAPI.retrieveLastStepAction(workflowTask.getId());
								WorkflowAction action = wAPI.findAction(whistory.getActionId(), APILocator.getUserAPI().getSystemUser());

								try {
									expAPI.escaleTask(workflowTask.getId(), APILocator.getRoleAPI().loadRoleByKey(roleKeyToEscale).getId());
								} catch (Exception e) {
									Logger.error(EscalationJob.class, e.getMessage(), e);
								}

							} catch (DotSecurityException e) {
								Logger.error(EscalationJob.class, e.getMessage(), e);
							}

						} else {

						}

					}
				}
			} catch (DotDataException e) {
				Logger.error(EscalationJob.class, e.getMessage(), e);
			}
		} catch (NumberFormatException e1) {
			Logger.error(EscalationJob.class, e1.getMessage(), e1);
		}

		List<Role> rolesL;

		Logger.info(EscalationJob.class, "Finishing Escalation  Job ---- CHRI -----");

	}

}
