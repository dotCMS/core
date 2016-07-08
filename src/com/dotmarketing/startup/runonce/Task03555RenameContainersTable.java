package com.dotmarketing.startup.runonce;

import com.dotmarketing.startup.AbstractJDBCStartupTask;

import java.util.List;

/**
 * @author Nollymar Longa
 * @since 07/08/2016
 */
public class Task03555RenameContainersTable extends AbstractJDBCStartupTask {

    @Override
    public boolean forceRun() {
        return true;
    }

    @Override
    public String getPostgresScript() {
        return "ALTER TABLE containers RENAME TO dot_containers";
    }

    @Override
    public String getMySQLScript() {
        return "RENAME TABLE containers TO dot_containers";
    }

    @Override
    public String getOracleScript() {
        return "ALTER TABLE containers RENAME TO dot_containers";
    }

    @Override
    public String getMSSQLScript() {
        return "exec sp_rename 'containers','dot_containers';";
    }

    @Override
    public String getH2Script() {
        return "ALTER TABLE containers RENAME TO dot_containers";
    }

    @Override
    protected List<String> getTablesToDropConstraints() {
        return null;
    }

}
