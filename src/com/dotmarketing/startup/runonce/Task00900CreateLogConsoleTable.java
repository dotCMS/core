package com.dotmarketing.startup.runonce;

import java.sql.SQLException;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.startup.StartupTask;

public class Task00900CreateLogConsoleTable implements StartupTask {
	
	private DotConnect dc;

	public boolean forceRun() {
		return true;
	}

	public void executeUpgrade() throws DotDataException, DotRuntimeException {
		try {
			DbConnectionFactory.getConnection().setAutoCommit(true);
			
			this.dc = new DotConnect();

			if (DbConnectionFactory.isOracle()) {
				createNewTablesOracle();
			} else if (DbConnectionFactory.isMsSql()) {
				createNewTablesSQLServer();
			} else if (DbConnectionFactory.isPostgres()) {
				createNewTablesPostgres();
			} else {
				createNewTablesMySQL();
			}

			APILocator.getWorkflowAPI().createDefaultScheme();

		} catch (Exception e) {
			throw new DotDataException(e.getMessage(), e);
		}
	}

	private void createNewTablesMySQL() throws DotDataException, SQLException {

		dc.executeStatement("create table log_mapper(\n" + 
							"log_name varchar(30) primary key,\n" + 
							"description varchar(50) not null,\n" + 
							"enabled bigint not null)");
		
		commonInsertStatement();

	}

	private void createNewTablesPostgres() throws DotDataException, SQLException {

		dc.executeStatement("create table log_mapper(\n" + 
							"log_name varchar(30) primary key,\n" + 
							"description varchar(50) not null,\n"+ 
							"enabled numeric(1,0) not null)");
		
		commonInsertStatement();

	}

	private void createNewTablesSQLServer() throws DotDataException, SQLException {

		dc.executeStatement("create table log_mapper(\n" + 
							"log_name varchar(30) primary key,\n" + 
							"description varchar(50) not null,\n" + 
							"enabled numeric(1,0) not null)");
		
		commonInsertStatement();

	}

	private void createNewTablesOracle() throws DotDataException, SQLException {
	
		dc.executeStatement("create table log_mapper(\n" + 
							"log_name nvarchar2(30) primary key,\n" + 
							"description nvarchar2(50) not null,\n" + 
							"enabled number(1,0) not null)");
		
		commonInsertStatement();
	}
	
	
	private void commonInsertStatement () throws DotDataException,SQLException{
		
		dc.executeStatement("insert into log_mapper (ENABLED,LOG_NAME,DESCRIPTION) " +
				"values ('1','dotcms-userActivity.log','Log Users action on pages, structures, documents.')");
		dc.executeStatement("insert into log_mapper (ENABLED,LOG_NAME,DESCRIPTION) " +
				"values ('1','dotcms-security.log','Log users login activity into dotCMS.')");
		dc.executeStatement("insert into log_mapper (ENABLED,LOG_NAME,DESCRIPTION) " +
				"values ('1','dotcms-adminaudit.log','Log Admin activity on dotCMS.')");
		
	}
	
}
