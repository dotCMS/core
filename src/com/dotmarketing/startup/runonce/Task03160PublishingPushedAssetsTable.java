package com.dotmarketing.startup.runonce;

import java.util.List;

import com.dotmarketing.startup.AbstractJDBCStartupTask;

/**
 * Task to create a new index for publishing_pushed_assets table
 * @author Rogelio Blanco
 * @version 1.0
 * @since 10-06-2015
 */
public class Task03160PublishingPushedAssetsTable extends AbstractJDBCStartupTask {

	private final String SQL_QUERY = "CREATE INDEX idx_pushed_assets_3 ON publishing_pushed_assets (asset_id, environment_id);"; 
	@Override
	public boolean forceRun() {
		return true;
	}

	/**
	 * The SQL for Postgres
	 *
	 * @return
	 */
	@Override
	public String getPostgresScript() {
		return SQL_QUERY;
	}

	/**
	 * The SQL for MySQL
	 *
	 * @return
	 */
	@Override
	public String getMySQLScript() {
		return SQL_QUERY;
	}

	/**
	 * The SQL for Oracle
	 *
	 * @return
	 */
	@Override
	public String getOracleScript() {
		return SQL_QUERY;
	}

	/**
	 * The SQL for MSSQL
	 *
	 * @return
	 */
	@Override
	public String getMSSQLScript() {
		return SQL_QUERY;
	}

	/**
	 * The SQL for H2
	 *
	 * @return
	 */
	@Override
	public String getH2Script() {
		return SQL_QUERY;
	}

	@Override
	protected List<String> getTablesToDropConstraints() {
		return null;
	}

}
