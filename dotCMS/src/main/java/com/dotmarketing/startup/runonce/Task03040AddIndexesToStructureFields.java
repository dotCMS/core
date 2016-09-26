package com.dotmarketing.startup.runonce;

import java.util.List;

import com.dotmarketing.startup.AbstractJDBCStartupTask;

public class Task03040AddIndexesToStructureFields extends AbstractJDBCStartupTask {

	private static final String CREATE_STRUCTURE_INDEXES_SQL = 
			"create index idx_structure_host on structure (host);\n"+
	        "create index idx_structure_folder on structure (folder);\n";
	@Override
	public boolean forceRun() {
		return true;
	}

	@Override
	public String getPostgresScript() {
		return CREATE_STRUCTURE_INDEXES_SQL;
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
		return null;
	}

	@Override
	public String getH2Script() {
		return null;
	}

	@Override
	protected List<String> getTablesToDropConstraints() {
		return null;
	}
	

}
