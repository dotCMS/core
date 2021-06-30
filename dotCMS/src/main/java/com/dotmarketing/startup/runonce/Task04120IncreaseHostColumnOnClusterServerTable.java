package com.dotmarketing.startup.runonce;


import java.util.List;

import com.dotmarketing.startup.AbstractJDBCStartupTask;


/**
 * Increase length of host column on cluster_server table
 * @author Jose Orsini
 *
 */
public class Task04120IncreaseHostColumnOnClusterServerTable extends AbstractJDBCStartupTask {

    @Override
    public boolean forceRun() {
        return true;
    }
    
    @Override
    public String getMSSQLScript() {
        return "ALTER TABLE cluster_server ALTER COLUMN host NVARCHAR(255);";
    }
    
    @Override
    public String getMySQLScript() {
        return "ALTER TABLE cluster_server MODIFY host varchar(255);";
    }

    @Override
    public String getOracleScript() {
        return "ALTER TABLE cluster_server MODIFY host varchar2(255);";
    }
    
    @Override
    public String getPostgresScript() {
        return "ALTER TABLE cluster_server ALTER COLUMN host TYPE varchar(255);";
    }

    @Override
    protected List<String> getTablesToDropConstraints() {
        return null;
    }

}