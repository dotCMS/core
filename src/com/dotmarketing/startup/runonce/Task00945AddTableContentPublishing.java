package com.dotmarketing.startup.runonce;

import java.math.BigDecimal;
import java.sql.SQLException;

import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.startup.StartupTask;

/**
 * Startup task for Content Publishing Framework
 * 
 * @author Graziano Aliberti - Engineering Ingegneria Informatica S.p.a
 *
 * Oct 5, 2012 - 11:14:40 AM
 */
public class Task00945AddTableContentPublishing implements StartupTask {
	
	// ********************************* BEGIN: ADD PUBLISHING_QUEUE
	
	private static final String PGVALIDATETABLESQL_PQ		=	"SELECT COUNT(table_name) as exist " +
																"FROM information_schema.tables " +
																"WHERE Table_Name = 'publishing_queue'";
	
	private static final String PGCREATESQL_PQ				=	"CREATE TABLE publishing_queue " +
																"(id bigserial PRIMARY KEY NOT NULL, " +
																"operation int8, asset VARCHAR(2000) NOT NULL, " +
																"language_id  int8 NOT NULL, entered_date TIMESTAMP, " +
																"last_try TIMESTAMP, num_of_tries int8 NOT NULL DEFAULT 0, " +
																"in_error bool DEFAULT 'f', last_results TEXT, " +
																"publish_date TIMESTAMP, server_id VARCHAR(256), " +
																"type VARCHAR(256), bundle_id VARCHAR(256), target text)";
	
	private static final String MYCREATESQL_PQ				=	"CREATE TABLE IF NOT EXISTS publishing_queue " +
																"(id BIGINT AUTO_INCREMENT PRIMARY KEY NOT NULL, " +
																"operation bigint, asset VARCHAR(2000) NOT NULL, " +
																"language_id bigint NOT NULL, entered_date DATETIME, " +
																"last_try DATETIME, num_of_tries bigint NOT NULL DEFAULT 0, " +
																"in_error varchar(1) DEFAULT '0', last_results LONGTEXT, " +
																"publish_date DATETIME, server_id VARCHAR(256), " +
																"type VARCHAR(256), bundle_id VARCHAR(256), target text)";
	
	private static final String MSVALIDATETABLESQL_PQ		=	"SELECT COUNT(*) as exist " +
																"FROM sysobjects " +
																"WHERE name = 'publishing_queue'";
	
	private static final String MSCREATESQL_PQ				=	"CREATE TABLE publishing_queue " +
																"(id bigint IDENTITY (1, 1)PRIMARY KEY NOT NULL, " +
																"operation numeric(19,0), asset VARCHAR(2000) NOT NULL, " +
																"language_id numeric(19,0) NOT NULL, entered_date DATETIME, " +
																"last_try DATETIME, num_of_tries numeric(19,0) NOT NULL DEFAULT 0, " +
																"in_error tinyint DEFAULT 0, last_results TEXT, " +
																"publish_date DATETIME, server_id VARCHAR(256), " +
																"type VARCHAR(256), bundle_id VARCHAR(256), target text)";
	
	private static final String OCLVALIDATETABLESQL_PQ		=	"SELECT COUNT(*) as exist " +
																"FROM user_tables WHERE table_name='publishing_queue'";
	
	private static final String OCLCREATESQL_PQ				=	"CREATE TABLE publishing_queue " +
																"(id INTEGER PRIMARY KEY NOT NULL, " +
																"operation number(19,0), asset VARCHAR2(2000) NOT NULL, " +
																"language_id number(19,0) NOT NULL, entered_date TIMESTAMP, " +
																"last_try TIMESTAMP, num_of_tries number(19,0) DEFAULT 0 NOT NULL, " +
																"in_error number(1,0) DEFAULT 0, last_results NCLOB, " +
																"publish_date TIMESTAMP, server_id VARCHAR2(256), " +
																"type VARCHAR2(256), bundle_id VARCHAR2(256), target nclob)";
	
	private static final String OCLCREATESEQSQL_PQ			=	"CREATE SEQUENCE PUBLISHING_QUEUE_SEQ START WITH 1 INCREMENT BY 1";
	
	private static final String OCLCREATETRIGERSQL_PQ		=	"CREATE OR REPLACE TRIGGER PUBLISHING_QUEUE_TRIGGER before insert on publishing_queue for each row begin select PUBLISHING_QUEUE_SEQ.nextval into :new.id from dual; end;";

	// ********************************* END: ADD publishing_queue
	
	// ********************************* BEGIN: ADD PUBLISHING_QUEUE_AUDIT
	
	private static final String PGVALIDATETABLESQL_PQA		=	"SELECT COUNT(table_name) as exist " +
																"FROM information_schema.tables " +
																"WHERE Table_Name = 'publishing_queue_audit'";

	private static final String PGCREATESQL_PQA				=	"CREATE TABLE publishing_queue_audit " +
																"(bundle_id VARCHAR(256) PRIMARY KEY NOT NULL, " +
																"status INTEGER, " +
																"status_pojo text, " +
																"status_updated TIMESTAMP, " +
																"create_date TIMESTAMP)";
	
	private static final String MYCREATESQL_PQA				=	"CREATE TABLE IF NOT EXISTS publishing_queue_audit " +
																"(bundle_id VARCHAR(256) PRIMARY KEY NOT NULL, " +
																"status INTEGER, " +
																"status_pojo text, " +
																"status_updated DATETIME, " +
																"create_date DATETIME)";
	
	private static final String MSVALIDATETABLESQL_PQA		=	"SELECT COUNT(*) as exist " +
																"FROM sysobjects " +
																"WHERE name = 'publishing_queue_audit'";
	
	private static final String MSCREATESQL_PQA				=	"CREATE TABLE publishing_queue_audit " +
																"(bundle_id VARCHAR(256) PRIMARY KEY NOT NULL, " +
																"status INTEGER, " +
																"status_pojo text, " +
																"status_updated DATETIME, " +
																"create_date DATETIME)";
	
	private static final String OCLVALIDATETABLESQL_PQA		=	"SELECT COUNT(*) as exist " +
																"FROM user_tables WHERE table_name='publishing_queue_audit'";
	
	private static final String OCLCREATESQL_PQA			=	"CREATE TABLE publishing_queue_audit " +
																"(bundle_id VARCHAR2(256) PRIMARY KEY NOT NULL, " +
																"status number(19,0), " +
																"status_pojo nclob, " +
																"status_updated TIMESTAMP, " +
																"create_date TIMESTAMP)";	
	
	// ********************************* END: ADD PUBLISHING_QUEUE_AUDIT
	
	@Override
	public boolean forceRun() {
		return true;
	}
	
	/**
	 * Method for add the publishing_queue
	 * 
	 * Oct 16, 2012 - 4:47:05 PM
	 */
	private void addPublishingQueueTable() throws SQLException, DotDataException {		
		DotConnect dc=new DotConnect();
		if(DbConnectionFactory.isMsSql()) {
			dc.setSQL(MSVALIDATETABLESQL_PQ);
			int existTable = (Integer)dc.loadObjectResults().get(0).get("exist");
			if(existTable == 0){
				dc.setSQL(MSCREATESQL_PQ);
				dc.loadResult();
			}
		}else if(DbConnectionFactory.isOracle()) {
			dc.setSQL(OCLVALIDATETABLESQL_PQ);
			BigDecimal existTable = (BigDecimal)dc.loadObjectResults().get(0).get("exist");
			if(existTable.longValue() == 0){
				dc.setSQL(OCLCREATESEQSQL_PQ);
				dc.loadResult();
				dc.setSQL(OCLCREATESQL_PQ);
				dc.loadResult();					
				dc.setSQL(OCLCREATETRIGERSQL_PQ);
				dc.loadResult();
			}
		}else if(DbConnectionFactory.isMySql()) {
			dc.setSQL(MYCREATESQL_PQ);
			dc.loadResult();
		}else if(DbConnectionFactory.isPostgres()) {			
			dc.setSQL(PGVALIDATETABLESQL_PQ);
			long existTable = (Long)dc.loadObjectResults().get(0).get("exist");
			if(existTable == 0){
				dc.setSQL(PGCREATESQL_PQ);
				dc.loadResult();	
			}
		}		
	}
	
	/**
	 * Method for add the publishing_queue_audit
	 * 
	 * Oct 16, 2012 - 4:47:05 PM
	 */
	private void addPublishingQueueAuditTable() throws SQLException, DotDataException {		
		DotConnect dc=new DotConnect();
		if(DbConnectionFactory.isMsSql()) {
			dc.setSQL(MSVALIDATETABLESQL_PQA);
			int existTable = (Integer)dc.loadObjectResults().get(0).get("exist");
			if(existTable == 0){
				dc.setSQL(MSCREATESQL_PQA);
				dc.loadResult();
			}
		}else if(DbConnectionFactory.isOracle()) {
			dc.setSQL(OCLVALIDATETABLESQL_PQA);
			BigDecimal existTable = (BigDecimal)dc.loadObjectResults().get(0).get("exist");
			if(existTable.longValue() == 0){
				dc.setSQL(OCLCREATESQL_PQA);
				dc.loadResult();					
			}
		}else if(DbConnectionFactory.isMySql()) {
			dc.setSQL(MYCREATESQL_PQA);
			dc.loadResult();
		}else if(DbConnectionFactory.isPostgres()) {			
			dc.setSQL(PGVALIDATETABLESQL_PQA);
			long existTable = (Long)dc.loadObjectResults().get(0).get("exist");
			if(existTable == 0){
				dc.setSQL(PGCREATESQL_PQA);
				dc.loadResult();	
			}
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
			addPublishingQueueTable();
			addPublishingQueueAuditTable();
		} catch (SQLException e) {		
			throw new DotRuntimeException(e.getMessage(),e);
		}

	}

}
