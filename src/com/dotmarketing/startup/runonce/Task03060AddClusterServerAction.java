/**
 * 
 */
package com.dotmarketing.startup.runonce;

import java.util.List;

import com.dotmarketing.startup.AbstractJDBCStartupTask;

/**
 * @author Oscar Arrieta.
 * 
 * This class will cehck if cluster_server_action already exists in the databse.
 * If the table doesn't exist this task will created it.
 *
 */
public class Task03060AddClusterServerAction extends AbstractJDBCStartupTask {

	/* (non-Javadoc)
	 * @see com.dotmarketing.startup.StartupTask#forceRun()
	 */
	@Override
	public boolean forceRun() {
		return true;
	}

	/* (non-Javadoc)
	 * @see com.dotmarketing.startup.AbstractJDBCStartupTask#getPostgresScript()
	 */
	@Override
	public String getPostgresScript() {
		return "create table cluster_server_action(server_action_id varchar(36) not null, originator_id varchar(36) not null, server_id varchar(36) not null,"
				+ " failed bool, response varchar(2048), action_id varchar(1024) not null,completed bool, entered_date timestamp not null,"
				+ "time_out_seconds bigint not null,PRIMARY KEY (server_action_id))";
	}

	/* (non-Javadoc)
	 * @see com.dotmarketing.startup.AbstractJDBCStartupTask#getMySQLScript()
	 */
	@Override
	public String getMySQLScript() {
		return "create table cluster_server_action(server_action_id varchar(36) not null, originator_id varchar(36) not null, server_id varchar(36) not null,"
				+ " failed boolean default false, response varchar(2048), action_id varchar(1024) not null,completed boolean default false, entered_date datetime not null,"
				+ "time_out_seconds bigint not null,PRIMARY KEY (server_action_id))";
	}

	/* (non-Javadoc)
	 * @see com.dotmarketing.startup.AbstractJDBCStartupTask#getOracleScript()
	 */
	@Override
	public String getOracleScript() {
		return "create table cluster_server_action(server_action_id varchar2(36) not null, originator_id varchar2(36) not null, server_id varchar2(36) not null,"
				+ " failed number(1, 0), response varchar2(2048), action_id varchar2(1024) not null,completed number(1, 0), entered_date date not null,"
				+ "time_out_seconds number(13) not null,PRIMARY KEY (server_action_id))";
	}

	/* (non-Javadoc)
	 * @see com.dotmarketing.startup.AbstractJDBCStartupTask#getMSSQLScript()
	 */
	@Override
	public String getMSSQLScript() {
		return "create table cluster_server_action(server_action_id varchar(36) not null, originator_id varchar(36) not null, server_id varchar(36) not null, "
				+ "failed bit not null, response varchar(2048), action_id varchar(1024) not null,completed bit not null, entered_date datetime not null,"
				+ "time_out_seconds bigint not null,PRIMARY KEY (server_action_id))";
	}

	/* (non-Javadoc)
	 * @see com.dotmarketing.startup.AbstractJDBCStartupTask#getH2Script()
	 */
	@Override
	public String getH2Script() {
		return "create table cluster_server_action(server_action_id varchar(36) not null, originator_id varchar(36) not null, server_id varchar(36) not null, "
				+ "failed boolean default false, response varchar(2048), action_id varchar(1024) not null,completed boolean default false,"
				+ " entered_date timestamp not null,time_out_seconds bigint not null,PRIMARY KEY (server_action_id))";
	}

	/* (non-Javadoc)
	 * @see com.dotmarketing.startup.AbstractJDBCStartupTask#getTablesToDropConstraints()
	 */
	@Override
	protected List<String> getTablesToDropConstraints() {
		return null;
	}

}
