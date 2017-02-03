package com.dotmarketing.startup.runonce;

import java.util.ArrayList;
import java.util.List;

import com.dotmarketing.startup.AbstractJDBCStartupTask;

/**
 * This update task will update every <code>VARCHAR</code> and <code>TEXT</code>
 * column types to <code>NVARCHAR</code> and <code>NVARCHAR(MAX)</code>
 * respectively.
 * <p>
 * Currently, all parameterized DB queries cannot use the indexes on the text
 * fields because the indexes are for ASCII text. SQL Server JDBC Drivers are
 * passing query parameters as UTF-8, which forces a whole table scan for all
 * tables: inode, tree, identifier tables, etc. This makes SQL Server operations
 * slow. In its current state, it is incapable of serving UTF-8 content.
 * 
 * @author Jose Castro
 * @version 3.7
 * @since Nov 10, 2016
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
		return  "ALTER TABLE workflow_task ALTER COLUMN assigned_to NVARCHAR(36) NULL;"
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