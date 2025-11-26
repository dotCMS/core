package com.dotmarketing.startup;

import com.dotmarketing.exception.DotHibernateException;
import com.google.common.annotations.VisibleForTesting;
import java.sql.Connection;
import java.util.Date;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.common.reindex.ReindexThread;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.TaskLocatorUtil;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StartupTasksExecutor {

	private static StartupTasksExecutor executor;

    private final String pgCreate  = "CREATE TABLE db_version (db_version integer NOT NULL, date_update timestamp with time zone NOT NULL, CONSTRAINT db_version_pkey PRIMARY KEY (db_version));";
    private final String pgCreateDataVersion  = "CREATE TABLE data_version (data_version integer NOT NULL, date_update timestamp with time zone NOT NULL, CONSTRAINT data_version_pkey PRIMARY KEY (data_version));";
    private final String myCreate  = "CREATE TABLE `db_version` (`db_version` INTEGER UNSIGNED NOT NULL,`date_update` DATETIME NOT NULL, PRIMARY KEY (`db_version`))";
    private final String myCreateDataVersion  = "CREATE TABLE `data_version` (`data_version` INTEGER UNSIGNED NOT NULL,`date_update` DATETIME NOT NULL, PRIMARY KEY (`data_version`))";
    private final String oraCreate = "CREATE TABLE \"DB_VERSION\" ( \"DB_VERSION\" INTEGER NOT NULL , \"DATE_UPDATE\" TIMESTAMP NOT NULL, PRIMARY KEY (\"DB_VERSION\") )";
    private final String oraCreateDataVersion = "CREATE TABLE \"DATA_VERSION\" ( \"DATA_VERSION\" INTEGER NOT NULL , \"DATE_UPDATE\" TIMESTAMP NOT NULL, PRIMARY KEY (\"DATA_VERSION\") )";
    private final String msCreate  = "CREATE TABLE db_version (	db_version int NOT NULL , date_update datetime NOT NULL, PRIMARY KEY (db_version) )";
    private final String msCreateDataVersion  = "CREATE TABLE data_version ( data_version int NOT NULL , date_update datetime NOT NULL, PRIMARY KEY (data_version) )";

    private final String SELECT = "SELECT max(db_version) AS test FROM db_version";
    private final String SELECT_DATA_VERSION = "SELECT max(data_version) AS test FROM data_version";
    private final String INSERT = "INSERT INTO db_version (db_version,date_update) VALUES (?,?)";
    private final String INSERT_DATA_VERSION = "INSERT INTO data_version (data_version,date_update) VALUES (?,?)";

	private static final Pattern TASK_ID_PATTERN = Pattern.compile("[0-9]+");

	final boolean firstTimeStart;

    private StartupTasksExecutor() {

        insureDbVersionTable();
        insureDataVersionTable();
        Config.DB_VERSION = currentDbVersion();
        Config.DATA_VERSION = currentDataVersion();
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
     * Returns the SQL to create the data_version table
     */
    private final String createDataVersionTableSQL() {

        return (DbConnectionFactory.isPostgres())
                ? pgCreateDataVersion
                : DbConnectionFactory.isMySql()
                        ? myCreateDataVersion
                        : DbConnectionFactory.isOracle()
                                ? oraCreateDataVersion
                                : msCreateDataVersion;
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
     * This will create the data version table if it does not already exist
     */
    @VisibleForTesting
    boolean insureDataVersionTable() {

        try {
            currentDataVersion();
            return true;
        } catch (Exception e) {
            return createDataVersionTable();
        }

    }

    /**
     * Runs with a separate DB connection
     * @return
     */
    private int currentDbVersion() {
        try (Connection conn = DbConnectionFactory.getDataSource().getConnection()) {
            DotConnect db =  new DotConnect().setSQL(SELECT);
            return  db.loadInt("test",conn);

        } catch (Exception e) {
            throw new DotRuntimeException(e);
        }
    }

    /**
     * Returns the current data version. Runs with a separate DB connection
     */
    @VisibleForTesting
    int currentDataVersion() {
        try (Connection conn = DbConnectionFactory.getDataSource().getConnection()) {
            DotConnect db = new DotConnect().setSQL(SELECT_DATA_VERSION);
            return db.loadInt("test",conn);

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
            new DotConnect().setSQL(INSERT).addParam(0).addParam(new Date()).loadResult(conn);
            return true;

        } catch (Exception e) {
            Logger.debug(this.getClass(), e.getMessage(),e);
            throw new DotRuntimeException(e);
        }

    }

    /**
     * Creates the data version table. Runs with a separate DB connection
     */
    private boolean createDataVersionTable() {

        try (Connection conn = DbConnectionFactory.getDataSource().getConnection()) {
            new DotConnect().setSQL(createDataVersionTableSQL()).loadResult(conn);
            new DotConnect().setSQL(INSERT_DATA_VERSION).addParam(0).addParam(new Date()).loadResult(conn);
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
                    StartupTask  task = (StartupTask) c.getDeclaredConstructor().newInstance();
                    if (task.forceRun()) {
                        if(!TaskLocatorUtil.getTaskClassesNoTransaction().contains(c)){
                            HibernateUtil.startTransaction();
                        }
                        Logger.info(this, "Running Startup Tasks : " + name);
                        task.executeUpgrade();
                    } else {
                        Logger.info(this, "Not Running Startup Tasks: " + name);
                    }
                }
                if(!TaskLocatorUtil.getTaskClassesNoTransaction().contains(c)){
                    HibernateUtil.closeAndCommitTransaction();
                }
            }
            Logger.info(this, "Finishing startup tasks.");
        } catch (Throwable e) {
            taskFailure(e);
        } finally {
            // This will commit the changes and close the connection
            HibernateUtil.closeAndCommitTransaction();
        }

        
    }

    /**
     * Returns the id part of a task name
     * @param taskName
     * @return
     */
    @VisibleForTesting
    String getTaskId(final String taskName){
        final Matcher matcher = TASK_ID_PATTERN.matcher(taskName);

        if (matcher.find()){
            return matcher.group();
        }
        return "-1";
    }


    public void executeSchemaUpgrades() throws DotDataException {

        Logger.info(this, "---");
        Logger.info(this, "");
        Logger.info(this, "Running Upgrade Tasks");

        Logger.info(this, "Database version: " + Config.DB_VERSION);

        String name = null;
        
        for (Class<?> c : TaskLocatorUtil.getStartupRunOnceTaskClasses()) {
            name = c.getCanonicalName();
            name = name.substring(name.lastIndexOf(".") + 1);
            String id = getTaskId(name);
            try {
                int taskId = Integer.parseInt(id);
                if (StartupTask.class.isAssignableFrom(c) && taskId > Config.DB_VERSION) {
                    StartupTask task;
                    try {
                        task = (StartupTask) c.getDeclaredConstructor().newInstance();
                    } catch (Exception e) {
                        throw new DotRuntimeException(e.getMessage(), e);
                    }

                    if (!firstTimeStart && task.forceRun()) {
                        if(!TaskLocatorUtil.getTaskClassesNoTransaction().contains(c)){
                            HibernateUtil.startTransaction();
                        }
                        Logger.info(this, "Running Upgrade Tasks: " + name);
                        task.executeUpgrade();

                    }
 
                    new DotConnect()
                        .setSQL(INSERT)
                        .addParam(taskId)
                        .addParam(new Date())
                        .loadResult();
                    Logger.info(this, "Database upgraded to version: " + taskId);
                    if(!TaskLocatorUtil.getTaskClassesNoTransaction().contains(c)){
                        HibernateUtil.closeAndCommitTransaction();
                    }
                    Config.DB_VERSION = taskId;
                }
            } catch (Exception e) {
                taskFailure(e);
            } finally {
                if(!TaskLocatorUtil.getTaskClassesNoTransaction().contains(c)){
                    HibernateUtil.closeAndCommitTransaction();
                }
            }

        }

        ReindexThread.startThread();

        Logger.info(this, "Finishing upgrade tasks.");


    }

    /**
     * Runs the data upgrade tasks, those that are not related to the database schema upgrade tasks,
     * data tasks are mostly tasks to solve data issues using our existing APIs.
     * <p>
     * The list of data upgrade tasks is obtained from the
     * {@link TaskLocatorUtil#getStartupRunOnceDataTaskClasses()}
     *
     * @throws DotDataException
     */
    public void executeDataUpgrades() throws DotDataException {

        Logger.info(this, "---");
        Logger.info(this, "");
        Logger.info(this, "Running Data Upgrade Tasks");
        Logger.info(this, "Database data version: " + Config.DATA_VERSION);

        String name;

        for (Class<?> c : TaskLocatorUtil.getStartupRunOnceDataTaskClasses()) {

            name = c.getCanonicalName();
            name = name.substring(name.lastIndexOf(".") + 1);
            String id = getTaskId(name);

            try {
                int taskId = Integer.parseInt(id);
                if (StartupTask.class.isAssignableFrom(c) && taskId > Config.DATA_VERSION) {
                    StartupTask task;
                    try {
                        task = (StartupTask) c.getDeclaredConstructor().newInstance();
                    } catch (Exception e) {
                        throw new DotRuntimeException(e.getMessage(), e);
                    }

                    if (!firstTimeStart && task.forceRun()) {
                        if(!TaskLocatorUtil.getTaskClassesNoTransaction().contains(c)){
                            HibernateUtil.startTransaction();
                        }
                        Logger.info(this, "Running Data Upgrade Tasks: " + name);
                        task.executeUpgrade();
                    }

                    new DotConnect()
                            .setSQL(INSERT_DATA_VERSION)
                            .addParam(taskId)
                            .addParam(new Date())
                            .loadResult();
                    Logger.info(this, "Data upgraded to version: " + taskId);
                    if(!TaskLocatorUtil.getTaskClassesNoTransaction().contains(c)){
                        HibernateUtil.closeAndCommitTransaction();
                    }
                    Config.DATA_VERSION = taskId;
                }
            } catch (Exception e) {
                taskFailure(e);
            } finally {
                if(!TaskLocatorUtil.getTaskClassesNoTransaction().contains(c)){
                    HibernateUtil.closeAndCommitTransaction();
                }
            }

        }

        Logger.info(this, "Finishing data upgrade tasks.");
    }

	/**
     * This will execute all the UT that were backported to the LTS version.
     * Will be run everytime the server gets restarted just like the Startup Tasks.
     * But these will be run after the upgradeTasks.
     *
     * @throws DotDataException
     */
    public void executeBackportedTasks() throws DotDataException {
        Logger.debug(this.getClass(), "Running Backported Tasks");
        String name = null;
        try {
            Logger.info(this, "Running Backported Tasks");


            for (Class<?> c : TaskLocatorUtil.getBackportedUpgradeTaskClasses()) {
                if(!TaskLocatorUtil.getTaskClassesNoTransaction().contains(c)){
                    HibernateUtil.startTransaction();
                }
                name = c.getCanonicalName();
                name = name.substring(name.lastIndexOf(".") + 1);
		String id = getTaskId(name);
                int taskId = Integer.parseInt(id);
		if (StartupTask.class.isAssignableFrom(c) && taskId > Config.DB_VERSION) {
                    StartupTask  task = (StartupTask) c.getDeclaredConstructor().newInstance();
                    if (task.forceRun()) {
                        if(!TaskLocatorUtil.getTaskClassesNoTransaction().contains(c)){
                            HibernateUtil.startTransaction();
                        }
                        Logger.info(this, "Running Backported Tasks : " + name);
                        task.executeUpgrade();
                    } else {
                        Logger.info(this, "Not Running Backported Tasks: " + name);
                    }
                }
                if(!TaskLocatorUtil.getTaskClassesNoTransaction().contains(c)){
                    HibernateUtil.closeAndCommitTransaction();
                }
            }
            Logger.info(this, "Finishing Backported tasks.");
        } catch (Throwable e) {
            taskFailure(e);
        } finally {
            // This will commit the changes and close the connection
            HibernateUtil.closeAndCommitTransaction();
        }


    }

    private void taskFailure(Throwable e) throws DotHibernateException {
        HibernateUtil.rollbackTransaction();
        for(int i = 0; i < 3; i++){
            System.err.println("FATAL ERROR RUNNING TASK: " + e.getMessage());
            Logger.error(this, "FATAL ERROR RUNNING TASK: " + e.getMessage(), e);
        }

        e.printStackTrace();

        if (Config.getBooleanProperty("SYSTEM_EXIT_ON_STARTUP_FAILURE", true)) {
            try {
                Thread.sleep(10000);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                Logger.debug(this,"Thread was interrupted", ex);
            }
            com.dotcms.shutdown.SystemExitManager.startupFailureExit("Startup task execution failed: " + e.getMessage());
        }
    }


}
