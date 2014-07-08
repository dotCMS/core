package com.dotmarketing.startup.runonce;

import com.dotmarketing.startup.AbstractJDBCStartupTask;

import java.util.List;

public class Task01095CreateIntegrityCheckerResultTables extends AbstractJDBCStartupTask {

	private String script = "create table folders_ir(folder varchar(255), local_inode varchar(36), remote_inode varchar(36), local_identifier varchar(36), remote_identifier varchar(36), endpoint_id varchar(36), PRIMARY KEY (local_inode, endpoint_id));\n"
			 + "create table structures_ir(velocity_name varchar(255), local_inode varchar(36), remote_inode varchar(36), endpoint_id varchar(36), PRIMARY KEY (local_inode, endpoint_id));\n"
			 + "create table schemes_ir(name varchar(255), local_inode varchar(36), remote_inode varchar(36), endpoint_id varchar(36), PRIMARY KEY (local_inode, endpoint_id));\n";

	@Override
	public boolean forceRun() {
		return true;
	}

	@Override
	public String getPostgresScript() {
		return script;

	}

	@Override
	public String getMySQLScript() {
		return script;
	}

	@Override
	public String getOracleScript() {
		return script.replace("varchar", "varchar2");
	}

	@Override
	public String getMSSQLScript() {
		return script;
	}

    @Override
    public String getH2Script () {
        return script;
    }


    @Override
	protected List<String> getTablesToDropConstraints() {
		// TODO Auto-generated method stub
		return null;
	}

}
