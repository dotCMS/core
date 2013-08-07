package com.dotmarketing.startup.runonce;

import java.sql.SQLException;

import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.startup.StartupTask;

public class Task01055CreatePushPublishEnvironmentTable implements StartupTask {

	private static final String POSTGRES_CREATE_QUEUE_TABLE = "CREATE TABLE publishing_queue "
			+ "(id bigserial PRIMARY KEY NOT NULL, "
			+ "operation int8, "
			+ "asset VARCHAR(2000) NOT NULL, "
			+ "language_id  int8 NOT NULL, "
			+ "entered_date TIMESTAMP, "
			+ "publish_date TIMESTAMP, "
			+ "type VARCHAR(256), "
			+ "bundle_id VARCHAR(256))";

	private static final String MYSQL_CREATE_QUEUE_TABLE = "CREATE TABLE publishing_queue "
			+ "(id BIGINT AUTO_INCREMENT PRIMARY KEY NOT NULL, "
			+ "operation bigint, "
			+ "asset VARCHAR(2000) NOT NULL, "
			+ "language_id bigint NOT NULL, "
			+ "entered_date DATETIME, "
			+ "publish_date DATETIME, "
			+ "type VARCHAR(256), "
			+ "bundle_id VARCHAR(256))";

	private static final String MSSQL_CREATE_QUEUE_TABLE = "CREATE TABLE publishing_queue "
			+ "(id bigint IDENTITY (1, 1)PRIMARY KEY NOT NULL, "
			+ "operation numeric(19,0), "
			+ "asset VARCHAR(2000) NOT NULL, "
			+ "language_id numeric(19,0) NOT NULL, "
			+ "entered_date DATETIME, "
			+ "publish_date DATETIME, "
			+ "type VARCHAR(256), "
			+ "bundle_id VARCHAR(256))";

	private static final String ORACLE_CREATE_QUEUE_TABLE	= "CREATE TABLE publishing_queue "
			+ "(id INTEGER PRIMARY KEY NOT NULL, "
			+ "operation number(19,0), "
			+ "asset VARCHAR2(2000) NOT NULL, "
			+ "language_id number(19,0) NOT NULL, "
			+ "entered_date TIMESTAMP, "
			+ "publish_date TIMESTAMP, "
			+ "type VARCHAR2(256), "
			+ "bundle_id VARCHAR2(256))";

	private static final String ORACLE_DROP_QUEUE_SEQ = "DROP SEQUENCE PUBLISHING_QUEUE_SEQ";

	private static final String ORACLE_CREATE_QUEUE_SEQ = "CREATE SEQUENCE PUBLISHING_QUEUE_SEQ START WITH 1 INCREMENT BY 1";

	private static final String ORACLE_CREATE_QUEUE_TRIG =	"CREATE OR REPLACE TRIGGER PUBLISHING_QUEUE_TRIGGER before insert on publishing_queue for each row begin select PUBLISHING_QUEUE_SEQ.nextval into :new.id from dual; end;";

	private void createEnvironmentTable(DotConnect dc) throws SQLException, DotDataException {
		if(DbConnectionFactory.isMsSql()) {
			dc.executeStatement("create table publishing_environment(id varchar(36) NOT NULL  primary key,name varchar(255) NOT NULL unique,push_to_all tinyint NOT NULL);");
		}else if(DbConnectionFactory.isOracle()) {
			dc.executeStatement("create table publishing_environment(id varchar2(36) NOT NULL  primary key,name varchar2(255) NOT NULL unique,push_to_all number(1,0) DEFAULT 0 NOT NULL)");
		}else if(DbConnectionFactory.isMySql()) {
			dc.executeStatement("create table publishing_environment(id varchar(36) NOT NULL  primary key,name varchar(255) NOT NULL unique,push_to_all bool NOT NULL);");
		}else if(DbConnectionFactory.isPostgres()) {
			dc.executeStatement("create table publishing_environment(id varchar(36) NOT NULL  primary key,name varchar(255) NOT NULL unique,push_to_all bool NOT NULL);");
		}
	}

	private void createBundleTable(DotConnect dc) throws SQLException, DotDataException {
		if(DbConnectionFactory.isMsSql()) {
			dc.executeStatement("create table publishing_bundle(id varchar(36) NOT NULL  primary key,name varchar(255) NOT NULL unique,publish_date DATETIME, expire_date DATETIME, owner varchar(100));");
		}else if(DbConnectionFactory.isOracle()) {
			dc.executeStatement("create table publishing_bundle(id varchar2(36) NOT NULL  primary key,name varchar2(255) NOT NULL unique,publish_date TIMESTAMP, expire_date TIMESTAMP, owner varchar2(100))");
		}else if(DbConnectionFactory.isMySql()) {
			dc.executeStatement("create table publishing_bundle(id varchar(36) NOT NULL  primary key,name varchar(255) NOT NULL unique,publish_date DATETIME, expire_date DATETIME, owner varchar(100));");
		}else if(DbConnectionFactory.isPostgres()) {
			dc.executeStatement("create table publishing_bundle(id varchar(36) NOT NULL  primary key,name varchar(255) NOT NULL unique,publish_date TIMESTAMP, expire_date TIMESTAMP,owner varchar(100));");
		}
	}

	private void createBundleEnvironmentTable(DotConnect dc) throws SQLException, DotDataException {

		if(DbConnectionFactory.isOracle()) {
			dc.executeStatement("create table publishing_bundle_environment(id varchar2(36) NOT NULL primary key,bundle_id varchar2(36) NOT NULL, environment_id varchar2(36) NOT NULL)");
		}else {
			dc.executeStatement("create table publishing_bundle_environment(id varchar(36) NOT NULL primary key,bundle_id varchar(36) NOT NULL, environment_id varchar(36) NOT NULL);");
		}

		dc.executeStatement("alter table publishing_bundle_environment add constraint FK_bundle_id foreign key (bundle_id) references publishing_bundle(id)");
		dc.executeStatement("alter table publishing_bundle_environment add constraint FK_environment_id foreign key (environment_id) references publishing_environment(id)");
	}

	private void recreatePublishingQueueTable(DotConnect dc) throws SQLException {

		dc.executeStatement("drop table publishing_queue");

		if(DbConnectionFactory.isMsSql()) {
			dc.executeStatement(MSSQL_CREATE_QUEUE_TABLE);
		}else if(DbConnectionFactory.isOracle()) {
			dc.executeStatement(ORACLE_DROP_QUEUE_SEQ);
			dc.executeStatement(ORACLE_CREATE_QUEUE_TABLE);
			dc.executeStatement(ORACLE_DROP_QUEUE_SEQ);
			dc.executeStatement(ORACLE_CREATE_QUEUE_SEQ);
			dc.executeStatement(ORACLE_CREATE_QUEUE_TRIG);
		}else if(DbConnectionFactory.isMySql()) {
			dc.executeStatement(MYSQL_CREATE_QUEUE_TABLE);
		}else if(DbConnectionFactory.isPostgres()) {
			dc.executeStatement(POSTGRES_CREATE_QUEUE_TABLE);
		}

	}

	@Override
	public void executeUpgrade() throws DotDataException, DotRuntimeException {
		try {
			DbConnectionFactory.getConnection().setAutoCommit(true);
		} catch (SQLException e) {
			throw new DotDataException(e.getMessage(), e);
		}
		try {
			DotConnect dc=new DotConnect();
			createEnvironmentTable(dc);
			recreatePublishingQueueTable(dc);
			createBundleTable(dc);
			createBundleEnvironmentTable(dc);
		} catch (SQLException e) {
			throw new DotRuntimeException(e.getMessage(),e);
		}

	}

	@Override
	public boolean forceRun() {
		return true;
	}

}
