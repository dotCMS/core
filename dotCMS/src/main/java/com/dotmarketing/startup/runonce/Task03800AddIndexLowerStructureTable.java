package com.dotmarketing.startup.runonce;

import com.dotmarketing.startup.AbstractJDBCStartupTask;

import java.util.List;

/**
 * Creates LOWER index in table: structure, column: velocity_var_name, databases: Postgres and Oracle.
 *
 * @author Oscar Arrieta
 * @version 3.8.0
 * @since Nov 30, 2016
 */
public class Task03800AddIndexLowerStructureTable extends AbstractJDBCStartupTask {

    @Override
    public boolean forceRun() {
        return true;
    }

    @Override
    public String getPostgresScript() {
        return "CREATE INDEX idx_lower_structure_name ON structure (LOWER(velocity_var_name));";
    }

    @Override
    public String getMySQLScript() {
        return null;
    }

    @Override
    public String getOracleScript() {
        return "CREATE INDEX idx_lower_structure_name ON structure (LOWER(velocity_var_name));";
    }

    @Override
    public String getMSSQLScript() {
        return null;
    }

    @Override
    protected List<String> getTablesToDropConstraints() {
        return null;
    }
}
