package com.dotmarketing.startup.runonce;

import java.util.List;

import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.startup.AbstractJDBCStartupTask;

public class Task01045FixUpgradeTriggerVarLength extends AbstractJDBCStartupTask {
    
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
        return "";
    }
    
    @Override
    public String getOracleScript() {
    	String parentPathCheckTrigger = 
                "CREATE OR REPLACE TRIGGER identifier_parent_path_check\n " +
				" AFTER INSERT OR UPDATE ON identifier\n " +
				" DECLARE\n " +
				"   rowcount varchar2(100);\n " +
				"   assetIdentifier varchar2(100);\n " +
				"   parentPath varchar2(255);\n " +
				"   hostInode varchar2(100);\n " +
				" BEGIN\n " +
				"    for i in 1 .. check_parent_path_pkg.newRows.count LOOP\n " +
				"       select id,parent_path,host_inode into assetIdentifier,parentPath,hostInode from identifier where rowid = check_parent_path_pkg.newRows(i);\n " +
				"       IF(parentPath='/' OR parentPath='/System folder') THEN\n " +
				"         return;\n " +
				"       ELSE\n " +
				"         select count(*) into rowcount from identifier where asset_type='folder' and host_inode = hostInode and parent_path||asset_name||'/' = parentPath and id <> assetIdentifier;\n " +
				"         IF (rowcount = 0) THEN    \n " +
				"            RAISE_APPLICATION_ERROR(-20000, 'Cannot insert/update for this path does not exist for the given host');   \n " +
				"         END IF;   \n " +
				"       END IF;\n " +
				" END LOOP;\n " +
				" END;\n " +
					"/\n";
        return parentPathCheckTrigger;
    }
    
    @Override
    public String getMSSQLScript() {
    	return "";
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