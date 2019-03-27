package com.dotmarketing.db;

import com.dotmarketing.util.Logger;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * @author Jonathan Gamba
 *         Date: 10/15/14
 */
public class DbConnectionUtil {

    /**
     * Locks a given table, any operation needed on that table after the lock must be done
     * on the same connection, a new connection will have to wait until the current connection
     * unlock first the table.
     *
     * @param tableName
     * @return
     */
    public static Connection lockTable ( String tableName ) {

        String lock = null;
        if ( DbConnectionFactory.isPostgres() ) {
            lock = "lock table " + tableName + ";";
        } else if ( DbConnectionFactory.isMySql() ) {
            lock = "lock table " + tableName + " write;";
        } else if ( DbConnectionFactory.isOracle() ) {
            lock = "LOCK TABLE " + tableName + " IN EXCLUSIVE MODE";
        } else if ( DbConnectionFactory.isMsSql() ) {
            lock = "SELECT * FROM " + tableName + " WITH (XLOCK)";
        } else if ( DbConnectionFactory.isH2() ) {
            lock = "SELECT * FROM " + tableName + " FOR UPDATE";
        }

        Statement statement;
        Connection connection = null;

        try {
            Logger.debug( DbConnectionUtil.class, "Locking " + tableName + " table." );

            //Get a connection
            connection = DbConnectionFactory.getDataSource().getConnection();
            connection.setAutoCommit( false );
            statement = connection.createStatement();

            /*
            Locking the table to avoid multiple reads/writes at the same time, avoiding race conditions

            Once obtained, the lock is held for the remainder of the current transaction.
            (Locks are always released at transaction end.)
             */
            statement.execute( lock );
        } catch ( SQLException e ) {

            Logger.fatal( DbConnectionUtil.class, "Locking of " + tableName + " table failed: " + e.getMessage(), e );

            try {
                if ( connection != null && !connection.isClosed() ) {
                    connection.rollback();
                    connection.close();
                }
            } catch ( SQLException sqlException ) {
                Logger.fatal( DbConnectionUtil.class, "Locking of " + tableName + " table failed: ", sqlException );
            }
        }

        return connection;
    }

    /**
     * Unlocks the database tables
     *
     * @param connection
     */
    public static void unlockTable ( Connection connection ) {
        unlockTable( connection, false );
    }

    /**
     * Unlocks the database tables. NOTE: This method will make sure to close the used connection for lock the tables
     *
     * @param connection
     * @param localTransaction
     */
    public static void unlockTable ( Connection connection, Boolean localTransaction ) {

        String unlock = null;
        if ( DbConnectionFactory.isPostgres() ) {
            unlock = "commit;";
        } else if ( DbConnectionFactory.isMySql() ) {
            unlock = "unlock tables";
        } else if ( DbConnectionFactory.isOracle() ) {
            unlock = "COMMIT";
        } else if ( DbConnectionFactory.isMsSql() ) {
            unlock = "COMMIT";
        } else if ( DbConnectionFactory.isH2() ) {
            unlock = "COMMIT";
        }

        try {
            if ( connection != null && !connection.isClosed() ) {

                Statement statement = connection.createStatement();
                if ( DbConnectionFactory.isMySql() ) {
                    statement.execute( unlock );
                }

                if ( !connection.getAutoCommit() ) {
                    connection.commit();//This commit will unlock the tables on most of the dbs (mysql was already handled)
                } else {
                    if ( !DbConnectionFactory.isMySql() ) {
                        statement.execute( unlock );
                    }
                }
            }
        } catch ( Exception e ) {
            Logger.error( DbConnectionUtil.class, "Unlocking of table failed: " + e.getMessage(), e );
        } finally {
            try {

                if ( localTransaction ) {
                    HibernateUtil.closeSession();
                }

                if ( connection != null && !connection.isClosed() ) {
                    connection.close();
                }
            } catch ( Exception e ) {
                Logger.error( DbConnectionUtil.class, "Closing connection while Unlocking of table failed: " + e.getMessage(), e );
            }
        }
    }

}