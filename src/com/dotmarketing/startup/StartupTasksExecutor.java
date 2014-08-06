package com.dotmarketing.startup;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Calendar;
import java.util.Comparator;
import java.util.List;

import com.dotmarketing.common.reindex.ReindexThread;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.TaskLocatorUtil;

import com.dotcms.repackage.edu.emory.mathcs.backport.java.util.Collections;

public class StartupTasksExecutor {

	
	private static StartupTasksExecutor executor;

	private String pgLock = "lock table db_version;";
	private String myLock = "lock table db_version write;";
	private String oraLock = "LOCK TABLE DB_VERSION IN EXCLUSIVE MODE";
	private String msLock = "SELECT * FROM db_version WITH (XLOCK)";
	private String h2Lock = "SELECT * FROM db_version FOR UPDATE";

	private String pgCommit = "commit;";
	private String myCommit = "unlock tables";
	private String oraCommit = "COMMIT";
	private String msCommit = "COMMIT";
	private String h2Commit = "COMMIT";

	private String pgCreate = "CREATE TABLE db_version (db_version integer NOT NULL, date_update timestamp with time zone NOT NULL, CONSTRAINT db_version_pkey PRIMARY KEY (db_version));";
	private String myCreate = "CREATE TABLE `db_version` (`db_version` INTEGER UNSIGNED NOT NULL,`date_update` DATETIME NOT NULL, PRIMARY KEY (`db_version`))";
	private String oraCreate = "CREATE TABLE \"DB_VERSION\" ( \"DB_VERSION\" INTEGER NOT NULL , \"DATE_UPDATE\" TIMESTAMP NOT NULL, PRIMARY KEY (\"DB_VERSION\") )";
	private String msCreate ="CREATE TABLE db_version (	db_version int NOT NULL , date_update datetime NOT NULL, PRIMARY KEY (db_version) )";
	private String h2Create = "CREATE TABLE db_version (db_version integer NOT NULL, date_update timestamp NOT NULL, CONSTRAINT db_version_pkey PRIMARY KEY (db_version))";
	
	private String pgSelect = "SELECT max(db_version) AS db_version FROM db_version";
	private String mySelect = "SELECT max(db_version) AS db_version FROM db_version";
	private String oraSelect = "SELECT max(db_version) AS db_version FROM db_version";
	private String msSelect = "SELECT max(db_version) AS db_version FROM db_version";
	private String h2Select = "SELECT max(db_version) AS db_version FROM db_version";

//	private String pgSelect = "SELECT * FROM db_version ORDER BY db_version DESC LIMIT 1;";
//	private String mySelect = "SELECT * FROM db_version ORDER BY db_version DESC LIMIT 1;";
//	private String oraSelect = "SELECT * FROM db_version WHERE rownum<=1 ORDER BY db_version DESC";
	
	private String lock;
	private String commit;
	private String create;
	private String select;


	
	
	private StartupTasksExecutor() {

	}

	public static StartupTasksExecutor getInstance() {
		if (executor == null)
			executor = new StartupTasksExecutor();
		return executor;
	}

	/**
	 * Check which database we're using, and select the apropiate SQL. In a
	 * different method to avoid further clutter
	 */
	private void setupSQL() {
		if (DbConnectionFactory.isPostgres()) {
			lock = pgLock;
			commit = pgCommit;
			create = pgCreate;
			select = pgSelect;
		}
		else if (DbConnectionFactory.isMySql()) {
			lock = myLock.toLowerCase();
			commit = myCommit.toLowerCase();
			create = myCreate.toLowerCase();
			select = mySelect.toLowerCase();
		}
		
		else if (DbConnectionFactory.isOracle()) {
			lock = oraLock;
			commit = oraCommit;
			create = oraCreate;
			select = oraSelect;
		}
		
		else if (DbConnectionFactory.isMsSql()) {
			lock = msLock;
			commit = msCommit;
			create = msCreate;
			select = msSelect;
		}
		
		else if(DbConnectionFactory.isH2()) {
		    lock = h2Lock;
		    commit = h2Commit;
		    create = h2Create;
		    select = h2Select;
		}

	}

	public void executeUpgrades(String appLocation) throws DotDataException, DotRuntimeException {

		List<Class<?>> runOnce;
		List<Class<?>> runAlways;
		Comparator<Class<?>> comparator = new Comparator<Class<?>>() {
			public int compare(Class<?> o1, Class<?> o2) {
				return o1.getName().compareTo(o2.getName());
			}
		};
		try {
			runOnce = TaskLocatorUtil.getStartupRunOnceTaskClasses();
			runAlways = TaskLocatorUtil.getStartupRunAlwaysTaskClasses();
		} catch (Exception e) {
			throw new DotRuntimeException(e.getMessage(), e);
		}
		Collections.sort(runOnce, comparator);
		Collections.sort(runAlways, comparator);
		Logger.debug(this.getClass(), "Locking db_version table");
		setupSQL();
		//Integer currentVersion = null;
		PreparedStatement update = null;
		Statement s = null;
		ResultSet rs = null;
		Connection conn = null;
		try {
			conn = DbConnectionFactory.getDataSource().getConnection();

			conn.setAutoCommit(false);
			s = conn.createStatement();
			update = conn
					.prepareStatement("INSERT INTO db_version (db_version,date_update) VALUES (?,?)");
			s.execute(lock);
			rs = s
					.executeQuery(select);
			rs.next();
			Config.DB_VERSION = rs.getInt("db_version");
		} catch (SQLException e) {
			// Maybe the table doesn't exist?
			Logger.debug(this.getClass(), "Trying to create db_version table");
			try {
				conn.rollback();
				if(DbConnectionFactory.isMySql()){
					s.execute("SET storage_engine=INNODB");
				}
				s.execute(create);
				if(update==null) {
				    // looks like H2 do an early table name check
				    update = conn.prepareStatement("INSERT INTO db_version (db_version,date_update) VALUES (?,?)");
				}
				update.setInt(1, 0);
				Date date = new Date(Calendar.getInstance().getTimeInMillis());
				update.setDate(2, date);
				update.execute();
				conn.commit();

				Logger
						.debug(this.getClass(),
								"Table db_version created.  Trying to lock db_table again.");
				s.execute(lock);
				rs = s
						.executeQuery(select);

				rs.next();
				Config.DB_VERSION = rs.getInt("db_version");

			} catch (SQLException e2) {
				Logger.fatal(this.getClass(),
						"Locking of db_version table failed: "
								+ e2.getMessage());
				throw new DotRuntimeException(
						"Locking of db_version table failed: "
								+ e2.getMessage(), e2);
			}

		}
		Logger.debug(this.getClass(), "Locking db_version succeeded");

		boolean firstTimeStart = false;
		String name = null;
		try {
			Logger.info(this, "Starting startup tasks.");
			HibernateUtil.startTransaction();

			for (Class<?> c : runAlways) {
				name = c.getCanonicalName();
				name = name.substring(name.lastIndexOf(".") + 1);
				if (StartupTask.class.isAssignableFrom(c)) {
					StartupTask task;
					try {
						task = (StartupTask) c.newInstance();
					} catch (Exception e) {
						throw new DotRuntimeException(e.getMessage(), e);
					}
					HibernateUtil.startTransaction();
					if (task.forceRun()) {
						HibernateUtil.commitTransaction();
						HibernateUtil.startTransaction();
						Logger.info(this, "Running: " + name);
						task.executeUpgrade();
						if(name.equals("Task00001LoadSchema")){
							firstTimeStart = true;
						}
					} else {
						Logger.info(this, "Not running: " + name);
					}
					HibernateUtil.commitTransaction();
				}
			}
			Logger.info(this, "Finishing startup tasks.");
		} catch (Exception e) {
			HibernateUtil.rollbackTransaction();
			Logger.fatal(this, "Unable to execute the upgrade task : " + name, e);
		} finally {
			// This will commit the changes and close the connection
			HibernateUtil.closeSession();
			try {
			    update.close();
//			    conn.close();
			} catch(Exception exx) { /* Ignore this */ }
		}

		Logger.info(this, "Starting upgrade tasks.");

		Logger.info(this, "Database version: " + Config.DB_VERSION);

		name = null;
		//HibernateUtil.startTransaction();
		try {
			if(runOnce.size() > 0)
				ReindexThread.stopThread();
			for (Class<?> c : runOnce) {
				name = c.getCanonicalName();
				name = name.substring(name.lastIndexOf(".") + 1);
				String id = name.substring(4, 9);
				try {
					int taskId = Integer.parseInt(id);
					if (StartupTask.class.isAssignableFrom(c)
							&& taskId > Config.DB_VERSION) {
						StartupTask task;
						try {
							task = (StartupTask) c.newInstance();
						} catch (Exception e) {
							throw new DotRuntimeException(e.getMessage(), e);
						}
						//HibernateUtil.startTransaction();

						if (!firstTimeStart && task.forceRun()) {
							HibernateUtil.commitTransaction();
							HibernateUtil.startTransaction();
							Logger.info(this, "Running: " + name);
							if(name.equals("Task00250UpdateMysqlTablesToINNODB")){
								s = conn.createStatement();
								s.execute(commit);
								task.executeUpgrade();
								s = conn.createStatement();
								s.execute(lock);
							}else{
							  task.executeUpgrade();
							}
						} 
						// Nothing to execute, or the task ran ok so bump
						// the db version.
						try {
//						    conn = DbConnectionFactory.getDataSource().getConnection();
						    conn.setAutoCommit(true);
						    update = conn.prepareStatement("INSERT INTO db_version (db_version,date_update) VALUES (?,?)");
    						update.setInt(1, taskId);
    						Date date = new Date(Calendar.getInstance().getTimeInMillis());
    						update.setDate(2, date);
    						update.execute();
						}
						finally {
//							s.execute(commit);
						    update.close();
//						    conn.close();
						}

						Logger.info(this, "Database upgraded to version: "
								+ taskId);
						HibernateUtil.commitTransaction();

					}
				} catch (NumberFormatException e) {
					Logger
							.error(
									this,
									"Class "
											+ name
											+ " has invalid name or shouldn't be in the tasks package.");

				}
			}
			//if(runOnce.size() > 0)
				//ReindexThread.startThread(Config.getIntProperty("REINDEX_THREAD_SLEEP", 500), Config.getIntProperty("REINDEX_THREAD_INIT_DELAY", 5000));
			
		} catch (Exception e) {
			HibernateUtil.rollbackTransaction();
			Logger.fatal(this, "Unable to execute the upgrade task : " + name, e);
			throw new DotDataException("Unable to execute startup task : ",e);
		} finally {
			// This will commit the changes and close the connection
			HibernateUtil.closeSession();
			
			// DOTCMS-4352
			try {
				if(conn != null && !conn.isClosed()){
					Statement s1 = conn.createStatement();
					if(DbConnectionFactory.isMySql()){
						s1.execute(commit);
					}
					if(!conn.getAutoCommit()){
						conn.commit();
					}
					conn.close();
				}
			} catch (Exception e) {
				Logger.debug(StartupTasksExecutor.class, "Exception: "
						+ e.getMessage(), e);
				throw new DotDataException(
						"Exception finishing upgrade tasks: " + e.getMessage(),
						e);
			}
			Logger.info(this, "Finishing upgrade tasks.");
		}

	}



}
