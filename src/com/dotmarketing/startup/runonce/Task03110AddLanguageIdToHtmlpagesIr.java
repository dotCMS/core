package com.dotmarketing.startup.runonce;

import java.sql.SQLException;

import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.startup.StartupTask;

/**
 * This task adds an extra column to the HTMLPAGES_IR table: The "language_id"
 * column. This column is necessary to resolve integrity conflicts with the new
 * Content Pages since the pages are now classified by language.
 * 
 * @author Jose Castro
 * @version 1.0
 * @since 02-02-2014
 *
 */
public class Task03110AddLanguageIdToHtmlpagesIr implements StartupTask {

	private static final String H2_QUERY = "ALTER TABLE htmlpages_ir ADD language_id bigint DEFAULT 0;";
	private static final String POSTGRESQL_QUERY = "ALTER TABLE htmlpages_ir ADD COLUMN language_id int8 DEFAULT 0;";
	private static final String MYSQL_QUERY = "ALTER TABLE htmlpages_ir ADD language_id bigint DEFAULT 0;";
	private static final String MSSQL_QUERY = "ALTER TABLE htmlpages_ir ADD language_id numeric(19,0) DEFAULT 0;";
	private static final String ORACLE_QUERY = "ALTER TABLE htmlpages_ir ADD language_id number(19,0) DEFAULT 0";

	@Override
	public boolean forceRun() {
		return true;
	}

	@Override
	public void executeUpgrade() throws DotDataException, DotRuntimeException {
		try {
			DbConnectionFactory.getConnection().setAutoCommit(true);
		} catch (SQLException e) {
			throw new DotDataException(e.getMessage(), e);
		}
		try {
			addLanguageColumn();
		} catch (SQLException e) {
			throw new DotRuntimeException(e.getMessage(), e);
		}
	}

	/**
	 * Adds a new column to the <b>HTMLPAGES_IR</b> table, called
	 * <code>language_id</code>. The new content pages have a specific language
	 * ID, which is a very important value when resolving integrity conflicts.
	 * 
	 * @throws SQLException
	 *             An error occurred when executing a SQL query.
	 */
	private void addLanguageColumn() throws SQLException {
		DotConnect dc = new DotConnect();
		if (DbConnectionFactory.isH2()) {
			dc.executeStatement(H2_QUERY);
		}
		if (DbConnectionFactory.isPostgres()) {
			dc.executeStatement(POSTGRESQL_QUERY);
		}
		if (DbConnectionFactory.isMySql()) {
			dc.executeStatement(MYSQL_QUERY);
		}
		if (DbConnectionFactory.isMsSql()) {
			dc.executeStatement(MSSQL_QUERY);
		}
		if (DbConnectionFactory.isOracle()) {
			dc.executeStatement(ORACLE_QUERY);
		}
	}

}
