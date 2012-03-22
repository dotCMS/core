package com.dotmarketing.quartz;


import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import net.sourceforge.jtds.jdbc.ConnectionJDBC2;

import org.quartz.JobDetail;
import org.quartz.JobPersistenceException;
import org.quartz.ObjectAlreadyExistsException;
import org.quartz.SchedulerConfigException;
import org.quartz.Trigger;
import org.quartz.core.SchedulingContext;
import org.quartz.impl.jdbcjobstore.JobStoreCMT;
import org.quartz.impl.jdbcjobstore.UpdateLockRowSemaphore;
import org.quartz.spi.ClassLoadHelper;
import org.quartz.spi.SchedulerSignaler;
import org.quartz.utils.ConnectionProvider;
import org.quartz.utils.DBConnectionManager;
import org.springframework.jdbc.datasource.DataSourceUtils;

import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;

public class DotJobStore extends JobStoreCMT {
	@Override
	public void storeJobAndTrigger(SchedulingContext ctxt, JobDetail newJob, Trigger newTrigger) throws ObjectAlreadyExistsException, JobPersistenceException {
		super.storeJobAndTrigger(ctxt, newJob, newTrigger);
	}

	public static final String TX_DATA_SOURCE_PREFIX = "TxDataSource.";
	
	public static final String NON_TX_DATA_SOURCE_PREFIX = "NonTxDataSource.";
	
	private DataSource dataSource;
	
	public void initialize(ClassLoadHelper loadHelper, SchedulerSignaler signaler)
	    throws SchedulerConfigException {
		
		this.dataSource = DbConnectionFactory.getDataSource();
		
		setDataSource(TX_DATA_SOURCE_PREFIX + getInstanceName());
		setDontSetAutoCommitFalse(true);
		
		String serverName = Config.getStringProperty("DIST_INDEXATION_SERVER_ID");
		if(!UtilMethods.isSet(serverName)){
			serverName = "dotCMSServer";
		}
		setInstanceId(serverName);
		
		DBConnectionManager.getInstance().addConnectionProvider(
				TX_DATA_SOURCE_PREFIX + getInstanceName(),
				new ConnectionProvider() {
					public Connection getConnection() throws SQLException {
						return DataSourceUtils.doGetConnection(dataSource);
					}
					public void shutdown() {
						// Do nothing - a Spring-managed DataSource has its own lifecycle.
					}
				}
		);
		
		final DataSource nonTxDataSourceToUse = this.dataSource;
		
		setNonManagedTXDataSource(NON_TX_DATA_SOURCE_PREFIX + getInstanceName());
		
		DBConnectionManager.getInstance().addConnectionProvider(
				NON_TX_DATA_SOURCE_PREFIX + getInstanceName(),
				new ConnectionProvider() {
					public Connection getConnection() throws SQLException {
						Connection c = nonTxDataSourceToUse.getConnection();
						c.setTransactionIsolation(ConnectionJDBC2.TRANSACTION_READ_COMMITTED);
						return  c;
					}
					public void shutdown() {
						// Do nothing - a Spring-managed DataSource has its own lifecycle.
					}
				}
		);
		
		String dbType = DbConnectionFactory.getDBType();
		
		//This is done because http://jira.opensymphony.com/browse/QUARTZ-497
		UpdateLockRowSemaphore sem = new UpdateLockRowSemaphore();
		if (dbType.equals(DbConnectionFactory.MYSQL)) {
			tablePrefix = tablePrefix.toLowerCase();
		}else{
		  sem.setUpdateLockRowSQL("UPDATE {0}LOCKS SET LOCK_NAME = LOCK_NAME WHERE LOCK_NAME = ?");
		  sem.setTablePrefix(tablePrefix);
		}
		
		//http://jira.dotmarketing.net/browse/DOTCMS-6699
		String driverClass = Config.getStringProperty("QUARTZ_DRIVER_CLASS", "");
		if(UtilMethods.isSet(driverClass) && driverClass.trim().length()>1){
			try {
				setDriverDelegateClass(driverClass);
			} catch (Exception e) {
				Logger.info(this, e.getMessage());
			}
		}else if (dbType.equals(DbConnectionFactory.MYSQL)) {
			try {
				setDriverDelegateClass("com.dotmarketing.quartz.MySQLJDBCDelegate");
				MySQLLockSemaphore mySQLSem = new MySQLLockSemaphore(tablePrefix);
				setLockHandler(mySQLSem);
			} catch (Exception e) {
				Logger.info(this, e.getMessage());
			}
		} else if (dbType.equals(DbConnectionFactory.POSTGRESQL)) {
			try {
				setDriverDelegateClass("org.quartz.impl.jdbcjobstore.PostgreSQLDelegate");
			} catch (Exception e) {
				Logger.info(this, e.getMessage());
			}
		} else if (dbType.equals(DbConnectionFactory.MSSQL)) {
			try {
				setDriverDelegateClass("org.quartz.impl.jdbcjobstore.DotMSSQLDelegate");
				setLockHandler(sem);
			} catch (Exception e) {
				Logger.info(this, e.getMessage());
			}
		} else if (dbType.equals(DbConnectionFactory.ORACLE)) {
			try {
				setDriverDelegateClass("org.quartz.impl.jdbcjobstore.oracle.OracleDelegate");
				setLockHandler(sem);
			} catch (Exception e) {
				Logger.info(this, e.getMessage());
			}
		}
		
		super.initialize(loadHelper, signaler);
	}
	
	protected void closeConnection(Connection con) {
		DataSourceUtils.releaseConnection(con, this.dataSource);
	}
	
	
	


}