package com.dotmarketing.startup.runonce;

import static org.junit.Assert.assertFalse;

import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import java.sql.SQLException;
import org.junit.BeforeClass;
import org.junit.Test;

public class Task05225RemoveLoadRecordsToIndexTest {

    @BeforeClass
    public static void prepare() throws Exception {
        // Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }

    @Test
    public void testUpgradeTaskShouldPass() throws DotDataException, SQLException {

        createLoadRecordsToIndexProcedure();
        final Task05225RemoveLoadRecordsToIndex task = new Task05225RemoveLoadRecordsToIndex();
        task.executeUpgrade();
        assertFalse(procedureExists());

    }

    private void createLoadRecordsToIndexProcedure() throws SQLException {
        String script = null;

        switch (DbConnectionFactory.getDBType()){
            case "MySQL":
                script = getMySQLScript();
                break;

            case "PostgreSQL":
                script = getPostgreSQLScript();
                break;

            case "Oracle":
                script = getOracleScript();
                break;

            case "Microsoft SQL Server":
                script = getSQLServerScript();
                break;
        }

        final DotConnect dotConnect = new DotConnect();
        dotConnect.executeStatement(script);

    }

    private String getOracleScript() {
        return "CREATE OR REPLACE FUNCTION load_records_to_index(server_id VARCHAR2, records_to_fetch NUMBER, priority_level NUMBER)\n"
                + "   RETURN types.ref_cursor IS\n"
                + " cursor_ret types.ref_cursor;\n"
                + " data_ret reindex_record_list;\n"
                + "BEGIN\n"
                + "  data_ret := reindex_record_list();\n"
                + "  FOR dj in (SELECT * FROM dist_reindex_journal\n"
                + "         WHERE serverid IS NULL AND priority <= priority_level AND rownum<=records_to_fetch\n"
                + "         ORDER BY priority ASC\n"
                + "         FOR UPDATE)\n"
                + "  LOOP\n"
                + "    UPDATE dist_reindex_journal SET serverid=server_id WHERE id=dj.id;\n"
                + "    data_ret.extend;\n"
                + "    data_ret(data_ret.Last) := reindex_record(dj.id,dj.inode_to_index,dj.ident_to_index,dj.priority,dj.dist_action);\n"
                + "  END LOOP;\n"
                + "  OPEN cursor_ret FOR\n"
                + "    SELECT * FROM TABLE(CAST(data_ret AS reindex_record_list));\n"
                + "  RETURN cursor_ret;\n"
                + "END;";
    }

    private String getPostgreSQLScript() {
        return "CREATE OR REPLACE FUNCTION load_records_to_index(server_id character varying, records_to_fetch int, priority_level int)\n"
                + "  RETURNS SETOF dist_reindex_journal AS'\n"
                + "DECLARE\n"
                + "   dj dist_reindex_journal;\n"
                + "BEGIN\n"
                + "\n"
                + "    FOR dj IN SELECT * FROM dist_reindex_journal\n"
                + "       WHERE serverid IS NULL\n"
                + "       AND priority <= priority_level\n"
                + "       ORDER BY priority ASC\n"
                + "       LIMIT records_to_fetch\n"
                + "       FOR UPDATE\n"
                + "    LOOP\n"
                + "        UPDATE dist_reindex_journal SET serverid=server_id WHERE id=dj.id;\n"
                + "        RETURN NEXT dj;\n"
                + "    END LOOP;\n"
                + "\n"
                + "END'"
                + "LANGUAGE plpgsql;";
    }

    private String getSQLServerScript() {
        return "CREATE PROCEDURE load_records_to_index(@server_id NVARCHAR(100), @records_to_fetch INT, @priority_level INT)\n"
                + "AS\n"
                + "BEGIN\n"
                + "WITH cte AS (\n"
                + "  SELECT TOP(@records_to_fetch) *\n"
                + "  FROM dist_reindex_journal WITH (ROWLOCK, READPAST, UPDLOCK)\n"
                + "  WHERE serverid IS NULL\n"
                + "  AND priority <= @priority_level\n"
                + "  ORDER BY priority ASC)\n"
                + "UPDATE cte\n"
                + "  SET serverid=@server_id\n"
                + "OUTPUT\n"
                + "  INSERTED.*\n"
                + "END;";
    }

    private String getMySQLScript() {
        return "CREATE PROCEDURE load_records_to_index(IN server_id VARCHAR(100), IN records_to_fetch INT, IN priority_level INT)\n"
                + "BEGIN\n"
                + "DECLARE v_id BIGINT;\n"
                + "DECLARE v_inode_to_index VARCHAR(100);\n"
                + "DECLARE v_ident_to_index VARCHAR(100);\n"
                + "DECLARE v_serverid VARCHAR(64);\n"
                + "DECLARE v_priority INT;\n"
                + "DECLARE v_time_entered TIMESTAMP;\n"
                + "DECLARE v_index_val VARCHAR(325);\n"
                + "DECLARE v_dist_action INT;\n"
                + "DECLARE cursor_end BOOL DEFAULT FALSE;\n"
                + "DECLARE cur1 CURSOR FOR SELECT * FROM dist_reindex_journal WHERE serverid IS NULL or serverid='' AND priority <= priority_level ORDER BY priority ASC LIMIT records_to_fetch;\n"
                + "DECLARE CONTINUE HANDLER FOR NOT FOUND SET cursor_end:=TRUE;\n"
                + "\n"
                + "DROP TEMPORARY TABLE IF EXISTS tmp_records_reindex;\n"
                + "CREATE TEMPORARY TABLE tmp_records_reindex (\n"
                + "  id BIGINT PRIMARY KEY,\n"
                + "  inode_to_index varchar(36),\n"
                + "  ident_to_index varchar(36),\n"
                + "  dist_action INT,\n"
                + "  priority INT\n"
                + ") ENGINE=MEMORY;\n"
                + "\n"
                + "OPEN cur1;\n"
                + "WHILE (NOT cursor_end) DO\n"
                + "  FETCH cur1 INTO v_id,v_inode_to_index,v_ident_to_index,v_serverid,v_priority,v_time_entered,v_index_val,v_dist_action;\n"
                + "  IF (NOT cursor_end) THEN\n"
                + "    UPDATE dist_reindex_journal SET serverid=server_id WHERE id=v_id;\n"
                + "    INSERT INTO tmp_records_reindex VALUES (v_id, v_inode_to_index, v_ident_to_index, v_dist_action, v_priority);\n"
                + "  END IF;\n"
                + "END WHILE;\n"
                + "CLOSE cur1;\n"
                + "\n"
                + "SELECT * FROM tmp_records_reindex;\n"
                + "END;";
    }

    private boolean procedureExists() throws DotDataException {
        String query = null;

        switch (DbConnectionFactory.getDBType()){
            case "MySQL":
                query = "SHOW PROCEDURE STATUS WHERE name = 'load_records_to_index';";
                break;

            case "PostgreSQL":
                query = "SELECT * FROM information_schema.routines where routine_name='load_records_to_index'";
                break;

            case "Oracle":
                query = "select * from dba_objects where object_type = 'FUNCTION' and  object_name='LOAD_RECORDS_TO_INDEX'";
                break;

            case "Microsoft SQL Server":
                query = "select * from sys.objects obj where obj.type in ('P', 'X') and obj.name = 'load_records_to_index'";
                break;
        }

        final DotConnect dc = new DotConnect();
        dc.setSQL(query);
        return dc.loadResults().size() > 0;
    }

}
