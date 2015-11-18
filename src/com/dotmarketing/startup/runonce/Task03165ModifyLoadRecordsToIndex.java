package com.dotmarketing.startup.runonce;

import java.util.List;

import com.dotmarketing.startup.AbstractJDBCStartupTask;

/**
 * Updates the existing {@code load_records_to_index} stored procedure to allow
 * filtering the re-index records by priority now.
 * 
 * @author Jose Castro
 * @version 3.3
 * @since 11-19-2015
 */
public class Task03165ModifyLoadRecordsToIndex extends AbstractJDBCStartupTask {

	private final String POSTGRES_SCRIPT = "";
	
	private final String MYSQL_SCRIPT = "DROP PROCEDURE IF EXISTS load_records_to_index;\n"
						+ "CREATE PROCEDURE load_records_to_index(IN server_id VARCHAR(100), IN records_to_fetch INT, IN priority_level INT)\n"
						+ "BEGIN\n"
						+ "DECLARE v_id BIGINT\n;"
						+ "DECLARE v_inode_to_index VARCHAR(100);\n"
						+ "DECLARE v_ident_to_index VARCHAR(100);\n"
						+ "DECLARE v_serverid VARCHAR(64);\n"
						+ "DECLARE v_priority INT;\n"
						+ "DECLARE v_time_entered TIMESTAMP;\n"
						+ "DECLARE v_index_val VARCHAR(325);\n"
						+ "DECLARE v_dist_action INT;\n"
						+ "DECLARE cursor_end BOOL DEFAULT FALSE;\n"
						+ "DECLARE cur1 CURSOR FOR SELECT * FROM dist_reindex_journal WHERE serverid IS NULL or serverid='' AND priority <= priority_level ORDER BY priority ASC LIMIT records_to_fetch FOR UPDATE;\n"
						+ "DECLARE CONTINUE HANDLER FOR NOT FOUND SET cursor_end:=TRUE;\n"
						+ "DROP TEMPORARY TABLE IF EXISTS tmp_records_reindex;\n"
						+ "CREATE TEMPORARY TABLE tmp_records_reindex (\n"
  							+ "id BIGINT PRIMARY KEY,\n"
  							+ "inode_to_index varchar(36),\n"
  							+ "ident_to_index varchar(36),\n"
  							+ "dist_action INT,\n"
  							+ "priority INT\n"
						+ ") ENGINE=MEMORY;\n"
						+ "OPEN cur1;\n"
						+ "WHILE (NOT cursor_end) DO\n"
  							+ "FETCH cur1 INTO v_id,v_inode_to_index,v_ident_to_index,v_serverid,v_priority,v_time_entered,v_index_val,v_dist_action;\n"
  							+ "IF (NOT cursor_end) THEN\n"
  								+ "UPDATE dist_reindex_journal SET serverid=server_id WHERE id=v_id;\n"
  								+ "INSERT INTO tmp_records_reindex VALUES (v_id, v_inode_to_index, v_ident_to_index, v_dist_action, v_priority);\n"
  							+ "END IF;\n"
						+ "END WHILE;\n"
						+ "CLOSE cur1;\n"
						+ "SELECT * FROM tmp_records_reindex;\n"
						+ "END;\n"
						+ "#\n";
	
	private final String MSSQL_SCRIPT = "";
	
	private final String ORACLE_SCRIPT = "";

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
		return POSTGRES_SCRIPT;
	}

	/**
	 * The SQL for MySQL
	 *
	 * @return
	 */
	@Override
	public String getMySQLScript() {
		return MYSQL_SCRIPT;
	}

	/**
	 * The SQL for Oracle
	 *
	 * @return
	 */
	@Override
	public String getOracleScript() {
		return ORACLE_SCRIPT;
	}

	/**
	 * The SQL for MSSQL
	 *
	 * @return
	 */
	@Override
	public String getMSSQLScript() {
		return MSSQL_SCRIPT;
	}

	/**
	 * The SQL for H2. The stored procedure to update is executed by a Java
	 * class, there's no SQL for it in H2.
	 *
	 * @return
	 */
	@Override
	public String getH2Script() {
		return "";
	}

	@Override
	protected List<String> getTablesToDropConstraints() {
		return null;
	}

}
