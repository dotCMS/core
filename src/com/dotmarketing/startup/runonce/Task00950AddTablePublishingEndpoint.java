package com.dotmarketing.startup.runonce;

import java.math.BigDecimal;
import java.sql.SQLException;

import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.startup.StartupTask;

/**
 * Table creation for manage the end points.
 * 
 * @author Graziano Aliberti - Engineering Ingegneria Informatica S.p.a
 *
 * Oct 25, 2012 - 17:23:17 AM
 */
public class Task00950AddTablePublishingEndpoint implements StartupTask {
	
	// ********************************* BEGIN: ADD PUBLISHING_END_POINT
	
	private static final String PGVALIDATETABLESQL_PEP		=	"SELECT COUNT(table_name) as exist " +
																"FROM information_schema.tables " +
																"WHERE Table_Name = 'publishing_end_point'";
	
	private static final String PGCREATESQL_PEP				=	"CREATE TABLE publishing_end_point " +
																"(server_name varchar(1024) unique, " +
																"address varchar(250), " +
																"enabled bool, " +
																"auth_key varchar(1024), " +
																"sending bool)";
	
	private static final String MYCREATESQL_PEP				=	"CREATE TABLE IF NOT EXISTS publishing_end_point " +
																"(server_name varchar(1024), " +
																"address varchar(250), " +
																"enabled varchar(1) DEFAULT '0', " +
																"auth_key varchar(1024), " +
																"sending varchar(1) DEFAULT '0')";
	
	private static final String MSVALIDATETABLESQL_PEP		=	"SELECT COUNT(*) as exist " +
																"FROM sysobjects " +
																"WHERE name = 'publishing_end_point'";
	
	private static final String MSCREATESQL_PEP				=	"CREATE TABLE publishing_end_point " +
																"(server_name varchar(1024) unique, " +
																"address varchar(250), " +
																"enabled tinyint DEFAULT 0, " +
																"auth_key varchar(1024), " +
																"sending tinyint DEFAULT 0)";
	
	private static final String OCLVALIDATETABLESQL_PEP		=	"SELECT COUNT(*) as exist " +
																"FROM user_tables WHERE table_name='publishing_end_point'";
	
	private static final String OCLCREATESQL_PEP			=	"CREATE TABLE publishing_end_point " +
																"(server_name VARCHAR2(1024) unique, " +
																"address VARCHAR2(250), " +
																"enabled number(1,0) DEFAULT 0, " +
																"auth_key VARCHAR2(1024), " +
																"sending number(1,0) DEFAULT 0)";
	
	// ********************************* END: ADD PUBLISHING_END_POINT
	
	@Override
	public boolean forceRun() {
		return true;
	}
	
	/**
	 * Method for add the publishing_end_point table
	 * 
	 * Oct 16, 2012 - 4:47:05 PM
	 */
	private void addPublishingEndPoint() throws SQLException, DotDataException {		
		DotConnect dc=new DotConnect();
		if(DbConnectionFactory.isMsSql()) {
			dc.setSQL(MSVALIDATETABLESQL_PEP);
			int existTable = (Integer)dc.loadObjectResults().get(0).get("exist");
			if(existTable == 0){
				dc.setSQL(MSCREATESQL_PEP);
				dc.loadResult();
			}
		}else if(DbConnectionFactory.isOracle()) {
			dc.setSQL(OCLVALIDATETABLESQL_PEP);
			BigDecimal existTable = (BigDecimal)dc.loadObjectResults().get(0).get("exist");
			if(existTable.longValue() == 0){
				dc.setSQL(OCLCREATESQL_PEP);
				dc.loadResult();					
			}
		}else if(DbConnectionFactory.isMySql()) {
			dc.setSQL(MYCREATESQL_PEP);
			dc.loadResult();
		}else if(DbConnectionFactory.isPostgres()) {			
			dc.setSQL(PGVALIDATETABLESQL_PEP);
			long existTable = (Long)dc.loadObjectResults().get(0).get("exist");
			if(existTable == 0){
				dc.setSQL(PGCREATESQL_PEP);
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
			addPublishingEndPoint();
		} catch (SQLException e) {		
			throw new DotRuntimeException(e.getMessage(),e);
		}

	}

}
