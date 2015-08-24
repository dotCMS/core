package com.dotmarketing.startup.runonce;

import java.util.List;

import com.dotmarketing.startup.AbstractJDBCStartupTask;

/**
 * Task to lower Case all the url in the virtual_link table
 * @author Oswaldo Gallango
 * @version 1.0
 * @since 08-24-2015
 */
public class Task03150LoweCaseURLOnVirtualLinksTable extends AbstractJDBCStartupTask {

	private final String SQL_QUERY ="UPDATE virtual_link SET url=LOWER(url);"; 
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
