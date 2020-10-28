package com.dotmarketing.startup.runonce;

import com.dotmarketing.startup.AbstractJDBCStartupTask;
import java.util.List;

/**
 * Remove {@code load_records_to_index} stored procedure
 *
 * @author nollymar
 */
public class Task05225RemoveLoadRecordsToIndex extends AbstractJDBCStartupTask {

    private final String POSTGRES_SCRIPT = "DROP FUNCTION IF EXISTS load_records_to_index(server_id character varying, records_to_fetch int, priority_level int);";

    private final String MYSQL_SCRIPT = "DROP PROCEDURE IF EXISTS load_records_to_index;";

    private final String MSSQL_SCRIPT =
            "IF EXISTS (SELECT * FROM sys.objects WHERE type = 'P' AND name = 'load_records_to_index')\n"
                    + "DROP PROCEDURE load_records_to_index;";


    private final String ORACLE_SCRIPT = "begin\n"
            + "   execute immediate 'DROP FUNCTION load_records_to_index';\n"
            + "exception when others then\n"
            + "   if sqlcode != -4043 then\n"
            + "      raise;\n"
            + "   end if;\n"
            + "end;\n"
            + "/";

    @Override
    public boolean forceRun() {
        return true;
    }

    /**
     * The SQL for Postgres
     */
    @Override
    public String getPostgresScript() {
        return POSTGRES_SCRIPT;
    }

    /**
     * The SQL for MySQL
     */
    @Override
    public String getMySQLScript() {
        return MYSQL_SCRIPT;
    }

    /**
     * The SQL for Oracle
     */
    @Override
    public String getOracleScript() {
        return ORACLE_SCRIPT;
    }

    /**
     * The SQL for MSSQL
     */
    @Override
    public String getMSSQLScript() {
        return MSSQL_SCRIPT;
    }

    /**
     * The SQL for H2. The stored procedure to update is simulated by a Java class, so there's no
     * SQL for it in H2.
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
