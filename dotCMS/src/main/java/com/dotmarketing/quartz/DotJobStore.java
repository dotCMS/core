package com.dotmarketing.quartz;

import com.dotcms.util.CloseUtils;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.microsoft.sqlserver.jdbc.ISQLServerConnection;
import org.quartz.*;
import org.quartz.impl.jdbcjobstore.JobStoreCMT;
import org.quartz.impl.jdbcjobstore.UpdateLockRowSemaphore;
import org.quartz.spi.ClassLoadHelper;
import org.quartz.spi.OperableTrigger;
import org.quartz.spi.SchedulerSignaler;
import org.quartz.utils.ConnectionProvider;
import org.quartz.utils.DBConnectionManager;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

public class DotJobStore extends JobStoreCMT {

	@Override
	public void storeJobAndTrigger(JobDetail newJob, OperableTrigger newTrigger) throws ObjectAlreadyExistsException, JobPersistenceException {
		super.storeJobAndTrigger(newJob, newTrigger);
	}

	public static final String TX_DATA_SOURCE_PREFIX = "TxDataSource.";
	public static final String NON_TX_DATA_SOURCE_PREFIX = "NonTxDataSource.";

	private DataSource dataSource;

	@Override
	public void initialize(ClassLoadHelper loadHelper, SchedulerSignaler signaler) throws SchedulerConfigException {
		this.dataSource = DbConnectionFactory.getDataSource();

		setDataSource(TX_DATA_SOURCE_PREFIX + getInstanceName());
		setDontSetAutoCommitFalse(true);

		String serverName = APILocator.getServerAPI().readServerId();
		if (!UtilMethods.isSet(serverName)) {
			serverName = "dotCMSServer";
		}
		setInstanceId(serverName);

		DBConnectionManager.getInstance().addConnectionProvider(
				TX_DATA_SOURCE_PREFIX + getInstanceName(),
				new ConnectionProvider() {
					@Override
					public Connection getConnection() throws SQLException {
						return dataSource.getConnection();
					}

					@Override
					public void shutdown() {
						// Do nothing - a Spring-managed DataSource has its own lifecycle.
					}

					@Override
					public void initialize() {
						// Initialization logic if needed
					}
				}
		);

		final DataSource nonTxDataSourceToUse = this.dataSource;

		setNonManagedTXDataSource(NON_TX_DATA_SOURCE_PREFIX + getInstanceName());

		DBConnectionManager.getInstance().addConnectionProvider(
				NON_TX_DATA_SOURCE_PREFIX + getInstanceName(),
				new ConnectionProvider() {
					@Override
					public Connection getConnection() throws SQLException {
						Connection c = nonTxDataSourceToUse.getConnection();
						if (ISQLServerConnection.TRANSACTION_READ_COMMITTED != c.getTransactionIsolation()) {
							c.setTransactionIsolation(ISQLServerConnection.TRANSACTION_READ_COMMITTED);
						}
						return c;
					}

					@Override
					public void shutdown() {
						// Do nothing - a Spring-managed DataSource has its own lifecycle.
					}

					@Override
					public void initialize() {
						// Initialization logic if needed
					}
				}
		);

		// Set lock handler
		if (DbConnectionFactory.isMySql()) {
			tablePrefix = tablePrefix.toLowerCase();
			setLockHandler(new DotSelectLockRowSemaphore(tablePrefix));
		} else if (DbConnectionFactory.isMsSql()) {
			setLockHandler(new DotSelectLockRowSemaphore(tablePrefix, "SELECT * FROM {0}LOCKS WITH (UPDLOCK ROWLOCK) WHERE LOCK_NAME = ?"));
		} else if (DbConnectionFactory.isOracle()) {
			UpdateLockRowSemaphore updateLockHandler = new UpdateLockRowSemaphore();
			updateLockHandler.setUpdateLockRowSQL("UPDATE {0}LOCKS SET LOCK_NAME = LOCK_NAME WHERE LOCK_NAME = ?");
			updateLockHandler.setTablePrefix(tablePrefix);
			setLockHandler(updateLockHandler);
		}

		// Set driver delegate class
		String driverClass = Config.getStringProperty("QUARTZ_DRIVER_CLASS", "");
		if (UtilMethods.isSet(driverClass) && driverClass.trim().length() > 1) {
			try {
				setDriverDelegateClass(driverClass);
			} catch (Exception e) {
				Logger.info(this, e.getMessage());
			}
		} else if (DbConnectionFactory.isMySql()) {
			try {
				setDriverDelegateClass("com.dotmarketing.quartz.MySQLJDBCDelegate");
			} catch (Exception e) {
				Logger.info(this, e.getMessage());
			}
		} else if (DbConnectionFactory.isPostgres()) {
			try {
				setDriverDelegateClass("org.quartz.impl.jdbcjobstore.PostgreSQLDelegate");
			} catch (Exception e) {
				Logger.info(this, e.getMessage());
			}
		} else if (DbConnectionFactory.isMsSql()) {
			try {
				setDriverDelegateClass("org.quartz.impl.jdbcjobstore.DotMSSQLDelegate");
			} catch (Exception e) {
				Logger.info(this, e.getMessage());
			}
		} else if (DbConnectionFactory.isOracle()) {
			try {
				setDriverDelegateClass("org.quartz.impl.jdbcjobstore.oracle.OracleDelegate");
			} catch (Exception e) {
				Logger.info(this, e.getMessage());
			}
		}

		super.initialize(loadHelper, signaler);
	}

	@Override
	protected void closeConnection(final Connection conn) {
		if (DbConnectionFactory.connectionExists()) {
			final Connection conn2 = DbConnectionFactory.getConnection();
			if (conn == conn2) {
				Logger.warnAndDebug(this.getClass(), new DotRuntimeException("Quartz using a dotCMS connection, should not be.  Closing"));
				DbConnectionFactory.closeSilently();
			}
		}
		CloseUtils.closeQuietly(conn);
	}
}