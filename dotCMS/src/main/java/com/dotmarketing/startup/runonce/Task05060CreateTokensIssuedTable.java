package com.dotmarketing.startup.runonce;

import java.util.List;

import com.dotmarketing.startup.AbstractJDBCStartupTask;

public class Task05060CreateTokensIssuedTable extends AbstractJDBCStartupTask {

	@Override
	public boolean forceRun() {
		return true;
	}

	
	final static String BASE_SCRIPT = "create table jwt_token_issued("
	        + "token_id varchar(255) NOT NULL, "
	        + "token_userid varchar(255) NOT NULL, "
	        + "issue_date TIMESTAMP NOT NULL, "
	        + "expire_date TIMESTAMP NOT NULL, "
	        + "requested_by_userid  varchar(255) NOT NULL, "
	        + "requested_by_ip  varchar(255) NOT NULL, "
	        + "revoke_date TIMESTAMP NULL DEFAULT NULL, "
	        + "allowed_from  varchar(255) , "
	        + "cluster_id  varchar(255) , "
	        + "meta_data  text , "
	        + "mod_date  TIMESTAMP NOT NULL, "
	        + "PRIMARY KEY (token_id));\n"
	        + "create index idx_jwt_tokend_issue_user ON jwt_token_issued (token_userid);";
	
	
	
	
	@Override
	public String getPostgresScript() {
		return BASE_SCRIPT;

	}

	@Override
	public String getMySQLScript() {
	    return BASE_SCRIPT;
	}

	@Override
	public String getOracleScript() {
	    return BASE_SCRIPT.replaceAll(" text", " nclob");
	}

	@Override
	public String getMSSQLScript() {
        return BASE_SCRIPT;
	}

	@Override
	public String getH2Script() {
        return BASE_SCRIPT;
	}

	@Override
	protected List<String> getTablesToDropConstraints() {
		// TODO Auto-generated method stub
		return null;
	}

}
