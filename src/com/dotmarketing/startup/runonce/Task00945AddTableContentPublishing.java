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
	

	private static final String PGVALIDATETABLESQL		=	"SELECT COUNT(table_name) as exist " +
															"FROM information_schema.tables " +
															"WHERE Table_Name = 'contentlet_publishing_queue'";
	
	private static final String PGCREATESQL				=	"CREATE TABLE publishing_queue " +
															"(id bigserial PRIMARY KEY NOT NULL, " +
															"operation int8, asset_identifier VARCHAR(36) NOT NULL, " +
															"language_id  int8 NOT NULL, entered_date TIMESTAMP, " +
															"last_try TIMESTAMP, num_of_tries int8 NOT NULL DEFAULT 0, " +
															"in_error bool DEFAULT 'f', last_results TEXT, " +
															"publish_date TIMESTAMP, server_id VARCHAR(256), " +
															"type VARCHAR(256), bundle_id VARCHAR(256))";
	
	private static final String MYCREATESQL				=	"CREATE TABLE IF NOT EXISTS publishing_queue " +
															"(id BIGINT AUTO_INCREMENT PRIMARY KEY NOT NULL, " +
															"operation bigint, asset_identifier VARCHAR(36) NOT NULL, " +
															"language_id bigint NOT NULL, entered_date DATETIME, " +
															"last_try DATETIME, num_of_tries bigint NOT NULL DEFAULT 0, " +
															"in_error varchar(1) DEFAULT '0', last_results LONGTEXT, " +
															"publish_date DATETIME, server_id VARCHAR(256), " +
															"type VARCHAR(256), bundle_id VARCHAR(256))";
	
	private static final String MSVALIDATETABLESQL		=	"SELECT COUNT(*) as exist " +
															"FROM sysobjects " +
															"WHERE name = 'publishing_queue'";
	
	private static final String MSCREATESQL				=	"CREATE TABLE publishing_queue " +
															"(id bigint IDENTITY (1, 1)PRIMARY KEY NOT NULL, " +
															"operation numeric(19,0), asset_identifier VARCHAR(36) NOT NULL, " +
															"language_id numeric(19,0) NOT NULL, entered_date DATETIME, " +
															"last_try DATETIME, num_of_tries numeric(19,0) NOT NULL DEFAULT 0, " +
															"in_error tinyint DEFAULT 0, last_results TEXT, " +
															"publish_date DATETIME, server_id VARCHAR(256), " +
															"type VARCHAR(256), bundle_id VARCHAR(256))";
	
	private static final String OCLVALIDATETABLESQL		=	"SELECT COUNT(*) as exist " +
															"FROM user_tables WHERE table_name='PUBLISHING_QUEUE'";
	
	private static final String OCLCREATESQL			=	"CREATE TABLE PUBLISHING_QUEUE " +
															"(id INTEGER NOT NULL, " +
															"operation number(19,0), asset_identifier VARCHAR2(36) NOT NULL, " +
															"language_id number(19,0) NOT NULL, entered_date DATE, " +
															"last_try DATE, num_of_tries number(19,0) DEFAULT 0 NOT NULL, " +
															"in_error number(1,0) DEFAULT 0, last_results NCLOB,PRIMARY KEY (id), " +
															"publish_date DATE, server_id VARCHAR2(256), " +
															"type VARCHAR2(256), bundle_id VARCHAR2(256))";
	
	private static final String OCLCREATESEQSQL			=	"CREATE SEQUENCE PUBLISHING_QUEUE_SEQ START WITH 1 INCREMENT BY 1";
	
	private static final String OCLCREATETRIGERSQL		=	"CREATE OR REPLACE TRIGGER PUBLISHING_QUEUE_TRIGGER before insert on CONTENTLET_PUBLISHING_QUEUE for each row begin select SOLR_QUEUE_SEQ.nextval into :new.id from dual; end;";
	
	@Override
	public boolean forceRun() {
		return true;
	}
	
	private void addTable() throws SQLException, DotDataException {		
		DotConnect dc=new DotConnect();
		if(DbConnectionFactory.isMsSql()) {
			dc.setSQL(MSVALIDATETABLESQL);
			int existTable = (Integer)dc.loadObjectResults().get(0).get("exist");
			if(existTable == 0){
				dc.setSQL(MSCREATESQL);
				dc.loadResult();
			}
		}else if(DbConnectionFactory.isOracle()) {
			dc.setSQL(OCLVALIDATETABLESQL);
			BigDecimal existTable = (BigDecimal)dc.loadObjectResults().get(0).get("exist");
			if(existTable.longValue() == 0){
				dc.setSQL(OCLCREATESEQSQL);
				dc.loadResult();
				dc.setSQL(OCLCREATESQL);
				dc.loadResult();					
				dc.setSQL(OCLCREATETRIGERSQL);
				dc.loadResult();
			}
		}else if(DbConnectionFactory.isMySql()) {
			dc.setSQL(MYCREATESQL);
			dc.loadResult();
		}else if(DbConnectionFactory.isPostgres()) {			
			dc.setSQL(PGVALIDATETABLESQL);
			long existTable = (Long)dc.loadObjectResults().get(0).get("exist");
			if(existTable == 0){
				dc.setSQL(PGCREATESQL);
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
			addTable();
		} catch (SQLException e) {		
			throw new DotRuntimeException(e.getMessage(),e);
		}

	}

}
