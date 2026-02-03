
package com.dotmarketing.db;

import static com.dotmarketing.util.Constants.DATABASE_DEFAULT_DATASOURCE;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import javax.naming.Binding;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.sql.DataSource;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Constants;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.StringUtils;
import com.dotmarketing.util.UtilMethods;
import com.liferay.util.JNDIUtil;
import io.vavr.control.Try;

public class DbConnectionFactory {



    public static final String POSTGRESQL = "PostgreSQL";


    private static DataSource defaultDataSource = null;

    /**
     * Gets the autoCommit for the current connection
     * @return boolean
     */
    public static boolean getAutoCommit() {

        boolean autoCommit = false;

        try {
            autoCommit = getConnection().getAutoCommit();
        } catch (SQLException e) {
            autoCommit = false;
        }

        return autoCommit;
    } // getAutoCommit.

    /**
     * If is in transaction, will set the autocommit value
     * @param autoCommit boolean
     */
    public static void setAutoCommit(final boolean autoCommit) {

        try {
            getConnection().setAutoCommit(autoCommit);
        } catch (SQLException e) {
            Logger.error(DbConnectionFactory.class,
                    "---------- DBConnectionFactory: error setting the autocommit " + DATABASE_DEFAULT_DATASOURCE,
                    e);
        }
    } // setAutoCommit.

    /**
     * Returns the current date (now) depending of the right type for the current database type.
     * @return Date
     */
    public static Date now() {

        return new Timestamp(System.currentTimeMillis());
    }


    /**
     * Return the default db type to map the String
     * @return int
     */
    public static int getDBStringType() {

        return Types.VARCHAR;
    }

    
    public enum DataBaseType {
        POSTGRES;
    }



    private static final ThreadLocal<HashMap<String, Connection>>
        connectionsHolder =
        new ThreadLocal<>();

    public static DataSource getDataSource() {

        if (null == defaultDataSource) {

            synchronized (DbConnectionFactory.class) {

                if (null == defaultDataSource) {
                    // Prevent DataSource recreation during shutdown
                    if (com.dotcms.shutdown.ShutdownCoordinator.isShutdownStarted()) {
                        Logger.debug(DbConnectionFactory.class, "Shutdown in progress - DataSource not available");
                        throw new com.dotcms.shutdown.ShutdownException("DataSource not available during shutdown");
                    }
                    
                    try {
                        defaultDataSource = DataSourceStrategyProvider.getInstance().get();
                        addDatasourceToJNDIIfNeeded();
                    } catch (Throwable e) {
                        Logger.error(DbConnectionFactory.class,
                                "---------- DBConnectionFactory: error getting dbconnection " + Constants.DATABASE_DEFAULT_DATASOURCE,
                                e);
                        if(Config.getBooleanProperty("SYSTEM_EXIT_ON_STARTUP_FAILURE", true)){
                            e.printStackTrace();
                            com.dotcms.shutdown.SystemExitManager.databaseFailureExit("Database connection initialization failed: " + e.getMessage());
                        }

                        throw new DotRuntimeException(e.toString());
                    }
                }
            }
        }

        return defaultDataSource;
    }

    /**
     * Saves a datasource in JNDI in case <b>ADD_DATASOURCE_TO_JNDI</b> is set to true.
     * By default, <b>ADD_DATASOURCE_TO_JNDI</b> is set to false
     */
    private static void addDatasourceToJNDIIfNeeded() {
        try {
            if (Config.getBooleanProperty("ADD_DATASOURCE_TO_JNDI", false)) {
                final Context context = new InitialContext();
                context.createSubcontext("jdbc");
                context.bind(DATABASE_DEFAULT_DATASOURCE, defaultDataSource);
                Logger.info(DbConnectionFactory.class,
                        "---------- DBConnectionFactory:Datasource added to JNDI context ---------------");
            }
        } catch (NamingException e) {
            Logger.error(DbConnectionFactory.class,
                    "---------- DBConnectionFactory: Error setting datasource in JNDI context ---------------", e);
            throw new DotRuntimeException(e.toString());
        }
    }


    /**
     * This is used to get data source to other database != than the default dotCMS one
     */
    public static DataSource getDataSource(String dataSource) {
        try {
            InitialContext ctx = new InitialContext();
            DataSource ds = (DataSource) JNDIUtil.lookup(ctx, dataSource);
            return ds;

        } catch (Exception e) {
            Logger.error(DbConnectionFactory.class,
                "---------- DBConnectionFactory: error getting dbconnection ---------------" + dataSource, e);
            throw new DotRuntimeException(e.toString());
        }
    }

    /**
     * return the number of db connections called for
     * (Useful in tests where we want to make sure
     * we are not hitting the db)
     * @return
     */
    public static long connectionsCalledFor() {
        return connectionsCalledFor;
    }
    
    private static long connectionsCalledFor=0;

    /**
     * This method sets on the current thread a connection
     * @param connection {@link Connection}
     */
    public static void setConnection(final Connection connection) {

        try {

            if (null != connection && !connection.isClosed()) {

                HashMap<String, Connection> connectionsList = connectionsHolder.get();
                if (connectionsList == null) {
                    connectionsList = new HashMap<>();
                    connectionsHolder.set(connectionsList);
                }

                connectionsList.put(DATABASE_DEFAULT_DATASOURCE, connection);
            }
        } catch (Exception e) {
            Logger.error(DbConnectionFactory.class, "---------- DBConnectionFactory: error : " + e);
            Logger.debug(DbConnectionFactory.class, "---------- DBConnectionFactory: error ", e);
            throw new DotRuntimeException(e.getMessage(), e);
        }
    }
    
    /**
     * This method retrieves the default connection to the dotCMS DB
     */
    public static Connection getConnection() {

        try {
            HashMap<String, Connection> connectionsList = (HashMap<String, Connection>) connectionsHolder.get();
            Connection connection = null;
            connectionsCalledFor++;
            if (connectionsList == null) {
                connectionsList = new HashMap<>();
                connectionsHolder.set(connectionsList);
            }

            connection = connectionsList.get(DATABASE_DEFAULT_DATASOURCE);

            if (connection == null || connection.isClosed()) {
                DataSource db = getDataSource();
                connection = db.getConnection();
                connectionsList.put(DATABASE_DEFAULT_DATASOURCE, connection);
                Logger.debug(DbConnectionFactory.class,
                    "Connection opened for thread " + Thread.currentThread().getId() + "-" +
                        DATABASE_DEFAULT_DATASOURCE);
            }

 

            return connection;
        } catch (com.dotcms.shutdown.ShutdownException e) {
            // Handle shutdown exceptions quietly
            Logger.debug(DbConnectionFactory.class, "Database access during shutdown: " + e.getMessage());
            throw e;
        } catch (Exception e) {
            // Check if this is during shutdown to reduce noise
            if (com.dotcms.shutdown.ShutdownCoordinator.isShutdownStarted()) {
                Logger.debug(DbConnectionFactory.class, "Database access during shutdown: " + e.getMessage());
            } else {
                Logger.error(DbConnectionFactory.class, "---------- DBConnectionFactory: error : " + e);
                Logger.debug(DbConnectionFactory.class, "---------- DBConnectionFactory: error ", e);
            }
            throw new DotRuntimeException(e.getMessage(), e);
        }
    }

    /**
     * Returns true if the connection is already create and will be reuse in a transaction.
     * Otherwise will returns false, that means a new connection will be created.
     * @return boolean
     */
    public static boolean connectionExists() {

        boolean isCreated = false;
        final Map<String, Connection> connectionsMap =
                (HashMap<String, Connection>) connectionsHolder.get();

        if (connectionsMap != null && connectionsMap.size() > 0) {
            final Connection connection =
                    connectionsMap.get(DATABASE_DEFAULT_DATASOURCE);
            try {
                isCreated = (connection != null && !connection.isClosed());
            } catch (SQLException e) {
                Logger.error(DbConnectionFactory.class, "---------- DBConnectionFactory: error : " + e);
            }
        }

        return isCreated;
    } // connectionExists.

    /**
     * Returns if the db is in a transaction - it will not open a db connection
     * if there is not one already open - instead, it will return false
     */
    public static boolean inTransaction() {

        HashMap<String, Connection> connectionsList = (HashMap<String, Connection>) connectionsHolder.get();
        if (connectionsList == null || connectionsList.size() == 0) {
            return false;
        }
        Connection connection = connectionsList.get(DATABASE_DEFAULT_DATASOURCE);

        try {
            if (connection == null || connection.isClosed()) {
                return false;
            }
            return (!connection.getAutoCommit());
        } catch (SQLException e) {
            Logger.error(DbConnectionFactory.class, "---------- DBConnectionFactory: error : " + e);
            throw new DotRuntimeException(e.toString());

        }

    }


    /**
     * Retrieves the list of all valid dataSources setup in the dotCMS
     */
    @SuppressWarnings("unchecked")
    public static ArrayList<String> getAllDataSources() throws NamingException {
        ArrayList<String> results = new ArrayList<>();
        Context ctx;

        ctx = (Context) new InitialContext().lookup("java:comp/env");
        NamingEnumeration ne = null;
        try {
            ne = ctx.listBindings("jdbc");
        } catch (NamingException e) {
            ctx = new InitialContext();
            ne = ctx.listBindings("jdbc");
        }
        while (ne.hasMore()) {
            Binding binding = (Binding) ne.next();
            Connection cn = null;
            try {
                DataSource db = (DataSource) ctx.lookup("jdbc/" + binding.getName());
                cn = db.getConnection();
                results.add("jdbc/" + binding.getName());
            } catch (Exception e) {
                Logger.info(DbConnectionFactory.class,
                    "Unable to add " + binding.getName() + " to list of datasources: " + e.getMessage());
            } finally {
                if (cn != null) {
                    try {
                        cn.close();
                    } catch (SQLException e) {
                    }
                }
            }
        }
        return results;
    }
    
    /**
     * Used to test db connectivity
     * can be useful when creating unit tests that otherwise
     * end up calling APIs that expect a DB to be present
     * @return
     */
    @SuppressWarnings("unchecked")
    public static boolean dbAvailable()  {
       return Try.of(()->getDBType()!=null).getOrElse(false);
    }
    
    /**
     * Retrieves a connection to the given dataSource
     */
    public static Connection getConnection(String dataSource) {

        try {
            HashMap<String, Connection> connectionsList = (HashMap<String, Connection>) connectionsHolder.get();
            Connection connection = null;

            if (connectionsList == null) {
                connectionsList = new HashMap<>();
                connectionsHolder.set(connectionsList);
            }

            connection = connectionsList.get(dataSource);

            if (connection == null || connection.isClosed()) {
                DataSource db = getDataSource(dataSource);
                Logger.debug(DbConnectionFactory.class,
                                ()-> "Opening connection for thread " + Thread.currentThread().getId() + "-" +
                        dataSource + "\n" + UtilMethods.getDotCMSStackTrace());
                connection = db.getConnection();
                connectionsList.put(dataSource, connection);
                Logger.debug(DbConnectionFactory.class,
                    "Connection opened for thread " + Thread.currentThread().getId() + "-" +
                        dataSource);
            }

            return connection;
        } catch (Exception e) {
            Logger.error(DbConnectionFactory.class,
                "---------- DBConnectionFactory: error getting dbconnection conn named", e);
            throw new DotRuntimeException(e.toString());
        }

    }

    /**
     * @deprecated in favor of {@link com.dotcms.business.CloseDBIfOpened}
     * This method closes all the possible opened connections
     */
    @Deprecated
    public static void closeConnection() {
        try {
            HashMap<String, Connection> connectionsList = (HashMap<String, Connection>) connectionsHolder.get();

            if (connectionsList == null) {
                connectionsList = new HashMap<>();
                connectionsHolder.set(connectionsList);
            }

            Logger.debug(DbConnectionFactory.class, ()-> "Closing all connections for " + Thread.currentThread().getId() +
                "\n" + UtilMethods.getDotCMSStackTrace());
            for (Entry<String, Connection> entry : connectionsList.entrySet()) {

                String ds = entry.getKey();
                Connection cn = entry.getValue();
                if (cn != null) {
                    try {
                        cn.close();
                    } catch (Exception e) {
                        Logger.warn(DbConnectionFactory.class,
                            "---------- DBConnectionFactory: error closing the db dbconnection: " + ds
                                + " ---------------", e);
                    }
                }
            }

            Logger.debug(DbConnectionFactory.class, ()-> "All connections closed for " + Thread.currentThread().getId());
            connectionsList.clear();

        } catch (Exception e) {
            Logger.error(DbConnectionFactory.class,
                "---------- DBConnectionFactory: error closing the db dbconnection ---------------", e);
            throw new DotRuntimeException(e.toString());
        }

    }

    /**
     * @deprecated in favor of {@link com.dotcms.business.CloseDBIfOpened}
     * This method closes a connection to the given datasource
     */
    @Deprecated
    public static void closeConnection(String ds) {
        try {
            HashMap<String, Connection> connectionsList = (HashMap<String, Connection>) connectionsHolder.get();

            if (connectionsList == null) {
                connectionsList = new HashMap<>();
                connectionsHolder.set(connectionsList);
            }

            Connection cn = connectionsList.get(ds);

            if (cn != null) {
                Logger.debug(DbConnectionFactory.class,
                    "Closing connection for ()-> " + Thread.currentThread().getId() + "-" + ds +
                        "\n" + UtilMethods.getDotCMSStackTrace());
                cn.close();
                connectionsList.remove(ds);
                Logger.debug(DbConnectionFactory.class,
                    "Connection closed for " + Thread.currentThread().getId() + "-" + ds);
            }

        } catch (Exception e) {
            Logger.error(DbConnectionFactory.class,
                "---------- DBConnectionFactory: error closing the db dbconnection: " + ds + " ---------------", e);
            throw new DotRuntimeException(e.toString());
        }

    }

    public static String getDBType() {

        return POSTGRESQL;
    }

    public static String getDBDateTimeFunction() {

            return "now()";
        
    }

    public static String getDBDateTimeType() {
        return "timestamp";

    }

    public static String getDBTrue() {
        return "'true'";


    }

    public static String getDBFalse() {

            return "'false'";


    }

    /**
     * Return true if the String represent one of the true
     * boolean value for the current database
     * @param value String representation of the boolean
     * @return true is the string represents a DB true value
     */
    public static boolean isDBTrue(String value) {

            return "t".equals(value.trim()) || "true".equals(value.trim());


    }

    /**
     * Return true if the String represent one on the false
     * boolean value for the current database
     * @param value String representation of the boolean
     * @return true is the string represents a DB false value
     */
    public static boolean isDBFalse(final String value) {

            return "f".equals(value.trim()) || "false".equals(value.trim());


    }

    /**
     * Return true if the String represent one on the true or false
     * values accepted by the current database
     * @param value String representation of the boolean
     * @return true is the string represents a DB false or true value
     */
    public static boolean isDBBoolean(final String value) {
        return isDBTrue(value) || isDBFalse(value);
    }

    public static boolean isOracle() {
        return false;
    }

    public static boolean isMsSql() {
        return false;
    }

    public static boolean isPostgres() {
        return true;
    }

    public static boolean isMySql() {
        return false;
    }

    public static int getDbVersion() {
        int version = 0;
        try {
            Connection con = getConnection();
            DatabaseMetaData meta = con.getMetaData();
            version = meta.getDatabaseMajorVersion();
        } catch (Exception e) {
            Logger.error(DbConnectionFactory.class,
                "---------- DBConnectionFactory: Error getting DB version " + "---------------", e);
            throw new DotRuntimeException(e.toString());
        }
        return version;
    }

    /**
     * Method to get the Database Full Version
     * @return Database Version (Major.Minor)
     */
    public static float getDbFullVersion() {
        try {
            Connection con = getConnection();
            DatabaseMetaData meta = con.getMetaData();
            String version = "%d.%d";
            version = String.format(version, meta.getDatabaseMajorVersion(), meta.getDatabaseMinorVersion());
            return Float.parseFloat(version);
        } catch (SQLException e) {
            Logger.error(DbConnectionFactory.class,
                    "---------- DBConnectionFactory: Error getting DB Full version " + "---------------", e);
            throw new DotRuntimeException(e.toString());
        }
    }

    /**
     * Returns the correct MySQL system variable used to define the database
     * Storage Engine. The old {@code storage_variable} was deprecated as of
     * version 5.5.3, and deemed completely invalid as of version 5.7.5.
     * <p>
     * This method reads the {@code mysql_storage_engine_varname} property from
     * the {@code dotmarketing-config.properties} file to get the correct
     * variable name. By default, returns the most recent system variable name.
     * If your MySQL database version is old, this variable name must change.
     * </p>
     *
     * @return The most recent system variable name for the Storage Engine
     * definition.
     */
    public static String getMySQLStorageEngine() {
        return Config.getStringProperty("mysql_storage_engine_varname", "default_storage_engine");
    }

    /**
     * Return Temporary word depending on the Database in the data source.
     */
    public static String getTempKeyword() {
        String tempKeyword = "temporary";

        if (isMsSql()) {
            tempKeyword = "";
        } else if (isOracle()) {
            tempKeyword = "global " + tempKeyword;
        }

        return tempKeyword;
    }

    /**
     * Moved from UtilMethods
     */
    public static void closeSilently() {
        try {
            HibernateUtil.closeSession();
        } catch (Exception e) {

        } finally {
            try {

                DbConnectionFactory.closeConnection();
            } catch (Exception e) {

            }
        }
    }

    /**
     * Returns if the db is in a transaction - it will not open a db connection
     * if there is not one already open - instead, it will return false
     */
    public static boolean startTransactionIfNeeded() throws DotDataException {
        boolean startTransaction = !inTransaction();

        try {
            if (startTransaction) {
                DbConnectionFactory.getConnection().setAutoCommit(false);
            }
        } catch (SQLException e) {
            Logger.error(DbConnectionFactory.class, e.getMessage(), e);
            throw new DotDataException(e.getMessage(), e);
        }
        return startTransaction;
    }

    public static void commit () throws DotDataException {

        try {
            if (inTransaction()) {
                DbConnectionFactory.getConnection().commit();

            }
        } catch (Exception e) {
            throw new DotDataException(e.getMessage(), e);
        }
    }

    public static void closeAndCommit() throws DotDataException {
        try {
            if (inTransaction()) {
                DbConnectionFactory.getConnection().commit();
            }
            closeConnection();
        } catch (Exception e) {
            throw new DotDataException(e.getMessage(), e);
        }

    }


    public static void rollbackTransaction() throws DotDataException {
        boolean inTransaction = inTransaction();
        try {
            if (inTransaction) {
                DbConnectionFactory.getConnection().rollback();
                DbConnectionFactory.getConnection().setAutoCommit(true);
            }
        } catch (SQLException e) {
            Logger.error(DbConnectionFactory.class, e.getMessage(), e);
            throw new DotDataException(e.getMessage(), e);
        }
    }

    public static Integer getInt(String value) {
        Integer defaultInteger = 0;

        if (StringUtils.isSet(value)) {
            try {
                defaultInteger = Integer.parseInt(value);
            } catch (Exception e) {
                Logger.error(DbConnectionFactory.class, "Can't parse String to Integer, value: " + value, e);
            }
        }

        return defaultInteger;
    }

    /**
     * Returns true if the throwable is a constraint violation
     * @param throwable Throwable
     * @return boolean
     */
    public static boolean isConstraintViolationException(final Throwable throwable) {

        boolean isConstraintViolationException = false;

        if (null != throwable && throwable instanceof SQLException) {

            isConstraintViolationException =
                    (throwable instanceof SQLIntegrityConstraintViolationException ||
                            throwable.getClass().getName().contains("IntegrityConstraint")   ||
                            throwable.getMessage().toLowerCase().contains("duplicate"));
        }

        return isConstraintViolationException;
    } // isConstraintViolationException.

    /**
     * Shuts down the default DataSource to stop connection pool threads and prevent memory leaks.
     * This should be called during application shutdown to properly close the HikariCP connection pool.
     */
    public static void shutdownDataSource() {
        if (defaultDataSource != null) {
            try {
                // Check if it's a HikariDataSource and close it properly
                if (defaultDataSource instanceof com.zaxxer.hikari.HikariDataSource) {
                    Logger.info(DbConnectionFactory.class, "Shutting down HikariCP DataSource");
                    ((com.zaxxer.hikari.HikariDataSource) defaultDataSource).close();
                    Logger.info(DbConnectionFactory.class, "HikariCP DataSource shutdown completed");
                } else {
                    Logger.debug(DbConnectionFactory.class, "DataSource is not HikariDataSource, no shutdown needed");
                }
            } catch (Exception e) {
                Logger.warn(DbConnectionFactory.class, "Error shutting down DataSource: " + e.getMessage(), e);
            } finally {
                defaultDataSource = null;
            }
        } else {
            Logger.debug(DbConnectionFactory.class, "No DataSource to shutdown");
        }
    }

    /**
     * Executes an operation with connection management but WITHOUT transaction semantics.
     *
     * <p>This method is designed for read-only operations (SELECT queries) that don't
     * require transaction management. It provides connection lifecycle management only:
     * opens a connection if needed, executes the operation, and closes the connection
     * if it was opened by this call.</p>
     *
     * <p>This is semantically equivalent to the {@code @CloseDBIfOpened} annotation but
     * works correctly on CDI beans where ByteBuddy annotations don't fire due to Weld proxies.</p>
     *
     * <p><b>Usage Example:</b></p>
     * <pre>
     * return DbConnectionFactory.wrapConnection(() -&gt; {
     *     return APILocator.getMetricsAPI().getValue("SELECT COUNT(*) FROM contentlet");
     * });
     * </pre>
     *
     * <p><b>When to use this vs LocalTransaction.wrapReturn():</b></p>
     * <ul>
     *   <li>Use this for read-only SELECT queries that don't need transactions</li>
     *   <li>Use LocalTransaction.wrapReturn() for operations that modify data</li>
     * </ul>
     *
     * @param delegate the operation to execute with connection management
     * @param <T> the return type of the operation
     * @return the result of the operation
     * @throws DotDataException if a database error occurs
     * @see com.dotmarketing.db.CloseDBIfOpened
     * @see com.dotmarketing.db.LocalTransaction#wrapReturn
     */
    public static <T> T wrapConnection(final com.dotcms.util.ReturnableDelegate<T> delegate) throws DotDataException {
        final boolean isNewConnection = !connectionExists();

        try {
            return delegate.execute();
        } catch (final Throwable e) {
            if (e instanceof DotDataException) {
                throw (DotDataException) e;
            }
            throw new DotDataException("Error executing operation with connection", e);
        } finally {
            if (isNewConnection && connectionExists()) {
                // Preserve interrupted status but ensure connection cleanup completes
                // This prevents orphaned connections when timeout fires during cleanup
                final boolean wasInterrupted = Thread.interrupted();
                try {
                    Logger.debug(DbConnectionFactory.class,
                        "Closing connection opened by wrapConnection()");
                    closeSilently();
                } finally {
                    // Restore interrupted flag after cleanup
                    if (wasInterrupted) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }
    }

}
