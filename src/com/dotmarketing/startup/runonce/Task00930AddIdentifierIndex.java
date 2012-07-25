package com.dotmarketing.startup.runonce;

import java.util.List;

import com.dotmarketing.startup.AbstractJDBCStartupTask;

public class Task00930AddIdentifierIndex extends AbstractJDBCStartupTask {
    
    private static final String INDEX_SQL = 
    "create index idx_identifier_perm on identifier (asset_type,host_inode);\n";

    @Override
    public boolean forceRun() {
        return true;
    }
    
    @Override
    public String getPostgresScript() {
        return INDEX_SQL;
    }
    
    @Override
    public String getMySQLScript() {
        return INDEX_SQL;
    }
    
    @Override
    public String getOracleScript() {
        return INDEX_SQL;
    }
    
    @Override
    public String getMSSQLScript() {
        return INDEX_SQL;
    }
    
    @Override
    protected List<String> getTablesToDropConstraints() {
        return null;
    }
    
}
