package com.dotmarketing.quartz;

import com.dotcms.util.CloseUtils;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.microsoft.sqlserver.jdbc.ISQLServerConnection;
import com.rainerhahnekamp.sneakythrow.Sneaky;
import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.quartz.JobDetail;
import org.quartz.JobPersistenceException;
import org.quartz.ObjectAlreadyExistsException;
import org.quartz.SchedulerConfigException;
import org.quartz.StatefulJob;
import org.quartz.Trigger;
import org.quartz.core.SchedulingContext;
import org.quartz.impl.jdbcjobstore.JobStoreCMT;
import org.quartz.impl.jdbcjobstore.JobStoreSupport;
import org.quartz.impl.jdbcjobstore.UpdateLockRowSemaphore;
import org.quartz.spi.ClassLoadHelper;
import org.quartz.spi.SchedulerSignaler;
import org.quartz.utils.ConnectionProvider;
import org.quartz.utils.DBConnectionManager;

/**
 *
 * @author root
 * @since Mar 22nd, 2012
 */
public class DotJobStore extends JobStoreCMT {

	@Override
	public void storeJobAndTrigger(SchedulingContext ctxt, JobDetail newJob, Trigger newTrigger) throws ObjectAlreadyExistsException, JobPersistenceException {

        if (StatefulJob.class.isAssignableFrom(newJob.getJobClass())) {
            this.executeInLock(this.isLockOnInsert() ? "TRIGGER_ACCESS" : null, conn -> {
                if (newJob.isVolatile() && !newTrigger.isVolatile()) {
                    JobPersistenceException jpe = new JobPersistenceException(
                            "Cannot associate non-volatile trigger with a volatile job!");
                    jpe.setErrorCode(100);
                    throw jpe;
                } else {
                    storeJob(conn, ctxt, newJob, false);
                    storeTrigger(conn, ctxt, newTrigger, newJob, true, "WAITING", false, false);
                }
            });
        } else {
            super.storeJobAndTrigger(ctxt, newJob, newTrigger);
        }
	}

	public static final String TX_DATA_SOURCE_PREFIX = "TxDataSource.";

	public static final String NON_TX_DATA_SOURCE_PREFIX = "NonTxDataSource.";

	private DataSource dataSource;

	public void initialize(ClassLoadHelper loadHelper, SchedulerSignaler signaler)
	    throws SchedulerConfigException {

		this.dataSource = DbConnectionFactory.getDataSource();

		setDataSource(TX_DATA_SOURCE_PREFIX + getInstanceName());
		setDontSetAutoCommitFalse(true);

		String serverName = APILocator.getServerAPI().readServerId();
		if(!UtilMethods.isSet(serverName)){
			serverName = "dotCMSServer";
		}
		setInstanceId(serverName);

		DBConnectionManager.getInstance().addConnectionProvider(
				TX_DATA_SOURCE_PREFIX + getInstanceName(),
				new ConnectionProvider() {
					public Connection getConnection() throws SQLException {
						return dataSource.getConnection();
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
						if (ISQLServerConnection.TRANSACTION_READ_COMMITTED != c.getTransactionIsolation()) {
							c.setTransactionIsolation(ISQLServerConnection.TRANSACTION_READ_COMMITTED);
						}
						return  c;
					}
					public void shutdown() {
						// Do nothing - a Spring-managed DataSource has its own lifecycle.
					}
				}
		);


		//Set lock handler because of http://jira.opensymphony.com/browse/QUARTZ-497
		if (DbConnectionFactory.isMySql()) {
			tablePrefix = tablePrefix.toLowerCase();

			setLockHandler( new DotSelectLockRowSemaphore( tablePrefix ) );

		} else if (DbConnectionFactory.isMsSql()) {

			//https://github.com/dotCMS/core/tree/issue-10331-fix-snapshot-isolation-exception-on-quartz
			setLockHandler(
				new DotSelectLockRowSemaphore(tablePrefix,
					"SELECT * FROM {0}LOCKS WITH (UPDLOCK ROWLOCK) WHERE LOCK_NAME = ?"
				)
			);

		} else if (DbConnectionFactory.isOracle() || DbConnectionFactory.isH2()) {

			UpdateLockRowSemaphore updateLockHandler = new UpdateLockRowSemaphore();
			updateLockHandler.setUpdateLockRowSQL("UPDATE {0}LOCKS SET LOCK_NAME = LOCK_NAME WHERE LOCK_NAME = ?");
			updateLockHandler.setTablePrefix(tablePrefix);

			setLockHandler( updateLockHandler );
		}


		//Set driver delegate class because of http://jira.dotmarketing.net/browse/DOTCMS-6699
		String driverClass = Config.getStringProperty("QUARTZ_DRIVER_CLASS", "");
		if(UtilMethods.isSet(driverClass) && driverClass.trim().length()>1){
			try {
				setDriverDelegateClass(driverClass);
			} catch (Exception e) {
				Logger.info(this, e.getMessage());
			}
		}else if (DbConnectionFactory.isMySql()) {
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
		} else if (DbConnectionFactory.isH2()) {
            try {
                setDriverDelegateClass("org.quartz.impl.jdbcjobstore.StdJDBCDelegate");
            } catch (Exception e) {
                Logger.info(this, e.getMessage());
            }
		}

		super.initialize(loadHelper, signaler);
	}

	protected void closeConnection(final Connection conn) {
	    if(DbConnectionFactory.connectionExists()) {
	        final Connection conn2 = DbConnectionFactory.getConnection();
	        if(conn == conn2) {
	            Logger.warnAndDebug(this.getClass(), new DotRuntimeException("Quartz using a dotCMS connection, should not be.  Closing"));
	            DbConnectionFactory.closeSilently();
	        }
	    }
	    CloseUtils.closeQuietly(conn);

	}

}
