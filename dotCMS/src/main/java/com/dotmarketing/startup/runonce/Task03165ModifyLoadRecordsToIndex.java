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

	private final String POSTGRES_SCRIPT = "CREATE OR REPLACE FUNCTION load_records_to_index(server_id character varying, records_to_fetch int, priority_level int) RETURNS SETOF dist_reindex_journal AS'\n"
						+ "DECLARE\n"
   							+ "dj dist_reindex_journal;\n"
						+ "BEGIN\n"
							+ "FOR dj IN SELECT * FROM dist_reindex_journal\n"
								+ "WHERE serverid IS NULL\n"
								+ "AND priority <= priority_level\n"
								+ "ORDER BY priority ASC\n"
								+ "LIMIT records_to_fetch\n"
								+ "FOR UPDATE\n"
							+ "LOOP\n"
								+ "UPDATE dist_reindex_journal SET serverid=server_id WHERE id=dj.id;\n"
								+ "RETURN NEXT dj;\n"
							+ "END LOOP;\n"
						+ "END'\n"
						+ "LANGUAGE 'plpgsql';\n";
	
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
	
	private final String MSSQL_SCRIPT = "IF EXISTS (SELECT * FROM sys.objects WHERE type = 'P' AND name = 'load_records_to_index')\n"
							+ "DROP PROCEDURE load_records_to_index;\n"
						+ "GO\n"
						+ "CREATE PROCEDURE load_records_to_index(@server_id VARCHAR(100), @records_to_fetch INT, @priority_level INT)\n"
						+ "AS\n"
						+ "BEGIN\n"
							+ "WITH cte AS (\n"
	  							+ "SELECT TOP(@records_to_fetch) *\n"
	  							+ "FROM dist_reindex_journal WITH (ROWLOCK, READPAST, UPDLOCK)\n"
	  							+ "WHERE serverid IS NULL\n"
	  							+ "AND priority <= @priority_level\n"
	  							+ "ORDER BY priority ASC)\n"
							+ "UPDATE cte\n"
	  							+ "SET serverid=@server_id\n"
							+ "OUTPUT\n"
	  							+ "INSERTED.*\n"
						+ "END;\n";
	
	private final String ORACLE_SCRIPT = "CREATE OR REPLACE FUNCTION load_records_to_index(server_id VARCHAR2, records_to_fetch NUMBER, priority_level NUMBER)\n"
								+ "RETURN types.ref_cursor IS\n"
							+ "cursor_ret types.ref_cursor;\n"
 							+ "data_ret reindex_record_list;\n"
						+ "BEGIN\n"
  							+ "data_ret := reindex_record_list();\n"
  							+ "FOR dj in (SELECT * FROM dist_reindex_journal\n"
         					+ "WHERE serverid IS NULL AND priority <= priority_level AND rownum<=records_to_fetch\n"
         					+ "ORDER BY priority ASC\n"
         					+ "FOR UPDATE)\n"
         					+ "LOOP\n"
         						+ "UPDATE dist_reindex_journal SET serverid=server_id WHERE id=dj.id;\n"
         						+ "data_ret.extend;\n"
         						+ "data_ret(data_ret.Last) := reindex_record(dj.id,dj.inode_to_index,dj.ident_to_index,dj.priority,dj.dist_action);\n"
         					+ "END LOOP;\n"
         					+ "OPEN cursor_ret FOR\n"
         						+ "SELECT * FROM TABLE(CAST(data_ret AS reindex_record_list));\n"
         					+ "RETURN cursor_ret;\n"
						+ "END;\n"
						+ "/\n";

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
	 * The SQL for H2. The stored procedure to update is simulated by a Java
	 * class, so there's no SQL for it in H2.
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
