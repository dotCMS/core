package com.dotmarketing.startup.runonce;

import java.util.List;

import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.startup.AbstractJDBCStartupTask;

public class Task01310AddContentletIdentifierIndex extends AbstractJDBCStartupTask {
    	
	private static final String INDEX_SQL = 
	"CREATE INDEX idx_contentlet_identifier ON contentlet (identifier);\n";
	
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

    @Override
    public String getH2Script() {
        return null;
    }	
    
}
