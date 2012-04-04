package com.dotmarketing.startup.runonce;

import java.util.List;

import com.dotmarketing.startup.AbstractJDBCStartupTask;

public class Task00825UpdateLoadRecordsToIndex extends AbstractJDBCStartupTask {
    
    public boolean forceRun() {
        return true;
    }
    
    @Override
    public String getPostgresScript() {
        return 
        "CREATE OR REPLACE FUNCTION load_records_to_index(server_id character varying, records_to_fetch int)\n"+
        "  RETURNS SETOF dist_reindex_journal AS'\n"+
        "  DECLARE\n"+ 
        "  dj dist_reindex_journal;\n"+
        "BEGIN\n"+
        "  FOR dj IN SELECT * FROM dist_reindex_journal\n"+ 
        "       WHERE serverid IS NULL\n"+ 
        "       ORDER BY priority ASC\n"+ 
        "       LIMIT records_to_fetch\n"+
        "       FOR UPDATE\n"+ 
        "  LOOP\n"+
        "    UPDATE dist_reindex_journal SET serverid=server_id WHERE id=dj.id;\n"+
        "    RETURN NEXT dj;\n"+
        "  END LOOP;\n"+
        "END'\n"+
        "LANGUAGE 'plpgsql';";
    }
    
    @Override
    public String getMySQLScript() {
        return  "alter table dist_reindex_journal modify serverid varchar(64) default null;\n"+
                "DROP PROCEDURE IF EXISTS load_records_to_index;\n"+
                "CREATE PROCEDURE load_records_to_index(IN server_id VARCHAR(100), IN records_to_fetch INT)\n"+
                "BEGIN\n"+
                "DECLARE v_id BIGINT;\n"+
                "DECLARE v_inode_to_index VARCHAR(100);\n"+
                "DECLARE v_ident_to_index VARCHAR(100);\n"+
                "DECLARE v_serverid VARCHAR(64);\n"+
                "DECLARE v_priority INT;\n"+
                "DECLARE v_time_entered TIMESTAMP;\n"+
                "DECLARE v_index_val VARCHAR(325);\n"+
                "DECLARE v_dist_action INT;\n"+
                "DECLARE cursor_end BOOL DEFAULT FALSE;\n"+
                "DECLARE cur1 CURSOR FOR SELECT * FROM dist_reindex_journal WHERE serverid IS NULL or serverid='' ORDER BY priority ASC LIMIT 10 FOR UPDATE;\n"+
                "DECLARE CONTINUE HANDLER FOR NOT FOUND SET cursor_end:=TRUE;\n"+
                "OPEN cur1;\n"+
                "WHILE (NOT cursor_end) DO\n"+
                "  FETCH cur1 INTO v_id,v_inode_to_index,v_ident_to_index,v_serverid,v_priority,v_time_entered,v_index_val,v_dist_action;\n"+
                "  IF (NOT cursor_end) THEN\n"+
                "    UPDATE dist_reindex_journal SET serverid=server_id WHERE id=v_id;\n"+
                "  END IF;\n"+
                "END WHILE;\n"+
                "CLOSE cur1;\n"+
                "\n"+
                "SELECT * FROM dist_reindex_journal WHERE serverid=server_id;\n"+
                "END\n"+
                "#\n";
    }
    
    @Override
    public String getOracleScript() {
        return "create or replace \n"+
               " PACKAGE types \n"+
               "AS\n"+
               "TYPE ref_cursor IS REF CURSOR;\n"+
               "END;\n"+
               "/\n"+
               "create or replace FUNCTION load_records_to_index(server_id VARCHAR2, records_to_fetch NUMBER)\n"+
               "   RETURN types.ref_cursor IS\n"+
               " cursor_ret types.ref_cursor;\n"+
               " cursor dj_cursor is\n"+
               "   SELECT * FROM dist_reindex_journal\n"+
               "   WHERE serverid IS NULL\n"+
               "   ORDER BY priority ASC\n"+
               "   FOR UPDATE;\n"+
               "   x number;\n"+
               "BEGIN\n"+
               "  x:=0;\n"+
               "  FOR dj in dj_cursor\n"+
               "  LOOP\n"+
               "    UPDATE dist_reindex_journal SET serverid=server_id WHERE id=dj.id;\n"+
               "    x:=x+1;\n"+
               "    EXIT WHEN x>=records_to_fetch;\n"+
               "  END LOOP;\n"+
               "  OPEN cursor_ret FOR\n"+ 
               "    select * from dist_reindex_journal where serverid=server_id;\n"+    
               "  RETURN cursor_ret;\n"+
               "END;\n"+
               "/\n"+
               "drop type dj_table_type;\n"+
               "drop type dj_table;\n";
    }
    
    @Override
    public String getMSSQLScript() {
        return  "ALTER TABLE dist_reindex_journal ALTER COLUMN serverid VARCHAR(64) NULL;\n"+
                "DROP FUNCTION load_records_to_index;\n" +
                "CREATE PROCEDURE load_records_to_index(@server_id VARCHAR, @records_to_fetch INT)\n"+
                "AS \n"+
                "BEGIN\n"+
                "WITH cte AS (\n"+
                "  SELECT TOP(@records_to_fetch) *\n"+
                "  FROM dist_reindex_journal WITH (ROWLOCK, READPAST, UPDLOCK)\n"+
                "  WHERE serverid IS NULL \n"+
                "  ORDER BY priority ASC)\n"+
                "UPDATE cte\n"+
                "  SET serverid=@server_id\n"+
                "OUTPUT\n"+
                "  INSERTED.*\n"+
                "END;\n";
    }
    
    @Override
    protected List<String> getTablesToDropConstraints() {
        return null;
    }
    
}
