package com.dotmarketing.startup.runonce;

import java.util.List;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.startup.AbstractJDBCStartupTask;

public class Task01070FixContainerTriggerInfo extends AbstractJDBCStartupTask {

    @Override
    public boolean forceRun() {
        return DbConnectionFactory.isMySql();
    }
    
    @Override
    public String getPostgresScript() {
        return "";
    }
    
    @Override
    public String getMySQLScript() {
    	String parentPathCheckWhenUpdate = " DROP TRIGGER IF EXISTS check_container_versions;\n"+
    			" CREATE TRIGGER check_container_versions BEFORE DELETE \n"+
    			"	on containers \n"+
    			" FOR EACH ROW \n"+
    			" BEGIN \n"+
    			" DECLARE tableName VARCHAR(20); \n"+
    			" DECLARE count INT; \n"+
    			" SET tableName = 'containers'; \n"+
    			" CALL checkVersions(OLD.identifier,tableName,count); \n"+
    			" IF(count = 0)THEN \n"+
    			" 	delete from identifier where id = OLD.identifier; \n"+
    			" END IF; \n"+
    			" END \n"+
    			"#\n";

        return parentPathCheckWhenUpdate;
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

