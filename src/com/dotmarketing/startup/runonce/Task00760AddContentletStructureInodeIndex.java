package com.dotmarketing.startup.runonce;

import java.util.List;

import com.dotmarketing.startup.*;

public class Task00760AddContentletStructureInodeIndex extends AbstractJDBCStartupTask {

	@Override
	public String getMSSQLScript() {
		return "CREATE INDEX idx_contentlet_4_st ON contentlet (structure_inode);";
	}

	@Override
	public String getMySQLScript() {
		return "CREATE INDEX idx_contentlet_4_st ON contentlet (structure_inode);";
	}

	@Override
	public String getOracleScript() {
		return "CREATE INDEX idx_contentlet_4_st ON contentlet (structure_inode);";
	}

	@Override
	public String getPostgresScript() {
		return "CREATE INDEX idx_contentlet_4_st ON contentlet (structure_inode);";
	}

	@Override
	protected List<String> getTablesToDropConstraints() {
		return null;
	}

	public boolean forceRun() {
		return true;
	}

}
