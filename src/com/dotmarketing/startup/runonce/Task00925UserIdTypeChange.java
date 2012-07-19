package com.dotmarketing.startup.runonce;

import java.util.List;

import com.dotmarketing.startup.AbstractJDBCStartupTask;

public class Task00925UserIdTypeChange extends AbstractJDBCStartupTask {
    
    @Override
    public boolean forceRun() {
        return true;
    }
    
    @Override
    public String getPostgresScript() {
        return "alter table tag alter column user_id TYPE text;";
    }
    
    @Override
    public String getMySQLScript() {
    	return "alter table tag modify user_id varchar(9999);"+
               "alter table tag modify user_id longtext;";
    }
    
    @Override
    public String getOracleScript() {
        return "drop index tag_user_id_index;"+
        	   "alter table tag modify user_id long;"+
               "alter table tag modify user_id CLOB;"+
        	   "create index tag_user_id_index on tag(user_id) indextype is ctxsys.context;";
    }
    
    @Override
    public String getMSSQLScript() {
        return "drop index tag_user_id_index on tag;"+
        	   "alter table tag alter column user_id text;";
    }
    
    @Override
    protected List<String> getTablesToDropConstraints() {
        return null;
    }
    
}
