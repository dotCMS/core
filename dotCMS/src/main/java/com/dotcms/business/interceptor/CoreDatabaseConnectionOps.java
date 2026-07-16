package com.dotcms.business.interceptor;

import com.dotmarketing.db.DbConnectionFactory;

import java.sql.Connection;

/**
 * Core implementation of {@link DatabaseConnectionOps} that delegates to
 * {@link DbConnectionFactory}. This class stays in the core module when the SPI interfaces
 * and handlers are extracted to a utility module.
 */
public final class CoreDatabaseConnectionOps implements DatabaseConnectionOps {

    public static final CoreDatabaseConnectionOps INSTANCE = new CoreDatabaseConnectionOps();

    private CoreDatabaseConnectionOps() { }

    @Override
    public boolean connectionExists() {
        return DbConnectionFactory.connectionExists();
    }

    @Override
    public Connection getConnection() {
        return DbConnectionFactory.getConnection();
    }

    @Override
    public void setConnection(final Connection connection) {
        DbConnectionFactory.setConnection(connection);
    }

    @Override
    public Connection newConnection() throws Exception {
        return DbConnectionFactory.getDataSource().getConnection();
    }

    @Override
    public void closeAndCommit() throws Exception {
        DbConnectionFactory.closeAndCommit();
    }

    @Override
    public void closeSilently() {
        DbConnectionFactory.closeSilently();
    }

    @Override
    public void setAutoCommit(final boolean autoCommit) {
        DbConnectionFactory.setAutoCommit(autoCommit);
    }

    @Override
    public void closeConnection() {
        DbConnectionFactory.closeConnection();
    }
}
