package com.dotmarketing.startup;

import java.sql.Connection;
import java.util.Date;
import java.util.Map;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.common.reindex.ReindexThread;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.TaskLocatorUtil;

public class StartupTasksExecutor {

	
	private static StartupTasksExecutor executor;




	private final String pgCreate  = "CREATE TABLE db_version (db_version integer NOT NULL, date_update timestamp with time zone NOT NULL, CONSTRAINT db_version_pkey PRIMARY KEY (db_version));";
	private final String myCreate  = "CREATE TABLE `db_version` (`db_version` INTEGER UNSIGNED NOT NULL,`date_update` DATETIME NOT NULL, PRIMARY KEY (`db_version`))";
	private final String oraCreate = "CREATE TABLE \"DB_VERSION\" ( \"DB_VERSION\" INTEGER NOT NULL , \"DATE_UPDATE\" TIMESTAMP NOT NULL, PRIMARY KEY (\"DB_VERSION\") )";
	private final String msCreate  = "CREATE TABLE db_version (	db_version int NOT NULL , date_update datetime NOT NULL, PRIMARY KEY (db_version) )";

	private final String select = "SELECT count(*) as count_versions, max(db_version) AS db_version FROM db_version";



	final boolean firstTimeStart;
	
	
	private StartupTasksExecutor() {

	    insureDbVersionTable();
	    Config.DB_VERSION = currentDbVersion();
        this.firstTimeStart = (Config.DB_VERSION==0);
	    
	}

	public static synchronized StartupTasksExecutor getInstance() {
		if (executor == null)
			executor = new StartupTasksExecutor();
		return executor;
	}


	private final String createTableSQL() {
	    
	       return (DbConnectionFactory.isPostgres()) 
	                        ? pgCreate
	                        : DbConnectionFactory.isMySql() 
	                            ? myCreate 
	                            : DbConnectionFactory.isOracle()
	                                ? oraCreate
	                                : msCreate;
	}

    /**
     * This will create the db version table if it does not already exist
     * @return
     */
	private boolean insureDbVersionTable() {

        try {
            currentDbVersion();
            return true;

        } catch (Exception e) {
            return createDbVersionTable();
        }

    }
    
    /**
     * Runs with a separate DB connection
     * @return
     */
    private int currentDbVersion() {
        try (Connection conn = DbConnectionFactory.getDataSource().getConnection()) {
            DotConnect db =  new DotConnect().setSQL(select);
            return  db.getInt("count_versions") >0
                        ? db.getInt("db_version")
                        : 0;

        } catch (Exception e) {
            throw new DotRuntimeException(e);
        }
    }
    
    /**
     * Runs with a separate DB connection
     * @return
     */
    private boolean createDbVersionTable() {

        try (Connection conn = DbConnectionFactory.getDataSource().getConnection()) {
            new DotConnect().setSQL(createTableSQL()).loadResult(conn);
            return true;

        } catch (Exception e) {
            Logger.debug(this.getClass(), e.getMessage(),e);
            throw new DotRuntimeException(e);
        }

    }
    
    
    public void executeStartUpTasks() throws DotDataException {
        
        Logger.debug(this.getClass(), "Running Startup Tasks");


        String name = null;
        try {
            Logger.info(this, "Running Startup Tasks");


            for (Class<?> c : TaskLocatorUtil.getStartupRunAlwaysTaskClasses()) {
                HibernateUtil.startTransaction();
                name = c.getCanonicalName();
                name = name.substring(name.lastIndexOf(".") + 1);
                if (StartupTask.class.isAssignableFrom(c)) {
                    StartupTask  task = (StartupTask) c.newInstance();
                    if (task.forceRun()) {
                        HibernateUtil.startTransaction();
                        Logger.info(this, "Running Startup Tasks : " + name);
                        task.executeUpgrade();
                    } else {
                        Logger.info(this, "Not Running Startup Tasks: " + name);
                    }
                }
                HibernateUtil.closeAndCommitTransaction();
            }
            Logger.info(this, "Finishing startup tasks.");
        } catch (Throwable e) {
            HibernateUtil.rollbackTransaction();
            Logger.error(this, "FATAL: Unable to execute the upgrade task : " + name, e);
            if(Config.getBooleanProperty("SYSTEM_EXIT_ON_STARTUP_FAILURE", true)){
              e.printStackTrace();
              System.exit(1);
            }
        } finally {
            // This will commit the changes and close the connection
            HibernateUtil.closeAndCommitTransaction();
        }

        
    }
    
    
    
    
    
    public void executeUpgrades() throws DotDataException {

        Logger.info(this, "---");
        Logger.info(this, "");
        Logger.info(this, "Running Upgrade Tasks");

        Logger.info(this, "Database version: " + Config.DB_VERSION);

        String name = null;


        ReindexThread.pause();
        for (Class<?> c : TaskLocatorUtil.getStartupRunOnceTaskClasses()) {
            name = c.getCanonicalName();
            name = name.substring(name.lastIndexOf(".") + 1);
            String id = name.substring(4, 9);
            try {
                int taskId = Integer.parseInt(id);
                if (StartupTask.class.isAssignableFrom(c) && taskId > Config.DB_VERSION) {
                    StartupTask task;
                    try {
                        task = (StartupTask) c.newInstance();
                    } catch (Exception e) {
                        throw new DotRuntimeException(e.getMessage(), e);
                    }

                    if (!firstTimeStart && task.forceRun()) {
                        HibernateUtil.startTransaction();
                        Logger.info(this, "Running Upgrade Tasks: " + name);
                        task.executeUpgrade();

                    }
 
                    new DotConnect()
                        .setSQL("INSERT INTO db_version (db_version,date_update) VALUES (?,?)")
                        .addParam(taskId)
                        .addParam(new Date())
                        .loadResult();
                    Logger.info(this, "Database upgraded to version: " + taskId);
                    HibernateUtil.closeAndCommitTransaction();
                    Config.DB_VERSION = taskId;
                }
            } catch (Exception e) {
                HibernateUtil.rollbackTransaction();
                if (Config.getBooleanProperty("SYSTEM_EXIT_ON_STARTUP_FAILURE", true)) {
                    Logger.error(this, "FATAL: " +e.getMessage(),e);
                    System.exit(1);
                }
            } finally {
                HibernateUtil.closeAndCommitTransaction();
            }

        }

        ReindexThread.startThread();

        Logger.info(this, "Finishing upgrade tasks.");


    }



}
