package com.dotmarketing.startup.runonce;

import com.dotmarketing.startup.AbstractJDBCStartupTask;
import java.util.List;

public class Task05205UpdateIndexNameLength extends AbstractJDBCStartupTask {

	@Override
	public String getMSSQLScript() {
		return "alter table indicies alter column index_name nvarchar(100);";
	}

	@Override
	public String getMySQLScript() {
		return "alter table indicies modify index_name varchar(100);";
	}

	@Override
	public String getOracleScript() {
		return "alter table indicies modify index_name varchar2(100);";
	}

	@Override
	public String getPostgresScript() {
		return "alter table indicies alter column index_name type varchar(100);";
	}

	@Override
	protected List<String> getTablesToDropConstraints() {
		return null;
	}

	public boolean forceRun() {
		return true;
	}

    @Override
    public String getH2Script() {
        return "alter table indicies alter column index_name type varchar(100);";
    }

}
