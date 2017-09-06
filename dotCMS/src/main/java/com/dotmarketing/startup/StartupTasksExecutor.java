package com.dotmarketing.startup;

import java.sql.Connection;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.common.reindex.ReindexThread;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.LocalTransaction;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.startup.runalways.Task00001LoadSchema;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.TaskLocatorUtil;


public class StartupTasksExecutor {

	
	private static StartupTasksExecutor executor;


	private String pgCreate = "CREATE TABLE db_version (db_version integer NOT NULL, date_update timestamp with time zone NOT NULL, CONSTRAINT db_version_pkey PRIMARY KEY (db_version));";
	private String myCreate = "CREATE TABLE `db_version` (`db_version` INTEGER UNSIGNED NOT NULL,`date_update` DATETIME NOT NULL, PRIMARY KEY (`db_version`))";
	private String oraCreate = "CREATE TABLE \"DB_VERSION\" ( \"DB_VERSION\" INTEGER NOT NULL , \"DATE_UPDATE\" TIMESTAMP NOT NULL, PRIMARY KEY (\"DB_VERSION\") )";
	private String msCreate ="CREATE TABLE db_version (	db_version int NOT NULL , date_update datetime NOT NULL, PRIMARY KEY (db_version) )";
	private String h2Create = "CREATE TABLE db_version (db_version integer NOT NULL, date_update timestamp NOT NULL, CONSTRAINT db_version_pkey PRIMARY KEY (db_version))";
	




	private final String DOTCMS_FIRST_TIME_START ="DOTCMS_FIRST_TIME_START";
	private final String create;
	private final String insert = "INSERT INTO db_version (db_version,date_update) VALUES (?,?)";
	private final String select = "SELECT max(db_version) AS db_version FROM db_version";
	
	
	private StartupTasksExecutor() {
		if (DbConnectionFactory.isPostgres()) {
			create = pgCreate;
		}
		else if (DbConnectionFactory.isMySql()) {
			create = myCreate.toLowerCase();
		}
		else if (DbConnectionFactory.isOracle()) {
			create = oraCreate;

		}
		else if (DbConnectionFactory.isMsSql()) {
			create = msCreate;
		}
		else {
		    create = h2Create;
		}
	}

	public static StartupTasksExecutor getInstance() {
		if (executor == null)
			executor = new StartupTasksExecutor();
		return executor;
	}

	/**
	 * Check which database we're using, and select the apropiate SQL. In a
	 * different method to avoid further clutter
	 * @throws DotDataException 
	 */
	private void setupDBVersionTable() throws DotDataException {
		try{
			LocalTransaction.wrap(()->{
				Connection conn  = DbConnectionFactory.getConnection();
				ResultSet rs;
				try {
					rs = conn.prepareStatement(select).executeQuery();
					rs.next();
					Config.DB_VERSION = rs.getInt("db_version");
				} catch (SQLException e) {
					throw new DotDataException(e);
				}

			});
		} catch (DotDataException e) {
			LocalTransaction.wrap(()->{
				Config.DB_VERSION = 0;
				new DotConnect().setSQL(create).loadResult(); 
			});
			
		}
	}
	
	Comparator<Class<?>> comparator = new Comparator<Class<?>>() {
		public int compare(Class<?> o1, Class<?> o2) {
			return o1.getName().compareTo(o2.getName());
		}
	};
	
	
	public void executeStartup() throws DotDataException, DotRuntimeException {
		setupDBVersionTable();
		List<Class<?>> runAlways= TaskLocatorUtil.getStartupRunAlwaysTaskClasses();
		Collections.sort(runAlways, comparator);
		String name = null;
		try {
			Logger.info(this, "Starting startup tasks.");
			for (final Class<?> c : runAlways) {
				name = c.getCanonicalName();
				name = name.substring(name.lastIndexOf(".") + 1);
				if (StartupTask.class.isAssignableFrom(c)) {
					
					LocalTransaction.wrap(()->{

						StartupTask task;
						try {
							task = (StartupTask) c.newInstance();
						} catch (Exception e) {
							throw new DotRuntimeException(e.getMessage(), e);
						}

						if (task.forceRun()) {
							Logger.info(this, "Running: " + c.getName());
							task.executeUpgrade();
							if(c.getName().equals(Task00001LoadSchema.class.getName())){
								System.setProperty(DOTCMS_FIRST_TIME_START, "true");
							}
						} else {
							Logger.info(this, "Not running: " + c.getName());
						}
					});

				}
			}
			Logger.info(this, "Finishing startup tasks.");
		} catch (Exception e) {
			Logger.fatal(this, "Unable to execute the upgrade task : " + name, e);
			throw new DotDataException("Unable to execute startup task : ",e);
		}
	}
	
	
	public void executeUpgrades() throws DotDataException, DotRuntimeException {

		List<Class<?>> runOnce = TaskLocatorUtil.getStartupRunOnceTaskClasses();
		Collections.sort(runOnce, comparator);

		String name = null;
		Logger.info(this, "Starting upgrade tasks.");
		Logger.info(this, "Database version: " + Config.DB_VERSION);


		try {
			if(runOnce.size() > 0){
				ReindexThread.stopThread();
			}
			for (final Class<?> c : runOnce) {
				name = c.getCanonicalName();
				name = name.substring(name.lastIndexOf(".") + 1);
				String id = name.substring(4, 9);

				int taskId = Integer.parseInt(id);
				if (StartupTask.class.isAssignableFrom(c) && taskId > Config.DB_VERSION) {
					LocalTransaction.wrap(()->{
					
						StartupTask task;
						try {
							task = (StartupTask) c.newInstance();
						} catch (Exception e) {
							throw new DotRuntimeException(e.getMessage(), e);
						}

						if (System.getProperty("DOTCMS_FIRST_TIME_START")==null && task.forceRun()) {
							Logger.info(this, "Running: " + c.getName());

							task.executeUpgrade();
							Logger.info(this, "Database upgraded to version: " + taskId);
						} 
						Date date = new Date(Calendar.getInstance().getTimeInMillis());
						new DotConnect().setSQL(insert).addParam(taskId).addParam(date).loadResult();
						Config.DB_VERSION=taskId;
					});
				}
			}
		} catch (Exception e) {
			Logger.fatal(this, "Unable to execute the upgrade task : " + name, e);
			throw new DotDataException("Unable to execute startup task : ",e);
		}
		Logger.info(this, "Finishing upgrade tasks.");
		Logger.info(this, "Database version: " + Config.DB_VERSION);
	}

}


