package com.dotmarketing.db;

import com.dotmarketing.util.Logger;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.service.spi.Configurable;
import org.hibernate.service.spi.Stoppable;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

/**
 * Custom Hibernate ConnectionProvider that integrates with dotCMS's DbConnectionFactory.
 * This provider uses dotCMS's existing DataSource management instead of JNDI lookups.
 * 
 * @author dotCMS
 */
public class DotCMSConnectionProvider implements ConnectionProvider, Configurable, Stoppable {

    private static volatile boolean isConfigured = false;
    private DataSource dataSource;

    @Override
    public void configure(Map configurationValues) {
        try {
            // Get the DataSource from dotCMS's DbConnectionFactory
            this.dataSource = DbConnectionFactory.getDataSource();
            if (!isConfigured) {
                Logger.info(this, "DotCMSConnectionProvider configured successfully with DbConnectionFactory DataSource");
                isConfigured = true;
            } else {
                Logger.debug(this, "DotCMSConnectionProvider configured (additional instance)");
            }
        } catch (Exception e) {
            Logger.error(this, "Failed to configure DotCMSConnectionProvider", e);
            throw new RuntimeException("Unable to configure DotCMSConnectionProvider", e);
        }
    }

    @Override
    public Connection getConnection() throws SQLException {
        if (dataSource == null) {
            throw new SQLException("DataSource not configured in DotCMSConnectionProvider");
        }
        
        try {
            Connection connection = dataSource.getConnection();
            Logger.debug(this, "Provided connection from dotCMS DataSource: " + connection);
            return connection;
        } catch (SQLException e) {
            Logger.error(this, "Failed to get connection from dotCMS DataSource", e);
            throw e;
        }
    }

    @Override
    public void closeConnection(Connection conn) throws SQLException {
        if (conn != null && !conn.isClosed()) {
            Logger.debug(this, "Closing connection: " + conn);
            conn.close();
        }
    }

    @Override
    public boolean supportsAggressiveRelease() {
        // Return true to support aggressive connection release in managed environments
        return true;
    }

    @Override
    public boolean isUnwrappableAs(Class unwrapType) {
        return ConnectionProvider.class.equals(unwrapType) || 
               DotCMSConnectionProvider.class.equals(unwrapType) ||
               DataSource.class.equals(unwrapType);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T unwrap(Class<T> unwrapType) {
        if (ConnectionProvider.class.equals(unwrapType) || 
            DotCMSConnectionProvider.class.equals(unwrapType)) {
            return (T) this;
        } else if (DataSource.class.equals(unwrapType)) {
            return (T) dataSource;
        } else {
            throw new IllegalArgumentException("Cannot unwrap to type: " + unwrapType);
        }
    }

    @Override
    public void stop() {
        Logger.debug(this, "DotCMSConnectionProvider stopped");
        // Don't close the DataSource here - it's managed by dotCMS lifecycle
        dataSource = null;
    }
}