package com.dotmarketing.startup.runonce;

import com.dotmarketing.startup.AbstractJDBCStartupTask;

import java.util.List;

/**
 * Adds the mod_date column to the User_ table.
 * 
 * @author jsanca
 * @version 3.7
 * @since Jun 14, 2016
 */
public class Task03700ModificationDateColumnAddedToUserTable extends AbstractJDBCStartupTask {

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
	public String getPostgresScript () {
		return "ALTER TABLE User_ ADD COLUMN mod_date timestamp null;";
	}

	/**
	 * The SQL for MySQL
	 *
	 * @return
	 */
	@Override
	public String getMySQLScript () {
		return "ALTER TABLE User_ ADD mod_date datetime null;";
	}

	/**
	 * The SQL for Oracle
	 *
	 * @return
	 */
	@Override
	public String getOracleScript () {
		return "ALTER TABLE User_ ADD mod_date date null;";
	}

	/**
	 * The SQL for MSSQL
	 *
	 * @return
	 */
	@Override
	public String getMSSQLScript () {
		return "ALTER TABLE User_ ADD mod_date   datetime null;";
	}

	/**
	 * The SQL for H2
	 *
	 * @return
	 */
	@Override
	public String getH2Script () {
		return "ALTER TABLE User_ ADD COLUMN mod_date timestamp null;";
	}

	@Override
	protected List<String> getTablesToDropConstraints() {
		return null;
	}

}
