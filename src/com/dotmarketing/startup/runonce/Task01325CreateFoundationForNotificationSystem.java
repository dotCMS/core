package com.dotmarketing.startup.runonce;

import java.util.List;

import com.dotmarketing.startup.AbstractJDBCStartupTask;

public class Task01325CreateFoundationForNotificationSystem extends AbstractJDBCStartupTask {

	@Override
	public boolean forceRun() {
		return true;
	}

	@Override
	public String getPostgresScript() {
		return "create table notification(id varchar(36) NOT NULL,message text NOT NULL, notification_type varchar(100), notification_level varchar(100), user_id varchar(255) NOT NULL, time_sent TIMESTAMP NOT NULL, was_read bool default false, PRIMARY KEY (id));\n"
				+ "create index idx_not_user ON notification (user_id);"
		 		+ "create index idx_not_read ON notification (was_read);";
	}

	@Override
	public String getMySQLScript() {
		return "create table notification(id varchar(36) NOT NULL,message text NOT NULL, notification_type varchar(100), notification_level varchar(100), user_id varchar(255) NOT NULL, time_sent DATETIME NOT NULL, was_read bit default 0, PRIMARY KEY (id));\n"
				+ "create index idx_not_user ON notification (user_id);"
				+ "create index idx_not_read ON notification (was_read);";
	}

	@Override
	public String getOracleScript() {
		return "create table notification(id varchar2(36) NOT NULL,message nclob NOT NULL, notification_type varchar2(100), notification_level varchar2(100), user_id varchar2(255) NOT NULL, time_sent TIMESTAMP NOT NULL, was_read number(1,0) default 0, PRIMARY KEY (id));\n"
				+ "create index idx_not_user ON notification (user_id);"
				+ "create index idx_not_read ON notification (was_read);";
	}

	@Override
	public String getMSSQLScript() {
		return "create table notification(id varchar(36) NOT NULL,message text NOT NULL, notification_type varchar(100), notification_level varchar(100), user_id varchar2(255) NOT NULL, time_sent DATETIME NOT NULL, was_read tinyint default 0, PRIMARY KEY (id));\n"
				+ "create index idx_not_user ON notification (user_id);"
				+ "create index idx_not_read ON notification (was_read);";
	}

	@Override
	public String getH2Script() {
		return "create table notification(id varchar(36) NOT NULL,message text NOT NULL, notification_type varchar(100), notification_level varchar(100), user_id varchar(255) NOT NULL, time_sent TIMESTAMP NOT NULL, was_read bit default 0, PRIMARY KEY (id));\n"
				+ "create index idx_not_user ON notification (user_id);"
				+ "create index idx_not_read ON notification (was_read);";
	}

	@Override
	protected List<String> getTablesToDropConstraints() {
		// TODO Auto-generated method stub
		return null;
	}

}
