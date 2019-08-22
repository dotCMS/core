package com.dotmarketing.startup.runonce;

import com.dotmarketing.business.Role;
import com.dotmarketing.util.Config;
import java.util.ArrayList;
import java.util.List;

import com.dotmarketing.startup.AbstractJDBCStartupTask;

/**
 * This update task will update the length of some columns and create the constraints that 
 * were failing in 3.7.
 * <p>
 * 
 * @author Erick Gonzalez
 * @version 3.7.1
 * @since Feb 3, 2017
 *
 */
public class Task03605FixMSSQLMissingConstraints extends AbstractJDBCStartupTask {

	@Override
	public boolean forceRun() {
		return true;
	}

	@Override
	public String getPostgresScript() {
		return null;
	}

	@Override
	public String getMySQLScript() {
		return null;
	}

	@Override
	public String getOracleScript() {
		return null;
	}

	@Override
	public String getMSSQLScript() {
		final String UPDATE_MISSING_WORKFLOW_ASSIGNMENTS = "UPDATE workflow_task SET assigned_to = "
				+ " (SELECT id FROM cms_role WHERE role_name = '" + Role.CMS_ADMINISTRATOR_ROLE + "') "
				+ " WHERE NOT EXISTS (SELECT 1 FROM cms_role rl WHERE rl.id = assigned_to); ";

		return  UPDATE_MISSING_WORKFLOW_ASSIGNMENTS
				+ "ALTER TABLE workflow_task ALTER COLUMN assigned_to NVARCHAR(36) NULL;"
				+ "ALTER TABLE workflow_task ALTER COLUMN status NVARCHAR(36) NULL;"
				+ "ALTER TABLE workflow_task ALTER COLUMN webasset NVARCHAR(36) NULL;"
				+ "ALTER TABLE workflow_task ADD CONSTRAINT FK_workflow_assign FOREIGN KEY (assigned_to) REFERENCES cms_role (id);"
				+ "ALTER TABLE workflow_task ADD CONSTRAINT FK_workflow_task_asset FOREIGN KEY (webasset) REFERENCES identifier (id);"
				+ "ALTER TABLE workflow_task ADD CONSTRAINT FK_workflow_step FOREIGN KEY (status) REFERENCES workflow_step (id);";
	}

	@Override
	public String getH2Script() {
		return null;
	}

	@Override
	protected List<String> getTablesToDropConstraints() {
		final List<String> tableList = new ArrayList<>();
		tableList.add("workflow_comment");
		tableList.add("workflow_history");
		tableList.add("workflow_task");
		tableList.add("workflowtask_files");
		
		return tableList;
	}
}