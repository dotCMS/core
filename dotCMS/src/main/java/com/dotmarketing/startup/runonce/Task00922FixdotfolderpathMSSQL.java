package com.dotmarketing.startup.runonce;

import java.util.List;

import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.startup.AbstractJDBCStartupTask;

public class Task00922FixdotfolderpathMSSQL extends AbstractJDBCStartupTask {
    
    @Override
    public boolean forceRun() {
        return DbConnectionFactory.isMsSql();
    }
    
    @Override
    public String getPostgresScript() {
        return null;
    }
    
    @Override
    public String getMySQLScript() {
        return null;
    }
    
    @Override
    public String getOracleScript() {
        return null;
    }
    
    @Override
    public String getMSSQLScript() {
        return "ALTER FUNCTION dbo.dotFolderPath(@parent_path varchar(36), @asset_name varchar(36)) \n"+
                "RETURNS varchar(36) \n"+
                "BEGIN \n"+
                "  IF(@parent_path='/System folder') \n"+
                "  BEGIN \n"+
                "    RETURN '/' \n"+
                "  END \n"+
                "  RETURN @parent_path+@asset_name+'/' \n"+
                "END;\n";
    }
    
    @Override
    protected List<String> getTablesToDropConstraints() {
        return null;
    }
    
}
