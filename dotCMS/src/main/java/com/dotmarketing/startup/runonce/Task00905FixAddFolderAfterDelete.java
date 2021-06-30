package com.dotmarketing.startup.runonce;

import java.util.List;

import com.dotmarketing.startup.AbstractJDBCStartupTask;

public class Task00905FixAddFolderAfterDelete extends AbstractJDBCStartupTask {
    
    @Override
    public boolean forceRun() {
        return true;
    }
    
    @Override
    public String getPostgresScript() {
        return "";
    }
    
    @Override
    public String getMySQLScript() {
        return "DROP TRIGGER IF EXISTS folder_identifier_check;\n"+
                "CREATE TRIGGER folder_identifier_check AFTER DELETE\n"+
                "on folder\n"+
                "FOR EACH ROW\n"+
                "BEGIN\n"+
                "DECLARE tableName VARCHAR(20);\n"+
                "DECLARE count INT;\n"+
                "SET tableName = 'folder';\n"+
                "CALL checkVersions(OLD.identifier,tableName,count);\n"+
                "IF(count = 0)THEN\n"+
                "delete from identifier where id = OLD.identifier;\n"+
                "END IF;\n"+
                "END\n"+                
                "#\n";
    }
    
    @Override
    public String getOracleScript() {
        return "";
        }
    
    @Override
    public String getMSSQLScript() {
        return "";
    }
    
    @Override
    protected List<String> getTablesToDropConstraints() {
        return null;
    }
}
